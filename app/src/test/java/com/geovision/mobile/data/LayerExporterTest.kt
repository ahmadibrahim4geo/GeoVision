package com.geovision.mobile.data

import com.geovision.mobile.ui.screens.layers.FeatureRow
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LayerExporterTest {

    private val sampleFeatures = listOf(
        FeatureRow(
            id = "1",
            properties = mapOf("Name" to "Tower A", "Height" to "45", "Type" to "Telecommunication"),
            geometryType = "Point",
            geometryCoordinates = "[31.2357,30.0444]"
        ),
        FeatureRow(
            id = "2",
            properties = mapOf("Name" to "Tower B", "Height" to "60", "Type" to "Power"),
            geometryType = "Point",
            geometryCoordinates = "[31.2500,30.0500,120.0]"
        )
    )

    @Test
    fun exportToGeoJson_producesValidFeatureCollection() {
        val json = LayerExporter.exportToGeoJson("Sample Layer", sampleFeatures)
        val obj = JSONObject(json)

        assertEquals("FeatureCollection", obj.getString("type"))
        assertEquals("Sample Layer", obj.getString("name"))

        val features = obj.getJSONArray("features")
        assertEquals(2, features.length())

        val f1 = features.getJSONObject(0)
        assertEquals("1", f1.getString("id"))
        assertEquals("Tower A", f1.getJSONObject("properties").getString("Name"))
        assertEquals("Point", f1.getJSONObject("geometry").getString("type"))
        val coords = f1.getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(31.2357, coords.getDouble(0), 0.0001)
        assertEquals(30.0444, coords.getDouble(1), 0.0001)
    }

    @Test
    fun exportToCsv_includesCoordinatesAndAllProperties() {
        val csv = LayerExporter.exportToCsv(sampleFeatures)
        val lines = csv.trim().split("\n")

        assertTrue(lines.size >= 3)
        val header = lines[0]
        assertTrue(header.contains("FID"))
        assertTrue(header.contains("X"))
        assertTrue(header.contains("Y"))
        assertTrue(header.contains("Z"))
        assertTrue(header.contains("Name"))
        assertTrue(header.contains("Height"))
        assertTrue(header.contains("Type"))

        val row1 = lines[1]
        assertTrue(row1.contains("Tower A"))
        assertTrue(row1.contains("31.2357"))
        assertTrue(row1.contains("30.0444"))

        val row2 = lines[2]
        assertTrue(row2.contains("Tower B"))
        assertTrue(row2.contains("120.0"))
    }

    @Test
    fun exportToKml_producesPlacemarksAndExtendedData() {
        val kml = LayerExporter.exportToKml("Sample KML", sampleFeatures)

        assertTrue(kml.contains("<kml"))
        assertTrue(kml.contains("<Document>"))
        assertTrue(kml.contains("<name>Sample KML</name>"))
        assertTrue(kml.contains("<Placemark>"))
        assertTrue(kml.contains("<name>Tower A</name>"))
        assertTrue(kml.contains("<Point><coordinates>31.2357,30.0444,0</coordinates></Point>"))
        assertTrue(kml.contains("<ExtendedData>"))
        assertTrue(kml.contains("""<Data name="Height">"""))
        assertTrue(kml.contains("<value>45</value>"))
    }

    @Test
    fun mobileGdbReader_parseEsriShapePointBuffer() {
        // Point: shapeType=1 (LE int), x (LE double), y (LE double)
        val bb = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(1) // Point
        bb.putDouble(31.2357)
        bb.putDouble(30.0444)

        val geom = MobileGdbReader.parseGeometryBytes(bb.array())
        assertNotNull(geom)
        assertEquals("Point", geom!!.first)
        assertEquals("[31.2357,30.0444]", geom.second)
    }

    @Test
    fun mobileGdbReader_parseEsriShapePolylineBuffer() {
        // Polyline: shapeType=3 (LE int), box (4 doubles = 32B), numParts=1 (int), numPoints=2 (int), parts=[0], points=[(1,2), (3,4)]
        val bb = ByteBuffer.allocate(4 + 32 + 4 + 4 + 4 + (2 * 16)).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(3) // Polyline
        // bbox
        bb.putDouble(1.0); bb.putDouble(2.0); bb.putDouble(3.0); bb.putDouble(4.0)
        bb.putInt(1) // numParts
        bb.putInt(2) // numPoints
        bb.putInt(0) // part 0 starts at index 0
        // points
        bb.putDouble(1.0); bb.putDouble(2.0)
        bb.putDouble(3.0); bb.putDouble(4.0)

        val geom = MobileGdbReader.parseGeometryBytes(bb.array())
        assertNotNull(geom)
        assertEquals("LineString", geom!!.first)
        assertEquals("[[1.0,2.0],[3.0,4.0]]", geom.second)
    }
}
