package com.geovision.mobile

// ── الاستيرادات ──
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.geovision.mobile.data.PreferencesManager
import com.geovision.mobile.ui.navigation.BottomNavTab
import com.geovision.mobile.ui.screens.calculator.GeoCalculatorScreen
import com.geovision.mobile.ui.screens.details.DetailsScreen
import com.geovision.mobile.ui.screens.layers.LayerDetailsScreen
import com.geovision.mobile.ui.screens.layers.LayerManagerScreen
import com.geovision.mobile.ui.screens.layers.LayerViewModel
import com.geovision.mobile.ui.screens.map.MapScreen
import com.geovision.mobile.ui.screens.settings.SettingsScreen
import com.geovision.mobile.ui.screens.terms.TermsScreen
import com.geovision.mobile.ui.screens.terms.UserGuideScreen
import com.geovision.mobile.ui.screens.terms.PrivacyScreen
import com.geovision.mobile.ui.screens.splash.OnboardingScreen
import com.geovision.mobile.ui.screens.splash.SplashScreen
import com.geovision.mobile.ui.theme.GeoVisionMobileTheme
import java.util.Locale

/**
 * Main Application Entry Point
 * نقطة دخول التطبيق الرئيسية
 *
 * FUNCTIONALITY / الوظائف:
 * - Manages application lifecycle and initialization
 *   إدارة دورة حياة التطبيق والتهيئة
 * - Applies saved language preference (Arabic/English)
 *   تطبيق تفضيل اللغة المحفوظ (عربي/إنجليزي)
 * - Handles RTL/LTR layout direction automatically
 *   معالجة اتجاه التخطيط من اليمين لليسار تلقائياً
 * - Displays Splash screen on app start
 *   عرض شاشة البداية عند تشغيل التطبيق
 * - Manages onboarding experience for first-time users
 *   إدارة تجربة التعريف للمستخدمين لأول مرة
 * - Provides theme switching (Light/Dark mode)
 *   توفير تبديل المظهر (الوضع الفاتح/الليلي)
 *
 * ARCHITECTURE / العمارة:
 * - Uses Jetpack Compose for modern UI
 *   يستخدم Jetpack Compose للواجهة الحديثة
 * - Manages UI state with mutable state variables
 *   إدارة حالة الواجهة باستخدام متغيرات الحالة
 * - Integrates with PreferencesManager for persistent storage
 *   يتكامل مع PreferencesManager للتخزين الدائم
 *
 * LANGUAGE SUPPORT / دعم اللغة:
 * - Default language: Arabic (ar)
 *   اللغة الافتراضية: العربية
 * - Supported languages: Arabic, English
 *   اللغات المدعومة: العربية، الإنجليزية
 * - RTL layout for Arabic, LTR for English
 *   تخطيط من اليمين لليسار للعربية، من اليسار لليمين للإنجليزية
 *
 * @see PreferencesManager for storing user preferences
 * @see GeoVisionMobileApp for main content navigation
 */
class MainActivity : ComponentActivity() {

    // مدير التفضيلات – لحفظ/تحميل إعدادات المستخدم
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager(this)
        applyLanguage() // تطبيق اللغة المحفوظة قبل بناء الواجهة

        enableEdgeToEdge()
        setContent {
            // ── حالة واجهة المستخدم ──
            var showSplash by remember { mutableStateOf(true) }  // التحكم في ظهور شاشة البداية
            var showOnboarding by remember { mutableStateOf(false) }  // شاشة التعريف
            var isDarkMode by remember { mutableStateOf(prefs.isDarkMode) }  // الوضع الليلي
            val raw = prefs.language
            val safeLang = if (raw.isNullOrBlank() || raw !in listOf("ar", "en")) "ar" else raw
            val isRtl = android.text.TextUtils.getLayoutDirectionFromLocale(java.util.Locale.Builder().setLanguage(safeLang).build()) == android.view.View.LAYOUT_DIRECTION_RTL
            // ضبط اتجاه التخطيط: من اليمين لليسار للعربية، ومن اليسار لليمين للإنجليزية
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            // توفير اتجاه التخطيط لجميع التوابع (Composables) داخل النطاق
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                GeoVisionMobileTheme(darkTheme = isDarkMode) {
                    if (showSplash) {
                        SplashScreen(onSplashFinished = {
                            showSplash = false
                            if (!prefs.onboardingShown) {
                                showOnboarding = true
                            }
                        })
                    } else if (showOnboarding) {
                        OnboardingScreen(onFinished = {
                            prefs.onboardingShown = true
                            showOnboarding = false
                        })
                    } else {
                        GeoVisionMobileApp(
                            isDarkMode = isDarkMode,
                            onDarkModeChanged = { dark -> isDarkMode = dark; prefs.isDarkMode = dark },
                            onLanguageChanged = { lang -> prefs.language = lang; recreate() }
                        )
                    }
                }
            }
        }
    }

    // ── تطبيق اللغة المختارة على موارد النظام ──
    private fun applyLanguage() {
        val raw = prefs.language
        val lang = if (raw.isNullOrBlank() || raw !in listOf("ar", "en")) "ar" else raw
        val locale = Locale.Builder().setLanguage(lang).build()
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

// ── الواجهة الرئيسية للتطبيق بعد شاشة البداية ──
/**
 * واجهة التطبيق الأساسية التي تحتوي على نظام التنقل (NavHost)
 * بين الشاشات المختلفة (الطبقات، الإعدادات، التفاصيل، الخريطة).
 */
@Composable
fun GeoVisionMobileApp(
    isDarkMode: Boolean = true,
    onDarkModeChanged: (Boolean) -> Unit = {},
    onLanguageChanged: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    // نموذج مشترك للعرض (ViewModel) يُستخدم عبر الشاشات لمشاركة بيانات الطبقات
    val sharedViewModel: LayerViewModel = viewModel(LocalContext.current as ComponentActivity)

    // ── دالة مساعدة للتنقل بين التبويبات مع حفظ الحالة ──
    val navigateToTab: (BottomNavTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(BottomNavTab.LAYERS.route) { saveState = true }
            launchSingleTop = true   // لا تفتح نسخة مكررة من التبويب
            restoreState = true      // استعادة الحالة السابقة للتبويب
        }
    }

    // ── نظام التنقل بين الشاشات (NavHost) ──
    NavHost(
        navController = navController,
        startDestination = BottomNavTab.LAYERS.route
    ) {
        // شاشة إدارة الطبقات
        composable(BottomNavTab.LAYERS.route) {
            LayerManagerScreen(
                onNavigateToTab = navigateToTab,
                onLayerClick = { layerId, layerName ->
                    navController.navigate("layer_details/$layerId/${android.net.Uri.encode(layerName)}")
                },
                viewModel = sharedViewModel
            )
        }

        // شاشة تفاصيل طبقة محددة (تستقبل معرّف الطبقة واسمها كوسائط مسار)
        composable(
            route = "layer_details/{layerId}/{layerName}",
            arguments = listOf(
                navArgument("layerId") { type = NavType.StringType },
                navArgument("layerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val layerId = backStackEntry.arguments?.getString("layerId") ?: ""
            val layerName = backStackEntry.arguments?.getString("layerName") ?: ""
            LayerDetailsScreen(
                layerId = layerId,
                layerName = layerName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTab = navigateToTab,
                onZoomToFeature = { _, lat, lon ->
                    sharedViewModel.requestZoomToFeature(lat, lon)
                    navController.navigate(BottomNavTab.MAP.route) {
                        popUpTo(BottomNavTab.LAYERS.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                viewModel = sharedViewModel
            )
        }

        // شاشة الإعدادات
        composable(BottomNavTab.SETTINGS.route) {
            SettingsScreen(
                onNavigateToTab = navigateToTab,
                onDarkModeChanged = onDarkModeChanged,
                onLanguageChanged = onLanguageChanged,
                onNavigateToTerms = { navController.navigate("terms") },
                onNavigateToGuide = { navController.navigate("guide") },
                onNavigateToPrivacy = { navController.navigate("privacy") }
            )
        }

        // شاشة شروط الاستخدام
        composable("terms") { TermsScreen(onNavigateBack = { navController.popBackStack() }) }
        // شاشة طريقة الاستخدام
        composable("guide") { UserGuideScreen(onNavigateBack = { navController.popBackStack() }) }
        // شاشة سياسة الخصوصية
        composable("privacy") { PrivacyScreen(onNavigateBack = { navController.popBackStack() }) }

        // شاشة الآلة الحاسبة الجغرافية
        composable(BottomNavTab.CALCULATOR.route) {
            GeoCalculatorScreen(
                onNavigateToTab = navigateToTab
            )
        }

        // شاشة التفاصيل العامة
        composable(BottomNavTab.DETAILS.route) {
            DetailsScreen(
                onNavigateToTab = navigateToTab,
                onLayerClick = { id, name -> navController.navigate("layer_details/$id/$name") }
            )
        }

        // شاشة الخريطة
        composable(BottomNavTab.MAP.route) {
            MapScreen(
                onNavigateToTab = navigateToTab,
                viewModel = sharedViewModel,
                onNavigateToLayerDetails = { layerId, layerName ->
                    navController.navigate("layer_details/$layerId/$layerName")
                }
            )
        }
    }
}
