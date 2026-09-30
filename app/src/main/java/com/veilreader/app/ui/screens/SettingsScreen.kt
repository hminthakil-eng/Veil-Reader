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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.data.settings.AmbientSound
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderLayoutMode
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere
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
    val context = LocalContext.current
    val unknownVersion = stringResource(R.string.settings_unknown)
    val appVersion = remember(context, unknownVersion) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("").ifBlank { unknownVersion }
    }
    val appThemeLabels = mapOf(
        AppThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
        AppThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
        AppThemeMode.DARK to stringResource(R.string.settings_theme_dark)
    )
    val readerThemeLabels = mapOf(
        ReaderTheme.PAPER to stringResource(R.string.reader_theme_paper),
        ReaderTheme.SEPIA to stringResource(R.string.reader_theme_sepia),
        ReaderTheme.DUSK to stringResource(R.string.reader_theme_dusk),
        ReaderTheme.OLED to stringResource(R.string.reader_theme_night)
    )
    val ambientSoundLabels = mapOf(
        AmbientSound.OFF to stringResource(R.string.settings_ambient_off),
        AmbientSound.LIBRARY to stringResource(R.string.settings_ambient_library),
        AmbientSound.RAIN to stringResource(R.string.settings_ambient_rain),
        AmbientSound.FIRE to stringResource(R.string.settings_ambient_fire)
    )
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
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = appVersion.hashCode(),
                intensity = 0.46f
            )
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
                stringResource(R.string.settings_grayfog_title),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
            Text(
                stringResource(R.string.settings_reading_room),
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
            description = stringResource(R.string.settings_appearance_body)
        ) {
            ChoiceRow(
                entries = AppThemeMode.entries,
                selected = settings.appThemeMode,
                label = { appThemeLabels.getValue(it) },
                onSelected = onSetAppThemeMode
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_reading_title),
            description = stringResource(R.string.settings_reading_body)
        ) {
            Text(stringResource(R.string.settings_publication_theme), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = ReaderTheme.entries,
                selected = appearance.theme,
                label = { readerThemeLabels.getValue(it) },
                onSelected = { theme ->
                    commitReaderAppearance { current -> current.withTheme(theme) }
                }
            )

            ReaderSlider(
                label = stringResource(R.string.settings_text_size),
                value = appearance.fontScale.toFloat(),
                valueRange = 0.75f..1.8f,
                displayValue = { "${(it * 100).toInt()}%" },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withFontScale(value.toDouble()) }
                }
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

            Text(
                stringResource(R.string.reader_flow_mode),
                style = MaterialTheme.typography.labelLarge
            )
            ReaderFlowSelector(
                selected = appearance.layoutMode,
                onSelect = { mode ->
                    commitReaderAppearance { current -> current.withLayoutMode(mode) }
                }
            )
            Text(
                if (appearance.layoutMode == ReaderLayoutMode.PAGED) {
                    stringResource(R.string.reader_flow_paged_description)
                } else {
                    stringResource(R.string.reader_mode_scroll_description)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                stringResource(R.string.reader_page_turn_effect),
                style = MaterialTheme.typography.labelLarge
            )
            ReaderPageTurnSelector(
                selected = appearance.pageTurnStyle,
                enabled = appearance.layoutMode == ReaderLayoutMode.PAGED,
                onSelect = { style ->
                    commitReaderAppearance { current -> current.withPageTurnStyle(style) }
                }
            )
            Text(
                if (appearance.layoutMode == ReaderLayoutMode.SCROLL) {
                    stringResource(R.string.reader_turn_inactive_scroll)
                } else {
                    when (appearance.pageTurnStyle) {
                        com.veilreader.app.domain.PageTurnStyle.PAPER ->
                            stringResource(R.string.reader_mode_curl_description)
                        com.veilreader.app.domain.PageTurnStyle.SLIDE ->
                            stringResource(R.string.reader_mode_slide_description)
                        com.veilreader.app.domain.PageTurnStyle.NONE ->
                            stringResource(R.string.reader_mode_paged_description)
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_publisher_styles),
                subtitle = stringResource(R.string.settings_publisher_styles_body),
                checked = appearance.publisherStyles,
                onCheckedChange = { enabled ->
                    commitReaderAppearance { current -> current.copy(publisherStyles = enabled) }
                }
            )

            Text(stringResource(R.string.settings_reading_brightness), style = MaterialTheme.typography.labelLarge)
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
            title = stringResource(R.string.settings_sound_touch_title),
            description = stringResource(R.string.settings_sound_touch_body)
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_haptic_feedback),
                subtitle = stringResource(R.string.settings_haptic_feedback_body),
                checked = settings.sensory.hapticsEnabled,
                onCheckedChange = { enabled ->
                    onSaveSensorySettings(
                        settings.sensory.copy(hapticsEnabled = enabled)
                    )
                }
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_interaction_sounds),
                subtitle = stringResource(R.string.settings_interaction_sounds_body),
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
                label = { mode -> ambientSoundLabels.getValue(mode) },
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
                    displayValue = { "${(it * 100).toInt()}%" },
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
            description = stringResource(R.string.settings_library_backup_body)
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
            ) { Text(if (exporting) stringResource(R.string.settings_exporting) else stringResource(R.string.settings_export_backup)) }
            OutlinedButton(
                enabled = !exporting && !restoring,
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (restoring) stringResource(R.string.settings_restoring) else stringResource(R.string.settings_restore_backup)) }
            OutlinedButton(
                enabled = !exporting && !restoring,
                onClick = { notesPicker.launch("veil-reader-notebook.md") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.settings_export_markdown)) }
        }

        SettingsSection(
            title = stringResource(R.string.settings_privacy_about_title),
            description = stringResource(R.string.settings_privacy_about_body)
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
            description = stringResource(R.string.settings_reset_body)
        ) {
            OutlinedButton(
                onClick = { commitReaderAppearance { ReaderAppearance() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.settings_reset_reader_defaults))
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
                    stringResource(R.string.settings_restore_body)
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
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

    Surface(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (expanded) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
        },
        border = BorderStroke(
            1.dp,
            if (expanded) {
                VeilPalette.Brass.copy(alpha = 0.48f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
            }
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = VeilSpacing.md,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!expanded) {
                        Text(
                            description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    if (expanded) "−" else "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Brass
                )
            }

            if (expanded) {
                BrassRule(Modifier.fillMaxWidth())
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                ) {
                    content()
                }
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
