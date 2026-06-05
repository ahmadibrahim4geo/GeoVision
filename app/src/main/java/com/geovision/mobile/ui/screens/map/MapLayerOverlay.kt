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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.geovision.mobile.data.GeoPhoto
import com.geovision.mobile.data.GeometryParser
import com.geovision.mobile.data.SpatialIndex
import com.geovision.mobile.data.ViewportFeatureQueryService
import com.geovision.mobile.ui.screens.layers.FileType
import com.geovision.mobile.ui.screens.layers.FeatureRow
import com.geovision.mobile.ui.screens.layers.Layer
import com.geovision.mobile.ui.screens.layers.LayerDetailInfo
import com.geovision.mobile.ui.screens.layers.LayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

private data class PreparedVectorLayer(
    val layerId: String,
    val signature: String,
    val features: List<OptimizedCanvasOverlay.OverlayFeature>,
    val spatialIndex: SpatialIndex<OptimizedCanvasOverlay.OverlayFeature>,
    val points: List<GeoPoint>
)

private fun renderSignature(layer: Layer, detail: LayerDetailInfo): String {
    return listOf(
        layer.id,
        layer.color.value.toString(),
        layer.transparency.toString(),
        layer.pointSize.toString(),
        layer.lineWidth.toString(),
        detail.features.size.toString(),
        System.identityHashCode(detail).toString()
    ).joinToString(":")
}

private fun renderDatabaseSignature(layer: Layer, mapView: MapView): String {
    val bb = mapView.boundingBox
    fun Double.roundForSignature(): String = String.format(java.util.Locale.US, "%.5f", this)
    return listOf(
        "db",
        layer.id,
        layer.color.value.toString(),
        layer.transparency.toString(),
        layer.pointSize.toString(),
        layer.lineWidth.toString(),
        mapView.zoomLevelDouble.toInt().toString(),
        bb.lonWest.roundForSignature(),
        bb.latSouth.roundForSignature(),
        bb.lonEast.roundForSignature(),
        bb.latNorth.roundForSignature()
    ).joinToString(":")
}

private fun currentViewportBbox(mapView: MapView): ViewportFeatureQueryService.BoundingBox {
    val bb = mapView.boundingBox
    return ViewportFeatureQueryService.BoundingBox(
        minX = bb.lonWest,
        minY = bb.latSouth,
        maxX = bb.lonEast,
        maxY = bb.latNorth
    )
}

private suspend fun prepareVectorLayer(
    layer: Layer,
    detail: LayerDetailInfo,
    signature: String,
    onFeatureClick: (String, FeatureRow) -> Unit
): PreparedVectorLayer = withContext(Dispatchers.Default) {
    val overlayFeatures = mutableListOf<OptimizedCanvasOverlay.OverlayFeature>()
    val spatialIndex = SpatialIndex<OptimizedCanvasOverlay.OverlayFeature>()
    val layerPoints = mutableListOf<GeoPoint>()

    detail.features.forEachIndexed { index, feature ->
        if (index % 256 == 0) currentCoroutineContext().ensureActive()

        when (feature.geometryType) {
            "Point", "MultiPoint" -> {
                val rings = GeometryParser.parseRings(feature.geometryType, feature.geometryCoordinates)
                if (rings.isEmpty()) return@forEachIndexed
                val geoPts = listOf(rings.flatten())
                val overlayFeature = OptimizedCanvasOverlay.OverlayFeature(
                    feature,
                    feature.geometryType ?: "Point",
                    geoPts
                ) { onFeatureClick(layer.id, feature) }
                overlayFeatures.add(overlayFeature)
                spatialIndex.insert(overlayFeature, geoPts.flatten())
                rings.forEach { layerPoints.addAll(it) }
            }
            "LineString", "MultiLineString" -> {
                val rings = GeometryParser.parseRings(feature.geometryType, feature.geometryCoordinates)
                if (rings.isEmpty()) return@forEachIndexed
                val overlayFeature = OptimizedCanvasOverlay.OverlayFeature(
                    feature,
                    feature.geometryType ?: "LineString",
                    rings
                ) { onFeatureClick(layer.id, feature) }
                overlayFeatures.add(overlayFeature)
                spatialIndex.insert(overlayFeature, rings.flatten())
                rings.forEach { layerPoints.addAll(it) }
            }
            "Polygon", "MultiPolygon" -> {
                val polyRings = GeometryParser.parsePolygonSets(feature.geometryType, feature.geometryCoordinates)
                if (polyRings.isEmpty()) return@forEachIndexed
                polyRings.forEach { polygonRings ->
                    val overlayFeature = OptimizedCanvasOverlay.OverlayFeature(
                        feature,
                        feature.geometryType ?: "Polygon",
                        polygonRings
                    ) { onFeatureClick(layer.id, feature) }
                    overlayFeatures.add(overlayFeature)
                    spatialIndex.insert(overlayFeature, polygonRings.flatten())
                    polygonRings.forEach { layerPoints.addAll(it) }
                }
            }
        }
    }
    spatialIndex.build()
    AppLogger.d(AppLogger.Tags.MAP, "Prepared ${overlayFeatures.size} overlay features for ${layer.id} on background thread")
    PreparedVectorLayer(layer.id, signature, overlayFeatures, spatialIndex, layerPoints)
}

private fun zoomToPreparedLayer(mapView: MapView, points: List<GeoPoint>) {
    try {
        when {
            points.size >= 2 -> {
                val north = points.maxOf { it.latitude }
                val south = points.minOf { it.latitude }
                val east = points.maxOf { it.longitude }
                val west = points.minOf { it.longitude }
                if (north != south && east != west) {
                    mapView.zoomToBoundingBox(
                        org.osmdroid.util.BoundingBox(north, east, south, west).increaseByScale(1.3f),
                        true
                    )
                }
            }
            points.size == 1 -> mapView.controller.animateTo(points[0], 16.0, 800L)
        }
    } catch (_: Exception) {
    }
}

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
    val vectorOverlays = remember { mutableMapOf<String, Overlay>() }
    val vectorSignatures = remember { mutableMapOf<String, String>() }
    val vectorJobs = remember { mutableMapOf<String, Job>() }
    val vectorPoints = remember { mutableMapOf<String, List<GeoPoint>>() }
    val photoHolders = remember { mutableListOf<PhotoMarkerHolder>() }
    val selectModeState = remember { mutableStateOf(false) }; selectModeState.value = selectActive
    val viewportRefreshTick = remember { mutableStateOf(0L) }

    DisposableEffect(mv) {
        val m = mv ?: return@DisposableEffect onDispose {}
        val listener = object : MapListener {
            override fun onScroll(e: ScrollEvent): Boolean {
                viewportRefreshTick.value = System.nanoTime()
                return false
            }

            override fun onZoom(e: ZoomEvent): Boolean {
                viewportRefreshTick.value = System.nanoTime()
                return false
            }
        }
        m.addMapListener(listener)
        onDispose { m.removeMapListener(listener) }
    }

    LaunchedEffect(mv, showGeoPhotos, onFeatureClick, onPhotoClick) {
        val m = mv ?: return@LaunchedEffect
        snapshotFlow {
            viewModel.requestedZoomLayerId
            viewModel.layerDetailsVersionFlow.value
            viewportRefreshTick.value
            showGeoPhotos
            val layers = viewModel.layers.toList()
            val details = layers.map { it.id to viewModel.getCachedDetail(it.id) }
            layers to details
        }.debounce(200).collect { (layers, _) ->
            val visibleVectorLayers = layers.filter {
                it.isVisible && it.fileType != FileType.PHOTO &&
                    (viewModel.getCachedDetail(it.id) != null || it.filePath?.startsWith("geovision://project/") == true)
            }
            val visibleVectorIds = visibleVectorLayers.map { it.id }.toSet()

            val vectorIdsToRemove = (vectorOverlays.keys + vectorJobs.keys + vectorSignatures.keys + vectorPoints.keys) - visibleVectorIds
            vectorIdsToRemove.forEach { layerId ->
                vectorJobs.remove(layerId)?.cancel()
                vectorOverlays.remove(layerId)?.let { m.overlays.remove(it) }
                vectorSignatures.remove(layerId)
                vectorPoints.remove(layerId)
            }

            val oldHolders = photoHolders.toList()
            photoHolders.clear()
            if (oldHolders.isNotEmpty()) m.overlays.removeAll(oldHolders.map { it.marker })

            layers.forEach { layer ->
                if (!layer.isVisible) return@forEach

                if (layer.fileType == FileType.PHOTO) {
                    if (!showGeoPhotos) return@forEach
                    val photos = viewModel.getPhotos(layer.id)
                    val layerPts = mutableListOf<GeoPoint>(); vectorPoints[layer.id] = layerPts
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
                            m.overlays.add(this)
                            if (count == 1) photoHolders.add(PhotoMarkerHolder(this, cluster[0], lc))
                        }
                    }
                    return@forEach
                }

                val detail = viewModel.getCachedDetail(layer.id)
                val isDatabaseLayer = detail == null && layer.filePath?.startsWith("geovision://project/") == true
                val signature = if (detail != null) renderSignature(layer, detail) else renderDatabaseSignature(layer, m)
                if (vectorSignatures[layer.id] == signature && vectorOverlays.containsKey(layer.id)) return@forEach
                if (vectorJobs[layer.id]?.isActive == true) return@forEach

                vectorJobs[layer.id] = launch {
                    try {
                        val effectiveDetail = detail ?: if (isDatabaseLayer) {
                            val features = viewModel.getFeaturesInViewport(
                                context = m.context,
                                layerId = layer.id,
                                bbox = currentViewportBbox(m),
                                zoom = m.zoomLevelDouble,
                                limit = 1_200,
                                offset = 0
                            )
                            LayerDetailInfo(
                                fileName = layer.name,
                                filePath = layer.filePath ?: "",
                                crs = layer.crs,
                                extent = "",
                                features = features
                            )
                        } else {
                            return@launch
                        }
                        val prepared = prepareVectorLayer(layer, effectiveDetail, signature, onFeatureClick)
                        val currentLayer = viewModel.getLayerById(prepared.layerId)
                        if (currentLayer == null || !currentLayer.isVisible) return@launch
                        val currentDetail = viewModel.getCachedDetail(prepared.layerId)
                        val currentSignature = if (currentDetail != null) renderSignature(currentLayer, currentDetail) else renderDatabaseSignature(currentLayer, m)
                        if (currentSignature != prepared.signature) return@launch

                        val alpha = (currentLayer.color.alpha * currentLayer.transparency * 255).toInt().coerceIn(0, 255)
                        val lc = AColor.argb(alpha, (currentLayer.color.red * 255).toInt(), (currentLayer.color.green * 255).toInt(), (currentLayer.color.blue * 255).toInt())
                        val overlay = OptimizedCanvasOverlay(
                            prepared.layerId,
                            prepared.features,
                            selectModeState,
                            { layId, feature, geoPts -> onSelectLayerFeature(layId, feature, geoPts) },
                            lc,
                            lc,
                            lc,
                            currentLayer.pointSize,
                            currentLayer.lineWidth,
                            skipFeatureTaps = skipFeatureTaps,
                            preparedSpatialIndex = prepared.spatialIndex
                        )

                        vectorOverlays.remove(prepared.layerId)?.let { m.overlays.remove(it) }
                        vectorOverlays[prepared.layerId] = overlay
                        vectorSignatures[prepared.layerId] = prepared.signature
                        vectorPoints[prepared.layerId] = prepared.points
                        m.overlays.add(overlay)

                        if (viewModel.requestedZoomLayerId == prepared.layerId) {
                            zoomToPreparedLayer(m, prepared.points)
                            viewModel.requestedZoomLayerId = null
                        }
                        m.invalidate()
                    } finally {
                        vectorJobs.remove(layer.id)
                    }
                }
            }

            val zoomTarget = viewModel.requestedZoomLayerId
            if (zoomTarget != null) {
                val pts = vectorPoints[zoomTarget]
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

    DisposableEffect(mv) {
        val m = mv ?: return@DisposableEffect onDispose {}
        onDispose {
            vectorJobs.values.forEach { it.cancel() }
            vectorJobs.clear()
            vectorOverlays.values.forEach { m.overlays.remove(it) }
            vectorOverlays.clear()
            vectorSignatures.clear()
            vectorPoints.clear()
            photoHolders.forEach { m.overlays.remove(it.marker) }
            photoHolders.clear()
            m.invalidate()
        }
    }

    val photoScope = rememberCoroutineScope()
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
                                withContext(Dispatchers.Main) { ref.marker.icon = icon }
                            }
                        } catch (_: Exception) {}
                    }
                    withContext(Dispatchers.Main) { m.invalidate() }
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
