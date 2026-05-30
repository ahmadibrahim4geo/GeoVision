package com.geovision.mobile.ui.screens.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Point
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import com.geovision.mobile.core.AppLogger
import android.graphics.Color as AColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geovision.mobile.R
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.roundToInt

val measureAccent: Color get() = Color(0xFF2196F3)
val measureAccentContainer: Color get() = Color(0xFFBBDEFB)

private const val MEASURE_DRAW_TAG = "MeasureDraw"

private fun logMeasureAnchor(mode: MeasureMode, action: String, point: GeoPoint) {
    AppLogger.d(AppLogger.Tags.MAP, "$action mode=$mode lat=${"%.7f".fmt(point.latitude)} lon=${"%.7f".fmt(point.longitude)}")
}

class TapAnimOverlay : Overlay() {
    private data class AP(val pt: GeoPoint, val startMs: Long)
    private val list = mutableListOf<AP>()

    fun animateAt(m: MapView, p: GeoPoint) {
        list.add(AP(p, System.currentTimeMillis()))
        m.postInvalidateDelayed(16)
    }

    override fun draw(c: Canvas, m: MapView, shadow: Boolean) {
        if (shadow) return
        val now = System.currentTimeMillis()
        val d = m.context.resources.displayMetrics.density
        list.removeAll { now - it.startMs > 400L }
        if (list.isEmpty()) return
        val proj = m.projection; val sp = Point()
        for (a in list) {
            val p = (now - a.startMs).toFloat() / 400f
            proj.toPixels(a.pt, sp)
            val r = p * 36f * d
            val al = ((1f - p) * 180).toInt()
            val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(al, 33, 150, 243); style = Paint.Style.STROKE; strokeWidth = 3f * d }
            c.drawCircle(sp.x.toFloat(), sp.y.toFloat(), r, p1)
            val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb((al * 0.4f).toInt().coerceIn(0, 255), 33, 150, 243); style = Paint.Style.FILL }
            c.drawCircle(sp.x.toFloat(), sp.y.toFloat(), r * 0.7f, p2)
        }
        m.postInvalidateDelayed(16)
    }
}

private fun modeAccent(mode: MeasureMode): Color = when (mode) {
    MeasureMode.DISTANCE -> Color(0xFF2196F3)
    MeasureMode.AREA -> Color(0xFF1565C0)
    MeasureMode.CIRCLE -> Color(0xFFE91E63)
    MeasureMode.ELLIPSE -> Color(0xFF9C27B0)
    MeasureMode.SELECT -> Color(0xFFFF9800)
    MeasureMode.COORDINATE -> Color(0xFF9C27B0)
    else -> Color(0xFF2196F3)
}

private fun modeIcon(mode: MeasureMode) = when (mode) {
    MeasureMode.DISTANCE -> Icons.Default.Timeline
    MeasureMode.AREA -> ParallelogramIcon
    MeasureMode.CIRCLE -> Icons.Default.RadioButtonUnchecked
    MeasureMode.ELLIPSE -> EllipseIcon
    MeasureMode.SELECT -> Icons.Default.TouchApp
    MeasureMode.COORDINATE -> Icons.Default.MyLocation
    else -> Icons.Default.Timeline
}

private fun modeNameRes(mode: MeasureMode) = when (mode) {
    MeasureMode.DISTANCE -> R.string.measure_distance_short
    MeasureMode.AREA -> R.string.measure_area_short
    MeasureMode.CIRCLE -> R.string.measure_circle_short
    MeasureMode.ELLIPSE -> R.string.measure_ellipse_short
    MeasureMode.SELECT -> R.string.measure_select_short
    MeasureMode.COORDINATE -> R.string.measure_coord_short
    else -> R.string.measure_distance_short
}

private fun resultParts(result: String): List<String> =
    result.split(" \u00B7 ").map { it.trim() }.filter { it.isNotEmpty() }

private fun primaryResultText(result: String): String =
    resultParts(result).firstOrNull() ?: result

private fun withLenSmaller(text: String, style: androidx.compose.ui.text.TextStyle): AnnotatedString = buildAnnotatedString {
    val lenSym = "\u2192"
    val halfSize = style.fontSize * 0.5f
    var pos = 0
    while (true) {
        val idx = text.indexOf(lenSym, pos)
        if (idx < 0) { append(text.substring(pos)); break }
        append(text.substring(pos, idx))
        withStyle(SpanStyle(fontSize = halfSize)) { append(lenSym) }
        pos = idx + lenSym.length
    }
}

data class SelectRawData(
    val modeName: String,
    val points: List<GeoPoint>,
    val radius: Double? = null,
    val center: GeoPoint? = null
)

@Immutable
data class MeasureUiState(
    val mode: MeasureMode = MeasureMode.DISTANCE,
    val accent: Color = Color(0xFF2196F3),
    val icon: ImageVector = Icons.Default.Timeline,
    val modeName: String = "",
    val mainValue: String = "",
    val unit: String = "",
    val subtitle: String = "",
    val isCompleted: Boolean = false,
    val hasPoints: Boolean = false,
    val pointsCount: Int = 0,
    val progressText: String? = null,
    val instr: String = "",
    val result: String? = null,
    val distUnit: String = "auto",
    val areaUnit: String = "auto",
    val units: List<String> = emptyList(),
    val unitLabels: List<String> = emptyList(),
    val segments: List<MeasureSegment> = emptyList(),
    // Circle
    val circleRadius: Double = 0.0,
    val circleDiameter: String = "",
    val circleCircumference: String = "",
    val circleArea: String = "",
    val circleCenterSet: Boolean = false,
    val circleEdgeSet: Boolean = false,
    // Ellipse
    val ellipseMajorAxis: String = "",
    val ellipseMinorAxis: String = "",
    val ellipsePerimeter: String = "",
    val ellipseArea: String = "",
    val ellipseAngle: String = "",
    val ellipseCenterSet: Boolean = false,
    val ellipseMajorSet: Boolean = false,
    val ellipseMinorSet: Boolean = false,
    // Coordinate
    val coordPoints: List<String> = emptyList(),
    val coordFormats: List<String> = listOf("DD", "DMS", "UTM"),
    val selectedCoordFmt: String = "DD",
    // Select
    val selectedTitle: String? = null,
    val selectedInfo: String? = null,
    val selectRawData: SelectRawData? = null
)

data class MeasureSegment(
    val id: Int,
    val distance: String,
    val bearing: String? = null
)

private fun buildMeasureState(
    mode: MeasureMode,
    pts: List<GeoPoint>,
    res: String?,
    done: Boolean,
    distUnit: String,
    areaUnit: String,
    circleCenter: GeoPoint?,
    circleEdge: GeoPoint?,
    circleRadius: Double,
    coordFmt: String,
    coordPoints: List<String>,
    selectedTitle: String?,
    selectedInfo: String?,
    selectRawData: SelectRawData?,
    ellipseCenter: GeoPoint?,
    ellipseMajor: GeoPoint?,
    ellipseMinor: GeoPoint?,
    ellipseResult: String?,
    subtitle: String = "",
    instr: String = "",
    autoLabel: String = "Auto"
): MeasureUiState {
    val accent = modeAccent(mode)
    val icon = modeIcon(mode)
    val mainResult = primaryResultText(res ?: "")
    val (mainValue, unit) = parseMainValue(mainResult)

    val distUnits = listOf("auto", "m", "km", "ft", "mi")
    val areaUnits = listOf("auto", "m2", "km2", "ft2", "mi2", "ac", "ha")
    val curUnit = when (mode) {
        MeasureMode.AREA -> areaUnit
        else -> distUnit
    }
    val units = when (mode) {
        MeasureMode.DISTANCE, MeasureMode.CIRCLE, MeasureMode.ELLIPSE -> distUnits
        MeasureMode.AREA -> areaUnits
        else -> emptyList()
    }
    val unitLabels = units.map { u ->
        when (u) {
            "auto" -> autoLabel
            "m" -> "m"; "km" -> "km"; "ft" -> "ft"; "mi" -> "mi"
            "m2" -> "m\u00B2"; "km2" -> "km\u00B2"; "ft2" -> "ft\u00B2"; "mi2" -> "mi\u00B2"; "ac" -> "ac"; "ha" -> "ha"
            else -> u
        }
    }

    val progressText = computeProgressText(mode, pts, circleCenter, circleEdge, ellipseCenter, ellipseMajor, ellipseMinor, coordPoints, selectedInfo)

    val segments = if ((mode == MeasureMode.DISTANCE || mode == MeasureMode.AREA) && pts.size >= 2) {
        pts.zipWithNext().mapIndexed { idx, (a, b) ->
            MeasureSegment(
                id = idx + 1,
                distance = fmtDist(gcDist(a, b), distUnit),
                bearing = null
            )
        }
    } else emptyList()

    val circleDiameter = if (circleEdge != null) fmtDist(2.0 * circleRadius, distUnit) else ""
    val circleCircumference = if (circleEdge != null) fmtDist(2.0 * Math.PI * circleRadius, distUnit) else ""
    val circleAreaVal = if (circleEdge != null) fmtArea(Math.PI * circleRadius * circleRadius, areaUnit) else ""

    val ellipseMajorStr: String
    val ellipseMinorStr: String
    val ellipsePerimStr: String
    val ellipseAreaStr: String
    val ellipseAngleStr: String
    if (ellipseCenter != null && ellipseMajor != null && ellipseMinor != null) {
        val a = gcDist(ellipseCenter, ellipseMajor)
        val b = ellipseMinorRadius(ellipseCenter, ellipseMajor, ellipseMinor)
        val area = Math.PI * a * b
        val perim = Math.PI * (3.0 * (a + b) - sqrt((3.0 * a + b) * (a + 3.0 * b)))
        val angle = ellipseBearing(ellipseCenter, ellipseMajor)
        ellipseMajorStr = fmtDist(2.0 * a, distUnit)
        ellipseMinorStr = fmtDist(2.0 * b, distUnit)
        ellipsePerimStr = fmtDist(perim, distUnit)
        ellipseAreaStr = fmtArea(area, areaUnit)
        ellipseAngleStr = "${"%.1f".fmt(angle)}\u00B0"
    } else {
        ellipseMajorStr = ""
        ellipseMinorStr = ""
        ellipsePerimStr = ""
        ellipseAreaStr = ""
        ellipseAngleStr = ""
    }

    return MeasureUiState(
        mode = mode,
        accent = accent,
        icon = icon,
        modeName = "",
        mainValue = mainValue,
        unit = unit,
        subtitle = subtitle,
        isCompleted = done,
        hasPoints = pts.isNotEmpty(),
        pointsCount = pts.size,
        progressText = progressText,
        instr = instr,
        result = res,
        distUnit = distUnit,
        areaUnit = areaUnit,
        units = units,
        unitLabels = unitLabels,
        segments = segments,
        circleRadius = circleRadius,
        circleDiameter = circleDiameter,
        circleCircumference = circleCircumference,
        circleArea = circleAreaVal,
        circleCenterSet = circleCenter != null,
        circleEdgeSet = circleEdge != null,
        ellipseMajorAxis = ellipseMajorStr,
        ellipseMinorAxis = ellipseMinorStr,
        ellipsePerimeter = ellipsePerimStr,
        ellipseArea = ellipseAreaStr,
        ellipseAngle = ellipseAngleStr,
        ellipseCenterSet = ellipseCenter != null,
        ellipseMajorSet = ellipseMajor != null,
        ellipseMinorSet = ellipseMinor != null,
        coordPoints = coordPoints,
        selectedCoordFmt = coordFmt,
        selectedTitle = selectedTitle,
        selectedInfo = selectedInfo,
        selectRawData = selectRawData
    )
}

private fun computeProgressText(
    mode: MeasureMode,
    pts: List<GeoPoint>,
    circleCenter: GeoPoint?,
    circleEdge: GeoPoint?,
    ellipseCenter: GeoPoint?,
    ellipseMajor: GeoPoint?,
    ellipseMinor: GeoPoint?,
    coordPoints: List<String>,
    selectedInfo: String?
): String? = when (mode) {
    MeasureMode.DISTANCE, MeasureMode.AREA -> if (pts.isNotEmpty()) "${pts.size}" else null
    MeasureMode.CIRCLE -> {
        val count = (if (circleCenter != null) 1 else 0) + (if (circleEdge != null) 1 else 0)
        if (count > 0) "$count/2" else null
    }
    MeasureMode.ELLIPSE -> {
        val count = (if (ellipseCenter != null) 1 else 0) + (if (ellipseMajor != null) 1 else 0) + (if (ellipseMinor != null) 1 else 0)
        if (count > 0) "$count/3" else null
    }
    MeasureMode.COORDINATE -> if (coordPoints.isNotEmpty()) "${coordPoints.size}" else null
    MeasureMode.SELECT -> if (selectedInfo != null) "OK" else null
    else -> null
}

private fun parseMainValue(result: String): Pair<String, String> {
    val eqParts = result.split(Regex("\\s*=\\s*"), limit = 2)
    val valuePart = if (eqParts.size == 2) eqParts[1] else eqParts[0]
    val parts = valuePart.split(Regex("\\s+"), limit = 2)
    return if (parts.size == 2) Pair(parts[0], parts[1])
    else if (parts.size == 1) Pair(parts[0], "")
    else Pair("", "")
}

private fun buildSubtitle(mode: MeasureMode, done: Boolean, progressText: String?): String {
    val status = if (done) " ✓" else ""
    val count = progressText?.let { " [$it]" } ?: ""
    return "$count$status"
}

@Composable
private fun buildInstruction(
    mode: MeasureMode,
    done: Boolean,
    circleCenter: GeoPoint?,
    circleEdge: GeoPoint?,
    ellipseCenter: GeoPoint?,
    ellipseMajor: GeoPoint?,
    ellipseMinor: GeoPoint?,
    selectedInfo: String?,
    coordPoints: List<String>
): String {
    return when (mode) {
        MeasureMode.DISTANCE -> if (done) "" else stringResource(R.string.distance_instruction)
        MeasureMode.AREA -> if (done) "" else stringResource(R.string.area_instruction)
        MeasureMode.CIRCLE -> when {
            done -> ""
            circleCenter == null -> stringResource(R.string.circle_instr_1)
            circleEdge == null -> stringResource(R.string.circle_instr_2)
            else -> ""
        }
        MeasureMode.ELLIPSE -> when {
            done -> ""
            ellipseCenter == null -> stringResource(R.string.ellipse_instr_1)
            ellipseMajor == null -> stringResource(R.string.ellipse_instr_2)
            ellipseMinor == null -> stringResource(R.string.ellipse_instr_3)
            else -> ""
        }
        MeasureMode.SELECT -> if (selectedInfo != null) "" else stringResource(R.string.select_instruction)
        MeasureMode.COORDINATE -> if (coordPoints.isEmpty()) stringResource(R.string.coord_instruction) else ""
        else -> ""
    }
}

@Composable
fun MeasureBar(
    mode: MeasureMode,
    pts: List<GeoPoint>,
    res: String?,
    done: Boolean,
    distUnit: String,
    areaUnit: String,
    circleCenter: GeoPoint?,
    circleEdge: GeoPoint?,
    circleRadius: Double,
    onUndo: () -> Unit,
    onRedo: () -> Unit = {},
    canRedo: Boolean = false,
    onClear: () -> Unit,
    onComplete: () -> Unit,
    onUnitChange: (String) -> Unit,
    onCopy: () -> Unit = {},
    onAddPoint: () -> Unit = {},
    onClose: () -> Unit = {},
    coordFmt: String = "DD",
    onCoordFmtChange: (String) -> Unit = {},
    coordPoints: List<String> = emptyList(),
    onCopyCoords: () -> Unit = {},
    onAddCoord: () -> Unit = {},
    selectedInfo: String? = null,
    selectedTitle: String? = null,
    selectRawData: SelectRawData? = null,
    onCopySelected: () -> Unit = {},
    onAreaUnitChange: (String) -> Unit = {},
    ellipseCenter: GeoPoint? = null,
    ellipseMajor: GeoPoint? = null,
    ellipseMinor: GeoPoint? = null,
    ellipseResult: String? = null,
    modifier: Modifier = Modifier
) {
    var collapsed by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showSegments by remember { mutableStateOf(false) }

    val progressText = computeProgressText(mode, pts, circleCenter, circleEdge, ellipseCenter, ellipseMajor, ellipseMinor, coordPoints, selectedInfo)
    val instr = buildInstruction(mode, done, circleCenter, circleEdge, ellipseCenter, ellipseMajor, ellipseMinor, selectedInfo, coordPoints)
    val autoLabel = stringResource(R.string.unit_auto)

    val state = remember(mode, pts, res, done, distUnit, areaUnit, circleCenter, circleEdge, circleRadius,
        coordFmt, coordPoints, selectedTitle, selectedInfo, selectRawData,
        ellipseCenter, ellipseMajor, ellipseMinor, ellipseResult, progressText, instr, autoLabel) {
        val sub = buildSubtitle(mode, done, progressText)
        buildMeasureState(
            mode, pts, res, done, distUnit, areaUnit,
            circleCenter, circleEdge, circleRadius,
            coordFmt, coordPoints,
            selectedTitle, selectedInfo, selectRawData,
            ellipseCenter, ellipseMajor, ellipseMinor, ellipseResult,
            subtitle = sub, instr = instr,
            autoLabel = autoLabel
        )
    }

    MeasureSheet(
        state = state,
        collapsed = collapsed,
        showDetails = showDetails,
        showSegments = showSegments,
        canRedo = canRedo,
        onCollapse = { collapsed = true },
        onExpand = { collapsed = false },
        onToggleDetails = { showDetails = !showDetails },
        onToggleSegments = { showSegments = !showSegments },
        onAddPoint = onAddPoint,
        onAddCoord = onAddCoord,
        onUndo = onUndo,
        onRedo = onRedo,
        onComplete = onComplete,
        onClear = onClear,
        onCopy = onCopy,
        onCopyCoords = onCopyCoords,
        onCopySelected = onCopySelected,
        onClose = onClose,
        onUnitChange = onUnitChange,
        onAreaUnitChange = onAreaUnitChange,
        onCoordFmtChange = onCoordFmtChange,
        modifier = modifier
    )
}

@Composable
private fun MeasureSheet(
    state: MeasureUiState,
    collapsed: Boolean,
    showDetails: Boolean,
    showSegments: Boolean,
    canRedo: Boolean,
    onCollapse: () -> Unit,
    onExpand: () -> Unit,
    onToggleDetails: () -> Unit,
    onToggleSegments: () -> Unit,
    onAddPoint: () -> Unit,
    onAddCoord: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onComplete: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onCopyCoords: () -> Unit,
    onCopySelected: () -> Unit,
    onClose: () -> Unit,
    onUnitChange: (String) -> Unit,
    onAreaUnitChange: (String) -> Unit,
    onCoordFmtChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = collapsed,
        transitionSpec = {
            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
        },
        label = "measure_sheet",
        modifier = modifier
    ) { isCollapsed ->
        if (isCollapsed) {
            CollapsedMeasureBar(
                state = state,
                onAddPoint = onAddPoint,
                onAddCoord = onAddCoord,
                onExpand = onExpand
            )
        } else {
            ExpandedMeasureSheet(
                state = state,
                showDetails = showDetails,
                showSegments = showSegments,
                canRedo = canRedo,
                onCollapse = onCollapse,
                onToggleDetails = onToggleDetails,
                onToggleSegments = onToggleSegments,
                onAddPoint = onAddPoint,
                onAddCoord = onAddCoord,
                onUndo = onUndo,
                onRedo = onRedo,
                onComplete = onComplete,
                onClear = onClear,
                onCopy = onCopy,
                onCopyCoords = onCopyCoords,
                onCopySelected = onCopySelected,
                onClose = onClose,
                onUnitChange = onUnitChange,
                onAreaUnitChange = onAreaUnitChange,
                onCoordFmtChange = onCoordFmtChange
            )
        }
    }
}

@Composable
private fun CollapsedMeasureBar(
    state: MeasureUiState,
    onAddPoint: () -> Unit,
    onAddCoord: () -> Unit,
    onExpand: () -> Unit
) {
    val accent = state.accent
    val canAdd = !state.isCompleted && state.mode in listOf(
        MeasureMode.DISTANCE, MeasureMode.AREA, MeasureMode.CIRCLE,
        MeasureMode.ELLIPSE, MeasureMode.COORDINATE
    )

    val displayValue = when {
        state.mainValue.isNotEmpty() -> state.mainValue
        state.mode == MeasureMode.COORDINATE && state.coordPoints.isNotEmpty() ->
            state.coordPoints.last()
        state.selectedInfo != null -> state.selectedInfo ?: ""
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = state.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "=",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = displayValue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = state.unit,
                        fontSize = 18.sp,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (canAdd && state.mode != MeasureMode.COORDINATE) {
                FilledIconButton(
                    onClick = onAddPoint,
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
            }
            if (canAdd && state.mode == MeasureMode.COORDINATE) {
                FilledIconButton(
                    onClick = onAddCoord,
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
            }

            FilledTonalIconButton(
                onClick = onExpand,
                modifier = Modifier.size(34.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ExpandedMeasureSheet(
    state: MeasureUiState,
    showDetails: Boolean,
    showSegments: Boolean,
    canRedo: Boolean,
    onCollapse: () -> Unit,
    onToggleDetails: () -> Unit,
    onToggleSegments: () -> Unit,
    onAddPoint: () -> Unit,
    onAddCoord: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onComplete: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onCopyCoords: () -> Unit,
    onCopySelected: () -> Unit,
    onClose: () -> Unit,
    onUnitChange: (String) -> Unit,
    onAreaUnitChange: (String) -> Unit,
    onCoordFmtChange: (String) -> Unit
) {
    val accent = state.accent

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent)
            )

            MeasurementHeader(
                state = state,
                onCollapse = onCollapse
            )

            if (state.mode != MeasureMode.CIRCLE && state.mode != MeasureMode.ELLIPSE
                && state.mode != MeasureMode.DISTANCE && state.mode != MeasureMode.AREA
                && state.mode != MeasureMode.SELECT) {
                if (state.result != null || state.mode == MeasureMode.COORDINATE || state.mode == MeasureMode.SELECT) {
                    MeasurementHeroSection(
                        state = state,
                        showDetails = showDetails,
                        onToggleDetails = onToggleDetails
                    )
                }
            }

            when (state.mode) {
                MeasureMode.DISTANCE -> DistanceMeasurementDetails(state, showDetails, onToggleDetails)
                MeasureMode.AREA -> AreaMeasurementDetails(state, showDetails, onToggleDetails)
                MeasureMode.CIRCLE -> CircleMeasurementDetails(state, showDetails, onToggleDetails)
                MeasureMode.ELLIPSE -> EllipseMeasurementDetails(state, showDetails, onToggleDetails)
                MeasureMode.COORDINATE -> CoordinateMeasurementDetails(state, showDetails, onToggleDetails)
                MeasureMode.SELECT -> SelectMeasurementDetails(state, showDetails, onToggleDetails, onCopySelected)
                else -> {}
            }

            if (state.mode == MeasureMode.DISTANCE || state.mode == MeasureMode.AREA) {
            if (state.segments.isNotEmpty()) {
                SegmentDetailToggle(
                    showDetails = showSegments,
                    onToggle = onToggleSegments,
                    segments = state.segments,
                    accent = accent,
                    isArea = state.mode == MeasureMode.AREA
                )
            }
            }

            if (state.instr.isNotBlank()) {
                Text(
                    text = state.instr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.units.isNotEmpty()) {
                MeasurementUnitsSelector(
                    units = state.units,
                    unitLabels = state.unitLabels,
                    selectedUnit = if (state.mode == MeasureMode.AREA) state.areaUnit else state.distUnit,
                    accent = accent,
                    onUnitChange = { u ->
                        if (state.mode == MeasureMode.AREA) onAreaUnitChange(u) else onUnitChange(u)
                    }
                )
            }

            if (state.mode == MeasureMode.COORDINATE || state.mode == MeasureMode.SELECT) {
                CoordFormatSelector(
                    formats = state.coordFormats,
                    selectedFormat = state.selectedCoordFmt,
                    accent = accent,
                    onFormatChange = onCoordFmtChange
                )
            }

            MeasurementActionBar(
                state = state,
                canRedo = canRedo,
                onAddPoint = onAddPoint,
                onAddCoord = onAddCoord,
                onUndo = onUndo,
                onRedo = onRedo,
                onComplete = onComplete,
                onClear = onClear,
                onCopy = onCopy,
                onCopyCoords = onCopyCoords,
                onCopySelected = onCopySelected,
                onClose = onClose
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MeasurementHeader(
    state: MeasureUiState,
    onCollapse: () -> Unit
) {
    val accent = state.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(accent, accent.copy(alpha = 0.4f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = state.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.surface
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val modeTitleRes = when (state.mode) {
                MeasureMode.DISTANCE -> R.string.measure_distance
                MeasureMode.AREA -> R.string.measure_area
                MeasureMode.CIRCLE -> R.string.measure_circle_short
                MeasureMode.ELLIPSE -> R.string.measure_ellipse_short
                MeasureMode.COORDINATE -> R.string.measure_coord_short
                MeasureMode.SELECT -> R.string.measure_select_short
                else -> R.string.measure_distance
            }
            Text(
                text = stringResource(modeTitleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val chipColor = if (state.isCompleted)
                    MaterialTheme.colorScheme.tertiary
                else
                    accent
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(chipColor.copy(alpha = 0.85f))
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (state.isCompleted) stringResource(R.string.measure_completed) else stringResource(R.string.measure_new),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.progressText != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        FilledTonalIconButton(
            onClick = onCollapse,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MeasurementHeroSection(
    state: MeasureUiState,
    showDetails: Boolean,
    onToggleDetails: () -> Unit
) {
    val accent = state.accent

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (state.result != null && state.mainValue.isNotEmpty()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.mainValue,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = state.unit,
                    fontSize = 20.sp,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            val parts = resultParts(state.result)
            if (parts.size > 1) {
                parts.drop(1).forEach { part ->
                    Text(
                        text = withLenSmaller(part, MaterialTheme.typography.bodyMedium),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (state.mode == MeasureMode.COORDINATE && state.coordPoints.isNotEmpty()) {
            Text(
                text = state.coordPoints.last(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else if (state.mode == MeasureMode.SELECT && state.selectedInfo != null) {
            if (state.selectedTitle != null) {
                Text(
                    text = state.selectedTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = state.selectedInfo ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun MeasurementSegmentList(
    segments: List<MeasureSegment>,
    accent: Color
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        segments.forEach { segment ->
            SegmentCard(segment = segment, accent = accent)
        }
    }
}

@Composable
private fun SegmentCard(
    segment: MeasureSegment,
    accent: Color
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.4f))
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stringResource(R.string.measure_segment)} ${segment.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = segment.distance,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (segment.bearing != null) {
                Text(
                    text = segment.bearing,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SegmentDetailToggle(
    showDetails: Boolean,
    onToggle: () -> Unit,
    segments: List<MeasureSegment>,
    accent: Color,
    isArea: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = onToggle,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (showDetails) stringResource(R.string.hide_details) else "${segments.size} \u2022 ${stringResource(R.string.show_details).lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }

    AnimatedVisibility(visible = showDetails) {
        Column {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(2.dp))
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                var accum = 0.0
                segments.forEach { segment ->
                    val segDist = segment.distance
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.4f))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${stringResource(R.string.measure_segment)} ${segment.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = segment.distance,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceMeasurementDetails(state: MeasureUiState, showDetails: Boolean, onToggleDetails: () -> Unit) {
    if (state.result == null) return
    val primary = primaryResultText(state.result)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        CirclePropertyRow(valueFromPart(primary), state.accent, "len")
        AnimatedVisibility(visible = showDetails) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
                PropertyTextRow(valueFromPart(primary), state.accent.copy(alpha = 0.55f), descRes("len"))
            }
        }
        DetailToggle(showDetails, onToggleDetails)
    }
}

@Composable
private fun AreaMeasurementDetails(state: MeasureUiState, showDetails: Boolean, onToggleDetails: () -> Unit) {
    if (state.result == null) return
    val parts = resultParts(state.result)
    if (parts.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        CirclePropertyRow(valueFromPart(parts[0]), state.accent, "area")
        if (parts.size > 1) {
            CirclePropertyRow(valueFromPart(parts[1]), state.accent.copy(alpha = 0.7f), "perim")
                AnimatedVisibility(visible = showDetails) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                        PropertyTextRow(valueFromPart(parts[0]), state.accent.copy(alpha = 0.55f), descRes("area"))
                        PropertyTextRow(valueFromPart(parts[1]), state.accent.copy(alpha = 0.55f), descRes("perim"))
                    }
                }
            DetailToggle(showDetails, onToggleDetails)
        }
    }
}

@Composable
private fun CircleMeasurementDetails(state: MeasureUiState, showDetails: Boolean, onToggleDetails: () -> Unit) {
    if (!state.circleEdgeSet) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        CirclePropertyRow(state.circleArea, state.accent, "cArea")
        CirclePropertyRow(state.circleCircumference, state.accent.copy(alpha = 0.7f), "circ")
        CirclePropertyRow(state.circleDiameter, state.accent.copy(alpha = 0.55f), "diam")
        CirclePropertyRow(state.circleRadius.let { fmtDist(it, state.distUnit) }, state.accent.copy(alpha = 0.55f), "rad")
        AnimatedVisibility(visible = showDetails) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
                PropertyTextRow(state.circleArea, state.accent.copy(alpha = 0.55f), descRes("cArea"))
                PropertyTextRow(state.circleCircumference, state.accent.copy(alpha = 0.55f), descRes("circ"))
                PropertyTextRow(state.circleDiameter, state.accent.copy(alpha = 0.55f), descRes("diam"))
                PropertyTextRow(state.circleRadius.let { fmtDist(it, state.distUnit) }, state.accent.copy(alpha = 0.55f), descRes("rad"))
            }
        }
        DetailToggle(showDetails, onToggleDetails)
    }
}

@Composable
private fun EllipseMeasurementDetails(state: MeasureUiState, showDetails: Boolean, onToggleDetails: () -> Unit) {
    if (!state.ellipseMinorSet) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        CirclePropertyRow(state.ellipseArea, state.accent, "ellipse_area")
        CirclePropertyRow(state.ellipsePerimeter, state.accent.copy(alpha = 0.7f), "ellipse_perimeter")
        CirclePropertyRow(state.ellipseMajorAxis, state.accent.copy(alpha = 0.55f), "ellipse_major_axis")
        CirclePropertyRow(state.ellipseMinorAxis, state.accent.copy(alpha = 0.55f), "ellipse_minor_axis")
        CirclePropertyRow(state.ellipseAngle, state.accent.copy(alpha = 0.55f), "ellipse_angle")
        AnimatedVisibility(visible = showDetails) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
                PropertyTextRow(state.ellipseArea, state.accent.copy(alpha = 0.55f), descRes("ellipse_area"))
                PropertyTextRow(state.ellipsePerimeter, state.accent.copy(alpha = 0.55f), descRes("ellipse_perimeter"))
                PropertyTextRow(state.ellipseMajorAxis, state.accent.copy(alpha = 0.55f), descRes("ellipse_major_axis"))
                PropertyTextRow(state.ellipseMinorAxis, state.accent.copy(alpha = 0.55f), descRes("ellipse_minor_axis"))
                PropertyTextRow(state.ellipseAngle, state.accent.copy(alpha = 0.55f), descRes("ellipse_angle"))
            }
        }
        DetailToggle(showDetails, onToggleDetails)
    }
}

private fun valueFromPart(part: String): String {
    val eq = part.indexOf("=")
    return if (eq >= 0) part.substring(eq + 1).trim() else part.trim()
}

@Composable
private fun DetailToggle(showDetails: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = onToggle,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (showDetails) stringResource(R.string.hide_details) else stringResource(R.string.show_details),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun CirclePropertyRow(value: String, accent: Color, symbolKey: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = lbl(symbolKey),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = " = ",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

private fun descRes(key: String): Int = when (key) {
    "len" -> R.string.msr_len
    "perim" -> R.string.msr_perim
    "area" -> R.string.msr_area
    "circ" -> R.string.msr_circ
    "diam" -> R.string.msr_diam
    "rad" -> R.string.msr_radius
    "cArea" -> R.string.msr_area
    "ellipse_area" -> R.string.msr_area
    "ellipse_perimeter" -> R.string.ellipse_perimeter
    "ellipse_major_axis" -> R.string.ellipse_major_axis
    "ellipse_minor_axis" -> R.string.ellipse_minor_axis
    "ellipse_angle" -> R.string.ellipse_angle_label
    else -> R.string.msr_len
}

@Composable
private fun PropertyTextRow(value: String, accent: Color, labelRes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = " = ",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
private fun CoordinateMeasurementDetails(
    state: MeasureUiState,
    showDetails: Boolean,
    onToggleDetails: () -> Unit
) {
    if (state.coordPoints.isEmpty()) return

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = onToggleDetails,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${state.coordPoints.size} \u2022 ${if (showDetails) stringResource(R.string.hide_details).lowercase() else stringResource(R.string.show_details).lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        AnimatedVisibility(visible = showDetails) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                state.coordPoints.forEachIndexed { idx, pt ->
                    val isLast = idx == state.coordPoints.lastIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLast) state.accent.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isLast) state.accent
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLast) state.accent
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pt,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLast) state.accent
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectMeasurementDetails(
    state: MeasureUiState,
    showDetails: Boolean,
    onToggleDetails: () -> Unit,
    onCopySelected: () -> Unit
) {
    val raw = state.selectRawData ?: return
    var showSegments by remember { mutableStateOf(false) }

    val symbolKeys = when (raw.modeName) {
        "AREA" -> listOf("area", "perim")
        "CIRCLE" -> listOf("cArea", "circ", "diam", "rad")
        else -> listOf("len")
    }

    Column {
        if (state.selectedInfo != null) {
            val parts = resultParts(state.selectedInfo)
            if (parts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    CirclePropertyRow(valueFromPart(parts[0]), state.accent, symbolKeys[0])
                    if (parts.size > 1) {
                        parts.drop(1).forEachIndexed { idx, part ->
                            val key = symbolKeys.getOrElse(idx + 1) { symbolKeys[0] }
                            val alpha = if (idx == 0) 0.7f else 0.55f
                            CirclePropertyRow(valueFromPart(part), state.accent.copy(alpha = alpha), key)
                        }
                        AnimatedVisibility(visible = showDetails) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                                parts.drop(1).forEachIndexed { idx, part ->
                                    val key = symbolKeys.getOrElse(idx + 1) { symbolKeys[0] }
                                    PropertyTextRow(valueFromPart(part), state.accent.copy(alpha = 0.55f), descRes(key))
                                }
                            }
                        }
                        DetailToggle(showDetails, onToggleDetails)
                    }
                }
            }
        }

        if (raw.points.size >= 2) {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { showSegments = !showSegments },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = if (showSegments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.show_details)} (${raw.points.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                AnimatedVisibility(visible = showSegments) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        val pts = raw.points
                        if (raw.modeName == "AREA" && pts.size >= 3) {
                            for (i in pts.indices) {
                                val j = (i + 1) % pts.size
                                val seg = gcDist(pts[i], pts[j])
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${lbl("len")} ${i + 1}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = state.accent.copy(alpha = 0.55f)
                                    )
                                    Text(
                                        text = " = ",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = state.accent.copy(alpha = 0.55f)
                                    )
                                    Text(
                                        text = fmtDist(seg, state.distUnit),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = state.accent.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        } else {
                            for (i in 1 until pts.size) {
                                val seg = gcDist(pts[i - 1], pts[i])
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${lbl("len")} $i",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = state.accent.copy(alpha = 0.55f)
                                    )
                                    Text(
                                        text = " = ",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = state.accent.copy(alpha = 0.55f)
                                    )
                                    Text(
                                        text = fmtDist(seg, state.distUnit),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = state.accent.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementUnitsSelector(
    units: List<String>,
    unitLabels: List<String>,
    selectedUnit: String,
    accent: Color,
    onUnitChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        units.forEachIndexed { idx, unit ->
            val isSelected = selectedUnit == unit
            Surface(
                onClick = { onUnitChange(unit) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) accent.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                border = if (isSelected) BorderStroke(1.dp, accent.copy(alpha = 0.4f)) else null
            ) {
                Text(
                    text = unitLabels.getOrElse(idx) { unit },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoordFormatSelector(
    formats: List<String>,
    selectedFormat: String,
    accent: Color,
    onFormatChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        formats.forEach { fmt ->
            val isSelected = selectedFormat == fmt
            Surface(
                onClick = { onFormatChange(fmt) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) accent.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                border = if (isSelected) BorderStroke(1.dp, accent.copy(alpha = 0.4f)) else null
            ) {
                Text(
                    text = fmt,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun MeasurementMiniPreview(
    pts: List<GeoPoint>,
    accent: Color
) {
    if (pts.size < 2) return

    val maxPts = pts.size.coerceAtMost(8)
    val samplePts = if (pts.size > 8) {
        val step = (pts.size - 1).toFloat() / (maxPts - 1).toFloat()
        (0 until maxPts).map { pts[(it * step).toInt()] }
    } else pts
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        val canvasW = size.width
        val canvasH = size.height
        if (canvasW <= 0 || canvasH <= 0) return@Canvas

        val lats = samplePts.map { it.latitude }
        val lons = samplePts.map { it.longitude }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLon = lons.min(); val maxLon = lons.max()
        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)
        val padding = 16f
        val drawW = canvasW - padding * 2
        val drawH = canvasH - padding * 2

        val points = samplePts.map { pt ->
            Offset(
                x = padding + ((pt.longitude - minLon) / lonRange * drawW).toFloat(),
                y = padding + drawH - ((pt.latitude - minLat) / latRange * drawH).toFloat()
            )
        }

        for (i in 0 until points.lastIndex) {
            drawLine(
                color = accent,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        points.forEachIndexed { idx, pt ->
            val radius = if (idx == 0 || idx == points.lastIndex) 5f else 3.5f
            drawCircle(
                color = if (idx == points.lastIndex) accent else accent.copy(alpha = 0.6f),
                radius = radius,
                center = pt
            )
            if (idx == 0 || idx == points.lastIndex) {
                drawCircle(
                    color = surfaceColor.copy(alpha = 0.6f),
                    radius = radius * 0.5f,
                    center = pt
                )
            }
        }
    }
}

@Composable
private fun MeasurementActionBar(
    state: MeasureUiState,
    canRedo: Boolean,
    onAddPoint: () -> Unit,
    onAddCoord: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onComplete: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onCopyCoords: () -> Unit,
    onCopySelected: () -> Unit,
    onClose: () -> Unit
) {
    val accent = state.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state.mode) {
            MeasureMode.DISTANCE, MeasureMode.AREA -> {
                if (state.isCompleted) {
                    ActionCopyButton(onCopy, accent)
                    ActionDeleteButton(onClear)
                } else {
                    ActionAddButton(onAddPoint, accent)
                    ActionUndoButton(onUndo, state.hasPoints)
                    if (canRedo) ActionRedoButton(onRedo)
                    if (state.hasPoints && (state.mode != MeasureMode.AREA || state.pointsCount >= 3)) {
                        ActionFinishButton(onComplete, accent)
                    }
                    if (state.hasPoints) ActionCopyButton(onCopy, accent)
                    ActionDeleteButton(onClear)
                }
            }
            MeasureMode.CIRCLE -> {
                if (state.isCompleted) {
                    ActionCopyButton(onCopy, accent)
                    ActionDeleteButton(onClear)
                } else {
                    ActionAddButton(onAddPoint, accent)
                    if (state.circleCenterSet) {
                        ActionFinishButton(onComplete, accent)
                    }
                    if (state.circleEdgeSet) ActionCopyButton(onCopy, accent)
                    ActionDeleteButton(onClear)
                }
            }
            MeasureMode.ELLIPSE -> {
                if (state.isCompleted) {
                    ActionCopyButton(onCopy, accent)
                    ActionDeleteButton(onClear)
                } else {
                    ActionAddButton(onAddPoint, accent)
                    if (state.ellipseCenterSet && state.ellipseMajorSet) {
                        ActionFinishButton(onComplete, accent)
                    }
                    if (state.ellipseMinorSet) ActionCopyButton(onCopy, accent)
                    ActionDeleteButton(onClear)
                }
            }
            MeasureMode.SELECT -> {
                if (state.selectedInfo != null) {
                    ActionCopyButton(onCopySelected, accent)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            MeasureMode.COORDINATE -> {
                ActionAddButton(onAddCoord, accent)
                if (state.coordPoints.isNotEmpty()) {
                    ActionCopyButton(onCopyCoords, accent)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Stop, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun ActionAddButton(onClick: () -> Unit, accent: Color) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = accent.copy(alpha = 0.2f),
            contentColor = accent
        )
    ) {
        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.add_point), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionUndoButton(onClick: () -> Unit, enabled: Boolean) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.undo), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ActionRedoButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(Icons.AutoMirrored.Filled.Redo, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.redo), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ActionFinishButton(onClick: () -> Unit, accent: Color) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.measure_complete), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionCopyButton(onClick: () -> Unit, accent: Color) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = accent.copy(alpha = 0.12f),
            contentColor = accent
        )
    ) {
        Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.copy_result), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionDeleteButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(Icons.Default.Delete, null, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.clear_measurement), style = MaterialTheme.typography.labelSmall)
    }
}

// =============================================================================
// MEASURE OVERLAYS (map drawing - unchanged)
// =============================================================================

@Composable
fun MeasureOverlays(mv: MapView?, mode: MeasureMode, pts: List<GeoPoint>, done: Boolean, result: String?,
                    onPts: (List<GeoPoint>) -> Unit, onRes: (String?) -> Unit, distUnit: String, areaUnit: String,
                    circleCenter: GeoPoint?, onCircleCenter: (GeoPoint?) -> Unit,
                    circleEdge: GeoPoint?, onCircleEdge: (GeoPoint?) -> Unit,
                    onCircleRadius: (Double) -> Unit, onCircleDone: () -> Unit,
                    center: IGeoPoint? = null, addPointTrigger: Int = 0,
                    selectActive: Boolean = false,
                    onSelectedInfo: (String?, String?) -> Unit = { _, _ -> },
                    onSelectRawData: (SelectRawData?) -> Unit = {},
                    onCopySelectedText: (String) -> Unit = {},
                    coordPositions: List<GeoPoint> = emptyList(),
                    ellipseCenter: GeoPoint? = null, onEllipseCenter: (GeoPoint?) -> Unit = {},
                    ellipseMajor: GeoPoint? = null, onEllipseMajor: (GeoPoint?) -> Unit = {},
                    ellipseMinor: GeoPoint? = null, onEllipseMinor: (GeoPoint?) -> Unit = {},
                     ellipseResult: String? = null, onEllipseResult: (String?) -> Unit = {},
                     snappedPoint: GeoPoint? = null,
                     drawMode: String = "polar") {
    val ref = remember { PointsRef() }; ref.v = pts
    val mvRef = remember { mutableListOf<MapView?>() }; mvRef.clear(); mvRef.add(mv)
    val distUnitRef = remember { mutableListOf("auto") }; distUnitRef[0] = distUnit
    val areaUnitRef = remember { mutableListOf("auto") }; areaUnitRef[0] = areaUnit
    val doneRef = remember { mutableListOf(false) }; doneRef[0] = done
    val centerRef = remember { mutableListOf<IGeoPoint?>() }; centerRef.clear(); centerRef.add(center)
    val circleCenterRef = remember { mutableListOf<GeoPoint?>() }; circleCenterRef.clear(); circleCenterRef.add(circleCenter)
    val circleEdgeRef = remember { mutableListOf<GeoPoint?>() }; circleEdgeRef.clear(); circleEdgeRef.add(circleEdge)
    val ellipseCenterRef = remember { mutableListOf<GeoPoint?>() }; ellipseCenterRef.clear(); ellipseCenterRef.add(ellipseCenter)
    val ellipseMajorRef = remember { mutableListOf<GeoPoint?>() }; ellipseMajorRef.clear(); ellipseMajorRef.add(ellipseMajor)
    val ellipseMinorRef = remember { mutableListOf<GeoPoint?>() }; ellipseMinorRef.clear(); ellipseMinorRef.add(ellipseMinor)
    val selectActiveRef = remember { mutableListOf(false) }; selectActiveRef[0] = selectActive
    val snappedRef = remember { mutableListOf<GeoPoint?>() }; snappedRef.clear(); snappedRef.add(snappedPoint)
    val drawModeRef = remember { mutableListOf("polar") }; drawModeRef[0] = drawMode
    val onSelectedInfoRef = remember { mutableListOf<(String?, String?) -> Unit>({ _, _ -> }) }; onSelectedInfoRef[0] = onSelectedInfo
    val onSelectRawDataRef = remember { mutableListOf<(SelectRawData?) -> Unit>({}) }; onSelectRawDataRef[0] = onSelectRawData
    val circleRadiusRef = remember { mutableDoubleStateOf(0.0) }
    val polyline = remember { Polyline() }
    val polygon = remember { Polygon() }
    val previewLine = remember { Polyline() }
    val circlePoly = remember { Polygon() }
    val ellipseMajorLine = remember { Polyline() }
    val ellipseMinorLine = remember { Polyline() }
    val ellipseMajorMarker = remember { mutableStateOf<Marker?>(null) }
    val ellipseMinorMarker = remember { mutableStateOf<Marker?>(null) }
    val markerPool = remember { mutableListOf<Marker>() }
    val resultLabel = remember { mutableStateOf<Marker?>(null) }
    val tapAnim = remember { TapAnimOverlay() }
    val initDone = remember { mutableStateOf(false) }
    val circleCenterMarker = remember { mutableStateOf<Marker?>(null) }
    val savedPolylines = remember { mutableListOf<Triple<List<GeoPoint>, String, MeasureMode>>() }
    val savedCircles = remember { mutableListOf<Pair<Triple<GeoPoint, GeoPoint, Double>, String>>() }
    val savedEllipses = remember { mutableListOf<Pair<Triple<GeoPoint, GeoPoint, GeoPoint>, String>>() }
    val coordMakers = remember { mutableListOf<Marker>() }

    fun constrainOrtho(gp: GeoPoint, prev: GeoPoint): GeoPoint {
        if (drawModeRef[0] != "ortho") return gp
        return if (abs(gp.longitude - prev.longitude) >= abs(gp.latitude - prev.latitude)) {
            GeoPoint(prev.latitude, gp.longitude)
        } else {
            GeoPoint(gp.latitude, prev.longitude)
        }
    }

    fun applyMeasurementAnchor(md: MeasureMode, gp: GeoPoint, m: MapView, force: Boolean = false) {
        if (md == MeasureMode.DISTANCE || md == MeasureMode.AREA) {
            val prev = ref.v.lastOrNull()
            val tap = if (prev != null) constrainOrtho(gp, prev) else gp
            val minDist = if (force) 0.0 else MIN_TAP_DIST_M
            val n = appendMeasurementPoint(ref.v, tap, minDist) ?: return
            onPts(n)
            onRes(calcResult(n, md, distUnitRef[0], areaUnitRef[0]))
            logMeasureAnchor(md, "point_added", gp)
            tapAnim.animateAt(m, gp)
        } else if (md == MeasureMode.CIRCLE) {
            val cc = circleCenterRef[0]
            if (cc == null) {
                onCircleCenter(gp)
                logMeasureAnchor(md, "circle_center_added", gp)
                tapAnim.animateAt(m, gp)
            } else if (circleEdgeRef[0] == null) {
                val edge = constrainOrtho(gp, cc)
                onCircleEdge(edge)
                val r = gcDist(cc, edge)
                onCircleRadius(r)
                circleRadiusRef.doubleValue = r
                logMeasureAnchor(md, "circle_edge_added", gp)
                tapAnim.animateAt(m, gp)
                onCircleDone()
            }
        } else if (md == MeasureMode.ELLIPSE) {
            val ec = ellipseCenterRef[0]
            if (ec == null) {
                onEllipseCenter(gp)
                logMeasureAnchor(md, "ellipse_center_added", gp)
                tapAnim.animateAt(m, gp)
            } else if (ellipseMajorRef[0] == null) {
                val major = constrainOrtho(gp, ec)
                onEllipseMajor(major)
                logMeasureAnchor(md, "ellipse_major_added", gp)
                tapAnim.animateAt(m, gp)
            } else if (ellipseMinorRef[0] == null) {
                val em = ellipseMajorRef[0]
                val minor = if (em != null && drawModeRef[0] == "ortho") {
                    if (abs(em.latitude - ec.latitude) < abs(em.longitude - ec.longitude)) {
                        GeoPoint(gp.latitude, ec.longitude)
                    } else {
                        GeoPoint(ec.latitude, gp.longitude)
                    }
                } else gp
                onEllipseMinor(minor)
                logMeasureAnchor(md, "ellipse_minor_added", gp)
                tapAnim.animateAt(m, gp)
                onEllipseResult(calcEllipseResult(ellipseCenterRef[0], ellipseMajorRef[0], gp, distUnitRef[0], areaUnitRef[0]))
            }
        }
    }

    LaunchedEffect(done) {
        if ((done && mode == MeasureMode.DISTANCE && ref.v.size >= 2) || (done && mode == MeasureMode.AREA && ref.v.size >= 3)) {
            savedPolylines.add(Triple(ref.v, result ?: "", mode))
        }
        if (done && mode == MeasureMode.CIRCLE) {
            val cc = circleCenterRef[0] ?: return@LaunchedEffect
            val ce = circleEdgeRef[0] ?: return@LaunchedEffect
            val r = circleRadiusRef.doubleValue
            if (r > 0.0) savedCircles.add(Triple(cc, ce, r) to (result ?: ""))
        }
        if (done && mode == MeasureMode.ELLIPSE) {
            val ec = ellipseCenterRef[0] ?: return@LaunchedEffect
            val em = ellipseMajorRef[0] ?: return@LaunchedEffect
            val en = ellipseMinorRef[0] ?: return@LaunchedEffect
            savedEllipses.add(Triple(ec, em, en) to (result ?: ""))
        }
    }

    val modeRef = remember { mutableListOf(MeasureMode.NONE) }; modeRef[0] = mode
    val selectTapHandler = remember {
        object : TapOverlay() {
            override fun onTap(p: GeoPoint) {
                if (!selectActiveRef[0]) return
                val m = mvRef[0] ?: return
                val du = distUnitRef[0]; val au = areaUnitRef[0]
                for ((plPts, _, plMode) in savedPolylines) {
                    for (i in 1 until plPts.size) {
                        val proj = m.projection; val a = Point(); val b = Point()
                        proj.toPixels(plPts[i-1], a); proj.toPixels(plPts[i], b)
                        val tp = Point(); proj.toPixels(p, tp)
                        val dx = (b.x - a.x).toDouble(); val dy = (b.y - a.y).toDouble()
                        val len = sqrt(dx*dx + dy*dy)
                        if (len < 1.0) continue
                        val t = ((tp.x - a.x) * dx + (tp.y - a.y) * dy) / (len * len)
                        val tClamped = t.coerceIn(0.0, 1.0)
                        val cx = a.x + (tClamped * dx).toInt(); val cy = a.y + (tClamped * dy).toInt()
                        val dist = sqrt(((tp.x - cx).toDouble() * (tp.x - cx) + (tp.y - cy).toDouble() * (tp.y - cy)))
                        if (dist < 30.0) {
                            val newRes = if (plMode == MeasureMode.AREA) calcAreaResult(plPts, du, au) else calcResult(plPts, MeasureMode.DISTANCE, du, au)
                            onSelectedInfoRef[0](plMode.name, newRes ?: "")
                            onSelectRawDataRef[0](SelectRawData(plMode.name, plPts))
                            tapAnim.animateAt(m, p); return
                        }
                    }
                }
                for ((ccData, _) in savedCircles) {
                    val (_, _, r) = ccData
                    val newRes = calcCircleResult(r, du, au)
                    onSelectedInfoRef[0]("CIRCLE", newRes)
                    onSelectRawDataRef[0](SelectRawData("CIRCLE", emptyList(), r, ccData.first))
                    tapAnim.animateAt(m, p); return
                }
            }
        }
    }

    val measureTapHandler = remember {
        object : TapOverlay() {
            override fun onTap(p: GeoPoint) {
                val m = mvRef[0] ?: return
                val md = modeRef[0]
                if (md != MeasureMode.DISTANCE && md != MeasureMode.AREA && md != MeasureMode.CIRCLE && md != MeasureMode.ELLIPSE) return
                val snap = snappedRef[0]
                applyMeasurementAnchor(md, snap ?: p, m)
            }
        }
    }

    LaunchedEffect(mode) {
        val m = mv ?: return@LaunchedEffect
        if (mode == MeasureMode.SELECT) {
            m.overlays.removeAll { it === measureTapHandler }
            m.overlays.add(selectTapHandler)
        } else if (mode == MeasureMode.DISTANCE || mode == MeasureMode.AREA || mode == MeasureMode.CIRCLE || mode == MeasureMode.ELLIPSE) {
            m.overlays.removeAll { it === selectTapHandler }
            m.overlays.add(measureTapHandler)
        } else {
            m.overlays.removeAll { it === selectTapHandler || it === measureTapHandler }
        }
        m.invalidate()
    }

    LaunchedEffect(addPointTrigger) {
        if (addPointTrigger == 0) return@LaunchedEffect
        val ctr = centerRef[0] ?: return@LaunchedEffect
        val m = mvRef[0] ?: return@LaunchedEffect
        val gp = GeoPoint(ctr.latitude, ctr.longitude)
        applyMeasurementAnchor(mode, gp, m, force = true)
    }

    LaunchedEffect(mode, pts, center, done) {
        val m = mvRef[0] ?: return@LaunchedEffect
        if ((mode == MeasureMode.DISTANCE || mode == MeasureMode.AREA) && !done && pts.isNotEmpty() && center != null) {
            val gp = GeoPoint(center.latitude, center.longitude)
            val last = pts.last()
            previewLine.setPoints(ArrayList(listOf(last, gp)))
            previewLine.outlinePaint.apply {
                color = AColor.argb(200, 100, 100, 100); strokeWidth = 3f; isAntiAlias = true
                setPathEffect(android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f))
            }
            previewLine.setVisible(true)
        } else {
            previewLine.setVisible(false)
        }
        m.postInvalidate()
    }

    LaunchedEffect(mv) {
        val m = mv ?: return@LaunchedEffect
        if (initDone.value) return@LaunchedEffect; initDone.value = true
        markerPool.clear()
        repeat(25) { i ->
            Marker(m).apply {
                setAnchor(0.5f, 0.5f); setInfoWindow(null); snippet = ""; setVisible(false); isDraggable = true
                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDragStart(marker: Marker?) {}
                    override fun onMarkerDrag(marker: Marker?) {}
                    override fun onMarkerDragEnd(marker: Marker?) {
                        val pos = marker?.position as? GeoPoint ?: return
                        val idx = marker?.snippet?.toIntOrNull() ?: return
                        if (idx < 0 || idx >= ref.v.size) return
                        val upd = ref.v.toMutableList().also { it[idx] = pos }
                        onPts(upd); onRes(calcResult(upd, mode, distUnitRef[0], areaUnitRef[0]))
                    }
                })
                markerPool.add(this); m.overlays.add(this)
            }
        }
        m.overlays.add(polyline); m.overlays.add(polygon); m.overlays.add(previewLine)
        m.overlays.add(circlePoly); m.overlays.add(tapAnim)
        coordMakers.forEach { m.overlays.add(it) }
        resultLabel.value = Marker(m).apply { setAnchor(0.5f, 0.5f); setInfoWindow(null); title = "measureResult"; setVisible(false) }
        m.overlays.add(resultLabel.value!!)
        m.invalidate()
    }

    val coordPositionsRef = remember { mutableListOf<List<GeoPoint>>() }; coordPositionsRef.clear(); coordPositionsRef.add(coordPositions)

    LaunchedEffect(coordPositions) {
        val m = mvRef[0] ?: return@LaunchedEffect
        while (coordMakers.size < coordPositions.size) {
            val num = coordMakers.size + 1
            Marker(m).apply {
                setAnchor(0.5f, 0.5f); setInfoWindow(null); setVisible(false); snippet = "coord"
                setIcon(coordMarkerIcon(m, num, COORD_COLOR))
            }.let { coordMakers.add(it); m.overlays.add(it) }
        }
        coordPositions.forEachIndexed { idx, gp ->
            if (idx < coordMakers.size) {
                coordMakers[idx].apply { position = gp; setVisible(true) }
            }
        }
        for (i in coordPositions.size until coordMakers.size) coordMakers[i].setVisible(false)
        m.invalidate()
    }

    LaunchedEffect(mode, pts, distUnit, areaUnit, done, circleCenter, circleEdge, result, ellipseCenter, ellipseMajor, ellipseMinor) {
        val m = mvRef[0] ?: return@LaunchedEffect
        val rl = resultLabel.value ?: return@LaunchedEffect

        if (mode == MeasureMode.NONE || mode == MeasureMode.SELECT) {
            polyline.setVisible(false); polygon.setVisible(false); previewLine.setVisible(false)
            circlePoly.setVisible(false); ellipseMajorLine.setVisible(false); ellipseMinorLine.setVisible(false)
            markerPool.forEach { it.setVisible(false) }; rl.setVisible(false)
            ellipseMajorMarker.value?.setVisible(false); ellipseMinorMarker.value?.setVisible(false)
            coordMakers.forEach { it.setVisible(false) }
            m.invalidate(); return@LaunchedEffect
        }

        if (mode == MeasureMode.COORDINATE) {
            polyline.setVisible(false); polygon.setVisible(false); previewLine.setVisible(false)
            circlePoly.setVisible(false); ellipseMajorLine.setVisible(false); ellipseMinorLine.setVisible(false)
            markerPool.forEach { it.setVisible(false) }; rl.setVisible(false)
            ellipseMajorMarker.value?.setVisible(false); ellipseMinorMarker.value?.setVisible(false)
            m.invalidate(); return@LaunchedEffect
        }

        if (mode == MeasureMode.DISTANCE || mode == MeasureMode.AREA) {
            if (pts.size >= 2) {
                val showPolygon = mode == MeasureMode.AREA && done && pts.size >= 3
                if (showPolygon) {
                    val c = pts.toMutableList().also { it.add(pts.first()) }
                    polygon.setPoints(ArrayList(c))
                    polygon.fillPaint.apply { color = AREA_FILL; isAntiAlias = true }
                    polygon.outlinePaint.apply { color = AREA_COLOR; strokeWidth = 4f; isAntiAlias = true }
                    polygon.setVisible(true); polyline.setVisible(false); previewLine.setVisible(false)
                } else {
                    polyline.setPoints(ArrayList(pts))
                    polyline.outlinePaint.apply { color = DIST_COLOR; strokeWidth = 5f; isAntiAlias = true }
                    polyline.setVisible(true); polygon.setVisible(false)
                }
                circlePoly.setVisible(false)
            } else { polyline.setVisible(false); polygon.setVisible(false) }
        } else if (mode == MeasureMode.CIRCLE) {
            polyline.setVisible(false); polygon.setVisible(false); previewLine.setVisible(false)
            ellipseMajorLine.setVisible(false); ellipseMinorLine.setVisible(false)
            ellipseMajorMarker.value?.setVisible(false); ellipseMinorMarker.value?.setVisible(false)
            if (circleCenter != null && circleEdge != null) {
                val r = gcDist(circleCenter, circleEdge)
                circlePoly.setPoints(ArrayList(circlePolygon(circleCenter, r)))
                circlePoly.fillPaint.apply { color = CIRCLE_FILL; isAntiAlias = true }
                circlePoly.outlinePaint.apply { color = CIRCLE_COLOR; strokeWidth = 4f; isAntiAlias = true }
                circlePoly.setVisible(true)
            } else { circlePoly.setVisible(false) }
        } else if (mode == MeasureMode.ELLIPSE) {
            polyline.setVisible(false); polygon.setVisible(false); previewLine.setVisible(false)
            if (ellipseCenter != null && ellipseMajor != null) {
                ellipseMajorLine.setPoints(ArrayList(listOf(ellipseCenter, ellipseMajor)))
                ellipseMajorLine.outlinePaint.apply { color = ELLIPSE_COLOR; strokeWidth = 3f; isAntiAlias = true; setPathEffect(android.graphics.DashPathEffect(floatArrayOf(10f, 8f), 0f)) }
                ellipseMajorLine.setVisible(true)
                if (ellipseMajorMarker.value == null) {
                    Marker(m).apply { setAnchor(0.5f, 0.5f); setInfoWindow(null); setIcon(circleIcon(m, 7, ELLIPSE_COLOR, AColor.WHITE, 2.5f, 1)); position = ellipseMajor; setVisible(true); isDraggable = false; ellipseMajorMarker.value = this; m.overlays.add(this) }
                } else { ellipseMajorMarker.value?.apply { position = ellipseMajor; setVisible(true) } }
            } else { ellipseMajorLine.setVisible(false); ellipseMajorMarker.value?.setVisible(false) }
            if (ellipseCenter != null && ellipseMinor != null) {
                ellipseMinorLine.setPoints(ArrayList(listOf(ellipseCenter, ellipseMinor)))
                ellipseMinorLine.outlinePaint.apply { color = ELLIPSE_COLOR; strokeWidth = 3f; isAntiAlias = true; setPathEffect(android.graphics.DashPathEffect(floatArrayOf(6f, 6f), 0f)) }
                ellipseMinorLine.setVisible(true)
                if (ellipseMinorMarker.value == null) {
                    Marker(m).apply { setAnchor(0.5f, 0.5f); setInfoWindow(null); setIcon(circleIcon(m, 7, AColor.parseColor("#CE93D8"), AColor.WHITE, 2.5f, 1)); position = ellipseMinor; setVisible(true); isDraggable = false; ellipseMinorMarker.value = this; m.overlays.add(this) }
                } else { ellipseMinorMarker.value?.apply { position = ellipseMinor; setVisible(true) } }
            } else { ellipseMinorLine.setVisible(false); ellipseMinorMarker.value?.setVisible(false) }
            if (ellipseCenter != null && ellipseMajor != null && ellipseMinor != null) {
                val a = gcDist(ellipseCenter, ellipseMajor); val b = ellipseMinorRadius(ellipseCenter, ellipseMajor, ellipseMinor)
                circlePoly.setPoints(ArrayList(ellipsePolygon(ellipseCenter, a, b, ellipseBearing(ellipseCenter, ellipseMajor))))
                circlePoly.fillPaint.apply { color = ELLIPSE_FILL; isAntiAlias = true }
                circlePoly.outlinePaint.apply { color = ELLIPSE_COLOR; strokeWidth = 4f; isAntiAlias = true }
                circlePoly.setVisible(true)
            } else { circlePoly.setVisible(false) }
        }

        markerPool.forEachIndexed { i, marker ->
            if (i < pts.size && (mode == MeasureMode.DISTANCE || mode == MeasureMode.AREA)) {
                marker.position = pts[i]
                marker.setIcon(if (i == 0) circleIcon(m, 8, FIRST_COLOR, FIRST_BORDER, 3f, 2)
                    else circleIcon(m, 7, AColor.WHITE, DIST_COLOR, 2.5f, 1))
                marker.setVisible(true); marker.isDraggable = !done
            } else marker.setVisible(false)
        }

        val centerPt = if (mode == MeasureMode.CIRCLE) circleCenter else if (mode == MeasureMode.ELLIPSE) ellipseCenter else null
        val centerClr = if (mode == MeasureMode.ELLIPSE) ELLIPSE_COLOR else CIRCLE_COLOR
        if ((mode == MeasureMode.CIRCLE || mode == MeasureMode.ELLIPSE) && centerPt != null) {
            val cm = circleCenterMarker.value
            if (cm == null) {
                Marker(m).apply {
                    setAnchor(0.5f, 0.5f); setInfoWindow(null)
                    setIcon(circleIcon(m, 8, centerClr, AColor.WHITE, 3f, 2))
                    position = centerPt; setVisible(true); isDraggable = false
                    circleCenterMarker.value = this; m.overlays.add(this)
                }
            } else { cm.position = centerPt; cm.setVisible(true) }
        } else { circleCenterMarker.value?.setVisible(false) }

        val showResult = result != null && (((mode == MeasureMode.DISTANCE || mode == MeasureMode.AREA) && pts.size >= 2) || mode == MeasureMode.CIRCLE || (mode == MeasureMode.ELLIPSE && ellipseCenter != null && ellipseMajor != null && ellipseMinor != null))
        if (showResult) {
            val pts2 = pts.ifEmpty { null }
            val screenPts = if (mode == MeasureMode.CIRCLE && circleCenter != null && circleEdge != null) {
                circlePolygon(circleCenter, gcDist(circleCenter, circleEdge)).map { val p = Point(); m.projection.toPixels(it, p); p }
            } else if (mode == MeasureMode.ELLIPSE && ellipseCenter != null && ellipseMajor != null && ellipseMinor != null) {
                val a = gcDist(ellipseCenter, ellipseMajor); val b = ellipseMinorRadius(ellipseCenter, ellipseMajor, ellipseMinor); val brg = ellipseBearing(ellipseCenter, ellipseMajor)
                ellipsePolygon(ellipseCenter, a, b, brg).map { val p = Point(); m.projection.toPixels(it, p); p }
            } else if (pts2 != null && pts2.size >= 2) {
                pts2.map { val p = Point(); m.projection.toPixels(it, p); p }
            } else emptyList()
            val avgX = if (screenPts.isNotEmpty()) screenPts.map { it.x }.average().toInt() else m.width / 2
            val avgY = if (screenPts.isNotEmpty()) screenPts.map { it.y }.average().toInt() else 0
            val posY = if (mode == MeasureMode.CIRCLE || mode == MeasureMode.ELLIPSE) screenPts.minOf { it.y } - 15 else avgY - 15
            rl.position = m.projection.fromPixels(avgX, posY.coerceAtLeast(0)) as GeoPoint

            val lenSym = "\u2192"
            val areaSym = "\u2B2D"
            val perimSym = "\u2B2C"
            val txtSz = 44f
            val smallSz = txtSz * 0.6f
            val textPaint = Paint().apply { textSize = txtSz; isAntiAlias = true; color = AColor.parseColor("#1A202C") }
            val smallPaint = Paint().apply { textSize = smallSz; isAntiAlias = true; color = AColor.parseColor("#1A202C") }
            val ellipseH = smallSz * 0.9f
            val ellipseW = ellipseH * 2.2f
            val symCount = result.count { it == '\u2B2D' || it == '\u2B2C' }
            val textBase = result.replace(areaSym, "").replace(perimSym, "")
            val textW = if (textBase.contains(lenSym))
                textPaint.measureText(textBase.replace(lenSym, "")) + smallPaint.measureText(lenSym) + ellipseW * symCount
            else textPaint.measureText(textBase) + ellipseW * symCount
            val textH = -(textPaint.ascent()) + textPaint.descent()
            val padH = 20f; val padV = 14f; val corner = 16f; val ptrH = 14; val ptrW = 12
            val bodyW = (textW + padH * 2).toInt(); val bodyH = (textH + padV * 2).toInt()
            val bmpW = bodyW; val bmpH = bodyH + ptrH
            val fb = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val fc = Canvas(fb)

            val shadowPaint = Paint().apply { color = 0x28000000.toInt(); isAntiAlias = true }
            fc.drawRoundRect(0f, 4f, bmpW.toFloat(), (bodyH + 4f).toFloat(), corner, corner, shadowPaint)
            fc.drawRoundRect(3f, 4f, (bmpW - 3f).toFloat(), (bodyH + 4f).toFloat(), corner, corner, shadowPaint)

            val bgPaint = Paint().apply { color = AColor.WHITE; isAntiAlias = true }
            fc.drawRoundRect(0f, 0f, bmpW.toFloat(), bodyH.toFloat(), corner, corner, bgPaint)

            val borderPaint = Paint().apply { color = AColor.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true }
            fc.drawRoundRect(0f, 0f, bmpW.toFloat(), bodyH.toFloat(), corner, corner, borderPaint)

            val ptrPath = Path().apply {
                val cx = bmpW / 2f
                moveTo(cx - ptrW, bodyH.toFloat()); lineTo(cx, (bodyH + ptrH).toFloat()); lineTo(cx + ptrW, bodyH.toFloat()); close()
            }
            fc.drawPath(ptrPath, Paint().apply { color = AColor.WHITE; isAntiAlias = true })
            val ptrBorder = Paint().apply { color = AColor.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true }
            fc.drawPath(ptrPath, ptrBorder)

            val drawText = result ?: ""
            val ellipseFillPaint = Paint().apply { color = AColor.parseColor("#1A202C"); isAntiAlias = true; style = Paint.Style.FILL }
            val ellipseStrokePaint = Paint().apply { color = AColor.parseColor("#1A202C"); isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2.5f }
            val baseY = padV - textPaint.ascent()
            val smallBaseY = baseY - (txtSz - smallSz) * 0.3f
            val allSyms = listOf(lenSym, areaSym, perimSym)
            if (allSyms.any { drawText.contains(it) }) {
                val lenParts = drawText.split(lenSym)
                var xOff = padH
                lenParts.forEachIndexed { lenIdx, lenPart ->
                    if (lenPart.isNotEmpty()) {
                        val hasEllipseSym = lenPart.contains(areaSym) || lenPart.contains(perimSym)
                        if (hasEllipseSym) {
                            val regex = Regex("[${areaSym}${perimSym}]")
                            val parts = regex.split(lenPart)
                            val matches = regex.findAll(lenPart).map { it.value }.toList()
                            parts.forEachIndexed { idx, part ->
                                if (part.isNotEmpty()) {
                                    fc.drawText(part, xOff, baseY, textPaint)
                                    xOff += textPaint.measureText(part)
                                }
                                if (idx < parts.size - 1 && idx < matches.size) {
                                    val eTop = baseY - ellipseH
                                    val p = if (matches[idx] == areaSym) ellipseFillPaint else ellipseStrokePaint
                                    fc.drawOval(RectF(xOff, eTop, xOff + ellipseW, eTop + ellipseH), p)
                                    xOff += ellipseW
                                }
                            }
                        } else {
                            fc.drawText(lenPart, xOff, baseY, textPaint)
                            xOff += textPaint.measureText(lenPart)
                        }
                    }
                    if (lenIdx < lenParts.size - 1) {
                        fc.drawText(lenSym, xOff, smallBaseY, smallPaint)
                        xOff += smallPaint.measureText(lenSym)
                    }
                }
            } else {
                fc.drawText(drawText, padH, baseY, textPaint)
            }

            rl.setIcon(BitmapDrawable(m.context.resources, fb))
            rl.setAnchor(0.5f, 1.0f)
            rl.setVisible(true)
        } else rl.setVisible(false)
        m.invalidate()
    }
}
