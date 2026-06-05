package com.geovision.esri.viewer

import android.content.Context
import android.net.Uri
import android.os.StatFs
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject

class EsriViewerStorage(private val context: Context) {
    data class Paths(
        val root: File,
        val packages: File,
        val cache: File,
        val temp: File,
        val reports: File
    )

    fun ensurePaths(): Paths {
        val root = File(context.filesDir, "GeoVision_Project/esri_viewer")
        val packages = File(root, "packages")
        val cache = File(root, "cache")
        val temp = File(root, "temp")
        val reports = File(root, "reports")
        listOf(root, packages, cache, temp, reports).forEach { it.mkdirs() }
        return Paths(root, packages, cache, temp, reports)
    }

    fun copyPackage(uri: Uri, displayName: String, warnLargeBytes: Long = 512L * 1024L * 1024L): File {
        val paths = ensurePaths()
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "package" }
        val target = uniqueFile(paths.packages, safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: error("Cannot open Esri package")
        if (target.length() >= warnLargeBytes) {
            android.util.Log.w("EsriViewerStorage", "Large Esri package copied: ${target.length()} bytes")
        }
        return target
    }

    fun stats(): EsriCacheStats {
        val paths = ensurePaths()
        return EsriCacheStats(
            cacheSizeBytes = paths.cache.sizeRecursive(),
            packagesSizeBytes = paths.packages.sizeRecursive(),
            tempSizeBytes = paths.temp.sizeRecursive(),
            reportsSizeBytes = paths.reports.sizeRecursive(),
            availableDeviceBytes = StatFs(paths.root.absolutePath).availableBytes
        )
    }

    fun clearCache() {
        val paths = ensurePaths()
        paths.cache.deleteChildren()
        paths.temp.deleteChildren()
    }

    fun saveCacheLimit(limitBytes: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_CACHE_LIMIT, limitBytes)
            .apply()
    }

    fun cacheLimitBytes(): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_CACHE_LIMIT, 2L * 1024L * 1024L * 1024L)
    }

    fun addRecentPackage(file: File, type: EsriPackageType) {
        val recent = recentPackages().filterNot { it.path == file.absolutePath }.toMutableList()
        recent.add(
            0,
            EsriRecentPackage(
                name = file.name,
                path = file.absolutePath,
                packageType = type,
                openedAt = System.currentTimeMillis(),
                sizeBytes = file.length()
            )
        )
        val arr = JSONArray()
        recent.take(12).forEach { item ->
            arr.put(
                JSONObject()
                    .put("name", item.name)
                    .put("path", item.path)
                    .put("packageType", item.packageType.name)
                    .put("openedAt", item.openedAt)
                    .put("sizeBytes", item.sizeBytes)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT, arr.toString())
            .apply()
    }

    fun recentPackages(): List<EsriRecentPackage> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_RECENT, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { index ->
                val obj = arr.optJSONObject(index) ?: return@mapNotNull null
                val path = obj.optString("path")
                if (path.isBlank() || !File(path).exists()) return@mapNotNull null
                EsriRecentPackage(
                    name = obj.optString("name", File(path).name),
                    path = path,
                    packageType = runCatching { EsriPackageType.valueOf(obj.optString("packageType")) }.getOrDefault(EsriPackageType.UNSUPPORTED),
                    openedAt = obj.optLong("openedAt"),
                    sizeBytes = obj.optLong("sizeBytes")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun removeRecentPackage(path: String) {
        val remaining = recentPackages().filterNot { it.path == path }
        val arr = JSONArray()
        remaining.forEach { item ->
            arr.put(
                JSONObject()
                    .put("name", item.name)
                    .put("path", item.path)
                    .put("packageType", item.packageType.name)
                    .put("openedAt", item.openedAt)
                    .put("sizeBytes", item.sizeBytes)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT, arr.toString())
            .apply()
    }

    private fun uniqueFile(dir: File, name: String): File {
        var target = File(dir, name)
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var suffix = 1
        while (target.exists()) {
            target = File(dir, if (ext.isBlank()) "${stem}_$suffix" else "${stem}_$suffix.$ext")
            suffix++
        }
        return target
    }

    private fun File.sizeRecursive(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return listFiles()?.sumOf { it.sizeRecursive() } ?: 0L
    }

    private fun File.deleteChildren() {
        listFiles()?.forEach { it.deleteRecursively() }
    }

    companion object {
        private const val PREFS = "esri_viewer_storage"
        private const val KEY_CACHE_LIMIT = "cache_limit_bytes"
        private const val KEY_RECENT = "recent_packages"
    }
}
