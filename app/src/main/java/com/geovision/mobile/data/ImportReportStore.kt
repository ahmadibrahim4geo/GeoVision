package com.geovision.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ImportReportStore {
    fun save(context: Context, report: ImportReport): File? {
        return try {
            val dir = File(context.filesDir, "GeoVision_Project/import_reports").also { it.mkdirs() }
            val safeName = report.sourceName.replace(Regex("""[^A-Za-z0-9._-]+"""), "_").ifBlank { "import" }
            val file = File(dir, "${System.currentTimeMillis()}_$safeName.json")
            file.writeText(toJson(report).toString(2), Charsets.UTF_8)
            file
        } catch (_: Exception) {
            null
        }
    }

    fun toJson(report: ImportReport): JSONObject {
        return JSONObject().apply {
            put("sourceName", report.sourceName)
            put("sourceType", report.sourceType)
            put("layers", JSONArray().apply {
                report.layers.forEach { layer ->
                    put(JSONObject().apply {
                        put("id", layer.id)
                        put("name", layer.name)
                        put("geometryType", layer.geometryType)
                        put("featureCount", layer.featureCount)
                        put("status", layer.status.name)
                        put("message", layer.message)
                        put("crs", layer.crs?.let { crs ->
                            JSONObject().apply {
                                put("authority", crs.authority)
                                put("code", crs.code)
                                put("name", crs.name)
                                put("isKnown", crs.isKnown)
                                put("requiresUserSelection", crs.requiresUserSelection)
                            }
                        })
                    })
                }
            })
            put("warnings", JSONArray().apply {
                report.warnings.forEach { warning ->
                    put(JSONObject().apply {
                        put("code", warning.code)
                        put("message", warning.message)
                    })
                }
            })
            put("errors", JSONArray().apply {
                report.errors.forEach { error ->
                    put(JSONObject().apply {
                        put("code", error.code)
                        put("message", error.message)
                        put("cause", error.cause)
                    })
                }
            })
        }
    }
}

