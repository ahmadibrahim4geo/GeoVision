package com.geovision.esri.viewer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.view.MapView
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LARGE_PACKAGE_WARNING_BYTES = 512L * 1024L * 1024L

class EsriViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    EsriViewerRoute()
                }
            }
        }
    }
}

@Composable
private fun EsriViewerRoute() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { EsriViewerStorage(context) }
    val loader = remember { EsriReadOnlyPackageLoader() }
    val reportStore = remember { EsriReportStore(context) }
    var report by remember { mutableStateOf<EsriPackageReport?>(null) }
    var cacheStats by remember { mutableStateOf<EsriCacheStats?>(null) }
    var cacheLimitBytes by remember { mutableStateOf(storage.cacheLimitBytes()) }
    var recentPackages by remember { mutableStateOf(storage.recentPackages()) }
    var map by remember { mutableStateOf<ArcGISMap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingLargePackageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingLargePackageInfo by remember { mutableStateOf<Pair<String, Long>?>(null) }

    suspend fun openLocalFile(file: File) {
        error = null
        val loaded = withContext(Dispatchers.IO) { loader.open(context, file) }
        report = loaded.report
        reportStore.save(loaded.report)
        storage.addRecentPackage(file, loaded.report.packageType)
        recentPackages = storage.recentPackages()
        map = loaded.maps.firstOrNull() ?: loaded.layers.toReadOnlyMap()
        cacheStats = storage.stats()
    }

    suspend fun copyAndOpen(uri: Uri) {
        val displayName = resolveName(context, uri)
        val localFile = withContext(Dispatchers.IO) { storage.copyPackage(uri, displayName) }
        openLocalFile(localFile)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val displayName = resolveName(context, uri)
        val sizeBytes = resolveSize(context, uri)
        if (sizeBytes >= LARGE_PACKAGE_WARNING_BYTES) {
            pendingLargePackageUri = uri
            pendingLargePackageInfo = displayName to sizeBytes
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                copyAndOpen(uri)
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            }
        }
    }

    LaunchedEffect(Unit) {
        cacheStats = storage.stats()
        val incoming = (context as? android.app.Activity)?.intent?.data
        if (incoming != null) {
            try {
                copyAndOpen(incoming)
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (map != null) {
            EsriReadOnlyMapView(map = map!!, modifier = Modifier.fillMaxWidth().height(360.dp))
        } else if (error != null) {
            Box(Modifier.fillMaxWidth().height(120.dp)) {
                Text(error ?: "")
            }
        }
        EsriViewerScreen(
            report = report,
            cacheStats = cacheStats,
            cacheLimitBytes = cacheLimitBytes,
            recentPackages = recentPackages,
            onOpenPackage = {
                picker.launch(
                    arrayOf(
                        "application/vnd.esri.mmpk",
                        "application/x-sqlite3",
                        "application/vnd.sqlite3",
                        "application/octet-stream",
                        "*/*"
                    )
                )
            },
            onOpenRecent = { item ->
                scope.launch {
                    try {
                        openLocalFile(File(item.path))
                    } catch (e: Exception) {
                        error = e.message ?: e.toString()
                    }
                }
            },
            onRemoveRecent = { item ->
                storage.removeRecentPackage(item.path)
                recentPackages = storage.recentPackages()
            },
            onClearCache = {
                storage.clearCache()
                cacheStats = storage.stats()
            },
            onCacheLimitChanged = { limit ->
                storage.saveCacheLimit(limit)
                cacheLimitBytes = limit
            },
            modifier = Modifier.weight(1f)
        )
    }

    val largePackageInfo = pendingLargePackageInfo
    val largePackageUri = pendingLargePackageUri
    if (largePackageInfo != null && largePackageUri != null) {
        AlertDialog(
            onDismissRequest = {
                pendingLargePackageUri = null
                pendingLargePackageInfo = null
            },
            title = { Text(stringResource(R.string.esri_large_package_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.esri_large_package_message,
                        largePackageInfo.first,
                        largePackageInfo.second.toHumanSize()
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uriToOpen = largePackageUri
                        pendingLargePackageUri = null
                        pendingLargePackageInfo = null
                        scope.launch {
                            try {
                                copyAndOpen(uriToOpen)
                            } catch (e: Exception) {
                                error = e.message ?: e.toString()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.esri_copy_and_open))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingLargePackageUri = null
                        pendingLargePackageInfo = null
                    }
                ) {
                    Text(stringResource(R.string.esri_cancel))
                }
            }
        )
    }
}

@Composable
private fun EsriReadOnlyMapView(map: ArcGISMap, modifier: Modifier = Modifier) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mapView: MapView? by remember { mutableStateOf(null) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                this.map = map
                onCreate(lifecycleOwner)
                mapView = this
            }
        },
        update = { view ->
            if (view.map !== map) view.map = map
            mapView = view
        }
    )
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val view = mapView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.onResume(lifecycleOwner)
                Lifecycle.Event.ON_PAUSE -> view.onPause(lifecycleOwner)
                Lifecycle.Event.ON_DESTROY -> view.onDestroy(lifecycleOwner)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy(lifecycleOwner)
            mapView = null
        }
    }
}

private fun List<Layer>.toReadOnlyMap(): ArcGISMap? {
    if (isEmpty()) return null
    val map = ArcGISMap(com.arcgismaps.geometry.SpatialReference.wgs84())
    map.operationalLayers.addAll(this)
    return map
}

private fun resolveName(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }
    } catch (_: Exception) {
        null
    } ?: uri.lastPathSegment ?: "esri_package"
}

private fun resolveSize(context: android.content.Context, uri: Uri): Long {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst() && index >= 0) cursor.getLong(index) else -1L
        } ?: -1L
    } catch (_: Exception) {
        -1L
    }
}

private fun Long.toHumanSize(): String {
    val gb = 1024.0 * 1024.0 * 1024.0
    val mb = 1024.0 * 1024.0
    return if (this >= gb) {
        String.format(java.util.Locale.US, "%.2f GB", this / gb)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", this / mb)
    }
}
