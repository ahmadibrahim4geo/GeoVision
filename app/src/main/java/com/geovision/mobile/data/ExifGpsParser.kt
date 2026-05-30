package com.geovision.mobile.data

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import com.geovision.mobile.core.AppLogger
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object ExifGpsParser {
    /** إغلاق ExifInterface عبر try-fallback لضمان التوافق مع API < 33 */
    private fun closeExif(exif: ExifInterface) {
        try { (exif as? Closeable)?.close() } catch (_: Exception) {}
    }

    fun parse(context: Context, uri: Uri, fileName: String): GeoPhoto? {
        val tempFile = File(context.cacheDir, "exif_${UUID.randomUUID()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    val buf = ByteArray(16384)
                    var read: Int
                    while (input.read(buf).also { read = it } >= 0) { output.write(buf, 0, read) }
                }
            } ?: return null

            val exif = ExifInterface(tempFile.absolutePath)
            try {
                val latLon = FloatArray(2)
                if (!exif.getLatLong(latLon)) {
                    AppLogger.w(AppLogger.Tags.PARSER, "No GPS data in $fileName")
                    return null
                }

                val altitude = exif.getAltitude(java.lang.Double.NaN).let { if (it.isNaN()) null else it }
                val timestamp = parseDateTime(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                val bearing = parseBearing(exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION))
                val size = tempFile.length()

                return GeoPhoto(
                    uri = uri, latitude = latLon[0].toDouble(), longitude = latLon[1].toDouble(),
                    altitude = altitude, timestamp = timestamp, fileName = fileName, fileSize = size, bearing = bearing
                )
            } finally { closeExif(exif) }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tags.PARSER, "EXIF parse failed for $fileName: ${e.message}")
            return null
        } finally { tempFile.delete() }
    }

    fun hasGps(context: Context, uri: Uri): Boolean {
        val tempFile = File(context.cacheDir, "exif_chk_${UUID.randomUUID()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            val exif = ExifInterface(tempFile.absolutePath)
            try {
                val latLon = FloatArray(2)
                return exif.getLatLong(latLon)
            } finally { closeExif(exif) }
        } catch (_: Exception) { return false }
        finally { tempFile.delete() }
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
