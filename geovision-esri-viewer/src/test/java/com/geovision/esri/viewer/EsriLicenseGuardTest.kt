package com.geovision.esri.viewer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsriLicenseGuardTest {
    @Test
    fun guard_allowsOnlyReadOnlyLocalViewerByDefault() {
        EsriLicenseGuard.configure()

        assertTrue(EsriLicenseGuard.requireAllowed(EsriLicenseGuard.Capability.READ_ONLY_LOCAL_VIEW))
        assertFalse(EsriLicenseGuard.requireAllowed(EsriLicenseGuard.Capability.EDITING))
        assertFalse(EsriLicenseGuard.requireAllowed(EsriLicenseGuard.Capability.SYNC))
        assertFalse(EsriLicenseGuard.requireAllowed(EsriLicenseGuard.Capability.OFFLINE_GENERATE))
        assertFalse(EsriLicenseGuard.requireAllowed(EsriLicenseGuard.Capability.ONLINE_SERVICE))
    }

    @Test
    fun guard_detectsLikelyLicenseWarnings() {
        val warning = EsriLicenseGuard.licenseWarningFor("Layer requires Standard license")

        assertTrue(warning?.contains("ترخيص Esri أعلى من Lite") == true)
    }
}
