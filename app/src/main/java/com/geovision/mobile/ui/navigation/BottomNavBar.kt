/**
 * ملف التنقل السفلي (Bottom Navigation).
 * يوفّر شريط التنقل السفلي للتطبيق مع التبويبات:
 * الإعدادات، التفاصيل، الطبقات، والخريطة.
 */
package com.geovision.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.geovision.mobile.R

// ── تعريف تبويبات التنقل السفلي ──

/**
 * تعداد (Enum) يمثل تبويبات شريط التنقل السفلي.
 * كل تبويب يحتوي على:
 * - labelRes: معرف النص المترجم (string resource)
 * - icon: الأيقونة المعروضة
 * - route: مسار التنقل (NavHost route)
 */
enum class BottomNavTab(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String
) {
    SETTINGS(labelRes = R.string.nav_settings, icon = Icons.Default.Settings, route = "settings"),
    CALCULATOR(labelRes = R.string.nav_calculator, icon = Icons.Default.Calculate, route = "calculator"),
    DETAILS(labelRes = R.string.nav_details, icon = Icons.Default.Info, route = "details"),
    LAYERS(labelRes = R.string.nav_layers, icon = Icons.Default.Layers, route = "layers"),
    MAP(labelRes = R.string.nav_map, icon = Icons.Default.Map, route = "map")
}

// ── مكوّن شريط التنقل السفلي ──

/**
 * شريط التنقل السفلي (BottomNavigationBar) المبني على Material 3.
 *
 * @param selectedTab التبويب المحدد حالياً
 * @param onTabSelected دالة تستدعى عند النقر على تبويب
 */
@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        // تكرار عبر جميع التبويبات وعرض عنصر (NavigationBarItem) لكل منها
        BottomNavTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                // تخصيص ألوان التبويب المحدد / غير المحدد
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    }
}
