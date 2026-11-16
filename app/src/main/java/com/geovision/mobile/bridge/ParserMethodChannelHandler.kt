package com.geovision.mobile.bridge

import android.content.Context
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.data.ShapefileImporter
import com.geovision.mobile.data.GeoJsonParser
import com.geovision.mobile.data.GdbParser
import com.geovision.mobile.data.KmlParser
import com.geovision.mobile.data.GpxParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * معالج القناة المنصية (Platform Channel Handler)
 * يربط بين طلبات واجهة Flutter المتميزة ومحللات البيانات الجغرافية السريعة في أندرويد.
 */
class ParserMethodChannelHandler(private val context: Context, private val scope: CoroutineScope) {

    fun handleParseRequest(
        filePath: String,
        fileType: String,
        onResult: (Map<String, Any?>) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        onResult(mapOf("success" to false, "error" to "الملف غير موجود في المسار المخصص"))
                    }
                    return@launch
                }

                val lowerType = fileType.lowercase()
                val featureCount = when {
                    lowerType.contains("shape") || filePath.endsWith(".shp") -> {
                        val imp = ShapefileImporter.importFromUri(android.net.Uri.fromFile(file), context, file.name)
                        imp.features.size
                    }
                    lowerType.contains("json") || filePath.endsWith(".geojson") -> {
                        val parsed = GeoJsonParser.parse(file.readText(), file.name)
                        parsed.features.size
                    }
                    lowerType.contains("kml") || filePath.endsWith(".kml") -> {
                        val parsed = KmlParser.parse(file.readText(), file.name)
                        parsed.features.size
                    }
                    else -> 0
                }

                withContext(Dispatchers.Main) {
                    onResult(
                        mapOf(
                            "success" to true,
                            "fileName" to file.name,
                            "featureCount" to featureCount,
                            "fileType" to fileType
                        )
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tags.LAYER, "Native bridge parse failure: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(mapOf("success" to false, "error" to (e.message ?: "فشل التحليل الأولي")))
                }
            }
        }
    }
}
