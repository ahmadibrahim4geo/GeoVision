package com.geovision.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.geovision.mobile.ui.screens.layers.FeatureRow
import org.json.JSONObject

object DatabaseImportWriter {
    data class PersistedImport(
        val projectId: String,
        val sourceFileId: String,
        val importJobId: String,
        val layerId: String,
        val gpkgTableName: String,
        val featureCount: Int
    )

    fun persistLayer(
        context: Context,
        appLayerId: String,
        layerName: String,
        sourceUri: Uri,
        sourceType: String,
        crs: String?,
        features: List<FeatureRow>,
        visible: Boolean,
        styleJson: String? = null,
        report: ImportReport? = null
    ): PersistedImport {
        val paths = ProjectStorageManager.ensureDefaultProject(context)
        val metadata = MetadataDatabase.open(context, paths)
        val writer = GeoPackageWriter(paths.projectGpkg)
        val now = System.currentTimeMillis()

        val sourceFileId = MetadataDatabase.newId("src")
        val importJobId = MetadataDatabase.newId("job")
        val layerId = appLayerId

        val written = writer.writeLayer(layerName, features, crs)

        metadata.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                db.insertOrThrow("source_files", null, ContentValues().apply {
                    put("id", sourceFileId)
                    put("project_id", ProjectStorageManager.DEFAULT_PROJECT_ID)
                    put("original_name", layerName)
                    put("original_uri", sourceUri.toString())
                    put("file_type", sourceType)
                    put("imported_at", now)
                    put("status", "IMPORTED")
                })
                db.insertOrThrow("import_jobs", null, ContentValues().apply {
                    put("id", importJobId)
                    put("project_id", ProjectStorageManager.DEFAULT_PROJECT_ID)
                    put("source_file_id", sourceFileId)
                    put("status", "COMPLETED")
                    put("current_step", "stored_in_project_gpkg")
                    put("progress_percent", 100)
                    put("started_at", now)
                    put("finished_at", now)
                })
                db.insertWithOnConflict("layers", null, ContentValues().apply {
                    put("id", layerId)
                    put("project_id", ProjectStorageManager.DEFAULT_PROJECT_ID)
                    put("source_file_id", sourceFileId)
                    put("name", layerName)
                    put("original_name", layerName)
                    put("geometry_type", written.geometryType)
                    put("feature_count", written.featureCount)
                    put("gpkg_table_name", written.tableName)
                    put("visible", if (visible) 1 else 0)
                    put("style_json", styleJson)
                    put("status", "READY")
                    put("created_at", now)
                    put("updated_at", now)
                }, SQLiteDatabase.CONFLICT_REPLACE)
                val crsInfo = CrsValidator.inspect(crs)
                db.insertOrThrow("layer_crs", null, ContentValues().apply {
                    put("id", MetadataDatabase.newId("crs"))
                    put("layer_id", layerId)
                    put("original_crs_name", crs)
                    put("target_epsg", 4326)
                    put("target_crs_name", "EPSG:4326")
                    put("transform_status", if (crsInfo.requiresUserSelection) "NEEDS_REVIEW" else "OK")
                    put("warning_message", CrsValidator.warningFor(crs)?.message)
                })
                persistReportLayers(db, importJobId, report, layerName, crs, written)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        return PersistedImport(
            projectId = ProjectStorageManager.DEFAULT_PROJECT_ID,
            sourceFileId = sourceFileId,
            importJobId = importJobId,
            layerId = layerId,
            gpkgTableName = written.tableName,
            featureCount = written.featureCount
        )
    }

    private fun persistReportLayers(
        db: SQLiteDatabase,
        importJobId: String,
        report: ImportReport?,
        layerName: String,
        crs: String?,
        written: GeoPackageWriter.WrittenLayer
    ) {
        val reportLayers = report?.layers?.takeIf { it.isNotEmpty() }
            ?: listOf(
                ImportLayerInfo(
                    id = layerName,
                    name = layerName,
                    geometryType = written.geometryType,
                    featureCount = written.featureCount.toLong(),
                    crs = CrsValidator.inspect(crs),
                    status = ImportStatus.IMPORTED
                )
            )
        reportLayers.forEach { layer ->
            db.insertOrThrow("import_report_layers", null, ContentValues().apply {
                put("id", MetadataDatabase.newId("report_layer"))
                put("import_job_id", importJobId)
                put("layer_name", layer.name)
                put("source_layer_name", layer.id)
                put("geometry_type", layer.geometryType)
                put("feature_count", layer.featureCount)
                put("original_crs", layer.crs?.displayName ?: crs)
                put("target_crs", "EPSG:4326")
                put("support_status", layer.status.name)
                put("import_status", layer.status.name)
                put("warning_message", layer.message)
                put("error_message", report?.errors?.firstOrNull { it.cause == layer.id }?.message)
            })
        }
    }

    fun styleJson(color: Long, transparency: Float, pointSize: Float, lineWidth: Float): String {
        return JSONObject()
            .put("color", color)
            .put("opacity", transparency.toDouble())
            .put("pointSize", pointSize.toDouble())
            .put("lineWidth", lineWidth.toDouble())
            .toString()
    }
}
