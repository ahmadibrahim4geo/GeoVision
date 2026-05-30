package com.geovision.mobile.ui.screens.layers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.geovision.mobile.data.GeoPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun String.fmt(vararg args: Any?): String = java.lang.String.format(Locale.US, this, *args)

@Composable
fun PhotoViewDialog(geoPhoto: GeoPhoto, onDismiss: () -> Unit, onNavigateToLocation: (GeoPhoto) -> Unit = {}) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var coordFmt by remember { mutableStateOf("dd") }

    LaunchedEffect(geoPhoto.uri) {
        bitmap = withContext(Dispatchers.IO) { loadThumbnail(context, geoPhoto.uri, 1200) }
    }

    val (latStr, lonStr) = fmtCoord(geoPhoto.latitude, geoPhoto.longitude, coordFmt)
    val dateStr = geoPhoto.timestamp?.let {
        try { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(it)) } catch (_: Exception) { null }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // ── Header: filename + close ──
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(geoPhoto.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.outline) }
                }

                // ── Image ──
                bitmap?.let { bmp ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                        .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = geoPhoto.fileName, modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Info card ──
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Coordinates row with format toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f)) {
                                Text(latStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
                                Text(lonStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        coordFmt = when (coordFmt) { "dd" -> "dms"; "dms" -> "utm"; else -> "dd" }
                                    }) {
                                    Text(coordFmt.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W800, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Detail rows
                        DetailRow(Icons.Default.Height, "${"%.1f".fmt(geoPhoto.latitude)}°, ${"%.1f".fmt(geoPhoto.longitude)}°")
                        geoPhoto.altitude?.let { DetailRow(Icons.Default.Straighten, "${"%.1f".fmt(it)} m") }
                        dateStr?.let { DetailRow(Icons.Default.Schedule, it) }
                        DetailRow(Icons.Default.Description, formatSize(geoPhoto.fileSize))

                        // Bearing
                        if (geoPhoto.bearing != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    Canvas(Modifier.size(18.dp)) {
                                        val c = Offset(size.width / 2, size.height / 2)
                                        drawLine(Color(0xFFFF5722), Offset(c.x, c.y + 8f), Offset(c.x, c.y - 8f), 2.5f, StrokeCap.Round)
                                        drawLine(Color(0xFFFF5722), Offset(c.x - 5f, c.y - 2f), Offset(c.x, c.y - 8f), 2.5f, StrokeCap.Round)
                                        drawLine(Color(0xFFFF5722), Offset(c.x + 5f, c.y - 2f), Offset(c.x, c.y - 8f), 2.5f, StrokeCap.Round)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("${"%.0f".fmt(geoPhoto.bearing)}° ${bearingText(geoPhoto.bearing)}",
                                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Action buttons ──
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        onNavigateToLocation(geoPhoto)
                        onDismiss()
                    }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.NearMe, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("عرض على الخريطة")
                    }
                    OutlinedButton(onClick = {
                        val uri = Uri.parse("geo:${geoPhoto.latitude},${geoPhoto.longitude}?q=${geoPhoto.latitude},${geoPhoto.longitude}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("فتح في خرائط")
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun fmtCoord(lat: Double, lon: Double, fmt: String): Pair<String, String> {
    return when (fmt) {
        "dms" -> {
            fun dms(v: Double, pos: String, neg: String): String {
                val d = v.toInt(); val m = ((v - d) * 60).toInt()
                val s = (v - d - m / 60.0) * 3600
                return "${d}°${m}'${"%.1f".fmt(s)}\"${if (v >= 0) pos else neg}"
            }
            Pair(dms(lat, "N", "S"), dms(lon, "E", "W"))
        }
        "utm" -> {
            val zone = ((lon + 180) / 6).toInt() + 1
            val hem = if (lat >= 0) "N" else "S"
            Pair("${"%.6f".fmt(lat)}°", "UTM ${zone}${hem}")
        }
        else -> Pair("${"%.6f".fmt(lat)}°", "${"%.6f".fmt(lon)}°")
    }
}

private fun bearingText(b: Float): String = when {
    b < 22.5f || b >= 337.5f -> "N"
    b < 67.5f -> "NE"; b < 112.5f -> "E"; b < 157.5f -> "SE"
    b < 202.5f -> "S"; b < 247.5f -> "SW"; b < 292.5f -> "W"
    else -> "NW"
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".fmt(bytes / (1024.0 * 1024.0))} MB"
}

private fun loadThumbnail(context: Context, uri: Uri, maxSize: Int): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val scale = maxOf(opts.outWidth, opts.outHeight) / maxSize
        opts.inSampleSize = scale.coerceAtLeast(1)
        opts.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) { null }
}
