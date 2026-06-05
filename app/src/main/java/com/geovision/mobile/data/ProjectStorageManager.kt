package com.geovision.mobile.data

import android.content.Context
import java.io.File

object ProjectStorageManager {
    const val DEFAULT_PROJECT_ID = "default"
    const val DEFAULT_PROJECT_NAME = "GeoVision Project"

    data class ProjectPaths(
        val root: File,
        val dataDir: File,
        val mediaDir: File,
        val photosDir: File,
        val tilesDir: File,
        val mbTilesDir: File,
        val vectorTilesDir: File,
        val importReportsDir: File,
        val originalSourcesDir: File,
        val projectGpkg: File,
        val metadataDb: File
    )

    fun ensureDefaultProject(context: Context): ProjectPaths {
        val root = File(context.filesDir, "GeoVision_Project")
        val data = File(root, "data")
        val media = File(root, "media")
        val photos = File(media, "photos")
        val tiles = File(root, "tiles")
        val mbTiles = File(tiles, "mbtiles")
        val vectorTiles = File(tiles, "vector")
        val importReports = File(root, "import_reports")
        val originalSources = File(root, "original_sources")

        listOf(root, data, media, photos, tiles, mbTiles, vectorTiles, importReports, originalSources)
            .forEach { it.mkdirs() }

        return ProjectPaths(
            root = root,
            dataDir = data,
            mediaDir = media,
            photosDir = photos,
            tilesDir = tiles,
            mbTilesDir = mbTiles,
            vectorTilesDir = vectorTiles,
            importReportsDir = importReports,
            originalSourcesDir = originalSources,
            projectGpkg = File(data, "project.gpkg"),
            metadataDb = File(data, "metadata.sqlite")
        )
    }
}
