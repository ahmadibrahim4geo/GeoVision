package com.geovision.esri.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun EsriViewerScreen(
    report: EsriPackageReport?,
    cacheStats: EsriCacheStats?,
    cacheLimitBytes: Long,
    recentPackages: List<EsriRecentPackage>,
    onOpenPackage: () -> Unit,
    onOpenRecent: (EsriRecentPackage) -> Unit,
    onRemoveRecent: (EsriRecentPackage) -> Unit,
    onClearCache: () -> Unit,
    onCacheLimitChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.esri_local_viewer_title), style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Text(
                stringResource(R.string.esri_local_viewer_mode_description),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenPackage, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.esri_open_package))
                }
                Button(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.esri_clear_cache))
                }
            }
        }
        cacheStats?.let { stats ->
            item { CacheStatsCard(stats, cacheLimitBytes, onCacheLimitChanged) }
        }
        if (recentPackages.isNotEmpty()) {
            item { RecentPackagesCard(recentPackages, onOpenRecent, onRemoveRecent) }
        }
        report?.let { packageReport ->
            item { PackageReportCard(packageReport) }
        }
    }
}

@Composable
private fun CacheStatsCard(
    stats: EsriCacheStats,
    cacheLimitBytes: Long,
    onCacheLimitChanged: (Long) -> Unit
) {
    var customGbText by remember(cacheLimitBytes) {
        mutableStateOf((cacheLimitBytes / BYTES_IN_GB).toString())
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.esri_storage_cache_settings), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.esri_cache_limit, cacheLimitBytes.toHumanSize()))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "1 GB" to (1L * BYTES_IN_GB),
                    "2 GB" to (2L * BYTES_IN_GB),
                    "5 GB" to (5L * BYTES_IN_GB),
                    "10 GB" to (10L * BYTES_IN_GB)
                ).forEach { (label, bytes) ->
                    AssistChip(onClick = { onCacheLimitChanged(bytes) }, label = { Text(label) })
                }
            }
            OutlinedTextField(
                value = customGbText,
                onValueChange = { value ->
                    customGbText = value.filter { it.isDigit() }.take(4)
                    customGbText.toLongOrNull()
                        ?.takeIf { it > 0L }
                        ?.let { onCacheLimitChanged(it * BYTES_IN_GB) }
                },
                label = { Text(stringResource(R.string.esri_custom_cache_limit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.esri_used_cache, stats.cacheSizeBytes.toHumanSize()))
            Text(stringResource(R.string.esri_packages_size, stats.packagesSizeBytes.toHumanSize()))
            Text(stringResource(R.string.esri_temp_size, stats.tempSizeBytes.toHumanSize()))
            Text(stringResource(R.string.esri_reports_size, stats.reportsSizeBytes.toHumanSize()))
            Text(stringResource(R.string.esri_available_storage, stats.availableDeviceBytes.toHumanSize()))
        }
    }
}

@Composable
private fun RecentPackagesCard(
    recentPackages: List<EsriRecentPackage>,
    onOpenRecent: (EsriRecentPackage) -> Unit,
    onRemoveRecent: (EsriRecentPackage) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.esri_recent_packages), style = MaterialTheme.typography.titleMedium)
            recentPackages.forEach { item ->
                Card {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text("${item.packageType.name} - ${item.sizeBytes.toHumanSize()}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpenRecent(item) }) { Text(stringResource(R.string.esri_open)) }
                            TextButton(onClick = { onRemoveRecent(item) }) { Text(stringResource(R.string.esri_remove_recent)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageReportCard(report: EsriPackageReport) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(report.packageName, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(onClick = {}, label = { Text(report.packageType.name) })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.esri_loaded_count, report.loadedCount)) })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.esri_failed_count, report.failedCount)) })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.esri_skipped_count, report.skippedCount)) })
            }
            report.warnings.forEach { warning ->
                Text(warning, style = MaterialTheme.typography.bodySmall)
            }
            report.errors.forEach { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.layers.forEach { layer ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (layer.status) {
                                EsriLayerLoadStatus.LOADED -> MaterialTheme.colorScheme.surfaceVariant
                                EsriLayerLoadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                                EsriLayerLoadStatus.SKIPPED -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(layer.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.esri_layer_status_type, layer.status.name, layer.type),
                                style = MaterialTheme.typography.bodySmall
                            )
                            layer.licenseWarning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            layer.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

private fun Long.toHumanSize(): String {
    val mb = 1024.0 * 1024.0
    return if (this >= BYTES_IN_GB) {
        String.format(Locale.US, "%.2f GB", this / BYTES_IN_GB.toDouble())
    } else {
        String.format(Locale.US, "%.1f MB", this / mb)
    }
}

private const val BYTES_IN_GB = 1024L * 1024L * 1024L
