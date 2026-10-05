package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.VeilPalette

/** Explicit, ordinary tap targets. Publication input arbitration remains the Reader's owner. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderAccessDock(
    settingsLabel: String,
    background: Color,
    foreground: Color,
    accent: Color,
    onMenu: () -> Unit,
    onSettings: () -> Unit,
    settingsAction: ReaderAction = ReaderAction.APPEARANCE
) {
    Surface(
        color = background,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.35f))
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            TextButton(onClick = onMenu, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) {
                Text(stringResource(R.string.reader_reading_menu), color = foreground, style = MaterialTheme.typography.labelLarge)
            }
            ReaderChromeButton(settingsAction, accessibilityLabel = settingsLabel, tint = accent,
                onClick = onSettings)
        }
    }
}

@Composable
internal fun ReaderMenuAutoHideControl(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val label = stringResource(R.string.reader_menu_auto_hide)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.reader_menu_auto_hide_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}

internal data class ReaderAccessColors(val background: Color, val foreground: Color, val accent: Color)

/** The existing Sanctuary chrome palette, shared by live Reader and review specimens. */
internal fun readerAccessColors(theme: ReaderTheme): ReaderAccessColors {
    val light = theme == ReaderTheme.PAPER || theme == ReaderTheme.SEPIA
    return ReaderAccessColors(
        background = (if (light) Color(0xFFF0E4CC) else VeilPalette.Ink).copy(alpha = 0.94f),
        foreground = if (light) Color(0xFF2B241B) else VeilPalette.Moon,
        accent = if (light) Color(0xFF8A6630) else VeilPalette.Brass
    )
}
