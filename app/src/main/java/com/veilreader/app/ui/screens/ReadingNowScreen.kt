package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPolicy

@Composable
fun ReadingNowScreen(
    books: List<Book>,
    profile: ReaderProfile,
    quests: List<Quest>,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCastle: () -> Unit
) {
    val current = books.filterNot { it.finished }.ifEmpty { books }.maxByOrNull { it.lastOpenedAtEpochMs.takeIf { time -> time > 0L } ?: it.addedAtEpochMs }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ScreenHeader(
            eyebrow = "Reading now",
            title = if (current == null) "Begin your Path" else "Return to the story",
            subtitle = "Every attentive reading session leaves a mark on your Castle."
        )

        if (current == null) {
            MysteryCard(Modifier.fillMaxWidth()) {
                Text("✦", fontSize = 42.sp, color = MaterialTheme.colorScheme.secondary)
                Text("The shelves are silent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Import your first EPUB or PDF to begin gaining XP, building a streak, and advancing your Path.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Go to Grand Library") }
            }
        } else {
            MysteryCard(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    BookCover(current.title, Modifier.width(116.dp).height(168.dp))
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(current.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(current.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(current.format.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        LinearProgressIndicator(
                            progress = { current.progress },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                        Text(
                            if (current.finished) "Completed" else "${(current.progress * 100).toInt()}% complete",
                            fontSize = 12.sp
                        )
                        Button(onClick = { onOpenBook(current) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text(if (current.progress > 0f) "Continue reading" else "Begin reading")
                        }
                    }
                }
            }
        }

        MysteryCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${profile.path.name} · ${profile.rankName}", fontWeight = FontWeight.SemiBold)
                    Text("Advancement ritual", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Text("${profile.ritualProgress}/${profile.ritualTarget}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { profile.ritualProgress / profile.ritualTarget.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex), fontSize = 13.sp)
        }

        Text("Today's quests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        quests.forEach { quest ->
            MysteryCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(quest.title, fontWeight = FontWeight.Medium)
                    Text(
                        if (quest.progress >= quest.target) "CLAIMED" else "+${quest.xpReward} XP",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { quest.progress / quest.target.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(5.dp))
                Text("${quest.progress}/${quest.target}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedButton(onClick = onOpenCastle, modifier = Modifier.fillMaxWidth()) {
            Text("Enter the Castle")
        }
    }
}
