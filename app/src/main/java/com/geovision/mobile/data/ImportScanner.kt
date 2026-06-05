package com.geovision.mobile.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/**
 * Lightweight source scanner used before the full Import Engine exists.
 *
 * It reports available layers, counts, CRS assumptions, and partial-support
 * warnings without changing the current parser/import flow.
 */
object ImportScanner {
    suspend fun scan(context: Context, uri: Uri, fileName: String): ImportReport = withContext(Dispatchers.IO) {
        val lower = fileName.lowercase()
        when {
            lower.endsWith(".kml") -> scanKml(context, uri, fileName)
            lower.endsWith(".kmz") -> scanKmz(context, uri, fileName)
            lower.endsWith(".gpkg") -> scanGeoPackage(context, uri, fileName)
            lower.endsWith(".gdb") || uri.toString().lowercase().contains(".gdb") -> scanFileGdb(context, uri, fileName)
            else -> ImportReport(
                sourceName = fileName,
                sourceType = LayerRepository.inferFileTypeFromPath(fileName)?.label ?: "GIS",
                layers = listOf(
                    ImportLayerInfo(
                        id = fileName,
                        name = fileName,
                        geometryType = null,
                        featureCount = null,
                        crs = null,
                        status = ImportStatus.SUPPORTED,
                        message = "تفاصيل الفحص غير متاحة لهذا النوع حالياً."
                    )
                )
            )
        }
    }

    private fun scanKml(context: Context, uri: Uri, fileName: String): ImportReport {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8).removePrefix("\uFEFF")
        }.orEmpty()
        return scanKmlText(fileName, "KML", text)
    }

    private fun scanKmz(context: Context, uri: Uri, fileName: String): ImportReport {
        val entries = mutableListOf<Pair<String, ByteArray>>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().endsWith(".kml")) {
                        entries.add(entry.name to zip.readBytes())
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val selected = entries.find { it.first.equals("doc.kml", ignoreCase = true) } ?: entries.firstOrNull()
        val text = selected?.second?.toString(Charsets.UTF_8)?.removePrefix("\uFEFF").orEmpty()
        val report = scanKmlText(fileName, "KMZ", text)
        val packageWarning = if (entries.size > 1) {
            ImportWarning("KMZ_MULTIPLE_KML", "ملف KMZ يحتوي على ${entries.size} ملفات KML؛ سيتم استيراد ${selected?.first ?: "أول ملف"}.")
        } else null
        return report.copy(warnings = report.warnings + listOfNotNull(packageWarning))
    }

    private fun scanKmlText(fileName: String, sourceType: String, text: String): ImportReport {
        if (text.isBlank()) {
            return ImportReport(
                sourceName = fileName,
                sourceType = sourceType,
                layers = emptyList(),
                errors = listOf(ImportError("KML_EMPTY", "لم يتم العثور على محتوى KML قابل للقراءة."))
            )
        }

        val placemarkCount = Regex("<\\s*Placemark\\b", RegexOption.IGNORE_CASE).findAll(text).count()
        val styleCount = Regex("<\\s*Style\\b", RegexOption.IGNORE_CASE).findAll(text).count()
        val hasHtmlDescription = Regex("<\\s*description\\b.*?(<html|&lt;html|<table|&lt;table)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).containsMatchIn(text)
        val crs = LayerCrsInfo(
            authority = "EPSG",
            code = "4326",
            name = "EPSG:4326 (KML default)",
            isKnown = true
        )
        val warnings = buildList {
            if (hasHtmlDescription) add(ImportWarning("KML_HTML_DESCRIPTION", "الوصف يحتوي على HTML/XSLT وسيتم تنظيفه أثناء الاستيراد."))
            if (placemarkCount > GeoJsonParser.MAX_FEATURES) add(ImportWarning("KML_FEATURE_LIMIT", "سيتم استيراد أول ${GeoJsonParser.MAX_FEATURES} عنصر فقط في مسار الذاكرة الحالي."))
        }

        return ImportReport(
            sourceName = fileName,
            sourceType = sourceType,
            layers = listOf(
                ImportLayerInfo(
                    id = fileName,
                    name = fileName,
                    geometryType = "Mixed KML",
                    featureCount = placemarkCount.toLong(),
                    crs = crs,
                    status = if (warnings.isEmpty()) ImportStatus.SUPPORTED else ImportStatus.WARNING,
                    message = if (styleCount > 0) "تم اكتشاف $styleCount نمط" else null
                )
            ),
            warnings = warnings
        )
    }

    private suspend fun scanGeoPackage(context: Context, uri: Uri, fileName: String): ImportReport {
        val names = GpkgReader.listLayerNames(context, uri)
        val layers = names.map { name ->
            ImportLayerInfo(
                id = name,
                name = name,
                geometryType = null,
                featureCount = null,
                crs = null,
                status = ImportStatus.SUPPORTED,
                message = "طبقة GeoPackage؛ ستتم قراءة التفاصيل عند الاستيراد."
            )
        }
        return ImportReport(
            sourceName = fileName,
            sourceType = "GeoPackage",
            layers = layers,
            warnings = if (layers.isEmpty()) listOf(ImportWarning("GPKG_NO_LAYERS", "لم يتم العثور على طبقات مكانية.")) else emptyList()
        )
    }

    private suspend fun scanFileGdb(context: Context, uri: Uri, fileName: String): ImportReport {
        val tables = GdbParser.listTableNames(context, uri, fileName)
        return ImportReport(
            sourceName = fileName,
            sourceType = "FileGDB",
            layers = tables.map { table ->
                ImportLayerInfo(
                    id = table.fileName,
                    name = table.tableName,
                    geometryType = table.geometryType,
                    featureCount = table.estimatedCount.toLong(),
                    crs = LayerCrsInfo(null, null, null, isKnown = false, requiresUserSelection = true),
                    status = ImportStatus.WARNING,
                    message = "دعم FileGDB جزئي؛ نظام الإحداثيات يحتاج مراجعة."
                )
            },
            warnings = listOf(
                ImportWarning("FILEGDB_PARTIAL_SUPPORT", "قارئ FileGDB الحالي تجريبي ولا يغطي كل بيانات Esri."),
                ImportWarning("CRS_REQUIRES_REVIEW", "يجب مراجعة نظام إحداثيات FileGDB قبل الاعتماد على موقع الطبقة.")
            )
        )
    }
}
