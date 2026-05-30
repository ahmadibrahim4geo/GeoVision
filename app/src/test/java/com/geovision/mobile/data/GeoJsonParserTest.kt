package com.geovision.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoJsonParserTest {

    @Test
    fun parseErrorType_enumValues() {
        assertEquals(9, GeoJsonParser.ParseErrorType.values().size)
        assertTrue(GeoJsonParser.ParseErrorType.NONE.name == "NONE")
        assertTrue(GeoJsonParser.ParseErrorType.CORRUPTED.name == "CORRUPTED")
        assertTrue(GeoJsonParser.ParseErrorType.OUT_OF_MEMORY.name == "OUT_OF_MEMORY")
    }

    @Test
    fun maxFeatures_positive() {
        assertTrue(GeoJsonParser.MAX_FEATURES > 0)
    }

    @Test
    fun parseResult_defaults() {
        val result = GeoJsonParser.ParseResult()
        assertEquals(0, result.features.size)
        assertEquals(0, result.featureCount)
        assertNotNull(result.extent)
    }

    @Test
    fun parseResult_withFeatures() {
        val features = listOf(
            com.geovision.mobile.ui.screens.layers.FeatureRow(
                id = "f1",
                properties = mapOf("name" to "test"),
                geometryType = "Point",
                geometryCoordinates = "[46.7,24.7]"
            )
        )
        val result = GeoJsonParser.ParseResult(
            features = features,
            fileName = "test.geojson",
            crs = "EPSG:4326",
            extent = "24.7\u00B0 \u2014 24.7\u00B0 | 46.7\u00B0 \u2014 46.7\u00B0"
        )
        assertEquals(1, result.features.size)
        assertEquals("test.geojson", result.fileName)
        assertEquals("EPSG:4326", result.crs)
    }

    @Test
    fun featureRow_constructs() {
        val row = com.geovision.mobile.ui.screens.layers.FeatureRow(
            id = "test-id",
            properties = mapOf("key1" to "val1", "key2" to "val2"),
            geometryType = "Polygon",
            geometryCoordinates = "[[[0,0],[1,0],[1,1],[0,0]]]"
        )
        assertEquals("test-id", row.id)
        assertEquals(2, row.properties.size)
        assertEquals("Polygon", row.geometryType)
    }
}
