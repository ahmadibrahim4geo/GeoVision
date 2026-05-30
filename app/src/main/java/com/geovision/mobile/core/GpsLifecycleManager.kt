package com.geovision.mobile.core

import android.location.Location
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.geovision.mobile.core.AppLogger
import org.osmdroid.views.MapView

/**
 * ────────────────────────────────────────────────────────────────────────────────
 * GpsLifecycleManager - إدارة دورة حياة خدمات الـ GPS
 * ────────────────────────────────────────────────────────────────────────────────
 *
 * يدير:
 * 1. بدء/إيقاف GPS providers بناءً على lifecycle
 * 2. تنظيف resources عند pause
 * 3. منع battery drain من GPS مستمر
 * 4. التعامل مع الأخطاء بشكل آمن
 *
 * الاستخدام:
 * ```
 * val manager = GpsLifecycleManager(mapView, lifecycle)
 * // يدير GPS تلقائياً عند pause/resume
 * ```
 */
class GpsLifecycleManager(
    private val mapView: MapView?,
    lifecycle: Lifecycle
) : DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    /**
     * عند شروع النشاط (الشاشة مرئية)
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        AppLogger.d(AppLogger.Tags.GPS, "▶ GPS: Starting location providers")
        startLocationProviders()
    }

    /**
     * عند إيقاف النشاط (الشاشة غير مرئية)
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        AppLogger.d(AppLogger.Tags.GPS, "⏹ GPS: Stopping location providers")
        stopLocationProviders()
    }

    /**
     * عند تدمير النشاط
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        AppLogger.d(AppLogger.Tags.GPS, "❌ GPS: Destroying lifecycle observer")
        cleanup()
    }

    /**
     * بدء جميع location providers
     */
    private fun startLocationProviders() {
        try {
            mapView?.overlayManager?.let { overlayManager ->
                overlayManager.forEach { overlay ->
                    if (overlay is com.geovision.mobile.data.GpsMyLocationProvider) {
                        // بدء GPS إذا كان موجوداً
                        try {
                            overlay.startLocationUpdates()
                            AppLogger.i(AppLogger.Tags.GPS, "✓ GPS provider started")
                        } catch (e: Exception) {
                            AppLogger.e(AppLogger.Tags.GPS, "Failed to start GPS", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.GPS, "Error in startLocationProviders", e)
        }
    }

    /**
     * إيقاف جميع location providers (توفير البطارية)
     */
    private fun stopLocationProviders() {
        try {
            mapView?.overlayManager?.let { overlayManager ->
                overlayManager.forEach { overlay ->
                    if (overlay is com.geovision.mobile.data.GpsMyLocationProvider) {
                        // إيقاف GPS
                        try {
                            overlay.stopLocationUpdates()
                            AppLogger.i(AppLogger.Tags.GPS, "✓ GPS provider stopped")
                        } catch (e: Exception) {
                            AppLogger.e(AppLogger.Tags.GPS, "Failed to stop GPS", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.GPS, "Error in stopLocationProviders", e)
        }
    }

    /**
     * تنظيف كامل الـ resources
     */
    private fun cleanup() {
        try {
            stopLocationProviders()
            AppLogger.i(AppLogger.Tags.GPS, "GPS resources cleaned up")
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.GPS, "Error during cleanup", e)
        }
    }

    /**
     * الحصول على آخر موقع معروف
     */
    fun getLastKnownLocation(): Location? {
        return try {
            mapView?.overlayManager?.forEach { overlay ->
                if (overlay is com.geovision.mobile.data.GpsMyLocationProvider) {
                    return overlay.lastFix()
                }
            }
            null
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.GPS, "Error getting last known location", e)
            null
        }
    }

    /**
     * التحقق من حالة GPS
     */
    fun isGpsEnabled(): Boolean {
        return try {
            mapView?.overlayManager?.any { overlay ->
                overlay is com.geovision.mobile.data.GpsMyLocationProvider &&
                overlay.isLocationEnabled
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.GPS, "Error checking GPS status", e)
            false
        }
    }
}
