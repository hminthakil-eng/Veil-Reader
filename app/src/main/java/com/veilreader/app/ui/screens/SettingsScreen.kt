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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    val surface = MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        surface.copy(alpha = 0.96f)
                    )
                )
            )
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
                eyebrow = "App",
                title = "Interface appearance",
                subtitle = "Choose how Veil Reader itself follows light and dark mode."
            ) {
                ThemeModeSelector(
                    selected = appThemeMode,
                    onSelect = onAppThemeModeChange
                )
            }

            SettingsSection(
                eyebrow = "Reading",
                title = "Reading experience",
                subtitle = "Typography, page layout and persistent EPUB defaults."
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
                eyebrow = "Library",
                title = "Data & portability",
                subtitle = "Your library stays local until you explicitly export it."
            ) {
                SettingsActionRow(
                    title = "Export library backup",
                    subtitle = "Books, positions, annotations, Path and Castle state.",
                    action = if (exporting) "Exporting…" else "Export",
                    enabled = !exporting && !restoring,
                    onClick = { backupPicker.launch("veil-reader-backup.zip") }
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Restore backup",
                    subtitle = "Replace the local library with a Veil backup.",
                    action = if (restoring) "Restoring…" else "Restore",
                    enabled = !exporting && !restoring,
                    onClick = { confirmRestore = true }
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Export notebook",
                    subtitle = "Highlights and notes as portable Markdown.",
                    action = "Export",
                    enabled = !exporting && !restoring,
                    onClick = { notesPicker.launch("veil-reader-notebook.md") }
                )
            }

            SettingsSection(
                eyebrow = "Privacy",
                title = "Local by default",
                subtitle = "No account is required for the core reading experience."
            ) {
                InfoRow(
                    badge = "01",
                    title = "Library",
                    body = "Imported publications, reading progress and annotations remain on this device."
                )
                SettingsDivider()
                InfoRow(
                    badge = "02",
                    title = "Exports",
                    body = "Files leave the app only when you choose an export destination."
                )
                SettingsDivider()
                InfoRow(
                    badge = "03",
                    title = "Reader core",
                    body = "Readium Kotlin Toolkit 3.4 · offline-first reader foundation."
                )
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace local Veil Reader data?") },
            text = {
                Text(
                    "Restore replaces your current library, annotations, reading progress, Path progress and Castle state. Export a fresh backup first if you need the current state."
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
                    }
                ) { Text("Choose backup") }
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
            Text("‹ Back")
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ) {
            Text(
                "SETTINGS",
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
        )
    ) {
        Column(
            Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            VeilBrandMark(showWordmark = true)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Your reading environment",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Calm defaults, explicit data controls and one place for every reader preference.",
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
                    label = if (appearance.scroll) "Scroll" else "Pages",
                    modifier = Modifier.weight(1f)
                )
                SettingsStatusPill(
                    label = ((appearance.fontScale * 100).toInt().toString() + "% text"),
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
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f)
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
                eyebrow.uppercase(),
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
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)
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
                AppThemeMode.SYSTEM -> "System"
                AppThemeMode.LIGHT -> "Light"
                AppThemeMode.DARK -> "Dark"
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
            AppThemeMode.SYSTEM -> "Veil follows your device appearance."
            AppThemeMode.LIGHT -> "The app shell stays in the warm paper palette."
            AppThemeMode.DARK -> "The app shell stays in the low-glare dark palette."
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
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)),
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

private fun themeLabel(theme: ReaderTheme): String = when (theme) {
    ReaderTheme.PAPER -> "Paper"
    ReaderTheme.SEPIA -> "Sepia"
    ReaderTheme.DUSK -> "Dusk"
    ReaderTheme.OLED -> "OLED"
}
