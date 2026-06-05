package com.geovision.mobile.ui.screens.map

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.core.GpsLifecycleManager
import com.geovision.mobile.core.MemoryMonitor
import com.geovision.mobile.core.OverlayManager
import com.geovision.mobile.data.ElevationService
import com.geovision.mobile.data.GeoPhoto
import com.geovision.mobile.data.GeometryParser
import com.geovision.mobile.data.GpsService
import com.geovision.mobile.data.PreferencesManager
import com.geovision.mobile.R
import com.geovision.mobile.ui.navigation.BottomNavBar
import com.geovision.mobile.ui.navigation.BottomNavTab
import com.geovision.mobile.ui.screens.layers.*
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView as MMapView
import org.maplibre.android.maps.MapLibreMap
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯
//  ط·آ§ط¸â€‍ط·آ´ط·آ§ط·آ´ط·آ© ط·آ§ط¸â€‍ط·آ±ط·آ¦ط¸ظ¹ط·آ³ط¸ظ¹ط·آ© ط¸â€‍ط¸â€‍ط·آ®ط·آ±ط¸ظ¹ط·آ·ط·آ© (MAIN SCREEN)
//  ط·ع¾ط·آ­ط·ع¾ط¸ث†ط¸ظ¹ ط·آ¹ط¸â€‍ط¸â€° Scaffold ط¸ئ’ط·آ§ط¸â€¦ط¸â€‍ ط¸â€¦ط·آ¹ ط·آ§ط¸â€‍ط·آ®ط·آ±ط¸ظ¹ط·آ·ط·آ© ط¸ث†ط·آ£ط·آ¯ط¸ث†ط·آ§ط·ع¾ ط·آ§ط¸â€‍ط·ع¾ط·آ­ط¸ئ’ط¸â€¦
// أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯أ¢â€¢ع¯
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToTab: (BottomNavTab) -> Unit,
    viewModel: LayerViewModel,
    onNavigateToLayerDetails: (String, String) -> Unit
) {
    val ctx = LocalContext.current; val lo = LocalLifecycleOwner.current
    val prefs = remember { PreferencesManager(ctx) }; val imp = prefs.unitSystem == "imperial"
    MsrLabels.useSymbols = prefs.measureSymbols == "symbol"
    var mv by remember { mutableStateOf<MapView?>(null) }
    var mlMap by remember { mutableStateOf<MapLibreMap?>(null) }

    remember { MapLibre.getInstance(ctx) }

    // طھظ‡ظٹط¦ط© osmdroid ظ‚ط¨ظ„ ط¥ظ†ط´ط§ط، MapView
    remember(prefs.tileCacheSize) {
        val c = if (prefs.tileCacheSize == "1gb" || prefs.tileCacheSize == "500mb") 1024L * 1024L * 1024L else 512L * 1024L * 1024L
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName; osmdroidTileCache = ctx.cacheDir.resolve("tiles")
            tileFileSystemCacheMaxBytes = c; tileFileSystemCacheTrimBytes = c * 3 / 4
        }
    }
    var mlMv by remember { mutableStateOf<MMapView?>(null) }
    val lifecycle = lo.lifecycle
    DisposableEffect(lo) {
        val gpsManager = GpsLifecycleManager(mv, lifecycle)
        val overlayManager = OverlayManager(mv)
        val memoryMonitor = MemoryMonitor(ctx)
        val o = LifecycleEventObserver { _, e -> when (e) {
            Lifecycle.Event.ON_RESUME -> { mv?.onResume(); mlMv?.onResume(); AppLogger.d(AppLogger.Tags.MAP, "MapScreen resumed") }
            Lifecycle.Event.ON_PAUSE -> { mv?.onPause(); mlMv?.onPause(); overlayManager.periodicCleanup(); AppLogger.d(AppLogger.Tags.MAP, "MapScreen paused") }
            Lifecycle.Event.ON_DESTROY -> {
                mv?.onDetach(); mlMv?.onDestroy()
                overlayManager.clearAll()
                memoryMonitor.log()
                AppLogger.d(AppLogger.Tags.MAP, "MapScreen destroyed - resources cleaned up")
            }
            else -> {}
        } }
        lifecycle.addObserver(o)
        memoryMonitor.log()
        onDispose {
            lifecycle.removeObserver(o)
            AppLogger.d(AppLogger.Tags.MAP, "MapScreen DisposableEffect disposed")
        }
    }
    val mapView = LocalView.current

    // ط·آ§ط¸â€‍ط·آ§ط·آ³ط·ع¾ط¸â€¦ط·آ§ط·آ¹ ط¸â€‍ط·ع¾ط·ط›ط¸ظ¹ط¸ظ¹ط·آ±ط·آ§ط·ع¾ ط·آ§ط¸â€‍ط·آ¥ط·آ¹ط·آ¯ط·آ§ط·آ¯ط·آ§ط·ع¾ ط¸ث†ط·ع¾ط·آ·ط·آ¨ط¸ظ¹ط¸â€ڑط¸â€،ط·آ§ ط¸ظ¾ط¸ث†ط·آ±ط·آ§ط¸â€¹
    var prefsTick by remember { mutableIntStateOf(0) }
    var unitSysTick by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "unit_system") unitSysTick++
            if (key in setOf("default_map_type", "default_coord_format", "keep_screen_on", "unit_system", "measure_symbols")) prefsTick++
        }
        prefs.registerListener(listener)
        onDispose { prefs.unregisterListener(listener) }
    }
    DisposableEffect(prefsTick, prefs.keepScreenOn) { mapView.keepScreenOn = prefs.keepScreenOn; onDispose { } }

    var center by remember { mutableStateOf<IGeoPoint>(GeoPoint(24.7136, 46.6753)) }
    var zoom by remember { mutableDoubleStateOf(15.0) }; var rot by remember { mutableDoubleStateOf(0.0) }
    var coordFmt by remember { mutableStateOf(prefs.defaultCoordFormat.uppercase()) }
    LaunchedEffect(prefsTick, prefs.defaultCoordFormat) { coordFmt = prefs.defaultCoordFormat.uppercase() }
    var tileKey by remember { mutableStateOf(
        if (tileSources.any { it.key == prefs.defaultMapType }) prefs.defaultMapType else defaultTileKey
    ) }
    var labelOv by remember { mutableStateOf<org.osmdroid.views.overlay.TilesOverlay?>(null) }
    LaunchedEffect(prefsTick, prefs.defaultMapType) { tileKey = prefs.defaultMapType.let { if (tileSources.any { k -> k.key == it }) it else defaultTileKey } }
    LaunchedEffect(tileKey) {
        prefs.lastMapType = tileKey
        if (!prefs.showBasemap) return@LaunchedEffect
        val def = tileSources.find { it.key == tileKey } ?: return@LaunchedEffect
        mv?.let { v ->
            try { v.setTileSource(def.source) } catch (e: Exception) { AppLogger.e(AppLogger.Tags.MAP, "tile src fail", e); try { v.setTileSource(TileSourceFactory.MAPNIK) } catch (_: Exception) {} }
            if (tileKey == "satellite") {
                if (labelOv == null) {
                    val tp = org.osmdroid.tileprovider.MapTileProviderBasic(ctx, ESRI_LABELS)
                    labelOv = org.osmdroid.views.overlay.TilesOverlay(tp, ctx).also {
                        v.overlays.add(1, it)
                    }
                } else if (labelOv !in v.overlays) {
                    v.overlays.add(1, labelOv)
                }
            } else {
                labelOv?.let { v.overlays.remove(it) }
            }
            v.invalidate()
        }
        if (def.styleUrl != null) {
            mlMap?.setStyle(def.styleUrl)
            mlMv?.visibility = android.view.View.VISIBLE
        } else {
            mlMap?.setStyle(org.maplibre.android.maps.Style.Builder().fromJson("{\"version\":8,\"sources\":{},\"layers\":[]}"))
            mlMv?.visibility = android.view.View.GONE
        }
    }
    LaunchedEffect(prefsTick, prefs.showBasemap) {
        mv?.let { v ->
            if (prefs.showBasemap) {
                val def = tileSources.find { it.key == tileKey } ?: return@let
                try { v.setTileSource(def.source) } catch (e: Exception) { AppLogger.e(AppLogger.Tags.MAP, "tile src fail", e); try { v.setTileSource(TileSourceFactory.MAPNIK) } catch (_: Exception) {} }
                if (tileKey == "satellite" && labelOv != null && labelOv !in v.overlays) {
                    v.overlays.add(1, labelOv)
                } else if (tileKey != "satellite") {
                    labelOv?.let { v.overlays.remove(it) }
                }
                if (def.styleUrl != null) mlMap?.setStyle(def.styleUrl)
                mlMv?.visibility = android.view.View.VISIBLE
            } else {
                v.setTileSource(EMPTY_TILE_SOURCE)
                labelOv?.let { v.overlays.remove(it) }
                mlMv?.visibility = android.view.View.GONE
            }
            v.invalidate()
        }
    }
    var showMapTypeMenu by remember { mutableStateOf(false) }
    var showCoordPicker by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(listOf<Triple<String, Double, Double>>()) }
    var searchQ by remember { mutableStateOf("") }; var showSearch by remember { mutableStateOf(false) }
    var loadingTiles by remember { mutableStateOf(false) }

    var locOk by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { p -> locOk = p[Manifest.permission.ACCESS_FINE_LOCATION] == true || p[Manifest.permission.ACCESS_COARSE_LOCATION] == true }
    // ── GPS: إنشاء overlay للموقع مرة واحدة ──
    var locOv by remember { mutableStateOf<MyLocationNewOverlay?>(null) }; var locOn by remember { mutableStateOf(false) }
    fun ensureLocOv(): MyLocationNewOverlay? {
        val v = mv ?: return null
        if (locOv == null) {
            locOv = MyLocationNewOverlay(GpsMyLocationProvider(ctx), v).also { ov ->
                blueDot(v)?.let {
                    ov.setPersonIcon(it)
                    ov.setDirectionIcon(it)
                }
                ov.setPersonAnchor(0.5f, 0.5f)
                ov.setDirectionAnchor(0.5f, 0.5f)
                ov.setDrawAccuracyEnabled(true)
                v.overlays.add(0, ov); v.invalidate()
            }
        }
        return locOv
    }
    var gpsPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var gpsZoomed by remember { mutableStateOf(false) }
    LaunchedEffect(locOn) {
        if (!locOn) { gpsPoint = null; gpsZoomed = false; return@LaunchedEffect }
        GpsService.observeLocation(ctx).collect { state ->
            state.location?.let { loc ->
                gpsPoint = GeoPoint(loc.latitude, loc.longitude)
            }
        }
    }
    LaunchedEffect(gpsPoint, locOn) {
        if (locOn && gpsPoint != null && !gpsZoomed) {
            mv?.controller?.animateTo(gpsPoint!!, 18.0, 800L)
            gpsZoomed = true
        }
    }

    var mm by remember { mutableStateOf(MeasureMode.NONE) }
    var mPts by remember { mutableStateOf(listOf<GeoPoint>()) }; var mRes by remember { mutableStateOf<String?>(null) }; var mDone by remember { mutableStateOf(false) }
    var mRedoStack = remember { mutableStateListOf<List<GeoPoint>>() }
    var mRedone by remember { mutableStateOf<List<GeoPoint>?>(null) }
    var addPointTrigger by remember { mutableIntStateOf(0) }
    var mDistUnit by remember { mutableStateOf(if (imp) "mi" else "m") }
    var mAreaUnit by remember { mutableStateOf(if (imp) "ac" else "m2") }
    var mCircleCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var mCircleEdge by remember { mutableStateOf<GeoPoint?>(null) }
    var mCircleRadius by remember { mutableDoubleStateOf(0.0) }
    var mCircleResult by remember { mutableStateOf<String?>(null) }
    var mEllipseCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var mEllipseMajor by remember { mutableStateOf<GeoPoint?>(null) }
    var mEllipseMinor by remember { mutableStateOf<GeoPoint?>(null) }
    var mEllipseResult by remember { mutableStateOf<String?>(null) }
    var mSelectTitle by remember { mutableStateOf<String?>(null) }
    var mSelectInfo by remember { mutableStateOf<String?>(null) }
    var mSelectRawData by remember { mutableStateOf<SelectRawData?>(null) }
    var mCoordPoints by remember { mutableStateOf<List<String>>(emptyList()) }
    var mCoordPositions by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var snappedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var elevation by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(unitSysTick, prefs.unitSystem) { mDistUnit = if (prefs.unitSystem == "imperial") "mi" else "m"; mAreaUnit = if (prefs.unitSystem == "imperial") "ac" else "m2" }
    LaunchedEffect(prefsTick) { MsrLabels.useSymbols = prefs.measureSymbols == "symbol" }

    var showFi by remember { mutableStateOf(false) }; var fiId by remember { mutableStateOf("") }; var fiLayerId by remember { mutableStateOf("") }; var fiFeatureProps by remember { mutableStateOf(mapOf<String, String>()) }
    var showLayerPicker by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<GeoPhoto?>(null) }
    var showGeoPhotos by remember { mutableStateOf(true) }

    // Recalculate display strings when units change (real-time unit switching)
    LaunchedEffect(mDistUnit, mAreaUnit) {
        when (mm) {
            MeasureMode.DISTANCE -> mRes = calcResult(mPts, mm, mDistUnit, mAreaUnit)
            MeasureMode.AREA -> if (mPts.size >= 3) mRes = calcAreaResult(mPts, mDistUnit, mAreaUnit)
            MeasureMode.CIRCLE -> if (mCircleRadius > 0) mCircleResult = calcCircleResult(mCircleRadius, mDistUnit, mAreaUnit)
            MeasureMode.ELLIPSE -> mEllipseResult = calcEllipseResult(mEllipseCenter, mEllipseMajor, mEllipseMinor, mDistUnit, mAreaUnit)
            else -> {}
        }
    }
            LaunchedEffect(mSelectRawData, mDistUnit, mAreaUnit, coordFmt) {
                mSelectRawData?.let { raw ->
                    val du = mDistUnit; val au = mAreaUnit; val cf = coordFmt
                    mSelectInfo = when (raw.modeName) {
                        "AREA", "Polygon", "MultiPolygon" -> calcAreaResult(raw.points, du, au)
                        "CIRCLE" -> calcCircleResult(raw.radius ?: 0.0, du, au)
                        "Point", "MultiPoint" -> if (raw.points.isNotEmpty()) {
                            val pt = raw.points[0]
                            val (lat, lon) = fmtCoord(pt.latitude, pt.longitude, cf)
                            val utm = toUtm(pt.latitude, pt.longitude)
                            when (cf) {
                                "DMS" -> "$lat, $lon"
                                "UTM" -> "${utm.zone}${utm.hemisphere} ${"%.2f".fmt(utm.easting)} E ${"%.2f".fmt(utm.northing)} N"
                                else -> "$lat, $lon"
                            }
                        } else ""
                        else -> calcResult(raw.points, MeasureMode.DISTANCE, du, au) ?: ""
                    }
                }
            }

    // ظˆط§ط¬ظ‡ط© ط§ظ„طھط·ط¨ظٹظ‚ ط§ظ„ط±ط¦ظٹط³ظٹط©: ط´ط±ظٹط· ط¹ظ„ظˆظٹطŒ ط´ط±ظٹط· ط³ظپظ„ظٹطŒ ظˆظ…ط­طھظˆظ‰ ط§ظ„ط®ط±ظٹط·ط©
    val snackHost = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    val copiedStr = stringResource(R.string.copied)

    // مراقبة التخزين المؤقت: فحص الحجم كل 30 ثانية وإشعار عند الحذف التلقائي
    var lastCacheBytes by remember { mutableLongStateOf(-1L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(300_000)
            val dirs = listOf(ctx.cacheDir.resolve("tiles"), ctx.cacheDir.resolve("osmdroid"))
            val cur = withContext(Dispatchers.IO) { try { dirs.sumOf { d -> if (d.exists()) d.walkTopDown().onFail { _, _ -> }.filter { it.isFile }.sumOf { it.length() } else 0L } } catch (_: Exception) { -1L } }
            if (lastCacheBytes >= 0L && cur >= 0L && cur < lastCacheBytes) {
                val diff = lastCacheBytes - cur
                val msg = if (diff < 1024L * 1024L) "تم حذف ${diff / 1024L} KB تلقائياً"
                else java.lang.String.format(java.util.Locale.US, "تم حذف %.2f MB تلقائياً", diff.toDouble() / (1024.0 * 1024.0))
                snackScope.launch { snackHost.showSnackbar(msg) }
            }
            if (cur >= 0L) lastCacheBytes = cur
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.map_screen_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.92f))) },
        bottomBar = { BottomNavBar(BottomNavTab.MAP, onNavigateToTab) }, containerColor = MaterialTheme.colorScheme.surfaceDim,
        snackbarHost = { SnackbarHost(snackHost) }) { pad ->
        Box(modifier = Modifier.fillMaxSize().padding(pad)) {

             // أ¢â€‌â‚¬أ¢â€‌â‚¬ ط·آ®ط·آ±ط¸ظ¹ط·آ·ط·آ© (MapLibre + osmdroid) أ¢â€‌â‚¬أ¢â€‌â‚¬
            AndroidView(factory = { c ->
                android.widget.FrameLayout(c).apply {
                    val osmd = MapView(c).apply {
                        val savedSource = tileSources.find { it.key == prefs.defaultMapType }?.source ?: TileSourceFactory.MAPNIK
                        try { setTileSource(if (prefs.showBasemap) savedSource else EMPTY_TILE_SOURCE) } catch (e: Exception) { AppLogger.e(AppLogger.Tags.MAP, "tile src fail", e); setTileSource(TileSourceFactory.MAPNIK) }; setMultiTouchControls(true); setBuiltInZoomControls(false)
                        setHorizontalMapRepetitionEnabled(true); setVerticalMapRepetitionEnabled(false)
                        val savedLat = prefs.mapCenterLat; val savedLon = prefs.mapCenterLon; val savedZ = prefs.mapZoom
                        controller.setZoom(savedZ); controller.setCenter(GeoPoint(savedLat, savedLon)); minZoomLevel = 3.0; maxZoomLevel = 25.0
                        EnglishScaleBarOverlay(this).apply {
                            setAlignBottom(true); setAlignRight(true); setMaxLength(1.8f)
                        }.let { overlays.add(it) }
                        RotationGestureOverlay(this).let { it.isEnabled = true; overlays.add(it) }
                        addMapListener(object : MapListener {
                            override fun onScroll(e: ScrollEvent): Boolean { zoom = zoomLevelDouble; center = mapCenter; rot = mapOrientation.toDouble(); loadingTiles = true; return true }
                            override fun onZoom(e: ZoomEvent): Boolean { zoom = zoomLevelDouble; center = mapCenter; rot = mapOrientation.toDouble(); loadingTiles = true; return true }
                        })
                        isFocusable = true; requestFocus()
                        setOnKeyListener { _, keyCode, event ->
                            if (event.action == KeyEvent.ACTION_DOWN && (mm == MeasureMode.DISTANCE || mm == MeasureMode.AREA || mm == MeasureMode.CIRCLE || mm == MeasureMode.ELLIPSE) && !mDone &&
                                (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER)) {
                                addPointTrigger++; true
                            } else false
                        }
                        mv = this
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                    addView(osmd)
                    val mmlv = MMapView(c).apply {
                        visibility = if (prefs.showBasemap) android.view.View.VISIBLE else android.view.View.GONE
                        getMapAsync { map ->
                            mlMap = map
                            if (prefs.showBasemap) {
                                val def = tileSources.find { it.key == prefs.defaultMapType }
                                if (def?.styleUrl != null) map.setStyle(def.styleUrl)
                            }
                            map.addOnCameraMoveListener {
                                val cp = map.cameraPosition.target
                                if (cp != null) {
                                    osmd.controller.setZoom(map.cameraPosition.zoom)
                                    osmd.controller.setCenter(GeoPoint(cp.latitude, cp.longitude))
                                }
                            }
                        }
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                    addView(mmlv, 0)
                    mlMv = mmlv
                }
            }, modifier = Modifier.fillMaxSize())

            // أ¢â€‌â‚¬أ¢â€‌â‚¬ ط·آ­ط¸ظ¾ط·آ¸ ط¸â€¦ط¸ث†ط¸â€ڑط·آ¹ ط·آ§ط¸â€‍ط·آ®ط·آ±ط¸ظ¹ط·آ·ط·آ© ط¸â€¦ط·آ¹ debounce (ط¸â€ ط¸ئ’ط·ع¾ط·آ¨ ط·آ¹ط¸â€‍ط¸â€° ط·آ§ط¸â€‍ط¸â€ڑط·آ±ط·آµ ط·آ¨ط·آ¹ط·آ¯ ط·ع¾ط¸ث†ط¸â€ڑط¸ظ¾ ط·آ§ط¸â€‍ط¸â€¦ط·آ³ط·ع¾ط·آ®ط·آ¯ط¸â€¦ 500ms) أ¢â€‌â‚¬أ¢â€‌â‚¬
            LaunchedEffect(Unit) {
                snapshotFlow { Triple(center, zoom, rot) }.collect { (c, z, r) ->
                    kotlinx.coroutines.delay(500)
                    prefs.run { mapCenterLat = c.latitude; mapCenterLon = c.longitude; mapZoom = z }
                }
            }

            // أ¢â€‌â‚¬أ¢â€‌â‚¬ ط·آ¥ط·آ®ط¸ظ¾ط·آ§ط·طŒ ط¸â€¦ط·آ¤ط·آ´ط·آ± ط·ع¾ط·آ­ط¸â€¦ط¸ظ¹ط¸â€‍ ط·آ§ط¸â€‍ط·آ¨ط¸â€‍ط·آ§ط·آ· ط·آ¨ط·آ¹ط·آ¯ 2 ط·آ«ط·آ§ط¸â€ ط¸ظ¹ط·آ© ط¸â€¦ط¸â€  ط·آ¢ط·آ®ط·آ± ط·آ­ط·آ¯ط·آ« أ¢â€‌â‚¬أ¢â€‌â‚¬
            LaunchedEffect(Unit) {
                snapshotFlow { loadingTiles }.collect {
                    if (it) { kotlinx.coroutines.delay(2000); loadingTiles = false }
                }
            }

            // ── جلب الارتفاع (elevation) عند تغيير مركز الخريطة مع debouncing ──
            LaunchedEffect(center.latitude, center.longitude) {
                delay(500)
                elevation = null
                elevation = ElevationService.fetch(center.latitude, center.longitude)
            }

            Box(Modifier.align(Alignment.Center).size(64.dp)) {
                CrosshairCenter()
            }

            // أ¢â€‌â‚¬أ¢â€‌â‚¬ ط·ع¾ط·آ±ط·آ§ط¸ئ’ط·آ¨ط·آ§ط·ع¾ ط·آ§ط¸â€‍ط¸â€ڑط¸ظ¹ط·آ§ط·آ³ (ط·آ±ط·آ³ط¸â€¦ ط·آ§ط¸â€‍ط·آ®ط·آ·ط¸ث†ط·آ· ط¸ث†ط·آ§ط¸â€‍ط¸â€ ط¸â€ڑط·آ§ط·آ· ط·آ¹ط¸â€‍ط¸â€° ط·آ§ط¸â€‍ط·آ®ط·آ±ط¸ظ¹ط·آ·ط·آ©) أ¢â€‌â‚¬أ¢â€‌â‚¬
            MeasureOverlays(mv, mm, mPts, mDone, if (mm == MeasureMode.CIRCLE) mCircleResult else if (mm == MeasureMode.ELLIPSE) mEllipseResult else mRes,
                { mPts = it; mRedoStack.clear() }, { if (mm != MeasureMode.CIRCLE && mm != MeasureMode.ELLIPSE) mRes = it }, mDistUnit, mAreaUnit,
                mCircleCenter, { mCircleCenter = it }, mCircleEdge, { mCircleEdge = it },
                { r -> mCircleRadius = r }, { mCircleResult = calcCircleResult(mCircleRadius, mDistUnit, mAreaUnit); mDone = true },
                snappedPoint ?: center, addPointTrigger, selectActive = (mm == MeasureMode.SELECT),
                onSelectedInfo = { title, info -> mSelectTitle = title; mSelectInfo = info },
                onSelectRawData = { mSelectRawData = it },
                coordPositions = mCoordPositions,
                ellipseCenter = mEllipseCenter, onEllipseCenter = { mEllipseCenter = it },
                ellipseMajor = mEllipseMajor, onEllipseMajor = { mEllipseMajor = it },
                ellipseMinor = mEllipseMinor, onEllipseMinor = { mEllipseMinor = it },
                ellipseResult = mEllipseResult, onEllipseResult = { mEllipseResult = it },
                snappedPoint = snappedPoint,
                drawMode = prefs.drawMode)

            // أ¢â€‌â‚¬أ¢â€‌â‚¬ ط·ع¾ط·آ±ط·آ§ط¸ئ’ط·آ¨ط·آ§ط·ع¾ ط¸â€¦ط·آ¹ط·آ§ط¸â€‍ط¸â€¦ ط·آ§ط¸â€‍ط·آ·ط·آ¨ط¸â€ڑط·آ§ط·ع¾ (ط·آ¹ط·آ±ط·آ¶ ط·آ§ط¸â€‍ط¸â€ ط¸â€ڑط·آ§ط·آ· ط¸ث†ط·آ§ط¸â€‍ط·آ®ط·آ·ط¸ث†ط·آ· ط¸ث†ط·آ§ط¸â€‍ط¸â€¦ط·آ¶ط¸â€‍ط·آ¹ط·آ§ط·ع¾ ط¸â€¦ط¸â€  GeoJSON) أ¢â€‌â‚¬أ¢â€‌â‚¬
            val isMeasuringRef = remember { mutableStateOf(false) }
            isMeasuringRef.value = mm != MeasureMode.NONE && mm != MeasureMode.SELECT && mm != MeasureMode.COORDINATE
            LayerOverlays(mv, viewModel, showGeoPhotos, { l, f ->
                fiId = f.id; fiLayerId = l; fiFeatureProps = f.properties; showFi = true
            }, { photo ->
                selectedPhoto = photo
            }, selectActive = (mm == MeasureMode.SELECT),
                skipFeatureTaps = { isMeasuringRef.value },
                onSelectLayerFeature = { layerId, feature, geoPts ->
                    val geomType = feature.geometryType ?: ""
                    val modeName = when {
                        geomType in arrayOf("Polygon", "MultiPolygon") -> "AREA"
                        geomType in arrayOf("Point", "MultiPoint") -> "Point"
                        else -> "DISTANCE"
                    }
                    val res = when {
                        geomType in arrayOf("Polygon", "MultiPolygon") -> calcAreaResult(geoPts, mDistUnit, mAreaUnit)
                        geomType in arrayOf("Point", "MultiPoint") -> {
                            val (lat, lon) = fmtCoord(geoPts[0].latitude, geoPts[0].longitude, coordFmt)
                            val utm = toUtm(geoPts[0].latitude, geoPts[0].longitude)
                            when (coordFmt) {
                                "DMS" -> "$lat, $lon"
                                "UTM" -> "${utm.zone}${utm.hemisphere} ${"%.2f".fmt(utm.easting)} E ${"%.2f".fmt(utm.northing)} N"
                                else -> "$lat, $lon"
                            }
                        }
                        else -> calcResult(geoPts, MeasureMode.DISTANCE, mDistUnit, mAreaUnit) ?: ""
                    }
                    mSelectTitle = geomType; mSelectInfo = res
                    mSelectRawData = SelectRawData(modeName, geoPts)
                })


            // أ¢â€‌â‚¬أ¢â€‌â‚¬ ط¸â€¦ط·آ¤ط·آ´ط·آ± ط·ع¾ط·آ­ط¸â€¦ط¸ظ¹ط¸â€‍ ط·آ§ط¸â€‍ط·آ¨ط¸â€‍ط·آ§ط·آ· (ط¸ظ¹ط·آ¸ط¸â€،ط·آ± ط·آ¹ط¸â€ ط·آ¯ ط·آ§ط¸â€‍ط·ع¾ط¸â€¦ط·آ±ط¸ظ¹ط·آ± ط·آ£ط¸ث† ط·آ§ط¸â€‍ط·ع¾ط¸ئ’ط·آ¨ط¸ظ¹ط·آ±) أ¢â€‌â‚¬أ¢â€‌â‚¬
            AnimatedVisibility(
                visible = loadingTiles,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f))
            }

            SnapEngine(mv, viewModel, mm, prefs.snapEnabled, prefs.snapDistanceMeters, center, onSnappedPoint = { snappedPoint = it }, measurePoints = mPts, gpsLocation = gpsPoint)

            // ── بطاقة معلومات الخريطة (أعلى يسار/يمين): إحداثيات الخريطة مع تنسيق الصيغة ──
            InfoCard(center.latitude, center.longitude, zoom, coordFmt, elevation = elevation, onSelectFmt = { fmt ->
                coordFmt = fmt; prefs.defaultCoordFormat = fmt.lowercase()
            }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).widthIn(max = 340.dp))

            // أ¢â€‌â‚¬أ¢â€‌â‚¬ ط·آ´ط·آ±ط¸ظ¹ط·آ· ط·آ§ط¸â€‍ط¸â€ڑط¸ظ¹ط·آ§ط·آ³ (ط·آ£ط·آ³ط¸ظ¾ط¸â€‍ ط·آ§ط¸â€‍ط·آ´ط·آ§ط·آ´ط·آ©ط·إ’ ط¸ظ¾ط¸ث†ط¸â€ڑ ط·آ£ط·آ²ط·آ±ط·آ§ط·آ± ط·آ§ط¸â€‍ط·ع¾ط·آ­ط¸ئ’ط¸â€¦ ط·آ§ط¸â€‍ط¸ظ¹ط¸â€¦ط¸â€ ط¸â€°) أ¢â€‌â‚¬أ¢â€‌â‚¬
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 72.dp, bottom = 52.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                visible = mm != MeasureMode.NONE,
                // يدخل من الأسفل ويخرج للأسفل — مناسب لعنصر مثبّت في أسفل الشاشة
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300))
            ) {
                MeasureBar(mm, mPts, if (mm == MeasureMode.CIRCLE) mCircleResult else if (mm == MeasureMode.ELLIPSE) mEllipseResult else mRes, mDone, mDistUnit, mAreaUnit,
                    mCircleCenter, mCircleEdge, mCircleRadius,
                    onUndo = { if (mRedoStack.isNotEmpty()) { val cur = mPts; mPts = mRedoStack.last(); mRedoStack.removeAt(mRedoStack.lastIndex); mRedone = cur } else { val last = mPts.lastOrNull(); if (last != null) { mRedoStack.add(mPts); mPts = mPts.dropLast(1); mRedone = mPts; if (mPts.size < 2) mRes = null } } },
                    onRedo = { if (mRedone != null) { mRedoStack.add(mPts); mPts = mRedone!!; mRedone = null } },
                    canRedo = mRedone != null,
                    onClear = { mPts = emptyList(); mRes = null; mDone = false; mRedoStack.clear(); mRedone = null; mCircleCenter = null; mCircleEdge = null; mCircleRadius = 0.0; mCircleResult = null; mEllipseCenter = null; mEllipseMajor = null; mEllipseMinor = null; mEllipseResult = null; mSelectTitle = null; mSelectInfo = null; mSelectRawData = null; mCoordPoints = emptyList(); mCoordPositions = emptyList() },
                    ellipseCenter = mEllipseCenter, ellipseMajor = mEllipseMajor, ellipseMinor = mEllipseMinor, ellipseResult = mEllipseResult,
                    onComplete = { if (mm == MeasureMode.CIRCLE) { mCircleResult = calcCircleResult(mCircleRadius, mDistUnit, mAreaUnit); mDone = true } else if (mm == MeasureMode.ELLIPSE) { mEllipseResult = calcEllipseResult(mEllipseCenter, mEllipseMajor, mEllipseMinor, mDistUnit, mAreaUnit); mDone = true } else if (mm == MeasureMode.AREA) { mRes = calcAreaResult(mPts, mDistUnit, mAreaUnit); mDone = true } else mDone = true },
                    onUnitChange = { when (mm) {
                        MeasureMode.DISTANCE -> mDistUnit = it
                        MeasureMode.AREA -> { mAreaUnit = it; mDistUnit = distanceUnitForAreaUnit(it) }
                        MeasureMode.CIRCLE -> { mDistUnit = it; mAreaUnit = areaUnitForDistanceUnit(it) }
                        MeasureMode.ELLIPSE -> { mDistUnit = it; mAreaUnit = areaUnitForDistanceUnit(it) }
                        MeasureMode.SELECT -> mDistUnit = it
                        else -> mDistUnit = it
                    } },
                    onAreaUnitChange = { when (mm) {
                        MeasureMode.SELECT -> mAreaUnit = it
                        else -> {}
                    } },
                    onCopy = { (when (mm) {
                        MeasureMode.CIRCLE -> mCircleResult
                        MeasureMode.ELLIPSE -> mEllipseResult
                        else -> mRes
                    })?.let { r ->
                        val clipMgr = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipMgr?.setPrimaryClip(android.content.ClipData.newPlainText("measurement", r))
                        snackScope.launch { snackHost.showSnackbar(copiedStr) }
                    }},
                    onAddPoint = { if (mm == MeasureMode.COORDINATE) {
                        val cf = coordFmt
                        val (latS, lonS) = fmtCoord(center.latitude, center.longitude, cf)
                        val utm = toUtm(center.latitude, center.longitude)
                        mCoordPoints = mCoordPoints + when (cf) {
                            "DMS" -> "$lonS, $latS"
                            "UTM" -> "${utm.zone}${utm.hemisphere} ${"%.3f".fmt(utm.easting)}E ${"%.3f".fmt(utm.northing)}N"
                            else -> "$lonS, $latS"
                        }
                        mCoordPositions = mCoordPositions + GeoPoint(center.latitude, center.longitude)
                    } else addPointTrigger++ },
                    onClose = { mm = MeasureMode.NONE; mSelectTitle = null; mSelectInfo = null; mSelectRawData = null; mCoordPoints = emptyList(); mCoordPositions = emptyList() },
                    coordFmt = coordFmt, onCoordFmtChange = { coordFmt = it; prefs.defaultCoordFormat = it.lowercase() },
                    coordPoints = mCoordPoints,
                    onAddCoord = {
                        val cf = coordFmt
                        val (latS, lonS) = fmtCoord(center.latitude, center.longitude, cf)
                        val utm = toUtm(center.latitude, center.longitude)
                        val text = when (cf) {
                            "DMS" -> "$lonS, $latS"
                            "UTM" -> "${utm.zone}${utm.hemisphere} ${"%.3f".fmt(utm.easting)}E ${"%.3f".fmt(utm.northing)}N"
                            else -> "$lonS, $latS"
                        }
                        mCoordPoints = mCoordPoints + text
                        mCoordPositions = mCoordPositions + GeoPoint(center.latitude, center.longitude)
                    },
                    onCopyCoords = {
                        val clipMgr = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipMgr?.setPrimaryClip(android.content.ClipData.newPlainText("coordinates", mCoordPoints.withIndex().joinToString("\n") { "${it.index+1}: ${it.value}" }))
                        snackScope.launch { snackHost.showSnackbar(copiedStr) }
                    },
                    selectedInfo = mSelectInfo, selectedTitle = mSelectTitle, selectRawData = mSelectRawData,
                    onCopySelected = {
                        val clipMgr = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipMgr?.setPrimaryClip(android.content.ClipData.newPlainText("element", "$mSelectTitle: $mSelectInfo"))
                        snackScope.launch { snackHost.showSnackbar(copiedStr) }
                    },
                    modifier = Modifier  // التموضع والحجم معالَجان من AnimatedVisibility الخارجية
                )
            }

            MapSideToolbar(
                mv = mv, locOk = locOk, locOn = locOn, locOv = locOv, mm = mm,
                tileKey = tileKey, defaultTileKey = defaultTileKey, showGeoPhotos = showGeoPhotos, rot = rot,
                onRequestLocationPermission = { locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                onEnsureLocOv = ::ensureLocOv,
                onToggleLocOn = { locOn = it },
                onResetRotation = {
                    if (rot.roundToInt() == 0) return@MapSideToolbar
                    val v = mv ?: return@MapSideToolbar
                    val startRot = v.mapOrientation
                    val anim = android.animation.ValueAnimator.ofFloat(startRot, 0f).apply {
                        duration = 350
                        interpolator = android.view.animation.DecelerateInterpolator()
                        addUpdateListener { a ->
                            val r = a.animatedValue as Float
                            v.mapOrientation = r; rot = r.toDouble(); v.invalidate()
                        }
                        start()
                    }
                },
                onShowInfo = {
                    val visible = viewModel.layers.filter { it.isVisible }
                    when {
                        visible.isEmpty() -> {}
                        visible.size == 1 -> onNavigateToLayerDetails(visible[0].id, visible[0].name)
                        else -> showLayerPicker = true
                    }
                },
                onToggleGeoPhotos = { showGeoPhotos = !showGeoPhotos },
                onShowMapTypeMenu = { showMapTypeMenu = true },
                onShowSearch = { showSearch = !showSearch },
                onShowBookmarks = { showBookmarks = !showBookmarks },
                onSetMeasureMode = { mode ->
                    mm = mode; mPts = emptyList(); mRes = null; mDone = false
                    mCircleCenter = null; mCircleEdge = null; mCircleRadius = 0.0; mCircleResult = null
                    mEllipseCenter = null; mEllipseMajor = null; mEllipseMinor = null; mEllipseResult = null
                    mSelectTitle = null; mSelectInfo = null; mSelectRawData = null
                    mCoordPoints = emptyList(); mCoordPositions = emptyList()
                },
                ctx = ctx,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 12.dp)
            )

            MapZoomControls(mv = mv, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
     }
    if (showFi) ModalBottomSheet(onDismissRequest = { showFi = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        FiContent(fiId, fiFeatureProps, onNavigateToLayerDetails = {
            val fn = viewModel.layers.firstOrNull { it.id == fiLayerId }?.name ?: fiLayerId
            onNavigateToLayerDetails(fiLayerId, fn)
        })
    }

    LayerPickerDialog(
        show = showLayerPicker,
        layers = viewModel.layers,
        onDismiss = { showLayerPicker = false },
        onNavigateToLayerDetails = onNavigateToLayerDetails
    )

    selectedPhoto?.let { PhotoViewDialog(geoPhoto = it, onDismiss = { selectedPhoto = null }, onNavigateToLocation = {
        mv?.controller?.animateTo(GeoPoint(it.latitude, it.longitude), 17.0, 800L)
    }) }

    MapTypeDialog(
        show = showMapTypeMenu,
        tileKey = tileKey,
        defaultMapType = prefs.defaultMapType,
        mapClassification = prefs.mapClassification,
        showBasemap = prefs.showBasemap,
        mv = mv,
        onDismiss = { showMapTypeMenu = false },
        onSelect = { key -> tileKey = key },
        snackScope = snackScope,
        snackHost = snackHost,
        ctx = ctx
    )

    SearchDialog(
        show = showSearch,
        searchQ = searchQ,
        onSearchQChange = { searchQ = it },
        onDismiss = { showSearch = false },
        layers = viewModel.layers,
        getCachedDetail = { viewModel.getCachedDetail(it) },
        onSelectFeature = { featRow, layerId ->
            showSearch = false
            GeometryParser.parseCentroid(featRow.geometryType, featRow.geometryCoordinates)?.let { centroid ->
                mv?.controller?.animateTo(centroid, 15.0, 1000L)
            }
            fiId = featRow.id; fiLayerId = layerId; fiFeatureProps = featRow.properties; showFi = true
        }
    )

    BookmarksDialog(
        show = showBookmarks,
        bookmarks = bookmarks.map { Bookmark(it.first, it.second, it.third) },
        onDismiss = { showBookmarks = false },
        onSave = { name -> bookmarks = bookmarks + Triple(name, center.latitude, center.longitude) },
        onNavigate = { lat, lon -> mv?.controller?.animateTo(GeoPoint(lat, lon), 15.0, 1000L) },
        onDelete = { i -> bookmarks = bookmarks.toMutableList().also { it.removeAt(i) } }
    )

}
