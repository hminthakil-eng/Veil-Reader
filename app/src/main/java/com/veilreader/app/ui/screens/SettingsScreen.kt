package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.VeilSpacing
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSetAppThemeMode: (AppThemeMode) -> Unit,
    onSaveReaderAppearance: (ReaderAppearance) -> Unit,
    onClose: () -> Unit
) {
    val appearance = settings.readerAppearance
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        TextButton(
            onClick = onClose,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("← Back")
        }

        ScreenHeader(
            eyebrow = "Settings",
            title = "Reader & app",
            subtitle = "Keep the interface quiet and set reading defaults once. Changes are stored locally."
        )

        SettingsSection(
            title = "App appearance",
            description = "Choose how Veil Reader's own interface follows your device."
        ) {
            ChoiceRow(
                entries = AppThemeMode.entries,
                selected = settings.appThemeMode,
                label = { it.name.lowercase(Locale.ROOT).replaceFirstChar(Char::titlecase) },
                onSelected = onSetAppThemeMode
            )
        }

        SettingsSection(
            title = "Reading surface",
            description = "These are the defaults used when a publication opens. Book content still follows its own metadata and direction."
        ) {
            Text("Publication theme", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = ReaderTheme.entries,
                selected = appearance.theme,
                label = { it.name.lowercase(Locale.ROOT).replaceFirstChar(Char::titlecase) },
                onSelected = { theme ->
                    onSaveReaderAppearance(appearance.copy(theme = theme))
                }
            )

            ReaderSlider(
                label = "Text size",
                value = appearance.fontScale.toFloat(),
                valueRange = 0.75f..1.8f,
                displayValue = { "${(it * 100).toInt()}%" },
                onCommit = { value ->
                    onSaveReaderAppearance(appearance.copy(fontScale = value.toDouble()))
                }
            )
            ReaderSlider(
                label = "Line height",
                value = appearance.lineHeight.toFloat(),
                valueRange = 1.1f..2.0f,
                displayValue = { String.format(Locale.US, "%.2f×", it) },
                onCommit = { value ->
                    onSaveReaderAppearance(appearance.copy(lineHeight = value.toDouble()))
                }
            )
            ReaderSlider(
                label = "Page margins",
                value = appearance.pageMargins.toFloat(),
                valueRange = 0.5f..2.0f,
                displayValue = { String.format(Locale.US, "%.2f×", it) },
                onCommit = { value ->
                    onSaveReaderAppearance(appearance.copy(pageMargins = value.toDouble()))
                }
            )

            SettingsSwitchRow(
                title = "Scroll mode",
                subtitle = "Use continuous vertical reading instead of pagination when the format supports it.",
                checked = appearance.scroll,
                onCheckedChange = { enabled ->
                    onSaveReaderAppearance(appearance.copy(scroll = enabled))
                }
            )
            SettingsSwitchRow(
                title = "Publisher styles",
                subtitle = "Keep the publication's typography and styling when available.",
                checked = appearance.publisherStyles,
                onCheckedChange = { enabled ->
                    onSaveReaderAppearance(appearance.copy(publisherStyles = enabled))
                }
            )
        }

        SettingsSection(
            title = "Reset",
            description = "Restore Veil Reader's reader defaults without touching books, progress, highlights, notes or backups."
        ) {
            OutlinedButton(
                onClick = { onSaveReaderAppearance(ReaderAppearance()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Reset reader defaults")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    MysteryCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VeilSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            content()
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    entries: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
    ) {
        entries.forEach { entry ->
            FilterChip(
                selected = entry == selected,
                onClick = { onSelected(entry) },
                label = { Text(label(entry)) }
            )
        }
    }
}

@Composable
private fun ReaderSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: (Float) -> String,
    onCommit: (Float) -> Unit
) {
    var draft by remember(value) { mutableFloatStateOf(value) }

    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                displayValue(draft),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onCommit(draft) },
            valueRange = valueRange
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
