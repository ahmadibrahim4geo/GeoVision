package com.geovision.mobile.ui.screens.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.geovision.mobile.ui.screens.map.CtrlBtn
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.geovision.mobile.R
import com.geovision.mobile.data.PreferencesManager
import com.geovision.mobile.ui.navigation.BottomNavBar
import com.geovision.mobile.ui.navigation.BottomNavTab
import com.geovision.mobile.ui.screens.map.defaultTileKey
import com.geovision.mobile.ui.screens.map.tileSources
import com.geovision.mobile.ui.screens.map.coordMarkerIcon
import com.geovision.mobile.ui.screens.map.COORD_COLOR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeoCalculatorScreen(
    onNavigateToTab: (BottomNavTab) -> Unit,
    viewModel: CalculatorViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun onCopied(label: String = "") {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = if (label.isNotEmpty()) "✅ تم النسخ: $label" else "✅ تم النسخ",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.calculator_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceDim
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = BottomNavTab.CALCULATOR,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceDim
    ) { paddingValues ->
        val cardShape = RoundedCornerShape(16.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InputCard(
                    cardShape = cardShape,
                    state = state,
                    onTextChange = { viewModel.updateInput(it) },
                    onConvert = { viewModel.convert() },
                    onClear = { viewModel.clear() },
                    onToggleFormat = { viewModel.toggleFormat(it) },
                    onSelectAllFormats = { viewModel.selectAllFormats() },
                    onSetLatHemisphere = { viewModel.setLatHemisphere(it) },
                    onSetLonHemisphere = { viewModel.setLonHemisphere(it) },
                    onSetUtmZone = { viewModel.setUtmZone(it) },
                    onSetUtmHemisphere = { viewModel.setUtmHemisphere(it) },
                    onCopied = { onCopied(it) },
                    context = context
                )

                ResultsCard(
                    cardShape = cardShape,
                    results = state.results,
                    visibleFormats = state.visibleFormats,
                    onCopied = { onCopied(it) },
                    context = context,
                    viewModel = viewModel
                )
            }

            MapCard(
                cardShape = cardShape,
                results = state.results,
                context = context
            )
        }
    }
}

// ═══════════════════════════════════════════
// Input Section
// ═══════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputCard(
    cardShape: RoundedCornerShape,
    state: CalculatorUiState,
    onTextChange: (String) -> Unit,
    onConvert: () -> Unit,
    onClear: () -> Unit,
    onToggleFormat: (OutputFormat) -> Unit,
    onSelectAllFormats: () -> Unit,
    onSetLatHemisphere: (Char) -> Unit,
    onSetLonHemisphere: (Char) -> Unit,
    onSetUtmZone: (Int) -> Unit,
    onSetUtmHemisphere: (Char) -> Unit,
    onCopied: (String) -> Unit,
    context: Context
) {
    ElevatedCard(
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                icon = Icons.Default.EditNote,
                title = stringResource(R.string.calculator_input_section)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.inputText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Start,
                    fontSize = 13.sp
                ),
                placeholder = {
                    Text(
                        stringResource(R.string.calculator_input_placeholder),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textDirection = TextDirection.Ltr
                        ),
                        textAlign = TextAlign.Start
                    )
                },
                maxLines = 8,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConvert() })
            )

            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = stringResource(R.string.calculator_input_formats_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.calculator_format_dd), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.calculator_format_dms), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.calculator_format_ddm), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.calculator_format_utm), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.calculator_hemisphere_lon),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf('E', 'W').forEach { h ->
                    FilterChip(
                        selected = state.lonHemisphere == h,
                        onClick = { onSetLonHemisphere(h) },
                        label = { Text(if (h == 'E') "E (شرق)" else "W (غرب)", fontSize = 12.sp) },
                        leadingIcon = if (state.lonHemisphere == h) {
                            { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.calculator_hemisphere_lat),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf('N', 'S').forEach { h ->
                    FilterChip(
                        selected = state.latHemisphere == h,
                        onClick = { onSetLatHemisphere(h) },
                        label = { Text(if (h == 'N') "N (شمال)" else "S (جنوب)", fontSize = 12.sp) },
                        leadingIcon = if (state.latHemisphere == h) {
                            { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.calculator_utm_zone),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.utmZone.toString(),
                    onValueChange = { it.toIntOrNull()?.let { n -> onSetUtmZone(n) } },
                    modifier = Modifier.width(72.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                IconButton(
                    onClick = { onSetUtmZone(state.utmZone - 1) },
                    enabled = state.utmZone > 1,
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Remove, null, Modifier.size(18.dp)) }
                IconButton(
                    onClick = { onSetUtmZone(state.utmZone + 1) },
                    enabled = state.utmZone < 60,
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                Spacer(Modifier.width(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf('N', 'S').forEach { h ->
                        FilterChip(
                            selected = state.utmHemisphere == h,
                            onClick = { onSetUtmHemisphere(h) },
                            label = { Text(if (h == 'N') "N (شمال)" else "S (جنوب)", fontSize = 12.sp) },
                            leadingIcon = if (state.utmHemisphere == h) {
                                { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.calculator_output_format),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = state.selectedFormats.isEmpty(),
                    onClick = onSelectAllFormats,
                    label = { Text(stringResource(R.string.calculator_format_all), fontSize = 12.sp) },
                    leadingIcon = if (state.selectedFormats.isEmpty()) {
                        { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                    } else null
                )
                OutputFormat.entries.forEach { format ->
                    val sel = format in state.visibleFormats
                    FilterChip(
                        selected = sel,
                        onClick = { onToggleFormat(format) },
                        label = { Text(format.name, fontSize = 12.sp) },
                        leadingIcon = if (sel) {
                            { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = stringResource(R.string.calculator_convert),
                    icon = Icons.Default.PlayArrow,
                    filled = true,
                    onClick = onConvert
                )
                ActionButton(
                    text = stringResource(R.string.calculator_copy_input),
                    icon = Icons.Default.ContentCopy,
                    filled = false,
                    onClick = {
                        if (state.inputText.isNotBlank()) {
                            copyToClipboard(context, state.inputText)
                            onCopied("النص المدخل")
                        }
                    }
                )
                ActionButton(
                    text = stringResource(R.string.calculator_clear),
                    icon = Icons.Default.Delete,
                    filled = false,
                    onClick = onClear
                )
                ActionButton(
                    text = stringResource(R.string.calculator_auto_detect),
                    icon = Icons.Default.Search,
                    filled = false,
                    onClick = onConvert
                )
            }

            if (state.statusMessage.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        state.statusMessage.contains("✅") ->
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                        state.statusMessage.contains("❌") || state.statusMessage.contains("⚠️") ->
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Results Section
// ═══════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultsCard(
    cardShape: RoundedCornerShape,
    results: List<ConvertedPoint>,
    visibleFormats: Set<OutputFormat>,
    onCopied: (String) -> Unit,
    context: Context,
    viewModel: CalculatorViewModel
) {
    ElevatedCard(
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                icon = Icons.Default.TableChart,
                title = "${stringResource(R.string.calculator_results_section)} (${results.size})"
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                val headerBg = MaterialTheme.colorScheme.surfaceContainerHigh
                val evenRowBg = MaterialTheme.colorScheme.surface
                val oddRowBg = MaterialTheme.colorScheme.surfaceContainerLowest

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                ) {
                    TableHeader("#", 38.dp)
                    VDivider()
                    TableHeader(stringResource(R.string.calculator_col_original), 130.dp)
                    VDivider()
                    if (OutputFormat.DD in visibleFormats) {
                        TableHeader("DD", 150.dp)
                        VDivider()
                    }
                    if (OutputFormat.DMS in visibleFormats) {
                        TableHeader("DMS", 190.dp)
                        VDivider()
                    }
                    if (OutputFormat.DDM in visibleFormats) {
                        TableHeader("DDM", 150.dp)
                        VDivider()
                    }
                    if (OutputFormat.UTM in visibleFormats) {
                        TableHeader("UTM", 190.dp)
                        VDivider()
                    }
                    if (OutputFormat.EPSG3857 in visibleFormats) {
                        TableHeader(stringResource(R.string.calculator_col_epsg3857), 190.dp)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                results.forEachIndexed { idx, point ->
                    val rowBg = if (point.error) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    } else if (idx % 2 == 0) evenRowBg else oddRowBg

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                    ) {
                        TableCell("${point.index}", 38.dp)
                        VDivider()
                        TableCell(text = point.original, width = 130.dp, maxLines = 2)
                        VDivider()
                        if (OutputFormat.DD in visibleFormats) {
                            TableCell(point.dd, 150.dp) { copyToClipboard(context, point.dd); onCopied("DD") }
                            VDivider()
                        }
                        if (OutputFormat.DMS in visibleFormats) {
                            TableCell(point.dms, 190.dp) { copyToClipboard(context, point.dms); onCopied("DMS") }
                            VDivider()
                        }
                        if (OutputFormat.DDM in visibleFormats) {
                            TableCell(point.ddm, 150.dp) { copyToClipboard(context, point.ddm); onCopied("DDM") }
                            VDivider()
                        }
                        if (OutputFormat.UTM in visibleFormats) {
                            TableCell(point.utm, 190.dp) { copyToClipboard(context, point.utm); onCopied("UTM") }
                            VDivider()
                        }
                        if (OutputFormat.EPSG3857 in visibleFormats) {
                            TableCell(point.epsg3857, 190.dp) { copyToClipboard(context, point.epsg3857); onCopied("EPSG:3857") }
                        }
                    }
                    if (idx < results.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = stringResource(R.string.calculator_copy_table),
                    icon = Icons.Default.ContentCopy,
                    filled = false,
                    onClick = {
                        copyToClipboard(context, viewModel.getResultsAsText())
                        onCopied("الجدول")
                    }
                )
                ActionButton(
                    text = stringResource(R.string.calculator_export_csv),
                    icon = Icons.Default.TableChart,
                    filled = false,
                    onClick = {
                        shareText(context, viewModel.getResultsAsCsv(), "text/csv", context.getString(R.string.calculator_share_title))
                    }
                )
                ActionButton(
                    text = stringResource(R.string.calculator_export_text),
                    icon = Icons.Default.Description,
                    filled = false,
                    onClick = {
                        shareText(context, viewModel.getResultsAsText(), "text/plain", context.getString(R.string.calculator_share_title))
                    }
                )
            }
        }
    }
}

@Composable
private fun MapCard(
    cardShape: RoundedCornerShape,
    results: List<ConvertedPoint>,
    context: Context
) {
    val validPoints = results.filter { !it.error && it.lat != null && it.lon != null }
    val mapPoints = validPoints.map { it.lat!! to it.lon!! }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.calculator_map_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "(${validPoints.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(200.dp).clipToBounds()) {
                MiniMap(
                    points = mapPoints,
                    modifier = Modifier.fillMaxSize()
                )
            }
    }
}

// ═══════════════════════════════════════════
// Reusable Composables
// ═══════════════════════════════════════════

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    filled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    if (filled) {
        Button(
            onClick = onClick,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(icon, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = shape) {
            Icon(icon, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TableHeader(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    width: Dp,
    maxLines: Int = 1,
    onClick: (() -> Unit)? = null
) {
    val cellModifier = if (onClick != null) {
        Modifier
            .width(width)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable(enabled = text != "N/A") { onClick() }
    } else {
        Modifier
            .width(width)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    }

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = if (onClick != null && text != "N/A")
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.onSurface,
                textDirection = TextDirection.Ltr
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(IntrinsicSize.Min)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

// ═══════════════════════════════════════════
// ═══════════════════════════════════════════
// MiniMap
// ═══════════════════════════════════════════

@Composable
private fun MiniMap(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { PreferencesManager(context) }

    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = context.cacheDir.resolve("tiles")
        }
    }

    val arrowsOutPath = "M216,48V96a8,8,0,0,1-16,0V67.31l-42.34,42.35a8,8,0,0,1-11.32-11.32L188.69,56H160a8,8,0,0,1,0-16h48A8,8,0,0,1,216,48ZM98.34,146.34,56,188.69V160a8,8,0,0,0-16,0v48a8,8,0,0,0,8,8H96a8,8,0,0,0,0-16H67.31l42.35-42.34a8,8,0,0,0-11.32-11.32ZM208,152a8,8,0,0,0-8,8v28.69l-42.34-42.35a8,8,0,0,0-11.32,11.32L188.69,200H160a8,8,0,0,0,0,16h48a8,8,0,0,0,8-8V160A8,8,0,0,0,208,152ZM67.31,56H96a8,8,0,0,0,0-16H48a8,8,0,0,0-8,8V96a8,8,0,0,0,16,0V67.31l42.34,42.35a8,8,0,0,0,11.32-11.32Z"
    val arrowsInPath = "M144,104V64a8,8,0,0,1,16,0V84.69l42.34-42.35a8,8,0,0,1,11.32,11.32L171.31,96H192a8,8,0,0,1,0,16H152A8,8,0,0,1,144,104Zm-40,40H64a8,8,0,0,0,0,16H84.69L42.34,202.34a8,8,0,0,0,11.32,11.32L96,171.31V192a8,8,0,0,0,16,0V152A8,8,0,0,0,104,144Zm67.31,16H192a8,8,0,0,0,0-16H152a8,8,0,0,0-8,8v40a8,8,0,0,0,16,0V171.31l42.34,42.35a8,8,0,0,0,11.32-11.32ZM104,56a8,8,0,0,0-8,8V84.69L53.66,42.34A8,8,0,0,0,42.34,53.66L84.69,96H64a8,8,0,0,0,0,16h40a8,8,0,0,0,8-8V64A8,8,0,0,0,104,56Z"
    val arrowsOutIcon = remember(arrowsOutPath) {
        ImageVector.Builder(name = "arrows_out", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 256f, viewportHeight = 256f)
            .addPath(pathData = addPathNodes(arrowsOutPath), fill = SolidColor(Color.Black))
            .build()
    }
    val arrowsInIcon = remember(arrowsInPath) {
        ImageVector.Builder(name = "arrows_in", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 256f, viewportHeight = 256f)
            .addPath(pathData = addPathNodes(arrowsInPath), fill = SolidColor(Color.Black))
            .build()
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun zoomToPoints(mv: MapView, pts: List<Pair<Double, Double>>) {
        mv.overlays.clear()
        if (pts.isEmpty()) {
            mv.controller.setZoom(3.0)
            mv.controller.setCenter(GeoPoint(0.0, 0.0))
            return
        }
        var north = -90.0; var south = 90.0
        var east = -180.0; var west = 180.0
        pts.forEachIndexed { idx, (lat, lon) ->
            north = maxOf(north, lat); south = minOf(south, lat)
            east = maxOf(east, lon); west = minOf(west, lon)
            val marker = Marker(mv)
            marker.position = GeoPoint(lat, lon)
            marker.setAnchor(0.5f, 0.5f)
            marker.setInfoWindow(null)
            marker.setIcon(coordMarkerIcon(mv, idx + 1, COORD_COLOR))
            mv.overlays.add(marker)
        }
        val centerLat = (north + south) / 2.0
        val centerLon = (east + west) / 2.0
        mv.controller.setCenter(GeoPoint(centerLat, centerLon))
        if (pts.size == 1) {
            mv.controller.setZoom(14.0)
        } else {
            mv.post {
                try {
                    val bb = org.osmdroid.util.BoundingBox(north, east, south, west)
                    mv.zoomToBoundingBox(bb, true, 50)
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    val savedSource = tileSources.find { it.key == prefs.defaultMapType }?.source ?: TileSourceFactory.MAPNIK
                    try { setTileSource(savedSource) } catch (_: Exception) { try { setTileSource(TileSourceFactory.MAPNIK) } catch (_: Exception) {} }
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    setHorizontalMapRepetitionEnabled(false)
                    setVerticalMapRepetitionEnabled(false)
                    minZoomLevel = 2.0
                    maxZoomLevel = 19.0
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    mapView = this
                    setOnTouchListener { v, event ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        false
                    }
                    post { zoomToPoints(this, points) }
                }
            },
            update = { mv -> mv.post { zoomToPoints(mv, points) } },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CtrlBtn(
                onClick = { mapView?.let { v -> v.controller.zoomTo((v.zoomLevelDouble - 5.0).coerceAtLeast(2.0), 1200L) } },
                icon = arrowsOutIcon,
                desc = "تكبير سريع -"
            )
            CtrlBtn(
                onClick = { mapView?.controller?.zoomOut(300L) },
                icon = Icons.Default.Remove,
                desc = "تصغير"
            )
            CtrlBtn(
                onClick = { mapView?.controller?.zoomIn(300L) },
                icon = Icons.Default.Add,
                desc = "تكبير"
            )
            CtrlBtn(
                onClick = { mapView?.let { v -> v.controller.zoomTo((v.zoomLevelDouble + 5.0).coerceAtMost(19.0), 1200L) } },
                icon = arrowsInIcon,
                desc = "تكبير سريع +"
            )
        }
    }
}

// ═══════════════════════════════════════════
// Utilities
// ═══════════════════════════════════════════

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("coord", text))
}

private fun shareText(context: Context, text: String, mimeType: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
