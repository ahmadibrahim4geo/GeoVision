package com.geovision.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportReportTest {

    @Test
    fun crsValidator_flagsCalculatedWgs84ForReview() {
        val crs = CrsValidator.inspect("WGS 84 (محسوب)")

        assertFalse(crs.isKnown)
        assertTrue(crs.requiresUserSelection)
        assertEquals("EPSG", crs.authority)
        assertEquals("4326", crs.code)
        assertNotNull(CrsValidator.warningFor("WGS 84 (محسوب)"))
    }

    @Test
    fun crsValidator_acceptsExplicitEpsg() {
        val crs = CrsValidator.inspect("EPSG:4326 (KML default)")

        assertTrue(crs.isKnown)
        assertFalse(crs.requiresUserSelection)
        assertEquals("EPSG", crs.authority)
        assertEquals("4326", crs.code)
    }
}

