package com.geovision.mobile.ui.screens.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import com.geovision.mobile.data.geo.CoordinateConverter
import com.geovision.mobile.data.geo.FormatDetector

data class ConvertedPoint(
    val index: Int,
    val original: String,
    val dd: String,
    val dms: String,
    val ddm: String,
    val utm: String,
    val epsg3857: String,
    val lat: Double?,
    val lon: Double?,
    val error: Boolean
)

enum class OutputFormat { DD, DMS, DDM, UTM, EPSG3857 }

data class CalculatorUiState(
    val inputText: String = "",
    val results: List<ConvertedPoint> = emptyList(),
    val statusMessage: String = "جاهز لاستقبال الإحداثيات. أدخل نقاطاً مفصولة بأسطر جديدة.",
    val selectedFormats: Set<OutputFormat> = emptySet(),
    val latHemisphere: Char = 'N',
    val lonHemisphere: Char = 'E',
    val utmZone: Int = 36,
    val utmHemisphere: Char = 'N'
) {
    val visibleFormats: Set<OutputFormat>
        get() = if (selectedFormats.isEmpty()) OutputFormat.entries.toSet() else selectedFormats
}

class CalculatorViewModel : ViewModel() {

    private val _stateFlow = MutableStateFlow(CalculatorUiState())
    val stateFlow: StateFlow<CalculatorUiState> = _stateFlow.asStateFlow()

    var state by mutableStateOf(CalculatorUiState())
        private set

    fun updateInput(text: String) {
        state = state.copy(inputText = text)
        _stateFlow.value = state
    }

    fun toggleFormat(format: OutputFormat) {
        val current = state.selectedFormats
        val updated = if (format in current) current - format else current + format
        state = state.copy(selectedFormats = updated)
        _stateFlow.value = state
    }

    fun selectAllFormats() {
        state = state.copy(selectedFormats = emptySet())
        _stateFlow.value = state
    }

    fun setLatHemisphere(hem: Char) {
        state = state.copy(latHemisphere = hem)
        _stateFlow.value = state
    }

    fun setLonHemisphere(hem: Char) {
        state = state.copy(lonHemisphere = hem)
        _stateFlow.value = state
    }

    fun setUtmZone(zone: Int) {
        state = state.copy(utmZone = zone.coerceIn(1, 60))
        _stateFlow.value = state
    }

    fun setUtmHemisphere(hem: Char) {
        state = state.copy(utmHemisphere = hem)
        _stateFlow.value = state
    }

    fun convert() {
        val input = state.inputText.trim()
        if (input.isBlank()) {
            state = state.copy(statusMessage = "⚠️ الرجاء إدخال إحداثيات.", results = emptyList())
            _stateFlow.value = state; return
        }

        val lines = input.split("\n").filter { it.isNotBlank() }
        val results = mutableListOf<ConvertedPoint>()

        lines.forEachIndexed { index, line ->
            val parsed = FormatDetector.detect(line, state.utmZone, state.utmHemisphere)
            if (parsed != null) {
                var (lat, lon) = parsed
                if (parsed.formatName == "DD") {
                    if (lat >= 0 && state.latHemisphere == 'S') lat = -lat
                    else if (lat < 0 && state.latHemisphere == 'N') lat = abs(lat)
                    if (lon >= 0 && state.lonHemisphere == 'W') lon = -lon
                    else if (lon < 0 && state.lonHemisphere == 'E') lon = abs(lon)
                }
                val utmResult = CoordinateConverter.ddToUtm(lat, lon)
                val mercResult = CoordinateConverter.ddToWebMercator(lat, lon)
                results.add(
                    ConvertedPoint(
                        index = index + 1,
                        original = line.trim(),
                        dd = CoordinateConverter.formatDD(lat, lon),
                        dms = "${CoordinateConverter.ddToDms(lat, true)} - ${CoordinateConverter.ddToDms(lon, false)}",
                        ddm = "${CoordinateConverter.ddToDdm(lat, true)} - ${CoordinateConverter.ddToDdm(lon, false)}",
                        utm = CoordinateConverter.formatUtm(utmResult),
                        epsg3857 = CoordinateConverter.formatWebMercator(mercResult.first, mercResult.second),
                        lat = lat,
                        lon = lon,
                        error = false
                    )
                )
            } else {
                results.add(
                    ConvertedPoint(
                        index = index + 1,
                        original = line.trim(),
                        dd = "N/A", dms = "N/A", ddm = "N/A", utm = "N/A", epsg3857 = "N/A",
                        lat = null, lon = null,
                        error = true
                    )
                )
            }
        }

        val successCount = results.count { !it.error }
        val errorCount = results.count { it.error }
        val msg = if (errorCount == 0) {
            "✅ تم تحويل جميع النقاط ($successCount) بنجاح."
        } else {
            "✅ تم تحويل $successCount نقاط. ❌ $errorCount أخطاء."
        }

        state = state.copy(results = results, statusMessage = msg)
        _stateFlow.value = state
    }

    fun clear() {
        state = CalculatorUiState(statusMessage = "تم مسح جميع البيانات.")
        _stateFlow.value = state
    }

    fun getResultsAsText(formats: Set<OutputFormat> = state.visibleFormats): String {
        val sb = StringBuilder()
        val headers = mutableListOf("#", "Original")
        if (OutputFormat.DD in formats) headers.add("DD")
        if (OutputFormat.DMS in formats) headers.add("DMS")
        if (OutputFormat.DDM in formats) headers.add("DDM")
        if (OutputFormat.UTM in formats) headers.add("UTM")
        if (OutputFormat.EPSG3857 in formats) headers.add("EPSG:3857")
        sb.appendLine(headers.joinToString("\t"))
        state.results.forEach { r ->
            val vals = mutableListOf("${r.index}", r.original)
            if (OutputFormat.DD in formats) vals.add(r.dd)
            if (OutputFormat.DMS in formats) vals.add(r.dms)
            if (OutputFormat.DDM in formats) vals.add(r.ddm)
            if (OutputFormat.UTM in formats) vals.add(r.utm)
            if (OutputFormat.EPSG3857 in formats) vals.add(r.epsg3857)
            sb.appendLine(vals.joinToString("\t"))
        }
        return sb.toString()
    }

    fun getResultsAsCsv(formats: Set<OutputFormat> = state.visibleFormats): String {
        val sb = StringBuilder()
        val headers = mutableListOf("#", "Original")
        if (OutputFormat.DD in formats) headers.add("DD")
        if (OutputFormat.DMS in formats) headers.add("DMS")
        if (OutputFormat.DDM in formats) headers.add("DDM")
        if (OutputFormat.UTM in formats) headers.add("UTM")
        if (OutputFormat.EPSG3857 in formats) headers.add("EPSG:3857")
        sb.appendLine(headers.joinToString(","))
        state.results.forEach { r ->
            val vals = mutableListOf("\"${r.index}\"", "\"${r.original}\"")
            if (OutputFormat.DD in formats) vals.add("\"${r.dd}\"")
            if (OutputFormat.DMS in formats) vals.add("\"${r.dms}\"")
            if (OutputFormat.DDM in formats) vals.add("\"${r.ddm}\"")
            if (OutputFormat.UTM in formats) vals.add("\"${r.utm}\"")
            if (OutputFormat.EPSG3857 in formats) vals.add("\"${r.epsg3857}\"")
            sb.appendLine(vals.joinToString(","))
        }
        return sb.toString()
    }
}
