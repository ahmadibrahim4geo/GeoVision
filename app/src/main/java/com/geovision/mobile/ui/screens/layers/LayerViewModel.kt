package com.geovision.mobile.ui.screens.layers

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.core.UserFriendlyErrors
import com.geovision.mobile.data.ExifGpsParser
import com.geovision.mobile.data.GeoJsonParser
import com.geovision.mobile.data.GeoPhoto
import com.geovision.mobile.data.LayerRepository
import com.geovision.mobile.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LayerLoadState(
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val loadWarning: String? = null,
    val lastParseTimeMs: Long = 0L,
    val layerDetailsVersion: Long = 0L
)

class LayerViewModel : ViewModel() {
    companion object {
        private val rng = java.util.Random()
        fun randomLayerColor(): Color = Color.hsl(rng.nextFloat() * 360f, 0.65f, 0.55f)
    }

    private val _layers = mutableStateListOf<Layer>()
    val layers: List<Layer> get() = _layers

    private var prefs: PreferencesManager? = null
    private var _nextLayerId = 1

    private val _layerDetails = mutableStateMapOf<String, LayerDetailInfo>()

    private val _loadState = MutableStateFlow(LayerLoadState())
    val loadState: StateFlow<LayerLoadState> = _loadState.asStateFlow()

    var isLoading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var loadWarning by mutableStateOf<String?>(null)
        private set
    var lastParseTimeMs by mutableStateOf(0L)
        private set

    private val _layerDetailsVersion = MutableStateFlow(0L)
    val layerDetailsVersionFlow: StateFlow<Long> = _layerDetailsVersion.asStateFlow()

    var requestedZoomLayerId by mutableStateOf<String?>(null)
    var requestedZoomFeatureCoords by mutableStateOf<Pair<Double, Double>?>(null)

    private val _photoData = mutableStateMapOf<String, List<GeoPhoto>>()
    val photoData: Map<String, List<GeoPhoto>> get() = _photoData

    private var _lastDetail by mutableStateOf<LayerDetailInfo?>(null)
    val layerDetail: LayerDetailInfo? get() = _lastDetail

    private val _zoomEvents = MutableSharedFlow<ZoomEvent>(extraBufferCapacity = 1)
    val zoomEvents = _zoomEvents.asSharedFlow()

    private fun updateLoadState(isLoading: Boolean = this.isLoading, loadError: String? = this.loadError, loadWarning: String? = this.loadWarning, lastParseTimeMs: Long = this.lastParseTimeMs) {
        _loadState.value = LayerLoadState(isLoading, loadError, loadWarning, lastParseTimeMs, _layerDetailsVersion.value)
    }

    fun init(context: Context) {
        if (prefs != null) return
        prefs = PreferencesManager(context)
        restoreLayers(context)
    }

    private fun restoreVisibility() {
        _layers.forEachIndexed { index, layer ->
            val saved = prefs?.isLayerVisible(layer.id)
            if (saved != null && saved != layer.isVisible) {
                _layers[index] = _layers[index].copy(isVisible = saved)
            }
        }
    }

    fun toggleVisibility(id: String) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1) {
            _layers[index] = _layers[index].copy(isVisible = !_layers[index].isVisible)
            prefs?.setLayerVisible(id, _layers[index].isVisible)
            persistLayers()
        }
    }

    fun updateTransparency(id: String, alpha: Float) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1) { _layers[index] = _layers[index].copy(transparency = alpha); prefs?.setLayerTransparency(id, alpha); persistLayers() }
    }

    fun removeLayer(id: String) { _layers.removeAll { it.id == id }; _layerDetails.remove(id); _photoData.remove(id); persistLayers() }

    fun restoreLayer(layer: Layer, detail: LayerDetailInfo?, index: Int) {
        val idx = index.coerceIn(0, _layers.size)
        if (idx < _layers.size) _layers.add(idx, layer) else _layers.add(layer)
        if (detail != null) _layerDetails[layer.id] = detail
        persistLayers()
    }

    fun updateLayerColor(id: String, color: Color) { val idx = _layers.indexOfFirst { it.id == id }; if (idx != -1) _layers[idx] = _layers[idx].copy(color = color); persistLayers() }
    fun updatePointSize(id: String, size: Float) { val idx = _layers.indexOfFirst { it.id == id }; if (idx != -1) _layers[idx] = _layers[idx].copy(pointSize = size); persistLayers() }
    fun updateLineWidth(id: String, width: Float) { val idx = _layers.indexOfFirst { it.id == id }; if (idx != -1) _layers[idx] = _layers[idx].copy(lineWidth = width); persistLayers() }

    fun moveLayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val item = _layers.removeAt(fromIndex)
        _layers.add(toIndex.coerceIn(0, _layers.size), item); persistLayers()
    }

    fun persistLayers() {
        val arr = org.json.JSONArray()
        _layers.forEach { layer ->
            val obj = org.json.JSONObject()
            obj.put("id", layer.id); obj.put("name", layer.name); obj.put("fileType", layer.fileType.name)
            obj.put("geomType", layer.geomType ?: ""); obj.put("isVisible", layer.isVisible)
            obj.put("color", layer.color.value.toLong()); obj.put("transparency", layer.transparency.toDouble())
            obj.put("pointSize", layer.pointSize.toDouble()); obj.put("lineWidth", layer.lineWidth.toDouble())
            obj.put("featureCount", layer.featureCount); obj.put("order", layer.order); obj.put("filePath", layer.filePath ?: "")
            arr.put(obj)
        }
        prefs?.layersJson = arr.toString()
    }

    private fun restoreLayers(context: Context) {
        val json = prefs?.layersJson ?: return
        if (json == "[]" || json.isBlank()) return
        try {
            val arr = org.json.JSONArray(json)
            val restored = mutableListOf<Layer>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                restored.add(Layer(
                    id = obj.getString("id"), name = obj.getString("name"),
                    fileType = FileType.valueOf(obj.getString("fileType")),
                    geomType = obj.optString("geomType", "").ifEmpty { null },
                    isVisible = obj.optBoolean("isVisible", true),
                    color = obj.optLong("color", -1L).let { if (it == -1L) randomLayerColor() else Color(it) },
                    transparency = obj.optDouble("transparency", 1.0).toFloat(),
                    pointSize = obj.optDouble("pointSize", 8.0).toFloat(),
                    lineWidth = obj.optDouble("lineWidth", 4.0).toFloat(),
                    featureCount = obj.optInt("featureCount", 0), progressPercent = 1.0f,
                    filePath = obj.optString("filePath", "").ifEmpty { null },
                    order = obj.optInt("order", i)
                ))
            }
            if (restored.isEmpty()) return
            _layers.addAll(restored)
            _nextLayerId = restored.maxOfOrNull { it.id.toIntOrNull() ?: 0 }?.plus(1) ?: _nextLayerId
            for (layer in restored) {
                val fp = layer.filePath ?: continue
                try {
                    val uri = if (fp.startsWith("/")) android.net.Uri.fromFile(java.io.File(fp)) else android.net.Uri.parse(fp)
                    if (layer.fileType == FileType.PHOTO) {
                        viewModelScope.launch {
                            val geoPhoto = withContext(Dispatchers.IO) { ExifGpsParser.parse(context, uri, layer.name) }
                            if (geoPhoto != null) { _photoData[layer.id] = listOf(geoPhoto); _layerDetailsVersion.value = System.nanoTime(); updateLoadState() }
                        }
                    } else {
                        loadAndCacheLayer(context, layer.id, uri, layer.name, removeOnError = false)
                    }
                } catch (_: Exception) {}
            }
            restoreVisibility()
        } catch (e: Exception) { AppLogger.w(AppLogger.Tags.LAYER, "فشل استعادة الطبقات المحفوظة: ${e.message}") }
    }

    fun addGeoJsonLayer(context: Context, fileName: String, geoJsonContent: String): String {
        val id = "meas_${System.currentTimeMillis()}"
        val dir = java.io.File(context.filesDir, "layers").also { it.mkdirs() }
        val file = java.io.File(dir, "$id.geojson"); file.writeText(geoJsonContent)
        val color = randomLayerColor()
        _layers.add(Layer(id = id, name = fileName, fileType = FileType.GEOJSON, featureCount = 0, progressPercent = 0f, isVisible = true, color = color, filePath = file.absolutePath))
        persistLayers()
        loadAndCacheLayer(context, id, android.net.Uri.fromFile(file), fileName)
        return id
    }

    fun addLayer(name: String, fileType: FileType, filePath: String? = null): String {
        val newId = (_nextLayerId++).toString()
        _layers.add(Layer(id = newId, name = name, fileType = fileType, featureCount = 0, progressPercent = 0f, isVisible = true, color = randomLayerColor(), filePath = filePath))
        persistLayers()
        return newId
    }

    fun inferFileType(uri: Uri, context: Context? = null): FileType? = LayerRepository.inferFileType(uri, context)
    fun getFileName(context: Context, uri: Uri): String? = LayerRepository.getFileName(context, uri)
    fun getLayerById(id: String): Layer? = _layers.find { it.id == id }
    fun getCachedDetail(layerId: String): LayerDetailInfo? = _layerDetails[layerId]
    fun getPhotos(layerId: String): List<GeoPhoto> = _photoData[layerId] ?: emptyList()
    fun findCompanionUri(shpUri: Uri, ext: String, context: Context): Uri? = LayerRepository.findCompanionUri(shpUri, ext, context)
    fun resolveShpFromAuxiliary(auxUri: Uri, context: Context): Uri? = LayerRepository.resolveShpFromAuxiliary(auxUri, context)

    fun importSinglePhoto(context: Context, uri: Uri, fileName: String) {
        val layerId = "photo_${System.currentTimeMillis()}_${_layers.size}"
        addLayer(fileName, FileType.PHOTO, uri.toString()).also { _ ->
            viewModelScope.launch {
                isLoading = true; updateLoadState(isLoading = true)
                val geoPhoto = withContext(Dispatchers.IO) { ExifGpsParser.parse(context, uri, fileName) }
                if (geoPhoto != null) {
                    _photoData[layerId] = listOf(geoPhoto)
                    val idx = _layers.indexOfFirst { it.id == layerId }
                    if (idx != -1) _layers[idx] = _layers[idx].copy(featureCount = 1, progressPercent = 1.0f)
                    requestedZoomLayerId = layerId
                } else { _layers.removeAll { it.id == layerId }; _photoData.remove(layerId) }
                persistLayers(); isLoading = false; updateLoadState(isLoading = false)
                AppLogger.d(AppLogger.Tags.LAYER, "Imported single photo: $fileName -> ${if (geoPhoto != null) "OK" else "no GPS data"}")
            }
        }
    }

    fun importPhotos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val layerId = "photos_${System.currentTimeMillis()}"
        val layerName = "Geo Photos (${uris.size})"
        addLayer(layerName, FileType.PHOTO)
        viewModelScope.launch {
            isLoading = true; updateLoadState(isLoading = true)
            val results = mutableListOf<GeoPhoto>()
            var processed = 0
            for (uri in uris) {
                yield()
                val fn = getFileName(context, uri) ?: uri.lastPathSegment ?: "photo_$processed"
                val geoPhoto = withContext(Dispatchers.IO) { ExifGpsParser.parse(context, uri, fn) }
                if (geoPhoto != null) results.add(geoPhoto)
                processed++
                val idx = _layers.indexOfFirst { it.id == layerId }
                if (idx != -1) _layers[idx] = _layers[idx].copy(featureCount = results.size, progressPercent = processed.toFloat() / uris.size)
                if (processed % 5 == 0) kotlinx.coroutines.yield()
            }
            _photoData[layerId] = results
            val idx = _layers.indexOfFirst { it.id == layerId }
            if (results.isEmpty()) { _layers.removeAll { it.id == layerId }; _photoData.remove(layerId) }
            else if (idx != -1) _layers[idx] = _layers[idx].copy(featureCount = results.size, progressPercent = 1.0f).also { requestedZoomLayerId = layerId }
            persistLayers(); isLoading = false; updateLoadState(isLoading = false)
            AppLogger.d(AppLogger.Tags.LAYER, "Imported ${results.size}/${uris.size} geotagged photos")
        }
    }

    fun loadAndCacheLayer(context: Context, layerId: String, uri: Uri, fileName: String, companionDbf: Uri? = null, companionPrj: Uri? = null, removeOnError: Boolean = true) {
        AppLogger.d(AppLogger.Tags.LAYER, "── بدء تحليل الملف: $fileName (URI: $uri) ──")
        viewModelScope.launch {
            isLoading = true; loadError = null; loadWarning = null; lastParseTimeMs = 0
            updateLoadState(isLoading = true, loadError = null, loadWarning = null)
            try {
                val result = LayerRepository.loadFromUri(context, uri, fileName, companionDbf, companionPrj)
                lastParseTimeMs = result.parseTimeMs
                if (result.error != null) {
                    val errorMsg = UserFriendlyErrors.getErrorMessage(result.errorType ?: GeoJsonParser.ParseErrorType.PARSER_ERROR, fileName)
                    AppLogger.w(AppLogger.Tags.LAYER, "فشل التحليل: $errorMsg")
                    loadError = errorMsg
                    if (removeOnError) { _layers.removeAll { it.id == layerId }; _layerDetails.remove(layerId); persistLayers() }
                    else { val idx = _layers.indexOfFirst { it.id == layerId }; if (idx != -1) _layers[idx] = _layers[idx].copy(progressPercent = 1.0f) }
                    updateLoadState(loadError = errorMsg)
                    return@launch
                }
                if (result.warning != null) { AppLogger.w(AppLogger.Tags.LAYER, "تحذير: ${result.warning}"); loadWarning = result.warning; updateLoadState(loadWarning = result.warning) }
                val info = LayerDetailInfo(fileName = result.fileName.ifEmpty { fileName }, filePath = uri.toString(), crs = result.crs.orEmpty(), extent = result.extent.orEmpty(), features = result.features)
                _layerDetails[layerId] = info; _layerDetailsVersion.value = System.nanoTime()
                val idx = _layers.indexOfFirst { it.id == layerId }
                if (idx != -1) _layers[idx] = _layers[idx].copy(featureCount = result.features.size, progressPercent = 1.0f, geomType = deriveGeomType(result.features)).also { persistLayers() }
                requestedZoomLayerId = layerId
                updateLoadState()
                AppLogger.d(AppLogger.Tags.LAYER, "تم حفظ ${result.features.size} معلم للطبقة $fileName")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                loadError = "انتهت مهلة التحليل (أكثر من 60 ثانية)."; AppLogger.e(AppLogger.Tags.LAYER, loadError!!); updateLoadState(loadError = loadError)
            } catch (e: OutOfMemoryError) {
                loadError = "الملف كبير جداً: تجاوز سعة الذاكرة."; AppLogger.e(AppLogger.Tags.LAYER, loadError!!); updateLoadState(loadError = loadError)
            } catch (e: Exception) {
                loadError = "حدث خطأ: ${e.message}"; AppLogger.e(AppLogger.Tags.LAYER, loadError!!, e); updateLoadState(loadError = loadError)
            } finally {
                isLoading = false; updateLoadState(isLoading = false)
                AppLogger.d(AppLogger.Tags.LAYER, "── انتهاء تحليل $fileName ──")
            }
        }
    }

    fun parseAndCacheGdbSelected(context: Context, uri: Uri, fileName: String, selectedTableNames: List<String>) {
        AppLogger.d(AppLogger.Tags.LAYER, "── بدء تحليل ${selectedTableNames.size} جدول من GeoDatabase: $fileName ──")
        viewModelScope.launch {
            isLoading = true; loadError = null; loadWarning = null; lastParseTimeMs = 0
            updateLoadState(isLoading = true, loadError = null, loadWarning = null)
            try {
                val results = LayerRepository.loadGdbTables(context, uri, fileName, selectedTableNames)
                if (results.isEmpty()) { loadError = "لم يتم العثور على معالم في الجداول المحددة"; updateLoadState(loadError = loadError); return@launch }
                var addedCount = 0; var totalElapsed = 0L
                for (result in results) {
                    if (result.features.isEmpty()) continue
                    totalElapsed += result.parseTimeMs
                    val layerFileName = result.fileName
                    val layerId = "gdb_${System.nanoTime()}_${addedCount}"
                    _layers.add(Layer(id = layerId, name = layerFileName, fileType = FileType.GEODATABASE, featureCount = result.features.size, progressPercent = 1.0f, isVisible = true, color = randomLayerColor(), filePath = uri.toString()))
                    _layerDetails[layerId] = LayerDetailInfo(fileName = layerFileName, filePath = uri.toString(), crs = result.crs.orEmpty(), extent = result.extent.orEmpty(), features = result.features)
                    addedCount++
                }
                lastParseTimeMs = totalElapsed / addedCount.coerceAtLeast(1); _layerDetailsVersion.value = System.nanoTime(); persistLayers()
                updateLoadState(lastParseTimeMs = lastParseTimeMs)
                AppLogger.d(AppLogger.Tags.LAYER, "GDB: تمت إضافة $addedCount طبقة (${totalElapsed}ms)")
                if (addedCount > 0) requestedZoomLayerId = _layers.lastOrNull()?.id ?: ""
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                loadError = "انتهت مهلة تحليل GeoDatabase (أكثر من 60 ثانية)."; AppLogger.e(AppLogger.Tags.LAYER, "GDB timeout"); updateLoadState(loadError = loadError)
            } catch (e: Exception) {
                loadError = "فشل تحليل GeoDatabase: ${e.message}"; AppLogger.e(AppLogger.Tags.LAYER, "GDB error: ${e.message}", e); updateLoadState(loadError = loadError)
            } finally { isLoading = false; updateLoadState(isLoading = false) }
        }
    }

    fun loadDetailForLayer(layerId: String) { _lastDetail = _layerDetails[layerId] }

    fun reloadLayer(context: Context, layerId: String) {
        val layer = _layers.find { it.id == layerId } ?: return
        val fp = layer.filePath ?: return
        val uri = if (fp.startsWith("/")) android.net.Uri.fromFile(java.io.File(fp)) else android.net.Uri.parse(fp)
        if (layer.fileType == FileType.PHOTO) {
            viewModelScope.launch {
                isLoading = true; loadError = null; updateLoadState(isLoading = true, loadError = null)
                val geoPhoto = withContext(Dispatchers.IO) { ExifGpsParser.parse(context, uri, layer.name) }
                if (geoPhoto != null) _photoData[layerId] = listOf(geoPhoto) else { loadError = "تعذّرت إعادة تحميل الصورة: لا توجد بيانات GPS"; updateLoadState(loadError = loadError) }
                isLoading = false; updateLoadState(isLoading = false)
            }
        } else loadAndCacheLayer(context, layerId, uri, layer.name, removeOnError = false)
    }

    fun requestZoomToLayer(layerId: String) {
        _zoomEvents.tryEmit(ZoomEvent.ZoomToLayer(layerId))
        requestedZoomLayerId = layerId
    }
    fun requestZoomToFeature(lat: Double, lng: Double) {
        _zoomEvents.tryEmit(ZoomEvent.ZoomToFeature(lat, lng))
        requestedZoomFeatureCoords = lat to lng
    }
}

sealed interface ZoomEvent {
    data class ZoomToLayer(val layerId: String) : ZoomEvent
    data class ZoomToFeature(val lat: Double, val lng: Double) : ZoomEvent
}

fun deriveGeomType(features: List<FeatureRow>): String? {
    if (features.isEmpty()) return null
    val counts = features.groupBy { feat ->
        when (feat.geometryType) {
            "Point", "MultiPoint" -> "Point"; "LineString", "MultiLineString" -> "Line"; "Polygon", "MultiPolygon" -> "Polygon"
            else -> null
        }
    }
    val known = counts.filterKeys { it != null }
    if (known.isEmpty()) return null
    return known.maxByOrNull { it.value.size }?.key
}
