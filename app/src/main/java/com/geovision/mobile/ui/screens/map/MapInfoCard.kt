package com.geovision.mobile.ui.screens.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geovision.mobile.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InfoCard(lat: Double, lon: Double, z: Double, fmt: String = "DD", elevation: Double? = null, onSelectFmt: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val isUtm = fmt == "UTM"
    val utm = if (isUtm) toUtm(lat, lon) else null
    val (latS, lonS) = if (!isUtm) fmtCoord(lat, lon, fmt) else ("" to "")

    val copyText = if (isUtm && utm != null) {
        "Zone: ${utm.zone}${utm.hemisphere}\nEasting: ${java.lang.String.format(java.util.Locale.US, "%.3f", utm.easting)} m\nNorthing: ${java.lang.String.format(java.util.Locale.US, "%.3f", utm.northing)} m"
    } else {
        "$latS, $lonS"
    }

    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val animTarget = fmt + (utm?.let { "${it.zone}${it.hemisphere}${java.lang.String.format(java.util.Locale.US, "%.3f", it.easting)}${java.lang.String.format(java.util.Locale.US, "%.3f", it.northing)}" } ?: "$latS$lonS")

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)),
        elevation = CardDefaults.elevatedCardElevation(8.dp),
        modifier = modifier
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(
                Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))
            ))

            Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
                AnimatedContent(
                    targetState = animTarget,
                    transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
                    label = "coord_anim"
                ) { targetState ->
                    val isUtmState = targetState.startsWith("UTM")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (isUtmState && utm != null) {
                            UTMItem(MaterialTheme.colorScheme.tertiary, stringResource(R.string.zone_label_short), "${utm.zone}${utm.hemisphere}")
                            UTMItem(MaterialTheme.colorScheme.primary, stringResource(R.string.easting_label_short), java.lang.String.format(java.util.Locale.US, "%.3f", utm.easting))
                            UTMItem(MaterialTheme.colorScheme.secondary, stringResource(R.string.northing_label_short), java.lang.String.format(java.util.Locale.US, "%.3f", utm.northing))
                        } else {
                            CoordItem(MaterialTheme.colorScheme.primary, stringResource(R.string.lon_label_short), lonS)
                            CoordItem(MaterialTheme.colorScheme.secondary, stringResource(R.string.lat_label_short), latS)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val formats = listOf("DD" to "DD", "DMS" to "DMS", "UTM" to "UTM")
                    formats.forEach { (key, label) ->
                        val isSelected = fmt == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 6.dp).clickable { onSelectFmt(key) },
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.W900 else FontWeight.W600,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)) {
                        Text("Z${z.toInt()}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W800, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    if (elevation != null) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)) {
                            Text("${java.lang.String.format(java.util.Locale.US, "%.0f", elevation)}m", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    FilledIconButton(
                        onClick = {
                            val clip = android.content.ClipData.newPlainText("coord", copyText)
                            ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)?.let { cm ->
                                (cm as android.content.ClipboardManager).setPrimaryClip(clip)
                            }
                            copied = true
                            scope.launch { delay(1500L); copied = false }
                        },
                        modifier = Modifier.size(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (copied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            contentColor = if (copied) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CoordItem(dotColor: Color, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W900,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(4.dp))
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 1.sp)
        }
    }
}

@Composable
fun UTMItem(dotColor: Color, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W800,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(3.dp))
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 1.sp)
        }
    }
}
