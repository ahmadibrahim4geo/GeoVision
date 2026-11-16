package com.geovision.mobile.data

import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader

class CsvParserTest {

    @Test
    fun testParseStandardCommaCsv() {
        val csv = """
            id,name,latitude,longitude,elevation
            1,Site Alpha,24.7136,46.6753,600
            2,Site Beta,24.7200,46.6800,610
        """.trimIndent()

        val result = CsvParser.streamParse(StringReader(csv), "sites.csv")
        assertNull(result.error)
        assertEquals(2, result.features.size)
        assertEquals("Point", result.features[0].geometryType)
        assertEquals("[46.6753, 24.7136, 600.0]", result.features[0].geometryCoordinates)
        assertEquals("Site Alpha", result.features[0].properties["name"])
    }

    @Test
    fun testParseSemicolonCsvWithXY() {
        val csv = """
            point_id;desc;x;y
            p1;Station 1;31.2357;30.0444
            p2;Station 2;31.2400;30.0500
        """.trimIndent()

        val result = CsvParser.streamParse(StringReader(csv), "stations.csv")
        assertNull(result.error)
        assertEquals(2, result.features.size)
        assertEquals("Point", result.features[0].geometryType)
        assertEquals("[31.2357, 30.0444]", result.features[0].geometryCoordinates)
        assertEquals("Station 1", result.features[0].properties["desc"])
    }

    @Test
    fun testParseArabicHeaders() {
        val csv = """
            الاسم,خط_العرض,خط_الطول,الارتفاع
            نقطة أ,21.4858,39.1925,12
            نقطة ب,21.5000,39.2000,15
        """.trimIndent()

        val result = CsvParser.streamParse(StringReader(csv), "arabic_points.csv")
        assertNull(result.error)
        assertEquals(2, result.features.size)
        assertEquals("Point", result.features[0].geometryType)
        assertEquals("[39.1925, 21.4858, 12.0]", result.features[0].geometryCoordinates)
        assertEquals("نقطة أ", result.features[0].properties["الاسم"])
    }

    @Test
    fun testMissingCoordinateColumnsReturnsError() {
        val csv = """
            id,name,city,country
            1,Place,Cairo,Egypt
        """.trimIndent()

        val result = CsvParser.streamParse(StringReader(csv), "invalid.csv")
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("لم يتم العثور على أعمدة الإحداثيات"))
    }
}
