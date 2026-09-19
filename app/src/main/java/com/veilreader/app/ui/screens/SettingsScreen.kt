package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.data.settings.AppThemeMode
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.VeilBrandMark
import com.veilreader.app.ui.theme.VeilSpacing

@Composable
fun SettingsScreen(
    initialAppearance: ReaderAppearance,
    appThemeMode: AppThemeMode,
    exporting: Boolean,
    restoring: Boolean,
    onAppearanceChange: (ReaderAppearance) -> Unit,
    onAppThemeModeChange: (AppThemeMode) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onExportNotes: (Uri) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { it?.let(onExportBackup) }
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { it?.let(onRestoreBackup) }
    val notesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { it?.let(onExportNotes) }

    var appearance by remember(initialAppearance) { mutableStateOf(initialAppearance) }
    var confirmRestore by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = VeilSpacing.lg)
                .padding(top = VeilSpacing.md, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
        ) {
            SettingsTopBar(onBack = onBack)

            SettingsHero(appearance = appearance)

            SettingsSection(
                eyebrow = stringResource(R.string.settings_section_app),
                title = stringResource(R.string.settings_interface_appearance),
                subtitle = stringResource(R.string.settings_interface_appearance_subtitle)
            ) {
                ThemeModeSelector(
                    selected = appThemeMode,
                    onSelect = onAppThemeModeChange
                )
            }

            SettingsSection(
                eyebrow = stringResource(R.string.settings_section_reading),
                title = stringResource(R.string.settings_reading_experience),
                subtitle = stringResource(R.string.settings_reading_experience_subtitle)
            ) {
                ReadingAppearanceControls(
                    appearance = appearance,
                    onChange = { next ->
                        appearance = next
                        onAppearanceChange(next)
                    }
                )
            }

            SettingsSection(
                eyebrow = stringResource(R.string.settings_section_library),
                title = stringResource(R.string.settings_data_portability),
                subtitle = stringResource(R.string.settings_data_portability_subtitle)
            ) {
                SettingsActionRow(
                    title = stringResource(R.string.settings_export_backup),
                    subtitle = stringResource(R.string.settings_export_backup_subtitle),
                    action = if (exporting) {
                        stringResource(R.string.action_exporting)
                    } else {
                        stringResource(R.string.action_export)
                    },
                    enabled = !exporting && !restoring,
                    onClick = { backupPicker.launch("veil-reader-backup.zip") }
                )
                SettingsDivider()
                SettingsActionRow(
                    title = stringResource(R.string.settings_restore_backup),
                    subtitle = stringResource(R.string.settings_restore_backup_subtitle),
                    action = if (restoring) {
                        stringResource(R.string.action_restoring)
                    } else {
                        stringResource(R.string.action_restore)
                    },
                    enabled = !exporting && !restoring,
                    onClick = { confirmRestore = true }
                )
                SettingsDivider()
                SettingsActionRow(
                    title = stringResource(R.string.settings_export_notebook),
                    subtitle = stringResource(R.string.settings_export_notebook_subtitle),
                    action = stringResource(R.string.action_export),
                    enabled = !exporting && !restoring,
                    onClick = { notesPicker.launch("veil-reader-notebook.md") }
                )
            }

            SettingsSection(
                eyebrow = stringResource(R.string.settings_section_privacy),
                title = stringResource(R.string.settings_local_by_default),
                subtitle = stringResource(R.string.settings_local_by_default_subtitle)
            ) {
                InfoRow(
                    badge = "01",
                    title = stringResource(R.string.settings_privacy_library_title),
                    body = stringResource(R.string.settings_privacy_library_body)
                )
                SettingsDivider()
                InfoRow(
                    badge = "02",
                    title = stringResource(R.string.settings_privacy_exports_title),
                    body = stringResource(R.string.settings_privacy_exports_body)
                )
                SettingsDivider()
                InfoRow(
                    badge = "03",
                    title = stringResource(R.string.settings_privacy_reader_core_title),
                    body = stringResource(R.string.settings_privacy_reader_core_body)
                )
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(stringResource(R.string.settings_restore_dialog_title)) },
            text = {
                Text(stringResource(R.string.settings_restore_dialog_body))
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRestore = false
                        restorePicker.launch(arrayOf("application/zip", "application/octet-stream"))
                    }
                ) { Text(stringResource(R.string.action_choose_backup)) }
            }
        )
    }
}
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.action_back))
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ) {
            Text(
                stringResource(R.string.settings),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsHero(
    appearance: ReaderAppearance
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        )
    ) {
        Column(
            Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            VeilBrandMark(showWordmark = true)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    stringResource(R.string.settings_hero_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    stringResource(R.string.settings_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                SettingsStatusPill(
                    label = themeLabel(appearance.theme),
                    modifier = Modifier.weight(1f)
                )
                SettingsStatusPill(
                    label = if (appearance.scroll) {
                        stringResource(R.string.reader_mode_scroll)
                    } else {
                        stringResource(R.string.reader_mode_pages)
                    },
                    modifier = Modifier.weight(1f)
                )
                SettingsStatusPill(
                    label = stringResource(
                        R.string.settings_text_percent,
                        (appearance.fontScale * 100).toInt()
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingsStatusPill(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsSection(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            )
        ) {
            Column(
                Modifier.padding(VeilSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                content = content
            )
        }
    }
}
@Composable
private fun ThemeModeSelector(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
    ) {
        AppThemeMode.entries.forEach { mode ->
            val label = when (mode) {
                AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                AppThemeMode.DARK -> stringResource(R.string.theme_dark)
            }
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(label) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
        }
    }
    Text(
        when (selected) {
            AppThemeMode.SYSTEM -> stringResource(R.string.theme_system_description)
            AppThemeMode.LIGHT -> stringResource(R.string.theme_light_description)
            AppThemeMode.DARK -> stringResource(R.string.theme_dark_description)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.heightIn(min = 46.dp)
        ) {
            Text(action)
        }
    }
}

@Composable
private fun InfoRow(
    badge: String,
    title: String,
    body: String
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
    )
}

@Composable
private fun themeLabel(theme: ReaderTheme): String = when (theme) {
    ReaderTheme.PAPER -> stringResource(R.string.reader_theme_paper)
    ReaderTheme.SEPIA -> stringResource(R.string.reader_theme_sepia)
    ReaderTheme.DUSK -> stringResource(R.string.reader_theme_dusk)
    ReaderTheme.OLED -> stringResource(R.string.reader_theme_oled)
}
