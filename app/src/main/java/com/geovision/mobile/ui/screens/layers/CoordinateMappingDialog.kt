package com.geovision.mobile.ui.screens.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.geovision.mobile.data.ExcelParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinateMappingDialog(
    fileName: String,
    inspection: ExcelParser.TableInspection,
    onDismiss: () -> Unit,
    onConfirm: (ExcelParser.CoordinateMapping) -> Unit
) {
    val headers = inspection.headers
    if (headers.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("خطأ في قراءة الجدول") },
            text = { Text("لم يتم العثور على أعمدة صالحة في ملف $fileName.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        )
        return
    }

    var selectedX by remember {
        mutableIntStateOf(
            if (inspection.suggestedXIndex in headers.indices) inspection.suggestedXIndex
            else 0
        )
    }
    var selectedY by remember {
        mutableIntStateOf(
            if (inspection.suggestedYIndex in headers.indices) inspection.suggestedYIndex
            else if (headers.size > 1) 1 else 0
        )
    }
    var selectedZ by remember {
        mutableIntStateOf(inspection.suggestedZIndex)
    }
    var selectedLabel by remember {
        mutableIntStateOf(inspection.suggestedLabelIndex)
    }

    // CRS Selection
    var selectedCrsType by remember { mutableStateOf("WGS84") } // "WGS84", "UTM", "EGYPT_RED", "CUSTOM"
    var selectedUtmZone by remember { mutableIntStateOf(36) }
    var isUtmNorth by remember { mutableStateOf(true) }
    var customEpsg by remember { mutableStateOf("4326") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── ترويسة النافذة ──
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                "تحديد إحداثيات الجدول",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                fileName,
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

                Spacer(Modifier.height(10.dp))

                // ── تنبيه توضيحي ──
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "اختر عمود الإحداثي السيني (X) والصادي (Y). سيتم ربط كافة الأعمدة الأخرى تلقائياً بالنقاط للاستعراض في جدول البيانات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── محتوى الاختيارات القابل للتمرير ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. عمود X وعمود Y (إلزامي)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // X Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "الإحداثي السيني (X / Lon) *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            DropdownSelector(
                                items = headers,
                                selectedIndex = selectedX,
                                onSelect = { selectedX = it }
                            )
                        }

                        // Y Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "الإحداثي الصادي (Y / Lat) *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            DropdownSelector(
                                items = headers,
                                selectedIndex = selectedY,
                                onSelect = { selectedY = it }
                            )
                        }
                    }

                    // 2. عمود Z وعمود التسمية (اختياري)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Z Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "الارتفاع (Z / Elev) - اختياري",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            DropdownSelector(
                                items = listOf("بدون منسوب Z") + headers,
                                selectedIndex = if (selectedZ >= 0) selectedZ + 1 else 0,
                                onSelect = { selectedZ = it - 1 }
                            )
                        }

                        // Label Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "حقل اسم المعلم (Label) - اختياري",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            DropdownSelector(
                                items = listOf("تسمية تلقائية") + headers,
                                selectedIndex = if (selectedLabel >= 0) selectedLabel + 1 else 0,
                                onSelect = { selectedLabel = it - 1 }
                            )
                        }
                    }

                    // 3. نظام الإحداثيات (CRS / Projection)
                    Column {
                        Text(
                            "نظام إحداثيات الملف (CRS)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCrsType == "WGS84",
                                onClick = { selectedCrsType = "WGS84" },
                                label = { Text("درجات جغرافية WGS84") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCrsType == "UTM",
                                onClick = { selectedCrsType = "UTM" },
                                label = { Text("مسقط متري UTM") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCrsType == "EGYPT_RED",
                                onClick = { selectedCrsType = "EGYPT_RED" },
                                label = { Text("مصر Red Belt (22992)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCrsType == "CUSTOM",
                                onClick = { selectedCrsType = "CUSTOM" },
                                label = { Text("رمز EPSG مخصص") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (selectedCrsType == "UTM") {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = selectedUtmZone.toString(),
                                    onValueChange = { selectedUtmZone = it.toIntOrNull()?.coerceIn(1, 60) ?: 36 },
                                    label = { Text("نطاق UTM Zone (1-60)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = isUtmNorth, onClick = { isUtmNorth = true })
                                    Text("شمال (N)", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.width(8.dp))
                                    RadioButton(selected = !isUtmNorth, onClick = { isUtmNorth = false })
                                    Text("جنوب (S)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else if (selectedCrsType == "CUSTOM") {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customEpsg,
                                onValueChange = { customEpsg = it },
                                label = { Text("كود EPSG (مثال: 3857 أو 32636)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // 4. جدول معاينة البيانات
                    if (inspection.previewRows.isNotEmpty()) {
                        Column {
                            Text(
                                "معاينة أولية للبيانات (${inspection.previewRows.size} صفوف):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Column {
                                    // Row Headers
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        headers.forEachIndexed { idx, h ->
                                            val isX = idx == selectedX
                                            val isY = idx == selectedY
                                            val isZ = idx == selectedZ
                                            val isLbl = idx == selectedLabel

                                            val tag = when {
                                                isX -> " [X]"
                                                isY -> " [Y]"
                                                isZ -> " [Z]"
                                                isLbl -> " [Label]"
                                                else -> ""
                                            }

                                            Text(
                                                text = h + tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isX || isY) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isX || isY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .padding(horizontal = 4.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Preview Rows
                                    inspection.previewRows.forEach { rowMap ->
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            headers.forEachIndexed { idx, h ->
                                                val isHighlighted = idx == selectedX || idx == selectedY
                                                Text(
                                                    text = rowMap[h] ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .width(110.dp)
                                                        .padding(horizontal = 4.dp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── أزرار الإجراءات ──
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
                            val epsg = when (selectedCrsType) {
                                "WGS84" -> 4326
                                "UTM" -> if (isUtmNorth) 32600 + selectedUtmZone else 32700 + selectedUtmZone
                                "EGYPT_RED" -> 22992
                                "CUSTOM" -> customEpsg.toIntOrNull() ?: 4326
                                else -> 4326
                            }

                            onConfirm(
                                ExcelParser.CoordinateMapping(
                                    xColumnIndex = selectedX,
                                    yColumnIndex = selectedY,
                                    zColumnIndex = selectedZ,
                                    labelColumnIndex = selectedLabel,
                                    epsgCode = epsg,
                                    utmZone = if (selectedCrsType == "UTM") selectedUtmZone else 0,
                                    isUtmNorth = isUtmNorth
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedX in headers.indices && selectedY in headers.indices
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("استيراد الطبقة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentText = if (selectedIndex in items.indices) items[selectedIndex] else "اختر عموداً"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            item,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
