package com.geovision.mobile.ui.screens.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val EXPANDED_COLOR_PALETTE = listOf(
    Color(0xFF2196F3), Color(0xFF1976D2), Color(0xFF0D47A1), // Blues
    Color(0xFF4CAF50), Color(0xFF388E3C), Color(0xFF1B5E20), // Greens
    Color(0xFFFF9800), Color(0xFFF57C00), Color(0xFFE65100), // Oranges
    Color(0xFFE91E63), Color(0xFFC2185B), Color(0xFF880E4F), // Pinks
    Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFF4A148C), // Purples
    Color(0xFF00BCD4), Color(0xFF0097A7), Color(0xFF006064), // Cyans
    Color(0xFFF44336), Color(0xFFD32F2F), Color(0xFFB71C1C), // Reds
    Color(0xFF607D8B), Color(0xFF455A64), Color(0xFF263238), // Blue Greys
    Color(0xFFFFEB3B), Color(0xFFFBC02D), Color(0xFFF57F17), // Yellows
    Color(0xFFFFFFFF), Color(0xFF9E9E9E), Color(0xFF000000)  // Monochrome
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LayerPropertiesDialog(
    layer: Layer,
    availableFields: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSaveStyle: (LayerStyle) -> Unit,
    onRename: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Symbology, 1: Labels, 2: Info

    // State for styling
    var currentStyle by remember { mutableStateOf(layer.style) }
    var fillColor by remember { mutableStateOf(currentStyle.fillColor) }
    var strokeColor by remember { mutableStateOf(currentStyle.strokeColor) }
    var strokeWidth by remember { mutableFloatStateOf(currentStyle.strokeWidth) }
    var fillAlpha by remember { mutableFloatStateOf(currentStyle.fillAlpha) }
    var pointSize by remember { mutableFloatStateOf(currentStyle.pointSize) }
    var lineWidth by remember { mutableFloatStateOf(currentStyle.lineWidth) }
    var pointSymbol by remember { mutableStateOf(currentStyle.pointSymbol) }
    var strokeStyle by remember { mutableStateOf(currentStyle.strokeStyle) }

    // State for Labeling
    var showLabels by remember { mutableStateOf(currentStyle.showLabels) }
    var labelField by remember { mutableStateOf(currentStyle.labelField ?: availableFields.firstOrNull()) }
    var labelSize by remember { mutableFloatStateOf(currentStyle.labelSize) }
    var labelColor by remember { mutableStateOf(currentStyle.labelColor) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(layer.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = layer.color
                            )
                        }
                        Column {
                            Text(
                                "خصائص وتنسيق الطبقة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                layer.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("الترميز والألوان", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.ColorLens, null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("تسميات الخريطة", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.TextFields, null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("معلومات وبيانات", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Info, null, Modifier.size(18.dp)) }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Tab Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // ── التبويب الأول: الترميز والألوان ──
                            val isPoint = layer.geomType == "Point"
                            val isLine = layer.geomType == "Line"
                            val isPolygon = layer.geomType == "Polygon" || (!isPoint && !isLine)

                            // 1. أشكال الرموز للنقاط
                            if (isPoint) {
                                Text(
                                    "شكل الرمز النقطي (Point Symbol):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PointSymbol.values().forEach { symbol ->
                                        FilterChip(
                                            selected = pointSymbol == symbol,
                                            onClick = { pointSymbol = symbol },
                                            label = { Text(symbol.labelAr, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // حجم النقطة
                                Text("حجم النقطة: ${pointSize.toInt()}px", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = pointSize,
                                    onValueChange = { pointSize = it },
                                    valueRange = 4f..28f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 2. نمط وسُمك الخطوط (للخطوط والمضلعات)
                            if (isLine || isPolygon) {
                                Text(
                                    "نمط الخط / الحدود (Stroke Style):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StrokeStyle.values().forEach { style ->
                                        FilterChip(
                                            selected = strokeStyle == style,
                                            onClick = { strokeStyle = style },
                                            label = { Text(style.labelAr) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                val widthVal = if (isLine) lineWidth else strokeWidth
                                Text("سمك الخط / الإطار: ${widthVal.toInt()}px", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = widthVal,
                                    onValueChange = {
                                        if (isLine) lineWidth = it else strokeWidth = it
                                    },
                                    valueRange = 1f..14f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 3. شفافية التعبئة (للمضلعات)
                            if (isPolygon) {
                                Text(
                                    "شفافية تعبئة المضلع: ${(fillAlpha * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = fillAlpha,
                                    onValueChange = { fillAlpha = it },
                                    valueRange = 0.05f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 4. اختيار لون التعبئة (Fill Color)
                            Text(
                                if (isLine) "لون الخط الرئيسي:" else "لون التعبئة (Fill Color):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EXPANDED_COLOR_PALETTE.forEach { c ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .then(
                                                if (c == fillColor) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                else Modifier.border(0.5.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                                            )
                                            .clickable { fillColor = c }
                                    )
                                }
                            }

                            // 5. اختيار لون الإطار / الحدود (Stroke Color)
                            if (isPoint || isPolygon) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "لون الإطار الخارجي (Border / Stroke Color):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    EXPANDED_COLOR_PALETTE.forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .then(
                                                    if (c == strokeColor) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                    else Modifier.border(0.5.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                                                )
                                                .clickable { strokeColor = c }
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ── التبويب الثاني: تسميات المعالم على الخريطة ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "إظهار التسميات على الخريطة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "عرض نص توضيحي فوق كل معلم من جدول البيانات",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = showLabels,
                                    onCheckedChange = { showLabels = it }
                                )
                            }

                            if (showLabels) {
                                Spacer(Modifier.height(10.dp))

                                Text(
                                    "اختر حقل التسمية (Label Field):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (availableFields.isNotEmpty()) {
                                    var fieldDropdownExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = fieldDropdownExpanded,
                                        onExpandedChange = { fieldDropdownExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = labelField ?: "اختر حقلاً",
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldDropdownExpanded) },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = fieldDropdownExpanded,
                                            onDismissRequest = { fieldDropdownExpanded = false }
                                        ) {
                                            availableFields.forEach { f ->
                                                DropdownMenuItem(
                                                    text = { Text(f) },
                                                    onClick = {
                                                        labelField = f
                                                        fieldDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "سيتم استخدام معرّف المعلم (Feature ID) كاسم افتراضي لعدم توفر حقول نصية أخرى.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Text("حجم خط التسمية: ${labelSize.toInt()}sp", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = labelSize,
                                    onValueChange = { labelSize = it },
                                    valueRange = 8f..22f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                Text("لون خط التسمية:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Color.White to "أبيض",
                                        Color.Black to "أسود",
                                        Color(0xFFFFEB3B) to "أصفر",
                                        Color(0xFF00E676) to "أخضر فوسفوري",
                                        Color(0xFFFF5252) to "أحمر"
                                    ).forEach { (color, name) ->
                                        FilterChip(
                                            selected = labelColor == color,
                                            onClick = { labelColor = color },
                                            label = { Text(name) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ── التبويب الثالث: معلومات الطبقة وبياناتها ──
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InfoRow("اسم الطبقة", layer.name)
                                    InfoRow("نوع الملف", layer.fileType.label)
                                    InfoRow("نوع الهندسة", layer.geomType ?: "غير محدد")
                                    InfoRow("عدد المعالم", "${layer.featureCount} معلم")
                                    InfoRow("نظام الإسناد (CRS)", layer.crs)
                                    InfoRow("مسار التخزين", layer.filePath ?: "ذاكرة التطبيق")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val updated = currentStyle.copy(
                                fillColor = fillColor,
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                fillAlpha = fillAlpha,
                                pointSize = pointSize,
                                lineWidth = lineWidth,
                                pointSymbol = pointSymbol,
                                strokeStyle = strokeStyle,
                                showLabels = showLabels,
                                labelField = labelField,
                                labelSize = labelSize,
                                labelColor = labelColor
                            )
                            onSaveStyle(updated)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("حفظ الخصائص", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
