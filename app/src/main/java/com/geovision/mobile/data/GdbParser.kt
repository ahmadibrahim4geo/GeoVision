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

    // FGDB v10 (File Geodatabase) format constants — discovered from binary analysis
    // Files start with a 4-byte LE uint32 version number (3 or 4), NOT 0x0FDB
    // Old code used MAGIC_HIGH = 0xFDB which never matches any real FGDB file
    private const val FGDB_VERSION_3 = 3
    private const val FGDB_VERSION_4 = 4
    private const val FGDB_HEADER_SIZE = 40        // fixed header is always 40 bytes
    private const val FGDB_VALID_ROWS_OFFSET = 4   // uint32 at offset 4 = valid row count
    private const val FGDB_SCHEMA_OFFSET = 40       // schema blob starts at offset 40

    // FGDB v10 field types (from reverse-engineered format)
    private const val FTYPE_INT16    = 0
    private const val FTYPE_INT32    = 1
    private const val FTYPE_FLOAT    = 2
    private const val FTYPE_DOUBLE   = 3
    private const val FTYPE_STRING   = 4   // variable-length UTF-16LE
    private const val FTYPE_DATETIME = 5
    private const val FTYPE_OID      = 6   // ObjectID
    private const val FTYPE_GEOMETRY = 7   // ESRI binary geometry blob
    private const val FTYPE_BINARY   = 8
    private const val FTYPE_UUID     = 9
    private const val FTYPE_GLOBALID = 10
    private const val FTYPE_XML      = 12

    private const val MAX_FEATURES = 50_000

    data class GdbField(val name: String, val type: Int, val length: Int)
    data class GdbGeometry(val geomType: String, val coordinates: String)
    private data class GdbShapeMetadata(
        val xOrigin: Double,
        val yOrigin: Double,
        val xyScale: Double,
        val xMin: Double,
        val yMin: Double,
        val xMax: Double,
        val yMax: Double,
        val crsWkt: String?
    )

    private data class ParsedGdbTable(
        val features: List<FeatureRow>,
        val crsLabel: String
    )

    data class GdbTableInfo(
        val tableName: String,
        val fileName: String,
        val estimatedCount: Int,
        val geometryType: String? = null,
        val catalogPath: String? = null
    ) {
        fun normalizedGeometryType(): String? = normalizeCatalogGeometryType(geometryType)
    }

    private data class GdbCatalogLayer(
        val dsid: Int,
        val name: String,
        val catalogPath: String?,
        val geometryType: String?
    ) {
        val fileName: String get() = "a${dsid.toString(16).padStart(8, '0')}.gdbtable"
    }

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
            val tableInfos = buildTableInfos(gdbDir, getTableFiles(gdbDir))
            val allTables = tableInfos.mapNotNull { File(gdbDir, it.fileName).takeIf { file -> file.exists() } }
            val results = mutableListOf<Pair<String, GeoJsonParser.ParseResult>>()

            for (tableName in selectedTableNames) {
                coroutineContext.ensureActive()
                val info = tableInfos.find { it.fileName == tableName } ?: continue
                val tableFile = allTables.find { it.name == info.fileName } ?: continue
                val displayName = info.tableName
                val parsed = readGdbTableParsed(tableFile, info.normalizedGeometryType())
                val features = parsed.features
                if (features.isNotEmpty()) {
                    val t0 = System.currentTimeMillis()
                    val extent = CoordinateConverter.computeExtent(features)
                    results.add(displayName to GeoJsonParser.ParseResult(
                        features = features,
                        fileName = "${fileName}/$displayName",
                        crs = parsed.crsLabel,
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
            buildTableInfos(gdbDir, tableFiles)
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "listTableNames failed: ${e.message}")
            emptyList()
        } finally {
            tempGdbPath?.let { try { File(it).deleteRecursively() } catch (_: Exception) {} }
        }
    }

    private fun buildTableInfos(gdbDir: File, tableFiles: List<File>): List<GdbTableInfo> {
        val catalogLayers = readCatalogLayers(gdbDir)
        return tableFiles.mapIndexed { index, file ->
            val catalog = catalogLayers.getOrNull(index)
            GdbTableInfo(
                tableName = catalog?.name ?: "Layer ${index + 1}",
                fileName = file.name,
                estimatedCount = estimateFeatureCount(file),
                geometryType = catalog?.geometryType,
                catalogPath = catalog?.catalogPath
            )
        }
    }

    private fun readCatalogLayers(gdbDir: File): List<GdbCatalogLayer> {
        val catalogFile = File(gdbDir, "a00000004.gdbtable")
        if (!catalogFile.exists()) return emptyList()

        return try {
            val text = catalogFile.readBytes().joinToString(separator = "") { byte ->
                val c = byte.toInt() and 0xFF
                if (c == 9 || c == 10 || c == 13 || c in 32..126) c.toChar().toString() else " "
            }
            val byDsid = linkedMapOf<Int, GdbCatalogLayer>()
            val blocks = Regex("""<DEFeatureClassInfo\b.*?</DEFeatureClassInfo>""").findAll(text)

            for (match in blocks) {
                val block = match.value
                if (!block.contains("<DatasetType>esriDTFeatureClass</DatasetType>")) continue
                val dsid = extractCatalogTag(block, "DSID")?.toIntOrNull() ?: continue
                val name = extractCatalogTag(block, "Name")?.takeIf { it.isNotBlank() } ?: continue
                if (isInternalGdbName(name)) continue

                byDsid.putIfAbsent(
                    dsid,
                    GdbCatalogLayer(
                        dsid = dsid,
                        name = name,
                        catalogPath = extractCatalogTag(block, "CatalogPath"),
                        geometryType = extractCatalogTag(block, "ShapeType")?.removePrefix("esriGeometry")
                    )
                )
            }

            byDsid.values.sortedBy { it.dsid }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Failed to read GDB catalog names: ${e.message}")
            emptyList()
        }
    }

    private fun extractCatalogTag(block: String, tag: String): String? {
        return Regex("<$tag>(.*?)</$tag>").find(block)?.groupValues?.get(1)?.trim()
    }

    private fun normalizeCatalogGeometryType(type: String?): String? {
        val normalized = type?.trim()?.removePrefix("esriGeometry")?.lowercase() ?: return null
        return when {
            normalized.contains("polygon") -> "Polygon"
            normalized.contains("polyline") || normalized.contains("line") -> "LineString"
            normalized.contains("multipoint") -> "MultiPoint"
            normalized.contains("point") -> "Point"
            else -> null
        }
    }

    private fun isInternalGdbName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".gdbtable") ||
            lower.endsWith(".gdbtablx") ||
            lower.endsWith(".gdbindexes") ||
            lower.endsWith(".spx") ||
            lower.endsWith(".atx") ||
            lower.endsWith(".freelist") ||
            lower.endsWith(".timestamps") ||
            lower.endsWith(".lock") ||
            Regex("""^a[0-9a-f]{8}(\..*)?$""").matches(lower)
    }

    private fun getTableFiles(gdbDir: File): List<File> {
        // FIXED: only return Feature Class tables — identified by having a .spx spatial index file
        // Old code returned ALL .gdbtable files including 30+ system catalog tables
        // The GDB catalog itself (a00000004) also has .spx but is not a feature class
        val systemPrefixes = setOf(
            "a00000001", "a00000002", "a00000003", "a00000004",
            "a00000005", "a00000006", "a00000007", "a00000008"
        )
        val spxBases = gdbDir.listFiles { f -> f.name.lowercase().endsWith(".spx") }
            ?.map { it.nameWithoutExtension.lowercase() }
            ?.toSet() ?: emptySet()

        return if (spxBases.isNotEmpty()) {
            // Prefer: feature classes are tables with spatial index, excluding system tables
            gdbDir.listFiles { f ->
                val base = f.nameWithoutExtension.lowercase()
                f.name.lowercase().endsWith(".gdbtable") &&
                    base in spxBases &&
                    systemPrefixes.none { prefix -> f.name.lowercase().startsWith(prefix) }
            }?.sortedBy { it.name } ?: emptyList()
        } else {
            // Fallback: exclude known system table ranges (a00000001–a00000008)
            gdbDir.listFiles { f ->
                f.name.lowercase().endsWith(".gdbtable") &&
                    systemPrefixes.none { prefix -> f.name.lowercase().startsWith(prefix) }
            }?.sortedBy { it.name } ?: emptyList()
        }
    }

    private fun estimateFeatureCount(file: File): Int {
        // FIXED: validRows is at offset 4 (uint32 LE), NOT offset 16-24
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 8) return 0
            val version = java.nio.ByteBuffer.wrap(bytes, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt()
            if (version != FGDB_VERSION_3 && version != FGDB_VERSION_4) return 0
            java.nio.ByteBuffer.wrap(bytes, FGDB_VALID_ROWS_OFFSET, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt()
        } catch (_: Exception) { 0 }
    }

    private fun readTableFiles(tableFiles: List<File>, fileName: String, tempGdbPath: String?): GeoJsonParser.ParseResult? {
        val t0 = System.currentTimeMillis()
        val allFeatures = mutableListOf<FeatureRow>()
        val crsLabels = linkedSetOf<String>()

        for (tableFile in tableFiles.take(10)) {
            val parsed = readGdbTableParsed(tableFile)
            val features = parsed.features
            allFeatures.addAll(features)
            crsLabels.add(parsed.crsLabel)

            if (allFeatures.size >= MAX_FEATURES) break
        }

        val extent = CoordinateConverter.computeExtent(allFeatures)

        return GeoJsonParser.ParseResult(
            features = allFeatures,
            fileName = fileName,
            crs = crsLabels.singleOrNull() ?: "FileGDB CRS مختلط أو غير معروف",
            extent = extent,
            parseTimeMs = System.currentTimeMillis() - t0
        )
    }

    /**
     * Reads features from an FGDB v10 .gdbtable file.
     *
     * FGDB v10 Binary Format (from binary analysis of real files):
     *   Offset 0-3:   Version uint32 LE (3 or 4) — NOT a "magic" value
     *   Offset 4-7:   Valid row count uint32 LE
     *   Offset 32-35: Header size = 40 always
     *   Offset 40-43: Schema blob size uint32 LE
     *   Offset 44-47: Number of fields uint32 LE
     *   Offset 48+:   Field definitions (UTF-16LE names with 1-byte length prefix)
     *   Offset 40+schemaSize: Data records
     *
     * Field name encoding: 1-byte char count + UTF-16LE bytes (charCount × 2)
     * Field types: 0=Int16, 1=Int32, 2=Float32, 3=Float64, 4=String(var),
     *              5=DateTime, 6=OID, 7=Geometry(blob), 8=Binary, 9=UUID
     */
    private fun readGdbTableParsed(file: File, expectedGeometryType: String? = null): ParsedGdbTable {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < FGDB_HEADER_SIZE) return emptyParsedGdbTable()

            // ── 1. Validate FGDB version (bytes 0-3) ──
            val version = readLittleInt(bytes, 0)
            if (version != FGDB_VERSION_3 && version != FGDB_VERSION_4) {
                AppLogger.w(AppLogger.Tags.PARSER, "Not FGDB v10 (version=$version) in ${file.name}")
                return emptyParsedGdbTable()
            }

            // ── 2. Read valid row count (bytes 4-7) ──
            val validRows = readLittleInt(bytes, FGDB_VALID_ROWS_OFFSET)
            if (validRows <= 0 || validRows > 1_000_000) {
                AppLogger.w(AppLogger.Tags.PARSER, "Invalid row count ($validRows) in ${file.name}")
                return emptyParsedGdbTable()
            }

            // ── 3. Read schema size and field count (at offset 40) ──
            val schemaSize = readLittleInt(bytes, FGDB_SCHEMA_OFFSET)
            val numFields  = readLittleInt(bytes, FGDB_SCHEMA_OFFSET + 4)

            val schemaEnd = (FGDB_SCHEMA_OFFSET + schemaSize).coerceIn(FGDB_HEADER_SIZE, bytes.size)
            val dataStart = findFgdbDataStart(bytes, schemaEnd, validRows)

            AppLogger.d(AppLogger.Tags.PARSER,
                "FGDB ${file.name}: v$version, rows=$validRows, fields=$numFields, dataStart=$dataStart")

            // ── 4. Parse field schema (UTF-16LE names with 1-byte length prefix) ──
            val fields = parseFgdbFieldSchema(bytes, FGDB_SCHEMA_OFFSET + 8, schemaEnd, numFields)
            val shapeMetadata = parseShapeMetadata(bytes, FGDB_SCHEMA_OFFSET + 8, schemaEnd)
            AppLogger.d(AppLogger.Tags.PARSER, "  Parsed fields: ${fields.map { it.name }}")

            // ── 5. Parse records from data section ──
            val features = parseFgdbRecords(bytes, dataStart, validRows, fields, file.nameWithoutExtension, shapeMetadata, expectedGeometryType)
            buildParsedGdbTable(features, shapeMetadata)

        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Failed to read ${file.name}: ${e.message}")
            emptyParsedGdbTable()
        }
    }

    private fun readGdbTable(file: File): List<FeatureRow> =
        readGdbTableParsed(file).features

    private fun emptyParsedGdbTable(): ParsedGdbTable =
        ParsedGdbTable(emptyList(), "FileGDB CRS غير معروف")

    private fun buildParsedGdbTable(features: List<FeatureRow>, metadata: GdbShapeMetadata?): ParsedGdbTable {
        val crsInfo = metadata?.crsWkt?.let { CrsTransform.parseWkt(it) }
        val converted = if (crsInfo != null && crsInfo.epsg > 0 && !(crsInfo.isWgs84 && crsInfo.isGeographic)) {
            features.map { feature ->
                feature.copy(
                    geometryCoordinates = CoordinateConverter.convertCoords(feature.geometryCoordinates, crsInfo),
                    crs = "EPSG:4326"
                )
            }
        } else {
            features
        }
        val label = when {
            crsInfo == null -> "FileGDB CRS غير معروف"
            crsInfo.epsg == 4326 -> "EPSG:4326 (FileGDB)"
            crsInfo.epsg > 0 -> "EPSG:4326 (محول من EPSG:${crsInfo.epsg} FileGDB)"
            crsInfo.isGeographic -> "FileGDB CRS جغرافي غير مؤكد"
            else -> "FileGDB CRS غير معروف"
        }
        return ParsedGdbTable(converted, label)
    }

    private fun findFgdbDataStart(bytes: ByteArray, schemaEnd: Int, validRows: Int): Int {
        val expectedRows = validRows.coerceAtMost(16)
        var bestStart = (schemaEnd + 4).coerceAtMost(bytes.size)
        var bestCount = -1

        for (candidate in (schemaEnd - 8)..(schemaEnd + 16)) {
            if (candidate < FGDB_HEADER_SIZE || candidate + 4 > bytes.size) continue

            var offset = candidate
            var count = 0
            while (offset + 4 <= bytes.size && count < expectedRows) {
                val rowSize = readLittleInt(bytes, offset)
                if (rowSize <= 0 || rowSize > 1_048_576 || offset + 4 + rowSize > bytes.size) break
                offset += 4 + rowSize
                count++
            }

            if (count > bestCount) {
                bestStart = candidate
                bestCount = count
            }
        }

        return bestStart
    }

    /**
     * Parses FGDB v10 field schema from the schema blob section.
     * Field name format: 1-byte char count + UTF-16LE string (charCount×2 bytes)
     * Followed by: 1-byte null term, 1-byte type, 1-byte width, 1-byte flags
     */
    private fun parseFgdbFieldSchema(bytes: ByteArray, schemaStart: Int, schemaEnd: Int, maxFields: Int): List<GdbField> {
        val fields = mutableListOf<GdbField>()
        val seenNames = mutableSetOf<String>()
        var pos = schemaStart

        while (pos < schemaEnd - 4 && fields.size < 200) {
            // Scan for 1-byte length prefix followed by valid UTF-16LE ASCII chars
            val nameLen = (bytes[pos].toInt() and 0xFF)
            if (nameLen < 2 || nameLen > 64) { pos++; continue }  // skip invalid
            val nameEndBytes = pos + 1 + nameLen * 2
            if (nameEndBytes + 2 > schemaEnd) { pos++; continue }

            // Check all chars are valid ASCII (UTF-16LE: every second byte should be 0x00)
            var validName = true
            val sb = StringBuilder(nameLen)
            for (ci in 0 until nameLen) {
                val lo = bytes[pos + 1 + ci * 2].toInt() and 0xFF
                val hi = bytes[pos + 1 + ci * 2 + 1].toInt() and 0xFF
                if (hi != 0 || lo < 0x20 || lo > 0x7E) { validName = false; break }
                sb.append(lo.toChar())
            }
            if (!validName || sb.isEmpty()) { pos++; continue }

            val fieldName = sb.toString()
            // Expect null terminator after name
            val termByte = (bytes.getOrNull(nameEndBytes)?.toInt() ?: -1) and 0xFF
            if (termByte != 0) { pos++; continue }

            val descriptor = readFieldDescriptor(bytes, nameEndBytes + 1, schemaEnd)
            if (descriptor == null) { pos++; continue }
            val (fieldType, fieldWidth, nextPos) = descriptor
            if (!seenNames.add(fieldName)) {
                pos = nextPos.coerceAtLeast(pos + 1)
                continue
            }

            val fixedLen = when (fieldType) {
                FTYPE_INT16    -> 2
                FTYPE_INT32    -> 4
                FTYPE_OID      -> 4
                FTYPE_FLOAT    -> 4
                FTYPE_DOUBLE   -> 8
                FTYPE_DATETIME -> 8
                FTYPE_UUID, FTYPE_GLOBALID -> 16
                FTYPE_STRING   -> -1  // variable length
                FTYPE_GEOMETRY -> -1  // variable length blob
                FTYPE_BINARY   -> -1  // variable length blob
                else           -> fieldWidth.coerceAtLeast(4)
            }

            fields.add(GdbField(fieldName, fieldType, fixedLen))
            AppLogger.d(AppLogger.Tags.PARSER, "  Field: '$fieldName' type=$fieldType len=$fixedLen")

            pos = nextPos.coerceAtLeast(pos + 1)
        }

        return fields
    }

    private fun readFieldDescriptor(bytes: ByteArray, start: Int, schemaEnd: Int): Triple<Int, Int, Int>? {
        fun descriptorAt(pos: Int): Triple<Int, Int, Int>? {
            if (pos + 2 >= schemaEnd) return null
            val type = bytes[pos].toInt() and 0xFF
            if (type !in 0..12 || type == 11) return null
            val width = bytes[pos + 1].toInt() and 0xFF
            return Triple(type, width, pos + 3)
        }

        descriptorAt(start)?.let { return it }

        val aliasLen = bytes.getOrNull(start)?.toInt()?.and(0xFF) ?: return null
        val aliasEnd = start + 1 + aliasLen * 2
        if (aliasLen in 1..96 && aliasEnd < schemaEnd) {
            descriptorAt(aliasEnd)?.let { return it }
        }
        return null
    }

    private fun parseShapeMetadata(bytes: ByteArray, schemaStart: Int, schemaEnd: Int): GdbShapeMetadata? {
        var pos = schemaStart
        while (pos < schemaEnd - 16) {
            val nameLen = bytes[pos].toInt() and 0xFF
            if (nameLen == 5 && pos + 16 < schemaEnd) {
                val name = try {
                    String(bytes, pos + 1, nameLen * 2, java.nio.charset.StandardCharsets.UTF_16LE)
                } catch (_: Exception) {
                    ""
                }
                if (name.equals("SHAPE", ignoreCase = true)) {
                    val nameEnd = pos + 1 + nameLen * 2
                    val wktLength = readLittleShort(bytes, nameEnd + 4).toInt() and 0xFFFF
                    val wktStart = nameEnd + 6
                    val wktEnd = (wktStart + wktLength).coerceAtMost(schemaEnd)
                    val crsWkt = extractWkt(bytes, wktStart, wktEnd)
                    val metaStart = wktEnd
                    if (metaStart + 113 <= schemaEnd) {
                        return GdbShapeMetadata(
                            xOrigin = readLittleDouble(bytes, metaStart + 1),
                            yOrigin = readLittleDouble(bytes, metaStart + 9),
                            xyScale = readLittleDouble(bytes, metaStart + 17),
                            xMin = readLittleDouble(bytes, metaStart + 81),
                            yMin = readLittleDouble(bytes, metaStart + 89),
                            xMax = readLittleDouble(bytes, metaStart + 97),
                            yMax = readLittleDouble(bytes, metaStart + 105),
                            crsWkt = crsWkt
                        )
                    }
                }
            }
            pos++
        }
        return null
    }

    private fun extractWkt(bytes: ByteArray, start: Int, end: Int): String? {
        if (start < 0 || end <= start || end > bytes.size) return null
        val raw = bytes.copyOfRange(start, end)
        val utf8 = raw.toString(java.nio.charset.StandardCharsets.UTF_8).trim('\u0000', ' ', '\n', '\r', '\t')
        val text = if (utf8.count { it == '\u0000' } > utf8.length / 8) {
            raw.toString(java.nio.charset.StandardCharsets.UTF_16LE).trim('\u0000', ' ', '\n', '\r', '\t')
        } else {
            utf8
        }
        return text.takeIf {
            it.contains("GEOGCS", ignoreCase = true) ||
                it.contains("PROJCS", ignoreCase = true) ||
                it.contains("AUTHORITY", ignoreCase = true)
        }
    }

    /**
     * Parses FGDB v10 data records.
     * Each record: 4-byte size header + null-bitmap + field values.
     * Geometry field (type 7) contains ESRI binary geometry blob.
     */
    private fun parseFgdbRecords(
        bytes: ByteArray, dataStart: Int, validRows: Int,
        fields: List<GdbField>, tableBase: String, shapeMetadata: GdbShapeMetadata?,
        expectedGeometryType: String? = null
    ): List<FeatureRow> {
        val features = mutableListOf<FeatureRow>()
        var offset = dataStart
        var recIndex = 0

        while (offset < bytes.size - 4 && features.size < MAX_FEATURES && recIndex < validRows * 2) {
            recIndex++
            val recordStart = offset

            // Each FileGDB data row starts with its size (4-byte LE int).
            val recSize = readLittleInt(bytes, offset)
            if (recSize <= 0 || recSize > 1_048_576) { offset++; continue }  // skip bad size
            if (offset + 4 + recSize > bytes.size) break
            offset += 4

            val recordDataEnd = recordStart + 4 + recSize

            val props = buildRecordProperties(
                fields = fields,
                recordBytes = bytes,
                recordStart = offset,
                recordEnd = recordDataEnd,
                tableBase = tableBase,
                recIndex = recIndex
            )
            val geom = if (shapeMetadata != null) {
                decodeCompressedGeometry(bytes, offset, recordDataEnd, shapeMetadata, expectedGeometryType)
                    ?: findCompressedGeometry(bytes, offset, recordDataEnd, shapeMetadata, expectedGeometryType)
                    ?: geometryFromAttributeCoordinates(props, expectedGeometryType)
            } else {
                geometryFromAttributeCoordinates(props, expectedGeometryType)
            }

            // Always advance to record end to maintain sync
            offset = recordDataEnd

            val fid = "${tableBase}_${recIndex}"
            if (geom != null) {
                features.add(FeatureRow(id = fid, properties = props, geometryType = geom.geomType, geometryCoordinates = geom.coordinates))
            }
        }

        AppLogger.d(AppLogger.Tags.PARSER, "${tableBase}: extracted ${features.size}/$validRows features")
        return features
    }

    private fun buildRecordProperties(
        fields: List<GdbField>,
        recordBytes: ByteArray,
        recordStart: Int,
        recordEnd: Int,
        tableBase: String,
        recIndex: Int
    ): MutableMap<String, String> {
        val props = linkedMapOf(
            "gdb_table" to tableBase,
            "gdb_record" to recIndex.toString()
        )
        val values = extractRecordTextValues(recordBytes, recordStart, recordEnd)
        val dataFields = fields
            .filterNot { it.type == FTYPE_OID || it.type == FTYPE_GEOMETRY }
            .filterNot { it.name.equals("SHAPE", ignoreCase = true) }

        var valueIndex = 0
        for (field in dataFields) {
            if (valueIndex >= values.size) break
            props[field.name] = values[valueIndex++]
        }
        while (valueIndex < values.size) {
            props["gdb_value_${valueIndex + 1}"] = values[valueIndex]
            valueIndex++
        }

        addCoordinatePropertiesFromText(props, values)
        return props
    }

    private fun extractRecordTextValues(bytes: ByteArray, start: Int, end: Int): List<String> {
        val values = mutableListOf<String>()
        var pos = start
        while (pos < end - 2) {
            val len = bytes[pos].toInt() and 0xFF
            if (len in 2..180 && pos + 1 + len <= end) {
                val raw = bytes.copyOfRange(pos + 1, pos + 1 + len)
                val printable = raw.count { byte ->
                    val c = byte.toInt() and 0xFF
                    c in 32..126 || c == 9 || c == 10 || c == 13
                }
                if (printable >= (len * 0.85).toInt()) {
                    val text = raw.toString(Charsets.UTF_8)
                        .trim('\u0000', ' ', '\n', '\r', '\t')
                    if (text.length >= 2 && text.any { it.isLetterOrDigit() }) {
                        values.add(text)
                        pos += 1 + len
                        continue
                    }
                }
            }
            pos++
        }
        return values.distinct()
    }

    private fun addCoordinatePropertiesFromText(props: MutableMap<String, String>, values: List<String>) {
        val joined = values.joinToString("*")
        val pairs = Regex("""(\d{5,}(?:\.\d+)?)-(\d{5,}(?:\.\d+)?)""")
            .findAll(joined)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
        if (pairs.isEmpty()) return
        props.putIfAbsent("X_Start", pairs[0].first)
        props.putIfAbsent("Y_Start", pairs[0].second)
        if (pairs.size > 1) {
            props.putIfAbsent("X_End", pairs[1].first)
            props.putIfAbsent("Y_End", pairs[1].second)
        }
    }

    private fun geometryFromAttributeCoordinates(
        props: Map<String, String>,
        expectedGeometryType: String?
    ): GdbGeometry? {
        val expected = normalizeCatalogGeometryType(expectedGeometryType) ?: return null
        val xStart = props["X_Start"]?.toDoubleOrNull() ?: return null
        val yStart = props["Y_Start"]?.toDoubleOrNull() ?: return null
        if (expected == "Point") return GdbGeometry("Point", "[$xStart,$yStart]")
        val xEnd = props["X_End"]?.toDoubleOrNull() ?: return null
        val yEnd = props["Y_End"]?.toDoubleOrNull() ?: return null
        return when (expected) {
            "LineString" -> GdbGeometry("LineString", "[[$xStart,$yStart],[$xEnd,$yEnd]]")
            "Polygon" -> {
                val minX = minOf(xStart, xEnd)
                val maxX = maxOf(xStart, xEnd)
                val minY = minOf(yStart, yEnd)
                val maxY = maxOf(yStart, yEnd)
                if (minX == maxX || minY == maxY) null else {
                    GdbGeometry("Polygon", "[[[$minX,$minY],[$maxX,$minY],[$maxX,$maxY],[$minX,$maxY],[$minX,$minY]]]")
                }
            }
            else -> null
        }
    }

    private fun findCompressedGeometry(
        bytes: ByteArray,
        recordStart: Int,
        recordEnd: Int,
        metadata: GdbShapeMetadata?,
        expectedGeometryType: String? = null
    ): GdbGeometry? {
        if (metadata == null || metadata.xyScale == 0.0) return null
        val scanEnd = (recordStart + 64).coerceAtMost(recordEnd - 4)
        for (pos in recordStart until scanEnd) {
            val blobLen = readLittleInt(bytes, pos)
            if (blobLen <= 0 || pos + 4 + blobLen > recordEnd) continue
            decodeCompressedGeometry(bytes, pos + 4, pos + 4 + blobLen, metadata, expectedGeometryType)?.let { return it }
        }
        return null
    }

    private fun decodeCompressedGeometry(
        bytes: ByteArray,
        start: Int,
        end: Int,
        metadata: GdbShapeMetadata,
        expectedGeometryType: String?
    ): GdbGeometry? {
        val expected = normalizeCatalogGeometryType(expectedGeometryType)
        if (expected == null || expected == "Point" || expected == "MultiPoint") {
            return decodeCompressedPoint(bytes, start, end, metadata)
        }

        val points = decodeCompressedPathPoints(bytes, start, end, metadata)
        if (expected == "LineString" && points.size >= 2) {
            return GdbGeometry("LineString", "[${points.joinToString(",") { "[${it.first},${it.second}]" }}]")
        }
        if (expected == "Polygon" && points.size >= 3) {
            val ring = points.toMutableList()
            if (ring.first() != ring.last()) ring.add(ring.first())
            return GdbGeometry("Polygon", "[[${ring.joinToString(",") { "[${it.first},${it.second}]" }}]]")
        }
        return null
    }

    private data class VarValue(val value: Long, val offset: Int, val next: Int)

    private fun decodeCompressedPathPoints(
        bytes: ByteArray,
        start: Int,
        end: Int,
        metadata: GdbShapeMetadata
    ): List<Pair<Double, Double>> {
        val values = readVarValues(bytes, start, end, 2048)
        if (values.size < 4) return emptyList()

        val firstIndex = values.indices.firstOrNull { i ->
            i + 1 < values.size && pointFromRaw(values[i].value, values[i + 1].value, metadata) != null
        } ?: return emptyList()
        val pointCount = inferCompressedPointCount(values, firstIndex)
        val first = pointFromRaw(values[firstIndex].value, values[firstIndex + 1].value, metadata) ?: return emptyList()
        val points = mutableListOf(first)
        var index = firstIndex + 2

        while (index + 1 < values.size && points.size < pointCount.coerceIn(2, 100_000)) {
            val next = chooseNextCompressedPoint(points.last(), values[index].value, values[index + 1].value, metadata)
                ?: break
            if (next != points.last()) points.add(next)
            index += 2
        }
        return points
    }

    private fun inferCompressedPointCount(values: List<VarValue>, firstCoordinateIndex: Int): Int {
        for (i in firstCoordinateIndex - 1 downTo 0) {
            val value = values[i].value
            if (value > 1 && value <= 100_000) return value.toInt()
        }
        return 2
    }

    private fun chooseNextCompressedPoint(
        previous: Pair<Double, Double>,
        rawX: Long,
        rawY: Long,
        metadata: GdbShapeMetadata
    ): Pair<Double, Double>? {
        val candidates = listOfNotNull(
            pointFromDelta(previous, rawX, rawY, metadata, signed = false),
            pointFromDelta(previous, rawX, rawY, metadata, signed = true),
            pointFromRaw(rawX, rawY, metadata),
            pointFromRaw(zigZagDecode(rawX), zigZagDecode(rawY), metadata)
        )
        return candidates.minByOrNull { squaredDistance(previous, it) }
    }

    private fun pointFromDelta(
        previous: Pair<Double, Double>,
        rawX: Long,
        rawY: Long,
        metadata: GdbShapeMetadata,
        signed: Boolean
    ): Pair<Double, Double>? {
        val dx = if (signed) zigZagDecode(rawX) else rawX
        val dy = if (signed) zigZagDecode(rawY) else rawY
        if (kotlin.math.abs(dx) > 10_000_000L || kotlin.math.abs(dy) > 10_000_000L) return null
        val point = previous.first + dx.toDouble() / metadata.xyScale to
            previous.second + dy.toDouble() / metadata.xyScale
        return point.takeIf { isWithinShapeExtent(it, metadata) }
    }

    private fun pointFromRaw(rawX: Long, rawY: Long, metadata: GdbShapeMetadata): Pair<Double, Double>? {
        val point = rawX.toDouble() / metadata.xyScale + metadata.xOrigin to
            rawY.toDouble() / metadata.xyScale + metadata.yOrigin
        return point.takeIf { isWithinShapeExtent(it, metadata) }
    }

    private fun isWithinShapeExtent(point: Pair<Double, Double>, metadata: GdbShapeMetadata): Boolean {
        val xMargin = ((metadata.xMax - metadata.xMin).coerceAtLeast(1.0)) * 0.25
        val yMargin = ((metadata.yMax - metadata.yMin).coerceAtLeast(1.0)) * 0.25
        return point.first.isFinite() && point.second.isFinite() &&
            point.first in (metadata.xMin - xMargin)..(metadata.xMax + xMargin) &&
            point.second in (metadata.yMin - yMargin)..(metadata.yMax + yMargin)
    }

    private fun squaredDistance(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val dx = a.first - b.first
        val dy = a.second - b.second
        return dx * dx + dy * dy
    }

    private fun zigZagDecode(value: Long): Long = (value ushr 1) xor -(value and 1L)

    private fun readVarValues(bytes: ByteArray, start: Int, end: Int, maxValues: Int): List<VarValue> {
        var pos = start
        val values = mutableListOf<VarValue>()
        while (pos < end && values.size < maxValues) {
            val read = readVarUInt(bytes, pos, end) ?: break
            values.add(VarValue(read.first, pos, read.second))
            pos = read.second
            if (pos < end) {
                val b = bytes[pos].toInt() and 0xFF
                if (b in 32..126 && values.size > 8) {
                    var printableRun = 0
                    var probe = pos
                    while (probe < end && printableRun < 8) {
                        val c = bytes[probe].toInt() and 0xFF
                        if (c !in 32..126) break
                        printableRun++
                        probe++
                    }
                    if (printableRun >= 4) break
                }
            }
        }
        return values
    }

    private fun decodeCompressedPoint(
        bytes: ByteArray,
        start: Int,
        end: Int,
        metadata: GdbShapeMetadata
    ): GdbGeometry? {
        var pos = start
        val values = mutableListOf<Long>()
        while (pos < end && values.size < 12) {
            val read = readVarUInt(bytes, pos, end) ?: break
            values.add(read.first)
            pos = read.second
        }

        for (i in 0 until values.size - 1) {
            val x = values[i].toDouble() / metadata.xyScale + metadata.xOrigin
            val y = values[i + 1].toDouble() / metadata.xyScale + metadata.yOrigin
            val xMargin = ((metadata.xMax - metadata.xMin).coerceAtLeast(1.0)) * 0.25
            val yMargin = ((metadata.yMax - metadata.yMin).coerceAtLeast(1.0)) * 0.25
            if (x.isFinite() && y.isFinite() &&
                x in (metadata.xMin - xMargin)..(metadata.xMax + xMargin) &&
                y in (metadata.yMin - yMargin)..(metadata.yMax + yMargin)
            ) {
                return GdbGeometry("Point", "[$x,$y]")
            }
        }
        return null
    }

    private fun readVarUInt(bytes: ByteArray, start: Int, end: Int): Pair<Long, Int>? {
        var pos = start
        var shift = 0
        var result = 0L
        while (pos < end && shift < 63) {
            val b = bytes[pos].toInt() and 0xFF
            pos++
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) return result to pos
            shift += 7
        }
        return null
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

        val json = if (type == "Polygon") {
            "[${jsonParts.joinToString(",")}]"
        } else if (numParts > 1) {
            "[${jsonParts.joinToString(",")}]"
        } else {
            jsonParts.firstOrNull() ?: "[]"
        }
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

    private fun readLittleDouble(bytes: ByteArray, offset: Int): Double {
        if (offset + 8 > bytes.size) return Double.NaN
        return java.nio.ByteBuffer.wrap(bytes, offset, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).getDouble()
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
            val gdbRoot = findCopiedGdbRoot(tempDir)
            if (gdbRoot != null && gdbRoot.listFiles()?.isNotEmpty() == true) {
                gdbRoot.absolutePath
            } else {
                AppLogger.w(
                    AppLogger.Tags.PARSER,
                    "Copied GDB tree has no FileGDB tables. selected=$fileName, copied=${tempDir.listFiles()?.joinToString { it.name }}"
                )
                null
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to copy GDB folder: ${e.message}")
            null
        }
    }

    private fun findCopiedGdbRoot(root: File): File? {
        if (root.hasGdbTables()) return root
        val directGdbFolders = root.listFiles { file -> file.isDirectory && file.name.lowercase().endsWith(".gdb") }
            ?.filter { it.hasGdbTables() }
            .orEmpty()
        if (directGdbFolders.isNotEmpty()) return directGdbFolders.first()
        return root.walkTopDown()
            .maxDepth(3)
            .filter { it.isDirectory && it.hasGdbTables() }
            .firstOrNull()
    }

    private fun File.hasGdbTables(): Boolean {
        return listFiles()?.any { it.isFile && it.name.lowercase().endsWith(".gdbtable") } == true
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
