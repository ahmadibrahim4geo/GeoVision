package com.geovision.mobile.data

import org.junit.Assert.*
import org.junit.Test

class LayerRepositoryTest {

    @Test
    fun inferFileType_shapefile_shp() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE, LayerRepository.inferFileTypeFromPath("file:///data/test.shp"))
    }

    @Test
    fun inferFileType_shapefile_zip() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE, LayerRepository.inferFileTypeFromPath("file:///data/test.zip"))
    }

    @Test
    fun inferFileType_shapefile_shx() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE, LayerRepository.inferFileTypeFromPath("file:///data/test.shx"))
    }

    @Test
    fun inferFileType_shapefile_dbf() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE, LayerRepository.inferFileTypeFromPath("file:///data/test.dbf"))
    }

    @Test
    fun inferFileType_shapefile_prj() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE, LayerRepository.inferFileTypeFromPath("file:///data/test.prj"))
    }

    @Test
    fun inferFileType_geojson() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.GEOJSON, LayerRepository.inferFileTypeFromPath("file:///data/test.geojson"))
    }

    @Test
    fun inferFileType_geojson_json() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.GEOJSON, LayerRepository.inferFileTypeFromPath("file:///data/test.json"))
    }

    @Test
    fun inferFileType_kml() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.KML, LayerRepository.inferFileTypeFromPath("file:///data/test.kml"))
    }

    @Test
    fun inferFileType_kmz() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.KML, LayerRepository.inferFileTypeFromPath("file:///data/test.kmz"))
    }

    @Test
    fun inferFileType_gpx() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.GPX, LayerRepository.inferFileTypeFromPath("file:///data/test.gpx"))
    }

    @Test
    fun inferFileType_gpkg() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.GEOPACKAGE, LayerRepository.inferFileTypeFromPath("file:///data/test.gpkg"))
    }

    @Test
    fun inferFileType_photo_jpg() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.PHOTO, LayerRepository.inferFileTypeFromPath("file:///data/photo.jpg"))
    }

    @Test
    fun inferFileType_photo_jpeg() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.PHOTO, LayerRepository.inferFileTypeFromPath("file:///data/photo.jpeg"))
    }

    @Test
    fun inferFileType_photo_png() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.PHOTO, LayerRepository.inferFileTypeFromPath("file:///data/photo.png"))
    }

    @Test
    fun inferFileType_photo_webp() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.PHOTO, LayerRepository.inferFileTypeFromPath("file:///data/photo.webp"))
    }

    @Test
    fun inferFileType_unknown_extension() {
        assertNull(LayerRepository.inferFileTypeFromPath("file:///data/file.xyz"))
    }

    @Test
    fun inferFileType_no_extension() {
        assertNull(LayerRepository.inferFileTypeFromPath("file:///data/file"))
    }

    @Test
    fun inferFileType_http_uri() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.GEOJSON, LayerRepository.inferFileTypeFromPath("https://example.com/data/layer.geojson"))
    }

    @Test
    fun inferFileType_content_uri_noContext() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.KML, LayerRepository.inferFileTypeFromPath("content://com.example/data/test.kml"))
    }

    @Test
    fun inferFileType_withMime() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.KML, LayerRepository.inferFileTypeFromPath("file:///data/test.bin", mime = "application/vnd.google-earth.kml+xml"))
    }

    @Test
    fun inferFileType_noExtWithMimeGeoJson() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.GEOJSON, LayerRepository.inferFileTypeFromPath("file:///data/test.bin", mime = "application/geo+json"))
    }

    @Test
    fun inferFileType_noExtWithMimeShapefile() {
        assertEquals(com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE, LayerRepository.inferFileTypeFromPath("file:///data/test.bin", mime = "application/x-esri-shape"))
    }

    @Test
    fun loadResult_defaults() {
        val result = LayerRepository.LoadResult()
        assertTrue(result.features.isEmpty())
        assertNull(result.crs)
        assertNull(result.extent)
        assertEquals("", result.fileName)
        assertEquals(0L, result.parseTimeMs)
        assertNull(result.error)
        assertNull(result.errorType)
        assertNull(result.warning)
    }

    @Test
    fun loadResult_withValues() {
        val result = LayerRepository.LoadResult(
            features = emptyList(),
            crs = "EPSG:4326",
            extent = "extent",
            fileName = "test.geojson",
            parseTimeMs = 100L,
            error = "err",
            errorType = GeoJsonParser.ParseErrorType.CORRUPTED,
            warning = "warn"
        )
        assertEquals("EPSG:4326", result.crs)
        assertEquals("test.geojson", result.fileName)
        assertEquals(100L, result.parseTimeMs)
        assertEquals("err", result.error)
        assertEquals(GeoJsonParser.ParseErrorType.CORRUPTED, result.errorType)
        assertEquals("warn", result.warning)
    }
}
