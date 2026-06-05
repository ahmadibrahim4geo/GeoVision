package com.geovision.mobile.data

import android.content.Context
import com.geovision.mobile.ui.screens.layers.FeatureRow

class ViewportFeatureQueryService(context: Context) {
    private val appContext = context.applicationContext
    private val metadata = LayerMetadataRepository(appContext)

    data class BoundingBox(
        val minX: Double,
        val minY: Double,
        val maxX: Double,
        val maxY: Double
    )

    fun getFeaturesInViewport(
        layerId: String,
        bbox: BoundingBox,
        zoom: Double,
        limit: Int = 500,
        offset: Int = 0
    ): List<FeatureRow> {
        val layer = metadata.findLayer(layerId) ?: return emptyList()
        val tableName = layer.gpkgTableName ?: return emptyList()
        val paths = ProjectStorageManager.ensureDefaultProject(appContext)
        if (!paths.projectGpkg.exists()) return emptyList()
        val queryLimit = limitForZoom(zoom, limit)
        return GeoPackageWriter(paths.projectGpkg).getFeaturesInViewport(
            tableName = tableName,
            minX = bbox.minX,
            minY = bbox.minY,
            maxX = bbox.maxX,
            maxY = bbox.maxY,
            limit = queryLimit,
            offset = offset
        )
    }

    private fun limitForZoom(zoom: Double, requestedLimit: Int): Int {
        val cap = when {
            zoom < 8.0 -> 250
            zoom < 12.0 -> 500
            zoom < 16.0 -> 1_000
            else -> 2_000
        }
        return requestedLimit.coerceIn(1, cap)
    }
}
