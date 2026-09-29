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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.data.settings.AmbientSound
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderTextAlignment
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
    onSaveSensorySettings: (SensorySettings) -> Unit,
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
    val formatPercent = rememberVeilPercentFormatter()
    val context = LocalContext.current
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("").ifBlank { context.getString(R.string.settings_unknown) }
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
            Text(VeilBackLabel(stringResource(R.string.common_back)))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Text(
                stringResource(R.string.settings_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
            Text(
                stringResource(R.string.settings_heading),
                style = MaterialTheme.typography.headlineLarge
            )
            BrassRule(Modifier.width(92.dp), strong = true)
            Text(
                stringResource(R.string.settings_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_appearance_title),
            description = stringResource(R.string.settings_appearance_description)
        ) {
            ChoiceRow(
                entries = AppThemeMode.entries,
                selected = settings.appThemeMode,
                label = { mode -> when (mode) {
                    AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                    AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                    AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                } },
                onSelected = onSetAppThemeMode
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_reading_title),
            description = stringResource(R.string.settings_reading_description)
        ) {
            Text(stringResource(R.string.settings_publication_theme), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = ReaderTheme.entries,
                selected = appearance.theme,
                label = { theme -> when (theme) {
                    ReaderTheme.PAPER -> stringResource(R.string.settings_reader_paper)
                    ReaderTheme.SEPIA -> stringResource(R.string.settings_reader_sepia)
                    ReaderTheme.DUSK -> stringResource(R.string.settings_reader_dusk)
                    ReaderTheme.OLED -> stringResource(R.string.settings_reader_oled)
                } },
                onSelected = { theme ->
                    commitReaderAppearance { current -> current.withTheme(theme) }
                }
            )

            ReaderSlider(
                label = stringResource(R.string.settings_text_size),
                value = appearance.fontScale.toFloat(),
                valueRange = 0.75f..1.8f,
                displayValue = { formatPercent(it) },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withFontScale(value.toDouble()) }
                }
            )

            Text(stringResource(R.string.settings_font_family), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = ReaderFontFamily.entries,
                selected = appearance.fontFamily,
                label = { family ->
                    when (family) {
                        ReaderFontFamily.PUBLISHER -> stringResource(R.string.settings_book_default)
                        ReaderFontFamily.SERIF -> stringResource(R.string.settings_font_serif)
                        ReaderFontFamily.SANS_SERIF -> stringResource(R.string.settings_font_sans)
                        ReaderFontFamily.MONOSPACE -> stringResource(R.string.settings_font_mono)
                        ReaderFontFamily.OPEN_DYSLEXIC -> stringResource(R.string.settings_font_opendyslexic)
                        ReaderFontFamily.ACCESSIBLE_DFA -> stringResource(R.string.settings_font_accessible)
                        ReaderFontFamily.IA_WRITER_DUOSPACE -> stringResource(R.string.settings_font_duospace)
                    }
                },
                onSelected = { family ->
                    commitReaderAppearance { current -> current.withFontFamily(family) }
                }
            )
            ReaderOptionalSlider(
                label = stringResource(R.string.settings_font_weight),
                value = appearance.fontWeight,
                defaultValue = 1f,
                valueRange = 0f..2.5f,
                displayValue = { formatPercent(it) },
                onCommit = { value ->
                    commitReaderAppearance { current ->
                        current.withFontWeight(value?.toDouble()).let { updated ->
                            if (value == null) updated else updated.copy(publisherStyles = false)
                        }
                    }
                }
            )
            Text(
                stringResource(R.string.settings_font_weight_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            ReaderSlider(
                label = stringResource(R.string.settings_line_height),
                value = appearance.lineHeight.toFloat(),
                valueRange = 1.1f..2.0f,
                displayValue = { String.format(Locale.US, "%.2f×", it) },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withLineHeight(value.toDouble()) }
                }
            )
            ReaderSlider(
                label = stringResource(R.string.settings_page_margins),
                value = appearance.pageMargins.toFloat(),
                valueRange = 0.5f..2.0f,
                displayValue = { String.format(Locale.US, "%.2f×", it) },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withPageMargins(value.toDouble()) }
                }
            )

            Text(stringResource(R.string.settings_text_alignment), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = ReaderTextAlignment.entries,
                selected = appearance.textAlignment,
                label = { alignment ->
                    when (alignment) {
                        ReaderTextAlignment.PUBLISHER -> stringResource(R.string.settings_book_default)
                        ReaderTextAlignment.START -> stringResource(R.string.settings_align_start)
                        ReaderTextAlignment.JUSTIFY -> stringResource(R.string.settings_align_justify)
                        ReaderTextAlignment.CENTER -> stringResource(R.string.settings_align_center)
                    }
                },
                onSelected = { alignment ->
                    commitReaderAppearance { current -> current.withTextAlignment(alignment) }
                }
            )

            Text(stringResource(R.string.settings_columns), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = ReaderColumnMode.entries,
                selected = appearance.columnMode,
                label = { mode ->
                    when (mode) {
                        ReaderColumnMode.AUTO -> stringResource(R.string.settings_column_auto)
                        ReaderColumnMode.ONE -> stringResource(R.string.settings_column_one)
                        ReaderColumnMode.TWO -> stringResource(R.string.settings_column_two)
                    }
                },
                onSelected = { mode ->
                    commitReaderAppearance { current ->
                        current.copy(
                            columnMode = mode,
                            publisherStyles = if (mode == ReaderColumnMode.AUTO) {
                                current.publisherStyles
                            } else {
                                false
                            }
                        )
                    }
                }
            )

            Text(stringResource(R.string.settings_reading_motion), style = MaterialTheme.typography.labelLarge)
            ReaderMotionSelector(
                selected = appearance.navigationMode,
                onSelect = { mode ->
                    commitReaderAppearance { current -> current.withNavigationMode(mode) }
                }
            )
            Text(
                localizedReaderNavigationModeDescription(appearance.navigationMode),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_publisher_styles),
                subtitle = stringResource(R.string.settings_publisher_styles_description),
                checked = appearance.publisherStyles,
                onCheckedChange = { enabled ->
                    commitReaderAppearance { current -> current.copy(publisherStyles = enabled) }
                }
            )

            if (appearance.theme == ReaderTheme.PAPER || appearance.theme == ReaderTheme.SEPIA) {
                ReaderSlider(
                    label = stringResource(R.string.settings_paper_age),
                    value = appearance.paperPatina.toFloat(),
                    valueRange = 0f..1f,
                    displayValue = { value -> formatPercent(value) },
                    onCommit = { value ->
                        commitReaderAppearance { current ->
                            current.withPaperPatina(value.toDouble())
                        }
                    }
                )
                Text(
                    stringResource(R.string.settings_paper_age_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val darkReadingTheme =
                appearance.theme == ReaderTheme.DUSK || appearance.theme == ReaderTheme.OLED
            Text(stringResource(R.string.settings_dark_images), style = MaterialTheme.typography.labelLarge)
            if (darkReadingTheme) {
                ChoiceRow(
                    entries = ReaderDarkImageTreatment.entries,
                    selected = appearance.darkImageTreatment,
                    label = { treatment ->
                        when (treatment) {
                            ReaderDarkImageTreatment.NONE -> stringResource(R.string.settings_dark_images_original)
                            ReaderDarkImageTreatment.DARKEN -> stringResource(R.string.settings_dark_images_darken)
                            ReaderDarkImageTreatment.INVERT -> stringResource(R.string.settings_dark_images_invert)
                        }
                    },
                    onSelected = { treatment ->
                        commitReaderAppearance { current ->
                            current.withDarkImageTreatment(treatment)
                        }
                    }
                )
                Text(
                    stringResource(R.string.settings_dark_images_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    stringResource(R.string.settings_dark_images_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(stringResource(R.string.settings_brightness), style = MaterialTheme.typography.labelLarge)
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
            title = stringResource(R.string.settings_sound_title),
            description = stringResource(R.string.settings_sound_description)
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_haptics),
                subtitle = stringResource(R.string.settings_haptics_description),
                checked = settings.sensory.hapticsEnabled,
                onCheckedChange = { enabled ->
                    onSaveSensorySettings(
                        settings.sensory.copy(hapticsEnabled = enabled)
                    )
                }
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_interaction_sounds),
                subtitle = stringResource(R.string.settings_interaction_sounds_description),
                checked = settings.sensory.interactionSoundsEnabled,
                onCheckedChange = { enabled ->
                    onSaveSensorySettings(
                        settings.sensory.copy(interactionSoundsEnabled = enabled)
                    )
                }
            )

            Text(stringResource(R.string.settings_ambient_room), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = AmbientSound.entries,
                selected = settings.sensory.ambientSound,
                label = { mode ->
                    when (mode) {
                        AmbientSound.OFF -> stringResource(R.string.settings_ambient_off)
                        AmbientSound.LIBRARY -> stringResource(R.string.settings_ambient_library)
                        AmbientSound.RAIN -> stringResource(R.string.settings_ambient_rain)
                        AmbientSound.FIRE -> stringResource(R.string.settings_ambient_fire)
                    }
                },
                onSelected = { mode ->
                    onSaveSensorySettings(
                        settings.sensory.copy(ambientSound = mode)
                    )
                }
            )

            if (
                settings.sensory.interactionSoundsEnabled ||
                settings.sensory.ambientSound != AmbientSound.OFF
            ) {
                ReaderSlider(
                    label = stringResource(R.string.settings_audio_level),
                    value = settings.sensory.audioVolume.toFloat(),
                    valueRange = 0.05f..0.55f,
                    displayValue = { formatPercent(it) },
                    onCommit = { value ->
                        onSaveSensorySettings(
                            settings.sensory.copy(audioVolume = value.toDouble())
                        )
                    }
                )
            }

            Text(
                stringResource(R.string.settings_audio_silence_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_library_backup_title),
            description = stringResource(R.string.settings_library_backup_description)
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
            ) { Text(stringResource(if (exporting) R.string.settings_exporting else R.string.settings_export_backup)) }
            OutlinedButton(
                enabled = !exporting && !restoring,
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(stringResource(if (restoring) R.string.settings_restoring else R.string.settings_restore_backup)) }
            OutlinedButton(
                enabled = !exporting && !restoring,
                onClick = { notesPicker.launch("veil-reader-notebook.md") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.settings_export_notebook)) }
        }

        SettingsSection(
            title = stringResource(R.string.settings_privacy_title),
            description = stringResource(R.string.settings_privacy_description)
        ) {
            Text(stringResource(R.string.settings_app_version, appVersion), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.settings_reader_engine), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.settings_no_account),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_reset_title),
            description = stringResource(R.string.settings_reset_description)
        ) {
            OutlinedButton(
                onClick = { commitReaderAppearance { ReaderAppearance() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.settings_reset_button))
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
                        stringResource(R.string.settings_restore_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.settings_restore_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Text(
                    stringResource(R.string.settings_restore_warning)
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(stringResource(R.string.common_cancel)) }
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
                ) { Text(stringResource(R.string.settings_choose_backup)) }
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
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entries.forEach { entry ->
            val active = entry == selected
            Surface(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = active,
                        role = Role.RadioButton
                    ) { onSelected(entry) },
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
private fun ReaderOptionalSlider(
    label: String,
    value: Double?,
    defaultValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: (Float) -> String,
    onCommit: (Double?) -> Unit
) {
    var draft by remember(value, defaultValue) {
        mutableFloatStateOf(
            (value?.toFloat() ?: defaultValue)
                .coerceIn(valueRange.start, valueRange.endInclusive)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (value == null) stringResource(R.string.settings_book_default)
                    else displayValue(draft),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                TextButton(
                    onClick = {
                        draft = defaultValue.coerceIn(valueRange.start, valueRange.endInclusive)
                        onCommit(null)
                    },
                    enabled = value != null,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.reader_value_reset))
                }
            }
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onCommit(draft.toDouble()) },
            valueRange = valueRange,
            modifier = Modifier.semantics { contentDescription = label }
        )
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
            valueRange = valueRange,
            modifier = Modifier.semantics { contentDescription = label }
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
            modifier = Modifier.semantics { contentDescription = title },
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = VeilPalette.Moon,
                checkedTrackColor = VeilPalette.DeepBrass,
                checkedBorderColor = VeilPalette.Brass
            )
        )
    }
}
