package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.VeilPalette

/** Both settings surfaces edit the same existing persisted appearance fields. */
@Composable
internal fun ReaderThemeChoices(appearance: ReaderAppearance, onChange: (ReaderAppearance) -> Unit) {
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReaderTheme.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { theme ->
                    val (paper, ink) = readiumThemeColors(theme)
                    val selected = appearance.theme == theme
                    val label = when (theme) {
                        ReaderTheme.PAPER -> "Paper"
                        ReaderTheme.SEPIA -> "Sepia"
                        ReaderTheme.DUSK -> "Dusk"
                        ReaderTheme.OLED -> "OLED"
                    }
                    Surface(
                        modifier = Modifier.weight(1f).selectable(selected = selected, role = Role.RadioButton,
                            onClick = { onChange(appearance.withTheme(theme)) }),
                        shape = MaterialTheme.shapes.small,
                        color = Color(paper), contentColor = Color(ink),
                        border = BorderStroke(if (selected) 2.dp else 1.dp,
                            if (selected) VeilPalette.OldGold else VeilPalette.TarnishedBrass)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Aa", fontFamily = FontFamily.Serif, fontSize = 28.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                                Text(if (selected) "●" else "○", color = Color(ink))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReaderMotionChoices(appearance: ReaderAppearance, onChange: (ReaderAppearance) -> Unit) {
    val selected = when {
        appearance.scroll -> 2
        appearance.pageTurnStyle == PageTurnStyle.PAPER -> 0
        else -> 1
    }
    val labels = listOf("Paper curl", "Slide", "Scroll")
    val descriptions = listOf("Turn a paper page with your finger.",
        "Move horizontally between pages.", "Read continuously from top to bottom.")
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            Surface(
                modifier = Modifier.fillMaxWidth().selectable(selected = selected == index,
                    role = Role.RadioButton, onClick = {
                        onChange(when (index) {
                            0 -> appearance.copy(scroll = false, pageTurnStyle = PageTurnStyle.PAPER)
                            1 -> appearance.copy(scroll = false, pageTurnStyle = PageTurnStyle.SLIDE)
                            else -> appearance.copy(scroll = true)
                        })
                    }),
                shape = MaterialTheme.shapes.small,
                color = if (selected == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (selected == index) VeilPalette.OldGold else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = selected == index, onClick = null)
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(descriptions[index], style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** A labelled sample, not a substitute for the live publication or its publisher styling. */
@Composable
internal fun ReaderPageSample(appearance: ReaderAppearance) {
    val (paper, ink) = readiumThemeColors(appearance.theme)
    Surface(Modifier.fillMaxWidth(), color = Color(paper), contentColor = Color(ink),
        shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, VeilPalette.TarnishedBrass)) {
        Column(Modifier.padding(horizontal = (16 * appearance.pageMargins).dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PAGE PREVIEW", style = MaterialTheme.typography.labelSmall)
            Text("Every book is a door.", fontFamily = FontFamily.Serif,
                fontSize = (16 * appearance.fontScale).sp,
                lineHeight = (16 * appearance.fontScale * appearance.lineHeight).sp)
            if (appearance.publisherStyles) {
                Text("Publisher styles are on; the book may look different.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
