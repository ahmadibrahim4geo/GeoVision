package com.geovision.mobile.data

import com.geovision.mobile.ui.screens.map.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint

class MeasurementMathExtendedTest {

    @Test
    fun gcDist_zeroDistance() {
        val pt = GeoPoint(24.7, 46.7)
        assertEquals(0.0, gcDist(pt, pt), 0.001)
    }

    @Test
    fun gcDist_samePoint() {
        val pt = GeoPoint(24.7136, 46.6753)
        assertEquals(0.0, gcDist(pt, GeoPoint(pt.latitude, pt.longitude)), 0.001)
    }

    @Test
    fun gcDist_smallDistance() {
        val a = GeoPoint(24.7, 46.7)
        val b = GeoPoint(24.7001, 46.7001)
        val dist = gcDist(a, b)
        assertTrue("Distance should be positive", dist > 0)
        assertTrue("Distance should be small", dist < 50)
    }

    @Test
    fun gcArea_smallTriangle() {
        val pts = listOf(
            GeoPoint(24.7, 46.7),
            GeoPoint(24.71, 46.7),
            GeoPoint(24.7, 46.71)
        )
        val area = gcArea(pts)
        assertTrue("Area should be positive", area > 0)
        assertTrue("Area should be reasonable", area < 2_000_000)
    }

    @Test
    fun calcDistanceMeters_singlePoint() {
        assertEquals(0.0, calcDistanceMeters(listOf(GeoPoint(24.7, 46.7))), 0.001)
    }

    @Test
    fun calcDistanceMeters_twoPoints() {
        val a = GeoPoint(24.7, 46.7)
        val b = GeoPoint(24.71, 46.7)
        val dist = calcDistanceMeters(listOf(a, b))
        assertTrue("Distance should be ~1110m", dist > 1000)
        assertTrue("Distance should be ~1110m", dist < 1300)
    }

    @Test
    fun fmtDist_auto_under1000m() {
        assertEquals("999.0 m", fmtDist(999.0, "auto"))
    }

    @Test
    fun fmtDist_auto_over1000m() {
        assertEquals("1.50 km", fmtDist(1500.0, "auto"))
    }

    @Test
    fun fmtDist_meters() {
        assertEquals("500.0 m", fmtDist(500.0, "m"))
    }

    @Test
    fun fmtDist_kilometers() {
        assertEquals("5.00 km", fmtDist(5000.0, "km"))
    }

    @Test
    fun fmtDist_feet() {
        assertEquals("1640.4 ft", fmtDist(500.0, "ft"))
    }

    @Test
    fun fmtDist_miles() {
        assertEquals("0.31 mi", fmtDist(500.0, "mi"))
    }

    @Test
    fun fmtArea_auto_small() {
        assertEquals("5000 m\u00B2", fmtArea(5000.0, "auto"))
    }

    @Test
    fun fmtArea_auto_hectares() {
        assertEquals("2.50 ha", fmtArea(25_000.0, "auto"))
    }

    @Test
    fun fmtArea_auto_large() {
        assertEquals("2.50 km\u00B2", fmtArea(2_500_000.0, "auto"))
    }

    @Test
    fun fmtCoord_dd() {
        val (lat, lon) = fmtCoord(24.7136, 46.6753, "DD")
        assertTrue(lat.contains("N"))
        assertTrue(lon.contains("E"))
    }

    @Test
    fun fmtCoord_dms() {
        val (lat, lon) = fmtCoord(24.7136, 46.6753, "DMS")
        assertTrue(lat.contains("\u00B0"))
        assertTrue(lon.contains("E"))
    }

    @Test
    fun toUtm_northernHemisphere() {
        val result = toUtm(24.7, 46.7)
        assertEquals("N", result.hemisphere)
        assertTrue(result.easting > 600_000)
        assertTrue(result.easting < 700_000)
        assertTrue(result.northing > 2_700_000)
        assertTrue(result.northing < 2_800_000)
    }

    @Test
    fun calcCircleResult_positive() {
        val result = calcCircleResult(1000.0, "m", "m2")
        assertTrue(result.contains("R"))
        assertTrue(result.contains("1000.0 m"))
    }

    @Test
    fun calcAreaResult_twoPoints_returnsFormatted() {
        val pts = listOf(GeoPoint(24.7, 46.7), GeoPoint(24.71, 46.7))
        val result = calcAreaResult(pts, "m", "m2")
        assertTrue("Result should not be empty", result.isNotEmpty())
    }

    @Test
    fun ellipseBearing_north() {
        val center = GeoPoint(24.7, 46.7)
        val north = GeoPoint(24.71, 46.7)
        val bearing = ellipseBearing(center, north)
        assertTrue("North bearing should be near 0/360", bearing < 10.0 || bearing > 350.0)
    }

    @Test
    fun ellipseBearing_east() {
        val center = GeoPoint(24.7, 46.7)
        val east = GeoPoint(24.7, 46.71)
        val bearing = ellipseBearing(center, east)
        assertTrue("East bearing should be near 90", bearing in 80.0..100.0)
    }

    @Test
    fun appendMeasurementPoint_rejectsClosePoints() {
        val start = GeoPoint(30.0, 31.0)
        val tooClose = GeoPoint(30.0, 31.000001)
        assertNull(appendMeasurementPoint(listOf(start), tooClose))
    }

    @Test
    fun appendMeasurementPoint_acceptsFarPoints() {
        val start = GeoPoint(30.0, 31.0)
        val far = GeoPoint(30.0, 31.001)
        val updated = appendMeasurementPoint(listOf(start), far)
        assertEquals(2, updated?.size)
        assertTrue(gcDist(far, updated!!.last()) < 0.001)
    }

    @Test
    fun pointsToGeoJson_producesValidJson() {
        val pts = listOf(GeoPoint(24.7, 46.7), GeoPoint(24.71, 46.71))
        val json = pointsToGeoJson(pts, MeasureMode.DISTANCE, "1000 m")
        assertTrue(json.contains("FeatureCollection"))
        assertTrue(json.contains("LineString"))
        assertTrue(json.contains("1000 m"))
    }

    @Test
    fun fmtDD_northEast() {
        val result = fmtDD(24.7136, true)
        assertTrue(result.contains("N"))
    }

    @Test
    fun fmtDD_south() {
        val result = fmtDD(-33.8688, true)
        assertTrue(result.contains("S"))
    }

    @Test
    fun fmtDMS_positive() {
        val result = fmtDMS(24.7136, true)
        assertTrue(result.contains("\u00B0"))
        assertTrue(result.contains("N"))
    }

    @Test
    fun fmtDMS_negative() {
        val result = fmtDMS(-33.8688, true)
        assertTrue(result.contains("S"))
    }

    @Test
    fun unitConversions_distanceToArea() {
        assertEquals("m", distanceUnitForAreaUnit("m2"))
        assertEquals("km", distanceUnitForAreaUnit("km2"))
        assertEquals("m", distanceUnitForAreaUnit("ac"))
    }

    @Test
    fun unitConversions_areaToDistance() {
        assertEquals("m2", areaUnitForDistanceUnit("m"))
        assertEquals("km2", areaUnitForDistanceUnit("km"))
    }

    @Test
    fun labels_useSymbols() {
        MsrLabels.useSymbols = true
        assertEquals("\u2192", lbl("len"))
        assertEquals("\u25A0", lbl("area"))
        MsrLabels.useSymbols = false
        assertEquals("L", lbl("len"))
        assertEquals("A", lbl("area"))
    }
}
