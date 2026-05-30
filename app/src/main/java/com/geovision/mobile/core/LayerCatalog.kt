package com.geovision.mobile.core

import androidx.compose.runtime.mutableStateListOf
import com.geovision.mobile.data.GeoJsonParser
import com.geovision.mobile.data.GpkgReader
import com.geovision.mobile.ui.screens.layers.FeatureRow
import com.geovision.mobile.ui.screens.layers.Layer
import com.geovision.mobile.ui.screens.layers.LayerDetailInfo
import com.geovision.mobile.ui.screens.layers.LayerNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LayerCatalog {
    private val _layers = MutableStateFlow<List<Layer>>(emptyList())
    val layers: StateFlow<List<Layer>> = _layers.asStateFlow()

    private val _layerDetails = mutableMapOf<String, LayerDetailInfo>()

    fun addLayer(layer: Layer) {
        _layers.value = _layers.value + layer
    }

    fun removeLayer(id: String) {
        _layers.value = _layers.value.filter { it.id != id }
        _layerDetails.remove(id)
    }

    fun updateLayer(id: String, transform: (Layer) -> Layer) {
        _layers.value = _layers.value.map { if (it.id == id) transform(it) else it }
    }

    fun getLayer(id: String): Layer? = _layers.value.find { it.id == id }

    fun getDetail(id: String): LayerDetailInfo? = _layerDetails[id]

    fun setDetail(id: String, detail: LayerDetailInfo) {
        _layerDetails[id] = detail
    }

    fun getFeatureCount(id: String): Int = _layerDetails[id]?.features?.size ?: 0

    fun getGeometryType(id: String): String? {
        val features = _layerDetails[id]?.features ?: return null
        return deriveGeomType(features)
    }

    fun getExtent(id: String): String {
        val features = _layerDetails[id]?.features ?: return ""
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
        for (f in features) {
            val coords = GeoJsonParser.extractCoordinates(f.geometryCoordinates)
            for ((lon, lat) in coords) {
                if (lat < minLat) minLat = lat; if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon; if (lon > maxLon) maxLon = lon
            }
        }
        return if (minLat == Double.MAX_VALUE) ""
        else String.format(java.util.Locale.US, "%.4f", minLat) + "° — " +
             String.format(java.util.Locale.US, "%.4f", maxLat) + "° | " +
             String.format(java.util.Locale.US, "%.4f", minLon) + "° — " +
             String.format(java.util.Locale.US, "%.4f", maxLon) + "°"
    }

    fun getCRS(id: String): String = _layerDetails[id]?.crs ?: "EPSG:4326"

    fun reorderLayer(fromIndex: Int, toIndex: Int) {
        val list = _layers.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _layers.value = list
    }

    fun moveToTop(id: String) {
        val list = _layers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val item = list.removeAt(idx)
        list.add(item)
        _layers.value = list
    }

    fun moveToBottom(id: String) {
        val list = _layers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val item = list.removeAt(idx)
        list.add(0, item)
        _layers.value = list
    }

    fun getVisibleLayers(): List<Layer> = _layers.value.filter { it.isVisible }

    fun getLayersByType(type: String): List<Layer> =
        _layers.value.filter { it.fileType.name == type }

    fun toLayerTree(): List<LayerNode> = _layers.value.map { LayerNode(layer = it) }

    fun getVisibleLayerIds(): Set<String> =
        _layers.value.filter { it.isVisible }.map { it.id }.toSet()

    fun allLayerIds(): List<String> = _layers.value.map { it.id }

    companion object {
        private val _instance = LayerCatalog()

        fun getInstance(): LayerCatalog = _instance

        fun getVisibleLayerIds(): Set<String> = _instance.getVisibleLayerIds()

        fun allLayerIds(): List<String> = _instance.allLayerIds()
        fun deriveGeomType(features: List<FeatureRow>): String? {
            if (features.isEmpty()) return null
            val counts = features.groupBy { feat ->
                when (feat.geometryType) {
                    "Point", "MultiPoint" -> "Point"
                    "LineString", "MultiLineString" -> "Line"
                    "Polygon", "MultiPolygon" -> "Polygon"
                    else -> null
                }
            }
            val known = counts.filterKeys { it != null }
            if (known.isEmpty()) return null
            return known.maxByOrNull { it.value.size }?.key
        }
    }
}
