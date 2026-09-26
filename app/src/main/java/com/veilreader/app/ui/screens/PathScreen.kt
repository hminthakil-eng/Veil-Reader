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
import com.veilreader.app.ui.theme.VeilPalette
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
    val presentation = pathPresentations[profile.path.id]
        ?: PathPresentation("Reading", "A Path is shaped by returning to the page.")
    var showCeremony by rememberSaveable { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { reveal = true }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        ScreenHeader(
            eyebrow = "THE ${presentation.aspect.uppercase()} PATH",
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
                color = VeilPalette.Mist
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
    val xpTarget = profile.xpForNextLevel.coerceAtLeast(1)
    val xpTargetProgress = (profile.xp.toFloat() / xpTarget).coerceIn(0f, 1f)
    val xpProgress by animateFloatAsState(
        targetValue = xpTargetProgress,
        animationSpec = tween(650),
        label = "path-xp-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF121017),
                        VeilPalette.Archive.copy(alpha = 0.98f),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.34f)),
                MaterialTheme.shapes.small
            )
    ) {
        PathRitualBackdrop(
            pathId = profile.path.id,
            rankIndex = profile.rankIndex,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                presentation.aspect.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.75.sp),
                color = VeilPalette.Brass
            )

            PathSigil(
                pathId = profile.path.id,
                modifier = Modifier.size(128.dp),
                active = true
            )

            Text(
                profile.rankName,
                style = MaterialTheme.typography.headlineMedium,
                color = VeilPalette.Moon,
                textAlign = TextAlign.Center
            )
            Text(
                presentation.invocation,
                style = MaterialTheme.typography.bodyLarge,
                color = VeilPalette.Moon.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
            Text(
                profile.path.description,
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 460.dp)
            )

            Spacer(Modifier.height(4.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "LEVEL ${profile.level}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                    color = VeilPalette.Brass.copy(alpha = 0.84f)
                )
                Text(
                    "${profile.xp}/$xpTarget XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist
                )
            }
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = VeilPalette.Brass,
                trackColor = VeilPalette.Moon.copy(alpha = 0.09f),
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun PathRitualBackdrop(
    pathId: String,
    rankIndex: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.39f)
        val brass = VeilPalette.Brass
        val stone = VeilPalette.StrongBorderDark

        drawCircle(
            color = brass.copy(alpha = 0.045f),
            radius = size.minDimension * 0.31f,
            center = center,
            style = Stroke(1.dp.toPx())
        )
        drawCircle(
            color = brass.copy(alpha = 0.025f),
            radius = size.minDimension * 0.23f,
            center = center,
            style = Stroke(1.dp.toPx())
        )

        repeat(8) { index ->
            val angle = Math.toRadians(-90.0 + index * 45.0)
            val inner = size.minDimension * 0.19f
            val outer = size.minDimension * 0.33f
            val x1 = center.x + kotlin.math.cos(angle).toFloat() * inner
            val y1 = center.y + kotlin.math.sin(angle).toFloat() * inner
            val x2 = center.x + kotlin.math.cos(angle).toFloat() * outer
            val y2 = center.y + kotlin.math.sin(angle).toFloat() * outer
            drawLine(
                brass.copy(alpha = 0.032f),
                Offset(x1, y1),
                Offset(x2, y2),
                1.dp.toPx(),
                StrokeCap.Round
            )
        }

        val constellationAlpha = 0.06f + rankIndex.coerceAtLeast(0) * 0.01f
        repeat(9) { index ->
            val x = w * (0.10f + ((index * 31) % 80) / 100f)
            val y = h * (0.12f + ((index * 47) % 72) / 100f)
            drawCircle(
                color = brass.copy(alpha = constellationAlpha.coerceAtMost(0.16f)),
                radius = if (index % 3 == 0) 1.2.dp.toPx() else 0.8.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawLine(
            stone.copy(alpha = 0.12f),
            Offset(w * 0.08f, h * 0.86f),
            Offset(w * 0.92f, h * 0.86f),
            1.dp.toPx()
        )
    }
}

@Composable
private fun PathSigil(
    pathId: String,
    modifier: Modifier = Modifier,
    active: Boolean
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val tint = if (active) VeilPalette.Brass else VeilPalette.Mist
            val stroke = Stroke(1.1.dp.toPx())

            drawCircle(
                tint.copy(alpha = if (active) 0.58f else 0.24f),
                size.minDimension * 0.45f,
                center,
                style = stroke
            )
            drawCircle(
                tint.copy(alpha = if (active) 0.30f else 0.16f),
                size.minDimension * 0.34f,
                center,
                style = stroke
            )

            repeat(4) { index ->
                val angle = Math.toRadians(45.0 + index * 90.0)
                val r1 = size.minDimension * 0.36f
                val r2 = size.minDimension * 0.48f
                val start = Offset(
                    center.x + kotlin.math.cos(angle).toFloat() * r1,
                    center.y + kotlin.math.sin(angle).toFloat() * r1
                )
                val end = Offset(
                    center.x + kotlin.math.cos(angle).toFloat() * r2,
                    center.y + kotlin.math.sin(angle).toFloat() * r2
                )
                drawLine(
                    tint.copy(alpha = if (active) 0.42f else 0.20f),
                    start,
                    end,
                    stroke.width,
                    StrokeCap.Round
                )
            }
        }

        PathIcon(
            pathId = pathId,
            tint = if (active) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.60f),
            modifier = Modifier.size(52.dp)
        )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(VeilPalette.Archive.copy(alpha = 0.94f))
            .border(
                BorderStroke(
                    1.dp,
                    if (canAdvance) VeilPalette.Brass.copy(alpha = 0.62f)
                    else VeilPalette.BorderDark.copy(alpha = 0.86f)
                ),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.84f, size.height * 0.50f)
            drawCircle(
                color = VeilPalette.Brass.copy(alpha = if (canAdvance) 0.085f else 0.035f),
                radius = size.minDimension * 0.34f,
                center = center,
                style = Stroke(1.dp.toPx())
            )
            drawCircle(
                color = VeilPalette.Brass.copy(alpha = if (canAdvance) 0.055f else 0.025f),
                radius = size.minDimension * 0.23f,
                center = center,
                style = Stroke(1.dp.toPx())
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                if (nextRank == null) "RITUAL COMPLETE" else "NEXT THRESHOLD",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                color = VeilPalette.Brass
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        if (nextRank == null) "The Path remains open" else nextRank,
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon
                    )
                    Text(
                        if (nextRank == null) {
                            "No higher rank remains."
                        } else {
                            ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    "${profile.ritualProgress}/$target",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canAdvance) VeilPalette.Brass else VeilPalette.Mist
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = if (canAdvance) VeilPalette.Brass else VeilPalette.Spirit,
                trackColor = VeilPalette.Moon.copy(alpha = 0.08f),
                drawStopIndicator = {}
            )

            if (nextRank != null) {
                Button(
                    onClick = onPrepareCeremony,
                    enabled = canAdvance,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 42.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A),
                        disabledContainerColor = VeilPalette.RaisedIron.copy(alpha = 0.50f),
                        disabledContentColor = VeilPalette.Mist.copy(alpha = 0.62f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (canAdvance) "Perform advancement"
                        else "Keep reading · ${profile.ritualProgress}/$target",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RankConstellation(profile: ReaderProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(VeilPalette.Ink.copy(alpha = 0.42f))
            .border(
                BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.72f)),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 10.dp, vertical = 18.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val centerX = size.width * 0.5f
            drawLine(
                color = VeilPalette.Brass.copy(alpha = 0.13f),
                start = Offset(centerX, 20.dp.toPx()),
                end = Offset(centerX, size.height - 20.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            profile.path.ranks.forEachIndexed { index, rank ->
                val mastered = index < profile.rankIndex
                val current = index == profile.rankIndex
                val awakened = mastered || current
                val alignLeft = index % 2 == 0

                VeilReveal(
                    delayMillis = 70 + index * 55,
                    distance = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 78.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(if (current) 18.dp else 14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        current -> VeilPalette.Brass
                                        mastered -> VeilPalette.Spirit
                                        else -> VeilPalette.BorderDark
                                    }
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (awakened) VeilPalette.Moon.copy(alpha = 0.34f)
                                        else VeilPalette.StrongBorderDark.copy(alpha = 0.54f)
                                    ),
                                    CircleShape
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(
                                    if (alignLeft) Alignment.CenterStart
                                    else Alignment.CenterEnd
                                )
                                .widthIn(max = 156.dp)
                                .padding(
                                    start = if (alignLeft) 6.dp else 30.dp,
                                    end = if (alignLeft) 30.dp else 6.dp
                                ),
                            horizontalAlignment = if (alignLeft) Alignment.Start else Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "RANK ${(index + 1).toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                                color = if (awakened) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.46f)
                            )
                            Text(
                                rank,
                                style = if (current) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.titleSmall
                                },
                                color = if (awakened) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.52f),
                                textAlign = if (alignLeft) TextAlign.Start else TextAlign.End
                            )
                            Text(
                                when {
                                    mastered -> "MASTERED"
                                    current -> "CURRENT SEAL"
                                    else -> "SEALED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    mastered -> VeilPalette.Spirit
                                    current -> VeilPalette.Brass
                                    else -> VeilPalette.Mist.copy(alpha = 0.40f)
                                }
                            )
                        }
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
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.74f),
        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.78f))
    ) {
        Column(
            Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(VeilPalette.Ink.copy(alpha = if (enabled) 0.52f else 0.34f))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (enabled) VeilPalette.Brass.copy(alpha = 0.36f)
                                else VeilPalette.BorderDark
                            ),
                            MaterialTheme.shapes.extraSmall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PathIcon(
                        pathId = path.id,
                        tint = if (enabled) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.44f),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(path.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${presentation.aspect} · ${path.epithet}",
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Brass.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                path.description,
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Mist
            )
            OutlinedButton(
                onClick = onChoose,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                shape = MaterialTheme.shapes.extraSmall
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
        shape = MaterialTheme.shapes.small,
        containerColor = VeilPalette.Archive,
        tonalElevation = 0.dp,
        icon = {
            PathIcon(
                pathId = profile.path.id,
                tint = VeilPalette.Brass,
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
                    color = VeilPalette.Mist,
                    textAlign = TextAlign.Center
                )
                Text(
                    presentation.invocation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Brass,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VeilPalette.Brass,
                    contentColor = Color(0xFF17120A)
                )
            ) { Text("Advance") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not yet", color = VeilPalette.Moon.copy(alpha = 0.72f))
            }
        }
    )
}

@Composable
private fun PathIcon(pathId: String, tint: Color, modifier: Modifier = Modifier) {
    val cutout = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        when (pathId) {
            "oracle" -> {
                val eye = Path().apply {
                    moveTo(w * .10f, h * .50f)
                    quadraticTo(w * .50f, h * .16f, w * .90f, h * .50f)
                    quadraticTo(w * .50f, h * .84f, w * .10f, h * .50f)
                    close()
                }
                drawPath(eye, tint, style = stroke)
                drawCircle(tint, w * .10f, Offset(w * .50f, h * .50f), style = stroke)
            }
            "dreamwalker" -> {
                drawCircle(tint, w * .31f, Offset(w * .45f, h * .47f), style = stroke)
                drawCircle(cutout, w * .29f, Offset(w * .57f, h * .39f))
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
                drawCircle(cutout, w * .28f, Offset(w * .60f, h * .38f))
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
            color = VeilPalette.Brass
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
