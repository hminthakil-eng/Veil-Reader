package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPolicy

@Composable
fun PathScreen(
    profile: ReaderProfile,
    onAdvanceRank: () -> Unit,
    onChoosePath: (String) -> Unit
) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            eyebrow = "Your Path",
            title = profile.path.name,
            subtitle = profile.path.epithet
        )

        MysteryCard(Modifier.fillMaxWidth()) {
            Text("Current rank", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(profile.rankName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { profile.ritualProgress / profile.ritualTarget.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAdvanceRank,
                enabled = canAdvance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (canAdvance) "Advance to the next rank" else "Ritual ${profile.ritualProgress}/${profile.ritualTarget}")
            }
        }

        Text("Ranks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        profile.path.ranks.forEachIndexed { index, rank ->
            val state = when {
                index < profile.rankIndex -> "MASTERED"
                index == profile.rankIndex -> "CURRENT"
                else -> "SEALED"
            }
            MysteryCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 1}. $rank", fontWeight = FontWeight.SemiBold)
                    Text(state, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Text("Other paths", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SampleData.paths.filterNot { it.id == profile.path.id }.forEach { path ->
            MysteryCard(Modifier.fillMaxWidth()) {
                Text(path.name, fontWeight = FontWeight.Bold)
                Text(path.epithet, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(path.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onChoosePath(path.id) },
                    enabled = profile.rankIndex == 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (profile.rankIndex == 0) "Attune to this Path" else "Sealed after first advancement")
                }
            }
        }
    }
}
