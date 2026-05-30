package com.geovision.mobile.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geovision.mobile.R

@Composable
fun FiContent(id: String, properties: Map<String, String>, onNavigateToLayerDetails: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.feature_info_title, id), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))) {
            Column(Modifier.padding(16.dp)) {
                FRow(stringResource(R.string.feature_id), id.ifEmpty { "-" })
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                properties.entries.forEachIndexed { idx, (key, value) ->
                    if (idx > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    FRow(key, value)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToLayerDetails, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.TableChart, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.show_in_table))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun FRow(l: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(l, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}
