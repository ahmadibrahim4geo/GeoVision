package com.geovision.mobile.ui.screens.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import com.geovision.mobile.core.AppLogger
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.geovision.mobile.data.GeoPhoto
import com.geovision.mobile.data.GeometryParser
import com.geovision.mobile.ui.screens.layers.FileType
import com.geovision.mobile.ui.screens.layers.FeatureRow
import com.geovision.mobile.ui.screens.layers.LayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

// Old CanvasLayerOverlay replaced by OptimizedCanvasOverlay (imported above)

private fun loadPhotoThumbnailSync(ctx: android.content.Context, uri: Uri, targetSize: Int): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val scale = maxOf(opts.outWidth, opts.outHeight) / targetSize
        opts.inSampleSize = scale.coerceAtLeast(1)
        opts.inJustDecodeBounds = false
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) { null }
}

private suspend fun loadPhotoThumbnail(ctx: android.content.Context, uri: Uri, targetSize: Int): Bitmap? = withContext(Dispatchers.IO) {
    loadPhotoThumbnailSync(ctx, uri, targetSize)
}

private fun makeThumbnailDrawable(photoBmp: Bitmap, size: Int, accent: Int, label: String = ""): BitmapDrawable {
    val labelH = (size * 0.18f).toInt().coerceIn(18, 36)
    val pad = 6f; val s = size + pad * 2
    val totalH = s + labelH
    val bmp = Bitmap.createBitmap(s.toInt(), totalH.toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = AColor.argb(50, 0, 0, 0)
    c.drawRoundRect(RectF(3f, 5f, s - 1f, totalH - labelH + 3f), 10f, 10f, p)
    p.color = AColor.WHITE; p.style = Paint.Style.FILL
    c.drawRoundRect(RectF(1f, 1f, s - 1f, totalH - labelH + 1f), 9f, 9f, p)
    val inset = 4f
    val r = RectF(inset, inset, s - inset, totalH - labelH - inset)
    c.drawBitmap(photoBmp, null, r, null)
    p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f; p.color = accent
    c.drawRoundRect(r, 6f, 6f, p)
    p.style = Paint.Style.FILL; p.color = AColor.argb(180, 0, 0, 0)
    c.drawRoundRect(RectF(1f, totalH - labelH + 1f, s - 1f, totalH - 1f), 0f, 0f, p)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.WHITE; textSize = labelH * 0.45f; isAntiAlias = true
    }
    val displayText = if (label.length > 14) label.take(12) + ".." else label
    c.drawText(displayText, pad + 4f, totalH - labelH * 0.28f, tp)
    val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.WHITE; isAntiAlias = true }
    val cx = s - pad - 10f; val cy = pad + 10f
    cp.style = Paint.Style.FILL; cp.color = AColor.argb(160, 0, 0, 0)
    c.drawCircle(cx + 1f, cy + 1f, 11f, cp)
    cp.color = AColor.WHITE; c.drawCircle(cx, cy, 11f, cp)
    cp.style = Paint.Style.STROKE; cp.strokeWidth = 1.5f; cp.color = accent
    c.drawCircle(cx, cy, 11f, cp)
    cp.style = Paint.Style.FILL; cp.color = accent
    c.drawCircle(cx, cy, 6f, cp)
    return BitmapDrawable(null, bmp)
}

private fun thumbSize(zoom: Double): Int = (60 + zoom * 6).toInt().coerceIn(150, 450)

private class PhotoMarkerHolder(val marker: Marker, val photo: GeoPhoto, val accent: Int)

@OptIn(FlowPreview::class)
@Composable
fun LayerOverlays(
    mv: MapView?,
    viewModel: LayerViewModel,
    showGeoPhotos: Boolean = true,
    onFeatureClick: (String, FeatureRow) -> Unit,
    onPhotoClick: (GeoPhoto) -> Unit = {},
    selectActive: Boolean = false,
    onSelectLayerFeature: (String, FeatureRow, List<GeoPoint>) -> Unit = { _, _, _ -> },
    skipFeatureTaps: () -> Boolean = { false }
) {
    val layerOverlays = remember { mutableListOf<Overlay>() }
    val photoHolders = remember { mutableListOf<PhotoMarkerHolder>() }
    val selectModeState = remember { mutableStateOf(false) }; selectModeState.value = selectActive

    LaunchedEffect(mv, showGeoPhotos, onFeatureClick, onPhotoClick) {
        val m = mv ?: return@LaunchedEffect
        snapshotFlow {
            viewModel.requestedZoomLayerId
            viewModel.layerDetailsVersionFlow.value
            showGeoPhotos
            val layers = viewModel.layers.toList()
            val details = layers.map { it.id to viewModel.getCachedDetail(it.id) }
            layers to details
        }.debounce(200).collect { (layers, _) ->
            val oldHolders = photoHolders.toList()
            photoHolders.clear()
            if (oldHolders.isNotEmpty()) m.overlays.removeAll(oldHolders.map { it.marker })
            m.overlays.removeAll(layerOverlays); layerOverlays.clear()
            val layerPointsMap = mutableMapOf<String, MutableList<GeoPoint>>()

            layers.forEach { layer ->
                if (!layer.isVisible) return@forEach

                if (layer.fileType == FileType.PHOTO) {
                    if (!showGeoPhotos) return@forEach
                    val photos = viewModel.getPhotos(layer.id)
                    val layerPts = mutableListOf<GeoPoint>(); layerPointsMap[layer.id] = layerPts
                    val alpha = (layer.color.alpha * layer.transparency * 255).toInt().coerceIn(0, 255)
                    val lc = AColor.argb(alpha, (layer.color.red * 255).toInt(), (layer.color.green * 255).toInt(), (layer.color.blue * 255).toInt())

                    val clusterDist = 0.005
                    val clusters = mutableListOf<MutableList<GeoPhoto>>()
                    photos.forEach { photo ->
                        var added = false
                        for (cluster in clusters) {
                            val first = cluster.first()
                            val dLat = kotlin.math.abs(first.latitude - photo.latitude)
                            val dLon = kotlin.math.abs(first.longitude - photo.longitude)
                            if (dLat < clusterDist && dLon < clusterDist) { cluster.add(photo); added = true; break }
                        }
                        if (!added) clusters.add(mutableListOf(photo))
                    }

                    clusters.forEach { cluster ->
                        val avgLat = cluster.map { it.latitude }.average()
                        val avgLon = cluster.map { it.longitude }.average()
                        val pt = GeoPoint(avgLat, avgLon)
                        layerPts.add(pt)
                        val count = cluster.size
                        val icon = if (count == 1) {
                            val tSize = thumbSize(m.zoomLevelDouble)
                            val bmp = loadPhotoThumbnail(m.context, cluster[0].uri, tSize * 2)
                            if (bmp != null) makeThumbnailDrawable(bmp, tSize, lc, cluster[0].fileName)
                            else photoMarkerIcon(m, cluster[0], tSize, lc)
                        } else clusterMarkerIcon(m, count, lc)

                        Marker(m).apply {
                            position = pt; setAnchor(0.5f, 0.5f); setIcon(icon)
                            title = if (count == 1) cluster[0].fileName else "$count صور"
                            setOnMarkerClickListener { _, _ ->
                                if (count == 1) onPhotoClick(cluster[0])
                                else {
                                    val bounds = org.osmdroid.util.BoundingBox(
                                        cluster.maxOf { it.latitude }, cluster.maxOf { it.longitude },
                                        cluster.minOf { it.latitude }, cluster.minOf { it.longitude }
                                    ).increaseByScale(1.5f)
                                    m.zoomToBoundingBox(bounds, true)
                                }
                                true
                            }
                            layerOverlays.add(this); m.overlays.add(this)
                            if (count == 1) photoHolders.add(PhotoMarkerHolder(this, cluster[0], lc))
                        }
                    }
                    return@forEach
                }

                val detail = viewModel.getCachedDetail(layer.id) ?: return@forEach
                val layerPts = mutableListOf<GeoPoint>(); layerPointsMap[layer.id] = layerPts
                val alpha = (layer.color.alpha * layer.transparency * 255).toInt().coerceIn(0, 255)
                val lc = AColor.argb(alpha, (layer.color.red * 255).toInt(), (layer.color.green * 255).toInt(), (layer.color.blue * 255).toInt())

                val overlayFeatures = mutableListOf<OptimizedCanvasOverlay.OverlayFeature>()

                detail.features.forEach { feature ->
                    val rings = GeometryParser.parseRings(feature.geometryType, feature.geometryCoordinates)
                    if (rings.isEmpty()) {
                        AppLogger.w(AppLogger.Tags.MAP, "Empty rings for feature ${feature.id} type=${feature.geometryType}")
                        return@forEach
                    }
                    val cb = { onFeatureClick(layer.id, feature) }
                    when (feature.geometryType) {
                        "Point", "MultiPoint" -> {
                            val geoPts = listOf(rings.flatten())
                            overlayFeatures.add(OptimizedCanvasOverlay.OverlayFeature(feature, feature.geometryType ?: "Point", geoPts, cb))
                            rings.forEach { ring -> layerPts.addAll(ring) }
                        }
                        "LineString", "MultiLineString" -> {
                            overlayFeatures.add(OptimizedCanvasOverlay.OverlayFeature(feature, feature.geometryType ?: "LineString", rings, cb))
                            rings.forEach { ring -> layerPts.addAll(ring) }
                        }
                        "Polygon", "MultiPolygon" -> {
                            val polyRings = GeometryParser.parsePolygonSets(feature.geometryType, feature.geometryCoordinates)
                            polyRings.forEach { polygonRings ->
                                polygonRings.forEach { ring -> layerPts.addAll(ring) }
                                overlayFeatures.add(OptimizedCanvasOverlay.OverlayFeature(feature, feature.geometryType ?: "Polygon", polygonRings, cb))
                            }
                        }
                    }
                }
                val fillColor = lc
                OptimizedCanvasOverlay(layer.id, overlayFeatures, selectModeState,
                    { layId, feature, geoPts -> onSelectLayerFeature(layId, feature, geoPts) },
                    lc, lc, fillColor, layer.pointSize, layer.lineWidth,
                    skipFeatureTaps = skipFeatureTaps)
                    .let { layerOverlays.add(it); m.overlays.add(it) }
            }

            val zoomTarget = viewModel.requestedZoomLayerId
            if (zoomTarget != null) {
                val pts = layerPointsMap[zoomTarget]
                if (pts != null && pts.size >= 2) {
                    try {
                        val north = pts.maxOf { it.latitude }; val south = pts.minOf { it.latitude }
                        val east = pts.maxOf { it.longitude }; val west = pts.minOf { it.longitude }
                        if (north != south && east != west) m.zoomToBoundingBox(org.osmdroid.util.BoundingBox(north, east, south, west).increaseByScale(1.3f), true)
                    } catch (_: Exception) {}
                } else if (pts != null && pts.size == 1) m.controller.animateTo(pts[0], 16.0, 800L)
                else { val d = viewModel.getCachedDetail(zoomTarget); if (d != null && d.features.isNotEmpty()) zoomToLayerDetail(m, d) }
                viewModel.requestedZoomLayerId = null
            }
            val featureCoords = viewModel.requestedZoomFeatureCoords
            if (featureCoords != null) {
                val (lat, lon) = featureCoords
                try {
                    m.controller.animateTo(GeoPoint(lat, lon), 17.0, 800L)
                } catch (_: Exception) {}
                viewModel.requestedZoomFeatureCoords = null
            }
            m.invalidate()
        }
    }

    val photoScope = remember { CoroutineScope(SupervisorJob()) }
    DisposableEffect(mv, showGeoPhotos) {
        val m = mv ?: return@DisposableEffect onDispose {}
        if (!showGeoPhotos) return@DisposableEffect onDispose {}
        val listener = object : MapListener {
            override fun onScroll(e: ScrollEvent): Boolean = false
            override fun onZoom(e: ZoomEvent): Boolean {
                val refs = photoHolders.toList()
                if (refs.isEmpty()) return false
                val newSize = thumbSize(e.zoomLevel)
                photoScope.launch(Dispatchers.IO) {
                    refs.forEach { ref ->
                        try {
                            val bmp = loadPhotoThumbnailSync(m.context, ref.photo.uri, newSize * 2)
                            if (bmp != null) {
                                val icon = makeThumbnailDrawable(bmp, newSize, ref.accent, ref.photo.fileName)
                                android.os.Handler(m.context.mainLooper).post { ref.marker.icon = icon }
                            }
                        } catch (_: Exception) {}
                    }
                    android.os.Handler(m.context.mainLooper).post { m.invalidate() }
                }
                return false
            }
        }
        m.addMapListener(listener)
        onDispose {
            m.removeMapListener(listener)
        }
    }
}
