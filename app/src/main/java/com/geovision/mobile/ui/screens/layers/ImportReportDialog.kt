package com.geovision.mobile.ui.screens.layers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geovision.mobile.data.ImportReport
import com.geovision.mobile.data.ImportStatus

@Composable
fun ImportReportDialog(
    report: ImportReport,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Report, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("تقرير الاستيراد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800)
                    Text(
                        "${report.sourceType} - ${report.sourceName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (report.warnings.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            report.warnings.take(3).forEach {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(Modifier.width(6.dp))
                                    Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(report.layers, key = { it.id }) { layer ->
                        val color = when (layer.status) {
                            ImportStatus.IMPORTED, ImportStatus.SUPPORTED -> MaterialTheme.colorScheme.primary
                            ImportStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                            ImportStatus.FAILED, ImportStatus.UNSUPPORTED -> MaterialTheme.colorScheme.error
                        }
                        val icon = when (layer.status) {
                            ImportStatus.IMPORTED, ImportStatus.SUPPORTED -> Icons.Default.CheckCircle
                            ImportStatus.WARNING -> Icons.Default.Warning
                            ImportStatus.FAILED, ImportStatus.UNSUPPORTED -> Icons.Default.Error
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.42f)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, null, Modifier.size(20.dp), tint = color)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(layer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            listOfNotNull(
                                                layer.geometryType ?: "هندسة غير معروفة",
                                                layer.featureCount?.let { "$it عنصر" },
                                                layer.crs?.displayName
                                            ).joinToString(" - "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        enabled = false,
                                        label = { Text(importStatusLabel(layer.status)) },
                                        leadingIcon = { Icon(Icons.Default.Info, null, Modifier.size(16.dp)) }
                                    )
                                    layer.message?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            it,
                                            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("موافق")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp
    )
}

private fun importStatusLabel(status: ImportStatus): String = when (status) {
    ImportStatus.SUPPORTED -> "مدعوم"
    ImportStatus.IMPORTED -> "تم الاستيراد"
    ImportStatus.WARNING -> "تحذير"
    ImportStatus.FAILED -> "فشل"
    ImportStatus.UNSUPPORTED -> "غير مدعوم"
}
