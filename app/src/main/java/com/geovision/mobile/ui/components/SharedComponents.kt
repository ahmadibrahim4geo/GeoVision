package com.geovision.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AppLoadingBar(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun AppErrorDialog(
    message: String?,
    onDismiss: () -> Unit,
    title: String = "خطأ",
    confirmLabel: String = "حسناً"
) {
    if (message != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(title, fontWeight = FontWeight.W700) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(confirmLabel) }
            }
        )
    }
}

@Composable
fun AppEmptyState(
    icon: ImageVector = Icons.Default.LayersClear,
    title: String,
    subtitle: String? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W600)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 48.dp))
        }
        if (actions != null) {
            Spacer(Modifier.height(28.dp))
            actions()
        }
    }
}
