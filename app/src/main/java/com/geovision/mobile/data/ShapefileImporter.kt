package com.geovision.mobile.data

import android.content.Context
import android.net.Uri
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 *  ShapefileImporter — مستورد كامل لملفات Shapefile.
 *
 *  المزايا:
 *  1. قراءة جميع الملفات المرافقة: .shp, .shx, .dbf, .prj
 *  2. دعم ZIP: فك ضغط تلقائي واستيراد كل الطبقات
 *  3. نسخ SAF إلى مجلد مؤقت لضمان الوصول للملفات المرافقة
 *  4. اكتشاف الترميز العربي في DBF (CP1256)
 *  5. اكتشاف النظام الإحداثي من .prj وتحويل الإحداثيات إلى WGS84
 *  6. تحويل UTM و Web Mercator تلقائياً
 */
object ShapefileImporter {
    data class ShapefileSet(
        val shpFile: File,
        val dbfFile: File? = null,
        val shxFile: File? = null,
        val prjFile: File? = null,
        val sourceName: String = ""
    )

    /** استيراد من URI (يدعم .shp و .zip) */
    suspend fun importFromUri(
        uri: Uri,
        context: Context,
        fileName: String,
        companionDbf: Uri? = null,
        companionPrj: Uri? = null
    ): GeoJsonParser.ParseResult {
        val uriStr = uri.toString().lowercase()

        if (uriStr.endsWith(".zip")) return importZip(uri, context)

        if (uriStr.endsWith(".shp")) {
            val sets = prepareShapefileSet(uri, context, fileName, companionDbf, companionPrj)
            if (sets.isEmpty()) return GeoJsonParser.ParseResult(
                error = "لم يتم العثور على ملف .shp صالح", errorType = GeoJsonParser.ParseErrorType.CORRUPTED)
            return parseShapefileSet(sets.first(), context)
        }

        return GeoJsonParser.ParseResult(
            error = "امتداد غير مدعوم للمستورد: $fileName", errorType = GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT)
    }

    /** استيراد من ZIP: فك ضغط إلى مجلد مؤقت، إيجاد .shp ومعالجتها */
    private suspend fun importZip(uri: Uri, context: Context): GeoJsonParser.ParseResult {
        val extractDir = File(context.cacheDir, "shp_zip_${System.currentTimeMillis()}")
        extractDir.mkdirs()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                extractZip(stream, extractDir)
            } ?: return GeoJsonParser.ParseResult(
                error = "لا يمكن فتح ملف ZIP", errorType = GeoJsonParser.ParseErrorType.ACCESS_DENIED)

            val shpFiles = extractDir.walkTopDown().filter { it.isFile && it.name.lowercase().endsWith(".shp") }.toList()
            if (shpFiles.isEmpty()) return GeoJsonParser.ParseResult(
                error = "لم يُعثر على ملفات Shapefile داخل ZIP", errorType = GeoJsonParser.ParseErrorType.CORRUPTED)

            // دمج جميع الطبقات في نتيجة واحدة (أو إرجاع الأولى)
            var combined: GeoJsonParser.ParseResult? = null
            for (shp in shpFiles) {
                val set = ShapefileSet(
                    shpFile = shp,
                    dbfFile = companionFile(shp, ".dbf"),
                    shxFile = companionFile(shp, ".shx"),
                    prjFile = companionFile(shp, ".prj"),
                    sourceName = shp.nameWithoutExtension
                )
                val r = parseShapefileSet(set, context)
                if (r.error == null) {
                    if (combined == null) {
                        combined = r
                    } else {
                        combined = combined.copy(features = combined.features + r.features)
                    }
                }
            }
            return combined ?: GeoJsonParser.ParseResult(
                error = "فشل تحليل جميع ملفات Shapefile داخل ZIP", errorType = GeoJsonParser.ParseErrorType.CORRUPTED)
        } finally {
            extractDir.deleteRecursively()
        }
    }

    /** فك ضغط ZIP إلى مجلد */
    private fun extractZip(stream: InputStream, targetDir: File) {
        val zis = ZipInputStream(stream)
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val outFile = File(targetDir, entry.name)
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { zis.copyTo(it) }
            }
            entry = zis.nextEntry
        }
        zis.close()
    }

    /** تحضير ShapefileSet من URI — نسخ SAF إلى مجلد مؤقت لضمان الوصول للملفات المرافقة */
    private suspend fun prepareShapefileSet(
        uri: Uri, context: Context, fileName: String,
        companionDbf: Uri? = null, companionPrj: Uri? = null
    ): List<ShapefileSet> {
        val uriStr = uri.toString()
        val baseName = fileName.lowercase().removeSuffix(".shp")

        if (uriStr.startsWith("file://")) {
            val file = File(java.net.URI(uriStr).path)
            return listOf(ShapefileSet(
                shpFile = file,
                dbfFile = companionFile(file, ".dbf"),
                shxFile = companionFile(file, ".shx"),
                prjFile = companionFile(file, ".prj"),
                sourceName = file.nameWithoutExtension
            ))
        }

        // content:// SAF — نسخ إلى مجلد مؤقت مع جميع الملفات التي يمكن الوصول إليها
        val tempDir = File(context.cacheDir, "shp_saf_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val shpTarget = File(tempDir, "$baseName.shp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                shpTarget.outputStream().use { input.copyTo(it) }
            } ?: return emptyList()

            // نسخ DBF المرافق إذا كان URI متاحاً (من التحديد المجمع)
            val companionUris = mapOf(
                ".dbf" to companionDbf,
                ".prj" to companionPrj,
                ".shx" to null
            )
            for ((ext, compUri) in companionUris) {
                try {
                    val compStream = if (compUri != null) {
                        context.contentResolver.openInputStream(compUri)
                    } else {
                        // محاولة الوصول عبر تغيير الامتداد في URI
                        val guessed = Uri.parse("${uriStr.substringBeforeLast('/')}/${baseName}$ext")
                        try { context.contentResolver.openInputStream(guessed) } catch (_: Exception) { null }
                    }
                    compStream?.use { input ->
                        File(tempDir, "$baseName$ext").outputStream().use { input.copyTo(it) }
                    }
                } catch (_: Exception) { /* الملف المرافق غير متاح */ }
            }

            return listOf(ShapefileSet(
                shpFile = shpTarget,
                dbfFile = File(tempDir, "$baseName.dbf").takeIf { it.exists() },
                shxFile = File(tempDir, "$baseName.shx").takeIf { it.exists() },
                prjFile = File(tempDir, "$baseName.prj").takeIf { it.exists() },
                sourceName = baseName.removeSuffix(".")
            ))
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            return emptyList()
        }
    }

    /** تحليل مجموعة Shapefile كاملة مع تحويل CRS */
    private suspend fun parseShapefileSet(set: ShapefileSet, context: Context): GeoJsonParser.ParseResult {
        val startTime = System.nanoTime()
        val baseName = set.sourceName.ifEmpty { set.shpFile.nameWithoutExtension }

        return try {
            var crsInfo = CrsTransform.CrsInfo()
            if (set.prjFile?.exists() == true) {
                val wkt = set.prjFile.readText().trim()
                if (wkt.isNotEmpty()) {
                    crsInfo = CrsTransform.parseWkt(wkt)
                    AppLogger.d(AppLogger.Tags.PARSER, "CRS detected: ${crsInfo.name} EPSG:${crsInfo.epsg} isUtm=${crsInfo.isUtm}")
                }
            }

            val dbfRecords = readDbfWithEncoding(set.dbfFile)
            val shpStream = FileInputStream(set.shpFile)
            try {
                val parseResult = ShapefileParser.streamParse(shpStream, set.shpFile.absolutePath, null, context, dbfRecords)
                if (parseResult.error == null && !crsInfo.isWgs84) {
                    val transformed = transformFeatures(parseResult.features, crsInfo)
                    val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
                    val crsStr = if (crsInfo.epsg != null) "EPSG:${crsInfo.epsg} (${crsInfo.name})" else crsInfo.name
                    GeoJsonParser.ParseResult(
                        features = transformed,
                        fileName = baseName,
                        crs = if (crsInfo.isWgs84) "EPSG:4326 (WGS84)" else "محوّل من $crsStr",
                        extent = computeExtent(transformed),
                        parseTimeMs = elapsedMs
                    )
                } else {
                    val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
                    parseResult.copy(parseTimeMs = elapsedMs)
                }
            } finally {
                try { shpStream.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            GeoJsonParser.ParseResult(
                error = "فشل تحليل Shapefile $baseName: ${e.message}",
                errorType = GeoJsonParser.ParseErrorType.PARSER_ERROR)
        }
    }

    /** تحويل إحداثيات جميع المعالم من النظام الإحداثي المكتشف إلى WGS84 */
    private fun transformFeatures(features: List<FeatureRow>, crsInfo: CrsTransform.CrsInfo): List<FeatureRow> {
        if (crsInfo.isWgs84 || crsInfo.epsg == null) return features
        return features.map { feature ->
            val newCoords = transformCoordinates(feature.geometryCoordinates, crsInfo)
            feature.copy(geometryCoordinates = newCoords)
        }
    }

    private fun computeExtent(features: List<FeatureRow>): String {
        if (features.isEmpty()) return "\u2014"
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        for (feature in features) {
            for ((lon, lat) in GeoJsonParser.extractCoordinates(feature.geometryCoordinates)) {
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon
                if (lon > maxLon) maxLon = lon
            }
        }

        return if (minLat == Double.MAX_VALUE) "\u2014"
        else java.lang.String.format(java.util.Locale.US, "%.4f", minLat) + "\u00B0 \u2014 " +
            java.lang.String.format(java.util.Locale.US, "%.4f", maxLat) + "\u00B0 | " +
            java.lang.String.format(java.util.Locale.US, "%.4f", minLon) + "\u00B0 \u2014 " +
            java.lang.String.format(java.util.Locale.US, "%.4f", maxLon) + "\u00B0"
    }

    /** تحويل سلسلة JSON للإحداثيات — يعالج Point, LineString, Polygon, MultiPoint */
    private fun transformCoordinates(coordsJson: String?, crsInfo: CrsTransform.CrsInfo): String? {
        if (coordsJson.isNullOrBlank()) return coordsJson
        return try {
            val arr = org.json.JSONArray(coordsJson)
            val transformed = transformJsonArray(arr, crsInfo)
            transformed.toString()
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "فشل تحويل الإحداثيات: ${e.message}")
            coordsJson
        }
    }

    /** تحويل مصفوفة JSON بشكل متكرر — عند الوصول لزوج [x,y] يُحوّل */
    private fun transformJsonArray(arr: org.json.JSONArray, crsInfo: CrsTransform.CrsInfo): org.json.JSONArray {
        if (arr.length() >= 2) {
            // التحقق هل هو زوج إحداثيات [x, y]؟
            try {
                val v0 = arr.optDouble(0)
                val v1 = arr.optDouble(1)
                if (!v0.isNaN() && !v1.isNaN() && arr.length() <= 3) {
                    val (lon, lat) = CrsTransform.toWgs84(v0, v1, crsInfo)
                    return org.json.JSONArray().apply { put(lon); put(lat) }
                }
            } catch (_: Exception) {}
        }
        val result = org.json.JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            if (item is org.json.JSONArray) {
                result.put(transformJsonArray(item, crsInfo))
            } else {
                result.put(item)
            }
        }
        return result
    }

    /** البحث عن ملف مرافق بنفس الاسم والامتداد */
    private fun companionFile(shp: File, ext: String): File? {
        val f = File(shp.parent, shp.nameWithoutExtension + ext)
        return f.takeIf { it.exists() }
    }

    /** قراءة DBF مع دعم الترميز العربي */
    private fun readDbfWithEncoding(dbfFile: File?): List<Map<String, String>>? {
        if (dbfFile?.exists() != true) return null
        return try {
            val bytes = dbfFile.readBytes()
            val utf8Result = ShapefileParser.parseDbfBytes(bytes, "UTF-8")
            if (utf8Result.isNotEmpty()) utf8Result
            else {
                val arabicResult = ShapefileParser.parseDbfBytes(bytes, "CP1256")
                if (arabicResult.isNotEmpty()) {
                    AppLogger.d(AppLogger.Tags.PARSER, "DBF successfully read with CP1256 (Arabic)")
                    arabicResult
                } else {
                    ShapefileParser.parseDbfBytes(bytes, "ISO-8859-1")
                }
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "فشل قراءة DBF: ${e.message}")
            null
        }
    }
}
