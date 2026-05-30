package com.geovision.mobile.ui.screens.layers

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LayerViewModelTest {

    private lateinit var viewModel: LayerViewModel

    @Before
    fun setUp() {
        viewModel = LayerViewModel()
    }

    @Test
    fun initialState_emptyLayers() {
        assertTrue(viewModel.layers.isEmpty())
        assertFalse(viewModel.isLoading)
        assertNull(viewModel.loadError)
        assertNull(viewModel.loadWarning)
    }

    @Test
    fun addLayer_layerAdded() {
        val id = viewModel.addLayer("test", FileType.GEOJSON)
        assertEquals(1, viewModel.layers.size)
        assertEquals("test", viewModel.layers[0].name)
        assertEquals(FileType.GEOJSON, viewModel.layers[0].fileType)
        assertEquals(id, viewModel.layers[0].id)
    }

    @Test
    fun addLayer_multipleSequentialIds() {
        val id1 = viewModel.addLayer("a", FileType.KML)
        val id2 = viewModel.addLayer("b", FileType.GPX)
        assertEquals("1", id1)
        assertEquals("2", id2)
    }

    @Test
    fun removeLayer_layerRemoved() {
        val id = viewModel.addLayer("test", FileType.SHAPEFILE)
        assertEquals(1, viewModel.layers.size)
        viewModel.removeLayer(id)
        assertTrue(viewModel.layers.isEmpty())
    }

    @Test
    fun removeLayer_nonexistent_noCrash() {
        viewModel.removeLayer("does_not_exist")
        assertTrue(viewModel.layers.isEmpty())
    }

    @Test
    fun toggleVisibility_changesState() {
        val id = viewModel.addLayer("test", FileType.GEOJSON)
        assertTrue(viewModel.layers[0].isVisible)
        viewModel.toggleVisibility(id)
        assertFalse(viewModel.layers[0].isVisible)
        viewModel.toggleVisibility(id)
        assertTrue(viewModel.layers[0].isVisible)
    }

    @Test
    fun toggleVisibility_nonexistent_noCrash() {
        viewModel.toggleVisibility("does_not_exist")
    }

    @Test
    fun updateTransparency_changesAlpha() {
        val id = viewModel.addLayer("test", FileType.GEOJSON)
        assertEquals(1.0f, viewModel.layers[0].transparency)
        viewModel.updateTransparency(id, 0.5f)
        assertEquals(0.5f, viewModel.layers[0].transparency)
    }

    @Test
    fun updateLayerColor_changesColor() {
        val id = viewModel.addLayer("test", FileType.KML)
        val original = viewModel.layers[0].color
        viewModel.updateLayerColor(id, androidx.compose.ui.graphics.Color.Red)
        assertEquals(androidx.compose.ui.graphics.Color.Red, viewModel.layers[0].color)
        assertNotEquals(original, viewModel.layers[0].color)
    }

    @Test
    fun updatePointSize_changesSize() {
        val id = viewModel.addLayer("test", FileType.GPX)
        viewModel.updatePointSize(id, 12f)
        assertEquals(12f, viewModel.layers[0].pointSize)
    }

    @Test
    fun updateLineWidth_changesWidth() {
        val id = viewModel.addLayer("test", FileType.GEOJSON)
        viewModel.updateLineWidth(id, 6f)
        assertEquals(6f, viewModel.layers[0].lineWidth)
    }

    @Test
    fun moveLayer_reorders() {
        val id1 = viewModel.addLayer("a", FileType.KML)
        val id2 = viewModel.addLayer("b", FileType.GPX)
        viewModel.moveLayer(1, 0)
        assertEquals(id2, viewModel.layers[0].id)
        assertEquals(id1, viewModel.layers[1].id)
    }

    @Test
    fun moveLayer_sameIndex_noChange() {
        viewModel.addLayer("a", FileType.KML)
        viewModel.addLayer("b", FileType.GPX)
        viewModel.moveLayer(0, 0)
        assertEquals(2, viewModel.layers.size)
    }

    @Test
    fun getLayerById_found() {
        val id = viewModel.addLayer("test", FileType.GEOJSON)
        val layer = viewModel.getLayerById(id)
        assertNotNull(layer)
        assertEquals(id, layer?.id)
    }

    @Test
    fun getLayerById_notFound() {
        assertNull(viewModel.getLayerById("nonexistent"))
    }

    @Test
    fun getCachedDetail_noData() {
        assertNull(viewModel.getCachedDetail("any_id"))
    }

    @Test
    fun getPhotos_empty_noData() {
        assertTrue(viewModel.getPhotos("any_id").isEmpty())
    }

    @Test
    fun loadDetailForLayer_noDetail() {
        viewModel.loadDetailForLayer("any_id")
        assertNull(viewModel.layerDetail)
    }

    @Test
    fun randomLayerColor_returnsDifferentColors() {
        val colors = (1..100).map { LayerViewModel.randomLayerColor() }.distinct()
        assertTrue(colors.size > 50)
    }

    @Test
    fun addLayer_withFilePath() {
        val id = viewModel.addLayer("test", FileType.SHAPEFILE, "/data/test.shp")
        assertEquals("/data/test.shp", viewModel.getLayerById(id)?.filePath)
    }

    @Test
    fun removeLayer_clearsDetails() {
        val id = viewModel.addLayer("test", FileType.GEOJSON)
        viewModel.removeLayer(id)
        assertNull(viewModel.getCachedDetail(id))
        assertTrue(viewModel.getPhotos(id).isEmpty())
    }

    @Test
    fun loadState_initialValues() {
        val state = viewModel.loadState.value
        assertFalse(state.isLoading)
        assertNull(state.loadError)
        assertNull(state.loadWarning)
        assertEquals(0L, state.lastParseTimeMs)
    }

    @Test
    fun requestZoomToLayer_setsRequestedId() {
        val id = viewModel.addLayer("test", FileType.GEOJSON)
        assertNull(viewModel.requestedZoomLayerId)
        viewModel.requestZoomToLayer(id)
        assertEquals(id, viewModel.requestedZoomLayerId)
    }

    @Test
    fun requestZoomToFeature_setsCoords() {
        assertNull(viewModel.requestedZoomFeatureCoords)
        viewModel.requestZoomToFeature(24.7, 46.7)
        assertEquals(24.7 to 46.7, viewModel.requestedZoomFeatureCoords)
    }
}
