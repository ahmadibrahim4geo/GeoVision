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
// محلل ملفات GPX (GPS Exchange Format)
// =============================================================================
// GPX هو تنسيق XML لتبادل بيانات GPS. يدعم هذا المحلل:
//   - wpt (Waypoint) → Point
//   - trk (Track) → trkseg → LineString
//   - rte (Route) → rtept → LineString
// =============================================================================
// تحسينات الأداء (v2):
//   1. قراءة متدفقة (Streaming-first) — Reader يُمرّر إلى XmlPullParser مباشرة.
//   2. إضافة ensureActive() لدعم إلغاء العملية.
//   3. إضافة yield() كل 100 معلم لتحسين استجابة الواجهة.
// =============================================================================
object GpxParser {
    private const val TAG = "GpxParser"

    // ── التحليل المتدفق (Streaming-first) ──
    /**
     * يقرأ ملف GPX بشكل متدفق باستخدام XmlPullParser.
     *
     * الإستراتيجية:
     *   1. المحاولة الأولى: نمرر Reader مباشرة (قراءة متدفقة).
     *   2. إذا فشلت: نقرأ النص بالكامل، نصلح &، نعيد المحاولة.
     *
     * @param reader مصدر القراءة
     * @param fileName اسم الملف للعرض
     * @return ParseResult يحتوي على المعالم المستخرجة
     */
    suspend fun streamParse(reader: Reader, fileName: String = ""): GeoJsonParser.ParseResult {
        val startTime = System.nanoTime()
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader, 8192)

        // المحاولة الأولى: قراءة متدفقة
        try {
            val result = parseFromReader(bufferedReader, fileName, startTime)
            if (result.error == null || result.errorType != GeoJsonParser.ParseErrorType.PARSER_ERROR) {
                return result
            }
        } catch (_: Exception) {
            // المحاولة الأولى فشلت — نتابع إلى المحاولة الثانية
        }

        // المحاولة الثانية: قراءة كامل النص، تنقية &، إعادة المحاولة
        try {
            val fullText = bufferedReader.readText()
            val sanitized = fullText.replace(Regex("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)")) { "&amp;" }
            return parseFromString(sanitized, fileName, startTime)
        } catch (e2: Exception) {
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            return GeoJsonParser.ParseResult(
                error = "فشل تحليل GPX: ${e2.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR,
                parseTimeMs = elapsedMs
            )
        }
    }

    private suspend fun parseFromReader(reader: Reader, fileName: String, startTime: Long): GeoJsonParser.ParseResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(reader)
        return parseFeatures(parser, fileName, startTime)
    }

    private suspend fun parseFromString(text: String, fileName: String, startTime: Long): GeoJsonParser.ParseResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(text))
        return parseFeatures(parser, fileName, startTime)
    }

    private suspend fun parseFeatures(parser: XmlPullParser, fileName: String, startTime: Long): GeoJsonParser.ParseResult {
        val features = mutableListOf<FeatureRow>()
        try {
            var eventType = parser.eventType
            var featureCount = 0
            while (eventType != XmlPullParser.END_DOCUMENT) {
                coroutineContext.ensureActive()
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "wpt" -> { if (features.size < GeoJsonParser.MAX_FEATURES) { parseWpt(parser, features); featureCount++ } else { skipTag(parser) } }
                        "trk" -> { if (features.size < GeoJsonParser.MAX_FEATURES) parseTrk(parser, features) else skipTag(parser) }
                        "rte" -> { if (features.size < GeoJsonParser.MAX_FEATURES) parseRte(parser, features) else skipTag(parser) }
                    }
                }
                eventType = parser.next()
                if (featureCount > 0 && featureCount % 100 == 0) {
                    yield()
                    coroutineContext.ensureActive()
                }
            }
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            return GeoJsonParser.ParseResult(
                features = features, fileName = fileName,
                crs = "EPSG:4326 (GPX default)",
                extent = computeExtent(features), parseTimeMs = elapsedMs
            )
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            return GeoJsonParser.ParseResult(
                error = "فشل تحليل GPX: ${e.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR,
                parseTimeMs = elapsedMs
            )
        }
    }

    // ── التحليل التقليدي (من سلسلة نصية) ──
    fun parse(gpx: String, fileName: String = ""): GeoJsonParser.ParseResult {
        val attempt = { input: String ->
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(input))
            val features = mutableListOf<FeatureRow>()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "wpt" -> parseWpt(parser, features)
                        "trk" -> parseTrk(parser, features)
                        "rte" -> parseRte(parser, features)
                    }
                }
                eventType = parser.next()
            }
            GeoJsonParser.ParseResult(features = features, fileName = fileName, crs = "EPSG:4326 (GPX default)", extent = computeExtent(features))
        }
        try {
            return attempt(gpx)
        } catch (_: Exception) {
            try {
                val sanitized = gpx.replace(Regex("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)")) { "&amp;" }
                return attempt(sanitized)
            } catch (e2: Exception) {
                return GeoJsonParser.ParseResult(error = "Failed to parse GPX: ${e2.message}", errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR)
            }
        }
    }

    // ── Waypoint ──
    private fun parseWpt(parser: XmlPullParser, features: MutableList<FeatureRow>) {
        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
        val props = mutableMapOf<String, String>()
        var name = ""

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "wpt")) {
            if (eventType == XmlPullParser.START_TAG) {
                val text = parser.nextText().trim()
                when (parser.name) {
                    "name" -> name = text
                    "cmt" -> props["comment"] = text
                    "desc" -> props["description"] = text
                    "ele" -> props["elevation"] = text
                    "sym" -> props["symbol"] = text
                    "type" -> props["type"] = text
                    else -> if (parser.name !in setOf("wpt")) props[parser.name] = text
                }
            }
            eventType = parser.next()
        }

        val id = name.ifEmpty { "WPT-${features.size + 1}" }
        if (lon.isFinite() && lat.isFinite()) {
            val coords = "[$lon,$lat]"
            features.add(FeatureRow(id = id, properties = props, geometryType = "Point", geometryCoordinates = coords))
        }
    }

    // ── Track ──
    private fun parseTrk(parser: XmlPullParser, features: MutableList<FeatureRow>) {
        val props = mutableMapOf<String, String>()
        var trackName = ""

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "trk")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "name" -> trackName = parser.nextText().trim()
                    "cmt" -> props["comment"] = parser.nextText().trim()
                    "desc" -> props["description"] = parser.nextText().trim()
                    "trkseg" -> parseTrkSeg(parser, features, trackName, props)
                    "extensions" -> skipToEnd(parser)
                    else -> { /* skip */ }
                }
            }
            eventType = parser.next()
        }
    }

    // ── Track Segment ──
    private fun parseTrkSeg(parser: XmlPullParser, features: MutableList<FeatureRow>, trackName: String, sharedProps: Map<String, String>) {
        val pts = mutableListOf<String>()
        val segProps = mutableMapOf<String, String>()
        segProps.putAll(sharedProps)

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "trkseg")) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "trkpt") {
                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                if (lon.isFinite() && lat.isFinite()) pts.add("[$lon,$lat]")

                var inner = parser.next()
                while (!(inner == XmlPullParser.END_TAG && parser.name == "trkpt")) {
                    if (inner == XmlPullParser.START_TAG && parser.name == "ele") {
                        segProps["elevation"] = parser.nextText().trim()
                    }
                    inner = parser.next()
                }
            }
            eventType = parser.next()
        }

        if (pts.isNotEmpty() && features.size < GeoJsonParser.MAX_FEATURES) {
            val id = "${trackName.ifEmpty { "Track" }}-Seg${features.count { it.id.startsWith(trackName.ifEmpty { "Track" }) } + 1}"
            val coords = "[${pts.joinToString(",")}]"
            features.add(FeatureRow(id = id, properties = segProps, geometryType = "LineString", geometryCoordinates = coords))
        }
    }

    // ── Route ──
    private fun parseRte(parser: XmlPullParser, features: MutableList<FeatureRow>) {
        val pts = mutableListOf<String>()
        val props = mutableMapOf<String, String>()
        var routeName = ""

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "rte")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "name" -> routeName = parser.nextText().trim()
                    "cmt" -> props["comment"] = parser.nextText().trim()
                    "desc" -> props["description"] = parser.nextText().trim()
                    "rtept" -> {
                        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                        if (lon.isFinite() && lat.isFinite()) pts.add("[$lon,$lat]")
                        skipToEnd(parser)
                    }
                    else -> { /* skip */ }
                }
            }
            eventType = parser.next()
        }

        if (pts.isNotEmpty() && features.size < GeoJsonParser.MAX_FEATURES) {
            val id = routeName.ifEmpty { "Route-${features.size + 1}" }
            val coords = "[${pts.joinToString(",")}]"
            features.add(FeatureRow(id = id, properties = props, geometryType = "LineString", geometryCoordinates = coords))
        }
    }

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

    private fun skipToEnd(parser: XmlPullParser) {
        var depth = 1
        var eventType = parser.next()
        while (depth > 0 && eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) depth++
            else if (eventType == XmlPullParser.END_TAG) depth--
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
}
