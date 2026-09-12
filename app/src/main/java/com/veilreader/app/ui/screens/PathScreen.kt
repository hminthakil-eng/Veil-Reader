package com.veilreader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.ui.theme.VeilSpacing

private data class PathPresentation(
    val aspect: String,
    val invocation: String
)

private val pathPresentations = mapOf(
    "oracle" to PathPresentation("Sight", "What is hidden may still be understood."),
    "dreamwalker" to PathPresentation("Wonder", "Every page is a door that did not exist before."),
    "archivist" to PathPresentation("Memory", "What is learned deserves a place to remain."),
    "vanguard" to PathPresentation("Momentum", "Forward is a discipline, not a speed."),
    "nocturne" to PathPresentation("Shadow", "Some truths are visible only after the lantern dims."),
    "artificer" to PathPresentation("Making", "Understand the mechanism and the miracle changes shape.")
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
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { reveal = true }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        ScreenHeader(
            eyebrow = "Path",
            title = profile.path.name,
            subtitle = "${profile.path.epithet} · ${profile.rankName}"
        )

        AnimatedVisibility(
            visible = reveal,
            enter = fadeIn(tween(360)) + slideInVertically(tween(420)) { it / 6 }
        ) {
            PathIdentityPanel(profile)
        }

        RitualPanel(
            profile = profile,
            canAdvance = canAdvance,
            nextRank = nextRank,
            onPrepareCeremony = { showCeremony = true }
        )

        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            SectionHeading(
                eyebrow = "Progression",
                title = "Your ascent"
            )
            RankConstellation(profile)
        }

        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            SectionHeading(
                eyebrow = "Other paths",
                title = if (profile.rankIndex == 0) "Choose what fits you" else "Your choice is rooted"
            )
            Text(
                if (profile.rankIndex == 0) {
                    "You can change Path until your first advancement. It changes progression flavor, never access to your books."
                } else {
                    "Other Paths stay visible as lore. Your current Path remains fixed for this journey."
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
        ?: PathPresentation("Reading", "A Path is shaped by returning to the page.")
    val shape = MaterialTheme.shapes.extraLarge
    val xpTarget = profile.xpForNextLevel.coerceAtLeast(1)
    val xpTargetProgress = (profile.xp.toFloat() / xpTarget).coerceIn(0f, 1f)
    val xpProgress by animateFloatAsState(
        targetValue = xpTargetProgress,
        animationSpec = tween(650),
        label = "path-xp-progress"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f)),
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
                    .size(94.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                PathIcon(
                    pathId = profile.path.id,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.74f)
            ) {
                Text(
                    presentation.aspect.uppercase(),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.35.sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

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

            Spacer(Modifier.height(VeilSpacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Level ${profile.level}", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${profile.xp}/$xpTarget XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
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
    val targetProgress = (profile.ritualProgress.toFloat() / target).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(700),
        label = "ritual-progress"
    )

    MysteryCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (nextRank == null) "FINAL RANK" else "NEXT RANK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    if (nextRank == null) "Path complete" else nextRank,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Surface(
                shape = CircleShape,
                color = if (canAdvance) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                }
            ) {
                Text(
                    "${profile.ritualProgress}/$target",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (canAdvance) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = if (canAdvance) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        )

        Text(
            if (nextRank == null) {
                "No higher rank remains. Reading continues without another gate."
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = VeilSpacing.xs)
            ) {
                Text(if (canAdvance) "Advance to $nextRank" else "Keep reading · ${profile.ritualProgress}/$target")
            }
        }
    }
}

@Composable
private fun RankConstellation(profile: ReaderProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        profile.path.ranks.forEachIndexed { index, rank ->
            val mastered = index < profile.rankIndex
            val current = index == profile.rankIndex
            val accent = when {
                mastered -> MaterialTheme.colorScheme.tertiary
                current -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outline
            }
            val state = when {
                mastered -> "MASTERED"
                current -> "CURRENT"
                else -> "SEALED"
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = if (current) 0.18f else 0.09f))
                            .border(BorderStroke(1.dp, accent.copy(alpha = 0.72f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mastered) {
                            CheckMarkIcon(Modifier.size(18.dp), accent)
                        } else {
                            Text(
                                (index + 1).toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = accent
                            )
                        }
                    }
                    if (index != profile.path.ranks.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(38.dp)
                                .background(
                                    if (mastered) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.48f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)
                                )
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).padding(bottom = 10.dp),
                    color = if (current) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                    } else {
                        Color.Transparent
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        Modifier.padding(horizontal = if (current) VeilSpacing.sm else 0.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(rank, style = MaterialTheme.typography.titleMedium)
                        Text(
                            state,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PathChoiceCard(path: ReadingPath, enabled: Boolean, onChoose: () -> Unit) {
    val presentation = pathPresentations[path.id]
        ?: PathPresentation("Reading", "A different way through the archive.")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
    ) {
        Column(
            Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.48f else 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    PathIcon(
                        pathId = path.id,
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(path.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${presentation.aspect} · ${path.epithet}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(if (enabled) "Choose this Path" else "Locked after first advancement")
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
        ?: PathPresentation("Reading", "A Path is shaped by returning to the page.")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            PathIcon(
                pathId = profile.path.id,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                "Advance to $nextRank",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                Text(
                    "Your ritual is complete. This marks a permanent rank on ${profile.path.name}.",
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
        confirmButton = { Button(onClick = onConfirm) { Text("Advance") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not yet") } }
    )
}

@Composable
private fun PathIcon(pathId: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        when (pathId) {
            "oracle" -> {
                val eye = Path().apply {
                    moveTo(w * .10f, h * .50f)
                    quadraticBezierTo(w * .50f, h * .16f, w * .90f, h * .50f)
                    quadraticBezierTo(w * .50f, h * .84f, w * .10f, h * .50f)
                    close()
                }
                drawPath(eye, tint, style = stroke)
                drawCircle(tint, w * .10f, Offset(w * .50f, h * .50f), style = stroke)
            }
            "dreamwalker" -> {
                drawCircle(tint, w * .31f, Offset(w * .45f, h * .47f), style = stroke)
                drawCircle(MaterialTheme.colorScheme.surface, w * .29f, Offset(w * .57f, h * .39f))
                drawCircle(tint, w * .04f, Offset(w * .74f, h * .24f))
            }
            "archivist" -> {
                drawLine(tint, Offset(w * .18f, h * .20f), Offset(w * .18f, h * .82f), stroke.width, StrokeCap.Round)
                repeat(3) { i ->
                    val y = h * (.26f + i * .20f)
                    drawLine(tint, Offset(w * .28f, y), Offset(w * .82f, y), stroke.width, StrokeCap.Round)
                }
            }
            "vanguard" -> {
                val p = Path().apply {
                    moveTo(w * .50f, h * .08f)
                    lineTo(w * .84f, h * .78f)
                    lineTo(w * .50f, h * .64f)
                    lineTo(w * .16f, h * .78f)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            "nocturne" -> {
                drawCircle(tint, w * .31f, Offset(w * .45f, h * .47f), style = stroke)
                drawCircle(MaterialTheme.colorScheme.surface, w * .28f, Offset(w * .60f, h * .38f))
            }
            "artificer" -> {
                drawCircle(tint, w * .25f, Offset(w * .50f, h * .50f), style = stroke)
                repeat(6) { i ->
                    val angle = Math.toRadians((i * 60.0) - 90.0)
                    val x1 = w * .50f + kotlin.math.cos(angle).toFloat() * w * .28f
                    val y1 = h * .50f + kotlin.math.sin(angle).toFloat() * h * .28f
                    val x2 = w * .50f + kotlin.math.cos(angle).toFloat() * w * .42f
                    val y2 = h * .50f + kotlin.math.sin(angle).toFloat() * h * .42f
                    drawLine(tint, Offset(x1, y1), Offset(x2, y2), stroke.width, StrokeCap.Round)
                }
                drawCircle(tint, w * .06f, Offset(w * .50f, h * .50f), style = stroke)
            }
            else -> drawCircle(tint, w * .28f, Offset(w * .50f, h * .50f), style = stroke)
        }
    }
}

@Composable
private fun CheckMarkIcon(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val width = 1.8.dp.toPx()
        drawLine(tint, Offset(size.width * .18f, size.height * .52f), Offset(size.width * .42f, size.height * .75f), width, StrokeCap.Round)
        drawLine(tint, Offset(size.width * .42f, size.height * .75f), Offset(size.width * .82f, size.height * .26f), width, StrokeCap.Round)
    }
}

@Composable
private fun SectionHeading(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.35.sp),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
