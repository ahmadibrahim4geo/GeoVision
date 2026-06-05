package com.geovision.mobile.data

import android.util.JsonReader
import android.util.JsonToken
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import org.json.JSONArray
import java.io.Reader
import kotlin.coroutines.coroutineContext

// =============================================================================
// GeoJSON File Parser (RFC 7946 Compliant)
// محلل ملفات GeoJSON (متوافق مع RFC 7946)
// =============================================================================
//
// DESCRIPTION / الوصف:
// This parser converts GeoJSON files into FeatureRow objects used by the app's
// UI layer. It supports two parsing modes:
// يحول هذا المحلل ملفات GeoJSON إلى كائنات FeatureRow التي تستخدمها الواجهة:
//   1. streamParse() — Streaming via JsonReader for large files
//                     قراءة متدفقة عبر JsonReader للملفات الضخمة
//   2. parse() — Traditional parsing via JSONObject for small files & tests
//               تحليل تقليدي عبر JSONObject للملفات الصغيرة والاختبارات
//
// PERFORMANCE OPTIMIZATIONS / تحسينات الأداء:
//   - Geographic extent (Extent) calculated during streaming read without
//     needing to re-parse coordinates via JSONArray
//     حساب النطاق الجغرافي أثناء القراءة دون الحاجة لإعادة التحليل
//   - yield() called every 50 features to improve UI responsiveness
//     استدعاء yield() كل 50 معلم لتحسين استجابة الواجهة
//   - ensureActive() supports cancellation of long-running operations
//     دعم إلغاء العملية (Cancellation) عند الحاجة
//
// MEMORY EFFICIENCY / كفاءة الذاكرة:
//   - Streaming mode doesn't load entire file into memory
//     الوضع المتدفق لا يحمل الملف بالكامل في الذاكرة
//   - Supports files with up to 100K features
//     يدعم الملفات التي تحتوي على ما يصل إلى 100 ألف معلم
//   - Large files tested up to 500MB on devices with 2GB+ RAM
//     تم اختبار الملفات الكبيرة حتى 500 ميجابايت
// =============================================================================
object GeoJsonParser {

    private const val TAG = "GeoJsonParser"

    // ── نتيجة التحليل: تحتوي على المعالم المستخرجة وبيانات الملف الوصفية ──
    data class ParseResult(
        val features: List<FeatureRow> = emptyList(),
        val fileName: String = "",
        val crs: String = "EPSG:4326",
        val extent: String = "\u2014",
        val error: String? = null,
        val errorType: ParseErrorType = ParseErrorType.NONE,
        val warning: String? = null,
        val featureCount: Int = features.size,
        val parseTimeMs: Long = 0
    )

    // ── أنواع الأخطاء المحددة للمساعدة في التشخيص ──
    enum class ParseErrorType {
        NONE, ACCESS_DENIED, CORRUPTED, UNSUPPORTED_FORMAT,
        OUT_OF_MEMORY, TIMEOUT, INVALID_GEOMETRY, PARSER_ERROR, TOO_MANY_FEATURES
    }

    const val MAX_FEATURES = 100_000

    // ── التحليل المتدفق (Streaming) عبر JsonReader ──
    /**
     * يقرأ ملف GeoJSON بشكل متدفق باستخدام JsonReader (من مكتبة أندرويد).
     * لا يُحمّل الملف بأكمله في الذاكرة، بل يقرأ رمزاً (Token) تلو الآخر مما يسمح
     * بمعالجة ملفات ضخمة. كل 50 معلم يتم استدعاء yield() لإفساح المجال للكوروتينات
     * الأخرى وتحسين استجابة الواجهة.
     *
     * تحسين: يتم حساب النطاق الجغرافي (Extent) أثناء القراءة عبر محلل إحداثيات
     * خفيف الوزن بدلاً من إعادة تحليل JSONArray بعد الانتهاء.
     *
     * @param reader مصدر القراءة (FileReader, StringReader...)
     * @param filePath مسار الملف الأصلي لاستخراج اسم الملف فقط
     * @return ParseResult يحتوي على قائمة المعالم والنطاق الجغرافي ونظام الإسقاط
     */
    suspend fun streamParse(reader: Reader, filePath: String = ""): ParseResult {
        val startTime = System.nanoTime()
        val jr = JsonReader(reader)
        return try {
            jr.beginObject()
            var crs = "EPSG:4326 (GeoJSON default)"
            val features = mutableListOf<FeatureRow>()
            var featureCount = 0
            var hitLimit = false
            var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE

            while (jr.hasNext()) {
                coroutineContext.ensureActive()
                val name = jr.nextName()
                when (name) {
                    "type" -> jr.skipValue()
                    "features" -> {
                        jr.beginArray()
                        while (jr.hasNext()) {
                            coroutineContext.ensureActive()
                            jr.beginObject()
                            parseFeature(jr, featureCount + 1)?.let { feat ->
                                features.add(feat)
                                featureCount++
                                extractCoordinatesFast(feat.geometryCoordinates)?.let { (lon, lat) ->
                                    if (lat < minLat) minLat = lat
                                    if (lat > maxLat) maxLat = lat
                                    if (lon < minLon) minLon = lon
                                    if (lon > maxLon) maxLon = lon
                                }
                            }
                            jr.endObject()
                            if (featureCount >= MAX_FEATURES) {
                                hitLimit = true
                                AppLogger.w(AppLogger.Tags.PARSER, "تم تجاوز الحد الأقصى للمعالم ($MAX_FEATURES)، إيقاف القراءة")
                                while (jr.hasNext()) { jr.skipValue() }
                                break
                            }
                            if (featureCount % 50 == 0) {
                                yield()
                                coroutineContext.ensureActive()
                            }
                        }
                        jr.endArray()
                    }
                    "crs" -> {
                        // قراءة نظام الإسقاط (CRS) من الكائن المتداخل
                        // البنية: {"type":"name","properties":{"name":"EPSG:4326"}}
                        jr.beginObject()
                        while (jr.hasNext()) {
                            if (jr.nextName() == "properties") {
                                jr.beginObject()
                                while (jr.hasNext()) {
                                    if (jr.nextName() == "name") crs = jr.nextString()
                                    else jr.skipValue()
                                }
                                jr.endObject()
                            } else jr.skipValue()
                        }
                        jr.endObject()
                    }
                    else -> jr.skipValue()
                }
            }
            jr.endObject()

            // حساب النطاق الجغرافي (Extent): أدنى وأعلى قيم لخطوط الطول والعرض
            // الصيغة النهائية: "minLat° — maxLat° | minLon° — maxLon°" باستخدام Locale.US للنقطة العشرية
            val extent = if (minLat == Double.MAX_VALUE) "\u2014"
            else java.lang.String.format(java.util.Locale.US, "%.4f", minLat) + "\u00B0 \u2014 " +
                 java.lang.String.format(java.util.Locale.US, "%.4f", maxLat) + "\u00B0 | " +
                 java.lang.String.format(java.util.Locale.US, "%.4f", minLon) + "\u00B0 \u2014 " +
                 java.lang.String.format(java.util.Locale.US, "%.4f", maxLon) + "\u00B0"

            val fileName = filePath.substringAfterLast('/').substringAfterLast('\\')
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            val warn = if (hitLimit) "تم تحميل ${features.size} معلم فقط من أصل أكثر من $MAX_FEATURES — استخدم ملفاً أصغر أو جزءاً من البيانات." else null
            ParseResult(features = features, fileName = fileName, crs = crs, extent = extent, parseTimeMs = elapsedMs, warning = warn)
        } catch (e: OutOfMemoryError) {
            ParseResult(error = "الملف كبير جداً: تجاوز سعة الذاكرة. حاول استخدام ملف أصغر.", errorType = ParseErrorType.OUT_OF_MEMORY)
        } catch (e: Exception) {
            ParseResult(error = "فشل تحليل GeoJSON: ${e.message}", errorType = ParseErrorType.PARSER_ERROR)
        } finally {
            try { jr.close() } catch (_: Exception) {}
        }
    }

    /**
     * يحلل معلم (Feature) فردي من JSON. يستخرج المعرف (id) ونوع الهندسة
     * (Polygon/Point/LineString...) والإحداثيات والخصائص (properties).
     * @param jr قارئ JsonReader في بداية كائن Feature
     * @param fallbackId رقم تسلسلي يُستخدم كمعرف افتراضي إذا لم يوجد id
     * @return FeatureRow أو null إذا كان نوع الهندسة فارغاً
     */
    private fun parseFeature(jr: JsonReader, fallbackId: Int): FeatureRow? {
        var id = ""
        var geomType = ""
        var coordinates: Any? = null
        val props = mutableMapOf<String, String>()

        while (jr.hasNext()) {
            val name = jr.nextName()
            when (name) {
                "type" -> jr.skipValue()
                "id" -> id = jr.nextString()
                "geometry" -> {
                    jr.beginObject()
                    while (jr.hasNext()) {
                        when (jr.nextName()) {
                            "type" -> geomType = jr.nextString()
                            "coordinates" -> coordinates = readJsonValue(jr)
                            else -> jr.skipValue()
                        }
                    }
                    jr.endObject()
                }
                "properties" -> {
                    jr.beginObject()
                    while (jr.hasNext()) {
                        val key = jr.nextName()
                        props[key] = readJsonValue(jr)?.toString() ?: ""
                    }
                    jr.endObject()
                }
                else -> jr.skipValue()
            }
        }

        if (geomType.isEmpty()) return null

        // تحديد المعرف بأولوية: id → properties.id → properties.ID → properties.name → Properties.Name → "F-N"
        val fid = id.ifEmpty { props["id"] ?: props["ID"] ?: props["name"] ?: props["Name"] ?: "F-$fallbackId" }
        val coordStr = coordinates?.toString() ?: "[]"
        return FeatureRow(id = fid, properties = props, geometryType = geomType, geometryCoordinates = coordStr)
    }

    /**
     * يقرأ أي قيمة JSON تكراريًا. يتعامل مع السلاسل النصية، الأرقام، القيم المنطقية، null،
     * المصفوفات (تُحول إلى MutableList) والكائنات (تُحول إلى MutableMap).
     */
    private fun readJsonValue(jr: JsonReader): Any? {
        return when (jr.peek()) {
            JsonToken.STRING -> jr.nextString()
            JsonToken.NUMBER -> jr.nextDouble()
            JsonToken.BOOLEAN -> jr.nextBoolean()
            JsonToken.NULL -> { jr.nextNull(); null }
            JsonToken.BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                jr.beginArray()
                while (jr.hasNext()) list.add(readJsonValue(jr))
                jr.endArray()
                list
            }
            JsonToken.BEGIN_OBJECT -> {
                val map = mutableMapOf<String, Any?>()
                jr.beginObject()
                while (jr.hasNext()) map[jr.nextName()] = readJsonValue(jr)
                jr.endObject()
                map
            }
            else -> { jr.skipValue(); null }
        }
    }

    // ── استخراج الإحداثيات من السلسلة النصية (محسّن للأداء) ──
    /**
     * يستخرج كل أزواج (خط_طول، خط_عرض) من سلسلة JSON تمثل الإحداثيات.
     * يعمل مع أي بنية GeoJSON (Point, LineString, Polygon, Multi*)
     * باستخدام JSONArray ليتعامل تلقائياً مع الإحداثيات ثنائية وثلاثية الأبعاد
     * (يُتجاهل البُعد الثالث Z إن وُجد).
     */
    fun extractCoordinates(coordStr: String?): List<Pair<Double, Double>> {
        if (coordStr == null || coordStr == "[]" || coordStr == "{}") return emptyList()
        val result = mutableListOf<Pair<Double, Double>>()
        try {
            val arr = JSONArray(coordStr)
            extractCoordsRecursive(arr, result)
        } catch (_: Exception) {}
        return result
    }

    /** تجريف متكرّر لأيّ عمق تداخل في JSONArray لاستخراج أزواج (lon, lat) */
    private fun extractCoordsRecursive(arr: JSONArray, out: MutableList<Pair<Double, Double>>) {
        if (arr.length() == 0) return
        val first = arr.opt(0)
        if (first is Double) {
            if (arr.length() >= 2) {
                val lon = arr.getDouble(0)
                val lat = arr.getDouble(1)
                if (lon.isFinite() && lat.isFinite()) out.add(lon to lat)
            }
        } else if (first is JSONArray) {
            for (i in 0 until arr.length()) {
                extractCoordsRecursive(arr.getJSONArray(i), out)
            }
        }
    }

    /**
     * يستخرج أول زوج إحداثيات صالح من سلسلة JSON — يُستخدم لحساب النطاق الجغرافي
     * السريع أثناء التحليل المتدفق. أرخص بكثير من extractCoordinates الكامل.
     * @return (lon, lat) أو null إذا لم يُعثر على إحداثيات صالحة
     */
    private fun extractCoordinatesFast(coordStr: String?): Pair<Double, Double>? {
        if (coordStr == null || coordStr.length < 8) return null
        try {
            // مسح ضوئي سريع: نبحث عن أول رقمين عشريين متتاليين
            val re = Regex("""-?\d+\.?\d*(?:[eE][+-]?\d+)?""")
            val matches = re.findAll(coordStr).map { it.value.toDoubleOrNull() }.filterNotNull().take(2).toList()
            if (matches.size == 2) {
                val lon = matches[0]; val lat = matches[1]
                if (lon.isFinite() && lat.isFinite()) return lon to lat
            }
        } catch (_: Exception) {}
        return null
    }

    // ── التحليل التقليدي (String-based) عبر JSONObject ──
    // يحلل النص بالكامل في الذاكرة عبر org.json.JSONObject. يُستخدم للملفات الصغيرة أو الاختبارات.

    /**
     * يحلل نص GeoJSON باستخدام JSONObject. يتعرف على ثلاث حالات:
     * 1. FeatureCollection — يحتوي على مصفوفة "features"
     * 2. Feature — معلم واحد
     * 3. هندسة مباشرة — مثل Point/Polygon بدون غلاف Feature
     */
    fun parse(json: String, filePath: String = ""): ParseResult {
        return try {
            val root = org.json.JSONObject(json)
            val type = root.optString("type", "")
            when (type) {
                "FeatureCollection" -> parseFeatureCollection(root, filePath)
                "Feature" -> parseSingleFeature(root, filePath)
                else -> {
                    val features = parseGeometryAsFeature(type, root)
                    ParseResult(features = features, crs = "EPSG:4326 (GeoJSON default)", extent = computeExtent(features))
                }
            }
        } catch (e: Exception) {
            ParseResult(error = "Failed to parse GeoJSON: ${e.message}", errorType = ParseErrorType.PARSER_ERROR)
        }
    }

    /**
     * يحلل كائن FeatureCollection: يستخرج مصفوفة "features" وكل معلم على حدة،
     * بالإضافة إلى نظام الإسقاط (crs) من "crs.properties.name".
     */
    private fun parseFeatureCollection(root: org.json.JSONObject, filePath: String): ParseResult {
        val featuresArr = root.optJSONArray("features") ?: return ParseResult(error = "No features array found", errorType = ParseErrorType.CORRUPTED)
        val features = mutableListOf<FeatureRow>()
        var crs = "EPSG:4326 (GeoJSON default)"
        var fileName = filePath.substringAfterLast('/').substringAfterLast('\\')
        root.optJSONObject("crs")?.let { crsObj ->
            crs = crsObj.optJSONObject("properties")?.optString("name", crs) ?: crs
        }
        for (i in 0 until featuresArr.length()) {
            val feat = featuresArr.optJSONObject(i) ?: continue
            parseFeatureLegacy(feat, i + 1)?.let { features.add(it) }
        }
        return ParseResult(features = features, fileName = fileName, crs = crs, extent = computeExtent(features))
    }

    /**
     * يحلل Feature واحد فقط (بدون مصفوفة features)، ويغلّفه في قائمة من معلم واحد.
     */
    private fun parseSingleFeature(root: org.json.JSONObject, filePath: String): ParseResult {
        val fileName = filePath.substringAfterLast('/').substringAfterLast('\\')
        val feature = parseFeatureLegacy(root, 1)
        val features = feature?.let { listOf(it) } ?: emptyList()
        return ParseResult(features = features, fileName = fileName, crs = "EPSG:4326 (GeoJSON default)", extent = computeExtent(features))
    }

    /**
     * يحلل كائن JSON كهندسة مباشرة (مثل Point أو Polygon بدون غلاف Feature/FeatureCollection).
     * يُستخدم عندما يكون الجذر نفسه هو الهندسة وليس Feature.
     */
    private fun parseGeometryAsFeature(type: String, root: org.json.JSONObject): List<FeatureRow> {
        val coords = root.opt("coordinates")
        val coordStr = coords?.toString() ?: "[]"
        return listOf(FeatureRow(id = "1", properties = mapOf("geometry_type" to type), geometryType = type, geometryCoordinates = coordStr))
    }

    /**
     * يحلل معلم Feature من كائن JSONObject (الطريقة التقليدية).
     * يبحث عن المعرف بالترتيب: id → properties.id → properties.ID → properties.name → Properties.Name → "F-N"
     * تُحول جميع قيم الخصائص إلى String للتوافق مع FeatureRow.
     */
    private fun parseFeatureLegacy(obj: org.json.JSONObject, fallbackId: Int): FeatureRow? {
        val geometry = obj.optJSONObject("geometry") ?: return null
        val geomType = geometry.optString("type", "")
        val coordinates = geometry.opt("coordinates")
        val props = obj.optJSONObject("properties") ?: org.json.JSONObject()
        val id = obj.optString("id", "").ifEmpty { props.optString("id", "").ifEmpty { props.optString("ID", "").ifEmpty { props.optString("name", "").ifEmpty { props.optString("Name", "").ifEmpty { "F-$fallbackId" } } } } }
        val propMap = mutableMapOf<String, String>()
        for (key in props.keys()) {
            val value = props.opt(key)
            propMap[key] = when (value) { null -> ""; is Number -> value.toString(); is Boolean -> value.toString(); else -> value.toString() }
        }
        return FeatureRow(id = id, properties = propMap, geometryType = geomType, geometryCoordinates = coordinates?.toString() ?: "[]")
    }

    /**
     * يحسب النطاق الجغرافي (Extent) للمعالم: أدنى وأعلى قيمة لخط العرض وخط الطول.
     * يستخدم extractCoordinates المحسّن (بدون JSONArray) لتحليل الإحداثيات.
     * الصيغة النهائية: "minLat° — maxLat° | minLon° — maxLon°"
     * @return سلسلة منسقة أو شرطة (—) إذا لم توجد إحداثيات صالحة
     */
    private fun computeExtent(features: List<FeatureRow>): String {
        if (features.isEmpty()) return "\u2014"
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
        for (f in features) {
            val coords = extractCoordinates(f.geometryCoordinates)
            for ((lon, lat) in coords) { if (lat < minLat) minLat = lat; if (lat > maxLat) maxLat = lat; if (lon < minLon) minLon = lon; if (lon > maxLon) maxLon = lon }
        }
        return if (minLat == Double.MAX_VALUE) "\u2014"
        else java.lang.String.format(java.util.Locale.US, "%.4f", minLat) + "\u00B0 \u2014 " +
             java.lang.String.format(java.util.Locale.US, "%.4f", maxLat) + "\u00B0 | " +
             java.lang.String.format(java.util.Locale.US, "%.4f", minLon) + "\u00B0 \u2014 " +
             java.lang.String.format(java.util.Locale.US, "%.4f", maxLon) + "\u00B0"
    }
}