package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LibraryDataControls(
    exporting: Boolean,
    restoring: Boolean,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onExportNotes: (Uri) -> Unit
) {
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(onExportBackup) }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onRestoreBackup) }
    val notesPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { it?.let(onExportNotes) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your books. Your data.", style = MaterialTheme.typography.headlineSmall)
        Text("Your library lives on this device. Save a backup somewhere you can find it before changing phones or reinstalling Veil.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MysteryCard(Modifier.fillMaxWidth()) {
            Text("A whole-library backup", style = MaterialTheme.typography.titleMedium)
            Text("Includes book files, reading positions, highlights, notes, bookmarks, appearance, quiet mode, quests, and your home design.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(enabled = !exporting && !restoring,
            onClick = { backupPicker.launch("veil-reader-backup.zip") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (exporting) "Exporting…" else "Export library backup")
        }
        OutlinedButton(enabled = !exporting && !restoring, onClick = { confirmRestore = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (restoring) "Restoring…" else "Restore library backup")
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        Text("Take your notes anywhere", style = MaterialTheme.typography.titleLarge)
        Text("Export your highlights and notes as a readable Markdown notebook.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(enabled = !exporting && !restoring,
            onClick = { notesPicker.launch("veil-reader-notebook.md") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Export notebook as Markdown")
        }
        Text("No account is needed. Veil does not upload your books or automatically sync this backup.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (confirmRestore) {
        AlertDialog(onDismissRequest = { confirmRestore = false },
            title = { Text("Replace this library?") },
            text = { Text("The selected backup will replace your current books, notes, reading progress, settings, and castle. Export a fresh backup first if you want to keep the current state.") },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
            confirmButton = { Button(onClick = {
                confirmRestore = false
                restorePicker.launch(arrayOf("application/zip", "application/octet-stream"))
            }) { Text("Choose backup") } })
    }
}
