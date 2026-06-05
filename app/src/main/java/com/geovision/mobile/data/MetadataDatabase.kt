package com.geovision.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

class MetadataDatabase private constructor(
    context: Context,
    private val dbPath: String
) : SQLiteOpenHelper(context, dbPath, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS projects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                project_path TEXT NOT NULL,
                gpkg_path TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                app_version TEXT,
                notes TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS source_files (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                original_name TEXT NOT NULL,
                original_uri TEXT,
                local_copy_path TEXT,
                file_type TEXT NOT NULL,
                file_size INTEGER,
                file_hash TEXT,
                imported_at INTEGER,
                status TEXT NOT NULL,
                error_message TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS layers (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                source_file_id TEXT,
                name TEXT NOT NULL,
                original_name TEXT,
                geometry_type TEXT,
                feature_count INTEGER DEFAULT 0,
                gpkg_table_name TEXT,
                visible INTEGER DEFAULT 1,
                min_zoom REAL,
                max_zoom REAL,
                style_json TEXT,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS layer_crs (
                id TEXT PRIMARY KEY,
                layer_id TEXT NOT NULL,
                original_crs_name TEXT,
                original_epsg INTEGER,
                original_wkt TEXT,
                target_epsg INTEGER NOT NULL,
                target_crs_name TEXT,
                transform_status TEXT NOT NULL,
                warning_message TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS import_jobs (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                source_file_id TEXT,
                status TEXT NOT NULL,
                current_step TEXT,
                progress_percent INTEGER DEFAULT 0,
                started_at INTEGER,
                finished_at INTEGER,
                error_message TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS import_report_layers (
                id TEXT PRIMARY KEY,
                import_job_id TEXT NOT NULL,
                layer_name TEXT NOT NULL,
                source_layer_name TEXT,
                geometry_type TEXT,
                feature_count INTEGER,
                original_crs TEXT,
                target_crs TEXT,
                support_status TEXT NOT NULL,
                import_status TEXT NOT NULL,
                warning_message TEXT,
                error_message TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS media_files (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                layer_id TEXT,
                feature_id TEXT,
                original_uri TEXT,
                local_path TEXT NOT NULL,
                media_type TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                altitude REAL,
                captured_at INTEGER,
                metadata_json TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS layer_display_settings (
                id TEXT PRIMARY KEY,
                layer_id TEXT NOT NULL,
                renderer_type TEXT,
                color TEXT,
                stroke_width REAL,
                icon_name TEXT,
                opacity REAL DEFAULT 1.0,
                label_field TEXT,
                clustering_enabled INTEGER DEFAULT 0,
                simplification_enabled INTEGER DEFAULT 1,
                settings_json TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_layers_project ON layers(project_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_source_files_project ON source_files(project_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_import_jobs_project ON import_jobs(project_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun ensureProject(paths: ProjectStorageManager.ProjectPaths) {
        val now = System.currentTimeMillis()
        writableDatabase.insertWithOnConflict(
            "projects",
            null,
            ContentValues().apply {
                put("id", ProjectStorageManager.DEFAULT_PROJECT_ID)
                put("name", ProjectStorageManager.DEFAULT_PROJECT_NAME)
                put("project_path", paths.root.absolutePath)
                put("gpkg_path", paths.projectGpkg.absolutePath)
                put("created_at", now)
                put("updated_at", now)
                put("app_version", "1.0.0")
            },
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    companion object {
        private const val VERSION = 1

        fun open(context: Context, paths: ProjectStorageManager.ProjectPaths): MetadataDatabase {
            return MetadataDatabase(context.applicationContext, paths.metadataDb.absolutePath).also {
                it.ensureProject(paths)
            }
        }

        fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }
}
