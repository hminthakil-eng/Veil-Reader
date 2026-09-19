package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.ui.theme.VeilSpacing

@Composable
fun SettingsScreen(
    initialAppearance: ReaderAppearance,
    exporting: Boolean,
    restoring: Boolean,
    onAppearanceChange: (ReaderAppearance) -> Unit,
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("‹ Back to profile")
        }

        ScreenHeader(
            eyebrow = "Preferences",
            title = "Settings",
            subtitle = "Reading defaults, local data, privacy and app information."
        )

        MysteryCard(Modifier.fillMaxWidth()) {
            ReadingAppearanceControls(
                appearance = appearance,
                onChange = { next ->
                    appearance = next
                    onAppearanceChange(next)
                }
            )
        }

        SettingsHeading("Data & backup")
        Text(
            "Backups include imported books, reading positions, annotations, Path progress, quests and Castle identity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            enabled = !exporting && !restoring,
            onClick = { backupPicker.launch("veil-reader-backup.zip") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(if (exporting) "Exporting…" else "Export library backup")
        }
        OutlinedButton(
            enabled = !exporting && !restoring,
            onClick = { confirmRestore = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(if (restoring) "Restoring…" else "Restore library backup")
        }
        OutlinedButton(
            enabled = !exporting && !restoring,
            onClick = { notesPicker.launch("veil-reader-notebook.md") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text("Export notebook as Markdown")
        }

        SettingsHeading("Privacy & about")
        MysteryCard(Modifier.fillMaxWidth()) {
            Text("Local by default", style = MaterialTheme.typography.titleMedium)
            Text(
                "Your library, reading progress and annotations stay on this device unless you explicitly export a backup or notebook.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Core reader · Readium Kotlin Toolkit 3.4",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace local Veil Reader data?") },
            text = {
                Text(
                    "Restore replaces your current library, annotations, reading progress, Path progress and Castle state with the selected backup. Export a fresh backup first if you need the current state."
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(onClick = {
                    confirmRestore = false
                    restorePicker.launch(arrayOf("application/zip", "application/octet-stream"))
                }) { Text("Choose backup") }
            }
        )
    }
}

@Composable
private fun SettingsHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}
