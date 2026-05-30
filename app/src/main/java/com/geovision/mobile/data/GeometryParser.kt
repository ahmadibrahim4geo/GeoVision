/**
 * محلّل الهندسة الجغرافية (GeometryParser).
 * مسؤول عن تحويل إحداثيات JSON الواردة من ملفات GeoJSON
 * إلى نقاط جغرافية (GeoPoint) يمكن عرضها على خريطة osmdroid.
 */
package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import org.json.JSONArray
import org.osmdroid.util.GeoPoint

object GeometryParser {
    private const val TAG = "GeometryParser"

    // ── تحويل الإحداثيات إلى حلقات (Rings) من النقاط ──

    /**
     * يحوّل سلسلة JSON للإحداثيات إلى قائمة من الحلقات (rings)،
     * حيث كل حلقة هي قائمة نقاط GeoPoint.
     * يعالج أنواع الهندسة: Point, MultiPoint, LineString, MultiLineString, Polygon, MultiPolygon.
     *
     * @param geometryType نوع الهندسة (Point, LineString...)
     * @param coordinatesJson سلسلة JSON تحتوي على الإحداثيات
     * @return قائمة من الحلقات، كل حلقة هي قائمة نقاط GeoPoint
     */
    fun parseRings(geometryType: String?, coordinatesJson: String?): List<List<GeoPoint>> {
        if (coordinatesJson.isNullOrBlank() || geometryType == null) return emptyList()
        return try {
            val arr = JSONArray(coordinatesJson)
            when (geometryType) {
                "Point" -> listOf(listOf(parsePoint(arr)))
                "MultiPoint" -> listOf(parseLine(arr))
                "LineString" -> listOf(parseLine(arr))
                "MultiLineString" -> (0 until arr.length()).map { parseLine(arr.getJSONArray(it)) }
                "Polygon" -> (0 until arr.length()).map { parseLine(arr.getJSONArray(it)) }
                "MultiPolygon" -> arr.flatMapRings() // دالة مساعدة لتسطيح المضلّعات المتعددة
                else -> emptyList()
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "parseRings failed: type=$geometryType, error=${e.message}")
            emptyList()
        }
    }

    // ── حساب النقطة المركزية (Centroid) ──

    /**
     * يحسب النقطة المركزية (centroid) لهندسة ما عن طريق
     * حساب متوسط خطوط الطول والعرض لجميع النقاط في الحلقة الأولى.
     *
     * @param geometryType نوع الهندسة
     * @param coordinatesJson سلسلة JSON للإحداثيات
     * @return نقطة GeoPoint تمثل المركز، أو null في حال الفشل
     */
    fun parseCentroid(geometryType: String?, coordinatesJson: String?): GeoPoint? {
        val rings = parseRings(geometryType, coordinatesJson) ?: return null
        if (rings.isEmpty() || rings[0].isEmpty()) return null
        val pts = rings[0]
        val lat = pts.map { it.latitude }.average()   // متوسط خطوط العرض
        val lon = pts.map { it.longitude }.average()  // متوسط خطوط الطول
        return GeoPoint(lat, lon)
    }

    /**
     * يحوّل الإحداثيات إلى مجموعات مضلعات (polygon sets).
     * كل مجموعة تمثل مضلعًا واحدًا مع جميع حلقاته (الخارجية + الثقوب).
     * مناسب لرسم المضلعات على الخريطة.
     */
    fun parsePolygonSets(geometryType: String?, coordinatesJson: String?): List<List<List<GeoPoint>>> {
        if (coordinatesJson.isNullOrBlank() || geometryType == null) return emptyList()
        return try {
            val arr = JSONArray(coordinatesJson)
            when (geometryType) {
                "Polygon" -> listOf((0 until arr.length()).map { parseLine(arr.getJSONArray(it)) })
                "MultiPolygon" -> (0 until arr.length()).map { i ->
                    val polygonArr = arr.getJSONArray(i)
                    (0 until polygonArr.length()).map { j -> parseLine(polygonArr.getJSONArray(j)) }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "parsePolygonSets failed: type=$geometryType, error=${e.message}")
            emptyList()
        }
    }

    // ── دوال مساعدة خاصّة ──

    /** تحويل مصفوفة JSON إلى نقطة GeoPoint (تنسيق: [longitude, latitude]) */
    private fun parsePoint(arr: JSONArray): GeoPoint {
        return GeoPoint(arr.getDouble(1), arr.getDouble(0))
    }

    /** تحويل مصفوفة JSON إلى قائمة نقاط GeoPoint */
    private fun parseLine(arr: JSONArray): List<GeoPoint> {
        return (0 until arr.length()).map { parsePoint(arr.getJSONArray(it)) }
    }

    /** دالة إضافيّة لـ JSONArray لتسطيح حلقات المضلّعات المتعددة (MultiPolygon) */
    private fun JSONArray.flatMapRings(): List<List<GeoPoint>> {
        val result = mutableListOf<List<GeoPoint>>()
        for (i in 0 until length()) {
            val polygon = getJSONArray(i)              // كل عنصر هو مضلّع
            for (j in 0 until polygon.length()) {
                result.add(parseLine(polygon.getJSONArray(j))) // كل مضلّع قد يضم عدّة حلقات
            }
        }
        return result
    }

    // ── تحويل سلسلة JSON إحداثيات إلى JTS Geometry ──

    /**
     * تحويل سلسلة JSON للإحداثيات (بنفس تنسيق GeometryParser.geometryToCoords)
     * إلى كائن JTS Geometry للنوع المحدد.
     * @return JTS Geometry أو null في حال الفشل
     */
    fun coordsToGeometry(coordinatesJson: String?, geomType: String): org.locationtech.jts.geom.Geometry? {
        if (coordinatesJson.isNullOrBlank()) return null
        return try {
            val factory = org.locationtech.jts.geom.GeometryFactory()
            val arr = JSONArray(coordinatesJson)
            when (geomType) {
                "Point" -> {
                    if (arr.length() >= 2) factory.createPoint(org.locationtech.jts.geom.Coordinate(arr.getDouble(0), arr.getDouble(1)))
                    else null
                }
                "MultiPoint" -> {
                    val pts = (0 until arr.length()).map { i ->
                        val p = arr.getJSONArray(i)
                        org.locationtech.jts.geom.Coordinate(p.getDouble(0), p.getDouble(1))
                    }
                    factory.createMultiPoint(pts.map { factory.createPoint(it) }.toTypedArray())
                }
                "LineString" -> {
                    val coords = (0 until arr.length()).map { i ->
                        val p = arr.getJSONArray(i)
                        org.locationtech.jts.geom.Coordinate(p.getDouble(0), p.getDouble(1))
                    }
                    factory.createLineString(coords.toTypedArray())
                }
                "MultiLineString" -> {
                    val lines = (0 until arr.length()).map { i ->
                        val lineArr = arr.getJSONArray(i)
                        val coords = (0 until lineArr.length()).map { j ->
                            val p = lineArr.getJSONArray(j)
                            org.locationtech.jts.geom.Coordinate(p.getDouble(0), p.getDouble(1))
                        }
                        factory.createLineString(coords.toTypedArray())
                    }
                    factory.createMultiLineString(lines.toTypedArray())
                }
                "Polygon" -> {
                    val rings = (0 until arr.length()).map { i ->
                        val ringArr = arr.getJSONArray(i)
                        val coords = (0 until ringArr.length()).map { j ->
                            val p = ringArr.getJSONArray(j)
                            org.locationtech.jts.geom.Coordinate(p.getDouble(0), p.getDouble(1))
                        }
                        factory.createLinearRing(coords.toTypedArray())
                    }
                    if (rings.isNotEmpty()) factory.createPolygon(rings[0], rings.drop(1).toTypedArray())
                    else null
                }
                "MultiPolygon" -> {
                    val polys = (0 until arr.length()).map { i ->
                        val polyArr = arr.getJSONArray(i)
                        val rings = (0 until polyArr.length()).map { j ->
                            val ringArr = polyArr.getJSONArray(j)
                            val coords = (0 until ringArr.length()).map { k ->
                                val p = ringArr.getJSONArray(k)
                                org.locationtech.jts.geom.Coordinate(p.getDouble(0), p.getDouble(1))
                            }
                            factory.createLinearRing(coords.toTypedArray())
                        }
                        if (rings.isNotEmpty()) factory.createPolygon(rings[0], rings.drop(1).toTypedArray())
                        else null
                    }
                    factory.createMultiPolygon(polys.filterNotNull().toTypedArray())
                }
                else -> null
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "coordsToGeometry failed: type=$geomType, error=${e.message}")
            null
        }
    }

    // ── تحويل JTS Geometry إلى تنسيق الإحداثيات المستخدم في التطبيق ──

    /**
     * تحويل كائن JTS Geometry إلى سلسلة JSON للإحداثيات بنفس تنسيق التطبيق.
     */
    fun geometryToCoords(geom: org.locationtech.jts.geom.Geometry): String {
        return when (geom) {
            is org.locationtech.jts.geom.Point -> "[${geom.x},${geom.y}]"
            is org.locationtech.jts.geom.LineString -> {
                val pts = (0 until geom.numPoints).joinToString(",") { i ->
                    val c = geom.getCoordinateN(i)
                    "[${c.x},${c.y}]"
                }
                "[$pts]"
            }
            is org.locationtech.jts.geom.Polygon -> {
                val rings = mutableListOf<String>()
                rings.add(ringToCoords(geom.exteriorRing))
                for (i in 0 until geom.numInteriorRing) {
                    rings.add(ringToCoords(geom.getInteriorRingN(i)))
                }
                "[${rings.joinToString(",")}]"
            }
            is org.locationtech.jts.geom.MultiPolygon -> {
                val polys = (0 until geom.numGeometries).joinToString(",") { i ->
                    geometryToCoords(geom.getGeometryN(i))
                }
                "[$polys]"
            }
            is org.locationtech.jts.geom.MultiLineString -> {
                val lines = (0 until geom.numGeometries).joinToString(",") { i ->
                    geometryToCoords(geom.getGeometryN(i))
                }
                "[$lines]"
            }
            is org.locationtech.jts.geom.MultiPoint -> {
                val pts = (0 until geom.numGeometries).joinToString(",") { i ->
                    val c = geom.getGeometryN(i).coordinate
                    "[${c.x},${c.y}]"
                }
                "[$pts]"
            }
            else -> "[]"
        }
    }

    private fun ringToCoords(ring: org.locationtech.jts.geom.LinearRing): String {
        val coords = ring.coordinates
        val pts = coords.joinToString(",") { c -> "[${c.x},${c.y}]" }
        return "[$pts]"
    }
}
