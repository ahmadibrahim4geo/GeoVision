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
// KML (Keyhole Markup Language) File Parser
// محلل ملفات KML (لغة ترميز الثقب الرئيسي)
// =============================================================================
//
// DESCRIPTION / الوصف:
// KML is an XML-based format developed by Google and used in Google Earth and Maps.
// It allows representation of geographic data including placemarks, paths, and polygons.
// KML هو تنسيق قائم على XML طورته Google ويُستخدم في Google Earth و Maps.
// يسمح بتمثيل البيانات الجغرافية بما فيها الأماكن والمسارات والمضلعات.
//
// SUPPORTED ELEMENTS / العناصر المدعومة:
//   - Placemarks (Points, LineStrings, Polygons)
//     علامات المكان (نقاط، خطوط، مضلعات)
//   - Styling (Colors, line width, fill properties)
//     الأنماط (الألوان، عرض الخط، خصائص التعبئة)
//   - StyleMap references (styleUrl)
//     مراجع خريطة الأنماط (styleUrl)
//   - Coordinates in various formats
//     الإحداثيات بصيغ متعددة
//
// PERFORMANCE OPTIMIZATIONS (v2) / تحسينات الأداء (v2):
//   - Streaming-first approach using XmlPullParser (no full file loading)
//     نهج متدفق أولاً باستخدام XmlPullParser (بدون تحميل الملف بالكامل)
//   - If parsing fails, attempts character fixing (& symbols) then retries
//     إذا فشل التحليل، يحاول إصلاح الأحرف ثم يحاول مرة أخرى
//   - Full support for KML styles and styling
//     دعم كامل لأنماط KML والتنسيق
//   - Supports cancellation via ensureActive()
//     دعم إلغاء العملية عبر ensureActive()
//
// KNOWN LIMITATIONS / القيود المعروفة:
//   - Network links (<NetworkLink> elements) are NOT fetched
//     روابط الشبكة لا يتم جلبها من الإنترنت
//   - Complex styling may be simplified
//     قد يتم تبسيط الأنماط المعقدة
//   - Large KML files (>100MB) require powerful devices
//     ملفات KML الكبيرة تتطلب أجهزة قوية
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

    private fun tagName(parser: XmlPullParser): String =
        parser.name?.substringAfterLast(':')?.substringAfterLast('}') ?: ""

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
                    when (tagName(parser)) {
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
                when (tagName(parser)) {
                    "Style" -> {
                        val styleId = parser.getAttributeValue(null, "id") ?: ""
                        if (styleId.isNotBlank()) parseStyle(parser, styleId)?.let { s -> styles[s.id] = s }
                        else skipTag(parser)
                    }
                    "Placemark" -> parsePlacemark(parser, features, styles)
                    "Folder", "Document" -> { }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parsePlacemark(parser: XmlPullParser, features: MutableList<FeatureRow>, styles: Map<String, KmlStyle> = emptyMap()) {
        val placemarkId = parser.getAttributeValue(null, "id") ?: ""
        val props = mutableMapOf<String, String>()
        var name = ""
        val geoms = mutableListOf<Pair<String, String>>()
        var styleUrl: String? = null
        val seenGeomSignatures = mutableSetOf<String>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && tagName(parser) == "Placemark")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName(parser)) {
                    "name" -> name = parser.nextText().trim()
                    "description" -> {
                        val rawDesc = parser.nextText().trim()
                        if (rawDesc.isNotBlank()) {
                            val cleanedDescription = stripHtmlTags(rawDesc)
                            if (cleanedDescription.isNotBlank()) {
                                props["description"] = cleanedDescription
                            }
                            parseDescriptionTable(rawDesc, props)
                        }
                    }
                    "styleUrl" -> styleUrl = parser.nextText().trim().removePrefix("#")
                    "Point" -> {
                        val coords = parsePointCoord(findChildText(parser, "coordinates"))
                        if (coords !in seenGeomSignatures) {
                            seenGeomSignatures.add(coords)
                            geoms.add("Point" to coords)
                        }
                    }
                    "LineString" -> {
                        val coords = parseCoordinates(findChildText(parser, "coordinates"))
                        if (coords !in seenGeomSignatures) {
                            seenGeomSignatures.add(coords)
                            geoms.add("LineString" to coords)
                        }
                    }
                    "Polygon" -> {
                        val poly = parsePolygon(parser, props)
                        if (poly != null && poly.second !in seenGeomSignatures) {
                            seenGeomSignatures.add(poly.second)
                            geoms.add(poly)
                        }
                    }
                    "MultiGeometry" -> {
                        var mgEvent = parser.next()
                        while (!(mgEvent == XmlPullParser.END_TAG && tagName(parser) == "MultiGeometry")) {
                            if (mgEvent == XmlPullParser.START_TAG) {
                                when (tagName(parser)) {
                                    "Point" -> {
                                        val coords = parsePointCoord(findChildText(parser, "coordinates"))
                                        if (coords !in seenGeomSignatures) {
                                            seenGeomSignatures.add(coords)
                                            geoms.add("Point" to coords)
                                        }
                                    }
                                    "LineString" -> {
                                        val coords = parseCoordinates(findChildText(parser, "coordinates"))
                                        if (coords !in seenGeomSignatures) {
                                            seenGeomSignatures.add(coords)
                                            geoms.add("LineString" to coords)
                                        }
                                    }
                                    "Polygon" -> {
                                        val poly = parsePolygon(parser, props)
                                        if (poly != null && poly.second !in seenGeomSignatures) {
                                            seenGeomSignatures.add(poly.second)
                                            geoms.add(poly)
                                        }
                                    }
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
            props["kml_style_id"] = styleUrl
        }

        // تحديد معرف فريد للـ Placemark
        // استخدام placemarkId إذا كان متوفراً، وإلا استخدام الاسم + مؤشر العدد
        val uniqueId = when {
            placemarkId.isNotBlank() -> placemarkId
            name.isNotBlank() -> name + "_${features.size}"
            else -> "F-${features.size + 1}"
        }

        geoms.forEachIndexed { geomIdx, (geomType, coordsStr) ->
            // إنشاء معرف فريد للميزة بناءً على الإحداثيات و الهندسة
            // هذا يساعد في منع التكرار
            val fid = when {
                geoms.size > 1 -> "$uniqueId [$geomIdx]"
                else -> uniqueId
            }
            
            val featureProps = props.toMutableMap()
            featureProps["_name"] = name
            featureProps["_placemark_id"] = uniqueId
            featureProps["_geometry_index"] = geomIdx.toString()
            featureProps.remove("description_parsed")
            
            features.add(FeatureRow(
                id = fid,
                properties = featureProps,
                geometryType = geomType,
                geometryCoordinates = coordsStr
            ))
        }
    }

    /**
     * Parse HTML table from KML <description> into individual properties.
     * The description typically contains: <table><tr><td>key</td><td>value</td></tr>...</table>
     */
    @JvmStatic
    private fun parseDescriptionTable(html: String, props: MutableMap<String, String>) {
        var parsed = false
        try {
            val normalizedHtml = decodeHtmlEntities(html)
            val rowRegex = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
            val cellRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", RegexOption.DOT_MATCHES_ALL)
            val rows = rowRegex.findAll(normalizedHtml)
            var key: String? = null
            for (row in rows) {
                val cells = cellRegex.findAll(row.groupValues[1]).map { it.groupValues[1] }.toList()
                if (cells.size >= 2) {
                    val k = stripHtmlTags(cells[0]).trim()
                    val v = stripHtmlTags(cells[1]).trim()
                    if (k.isNotBlank()) {
                        if (key == null) {
                            key = k
                        } else {
                            props[key!!] = decodeHtmlEntities(k) // header cell
                            key = null
                        }
                        props[k] = decodeHtmlEntities(v)
                        parsed = true
                    }
                } else if (cells.size == 1 && key != null) {
                    props[key!!] = decodeHtmlEntities(cells[0])
                }
            }
        } catch (_: Exception) { }
        if (parsed) props["description_parsed"] = "true"
    }

    /**
     * تنظيف نص HTML/XSLT وإزالة جميع الوسوم والأنماط.
     * يتعامل مع:
     * - وسوم HTML العادية (<div>, <span>, etc)
     * - XSLT و namespace declarations
     * - CSS styles
     * - CDATA sections
     */
    @JvmStatic
    private fun stripHtmlTags(text: String): String {
        var cleaned = decodeHtmlEntities(text)
        
        // إزالة XSLT namespace attributes
        cleaned = cleaned.replace(Regex("xmlns[^=]*=\"[^\"]*\""), " ")
        
        // إزالة CSS styles
        cleaned = cleaned.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
        
        // إزالة script tags
        cleaned = cleaned.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
        
        // إزالة جميع وسوم HTML
        cleaned = cleaned.replace(Regex("<[^>]*>"), " ")
        
        // فك ترميز HTML entities مرة أخرى لأي نصوص ظهرت بعد تنظيف الوسوم
        cleaned = decodeHtmlEntities(cleaned)
        
        // توحيد المسافات البيضاء
        cleaned = cleaned.replace(Regex("\\s+"), " ")
        
        return cleaned.trim()
    }

    /**
     * فك ترميز HTML entities و XML entities
     */
    @JvmStatic
    private fun decodeHtmlEntities(text: String): String {
        var result = text
        
        // Named entities
        result = result.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#60;", "<")
            .replace("&#62;", ">")
            .replace("&#160;", " ")
            .replace("&nbsp;", " ")
        
        // Numeric entities (&#123;)
        result = result.replace(Regex("&#(\\d+);")) { match ->
            try {
                match.groupValues[1].toInt().toChar().toString()
            } catch (_: Exception) {
                match.value
            }
        }
        
        // Hex entities (&#x1a;)
        result = result.replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            try {
                match.groupValues[1].toInt(16).toChar().toString()
            } catch (_: Exception) {
                match.value
            }
        }
        
        return result
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
        while (!(eventType == XmlPullParser.END_TAG && tagName(parser) == "Style")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName(parser)) {
                    "PolyStyle" -> {
                        var psEvent = parser.next()
                        while (!(psEvent == XmlPullParser.END_TAG && tagName(parser) == "PolyStyle")) {
                            if (psEvent == XmlPullParser.START_TAG) {
                                when (tagName(parser)) {
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
                        while (!(lsEvent == XmlPullParser.END_TAG && tagName(parser) == "LineStyle")) {
                            if (lsEvent == XmlPullParser.START_TAG) {
                                when (tagName(parser)) {
                                    "color" -> { val c = parser.nextText().trim(); if (c.length == 8) style.lineColor = c }
                                    "width" -> style.lineWidth = parser.nextText().trim().toFloatOrNull() ?: 1f
                                }
                            }
                            lsEvent = parser.next()
                        }
                    }
                    "IconStyle" -> {
                        var isEvent = parser.next()
                        while (!(isEvent == XmlPullParser.END_TAG && tagName(parser) == "IconStyle")) {
                            if (isEvent == XmlPullParser.START_TAG) {
                                when (tagName(parser)) {
                                    "color" -> { val c = parser.nextText().trim(); if (c.length == 8) style.iconColor = c }
                                    "scale" -> { }
                                    "Icon" -> {
                                        var icEvent = parser.next()
                                        while (!(icEvent == XmlPullParser.END_TAG && tagName(parser) == "Icon")) {
                                            if (icEvent == XmlPullParser.START_TAG && tagName(parser) == "href") {
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
                        while (!(lbEvent == XmlPullParser.END_TAG && tagName(parser) == "LabelStyle")) {
                            if (lbEvent == XmlPullParser.START_TAG) {
                                when (tagName(parser)) {
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
        while (!(eventType == XmlPullParser.END_TAG && tagName(parser) == "Polygon")) {
            if (eventType == XmlPullParser.START_TAG && tagName(parser) == "outerBoundaryIs") {
                findChildText(parser, "coordinates")?.let { rings.add(parseCoordinates(it)) }
            }
            if (eventType == XmlPullParser.START_TAG && tagName(parser) == "innerBoundaryIs") {
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
                eventType == XmlPullParser.START_TAG && tagName(parser) == tag -> return parser.nextText()
                eventType == XmlPullParser.START_TAG -> depth++
                eventType == XmlPullParser.END_TAG -> depth--
            }
            eventType = parser.next()
        }
        return null
    }

    private fun parseExtendedData(parser: XmlPullParser, props: MutableMap<String, String>) {
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && tagName(parser) == "ExtendedData")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName(parser)) {
                    "Data" -> {
                        val key = parser.getAttributeValue(null, "name") ?: ""
                        val value = findChildText(parser, "value") ?: ""
                        if (key.isNotBlank()) props[key] = value
                    }
                    "SchemaData" -> {
                        var sdEvent = parser.next()
                        while (!(sdEvent == XmlPullParser.END_TAG && tagName(parser) == "SchemaData")) {
                            if (sdEvent == XmlPullParser.START_TAG && tagName(parser) == "SimpleData") {
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
