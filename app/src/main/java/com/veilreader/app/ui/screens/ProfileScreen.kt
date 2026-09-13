package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.ui.theme.VeilSpacing

private data class VeiledDiscovery(
    val id: String,
    val symbol: String,
    val title: String,
    val clue: String,
    val lore: String,
    val revealed: (ReaderProfile, Int) -> Boolean
)

private val veiledDiscoveries = listOf(
    VeiledDiscovery(
        id = "patient_flame",
        symbol = "◈",
        title = "The Patient Flame",
        clue = "A flame kept for many returns begins to remember the hand that lit it.",
        lore = "Consistency leaves a different mark than intensity. The Castle has begun to recognize your return.",
        revealed = { profile, _ -> profile.streakDays >= 7 && profile.minutesRead >= 600 }
    ),
    VeiledDiscovery(
        id = "marginalia_gate",
        symbol = "✧",
        title = "The Marginalia Gate",
        clue = "Some doors are written in the margins rather than printed on the page.",
        lore = "Enough passages have been preserved that your annotations now form a second text beside the books themselves.",
        revealed = { profile, highlights -> highlights >= 10 && profile.pagesRead >= 1_000 }
    ),
    VeiledDiscovery(
        id = "deep_shelf",
        symbol = "▥",
        title = "The Deep Shelf",
        clue = "Finished volumes gather weight. Eventually the shelf becomes a foundation.",
        lore = "Your completed books and first Path threshold now reinforce one another. The archive is becoming a place, not a list.",
        revealed = { profile, _ -> profile.booksFinished >= 10 && profile.rankIndex >= 1 }
    ),
    VeiledDiscovery(
        id = "long_watch",
        symbol = "◐",
        title = "The Long Watch",
        clue = "There is a point when time spent reading stops feeling counted.",
        lore = "Fifty hours have passed inside books. The Castle records the duration, but the deeper change cannot be measured in minutes.",
        revealed = { profile, _ -> profile.minutesRead >= 3_000 }
    ),
    VeiledDiscovery(
        id = "veil_thins",
        symbol = "⌁",
        title = "When the Veil Thins",
        clue = "Several marks must awaken before they begin to answer one another.",
        lore = "Your earned sigils are no longer isolated milestones. Together they form the first readable pattern in the Veil.",
        revealed = { profile, _ -> profile.earnedSigils.size >= 4 }
    ),
    VeiledDiscovery(
        id = "unnamed_chamber",
        symbol = "⬡",
        title = "The Unnamed Chamber",
        clue = "The deepest chamber does not open to a single achievement.",
        lore = "A mature Path and a complete core sigil constellation have revealed a chamber that the early Castle could not name.",
        revealed = { profile, _ -> profile.rankIndex >= 3 && profile.earnedSigils.size >= 5 }
    )
)

@Composable
fun ProfileScreen(
    profile: ReaderProfile,
    highlightCount: Int,
    gameVisible: Boolean,
    dailyGoalMinutes: Int,
    castleTitle: String,
    equippedSigilName: String?,
    onSetDailyGoal: (Int) -> Unit,
    onOpenSettings: (String) -> Unit,
    onOpenArchive: () -> Unit
) {
    val p = profile
    val revealedDiscoveries = veiledDiscoveries.count { it.revealed(p, highlightCount) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        ScreenHeader("Reader profile", if (gameVisible) castleTitle else "Your reading life",
            if (gameVisible) "${p.path.name} · ${p.rankName}" else "A little space for books, at your own pace.")
        FilledTonalButton(onClick = { onOpenSettings("general") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Settings & reading comfort")
        }

        if (gameVisible) {

            MysteryCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Experience", style = MaterialTheme.typography.titleMedium)
                    Text("${p.xp}/${p.xpForNextLevel} XP", color = MaterialTheme.colorScheme.secondary)
                }
                LinearProgressIndicator(
                    progress = { (p.xp.toFloat() / p.xpForNextLevel.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = VeilSpacing.xs)
                )
                equippedSigilName?.let {
                    Text(
                        "Equipped sigil · $it",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = VeilSpacing.xs)
                    )
                }
            }

        }

        Row(horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            StatCard("◇", "${p.streakDays}", "day streak", Modifier.weight(1f))
            StatCard("▥", "${p.booksFinished}", "books", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            StatCard("▤", "${p.pagesRead}", "page turns", Modifier.weight(1f))
            StatCard("◷", formatMinutes(p.minutesRead), "reading", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            StatCard("✦", "$highlightCount", "highlights", Modifier.weight(1f))
            if (gameVisible) StatCard("♜", "${p.rankIndex + 1}", "Path rank", Modifier.weight(1f))
        }

        ProfileSectionHeading(
            eyebrow = "Rhythm",
            title = "Daily reading goal"
        )
        Text(
            if (gameVisible) "Your first daily quest follows this target. Choose a comfortable pace." else "A gentle target for your reading time. Change it whenever you like.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            listOf(10, 20, 30, 60).forEach { minutes ->
                if (minutes == dailyGoalMinutes) {
                    Button(onClick = { onSetDailyGoal(minutes) }) { Text("${minutes}m ✓") }
                } else {
                    OutlinedButton(onClick = { onSetDailyGoal(minutes) }) { Text("${minutes}m") }
                }
            }
        }

        if (gameVisible) {
            ProfileSectionHeading(
                eyebrow = "Known marks",
                title = "Earned sigils"
            )
            listOf(
                Triple("first_hour", "First Hour", p.minutesRead to 60),
                Triple("passage_keeper", "Passage Keeper", highlightCount to 10),
                Triple("seven_days", "Seven-Day Journey", p.streakDays to 7),
                Triple("ten_tomes", "Ten Tomes", p.booksFinished to 10),
                Triple("first_threshold", "First Threshold", p.rankIndex to 1)
            ).forEach { (id, name, progress) ->
                val (value, target) = progress
                val earned = id in p.earnedSigils
                SigilProgressRow(
                    name = name,
                    value = value,
                    target = target,
                    earned = earned
                )
            }

            ProfileSectionHeading(
                eyebrow = "Behind the known marks",
                title = "Veiled discoveries",
                trailing = "$revealedDiscoveries/${veiledDiscoveries.size} revealed"
            )
            Text(
                "Discoveries are not quests. Their conditions stay hidden; they surface naturally when different parts of your reading life begin to form a pattern.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            veiledDiscoveries.forEachIndexed { index, discovery ->
                DiscoveryCard(
                    index = index,
                    discovery = discovery,
                    revealed = discovery.revealed(p, highlightCount)
                )
            }

        }

        OutlinedButton(
            onClick = onOpenArchive,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Explore all highlights & notes")
        }

        OutlinedButton(onClick = { onOpenSettings("data") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Backups & data")
        }
    }
}

@Composable
private fun SigilProgressRow(name: String, value: Int, target: Int, earned: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                MaterialTheme.shapes.medium
            )
            .padding(VeilSpacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (earned) "AWAKENED" else "${value.coerceAtMost(target)}/$target",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (earned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                )
            }
            LinearProgressIndicator(
                progress = { if (earned) 1f else (value.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (earned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun DiscoveryCard(index: Int, discovery: VeiledDiscovery, revealed: Boolean) {
    val shape = MaterialTheme.shapes.large
    val accent = if (revealed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (revealed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
            )
            .border(BorderStroke(1.dp, accent.copy(alpha = if (revealed) 0.55f else 0.35f)), shape)
            .padding(VeilSpacing.lg)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(accent.copy(alpha = if (revealed) 0.13f else 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (revealed) discovery.symbol else "?",
                    fontSize = if (revealed) 26.sp else 20.sp,
                    color = accent
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                Text(
                    if (revealed) discovery.title else "Veiled Fragment ${index + 1}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (revealed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (revealed) "REVEALED" else "CLUE",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp),
                    color = accent
                )
                Text(
                    if (revealed) discovery.lore else discovery.clue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (revealed) {
                    Text(
                        "This discovery emerged from your existing reading history; no action was consumed and nothing expires.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionHeading(eyebrow: String, title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.secondary
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
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

