package com.geovision.mobile.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import com.geovision.mobile.ui.screens.layers.LayerDetailInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.Reader

object LayerRepository {
    private const val PARSE_TIMEOUT_MS = 60_000L

    suspend fun loadFromUri(
        context: Context,
        uri: Uri,
        fileName: String,
        companionDbf: Uri? = null,
        companionPrj: Uri? = null
    ): LoadResult {
        return withTimeout(PARSE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                ensureActive()
                val uriStr = uri.toString().lowercase()

                when {
                    uriStr.endsWith(".shp") || uriStr.endsWith(".zip") -> {
                        val result = ShapefileImporter.importFromUri(uri, context, fileName, companionDbf, companionPrj)
                        result.toLoadResult(fileName)
                    }
                    uriStr.endsWith(".gpkg") -> loadGpkg(context, uri, fileName)
                    uriStr.endsWith(".kmz") -> loadKmz(context, uri, fileName)
                    uriStr.contains(".gdb") || fileName.lowercase().endsWith(".gdb") -> loadGdb(context, uri, fileName)
                    else -> loadGeneric(context, uri, uriStr, fileName)
                }
            }
        }
    }

    suspend fun loadGdbTables(
        context: Context,
        uri: Uri,
        fileName: String,
        selectedTableNames: List<String>
    ): List<LoadResult> {
        return withTimeout(PARSE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val results = GdbParser.readEachSelected(context, uri, fileName, selectedTableNames)
                results.map { (tableName, result) ->
                    result.toLoadResult("$fileName / ${tableName.removeSuffix(".gdbtable")}")
                }
            }
        }
    }

    private suspend fun loadGpkg(context: Context, uri: Uri, fileName: String): LoadResult {
        val detail = GpkgReader.read(context, uri, fileName)
        return if (detail != null) LoadResult(
            features = detail.features, crs = detail.crs,
            extent = detail.extent, fileName = fileName
        ) else LoadResult(
            error = "تعذّر فتح ملف GeoPackage أو أنه غير صالح",
            errorType = GeoJsonParser.ParseErrorType.CORRUPTED
        )
    }

    private suspend fun loadKmz(context: Context, uri: Uri, fileName: String): LoadResult {
        val zipStream = context.contentResolver.openInputStream(uri)
            ?: return LoadResult(error = "لا يمكن فتح ملف KMZ", errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED)
        return try {
            val entries = mutableListOf<Pair<String, ByteArray>>()
            java.util.zip.ZipInputStream(zipStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().endsWith(".kml")) {
                        entries.add(entry.name to zis.readBytes())
                    }
                    entry = zis.nextEntry
                }
            }
            val selected = entries.find { (name, _) -> name.equals("doc.kml", ignoreCase = true) }
                ?: entries.firstOrNull()
            if (selected != null) {
                val text = selected.second.toString(java.nio.charset.StandardCharsets.UTF_8)
                val result = KmlParser.streamParse(java.io.StringReader(text), fileName)
                result.toLoadResult(fileName)
            } else LoadResult(error = "لم يتم العثور على ملف KML صالح داخل KMZ", errorType = GeoJsonParser.ParseErrorType.CORRUPTED)
        } catch (e: java.util.zip.ZipException) {
            LoadResult(error = "ملف KMZ تالف: ${e.message}", errorType = GeoJsonParser.ParseErrorType.CORRUPTED)
        }
    }

    private suspend fun loadGdb(context: Context, uri: Uri, fileName: String): LoadResult {
        val result = GdbParser.read(context, uri, fileName)
        return result.toLoadResult(fileName)
    }

    private suspend fun loadGeneric(context: Context, uri: Uri, uriStr: String, fileName: String): LoadResult {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return LoadResult(error = "لا يمكن فتح الملف", errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED)
        return try {
            val reader = java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)
            val mime = context.contentResolver.getType(uri)
            val result = when {
                (mime != null && mime.contains("json")) || uriStr.endsWith(".geojson") || uriStr.endsWith(".geo.json") || uriStr.endsWith(".json") ->
                    GeoJsonParser.streamParse(reader, fileName)
                (mime != null && mime.contains("kml")) || uriStr.endsWith(".kml") ->
                    KmlParser.streamParse(reader, fileName)
                (mime != null && (mime.contains("gpx") || mime.contains("xml"))) || uriStr.endsWith(".gpx") ->
                    GpxParser.streamParse(reader, fileName)
                uriStr.endsWith(".mbtiles") || uriStr.endsWith(".tif") || uriStr.endsWith(".tiff") ->
                    GeoJsonParser.ParseResult(error = "صيغة الملف غير مدعومة", errorType = GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT)
                else ->
                    GeoJsonParser.ParseResult(error = "نوع الملف غير مدعوم", errorType = GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT)
            }
            reader.close()
            result.toLoadResult(fileName)
        } finally { try { stream.close() } catch (_: Exception) {} }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    fun inferFileType(uri: Uri, context: Context? = null): com.geovision.mobile.ui.screens.layers.FileType? {
        return inferFileTypeFromPath(uri.toString(), if (context != null) { context.contentResolver.getType(uri) } else null)
    }

    fun inferFileTypeFromPath(path: String, mime: String? = null): com.geovision.mobile.ui.screens.layers.FileType? {
        val p = path.lowercase()
        val ext = p.substringAfterLast('.').take(4)
        when {
            ext == "shp" || ext in setOf("shb", "shx", "dbf", "prj", "cpg") || p.endsWith(".zip") -> return com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE
            p.endsWith(".geojson") || p.endsWith(".geo.json") || p.endsWith(".json") -> return com.geovision.mobile.ui.screens.layers.FileType.GEOJSON
            p.endsWith(".kml") || p.endsWith(".kmz") -> return com.geovision.mobile.ui.screens.layers.FileType.KML
            p.endsWith(".gpx") -> return com.geovision.mobile.ui.screens.layers.FileType.GPX
            p.endsWith(".gdb") || p.contains("%2Egdb") || p.contains(".gdb/") -> return com.geovision.mobile.ui.screens.layers.FileType.GEODATABASE
            p.endsWith(".gpkg") -> return com.geovision.mobile.ui.screens.layers.FileType.GEOPACKAGE
            p.endsWith(".jpg") || p.endsWith(".jpeg") || p.endsWith(".png") || p.endsWith(".webp") -> return com.geovision.mobile.ui.screens.layers.FileType.PHOTO
        }
        if (mime != null) {
            val m = mime.lowercase()
            if (m.contains("geo+json") || m.contains("json")) return com.geovision.mobile.ui.screens.layers.FileType.GEOJSON
            if (m.contains("kml")) return com.geovision.mobile.ui.screens.layers.FileType.KML
            if (m.contains("gpx")) return com.geovision.mobile.ui.screens.layers.FileType.GPX
            if (m.contains("geopackage")) return com.geovision.mobile.ui.screens.layers.FileType.GEOPACKAGE
            if (m.contains("shape") || m.contains("shp")) return com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE
            if (m.contains("jpeg") || m.contains("png") || m.contains("webp")) return com.geovision.mobile.ui.screens.layers.FileType.PHOTO
        }
        return null
    }

    fun findCompanionUri(shpUri: Uri, ext: String, context: Context): Uri? {
        return if (ext == ".dbf") findDbfUri(shpUri, context)
        else if (ext == ".prj") findPrjUri(shpUri, context)
        else null
    }

    private fun findDbfUri(shpUri: Uri, context: Context): Uri? {
        try {
            val uriStr = shpUri.toString()
            if (uriStr.startsWith("content://")) {
                val fileName = getFileName(context, shpUri)?.lowercase() ?: return null
                if (!fileName.endsWith(".shp")) return null
                val dbfName = fileName.removeSuffix(".shp") + ".dbf"
                val lastSlash = uriStr.lastIndexOf('/')
                if (lastSlash > 0) return Uri.parse("${uriStr.substring(0, lastSlash + 1)}$dbfName")
            } else if (uriStr.startsWith("file://")) {
                val filePath = java.net.URI(uriStr).path
                val dbfFile = java.io.File(filePath.removeSuffix(".shp").removeSuffix(".SHP") + ".dbf")
                if (dbfFile.exists()) return Uri.fromFile(dbfFile)
            }
        } catch (e: Exception) { AppLogger.w(AppLogger.Tags.PARSER, "فشل البحث عن DBF: ${e.message}") }
        return null
    }

    private fun findPrjUri(shpUri: Uri, context: Context): Uri? {
        try {
            val uriStr = shpUri.toString()
            if (uriStr.startsWith("content://")) {
                val fileName = getFileName(context, shpUri)?.lowercase() ?: return null
                if (!fileName.endsWith(".shp")) return null
                val prjName = fileName.removeSuffix(".shp") + ".prj"
                val lastSlash = uriStr.lastIndexOf('/')
                if (lastSlash > 0) return Uri.parse("${uriStr.substring(0, lastSlash + 1)}$prjName")
            } else if (uriStr.startsWith("file://")) {
                val filePath = java.net.URI(uriStr).path
                val prjFile = java.io.File(filePath.removeSuffix(".shp").removeSuffix(".SHP") + ".prj")
                if (prjFile.exists()) return Uri.fromFile(prjFile)
            }
        } catch (e: Exception) { AppLogger.w(AppLogger.Tags.PARSER, "فشل البحث عن PRJ: ${e.message}") }
        return null
    }

    fun resolveShpFromAuxiliary(auxUri: Uri, context: Context): Uri? {
        try {
            val uriStr = auxUri.toString()
            if (uriStr.startsWith("content://")) {
                val name = getFileName(context, auxUri)?.lowercase() ?: return null
                val base = name.removeSuffix(".shp").removeSuffix(".shx").removeSuffix(".shb")
                    .removeSuffix(".dbf").removeSuffix(".prj").removeSuffix(".cpg")
                if (base.length >= name.length) return null
                val shpName = "$base.shp"
                val lastSlash = uriStr.lastIndexOf('/')
                if (lastSlash > 0) {
                    val shpUri = Uri.parse("${uriStr.substring(0, lastSlash + 1)}$shpName")
                    context.contentResolver.openInputStream(shpUri)?.close()
                    return shpUri
                }
            } else if (uriStr.startsWith("file://")) {
                val filePath = java.net.URI(uriStr).path
                val shpPath = filePath.removeSuffix(".shx").removeSuffix(".shb")
                    .removeSuffix(".dbf").removeSuffix(".prj").removeSuffix(".cpg") + ".shp"
                if (java.io.File(shpPath).exists()) return Uri.fromFile(java.io.File(shpPath))
            }
        } catch (e: Exception) { AppLogger.w(AppLogger.Tags.PARSER, "فشل اكتشاف .shp: ${e.message}") }
        return null
    }

    data class LoadResult(
        val features: List<FeatureRow> = emptyList(),
        val crs: String? = null,
        val extent: String? = null,
        val fileName: String = "",
        val parseTimeMs: Long = 0L,
        val error: String? = null,
        val errorType: GeoJsonParser.ParseErrorType? = null,
        val warning: String? = null
    )

    private fun GeoJsonParser.ParseResult.toLoadResult(fileName: String) = LoadResult(
        features = features, crs = crs, extent = extent,
        fileName = fileName.ifEmpty { this.fileName },
        parseTimeMs = parseTimeMs, error = error,
        errorType = errorType, warning = warning
    )
}
