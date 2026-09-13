package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance

@Composable
internal fun ReaderModeChoices(appearance: ReaderAppearance, onChange: (ReaderAppearance) -> Unit) {
    val labels = listOf("Slide", "3D curl", "Instant", "Scroll")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (appearance.reduceMotion && !appearance.scroll) {
            Text("Reduced motion is on: EPUB pages turn instantly. Your selected style is kept for later.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (index in row * 2..row * 2 + 1) {
                    val chosen = if (index == 3) appearance.scroll else !appearance.scroll && appearance.pageTurnStyle.ordinal == index
                    Surface(
                        onClick = { onChange(if (index == 3) appearance.copy(scroll = true) else appearance.copy(scroll = false, pageTurnStyle = PageTurnStyle.entries[index])) },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton; selected = chosen },
                        shape = MaterialTheme.shapes.large,
                        color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PageModeGlyph(index, MaterialTheme.colorScheme.onSurface)
                            Text(labels[index], style = MaterialTheme.typography.labelLarge)
                            Text(if (index == 1) "Beta" else if (chosen) "Selected" else when (index) { 0 -> "Gentle page slides"; 2 -> "No animation"; else -> "Continuous reading" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Text(if (appearance.scroll) "Scroll vertically. Tap the page to show your controls." else "Tap the edges or swipe to turn. Tap the center for controls.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (appearance.pageTurnStyle == PageTurnStyle.CURL && !appearance.scroll) {
            Text("3D curl is experimental. Switch to Slide or Instant if your book’s pages do not render comfortably.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PageModeGlyph(mode: Int, tint: Color) {
    Canvas(Modifier.size(48.dp, 42.dp)) {
        val w = size.width; val h = size.height
        val stroke = Stroke(1.4.dp.toPx())
        drawRoundRect(tint.copy(alpha = .22f), Offset(w * .25f, h * .10f), Size(w * .52f, h * .84f), CornerRadius(3.dp.toPx()), style = stroke)
        if (mode == 1) {
            val curl = Path().apply { moveTo(w * .2f, h * .1f); quadraticTo(w * .95f, h * .0f, w * .45f, h * .44f); quadraticTo(w * .36f, h * .7f, w * .2f, h * .94f); close() }
            drawPath(curl, tint.copy(alpha = .3f)); drawPath(curl, tint, style = stroke)
        } else {
            repeat(if (mode == 3) 6 else 4) { line ->
                val y = h * (.24f + line * .105f)
                drawLine(tint.copy(alpha = .8f), Offset(w * .35f, y), Offset(w * .66f, y), 1.dp.toPx())
            }
            if (mode == 0) {
                drawLine(tint, Offset(w * .04f, h * .55f), Offset(w * .19f, h * .55f), 1.4.dp.toPx())
                drawLine(tint, Offset(w * .82f, h * .55f), Offset(w * .97f, h * .55f), 1.4.dp.toPx())
            }
        }
    }
}

@Composable
internal fun ReaderOption(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
        .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
        .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
    }
}
