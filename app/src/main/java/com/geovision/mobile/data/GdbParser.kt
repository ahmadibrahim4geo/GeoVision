package com.geovision.mobile.data

import android.content.Context
import android.net.Uri
import com.geovision.mobile.core.AppLogger
import androidx.documentfile.provider.DocumentFile
import com.geovision.mobile.ui.screens.layers.FeatureRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.coroutines.coroutineContext

object GdbParser {
    private const val TAG = "GdbParser"
    private const val MAGIC_HIGH = 0xFDB
    private const val MAX_FEATURES = 50_000

    data class GdbField(val name: String, val type: Int, val length: Int)
    data class GdbGeometry(val geomType: String, val coordinates: String)

    data class GdbTableInfo(
        val tableName: String,
        val fileName: String,
        val estimatedCount: Int
    )

    /**
     * قراءة جميع جداول GeoDatabase (السلوك الافتراضي الحالي).
     */
    suspend fun read(context: Context, treeUri: Uri, fileName: String): GeoJsonParser.ParseResult = withContext(Dispatchers.IO) {
        var tempGdbPath: String? = null
        try {
            coroutineContext.ensureActive()
            tempGdbPath = copyGdbFolder(context, treeUri, fileName)
            if (tempGdbPath == null) {
                return@withContext GeoJsonParser.ParseResult(
                    error = "تعذّر نسخ مجلد GeoDatabase إلى الذاكرة المؤقتة",
                    errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED
                )
            }

            val gdbDir = File(tempGdbPath)
            val tableFiles = getTableFiles(gdbDir)
            if (tableFiles.isEmpty()) {
                return@withContext GeoJsonParser.ParseResult(
                    error = "لم يتم العثور على جداول معالم في GeoDatabase",
                    errorType = GeoJsonParser.ParseErrorType.CORRUPTED
                )
            }

            val result = readTableFiles(tableFiles, fileName, tempGdbPath)
            if (result != null) return@withContext result

            GeoJsonParser.ParseResult(
                error = "فشل قراءة GeoDatabase",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "GDB parse failed: ${e.message}", e)
            GeoJsonParser.ParseResult(
                error = "فشل قراءة GeoDatabase: ${e.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR
            )
        } finally {
            tempGdbPath?.let { try { File(it).deleteRecursively() } catch (_: Exception) {} }
        }
    }

    /**
     * قراءة جداول محددة فقط من GeoDatabase (مشابه لـ GpkgReader.readSelected).
     * @param selectedTableNames قائمة بأسماء الجداول المطلوبة
     */
    suspend fun readSelected(
        context: Context, treeUri: Uri, fileName: String, selectedTableNames: List<String>
    ): GeoJsonParser.ParseResult = withContext(Dispatchers.IO) {
        var tempGdbPath: String? = null
        try {
            coroutineContext.ensureActive()
            tempGdbPath = copyGdbFolder(context, treeUri, fileName)
            if (tempGdbPath == null) {
                return@withContext GeoJsonParser.ParseResult(
                    error = "تعذّر نسخ مجلد GeoDatabase إلى الذاكرة المؤقتة",
                    errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED
                )
            }

            val gdbDir = File(tempGdbPath)
            val allTables = getTableFiles(gdbDir)
            val selectedFiles = allTables.filter { it.name in selectedTableNames }
            if (selectedFiles.isEmpty()) {
                return@withContext GeoJsonParser.ParseResult(
                    error = "لم يتم العثور على الجداول المحددة",
                    errorType = GeoJsonParser.ParseErrorType.CORRUPTED
                )
            }

            val result = readTableFiles(selectedFiles, fileName, tempGdbPath)
            if (result != null) return@withContext result

            GeoJsonParser.ParseResult(
                error = "فشل قراءة الجداول المحددة",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "GDB readSelected failed: ${e.message}", e)
            GeoJsonParser.ParseResult(
                error = "فشل قراءة GeoDatabase: ${e.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR
            )
        } finally {
            tempGdbPath?.let { try { File(it).deleteRecursively() } catch (_: Exception) {} }
        }
    }

    /**
     * قراءة كل جدول مختار على حدة مع إرجاع النتائج مجمّعة حسب اسم الجدول.
     * تستخدم لإنشاء طبقة منفصلة لكل جدول في واجهة المستخدم.
     * @return قائمة أزواج (اسم الجدول, النتيجة)
     */
    suspend fun readEachSelected(
        context: Context, treeUri: Uri, fileName: String, selectedTableNames: List<String>
    ): List<Pair<String, GeoJsonParser.ParseResult>> = withContext(Dispatchers.IO) {
        var tempGdbPath: String? = null
        try {
            coroutineContext.ensureActive()
            tempGdbPath = copyGdbFolder(context, treeUri, fileName)
            if (tempGdbPath == null) return@withContext emptyList()

            val gdbDir = File(tempGdbPath)
            val allTables = getTableFiles(gdbDir)
            val results = mutableListOf<Pair<String, GeoJsonParser.ParseResult>>()

            for (tableName in selectedTableNames) {
                coroutineContext.ensureActive()
                val tableFile = allTables.find { it.name == tableName } ?: continue
                val features = readGdbTable(tableFile)
                if (features.isNotEmpty()) {
                    val t0 = System.currentTimeMillis()
                    var firstCoord = true
                    var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE
                    var maxX = Double.MIN_VALUE; var maxY = Double.MIN_VALUE
                    for (f in features) {
                        val coords = f.geometryCoordinates ?: continue
                        val nums = coords.replace(Regex("[\\[\\]\\s]"), "").split(",")
                        for (i in 0 until nums.size - 1 step 2) {
                            try {
                                val x = nums[i].toDouble(); val y = nums[i + 1].toDouble()
                                if (firstCoord) { minX = x; maxX = x; minY = y; maxY = y; firstCoord = false }
                                else { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y }
                            } catch (_: Exception) {}
                        }
                    }
                    val extent = if (!firstCoord)
                        String.format(java.util.Locale.US, "(%.6f, %.6f) to (%.6f, %.6f)", minX, minY, maxX, maxY)
                    else "—"
                    results.add(tableName to GeoJsonParser.ParseResult(
                        features = features,
                        fileName = "${fileName}/${tableFile.nameWithoutExtension}",
                        crs = "WGS 84 (محسوب)",
                        extent = extent,
                        parseTimeMs = System.currentTimeMillis() - t0
                    ))
                }
            }
            results
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "GDB readEachSelected failed: ${e.message}", e)
            emptyList()
        } finally {
            tempGdbPath?.let { try { File(it).deleteRecursively() } catch (_: Exception) {} }
        }
    }

    /**
     * عرض أسماء الجداول المتاحة في GeoDatabase (مشابه لـ GpkgReader.listLayerNames).
     */
    suspend fun listTableNames(context: Context, treeUri: Uri, fileName: String): List<GdbTableInfo> = withContext(Dispatchers.IO) {
        var tempGdbPath: String? = null
        try {
            coroutineContext.ensureActive()
            tempGdbPath = copyGdbFolder(context, treeUri, fileName)
            if (tempGdbPath == null) return@withContext emptyList()

            val gdbDir = File(tempGdbPath)
            val tableFiles = getTableFiles(gdbDir)
            tableFiles.map { file ->
                val count = estimateFeatureCount(file)
                GdbTableInfo(
                    tableName = file.name,
                    fileName = file.nameWithoutExtension,
                    estimatedCount = count
                )
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "listTableNames failed: ${e.message}")
            emptyList()
        } finally {
            tempGdbPath?.let { try { File(it).deleteRecursively() } catch (_: Exception) {} }
        }
    }

    private fun getTableFiles(gdbDir: File): List<File> {
        return gdbDir.listFiles { f -> f.name.lowercase().endsWith(".gdbtable") && !f.name.startsWith("a00000001") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun estimateFeatureCount(file: File): Int {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 44) return 0
            val bb = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            bb.position(16)
            bb.getInt() // shapeType
            bb.getInt() // numFields
            bb.getInt() // recordCount
        } catch (_: Exception) { 0 }
    }

    private fun readTableFiles(tableFiles: List<File>, fileName: String, tempGdbPath: String?): GeoJsonParser.ParseResult? {
        val t0 = System.currentTimeMillis()
        val allFeatures = mutableListOf<FeatureRow>()
        var firstCoord = true
        var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE; var maxY = Double.MIN_VALUE

        for (tableFile in tableFiles.take(10)) {
            val features = readGdbTable(tableFile)
            allFeatures.addAll(features)

            for (f in features) {
                val coords = f.geometryCoordinates ?: continue
                val nums = coords.replace(Regex("[\\[\\]\\s]"), "").split(",")
                for (i in 0 until nums.size - 1 step 2) {
                    try {
                        val x = nums[i].toDouble(); val y = nums[i + 1].toDouble()
                        if (firstCoord) { minX = x; maxX = x; minY = y; maxY = y; firstCoord = false }
                        else { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y }
                    } catch (_: Exception) {}
                }
            }

            if (allFeatures.size >= MAX_FEATURES) break
        }

        val extent = if (!firstCoord)
            String.format(java.util.Locale.US, "(%.6f, %.6f) to (%.6f, %.6f)", minX, minY, maxX, maxY)
        else "—"

        return GeoJsonParser.ParseResult(
            features = allFeatures,
            fileName = fileName,
            crs = "WGS 84 (محسوب)",
            extent = extent,
            parseTimeMs = System.currentTimeMillis() - t0
        )
    }

    private fun readGdbTable(file: File): List<FeatureRow> {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 40) return emptyList()
            val bb = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)

            val magic = (bb.get().toInt() and 0xFF) or ((bb.get().toInt() and 0xFF) shl 8)
            if (magic != MAGIC_HIGH) {
                AppLogger.w(AppLogger.Tags.PARSER, "Bad magic in ${file.name}")
                return emptyList()
            }

            bb.position(12)
            val shapeType = bb.getInt()
            val numFields = bb.getInt()
            val recordCount = bb.getInt()

            if (recordCount <= 0 || recordCount > 1_000_000) return emptyList()

            bb.position(40)
            val fields = mutableListOf<GdbField>()
            for (i in 0 until numFields.coerceAtMost(100)) {
                if (bb.remaining() < 4) break
                val fieldType = bb.getShort().toInt() and 0xFFFF
                val nameBuf = mutableListOf<Byte>()
                var b = bb.get()
                while (b != 0.toByte() && nameBuf.size < 128) { nameBuf.add(b); b = bb.get() }
                val fieldName = String(nameBuf.toByteArray(), Charsets.UTF_8).trim()
                if (fieldName.isBlank()) continue

                val length = when (fieldType) {
                    0, 1 -> 4       // int16, int32
                    2 -> 8          // float64
                    3 -> 16         // string
                    4 -> 1          // datetime
                    5 -> bb.getInt() // binary/blob
                    6 -> 1          // uuid
                    7 -> 1          // xml
                    else -> 8
                }

                for (j in 0 until 3) if (bb.remaining() > 0) bb.getShort()

                if (fieldType == 5) {
                    val actualLen = bb.getInt()
                    fields.add(GdbField(fieldName, fieldType, actualLen))
                } else if (fieldType == 3) {
                    val strLen = bb.getInt()
                    fields.add(GdbField(fieldName, fieldType, strLen))
                } else {
                    fields.add(GdbField(fieldName, fieldType, length))
                }
            }

            val dataOffset = if (bb.remaining() > 0) {
                bb.position(bb.position() + 4)
                bb.getInt()
            } else bytes.size

            val features = mutableListOf<FeatureRow>()
            var offset = dataOffset

            for (rec in 0 until recordCount.coerceAtMost(MAX_FEATURES)) {
                if (offset + 8 > bytes.size) break
                val recordStart = offset
                val recHeader = java.nio.ByteBuffer.wrap(bytes, offset, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                val recSize = recHeader.getInt()
                val recFlags = recHeader.getInt()
                offset += 8

                if (recSize <= 0 || offset + recSize > bytes.size) break

                val nullMaskLen = (numFields + 7) / 8
                offset += nullMaskLen

                val props = mutableMapOf<String, String>()
                var geom: GdbGeometry? = null

                for (fi in fields.indices) {
                    if (offset >= bytes.size) break
                    val field = fields[fi]

                    if (fi == 0 && field.type == 4) {
                        val ts = readLittleLong(bytes, offset)
                        offset += 8
                        continue
                    }

                    when (field.type) {
                        0 -> { props[field.name] = readLittleShort(bytes, offset).toString(); offset += 2 }
                        1 -> { props[field.name] = readLittleInt(bytes, offset).toString(); offset += 4 }
                        2 -> {
                            val d = java.nio.ByteBuffer.wrap(bytes, offset, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).getDouble()
                            props[field.name] = if (d == d) d.toString() else "null"
                            offset += 8
                        }
                        3 -> {
                            val strLen = field.length.coerceAtMost(2000)
                            if (offset + strLen <= bytes.size) {
                                var end = -1; for (i in offset until (offset + strLen).coerceAtMost(bytes.size)) { if (bytes[i] == 0.toByte()) { end = i; break } }
                                val actualEnd = if (end in (offset + 1)..(offset + strLen)) end else offset + strLen
                                val s = String(bytes.sliceArray(offset until actualEnd.coerceAtMost(bytes.size)), java.nio.charset.StandardCharsets.UTF_8).trim { it <= ' ' }
                                if (s.isNotEmpty()) props[field.name] = s
                                offset += strLen
                            } else offset += field.length
                        }
                        5 -> {
                            val blobLen = if (offset + 4 <= bytes.size) readLittleInt(bytes, offset) else 0
                            offset += 4
                            if (blobLen > 0 && blobLen < 1_000_000 && offset + blobLen <= bytes.size) {
                                geom = parseGeometry(bytes, offset, blobLen)
                            }
                            offset += blobLen
                        }
                        else -> offset += field.length.coerceAtMost(64)
                    }
                }

                val fid = "F${rec + 1}_${file.nameWithoutExtension}"
                if (geom != null) {
                    features.add(FeatureRow(id = fid, properties = props.toMap(), geometryType = geom.geomType, geometryCoordinates = geom.coordinates))
                }
                offset = recordStart + recSize
            }

            features
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Failed to read ${file.name}: ${e.message}")
            emptyList()
        }
    }

    private fun parseGeometry(bytes: ByteArray, offset: Int, length: Int): GdbGeometry? {
        if (length < 40) return null
        return try {
            val bb = java.nio.ByteBuffer.wrap(bytes, offset, length).order(java.nio.ByteOrder.LITTLE_ENDIAN)

            val geomType = bb.getInt()
            bb.getDouble()  // minX
            bb.getDouble()  // minY
            bb.getDouble()  // maxX
            bb.getDouble()  // maxY

            when (geomType) {
                1 -> parsePointGeometry(bb, length)
                3, 5 -> parsePathGeometry(bb, length, geomType)
                8 -> parseMultiPointGeometry(bb, length)
                11, 12, 13, 14 -> parsePointGeometry(bb, length)
                17, 18, 19, 20 -> parsePathGeometry(bb, length, if (geomType in 17..20) 3 else geomType)
                21, 22, 23, 24 -> parsePathGeometry(bb, length, if (geomType in 21..24) 5 else geomType)
                25 -> parseMultiPointGeometry(bb, length)
                else -> { AppLogger.w(AppLogger.Tags.PARSER, "Unsupported geom type: $geomType"); null }
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Geometry parse error: ${e.message}")
            null
        }
    }

    private fun parsePointGeometry(bb: java.nio.ByteBuffer, length: Int): GdbGeometry? {
        if (bb.remaining() < 16) return null
        val x = bb.getDouble(); val y = bb.getDouble()
        return GdbGeometry("Point", "[[$x,$y]]")
    }

    private fun parsePathGeometry(bb: java.nio.ByteBuffer, length: Int, geomType: Int): GdbGeometry? {
        if (bb.remaining() < 8) return null
        val numParts = bb.getInt()
        val numPoints = bb.getInt()
        if (numParts <= 0 || numPoints <= 0 || numParts > 10000 || numPoints > 1000000) return null
        if (bb.remaining() < numParts * 4 + numPoints * 16) return null

        val parts = IntArray(numParts) { bb.getInt() }
        val coords = mutableListOf<String>()
        for (i in 0 until numPoints.coerceAtMost(10000)) {
            if (bb.remaining() < 16) break
            val x = bb.getDouble(); val y = bb.getDouble()
            coords.add("$x,$y")
        }

        val type = if (geomType == 5 || geomType in 21..24) "Polygon" else "LineString"
        val jsonParts = mutableListOf<String>()
        var partStart = 0
        for (i in 0 until numParts.coerceAtMost(parts.size)) {
            val partEnd = if (i + 1 < parts.size) parts[i + 1].coerceAtMost(coords.size) else coords.size
            val pts = coords.subList(partStart.coerceAtMost(coords.size), partEnd).joinToString(",") { "[${it}]" }
            jsonParts.add("[$pts]")
            partStart = partEnd
        }

        val json = if (numParts > 1) "[${jsonParts.joinToString(",")}]" else jsonParts.firstOrNull() ?: "[]"
        return GdbGeometry(type, json)
    }

    private fun parseMultiPointGeometry(bb: java.nio.ByteBuffer, length: Int): GdbGeometry? {
        if (bb.remaining() < 4) return null
        val numPoints = bb.getInt()
        if (numPoints <= 0 || numPoints > 100000) return null
        if (bb.remaining() < numPoints * 16) return null

        val coords = mutableListOf<String>()
        for (i in 0 until numPoints.coerceAtMost(10000)) {
            if (bb.remaining() < 16) break
            val x = bb.getDouble(); val y = bb.getDouble()
            coords.add("[$x,$y]")
        }
        return GdbGeometry("MultiPoint", "[${coords.joinToString(",")}]")
    }

    private fun readLittleShort(bytes: ByteArray, offset: Int): Short {
        if (offset + 2 > bytes.size) return 0
        return java.nio.ByteBuffer.wrap(bytes, offset, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort()
    }

    private fun readLittleInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return java.nio.ByteBuffer.wrap(bytes, offset, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt()
    }

    private fun readLittleLong(bytes: ByteArray, offset: Int): Long {
        if (offset + 8 > bytes.size) return 0
        return java.nio.ByteBuffer.wrap(bytes, offset, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong()
    }

    private fun deriveGeomType(features: List<FeatureRow>): String? {
        if (features.isEmpty()) return null
        val counts = features.groupBy { feat ->
            when (feat.geometryType) {
                "Point", "MultiPoint" -> "Point"
                "LineString", "MultiLineString" -> "Line"
                "Polygon", "MultiPolygon" -> "Polygon"
                else -> null
            }
        }
        val known = counts.filterKeys { it != null }
        if (known.isEmpty()) return null
        return known.maxByOrNull { it.value.size }?.key
    }

    private fun copyGdbFolder(context: Context, treeUri: Uri, fileName: String): String? {
        return try {
            val docFolder = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val tempDir = File(context.cacheDir, "gdb_${System.nanoTime()}.gdb").also { it.mkdirs() }
            copyDocumentTree(context, docFolder, tempDir)
            if (tempDir.listFiles()?.isNotEmpty() == true) tempDir.absolutePath else null
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to copy GDB folder: ${e.message}")
            null
        }
    }

    private fun copyDocumentTree(context: Context, from: DocumentFile, toDir: File): Boolean {
        return try {
            for (child in from.listFiles()) {
                val target = File(toDir, child.name ?: continue)
                if (child.isDirectory) {
                    target.mkdirs()
                    copyDocumentTree(context, child, target)
                } else {
                    val uri = child.uri
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            target.outputStream().use { input.copyTo(it) }
                        }
                    } catch (_: Exception) {}
                }
            }
            true
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Copy error: ${e.message}")
            false
        }
    }
}
