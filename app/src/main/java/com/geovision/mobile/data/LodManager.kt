package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier
import org.locationtech.jts.simplify.TopologyPreservingSimplifier

/**
 * Level of Detail (LOD) Manager.
 * Controls geometry complexity based on zoom level for rendering optimization.
 */
object LodManager {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    /**
     * حساب tolerance التبسيط بناءً على مستوى الزوم.
     * Zoom عالي → تفاصيل أكثر (tolerance أقل)
     * Zoom منخفض → تفاصيل أقل (tolerance أعلى)
     */
    fun getTolerance(zoomLevel: Double): Double {
        return when {
            zoomLevel >= 18 -> 0.000001  // ~0.1m
            zoomLevel >= 16 -> 0.000005  // ~0.5m
            zoomLevel >= 14 -> 0.00001   // ~1m
            zoomLevel >= 12 -> 0.00005   // ~5m
            zoomLevel >= 10 -> 0.0001    // ~10m
            zoomLevel >= 8 -> 0.001      // ~100m
            zoomLevel >= 6 -> 0.01       // ~1km
            else -> 0.1                   // ~10km
        }
    }

    /**
     * LOD Level Indicator (0 = full detail, 4 = minimal)
     */
    fun getLodLevel(zoomLevel: Double, featureSize: Int = 0): Int {
        return when {
            zoomLevel >= 18 -> 0
            zoomLevel >= 15 -> 1
            zoomLevel >= 12 -> 2
            zoomLevel >= 8 -> 3
            else -> 4
        }
    }

    /**
     * هل يجب عرض هذا الـ feature عند مستوى الزوم الحالي؟
     * Point features تظهر دائماً، Polygon/Line تظهر حسب الحجم
     */
    fun shouldRender(lodLevel: Int, geometryType: String?, boundsArea: Double = 0.0): Boolean {
        return when (geometryType) {
            "Point", "MultiPoint" -> true  // النقاط تظهر دائماً
            "LineString", "MultiLineString" -> lodLevel <= 3  // الخطوط تظهر حتى LOD 3
            "Polygon", "MultiPolygon" -> when {
                lodLevel <= 1 -> true
                lodLevel == 2 -> boundsArea > 0.000001
                lodLevel == 3 -> boundsArea > 0.0001
                else -> boundsArea > 0.01
            }
            else -> true
        }
    }

    /**
     * تبسيط الهندسة باستخدام Douglas-Peucker.
     * @param coordsJson إحداثيات JSON الحالية
     * @param geometryType نوع الهندسة
     * @param zoomLevel مستوى الزوم الحالي
     * @return الإحداثيات المبسطة (أو الأصلية إن كان التبسيط غير ضروري)
     */
    fun simplify(coordsJson: String, geometryType: String, zoomLevel: Double): String {
        val tolerance = getTolerance(zoomLevel)
        if (tolerance <= 0.000001) return coordsJson  // لا تبسيط عند الزوم العالي

        return try {
            val wkt = GeometryValidator.coordsToWkt(geometryType, coordsJson)
            val geom = org.locationtech.jts.io.WKTReader(geometryFactory).read(wkt)

            val simplified = when {
                geom.numPoints < 20 -> geom  // لا تبسيط للهندسات الصغيرة
                tolerance < 0.0001 -> TopologyPreservingSimplifier.simplify(geom, tolerance)
                else -> DouglasPeuckerSimplifier.simplify(geom, tolerance)
            }

            if (simplified.numPoints < geom.numPoints * 0.8) {
                GeometryParser.geometryToCoords(simplified)
            } else {
                coordsJson
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Simplification failed: ${e.message}")
            coordsJson
        }
    }

    /**
     * حساب مساحة bbox التقريبية لـ feature
     */
    fun estimateBoundsArea(coordsJson: String): Double {
        try {
            val arr = org.json.JSONArray(coordsJson)
            val coords = GeometryValidator.parseCoordPairs(arr.toString())
            if (coords.isEmpty()) return 0.0
            var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
            for ((lon, lat) in coords) {
                if (lat < minLat) minLat = lat; if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon; if (lon > maxLon) maxLon = lon
            }
            return (maxLat - minLat) * (maxLon - minLon)
        } catch (_: Exception) { return 0.0 }
    }
}
