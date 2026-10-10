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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.veilreader.app.ui.theme.ReaderVisualGeometry
import com.veilreader.app.ui.theme.ReaderVisualOpacity
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.readerVisualThemeArgb

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
        color = background.copy(alpha = ReaderVisualOpacity.AccessDockSurface),
        shape = RoundedCornerShape(ReaderVisualGeometry.CompactControlRadius),
        border = BorderStroke(
            0.5.dp,
            accent.copy(alpha = ReaderVisualOpacity.HudAccentHairline)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ReaderVisualGeometry.HudControlGap),
            modifier = Modifier.padding(horizontal = ReaderVisualGeometry.HudControlGap)
        ) {
            TextButton(
                onClick = onMenu,
                modifier = Modifier.defaultMinSize(
                    minWidth = ReaderVisualGeometry.TouchTarget,
                    minHeight = ReaderVisualGeometry.TouchTarget
                )
            ) {
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
    val colors = readerVisualThemeArgb(theme)
    val light = theme == ReaderTheme.PAPER || theme == ReaderTheme.SEPIA
    return ReaderAccessColors(
        background = Color(colors.background).copy(alpha = ReaderVisualOpacity.ChromeSurface),
        foreground = Color(colors.text),
        accent = if (light) Color(0xFF8A6630) else VeilPalette.Brass
    )
}
