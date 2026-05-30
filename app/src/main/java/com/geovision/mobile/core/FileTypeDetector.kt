package com.geovision.mobile.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.geovision.mobile.ui.screens.layers.FileType

object FileTypeDetector {

    fun inferFileType(uri: Uri, context: Context? = null): FileType? {
        val path = uri.toString().lowercase()
        val ext = path.substringAfterLast('.').take(4)
        when {
            ext == "shp" -> return FileType.SHAPEFILE
            ext in setOf("shb", "shx", "dbf", "prj", "cpg") -> return FileType.SHAPEFILE
            path.endsWith(".zip") -> return FileType.SHAPEFILE
            path.endsWith(".geojson") || path.endsWith(".geo.json") || path.endsWith(".json") -> return FileType.GEOJSON
            path.endsWith(".kml") || path.endsWith(".kmz") -> return FileType.KML
            path.endsWith(".gpx") -> return FileType.GPX
            path.endsWith(".gdb") || path.contains("%2Egdb") || path.contains(".gdb/") -> return FileType.GEODATABASE
            path.endsWith(".gpkg") -> return FileType.GEOPACKAGE
            path.endsWith(".mbtiles") || path.endsWith(".tif") || path.endsWith(".tiff") -> return null
            path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".webp") -> return FileType.PHOTO
        }
        if (context != null) {
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: return null
            if (mime.contains("geo+json") || mime.contains("json")) return FileType.GEOJSON
            if (mime.contains("kml")) return FileType.KML
            if (mime.contains("gpx")) return FileType.GPX
            if (mime.contains("xml") && !mime.contains("svg")) return FileType.KML
            if (mime.contains("geopackage")) return FileType.GEOPACKAGE
            if (mime.contains("shape") || mime.contains("shp") || mime.contains("qgis") || mime.contains("x-gis")) return FileType.SHAPEFILE
            if (mime.contains("jpeg") || mime.contains("png") || mime.contains("webp")) return FileType.PHOTO
            val name = getFileName(context, uri)?.lowercase() ?: return null
            val nameExt = name.substringAfterLast('.').take(4)
            if (nameExt == "shp" || nameExt in setOf("shb", "shx", "dbf", "prj", "cpg")) return FileType.SHAPEFILE
            if (name.endsWith(".geojson") || name.endsWith(".geo.json") || name.endsWith(".json")) return FileType.GEOJSON
            if (name.endsWith(".kml") || name.endsWith(".kmz")) return FileType.KML
            if (name.endsWith(".gpx")) return FileType.GPX
            if (name.endsWith(".gpkg")) return FileType.GEOPACKAGE
            if (name.endsWith(".gdb")) return FileType.GEODATABASE
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) return FileType.PHOTO
        }
        return null
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
        if (name == null) name = uri.lastPathSegment
        return name
    }
}
