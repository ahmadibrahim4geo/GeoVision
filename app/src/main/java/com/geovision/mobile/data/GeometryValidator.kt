package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import org.locationtech.jts.geom.*
import org.locationtech.jts.io.ParseException
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.operation.valid.TopologyValidationError

object GeometryValidator {
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    data class RepairResult(
        val success: Boolean,
        val geometryType: String,
        val coordinates: String,
        val message: String = ""
    )

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
    private val reader = WKTReader(geometryFactory)

    /**
     * تحويل إحداثيات JSON إلى WKT
     */
    fun coordsToWkt(geometryType: String, coordinatesJson: String): String {
        return when (geometryType) {
            "Point" -> {
                val cleaned = coordinatesJson.trim('[', ']', ' ')
                val parts = cleaned.split(",").map { it.trim() }
                if (parts.size >= 2) "POINT (${parts[0]} ${parts[1]})" else "POINT EMPTY"
            }
            "LineString" -> {
                val pts = parseCoordPairs(coordinatesJson)
                if (pts.isEmpty()) "LINESTRING EMPTY"
                else "LINESTRING (${pts.joinToString(", ") { "${it.first} ${it.second}" }})"
            }
            "Polygon" -> {
                val rings = parseRingsJson(coordinatesJson)
                if (rings.isEmpty()) "POLYGON EMPTY"
                else "POLYGON (${rings.joinToString(", ") { ring -> "(${ring.joinToString(", ") { "${it.first} ${it.second}" }})" }})"
            }
            else -> "GEOMETRYCOLLECTION EMPTY"
        }
    }

    /**
     * التحقق من صحة الهندسة
     */
    fun validate(geometryType: String, coordinatesJson: String): ValidationResult {
        try {
            val wkt = coordsToWkt(geometryType, coordinatesJson)
            val geom = reader.read(wkt)
            val isValidOp = IsValidOp(geom)
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            if (!isValidOp.isValid) {
                val validationError = isValidOp.validationError
                if (validationError != null) {
                    errors.add("${validationError.errorType}: ${validationError.message} at ${validationError.coordinate}")
                } else {
                    errors.add("Invalid geometry")
                }
            }

            // تحقق من orientation الحلقات
            if (geom is Polygon) {
                val shell = geom.exteriorRing
                if (!isCcw(shell)) {
                    warnings.add("Outer ring should be counter-clockwise")
                }
                for (i in 0 until geom.numInteriorRing) {
                    val ring = geom.getInteriorRingN(i)
                    if (isCcw(ring)) {
                        warnings.add("Inner ring $i should be clockwise")
                    }
                }
            }

            // تحقق من closure
            if (geom is Polygon) {
                if (!geom.exteriorRing.isClosed) {
                    errors.add("Outer ring is not closed")
                }
                for (i in 0 until geom.numInteriorRing) {
                    if (!geom.getInteriorRingN(i).isClosed) {
                        errors.add("Inner ring $i is not closed")
                    }
                }
            }

            return ValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )
        } catch (e: ParseException) {
            return ValidationResult(
                isValid = false,
                errors = listOf("WKT parse error: ${e.message}")
            )
        } catch (e: Exception) {
            return ValidationResult(
                isValid = false,
                errors = listOf("Validation error: ${e.message}")
            )
        }
    }

    /**
     * إصلاح الهندسة (إغلاق الحلقات، تصحيح orientation)
     */
    fun repair(geometryType: String, coordinatesJson: String): RepairResult {
        try {
            val wkt = coordsToWkt(geometryType, coordinatesJson)
            val geom = reader.read(wkt)

            val repaired = when {
                geom is Polygon -> repairPolygon(geom)
                geom is MultiPolygon -> {
                    val repairedPolys = (0 until geom.numGeometries).map {
                        repairPolygon(geom.getGeometryN(it) as Polygon)
                    }
                    geometryFactory.createMultiPolygon(repairedPolys.toTypedArray())
                }
                geom is LineString -> repairLineString(geom)
                geom is MultiLineString -> {
                    val repairedLines = (0 until geom.numGeometries).map {
                        repairLineString(geom.getGeometryN(it) as LineString)
                    }
                    geometryFactory.createMultiLineString(repairedLines.toTypedArray())
                }
                else -> geom
            }

            val newCoords = GeometryParser.geometryToCoords(repaired)
            val newType = repaired.geometryType
            return RepairResult(
                success = true,
                geometryType = newType,
                coordinates = newCoords,
                message = if (repaired != geom) "Geometry repaired" else "Geometry is valid"
            )
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Repair failed: ${e.message}")
            return RepairResult(
                success = false,
                geometryType = geometryType,
                coordinates = coordinatesJson,
                message = "Repair failed: ${e.message}"
            )
        }
    }

    private fun repairPolygon(polygon: Polygon): Polygon {
        val shell = polygon.exteriorRing
        val fixedShell = if (!shell.isClosed) closeRing(shell) else shell
        val orientedShell = if (!isCcw(fixedShell)) reverseRing(fixedShell) else fixedShell

        val holes = mutableListOf<LinearRing>()
        for (i in 0 until polygon.numInteriorRing) {
            var ring = polygon.getInteriorRingN(i)
            if (!ring.isClosed) ring = closeRing(ring)
            if (isCcw(ring)) ring = reverseRing(ring)
            holes.add(ring)
        }

        val fixedShellRing = if (orientedShell is LinearRing) orientedShell
            else geometryFactory.createLinearRing(orientedShell.coordinates)
        val holeRings = holes.toTypedArray()

        return try {
            geometryFactory.createPolygon(fixedShellRing, holeRings)
        } catch (e: Exception) {
            geometryFactory.createPolygon(fixedShellRing)
        }
    }

    private fun repairLineString(line: LineString): LineString {
        return if (!line.isClosed && line.numPoints >= 2) line else line
    }

    private fun closeRing(ring: LineString): LinearRing {
        val coords = ring.coordinates
        if (coords.size < 2) return ring as? LinearRing ?: geometryFactory.createLinearRing(coords)
        if (coords.first().distance(coords.last()) > 0.0001) {
            val newCoords = coords + coords.first()
            return geometryFactory.createLinearRing(newCoords)
        }
        return ring as? LinearRing ?: geometryFactory.createLinearRing(coords)
    }

    private fun reverseRing(ring: LineString): LinearRing {
        val reversed = ring.reverse()
        return reversed as? LinearRing ?: geometryFactory.createLinearRing(reversed.coordinates)
    }

    /**
     * التحقق من أن الحلقة في اتجاه عكس عقارب الساعة (CCW)
     */
    private fun isCcw(ring: LineString): Boolean {
        if (ring.numPoints < 3) return true
        val coords = ring.coordinates
        var sum = 0.0
        for (i in 0 until coords.size - 1) {
            val p1 = coords[i]; val p2 = coords[i + 1]
            sum += (p2.x - p1.x) * (p2.y + p1.y)
        }
        return sum > 0
    }

    /**
     * تحليل أزواج الإحداثيات من JSON
     */
    fun parseCoordPairs(json: String): List<Pair<Double, Double>> {
            val result = mutableListOf<Pair<Double, Double>>()
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val pt = arr.getJSONArray(i)
                    result.add(pt.getDouble(0) to pt.getDouble(1))
                }
            } catch (_: Exception) {}
            return result
        }

    /**
     * تحليل حلقات Polygon من JSON
     */
    fun parseRingsJson(json: String): List<List<Pair<Double, Double>>> {
            val result = mutableListOf<List<Pair<Double, Double>>>()
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val ringArr = arr.getJSONArray(i)
                    val ring = mutableListOf<Pair<Double, Double>>()
                    for (j in 0 until ringArr.length()) {
                        val pt = ringArr.getJSONArray(j)
                        ring.add(pt.getDouble(0) to pt.getDouble(1))
                    }
                    result.add(ring)
                }
            } catch (_: Exception) {}
            return result
        }
}
