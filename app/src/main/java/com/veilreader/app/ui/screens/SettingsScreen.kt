package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.data.settings.AmbientSound
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = 0.34f,
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.08f),
                        0.42f to Color.Transparent,
                        0.78f to VeilPalette.Ink.copy(alpha = 0.72f),
                        1f to VeilPalette.Ink
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
        SettingsMasthead(onClose = onClose)

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

            Text("Reading motion", style = MaterialTheme.typography.labelLarge)
            ReaderMotionSelector(
                selected = appearance.navigationMode,
                onSelect = { mode ->
                    commitReaderAppearance { current -> current.withNavigationMode(mode) }
                }
            )
            Text(
                readerNavigationModeDescription(appearance.navigationMode),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
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
            title = "Sound & touch",
            description = "Keep feedback subtle, optional, and fully local. Haptics never require sound; ambient playback is off by default."
        ) {
            SettingsSwitchRow(
                title = "Haptic feedback",
                subtitle = "A restrained tactile cue for page turns, saved marks, and major unlocks.",
                checked = settings.sensory.hapticsEnabled,
                onCheckedChange = { enabled ->
                    onSaveSensorySettings(
                        settings.sensory.copy(hapticsEnabled = enabled)
                    )
                }
            )
            SettingsSwitchRow(
                title = "Interaction sounds",
                subtitle = "Soft paper and archive cues. Disabled by default and never required for reading.",
                checked = settings.sensory.interactionSoundsEnabled,
                onCheckedChange = { enabled ->
                    onSaveSensorySettings(
                        settings.sensory.copy(interactionSoundsEnabled = enabled)
                    )
                }
            )

            Text("Ambient room", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                entries = AmbientSound.entries,
                selected = settings.sensory.ambientSound,
                label = { mode ->
                    when (mode) {
                        AmbientSound.OFF -> "Off"
                        AmbientSound.LIBRARY -> "Library hush"
                        AmbientSound.RAIN -> "Rain"
                        AmbientSound.FIRE -> "Fireplace"
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
                    label = "Audio level",
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
                "Set Ambient room to Off and disable Interaction sounds for complete audio silence.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
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
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.52f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box {
                        GrayfogOrnamentFrame(
                            modifier = Modifier.matchParentSize(),
                            strength = 0.30f
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "RESTORE ARCHIVE · LOCAL",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.35.sp),
                                color = VeilPalette.Brass
                            )
                            Text(
                                "Replace local Veil Reader data?",
                                style = MaterialTheme.typography.titleLarge,
                                color = VeilPalette.Moon
                            )
                            BrassRule(Modifier.fillMaxWidth())
                            Text(
                                "Restore replaces your current library, annotations, reading progress, Path progress and Castle state with the selected backup. Export a fresh backup first if you need the current state.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { confirmRestore = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        confirmRestore = false
                                        restorePicker.launch(
                                            arrayOf("application/zip", "application/octet-stream")
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
                                    Text("Choose backup")
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
private fun SettingsMasthead(
    onClose: () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (fontScale > 1.35f) 320.dp else 250.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.58f)),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.12f),
                        0.42f to Color.Transparent,
                        1f to VeilPalette.Ink.copy(alpha = 0.96f)
                    )
                )
        )
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.82f
        )
        VeilRealmEmblem(
            realm = com.veilreader.app.ui.theme.VeilRealm.SANCTUARY,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = VeilSpacing.lg)
                .size(132.dp),
            tint = VeilPalette.Brass.copy(alpha = 0.18f)
        )

        TextButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = VeilPalette.Moon
            )
        ) {
            Text(VeilBackLabel("Back"))
        }

        Text(
            "SANCTUARY CONTROLS",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(VeilSpacing.md),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.25.sp),
            color = VeilPalette.Brass
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "The Reading Room",
                style = MaterialTheme.typography.displaySmall,
                color = VeilPalette.Moon
            )
            Text(
                "Reading, appearance, storage, privacy, and backup controls — local by default.",
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Moon.copy(alpha = 0.82f),
                modifier = Modifier.widthIn(max = 580.dp)
            )
            BrassRule(Modifier.width(138.dp), strong = true)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = Color(0xFF0D1015).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.22f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = VeilPalette.DeepBrass.copy(alpha = 0.28f),
                    shape = MaterialTheme.shapes.extraSmall
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                    color = VeilPalette.Brass,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            BrassRule(Modifier.fillMaxWidth())
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
