package com.geovision.mobile.ui.screens.calculator

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CalculatorViewModelTest {

    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        viewModel = CalculatorViewModel()
    }

    @Test
    fun initialState() {
        val state = viewModel.state
        assertTrue(state.inputText.isEmpty())
        assertTrue(state.results.isEmpty())
        assertTrue(state.statusMessage.isNotEmpty())
        assertTrue(state.visibleFormats.size == OutputFormat.entries.size)
    }

    @Test
    fun stateFlow_initialValue() {
        val state = viewModel.stateFlow.value
        assertNotNull(state)
        assertTrue(state.inputText.isEmpty())
    }

    @Test
    fun updateInput_updatesText() {
        viewModel.updateInput("24.7, 46.7")
        assertEquals("24.7, 46.7", viewModel.state.inputText)
        assertEquals("24.7, 46.7", viewModel.stateFlow.value.inputText)
    }

    @Test
    fun convert_blankInput_showsWarning() {
        viewModel.convert()
        assertTrue(viewModel.state.results.isEmpty())
        assertTrue(viewModel.state.statusMessage.contains("⚠️"))
    }

    @Test
    fun convert_singleDD() {
        viewModel.updateInput("24.7136, 46.6753")
        viewModel.convert()
        val state = viewModel.state
        assertTrue(state.results.isNotEmpty())
        assertEquals(1, state.results.size)
        val point = state.results[0]
        assertFalse(point.error)
        assertNotNull(point.lat)
        assertNotNull(point.lon)
        assertEquals(24.7136, point.lat!!, 0.0001)
        assertEquals(46.6753, point.lon!!, 0.0001)
    }

    @Test
    fun convert_multipleLines() {
        viewModel.updateInput("24.7, 46.7\n25.0, 47.0")
        viewModel.convert()
        assertEquals(2, viewModel.state.results.size)
        assertFalse(viewModel.state.results[0].error)
        assertFalse(viewModel.state.results[1].error)
    }

    @Test
    fun convert_invalidLine_showsError() {
        viewModel.updateInput("not coordinates")
        viewModel.convert()
        val result = viewModel.state.results
        assertEquals(1, result.size)
        assertTrue(result[0].error)
        assertEquals("N/A", result[0].dd)
    }

    @Test
    fun convert_mixedValidAndInvalid() {
        viewModel.updateInput("24.7, 46.7\ninvalid\n25.0, 47.0")
        viewModel.convert()
        val results = viewModel.state.results
        assertEquals(3, results.size)
        assertFalse(results[0].error)
        assertTrue(results[1].error)
        assertFalse(results[2].error)
    }

    @Test
    fun toggleFormat_togglesVisibility() {
        // Initially all formats visible (selectedFormats is empty)
        assertEquals(OutputFormat.entries.size, viewModel.state.visibleFormats.size)
        // Toggle DD: adds it to selectedFormats, so only DD is visible
        viewModel.toggleFormat(OutputFormat.DD)
        assertEquals(1, viewModel.state.visibleFormats.size)
        assertTrue(OutputFormat.DD in viewModel.state.visibleFormats)
        // Toggle DD again: removes from selectedFormats (empty), so all are visible again
        viewModel.toggleFormat(OutputFormat.DD)
        assertEquals(OutputFormat.entries.size, viewModel.state.visibleFormats.size)
    }

    @Test
    fun selectAllFormats_clearsSelection() {
        viewModel.toggleFormat(OutputFormat.DD)
        viewModel.selectAllFormats()
        assertTrue(viewModel.state.selectedFormats.isEmpty())
        assertTrue(viewModel.state.visibleFormats.size == OutputFormat.entries.size)
    }

    @Test
    fun setLatHemisphere_setsCorrectly() {
        assertEquals('N', viewModel.state.latHemisphere)
        viewModel.setLatHemisphere('S')
        assertEquals('S', viewModel.state.latHemisphere)
    }

    @Test
    fun setLonHemisphere_setsCorrectly() {
        assertEquals('E', viewModel.state.lonHemisphere)
        viewModel.setLonHemisphere('W')
        assertEquals('W', viewModel.state.lonHemisphere)
    }

    @Test
    fun setUtmZone_clamps() {
        viewModel.setUtmZone(0)
        assertEquals(1, viewModel.state.utmZone)
        viewModel.setUtmZone(61)
        assertEquals(60, viewModel.state.utmZone)
        viewModel.setUtmZone(36)
        assertEquals(36, viewModel.state.utmZone)
    }

    @Test
    fun setUtmHemisphere_setsCorrectly() {
        assertEquals('N', viewModel.state.utmHemisphere)
        viewModel.setUtmHemisphere('S')
        assertEquals('S', viewModel.state.utmHemisphere)
    }

    @Test
    fun clear_resetsState() {
        viewModel.updateInput("24.7, 46.7")
        viewModel.convert()
        assertTrue(viewModel.state.results.isNotEmpty())
        viewModel.clear()
        assertTrue(viewModel.state.inputText.isEmpty())
        assertTrue(viewModel.state.results.isEmpty())
    }

    @Test
    fun getResultsAsText_formatHeaders() {
        viewModel.updateInput("24.7, 46.7")
        viewModel.convert()
        val text = viewModel.getResultsAsText()
        assertTrue(text.contains("#"))
        assertTrue(text.contains("Original"))
        assertTrue(text.contains("DD"))
        assertTrue(text.contains("DMS"))
        assertTrue(text.contains("UTM"))
        assertTrue(text.contains("EPSG:3857"))
    }

    @Test
    fun getResultsAsCsv_formatHeaders() {
        viewModel.updateInput("24.7, 46.7")
        viewModel.convert()
        val csv = viewModel.getResultsAsCsv()
        assertTrue(csv.contains("#,Original,DD,DMS,DDM,UTM,EPSG:3857"))
    }

    @Test
    fun getResultsAsText_withFormatFilter() {
        viewModel.updateInput("24.7, 46.7")
        viewModel.convert()
        val text = viewModel.getResultsAsText(setOf(OutputFormat.DD, OutputFormat.DMS))
        assertTrue(text.contains("#\tOriginal\tDD\tDMS"))
        assertFalse(text.contains("EPSG:3857"))
        assertFalse(text.contains("UTM"))
    }

    @Test
    fun convert_multipleLinesResultsCorrect() {
        viewModel.updateInput("24.7, 46.7\n25.0, 47.0")
        viewModel.convert()
        val results = viewModel.state.results
        assertEquals(1, results[0].index)
        assertEquals(2, results[1].index)
    }

    @Test
    fun visibleFormats_allSelected_whenEmpty() {
        assertTrue(viewModel.state.selectedFormats.isEmpty())
        assertEquals(OutputFormat.entries.size, viewModel.state.visibleFormats.size)
    }

    @Test
    fun visibleFormats_filters_whenSelected() {
        viewModel.toggleFormat(OutputFormat.UTM)
        viewModel.toggleFormat(OutputFormat.EPSG3857)
        // Only toggled (selected) formats are visible
        assertTrue(OutputFormat.UTM in viewModel.state.visibleFormats)
        assertTrue(OutputFormat.EPSG3857 in viewModel.state.visibleFormats)
        assertFalse(OutputFormat.DD in viewModel.state.visibleFormats)
    }
}
