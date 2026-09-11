package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.ReaderProfile

private data class SigilPresentation(val name: String, val symbol: String, val description: String)

private val sigils = linkedMapOf(
    "first_hour" to SigilPresentation("Quiet Hour", "◷", "A full hour spent inside the written world."),
    "passage_keeper" to SigilPresentation("Passage Keeper", "✦", "Ten passages preserved from the books that changed you."),
    "seven_days" to SigilPresentation("Seven-Day Lantern", "🔥", "A reading flame kept alive for seven days."),
    "ten_tomes" to SigilPresentation("Ten Tomes", "📚", "Ten completed books now stand in the Grand Library."),
    "first_threshold" to SigilPresentation("First Threshold", "◇", "The first true advancement along your chosen Path.")
)

@Composable
fun TreasuryScreen(
    profile: ReaderProfile,
    equippedSigil: String?,
    onEquip: (String?) -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onClose) { Text("‹ Castle") }
        ScreenHeader(
            eyebrow = "Treasury",
            title = "Relics of your reading life",
            subtitle = "Sigils are earned by durable reading milestones. Equip one to represent your journey."
        )

        val equipped = equippedSigil?.let(sigils::get)
        MysteryCard(Modifier.fillMaxWidth()) {
            Text("DISPLAY PEDESTAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            if (equipped == null) {
                Text("No sigil equipped", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Choose an earned sigil below. This changes only your Castle identity—not your reading access.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(equipped.symbol, fontSize = 38.sp)
                    Column(Modifier.weight(1f)) {
                        Text(equipped.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(equipped.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { onEquip(null) }) { Text("Clear pedestal") }
            }
        }

        Text("Earned sigils", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        sigils.forEach { (id, presentation) ->
            val earned = id in profile.earnedSigils
            MysteryCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (earned) presentation.symbol else "🔒", fontSize = 30.sp)
                    Column(Modifier.weight(1f)) {
                        Text(presentation.name, fontWeight = FontWeight.Bold)
                        Text(
                            if (earned) presentation.description else "This relic has not awakened yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (earned) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = equippedSigil != id,
                        onClick = { onEquip(id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (equippedSigil == id) "Equipped" else "Equip ${presentation.name}")
                    }
                }
            }
        }
    }
}

@Composable
fun SanctumScreen(
    profile: ReaderProfile,
    castleTitle: String,
    availableTitles: List<String>,
    onSelectTitle: (String) -> Unit,
    onClose: () -> Unit
) {
    val finalRank = profile.path.ranks.lastIndex
    val rankProgress = if (finalRank == 0) 1f else profile.rankIndex.toFloat() / finalRank.toFloat()
    val sigilProgress = (profile.earnedSigils.size.coerceAtMost(5) / 5f)
    val sovereignReady = profile.rankIndex >= finalRank && profile.earnedSigils.size >= 5

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onClose) { Text("‹ Castle") }
        ScreenHeader(
            eyebrow = "Inner Sanctum",
            title = castleTitle,
            subtitle = "The Sanctum records what cannot be bought with XP: thresholds crossed and lasting reading milestones."
        )

        MysteryCard(Modifier.fillMaxWidth()) {
            Text("PATH COMPLETION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Text("${profile.path.name} · ${profile.rankName}", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { rankProgress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }

        MysteryCard(Modifier.fillMaxWidth()) {
            Text("SIGIL CONSTELLATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Text("${profile.earnedSigils.size.coerceAtMost(5)}/5 core sigils awakened", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { sigilProgress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                if (sovereignReady) "The final Castle title has awakened." else "Reach the final Path rank and awaken all five core sigils to reveal the deepest title.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("Castle title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        availableTitles.forEach { title ->
            OutlinedButton(
                onClick = { onSelectTitle(title) },
                enabled = title != castleTitle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (title == castleTitle) "✦ $title" else title)
            }
        }
    }
}
