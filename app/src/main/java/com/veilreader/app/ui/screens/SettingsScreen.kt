package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.data.settings.AmbientSound
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderHardwareKeyAction
import com.veilreader.app.domain.ReaderHardwareKeyMap
import com.veilreader.app.domain.ReaderTapAction
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.ReaderTapZone
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.withVeilTracking

@Composable
fun SettingsScreen(
    settings: AppSettings,
    exporting: Boolean,
    restoring: Boolean,
    onSetAppThemeMode: (AppThemeMode) -> Unit,
    onSetHighContrastEnabled: (Boolean) -> Unit,
    onSaveReaderAppearance: (ReaderAppearance) -> Unit,
    onSaveReaderTapGrid: (ReaderTapGrid) -> Unit,
    onSaveReaderHardwareKeys: (ReaderHardwareKeyMap) -> Unit,
    onSaveSensorySettings: (SensorySettings) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onExportNotes: (Uri) -> Unit,
    onClose: () -> Unit
) {
    var appearanceDraft by remember { mutableStateOf(settings.readerAppearance) }
    var pendingAppearance by remember { mutableStateOf<ReaderAppearance?>(null) }
    var tapGridDraft by remember { mutableStateOf(settings.readerTapGrid) }
    var pendingTapGrid by remember { mutableStateOf<ReaderTapGrid?>(null) }
    var hardwareKeysDraft by remember { mutableStateOf(settings.readerHardwareKeys) }
    var pendingHardwareKeys by remember { mutableStateOf<ReaderHardwareKeyMap?>(null) }

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

    LaunchedEffect(settings.readerTapGrid) {
        val persisted = settings.readerTapGrid
        when {
            pendingTapGrid == null -> tapGridDraft = persisted
            persisted == pendingTapGrid -> {
                tapGridDraft = persisted
                pendingTapGrid = null
            }
        }
    }

    LaunchedEffect(settings.readerHardwareKeys) {
        val persisted = settings.readerHardwareKeys
        when {
            pendingHardwareKeys == null -> hardwareKeysDraft = persisted
            persisted == pendingHardwareKeys -> {
                hardwareKeysDraft = persisted
                pendingHardwareKeys = null
            }
        }
    }

    fun commitReaderHardwareKeys(value: ReaderHardwareKeyMap) {
        hardwareKeysDraft = value
        pendingHardwareKeys = value
        onSaveReaderHardwareKeys(value)
    }

    fun commitReaderTapGrid(value: ReaderTapGrid) {
        tapGridDraft = value
        pendingTapGrid = value
        onSaveReaderTapGrid(value)
    }

    fun commitReaderAppearance(transform: (ReaderAppearance) -> ReaderAppearance) {
        val value = transform(appearanceDraft)
        appearanceDraft = value
        pendingAppearance = value
        onSaveReaderAppearance(value)
    }

    val appearance = appearanceDraft
    val highContrast = LocalVeilHighContrast.current
    val shellAccent = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
    val formatPercent = rememberVeilPercentFormatter()
    val formatNumber = rememberVeilNumberFormatter()
    val context = LocalContext.current
    val unknownVersion = stringResource(R.string.settings_unknown)
    val appVersion = remember(context, unknownVersion) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("").ifBlank { unknownVersion }
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = if (highContrast) 0.07f else 0.18f,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(
                    Brush.verticalGradient(
                        if (highContrast) {
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.76f),
                                MaterialTheme.colorScheme.background
                            )
                        } else {
                            listOf(
                                VeilPalette.Ink.copy(alpha = 0.16f),
                                VeilPalette.Ink.copy(alpha = 0.62f),
                                VeilPalette.Ink
                            )
                        }
                    )
                )
        )

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
                color = shellAccent
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
            SettingsSwitchRow(
                title = stringResource(R.string.settings_high_contrast),
                subtitle = stringResource(R.string.settings_high_contrast_description),
                checked = settings.highContrastEnabled,
                onCheckedChange = onSetHighContrastEnabled
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
                displayValue = { "${formatNumber(it)}×" },
                onCommit = { value ->
                    commitReaderAppearance { current -> current.withLineHeight(value.toDouble()) }
                }
            )
            ReaderSlider(
                label = stringResource(R.string.settings_page_margins),
                value = appearance.pageMargins.toFloat(),
                valueRange = 0.5f..2.0f,
                displayValue = { "${formatNumber(it)}×" },
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
            title = stringResource(R.string.settings_tap_matrix_title),
            description = stringResource(R.string.settings_tap_matrix_description)
        ) {
            ReaderTapGridEditor(
                grid = tapGridDraft,
                onChange = ::commitReaderTapGrid
            )
            Text(
                stringResource(R.string.settings_tap_matrix_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = { commitReaderTapGrid(ReaderTapGrid()) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.settings_tap_matrix_reset))
            }
        }

        SettingsSection(
            title = stringResource(R.string.settings_hardware_keys_title),
            description = stringResource(R.string.settings_hardware_keys_description)
        ) {
            Text(
                stringResource(R.string.settings_volume_up_key),
                style = MaterialTheme.typography.labelLarge
            )
            ChoiceRow(
                entries = ReaderHardwareKeyAction.entries,
                selected = hardwareKeysDraft.volumeUp,
                label = { localizedHardwareKeyAction(it) },
                onSelected = { action ->
                    commitReaderHardwareKeys(
                        hardwareKeysDraft.copy(volumeUp = action)
                    )
                }
            )

            Text(
                stringResource(R.string.settings_volume_down_key),
                style = MaterialTheme.typography.labelLarge
            )
            ChoiceRow(
                entries = ReaderHardwareKeyAction.entries,
                selected = hardwareKeysDraft.volumeDown,
                label = { localizedHardwareKeyAction(it) },
                onSelected = { action ->
                    commitReaderHardwareKeys(
                        hardwareKeysDraft.copy(volumeDown = action)
                    )
                }
            )

            Text(
                stringResource(R.string.settings_hardware_keys_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    commitReaderHardwareKeys(ReaderHardwareKeyMap())
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.settings_hardware_keys_reset))
            }
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

    }

    if (confirmRestore) {
        Dialog(
            onDismissRequest = { confirmRestore = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = VeilPalette.Archive,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.52f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box {
                        GrayfogOrnamentFrame(
                            modifier = Modifier.matchParentSize(),
                            strength = 0.30f
                        )
                        Column(
                            modifier = Modifier.padding(
                                horizontal = VeilSpacing.lg,
                                vertical = VeilSpacing.lg
                            ),
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                        ) {
                            Text(
                                stringResource(R.string.settings_restore_eyebrow),
                                style = MaterialTheme.typography.labelSmall,
                                color = VeilPalette.Brass
                            )
                            Text(
                                stringResource(R.string.settings_restore_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = VeilPalette.Moon
                            )
                            BrassRule(Modifier.fillMaxWidth())
                            Text(
                                stringResource(R.string.settings_restore_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                            ) {
                                OutlinedButton(
                                    onClick = { confirmRestore = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                                Button(
                                    onClick = {
                                        confirmRestore = false
                                        restorePicker.launch(
                                            arrayOf(
                                                "application/zip",
                                                "application/octet-stream"
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VeilPalette.Brass,
                                        contentColor = androidx.compose.ui.graphics.Color(0xFF17120A)
                                    )
                                ) {
                                    Text(stringResource(R.string.settings_choose_backup))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedHardwareKeyAction(action: ReaderHardwareKeyAction): String =
    stringResource(
        when (action) {
            ReaderHardwareKeyAction.SYSTEM -> R.string.settings_hardware_action_system
            ReaderHardwareKeyAction.PREVIOUS_PAGE -> R.string.settings_hardware_action_previous
            ReaderHardwareKeyAction.NEXT_PAGE -> R.string.settings_hardware_action_next
            ReaderHardwareKeyAction.TOGGLE_CONTROLS -> R.string.settings_hardware_action_controls
        }
    )

@Composable
private fun ReaderTapGridEditor(
    grid: ReaderTapGrid,
    onChange: (ReaderTapGrid) -> Unit
) {
    val rows = listOf(
        listOf(ReaderTapZone.TOP_LEFT, ReaderTapZone.TOP_CENTER, ReaderTapZone.TOP_RIGHT),
        listOf(ReaderTapZone.MIDDLE_LEFT, ReaderTapZone.MIDDLE_CENTER, ReaderTapZone.MIDDLE_RIGHT),
        listOf(ReaderTapZone.BOTTOM_LEFT, ReaderTapZone.BOTTOM_CENTER, ReaderTapZone.BOTTOM_RIGHT)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                row.forEach { zone ->
                    val action = grid[zone]
                    val zoneLabel = localizedReaderTapZone(zone)
                    val actionLabel = localizedReaderTapAction(action)
                    OutlinedButton(
                        onClick = {
                            onChange(
                                grid.withAction(
                                    zone,
                                    nextReaderTapAction(action)
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 70.dp)
                            .semantics {
                                contentDescription = "$zoneLabel: $actionLabel"
                            },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 6.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                readerTapActionGlyph(action),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                zoneLabel,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun nextReaderTapAction(action: ReaderTapAction): ReaderTapAction =
    when (action) {
        ReaderTapAction.VEIL_DEFAULT -> ReaderTapAction.PREVIOUS_PAGE
        ReaderTapAction.PREVIOUS_PAGE -> ReaderTapAction.TOGGLE_CONTROLS
        ReaderTapAction.TOGGLE_CONTROLS -> ReaderTapAction.NEXT_PAGE
        ReaderTapAction.NEXT_PAGE -> ReaderTapAction.RENDERER
        ReaderTapAction.RENDERER -> ReaderTapAction.VEIL_DEFAULT
    }

private fun readerTapActionGlyph(action: ReaderTapAction): String =
    when (action) {
        ReaderTapAction.VEIL_DEFAULT -> "V"
        ReaderTapAction.PREVIOUS_PAGE -> "←"
        ReaderTapAction.TOGGLE_CONTROLS -> "◎"
        ReaderTapAction.NEXT_PAGE -> "→"
        ReaderTapAction.RENDERER -> "·"
    }

@Composable
private fun localizedReaderTapAction(action: ReaderTapAction): String =
    stringResource(
        when (action) {
            ReaderTapAction.VEIL_DEFAULT -> R.string.settings_tap_action_default
            ReaderTapAction.PREVIOUS_PAGE -> R.string.settings_tap_action_previous
            ReaderTapAction.TOGGLE_CONTROLS -> R.string.settings_tap_action_controls
            ReaderTapAction.NEXT_PAGE -> R.string.settings_tap_action_next
            ReaderTapAction.RENDERER -> R.string.settings_tap_action_renderer
        }
    )

@Composable
private fun localizedReaderTapZone(zone: ReaderTapZone): String =
    stringResource(
        when (zone) {
            ReaderTapZone.TOP_LEFT -> R.string.settings_tap_zone_top_left
            ReaderTapZone.TOP_CENTER -> R.string.settings_tap_zone_top_center
            ReaderTapZone.TOP_RIGHT -> R.string.settings_tap_zone_top_right
            ReaderTapZone.MIDDLE_LEFT -> R.string.settings_tap_zone_middle_left
            ReaderTapZone.MIDDLE_CENTER -> R.string.settings_tap_zone_middle_center
            ReaderTapZone.MIDDLE_RIGHT -> R.string.settings_tap_zone_middle_right
            ReaderTapZone.BOTTOM_LEFT -> R.string.settings_tap_zone_bottom_left
            ReaderTapZone.BOTTOM_CENTER -> R.string.settings_tap_zone_bottom_center
            ReaderTapZone.BOTTOM_RIGHT -> R.string.settings_tap_zone_bottom_right
        }
    )

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
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.withVeilTracking(title, 1.10.sp),
            color = VeilPalette.Brass
        )

        BrassRule(Modifier.fillMaxWidth())

        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeilPalette.Ink.copy(alpha = 0.12f))
                .padding(horizontal = VeilSpacing.sm, vertical = VeilSpacing.md)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawLine(
                    color = VeilPalette.BorderDark.copy(alpha = 0.46f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            Column(
                modifier = Modifier.padding(start = 6.dp),
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
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        entries.forEach { entry ->
            val active = entry == selected

            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = active,
                        role = Role.RadioButton
                    ) { onSelected(entry) }
                    .background(
                        if (active) {
                            VeilPalette.Archive.copy(alpha = 0.24f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawLine(
                        color = if (active) {
                            VeilPalette.Brass
                        } else {
                            VeilPalette.BorderDark.copy(alpha = 0.40f)
                        },
                        start = Offset(0f, size.height - 1.dp.toPx()),
                        end = Offset(size.width, size.height - 1.dp.toPx()),
                        strokeWidth = if (active) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                Text(
                    label(entry),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) {
                        VeilPalette.Brass
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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

    val valueDescription = if (value == null) {
        stringResource(R.string.settings_book_default)
    } else {
        displayValue(draft)
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
                    valueDescription,
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
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = valueDescription
            }
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
    val valueDescription = displayValue(draft)

    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                valueDescription,
                color = VeilPalette.Brass,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onCommit(draft) },
            valueRange = valueRange,
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = valueDescription
            }
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
