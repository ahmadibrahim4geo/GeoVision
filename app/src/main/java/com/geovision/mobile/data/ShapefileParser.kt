package com.geovision.mobile.data

import android.net.Uri
import android.provider.DocumentsContract
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

// =============================================================================
// Shapefile Format Parser (.shp + .dbf)
// محلل تنسيق Shapefile (ملفات .shp و .dbf)
// =============================================================================
//
// DESCRIPTION / الوصف:
// Shapefile is a popular geospatial vector data format consisting of multiple files:
// Shapefile هو تنسيق بيانات جغرافية متجهة شائع يتكون من عدة ملفات:
//   - .shp : Geometric data in binary format
//            البيانات الهندسية بتنسيق ثنائي
//   - .dbf : Attribute table (xBase format)
//            جدول الخصائص (تنسيق xBase)
//   - .shx : Shape index (optional but recommended)
//            مؤشر الأشكال (اختياري لكن موصى به)
//   - .prj : Projection information (optional)
//            معلومات الإسقاط (اختياري)
//
// PERFORMANCE OPTIMIZATIONS (v2) / تحسينات الأداء (v2):
//   - Streaming mode replaces readBytes() - file no longer fully loaded into RAM
//     الوضع المتدفق بدلاً من readBytes() - الملف لا يُحمّل بالكامل في الذاكرة
//   - Support for content:// URI paths for DBF files
//     دعم مسارات URI من نوع content:// لملفات DBF
//   - Charset auto-detection (UTF-8, CP1256, etc.)
//     الكشف التلقائي للترميز (UTF-8, CP1256, إلخ)
//   - Structured error messages for diagnosis
//     رسائل أخطاء منظمة للتشخيص
//   - Supports cancellation via ensureActive()
//     دعم إلغاء العملية عبر ensureActive()
//
// KNOWN LIMITATIONS / القيود المعروفة:
//   - Some complex geometry types may not be fully supported
//     بعض أنواع الهندسة المعقدة قد لا تكون مدعومة بالكامل
//   - DBF encoding detection is heuristic-based
//     الكشف عن ترميز DBF يعتمد على الحدس
//   - Very large shapefiles (>500MB) require high-end devices
//     الملفات الكبيرة جداً (>500 ميجابايت) تتطلب أجهزة قوية
// =============================================================================
object ShapefileParser {
    private const val TAG = "ShapefileParser"
    private const val SHP_HEADER_SIZE = 100
    private const val RECORD_HEADER_SIZE = 8

    // ── هيكل رأس ملف SHP ──
    data class ShpHeader(
        val shapeType: Int,
        val xMin: Double, val yMin: Double,
        val xMax: Double, val yMax: Double,
        val fileLengthBytes: Int  // الحجم الكلي للملف بالبايت (من الرأس)
    )

    // ── التحليل المتدفق (Streaming) ──
    /**
     * تحليل متدفق (streaming) لملف SHP مع إمكانية تمرير سجلات DBF محلّلة مسبقاً.
     * ShapefileImporter يقرأ DBF أولاً (باكتشاف الترميز UTF-8/CP1256) ويمررها عبر preParsedDbf
     * لتجنب إعادة فتح ملف DBF مرتين.
     * @param shpStream دفق ملف .shp
     * @param filePath مسار الملف (للتسجيل فقط)
     * @param dbfUri مسار ملف .dbf (يُستخدم إن لم يُمرر preParsedDbf)
     * @param context سياق التطبيق
     * @param preParsedDbf سجلات DBF محلّلة مسبقاً (تتجاوز قراءة DBF من URI)
     * @return ParseResult يحتوي على المعالم مع خصائصها
     */
    suspend fun streamParse(
        shpStream: InputStream,
        filePath: String = "",
        dbfUri: Uri? = null,
        context: android.content.Context? = null,
        preParsedDbf: List<Map<String, String>>? = null
    ): GeoJsonParser.ParseResult {
        val startTime = System.nanoTime()
        val bis = if (shpStream is BufferedInputStream) shpStream else BufferedInputStream(shpStream)

        return try {
            // ── 1. قراءة رأس الملف (100 بايت) ──
            val headerBytes = ByteArray(SHP_HEADER_SIZE)
            var offset = 0
            while (offset < SHP_HEADER_SIZE) {
                val read = bis.read(headerBytes, offset, SHP_HEADER_SIZE - offset)
                if (read < 0) return GeoJsonParser.ParseResult(
                    error = "ملف SHP تالف: الملف أقصر من رأس الملف (100 بايت)",
                    errorType = GeoJsonParser.ParseErrorType.CORRUPTED
                )
                offset += read
            }

            val headerBuf = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN)
            val fileCode = headerBuf.getInt(0)
            if (fileCode != 9994) return GeoJsonParser.ParseResult(
                error = "ملف SHP تالف: توقيع غير صالح (code=$fileCode). تأكد من أن الملف بصيغة Shapefile صحيحة.",
                errorType = GeoJsonParser.ParseErrorType.CORRUPTED
            )

            val fileLengthWords = headerBuf.getInt(24) // طول الملف بوحدات 16-bit
            val fileLengthBytes = fileLengthWords * 2
            val shapeType = ByteBuffer.wrap(headerBytes, 32, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()
            if (shapeType == 0) return GeoJsonParser.ParseResult(
                error = "ملف SHP تالف: نوع الهندسة فارغ (Null shape).",
                errorType = GeoJsonParser.ParseErrorType.CORRUPTED
            )

            // أنواع الهندسة المدعومة
            val supportedTypes = setOf(1, 3, 5, 8, 11, 13, 15, 18)
            if (shapeType !in supportedTypes) return GeoJsonParser.ParseResult(
                error = "نوع الهندسة $shapeType غير مدعوم في Shapefile. الأنواع المدعومة: Point(1), Polyline(3), Polygon(5), MultiPoint(8), PointZ(11), PolylineZ(13), PolygonZ(15), MultiPointZ(18).",
                errorType = GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT
            )

            val xMin = ByteBuffer.wrap(headerBytes, 36, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble()
            val yMin = ByteBuffer.wrap(headerBytes, 44, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble()
            val xMax = ByteBuffer.wrap(headerBytes, 52, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble()
            val yMax = ByteBuffer.wrap(headerBytes, 60, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble()

            val header = ShpHeader(shapeType, xMin, yMin, xMax, yMax, fileLengthBytes)

            // ── 2. البحث عن ملف DBF ──
            val dbfRecords = preParsedDbf ?: readDbfRecords(filePath, dbfUri, context)

            // ── 3. قراءة السجلات بشكل متدفق ──
            val features = mutableListOf<FeatureRow>()
            var featureIdx = 0
            var recordCount = 0
            val recordContentBuf = ByteArray(8192) // مخزن مؤقت لمحتوى السجل (يُوسّع عند الحاجة)

            while (true) {
                coroutineContext.ensureActive()

                // قراءة رأس السجل (8 بايت: recordNumber + contentLength)
                val recHeader = ByteArray(RECORD_HEADER_SIZE)
                var recOffset = 0
                while (recOffset < RECORD_HEADER_SIZE) {
                    val r = bis.read(recHeader, recOffset, RECORD_HEADER_SIZE - recOffset)
                    if (r < 0) break // نهاية الملف
                    recOffset += r
                }
                if (recOffset < RECORD_HEADER_SIZE) break // انتهى الملف

                val recBuf = ByteBuffer.wrap(recHeader).order(ByteOrder.BIG_ENDIAN)
                val contentLengthWords = recBuf.getInt(4) // طول المحتوى بوحدات 16-bit
                val contentLengthBytes = contentLengthWords * 2
                if (contentLengthBytes <= 0 || contentLengthBytes > 10_000_000) {
                    AppLogger.w(AppLogger.Tags.PARSER, "تخطّي سجل غير صالح: طول=$contentLengthBytes")
                    break
                }

                // توسيع المخزن المؤقت إذا لزم الأمر
                val content = if (contentLengthBytes > recordContentBuf.size) {
                    ByteArray(contentLengthBytes)
                } else {
                    recordContentBuf
                }

                // قراءة محتوى السجل
                var contentRead = 0
                while (contentRead < contentLengthBytes) {
                    val r = bis.read(content, contentRead, contentLengthBytes - contentRead)
                    if (r < 0) {
                        AppLogger.w(AppLogger.Tags.PARSER, "نهاية غير متوقعة للملف أثناء قراءة السجل $recordCount")
                        break
                    }
                    contentRead += r
                }
                if (contentRead < contentLengthBytes) break

                // تحليل السجل (Little-Endian للمحتوى)
                val contentBuf = ByteBuffer.wrap(content, 0, contentLengthBytes).order(ByteOrder.LITTLE_ENDIAN)
                val recShapeType = contentBuf.getInt(0)

                // ربط السجل الحالي بالسجل المقابل من DBF (نفس الترتيب)
                val properties = if (featureIdx < dbfRecords.size) dbfRecords[featureIdx] else emptyMap()
                featureIdx++

                val fid = propertyValueIgnoreCase(properties, "id")
                    ?: propertyValueIgnoreCase(properties, "name")
                    ?: "F-${features.size + 1}"

                // حد أقصى للمعالم لتجنب OOM
                if (features.size >= GeoJsonParser.MAX_FEATURES) {
                    AppLogger.w(AppLogger.Tags.PARSER, "تم تجاوز الحد الأقصى للمعالم (${GeoJsonParser.MAX_FEATURES})")
                    break
                }

                when (recShapeType) {
                    1 -> parsePoint(contentBuf, 4, fid, properties, features)
                    3 -> parsePolyline(contentBuf, 4, fid, properties, features, "LineString")
                    5 -> parsePolyline(contentBuf, 4, fid, properties, features, "Polygon")
                    8 -> parseMultiPoint(contentBuf, 4, fid, properties, features)
                    11 -> parsePointZ(contentBuf, 4, fid, properties, features)
                    13 -> parsePolylineZ(contentBuf, 4, fid, properties, features, "LineString")
                    15 -> parsePolylineZ(contentBuf, 4, fid, properties, features, "Polygon")
                    18 -> parseMultiPointZ(contentBuf, 4, fid, properties, features)
                    else -> {
                        // نوع هندسة غير مدعوم — نسجّل ونتجاوز
                        AppLogger.d(AppLogger.Tags.PARSER, "نوع هندسة غير مدعوم: $recShapeType في السجل $recordCount")
                    }
                }

                recordCount++
                if (recordCount % 50 == 0) {
                    yield()
                    coroutineContext.ensureActive()
                }

                // محاذاة الكلمات: أحياناً يكون طول السجل فردياً، نضيف 1 للمحاذاة
                // لكن هذا لا ينطبق على القراءة المتدفقة لأننا نقرأ بالضبط contentLengthBytes
            }

            // ── 4. صياغة النتيجة ──
            val extent = if (header.xMin.isFinite())
                java.lang.String.format(java.util.Locale.US, "%.4f", header.yMin) + "\u00B0 \u2014 " +
                java.lang.String.format(java.util.Locale.US, "%.4f", header.yMax) + "\u00B0 | " +
                java.lang.String.format(java.util.Locale.US, "%.4f", header.xMin) + "\u00B0 \u2014 " +
                java.lang.String.format(java.util.Locale.US, "%.4f", header.xMax) + "\u00B0"
            else "\u2014"

            val fName = filePath.substringAfterLast('/').substringAfterLast('\\')
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000

            if (features.isEmpty()) {
                GeoJsonParser.ParseResult(
                    error = "لم يُعثر على أي معالم صالحة في ملف Shapefile. تأكد من أن الملف يحتوي على بيانات.",
                    errorType = GeoJsonParser.ParseErrorType.INVALID_GEOMETRY
                )
            } else {
                GeoJsonParser.ParseResult(
                    features = features, fileName = fName,
                    crs = "EPSG:4326 (Shapefile default)", extent = extent,
                    parseTimeMs = elapsedMs
                )
            }
        } catch (e: OutOfMemoryError) {
            GeoJsonParser.ParseResult(
                error = "الملف كبير جداً: تجاوز سعة الذاكرة. حاول استخدام ملف أصغر أو قلّل عدد المعالم.",
                errorType = GeoJsonParser.ParseErrorType.OUT_OF_MEMORY
            )
        } catch (e: Exception) {
            GeoJsonParser.ParseResult(
                error = "فشل تحليل Shapefile: ${e.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR
            )
        } finally {
            try { bis.close() } catch (_: Exception) {}
        }
    }

    // ── قراءة سجلات DBF (مع دعم مسارات content://) ──
    /**
     * يقرأ سجلات DBF من ملف .dbf المرافق لملف SHP.
     * يدعم:
     *   - مسارات ملفات نظامية (file://)
     *   - مسارات content:// URI (SAF) — عبر ContentResolver
     *   - URI مُمرَّر مباشرة كمعامل dbfUri
     *
     * إذا تعذر العثور على ملف DBF، نُعيد قائمة فارغة (shapefile بدون خصائص)
     * مع تسجيل تحذير في السجل.
     */
    private suspend fun readDbfRecords(filePath: String, dbfUri: Uri?, context: android.content.Context? = null): List<Map<String, String>> {
        // 1. إذا كان هناك URI صريح، نستخدمه
        if (dbfUri != null && dbfUri.toString().isNotBlank()) {
            try {
                if (context != null && dbfUri.toString().startsWith("content://")) {
                    context.contentResolver.openInputStream(dbfUri)?.use { input ->
                        val bytes = input.readBytes()
                        val header = readDbfHeader(bytes) ?: return emptyList()
                        val fields = readDbfFields(bytes, header.headerLength)
                        if (fields.isNotEmpty()) return readDbfRecords(bytes, header, fields)
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. البحث عن ملف نظامي
        try {
            val dbfFile = findDbfFile(filePath) ?: return emptyList()
            val bytes = dbfFile.readBytes()
            val header = readDbfHeader(bytes) ?: return emptyList()
            val fields = readDbfFields(bytes, header.headerLength)
            if (fields.isEmpty()) return emptyList()
            return readDbfRecords(bytes, header, fields)
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "فشل قراءة DBF: ${e.message}")
            return emptyList()
        }
    }

    // ── Point (النوع 1) ──
    private fun parsePoint(buf: ByteBuffer, pos: Int, fid: String, props: Map<String, String>, out: MutableList<FeatureRow>) {
        val x = buf.getDouble(pos); val y = buf.getDouble(pos + 8)
        if (x.isFinite() && y.isFinite()) {
            val coordStr = "[$x,$y]"
            out.add(FeatureRow(id = fid, properties = props, geometryType = "Point", geometryCoordinates = coordStr))
        }
    }

    // ── PointZ (النوع 11) ──
    private fun parsePointZ(buf: ByteBuffer, pos: Int, fid: String, props: Map<String, String>, out: MutableList<FeatureRow>) {
        val x = buf.getDouble(pos); val y = buf.getDouble(pos + 8); val z = buf.getDouble(pos + 16)
        if (x.isFinite() && y.isFinite()) {
            val coordStr = if (z.isFinite()) "[$x,$y,$z]" else "[$x,$y]"
            out.add(FeatureRow(id = fid, properties = props, geometryType = "Point", geometryCoordinates = coordStr))
        }
    }

    // ── Polyline (3) / Polygon (5) ──
    private fun parsePolyline(buf: ByteBuffer, pos: Int, fid: String, props: Map<String, String>, out: MutableList<FeatureRow>, geomType: String) {
        val numParts = buf.getInt(pos + 32)
        val numPoints = buf.getInt(pos + 36)
        if (numParts <= 0 || numPoints <= 0 || numPoints > 10_000_000) return

        val parts = IntArray(numParts)
        for (i in 0 until numParts) parts[i] = buf.getInt(pos + 40 + i * 4)

        val pointsStart = pos + 40 + numParts * 4
        val allCoords = mutableListOf<String>()

        if (geomType == "Polygon") {
            for (p in 0 until numParts) {
                val start = parts[p]
                val end = if (p + 1 < numParts) parts[p + 1] else numPoints
                if (start >= numPoints || end > numPoints) continue
                val ringCoords = mutableListOf<String>()
                for (j in start until end) {
                    val x = buf.getDouble(pointsStart + j * 16)
                    val y = buf.getDouble(pointsStart + j * 16 + 8)
                    if (x.isFinite() && y.isFinite()) ringCoords.add("[$x,$y]")
                }
                if (ringCoords.isNotEmpty()) allCoords.add("[${ringCoords.joinToString(",")}]")
            }
            if (allCoords.isNotEmpty()) {
                val coordStr = "[${allCoords.joinToString(",")}]"
                out.add(FeatureRow(id = fid, properties = props, geometryType = "Polygon", geometryCoordinates = coordStr))
            }
        } else {
            for (p in 0 until numParts) {
                val start = parts[p]
                val end = if (p + 1 < numParts) parts[p + 1] else numPoints
                if (start >= numPoints || end > numPoints) continue
                val ptCoords = mutableListOf<String>()
                for (j in start until end) {
                    val x = buf.getDouble(pointsStart + j * 16)
                    val y = buf.getDouble(pointsStart + j * 16 + 8)
                    if (x.isFinite() && y.isFinite()) ptCoords.add("[$x,$y]")
                }
                if (ptCoords.isNotEmpty()) {
                    val partId = if (numParts > 1) "${fid}_part${p + 1}" else fid
                    val coordStr = "[${ptCoords.joinToString(",")}]"
                    out.add(FeatureRow(id = partId, properties = props, geometryType = "LineString", geometryCoordinates = coordStr))
                }
            }
        }
    }

    // ── PolylineZ (13) / PolygonZ (15) ──
    private fun parsePolylineZ(buf: ByteBuffer, pos: Int, fid: String, props: Map<String, String>, out: MutableList<FeatureRow>, geomType: String) {
        val numParts = buf.getInt(pos + 32)
        val numPoints = buf.getInt(pos + 36)
        if (numParts <= 0 || numPoints <= 0 || numPoints > 10_000_000) return

        val parts = IntArray(numParts)
        for (i in 0 until numParts) parts[i] = buf.getInt(pos + 40 + i * 4)

        val pointsStart = pos + 40 + numParts * 4
        val zArrayStart = pointsStart + numPoints * 16 + 16
        val allCoords = mutableListOf<String>()

        for (p in 0 until numParts) {
            val start = parts[p]
            val end = if (p + 1 < numParts) parts[p + 1] else numPoints
            if (start >= numPoints || end > numPoints) continue
            val ptCoords = mutableListOf<String>()
            for (j in start until end) {
                val x = buf.getDouble(pointsStart + j * 16)
                val y = buf.getDouble(pointsStart + j * 16 + 8)
                val z = buf.getDouble(zArrayStart + j * 8)
                val coord = if (z.isFinite()) "[$x,$y,$z]" else "[$x,$y]"
                ptCoords.add(coord)
            }
            if (ptCoords.isNotEmpty()) {
                val partId = if (numParts > 1) "${fid}_part${p + 1}" else fid
                val coordStr = "[${ptCoords.joinToString(",")}]"
                val gType = if (geomType == "Polygon") "Polygon" else "LineString"
                out.add(FeatureRow(id = partId, properties = props, geometryType = gType, geometryCoordinates = coordStr))
            }
        }
    }

    // ── MultiPoint (8) ──
    private fun parseMultiPoint(buf: ByteBuffer, pos: Int, fid: String, props: Map<String, String>, out: MutableList<FeatureRow>) {
        val numPoints = buf.getInt(pos + 32)
        if (numPoints <= 0 || numPoints > 10_000_000) return
        val pts = mutableListOf<String>()
        for (j in 0 until numPoints) {
            val x = buf.getDouble(pos + 36 + j * 16)
            val y = buf.getDouble(pos + 36 + j * 16 + 8)
            if (x.isFinite() && y.isFinite()) pts.add("[$x,$y]")
        }
        if (pts.isNotEmpty()) {
            val coordStr = "[[${pts.joinToString(",")}]]"
            out.add(FeatureRow(id = fid, properties = props, geometryType = "MultiPoint", geometryCoordinates = coordStr))
        }
    }

    // ── MultiPointZ (18) ──
    private fun parseMultiPointZ(buf: ByteBuffer, pos: Int, fid: String, props: Map<String, String>, out: MutableList<FeatureRow>) {
        val numPoints = buf.getInt(pos + 32)
        if (numPoints <= 0 || numPoints > 10_000_000) return
        val zArrayStart = pos + 36 + numPoints * 16 + 16
        val pts = mutableListOf<String>()
        for (j in 0 until numPoints) {
            val x = buf.getDouble(pos + 36 + j * 16)
            val y = buf.getDouble(pos + 36 + j * 16 + 8)
            val z = buf.getDouble(zArrayStart + j * 8)
            val coord = if (z.isFinite()) "[$x,$y,$z]" else "[$x,$y]"
            pts.add(coord)
        }
        if (pts.isNotEmpty()) {
            val coordStr = "[[${pts.joinToString(",")}]]"
            out.add(FeatureRow(id = fid, properties = props, geometryType = "MultiPoint", geometryCoordinates = coordStr))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // قارئ ملفات DBF (xBase)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * يبحث عن ملف DBF بنفس اسم ملف SHP.
     * @param shpPath المسار الكامل لملف .shp
     * @return كائن File لملف .dbf أو null إذا لم يُعثر عليه
     */
    private fun findDbfFile(shpPath: String): File? {
        val baseName = shpPath.removeSuffix(".shp").removeSuffix(".SHP")
        if (baseName == shpPath) return null
        for (ext in listOf(".dbf", ".DBF")) {
            val f = File("$baseName$ext")
            if (f.exists()) return f
        }
        return null
    }

    private data class DbfHeader(
        val numRecords: Int,
        val headerLength: Int,
        val recordLength: Int
    )

    private data class FieldDescriptor(
        val name: String,
        val type: Char,
        val length: Int
    )

    private fun propertyValueIgnoreCase(properties: Map<String, String>, key: String): String? {
        return properties.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    /**
     * يحوّل محتوى DBF (بايتات) إلى سجلات خصائص.
     * يدعم ترميزات متعددة: ShapefileImporter يجرب UTF-8 → CP1256 (عربي) → ISO-8859-1.
     * يُستخدم عندما يكون DBF متاحاً كـ byte[] من LayerViewModel أو ShapefileImporter
     * (لمسارات content:// URI التي لا يمكن فتحها مرتين).
     * @param dbfBytes محتوى ملف DBF كاملاً
     * @param charsetName اسم الترميز (مثل "UTF-8", "CP1256", "ISO-8859-1")
     */
    fun parseDbfBytes(dbfBytes: ByteArray, charsetName: String = "UTF-8"): List<Map<String, String>> {
        return try {
            val header = readDbfHeader(dbfBytes) ?: return emptyList()
            val fields = readDbfFields(dbfBytes, header.headerLength)
            if (fields.isEmpty()) return emptyList()
            readDbfRecords(dbfBytes, header, fields, charsetName)
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "فشل تحليل بايتات DBF: ${e.message}")
            emptyList()
        }
    }

    private fun readDbfHeader(bytes: ByteArray): DbfHeader? {
        if (bytes.size < 32 || bytes[0] != 0x03.toByte() && bytes[0] != 0x83.toByte()) return null
        val numRecords = ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()
        val headerLength = ByteBuffer.wrap(bytes, 8, 2).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt() and 0xFFFF
        val recordLength = ByteBuffer.wrap(bytes, 10, 2).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt() and 0xFFFF
        if (headerLength < 32 || recordLength <= 0) return null
        return DbfHeader(numRecords, headerLength, recordLength)
    }

    private fun readDbfFields(bytes: ByteArray, headerLength: Int): List<FieldDescriptor> {
        val fields = mutableListOf<FieldDescriptor>()
        var pos = 32
        while (pos + 32 <= headerLength) {
            val terminator = bytes[pos].toInt() and 0xFF
            if (terminator == 0x0D) break
            val nameBytes = mutableListOf<Byte>()
            for (i in 0 until 11) {
                val b = bytes[pos + i]
                if (b.toInt() == 0) break
                nameBytes.add(b)
            }
            val name = String(nameBytes.toByteArray(), Charsets.UTF_8).trim()
            val type = bytes[pos + 11].toInt().toChar()
            val length = bytes[pos + 16].toInt() and 0xFF
            if (name.isNotEmpty() && (type == 'C' || type == 'N' || type == 'F' || type == 'L' || type == 'D') && length > 0) {
                fields.add(FieldDescriptor(name, type, length))
            }
            pos += 32
        }
        return fields
    }

    private fun readDbfRecords(bytes: ByteArray, header: DbfHeader, fields: List<FieldDescriptor>, charsetName: String = "UTF-8"): List<Map<String, String>> {
        val records = mutableListOf<Map<String, String>>()
        val charset = java.nio.charset.Charset.forName(charsetName)
        var pos = header.headerLength
        for (i in 0 until header.numRecords) {
            if (pos + header.recordLength > bytes.size) break
            val deleted = bytes[pos] == 0x2A.toByte()
            pos++
            val props = mutableMapOf<String, String>()
            for (field in fields) {
                if (pos + field.length > bytes.size) { pos = bytes.size; break }
                val value = String(bytes, pos, field.length, charset).trim { it <= ' ' }
                pos += field.length
                if (!deleted) {
                    props[field.name] = value
                }
            }
            if (!deleted) records.add(props)
        }
        return records
    }
}
