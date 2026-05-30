package com.geovision.mobile.ui.screens.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geovision.mobile.R
import com.geovision.mobile.ui.navigation.BottomNavBar
import com.geovision.mobile.ui.navigation.BottomNavTab
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerDetailsScreen(
    layerId: String,
    layerName: String,
    onNavigateBack: () -> Unit,
    onNavigateToTab: (BottomNavTab) -> Unit,
    onZoomToFeature: (String, Double, Double) -> Unit = { _, _, _ -> },
    viewModel: LayerViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFeature by remember { mutableStateOf<FeatureRow?>(null) }
    var sortColumn by remember { mutableStateOf<String?>(null) }
    var sortAscending by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    var currentPage by remember { mutableIntStateOf(0) }
    var pageSize by remember { mutableIntStateOf(50) }

    LaunchedEffect(layerId) {
        viewModel.loadDetailForLayer(layerId)
    }

    val detailInfo = viewModel.layerDetail

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.layer_details_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(stringResource(R.string.layer_details_subtitle, layerName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceDim)
            )
        },
        bottomBar = { BottomNavBar(selectedTab = BottomNavTab.DETAILS, onTabSelected = onNavigateToTab) },
        containerColor = MaterialTheme.colorScheme.surfaceDim
    ) { paddingValues ->
        if (detailInfo == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.loading_layer_data), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val allKeys = detailInfo.features.flatMap { it.properties.keys }.distinct()

            val filtered = detailInfo.features.filter { feature ->
                searchQuery.isBlank() ||
                    feature.id.contains(searchQuery, ignoreCase = true) ||
                    feature.properties.values.any { it.contains(searchQuery, ignoreCase = true) }
            }.let { list ->
                if (sortColumn != null) {
                    val col = sortColumn!!
                    if (sortAscending) list.sortedBy { it.properties[col] ?: "" }
                    else list.sortedByDescending { it.properties[col] ?: "" }
                } else list
            }

            val totalPages = (filtered.size + pageSize - 1) / pageSize
            val pagedFeatures = filtered.drop(currentPage * pageSize).take(pageSize)
            LaunchedEffect(searchQuery, sortColumn, sortAscending) { currentPage = 0 }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(bottom = 16.dp)) {
                item { FileInfoCard(detailInfo) }

                if (allKeys.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        FieldStatsCard(detailInfo, allKeys)
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        placeholder = { Text(stringResource(R.string.search_attributes), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_attributes), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { })
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.attribute_table, filtered.size),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (filtered.size != detailInfo.features.size) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "(${detailInfo.features.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (totalPages > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    "Page ${currentPage + 1}/$totalPages",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (filtered.isNotEmpty()) {
                    item {
                        DynamicTableHeader(
                            allKeys = allKeys,
                            sortColumn = sortColumn,
                            sortAscending = sortAscending,
                            scrollState = scrollState,
                            onSortClick = { key ->
                                if (sortColumn == key) sortAscending = !sortAscending
                                else { sortColumn = key; sortAscending = true }
                            }
                        )
                    }

                    items(pagedFeatures, key = { "${it.id}_${it.properties.hashCode()}" }) { feature ->
                        FeatureRow(
                            feature = feature,
                            allKeys = allKeys,
                            scrollState = scrollState,
                            isSelected = feature.id == selectedFeature?.id && feature.properties == selectedFeature?.properties,
                            onClick = { selectedFeature = feature }
                        )
                    }

                    if (totalPages > 1) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                                    enabled = currentPage > 0
                                ) {
                                    Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    "${currentPage + 1} / $totalPages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.W600,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                IconButton(
                                    onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages - 1) },
                                    enabled = currentPage < totalPages - 1
                                ) {
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                } else {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (searchQuery.isNotBlank()) stringResource(R.string.no_search_results_table)
                                else stringResource(R.string.no_attributes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedFeature != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedFeature = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    FeatureDetailsContent(
                        feature = selectedFeature!!,
                        onShowOnMap = { fid, lat, lon ->
                            selectedFeature = null
                            onZoomToFeature(fid, lat, lon)
                        },
                        onCopyCoords = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                        },
                        onDismiss = { selectedFeature = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldStatsCard(info: LayerDetailInfo, keys: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.field_statistics), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            keys.take(5).forEach { key ->
                val vals = info.features.mapNotNull { it.properties[key] }.filter { it.isNotBlank() }
                val unique = vals.distinct()
                val nullCount = info.features.count { it.properties[key].isNullOrBlank() }
                val mostCommon = vals.groupBy { it }.maxByOrNull { it.value.size }?.key
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                        Text("${stringResource(R.string.unique_values)}: ${unique.size}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)) {
                        Text("${stringResource(R.string.null_values)}: $nullCount", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
                if (mostCommon != null && unique.size > 1) {
                    Text("${stringResource(R.string.most_common_value)}: $mostCommon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(start = 8.dp))
                }
                if (key != keys.take(5).last()) Spacer(Modifier.height(2.dp))
            }
            if (keys.size > 5) {
                Text("+${keys.size - 5}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun FileInfoCard(info: LayerDetailInfo) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            FileInfoLabel(stringResource(R.string.file_name), info.fileName)
            Spacer(Modifier.height(6.dp))
            FileInfoLabel(stringResource(R.string.file_path), info.filePath)
            Spacer(Modifier.height(6.dp))
            FileInfoLabel(stringResource(R.string.crs), info.crs)
            Spacer(Modifier.height(6.dp))
            FileInfoLabel(stringResource(R.string.extent), info.extent)
        }
    }
}

@Composable
private fun FileInfoLabel(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W500, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.65f))
    }
}

@Composable
private fun DynamicTableHeader(
    allKeys: List<String>,
    sortColumn: String?,
    sortAscending: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onSortClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.attr_id), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
        Row(Modifier.horizontalScroll(scrollState)) {
            allKeys.forEach { key ->
                val active = sortColumn == key
                Row(
                    modifier = Modifier.width(120.dp).clickable { onSortClick(key) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(key, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (active) FontWeight.W700 else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (active) {
                        Icon(
                            if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    feature: FeatureRow,
    allKeys: List<String>,
    scrollState: androidx.compose.foundation.ScrollState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(feature.id, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(72.dp))
            Row(Modifier.horizontalScroll(scrollState)) {
                allKeys.forEach { key ->
                    val value = feature.properties[key]
                    val isEmpty = value.isNullOrBlank()
                    Text(
                        value ?: "—",
                        modifier = Modifier.width(120.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEmpty) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun FeatureDetailsContent(
    feature: FeatureRow,
    onShowOnMap: (String, Double, Double) -> Unit,
    onCopyCoords: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val coords = parsePointCoords(feature.geometryCoordinates)
    val isPoint = feature.geometryType == "Point" && coords != null

    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
        Text(stringResource(R.string.feature_details_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#${feature.id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (feature.geometryType != null) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)) {
                    Text(feature.geometryType, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        if (coords != null) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.coord_readout), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            val (lat, lng) = coords
            val dd = formatCoordDD(lat, lng)
            val dms = formatCoordDMS(lat, lng)
            InfoRow("DD", dd)
            Spacer(Modifier.height(4.dp))
            InfoRow("DMS", dms)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onShowOnMap(feature.id, lat, lng) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.MyLocation, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.feature_show_on_map))
                }
                OutlinedButton(
                    onClick = { onCopyCoords(dd) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.feature_copy_coords))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!isPoint && coords == null && feature.geometryCoordinates != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MyLocation, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text(feature.geometryType ?: "Geom", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (feature.properties.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.feature_properties), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            feature.properties.forEach { (key, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.35f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.65f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(value, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun parsePointCoords(coordStr: String?): Pair<Double, Double>? {
    if (coordStr == null) return null
    return try {
        val arr = JSONArray(coordStr)
        if (arr.length() >= 2) {
            val lng = arr.getDouble(0)
            val lat = arr.getDouble(1)
            lat to lng
        } else null
    } catch (e: Exception) {
        try {
            val cleaned = coordStr.trim('[', ']', ' ')
            val parts = cleaned.split(",").map { it.trim() }
            if (parts.size >= 2) parts[1].toDouble() to parts[0].toDouble()
            else null
        } catch (_: Exception) { null }
    }
}

private fun formatCoordDD(lat: Double, lng: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lngDir = if (lng >= 0) "E" else "W"
    return "%.6f%s, %.6f%s".format(kotlin.math.abs(lat), latDir, kotlin.math.abs(lng), lngDir)
}

private fun formatCoordDMS(lat: Double, lng: Double): String {
    fun toDMS(coord: Double): String {
        val abs = kotlin.math.abs(coord)
        val d = abs.toInt()
        val m = ((abs - d) * 60).toInt()
        val s = ((abs - d - m / 60.0) * 3600)
        return "%d°%d'%.1f\"".format(d, m, s)
    }
    val latDir = if (lat >= 0) "N" else "S"
    val lngDir = if (lng >= 0) "E" else "W"
    return "%s%s, %s%s".format(toDMS(lat), latDir, toDMS(lng), lngDir)
}
