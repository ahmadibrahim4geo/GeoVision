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

    enum class ZipArchiveType {
        SHAPEFILE,
        FILE_GDB,
        GEOPACKAGE,
        KML,
        PERSONAL_GDB,
        UNKNOWN
    }

    fun inspectZipType(context: Context, uri: Uri): ZipArchiveType {
        return try {
            val isStream = context.contentResolver.openInputStream(uri) ?: return ZipArchiveType.UNKNOWN
            java.util.zip.ZipInputStream(isStream).use { zis ->
                var entry = zis.nextEntry
                var hasShp = false
                var hasGdb = false
                var hasGpkg = false
                var hasKml = false
                var hasMdb = false
                var count = 0
                while (entry != null && count < 250) {
                    count++
                    val name = entry.name.lowercase()
                    when {
                        name.endsWith(".gdbtable") || name.contains(".gdb/") || name.endsWith(".gdb") -> hasGdb = true
                        name.endsWith(".geodatabase") -> hasGdb = true
                        name.endsWith(".shp") -> hasShp = true
                        name.endsWith(".gpkg") -> hasGpkg = true
                        name.endsWith(".kml") -> hasKml = true
                        name.endsWith(".mdb") -> hasMdb = true
                    }
                    if (hasGdb) return@use ZipArchiveType.FILE_GDB
                    if (hasGpkg) return@use ZipArchiveType.GEOPACKAGE
                    entry = zis.nextEntry
                }
                when {
                    hasGdb -> ZipArchiveType.FILE_GDB
                    hasGpkg -> ZipArchiveType.GEOPACKAGE
                    hasShp -> ZipArchiveType.SHAPEFILE
                    hasKml -> ZipArchiveType.KML
                    hasMdb -> ZipArchiveType.PERSONAL_GDB
                    else -> ZipArchiveType.UNKNOWN
                }
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "inspectZipType error: ${e.message}")
            ZipArchiveType.UNKNOWN
        }
    }

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
                val nameLower = fileName.lowercase()

                when {
                    nameLower.endsWith(".zip") || uriStr.endsWith(".zip") -> {
                        when (inspectZipType(context, uri)) {
                            ZipArchiveType.FILE_GDB -> loadGdb(context, uri, fileName)
                            ZipArchiveType.GEOPACKAGE -> loadGpkgFromZip(context, uri, fileName)
                            ZipArchiveType.KML -> loadKmz(context, uri, fileName)
                            ZipArchiveType.PERSONAL_GDB -> LoadResult(
                                fileName = fileName,
                                error = "ملف Personal Geodatabase (.mdb) يعتمد على محرك Microsoft Access 32-bit غير المدعوم على Android. يُرجى تحويله إلى GeoPackage (.gpkg) أو FileGDB (.gdb) عبر ArcGIS أو QGIS.",
                                errorType = GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT
                            )
                            else -> {
                                val result = ShapefileImporter.importFromUri(uri, context, fileName, companionDbf, companionPrj)
                                result.toLoadResult(fileName)
                            }
                        }
                    }
                    nameLower.endsWith(".shp") || uriStr.endsWith(".shp") -> {
                        val result = ShapefileImporter.importFromUri(uri, context, fileName, companionDbf, companionPrj)
                        result.toLoadResult(fileName)
                    }
                    nameLower.endsWith(".gpkg") || uriStr.endsWith(".gpkg") -> loadGpkg(context, uri, fileName)
                    nameLower.endsWith(".kmz") || uriStr.endsWith(".kmz") -> loadKmz(context, uri, fileName)
                    nameLower.endsWith(".gdb") || uriStr.contains(".gdb") -> loadGdb(context, uri, fileName)
                    nameLower.endsWith(".geodatabase") || uriStr.endsWith(".geodatabase") -> loadMobileGdb(context, uri, fileName)
                    nameLower.endsWith(".mdb") || uriStr.endsWith(".mdb") -> LoadResult(
                        fileName = fileName,
                        error = "قاعدة بيانات Personal Geodatabase (.mdb) غير مدعومة مباشرة على أندرويد لأنها تعتمد على Microsoft Access. يُنصح بتحويلها إلى GeoPackage (.gpkg) أو File Geodatabase (.gdb).",
                        errorType = GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT
                    )
                    nameLower.endsWith(".csv") || nameLower.endsWith(".tsv") || nameLower.endsWith(".txt") || uriStr.endsWith(".csv") ->
                        loadCsv(context, uri, fileName)
                    nameLower.endsWith(".xlsx") || nameLower.endsWith(".xls") || uriStr.endsWith(".xlsx") || uriStr.endsWith(".xls") ->
                        loadExcel(context, uri, fileName)
                    else -> loadGeneric(context, uri, uriStr, fileName)
                }
            }
        }
    }

    private suspend fun loadGpkgFromZip(context: Context, uri: Uri, fileName: String): LoadResult {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return LoadResult(error = "لا يمكن فتح الملف المضغوط", errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED)
        val tempFile = java.io.File(context.cacheDir, "unzipped_${System.nanoTime()}.gpkg")
        return try {
            java.util.zip.ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().endsWith(".gpkg")) {
                        tempFile.outputStream().use { zis.copyTo(it) }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                loadGpkg(context, Uri.fromFile(tempFile), fileName)
            } else {
                LoadResult(error = "لم يتم العثور على ملف GeoPackage داخل الأرشيف", errorType = GeoJsonParser.ParseErrorType.CORRUPTED)
            }
        } catch (e: Exception) {
            LoadResult(error = "فشل استخراج GeoPackage: ${e.message}", errorType = GeoJsonParser.ParseErrorType.CORRUPTED)
        } finally {
            try { tempFile.delete() } catch (_: Exception) {}
        }
    }

    private suspend fun loadCsv(context: Context, uri: Uri, fileName: String): LoadResult {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return LoadResult(error = "لا يمكن فتح ملف CSV", errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED)
        return try {
            val reader = java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)
            val result = CsvParser.streamParse(reader, fileName)
            reader.close()
            result.toLoadResult(fileName)
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    private suspend fun loadExcel(context: Context, uri: Uri, fileName: String): LoadResult {
        val inspection = ExcelParser.inspect(context, uri)
        val defaultMapping = ExcelParser.CoordinateMapping(
            xColumnIndex = if (inspection.suggestedXIndex >= 0) inspection.suggestedXIndex else 0,
            yColumnIndex = if (inspection.suggestedYIndex >= 0) inspection.suggestedYIndex else 1,
            zColumnIndex = inspection.suggestedZIndex,
            labelColumnIndex = inspection.suggestedLabelIndex,
            epsgCode = 4326
        )
        val result = ExcelParser.parseWithMapping(context, uri, fileName, defaultMapping)
        return result.toLoadResult(fileName)
    }

    suspend fun loadExcelWithMapping(
        context: Context,
        uri: Uri,
        fileName: String,
        mapping: ExcelParser.CoordinateMapping
    ): LoadResult {
        return withTimeout(PARSE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val result = ExcelParser.parseWithMapping(context, uri, fileName, mapping)
                result.toLoadResult(fileName)
            }
        }
    }

    suspend fun loadCsvWithMapping(
        context: Context,
        uri: Uri,
        fileName: String,
        mapping: ExcelParser.CoordinateMapping
    ): LoadResult {
        return withTimeout(PARSE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext LoadResult(error = "لا يمكن فتح ملف CSV", errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED)
                try {
                    val reader = java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)
                    val result = CsvParser.parseWithMapping(reader, fileName, mapping)
                    reader.close()
                    result.toLoadResult(fileName)
                } finally {
                    try { stream.close() } catch (_: Exception) {}
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

    suspend fun loadGpkgLayers(
        context: Context,
        uri: Uri,
        fileName: String,
        selectedLayerNames: List<String>
    ): List<LoadResult> {
        return withTimeout(PARSE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val layerDetails = GpkgReader.readEachSelected(context, uri, fileName, selectedLayerNames)
                if (layerDetails.isNotEmpty()) {
                    layerDetails.map { (layerName, detail) ->
                        val layerFileName = "$fileName / $layerName"
                        GeoJsonParser.ParseResult(
                            features = detail.features,
                            fileName = layerFileName,
                            crs = detail.crs,
                            extent = detail.extent
                        ).toLoadResult(layerFileName)
                    }
                } else {
                    selectedLayerNames.map { layerName ->
                        val layerFileName = "$fileName / $layerName"
                        val detail = GpkgReader.readSelected(context, uri, fileName, listOf(layerName))
                        if (detail != null) {
                            GeoJsonParser.ParseResult(
                                features = detail.features,
                                fileName = layerFileName,
                                crs = detail.crs,
                                extent = detail.extent
                            ).toLoadResult(layerFileName)
                        } else {
                            LoadResult(
                                fileName = layerFileName,
                                error = "تعذّر فتح طبقة GeoPackage: $layerName",
                                errorType = GeoJsonParser.ParseErrorType.CORRUPTED,
                                importReport = ImportReport(
                                    sourceName = layerFileName,
                                    sourceType = "GeoPackage",
                                    layers = listOf(
                                        ImportLayerInfo(
                                            id = layerName,
                                            name = layerName,
                                            geometryType = null,
                                            featureCount = null,
                                            crs = null,
                                            status = ImportStatus.FAILED,
                                            message = "تعذّر فتح الطبقة"
                                        )
                                    ),
                                    errors = listOf(ImportError("GPKG_LAYER_OPEN_FAILED", "تعذّر فتح الطبقة", layerName))
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadGpkg(context: Context, uri: Uri, fileName: String): LoadResult {
        val detail = GpkgReader.read(context, uri, fileName)
        return if (detail != null) GeoJsonParser.ParseResult(
            features = detail.features,
            fileName = fileName,
            crs = detail.crs,
            extent = detail.extent
        ).toLoadResult(fileName) else LoadResult(
            error = "تعذّر فتح ملف GeoPackage أو أنه غير صالح",
            errorType = GeoJsonParser.ParseErrorType.CORRUPTED
        )
    }

    private suspend fun loadKmz(context: Context, uri: Uri, fileName: String): LoadResult {
        val zipStream = context.contentResolver.openInputStream(uri)
            ?: return LoadResult(error = "لا يمكن فتح ملف KMZ", errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED)
        return try {
            val entries = mutableListOf<Pair<String, ByteArray>>()
            val kmzAssetDir = java.io.File(context.cacheDir, "kmz_assets_${System.nanoTime()}").also { it.mkdirs() }
            java.util.zip.ZipInputStream(zipStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zis.readBytes()
                        if (entry.name.lowercase().endsWith(".kml")) {
                            entries.add(entry.name to bytes)
                        } else {
                            // Extract icons / images for marker styling
                            try {
                                val outFile = java.io.File(kmzAssetDir, entry.name)
                                if (outFile.canonicalPath.startsWith(kmzAssetDir.canonicalPath)) {
                                    outFile.parentFile?.mkdirs()
                                    outFile.writeBytes(bytes)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            val selected = entries.find { (name, _) -> name.equals("doc.kml", ignoreCase = true) }
                ?: entries.firstOrNull()
            if (selected != null) {
                // محاولة فك ترميز البيانات بـ UTF-8 بشكل صحيح
                val text = try {
                    selected.second.toString(java.nio.charset.StandardCharsets.UTF_8)
                } catch (_: Exception) {
                    // في حالة الفشل، حاول بـ ISO-8859-1
                    selected.second.toString(java.nio.charset.StandardCharsets.ISO_8859_1)
                }
                
                // التأكد من عدم وجود BOM في النص
                val cleanedText = if (text.startsWith('\uFEFF')) {
                    text.substring(1)
                } else {
                    text
                }
                
                val result = KmlParser.streamParse(java.io.StringReader(cleanedText), fileName)
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

    private suspend fun loadMobileGdb(context: Context, uri: Uri, fileName: String): LoadResult {
        val detail = MobileGdbReader.read(context, uri, fileName)
        return if (detail != null) GeoJsonParser.ParseResult(
            features = detail.features,
            fileName = fileName,
            crs = detail.crs,
            extent = detail.extent
        ).toLoadResult(fileName) else LoadResult(
            fileName = fileName,
            error = "تعذّر فتح ملف ArcGIS Mobile Geodatabase أو أنه غير صالح",
            errorType = GeoJsonParser.ParseErrorType.CORRUPTED
        )
    }

    suspend fun loadMobileGdbLayers(
        context: Context,
        uri: Uri,
        fileName: String,
        selectedLayerNames: List<String>
    ): List<LoadResult> {
        return withTimeout(PARSE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val layerDetails = MobileGdbReader.readEachSelected(context, uri, fileName, selectedLayerNames)
                if (layerDetails.isNotEmpty()) {
                    layerDetails.map { (layerName, detail) ->
                        val layerFileName = "$fileName / $layerName"
                        GeoJsonParser.ParseResult(
                            features = detail.features,
                            fileName = layerFileName,
                            crs = detail.crs,
                            extent = detail.extent
                        ).toLoadResult(layerFileName)
                    }
                } else {
                    selectedLayerNames.map { layerName ->
                        val layerFileName = "$fileName / $layerName"
                        val detail = MobileGdbReader.readSelected(context, uri, fileName, listOf(layerName))
                        if (detail != null) {
                            GeoJsonParser.ParseResult(
                                features = detail.features,
                                fileName = layerFileName,
                                crs = detail.crs,
                                extent = detail.extent
                            ).toLoadResult(layerFileName)
                        } else {
                            LoadResult(
                                fileName = layerFileName,
                                error = "تعذّر فتح طبقة Mobile Geodatabase: $layerName",
                                errorType = GeoJsonParser.ParseErrorType.CORRUPTED
                            )
                        }
                    }
                }
            }
        }
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
        val path = uri.toString()
        val mime = if (context != null) context.contentResolver.getType(uri) else null
        val fileName = if (context != null) getFileName(context, uri) else null
        val checkName = (fileName ?: path).lowercase()

        if (context != null && checkName.endsWith(".zip")) {
            return when (inspectZipType(context, uri)) {
                ZipArchiveType.FILE_GDB -> com.geovision.mobile.ui.screens.layers.FileType.GEODATABASE
                ZipArchiveType.GEOPACKAGE -> com.geovision.mobile.ui.screens.layers.FileType.GEOPACKAGE
                ZipArchiveType.KML -> com.geovision.mobile.ui.screens.layers.FileType.KML
                ZipArchiveType.SHAPEFILE -> com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE
                ZipArchiveType.PERSONAL_GDB -> null
                ZipArchiveType.UNKNOWN -> com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE
                else -> com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE
            }
        }
        return inferFileTypeFromPath(fileName ?: path, mime)
    }

    fun inferFileTypeFromPath(path: String, mime: String? = null): com.geovision.mobile.ui.screens.layers.FileType? {
        val p = path.lowercase()
        val ext = p.substringAfterLast('.').take(4)
        when {
            ext == "shp" || ext in setOf("shb", "shx", "dbf", "prj", "cpg") || p.endsWith(".zip") -> return com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE
            p.endsWith(".geojson") || p.endsWith(".geo.json") || p.endsWith(".json") -> return com.geovision.mobile.ui.screens.layers.FileType.GEOJSON
            p.endsWith(".kml") || p.endsWith(".kmz") -> return com.geovision.mobile.ui.screens.layers.FileType.KML
            p.endsWith(".gpx") -> return com.geovision.mobile.ui.screens.layers.FileType.GPX
            p.endsWith(".gdb") || p.contains("%2Egdb") || p.contains(".gdb/") || p.endsWith(".geodatabase") -> return com.geovision.mobile.ui.screens.layers.FileType.GEODATABASE
            p.endsWith(".gpkg") -> return com.geovision.mobile.ui.screens.layers.FileType.GEOPACKAGE
            p.endsWith(".xlsx") || p.endsWith(".xls") -> return com.geovision.mobile.ui.screens.layers.FileType.EXCEL
            p.endsWith(".csv") || p.endsWith(".tsv") || p.endsWith(".txt") -> return com.geovision.mobile.ui.screens.layers.FileType.CSV
            p.endsWith(".jpg") || p.endsWith(".jpeg") || p.endsWith(".png") || p.endsWith(".webp") -> return com.geovision.mobile.ui.screens.layers.FileType.PHOTO
        }
        if (mime != null) {
            val m = mime.lowercase()
            if (m.contains("spreadsheet") || m.contains("excel") || m.contains("openxmlformats-officedocument.spreadsheetml")) return com.geovision.mobile.ui.screens.layers.FileType.EXCEL
            if (m.contains("geo+json") || m.contains("json")) return com.geovision.mobile.ui.screens.layers.FileType.GEOJSON
            if (m.contains("kml")) return com.geovision.mobile.ui.screens.layers.FileType.KML
            if (m.contains("gpx")) return com.geovision.mobile.ui.screens.layers.FileType.GPX
            if (m.contains("geopackage")) return com.geovision.mobile.ui.screens.layers.FileType.GEOPACKAGE
            if (m.contains("shape") || m.contains("shp")) return com.geovision.mobile.ui.screens.layers.FileType.SHAPEFILE
            if (m.contains("csv") || m.contains("comma-separated")) return com.geovision.mobile.ui.screens.layers.FileType.CSV
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
        val warning: String? = null,
        val importReport: ImportReport? = null
    )

    private fun GeoJsonParser.ParseResult.toLoadResult(fileName: String) = LoadResult(
        features = features, crs = crs, extent = extent,
        fileName = fileName.ifEmpty { this.fileName },
        parseTimeMs = parseTimeMs, error = error,
        errorType = errorType,
        warning = CrsValidator.appendWarning(warning, crs),
        importReport = toImportReport(fileName.ifEmpty { this.fileName })
    )

    private fun GeoJsonParser.ParseResult.toImportReport(sourceName: String): ImportReport {
        val sourceType = inferReportSourceType(sourceName)
        val crsInfo = CrsValidator.inspect(crs)
        val warnings = listOfNotNull(CrsValidator.warningFor(crs))
        val errors = listOfNotNull(
            error?.let {
                ImportError(
                    code = errorType?.name ?: "PARSER_ERROR",
                    message = it
                )
            }
        )
        val geometryType = deriveReportGeometryType(features)
        val status = when {
            error != null -> ImportStatus.FAILED
            warnings.isNotEmpty() -> ImportStatus.WARNING
            else -> ImportStatus.IMPORTED
        }
        val layer = ImportLayerInfo(
            id = sourceName.ifBlank { "layer" },
            name = sourceName.ifBlank { fileName.ifBlank { "Layer" } },
            geometryType = geometryType,
            featureCount = features.size.toLong(),
            crs = crsInfo,
            status = status,
            message = warning ?: error
        )
        return ImportReport(
            sourceName = sourceName.ifBlank { fileName },
            sourceType = sourceType,
            layers = listOf(layer),
            warnings = warnings,
            errors = errors
        )
    }

    private fun inferReportSourceType(sourceName: String): String {
        val lower = sourceName.lowercase()
        return when {
            lower.endsWith(".kmz") -> "KMZ"
            lower.endsWith(".kml") -> "KML"
            lower.endsWith(".gpkg") -> "GeoPackage"
            lower.endsWith(".gpx") -> "GPX"
            lower.endsWith(".geojson") || lower.endsWith(".json") -> "GeoJSON"
            lower.endsWith(".shp") || lower.endsWith(".zip") -> "Shapefile"
            lower.endsWith(".gdb") || lower.contains(".gdb/") -> "FileGDB"
            lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt") -> "CSV"
            else -> "GIS"
        }
    }

    private fun deriveReportGeometryType(features: List<FeatureRow>): String? {
        if (features.isEmpty()) return null
        return features.groupingBy { feature ->
            when (feature.geometryType) {
                "Point", "MultiPoint" -> "Point"
                "LineString", "MultiLineString" -> "LineString"
                "Polygon", "MultiPolygon" -> "Polygon"
                else -> feature.geometryType
            }
        }.eachCount().maxByOrNull { it.value }?.key
    }
}
