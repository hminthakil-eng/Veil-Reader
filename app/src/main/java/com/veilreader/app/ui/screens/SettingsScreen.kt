package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.AppPreferences
import com.veilreader.app.domain.AppTheme
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.ui.VeilWorldBackdrop

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    appearance: ReaderAppearance,
    section: String,
    dailyGoalMinutes: Int,
    exporting: Boolean,
    restoring: Boolean,
    onSection: (String) -> Unit,
    onPreferences: (AppPreferences) -> Unit,
    onAppearance: (ReaderAppearance) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onExportNotes: (Uri) -> Unit,
    onShowWelcome: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    VeilWorldBackdrop {
        Column(Modifier.fillMaxHeight().widthIn(max = 760.dp).fillMaxWidth().systemBarsPadding().align(Alignment.TopCenter)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("general" to "General", "reading" to "Reading", "data" to "Backups & data").forEach { (id, label) ->
                    FilterChip(selected = section == id, onClick = { onSection(id) },
                        label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (section) {
                    "reading" -> ReaderAppearancePanel(appearance, onAppearance, onClose, doneLabel = "Done")
                    "data" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
                        LibraryDataControls(exporting, restoring, onExportBackup, onRestoreBackup, onExportNotes)
                    }
                    else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        ScreenHeader("Your space", "A reader that feels like you", "Make room for the things you love. You can change these choices at any time.")
                        MysteryCard(Modifier.fillMaxWidth()) {
                            ReaderOption("Quiet mode", "Hide castle, quests, and Path screens. Reading progress still grows, and your world waits for you.", !preferences.gameVisible) {
                                onPreferences(preferences.copy(gameVisible = !it))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("App appearance", style = MaterialTheme.typography.titleLarge)
                            Text("The library and menus. Your book’s paper color is a separate reading choice.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppTheme.entries.forEach { theme ->
                                    FilterChip(selected = preferences.theme == theme, onClick = { onPreferences(preferences.copy(theme = theme)) },
                                        label = { Text(theme.label) }, modifier = Modifier.heightIn(min = 48.dp))
                                }
                            }
                        }
                        HorizontalDivider()
                        ReaderOption("Reduce motion", "Remove decorative transitions and EPUB page animations. Your preferred turn style stays saved.", appearance.reduceMotion) {
                            onAppearance(appearance.copy(reduceMotion = it))
                        }
                        ReaderOption("Keep the screen awake", "While a book is open. Your normal screen timeout returns when you leave the reader.", appearance.keepScreenOn) {
                            onAppearance(appearance.copy(keepScreenOn = it))
                        }
                        HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("A gentle daily goal", style = MaterialTheme.typography.titleLarge)
                            Text("$dailyGoalMinutes minutes a day. A little reading counts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(10, 20, 30, 60).forEach { minutes ->
                                    FilterChip(selected = minutes == dailyGoalMinutes, onClick = { onDailyGoal(minutes) },
                                        label = { Text("$minutes min") }, modifier = Modifier.heightIn(min = 48.dp))
                                }
                            }
                        }
                        FilledTonalButton(onClick = { onSection("reading") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Customize the reading page")
                        }
                        OutlinedButton(onClick = onShowWelcome, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Revisit the welcome guide")
                        }
                        Text("Veil Reader · 0.12 Preview", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
