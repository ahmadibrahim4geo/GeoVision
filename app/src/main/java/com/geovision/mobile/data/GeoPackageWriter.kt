package com.geovision.mobile.data

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteDatabase
import com.geovision.mobile.ui.screens.layers.FeatureRow
import org.json.JSONObject
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKBWriter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

class GeoPackageWriter(private val file: File) {

    data class WrittenLayer(
        val tableName: String,
        val featureCount: Int,
        val geometryType: String?,
        val minX: Double?,
        val minY: Double?,
        val maxX: Double?,
        val maxY: Double?
    )

    fun ensureCreated() {
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("PRAGMA foreign_keys=ON")
            db.execSQL("PRAGMA application_id=1196437808")
            db.execSQL("PRAGMA user_version=10200")
            createCoreTables(db)
            ensureWgs84(db)
        }
    }

    fun writeLayer(layerName: String, features: List<FeatureRow>, crs: String?): WrittenLayer {
        ensureCreated()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.beginTransaction()
            return try {
                val tableName = uniqueTableName(db, layerName)
                val geometryType = normalizeGeometryType(features.mapNotNull { it.geometryType }.firstOrNull())
                val hasRTree = createFeatureTable(db, tableName)

                var minX: Double? = null
                var minY: Double? = null
                var maxX: Double? = null
                var maxY: Double? = null
                var count = 0
                val wkbWriter = WKBWriter(2)

                features.forEach { feature ->
                    val geomType = feature.geometryType ?: return@forEach
                    val geom = GeometryParser.coordsToGeometry(feature.geometryCoordinates, geomType) ?: return@forEach
                    val wkb = toGeoPackageGeometry(wkbWriter.write(geom), 4326)
                    val values = ContentValues().apply {
                        put("geom", wkb)
                        put("properties", JSONObject(feature.properties).toString())
                        put("source_feature_id", feature.id)
                        put("min_x", geom.envelopeInternal.minX)
                        put("min_y", geom.envelopeInternal.minY)
                        put("max_x", geom.envelopeInternal.maxX)
                        put("max_y", geom.envelopeInternal.maxY)
                    }
                    val fid = db.insertOrThrow(quoteIdentifier(tableName), null, values)
                    if (hasRTree) insertRTree(db, tableName, fid, geom)
                    minX = minOfNullable(minX, geom.envelopeInternal.minX)
                    minY = minOfNullable(minY, geom.envelopeInternal.minY)
                    maxX = maxOfNullable(maxX, geom.envelopeInternal.maxX)
                    maxY = maxOfNullable(maxY, geom.envelopeInternal.maxY)
                    count++
                }

                insertGeoPackageMetadata(db, tableName, layerName, geometryType, count, minX, minY, maxX, maxY)
                db.setTransactionSuccessful()
                WrittenLayer(tableName, count, geometryType, minX, minY, maxX, maxY)
            } finally {
                db.endTransaction()
            }
        }
    }

    fun getFeaturesInViewport(
        tableName: String,
        minX: Double,
        minY: Double,
        maxX: Double,
        maxY: Double,
        limit: Int,
        offset: Int
    ): List<FeatureRow> {
        ensureCreated()
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val safeTable = quoteIdentifier(tableName)
            val rtreeName = "rtree_${tableName}_geom"
            val hasRTree = tableExists(db, rtreeName)
            val sql = if (hasRTree) {
                val safeRTree = quoteIdentifier(rtreeName)
                """
                    SELECT f.fid, f.geom, f.properties
                    FROM $safeTable f
                    JOIN $safeRTree r ON f.fid = r.id
                    WHERE r.maxx >= ? AND r.minx <= ? AND r.maxy >= ? AND r.miny <= ?
                    LIMIT ? OFFSET ?
                """.trimIndent()
            } else {
                """
                    SELECT f.fid, f.geom, f.properties
                    FROM $safeTable f
                    WHERE f.max_x >= ? AND f.min_x <= ? AND f.max_y >= ? AND f.min_y <= ?
                    LIMIT ? OFFSET ?
                """.trimIndent()
            }
            val args = arrayOf(
                minX.toString(),
                maxX.toString(),
                minY.toString(),
                maxY.toString(),
                limit.coerceAtLeast(1).toString(),
                offset.coerceAtLeast(0).toString()
            )
            val rows = mutableListOf<FeatureRow>()
            db.rawQuery(sql, args).use { cursor ->
                while (cursor.moveToNext()) {
                    val parsed = WkbParser.parse(cursor.getBlob(1)) ?: continue
                    val props = jsonToMap(cursor.getString(2))
                    rows.add(
                        FeatureRow(
                            id = cursor.getLong(0).toString(),
                            properties = props,
                            geometryType = parsed.first,
                            geometryCoordinates = parsed.second
                        )
                    )
                }
            }
            return rows
        }
    }

    fun dropLayer(tableName: String) {
        ensureCreated()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.beginTransaction()
            try {
                db.delete("gpkg_geometry_columns", "table_name = ?", arrayOf(tableName))
                db.delete("gpkg_contents", "table_name = ?", arrayOf(tableName))
                db.execSQL("DROP TABLE IF EXISTS ${quoteIdentifier("rtree_${tableName}_geom")}")
                db.execSQL("DROP TABLE IF EXISTS ${quoteIdentifier(tableName)}")
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun createCoreTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS gpkg_spatial_ref_sys (
                srs_name TEXT NOT NULL,
                srs_id INTEGER NOT NULL PRIMARY KEY,
                organization TEXT NOT NULL,
                organization_coordsys_id INTEGER NOT NULL,
                definition TEXT NOT NULL,
                description TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS gpkg_contents (
                table_name TEXT NOT NULL PRIMARY KEY,
                data_type TEXT NOT NULL,
                identifier TEXT UNIQUE,
                description TEXT DEFAULT '',
                last_change TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                min_x DOUBLE,
                min_y DOUBLE,
                max_x DOUBLE,
                max_y DOUBLE,
                srs_id INTEGER,
                CONSTRAINT fk_gc_r_srs_id FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS gpkg_geometry_columns (
                table_name TEXT NOT NULL,
                column_name TEXT NOT NULL,
                geometry_type_name TEXT NOT NULL,
                srs_id INTEGER NOT NULL,
                z TINYINT NOT NULL,
                m TINYINT NOT NULL,
                PRIMARY KEY (table_name, column_name),
                CONSTRAINT fk_gc_tn FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name),
                CONSTRAINT fk_gc_srs FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id)
            )
        """.trimIndent())
    }

    private fun ensureWgs84(db: SQLiteDatabase) {
        db.insertWithOnConflict(
            "gpkg_spatial_ref_sys",
            null,
            ContentValues().apply {
                put("srs_name", "WGS 84 geodetic")
                put("srs_id", 4326)
                put("organization", "EPSG")
                put("organization_coordsys_id", 4326)
                put("definition", "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]]")
                put("description", "longitude/latitude coordinates in decimal degrees on the WGS 84 datum")
            },
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    private fun createFeatureTable(db: SQLiteDatabase, tableName: String): Boolean {
        val qTable = quoteIdentifier(tableName)
        db.execSQL("""
            CREATE TABLE $qTable (
                fid INTEGER PRIMARY KEY AUTOINCREMENT,
                geom BLOB NOT NULL,
                properties TEXT,
                source_feature_id TEXT,
                min_x REAL,
                min_y REAL,
                max_x REAL,
                max_y REAL
            )
        """.trimIndent())
        return try {
            db.execSQL("CREATE VIRTUAL TABLE ${quoteIdentifier("rtree_${tableName}_geom")} USING rtree(id, minx, maxx, miny, maxy)")
            true
        } catch (e: SQLiteException) {
            false
        }
    }

    private fun insertGeoPackageMetadata(
        db: SQLiteDatabase,
        tableName: String,
        identifier: String,
        geometryType: String?,
        count: Int,
        minX: Double?,
        minY: Double?,
        maxX: Double?,
        maxY: Double?
    ) {
        db.insertWithOnConflict(
            "gpkg_contents",
            null,
            ContentValues().apply {
                put("table_name", tableName)
                put("data_type", "features")
                put("identifier", tableName)
                put("description", "Imported by GeoVision")
                if (minX != null) put("min_x", minX)
                if (minY != null) put("min_y", minY)
                if (maxX != null) put("max_x", maxX)
                if (maxY != null) put("max_y", maxY)
                put("srs_id", 4326)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.insertWithOnConflict(
            "gpkg_geometry_columns",
            null,
            ContentValues().apply {
                put("table_name", tableName)
                put("column_name", "geom")
                put("geometry_type_name", geometryType ?: "GEOMETRY")
                put("srs_id", 4326)
                put("z", 0)
                put("m", 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
        if (count == 0) {
            db.delete("gpkg_contents", "table_name = ?", arrayOf(tableName))
            db.delete("gpkg_geometry_columns", "table_name = ?", arrayOf(tableName))
        }
    }

    private fun insertRTree(db: SQLiteDatabase, tableName: String, fid: Long, geom: Geometry) {
        db.execSQL(
            "INSERT INTO ${quoteIdentifier("rtree_${tableName}_geom")} (id, minx, maxx, miny, maxy) VALUES (?, ?, ?, ?, ?)",
            arrayOf(fid, geom.envelopeInternal.minX, geom.envelopeInternal.maxX, geom.envelopeInternal.minY, geom.envelopeInternal.maxY)
        )
    }

    private fun uniqueTableName(db: SQLiteDatabase, layerName: String): String {
        val base = sanitizeIdentifier(layerName).ifBlank { "layer" }.take(48)
        var candidate = base
        var suffix = 1
        while (tableExists(db, candidate)) {
            candidate = "${base.take(44)}_${suffix++}"
        }
        return candidate
    }

    private fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { c ->
            return c.moveToFirst()
        }
    }

    private fun sanitizeIdentifier(value: String): String {
        return value.substringBeforeLast('.')
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .let { if (it.firstOrNull()?.isDigit() == true) "layer_$it" else it }
    }

    private fun normalizeGeometryType(type: String?): String? {
        return when (type) {
            "Point", "MultiPoint", "LineString", "MultiLineString", "Polygon", "MultiPolygon" -> type.uppercase(Locale.US)
            else -> type?.uppercase(Locale.US)
        }
    }

    private fun toGeoPackageGeometry(wkb: ByteArray, srsId: Int): ByteArray {
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        header.put('G'.code.toByte())
        header.put('P'.code.toByte())
        header.put(0)
        header.put(1)
        header.putInt(srsId)
        return header.array() + wkb
    }

    private fun jsonToMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { key -> obj.optString(key) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun minOfNullable(current: Double?, value: Double): Double = if (current == null) value else minOf(current, value)
    private fun maxOfNullable(current: Double?, value: Double): Double = if (current == null) value else maxOf(current, value)

    companion object {
        fun quoteIdentifier(identifier: String): String = "\"${identifier.replace("\"", "\"\"")}\""
    }
}
