package com.geovision.mobile.ui.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geovision.mobile.R
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import com.geovision.mobile.ui.screens.layers.Layer
import com.geovision.mobile.ui.screens.layers.LayerDetailInfo
import com.geovision.mobile.ui.screens.layers.fileTypeBgColor
import com.geovision.mobile.ui.screens.layers.fileTypeIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun CrosshairCenter() {
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2; val cy = size.height / 2
        val outerR = 18.dp.toPx()
        val crossLen = 10.dp.toPx()
        val lineW = 2.5.dp.toPx(); val outlineW = 5.dp.toPx()
        val dotR = 3.dp.toPx()

        drawCircle(Color.White.copy(alpha = 0.5f), outerR + outlineW * 0.5f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(outlineW))
        drawCircle(Color.Black.copy(alpha = 0.7f), outerR, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(lineW))

        val innerGap = 3.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.6f), Offset(cx, cy - outerR + innerGap), Offset(cx, cy - crossLen), outlineW, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.6f), Offset(cx, cy + crossLen), Offset(cx, cy + outerR - innerGap), outlineW, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.6f), Offset(cx - outerR + innerGap, cy), Offset(cx - crossLen, cy), outlineW, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.6f), Offset(cx + crossLen, cy), Offset(cx + outerR - innerGap, cy), outlineW, StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = 0.8f), Offset(cx, cy - outerR + innerGap), Offset(cx, cy - crossLen), lineW, StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = 0.8f), Offset(cx, cy + crossLen), Offset(cx, cy + outerR - innerGap), lineW, StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = 0.8f), Offset(cx - outerR + innerGap, cy), Offset(cx - crossLen, cy), lineW, StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = 0.8f), Offset(cx + crossLen, cy), Offset(cx + outerR - innerGap, cy), lineW, StrokeCap.Round)

        drawCircle(Color.White.copy(alpha = 0.8f), dotR, Offset(cx, cy))
        drawCircle(Color.Black.copy(alpha = 0.9f), dotR * 0.6f, Offset(cx, cy))
    }
}

@Composable
fun MapZoomControls(mv: MapView?, modifier: Modifier = Modifier) {
    val arrowsOutPath = "M216,48V96a8,8,0,0,1-16,0V67.31l-42.34,42.35a8,8,0,0,1-11.32-11.32L188.69,56H160a8,8,0,0,1,0-16h48A8,8,0,0,1,216,48ZM98.34,146.34,56,188.69V160a8,8,0,0,0-16,0v48a8,8,0,0,0,8,8H96a8,8,0,0,0,0-16H67.31l42.35-42.34a8,8,0,0,0-11.32-11.32ZM208,152a8,8,0,0,0-8,8v28.69l-42.34-42.35a8,8,0,0,0-11.32,11.32L188.69,200H160a8,8,0,0,0,0,16h48a8,8,0,0,0,8-8V160A8,8,0,0,0,208,152ZM67.31,56H96a8,8,0,0,0,0-16H48a8,8,0,0,0-8,8V96a8,8,0,0,0,16,0V67.31l42.34,42.35a8,8,0,0,0,11.32-11.32Z"
    val arrowsInPath = "M144,104V64a8,8,0,0,1,16,0V84.69l42.34-42.35a8,8,0,0,1,11.32,11.32L171.31,96H192a8,8,0,0,1,0,16H152A8,8,0,0,1,144,104Zm-40,40H64a8,8,0,0,0,0,16H84.69L42.34,202.34a8,8,0,0,0,11.32,11.32L96,171.31V192a8,8,0,0,0,16,0V152A8,8,0,0,0,104,144Zm67.31,16H192a8,8,0,0,0,0-16H152a8,8,0,0,0-8,8v40a8,8,0,0,0,16,0V171.31l42.34,42.35a8,8,0,0,0,11.32-11.32ZM104,56a8,8,0,0,0-8,8V84.69L53.66,42.34A8,8,0,0,0,42.34,53.66L84.69,96H64a8,8,0,0,0,0,16h40a8,8,0,0,0,8-8V64A8,8,0,0,0,104,56Z"
    val arrowsOutIcon = remember(arrowsOutPath) {
        ImageVector.Builder(name = "arrows_out", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 256f, viewportHeight = 256f)
            .addPath(pathData = addPathNodes(arrowsOutPath), fill = androidx.compose.ui.graphics.SolidColor(Color.Black))
            .build()
    }
    val arrowsInIcon = remember(arrowsInPath) {
        ImageVector.Builder(name = "arrows_in", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 256f, viewportHeight = 256f)
            .addPath(pathData = addPathNodes(arrowsInPath), fill = androidx.compose.ui.graphics.SolidColor(Color.Black))
            .build()
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CtrlBtn(onClick = { mv?.let { v -> v.controller.zoomTo((v.zoomLevelDouble - 5.0).coerceAtLeast(v.minZoomLevel.toDouble()), 1200L) } }, icon = arrowsOutIcon, desc = stringResource(R.string.zoom_out_fast), tooltip = stringResource(R.string.zoom_out_fast_tip))
        CtrlBtn(onClick = { mv?.let { v -> v.controller.zoomOut(300L) } }, icon = Icons.Default.Remove, desc = stringResource(R.string.zoom_out), tooltip = stringResource(R.string.zoom_out_tip))
        CtrlBtn(onClick = { mv?.let { v -> v.controller.zoomIn(300L) } }, icon = Icons.Default.Add, desc = stringResource(R.string.zoom_in), tooltip = stringResource(R.string.zoom_in_tip))
        CtrlBtn(onClick = { mv?.let { v -> v.controller.zoomTo((v.zoomLevelDouble + 5.0).coerceAtMost(v.maxZoomLevel.toDouble()), 1200L) } }, icon = arrowsInIcon, desc = stringResource(R.string.zoom_in_fast), tooltip = stringResource(R.string.zoom_in_fast_tip))
    }
}

@Composable
fun MapSideToolbar(
    mv: MapView?,
    locOk: Boolean,
    locOn: Boolean,
    locOv: org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay?,
    mm: MeasureMode,
    tileKey: String,
    defaultTileKey: String,
    showGeoPhotos: Boolean,
    rot: Double,
    onRequestLocationPermission: () -> Unit,
    onEnsureLocOv: () -> org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay?,
    onToggleLocOn: (Boolean) -> Unit,
    onResetRotation: () -> Unit,
    onShowInfo: () -> Unit,
    onToggleGeoPhotos: () -> Unit,
    onShowMapTypeMenu: () -> Unit,
    onShowSearch: () -> Unit,
    onShowBookmarks: () -> Unit,
    onSetMeasureMode: (MeasureMode) -> Unit,
    ctx: android.content.Context,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CtrlBtn(onClick = {
            if (!locOk) { onRequestLocationPermission(); return@CtrlBtn }
            val ov = onEnsureLocOv() ?: return@CtrlBtn
            val mapView = mv ?: return@CtrlBtn
            fun lastKnown(): GeoPoint? {
                val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                val last = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)
                return last?.let { GeoPoint(it.latitude, it.longitude) }
            }
            if (locOn) {
                ov.enableFollowLocation()
                val loc = ov.myLocation ?: lastKnown()
                if (loc != null) mapView.controller.animateTo(loc, 18.0, 800L)
            } else {
                if (ov !in (mapView.overlays ?: emptyList())) mapView.overlays.add(0, ov)
                ov.enableMyLocation(); ov.enableFollowLocation()
                mapView.invalidate(); onToggleLocOn(true)
                val loc = ov.myLocation ?: lastKnown()
                if (loc != null) mapView.controller.animateTo(loc, 18.0, 800L)
            }
        }, onLongClick = {
            locOv?.let { ov ->
                ov.disableFollowLocation(); ov.disableMyLocation()
                mv?.overlays?.remove(ov); mv?.invalidate(); onToggleLocOn(false)
            }
        }, icon = if (locOn) Icons.Default.NearMe else Icons.Default.MyLocation, desc = stringResource(R.string.locate_me), tooltip = stringResource(R.string.locate_me_tip), active = locOn, tooltipSide = true)
        CtrlBtn(onClick = { onResetRotation() }, icon = Icons.Default.Explore, desc = stringResource(R.string.compass), tooltip = stringResource(R.string.compass_tip), badge = if (rot.roundToInt() != 0) "${rot.roundToInt()}\u00B0" else null, iconRotation = rot.toFloat(), tooltipSide = true)
        CtrlBtn(onClick = { onShowInfo() }, icon = Icons.Default.Info, desc = stringResource(R.string.details), tooltip = stringResource(R.string.details_tip), tooltipSide = true)
        CtrlBtn(onClick = { onToggleGeoPhotos() }, icon = Icons.Default.PhotoCamera, desc = stringResource(R.string.photos_tip), tooltip = stringResource(R.string.photos_tip), active = showGeoPhotos, tooltipSide = true)
        HorizontalDivider(Modifier.width(28.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        CtrlBtn(onClick = { onShowMapTypeMenu() }, icon = Icons.Default.Layers, desc = stringResource(R.string.map_type), tooltip = stringResource(R.string.map_type_tip), active = tileKey != defaultTileKey, tooltipSide = true)
        CtrlBtn(onClick = { onShowSearch() }, icon = Icons.Default.Search, desc = stringResource(R.string.search), tooltip = stringResource(R.string.search_tip), active = false, tooltipSide = true)
        CtrlBtn(onClick = { onShowBookmarks() }, icon = Icons.Default.Bookmark, desc = stringResource(R.string.bookmarks), tooltip = stringResource(R.string.bookmarks_tip), tooltipSide = true)
        HorizontalDivider(Modifier.width(28.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        CtrlBtn(onClick = {
            if (mm == MeasureMode.DISTANCE) { onSetMeasureMode(MeasureMode.NONE) }
            else { onSetMeasureMode(MeasureMode.DISTANCE) }
        }, icon = Icons.Default.Timeline, desc = stringResource(R.string.measure_distance_short), tooltip = stringResource(R.string.measure_distance_tip), active = mm == MeasureMode.DISTANCE, tooltipSide = true)
        CtrlBtn(onClick = {
            if (mm == MeasureMode.AREA) { onSetMeasureMode(MeasureMode.NONE) }
            else { onSetMeasureMode(MeasureMode.AREA) }
        }, icon = ParallelogramIcon, desc = stringResource(R.string.measure_area_short), tooltip = stringResource(R.string.measure_area_tip), active = mm == MeasureMode.AREA, tooltipSide = true)
        CtrlBtn(onClick = {
            if (mm == MeasureMode.CIRCLE) { onSetMeasureMode(MeasureMode.NONE) }
            else { onSetMeasureMode(MeasureMode.CIRCLE) }
        }, icon = Icons.Default.RadioButtonUnchecked, desc = stringResource(R.string.measure_circle_short), tooltip = stringResource(R.string.measure_circle_tip), active = mm == MeasureMode.CIRCLE, tooltipSide = true)
        CtrlBtn(onClick = {
            if (mm == MeasureMode.ELLIPSE) { onSetMeasureMode(MeasureMode.NONE) }
            else { onSetMeasureMode(MeasureMode.ELLIPSE) }
        }, icon = EllipseIcon, desc = stringResource(R.string.measure_ellipse_short), tooltip = stringResource(R.string.measure_ellipse_tip), active = mm == MeasureMode.ELLIPSE, tooltipSide = true)
        CtrlBtn(onClick = {
            if (mm == MeasureMode.SELECT) { onSetMeasureMode(MeasureMode.NONE) }
            else { onSetMeasureMode(MeasureMode.SELECT) }
        }, icon = Icons.Default.TouchApp, desc = stringResource(R.string.measure_select_short), tooltip = stringResource(R.string.measure_select_tip), active = mm == MeasureMode.SELECT, tooltipSide = true)
        CtrlBtn(onClick = {
            if (mm == MeasureMode.COORDINATE) { onSetMeasureMode(MeasureMode.NONE) }
            else { onSetMeasureMode(MeasureMode.COORDINATE) }
        }, icon = Icons.Default.Flag, desc = stringResource(R.string.measure_coord_short), tooltip = stringResource(R.string.measure_coord_tip), active = mm == MeasureMode.COORDINATE, tooltipSide = true)
    }
}

data class Bookmark(val name: String, val lat: Double, val lon: Double)

@Composable
fun BookmarksDialog(
    show: Boolean,
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onNavigate: (Double, Double) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (!show) return
    var bmName by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bookmark, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.bookmarks), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700) } },
        text = {
            Column {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = bmName, onValueChange = { bmName = it },
                            placeholder = { Text(stringResource(R.string.bookmark_name)) },
                            modifier = Modifier.weight(1f), singleLine = true,
                            shape = RoundedCornerShape(10.dp))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if (bmName.isNotBlank()) { onSave(bmName); bmName = "" }
                        }, shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.save)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (bookmarks.isEmpty()) Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkBorder, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_search_results), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else bookmarks.forEachIndexed { i, bm ->
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                        onNavigate(bm.lat, bm.lon); onDismiss()
                    }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(bm.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600)
                                Text("${"%.4f".fmt(bm.lat)}, ${"%.4f".fmt(bm.lon)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDelete(i) }) {
                                Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerPickerDialog(
    show: Boolean,
    layers: List<Layer>,
    onDismiss: () -> Unit,
    onNavigateToLayerDetails: (String, String) -> Unit
) {
    if (!show) return
    val visibleLayers = layers.filter { it.isVisible }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            Text(stringResource(R.string.select_layer), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(visibleLayers, key = { it.id }) { layer ->
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onNavigateToLayerDetails(layer.id, layer.name)
                    }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(fileTypeBgColor(layer.fileType, MaterialTheme.colorScheme)), contentAlignment = Alignment.Center) {
                                Icon(fileTypeIcon(layer.fileType), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(layer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(layer.fileType.label + if (layer.featureCount > 0) " \u2022 ${layer.featureCount}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchDialog(
    show: Boolean,
    searchQ: String,
    onSearchQChange: (String) -> Unit,
    onDismiss: () -> Unit,
    layers: List<Layer>,
    getCachedDetail: (String) -> LayerDetailInfo?,
    onSelectFeature: (FeatureRow, String) -> Unit
) {
    if (!show) return
    AlertDialog(onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700) } },
        text = {
            Column {
                OutlinedTextField(value = searchQ, onValueChange = onSearchQChange,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                val results = remember(searchQ, layers) {
                    if (searchQ.isBlank()) emptyList<Triple<String, String, FeatureRow>>()
                    else layers.flatMap { layer ->
                        val det = getCachedDetail(layer.id)
                        if (det == null) emptyList()
                        else det.features.filter { feat ->
                            feat.id.contains(searchQ, ignoreCase = true) ||
                            feat.properties.values.any { it.contains(searchQ, ignoreCase = true) }
                        }.map { Triple(layer.id, layer.name, it) }
                    }.take(20)
                }
                if (searchQ.isNotBlank()) {
                    if (results.isEmpty()) Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_search_results), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("${results.size} found", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        results.forEach { result ->
                        val (layerId, layerName, featRow) = result
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                            onSelectFeature(featRow, layerId)
                        }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PinDrop, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(featRow.id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
                                    Text("$layerName \u2014 ${featRow.geometryType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp)
}

@Composable
fun MapTypeDialog(
    show: Boolean,
    tileKey: String,
    defaultMapType: String,
    mapClassification: String,
    showBasemap: Boolean,
    mv: MapView?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    snackScope: kotlinx.coroutines.CoroutineScope,
    snackHost: SnackbarHostState,
    ctx: android.content.Context
) {
    if (!show) return
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_type), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasemapPickerContent(
                    tileSources = tileSources,
                    selectedKey = tileKey,
                    defaultKey = defaultMapType,
                    classification = mapClassification,
                    onSelect = { key ->
                        onSelect(key); onDismiss()
                        if (showBasemap) { try { mv?.setTileSource(tileSources.find { it.key == key }?.source) } catch (e: Exception) { AppLogger.e(AppLogger.Tags.MAP, "tile src fail", e); try { mv?.setTileSource(TileSourceFactory.MAPNIK) } catch (_: Exception) {}; snackScope.launch { snackHost.showSnackbar(ctx.getString(R.string.tile_failed)) } }; mv?.invalidate() }
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp)
}

data class MeasureState(
    val mode: MeasureMode = MeasureMode.NONE,
    val points: List<GeoPoint> = emptyList(),
    val result: String? = null,
    val done: Boolean = false,
    val distUnit: String = "m",
    val areaUnit: String = "m2",
    val circleCenter: GeoPoint? = null,
    val circleEdge: GeoPoint? = null,
    val circleRadius: Double = 0.0,
    val circleResult: String? = null
)
