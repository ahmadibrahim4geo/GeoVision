package com.geovision.mobile.data

import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Method

class ExternalTrainingDataValidationTest {
    private val trainingRoot = File("C:/Users/ahmad/Desktop/DATA TEST")

    @Test
    fun worldCountriesGeoJson_matchesTrainingBaseline() {
        val file = File(trainingRoot, "World_Countries.json")
        assumeTrue("Training GeoJSON not available", file.exists())

        val result = GeoJsonParser.parse(file.readText(Charsets.UTF_8), file.absolutePath)

        assertNull(result.error, result.error)
        assertEquals(322, result.features.size)
        assertEquals(setOf("Polygon", "MultiPolygon"), result.features.map { it.geometryType }.toSet())
        assertTrue(result.features.first().properties.keys.containsAll(listOf("OBJECTID", "CNTRY_NAME", "Shape_Area")))
    }

    @Test
    fun kmlDoc_matchesTrainingBaseline() {
        val file = File(trainingRoot, "KML_TEST_EXTRACTED/doc.kml")
        assumeTrue("Training KML not available", file.exists())

        val result = KmlParser.parse(file.readText(Charsets.UTF_8), file.name)

        assertNull(result.error, result.error)
        assertEquals(1475, result.features.size)
        assertTrue(result.features.any { it.geometryType == "Point" })
        assertTrue(result.features.any { it.geometryType == "LineString" })
        assertTrue(result.features.any { it.geometryType == "Polygon" })
        assertEquals(result.features.size, result.features.map { it.id }.toSet().size)
    }

    @Test
    fun canalShapefile_preservesWideDbfTrainingSchema() = runTest {
        val file = File(trainingRoot, "Egy shp/CANAL.SHP")
        assumeTrue("Training Shapefile not available", file.exists())

        val result = FileInputStream(file).use { ShapefileParser.streamParse(it, file.absolutePath) }

        assertNull(result.error, result.error)
        assertEquals(40, result.features.size)
        assertEquals(setOf("LineString"), result.features.map { it.geometryType }.toSet())
        assertTrue(result.features.first().properties.keys.containsAll(listOf("Entity", "Handle", "Layer", "Color", "Linetype", "Elevation", "Thickness", "Text")))
    }

    @Test
    fun fileGdb_listsTrainingFeatureClassTablesAndParsesGeometry() {
        val gdbDir = File(trainingRoot, "GDP/GDP TEST.gdb")
        assumeTrue("Training FileGDB not available", gdbDir.exists())

        val tableFiles = invokePrivate<List<File>>("getTableFiles", arrayOf(File::class.java), gdbDir)
        val estimatedCounts = tableFiles.associate { file ->
            file.name to invokePrivate<Int>("estimateFeatureCount", arrayOf(File::class.java), file)
        }

        assertEquals(
            listOf(
                "a00000009.gdbtable",
                "a00000013.gdbtable",
                "a00000018.gdbtable",
                "a0000001d.gdbtable",
                "a00000022.gdbtable",
                "a00000027.gdbtable",
                "a00000028.gdbtable",
                "a00000029.gdbtable",
                "a0000002a.gdbtable",
                "a0000002b.gdbtable",
                "a0000002c.gdbtable"
            ),
            tableFiles.map { it.name }
        )
        assertEquals(2258, estimatedCounts.values.sum())

        val tableInfos = invokePrivate<List<GdbParser.GdbTableInfo>>(
            "buildTableInfos",
            arrayOf(File::class.java, List::class.java),
            gdbDir,
            tableFiles
        )
        assertEquals(
            listOf(
                "Storm_Drainage_Manholes",
                "Storm_Drainage_Pipes",
                "PRD",
                "CatchBasin",
                "Channel",
                "Side_walk",
                "Roads02",
                "Roads01",
                "Passages",
                "Green_Areas",
                "Buildings"
            ),
            tableInfos.map { it.tableName }
        )
        assertTrue(tableInfos.none { it.tableName.contains(".gdbtable", ignoreCase = true) })
        assertEquals(tableFiles.map { it.name }, tableInfos.map { it.fileName })
        assertEquals(2258, tableInfos.sumOf { it.estimatedCount })

        val parsedFeatures = tableFiles.flatMap { file ->
            invokePrivate<List<com.geovision.mobile.ui.screens.layers.FeatureRow>>(
                "readGdbTable",
                arrayOf(File::class.java),
                file
            )
        }
        assertEquals(2258, parsedFeatures.size)
        assertTrue(parsedFeatures.any { it.geometryCoordinates?.isNotBlank() == true })

        val parsedLine = invokePrivateParsedTable(File(gdbDir, "a00000013.gdbtable"), "LineString")
        val parsedPolygon = invokePrivateParsedTable(File(gdbDir, "a00000018.gdbtable"), "Polygon")
        assertTrue(parsedLine.any { it.geometryType == "LineString" && it.geometryCoordinates?.startsWith("[[") == true })
        assertTrue(parsedPolygon.any { it.geometryType == "Polygon" && it.geometryCoordinates?.startsWith("[[") == true })
        assertTrue(parsedLine.any { lineDistinctPointCount(it.geometryCoordinates) >= 2 })
        assertTrue(parsedPolygon.any { polygonDistinctPointCount(it.geometryCoordinates) >= 3 })
        assertTrue(parsedLine.first().properties.keys.any { it == "X_Start" || it.startsWith("gdb_value_") })
    }

    private fun lineDistinctPointCount(coordinates: String?): Int {
        if (coordinates.isNullOrBlank()) return 0
        val arr = JSONArray(coordinates)
        return (0 until arr.length())
            .map { arr.getJSONArray(it).toString() }
            .toSet()
            .size
    }

    private fun polygonDistinctPointCount(coordinates: String?): Int {
        if (coordinates.isNullOrBlank()) return 0
        val rings = JSONArray(coordinates)
        if (rings.length() == 0) return 0
        val ring = rings.getJSONArray(0)
        return (0 until ring.length())
            .map { ring.getJSONArray(it).toString() }
            .toSet()
            .size
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> invokePrivate(name: String, parameterTypes: Array<Class<*>>, vararg args: Any): T {
        val method: Method = GdbParser::class.java.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        return method.invoke(GdbParser, *args) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun invokePrivateParsedTable(file: File, expectedGeometryType: String): List<com.geovision.mobile.ui.screens.layers.FeatureRow> {
        val method = GdbParser::class.java.getDeclaredMethod("readGdbTableParsed", File::class.java, String::class.java)
        method.isAccessible = true
        val parsed = method.invoke(GdbParser, file, expectedGeometryType)
        val features = parsed.javaClass.getDeclaredField("features")
        features.isAccessible = true
        return features.get(parsed) as List<com.geovision.mobile.ui.screens.layers.FeatureRow>
    }
}
