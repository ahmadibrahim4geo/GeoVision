package com.geovision.mobile.data

import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.ui.screens.layers.FeatureRow
import java.io.BufferedReader
import java.io.Reader

object CsvParser {
    private const val TAG = "CsvParser"

    private val LAT_CANDIDATES = listOf(
        "lat", "latitude", "y", "northing", "north", "ycoord", "y_coord", "lat_deg",
        "خط_العرض", "عرض", "شمال", "شماليات", "ص", "صادي", "الاحداثي_الصادي"
    )

    private val LON_CANDIDATES = listOf(
        "lon", "long", "lng", "longitude", "x", "easting", "east", "xcoord", "x_coord", "lon_deg",
        "خط_الطول", "طول", "شرق", "شرقيات", "س", "سيني", "الاحداثي_السيني"
    )

    private val ELEV_CANDIDATES = listOf(
        "z", "elev", "elevation", "altitude", "height", "ele", "h", "الارتفاع", "منسوب", "ع"
    )

    private val LABEL_CANDIDATES = listOf(
        "name", "label", "id", "point", "pnt", "code", "desc", "description",
        "اسم", "رقم", "نقطة", "كود", "بيان", "وصف"
    )

    /**
     * فحص أولي لملف CSV لاستخراج الأعمدة ومعاينة أول بضعة صفوف
     */
    fun inspect(reader: Reader, maxPreviewRows: Int = 5): ExcelParser.TableInspection {
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)
        val firstLine = bufferedReader.readLine() ?: return ExcelParser.TableInspection(emptyList(), emptyList(), 0, -1, -1, -1, -1)

        val delimiter = detectDelimiter(firstLine)
        val headers = parseLine(firstLine, delimiter).mapIndexed { idx, it ->
            val clean = it.trim().trim('"', '\'')
            clean.ifBlank { "Column_${idx + 1}" }
        }

        val previewRows = mutableListOf<Map<String, String>>()
        var line: String?
        var count = 0
        while (bufferedReader.readLine().also { line = it } != null && count < maxPreviewRows) {
            val raw = line?.trim() ?: continue
            if (raw.isBlank() || raw.startsWith("#")) continue
            val tokens = parseLine(raw, delimiter)
            val rowMap = mutableMapOf<String, String>()
            for (i in headers.indices) {
                rowMap[headers[i]] = if (i < tokens.size) tokens[i].trim().trim('"', '\'') else ""
            }
            previewRows.add(rowMap)
            count++
        }

        val lonIdx = findColumnIndex(headers, LON_CANDIDATES)
        val latIdx = findColumnIndex(headers, LAT_CANDIDATES)
        val elevIdx = findColumnIndex(headers, ELEV_CANDIDATES)
        val labelIdx = findColumnIndex(headers, LABEL_CANDIDATES)

        return ExcelParser.TableInspection(
            headers = headers,
            previewRows = previewRows,
            totalEstimatedRows = previewRows.size,
            suggestedXIndex = lonIdx,
            suggestedYIndex = latIdx,
            suggestedZIndex = elevIdx,
            suggestedLabelIndex = labelIdx
        )
    }

    /**
     * استيراد CSV مع تحديد حقول X و Y و Z ونظام الإسقاط،
     * مع حفظ وربط كافة الأعمدة الأخرى كخصائص للنقاط.
     */
    fun parseWithMapping(
        reader: Reader,
        fileName: String,
        mapping: ExcelParser.CoordinateMapping
    ): GeoJsonParser.ParseResult {
        val t0 = System.currentTimeMillis()
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)

        val firstLine = bufferedReader.readLine() ?: return GeoJsonParser.ParseResult(
            error = "ملف CSV فارغ",
            errorType = GeoJsonParser.ParseErrorType.CORRUPTED
        )

        val delimiter = detectDelimiter(firstLine)
        val headers = parseLine(firstLine, delimiter).mapIndexed { idx, it ->
            val clean = it.trim().trim('"', '\'')
            clean.ifBlank { "Column_${idx + 1}" }
        }

        val xIdx = mapping.xColumnIndex
        val yIdx = mapping.yColumnIndex
        val zIdx = mapping.zColumnIndex
        val labelIdx = mapping.labelColumnIndex

        if (xIdx !in headers.indices || yIdx !in headers.indices) {
            return GeoJsonParser.ParseResult(
                error = "أعمدة الإحداثيات المحددة غير صالحة",
                errorType = GeoJsonParser.ParseErrorType.CORRUPTED
            )
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
        var lineNum = 1
        var line: String?

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        while (bufferedReader.readLine().also { line = it } != null) {
            lineNum++
            val raw = line?.trim() ?: continue
            if (raw.isBlank() || raw.startsWith("#")) continue

            val tokens = parseLine(raw, delimiter)
            if (tokens.size <= maxOf(xIdx, yIdx)) continue

            val xStr = tokens[xIdx].trim().replace(',', '.')
            val yStr = tokens[yIdx].trim().replace(',', '.')
            val xVal = xStr.toDoubleOrNull() ?: continue
            val yVal = yStr.toDoubleOrNull() ?: continue

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

            val elevStr = if (zIdx in headers.indices && zIdx < tokens.size) tokens[zIdx].trim().replace(',', '.') else null
            val elev = elevStr?.toDoubleOrNull()

            // ربط كافة الأعمدة بالكامل كخصائص لكل نقطة
            val props = mutableMapOf<String, String>()
            for (i in headers.indices) {
                val key = headers[i]
                val valStr = if (i < tokens.size) tokens[i].trim().trim('"', '\'') else ""
                props[key] = valStr
            }

            val coords = if (elev != null) "[$lon, $lat, $elev]" else "[$lon, $lat]"
            val fid = if (labelIdx in headers.indices && labelIdx < tokens.size && tokens[labelIdx].isNotBlank()) {
                tokens[labelIdx].trim().trim('"', '\'')
            } else {
                props["id"] ?: props["ID"] ?: props["name"] ?: "row_$lineNum"
            }

            features.add(
                FeatureRow(
                    id = fid,
                    properties = props,
                    geometryType = "Point",
                    geometryCoordinates = coords,
                    crs = "EPSG:4326"
                )
            )
        }

        if (features.isEmpty()) {
            return GeoJsonParser.ParseResult(
                error = "لم يتم العثور على أي نقاط ذات إحداثيات صالحة في الملف",
                errorType = GeoJsonParser.ParseErrorType.CORRUPTED
            )
        }

        val extent = if (minLat <= maxLat && minLon <= maxLon) {
            String.format(java.util.Locale.US, "%.5f, %.5f, %.5f, %.5f", minLon, minLat, maxLon, maxLat)
        } else {
            CoordinateConverter.computeExtent(features)
        }

        AppLogger.d(TAG, "CSV parsed with mapping: ${features.size} points from $fileName in ${System.currentTimeMillis() - t0}ms")

        return GeoJsonParser.ParseResult(
            features = features,
            fileName = fileName,
            crs = "EPSG:4326",
            extent = extent,
            parseTimeMs = System.currentTimeMillis() - t0
        )
    }

    /** دالة التحليل التلقائي القديمة (Fallback) للتوافق */
    fun streamParse(reader: Reader, fileName: String): GeoJsonParser.ParseResult {
        val t0 = System.currentTimeMillis()
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)

        val firstLine = bufferedReader.readLine() ?: return GeoJsonParser.ParseResult(
            error = "ملف CSV فارغ",
            errorType = GeoJsonParser.ParseErrorType.CORRUPTED
        )

        val delimiter = detectDelimiter(firstLine)
        val headers = parseLine(firstLine, delimiter).map { it.trim().trim('"', '\'') }

        val latIdx = findColumnIndex(headers, LAT_CANDIDATES)
        val lonIdx = findColumnIndex(headers, LON_CANDIDATES)

        if (latIdx == -1 || lonIdx == -1) {
            return GeoJsonParser.ParseResult(
                error = "لم يتم العثور على أعمدة الإحداثيات (Latitude/Longitude أو X/Y) في ملف CSV",
                errorType = GeoJsonParser.ParseErrorType.CORRUPTED
            )
        }

        return parseWithMapping(
            reader = java.io.StringReader(firstLine + "\n" + bufferedReader.readText()),
            fileName = fileName,
            mapping = ExcelParser.CoordinateMapping(
                xColumnIndex = lonIdx,
                yColumnIndex = latIdx,
                zColumnIndex = findColumnIndex(headers, ELEV_CANDIDATES),
                labelColumnIndex = findColumnIndex(headers, LABEL_CANDIDATES),
                epsgCode = 4326
            )
        )
    }

    private fun detectDelimiter(headerLine: String): Char {
        val commaCount = headerLine.count { it == ',' }
        val semiCount = headerLine.count { it == ';' }
        val tabCount = headerLine.count { it == '\t' }
        val pipeCount = headerLine.count { it == '|' }

        return when {
            tabCount > commaCount && tabCount > semiCount && tabCount > pipeCount -> '\t'
            semiCount > commaCount && semiCount > pipeCount -> ';'
            pipeCount > commaCount -> '|'
            else -> ','
        }
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    private fun findColumnIndex(headers: List<String>, candidates: List<String>): Int {
        for ((idx, h) in headers.withIndex()) {
            val clean = h.lowercase().replace(" ", "").replace("_", "").replace("-", "")
            for (c in candidates) {
                val cleanCandidate = c.lowercase().replace("_", "")
                if (clean == cleanCandidate || clean.startsWith(cleanCandidate) || clean.endsWith(cleanCandidate)) {
                    return idx
                }
            }
        }
        return -1
    }
}
