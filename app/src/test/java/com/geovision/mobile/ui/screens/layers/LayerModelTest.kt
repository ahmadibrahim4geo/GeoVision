package com.geovision.mobile.ui.screens.layers

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class LayerModelTest {

    @Test
    fun layer_defaultValues() {
        val layer = Layer(id = "1", name = "test", fileType = FileType.GEOJSON)
        assertEquals("1", layer.id)
        assertEquals("test", layer.name)
        assertEquals(FileType.GEOJSON, layer.fileType)
        assertNull(layer.geomType)
        assertTrue(layer.isVisible)
        assertEquals(8f, layer.pointSize)
        assertEquals(4f, layer.lineWidth)
        assertEquals(1.0f, layer.transparency)
        assertEquals(0, layer.featureCount)
        assertEquals(0f, layer.progressPercent)
        assertNull(layer.filePath)
        assertEquals("EPSG:4326", layer.crs)
    }

    @Test
    fun layer_withAllFields() {
        val layer = Layer(
            id = "test-id",
            name = "Test Layer",
            fileType = FileType.SHAPEFILE,
            geomType = "Polygon",
            isVisible = false,
            color = Color.Red,
            transparency = 0.5f,
            pointSize = 10f,
            lineWidth = 3f,
            featureCount = 100,
            progressPercent = 0.5f,
            filePath = "/data/test.shp",
            order = 5
        )
        assertEquals("test-id", layer.id)
        assertEquals("Test Layer", layer.name)
        assertEquals(FileType.SHAPEFILE, layer.fileType)
        assertEquals("Polygon", layer.geomType)
        assertFalse(layer.isVisible)
        assertEquals(Color.Red, layer.color)
        assertEquals(0.5f, layer.transparency)
        assertEquals(10f, layer.pointSize)
        assertEquals(3f, layer.lineWidth)
        assertEquals(100, layer.featureCount)
        assertEquals(0.5f, layer.progressPercent)
        assertEquals("/data/test.shp", layer.filePath)
        assertEquals(5, layer.order)
    }

    @Test
    fun layerExtent_defaultValue() {
        val layer = Layer(id = "1", name = "test", fileType = FileType.GEOJSON)
        assertEquals("", layer.extent)
    }

    @Test
    fun layer_copy() {
        val layer = Layer(id = "1", name = "test", fileType = FileType.KML, isVisible = true)
        val copied = layer.copy(isVisible = false, name = "modified")
        assertEquals("1", copied.id)
        assertEquals("modified", copied.name)
        assertFalse(copied.isVisible)
        assertTrue(layer.isVisible)
    }

    @Test
    fun fileType_labels() {
        assertEquals("Shapefile", FileType.SHAPEFILE.label)
        assertEquals("GeoJSON", FileType.GEOJSON.label)
        assertEquals("KML", FileType.KML.label)
        assertEquals("GPX", FileType.GPX.label)
        assertEquals("Photo", FileType.PHOTO.label)
        assertEquals("GeoPackage", FileType.GEOPACKAGE.label)
        assertEquals("GeoDatabase", FileType.GEODATABASE.label)
    }

    @Test
    fun fileType_values() {
        assertEquals(7, FileType.entries.size)
    }

    @Test
    fun featureRow_defaults() {
        val row = FeatureRow(id = "f1")
        assertEquals("f1", row.id)
        assertTrue(row.properties.isEmpty())
        assertNull(row.geometryType)
        assertNull(row.geometryCoordinates)
        assertEquals("EPSG:4326", row.crs)
    }

    @Test
    fun featureRow_withAllFields() {
        val props = mapOf("name" to "test", "height" to "100")
        val row = FeatureRow(
            id = "f2",
            properties = props,
            geometryType = "Point",
            geometryCoordinates = "[46.7, 24.7]",
            crs = "EPSG:3857"
        )
        assertEquals("f2", row.id)
        assertEquals(2, row.properties.size)
        assertEquals("Point", row.geometryType)
        assertEquals("[46.7, 24.7]", row.geometryCoordinates)
        assertEquals("EPSG:3857", row.crs)
    }

    @Test
    fun layerDetailInfo_defaults() {
        val info = LayerDetailInfo(
            fileName = "test.geojson",
            filePath = "/data/test.geojson",
            crs = "EPSG:4326",
            extent = "extent",
            features = emptyList()
        )
        assertEquals("test.geojson", info.fileName)
        assertEquals("/data/test.geojson", info.filePath)
        assertEquals("EPSG:4326", info.crs)
        assertEquals("extent", info.extent)
        assertTrue(info.features.isEmpty())
    }

    @Test
    fun layerStyle_defaults() {
        val style = LayerStyle()
        assertEquals(2f, style.strokeWidth)
        assertEquals(8f, style.pointSize)
        assertEquals(4f, style.lineWidth)
        assertEquals(0.3f, style.fillAlpha)
        assertNull(style.pointIcon)
        assertNull(style.labelField)
        assertEquals(12f, style.labelSize)
    }

    @Test
    fun layer_nodeDefaults() {
        val layer = Layer(id = "1", name = "test", fileType = FileType.GEOJSON)
        val node = LayerNode(layer = layer)
        assertEquals(layer, node.layer)
        assertTrue(node.children.isEmpty())
        assertFalse(node.isGroup)
    }

    @Test
    fun deriveGeomType_nullForEmpty() {
        assertNull(deriveGeomType(emptyList()))
    }

    @Test
    fun deriveGeomType_pointOnly() {
        val features = listOf(
            FeatureRow(id = "f1", geometryType = "Point"),
            FeatureRow(id = "f2", geometryType = "Point"),
            FeatureRow(id = "f3", geometryType = "MultiPoint")
        )
        assertEquals("Point", deriveGeomType(features))
    }

    @Test
    fun deriveGeomType_lineOnly() {
        val features = listOf(
            FeatureRow(id = "f1", geometryType = "LineString"),
            FeatureRow(id = "f2", geometryType = "MultiLineString")
        )
        assertEquals("Line", deriveGeomType(features))
    }

    @Test
    fun deriveGeomType_polygonOnly() {
        val features = listOf(
            FeatureRow(id = "f1", geometryType = "Polygon"),
            FeatureRow(id = "f2", geometryType = "MultiPolygon")
        )
        assertEquals("Polygon", deriveGeomType(features))
    }

    @Test
    fun deriveGeomType_mixedWithMajority() {
        val features = listOf(
            FeatureRow(id = "f1", geometryType = "Point"),
            FeatureRow(id = "f2", geometryType = "Point"),
            FeatureRow(id = "f3", geometryType = "Point"),
            FeatureRow(id = "f4", geometryType = "LineString"),
            FeatureRow(id = "f5", geometryType = "Polygon")
        )
        assertEquals("Point", deriveGeomType(features))
    }

    @Test
    fun deriveGeomType_unknownTypeReturnsNull() {
        val features = listOf(
            FeatureRow(id = "f1", geometryType = "UnknownType")
        )
        assertNull(deriveGeomType(features))
    }

    @Test
    fun deriveGeomType_mixedUnknownReturnsNull() {
        val features = listOf(
            FeatureRow(id = "f1", geometryType = "TypeA"),
            FeatureRow(id = "f2", geometryType = "TypeB")
        )
        assertNull(deriveGeomType(features))
    }
}
