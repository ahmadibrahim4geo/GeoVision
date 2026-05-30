package com.geovision.mobile.core

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.geovision.mobile.core.AppLogger
import kotlin.math.roundToInt

/**
 * ────────────────────────────────────────────────────────────────────────────────
 * MemoryMonitor - مراقبة استخدام الذاكرة للكشف عن مشاكل
 * ────────────────────────────────────────────────────────────────────────────────
 *
 * يقيس:
 * 1. Memory usage الحالي
 * 2. Heap size و Free memory
 * 3. Native memory (بـ Debug API)
 * 4. Warning عند الاقتراب من الحدود
 *
 * الاستخدام:
 * ```
 * val monitor = MemoryMonitor(context)
 * monitor.log()  // طبع معلومات الذاكرة
 * monitor.checkMemoryHealth()  // التحقق من الصحة
 * ```
 */
class MemoryMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val runtime = Runtime.getRuntime()

    /**
     * الحصول على معلومات الذاكرة الحالية
     */
    data class MemoryInfo(
        val usedMemory: Long,  // بالـ bytes
        val totalMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val percentageUsed: Float,
        val nativeHeap: Long,
        val isLowMemory: Boolean
    )

    /**
     * احصل على معلومات الذاكرة الحالية
     */
    fun getMemoryInfo(): MemoryInfo {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - freeMemory

        val nativeHeap = try {
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            memInfo.nativePss * 1024L
        } catch (e: Exception) {
            0L
        }

        val percentageUsed = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
        val isLowMemory = percentageUsed > 85f

        return MemoryInfo(
            usedMemory = usedMemory,
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            maxMemory = maxMemory,
            percentageUsed = percentageUsed,
            nativeHeap = nativeHeap,
            isLowMemory = isLowMemory
        )
    }

    /**
     * اطبع معلومات الذاكرة بصيغة آمنة
     */
    fun log() {
        if (!com.geovision.mobile.BuildConfig.DEBUG) return

        val info = getMemoryInfo()
        val message = buildString {
            append("🧠 Memory Status:\n")
            append("  Used: ${formatBytes(info.usedMemory)} / ${formatBytes(info.maxMemory)}\n")
            append("  Percentage: ${info.percentageUsed.roundToInt()}%\n")
            append("  Free: ${formatBytes(info.freeMemory)}\n")
            append("  Status: ${if (info.isLowMemory) "⚠️ LOW" else "✅ OK"}")
        }

        AppLogger.i(AppLogger.Tags.PERFORMANCE, message)
    }

    /**
     * التحقق من صحة الذاكرة وإصدار التحذيرات
     */
    fun checkMemoryHealth(): MemoryHealthStatus {
        val info = getMemoryInfo()

        return when {
            info.percentageUsed > 90f -> {
                AppLogger.e(
                    AppLogger.Tags.PERFORMANCE,
                    "🔴 CRITICAL: Memory usage ${info.percentageUsed.roundToInt()}% - App may crash soon!"
                )
                MemoryHealthStatus.CRITICAL
            }
            info.percentageUsed > 80f -> {
                AppLogger.w(
                    AppLogger.Tags.PERFORMANCE,
                    "🟠 WARNING: Memory usage ${info.percentageUsed.roundToInt()}% - Consider cleanup"
                )
                MemoryHealthStatus.WARNING
            }
            info.percentageUsed > 70f -> {
                AppLogger.d(
                    AppLogger.Tags.PERFORMANCE,
                    "🟡 INFO: Memory usage ${info.percentageUsed.roundToInt()}% - Monitor carefully"
                )
                MemoryHealthStatus.CAUTION
            }
            else -> {
                AppLogger.d(AppLogger.Tags.PERFORMANCE, "✅ Memory healthy (${info.percentageUsed.roundToInt()}%)")
                MemoryHealthStatus.HEALTHY
            }
        }
    }

    /**
     * احسب تسرب الذاكرة المحتمل بمقارنة الاستخدام الحالي مع المتوقع
     */
    fun estimateMemoryLeak(baselineMemory: Long): Long {
        val currentUsed = getMemoryInfo().usedMemory
        return if (currentUsed > baselineMemory) {
            currentUsed - baselineMemory
        } else {
            0L
        }
    }

    /**
     * اقتراح تنظيف إذا لزم الأمر
     */
    fun suggestCleanup(): Boolean {
        val info = getMemoryInfo()
        return if (info.percentageUsed > 75f) {
            AppLogger.w(
                AppLogger.Tags.PERFORMANCE,
                "💡 Suggesting cleanup: memory is ${info.percentageUsed.roundToInt()}% used"
            )
            true
        } else {
            false
        }
    }

    /**
     * الحصول على معلومات الذاكرة من النظام
     */
    fun getSystemMemoryInfo(): SystemMemoryInfo? {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val maxMemory = runtime.maxMemory()

            val actMemInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(actMemInfo)

            SystemMemoryInfo(
                appUsed = totalMemory - freeMemory,
                appMax = maxMemory,
                deviceAvailable = actMemInfo.availMem,
                deviceTotal = actMemInfo.totalMem,
                isLowMemory = actMemInfo.lowMemory
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PERFORMANCE, "Error getting system memory info", e)
            null
        }
    }

    /**
     * صيغة تنسيق الـ bytes إلى صيغة مقروءة
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "${(bytes / 1_000_000_000.0).roundToInt()}GB"
            bytes >= 1_000_000 -> "${(bytes / 1_000_000.0).roundToInt()}MB"
            bytes >= 1_000 -> "${(bytes / 1_000.0).roundToInt()}KB"
            else -> "$bytes B"
        }
    }

    /**
     * حالة الذاكرة
     */
    enum class MemoryHealthStatus {
        HEALTHY,      // < 70%
        CAUTION,      // 70-80%
        WARNING,      // 80-90%
        CRITICAL      // > 90%
    }

    /**
     * معلومات الذاكرة من النظام
     */
    data class SystemMemoryInfo(
        val appUsed: Long,
        val appMax: Long,
        val deviceAvailable: Long,
        val deviceTotal: Long,
        val isLowMemory: Boolean
    )
}
