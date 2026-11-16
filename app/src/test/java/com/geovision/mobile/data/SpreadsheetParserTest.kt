package com.geovision.mobile.data

import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader

class SpreadsheetParserTest {

    @Test
    fun testCsvInspectAndMapping() {
        val csvData = """
            PointID,Easting,Northing,Elevation,Description,Owner
            P1,500000.5,3200000.2,12.5,Survey Marker,Ministry
            P2,500100.0,3200150.8,14.2,Boundary Post,Private
            P3,500250.3,3200300.0,15.0,Control Point,Municipality
        """.trimIndent()

        // 1. Inspect
        val inspection = CsvParser.inspect(StringReader(csvData))
        assertEquals(6, inspection.headers.size)
        assertEquals(listOf("PointID", "Easting", "Northing", "Elevation", "Description", "Owner"), inspection.headers)
        assertEquals(3, inspection.previewRows.size)
        assertEquals("500000.5", inspection.previewRows[0]["Easting"])

        // Auto-suggestions
        assertEquals(1, inspection.suggestedXIndex) // Easting
        assertEquals(2, inspection.suggestedYIndex) // Northing
        assertEquals(3, inspection.suggestedZIndex) // Elevation
        assertEquals(0, inspection.suggestedLabelIndex) // PointID

        // 2. Parse with Mapping
        val mapping = ExcelParser.CoordinateMapping(
            xColumnIndex = 1, // Easting
            yColumnIndex = 2, // Northing
            zColumnIndex = 3, // Elevation
            labelColumnIndex = 0, // PointID
            epsgCode = 32636, // UTM Zone 36N
            utmZone = 36,
            isUtmNorth = true
        )

        val result = CsvParser.parseWithMapping(StringReader(csvData), "survey_points.csv", mapping)
        assertNull(result.error)
        assertEquals(3, result.features.size)

        // Verify First Point
        val f1 = result.features[0]
        assertEquals("P1", f1.id)
        assertEquals("Point", f1.geometryType)

        // All columns must be retained as properties!
        assertEquals("P1", f1.properties["PointID"])
        assertEquals("500000.5", f1.properties["Easting"])
        assertEquals("3200000.2", f1.properties["Northing"])
        assertEquals("12.5", f1.properties["Elevation"])
        assertEquals("Survey Marker", f1.properties["Description"])
        assertEquals("Ministry", f1.properties["Owner"])

        // Coordinates must be converted to valid WGS84
        assertTrue(f1.geometryCoordinates!!.startsWith("["))
        assertTrue(result.extent.isNotEmpty())
    }

    @Test
    fun testCsvArabicHeaders() {
        val csvData = """
            الاسم,الاحداثي_السيني,الاحداثي_الصادي,المنسوب,الملاحظات
            نقطة_1,31.235,30.044,25.0,القاهرة
            نقطة_2,29.976,31.131,18.5,الجيزة
        """.trimIndent()

        val inspection = CsvParser.inspect(StringReader(csvData))
        assertEquals(1, inspection.suggestedXIndex) // الاحداثي_السيني
        assertEquals(2, inspection.suggestedYIndex) // الاحداثي_الصادي
        assertEquals(3, inspection.suggestedZIndex) // المنسوب
        assertEquals(0, inspection.suggestedLabelIndex) // الاسم

        val mapping = ExcelParser.CoordinateMapping(
            xColumnIndex = 1,
            yColumnIndex = 2,
            zColumnIndex = 3,
            labelColumnIndex = 0,
            epsgCode = 4326
        )

        val result = CsvParser.parseWithMapping(StringReader(csvData), "arabic_coords.csv", mapping)
        assertNull(result.error)
        assertEquals(2, result.features.size)

        val f1 = result.features[0]
        assertEquals("نقطة_1", f1.id)
        assertEquals("القاهرة", f1.properties["الملاحظات"])
        assertEquals("25.0", f1.properties["المنسوب"])
        assertEquals("[31.235, 30.044, 25.0]", f1.geometryCoordinates)
    }
}
