package com.geovision.mobile.ui.screens.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.geovision.mobile.core.AppLogger
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.geovision.mobile.data.GeometryParser
import com.geovision.mobile.ui.screens.layers.FileType
import com.geovision.mobile.ui.screens.layers.LayerViewModel
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import android.graphics.Color as AColor
import kotlin.math.pow
import kotlin.math.sqrt

private const val SNAP_LOG_TAG = "MeasureSnap"

class SnapOverlay : Overlay() {
    var snappedPoint: android.graphics.Point? = null
    var snapPixel: GeoPoint? = null
    var crosshairPixel: Point? = null
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.parseColor("#FFEF5350"); style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.parseColor("#55EF5350"); style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.parseColor("#CCFFFFFF"); style = Paint.Style.STROKE; strokeWidth = 2.5f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.parseColor("#33FFFFFF"); style = Paint.Style.FILL
    }
    private val anchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.parseColor("#FFEF5350"); style = Paint.Style.FILL
    }
    private val gpsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.parseColor("#FF2196F3"); style = Paint.Style.FILL
    }

    override fun draw(c: Canvas, m: MapView, shadow: Boolean) {
        if (shadow) return
        val sp = snappedPoint ?: return
        val cp = crosshairPixel ?: return
        c.drawLine(cp.x.toFloat(), cp.y.toFloat(), sp.x.toFloat(), sp.y.toFloat(), linePaint)
        c.drawCircle(sp.x.toFloat(), sp.y.toFloat(), 42f, glowPaint)
        c.drawCircle(sp.x.toFloat(), sp.y.toFloat(), 24f, pulsePaint)
        c.drawCircle(sp.x.toFloat(), sp.y.toFloat(), 16f, fillPaint)
        c.drawCircle(sp.x.toFloat(), sp.y.toFloat(), 7f, anchorPaint)
        c.drawCircle(cp.x.toFloat(), cp.y.toFloat(), 12f, fillPaint)
        c.drawCircle(cp.x.toFloat(), cp.y.toFloat(), 6f, pulsePaint)
    }
}

@Composable
fun SnapEngine(
    mv: MapView?,
    viewModel: LayerViewModel,
    mm: MeasureMode,
    snapEnabled: Boolean,
    snapDistanceMeters: Float,
    center: IGeoPoint,
    onSnappedPoint: (GeoPoint?) -> Unit,
    measurePoints: List<GeoPoint> = emptyList(),
    gpsLocation: GeoPoint? = null
) {
    val snapOv = remember { SnapOverlay() }
    val lastSnapPoint = remember { mutableStateOf<GeoPoint?>(null) }
    val layerSnapKey = viewModel.layers.joinToString("|") { "${it.id}:${it.isVisible}:${it.featureCount}" }

    DisposableEffect(mv) {
        val m = mv
        if (m != null) {
            m.overlays.add(snapOv)
            m.invalidate()
        }
        onDispose {
            if (m != null) {
                m.overlays.remove(snapOv)
                m.invalidate()
            }
        }
    }

    LaunchedEffect(mv, snapEnabled, snapDistanceMeters, mm, center.latitude, center.longitude, measurePoints, layerSnapKey, gpsLocation) {
        val m = mv ?: return@LaunchedEffect

        fun clearSnap(reason: String) {
            if (lastSnapPoint.value != null) {
                AppLogger.d(AppLogger.Tags.MAP, "snap cleared reason=$reason")
            }
            lastSnapPoint.value = null
            snapOv.snappedPoint = null
            snapOv.snapPixel = null
            snapOv.crosshairPixel = null
            onSnappedPoint(null)
            m.invalidate()
        }

        if (!snapEnabled || snapDistanceMeters <= 0f || mm == MeasureMode.NONE || mm == MeasureMode.SELECT || mm == MeasureMode.COORDINATE) {
            clearSnap("disabled_or_mode")
            return@LaunchedEffect
        }
        if (!isSnapMode(mm)) {
            clearSnap("not_snap_mode")
            return@LaunchedEffect
        }

        val allPts = mutableListOf<GeoPoint>()
        val proj = m.projection
        for (layer in viewModel.layers) {
            if (!layer.isVisible) continue
            when (layer.fileType) {
                FileType.PHOTO -> {
                    val photos = viewModel.getPhotos(layer.id)
                    allPts.addAll(photos.map { GeoPoint(it.latitude, it.longitude) })
                }
                else -> {
                    val detail = viewModel.getCachedDetail(layer.id)
                    detail?.features?.forEach { feature ->
                        if (feature.geometryType == "Point" || feature.geometryType == "MultiPoint") {
                            val rings = GeometryParser.parseRings(feature.geometryType, feature.geometryCoordinates)
                            rings.forEach { ring -> allPts.addAll(ring) }
                        }
                    }
                }
            }
        }
        allPts.addAll(measurePoints)

        if (gpsLocation != null) {
            allPts.add(gpsLocation)
        }

        if (allPts.isEmpty()) {
            clearSnap("no_candidates")
            return@LaunchedEffect
        }

        val ctr = GeoPoint(center.latitude, center.longitude)
        val cp = Point().also { proj.toPixels(ctr, it) }
        snapOv.crosshairPixel = cp

        val nearest = allPts.minByOrNull { pt ->
            val sp = Point(); proj.toPixels(pt, sp)
            val dx = (sp.x - cp.x).toLong(); val dy = (sp.y - cp.y).toLong()
            dx * dx + dy * dy
        }

        val dLon = 0.01
        val refPt = GeoPoint(center.latitude, center.longitude + dLon)
        val refPx = Point().also { proj.toPixels(refPt, it) }
        val refPxDist = sqrt(((refPx.x - cp.x).toDouble().pow(2) + (refPx.y - cp.y).toDouble().pow(2)))
        val refMeters = gcDist(ctr, refPt)
        if (!refMeters.isFinite() || refMeters <= 0.0 || !refPxDist.isFinite() || refPxDist <= 0.0) {
            clearSnap("invalid_scale")
            return@LaunchedEffect
        }
        val snapPixels = (snapDistanceMeters * refPxDist / refMeters).toInt().coerceAtLeast(1)

        if (nearest != null) {
            val sp = Point(); proj.toPixels(nearest, sp)
            val dx = (sp.x - cp.x).toLong(); val dy = (sp.y - cp.y).toLong()
            if (dx * dx + dy * dy <= (snapPixels.toLong() * snapPixels.toLong())) {
                snapOv.snappedPoint = sp
                snapOv.snapPixel = nearest
                onSnappedPoint(nearest)
                val last = lastSnapPoint.value
                if (last == null || gcDist(last, nearest) > 0.05) {
                    AppLogger.d(AppLogger.Tags.MAP, "snap locked mode=$mm lat=${"%.7f".fmt(nearest.latitude)} lon=${"%.7f".fmt(nearest.longitude)}")
                }
                lastSnapPoint.value = nearest
                m.invalidate()
                return@LaunchedEffect
            }
        }

        clearSnap("outside_distance")
    }
}

private fun isSnapMode(mm: MeasureMode): Boolean = mm == MeasureMode.DISTANCE || mm == MeasureMode.AREA || mm == MeasureMode.CIRCLE || mm == MeasureMode.ELLIPSE
