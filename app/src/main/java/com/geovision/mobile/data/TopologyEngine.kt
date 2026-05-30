package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.operation.union.UnaryUnionOp

/**
 * JTS-based topological operations for GIS analysis.
 * Provides Union, Dissolve, Clip, and validity checks.
 */
object TopologyEngine {
    private const val TAG = "TopologyEngine"
    private val geometryFactory = org.locationtech.jts.geom.GeometryFactory()

    private fun toJtsGeometry(coords: String?, geomType: String?): org.locationtech.jts.geom.Geometry? {
        if (coords == null || geomType == null) return null
        return GeometryParser.coordsToGeometry(coords, geomType)
    }

    /**
     * Perform a unary union (dissolve) on all geometries in a layer.
     * Merges adjacent/overlapping polygons into single geometry.
     * @return new FeatureRow with dissolved geometry, or null if failed
     */
    fun dissolve(features: List<FeatureRow>, layerName: String): FeatureRow? {
        if (features.isEmpty()) return null
        val first = features.first()
        val geoms = features.mapNotNull { toJtsGeometry(it.geometryCoordinates, it.geometryType) }
            .filter { it.isValid }
        if (geoms.isEmpty()) return null

        return try {
            val union = UnaryUnionOp(geoms).union()
            val coords = GeometryParser.geometryToCoords(union)
            FeatureRow(
                id = "dissolved_$layerName",
                properties = mapOf("type" to "dissolved", "count" to geoms.size.toString()),
                geometryCoordinates = coords,
                geometryType = union.geometryType,
                crs = first.crs
            )
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.GIS, "Dissolve failed: ${e.message}")
            null
        }
    }

    /**
     * Clip features within a bounding box.
     * @return features whose geometry intersects the bbox
     */
    fun clipByBbox(features: List<FeatureRow>, minLon: Double, minLat: Double, maxLon: Double, maxLat: Double): List<FeatureRow> {
        val envelope = geometryFactory.toGeometry(
            Envelope(minLon, maxLon, minLat, maxLat)
        )
        return features.mapNotNull { feature ->
            val geom = toJtsGeometry(feature.geometryCoordinates, feature.geometryType) ?: return@mapNotNull null
            if (!envelope.intersects(geom)) return@mapNotNull null
            try {
                val clipped = envelope.intersection(geom)
                if (clipped.isEmpty) return@mapNotNull null
                feature.copy(
                    geometryCoordinates = GeometryParser.geometryToCoords(clipped),
                    geometryType = clipped.geometryType
                )
            } catch (e: Exception) { null }
        }
    }

    /**
     * Validate all geometries in a feature list and return only valid ones.
     * Reports count of invalid geometries.
     */
    fun validateAll(features: List<FeatureRow>): Pair<List<FeatureRow>, Int> {
        var invalid = 0
        val valid = features.filter { f ->
            val geom = toJtsGeometry(f.geometryCoordinates, f.geometryType)
            if (geom == null || !geom.isValid) { invalid++; false }
            else true
        }
        return Pair(valid, invalid)
    }

    /**
     * Count topological errors (self-intersections, gaps, slivers) in features.
     */
    fun topologyErrorCount(features: List<FeatureRow>): Int {
        return features.count { f ->
            val geom = toJtsGeometry(f.geometryCoordinates, f.geometryType)
            geom == null || !geom.isValid || !geom.isSimple
        }
    }
}
