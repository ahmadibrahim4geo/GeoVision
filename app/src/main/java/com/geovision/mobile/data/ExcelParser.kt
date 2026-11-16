package com.geovision.mobile.data

import android.content.Context
import android.net.Uri
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * محلل جداول Excel (.xlsx) خفيف وسريع ومحسن لنظام أندرويد.
 * يقرأ جداول OpenXML Streaming دون الحاجة لمكتبات خارجية ثقيلة،
 * ويستخرج نقاط الإحداثيات مع ربط كافة الأعمدة والسمات بالمعالم.
 */
object ExcelParser {
    private const val TAG = "ExcelParser"

    data class TableInspection(
        val headers: List<String>,
        val previewRows: List<Map<String, String>>,
        val totalEstimatedRows: Int,
        val suggestedXIndex: Int,
        val suggestedYIndex: Int,
        val suggestedZIndex: Int,
        val suggestedLabelIndex: Int
    )

    data class CoordinateMapping(
        val xColumnIndex: Int,
        val yColumnIndex: Int,
        val zColumnIndex: Int = -1,
        val labelColumnIndex: Int = -1,
        val epsgCode: Int = 4326,
        val utmZone: Int = 0,
        val isUtmNorth: Boolean = true
    )

    private val X_CANDIDATES = listOf(
        "x", "lon", "long", "lng", "longitude", "easting", "east", "xcoord", "x_coord",
        "شرق", "شرقيات", "س", "الاحداثي_السيني", "سيني", "خط_الطول", "طول"
    )

    private val Y_CANDIDATES = listOf(
        "y", "lat", "latitude", "northing", "north", "ycoord", "y_coord",
        "شمال", "شماليات", "ص", "الاحداثي_الصادي", "صادي", "خط_العرض", "عرض"
    )

    private val Z_CANDIDATES = listOf(
        "z", "elev", "elevation", "height", "altitude", "h", "ele",
        "الارتفاع", "منسوب", "ع", "ارتفاع"
    )

    private val LABEL_CANDIDATES = listOf(
        "name", "label", "id", "point", "pnt", "code", "desc", "description",
        "اسم", "رقم", "نقطة", "كود", "بيان", "وصف"
    )

    /**
     * فحص أولي لملف الإكسيل لاستخراج أسماء الأعمدة وعينات للمعاينة،
     * مع اقتراح ذكي لأعمدة X و Y و Z والتسمية.
     */
    fun inspect(context: Context, uri: Uri, maxPreviewRows: Int = 5): TableInspection {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return TableInspection(emptyList(), emptyList(), 0, -1, -1, -1, -1)

        return try {
            val sharedStrings = readSharedStrings(context.contentResolver.openInputStream(uri))
            val rawRows = readSheetRows(stream, sharedStrings, maxPreviewRows + 1)

            if (rawRows.isEmpty()) {
                return TableInspection(emptyList(), emptyList(), 0, -1, -1, -1, -1)
            }

            // الصف الأول هو رؤوس الأعمدة
            val rawHeaders = rawRows.first()
            val headers = rawHeaders.mapIndexed { idx, title ->
                title.ifBlank { "Column_${idx + 1}" }
            }

            val previewRows = mutableListOf<Map<String, String>>()
            for (i in 1 until rawRows.size) {
                val row = rawRows[i]
                val rowMap = mutableMapOf<String, String>()
                for (c in headers.indices) {
                    val value = if (c < row.size) row[c] else ""
                    rowMap[headers[c]] = value
                }
                previewRows.add(rowMap)
            }

            val xIdx = findBestColumnIndex(headers, X_CANDIDATES)
            val yIdx = findBestColumnIndex(headers, Y_CANDIDATES)
            val zIdx = findBestColumnIndex(headers, Z_CANDIDATES)
            val labelIdx = findBestColumnIndex(headers, LABEL_CANDIDATES)

            TableInspection(
                headers = headers,
                previewRows = previewRows,
                totalEstimatedRows = previewRows.size,
                suggestedXIndex = xIdx,
                suggestedYIndex = yIdx,
                suggestedZIndex = zIdx,
                suggestedLabelIndex = labelIdx
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inspecting Excel file: ${e.message}", e)
            TableInspection(emptyList(), emptyList(), 0, -1, -1, -1, -1)
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    /**
     * استيراد وتحليل كامل لملف الإكسيل بعد تحديد المستخدم لحقول X و Y و Z ونظام الإسقاط،
     * مع ربط كامل الأعمدة الأخرى كخصائص (properties) للنقاط.
     */
    fun parseWithMapping(
        context: Context,
        uri: Uri,
        fileName: String,
        mapping: CoordinateMapping
    ): GeoJsonParser.ParseResult {
        val t0 = System.currentTimeMillis()
        val stream = context.contentResolver.openInputStream(uri)
            ?: return GeoJsonParser.ParseResult(error = "لا يمكن فتح ملف الإكسيل")

        return try {
            val sharedStrings = readSharedStrings(context.contentResolver.openInputStream(uri))
            val rawRows = readSheetRows(stream, sharedStrings, maxRows = 100_000)

            if (rawRows.size < 2) {
                return GeoJsonParser.ParseResult(error = "الملف فارغ أو لا يحتوي على صفوف بيانات كافية")
            }

            val headers = rawRows.first().mapIndexed { idx, title ->
                title.ifBlank { "Column_${idx + 1}" }
            }

            val xIdx = mapping.xColumnIndex
            val yIdx = mapping.yColumnIndex
            val zIdx = mapping.zColumnIndex
            val labelIdx = mapping.labelColumnIndex

            if (xIdx !in headers.indices || yIdx !in headers.indices) {
                return GeoJsonParser.ParseResult(error = "أعمدة الإحداثيات المحددة غير صالحة")
            }

            val crsInfo = CrsTransform.CrsInfo(
                isWgs84 = mapping.epsgCode == 4326,
                name = "EPSG:${mapping.epsgCode}",
                epsg = mapping.epsgCode,
                isUtm = mapping.utmZone in 1..60,
                utmZone = mapping.utmZone,
                utmNorth = mapping.isUtmNorth,
                isGeographic = mapping.epsgCode == 4326
            )

            val features = mutableListOf<FeatureRow>()
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE

            for (rowIndex in 1 until rawRows.size) {
                val row = rawRows[rowIndex]
                if (row.all { it.isBlank() }) continue

                val rawX = if (xIdx < row.size) row[xIdx].trim().replace(',', '.') else ""
                val rawY = if (yIdx < row.size) row[yIdx].trim().replace(',', '.') else ""

                val xVal = rawX.toDoubleOrNull() ?: continue
                val yVal = rawY.toDoubleOrNull() ?: continue

                // تحويل الإحداثيات إلى WGS84 إذا كانت بنظام إسقاط آخر
                val (lon, lat) = if (mapping.epsgCode == 4326) {
                    Pair(xVal, yVal)
                } else {
                    CrsTransform.toWgs84(xVal, yVal, crsInfo)
                }

                if (!lon.isFinite() || !lat.isFinite()) continue

                if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                    if (lat < minLat) minLat = lat
                    if (lat > maxLat) maxLat = lat
                    if (lon < minLon) minLon = lon
                    if (lon > maxLon) maxLon = lon
                }

                val zVal = if (zIdx in headers.indices && zIdx < row.size) {
                    row[zIdx].trim().replace(',', '.').toDoubleOrNull()
                } else null

                // بناء كافة الخصائص من بقية الأعمدة كما طلب المستخدم بالضبط
                val props = mutableMapOf<String, String>()
                for (colIdx in headers.indices) {
                    val key = headers[colIdx]
                    val cellValue = if (colIdx < row.size) row[colIdx].trim() else ""
                    props[key] = cellValue
                }

                val coordsStr = if (zVal != null) "[$lon, $lat, $zVal]" else "[$lon, $lat]"
                val fid = if (labelIdx in headers.indices && labelIdx < row.size && row[labelIdx].isNotBlank()) {
                    row[labelIdx].trim()
                } else {
                    "row_$rowIndex"
                }

                features.add(
                    FeatureRow(
                        id = fid,
                        properties = props,
                        geometryType = "Point",
                        geometryCoordinates = coordsStr,
                        crs = "EPSG:4326"
                    )
                )
            }

            if (features.isEmpty()) {
                return GeoJsonParser.ParseResult(error = "لم يتم العثور على أي نقاط بإحداثيات صحيحة في الأعمدة المحددة")
            }

            val extent = if (minLat <= maxLat && minLon <= maxLon) {
                String.format(java.util.Locale.US, "%.5f, %.5f, %.5f, %.5f", minLon, minLat, maxLon, maxLat)
            } else {
                CoordinateConverter.computeExtent(features)
            }

            AppLogger.d(TAG, "Excel parsed successfully: ${features.size} points from $fileName in ${System.currentTimeMillis() - t0}ms")

            GeoJsonParser.ParseResult(
                features = features,
                fileName = fileName,
                crs = "EPSG:4326",
                extent = extent,
                parseTimeMs = System.currentTimeMillis() - t0
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse Excel: ${e.message}", e)
            GeoJsonParser.ParseResult(error = "فشل في قراءة ملف الإكسيل: ${e.message}")
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    private fun findBestColumnIndex(headers: List<String>, candidates: List<String>): Int {
        for ((idx, h) in headers.withIndex()) {
            val clean = h.lowercase().replace(" ", "").replace("_", "").replace("-", "")
            for (c in candidates) {
                val cleanC = c.lowercase().replace("_", "")
                if (clean == cleanC) return idx
            }
        }
        for ((idx, h) in headers.withIndex()) {
            val clean = h.lowercase().replace(" ", "").replace("_", "").replace("-", "")
            for (c in candidates) {
                val cleanC = c.lowercase().replace("_", "")
                if (clean.contains(cleanC)) return idx
            }
        }
        return -1
    }

    private fun readSharedStrings(stream: InputStream?): List<String> {
        if (stream == null) return emptyList()
        val strings = ArrayList<String>()
        try {
            ZipInputStream(stream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                        val parser = XmlPullParserFactory.newInstance().newPullParser()
                        parser.setInput(zis, "UTF-8")
                        var eventType = parser.eventType
                        var inStringItem = false
                        var inText = false
                        val currentText = java.lang.StringBuilder()

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    val name = parser.name
                                    if (name == "si") {
                                        inStringItem = true
                                        currentText.setLength(0)
                                    } else if (name == "t" && inStringItem) {
                                        inText = true
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    if (inText) {
                                        currentText.append(parser.text)
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    val name = parser.name
                                    if (name == "t") {
                                        inText = false
                                    } else if (name == "si") {
                                        inStringItem = false
                                        strings.add(currentText.toString())
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error reading sharedStrings: ${e.message}")
        }
        return strings
    }

    private fun readSheetRows(stream: InputStream, sharedStrings: List<String>, maxRows: Int): List<List<String>> {
        val rows = ArrayList<List<String>>()
        try {
            ZipInputStream(stream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        val parser = XmlPullParserFactory.newInstance().newPullParser()
                        parser.setInput(zis, "UTF-8")
                        var eventType = parser.eventType

                        var currentCells = mutableMapOf<Int, String>()
                        var currentCellRef = ""
                        var cellType = ""
                        var isCellValue = false
                        val cellText = java.lang.StringBuilder()

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    when (parser.name) {
                                        "row" -> {
                                            currentCells = mutableMapOf()
                                        }
                                        "c" -> {
                                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                                            cellType = parser.getAttributeValue(null, "t") ?: ""
                                            cellText.setLength(0)
                                        }
                                        "v", "t" -> {
                                            isCellValue = true
                                        }
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    if (isCellValue) {
                                        cellText.append(parser.text)
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    when (parser.name) {
                                        "v", "t" -> {
                                            isCellValue = false
                                        }
                                        "c" -> {
                                            val colIndex = parseColumnIndex(currentCellRef)
                                            val raw = cellText.toString()
                                            val finalVal = if (cellType == "s") {
                                                val sIdx = raw.toIntOrNull()
                                                if (sIdx != null && sIdx in sharedStrings.indices) sharedStrings[sIdx] else raw
                                            } else {
                                                raw
                                            }
                                            if (colIndex >= 0) {
                                                currentCells[colIndex] = finalVal
                                            }
                                        }
                                        "row" -> {
                                            if (currentCells.isNotEmpty()) {
                                                val maxCol = currentCells.keys.maxOrNull() ?: 0
                                                val rowList = ArrayList<String>(maxCol + 1)
                                                for (c in 0..maxCol) {
                                                    rowList.add(currentCells[c] ?: "")
                                                }
                                                rows.add(rowList)
                                                if (rows.size >= maxRows) return rows
                                            }
                                        }
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error reading sheet rows: ${e.message}")
        }
        return rows
    }

    private fun parseColumnIndex(cellRef: String): Int {
        if (cellRef.isBlank()) return -1
        var col = 0
        var hasCol = false
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
                hasCol = true
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
                hasCol = true
            } else {
                break
            }
        }
        return if (hasCol) col - 1 else -1
    }
}
