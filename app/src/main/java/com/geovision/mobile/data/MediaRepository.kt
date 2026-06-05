package com.geovision.mobile.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.geovision.mobile.data.MetadataDatabase.Companion.newId
import java.io.File
import java.io.FileOutputStream

class MediaRepository(context: Context) {
    private val appContext = context.applicationContext

    fun copyPhotoToProject(originalUri: Uri, fileName: String): File {
        val paths = ProjectStorageManager.ensureDefaultProject(appContext)
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "photo.jpg" }
        var target = File(paths.photosDir, safeName)
        var suffix = 1
        while (target.exists()) {
            val stem = safeName.substringBeforeLast('.', safeName)
            val ext = safeName.substringAfterLast('.', "")
            target = File(paths.photosDir, if (ext.isBlank()) "${stem}_$suffix" else "${stem}_$suffix.$ext")
            suffix++
        }
        appContext.contentResolver.openInputStream(originalUri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: error("Cannot open photo")
        return target
    }

    fun savePhoto(
        originalUri: Uri,
        localPath: String,
        layerId: String?,
        featureId: String?,
        latitude: Double?,
        longitude: Double?,
        altitude: Double?,
        capturedAt: Long?,
        metadataJson: String?
    ): String {
        val paths = ProjectStorageManager.ensureDefaultProject(appContext)
        val metadata = MetadataDatabase.open(appContext, paths)
        val id = newId("media")
        metadata.writableDatabase.use { db ->
            db.insertOrThrow("media_files", null, ContentValues().apply {
                put("id", id)
                put("project_id", ProjectStorageManager.DEFAULT_PROJECT_ID)
                put("layer_id", layerId)
                put("feature_id", featureId)
                put("original_uri", originalUri.toString())
                put("local_path", localPath)
                put("media_type", "photo")
                if (latitude != null) put("latitude", latitude)
                if (longitude != null) put("longitude", longitude)
                if (altitude != null) put("altitude", altitude)
                if (capturedAt != null) put("captured_at", capturedAt)
                put("metadata_json", metadataJson)
            })
        }
        return id
    }
}
