package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    exporting: Boolean,
    restoring: Boolean,
    onSetAppThemeMode: (AppThemeMode) -> Unit,
    onSaveReaderAppearance: (ReaderAppearance) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onExportNotes: (Uri) -> Unit,
    onClose: () -> Unit
) {
    var appearanceDraft by remember { mutableStateOf(settings.readerAppearance) }
    var pendingAppearance by remember { mutableStateOf<ReaderAppearance?>(null) }

    LaunchedEffect(settings.readerAppearance) {
        val persisted = settings.readerAppearance
        when {
            pendingAppearance == null -> appearanceDraft = persisted
            persisted == pendingAppearance -> {
                appearanceDraft = persisted
                pendingAppearance = null
            }
        }
    }

    fun commitReaderAppearance(transform: (ReaderAppearance) -> ReaderAppearance) {
        val value = transform(appearanceDraft)
        appearanceDraft = value
        pendingAppearance = value
        onSaveReaderAppearance(value)
    }

    val appearance = appearanceDraft
    val context = LocalContext.current
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("").ifBlank { "Unknown" }
    }
    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { it?.let(onExportBackup) }
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { it?.let(onRestoreBackup) }
    val notesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { it?.let(onExportNotes) }
    var confirmRestore by remember { mutableStateOf(false) }
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

        Column(
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Text(
                "GRAYFOG SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
            Text(
                "The Reading Room",
                style = MaterialTheme.typography.headlineLarge
            )
            BrassRule(Modifier.width(92.dp), strong = true)
            Text(
                "Reading, appearance, storage, privacy, and backup controls. Everything remains local unless you export it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(
            title = "Appearance",
            description = "Choose how the archive shell follows your device."
        ) {
            ChoiceRow(
                entries = AppThemeMode.entries,
                selected = settings.appThemeMode,
                label = { it.name.lowercase(Locale.ROOT).replaceFirstChar(Char::titlecase) },
                onSelected = onSetAppThemeMode
            )
        }

        SettingsSection(
            title = "Reading settings",
            description = "Set default theme, typography, page movement, brightness, and layout."
        ) {
            Text("Publication theme", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = ReaderTheme.entries,
                selected = appearance.theme,
                label = { it.name.lowercase(Locale.ROOT).replaceFirstChar(Char::titlecase) },
                onSelected = { theme ->
                    commitReaderAppearance { current -> current.withTheme(theme) }
                }
            )

            ReaderSlider(
                label = "Text size",
                value = appearance.fontScale.toFloat(),
                valueRange = 0.75f..1.8f,
                displayValue = { "${(it * 100).toInt()}%" },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withFontScale(value.toDouble()) }
                }
            )
            ReaderSlider(
                label = "Line height",
                value = appearance.lineHeight.toFloat(),
                valueRange = 1.1f..2.0f,
                displayValue = { String.format(Locale.US, "%.2f×", it) },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withLineHeight(value.toDouble()) }
                }
            )
            ReaderSlider(
                label = "Page margins",
                value = appearance.pageMargins.toFloat(),
                valueRange = 0.5f..2.0f,
                displayValue = { String.format(Locale.US, "%.2f×", it) },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withPageMargins(value.toDouble()) }
                }
            )

            Text("Page turn", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = PageTurnStyle.entries,
                selected = appearance.pageTurnStyle,
                label = { style ->
                    when (style) {
                        PageTurnStyle.PAPER -> "Paper curl"
                        PageTurnStyle.SLIDE -> "Simple slide"
                    }
                },
                onSelected = { style ->
                    commitReaderAppearance { current -> current.copy(pageTurnStyle = style) }
                }
            )
            Text(
                "Paper curl is the premium paginated mode; Simple slide remains the rollback-safe fallback. Continuous scroll ignores this setting.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSwitchRow(
                title = "Scroll mode",
                subtitle = "Use continuous vertical reading instead of pagination when the format supports it.",
                checked = appearance.scroll,
                onCheckedChange = { enabled ->
                    commitReaderAppearance { current -> current.copy(scroll = enabled) }
                }
            )
            SettingsSwitchRow(
                title = "Publisher styles",
                subtitle = "Keep the publication's typography and styling when available. This can override Veil theme colors.",
                checked = appearance.publisherStyles,
                onCheckedChange = { enabled ->
                    commitReaderAppearance { current -> current.copy(publisherStyles = enabled) }
                }
            )

            Text("Reading brightness", style = MaterialTheme.typography.labelLarge)
            ReaderBrightnessControls(
                appearance = appearance,
                onChange = { proposed ->
                    commitReaderAppearance { current ->
                        current.withScreenBrightness(proposed.screenBrightness)
                    }
                }
            )
        }

        SettingsSection(
            title = "Library & backup",
            description = "Export or restore your private local archive, annotations, and reading state."
        ) {
            Button(
                enabled = !exporting && !restoring,
                onClick = { backupPicker.launch("veil-reader-backup.zip") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VeilPalette.Brass,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF17120A)
                )
            ) { Text(if (exporting) "Exporting…" else "Export library backup") }
            OutlinedButton(
                enabled = !exporting && !restoring,
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (restoring) "Restoring…" else "Restore library backup") }
            OutlinedButton(
                enabled = !exporting && !restoring,
                onClick = { notesPicker.launch("veil-reader-notebook.md") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Export notebook as Markdown") }
        }

        SettingsSection(
            title = "Privacy & about",
            description = "Your library, progress, and annotations stay on this device unless you explicitly export them."
        ) {
            Text("App version · $appVersion", style = MaterialTheme.typography.labelLarge)
            Text("Reader engine · Readium Kotlin Toolkit 3.4.0", style = MaterialTheme.typography.labelLarge)
            Text(
                "No account or cloud sync is required for core reading.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        SettingsSection(
            title = "Reset reading defaults",
            description = "Restore reader preferences without touching books, progress, notes, highlights, or backups."
        ) {
            OutlinedButton(
                onClick = { commitReaderAppearance { ReaderAppearance() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Reset reader defaults")
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            shape = MaterialTheme.shapes.small,
            containerColor = VeilPalette.Archive,
            titleContentColor = VeilPalette.Moon,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "RESTORE ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        "Replace local Veil Reader data?",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Text(
                    "Restore replaces your current library, annotations, reading progress, Path progress and Castle state with the selected backup. Export a fresh backup first if you need the current state."
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRestore = false
                        restorePicker.launch(arrayOf("application/zip", "application/octet-stream"))
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF17120A)
                    )
                ) { Text("Choose backup") }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass,
                modifier = Modifier.weight(1f)
            )
        }

        BrassRule(Modifier.fillMaxWidth())

        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(VeilSpacing.md),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                content()
            }
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entries.forEach { entry ->
            val active = entry == selected
            Surface(
                onClick = { onSelected(entry) },
                shape = MaterialTheme.shapes.extraSmall,
                color = if (active) {
                    VeilPalette.DeepBrass.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
                },
                border = BorderStroke(
                    1.dp,
                    if (active) VeilPalette.Brass.copy(alpha = 0.82f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Text(
                    label(entry),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) VeilPalette.Moon
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                color = VeilPalette.Brass,
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = VeilPalette.Moon,
                checkedTrackColor = VeilPalette.DeepBrass,
                checkedBorderColor = VeilPalette.Brass
            )
        )
    }
}
