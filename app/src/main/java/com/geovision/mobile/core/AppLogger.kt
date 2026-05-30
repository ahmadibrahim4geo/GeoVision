package com.geovision.mobile.core

import android.util.Log
import com.geovision.mobile.BuildConfig

/**
 * ────────────────────────────────────────────────────────────────────────────────
 * AppLogger - تسجيل آمن مع شروط البناء
 * ────────────────────────────────────────────────────────────────────────────────
 * 
 * يوفر طبقة تجريد للـ logging تسمح بـ:
 * 1. تقليل الـ logs في Release builds (إخفاء حساس information)
 * 2. الحفاظ على التتبع الكامل في Debug builds
 * 3. منع تسرب معلومات حساسة مثل مسارات الملفات والـ API keys
 * 4. إنشاء audit logs محمية للأمان
 * 
 * الاستخدام:
 * ```
 * AppLogger.d("TAG", "رسالة debug")  // ظهر فقط في debug
 * AppLogger.e("TAG", "error message", exception)  // ظهر دائماً
 * AppLogger.secureLog("sensitive data")  // بدون حساس info
 * ```
 */
object AppLogger {

    private const val APP_TAG = "GeoVision"

    /**
     * Debug log - يظهر فقط في DEBUG builds
     */
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$APP_TAG/$tag", message)
        }
    }

    /**
     * Info log - يظهر فقط في DEBUG builds
     */
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i("$APP_TAG/$tag", message)
        }
    }

    /**
     * Warning log - يظهر في DEBUG و RELEASE (لكن بدون حساس details)
     */
    fun w(tag: String, message: String) {
        Log.w("$APP_TAG/$tag", sanitizeMessage(message))
    }

    /**
     * Warning log مع exception
     */
    fun w(tag: String, message: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w("$APP_TAG/$tag", message, tr)
        } else {
            // في Release: فقط الـ message بدون stack trace
            Log.w("$APP_TAG/$tag", sanitizeMessage(message))
        }
    }

    /**
     * Error log - يظهر دائماً لكن بدون حساس info في RELEASE
     */
    fun e(tag: String, message: String) {
        Log.e("$APP_TAG/$tag", sanitizeMessage(message))
    }

    /**
     * Error log مع exception
     */
    fun e(tag: String, message: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e("$APP_TAG/$tag", message, tr)
        } else {
            // في Release: الـ message بدون details من exception
            val errorMsg = "${sanitizeMessage(message)} [${tr::class.simpleName}]"
            Log.e("$APP_TAG/$tag", errorMsg)
        }
    }

    /**
     * Verbose log - يظهر فقط في DEBUG builds
     */
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v("$APP_TAG/$tag", message)
        }
    }

    /**
     * Secure log - للبيانات الحساسة (أبداً لا تطبع الـ sensitive data في production)
     */
    fun secureLog(tag: String, message: String, sensitiveData: Any? = null) {
        if (BuildConfig.DEBUG) {
            val fullMessage = if (sensitiveData != null) {
                "$message: $sensitiveData"
            } else {
                message
            }
            Log.d("$APP_TAG/$tag", "🔒 SECURE: $fullMessage")
        }
        // في Release: لا تطبع أي شيء
    }

    /**
     * Performance log - لقياس الأداء (debug only)
     */
    fun perf(tag: String, label: String, durationMs: Long) {
        if (BuildConfig.DEBUG) {
            Log.d("$APP_TAG/$tag", "⏱️  $label: ${durationMs}ms")
        }
    }

    /**
     * تنظيف الـ message من البيانات الحساسة
     * - إزالة مسارات الملفات
     * - إزالة IP addresses
     * - إزالة tokens و API keys
     */
    private fun sanitizeMessage(message: String): String {
        if (BuildConfig.DEBUG) return message

        var sanitized = message

        // إزالة مسارات الملفات (مثل /data/data/...)
        sanitized = sanitized.replace(Regex("""/data/.*?[\s,;]"""), "[FILE_PATH] ")
        sanitized = sanitized.replace(Regex("""[a-zA-Z]:\\.*?[\s,;]"""), "[FILE_PATH] ")

        // إزالة IP addresses
        sanitized = sanitized.replace(
            Regex("""(\d{1,3}\.){3}\d{1,3}"""),
            "[IP_ADDRESS]"
        )

        // إزالة tokens (bearer tokens, JWT, etc)
        sanitized = sanitized.replace(
            Regex("""bearer\s+[a-zA-Z0-9\-._~+/]+=*""", RegexOption.IGNORE_CASE),
            "[TOKEN]"
        )

        // إزالة API keys (محاولة عامة)
        sanitized = sanitized.replace(
            Regex("""api[_-]?key\s*[:=]\s*[a-zA-Z0-9\-._~+/]+=*""", RegexOption.IGNORE_CASE),
            "api_key=[HIDDEN]"
        )

        return sanitized
    }

    /**
     * Log crash بطريقة آمنة
     */
    fun logCrash(tag: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e("$APP_TAG/$tag", "💥 CRASH", tr)
        } else {
            Log.e(
                "$APP_TAG/$tag",
                "💥 CRASH: ${tr::class.simpleName} - ${tr.message ?: "Unknown error"}"
            )
        }
    }

    /**
     * قائمة الـ tags الشائعة
     */
    object Tags {
        const val PARSER = "Parser"
        const val MAP = "Map"
        const val LAYER = "Layer"
        const val GPS = "GPS"
        const val CACHE = "Cache"
        const val GIS = "GIS"
        const val PREF = "Preferences"
        const val SECURITY = "Security"
        const val PERFORMANCE = "Performance"
    }
}
