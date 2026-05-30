package com.geovision.mobile.ui.screens.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint

class MeasurementMathTest {

    @Test
    fun autoDistanceUsesMetersBelowOneKilometer() {
        assertEquals("999.0 m", fmtDist(999.0, "auto"))
    }

    @Test
    fun autoDistanceUsesKilometersAtOneKilometerAndAbove() {
        assertEquals("1.50 km", fmtDist(1500.0, "auto"))
    }

    @Test
    fun autoAreaUsesHectaresForMediumMetricAreas() {
        assertEquals("2.50 ha", fmtArea(25_000.0, "auto"))
    }

    @Test
    fun autoAreaUsesSquareKilometersForLargeMetricAreas() {
        assertEquals("2.50 km\u00B2", fmtArea(2_500_000.0, "auto"))
    }

    @Test
    fun appendingMeasurementPointRejectsAnchorsInsideMinimumDistance() {
        val start = GeoPoint(30.0, 31.0)
        val tooClose = GeoPoint(30.0, 31.000001)

        assertNull(appendMeasurementPoint(listOf(start), tooClose))
    }

    @Test
    fun appendingMeasurementPointKeepsAnchoredCoordinate() {
        val start = GeoPoint(30.0, 31.0)
        val anchor = GeoPoint(30.0, 31.001)
        val updated = appendMeasurementPoint(listOf(start), anchor)

        assertEquals(2, updated?.size)
        assertTrue(gcDist(anchor, updated!!.last()) < 0.001)
    }
}
