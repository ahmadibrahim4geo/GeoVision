package com.geovision.mobile.ui.screens.map

import android.os.SystemClock
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.abs
import kotlin.math.sqrt

open class TapOverlay : Overlay() {
    private var downX = 0f; private var downY = 0f
    private var downTime = 0L; private var lastTapTime = 0L
    private var maxMovePx = 0f
    private var downCenterLat = 0.0; private var downCenterLon = 0.0
    private var slopPx = 0f
    private var tappedX = 0; private var tappedY = 0

    open fun onTap(p: GeoPoint) {}

    override fun onTouchEvent(e: MotionEvent?, mv: MapView?): Boolean {
        if (e == null || mv == null) return false
        if (slopPx == 0f) slopPx = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 8f, mv.context.resources.displayMetrics)

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x; downY = e.y; downTime = SystemClock.uptimeMillis(); maxMovePx = 0f
                downCenterLat = mv.mapCenter.latitude; downCenterLon = mv.mapCenter.longitude
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                maxMovePx = Float.MAX_VALUE; return super.onTouchEvent(e, mv)
            }
            MotionEvent.ACTION_MOVE -> {
                if (e.pointerCount > 1) { maxMovePx = Float.MAX_VALUE; return super.onTouchEvent(e, mv) }
                val d = sqrt((e.x - downX) * (e.x - downX) + (e.y - downY) * (e.y - downY))
                if (d > maxMovePx) maxMovePx = d
            }
            MotionEvent.ACTION_UP -> {
                if (e.pointerCount > 1) return false
                val now = SystemClock.uptimeMillis()
                if (maxMovePx > slopPx) return false
                if (now - downTime < 80) return false
                if (now - lastTapTime in 1L..350L) { lastTapTime = now; return false }
                val centerMoved = abs(mv.mapCenter.latitude - downCenterLat) > 1e-6 ||
                                  abs(mv.mapCenter.longitude - downCenterLon) > 1e-6
                if (centerMoved) return false
                lastTapTime = now; tappedX = e.x.toInt(); tappedY = e.y.toInt()
                val g = mv.projection.fromPixels(tappedX, tappedY)
                if (g is GeoPoint) { onTap(g); return true }
            }
        }
        return super.onTouchEvent(e, mv)
    }
}
