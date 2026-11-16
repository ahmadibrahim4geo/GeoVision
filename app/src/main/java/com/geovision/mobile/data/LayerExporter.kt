package com.geovision.mobile.data

import android.content.Context
import com.geovision.mobile.ui.screens.layers.FeatureRow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Exporter utility for GIS vector layers supporting GeoJSON, CSV, KML, and GeoPackage formats.
 */
object LayerExporter {

    enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
        GEOJSON("geojson", "application/geo+json", "GeoJSON (.geojson)"),
        CSV("csv", "text/csv", "CSV (.csv)"),
        KML("kml", "application/vnd.google-earth.kml+xml", "KML (.kml)"),
        GEOPACKAGE("gpkg", "application/geopackage+sqlite3", "GeoPackage (.gpkg)")
    }

    /**
     * Exports features to a GeoJSON FeatureCollection string.
     */
    fun exportToGeoJson(layerName: String, features: List<FeatureRow>): String {
        val root = JSONObject()
        root.put("type", "FeatureCollection")
        root.put("name", layerName)

        val featuresArray = JSONArray()
        for (f in features) {
            val featObj = JSONObject()
            featObj.put("type", "Feature")
            featObj.put("id", f.id)

            val propsObj = JSONObject()
            for ((k, v) in f.properties) {
                propsObj.put(k, v)
            }
            featObj.put("properties", propsObj)

            if (f.geometryType != null && f.geometryCoordinates != null) {
                try {
                    val geomObj = JSONObject()
                    geomObj.put("type", f.geometryType)
                    geomObj.put("coordinates", JSONArray(f.geometryCoordinates))
                    featObj.put("geometry", geomObj)
                } catch (_: Exception) {
                    featObj.put("geometry", JSONObject.NULL)
                }
            } else {
                featObj.put("geometry", JSONObject.NULL)
            }

            featuresArray.put(featObj)
        }
        root.put("features", featuresArray)

        return root.toString(2)
    }

    /**
     * Exports features to a CSV formatted string with X, Y, Z coordinates and all attribute columns.
     */
    fun exportToCsv(features: List<FeatureRow>): String {
        val allPropKeys = linkedSetOf<String>()
        for (f in features) {
            allPropKeys.addAll(f.properties.keys)
        }
        // Remove x/y/z from property keys if they duplicate coordinate columns
        val attributeKeys = allPropKeys.filter { 
            !it.equals("x", ignoreCase = true) && 
            !it.equals("y", ignoreCase = true) && 
            !it.equals("z", ignoreCase = true) &&
            !it.equals("longitude", ignoreCase = true) &&
            !it.equals("latitude", ignoreCase = true)
        }

        val sb = StringBuilder()
        // Header
        sb.append("FID,X,Y,Z,GeometryType")
        for (k in attributeKeys) {
            sb.append(",").append(escapeCsv(k))
        }
        sb.append("\n")

        // Rows
        for (f in features) {
            sb.append(escapeCsv(f.id)).append(",")
            val coords = extractFirstCoordinate(f.geometryCoordinates)
            sb.append(coords.first?.toString() ?: "").append(",")
            sb.append(coords.second?.toString() ?: "").append(",")
            sb.append(coords.third?.toString() ?: "").append(",")
            sb.append(escapeCsv(f.geometryType ?: "")).append(",")

            for (i in attributeKeys.indices) {
                val v = f.properties[attributeKeys[i]].orEmpty()
                sb.append(escapeCsv(v))
                if (i < attributeKeys.size - 1) sb.append(",")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Exports features to a KML 2.2 string.
     */
    fun exportToKml(layerName: String, features: List<FeatureRow>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append("\n")
        sb.append("  <Document>\n")
        sb.append("    <name>").append(escapeXml(layerName)).append("</name>\n")

        for (f in features) {
            sb.append("    <Placemark>\n")
            val name = f.properties["name"] 
                ?: f.properties["Name"] 
                ?: f.properties["label"] 
                ?: f.properties["title"] 
                ?: "Feature ${f.id}"
            sb.append("      <name>").append(escapeXml(name)).append("</name>\n")

            // ExtendedData
            if (f.properties.isNotEmpty()) {
                sb.append("      <ExtendedData>\n")
                for ((k, v) in f.properties) {
                    sb.append("        <Data name=\"").append(escapeXml(k)).append("\">\n")
                    sb.append("          <value>").append(escapeXml(v)).append("</value>\n")
                    sb.append("        </Data>\n")
                }
                sb.append("      </ExtendedData>\n")
            }

            // Geometry
            val geomXml = buildKmlGeometry(f.geometryType, f.geometryCoordinates)
            if (geomXml.isNotBlank()) {
                sb.append(geomXml)
            }
            sb.append("    </Placemark>\n")
        }

        sb.append("  </Document>\n")
        sb.append("</kml>\n")
        return sb.toString()
    }

    /**
     * Exports features to an OGC GeoPackage file.
     */
    fun exportToGeoPackage(outputFile: File, layerName: String, features: List<FeatureRow>, crs: String? = "EPSG:4326"): File {
        val writer = GeoPackageWriter(outputFile)
        writer.ensureCreated()
        writer.writeLayer(layerName, features, crs)
        return outputFile
    }

    /**
     * Writes the exported data to a file in the app cache directory ready for sharing or saving.
     */
    fun exportToFile(
        context: Context,
        format: ExportFormat,
        layerName: String,
        features: List<FeatureRow>,
        crs: String? = "EPSG:4326"
    ): File {
        val safeName = layerName.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val outputFile = File(exportDir, "${safeName}.${format.extension}")

        when (format) {
            ExportFormat.GEOJSON -> {
                val json = exportToGeoJson(layerName, features)
                outputFile.writeText(json, StandardCharsets.UTF_8)
            }
            ExportFormat.CSV -> {
                val csv = exportToCsv(features)
                outputFile.writeText(csv, StandardCharsets.UTF_8)
            }
            ExportFormat.KML -> {
                val kml = exportToKml(layerName, features)
                outputFile.writeText(kml, StandardCharsets.UTF_8)
            }
            ExportFormat.GEOPACKAGE -> {
                if (outputFile.exists()) outputFile.delete()
                exportToGeoPackage(outputFile, layerName, features, crs)
            }
        }

        return outputFile
    }

    private fun extractFirstCoordinate(coordsJson: String?): Triple<Double?, Double?, Double?> {
        if (coordsJson.isNullOrBlank()) return Triple(null, null, null)
        val numRegex = Regex("""[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?""")
        val matches = numRegex.findAll(coordsJson).mapNotNull { it.value.toDoubleOrNull() }.toList()
        return when {
            matches.size >= 3 -> Triple(matches[0], matches[1], matches[2])
            matches.size >= 2 -> Triple(matches[0], matches[1], null)
            else -> Triple(null, null, null)
        }
    }

    private fun buildKmlGeometry(geomType: String?, coordsJson: String?): String {
        if (geomType.isNullOrBlank() || coordsJson.isNullOrBlank()) return ""
        val type = geomType.lowercase(Locale.US)
        return when {
            type.contains("point") -> {
                val coords = extractFirstCoordinate(coordsJson)
                if (coords.first != null && coords.second != null) {
                    val zStr = if (coords.third != null) ",${coords.third}" else ",0"
                    "      <Point><coordinates>${coords.first},${coords.second}$zStr</coordinates></Point>\n"
                } else ""
            }
            type.contains("linestring") || type.contains("line") -> {
                val pointsStr = parseCoordinatesToKmlTuple(coordsJson)
                "      <LineString><coordinates>$pointsStr</coordinates></LineString>\n"
            }
            type.contains("polygon") -> {
                val pointsStr = parseCoordinatesToKmlTuple(coordsJson)
                "      <Polygon><outerBoundaryIs><LinearRing><coordinates>$pointsStr</coordinates></LinearRing></outerBoundaryIs></Polygon>\n"
            }
            else -> ""
        }
    }

    private fun parseCoordinatesToKmlTuple(coordsJson: String): String {
        val numRegex = Regex("""[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?""")
        val numbers = numRegex.findAll(coordsJson).mapNotNull { it.value.toDoubleOrNull() }.toList()
        val tuples = mutableListOf<String>()
        for (i in 0 until numbers.size - 1 step 2) {
            tuples.add("${numbers[i]},${numbers[i + 1]},0")
        }
        return tuples.joinToString(" ")
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeXml(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
