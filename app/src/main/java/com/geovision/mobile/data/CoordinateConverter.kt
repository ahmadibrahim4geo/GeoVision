package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow

/**
 * Coordinate Converter for transforming between CRS systems.
 * Provides a unified API for all CRS conversions used in the app.
 *
 * Supported conversions:
 * - EPSG:4326 (WGS84) ← → EPSG:3857 (Web Mercator)
 * - EPSG:4326 ← → UTM zones (EPSG:326xx / 327xx)
 * - Any EPSG via WKT-based detection
 */
object CoordinateConverter {
    /**
     * نتيجة التحويل تحتوي على الإحداثيات المحولة ومعلومات عن العملية.
     */
    data class ConversionResult(
        val coordinatesJson: String,
        val targetCrs: String,
        val success: Boolean = true,
        val message: String = ""
    )

    /**
     * تحويل جميع المعالم في طبقة إلى WGS84.
     * @param features قائمة المعالم
     * @param sourceCrs نظام الإحداثيات المصدر (مثل "EPSG:3857")
     * @return قائمة المعالم بعد التحويل
     */
    fun convertFeaturesToWgs84(features: List<FeatureRow>, sourceCrs: String): List<FeatureRow> {
        if (sourceCrs == "EPSG:4326" || sourceCrs.startsWith("EPSG:4326")) return features

        val crsInfo = parseCrs(sourceCrs)
        if (crsInfo.isWgs84 && crsInfo.isGeographic) return features

        AppLogger.d(AppLogger.Tags.PARSER, "Converting ${features.size} features from $sourceCrs to WGS84")
        return features.map { feature ->
            val newCoords = convertCoords(feature.geometryCoordinates, crsInfo)
            feature.copy(geometryCoordinates = newCoords, crs = "EPSG:4326")
        }
    }

    /**
     * تحويل سلسلة إحداثيات JSON من CRS معين إلى WGS84.
     */
    fun convertCoords(coordsJson: String?, crsInfo: CrsTransform.CrsInfo): String? {
        if (coordsJson.isNullOrBlank() || (crsInfo.isWgs84 && crsInfo.isGeographic)) return coordsJson
        return try {
            val arr = org.json.JSONArray(coordsJson)
            val result = transformRecursive(arr, crsInfo)
            result.toString()
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Coordinate conversion failed: ${e.message}")
            coordsJson
        }
    }

    private fun transformRecursive(arr: org.json.JSONArray, crsInfo: CrsTransform.CrsInfo): org.json.JSONArray {
        if (arr.length() >= 2) {
            try {
                val v0 = arr.optDouble(0)
                val v1 = arr.optDouble(1)
                if (!v0.isNaN() && !v1.isNaN() && arr.length() <= 3) {
                    val (lon, lat) = CrsTransform.toWgs84(v0, v1, crsInfo)
                    return org.json.JSONArray().apply { put(lon); put(lat) }
                }
            } catch (_: Exception) {}
        }
        val result = org.json.JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            if (item is org.json.JSONArray) {
                result.put(transformRecursive(item, crsInfo))
            } else {
                result.put(item)
            }
        }
        return result
    }

    /**
     * تحليل سلسلة CRS (EPSG code أو WKT) إلى CrsInfo.
     */
    fun parseCrs(crs: String): CrsTransform.CrsInfo {
        val upper = crs.uppercase()

        // التعامل مع EPSG مباشرة
        val epsgMatch = Regex("""EPSG[:\s]*(\d+)""", RegexOption.IGNORE_CASE).find(crs)
        if (epsgMatch != null) {
            val code = epsgMatch.groupValues[1].toIntOrNull() ?: return CrsTransform.CrsInfo(name = crs)
            return CrsTransform.CrsInfo(
                isWgs84 = code == 4326,
                name = crs,
                epsg = code,
                isUtm = code in 32601..32660 || code in 32701..32760,
                utmZone = if (code in 32601..32660) code - 32600 else if (code in 32701..32760) code - 32700 else 0,
                utmNorth = code in 32601..32660
            )
        }

        // التعامل مع WKT
        if (upper.contains("GEOGCS") || upper.contains("PROJCS")) {
            return CrsTransform.parseWkt(crs)
        }

        return CrsTransform.CrsInfo(name = crs)
    }

    /**
     * صياغة النطاق الجغرافي (Bounding box) من قائمة المعالم.
     */
    fun computeExtent(features: List<FeatureRow>): String {
        if (features.isEmpty()) return "—"
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
        for (f in features) {
            val coords = GeoJsonParser.extractCoordinates(f.geometryCoordinates)
            for ((lon, lat) in coords) {
                if (lat < minLat) minLat = lat; if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon; if (lon > maxLon) maxLon = lon
            }
        }
        return if (minLat == Double.MAX_VALUE) "—"
        else java.lang.String.format(java.util.Locale.US, "%.4f", minLat) + "° — " +
             java.lang.String.format(java.util.Locale.US, "%.4f", maxLat) + "° | " +
             java.lang.String.format(java.util.Locale.US, "%.4f", minLon) + "° — " +
             java.lang.String.format(java.util.Locale.US, "%.4f", maxLon) + "°"
    }
}
