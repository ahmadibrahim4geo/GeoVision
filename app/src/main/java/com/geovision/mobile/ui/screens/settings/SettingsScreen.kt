package com.geovision.mobile.ui.screens.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geovision.mobile.R
import com.geovision.mobile.data.PreferencesManager
import com.geovision.mobile.ui.screens.map.BasemapPickerContent
import com.geovision.mobile.ui.screens.map.tileSources
import com.geovision.mobile.ui.navigation.BottomNavBar
import com.geovision.mobile.ui.navigation.BottomNavTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToTab: (BottomNavTab) -> Unit,
    prefs: PreferencesManager? = null,
    onDarkModeChanged: ((Boolean) -> Unit)? = null,
    onLanguageChanged: ((String) -> Unit)? = null,
    onNavigateToTerms: (() -> Unit)? = null,
    onNavigateToGuide: (() -> Unit)? = null,
    onNavigateToPrivacy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefsManager = prefs ?: remember { PreferencesManager(context) }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedUnit by remember { mutableStateOf(prefsManager.unitSystem) }
    var selectedCache by remember { mutableStateOf(prefsManager.tileCacheSize) }
    var selectedLanguage by remember { mutableStateOf(prefsManager.language) }
    var darkModeEnabled by remember { mutableStateOf(prefsManager.isDarkMode) }
    var selectedMapType by remember { mutableStateOf(prefsManager.defaultMapType) }
    var selectedCoordFmt by remember { mutableStateOf(prefsManager.defaultCoordFormat) }
    var keepScreenOnEnabled by remember { mutableStateOf(prefsManager.keepScreenOn) }
    var showBasemapEnabled by remember { mutableStateOf(prefsManager.showBasemap) }
    var measureSymbolsEnabled by remember { mutableStateOf(prefsManager.measureSymbols == "symbol") }
    var selectedDrawMode by remember { mutableStateOf(prefsManager.drawMode) }
    var cacheSize by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedClassification by remember { mutableStateOf(prefsManager.mapClassification) }

    val cacheClearedMsg = stringResource(R.string.cache_cleared)
    val cacheFailedMsg = stringResource(R.string.cache_failed)
    val settingsResetMsg = stringResource(R.string.settings_reset)

    suspend fun calcCacheSize(): String = withContext(Dispatchers.IO) {
        val base = context.cacheDir
        val external = try { context.externalCacheDir } catch (_: Exception) { null }
        val candidates = listOfNotNull(
            base.resolve("tiles"),
            base.resolve("osmdroid"),
            external?.resolve("tiles"),
            external?.resolve("osmdroid")
        )
        val bytes = try {
            candidates.sumOf { d -> if (d.exists()) d.walkTopDown().onFail { _, _ -> }.filter { it.isFile }.sumOf { it.length() } else 0L }
        } catch (_: Exception) { -1L }
        if (bytes < 0L) "---"
        else if (bytes < 1024L * 1024L) "${bytes / 1024L} KB"
        else java.lang.String.format(java.util.Locale.US, "%.2f MB", bytes.toDouble() / (1024.0 * 1024.0))
    }

    LaunchedEffect(Unit) { cacheSize = calcCacheSize() }

    // إعادة حساب حجم الخبأة عند العودة إلى الشاشة
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch { cacheSize = calcCacheSize() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cardShape = RoundedCornerShape(16.dp)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceDim)
            )
        },
        bottomBar = {
            BottomNavBar(selectedTab = BottomNavTab.SETTINGS, onTabSelected = onNavigateToTab)
        },
        containerColor = MaterialTheme.colorScheme.surfaceDim
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── لغة التطبيق ──
            SettingsCard(cardShape, Icons.Outlined.Language, stringResource(R.string.language)) {
                Column(modifier = Modifier.selectableGroup()) {
                    SettingsRadioItem(
                        label = stringResource(R.string.arabic),
                        selected = selectedLanguage == "ar",
                        badge = "ع",
                        onClick = { selectedLanguage = "ar"; prefsManager.language = "ar"; onLanguageChanged?.invoke("ar") }
                    )
                    SettingsRadioItem(
                        label = stringResource(R.string.english),
                        selected = selectedLanguage == "en",
                        badge = "EN",
                        onClick = { selectedLanguage = "en"; prefsManager.language = "en"; onLanguageChanged?.invoke("en") }
                    )
                }
            }

            // ── العرض ──
            SettingsCard(cardShape, Icons.Outlined.DisplaySettings, stringResource(R.string.display)) {
                SettingsSwitchItem(
                    label = stringResource(R.string.dark_mode),
                    icon = Icons.Outlined.DarkMode,
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it; prefsManager.isDarkMode = it; onDarkModeChanged?.invoke(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsSwitchItem(
                    label = stringResource(R.string.keep_screen_on),
                    icon = Icons.Outlined.ScreenLockPortrait,
                    checked = keepScreenOnEnabled,
                    onCheckedChange = { keepScreenOnEnabled = it; prefsManager.keepScreenOn = it }
                )
            }

            // ── وحدات القياس ──
            SettingsCard(cardShape, Icons.Outlined.Straighten, stringResource(R.string.measurement_units)) {
                Column(modifier = Modifier.selectableGroup()) {
                    SettingsRadioItem(
                        label = stringResource(R.string.unit_metric),
                        selected = selectedUnit == "metric",
                        icon = Icons.Outlined.Straighten,
                        onClick = { selectedUnit = "metric"; prefsManager.unitSystem = "metric" }
                    )
                    SettingsRadioItem(
                        label = stringResource(R.string.unit_imperial),
                        selected = selectedUnit == "imperial",
                        icon = Icons.Outlined.SocialDistance,
                        onClick = { selectedUnit = "imperial"; prefsManager.unitSystem = "imperial" }
                    )
                }
            }

            // ── تنسيق الإحداثيات الافتراضي ──
            SettingsCard(cardShape, Icons.Outlined.PinDrop, stringResource(R.string.default_coord_format)) {
                Column(modifier = Modifier.selectableGroup()) {
                    listOf(
                        "dd" to (R.string.coord_dd to Icons.Outlined.Pin),
                        "dms" to (R.string.coord_dms to Icons.Outlined.Edit),
                        "utm" to (R.string.coord_utm to Icons.Outlined.GridOn)
                    ).forEach { (key, pair) ->
                        val (labelRes, icon) = pair
                        SettingsRadioItem(
                            label = stringResource(labelRes),
                            selected = selectedCoordFmt == key,
                            icon = icon,
                            onClick = { selectedCoordFmt = key; prefsManager.defaultCoordFormat = key }
                        )
                    }
                }
            }

            // ── نمط الرسم (Polar / Ortho) ──
            SettingsCard(
                cardShape,
                Icons.Outlined.ShowChart,
                stringResource(R.string.draw_mode_title)
            ) {
                Column(modifier = Modifier.selectableGroup()) {
                    listOf(
                        "polar" to R.string.draw_mode_polar,
                        "ortho" to R.string.draw_mode_ortho
                    ).forEach { (key, labelRes) ->
                        SettingsRadioItem(
                            label = stringResource(labelRes),
                            selected = selectedDrawMode == key,
                            icon = if (key == "polar") {
                                Icons.Outlined.Loupe
                            } else {
                                Icons.Outlined.GridOn
                            },
                            onClick = { selectedDrawMode = key; prefsManager.drawMode = key }
                        )
                    }
                }
            }

            // ── تسميات القياس ──
            SettingsCard(cardShape, Icons.Outlined.Straighten, stringResource(R.string.measurement)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasureModeCard(
                        title = stringResource(R.string.measure_symbols),
                        icon = Icons.Outlined.SquareFoot,
                        selected = measureSymbolsEnabled,
                        onClick = { measureSymbolsEnabled = true; prefsManager.measureSymbols = "symbol" },
                        mod = Modifier.weight(1f)
                    ) {
                        MeasureSettingsLegend(useSymbols = true)
                    }
                    MeasureModeCard(
                        title = stringResource(R.string.msr_legend_switch),
                        icon = Icons.Outlined.TextFields,
                        selected = !measureSymbolsEnabled,
                        onClick = { measureSymbolsEnabled = false; prefsManager.measureSymbols = "letter" },
                        mod = Modifier.weight(1f)
                    ) {
                        MeasureSettingsLegend(useSymbols = false)
                    }
                }
            }

            // ── الالتقاط (Snap) ──
            val snapEnabled = remember { mutableStateOf(prefsManager.snapEnabled) }
            val snapMeters = remember { mutableFloatStateOf(prefsManager.snapDistanceMeters) }
            var snapText by remember(snapEnabled.value) { mutableStateOf(java.lang.String.format(java.util.Locale.US, "%.0f", snapMeters.floatValue)) }
            LaunchedEffect(snapEnabled.value) { prefsManager.snapEnabled = snapEnabled.value }
            LaunchedEffect(snapMeters.floatValue) { prefsManager.snapDistanceMeters = snapMeters.floatValue }
            SettingsCard(cardShape, Icons.Outlined.TouchApp, stringResource(R.string.snap_distance)) {
                Text(stringResource(R.string.snap_distance_desc), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.snap_enable), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = snapEnabled.value, onCheckedChange = { snapEnabled.value = it })
                }
                if (snapEnabled.value) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snapText,
                        onValueChange = { s ->
                            val clean = s.filter { it.isDigit() || it == '.' }
                            snapText = clean
                            val v = clean.toFloatOrNull()
                            if (v != null && v > 0f) {
                                snapMeters.floatValue = v.coerceIn(1f, 500f)
                            }
                        },
                        label = { Text(stringResource(R.string.snap_distance_meters)) },
                        suffix = { Text("m") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── تصنيف الخرائط الافتراضي ──
            SettingsCard(cardShape, Icons.Outlined.Category, stringResource(R.string.map_classification)) {
                Text(stringResource(R.string.map_classification_desc), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Column(modifier = Modifier.selectableGroup()) {
                    SettingsRadioItem(
                        label = stringResource(R.string.map_classification_provider),
                        selected = selectedClassification == "provider",
                        icon = Icons.Outlined.Dns,
                        onClick = { selectedClassification = "provider"; prefsManager.mapClassification = "provider" }
                    )
                    SettingsRadioItem(
                        label = stringResource(R.string.map_classification_category),
                        selected = selectedClassification == "category",
                        icon = Icons.Outlined.Category,
                        onClick = { selectedClassification = "category"; prefsManager.mapClassification = "category" }
                    )
                }
            }

            // ── نوع الخريطة الافتراضي ──
            SettingsCard(cardShape, Icons.Outlined.Map, stringResource(R.string.default_map_type)) {
                BasemapPickerContent(
                    tileSources = tileSources,
                    selectedKey = selectedMapType,
                    defaultKey = null,
                    classification = selectedClassification,
                    radioStyle = true,
                    onSelect = { key -> selectedMapType = key; prefsManager.defaultMapType = key }
                )
            }

            SettingsSwitchItem(
                label = stringResource(R.string.show_basemap),
                icon = Icons.Outlined.Map,
                checked = showBasemapEnabled,
                onCheckedChange = { showBasemapEnabled = it; prefsManager.showBasemap = it }
            )

            // ── إعدادات الخريطة ──
            SettingsCard(cardShape, Icons.Outlined.Map, stringResource(R.string.map_settings)) {
                Text(
                    stringResource(R.string.tile_cache_size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Text(
                    stringResource(R.string.tile_cache_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Column(modifier = Modifier.selectableGroup()) {
                    SettingsRadioItem(
                        label = "1GB",
                        selected = selectedCache == "1gb",
                        icon = Icons.Outlined.Storage,
                        onClick = { selectedCache = "1gb"; prefsManager.tileCacheSize = "1gb" }
                    )
                    SettingsRadioItem(
                        label = "500MB",
                        selected = selectedCache == "500mb",
                        icon = Icons.Outlined.Storage,
                        onClick = { selectedCache = "500mb"; prefsManager.tileCacheSize = "500mb" }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DeleteSweep, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.clear_cache), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                try {
                                    cacheSize = null
                                    context.cacheDir.resolve("tiles").takeIf { it.exists() }?.deleteRecursively()
                                    context.cacheDir.resolve("osmdroid").takeIf { it.exists() }?.deleteRecursively()
                                    cacheSize = calcCacheSize()
                                    snackbarHostState.showSnackbar(cacheClearedMsg)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("$cacheFailedMsg: ${e.message}")
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) { Text(stringResource(R.string.clear)) }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (cacheSize == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SdStorage, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.cache_current_size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                cacheSize ?: stringResource(R.string.calculating),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { scope.launch { cacheSize = null; cacheSize = calcCacheSize() } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.refresh), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // ── حول التطبيق ──
            SettingsCard(cardShape, Icons.Outlined.Info, stringResource(R.string.about)) {
                SettingsInfoItem(
                    label = stringResource(R.string.app_version),
                    value = getAppVersion(context),
                    icon = Icons.Outlined.Tag
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsClickableItem(
                    label = stringResource(R.string.user_guide),
                    icon = Icons.Outlined.Help
                ) {
                    context.startActivity(android.content.Intent(context, com.geovision.mobile.ui.screens.terms.UserGuideActivity::class.java))
                }
                SettingsClickableItem(
                    label = stringResource(R.string.privacy_title),
                    icon = Icons.Outlined.PrivacyTip
                ) {
                    context.startActivity(android.content.Intent(context, com.geovision.mobile.ui.screens.terms.PrivacyPolicyActivity::class.java))
                }
                SettingsClickableItem(
                    label = stringResource(R.string.terms_of_use),
                    icon = Icons.Outlined.Description
                ) {
                    context.startActivity(android.content.Intent(context, com.geovision.mobile.ui.screens.terms.TermsOfUseActivity::class.java))
                }
            }

            // ── شاشة التعريف ──
            ElevatedCard(
                shape = cardShape,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                onClick = { prefsManager.onboardingShown = false; context.startActivity(android.content.Intent(context, com.geovision.mobile.MainActivity::class.java).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK }) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Explore, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.show_intro_screen), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }

            // ── إعادة تعيين الإعدادات ──
            ElevatedCard(
                shape = cardShape,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                onClick = { showResetDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.RestartAlt, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.reset_settings),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.reset_settings)) },
            text = { Text(stringResource(R.string.reset_settings_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    prefsManager.resetToDefaults()
                    selectedUnit = prefsManager.unitSystem
                    selectedCache = prefsManager.tileCacheSize
                    selectedMapType = prefsManager.defaultMapType
                    selectedCoordFmt = prefsManager.defaultCoordFormat
                    darkModeEnabled = prefsManager.isDarkMode
                    keepScreenOnEnabled = prefsManager.keepScreenOn
                    showBasemapEnabled = prefsManager.showBasemap
                    measureSymbolsEnabled = prefsManager.measureSymbols == "symbol"
                    selectedLanguage = prefsManager.language
                    selectedClassification = prefsManager.mapClassification
                    showResetDialog = false
                    scope.launch { snackbarHostState.showSnackbar(settingsResetMsg) }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

private fun getAppVersion(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }
}

/** بطاقة إعدادات — حاوية مرتفعة (ElevatedCard) تحتوي على أيقونة، عنوان، ومحتوى القسم */
@Composable
private fun SettingsCard(shape: RoundedCornerShape, icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Icon(icon, null, Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** عنصر اختيار وحيد (راديو) مع أيقونة أو شارة نصية — يستخدم لعرض خيار واحد ضمن مجموعة اختيار حصرية */
@Composable
private fun SettingsRadioItem(label: String, selected: Boolean, icon: ImageVector? = null, badge: String? = null, subLabel: String? = null, onClick: () -> Unit) {
    val bgColor = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.surfaceContainerLow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
        )
        Spacer(Modifier.width(4.dp))
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (selected) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (subLabel != null) {
                    Spacer(Modifier.width(4.dp))
                    Text("($subLabel)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

/** عنصر تبديل (Switch) مع أيقونة — يستخدم للإعدادات الثنائية */
@Composable
private fun SettingsSwitchItem(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}

/** عنصر عرض معلومات — يعرض أيقونة، تسمية وقيمتها في صف أفقي واحد */
@Composable
private fun SettingsInfoItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

/** عنصر قابل للنقر — يعرض أيقونة مع تسمية وسهم جانبي ويستجيب للنقر */
@Composable
private fun SettingsClickableItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

/** جدول دليل القياس — يعرض الرمز ↔ الاختصار ↔ المعنى */
@Composable
private fun MeasureSettingsLegend(useSymbols: Boolean) {
    data class Entry(val symbol: String, val abbr: String, val labelRes: Int)
    data class Category(val titleRes: Int, val entries: List<Entry>)

    val categories = listOf(
        Category(R.string.msr_cat_distance, listOf(
            Entry("\u2192", "L", R.string.msr_len)
        )),
        Category(R.string.msr_cat_area, listOf(
            Entry("\u25A0", "A", R.string.msr_area),
            Entry("\u25A1", "P", R.string.msr_perim)
        )),
        Category(R.string.msr_cat_circle, listOf(
            Entry("\u25CF", "A", R.string.msr_area_circle),
            Entry("\u25CB", "C", R.string.msr_circ),
            Entry("\u2300", "D", R.string.msr_diam),
            Entry("\u25C9", "R", R.string.msr_radius)
        )),
        Category(R.string.msr_cat_ellipse, listOf(
            Entry("\u2B2D", "A", R.string.ellipse_area),
            Entry("\u2B2C", "P", R.string.ellipse_perimeter),
            Entry("\u2194", "a", R.string.ellipse_major_axis),
            Entry("\u2195", "b", R.string.ellipse_minor_axis),
            Entry("\u2220", "\u03B1", R.string.ellipse_angle_label)
        ))
    )

    Column {
        categories.forEach { cat ->
            Text(stringResource(cat.titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.W700,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            cat.entries.forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp).padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(32.dp).height(IntrinsicSize.Min),
                        contentAlignment = Alignment.Center) {
                        if (useSymbols && (e.symbol == "\u2B2C" || e.symbol == "\u2B2D")) {
                            val isPerimeter = e.symbol == "\u2B2C"
                            Icon(
                                if (isPerimeter) com.geovision.mobile.ui.screens.map.EllipseIcon
                                else com.geovision.mobile.ui.screens.map.EllipseFillIcon,
                                contentDescription = null,
                                modifier = Modifier.width(24.dp).height(14.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                        } else {
                            Text(if (useSymbols) e.symbol else e.abbr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center)
                        }
                    }
                    Text("=", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 6.dp))
                    Text(stringResource(e.labelRes), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** بطاقة اختيار وضع القياس — قابلة للنقر، تعرض دليل + معاينة حسب الوضع */
@Composable
private fun MeasureModeCard(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, mod: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
        modifier = mod
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Surface(shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Icon(icon, null, Modifier.padding(6.dp).size(16.dp),
                        tint = if (selected) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W700,
                    color = if (selected) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                }
            }
            content()
        }
    }
}
