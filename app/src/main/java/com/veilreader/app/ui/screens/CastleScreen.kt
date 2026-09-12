package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.ui.theme.VeilSpacing

/**
 * Castle 1.0 begins as a spatial place instead of a list of feature cards.
 *
 * The map is intentionally Compose-native: no game engine, no continuous animation and no reader
 * interference. Each chamber remains the same product destination; the presentation now makes the
 * progression legible as an evolving world.
 */
@Composable
fun CastleScreen(
    profile: ReaderProfile,
    onAdvanceRank: () -> Unit,
    onOpenRoom: (String) -> Unit
) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        ScreenHeader(
            eyebrow = "The Castle",
            title = "The halls remember",
            subtitle = "Every chamber is shaped by reading. New wings awaken only when your Path has earned them."
        )

        CastleKeep(profile = profile, canAdvance = canAdvance, onAdvanceRank = onAdvanceRank)

        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            Text(
                "CASTLE MAP",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.7.sp),
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                "Choose an awakened chamber",
                style = MaterialTheme.typography.titleLarge
            )
        }

        CastleMap(
            rankIndex = profile.rankIndex,
            onOpenRoom = onOpenRoom
        )

        Text(
            "Sealed rooms are not purchases or timers. They awaken through lasting reading progress along your Path.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CastleKeep(
    profile: ReaderProfile,
    canAdvance: Boolean,
    onAdvanceRank: () -> Unit
) {
    val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(1)
    val castleProgress = (profile.rankIndex.toFloat() / finalRank).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
                MaterialTheme.shapes.extraLarge
            )
            .padding(VeilSpacing.xl)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♜", fontSize = 34.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Castle Tier ${profile.rankIndex + 1}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "${profile.booksFinished} completed books have left traces in these halls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { castleProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${profile.path.name} · ${profile.rankName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${profile.rankIndex + 1}/${profile.path.ranks.size} wings",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (canAdvance) {
                Button(
                    onClick = onAdvanceRank,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Text("Enter the Ritual Chamber")
                }
            }
        }
    }
}

@Composable
private fun CastleMap(rankIndex: Int, onOpenRoom: (String) -> Unit) {
    val rooms = SampleData.rooms

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)),
                MaterialTheme.shapes.extraLarge
            )
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (rowIndex in 0 until 3) {
                val left = rooms[rowIndex * 2]
                val right = rooms[rowIndex * 2 + 1]

                RoomPair(
                    leftId = left.id,
                    leftName = left.name,
                    leftUnlockRank = left.unlockRankIndex,
                    rightId = right.id,
                    rightName = right.name,
                    rightUnlockRank = right.unlockRankIndex,
                    rankIndex = rankIndex,
                    onOpenRoom = onOpenRoom
                )

                if (rowIndex < 2) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f))
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomPair(
    leftId: String,
    leftName: String,
    leftUnlockRank: Int,
    rightId: String,
    rightName: String,
    rightUnlockRank: Int,
    rankIndex: Int,
    onOpenRoom: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CastleRoomNode(
            id = leftId,
            name = leftName,
            unlockRank = leftUnlockRank,
            unlocked = rankIndex >= leftUnlockRank,
            onOpenRoom = onOpenRoom,
            modifier = Modifier.weight(1f)
        )

        Box(
            Modifier
                .width(22.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f))
        )

        CastleRoomNode(
            id = rightId,
            name = rightName,
            unlockRank = rightUnlockRank,
            unlocked = rankIndex >= rightUnlockRank,
            onOpenRoom = onOpenRoom,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CastleRoomNode(
    id: String,
    name: String,
    unlockRank: Int,
    unlocked: Boolean,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    val edge = if (unlocked) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.54f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.54f)
    }
    val fill = if (unlocked) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
            )
        )
    }

    Column(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, edge), shape)
            .clickable(enabled = unlocked) { onOpenRoom(id) }
            .padding(horizontal = VeilSpacing.sm, vertical = VeilSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (unlocked) roomSigil(id) else "◇",
            fontSize = 30.sp,
            color = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(VeilSpacing.xs))
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (unlocked) "AWAKENED" else "SEALED · RANK ${unlockRank + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
        )
    }
}

private fun roomSigil(id: String): String = when (id) {
    "library" -> "▦"
    "ritual" -> "✦"
    "observatory" -> "◉"
    "archive" -> "◇"
    "treasury" -> "◆"
    "sanctum" -> "✧"
    else -> "·"
}
