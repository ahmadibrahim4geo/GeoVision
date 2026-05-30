package com.geovision.mobile.data

import android.graphics.Rect
import org.locationtech.jts.index.strtree.STRtree
import org.osmdroid.util.GeoPoint

/**
 * Spatial Index for fast viewport queries using JTS STRtree (R-Tree).
 * Enables O(log n) bounding-box lookups instead of O(n) full scans.
 */
class SpatialIndex<T> {
    private val tree = STRtree()
    private val items = mutableListOf<IndexEntry<T>>()
    private var built = false

    data class IndexEntry<T>(
        val minLat: Double, val minLon: Double,
        val maxLat: Double, val maxLon: Double,
        val data: T
    )

    fun insert(item: T, points: List<GeoPoint>) {
        if (points.isEmpty()) return
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
        for (p in points) {
            if (p.latitude < minLat) minLat = p.latitude
            if (p.latitude > maxLat) maxLat = p.latitude
            if (p.longitude < minLon) minLon = p.longitude
            if (p.longitude > maxLon) maxLon = p.longitude
        }
        items.add(IndexEntry(minLat, minLon, maxLat, maxLon, item))
        built = false
    }

    fun build() {
        if (built) return
        items.forEach { entry ->
            tree.insert(
                org.locationtech.jts.geom.Envelope(entry.minLon, entry.maxLon, entry.minLat, entry.maxLat),
                entry
            )
        }
        tree.build()
        built = true
    }

    /**
     * Query all items whose bounding box intersects the given viewport.
     */
    fun query(viewportLatMin: Double, viewportLatMax: Double, viewportLonMin: Double, viewportLonMax: Double): List<IndexEntry<T>> {
        if (!built) build()
        val envelope = org.locationtech.jts.geom.Envelope(viewportLonMin, viewportLonMax, viewportLatMin, viewportLatMax)
        @Suppress("UNCHECKED_CAST")
        return tree.query(envelope) as List<IndexEntry<T>>
    }

    /**
     * Query items visible in the given map viewport (in pixels).
     */
    fun queryViewport(
        viewportBounds: org.osmdroid.util.BoundingBox?,
        viewportRect: Rect?
    ): List<IndexEntry<T>> {
        if (viewportBounds == null) return items
        return query(
            viewportBounds.latSouth, viewportBounds.latNorth,
            viewportBounds.lonWest, viewportBounds.lonEast
        )
    }

    fun clear() {
        items.clear()
        built = false
    }

    val size: Int get() = items.size
    val isBuilt: Boolean get() = built
}
