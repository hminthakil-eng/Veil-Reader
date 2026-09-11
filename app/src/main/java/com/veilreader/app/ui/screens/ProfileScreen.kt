package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.ReaderProfile

@Composable
fun ProfileScreen(
    profile: ReaderProfile,
    highlightCount: Int,
    exporting: Boolean,
    restoring: Boolean,
    dailyGoalMinutes: Int,
    castleTitle: String,
    equippedSigilName: String?,
    onSetDailyGoal: (Int) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onExportNotes: (Uri) -> Unit,
    onOpenArchive: () -> Unit
) {
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(onExportBackup) }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onRestoreBackup) }
    val notesPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { it?.let(onExportNotes) }
    var confirmRestore by remember { mutableStateOf(false) }
    val p = profile

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader("Reader profile", castleTitle, "${p.path.name} · ${p.rankName}")

        MysteryCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Experience", fontWeight = FontWeight.SemiBold)
                Text("${p.xp}/${p.xpForNextLevel} XP", color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { p.xp / p.xpForNextLevel.toFloat() }, modifier = Modifier.fillMaxWidth())
            equippedSigilName?.let {
                Spacer(Modifier.height(10.dp))
                Text("Equipped sigil · $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("🔥", "${p.streakDays}", "day streak", Modifier.weight(1f))
            StatCard("📚", "${p.booksFinished}", "books", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("▤", "${p.pagesRead}", "page turns", Modifier.weight(1f))
            StatCard("◷", formatMinutes(p.minutesRead), "reading", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("✦", "$highlightCount", "highlights", Modifier.weight(1f))
            StatCard("♜", "${p.rankIndex + 1}", "castle tier", Modifier.weight(1f))
        }

        Text("Daily reading goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Your first daily quest follows this target. Choose a pace that supports reading instead of turning it into a chore.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10, 20, 30, 60).forEach { minutes ->
                if (minutes == dailyGoalMinutes) {
                    Button(onClick = { onSetDailyGoal(minutes) }) { Text("${minutes}m ✓") }
                } else {
                    OutlinedButton(onClick = { onSetDailyGoal(minutes) }) { Text("${minutes}m") }
                }
            }
        }

        Text("Earned sigils", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        listOf(
            Triple("first_hour", "First Hour", p.minutesRead to 60),
            Triple("passage_keeper", "Passage Keeper", highlightCount to 10),
            Triple("seven_days", "Seven-Day Journey", p.streakDays to 7),
            Triple("ten_tomes", "Ten Tomes", p.booksFinished to 10),
            Triple("first_threshold", "First Threshold", p.rankIndex to 1)
        ).forEach { (id, name, progress) ->
            val (value, target) = progress
            val earned = id in p.earnedSigils
            MysteryCard(Modifier.fillMaxWidth()) {
                Text(if (earned) "✦ $name · earned" else "$name · ${value.coerceAtMost(target)}/$target")
                LinearProgressIndicator(progress = { if (earned) 1f else (value.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        OutlinedButton(onClick = onOpenArchive, modifier = Modifier.fillMaxWidth()) { Text("Explore all highlights & notes") }

        Text("Your data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Backups contain your imported books, reading position, annotations, Path progress, quests, and Castle identity. Restore replaces the current local library after validating the archive.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(enabled = !exporting && !restoring, onClick = { backupPicker.launch("veil-reader-backup.zip") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (exporting) "Exporting…" else "Export library backup")
        }
        OutlinedButton(enabled = !exporting && !restoring, onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (restoring) "Restoring…" else "Restore library backup")
        }
        OutlinedButton(enabled = !exporting && !restoring, onClick = { notesPicker.launch("veil-reader-notebook.md") }, modifier = Modifier.fillMaxWidth()) {
            Text("Export notebook as Markdown")
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace local Veil Reader data?") },
            text = { Text("Restore replaces your current library, annotations, reading progress, Path progress, and Castle state with the selected backup. Export a fresh backup first if you need the current state.") },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    confirmRestore = false
                    restorePicker.launch(arrayOf("application/zip", "application/octet-stream"))
                }) { Text("Choose backup") }
            }
        )
    }
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

@Composable
private fun StatCard(symbol: String, value: String, label: String, modifier: Modifier = Modifier) {
    MysteryCard(modifier) {
        Text(symbol, fontSize = 24.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
