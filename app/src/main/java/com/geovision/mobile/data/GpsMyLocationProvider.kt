package com.geovision.mobile.data

import android.location.Location
import org.osmdroid.views.overlay.Overlay

class GpsMyLocationProvider : Overlay() {
    private var _isEnabled = false
    private var _lastFix: Location? = null

    fun startLocationUpdates() { _isEnabled = true }
    fun stopLocationUpdates() { _isEnabled = false }
    fun lastFix(): Location? = _lastFix
    val isLocationEnabled: Boolean get() = _isEnabled
}
