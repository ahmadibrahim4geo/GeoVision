package com.geovision.mobile.ui.screens.map

import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ScaleBarOverlay

class EnglishScaleBarOverlay(mapView: MapView) : ScaleBarOverlay(mapView) {
    override fun scaleBarLengthText(meters: Double): String {
        return super.scaleBarLengthText(meters).replace(Regex("[\\u0660-\\u0669]")) { c ->
            (c.value[0].code - 0x0660).toString()
        }
    }
}
