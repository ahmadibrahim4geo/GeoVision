package com.geovision.esri.viewer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class EsriReportStore(context: Context) {
    private val storage = EsriViewerStorage(context)

    fun save(report: EsriPackageReport): File {
        val paths = storage.ensurePaths()
        val file = File(paths.reports, "${report.packageName.replace(Regex("[^A-Za-z0-9._-]+"), "_")}_${System.currentTimeMillis()}.json")
        file.writeText(report.toJson().toString(2))
        return file
    }

    private fun EsriPackageReport.toJson(): JSONObject {
        return JSONObject()
            .put("packageName", packageName)
            .put("packageType", packageType.name)
            .put("packageVersion", packageVersion)
            .put("totalMaps", totalMaps)
            .put("totalLayers", totalLayers)
            .put("warnings", JSONArray(warnings))
            .put("errors", JSONArray(errors))
            .put("layers", JSONArray(layers.map { layer ->
                JSONObject()
                    .put("name", layer.name)
                    .put("type", layer.type)
                    .put("status", layer.status.name)
                    .put("errorMessage", layer.errorMessage)
                    .put("licenseWarning", layer.licenseWarning)
                    .put("isDisplayed", layer.isDisplayed)
            }))
    }
}
