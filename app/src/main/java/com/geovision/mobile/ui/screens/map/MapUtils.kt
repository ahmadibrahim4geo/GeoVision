package com.geovision.mobile.ui.screens.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.Color as AColor
import com.geovision.mobile.data.GeoPhoto
import com.geovision.mobile.data.GeometryParser
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

fun String.fmt(vararg args: Any?): String = java.lang.String.format(Locale.US, this, *args)

/** وضع رموز القياس: true = رموز هندسية (─▶ ■ ◉), false = أحرف (L P A R C) */
object MsrLabels { var useSymbols: Boolean = true }

enum class MeasureMode { NONE, DISTANCE, AREA, CIRCLE, ELLIPSE, SELECT, COORDINATE }

const val R_EARTH = 6_378_137.0
val DIST_COLOR = AColor.parseColor("#2196F3")
val AREA_COLOR = AColor.parseColor("#1565C0")
val AREA_FILL = AColor.parseColor("#BBDEFB")
val CIRCLE_COLOR = AColor.parseColor("#E91E63")
val CIRCLE_FILL = AColor.parseColor("#FCE4EC")
val ELLIPSE_COLOR = AColor.parseColor("#9C27B0")
val ELLIPSE_FILL = AColor.parseColor("#F3E5F5")
val SELECT_COLOR = AColor.parseColor("#FF9800")
val COORD_COLOR = AColor.parseColor("#9C27B0")
val FIRST_COLOR = AColor.parseColor("#4CAF50")
val FIRST_BORDER = AColor.parseColor("#2E7D32")

val ParallelogramIcon = ImageVector.Builder(
    name = "Parallelogram", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        moveTo(4f, 3f); lineTo(22f, 3f); lineTo(20f, 21f); lineTo(2f, 21f); close()
    }
}.build()

val EllipseIcon = ImageVector.Builder(
    name = "Ellipse", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        val a = 2.0 * Math.PI * 0 / 32
        moveTo(12f + 11f * kotlin.math.cos(a).toFloat(), 12f + 5f * kotlin.math.sin(a).toFloat())
        for (i in 1..32) {
            val angle = 2.0 * Math.PI * i / 32
            lineTo(12f + 11f * kotlin.math.cos(angle).toFloat(), 12f + 5f * kotlin.math.sin(angle).toFloat())
        }
        close()
    }
}.build()

val EllipseFillIcon = ImageVector.Builder(
    name = "EllipseFill", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        val a = 2.0 * Math.PI * 0 / 32
        moveTo(12f + 11f * kotlin.math.cos(a).toFloat(), 12f + 5f * kotlin.math.sin(a).toFloat())
        for (i in 1..32) {
            val angle = 2.0 * Math.PI * i / 32
            lineTo(12f + 11f * kotlin.math.cos(angle).toFloat(), 12f + 5f * kotlin.math.sin(angle).toFloat())
        }
        close()
    }
}.build()

fun gcDist(a: GeoPoint, b: GeoPoint): Double {
    val la = Math.toRadians(a.latitude); val lo = Math.toRadians(a.longitude)
    val lb = Math.toRadians(b.latitude); val l2 = Math.toRadians(b.longitude)
    val dLat = lb - la; val dLon = l2 - lo
    val x = sin(dLat / 2).let { it * it + cos(la) * cos(lb) * sin(dLon / 2).let { it * it } }
    return R_EARTH * 2 * atan2(sqrt(x), sqrt((1 - x).coerceAtLeast(0.0)))
}

fun gcArea(pts: List<GeoPoint>): Double {
    if (pts.size < 3) return 0.0; var s = 0.0
    for (i in pts.indices) {
        val j = (i + 1) % pts.size
        val y1 = Math.toRadians(pts[i].latitude); val x1 = Math.toRadians(pts[i].longitude)
        val y2 = Math.toRadians(pts[j].latitude); val x2 = Math.toRadians(pts[j].longitude)
        s += (x2 - x1) * (2 + sin(y1) + sin(y2))
    }
    return abs(s) * R_EARTH * R_EARTH / 2.0
}

fun calcDistanceMeters(pts: List<GeoPoint>): Double {
    var total = 0.0
    for (i in 1 until pts.size) total += gcDist(pts[i - 1], pts[i])
    return total
}

fun calcPerimeterMeters(pts: List<GeoPoint>): Double {
    if (pts.size < 2) return 0.0
    return calcDistanceMeters(pts) + gcDist(pts.last(), pts.first())
}

fun appendMeasurementPoint(
    pts: List<GeoPoint>,
    anchor: GeoPoint,
    minDistanceMeters: Double = MIN_TAP_DIST_M
): List<GeoPoint>? {
    if (pts.isNotEmpty() && gcDist(pts.last(), anchor) < minDistanceMeters) return null
    return pts + anchor
}

fun distanceUnitForAreaUnit(unit: String): String = when (unit) {
    "auto" -> "auto"
    "m2" -> "m"
    "km2" -> "km"
    "ft2" -> "ft"
    "mi2" -> "mi"
    "ac", "ha" -> "m"
    else -> "m"
}

fun areaUnitForDistanceUnit(unit: String): String = when (unit) {
    "auto" -> "auto"
    "m" -> "m2"
    "km" -> "km2"
    "ft" -> "ft2"
    "mi" -> "mi2"
    else -> "m2"
}

fun fmtDist(m: Double, unit: String): String = when (unit) {
    "auto" -> if (m < 1000.0) fmtDist(m, "m") else fmtDist(m, "km")
    "m" -> "${"%.1f".fmt(m)} m"
    "km" -> "${"%.2f".fmt(m / 1000)} km"
    "ft" -> "${"%.1f".fmt(m * 3.28084)} ft"
    "mi" -> "${"%.2f".fmt(m / 1609.344)} mi"
    else -> "${"%.1f".fmt(m)} m"
}

fun fmtArea(sqm: Double, unit: String): String = when (unit) {
    "auto" -> when {
        sqm < 10_000.0 -> fmtArea(sqm, "m2")
        sqm < 1_000_000.0 -> fmtArea(sqm, "ha")
        else -> fmtArea(sqm, "km2")
    }
    "m2" -> "${"%.0f".fmt(sqm)} m\u00B2"
    "ha" -> "${"%.2f".fmt(sqm / 10000)} ha"
    "km2" -> "${"%.2f".fmt(sqm / 1_000_000)} km\u00B2"
    "ft2" -> "${"%.0f".fmt(sqm * 10.7639)} ft\u00B2"
    "ac" -> "${"%.2f".fmt(sqm / 4046.856)} ac"
    "mi2" -> "${"%.2f".fmt(sqm / 2_589_988.11)} mi\u00B2"
    else -> "${"%.0f".fmt(sqm)} m\u00B2"
}

fun lbl(kind: String): String = if (MsrLabels.useSymbols) when (kind) {
    "len" -> "\u2192"; "perim" -> "\u25A1"; "area" -> "\u25A0"
    "rad" -> "\u25C9"; "diam" -> "\u2300"; "circ" -> "\u25CB"; "cArea" -> "\u25CF"
    "ellipse_major_axis" -> "\u2194"; "ellipse_minor_axis" -> "\u2195"; "ellipse_perimeter" -> "\u2B2C"; "ellipse_area" -> "\u2B2D"; "ellipse_angle" -> "\u2220"
    else -> ""
} else when (kind) {
    "len" -> "L"; "perim" -> "P"; "area" -> "A"
    "rad" -> "R"; "diam" -> "D"; "circ" -> "C"; "cArea" -> "A"
    "ellipse_major_axis" -> "a"; "ellipse_minor_axis" -> "b"; "ellipse_perimeter" -> "P"; "ellipse_area" -> "A"; "ellipse_angle" -> "\u03B1"
    else -> ""
}

fun calcResult(pts: List<GeoPoint>, mode: MeasureMode, distUnit: String, areaUnit: String): String? = when (mode) {
    MeasureMode.DISTANCE -> if (pts.size >= 2) "${lbl("len")} = ${fmtDist(calcDistanceMeters(pts), distUnit)}" else null
    MeasureMode.AREA -> if (pts.size >= 3) calcAreaResult(pts, distUnit, areaUnit) else null
    else -> null
}

fun calcAreaResult(pts: List<GeoPoint>, distUnit: String, areaUnit: String): String {
    return "${lbl("area")} = ${fmtArea(gcArea(pts), areaUnit)} \u00B7 ${lbl("perim")} = ${fmtDist(calcPerimeterMeters(pts), distUnit)}"
}

fun calcCircleResult(radius: Double, distUnit: String, areaUnit: String): String {
    val diam = 2.0 * radius
    val circ = 2.0 * Math.PI * radius
    val area = Math.PI * radius * radius
    return "${lbl("cArea")} = ${fmtArea(area, areaUnit)} \u00B7 ${lbl("circ")} = ${fmtDist(circ, distUnit)} \u00B7 ${lbl("diam")} = ${fmtDist(diam, distUnit)} \u00B7 ${lbl("rad")} = ${fmtDist(radius, distUnit)}"
}

fun calcEllipseResult(center: GeoPoint?, major: GeoPoint?, minor: GeoPoint?, distUnit: String, areaUnit: String): String? {
    if (center == null || major == null || minor == null) return null
    val a = gcDist(center, major); val b = ellipseMinorRadius(center, major, minor)
    if (a <= 0 || b <= 0) return null
    val majorAxis = 2.0 * a; val minorAxis = 2.0 * b
    val area = Math.PI * a * b
    val perimeter = Math.PI * (3.0 * (a + b) - sqrt((3.0 * a + b) * (a + 3.0 * b)))
    val bearing = ellipseBearing(center, major)
    return "${lbl("ellipse_area")} = ${fmtArea(area, areaUnit)} \u00B7 ${lbl("ellipse_perimeter")} = ${fmtDist(perimeter, distUnit)} \u00B7 ${lbl("ellipse_major_axis")} = ${fmtDist(majorAxis, distUnit)} \u00B7 ${lbl("ellipse_minor_axis")} = ${fmtDist(minorAxis, distUnit)} \u00B7 ${lbl("ellipse_angle")} = ${"%.1f".fmt(bearing)}\u00B0"
}

fun ellipsePolygon(center: GeoPoint, a: Double, b: Double, bearing: Double = 0.0, segments: Int = 64): List<GeoPoint> {
    val pts = mutableListOf<GeoPoint>()
    val rad = Math.toRadians(bearing)
    val cR = cos(rad); val sR = sin(rad)
    val cLat = cos(Math.toRadians(center.latitude))
    for (i in 0 until segments) {
        val angle = 2.0 * Math.PI * i / segments
        val xM = b * cos(angle)
        val yM = a * sin(angle)
        val rxM = xM * cR + yM * sR
        val ryM = -xM * sR + yM * cR
        val dLat = ryM / R_EARTH
        val dLon = rxM / (R_EARTH * cLat)
        pts.add(GeoPoint(center.latitude + Math.toDegrees(dLat), center.longitude + Math.toDegrees(dLon)))
    }
    return pts
}

fun ellipseBearing(center: GeoPoint, major: GeoPoint): Double {
    val dLat = major.latitude - center.latitude; val dLon = major.longitude - center.longitude
    return Math.toDegrees(atan2(dLon, dLat)).let { if (it < 0) it + 360 else it }
}

fun ellipseMinorRadius(center: GeoPoint, major: GeoPoint, minor: GeoPoint): Double {
    val bMajor = ellipseBearing(center, major)
    val bMinor = ellipseBearing(center, minor)
    val angle = Math.toRadians(abs(bMinor - bMajor).coerceAtMost(180.0))
    return gcDist(center, minor) * abs(sin(angle))
}

fun circleCenterRadius(center: GeoPoint, edge: GeoPoint): Double = gcDist(center, edge)

fun circlePolygon(center: GeoPoint, radius: Double, segments: Int = 64): List<GeoPoint> {
    val pts = mutableListOf<GeoPoint>()
    for (i in 0 until segments) {
        val a = 2.0 * Math.PI * i / segments
        val dLat = Math.toDegrees(radius / R_EARTH)
        val dLon = Math.toDegrees(radius / (R_EARTH * cos(Math.toRadians(center.latitude))))
        pts.add(GeoPoint(center.latitude + dLat * sin(a), center.longitude + dLon * cos(a)))
    }
    return pts
}

fun pointsToGeoJson(pts: List<GeoPoint>, mode: MeasureMode, result: String): String {
    val coords = pts.joinToString(",") { "[${it.longitude},${it.latitude}]" }
    val type = if (mode == MeasureMode.AREA && pts.size >= 3) "Polygon" else "LineString"
    val geom = if (type == "Polygon") """{"type":"Polygon","coordinates":[[$coords,[${pts.first().longitude},${pts.first().latitude}]]}"""
    else """{"type":"LineString","coordinates":[$coords]}"""
    val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date())
    return """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":$geom,"properties":{"measurement":"$result","timestamp":"$ts"}}]}"""
}

fun fmtDD(v: Double, ns: Boolean): String {
    val d = if (v >= 0) "${"%.6f".fmt(abs(v))}\u00B0 ${if (ns) (if (v >= 0) "N" else "S") else (if (v >= 0) "E" else "W")}"
    else "${"%.6f".fmt(abs(v))}\u00B0 ${if (ns) "S" else "W"}"
    return d
}

fun fmtDMS(v: Double, ns: Boolean): String {
    val a = abs(v); val d = a.toInt(); val m = ((a - d) * 60).toInt(); val s = (a - d - m / 60.0) * 3600
    val dir = when { ns -> if (v >= 0) "N" else "S"; else -> if (v >= 0) "E" else "W" }
    return "$d\u00B0$m\u2032${"%.1f".fmt(s)}\u2033 $dir"
}

data class UtmResult(val zone: Int, val hemisphere: String, val easting: Double, val northing: Double)

fun toUtm(lat: Double, lon: Double): UtmResult {
    val zone = ((lon + 180.0) / 6.0).toInt() + 1
    val hem = if (lat >= 0.0) "N" else "S"
    val a = 6378137.0; val f = 1.0 / 298.257223563; val k0 = 0.9996
    val e2 = 2.0 * f - f * f; val e4 = e2 * e2; val e6 = e4 * e2
    val latRad = Math.toRadians(lat); val lonRad = Math.toRadians(lon)
    val lon0Rad = Math.toRadians((zone * 6 - 183).toDouble())
    var dLon = lonRad - lon0Rad
    while (dLon > Math.PI) dLon -= 2.0 * Math.PI
    while (dLon < -Math.PI) dLon += 2.0 * Math.PI
    val sl = sin(latRad); val cl = cos(latRad); val tl = tan(latRad)
    val n = a / sqrt(1.0 - e2 * sl * sl); val t = tl * tl; val c = e2 * cl * cl / (1.0 - e2)
    val aa = cl * dLon; val a2 = aa * aa; val a3 = a2 * aa; val a4 = a2 * a2; val a5 = a4 * aa; val a6 = a3 * a3
    val m = a * ((1.0 - e2/4.0 - 3.0*e4/64.0 - 5.0*e6/256.0) * latRad
        - (3.0*e2/8.0 + 3.0*e4/32.0 + 45.0*e6/1024.0) * sin(2.0 * latRad)
        + (15.0*e4/256.0 + 45.0*e6/1024.0) * sin(4.0 * latRad) - (35.0*e6/3072.0) * sin(6.0 * latRad))
    val easting = k0 * n * (aa + (1.0 - t + c) * a3/6.0 + (5.0 - 18.0*t + t*t + 72.0*c - 58.0*e2) * a5/120.0) + 500000.0
    var northing = k0 * (m + n * tl * (a2/2.0 + (5.0 - t + 9.0*c + 4.0*c*c) * a4/24.0 + (61.0 - 58.0*t + t*t + 600.0*c - 330.0*e2) * a6/720.0))
    if (hem == "S") northing += 10000000.0
    return UtmResult(zone, hem, easting, northing)
}

fun zoomToLayerDetail(m: MapView, detail: com.geovision.mobile.ui.screens.layers.LayerDetailInfo) {
    val allPts = mutableListOf<GeoPoint>()
    detail.features.forEach { feature ->
        val rings = GeometryParser.parseRings(feature.geometryType, feature.geometryCoordinates)
        rings.forEach { ring -> allPts.addAll(ring) }
    }
    if (allPts.isEmpty()) return
    if (allPts.size == 1) { m.controller.animateTo(allPts[0], 16.0, 800L); return }
    try {
        val north = allPts.maxOf { it.latitude }; val south = allPts.minOf { it.latitude }
        val east = allPts.maxOf { it.longitude }; val west = allPts.minOf { it.longitude }
        if (north != south && east != west)
            m.zoomToBoundingBox(org.osmdroid.util.BoundingBox(north, east, south, west).increaseByScale(1.3f), true)
        else m.controller.animateTo(allPts[0], 16.0, 800L)
    } catch (_: Exception) { m.controller.animateTo(allPts[0], 15.0, 800L) }
}

fun fmtCoord(lat: Double, lon: Double, fmt: String): Pair<String, String> = when (fmt) {
    "DMS" -> (fmtDMS(lat, true) to fmtDMS(lon, false))
    else -> (fmtDD(lat, true) to fmtDD(lon, false))
}

fun circleIcon(mv: MapView, r: Int, fill: Int, stroke: Int, sw: Float, ring: Int = 0): BitmapDrawable {
    val s = (r * 2 + ring * 2).coerceAtLeast(8)
    val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(b); val cx = s / 2f; val cy = s / 2f
    if (ring > 0) { c.drawCircle(cx, cy, cx, Paint().apply { color = AColor.WHITE; isAntiAlias = true }) }
    c.drawCircle(cx, cy, r.toFloat(), Paint().apply { color = fill; isAntiAlias = true })
    if (sw > 0) c.drawCircle(cx, cy, r.toFloat(), Paint().apply { color = stroke; style = Paint.Style.STROKE; strokeWidth = sw; isAntiAlias = true })
    return BitmapDrawable(mv.context.resources, b)
}

fun photoMarkerIcon(mv: MapView, photo: GeoPhoto, size: Int, accent: Int): BitmapDrawable {
    val s = size + 4
    val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(b); val cx = s / 2f; val cy = s / 2f; val r = size / 2f
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = AColor.argb(40, 0, 0, 0)
    c.drawCircle(cx + 1.5f, cy + 2f, r + 2f, p)
    p.color = AColor.WHITE; p.style = Paint.Style.FILL
    c.drawCircle(cx, cy, r + 2f, p)
    p.color = accent; p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f
    c.drawCircle(cx, cy, r + 0.5f, p)
    p.style = Paint.Style.FILL; p.color = AColor.argb(220, 255, 255, 255)
    c.drawCircle(cx, cy, r - 3f, p)
    p.color = accent; p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f
    c.drawCircle(cx, cy, r - 3f, p)
    p.style = Paint.Style.FILL; p.color = accent
    c.drawCircle(cx + r * 0.25f, cy - r * 0.25f, r * 0.12f, p)
    if (photo.bearing != null) {
        p.color = AColor.parseColor("#FF5722"); p.style = Paint.Style.FILL
        val arrLen = r * 0.6f; val angle = Math.toRadians(photo.bearing.toDouble())
        val arrX = cx + (Math.sin(angle) * arrLen).toFloat()
        val arrY = cy - (Math.cos(angle) * arrLen).toFloat()
        c.drawCircle(arrX, arrY, r * 0.15f, p)
    }
    return BitmapDrawable(mv.context.resources, b)
}

fun clusterMarkerIcon(mv: MapView, count: Int, accent: Int): BitmapDrawable {
    val s = 72
    val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(b); val cx = s / 2f; val cy = s / 2f
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = AColor.argb(60, 0, 0, 0); c.drawCircle(cx + 2f, cy + 3f, cx - 2f, p)
    p.color = AColor.WHITE; p.style = Paint.Style.FILL; c.drawCircle(cx, cy, cx - 1f, p)
    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = accent; c.drawCircle(cx, cy, cx - 2f, p)
    p.style = Paint.Style.FILL; p.color = accent; c.drawCircle(cx, cy, cx - 7f, p)
    p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.color = AColor.WHITE; c.drawCircle(cx, cy, cx - 10f, p)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    c.drawText("$count", cx, cy + 10f, tp)
    return BitmapDrawable(mv.context.resources, b)
}

fun coordMarkerIcon(mv: MapView, number: Int, accent: Int): BitmapDrawable {
    val s = 72
    val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(b); val cx = s / 2f; val cy = s / 2f
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = AColor.WHITE; c.drawCircle(cx, cy, cx, p)
    p.color = accent; p.style = Paint.Style.FILL; c.drawCircle(cx, cy, 28f, p)
    p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = AColor.argb(80, 255, 255, 255); c.drawCircle(cx, cy, 28f, p)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.WHITE; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        textSize = if (number >= 100) 24f else if (number >= 10) 30f else 36f
    }
    val yOff = -(tp.descent() + tp.ascent()) / 2f
    c.drawText("$number", cx, cy + yOff, tp)
    return BitmapDrawable(mv.context.resources, b)
}

fun blueDot(mv: MapView): Bitmap? {
    return try {
        val d = 52; val glow = 20; val s = d + glow * 2
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(b); val cx = s / 2f; val cy = s / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = android.graphics.RadialGradient(cx, cy, s / 2f,
            intArrayOf(AColor.argb(50, 66, 165, 245), AColor.argb(18, 66, 165, 245), AColor.TRANSPARENT),
            floatArrayOf(0.3f, 0.6f, 1f), android.graphics.Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL; c.drawCircle(cx, cy, s / 2f, p); p.shader = null
        p.color = AColor.argb(30, 0, 0, 0); c.drawCircle(cx + 1f, cy + 2.5f, d / 2f + 1f, p)
        p.color = AColor.WHITE; p.alpha = 230; p.style = Paint.Style.STROKE; p.strokeWidth = 4f
        c.drawCircle(cx, cy, d / 2f + 1f, p)
        p.shader = android.graphics.RadialGradient(cx, cy, d / 2f,
            intArrayOf(AColor.parseColor("#82B1FF"), AColor.parseColor("#448AFF"), AColor.parseColor("#2962FF")),
            floatArrayOf(0.0f, 0.5f, 1f), android.graphics.Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL; p.alpha = 255; c.drawCircle(cx, cy, d / 2f, p); p.shader = null
        p.color = AColor.parseColor("#0039CB"); p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f; p.alpha = 160
        c.drawCircle(cx, cy, d / 2f - 2.5f, p)
        p.color = AColor.WHITE; p.alpha = 200; p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f
        c.drawCircle(cx, cy, 4f, p); p.style = Paint.Style.FILL; p.alpha = 230
        c.drawCircle(cx, cy, 1.5f, p)
        p.style = Paint.Style.FILL; p.color = AColor.argb(150, 255, 255, 255)
        c.drawCircle(cx - d * 0.17f, cy - d * 0.20f, d * 0.13f, p)
        p.color = AColor.argb(90, 255, 255, 255)
        c.drawCircle(cx - d * 0.07f, cy - d * 0.30f, d * 0.06f, p)
        b
    } catch (_: Exception) { null }
}

const val MIN_TAP_DIST_M = 2.0
const val TAP_DEBOUNCE_MS = 120L

class PointsRef { var v: List<GeoPoint> = emptyList() }
