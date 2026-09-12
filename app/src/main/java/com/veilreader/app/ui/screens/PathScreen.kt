package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.ui.theme.VeilSpacing

private data class PathPresentation(
    val sigil: String,
    val aspect: String,
    val invocation: String
)

private val pathPresentations = mapOf(
    "oracle" to PathPresentation("◇", "Sight", "What is hidden may still be understood."),
    "dreamwalker" to PathPresentation("◌", "Wonder", "Every page is a door that did not exist before."),
    "archivist" to PathPresentation("▱", "Memory", "What is learned deserves a place to remain."),
    "vanguard" to PathPresentation("⟁", "Momentum", "Forward is a discipline, not a speed."),
    "nocturne" to PathPresentation("◐", "Shadow", "Some truths are visible only after the lantern dims."),
    "artificer" to PathPresentation("⌬", "Making", "Understand the mechanism and the miracle changes shape.")
)

@Composable
fun PathScreen(
    profile: ReaderProfile,
    onAdvanceRank: () -> Unit,
    onChoosePath: (String) -> Unit
) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)
    val nextRank = profile.path.ranks.getOrNull(profile.rankIndex + 1)
    var showCeremony by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        ScreenHeader(
            eyebrow = "Your Path",
            title = profile.path.name,
            subtitle = profile.path.epithet
        )

        PathIdentityPanel(profile)

        RitualPanel(
            profile = profile,
            canAdvance = canAdvance,
            nextRank = nextRank,
            onPrepareCeremony = { showCeremony = true }
        )

        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            SectionHeading(
                eyebrow = "The ascent",
                title = "Ranks of ${profile.path.name.removePrefix("The ")}"
            )
            profile.path.ranks.forEachIndexed { index, rank ->
                RankWaypoint(
                    index = index,
                    rank = rank,
                    currentRankIndex = profile.rankIndex,
                    isLast = index == profile.path.ranks.lastIndex
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            SectionHeading(
                eyebrow = "The six ways",
                title = if (profile.rankIndex == 0) "Attunement is still open" else "Your choice has taken root"
            )
            Text(
                if (profile.rankIndex == 0) {
                    "Before the first advancement, you may still attune to another Path. Reading access never depends on this choice."
                } else {
                    "After the first advancement, other Paths remain visible as lore, but changing allegiance is sealed for this journey."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SampleData.paths.filterNot { it.id == profile.path.id }.forEach { path ->
                PathChoiceCard(
                    path = path,
                    enabled = profile.rankIndex == 0,
                    onChoose = { onChoosePath(path.id) }
                )
            }
        }
    }

    if (showCeremony && nextRank != null) {
        AdvancementCeremonyDialog(
            profile = profile,
            nextRank = nextRank,
            onDismiss = { showCeremony = false },
            onConfirm = {
                showCeremony = false
                onAdvanceRank()
            }
        )
    }
}

@Composable
private fun PathIdentityPanel(profile: ReaderProfile) {
    val presentation = pathPresentations[profile.path.id]
        ?: PathPresentation("◇", "Reading", "A Path is shaped by returning to the page.")
    val shape = MaterialTheme.shapes.extraLarge

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
                shape
            )
            .padding(VeilSpacing.xl)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Box(
                Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    presentation.sigil,
                    fontSize = 42.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                presentation.aspect.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                profile.rankName,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                presentation.invocation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                profile.path.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RitualPanel(
    profile: ReaderProfile,
    canAdvance: Boolean,
    nextRank: String?,
    onPrepareCeremony: () -> Unit
) {
    val target = profile.ritualTarget.coerceAtLeast(1)
    val progress = (profile.ritualProgress.toFloat() / target).coerceIn(0f, 1f)

    MysteryCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (nextRank == null) "FINAL RANK" else "ADVANCEMENT RITUAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    if (nextRank == null) "The Path is complete" else "Toward $nextRank",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                "${profile.ritualProgress}/$target",
                style = MaterialTheme.typography.labelLarge,
                color = if (canAdvance) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VeilSpacing.xs),
            color = if (canAdvance) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )

        Text(
            if (nextRank == null) {
                "No higher rank remains. Your reading continues without another gate."
            } else {
                ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (nextRank != null) {
            Button(
                onClick = onPrepareCeremony,
                enabled = canAdvance,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(top = VeilSpacing.xs)
            ) {
                Text(
                    if (canAdvance) "Prepare the advancement ceremony"
                    else "Ritual incomplete · ${profile.ritualProgress}/$target"
                )
            }
        }
    }
}

@Composable
private fun RankWaypoint(
    index: Int,
    rank: String,
    currentRankIndex: Int,
    isLast: Boolean
) {
    val mastered = index < currentRankIndex
    val current = index == currentRankIndex
    val stateLabel = when {
        mastered -> "MASTERED"
        current -> "CURRENT"
        else -> "SEALED"
    }
    val accent = when {
        mastered -> MaterialTheme.colorScheme.tertiary
        current -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (current) 0.18f else 0.10f))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.78f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        mastered -> "✓"
                        current -> "◇"
                        else -> (index + 1).toString()
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(1.dp)
                        .height(28.dp)
                        .background(
                            if (mastered) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(rank, style = MaterialTheme.typography.titleMedium)
            Text(
                stateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
    }
}

@Composable
private fun PathChoiceCard(path: ReadingPath, enabled: Boolean, onChoose: () -> Unit) {
    val presentation = pathPresentations[path.id]
        ?: PathPresentation("◇", "Reading", "A different way through the archive.")

    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f)),
                MaterialTheme.shapes.large
            )
            .padding(VeilSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    presentation.sigil,
                    fontSize = 28.sp,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Column(Modifier.weight(1f)) {
                    Text(path.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${presentation.aspect} · ${path.epithet}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                path.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onChoose,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(top = VeilSpacing.xs)
            ) {
                Text(if (enabled) "Attune to this Path" else "Sealed after first advancement")
            }
        }
    }
}

@Composable
private fun AdvancementCeremonyDialog(
    profile: ReaderProfile,
    nextRank: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val presentation = pathPresentations[profile.path.id]
        ?: PathPresentation("◇", "Reading", "A Path is shaped by returning to the page.")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                presentation.sigil,
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        },
        title = {
            Text(
                "Cross into $nextRank",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                Text(
                    "The ritual is complete. This ceremony marks a lasting threshold on ${profile.path.name}.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    presentation.invocation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Complete the ceremony") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not yet") }
        }
    )
}

@Composable
private fun SectionHeading(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
