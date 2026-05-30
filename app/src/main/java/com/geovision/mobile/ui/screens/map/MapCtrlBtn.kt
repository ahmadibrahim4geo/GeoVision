package com.geovision.mobile.ui.screens.map

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CtrlBtn(onClick: () -> Unit, icon: ImageVector, desc: String, tooltip: String = "", active: Boolean = false, badge: String? = null, iconRotation: Float = 0f, onLongClick: (() -> Unit)? = null, tooltipSide: Boolean = false) {
    var showTip by remember { mutableStateOf(false) }
    Box(modifier = Modifier.size(33.dp).combinedClickable(
        onClick = onClick,
        onLongClick = {
            if (onLongClick != null) onLongClick()
            else if (tooltip.isNotBlank()) showTip = true
        }
    )) {
        Box(Modifier.fillMaxSize().shadow(3.dp, CircleShape).clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(icon, desc, Modifier.size(16.dp).rotate(iconRotation), tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
        }
        if (badge != null) Box(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).background(MaterialTheme.colorScheme.error, CircleShape).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.W700) }
        if (showTip && tooltip.isNotBlank()) {
            val align = if (tooltipSide) Alignment.CenterStart else Alignment.TopCenter
            Popup(alignment = align, onDismissRequest = { showTip = false }) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 4.dp) {
                    Text(tooltip, modifier = Modifier.padding(12.dp, 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }
    }
}
