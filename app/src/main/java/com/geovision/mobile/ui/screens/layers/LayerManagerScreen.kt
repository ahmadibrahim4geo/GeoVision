package com.geovision.mobile.ui.screens.layers

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
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
import com.geovision.mobile.data.GdbParser
import com.geovision.mobile.ui.navigation.BottomNavBar
import com.geovision.mobile.ui.navigation.BottomNavTab
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val GIS_DOCUMENT_MIME_TYPES = arrayOf(
    "application/octet-stream",
    "application/geo+json",
    "application/vnd.google-earth.kml+xml",
    "application/vnd.google-earth.kmz",
    "application/x-qgis",
    "application/geopackage+sqlite3",
    "application/zip",
    "application/x-zip-compressed",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "text/csv",
    "text/comma-separated-values",
    "text/tab-separated-values",
    "text/plain",
    "image/jpeg",
    "image/png",
    "image/webp",
    "*/*"
)

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
    var gdbTableInfos by remember { mutableStateOf<List<GdbParser.GdbTableInfo>>(emptyList()) }

    // حالات مركز الاستيراد الموحد
    var showImportCenter by remember { mutableStateOf(false) }

    // حالات Coordinate Mapping Dialog للإكسيل و CSV
    var showCoordinateMappingDialog by remember { mutableStateOf(false) }
    var pendingSpreadsheetUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSpreadsheetName by remember { mutableStateOf("") }
    var pendingSpreadsheetIsExcel by remember { mutableStateOf(false) }
    var spreadsheetInspection by remember { mutableStateOf<com.geovision.mobile.data.ExcelParser.TableInspection?>(null) }

    // حالات Layer Properties & Styling Dialog
    var showLayerPropertiesDialog by remember { mutableStateOf(false) }
    var selectedLayerForProperties by remember { mutableStateOf<Layer?>(null) }

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
            val tables = GdbParser.listTableNames(context, uri, folderName)
            gdbTableInfos = tables
            gdbTableNames = tables.map { it.fileName }
            isLoadingLayers = false
            viewModel.scanImportReport(context, uri, folderName)
            if (tables.isNotEmpty()) showGdbSelection = true
            else snackbarHostState.showSnackbar("لم يتم العثور على جداول في $folderName")
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
            val fileName = viewModel.getFileName(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: ""
            val nameLower = fileName.lowercase()
            val fileType = viewModel.inferFileType(uri, context)
            if (fileType != null) {
                if (fileType == FileType.GEODATABASE) {
                    pendingGdbUri = uri
                    pendingGdbName = fileName
                    isLoadingLayers = true
                    scope.launch {
                        val tables = GdbParser.listTableNames(context, uri, fileName)
                        gdbTableInfos = tables
                        gdbTableNames = tables.map { it.fileName }
                        isLoadingLayers = false
                        viewModel.scanImportReport(context, uri, fileName)
                        if (tables.isNotEmpty()) showGdbSelection = true
                        else snackbarHostState.showSnackbar("لم يتم العثور على جداول في $fileName")
                    }
                    added++; continue
                } else if (fileType == FileType.SHAPEFILE) {
                    // ZIP مباشرة دون تجميع
                    if (nameLower.endsWith(".zip")) {
                        viewModel.scanImportReport(context, uri, fileName)
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
                    viewModel.scanImportReport(context, shpUri, shpName)
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
                        viewModel.scanImportReport(context, uri, fileName)
                        if (names.isNotEmpty()) showLayerSelection = true
                        else snackbarHostState.showSnackbar("لم يتم العثور على طبقات في $fileName")
                    }
                    added++
                } else if (fileType == FileType.EXCEL || fileType == FileType.CSV) {
                    val isExcel = fileType == FileType.EXCEL || nameLower.endsWith(".xlsx") || nameLower.endsWith(".xls")
                    pendingSpreadsheetUri = uri
                    pendingSpreadsheetName = fileName
                    pendingSpreadsheetIsExcel = isExcel
                    isLoadingLayers = true
                    scope.launch {
                        val inspection = viewModel.inspectSpreadsheet(context, uri, isExcel)
                        isLoadingLayers = false
                        if (inspection.headers.isNotEmpty()) {
                            spreadsheetInspection = inspection
                            showCoordinateMappingDialog = true
                        } else {
                            snackbarHostState.showSnackbar("تعذّر قراءة أعمدة ملف $fileName")
                        }
                    }
                    added++
                } else {
                    viewModel.scanImportReport(context, uri, fileName)
                    val layerId = viewModel.addLayer(fileName, fileType, uri.toString())
                    viewModel.loadAndCacheLayer(context, layerId, uri, fileName)
                    added++
                }
            } else {
                if (nameLower.endsWith(".mdb")) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "ملف Personal Geodatabase (.mdb) غير مدعوم على أندرويد لكونه يعتمد على Microsoft Access. يُنصح بتحويله إلى GeoPackage (.gpkg) أو FileGDB (.gdb) عبر ArcGIS أو QGIS.",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedGeomFilter by remember { mutableStateOf("ALL") }
    var selectedVisFilter by remember { mutableStateOf("ALL") }

    val filteredLayers = layers.filter { layer ->
        val matchesSearch = searchQuery.isBlank() || layer.name.contains(searchQuery, ignoreCase = true)
        val matchesGeom = when (selectedGeomFilter) {
            "Point" -> layer.geomType == "Point"
            "Line" -> layer.geomType == "Line"
            "Polygon" -> layer.geomType == "Polygon"
            else -> true
        }
        val matchesVis = when (selectedVisFilter) {
            "VISIBLE" -> layer.isVisible
            "HIDDEN" -> !layer.isVisible
            else -> true
        }
        matchesSearch && matchesGeom && matchesVis
    }

    val grouped = filteredLayers.groupBy { it.fileType }

    var collapsedSections by remember { mutableStateOf(setOf<String>()) }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var draggedFlatIndex by remember { mutableIntStateOf(0) }
    val itemHeights = remember { mutableMapOf<Int, Int>() }

    var renamingLayerId by remember { mutableStateOf<String?>(null) }
    var renamingLayerName by remember { mutableStateOf("") }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.layer_manager_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) },
                actions = {
                    FilledTonalButton(
                        onClick = { showImportCenter = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("مركز الاستيراد", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    if (layers.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, "مسح الكل", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceDim)
            )
        },
        bottomBar = { BottomNavBar(selectedTab = BottomNavTab.LAYERS, onTabSelected = onNavigateToTab) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportCenter = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, "إضافة واستيراد طبقات", Modifier.size(26.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceDim
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (layers.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Layers, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.no_layers),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "ابدأ باستيراد ملفاتك وقواعد بياناتك الجغرافية لعرضها وتحليلها على الخريطة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showImportCenter = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "مركز استيراد البيانات الجغرافية",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Esri GDB · GeoPackage · Shapefile · Excel · KML · GeoJSON",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/csv",
                                    "text/plain",
                                    "*/*"
                                ))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TableChart, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Excel / CSV", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = {
                                filePickerLauncher.launch(GIS_DOCUMENT_MIME_TYPES)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ملف GIS", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 80.dp)
                ) {
                    // ── شريط التحكم الجماعي بالطبقات (Batch Layer Controls) ──
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val visibleCount = layers.count { it.isVisible }
                            Text(
                                text = "${layers.size} طبقة ($visibleCount معروضة)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.W700,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = { viewModel.showAllLayers() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Visibility, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("إظهار الكل", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.hideAllLayers() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.VisibilityOff, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("إخفاء الكل", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W600)
                                }
                                IconButton(
                                    onClick = { viewModel.invertLayersVisibility() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.SwapVert, "عكس الرؤية", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // ── شريط البحث والتصفية الذكي ──
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("بحث عن طبقة باسمها...", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, "مسح البحث")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedGeomFilter == "ALL" && selectedVisFilter == "ALL",
                                onClick = { selectedGeomFilter = "ALL"; selectedVisFilter = "ALL" },
                                label = { Text("كل الطبقات (${layers.size})") }
                            )
                            FilterChip(
                                selected = selectedGeomFilter == "Point",
                                onClick = { selectedGeomFilter = if (selectedGeomFilter == "Point") "ALL" else "Point" },
                                label = { Text("نقاط 📍") }
                            )
                            FilterChip(
                                selected = selectedGeomFilter == "Line",
                                onClick = { selectedGeomFilter = if (selectedGeomFilter == "Line") "ALL" else "Line" },
                                label = { Text("خطوط 〰️") }
                            )
                            FilterChip(
                                selected = selectedGeomFilter == "Polygon",
                                onClick = { selectedGeomFilter = if (selectedGeomFilter == "Polygon") "ALL" else "Polygon" },
                                label = { Text("مضلعات 🔷") }
                            )
                            FilterChip(
                                selected = selectedVisFilter == "VISIBLE",
                                onClick = { selectedVisFilter = if (selectedVisFilter == "VISIBLE") "ALL" else "VISIBLE" },
                                label = { Text("ظاهرة 👁️") }
                            )
                            FilterChip(
                                selected = selectedVisFilter == "HIDDEN",
                                onClick = { selectedVisFilter = if (selectedVisFilter == "HIDDEN") "ALL" else "HIDDEN" },
                                label = { Text("مخفية 🙈") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (filteredLayers.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد طبقات تطابق شروط البحث والفلترة الحاليّة", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
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
                                            FileType.EXCEL -> Icons.Default.TableChart
                                            FileType.CSV -> Icons.Default.TableChart
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
                                        onOpenProperties = {
                                            selectedLayerForProperties = layer
                                            showLayerPropertiesDialog = true
                                        },
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
                                        onRename = { id ->
                                            renamingLayerId = id
                                            renamingLayerName = layers.find { it.id == id }?.name ?: ""
                                        },
                                        onColorChange = { id, c -> viewModel.updateLayerColor(id, c) },
                                        onPointSizeChanged = { id, s -> viewModel.updatePointSize(id, s) },
                                        onLineWidthChanged = { id, w -> viewModel.updateLineWidth(id, w) },
                                        onMoveUp = { id -> viewModel.moveLayerUp(id) },
                                        onMoveDown = { id -> viewModel.moveLayerDown(id) },
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
                        viewModel.parseAndCacheGpkgSelected(context, uri, name, selectedNames)
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
            val displayNames = gdbTableInfos.associate { it.fileName to it.tableName }
            val subtitles = gdbTableInfos.associate { info ->
                val parts = listOfNotNull(
                    info.geometryType,
                    info.estimatedCount.takeIf { it >= 0 }?.let { "$it عنصر" },
                    info.catalogPath
                )
                info.fileName to parts.joinToString(" - ")
            }
            LayerSelectionDialog(
                title = "GeoDatabase: $name",
                layers = gdbTableNames,
                displayNames = displayNames,
                subtitles = subtitles,
                onDismiss = {
                    showGdbSelection = false
                    pendingGdbUri = null
                    gdbTableNames = emptyList()
                    gdbTableInfos = emptyList()
                },
                onConfirm = { selectedNames ->
                    showGdbSelection = false
                    if (uri != null && selectedNames.isNotEmpty()) {
                        viewModel.parseAndCacheGdbSelected(context, uri, name, selectedNames)
                    }
                    pendingGdbUri = null
                    gdbTableNames = emptyList()
                    gdbTableInfos = emptyList()
                }
            )
        }

        viewModel.latestImportReport?.let { report ->
            ImportReportDialog(
                report = report,
                onDismiss = { viewModel.clearLatestImportReport() }
            )
        }

        if (isLoadingLayers) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Text("جار قراءة الطبقات...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (renamingLayerId != null) {
            AlertDialog(
                onDismissRequest = { renamingLayerId = null },
                title = { Text("إعادة تسمية الطبقة", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renamingLayerName,
                        onValueChange = { renamingLayerName = it },
                        singleLine = true,
                        label = { Text("اسم الطبقة الجديد") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val id = renamingLayerId
                        if (id != null && renamingLayerName.isNotBlank()) {
                            viewModel.renameLayer(id, renamingLayerName)
                        }
                        renamingLayerId = null
                    }) { Text("حفظ") }
                },
                dismissButton = {
                    TextButton(onClick = { renamingLayerId = null }) { Text("إلغاء") }
                }
            )
        }

        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text("مسح كافة الطبقات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = { Text("هل أنت تأكد من رغبتك في حذف جميع الطبقات المعروضة حالياً وإعادة ضبط الخريطة؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllLayers()
                            showClearAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("نعم، مسح الكل") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) { Text("إلغاء") }
                }
            )
        }

        if (showCoordinateMappingDialog && pendingSpreadsheetUri != null && spreadsheetInspection != null) {
            CoordinateMappingDialog(
                fileName = pendingSpreadsheetName,
                inspection = spreadsheetInspection!!,
                onDismiss = {
                    showCoordinateMappingDialog = false
                    pendingSpreadsheetUri = null
                },
                onConfirm = { mapping ->
                    showCoordinateMappingDialog = false
                    val targetUri = pendingSpreadsheetUri ?: return@CoordinateMappingDialog
                    val targetName = pendingSpreadsheetName
                    val isExcel = pendingSpreadsheetIsExcel
                    if (isExcel) {
                        viewModel.parseAndCacheExcel(context, targetUri, targetName, mapping)
                    } else {
                        viewModel.parseAndCacheCsv(context, targetUri, targetName, mapping)
                    }
                    pendingSpreadsheetUri = null
                }
            )
        }

        if (showImportCenter) {
            ImportCenterDialog(
                onDismiss = { showImportCenter = false },
                onSelectOption = { option ->
                    when (option) {
                        ImportSourceOption.EXCEL_CSV -> {
                            filePickerLauncher.launch(arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/comma-separated-values",
                                "text/tab-separated-values",
                                "text/plain",
                                "*/*"
                            ))
                        }
                        ImportSourceOption.GEOPACKAGE -> {
                            filePickerLauncher.launch(arrayOf(
                                "application/geopackage+sqlite3",
                                "application/octet-stream",
                                "*/*"
                            ))
                        }
                        ImportSourceOption.ESRI_GDB -> {
                            gdbFolderPickerLauncher.launch(null)
                        }
                        ImportSourceOption.SHAPEFILE -> {
                            filePickerLauncher.launch(arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                                "*/*"
                            ))
                        }
                        ImportSourceOption.KML_KMZ -> {
                            filePickerLauncher.launch(arrayOf(
                                "application/vnd.google-earth.kml+xml",
                                "application/vnd.google-earth.kmz",
                                "application/xml",
                                "*/*"
                            ))
                        }
                        ImportSourceOption.GEOJSON -> {
                            filePickerLauncher.launch(arrayOf(
                                "application/geo+json",
                                "application/json",
                                "*/*"
                            ))
                        }
                        ImportSourceOption.GEO_PHOTOS -> {
                            filePickerLauncher.launch(arrayOf(
                                "image/jpeg",
                                "image/png",
                                "image/*"
                            ))
                        }
                    }
                }
            )
        }

        if (showLayerPropertiesDialog && selectedLayerForProperties != null) {
            val targetLayer = selectedLayerForProperties!!
            val cachedDetail = viewModel.getCachedDetail(targetLayer.id)
            val fields = cachedDetail?.features?.firstOrNull()?.properties?.keys?.toList() ?: emptyList()
            LayerPropertiesDialog(
                layer = targetLayer,
                availableFields = fields,
                onDismiss = {
                    showLayerPropertiesDialog = false
                    selectedLayerForProperties = null
                },
                onSaveStyle = { updatedStyle ->
                    viewModel.updateLayerStyle(targetLayer.id, updatedStyle)
                }
            )
        }
    }
}
