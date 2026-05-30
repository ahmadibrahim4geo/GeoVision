/**
 * ملف السمة (Theme) للتطبيق.
 * يوفّر نظام ألوان Material 3 للوضع الليلي (Dark) والنهاري (Light)،
 * ويطبّق الألوان على شريط الحالة (Status Bar).
 */
package com.geovision.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── نظام الألوان للوضع الليلي (Dark Mode) ──
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest
)

// ── نظام الألوان للوضع النهاري (Light Mode) ──
private val LightColorScheme = lightColorScheme(
    primary = InversePrimary,
    onPrimary = OnPrimaryFixed,
    primaryContainer = PrimaryFixed,
    onPrimaryContainer = OnPrimaryFixedVariant,
    inversePrimary = Primary,
    secondary = OnSecondaryContainer,
    onSecondary = OnSecondaryFixed,
    secondaryContainer = SecondaryFixed,
    onSecondaryContainer = OnSecondaryFixedVariant,
    tertiary = OnTertiaryContainer,
    onTertiary = OnTertiaryFixed,
    tertiaryContainer = TertiaryFixed,
    onTertiaryContainer = OnTertiaryFixedVariant,
    error = ErrorContainer,
    onError = OnErrorContainer,
    errorContainer = Error,
    onErrorContainer = OnError,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceDim,
    onSurfaceVariant = LightOnSurface,
    outline = Outline,
    outlineVariant = LightSurfaceDim,
    inverseSurface = Surface,
    inverseOnSurface = OnSurface,
    surfaceBright = LightSurface,
    surfaceDim = LightSurfaceDim
)

// ── دالة السمة الرئيسية ──

/**
 * السمة الرئيسية للتطبيق (GeoVision Mobile Theme).
 * تطبّق نظام الألوان (فاتح/داكن) والخطوط على جميع التوابع (Composables).
 *
 * @param darkTheme هل يستخدم الوضع الليلي؟ (يتم الكشف تلقائياً من النظام إن لم يُحدد)
 * @param content محتوى الواجهة الذي ستُطبّق عليه السمة
 */
@Composable
fun GeoVisionMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // اختيار نظام الألوان حسب الوضع المطلوب
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    // ── ضبط مظهر أيقونات شريط الحالة (Status Bar): فاتح للوضع الليلي، داكن للنهاري ──
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // ── تطبيق سمة Material 3 على المحتوى ──
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
