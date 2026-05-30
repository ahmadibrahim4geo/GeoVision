package com.geovision.mobile.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geovision.mobile.R
import com.geovision.mobile.ui.navigation.BottomNavBar
import com.geovision.mobile.ui.navigation.BottomNavTab
import com.geovision.mobile.ui.screens.layers.FileType
import com.geovision.mobile.ui.screens.layers.LayerViewModel
import com.geovision.mobile.ui.screens.layers.fileTypeIcon
import com.geovision.mobile.ui.screens.layers.geomTypeIcon
import com.geovision.mobile.ui.screens.layers.FeatureRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onNavigateToTab: (BottomNavTab) -> Unit,
    onLayerClick: (String, String) -> Unit = { _, _ -> },
    viewModel: LayerViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.init(context) }

    val layers = viewModel.layers
    val totalLayers = layers.size
    val totalFeatures = layers.sumOf { it.featureCount }
    val detailsMap = layers.associate { it.id to viewModel.getCachedDetail(it.id) }

    var deleteConfirmLayerId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_screen_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceDim)
            )
        },
        bottomBar = {
            BottomNavBar(selectedTab = BottomNavTab.DETAILS, onTabSelected = onNavigateToTab)
        },
        containerColor = MaterialTheme.colorScheme.surfaceDim
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            SummaryCard(
                layers = layers,
                totalLayers = totalLayers,
                totalFeatures = totalFeatures,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (totalLayers == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LayersClear, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.details_no_layers), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.details_add_layer_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(24.dp))
                        FilledTonalButton(onClick = { onNavigateToTab(BottomNavTab.LAYERS) }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.details_add_layer_action))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(layers, key = { it.id }) { layer ->
                            LayerDetailCard(
                                layer = layer,
                                detail = detailsMap[layer.id],
                                onToggleVisibility = { viewModel.toggleVisibility(layer.id) },
                                onZoomTo = { viewModel.requestZoomToLayer(layer.id); onNavigateToTab(BottomNavTab.MAP) },
                                onOpenDetails = { onLayerClick(layer.id, layer.name) },
                                onDelete = { deleteConfirmLayerId = layer.id }
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

    if (deleteConfirmLayerId != null) {
        val layerName = layers.find { it.id == deleteConfirmLayerId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { deleteConfirmLayerId = null },
            title = { Text(stringResource(R.string.delete_layer), fontWeight = FontWeight.W700) },
            text = { Text(stringResource(R.string.delete_layer_confirm, layerName)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmLayerId?.let { viewModel.removeLayer(it) }
                        deleteConfirmLayerId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConfirmLayerId = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SummaryCard(layers: List<com.geovision.mobile.ui.screens.layers.Layer>, totalLayers: Int, totalFeatures: Int, modifier: Modifier = Modifier) {
    val points = layers.count { it.geomType == "Point" }
    val lines = layers.count { it.geomType == "Line" }
    val polygons = layers.count { it.geomType == "Polygon" }
    val fileTypeCount = layers.groupBy { it.fileType }.size

    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.details_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(Icons.Default.Layers, "$totalLayers", stringResource(R.string.nav_layers), MaterialTheme.colorScheme.primary)
                StatItem(Icons.Default.DatasetLinked, "$totalFeatures", stringResource(R.string.feature_item), MaterialTheme.colorScheme.secondary)
                StatItem(Icons.Default.FolderOpen, "$fileTypeCount", stringResource(R.string.file_types), MaterialTheme.colorScheme.tertiary)
            }
            if (points > 0 || lines > 0 || polygons > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (points > 0) GeomStat(Icons.Default.Circle, "$points", stringResource(R.string.details_geom_points), MaterialTheme.colorScheme.primary)
                    if (lines > 0) GeomStat(Icons.Default.Timeline, "$lines", stringResource(R.string.details_geom_lines), MaterialTheme.colorScheme.secondary)
                    if (polygons > 0) GeomStat(Icons.Default.Polyline, "$polygons", stringResource(R.string.details_geom_polygons), MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
private fun GeomStat(icon: ImageVector, value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.1f)) {
            Icon(icon, null, Modifier.padding(6.dp).size(18.dp), tint = color)
        }
        Column {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W800, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerDetailCard(
    layer: com.geovision.mobile.ui.screens.layers.Layer,
    detail: com.geovision.mobile.ui.screens.layers.LayerDetailInfo?,
    onToggleVisibility: () -> Unit,
    onZoomTo: () -> Unit,
    onOpenDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val fIcon = fileTypeIcon(layer.fileType)
    val gIcon = geomTypeIcon(layer.geomType)
    var showDeleteMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(layer.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(fIcon, null, Modifier.size(24.dp), tint = layer.color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(layer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (gIcon != null) {
                            Icon(gIcon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Text(layer.fileType.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Text("${layer.featureCount}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, Modifier.size(20.dp),
                        tint = if (layer.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onZoomTo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MyLocation, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                }
                Box {
                    IconButton(onClick = { showDeleteMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    DropdownMenu(expanded = showDeleteMenu, onDismissRequest = { showDeleteMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_layer), color = MaterialTheme.colorScheme.error) },
                            onClick = { showDeleteMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            if (detail != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailChip(Icons.Default.Map, "CRS", detail.crs)
                    DetailChip(Icons.Default.Public, stringResource(R.string.extent), detail.extent)
                }

                val propKeys = detail.features.flatMap { it.properties.keys }.distinct()
                if (propKeys.isNotEmpty()) {
                    Text(
                        "${stringResource(R.string.details)} (${propKeys.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        propKeys.take(8).forEach { key ->
                            val vals = detail.features.mapNotNull { it.properties[key] }.distinct()
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                                Text(
                                    "$key (${vals.size})",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.W500
                                )
                            }
                        }
                        if (propKeys.size > 8) {
                            Text("+${propKeys.size - 8}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Spacer(Modifier.height(6.dp))
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.tertiary)
                        Text(stringResource(R.string.loading_layer_data), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Box(Modifier.fillMaxWidth().clickable(onClick = onOpenDetails).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.OpenInFull, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.show_details), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DetailChip(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Text("$label:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
            Icon(icon, null, Modifier.padding(10.dp).size(24.dp), tint = color)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W900, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
