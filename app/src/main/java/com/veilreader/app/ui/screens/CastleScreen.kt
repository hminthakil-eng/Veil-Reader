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
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile

@Composable
fun CastleScreen(profile: ReaderProfile, onAdvanceRank: () -> Unit, onOpenRoom: (String) -> Unit) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            eyebrow = "The Castle",
            title = "A home built by reading",
            subtitle = "Rooms awaken as your Path advances."
        )

        MysteryCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("♜", fontSize = 50.sp)
                Column(Modifier.weight(1f)) {
                    Text("Castle Tier ${profile.rankIndex + 1}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${profile.booksFinished} completed books have left traces in these halls.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (canAdvance) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAdvanceRank, modifier = Modifier.fillMaxWidth()) {
                    Text("Perform advancement ritual")
                }
            }
        }

        SampleData.rooms.forEach { room ->
            val unlocked = profile.rankIndex >= room.unlockRankIndex
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = .45f)
                )
            ) {
                Row(
                    Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (unlocked) room.symbol else "🔒", fontSize = 30.sp)
                    Column(Modifier.weight(1f)) {
                        Text(room.name, fontWeight = FontWeight.Bold)
                        Text(room.purpose, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Text(
                        if (unlocked) "AWAKENED" else "RANK ${room.unlockRankIndex + 1}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (unlocked && room.id in setOf("library", "ritual", "observatory", "archive")) {
                    TextButton(onClick = { onOpenRoom(room.id) }, modifier = Modifier.padding(horizontal = 14.dp)) { Text("Enter ${room.name}") }
                }
            }
        }
    }
}
