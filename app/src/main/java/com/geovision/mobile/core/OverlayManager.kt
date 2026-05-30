package com.geovision.mobile.core

import com.geovision.mobile.core.AppLogger
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * ────────────────────────────────────────────────────────────────────────────────
 * OverlayManager - إدارة lifecycle الـ overlays على الخريطة
 * ────────────────────────────────────────────────────────────────────────────────
 *
 * المشكلة الأصلية:
 * - كانت overlays تتراكم بدون إزالة
 * - تسبب memory leaks و slowing down عند التفاعل مع الخريطة
 *
 * الحل:
 * - تتبع المحاضرة الحالية
 * - حد أقصى لعدد overlays
 * - تنظيف منتظم
 *
 * الاستخدام:
 * ```
 * val manager = OverlayManager(mapView)
 * manager.addOverlay(myOverlay, "measurement")
 * manager.removeOverlay("measurement")
 * manager.clearAll()
 * ```
 */
class OverlayManager(private val mapView: MapView?) {

    // تتبع الـ overlays حسب الفئة
    private val overlayMap = mutableMapOf<String, MutableList<Overlay>>()
    private var totalOverlays = 0
    private val MAX_OVERLAYS_PER_CATEGORY = 20
    private val MAX_TOTAL_OVERLAYS = 100

    /**
     * إضافة overlay جديد مع تتبعه
     */
    fun addOverlay(overlay: Overlay, category: String = "default"): Boolean {
        return try {
            // التحقق من الحد الأقصى
            if (totalOverlays >= MAX_TOTAL_OVERLAYS) {
                AppLogger.w(
                    AppLogger.Tags.MAP,
                    "⚠️ Max overlays reached ($MAX_TOTAL_OVERLAYS). Removing oldest..."
                )
                removeOldest()
            }

            // إضافة إلى الـ map
            val list = overlayMap.getOrPut(category) { mutableListOf() }
            if (list.size >= MAX_OVERLAYS_PER_CATEGORY) {
                AppLogger.w(
                    AppLogger.Tags.MAP,
                    "⚠️ Category '$category' has max overlays. Removing oldest..."
                )
                list.removeAt(0)
                mapView?.overlays?.remove(list[0])
                totalOverlays--
            }

            list.add(overlay)
            mapView?.overlays?.add(overlay)
            totalOverlays++

            AppLogger.i(
                AppLogger.Tags.MAP,
                "✓ Overlay added to category '$category' (total: $totalOverlays)"
            )
            true
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.MAP, "Error adding overlay", e)
            false
        }
    }

    /**
     * إزالة overlay من فئة معينة
     */
    fun removeOverlay(category: String, index: Int = 0): Boolean {
        return try {
            val list = overlayMap[category] ?: return false
            if (index >= list.size) return false

            val overlay = list[index]
            mapView?.overlays?.remove(overlay)
            list.removeAt(index)
            totalOverlays--

            if (list.isEmpty()) {
                overlayMap.remove(category)
            }

            AppLogger.i(AppLogger.Tags.MAP, "✓ Overlay removed from category '$category'")
            true
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.MAP, "Error removing overlay", e)
            false
        }
    }

    /**
     * إزالة جميع overlays من فئة معينة
     */
    fun removeCategory(category: String): Boolean {
        return try {
            val list = overlayMap[category] ?: return false
            list.forEach { overlay ->
                try {
                    mapView?.overlays?.remove(overlay)
                } catch (e: Exception) {
                    AppLogger.e(AppLogger.Tags.MAP, "Error removing individual overlay", e)
                }
            }
            totalOverlays -= list.size
            overlayMap.remove(category)

            AppLogger.i(AppLogger.Tags.MAP, "✓ Category '$category' cleared (${list.size} overlays)")
            true
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.MAP, "Error removing category", e)
            false
        }
    }

    /**
     * إزالة أقدم overlay (الأول)
     */
    private fun removeOldest() {
        try {
            for ((category, list) in overlayMap) {
                if (list.isNotEmpty()) {
                    val overlay = list.removeAt(0)
                    mapView?.overlays?.remove(overlay)
                    totalOverlays--
                    AppLogger.i(
                        AppLogger.Tags.MAP,
                        "✓ Oldest overlay removed from category '$category'"
                    )
                    return
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.MAP, "Error removing oldest overlay", e)
        }
    }

    /**
     * تنظيف كامل جميع overlays
     */
    fun clearAll() {
        try {
            mapView?.overlays?.let { overlays ->
                // احفظ base layers (قد تكون عادية)
                val baseSize = if (mapView.overlayManager.size > 2) 2 else 1

                while (overlays.size > baseSize) {
                    try {
                        overlays.removeAt(baseSize)
                    } catch (e: Exception) {
                        AppLogger.e(AppLogger.Tags.MAP, "Error removing overlay during clearAll", e)
                        break
                    }
                }
            }

            overlayMap.clear()
            totalOverlays = 0

            AppLogger.i(AppLogger.Tags.MAP, "✓ All overlays cleared")
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.MAP, "Error clearing all overlays", e)
        }
    }

    /**
     * الحصول على عدد overlays الكلي
     */
    fun getTotalCount(): Int = totalOverlays

    /**
     * الحصول على عدد overlays في فئة معينة
     */
    fun getCategoryCount(category: String): Int = overlayMap[category]?.size ?: 0

    /**
     * الحصول على معلومات عن الـ overlays الحالية
     */
    fun getInfo(): String {
        val details = overlayMap.entries.joinToString(", ") { (cat, list) ->
            "$cat: ${list.size}"
        }
        return "Overlays: total=$totalOverlays [$details]"
    }

    /**
     * تنظيف دوري (استدعاء كل N ثانية)
     */
    fun periodicCleanup() {
        try {
            if (totalOverlays > MAX_TOTAL_OVERLAYS * 0.8) {
                AppLogger.d(AppLogger.Tags.MAP, "🧹 Periodic overlay cleanup triggered")
                removeOldest()
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.MAP, "Error in periodic cleanup", e)
        }
    }
}
