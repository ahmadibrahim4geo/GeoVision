package com.geovision.mobile.ui.screens.layers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.geovision.mobile.data.LayerExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExportLayerDialog(
    layerName: String,
    features: List<FeatureRow>,
    crs: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedFormat by remember { mutableStateOf(LayerExporter.ExportFormat.GEOJSON) }
    var isExporting by remember { mutableStateOf(false) }
    var tempExportFile by remember { mutableStateOf<File?>(null) }

    // Launcher for saving directly to user chosen destination
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { destUri ->
        if (destUri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val exportFile = LayerExporter.exportToFile(
                        context, selectedFormat, layerName, features, crs
                    )
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        exportFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "تم حفظ الملف بنجاح", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "فشل حفظ الملف: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "تصدير بيانات الطبقة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$layerName (${features.size} معالم)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "صيغة التصدير المطلوبة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))
                LayerExporter.ExportFormat.values().forEach { format ->
                    val isSelected = format == selectedFormat
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFormat = format }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = format.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (format) {
                                        LayerExporter.ExportFormat.GEOJSON -> "معيار الويب الجغرافي الشامل لكافة الهندسات والسمات"
                                        LayerExporter.ExportFormat.CSV -> "جدول بيانات يتضمن إحداثيات X, Y, Z وكافة الحقول"
                                        LayerExporter.ExportFormat.KML -> "صيغة Google Earth متوافقة مع كافة تطبيقات الملاحة"
                                        LayerExporter.ExportFormat.GEOPACKAGE -> "قاعدة بيانات مكانية قياسية OGC قابلة للفتح في GIS"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (isExporting) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }

                        // Share action
                        FilledTonalButton(
                            onClick = {
                                isExporting = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val file = LayerExporter.exportToFile(
                                            context, selectedFormat, layerName, features, crs
                                        )
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            shareExportFile(context, file, selectedFormat)
                                            onDismiss()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            Toast.makeText(context, "فشل تجهيز التصدير: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("مشاركة")
                        }

                        // Save action
                        Button(
                            onClick = {
                                val safeName = layerName.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                                createDocLauncher.launch("${safeName}.${selectedFormat.extension}")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    }
}

private fun shareExportFile(context: Context, file: File, format: LayerExporter.ExportFormat) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "مشاركة ملف ${file.name}")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر مشاركة الملف: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
