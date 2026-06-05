package com.geovision.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ShapefileTrainingDataTest {

    @Test
    fun parseDbfBytes_handlesTrainingSchemaWithWideRecords() {
        val fields = listOf(
            DbfField("Entity", 'C', 16, "Polyline"),
            DbfField("Handle", 'C', 16, "1A"),
            DbfField("Layer", 'C', 254, "CANAL"),
            DbfField("Color", 'N', 9, "7"),
            DbfField("Linetype", 'C', 254, "Continuous"),
            DbfField("Elevation", 'F', 19, "0.0"),
            DbfField("Thickness", 'F', 19, "0.0"),
            DbfField("Text", 'C', 254, "Training")
        )

        val records = ShapefileParser.parseDbfBytes(buildDbf(fields), "UTF-8")

        assertEquals(1, records.size)
        val row = records.single()
        assertEquals(fields.map { it.name }, row.keys.toList())
        assertEquals("CANAL", row["Layer"])
        assertEquals("Training", row["Text"])
    }

    @Test
    fun parseWkt_trainingUtmZone36N_isNotMisclassifiedAsWebMercator() {
        val wkt = """PROJCS["WGS_1984_UTM_Zone_36N",GEOGCS["GCS_WGS_1984",DATUM["D_WGS_1984",SPHEROID["WGS_1984",6378137.0,298.2572235630016]],PRIMEM["Greenwich",0.0],UNIT["Degree",0.0174532925199433]],PROJECTION["Transverse_Mercator"],PARAMETER["false_easting",500000.0],PARAMETER["false_northing",0.0],PARAMETER["central_meridian",33.0],PARAMETER["scale_factor",0.9996],PARAMETER["latitude_of_origin",0.0],UNIT["Meter",1.0]]"""

        val info = CrsTransform.parseWkt(wkt)
        val (lon, lat) = CrsTransform.toWgs84(500165.0161757902, 3496460.6073725354, info)

        assertTrue(info.isUtm)
        assertEquals(36, info.utmZone)
        assertEquals(32636, info.epsg)
        assertFalse(info.epsg == 3857)
        assertEquals(33.0018, lon, 0.01)
        assertEquals(31.6000, lat, 0.01)
    }

    private data class DbfField(
        val name: String,
        val type: Char,
        val length: Int,
        val value: String
    )

    private fun buildDbf(fields: List<DbfField>): ByteArray {
        val headerLength = 32 + fields.size * 32 + 1
        val recordLength = 1 + fields.sumOf { it.length }
        val bytes = ByteArray(headerLength + recordLength + 1) { 0x20 }

        bytes[0] = 0x03
        ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(1)
        ByteBuffer.wrap(bytes, 8, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(headerLength.toShort())
        ByteBuffer.wrap(bytes, 10, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(recordLength.toShort())

        fields.forEachIndexed { index, field ->
            val pos = 32 + index * 32
            val nameBytes = field.name.toByteArray(Charsets.UTF_8)
            nameBytes.copyInto(bytes, pos, 0, nameBytes.size.coerceAtMost(11))
            bytes[pos + 11] = field.type.code.toByte()
            bytes[pos + 16] = field.length.toByte()
        }
        bytes[headerLength - 1] = 0x0D

        var recordPos = headerLength + 1
        fields.forEach { field ->
            val raw = field.value.padEnd(field.length).take(field.length).toByteArray(Charsets.UTF_8)
            raw.copyInto(bytes, recordPos)
            recordPos += field.length
        }
        bytes[headerLength + recordLength] = 0x1A
        return bytes
    }
}
