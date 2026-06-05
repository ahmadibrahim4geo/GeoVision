package com.geovision.mobile.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import com.geovision.mobile.ui.screens.layers.LayerDetailInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

object GpkgReader {
    private const val TAG = "GpkgReader"

    data class GpkgLayer(
        val tableName: String,
        val geomColumn: String,
        val geomType: String,
        val srsId: Int
    )

    suspend fun read(context: Context, uri: Uri, fileName: String): LayerDetailInfo? = withContext(Dispatchers.IO) {
        var tmpPath: String? = null
        try {
            coroutineContext.ensureActive()
            val pair = openDb(context, uri)
            val db = pair.first ?: return@withContext null
            tmpPath = pair.second
            db.use { dbh ->
                coroutineContext.ensureActive()
                val layers = listLayers(dbh)
                if (layers.isEmpty()) return@withContext null
                readSelectedLayers(dbh, layers, fileName, uri.toString())
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to read GeoPackage: ${e.message}", e)
            null
        } finally {
            if (tmpPath != null) try { File(tmpPath).delete() } catch (_: Exception) {}
        }
    }

    /**
     * قراءة طبقات محددة فقط من GeoPackage (بدلاً من كل الطبقات).
     * @param selectedTableNames قائمة بأسماء الجداول المطلوبة (null = كل الطبقات)
     */
    suspend fun readSelected(
        context: Context, uri: Uri, fileName: String, selectedTableNames: List<String>
    ): LayerDetailInfo? = withContext(Dispatchers.IO) {
        var tmpPath: String? = null
        try {
            coroutineContext.ensureActive()
            val pair = openDb(context, uri)
            val db = pair.first ?: return@withContext null
            tmpPath = pair.second
            db.use { dbh ->
                coroutineContext.ensureActive()
                val allLayers = listLayers(dbh)
                val layers = allLayers.filter { it.tableName in selectedTableNames }
                if (layers.isEmpty()) return@withContext null
                readSelectedLayers(dbh, layers, fileName, uri.toString())
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to read selected GeoPackage layers: ${e.message}", e)
            null
        } finally {
            if (tmpPath != null) try { File(tmpPath).delete() } catch (_: Exception) {}
        }
    }

    private suspend fun readSelectedLayers(
        dbh: SQLiteDatabase, layers: List<GpkgLayer>, fileName: String, filePath: String
    ): LayerDetailInfo {
        val allFeatures = mutableListOf<FeatureRow>()
        var crs = "WGS 84"
        var extent = ""
        for (layer in layers) {
            coroutineContext.ensureActive()
            val srs = getSrs(dbh, layer.srsId)
            if (srs != null) crs = srs
            val bbox = getBbox(dbh, layer.tableName)
            if (bbox != null) extent = bbox
            allFeatures.addAll(readFeatures(dbh, layer))
        }
        // Project features to WGS84 if the source CRS is not already geographic WGS84
        val projectedFeatures = CoordinateConverter.convertFeaturesToWgs84(allFeatures, crs)

        return LayerDetailInfo(
            fileName = fileName,
            filePath = filePath,
            crs = if (projectedFeatures !== allFeatures) "EPSG:4326" else crs,
            extent = extent,
            features = projectedFeatures
        )
    }

    suspend fun listLayerNames(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
        var tmpPath: String? = null
        try {
            val pair = openDb(context, uri)
            val db = pair.first ?: return@withContext emptyList()
            tmpPath = pair.second
            db.use { dbh ->
                val layers = mutableListOf<String>()
                dbh.rawQuery("SELECT table_name FROM gpkg_contents ORDER BY table_name", null).use { c ->
                    while (c.moveToNext()) layers.add(c.getString(0))
                }
                layers
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to list layers: ${e.message}")
            emptyList()
        } finally {
            if (tmpPath != null) try { File(tmpPath).delete() } catch (_: Exception) {}
        }
    }

    private fun openDb(context: Context, uri: Uri): Pair<SQLiteDatabase?, String?> {
        return try {
            val isContentUri = uri.scheme == "content"
            val path = if (isContentUri) copyToTemp(context, uri) else uri.path
            if (path == null) return Pair(null, null)
            val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
            Pair(db, if (isContentUri) path else null)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to open GeoPackage: ${e.message}")
            Pair(null, null)
        }
    }

    private fun copyToTemp(context: Context, uri: Uri): String? {
        val tmpFile = File(context.cacheDir, "gpkg_${System.nanoTime()}.gpkg")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
            }
            tmpFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to copy file: ${e.message}")
            null
        }
    }

    private fun listLayers(db: SQLiteDatabase): List<GpkgLayer> {
        val layers = mutableListOf<GpkgLayer>()
        try {
            db.rawQuery("""
                SELECT c.table_name, g.column_name, g.geometry_type_name, g.srs_id
                FROM gpkg_contents c
                JOIN gpkg_geometry_columns g ON c.table_name = g.table_name
                ORDER BY c.table_name
            """.trimIndent(), null).use { c ->
                while (c.moveToNext()) {
                    layers.add(GpkgLayer(
                        tableName = c.getString(0),
                        geomColumn = c.getString(1),
                        geomType = c.getString(2),
                        srsId = c.getInt(3)
                    ))
                }
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "No gpkg_geometry_columns: ${e.message}")
        }
        return layers
    }

    private fun getSrs(db: SQLiteDatabase, srsId: Int): String? {
        try {
            db.rawQuery("SELECT organization, organization_coordsys_id, description FROM gpkg_spatial_ref_sys WHERE srs_id = ?", arrayOf(srsId.toString())).use { c ->
                if (c.moveToFirst()) {
                    val org = c.getString(0)
                    val csId = c.getInt(1)
                    val desc = c.getString(2)
                    return "$org:$csId (${desc.take(60)})"
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun getBbox(db: SQLiteDatabase, tableName: String): String? {
        try {
            db.rawQuery("SELECT min_x, min_y, max_x, max_y FROM gpkg_contents WHERE table_name = ?", arrayOf(tableName)).use { c ->
                if (c.moveToFirst()) {
                    val minX = c.getDouble(0); val minY = c.getDouble(1)
                    val maxX = c.getDouble(2); val maxY = c.getDouble(3)
                    return java.lang.String.format(java.util.Locale.US, "(%.6f,%.6f) to (%.6f,%.6f)", minX, minY, maxX, maxY)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun readFeatures(db: SQLiteDatabase, layer: GpkgLayer): List<FeatureRow> {
        val features = mutableListOf<FeatureRow>()
        val cols = getColumns(db, layer.tableName)
        if (cols.isEmpty()) return features

        val qName = "\"${layer.tableName.replace("\"", "\"\"")}\""
        val qCols = cols.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        val sql = "SELECT rowid,$qCols FROM $qName"

        try {
            db.rawQuery(sql, null).use { c ->
                while (c.moveToNext()) {
                    val fid = c.getLong(0).toString()
                    val props = mutableMapOf<String, String>()
                    var wkbBytes: ByteArray? = null

                    for (i in 1 until c.columnCount) {
                        val colName = c.getColumnName(i)
                        if (colName == layer.geomColumn) {
                            wkbBytes = c.getBlob(i)
                        } else {
                            val v = when (c.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> null
                                Cursor.FIELD_TYPE_INTEGER -> c.getLong(i).toString()
                                Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i).toString()
                                Cursor.FIELD_TYPE_STRING -> c.getString(i)
                                Cursor.FIELD_TYPE_BLOB -> "[BLOB ${c.getBlob(i).size}B]"
                                else -> null
                            }
                            if (v != null) props[colName] = v
                        }
                    }

                    if (wkbBytes != null && wkbBytes.size >= 5) {
                        val geom = WkbParser.parse(wkbBytes)
                        if (geom != null) {
                            features.add(FeatureRow(id = fid, properties = props, geometryType = geom.first, geometryCoordinates = geom.second))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "Failed to read table ${layer.tableName}: ${e.message}")
        }
        return features
    }

    private fun getColumns(db: SQLiteDatabase, tableName: String): List<String> {
        val cols = mutableListOf<String>()
        try {
            db.rawQuery("PRAGMA table_info('${tableName.replace("'", "''")}')", null).use { c ->
                while (c.moveToNext()) cols.add(c.getString(1))
            }
        } catch (_: Exception) {}
        return cols
    }
}
