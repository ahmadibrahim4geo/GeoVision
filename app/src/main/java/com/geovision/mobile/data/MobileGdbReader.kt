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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Reader for ArcGIS Pro Mobile Geodatabase (.geodatabase) files.
 *
 * ArcGIS Pro Mobile Geodatabases are SQLite database files storing spatial data
 * in user tables with binary geometry columns (ST_GEOMETRY / Esri Shape buffer or WKB).
 */
object MobileGdbReader {
    private const val TAG = "MobileGdbReader"

    data class MobileGdbLayer(
        val tableName: String,
        val geomColumn: String,
        val srsId: Int = 4326,
        val srsName: String? = null
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
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to read Mobile Geodatabase: ${e.message}", e)
            null
        } finally {
            if (tmpPath != null) try { File(tmpPath).delete() } catch (_: Exception) {}
        }
    }

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
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to read selected Mobile Geodatabase layers: ${e.message}", e)
            null
        } finally {
            if (tmpPath != null) try { File(tmpPath).delete() } catch (_: Exception) {}
        }
    }

    suspend fun readEachSelected(
        context: Context, uri: Uri, fileName: String, selectedTableNames: List<String>
    ): List<Pair<String, LayerDetailInfo>> = withContext(Dispatchers.IO) {
        var tmpPath: String? = null
        try {
            coroutineContext.ensureActive()
            val pair = openDb(context, uri)
            val db = pair.first ?: return@withContext emptyList()
            tmpPath = pair.second
            db.use { dbh ->
                coroutineContext.ensureActive()
                val allLayers = listLayers(dbh)
                val layers = allLayers.filter { it.tableName in selectedTableNames }
                val results = mutableListOf<Pair<String, LayerDetailInfo>>()
                for (layer in layers) {
                    coroutineContext.ensureActive()
                    val detail = readSelectedLayers(dbh, listOf(layer), "$fileName / ${layer.tableName}", uri.toString())
                    results.add(layer.tableName to detail)
                }
                results
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to read each Mobile Geodatabase layer: ${e.message}", e)
            emptyList()
        } finally {
            if (tmpPath != null) try { File(tmpPath).delete() } catch (_: Exception) {}
        }
    }

    suspend fun listLayerNames(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
        var tmpPath: String? = null
        try {
            val pair = openDb(context, uri)
            val db = pair.first ?: return@withContext emptyList()
            tmpPath = pair.second
            db.use { dbh ->
                listLayers(dbh).map { it.tableName }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to list Mobile Geodatabase layers: ${e.message}")
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
            val db = SQLiteDatabase.openDatabase(
                path,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
            Pair(db, if (isContentUri) path else null)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to open Mobile Geodatabase: ${e.message}")
            Pair(null, null)
        }
    }

    private fun copyToTemp(context: Context, uri: Uri): String? {
        val tmpFile = File(context.cacheDir, "mobile_gdb_${System.nanoTime()}.geodatabase")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
            }
            tmpFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tags.PARSER, "Failed to copy Mobile Geodatabase: ${e.message}")
            null
        }
    }

    fun listLayers(db: SQLiteDatabase): List<MobileGdbLayer> {
        val layers = mutableListOf<MobileGdbLayer>()
        val defaultSrs = getSrsInfo(db)

        // 1. Try querying GDB_Items if it exists
        try {
            val gdbItemsQuery = """
                SELECT Name, Definition FROM GDB_Items 
                WHERE Type IN ('{70737809-B030-4614-B0A4-2E0F967B63FA}', '{D28A25D6-4F18-4694-81D2-8356976F5064}')
                ORDER BY Name
            """.trimIndent()
            db.rawQuery(gdbItemsQuery, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) {
                        val geomCol = findGeometryColumn(db, name)
                        if (geomCol != null) {
                            layers.add(MobileGdbLayer(name, geomCol, defaultSrs.first, defaultSrs.second))
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // GDB_Items not present or query failed; fallback to sqlite_master
        }

        // 2. Fallback: inspect sqlite_master for all non-system user tables
        if (layers.isEmpty()) {
            try {
                val tableQuery = """
                    SELECT name FROM sqlite_master 
                    WHERE type='table' 
                      AND name NOT LIKE 'sqlite_%' 
                      AND name NOT LIKE 'gdb_%' 
                      AND name NOT LIKE 'st_%' 
                      AND name NOT LIKE 'i_%' 
                      AND name NOT LIKE 's_%'
                      AND name NOT LIKE 'g_%'
                    ORDER BY name
                """.trimIndent()
                db.rawQuery(tableQuery, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrBlank()) {
                            val geomCol = findGeometryColumn(db, name)
                            if (geomCol != null) {
                                layers.add(MobileGdbLayer(name, geomCol, defaultSrs.first, defaultSrs.second))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tags.PARSER, "Failed to list tables from sqlite_master: ${e.message}")
            }
        }

        return layers
    }

    private fun findGeometryColumn(db: SQLiteDatabase, tableName: String): String? {
        val candidateNames = listOf("shape", "geom", "geometry", "the_geom", "geometria")
        var bestCandidate: String? = null
        try {
            db.rawQuery("PRAGMA table_info(\"${tableName.replace("\"", "\"\"")}\")", null).use { c ->
                val nameIdx = c.getColumnIndex("name")
                val typeIdx = c.getColumnIndex("type")
                while (c.moveToNext()) {
                    val colName = if (nameIdx >= 0) c.getString(nameIdx) else c.getString(1)
                    val colType = if (typeIdx >= 0) c.getString(typeIdx).uppercase(Locale.US) else ""
                    if (candidateNames.any { it.equals(colName, ignoreCase = true) }) {
                        return colName
                    }
                    if (colType.contains("GEOMETRY") || colType.contains("POINT") || colType.contains("POLYGON") || colType.contains("LINE")) {
                        bestCandidate = colName
                    }
                }
            }
        } catch (_: Exception) {}
        return bestCandidate
    }

    private fun getSrsInfo(db: SQLiteDatabase): Pair<Int, String?> {
        // Try ST_SPATIAL_REFERENCE_SYSTEMS
        try {
            db.rawQuery("SELECT srs_id, srs_name, definition FROM ST_SPATIAL_REFERENCE_SYSTEMS LIMIT 1", null).use { c ->
                if (c.moveToFirst()) {
                    val srsId = c.getInt(0)
                    val name = c.getString(1) ?: c.getString(2)
                    return Pair(srsId, "EPSG:$srsId ($name)")
                }
            }
        } catch (_: Exception) {}

        // Try spatial_ref_sys
        try {
            db.rawQuery("SELECT srid, auth_name, auth_srid, srtext FROM spatial_ref_sys LIMIT 1", null).use { c ->
                if (c.moveToFirst()) {
                    val srsId = c.getInt(2)
                    val auth = c.getString(1) ?: "EPSG"
                    return Pair(srsId, "$auth:$srsId")
                }
            }
        } catch (_: Exception) {}

        return Pair(4326, "WGS 84")
    }

    private suspend fun readSelectedLayers(
        dbh: SQLiteDatabase, layers: List<MobileGdbLayer>, fileName: String, filePath: String
    ): LayerDetailInfo {
        val allFeatures = mutableListOf<FeatureRow>()
        var crs = layers.firstOrNull()?.srsName ?: "WGS 84"

        for (layer in layers) {
            coroutineContext.ensureActive()
            allFeatures.addAll(readFeatures(dbh, layer))
        }

        // Project features to WGS84 if needed
        val projectedFeatures = CoordinateConverter.convertFeaturesToWgs84(allFeatures, crs)

        return LayerDetailInfo(
            fileName = fileName,
            filePath = filePath,
            crs = if (projectedFeatures !== allFeatures) "EPSG:4326" else crs,
            extent = calculateExtent(projectedFeatures),
            features = projectedFeatures
        )
    }

    private fun readFeatures(db: SQLiteDatabase, layer: MobileGdbLayer): List<FeatureRow> {
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
                    var geomBytes: ByteArray? = null

                    for (i in 1 until c.columnCount) {
                        val colName = c.getColumnName(i)
                        if (colName.equals(layer.geomColumn, ignoreCase = true)) {
                            geomBytes = c.getBlob(i)
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

                    if (geomBytes != null && geomBytes.size >= 5) {
                        val parsedGeom = parseGeometryBytes(geomBytes)
                        if (parsedGeom != null) {
                            features.add(FeatureRow(
                                id = fid,
                                properties = props,
                                geometryType = parsedGeom.first,
                                geometryCoordinates = parsedGeom.second
                            ))
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
            db.rawQuery("PRAGMA table_info(\"${tableName.replace("\"", "\"\"")}\")", null).use { c ->
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    val name = if (nameIdx >= 0) c.getString(nameIdx) else c.getString(1)
                    cols.add(name)
                }
            }
        } catch (_: Exception) {}
        return cols
    }

    /**
     * Parses geometry bytes: tries WKB (OGC/GeoPackage) first, then Esri Shape buffer.
     */
    fun parseGeometryBytes(bytes: ByteArray): Pair<String, String>? {
        if (bytes.size < 5) return null

        // 1. Try standard WkbParser
        try {
            val wkb = WkbParser.parse(bytes)
            if (wkb != null && wkb.second.isNotBlank()) {
                return Pair(wkb.first, wkb.second)
            }
        } catch (_: Exception) {}

        // 2. Parse Esri Shape buffer
        try {
            return parseEsriShapeBuffer(bytes)
        } catch (e: Exception) {
            AppLogger.d(TAG, "Failed to parse Esri shape buffer: ${e.message}")
        }

        return null
    }

    /**
     * Parses Esri Shape binary format stored in ST_GEOMETRY columns.
     * Format specification matches standard ESRI Shape type IDs:
     * 1 = Point, 3 = Polyline, 5 = Polygon, 8 = MultiPoint,
     * 11 = PointZ, 13 = PolylineZ, 15 = PolygonZ, 18 = MultiPointZ
     */
    private fun parseEsriShapeBuffer(bytes: ByteArray): Pair<String, String>? {
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val shapeType = bb.getInt()

        return when (shapeType) {
            1 -> { // Point
                val x = bb.getDouble()
                val y = bb.getDouble()
                Pair("Point", "[$x,$y]")
            }
            11 -> { // PointZ
                val x = bb.getDouble()
                val y = bb.getDouble()
                val z = if (bb.remaining() >= 8) bb.getDouble() else 0.0
                Pair("Point", "[$x,$y,$z]")
            }
            3, 13 -> { // Polyline / PolylineZ
                parsePolylineOrPolygon(bb, isPolygon = false)
            }
            5, 15 -> { // Polygon / PolygonZ
                parsePolylineOrPolygon(bb, isPolygon = true)
            }
            8, 18 -> { // MultiPoint / MultiPointZ
                parseMultiPoint(bb)
            }
            else -> null
        }
    }

    private fun parsePolylineOrPolygon(bb: ByteBuffer, isPolygon: Boolean): Pair<String, String>? {
        // Skip bounding box: 4 doubles = 32 bytes
        if (bb.remaining() < 32 + 8) return null
        bb.position(bb.position() + 32)

        val numParts = bb.getInt()
        val numPoints = bb.getInt()
        if (numParts <= 0 || numPoints <= 0) return null
        if (bb.remaining() < (numParts * 4) + (numPoints * 16)) return null

        val parts = IntArray(numParts)
        for (i in 0 until numParts) {
            parts[i] = bb.getInt()
        }

        val points = Array(numPoints) {
            val x = bb.getDouble()
            val y = bb.getDouble()
            "[$x,$y]"
        }

        val partList = mutableListOf<String>()
        for (i in 0 until numParts) {
            val startIdx = parts[i]
            val endIdx = if (i + 1 < numParts) parts[i + 1] else numPoints
            if (startIdx in 0 until numPoints && endIdx in (startIdx + 1)..numPoints) {
                val partPoints = points.sliceArray(startIdx until endIdx)
                partList.add("[" + partPoints.joinToString(",") + "]")
            }
        }

        return if (isPolygon) {
            val json = "[" + partList.joinToString(",") + "]"
            Pair("Polygon", json)
        } else {
            if (numParts == 1) {
                Pair("LineString", partList.first())
            } else {
                val json = "[" + partList.joinToString(",") + "]"
                Pair("MultiLineString", json)
            }
        }
    }

    private fun parseMultiPoint(bb: ByteBuffer): Pair<String, String>? {
        // Skip bounding box: 32 bytes
        if (bb.remaining() < 32 + 4) return null
        bb.position(bb.position() + 32)

        val numPoints = bb.getInt()
        if (numPoints <= 0 || bb.remaining() < numPoints * 16) return null

        val points = mutableListOf<String>()
        for (i in 0 until numPoints) {
            val x = bb.getDouble()
            val y = bb.getDouble()
            points.add("[$x,$y]")
        }

        return Pair("MultiPoint", "[" + points.joinToString(",") + "]")
    }

    private fun calculateExtent(features: List<FeatureRow>): String {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var hasCoords = false

        for (f in features) {
            val coords = f.geometryCoordinates ?: continue
            val numRegex = Regex("""[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?""")
            val matches = numRegex.findAll(coords).map { it.value.toDoubleOrNull() }.filterNotNull().toList()
            if (matches.size >= 2) {
                for (i in 0 until matches.size - 1 step 2) {
                    val x = matches[i]
                    val y = matches[i + 1]
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                    hasCoords = true
                }
            }
        }

        return if (hasCoords) {
            String.format(Locale.US, "(%.6f,%.6f) to (%.6f,%.6f)", minX, minY, maxX, maxY)
        } else ""
    }
}
