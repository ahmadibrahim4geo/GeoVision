package com.geovision.mobile.data

import org.json.JSONArray
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WkbParser {

    private const val GP_MAGIC_0: Byte = 0x47
    private const val GP_MAGIC_1: Byte = 0x50

    fun parse(wkb: ByteArray): Triple<String, String, String>? {
        if (wkb.size < 5) return null
        var offset = 0
        val buf = ByteBuffer.wrap(wkb)

        // GeoPackage extended WKB: skip "GP" header + envelope if present
        if (wkb.size >= 4 && wkb[0] == GP_MAGIC_0 && wkb[1] == GP_MAGIC_1) {
            val flags = wkb[3].toInt() and 0xFF
            val envType = flags and 0x07
            val headerLen = 4 + when (envType) {
                0 -> 0
                1 -> 4 * 8  // 2D envelope: minX,maxX,minY,maxY
                2 -> 6 * 8  // 3D envelope: + minZ,maxZ
                3 -> 8 * 8  // 4D envelope: + minM,maxM
                4 -> 8 * 8  // 2D+envelope
                else -> 0
            }
            offset = headerLen
        }

        if (wkb.size - offset < 5) return null
        buf.position(offset)
        return parseGeometry(buf)
    }

    private fun parseGeometry(buf: ByteBuffer): Triple<String, String, String>? {
        if (buf.remaining() < 5) return null
        val bo = if (buf.get() == 0.toByte()) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        buf.order(bo)
        val raw = buf.getInt()
        val hasZ = (raw and 0x20000) != 0
        val geomType = raw and 0xFF
        return when (geomType) {
            1 -> { val (x, y) = readPoint(buf, hasZ); Triple("Point", "[$x,$y]", "") }
            2 -> parseLineString(buf, hasZ)
            3 -> parsePolygon(buf, hasZ)
            4 -> parseMulti(buf, hasZ) { parseGeometry(it) }
            5 -> parseMulti(buf, hasZ) { parseGeometry(it) }
            6 -> parseMulti(buf, hasZ) { parseGeometry(it) }
            else -> null
        }
    }

    private fun readPoint(buf: ByteBuffer, hasZ: Boolean): Pair<Double, Double> {
        val x = buf.getDouble(); val y = buf.getDouble()
        if (hasZ) buf.getDouble()
        return x to y
    }

    private fun parseLineString(buf: ByteBuffer, hasZ: Boolean): Triple<String, String, String>? {
        val n = buf.getInt(); val arr = JSONArray()
        repeat(n) { val (x, y) = readPoint(buf, hasZ); arr.put(JSONArray(doubleArrayOf(x, y))) }
        return Triple("LineString", arr.toString(), "")
    }

    private fun parsePolygon(buf: ByteBuffer, hasZ: Boolean): Triple<String, String, String>? {
        val nRings = buf.getInt(); val rings = JSONArray()
        repeat(nRings) {
            val nPts = buf.getInt(); val ring = JSONArray()
            repeat(nPts) { val (x, y) = readPoint(buf, hasZ); ring.put(JSONArray(doubleArrayOf(x, y))) }
            rings.put(ring)
        }
        return Triple("Polygon", rings.toString(), "")
    }

    private fun parseMulti(buf: ByteBuffer, hasZ: Boolean, parser: (ByteBuffer) -> Triple<String, String, String>?): Triple<String, String, String>? {
        val n = buf.getInt(); val items = JSONArray()
        var subType = ""
        repeat(n) {
            val sub = parser(buf)
            if (sub != null) {
                subType = sub.first
                items.put(JSONArray(sub.second))
            }
        }
        val actualType = when (subType) {
            "LineString" -> "MultiLineString"
            "Polygon" -> "MultiPolygon"
            else -> "MultiPoint"
        }
        return Triple(actualType, items.toString(), "")
    }
}
