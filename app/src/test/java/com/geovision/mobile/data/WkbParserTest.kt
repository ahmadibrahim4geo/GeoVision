package com.geovision.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WkbParserTest {
    @Test
    fun parse_skipsGeoPackageGeometryHeaderIncludingSrsId() {
        val wkb = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(1)
            putInt(1)
            putDouble(31.25)
            putDouble(30.05)
        }.array()
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).apply {
            put('G'.code.toByte())
            put('P'.code.toByte())
            put(0)
            put(1)
            putInt(4326)
        }.array()

        val parsed = WkbParser.parse(header + wkb)

        assertNotNull(parsed)
        assertEquals("Point", parsed!!.first)
        assertEquals("[31.25,30.05]", parsed.second)
    }
}
