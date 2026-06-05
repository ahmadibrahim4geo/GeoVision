package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import java.util.concurrent.ConcurrentHashMap

/**
 * CRS transformation helper.
 * Uses Proj4J for EPSG reprojection, with lightweight fallbacks for common systems.
 */
object CrsTransform {
    private val crsFactory = CRSFactory()
    private val transformFactory = CoordinateTransformFactory()
    private val crsCache = ConcurrentHashMap<Int, CoordinateReferenceSystem>()
    private val transformCache = ConcurrentHashMap<Int, CoordinateTransform>()

    /**
     * Information about a CRS (Coordinate Reference System).
     */
    data class CrsInfo(
        val isWgs84: Boolean = false,
        val name: String = "WGS84",
        val epsg: Int = 4326,
        val isUtm: Boolean = false,
        val utmZone: Int = 0,
        val utmNorth: Boolean = true,
        val isGeographic: Boolean = false,
        val toWgs84Params: DoubleArray? = null
    )

    /**
     * Convert a single coordinate from source CRS to WGS84 (lon, lat).
     */
    fun toWgs84(x: Double, y: Double, source: CrsInfo): Pair<Double, Double> {
        if (source.epsg == 4326 || (source.isWgs84 && source.isGeographic)) return Pair(x, y)

        transformWithProj4j(x, y, source)?.let { return it }

        return when (source.epsg) {
            3857 -> webMercatorToWgs84(x, y)
            in 32601..32660 -> utmToWgs84(x, y, source.utmZone, north = true)
            in 32701..32760 -> utmToWgs84(x, y, source.utmZone, north = false)
            else -> {
                if (source.isGeographic) Pair(x, y)
                else {
                    AppLogger.w(AppLogger.Tags.PARSER, "Unsupported CRS ${source.epsg}, assuming WGS84")
                    Pair(x, y)
                }
            }
        }
    }

    private fun transformWithProj4j(x: Double, y: Double, source: CrsInfo): Pair<Double, Double>? {
        val transform = transformCache[source.epsg] ?: run {
            try {
                val src = crsFor(source) ?: return null
                val dst = crsFactory.createFromName("EPSG:4326")
                transformFactory.createTransform(src, dst).also { transformCache[source.epsg] = it }
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tags.PARSER, "Proj4J CRS ${source.epsg} unavailable: ${e.message}")
                return null
            }
        }

        return try {
            val srcCoord = ProjCoordinate(x, y)
            val dstCoord = ProjCoordinate()
            transform.transform(srcCoord, dstCoord)
            if (dstCoord.x.isFinite() && dstCoord.y.isFinite()) {
                Pair(dstCoord.x.coerceIn(-180.0, 180.0), dstCoord.y.coerceIn(-90.0, 90.0))
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Proj4J transform failed for EPSG:${source.epsg}: ${e.message}")
            null
        }
    }

    private fun crsFor(source: CrsInfo): CoordinateReferenceSystem? {
        crsCache[source.epsg]?.let { return it }
        return try {
            val crs = when {
                source.epsg > 0 -> crsFactory.createFromName("EPSG:${source.epsg}")
                source.isUtm && source.utmZone in 1..60 -> {
                    val southFlag = if (source.utmNorth) "" else " +south"
                    crsFactory.createFromParameters(
                        "UTM:${source.utmZone}${if (source.utmNorth) "N" else "S"}",
                        "+proj=utm +zone=${source.utmZone}$southFlag +datum=WGS84 +units=m +no_defs"
                    )
                }
                else -> null
            }
            if (crs != null) crsCache[source.epsg] = crs
            crs
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Failed to create Proj4J CRS for ${source.name}: ${e.message}")
            null
        }
    }

    /**
     * Convert WGS84 (lon, lat) to Web Mercator (x, y).
     */
    fun wgs84ToWebMercator(lon: Double, lat: Double): Pair<Double, Double> {
        val x = lon * 20037508.34 / 180.0
        val y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(lat) / 2)) / Math.PI * 20037508.34
        return Pair(x, y.coerceIn(-20037508.34, 20037508.34))
    }

    /**
     * Convert Web Mercator (x, y) to WGS84 (lon, lat).
     */
    fun webMercatorToWgs84(x: Double, y: Double): Pair<Double, Double> {
        val lon = x / 20037508.34 * 180.0
        val lat = Math.toDegrees(2 * Math.atan(Math.exp(y / 20037508.34 * Math.PI)) - Math.PI / 2)
        return Pair(lon, lat.coerceIn(-90.0, 90.0))
    }

    /**
     * Approximate UTM to WGS84 conversion using formulas.
     * Kept as a fallback when Proj4J cannot resolve a CRS on the device.
     */
    fun utmToWgs84(x: Double, y: Double, zone: Int, north: Boolean): Pair<Double, Double> {
        val eqr = 6378137.0          // WGS84 equatorial radius
        val flattening = 1.0 / 298.257223563
        val e2 = flattening * (2 - flattening)
        val e1 = (1 - Math.sqrt(1 - e2)) / (1 + Math.sqrt(1 - e2))
        val k0 = 0.9996
        val falseEasting = 500000.0
        val falseNorthing = if (north) 0.0 else 10000000.0
        val lonOrigin = (zone - 1) * 6 - 180 + 3 // central meridian

        val dx = x - falseEasting
        val dy = y - falseNorthing

        val m = dy / (k0 * eqr * (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256))
        val e1Sq = e1 * e1
        val e1Cu = e1Sq * e1
        val e1Qu = e1Cu * e1

        val phi1 = (m + (3 * e1 / 2 - 27 * e1Cu / 32) * Math.sin(2 * m)
            + (21 * e1Sq / 16 - 55 * e1Qu / 32) * Math.sin(4 * m)
            + (151 * e1Cu / 96) * Math.sin(6 * m) + (1097 * e1Qu / 512) * Math.sin(8 * m))

        val sin1 = Math.sin(phi1)
        val cos1 = Math.cos(phi1)
        val tan1 = sin1 / cos1
        val n = eqr / Math.sqrt(1 - e2 * sin1 * sin1)
        val r = eqr * (1 - e2) / Math.pow(1 - e2 * sin1 * sin1, 1.5)
        val t = tan1 * tan1
        val c = e2 * cos1 * cos1 / (1 - e2)

        val d = dx / (n * k0)
        val lat = phi1 - (n * tan1 / r) * (
            d * d / 2 - (5 + 3 * t + 10 * c - 4 * c * c - 9 * e2) * d * d * d * d / 24
            + (61 + 90 * t + 298 * c + 45 * t * t - 252 * e2 - 3 * c * c) * d * d * d * d * d * d / 720
        )
        val lon = (d - (1 + 2 * t + c) * d * d * d / 6
            + (5 - 2 * c + 28 * t - 3 * c * c + 8 * e2 + 24 * t * t) * d * d * d * d * d / 120) / Math.cos(phi1)

        return Pair(
            (lon * 180 / Math.PI + lonOrigin).coerceIn(-180.0, 180.0),
            Math.toDegrees(lat).coerceIn(-90.0, 90.0)
        )
    }

    /**
     * Parse basic WKT CRS string to extract EPSG code or geographic vs projected.
     */
    fun parseWkt(wkt: String): CrsInfo {
        val upper = wkt.uppercase()
        val isGeographic = upper.contains("GEOGCS")
        val isProjected = upper.contains("PROJCS")
        val hasWgs84 = upper.contains("WGS 84") || upper.contains("WGS_1984")

        // Try to extract AUTHORITY["EPSG","XXXX"]
        val epsgMatch = Regex("""AUTHORITY\s*\[\s*"[^"]*"\s*,\s*"(\d+)"\s*\]""", RegexOption.IGNORE_CASE).find(upper)
        val epsg = epsgMatch?.groupValues?.get(1)?.toIntOrNull()

        if (epsg != null) {
            return CrsInfo(
                name = "EPSG:$epsg",
                epsg = epsg,
                isWgs84 = epsg == 4326,
                isGeographic = isGeographic,
                isUtm = epsg in 32601..32660 || epsg in 32701..32760,
                utmZone = if (epsg in 32601..32660) epsg - 32600 else if (epsg in 32701..32760) epsg - 32700 else 0,
                utmNorth = epsg in 32601..32660
            )
        }

        // Try to extract UTM zone from names such as "UTM ZONE 38N" or "WGS_1984_UTM_Zone_36N".
        val utmMatch = Regex("""UTM[\s_]+ZONE[\s_]+(\d{1,2})\s*([NS])""", RegexOption.IGNORE_CASE).find(upper)
        if (utmMatch != null) {
            val zone = utmMatch.groupValues[1].toIntOrNull() ?: 0
            val north = utmMatch.groupValues[2] != "S"
            return CrsInfo(
                name = "UTM",
                epsg = if (north) 32600 + zone else 32700 + zone,
                isWgs84 = false,
                isGeographic = false,
                isUtm = true,
                utmZone = zone,
                utmNorth = north
            )
        }

        // If projected with Mercator in name and WGS84, assume EPSG:3857
        val hasWebMercator = upper.contains("PSEUDO-MERCATOR") ||
            upper.contains("MERCATOR_1SP") ||
            (upper.contains("MERCATOR") && !upper.contains("TRANSVERSE_MERCATOR"))
        if (isProjected && hasWebMercator && hasWgs84) {
            return CrsInfo(
                name = "EPSG:3857",
                epsg = 3857,
                isWgs84 = false,
                isGeographic = false,
                isUtm = false
            )
        }

        // If geographic with WGS84 in name, assume EPSG:4326
        if (isGeographic && hasWgs84) {
            return CrsInfo(
                name = "EPSG:4326",
                epsg = 4326,
                isWgs84 = true,
                isGeographic = true,
                isUtm = false
            )
        }

        return CrsInfo(name = "WKT", epsg = 0, isGeographic = isGeographic)
    }
}
