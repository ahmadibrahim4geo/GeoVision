package com.geovision.mobile.ui.screens.layers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geovision.mobile.R

private val COLOR_PALETTE = listOf(
    Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFE91E63),
    Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF607D8B),
    Color(0xFFCDDC39), Color(0xFF795548), Color(0xFF3F51B5), Color(0xFF009688)
)

fun fileTypeIcon(type: FileType) = when (type) {
    FileType.SHAPEFILE -> Icons.Default.Layers
    FileType.GEOJSON -> Icons.Default.Code
    FileType.KML -> Icons.Default.Place
    FileType.GPX -> Icons.Default.Timeline
    FileType.PHOTO -> Icons.Default.PhotoCamera
    FileType.GEOPACKAGE -> Icons.Default.Storage
    FileType.GEODATABASE -> Icons.Default.FolderOpen
}

/** أيقونة صغيرة تُظهر نوع الهندسة (Point/Line/Polygon) بجوار اسم الطبقة.
 *  تحل محل النص "Shapefile/GeoJSON/..." لتوفير مساحة وإعطاء معلومات أكثر فائدة.
 *  @param type أحد values من deriveGeomType: "Point", "Line", "Polygon", أو null
 *  @return ImageVector للأيقونة المناسبة، أو null إن كان النوع غير معروف */
fun geomTypeIcon(type: String?) = when (type) {
    "Point" -> Icons.Default.FmdGood
    "Line" -> Icons.Default.ShowChart
    "Polygon" -> Icons.Default.CropSquare
    else -> null
}

fun fileTypeBgColor(type: FileType, scheme: ColorScheme) = when (type) {
    FileType.SHAPEFILE -> scheme.primary
    FileType.GEOJSON -> scheme.secondary
    FileType.KML -> scheme.tertiary
    FileType.GPX -> scheme.error
    FileType.PHOTO -> scheme.outline
    FileType.GEOPACKAGE -> scheme.primary
    FileType.GEODATABASE -> scheme.tertiary
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LayerCard(
    layer: Layer,
    geometrySummary: String?,
    onOpenDetails: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onTransparencyChanged: (String, Float) -> Unit,
    onDelete: (String) -> Unit,
    onReloadLayer: (String) -> Unit = {},
    onZoomToLayer: (String) -> Unit = {},
    onColorChange: (String, Color) -> Unit = { _, _ -> },
                    onPointSizeChanged: (String, Float) -> Unit = { _, _ -> },
                    onLineWidthChanged: (String, Float) -> Unit = { _, _ -> },
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var localTransparency by remember(layer.id) { mutableFloatStateOf(layer.transparency) }
    var isTransparencyDragging by remember { mutableStateOf(false) }
    if (!isTransparencyDragging && localTransparency != layer.transparency) {
        localTransparency = layer.transparency
    }
    var deleteRequested by remember(layer.id) { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                if (!deleteRequested) {
                    deleteRequested = true
                    onDelete(layer.id)
                }
                false
            } else {
                false
            }
        }
    )
    val typeColor = fileTypeBgColor(layer.fileType, MaterialTheme.colorScheme)

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, null, Modifier.padding(end = 20.dp).size(28.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column {
                Box(Modifier.fillMaxWidth().height(3.dp).background(typeColor).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)))

                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DragHandle, "سحب لإعادة الترتيب",
                        Modifier.size(24.dp).pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount.y) },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() }
                            )
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(typeColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(fileTypeIcon(layer.fileType), null, Modifier.size(20.dp), tint = typeColor)
                    }
                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(layer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                            val gIcon = geomTypeIcon(layer.geomType)
                            if (gIcon != null) {
                                Icon(gIcon, layer.geomType, Modifier.size(16.dp), tint = typeColor.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.FeaturedPlayList, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text("${formatCount(layer.featureCount)} ${stringResource(R.string.feature_item)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = { onReloadLayer(layer.id) }, modifier = Modifier.size(32.dp), enabled = layer.progressPercent >= 1f || layer.progressPercent == 0f) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh), Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onZoomToLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MyLocation, stringResource(R.string.show_on_map), Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                    Switch(checked = layer.isVisible, onCheckedChange = { onToggleVisibility(layer.id) },
                        modifier = Modifier.padding(end = 2.dp),
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary, checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = if (expanded) 0.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (geometrySummary != null) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            geometrySummary.split(" · ").forEach { item ->
                                Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = 0.08f)) {
                                    Text(item, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = typeColor.copy(alpha = 0.8f), fontWeight = FontWeight.W600)
                                }
                            }
                        }
                    } else { Spacer(Modifier.weight(1f)) }

                    if (layer.progressPercent < 1f) {
                        CircularProgressIndicator(progress = { layer.progressPercent }, modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.tertiary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                        Spacer(Modifier.width(4.dp))
                        Text("${(layer.progressPercent * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.W600)
                    }
                }

                AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))

                        Text(stringResource(R.string.transparency), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VisibilityOff, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Slider(value = localTransparency,
                                onValueChange = { localTransparency = it; isTransparencyDragging = true },
                                onValueChangeFinished = { isTransparencyDragging = false; onTransparencyChanged(layer.id, localTransparency) },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f).height(24.dp), colors = SliderDefaults.colors(thumbColor = typeColor, activeTrackColor = typeColor))
                            Icon(Icons.Default.Visibility, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = 0.1f)) {
                                Text("${(localTransparency * 100).toInt()}%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W700, color = typeColor)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(stringResource(R.string.change_color), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
                        ) {
                            COLOR_PALETTE.forEach { c ->
                                Box(Modifier.size(32.dp).clip(CircleShape)
                                    .background(c)
                                    .then(if (c == layer.color) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { onColorChange(layer.id, c) })
                            }

                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))

                        Text(stringResource(R.string.symbol_properties), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))

                        SymbolSlider(
                            label = stringResource(R.string.point_size),
                            value = layer.pointSize,
                            valueRange = 2f..20f,
                            formatValue = { "${it.toInt()}px" },
                            onValueChange = { onPointSizeChanged(layer.id, it) },
                            typeColor = typeColor
                        )
                        Spacer(Modifier.height(4.dp))
                        SymbolSlider(
                            label = stringResource(R.string.line_width),
                            value = layer.lineWidth,
                            valueRange = 1f..12f,
                            formatValue = { "${it.toInt()}px" },
                            onValueChange = { onLineWidthChanged(layer.id, it) },
                            typeColor = typeColor
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onOpenDetails, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                Icon(Icons.Default.Info, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.details), style = MaterialTheme.typography.labelMedium)
                            }
                            val errorColor = MaterialTheme.colorScheme.error
                            OutlinedButton(onClick = { onDelete(layer.id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor),
                                border = BorderStroke(1.dp, errorColor)) {
                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.delete_layer), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String,
    onValueChange: (Float) -> Unit,
    typeColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange,
            modifier = Modifier.weight(1f).height(24.dp), colors = SliderDefaults.colors(thumbColor = typeColor, activeTrackColor = typeColor))
        Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = 0.1f)) {
            Text(formatValue(value), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W700, color = typeColor)
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
    count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K"
    else -> count.toString()
}
