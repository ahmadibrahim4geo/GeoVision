package com.geovision.mobile.ui.screens.layers

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geovision.mobile.R
import com.geovision.mobile.ui.navigation.BottomNavBar
import com.geovision.mobile.ui.navigation.BottomNavTab
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerManagerScreen(
    onNavigateToTab: (BottomNavTab) -> Unit,
    onLayerClick: (layerId: String, layerName: String) -> Unit,
    viewModel: LayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val layers = viewModel.layers
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.init(context) }
    val unsupportedFormatMsg = stringResource(R.string.unsupported_format)
    val deletedMsg = stringResource(R.string.deleted_layer)
    val undoLabel = stringResource(R.string.undo_delete)
    val filesImportedMsg = stringResource(R.string.files_imported)
    val filesSkippedMsg = stringResource(R.string.files_skipped)
    var undoState by remember { mutableStateOf<Triple<Layer, LayerDetailInfo?, Int>?>(null) }

    var showFabMenu by remember { mutableStateOf(false) }

    // حالات Layer Selection Dialog
    var showLayerSelection by remember { mutableStateOf(false) }
    var pendingGpkgUri by remember { mutableStateOf<Uri?>(null) }
    var pendingGpkgName by remember { mutableStateOf("") }
    var gpkgLayerNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingLayers by remember { mutableStateOf(false) }

    // حالات GDB Layer Selection
    var showGdbSelection by remember { mutableStateOf(false) }
    var pendingGdbUri by remember { mutableStateOf<Uri?>(null) }
    var pendingGdbName by remember { mutableStateOf("") }
    var gdbTableNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val gdbFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
        val docFile = DocumentFile.fromTreeUri(context, uri)
        val folderName = docFile?.name ?: uri.lastPathSegment?.substringAfterLast('%').orEmpty()
        pendingGdbUri = uri
        pendingGdbName = folderName
        isLoadingLayers = true
        scope.launch {
            val tables = com.geovision.mobile.data.GdbParser.listTableNames(context, uri, folderName)
            gdbTableNames = tables.map { it.tableName }
            isLoadingLayers = false
            if (tables.isNotEmpty()) showGdbSelection = true
            else snackbarHostState.showSnackbar("No tables found in $folderName")
        }
    }

    LaunchedEffect(undoState) {
        val s = undoState ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar("\u201C${s.first.name}\u201D $deletedMsg", actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.restoreLayer(s.first, s.second, s.third)
        }
        undoState = null
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        // ── صلاحيات URI دائمة (لإعادة الاستيراد بعد إغلاق التطبيق) ──
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
        }

        // ── المسح الأولي: تجميع مكونات Shapefile حسب الاسم الأساسي ──
        val shpByBase = mutableMapOf<String, Uri>()    // baseName -> .shp URI
        val dbfByBase = mutableMapOf<String, Uri>()    // baseName -> .dbf URI
        val prjByBase = mutableMapOf<String, Uri>()    // baseName -> .prj URI
        for (uri in uris) {
            val name = viewModel.getFileName(context, uri)?.lowercase() ?: ""
            val base = name.removeSuffix(".shp").removeSuffix(".shx").removeSuffix(".shb")
                .removeSuffix(".dbf").removeSuffix(".prj").removeSuffix(".cpg")
            if (base.length in 1 until name.length) {
                when {
                    name.endsWith(".shp") -> shpByBase[base] = uri
                    name.endsWith(".dbf") -> dbfByBase[base] = uri
                    name.endsWith(".prj") -> prjByBase[base] = uri
                }
            }
        }

        var added = 0; var skipped = 0
        val processedBases = mutableSetOf<String>()

        for (uri in uris) {
            val fileType = viewModel.inferFileType(uri, context)
            if (fileType != null) {
                val fileName = viewModel.getFileName(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: ""

                if (fileType == FileType.SHAPEFILE) {
                    val nameLower = fileName.lowercase()

                    // ZIP مباشرة دون تجميع
                    if (nameLower.endsWith(".zip")) {
                        val layerId = viewModel.addLayer(fileName, fileType, uri.toString())
                        viewModel.loadAndCacheLayer(context, layerId, uri, fileName)
                        added++; continue
                    }

                    val base = nameLower.removeSuffix(".shp").removeSuffix(".shx").removeSuffix(".shb")
                        .removeSuffix(".dbf").removeSuffix(".prj").removeSuffix(".cpg")
                    if (base.length >= nameLower.length) { skipped++; continue }

                    // تحديد الشيب فايل الأساسي: إما الملف الحالي (.shp) أو اكتشافه من ملف تابع
                    val shpUri = if (nameLower.endsWith(".shp")) uri
                                 else if (base in shpByBase) null  // .shp موجود ضمن الاختيارات — التابع يُتجاوز
                                 else viewModel.resolveShpFromAuxiliary(uri, context)  // اكتشاف .shp خارجياً

                    if (shpUri == null || base in processedBases) { skipped++; continue }
                    processedBases.add(base)

                    val shpName = viewModel.getFileName(context, shpUri) ?: "${base}.shp"
                    val companionDbf = dbfByBase[base] ?: viewModel.findCompanionUri(shpUri, ".dbf", context)
                    val companionPrj = prjByBase[base] ?: viewModel.findCompanionUri(shpUri, ".prj", context)

                    val layerId = viewModel.addLayer(shpName, fileType, shpUri.toString())
                    viewModel.loadAndCacheLayer(context, layerId, shpUri, shpName, companionDbf, companionPrj)
                    added++
                } else if (fileType == FileType.PHOTO) {
                    viewModel.importSinglePhoto(context, uri, fileName)
                    added++
                } else if (fileType == FileType.GEOPACKAGE) {
                    pendingGpkgUri = uri
                    pendingGpkgName = fileName
                    isLoadingLayers = true
                    scope.launch {
                        val names = com.geovision.mobile.data.GpkgReader.listLayerNames(context, uri)
                        gpkgLayerNames = names
                        isLoadingLayers = false
                        if (names.isNotEmpty()) showLayerSelection = true
                        else snackbarHostState.showSnackbar("No layers found in $fileName")
                    }
                    added++
                } else {
                    val layerId = viewModel.addLayer(fileName, fileType, uri.toString())
                    viewModel.loadAndCacheLayer(context, layerId, uri, fileName)
                    added++
                }
            } else {
                skipped++
            }
        }
        scope.launch {
            if (added > 0) snackbarHostState.showSnackbar("$added $filesImportedMsg")
            if (skipped > 0) snackbarHostState.showSnackbar("$skipped $filesSkippedMsg")
        }
    }

    LaunchedEffect(viewModel.loadError) {
        viewModel.loadError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(viewModel.loadWarning) {
        viewModel.loadWarning?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long) }
    }

    val grouped = layers.groupBy { it.fileType }

    var collapsedSections by remember { mutableStateOf(setOf<String>()) }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var draggedFlatIndex by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableMapOf<Int, Int>() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.layer_manager_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceDim)
            )
        },
        bottomBar = { BottomNavBar(selectedTab = BottomNavTab.LAYERS, onTabSelected = onNavigateToTab) },
        floatingActionButton = {
            if (layers.isNotEmpty()) {
                Box {
                    FloatingActionButton(onClick = { showFabMenu = true },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                        Icon(Icons.Default.Add, null, Modifier.size(24.dp))
                    }
                    DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("ملف GIS") },
                            onClick = {
                                showFabMenu = false
                                filePickerLauncher.launch(arrayOf("application/octet-stream", "application/geo+json", "application/vnd.google-earth.kml+xml", "application/x-qgis", "application/geopackage+sqlite3", "image/jpeg", "image/png", "image/webp", "*/*"))
                            },
                            leadingIcon = { Icon(Icons.Default.InsertDriveFile, "استيراد ملفات GIS") }
                        )
                        DropdownMenuItem(
                            text = { Text("GeoDatabase (.gdb)") },
                            onClick = {
                                showFabMenu = false
                                gdbFolderPickerLauncher.launch(null)
                            },
                            leadingIcon = { Icon(Icons.Default.Folder, "استيراد GeoDatabase") }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceDim
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (layers.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Layers, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.no_layers), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W600)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.add_layer_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 48.dp))
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { filePickerLauncher.launch(arrayOf("application/octet-stream", "application/geo+json", "application/vnd.google-earth.kml+xml", "application/x-qgis", "application/geopackage+sqlite3", "image/jpeg", "image/png", "image/webp", "*/*")) },
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                            Icon(Icons.Default.InsertDriveFile, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("GIS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.W600)
                        }
                        FilledTonalButton(onClick = { gdbFolderPickerLauncher.launch(null) },
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                            Icon(Icons.Default.FolderOpen, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("GDB", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.W600)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 80.dp)
                ) {
                    grouped.forEach { (fileType, groupLayers) ->
                        val sectionKey = fileType.name
                        val isExpanded = sectionKey !in collapsedSections

                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable {
                                collapsedSections = if (isExpanded) collapsedSections + sectionKey
                                else collapsedSections - sectionKey
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        when (fileType) {
                                            FileType.SHAPEFILE -> Icons.Default.Layers
                                            FileType.GEOJSON -> Icons.Default.Code
                                            FileType.KML -> Icons.Default.Place
                                            FileType.GPX -> Icons.Default.Timeline
                                            FileType.PHOTO -> Icons.Default.PhotoCamera
                                            FileType.GEOPACKAGE -> Icons.Default.Storage
                                            FileType.GEODATABASE -> Icons.Default.FolderOpen
                                        }, null, Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(fileType.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(4.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                        Text("${groupLayers.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                                    Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isExpanded) {
                            groupLayers.forEach { layer ->
                                val flatIndex = layers.indexOf(layer)
                                val isDragging = draggedItemId == layer.id
                                val currentDragOffset = if (isDragging) dragOffset else 0f

                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            translationY = currentDragOffset
                                            shadowElevation = if (isDragging) 8f else 0f
                                        }
                                        .onSizeChanged { size ->
                                            if (isDragging) itemHeights[flatIndex] = size.height
                                        }
                                ) {
                                    val detail = viewModel.getCachedDetail(layer.id)
                                    val geoSummary = detail?.features?.groupBy { it.geometryType }?.entries?.joinToString(" · ") { (type, list) ->
                                        val label = when (type) {
                                            "Point" -> "\u25CF"; "MultiPoint" -> "\u25CF\u00D7"; "LineString" -> "\u2571"; "MultiLineString" -> "\u2571\u00D7"
                                            "Polygon" -> "\u25A3"; "MultiPolygon" -> "\u25A3\u00D7"; else -> "?"
                                        }
                                        "$label${list.size}"
                                    }?.ifEmpty { null }

                                    LayerCard(
                                        layer = layer,
                                        geometrySummary = geoSummary,
                                        onOpenDetails = { onLayerClick(layer.id, layer.name) },
                                        onToggleVisibility = { viewModel.toggleVisibility(it) },
                                        onTransparencyChanged = { id, a -> viewModel.updateTransparency(id, a) },
                                        onDelete = { id ->
                                            val layerCopy = viewModel.getLayerById(id)?.let { it.copy() }
                                            val detailCopy = viewModel.getCachedDetail(id)
                                            val idx = viewModel.layers.indexOfFirst { it.id == id }
                                            viewModel.removeLayer(id)
                                            if (layerCopy != null && idx >= 0) {
                                                undoState = Triple(layerCopy, detailCopy, idx)
                                            }
                                        },
                                        onReloadLayer = { id -> viewModel.reloadLayer(context, id) },
                                        onZoomToLayer = { id -> viewModel.requestZoomToLayer(id); onNavigateToTab(BottomNavTab.MAP) },
                                        onColorChange = { id, c -> viewModel.updateLayerColor(id, c) },
                                        onPointSizeChanged = { id, s -> viewModel.updatePointSize(id, s) },
                                        onLineWidthChanged = { id, w -> viewModel.updateLineWidth(id, w) },
                                        onDragStart = {
                                            draggedItemId = layer.id
                                            draggedFlatIndex = flatIndex
                                            dragOffset = 0f
                                        },
                                        onDrag = { delta -> dragOffset += delta },
                                        onDragEnd = {
                                            val itemHeight = itemHeights[draggedFlatIndex] ?: 140
                                            val indexDelta = (dragOffset / itemHeight.coerceAtLeast(1)).roundToInt()
                                            val targetIndex = (draggedFlatIndex + indexDelta).coerceIn(0, layers.size - 1)
                                            if (targetIndex != draggedFlatIndex) {
                                                viewModel.moveLayer(draggedFlatIndex, targetIndex)
                                            }
                                            draggedItemId = null
                                            dragOffset = 0f
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f))
                }
            }
        }

        // ── GeoPackage Layer Selection Dialog ──
        if (showLayerSelection && gpkgLayerNames.isNotEmpty()) {
            val uri = pendingGpkgUri
            val name = pendingGpkgName
            LayerSelectionDialog(
                title = "GeoPackage: $name",
                layers = gpkgLayerNames,
                onDismiss = {
                    showLayerSelection = false
                    pendingGpkgUri = null
                    gpkgLayerNames = emptyList()
                },
                onConfirm = { selectedNames ->
                    showLayerSelection = false
                    if (uri != null) {
                        // إنشاء طبقة لكل layer مختارة
                        selectedNames.forEach { layerTableName ->
                            val layerFileName = "${name} / $layerTableName"
                            val layerId = viewModel.addLayer(layerFileName, FileType.GEOPACKAGE, uri.toString())
                            viewModel.loadAndCacheLayer(context, layerId, uri, layerFileName)
                        }
                    }
                    pendingGpkgUri = null
                    gpkgLayerNames = emptyList()
                }
            )
        }

        // ── GDB Table Selection Dialog ──
        if (showGdbSelection && gdbTableNames.isNotEmpty()) {
            val uri = pendingGdbUri
            val name = pendingGdbName
            LayerSelectionDialog(
                title = "GeoDatabase: $name",
                layers = gdbTableNames,
                onDismiss = {
                    showGdbSelection = false
                    pendingGdbUri = null
                    gdbTableNames = emptyList()
                },
                onConfirm = { selectedNames ->
                    showGdbSelection = false
                    if (uri != null && selectedNames.isNotEmpty()) {
                        viewModel.parseAndCacheGdbSelected(context, uri, name, selectedNames)
                    }
                    pendingGdbUri = null
                    gdbTableNames = emptyList()
                }
            )
        }

        if (isLoadingLayers) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Reading layers...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
