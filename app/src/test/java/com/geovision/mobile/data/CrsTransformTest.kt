package com.geovision.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrsTransformTest {

    @Test
    fun parseWkt_epsg4326() {
        val wkt = """GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["degree",0.01745329251994328],AUTHORITY["EPSG","4326"]]"""
        val info = CrsTransform.parseWkt(wkt)
        assertEquals(4326, info.epsg?.toInt())
        assertTrue(info.isWgs84)
    }

    @Test
    fun parseWkt_epsg3857() {
        val wkt = """PROJCS["WGS 84 / Pseudo-Mercator",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["degree",0.01745329251994328]],PROJECTION["Mercator_1SP"],UNIT["metre",1],AXIS["X",EAST],AXIS["Y",NORTH],AUTHORITY["EPSG","3857"]]"""
        val info = CrsTransform.parseWkt(wkt)
        assertEquals(3857, info.epsg?.toInt())
    }

    @Test
    fun parseWkt_epsg3857_withMercatorInName() {
        val wkt = """PROJCS["WGS 84 / Mercator",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["degree",0.01745329251994328]]]"""
        val info = CrsTransform.parseWkt(wkt)
        assertEquals(3857, info.epsg?.toInt())
    }

    @Test
    fun parseWkt_wgs84InName() {
        val wkt = """GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["degree",0.01745329251994328]]"""
        val info = CrsTransform.parseWkt(wkt)
        assertTrue(info.isWgs84)
        assertEquals(4326, info.epsg?.toInt())
    }

    @Test
    fun wgs84ToWgs84_noConversion() {
        val info = CrsTransform.CrsInfo(isWgs84 = true, epsg = 4326)
        val result = CrsTransform.toWgs84(46.7, 24.7, info)
        assertEquals(46.7, result.first, 0.001)
        assertEquals(24.7, result.second, 0.001)
    }

    @Test
    fun mercatorToWgs84_conversion() {
        val info = CrsTransform.CrsInfo(epsg = 3857)
        val result = CrsTransform.toWgs84(0.0, 0.0, info)
        assertEquals(0.0, result.first, 0.001)
        assertEquals(0.0, result.second, 0.001)
    }

    @Test
    fun mercatorToWgs84_knownPoint() {
        val info = CrsTransform.CrsInfo(epsg = 3857)
        val result = CrsTransform.toWgs84(20037508.34, 0.0, info)
        assertEquals(180.0, result.first, 0.001)
        assertEquals(0.0, result.second, 0.001)
    }

    @Test
    fun unknownCrs_returnsIdentity() {
        val info = CrsTransform.CrsInfo(epsg = 99999)
        val result = CrsTransform.toWgs84(100.0, 50.0, info)
        assertEquals(100.0, result.first, 0.001)
        assertEquals(50.0, result.second, 0.001)
    }

    @Test
    fun parseWkt_withUtmName() {
        val wkt = """PROJCS["UTM ZONE 38N",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["degree",0.01745329251994328]]]"""
        val info = CrsTransform.parseWkt(wkt)
        assertTrue(info.isUtm)
        assertEquals(38, info.utmZone)
        assertTrue(info.utmNorth)
    }

    @Test
    fun parseWkt_withUtmSouth() {
        val wkt = """PROJCS["UTM ZONE 56S",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["degree",0.01745329251994328]]]"""
        val info = CrsTransform.parseWkt(wkt)
        assertTrue(info.isUtm)
        assertEquals(56, info.utmZone)
        assertFalse(info.utmNorth)
    }
}
