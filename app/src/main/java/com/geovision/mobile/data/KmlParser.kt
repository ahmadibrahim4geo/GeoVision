package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader
import kotlin.coroutines.coroutineContext

// =============================================================================
// محلل ملفات KML (Keyhole Markup Language)
// =============================================================================
// KML هو تنسيق XML يستخدمه Google Earth لعرض البيانات الجغرافية.
// =============================================================================
// تحسينات الأداء (v2):
//   1. قراءة متدفقة (Streaming-first) — يتم تمرير Reader إلى XmlPullParser مباشرة
//      دون تحميل الملف بالكامل في الذاكرة. فقط في حال فشل التحليل بسبب أخطاء XML
//      نقرأ النص بالكامل ونصلح أحرف & ثم نعيد المحاولة.
//   2. إضافة ensureActive() لدعم إلغاء العملية (Cancellation).
//   3. تحسين رسائل الخطأ لتكون أكثر تحديداً.
//   4. دعم كامل لـ Style و styleUrl (الألوان والعرض والأيقونات).
// =============================================================================

/**
 * KML Style properties extracted from <Style> elements.
 */
data class KmlStyle(
    val id: String = "",
    var polyColor: String? = null,
    var polyFill: Boolean = true,
    var lineColor: String? = null,
    var lineWidth: Float = 1f,
    var iconColor: String? = null,
    var iconHref: String? = null,
    var labelColor: String? = null,
    var labelScale: Float = 1f
) {
    /**
     * Convert KML aabbggrr color format to hex rrggbbaa used by the app.
     */
    fun parseKmlColor(kmlColor: String?): String? {
        if (kmlColor == null || kmlColor.length < 8) return null
        return try {
            val a = kmlColor.substring(0, 2)
            val b = kmlColor.substring(2, 4)
            val g = kmlColor.substring(4, 6)
            val r = kmlColor.substring(6, 8)
            "#$r$g$b$a"
        } catch (_: Exception) { null }
    }

    override fun toString(): String {
        return listOfNotNull(
            polyColor?.let { "poly=${parseKmlColor(it)}" },
            lineColor?.let { "line=${parseKmlColor(it)}" },
            if (lineWidth != 1f) "width=$lineWidth" else null,
            iconHref?.let { "icon=$it" }
        ).joinToString(";")
    }
}

object KmlParser {
    private const val TAG = "KmlParser"

    // ── التحليل المتدفق (Streaming-first) ──
    /**
     * يقرأ ملف KML بشكل متدفق باستخدام XmlPullParser.
     *
     * الإستراتيجية:
     *   1. المحاولة الأولى: نمرر Reader مباشرة (قراءة متدفقة، لا تحميل كامل).
     *   2. إذا فشلت بسبب أخطاء XML: نقرأ النص بالكامل، نصلح أحرف & غير الصالحة،
     *      ثم نعيد المحاولة (هذه هي الحالة النادرة للملفات التالفة).
     *
     * @param reader مصدر القراءة
     * @param fileName اسم الملف للعرض
     * @return ParseResult يحتوي على المعالم المستخرجة
     */
    suspend fun streamParse(reader: Reader, fileName: String = ""): GeoJsonParser.ParseResult {
        val startTime = System.nanoTime()

        // المحاولة الأولى: قراءة متدفقة عبر BufferedReader
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader, 8192)
        try {
            val result = parseFromReader(bufferedReader, fileName, startTime)
            // إذا نجحت المحاولة، نعيد النتيجة فوراً
            if (result.error == null || result.errorType != GeoJsonParser.ParseErrorType.PARSER_ERROR) {
                return result
            }
        } catch (_: Exception) {
            // المحاولة الأولى فشلت — نتابع إلى المحاولة الثانية مع التنقية
        }

        // المحاولة الثانية: قراءة النص بالكامل، تنقية &، إعادة المحاولة
        try {
            val fullText = bufferedReader.readText()
            val sanitized = fullText.replace(Regex("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)")) { "&amp;" }
            return parseFromString(sanitized, fileName, startTime)
        } catch (e2: Exception) {
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            return GeoJsonParser.ParseResult(
                error = "فشل تحليل KML: ${e2.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR,
                parseTimeMs = elapsedMs
            )
        }
    }

    /**
     * يحاول التحليل المتدفق من Reader مباشرة.
     * لا يُحمّل الملف بالكامل في الذاكرة.
     */
    private suspend fun parseFromReader(
        reader: Reader,
        fileName: String,
        startTime: Long
    ): GeoJsonParser.ParseResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(reader)
        return parseFeatures(parser, fileName, startTime)
    }

    /**
     * يحلل النص من StringReader (بعد قراءة الملف بالكامل).
     * يُستخدم فقط كخطة احتياطية عند فشل التحليل المتدفق.
     */
    private suspend fun parseFromString(
        text: String,
        fileName: String,
        startTime: Long
    ): GeoJsonParser.ParseResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(text))
        return parseFeatures(parser, fileName, startTime)
    }

    /**
     * يقرأ المعالم من XmlPullParser بشكل متدفق.
     * يستدعي yield() كل 100 معلم للحفاظ على استجابة الواجهة.
     */
    private suspend fun parseFeatures(
        parser: XmlPullParser,
        fileName: String,
        startTime: Long
    ): GeoJsonParser.ParseResult {
        val features = mutableListOf<FeatureRow>()
        val styles = mutableMapOf<String, KmlStyle>()
        try {
            var featureCount = 0
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                coroutineContext.ensureActive()
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "Style" -> {
                            val styleId = parser.getAttributeValue(null, "id") ?: ""
                            if (styleId.isNotBlank()) {
                                parseStyle(parser, styleId)?.let { s -> styles[s.id] = s }
                            } else {
                                skipTag(parser)
                            }
                        }
                        "Placemark" -> {
                            if (featureCount < GeoJsonParser.MAX_FEATURES) {
                                parsePlacemark(parser, features, styles)
                                featureCount++
                            } else {
                                skipTag(parser)
                            }
                            if (featureCount % 100 == 0) {
                                yield()
                                coroutineContext.ensureActive()
                            }
                        }
                        "Folder", "Document" -> { }
                        else -> { /* ignore */ }
                    }
                }
                eventType = parser.next()
            }

            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            return GeoJsonParser.ParseResult(
                features = features,
                fileName = fileName,
                crs = "EPSG:4326 (KML default)",
                extent = computeExtent(features),
                parseTimeMs = elapsedMs
            )
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            return GeoJsonParser.ParseResult(
                error = "فشل تحليل KML: ${e.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR,
                parseTimeMs = elapsedMs
            )
        }
    }

    // ── التحليل التقليدي (من سلسلة نصية) ──
    /**
     * يحلل نص KML من سلسلة نصية. يعيد المحاولة مع تنقية XML إذا فشل التحليل الأول.
     */
    fun parse(kml: String, fileName: String = ""): GeoJsonParser.ParseResult {
        val attempt = { input: String ->
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(input))
            val features = mutableListOf<FeatureRow>()
            parseRecursive(parser, features)
            GeoJsonParser.ParseResult(
                features = features,
                fileName = fileName,
                crs = "EPSG:4326 (KML default)",
                extent = computeExtent(features)
            )
        }
        try {
            return attempt(kml)
        } catch (_: Exception) {
            try {
                val sanitized = kml.replace(Regex("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)")) { "&amp;" }
                return attempt(sanitized)
            } catch (e2: Exception) {
                return GeoJsonParser.ParseResult(error = "Failed to parse KML: ${e2.message}", errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR)
            }
        }
    }

    // ── التنقل التكراري في شجرة XML ──
    private fun parseRecursive(parser: XmlPullParser, features: MutableList<FeatureRow>) {
        val styles = mutableMapOf<String, KmlStyle>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "Style" -> {
                        val styleId = parser.getAttributeValue(null, "id") ?: ""
                        if (styleId.isNotBlank()) parseStyle(parser, styleId)?.let { s -> styles[s.id] = s }
                        else skipTag(parser)
                    }
                    "Placemark" -> parsePlacemark(parser, features, styles)
                    "Folder", "Document" -> parseRecursive(parser, features)
                }
            }
            eventType = parser.next()
        }
    }

    private fun parsePlacemark(parser: XmlPullParser, features: MutableList<FeatureRow>, styles: Map<String, KmlStyle> = emptyMap()) {
        val props = mutableMapOf<String, String>()
        var name = ""
        val geoms = mutableListOf<Pair<String, String>>()
        var styleUrl: String? = null

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "Placemark")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "name" -> name = parser.nextText().trim()
                    "description" -> props["description"] = parser.nextText().trim()
                    "styleUrl" -> styleUrl = parser.nextText().trim().removePrefix("#")
                    "Point" -> {
                        geoms.add("Point" to parsePointCoord(findChildText(parser, "coordinates")))
                    }
                    "LineString" -> {
                        geoms.add("LineString" to parseCoordinates(findChildText(parser, "coordinates")))
                    }
                    "Polygon" -> parsePolygon(parser, props)?.let { geoms.add(it) }
                    "MultiGeometry" -> {
                        var mgEvent = parser.next()
                        while (!(mgEvent == XmlPullParser.END_TAG && parser.name == "MultiGeometry")) {
                            if (mgEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "Point" -> geoms.add("Point" to parsePointCoord(findChildText(parser, "coordinates")))
                                    "LineString" -> geoms.add("LineString" to parseCoordinates(findChildText(parser, "coordinates")))
                                    "Polygon" -> parsePolygon(parser, props)?.let { geoms.add(it) }
                                }
                            }
                            mgEvent = parser.next()
                        }
                    }
                    "Style" -> {
                        val inlineId = parser.getAttributeValue(null, "id") ?: name
                        parseStyle(parser, inlineId)?.let { s -> props["kml_style_${s.id}"] = s.toString() }
                    }
                    "LookAt", "Region" -> skipTag(parser)
                    "ExtendedData" -> parseExtendedData(parser, props)
                    else -> { /* ignored */ }
                }
            }
            eventType = parser.next()
        }

        if (geoms.isEmpty()) return

        // تطبيق Style إذا وجد
        if (styleUrl != null && styleUrl in styles) {
            val s = styles[styleUrl]!!
            s.polyColor?.let { props["kml_poly_color"] = it }
            s.lineColor?.let { props["kml_line_color"] = it }
            if (s.lineWidth > 0f) props["kml_line_width"] = s.lineWidth.toString()
            s.iconHref?.let { props["kml_icon_href"] = it }
            // تضمين معلومات الـ Style للاستخدام في التطبيق
            props["kml_style_id"] = styleUrl
        }

        geoms.forEach { (geomType, coordsStr) ->
            val id = name.ifEmpty { props["id"] ?: "F-${features.size + 1}" }
            features.add(FeatureRow(id = id, properties = props, geometryType = geomType, geometryCoordinates = coordsStr))
        }
    }

    /**
     * Parse a KML <Style> element.
     * <Style id="foo">
     *   <PolyStyle><color>ff0000ff</color><fill>1</fill></PolyStyle>
     *   <LineStyle><color>ff0000ff</color><width>2</width></LineStyle>
     *   <IconStyle><Icon><href>http://...</href></Icon></IconStyle>
     * </Style>
     */
    private fun parseStyle(parser: XmlPullParser, styleId: String): KmlStyle? {
        val style = KmlStyle(id = styleId)
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "Style")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "PolyStyle" -> {
                        var psEvent = parser.next()
                        while (!(psEvent == XmlPullParser.END_TAG && parser.name == "PolyStyle")) {
                            if (psEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "color" -> { val c = parser.nextText().trim(); if (c.length == 8) style.polyColor = c }
                                    "fill" -> style.polyFill = parser.nextText().trim().toIntOrNull() != 0
                                    "outline" -> { }
                                }
                            }
                            psEvent = parser.next()
                        }
                    }
                    "LineStyle" -> {
                        var lsEvent = parser.next()
                        while (!(lsEvent == XmlPullParser.END_TAG && parser.name == "LineStyle")) {
                            if (lsEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "color" -> { val c = parser.nextText().trim(); if (c.length == 8) style.lineColor = c }
                                    "width" -> style.lineWidth = parser.nextText().trim().toFloatOrNull() ?: 1f
                                }
                            }
                            lsEvent = parser.next()
                        }
                    }
                    "IconStyle" -> {
                        var isEvent = parser.next()
                        while (!(isEvent == XmlPullParser.END_TAG && parser.name == "IconStyle")) {
                            if (isEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "color" -> { val c = parser.nextText().trim(); if (c.length == 8) style.iconColor = c }
                                    "scale" -> { }
                                    "Icon" -> {
                                        var icEvent = parser.next()
                                        while (!(icEvent == XmlPullParser.END_TAG && parser.name == "Icon")) {
                                            if (icEvent == XmlPullParser.START_TAG && parser.name == "href") {
                                                style.iconHref = parser.nextText().trim()
                                            }
                                            icEvent = parser.next()
                                        }
                                    }
                                }
                            }
                            isEvent = parser.next()
                        }
                    }
                    "LabelStyle" -> {
                        var lbEvent = parser.next()
                        while (!(lbEvent == XmlPullParser.END_TAG && parser.name == "LabelStyle")) {
                            if (lbEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "color" -> { val c = parser.nextText().trim(); if (c.length == 8) style.labelColor = c }
                                    "scale" -> style.labelScale = parser.nextText().trim().toFloatOrNull() ?: 1f
                                }
                            }
                            lbEvent = parser.next()
                        }
                    }
                    else -> skipTag(parser)
                }
            }
            eventType = parser.next()
        }
        return style
    }

    private fun parsePolygon(parser: XmlPullParser, props: MutableMap<String, String>): Pair<String, String>? {
        val rings = mutableListOf<String>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "Polygon")) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "outerBoundaryIs") {
                findChildText(parser, "coordinates")?.let { rings.add(parseCoordinates(it)) }
            }
            if (eventType == XmlPullParser.START_TAG && parser.name == "innerBoundaryIs") {
                findChildText(parser, "coordinates")?.let { rings.add(parseCoordinates(it)) }
            }
            eventType = parser.next()
        }
        if (rings.isEmpty()) return null
        return "Polygon" to "[${rings.joinToString(",")}]"
    }

    private fun parseCoordinates(text: String?): String {
        if (text.isNullOrBlank()) return "[]"
        val pts = text.trim().split(Regex("\\s+")).mapNotNull { token ->
            val parts = token.split(",")
            if (parts.size >= 2) "[${parts[0].trim()},${parts[1].trim()}]" else null
        }
        return "[${pts.joinToString(",")}]"
    }

    /** تحليل نقطة واحدة بصيغة [lon,lat] (بدون wrapping إضافي) */
    private fun parsePointCoord(text: String?): String {
        if (text.isNullOrBlank()) return "[]"
        val token = text.trim().split(Regex("\\s+")).firstOrNull() ?: return "[]"
        val parts = token.split(",")
        return if (parts.size >= 2) "[${parts[0].trim()},${parts[1].trim()}]" else "[]"
    }

    private fun findChildText(parser: XmlPullParser, tag: String): String? {
        var eventType = parser.next()
        var depth = 1
        while (depth > 0 && eventType != XmlPullParser.END_DOCUMENT) {
            when {
                eventType == XmlPullParser.START_TAG && parser.name == tag -> return parser.nextText()
                eventType == XmlPullParser.START_TAG -> depth++
                eventType == XmlPullParser.END_TAG -> depth--
            }
            eventType = parser.next()
        }
        return null
    }

    private fun parseExtendedData(parser: XmlPullParser, props: MutableMap<String, String>) {
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "ExtendedData")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "Data" -> {
                        val key = parser.getAttributeValue(null, "name") ?: ""
                        val value = findChildText(parser, "value") ?: ""
                        if (key.isNotBlank()) props[key] = value
                    }
                    "SchemaData" -> {
                        var sdEvent = parser.next()
                        while (!(sdEvent == XmlPullParser.END_TAG && parser.name == "SchemaData")) {
                            if (sdEvent == XmlPullParser.START_TAG && parser.name == "SimpleData") {
                                val key = parser.getAttributeValue(null, "name") ?: ""
                                val value = parser.nextText().trim()
                                if (key.isNotBlank()) props[key] = value
                            }
                            sdEvent = parser.next()
                        }
                    }
                    else -> skipTag(parser)
                }
            }
            eventType = parser.next()
        }
    }

    private fun computeExtent(features: List<FeatureRow>): String {
        if (features.isEmpty()) return "—"
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
        for (f in features) {
            val coords = GeoJsonParser.extractCoordinates(f.geometryCoordinates)
            for ((lon, lat) in coords) {
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon
                if (lon > maxLon) maxLon = lon
            }
        }
        return if (minLat == Double.MAX_VALUE) "—"
        else java.lang.String.format(java.util.Locale.US, "%.4f", minLat) + "\u00B0 \u2014 " +
             java.lang.String.format(java.util.Locale.US, "%.4f", maxLat) + "\u00B0 | " +
             java.lang.String.format(java.util.Locale.US, "%.4f", minLon) + "\u00B0 \u2014 " +
             java.lang.String.format(java.util.Locale.US, "%.4f", maxLon) + "\u00B0"
    }

    /** تخطي علامة XML بالكامل (محتواها) للوصول إلى علامة الإغلاق */
    private fun skipTag(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }
}
