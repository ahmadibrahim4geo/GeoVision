package com.geovision.mobile.data

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.geovision.mobile.core.AppLogger
import java.text.SimpleDateFormat
import java.util.Locale

object ExifGpsParser {

    fun parse(context: Context, uri: Uri, fileName: String): GeoPhoto? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = exif.latLong
                if (latLong == null) {
                    AppLogger.w(AppLogger.Tags.PARSER, "No GPS data in $fileName")
                    return null
                }

                val altitude = exif.getAltitude(Double.NaN).let { if (it.isNaN()) null else it }
                val timestamp = parseDateTime(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                val bearing = parseBearing(exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION))

                GeoPhoto(
                    uri = uri,
                    latitude = latLong[0],
                    longitude = latLong[1],
                    altitude = altitude,
                    timestamp = timestamp,
                    fileName = fileName,
                    fileSize = 0L,
                    bearing = bearing
                )
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "EXIF parse failed for $fileName: ${e.message}")
            null
        }
    }

    fun hasGps(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.latLong != null
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun parseBearing(dir: String?): Float? {
        if (dir == null) return null
        return try { dir.toFloat().let { if (it in 0f..360f) it else null } } catch (_: Exception) { null }
    }

    private fun parseDateTime(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try { SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(dateStr)?.time }
        catch (_: Exception) { null }
    }
}
