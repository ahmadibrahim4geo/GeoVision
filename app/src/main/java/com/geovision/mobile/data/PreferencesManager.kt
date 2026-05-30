package com.geovision.mobile.data

// ── الاستيرادات ──
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.geovision.mobile.core.AppLogger

/**
 * مدير التفضيلات – مسؤول عن حفظ واسترجاع إعدادات المستخدم
 * باستخدام EncryptedSharedPreferences لحماية البيانات الحساسة.
 *
 * الميزات الأمنية:
 * - تشفير البيانات على مستوى النظام (Android Keystore)
 * - حماية من الوصول المباشر لملف SharedPreferences
 * - حماية API keys والإحداثيات الحساسة
 *
 * الاستخدام:
 * ```
 * val prefs = PreferencesManager(context)
 * prefs.language = "en"
 * val lang = prefs.language
 * ```
 */
class PreferencesManager(context: Context) {

    // ── إعداد الـ Keystore والـ EncryptedSharedPreferences ──
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        AppLogger.e(AppLogger.Tags.SECURITY, "Failed to create EncryptedSharedPreferences", e)
        // Fallback إلى SharedPreferences العادي في حالة الفشل (لا يفضل)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // نظام الوحدات (metric / imperial) – القيمة الافتراضية: متري
    var unitSystem: String
        get() = prefs.getString(KEY_UNIT, "metric") ?: "metric"
        set(value) = prefs.edit().putString(KEY_UNIT, value).apply()

    // اللغة المختارة – القيمة الافتراضية: العربية (ar)
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    // تفعيل الوضع الليلي – القيمة الافتراضية: مفعّل (true)
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    // حجم ذاكرة التخزين المؤقت للخريطة – القيمة الافتراضية: 500MB
    var tileCacheSize: String
        get() = prefs.getString(KEY_CACHE_SIZE, "500mb") ?: "500mb"
        set(value) = prefs.edit().putString(KEY_CACHE_SIZE, value).apply()

    // نوع الخريطة الافتراضي (mapnik إلخ.)
    var defaultMapType: String
        get() = prefs.getString(KEY_DEFAULT_MAP_TYPE, "mapnik") ?: "mapnik"
        set(value) = prefs.edit().putString(KEY_DEFAULT_MAP_TYPE, value).apply()

    // آخر خريطة استخدمها المستخدم (تستعاد عند فتح التطبيق)
    var lastMapType: String
        get() = prefs.getString(KEY_LAST_MAP_TYPE, null) ?: defaultMapType
        set(value) = prefs.edit().putString(KEY_LAST_MAP_TYPE, value).apply()

    // إظهار/إخفاء البيز ماب – القيمة الافتراضية: ظاهر (true)
    var showBasemap: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BASEMAP, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BASEMAP, value).apply()

    // تنسيق الإحداثيات الافتراضي (dd = Decimal Degrees)
    var defaultCoordFormat: String
        get() = prefs.getString(KEY_DEFAULT_COORD_FORMAT, "dd") ?: "dd"
        set(value) = prefs.edit().putString(KEY_DEFAULT_COORD_FORMAT, value).apply()

    // إبقاء الشاشة مضاءة أثناء الاستخدام – القيمة الافتراضية: غير مفعّل
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    // رموز القياس (symbol = رموز, letter = أحرف) – القيمة الافتراضية: رموز
    var measureSymbols: String
        get() = prefs.getString(KEY_MEASURE_SYMBOLS, "symbol") ?: "symbol"
        set(value) = prefs.edit().putString(KEY_MEASURE_SYMBOLS, value).apply()

    // تصنيف الخرائط في القوائم (provider = حسب المصدر, category = حسب النوع)
    var mapClassification: String
        get() = prefs.getString(KEY_MAP_CLASSIFICATION, "category") ?: "category"
        set(value) = prefs.edit().putString(KEY_MAP_CLASSIFICATION, value).apply()

    // تفعيل/إيقاف الالتقاط (Snap)
    var snapEnabled: Boolean
        get() = prefs.getBoolean(KEY_SNAP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SNAP_ENABLED, value).apply()

    // مسافة الالتقاط بالأمتار (Snap Distance) – القيمة الافتراضية: 30 متر
    var snapDistanceMeters: Float
        get() = prefs.getFloat(KEY_SNAP_DISTANCE_METERS, 30f).coerceIn(1f, 500f)
        set(value) = prefs.edit().putFloat(KEY_SNAP_DISTANCE_METERS, value.coerceIn(1f, 500f)).apply()

    // نمط الرسم (polar = قطبي حر, ortho = عمودي) – القيمة الافتراضية: polar
    var drawMode: String
        get() = prefs.getString(KEY_DRAW_MODE, "polar") ?: "polar"
        set(value) = prefs.edit().putString(KEY_DRAW_MODE, value).apply()

    // آخر موقع للخريطة (خط العرض، خط الطول، مستوى التكبير)
    // نخزّن كـ String للحفاظ على دقة Double الكاملة
    var mapCenterLat: Double
        get() = prefs.getString(KEY_MAP_LAT, null)?.toDoubleOrNull() ?: 24.7136
        set(value) = prefs.edit().putString(KEY_MAP_LAT, value.toString()).apply()

    var mapCenterLon: Double
        get() = prefs.getString(KEY_MAP_LON, null)?.toDoubleOrNull() ?: 46.6753
        set(value) = prefs.edit().putString(KEY_MAP_LON, value.toString()).apply()

    var mapZoom: Double
        get() = prefs.getString(KEY_MAP_ZOOM, null)?.toDoubleOrNull() ?: 15.0
        set(value) = prefs.edit().putString(KEY_MAP_ZOOM, value.toString()).apply()

    // ── دوال خاصّة بالطبقات (Layers) ──

    /** هل الطبقة مرئية؟ – القيمة الافتراضية: مرئية */
    fun isLayerVisible(layerId: String): Boolean {
        return prefs.getBoolean("layer_visible_$layerId", true)
    }

    /** تعيين حالة رؤية الطبقة */
    fun setLayerVisible(layerId: String, visible: Boolean) {
        prefs.edit().putBoolean("layer_visible_$layerId", visible).apply()
    }

    /** الحصول على مستوى شفافية الطبقة (0.0 = شفاف كلياً، 1.0 = معتم كلياً) */
    fun getLayerTransparency(layerId: String): Float {
        return prefs.getFloat("layer_alpha_$layerId", 1.0f)
    }

    /** تعيين مستوى شفافية الطبقة */
    fun setLayerTransparency(layerId: String, alpha: Float) {
        prefs.edit().putFloat("layer_alpha_$layerId", alpha).apply()
    }

    // ── حفظ/استرجاع قائمة الطبقات بصيغة JSON ──
    var layersJson: String
        get() = prefs.getString(KEY_LAYERS_JSON, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_LAYERS_JSON, value).apply()

    // ── شاشة التعريف (Onboarding) ──
    var onboardingShown: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, value).apply()

    // ── دوال الصيانة وإعادة التعيين ──

    /** إعادة تعيين جميع الإعدادات إلى القيم الافتراضية */
    fun resetToDefaults() {
        prefs.edit()
            .putString(KEY_UNIT, "metric")
            .putString(KEY_CACHE_SIZE, "500mb")
            .putString(KEY_DEFAULT_MAP_TYPE, "mapnik")
            .putString(KEY_DEFAULT_COORD_FORMAT, "dd")
            .putBoolean(KEY_KEEP_SCREEN_ON, false)
            .putString(KEY_MEASURE_SYMBOLS, "symbol")
            .putString(KEY_MAP_CLASSIFICATION, "category")
            .putString(KEY_DRAW_MODE, "polar")
            .putBoolean(KEY_SNAP_ENABLED, true)
            .putFloat(KEY_SNAP_DISTANCE_METERS, 30f)
            .putString(KEY_LANGUAGE, "ar")
            .putBoolean(KEY_DARK_MODE, false)
            .putBoolean(KEY_SHOW_BASEMAP, true)
            .remove(KEY_MAP_LAT)
            .remove(KEY_MAP_LON)
            .remove(KEY_MAP_ZOOM)
            .apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    // ── مفاتيح التفضيلات (ثوابت) ──
    companion object {
        private const val PREFS_NAME = "geovision_prefs"              // اسم ملف التفضيلات
        private const val KEY_UNIT = "unit_system"                    // مفتاح نظام الوحدات
        private const val KEY_LANGUAGE = "language"                   // مفتاح اللغة
        private const val KEY_DARK_MODE = "dark_mode"                 // مفتاح الوضع الليلي
        private const val KEY_CACHE_SIZE = "tile_cache_size"          // مفتاح حجم ذاكرة التخزين
        private const val KEY_DEFAULT_MAP_TYPE = "default_map_type"   // مفتاح نوع الخريطة
        private const val KEY_DEFAULT_COORD_FORMAT = "default_coord_format" // مفتاح تنسيق الإحداثيات
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"       // مفتاح إبقاء الشاشة مضاءة
        private const val KEY_MEASURE_SYMBOLS = "measure_symbols"     // مفتاح رموز القياس
        private const val KEY_MAP_CLASSIFICATION = "map_classification" // مفتاح تصنيف الخرائط
        private const val KEY_MAP_LAT = "map_center_lat"              // مفتاح خط عرض الخريطة
        private const val KEY_MAP_LON = "map_center_lon"              // مفتاح خط طول الخريطة
        private const val KEY_MAP_ZOOM = "map_zoom"                   // مفتاح مستوى تكبير الخريطة
        private const val KEY_LAYERS_JSON = "layers_json"             // مفتاح قائمة الطبقات
        private const val KEY_LAST_MAP_TYPE = "last_map_type"          // مفتاح آخر خريطة مستخدمة
        private const val KEY_SHOW_BASEMAP = "show_basemap"           // مفتاح إظهار البيز ماب
        private const val KEY_SNAP_ENABLED = "snap_enabled"            // مفتاح تفعيل الالتقاط
        private const val KEY_SNAP_DISTANCE_METERS = "snap_distance_meters" // مفتاح مسافة الالتقاط بالأمتار
        private const val KEY_SNAP_DISTANCE = "snap_distance"          // قديم – لم يعد مستخدماً
        private const val KEY_DRAW_MODE = "draw_mode"                  // مفتاح نمط الرسم (polar/ortho)
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"   // مفتاح شاشة التعريف
    }
}
