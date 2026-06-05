package com.geovision.mobile.data

import android.content.Context

class LayerMetadataRepository(context: Context) {
    private val appContext = context.applicationContext

    data class StoredLayer(
        val id: String,
        val name: String,
        val geometryType: String?,
        val featureCount: Int,
        val gpkgTableName: String?,
        val visible: Boolean,
        val status: String,
        val sourceFileId: String?,
        val sourceType: String?
    )

    fun listLayers(): List<StoredLayer> {
        val paths = ProjectStorageManager.ensureDefaultProject(appContext)
        val metadata = MetadataDatabase.open(appContext, paths)
        metadata.readableDatabase.use { db ->
            val layers = mutableListOf<StoredLayer>()
            db.rawQuery(
                """
                    SELECT l.id, l.name, l.geometry_type, l.feature_count, l.gpkg_table_name,
                           l.visible, l.status, l.source_file_id, s.file_type
                    FROM layers l
                    LEFT JOIN source_files s ON s.id = l.source_file_id
                    WHERE l.project_id = ?
                    ORDER BY l.created_at ASC
                """.trimIndent(),
                arrayOf(ProjectStorageManager.DEFAULT_PROJECT_ID)
            ).use { c ->
                while (c.moveToNext()) {
                    layers.add(
                        StoredLayer(
                            id = c.getString(0),
                            name = c.getString(1),
                            geometryType = c.getString(2),
                            featureCount = c.getInt(3),
                            gpkgTableName = c.getString(4),
                            visible = c.getInt(5) == 1,
                            status = c.getString(6),
                            sourceFileId = c.getString(7),
                            sourceType = c.getString(8)
                        )
                    )
                }
            }
            return layers
        }
    }

    fun findLayer(layerId: String): StoredLayer? {
        val paths = ProjectStorageManager.ensureDefaultProject(appContext)
        val metadata = MetadataDatabase.open(appContext, paths)
        metadata.readableDatabase.use { db ->
            db.rawQuery(
                """
                    SELECT l.id, l.name, l.geometry_type, l.feature_count, l.gpkg_table_name,
                           l.visible, l.status, l.source_file_id, s.file_type
                    FROM layers l
                    LEFT JOIN source_files s ON s.id = l.source_file_id
                    WHERE l.id = ?
                """.trimIndent(),
                arrayOf(layerId)
            ).use { c ->
                if (!c.moveToFirst()) return null
                return StoredLayer(
                    id = c.getString(0),
                    name = c.getString(1),
                    geometryType = c.getString(2),
                    featureCount = c.getInt(3),
                    gpkgTableName = c.getString(4),
                    visible = c.getInt(5) == 1,
                    status = c.getString(6),
                    sourceFileId = c.getString(7),
                    sourceType = c.getString(8)
                )
            }
        }
    }

    fun deleteLayer(layerId: String) {
        val paths = ProjectStorageManager.ensureDefaultProject(appContext)
        val metadata = MetadataDatabase.open(appContext, paths)
        val layer = findLayer(layerId)
        metadata.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                db.delete("layer_display_settings", "layer_id = ?", arrayOf(layerId))
                db.delete("layer_crs", "layer_id = ?", arrayOf(layerId))
                db.delete("media_files", "layer_id = ?", arrayOf(layerId))
                db.delete("layers", "id = ?", arrayOf(layerId))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        val tableName = layer?.gpkgTableName
        if (!tableName.isNullOrBlank() && paths.projectGpkg.exists()) {
            GeoPackageWriter(paths.projectGpkg).dropLayer(tableName)
        }
    }
}
