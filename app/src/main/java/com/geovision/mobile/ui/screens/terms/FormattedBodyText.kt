package com.geovision.mobile.ui.screens.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF1565C0)

@Composable
fun FormattedBodyText(text: String) {
    val normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    val blocks = normalized.split("\n\n").filter { it.isNotBlank() }
    if (blocks.isEmpty()) return

    // ── Block 0: Title ──
    val titleLines = blocks[0].trim().split("\n")
    Text(
        text = titleLines[0],
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
    )
    if (titleLines.size > 1) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = titleLines.drop(1).joinToString("\n").trim(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            lineHeight = 26.sp
        )
    }
    Spacer(Modifier.height(14.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    Spacer(Modifier.height(24.dp))

    // ── Blocks 1..N ──
    var i = 1
    while (i < blocks.size) {
        val raw = blocks[i].trim()
        val lines = raw.split("\n")
        val fl = lines[0].trim()

        // Detect numbered item: "1. Title"
        val numMatch = Regex("""^(\d+)\s*[\.\u060D]\s+(.*)""").find(fl)
        if (numMatch != null) {
            val num = numMatch.groupValues[1].toIntOrNull() ?: (i)
            val title = numMatch.groupValues[2].trim()
            val body = if (lines.size > 1) lines.drop(1).joinToString("\n").trim() else ""
            Spacer(Modifier.height(14.dp))
            NumberedCard(num, title, body)
            i++
            continue
        }

        // Detect section heading ending with ":"
        if (fl.endsWith(":")) {
            val title = fl.removeSuffix(":")
            val items = mutableListOf<String>()
            if (lines.size > 1) {
                lines.drop(1).forEach { l ->
                    val t = l.trim().removePrefix("–").removePrefix("-").removePrefix("*").trim()
                    if (t.isNotEmpty()) items.add(t)
                }
            }
            Spacer(Modifier.height(14.dp))
            GuideCard(title, items)
            i++
            continue
        }

        // Detect standalone heading (single line, short, no period)
        if (lines.size == 1 && fl.length < 60 && !fl.contains(".") && !fl.startsWith("–") && !fl.startsWith("-")) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = fl,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(
                modifier = Modifier.width(56.dp),
                color = Accent,
                thickness = 3.dp
            )
            i++
            continue
        }

        // Plain paragraph
        Spacer(Modifier.height(6.dp))
        Text(
            text = raw,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            lineHeight = 26.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        i++
    }
}

@Composable
private fun NumberedCard(number: Int, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 6.dp)) {
            // Number badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 8.dp, top = 10.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(top = 10.dp, bottom = 8.dp, end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                if (body.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val items = body.split("\n")
                    items.forEach { line ->
                        val l = line.trim()
                        if (l.startsWith("–") || l.startsWith("-")) {
                            Row(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.Top) {
                                Text("\u2022", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(14.dp))
                                Text(l.removePrefix("–").removePrefix("-").trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                            }
                        } else {
                            Text(l, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCard(title: String, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Accent))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent, textAlign = TextAlign.Start)
            }
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                items.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.Top) {
                        Text("\u2022", fontWeight = FontWeight.Bold, color = Accent.copy(alpha = 0.7f), modifier = Modifier.width(16.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}
