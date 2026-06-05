package com.geovision.mobile.ui.screens.map

import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import com.geovision.mobile.core.AppLogger
import android.view.MotionEvent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.geovision.mobile.data.LodManager
import com.geovision.mobile.data.PathPool
import com.geovision.mobile.data.SpatialIndex
import com.geovision.mobile.ui.screens.layers.FeatureRow
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.sqrt

/**
 * نسخة محسّنة من CanvasLayerOverlay مع:
 * - Spatial Indexing (R-Tree) لاستعلام سريع
 * - Viewport Culling (عدم رسم ما هو خارج الشاشة)
 * - LOD (Level of Detail) تبسيط الهندسة حسب الزوم
 * - Object Pooling لإعادة استخدام Path/Paint
 */
class OptimizedCanvasOverlay(
    private val layerId: String,
    private val features: List<OverlayFeature>,
    private val selectMode: MutableState<Boolean> = mutableStateOf(false),
    private val onSelectGeometry: (String, FeatureRow, List<GeoPoint>) -> Unit = { _, _, _ -> },
    private val pointColor: Int,
    private val strokeColor: Int,
    private val fillColor: Int,
    private val pointSize: Float = 8f,
    private val lineWidth: Float = 4f,
    private val skipFeatureTaps: () -> Boolean = { false },
    preparedSpatialIndex: SpatialIndex<OverlayFeature>? = null
) : Overlay() {

    data class OverlayFeature(
        val feature: FeatureRow,
        val geometryType: String,
        val geoPoints: List<List<GeoPoint>>,
        val onClick: () -> Unit
    )

    private val pathPool = PathPool()
    private val spatialIndex = preparedSpatialIndex ?: SpatialIndex<OverlayFeature>().also { index ->
        features.forEach { of ->
            val allPts = of.geoPoints.flatten()
            index.insert(of, allPts)
        }
        index.build()
    }
    private val reusePt = Point()
    private var lastZoomLevel = -1.0
    private var zoomLevel = 0.0

    // Paint objects (mutated but not recreated)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; isAntiAlias = true; isDither = true
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; isAntiAlias = true; isDither = true
    }
    private val ptOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = AColor.WHITE; isAntiAlias = true
    }
    private val ptInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; isAntiAlias = true
    }

    init {
        // بناء الـ Spatial Index
        AppLogger.d(AppLogger.Tags.MAP, "SpatialIndex built: ${spatialIndex.size} entries for layer $layerId")
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        zoomLevel = mapView.zoomLevelDouble
        lastZoomLevel = zoomLevel

        val proj = mapView.projection
        val viewportBounds = mapView.boundingBox
        val lodLevel = LodManager.getLodLevel(zoomLevel)

        // Viewport Culling: استعلم فقط الـ features داخل الشاشة
        val visible = spatialIndex.queryViewport(viewportBounds, null)

        // تقسيم حسب النوع للرسم
        for (entry in visible) {
            val of = entry.data
            val geomType = of.geometryType

            // LOD filter — compute bounds area from index entry for polygon culling
            val boundsArea = if (geomType == "Polygon" || geomType == "MultiPolygon") {
                (entry.maxLat - entry.minLat) * (entry.maxLon - entry.minLon)
            } else 0.0
            if (!LodManager.shouldRender(lodLevel, geomType, boundsArea)) continue

            when {
                geomType in arrayOf("Point", "MultiPoint") -> drawPoints(canvas, proj, of)
                geomType in arrayOf("LineString", "MultiLineString") -> drawPolyline(canvas, proj, of)
                geomType in arrayOf("Polygon", "MultiPolygon") -> drawPolygon(canvas, proj, of)
            }
        }
    }

    private fun drawPoints(canvas: Canvas, proj: org.osmdroid.views.Projection, of: OverlayFeature) {
        for (ring in of.geoPoints) {
            for (pt in ring) {
                proj.toPixels(pt, reusePt)
                canvas.drawCircle(reusePt.x.toFloat(), reusePt.y.toFloat(), pointSize, ptOuterPaint)
                ptInnerPaint.color = pointColor
                canvas.drawCircle(reusePt.x.toFloat(), reusePt.y.toFloat(), pointSize * 0.625f, ptInnerPaint)
            }
        }
    }

    private fun drawPolyline(canvas: Canvas, proj: org.osmdroid.views.Projection, of: OverlayFeature) {
        for (ring in of.geoPoints) {
            if (ring.size < 2) continue
            val path = pathPool.obtainPath()
            proj.toPixels(ring[0], reusePt)
            path.moveTo(reusePt.x.toFloat(), reusePt.y.toFloat())
            for (i in 1 until ring.size) {
                proj.toPixels(ring[i], reusePt)
                path.lineTo(reusePt.x.toFloat(), reusePt.y.toFloat())
            }
            strokePaint.color = strokeColor
            strokePaint.strokeWidth = lineWidth
            canvas.drawPath(path, strokePaint)
            pathPool.recyclePath(path)
        }
    }

    private fun drawPolygon(canvas: Canvas, proj: org.osmdroid.views.Projection, of: OverlayFeature) {
        if (of.geoPoints.isEmpty()) return
        val path = pathPool.obtainPath()
        path.fillType = Path.FillType.EVEN_ODD  // enables polygon holes (inner rings)

        for ((ringIdx, ring) in of.geoPoints.withIndex()) {
            if (ring.size < 3) continue
            proj.toPixels(ring[0], reusePt)
            path.moveTo(reusePt.x.toFloat(), reusePt.y.toFloat())
            for (i in 1 until ring.size) {
                proj.toPixels(ring[i], reusePt)
                path.lineTo(reusePt.x.toFloat(), reusePt.y.toFloat())
            }
            path.close()
        }

        fillPaint.color = fillColor
        canvas.drawPath(path, fillPaint)
        strokePaint.color = strokeColor
        strokePaint.strokeWidth = lineWidth
        canvas.drawPath(path, strokePaint)
        pathPool.recyclePath(path)
    }

    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
        if (e == null || mapView == null) return false
        if (skipFeatureTaps()) return false
        val tx = e.x; val ty = e.y; val thr = 28f
        val proj = mapView.projection

        // Query spatial index for tapped point
        val tappedGeo = proj.fromPixels(tx.toInt(), ty.toInt()) as? GeoPoint ?: return false
        val tapBounds = org.osmdroid.util.BoundingBox(
            tappedGeo.latitude + 0.01, tappedGeo.longitude + 0.01,
            tappedGeo.latitude - 0.01, tappedGeo.longitude - 0.01
        )
        val nearby = spatialIndex.queryViewport(tapBounds, null)

        if (selectMode.value) {
            for (entry in nearby) {
                val of = entry.data
                when {
                    of.geometryType in arrayOf("Point", "MultiPoint") -> {
                        for (ring in of.geoPoints) {
                            for (pt in ring) {
                                proj.toPixels(pt, reusePt)
                                val dx = tx - reusePt.x; val dy = ty - reusePt.y
                                if (dx * dx + dy * dy <= thr * thr) {
                                    onSelectGeometry(layerId, of.feature, listOf(pt))
                                    return true
                                }
                            }
                        }
                    }
                    of.geometryType in arrayOf("Polygon", "MultiPolygon") -> {
                        for (rings in of.geoPoints) {
                            if (rings.size < 3) continue
                            val pixPts = rings.map { proj.toPixels(it, Point()) }
                            if (pointInPolygon(tx.toInt(), ty.toInt(), pixPts)) {
                                onSelectGeometry(layerId, of.feature, rings)
                                return true
                            }
                        }
                    }
                    of.geometryType in arrayOf("LineString", "MultiLineString") -> {
                        for (ring in of.geoPoints) {
                            if (ring.size < 2) continue
                            val pixPts = ring.map { proj.toPixels(it, Point()) }
                            for (i in 0 until pixPts.size - 1) {
                                val p1 = pixPts[i]; val p2 = pixPts[i + 1]
                                if (distToSegment(tx, ty, p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat()) <= thr) {
                                    onSelectGeometry(layerId, of.feature, ring)
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (entry in nearby) {
                val of = entry.data
                when {
                    of.geometryType in arrayOf("Point", "MultiPoint") -> {
                        for (ring in of.geoPoints) {
                            for (pt in ring) {
                                proj.toPixels(pt, reusePt)
                                val dx = tx - reusePt.x; val dy = ty - reusePt.y
                                if (dx * dx + dy * dy <= thr * thr) { of.onClick(); return true }
                            }
                        }
                    }
                    of.geometryType in arrayOf("Polygon", "MultiPolygon") -> {
                        for (rings in of.geoPoints) {
                            if (rings.size < 3) continue
                            val pixPts = rings.map { proj.toPixels(it, Point()) }
                            if (pointInPolygon(tx.toInt(), ty.toInt(), pixPts)) { of.onClick(); return true }
                        }
                    }
                    of.geometryType in arrayOf("LineString", "MultiLineString") -> {
                        for (ring in of.geoPoints) {
                            if (ring.size < 2) continue
                            val pixPts = ring.map { proj.toPixels(it, Point()) }
                            for (i in 0 until pixPts.size - 1) {
                                val p1 = pixPts[i]; val p2 = pixPts[i + 1]
                                if (distToSegment(tx, ty, p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat()) <= thr) { of.onClick(); return true }
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    private fun pointInPolygon(px: Int, py: Int, poly: List<Point>): Boolean {
        var inside = false; var j = poly.size - 1
        for (i in poly.indices) {
            if ((poly[i].y > py) != (poly[j].y > py) && px < (poly[j].x - poly[i].x) * (py - poly[i].y) / (poly[j].y - poly[i].y) + poly[i].x) inside = !inside
            j = i
        }
        return inside
    }

    private fun distToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax; val dy = by - ay; val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return sqrt((px - ax) * (px - ax) + (py - ay) * (py - ay))
        var t = ((px - ax) * dx + (py - ay) * dy) / lenSq
        t = t.coerceIn(0f, 1f)
        val px2 = ax + t * dx; val py2 = ay + t * dy
        return sqrt((px - px2) * (px - px2) + (py - py2) * (py - py2))
    }
}
