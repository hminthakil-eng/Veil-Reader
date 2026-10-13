package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.VeilSpacing

/** Small literal choices; publication colors stay independent from the app shell. */
@Composable
internal fun ReaderCompactThemeSelector(selected: ReaderTheme, onSelect: (ReaderTheme) -> Unit) {
    val themes = listOf(ReaderTheme.PAPER, ReaderTheme.SEPIA, ReaderTheme.DUSK, ReaderTheme.OLED)
    val colors = MaterialTheme.colorScheme
    @Composable
    fun Choice(theme: ReaderTheme, modifier: Modifier) {
        val label = stringResource(when (theme) {
            ReaderTheme.PAPER -> R.string.settings_reader_paper
            ReaderTheme.SEPIA -> R.string.settings_reader_sepia
            ReaderTheme.DUSK -> R.string.settings_reader_dusk
            ReaderTheme.OLED -> R.string.settings_reader_oled
        })
        val active = theme == selected
        val (paper, ink) = readiumThemeColors(theme)
        Surface(
            modifier = modifier.heightIn(min = 48.dp).selectable(
                selected = active, role = Role.RadioButton, onClick = { onSelect(theme) }
            ),
            color = if (active) colors.surfaceVariant else colors.surface,
            contentColor = colors.onSurface,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, if (active) colors.primary else colors.outlineVariant)
        ) {
            Row(Modifier.padding(horizontal = VeilSpacing.sm, vertical = VeilSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
                // A sample of the actual publication palette, never a simulated book page.
                Box(Modifier.size(24.dp).background(Color(paper), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size(8.dp, 1.dp).background(Color(ink)))
                }
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                RadioButton(selected = active, onClick = null,
                    modifier = Modifier.size(20.dp).clearAndSetSemantics { })
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(maxWidth.value.toInt(), LocalDensity.current.fontScale, 2)
        Column(Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
            if (stacked) themes.forEach { Choice(it, Modifier.fillMaxWidth()) }
            else themes.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
                    pair.forEach { Choice(it, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}
