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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere

internal enum class PathGeometryKind {
    RADIAL_EYE,
    ASYMMETRIC_CONSTELLATION,
    CONCENTRIC_ARCHIVE,
    AXIAL_SPEAR,
    ECLIPSE,
    MECHANICAL
}

internal fun pathGeometryFor(pathId: String): PathGeometryKind =
    when (pathId) {
        "oracle" -> PathGeometryKind.RADIAL_EYE
        "dreamwalker" -> PathGeometryKind.ASYMMETRIC_CONSTELLATION
        "archivist" -> PathGeometryKind.CONCENTRIC_ARCHIVE
        "vanguard" -> PathGeometryKind.AXIAL_SPEAR
        "nocturne" -> PathGeometryKind.ECLIPSE
        "artificer" -> PathGeometryKind.MECHANICAL
        else -> PathGeometryKind.RADIAL_EYE
    }

@Composable
fun PathScreen(
    profile: ReaderProfile,
    onAdvanceRank: () -> Unit,
    onChoosePath: (String) -> Unit
) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)
    val nextRankIndex = profile.rankIndex + 1
    val nextRank = profile.path.ranks.getOrNull(nextRankIndex)?.let {
        localizedPathRank(profile.path, nextRankIndex)
    }
    val aspect = localizedPathAspect(profile.path.id)
    var showCeremony by rememberSaveable { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { reveal = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.RITUAL,
                seed = profile.path.id.hashCode() xor profile.rankIndex
            ),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .widthIn(max = 920.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        ScreenHeader(
            eyebrow = aspect.uppercase(),
            title = localizedPathName(profile.path),
            subtitle = "${localizedPathEpithet(profile.path)} · ${localizedPathRank(profile.path, profile.rankIndex)}"
        )

        AnimatedVisibility(
            visible = reveal,
            enter = fadeIn(tween(VeilMotion.SPATIAL_MS)) + slideInVertically(tween(VeilMotion.SPATIAL_MS)) { it / 6 }
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
                eyebrow = stringResource(R.string.path_progression),
                title = stringResource(R.string.path_your_ascent)
            )
            RankConstellation(profile)
        }

        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            SectionHeading(
                eyebrow = stringResource(R.string.path_other_paths),
                title = if (profile.rankIndex == 0) {
                    stringResource(R.string.path_choose_fits)
                } else {
                    stringResource(R.string.path_choice_rooted)
                }
            )
            Text(
                if (profile.rankIndex == 0) {
                    stringResource(R.string.path_change_before_advancement)
                } else {
                    stringResource(R.string.path_lore_after_advancement)
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
    val aspect = localizedPathAspect(profile.path.id)
    val invocation = localizedPathInvocation(profile.path.id)
    val xpTarget = profile.xpForNextLevel.coerceAtLeast(1)
    val xpTargetProgress = (profile.xp.toFloat() / xpTarget).coerceIn(0f, 1f)
    val xpProgress by animateFloatAsState(
        targetValue = xpTargetProgress,
        animationSpec = tween(VeilMotion.SPATIAL_MS),
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
                aspect.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.75.sp),
                color = VeilPalette.Brass
            )

            PathSigil(
                pathId = profile.path.id,
                modifier = Modifier.size(128.dp),
                active = true
            )

            Text(
                localizedPathRank(profile.path, profile.rankIndex),
                style = MaterialTheme.typography.headlineMedium,
                color = VeilPalette.Moon,
                textAlign = TextAlign.Center
            )
            Text(
                invocation,
                style = MaterialTheme.typography.bodyLarge,
                color = VeilPalette.Moon.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
            Text(
                localizedPathDescription(profile.path),
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
                    stringResource(R.string.path_level, profile.level),
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
        val geometry = pathGeometryFor(pathId)
        val rankAlpha = (0.045f + rankIndex.coerceAtLeast(0) * 0.009f)
            .coerceAtMost(0.14f)
        val stroke = 1.dp.toPx()

        when (geometry) {
            PathGeometryKind.RADIAL_EYE -> {
                val eye = Path().apply {
                    moveTo(w * 0.20f, center.y)
                    quadraticTo(w * 0.50f, h * 0.18f, w * 0.80f, center.y)
                    quadraticTo(w * 0.50f, h * 0.60f, w * 0.20f, center.y)
                    close()
                }
                drawPath(
                    eye,
                    brass.copy(alpha = 0.075f + rankAlpha),
                    style = Stroke(stroke)
                )
                drawCircle(
                    brass.copy(alpha = 0.055f + rankAlpha),
                    size.minDimension * 0.13f,
                    center,
                    style = Stroke(stroke)
                )
                repeat(8) { index ->
                    val angle = Math.toRadians(-90.0 + index * 45.0)
                    val inner = size.minDimension * 0.19f
                    val outer = size.minDimension * 0.33f
                    drawLine(
                        brass.copy(alpha = 0.028f + rankAlpha * 0.28f),
                        Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * inner,
                            center.y + kotlin.math.sin(angle).toFloat() * inner
                        ),
                        Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * outer,
                            center.y + kotlin.math.sin(angle).toFloat() * outer
                        ),
                        stroke,
                        StrokeCap.Round
                    )
                }
            }

            PathGeometryKind.ASYMMETRIC_CONSTELLATION -> {
                val points = listOf(
                    Offset(w * 0.24f, h * 0.30f),
                    Offset(w * 0.43f, h * 0.20f),
                    Offset(w * 0.63f, h * 0.36f),
                    Offset(w * 0.76f, h * 0.23f),
                    Offset(w * 0.56f, h * 0.56f),
                    Offset(w * 0.32f, h * 0.52f)
                )
                points.zipWithNext().forEach { (a, b) ->
                    drawLine(
                        brass.copy(alpha = 0.040f + rankAlpha * 0.34f),
                        a,
                        b,
                        stroke
                    )
                }
                points.forEachIndexed { index, point ->
                    drawCircle(
                        brass.copy(alpha = 0.10f + rankAlpha * 0.72f),
                        radius = if (index % 2 == 0) 1.6.dp.toPx() else 1.dp.toPx(),
                        center = point
                    )
                }
                drawCircle(
                    brass.copy(alpha = 0.050f + rankAlpha * 0.30f),
                    radius = size.minDimension * 0.18f,
                    center = Offset(w * 0.48f, h * 0.39f),
                    style = Stroke(stroke)
                )
            }

            PathGeometryKind.CONCENTRIC_ARCHIVE -> {
                repeat(4) { index ->
                    val insetX = w * (0.20f + index * 0.055f)
                    val insetY = h * (0.18f + index * 0.045f)
                    drawRect(
                        color = brass.copy(
                            alpha = 0.030f + rankAlpha * (0.22f + index * 0.05f)
                        ),
                        topLeft = Offset(insetX, insetY),
                        size = androidx.compose.ui.geometry.Size(
                            w - insetX * 2f,
                            h * 0.48f - index * h * 0.055f
                        ),
                        style = Stroke(stroke)
                    )
                }
                repeat(5) { index ->
                    val y = h * (0.27f + index * 0.075f)
                    drawLine(
                        brass.copy(alpha = 0.032f + rankAlpha * 0.26f),
                        Offset(w * 0.31f, y),
                        Offset(w * 0.69f, y),
                        stroke
                    )
                }
            }

            PathGeometryKind.AXIAL_SPEAR -> {
                drawLine(
                    brass.copy(alpha = 0.085f + rankAlpha * 0.60f),
                    Offset(w * 0.50f, h * 0.12f),
                    Offset(w * 0.50f, h * 0.72f),
                    1.4.dp.toPx(),
                    StrokeCap.Round
                )
                repeat(4) { index ->
                    val y = h * (0.25f + index * 0.10f)
                    val spread = w * (0.08f + index * 0.025f)
                    drawLine(
                        brass.copy(alpha = 0.045f + rankAlpha * 0.34f),
                        Offset(w * 0.50f - spread, y),
                        Offset(w * 0.50f, y + h * 0.055f),
                        stroke
                    )
                    drawLine(
                        brass.copy(alpha = 0.045f + rankAlpha * 0.34f),
                        Offset(w * 0.50f + spread, y),
                        Offset(w * 0.50f, y + h * 0.055f),
                        stroke
                    )
                }
            }

            PathGeometryKind.ECLIPSE -> {
                val eclipseCenter = Offset(w * 0.50f, h * 0.37f)
                drawCircle(
                    brass.copy(alpha = 0.070f + rankAlpha * 0.55f),
                    size.minDimension * 0.24f,
                    eclipseCenter,
                    style = Stroke(1.2.dp.toPx())
                )
                drawCircle(
                    VeilPalette.Ink.copy(alpha = 0.94f),
                    size.minDimension * 0.215f,
                    Offset(
                        eclipseCenter.x + size.minDimension * 0.055f,
                        eclipseCenter.y - size.minDimension * 0.025f
                    )
                )
                repeat(6) { index ->
                    val y = h * (0.22f + index * 0.075f)
                    drawLine(
                        stone.copy(alpha = 0.055f + rankAlpha * 0.20f),
                        Offset(w * 0.18f, y),
                        Offset(w * 0.82f, y),
                        stroke
                    )
                }
            }

            PathGeometryKind.MECHANICAL -> {
                drawCircle(
                    brass.copy(alpha = 0.055f + rankAlpha * 0.42f),
                    size.minDimension * 0.24f,
                    center,
                    style = Stroke(stroke)
                )
                drawCircle(
                    brass.copy(alpha = 0.040f + rankAlpha * 0.32f),
                    size.minDimension * 0.13f,
                    center,
                    style = Stroke(stroke)
                )
                repeat(6) { index ->
                    val angle = Math.toRadians(-90.0 + index * 60.0)
                    val inner = size.minDimension * 0.14f
                    val outer = size.minDimension * 0.31f
                    drawLine(
                        brass.copy(alpha = 0.055f + rankAlpha * 0.40f),
                        Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * inner,
                            center.y + kotlin.math.sin(angle).toFloat() * inner
                        ),
                        Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * outer,
                            center.y + kotlin.math.sin(angle).toFloat() * outer
                        ),
                        stroke
                    )
                }
            }
        }

        drawLine(
            stone.copy(alpha = 0.12f),
            Offset(w * 0.08f, h * 0.86f),
            Offset(w * 0.92f, h * 0.86f),
            stroke
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
        animationSpec = tween(VeilMotion.SPATIAL_MS),
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
                if (nextRank == null) {
                    stringResource(R.string.path_ritual_complete)
                } else {
                    stringResource(R.string.path_next_threshold)
                },
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
                        if (nextRank == null) {
                            stringResource(R.string.path_remains_open)
                        } else {
                            nextRank
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon
                    )
                    Text(
                        if (nextRank == null) {
                            stringResource(R.string.path_no_higher_rank)
                        } else {
                            localizedRitualDescription(profile.path.id, profile.rankIndex)
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
                        .heightIn(min = 48.dp),
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
                        if (canAdvance) {
                            stringResource(R.string.path_perform_advancement)
                        } else {
                            stringResource(
                                R.string.path_keep_reading,
                                profile.ritualProgress,
                                target
                            )
                        },
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
            profile.path.ranks.forEachIndexed { index, _ ->
                val rank = localizedPathRank(profile.path, index)
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
                                stringResource(
                                    R.string.path_rank,
                                    (index + 1).toString().padStart(2, '0')
                                ),
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
                                    mastered -> stringResource(R.string.path_mastered)
                                    current -> stringResource(R.string.path_current_seal)
                                    else -> stringResource(R.string.path_sealed)
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
    val aspect = localizedPathAspect(path.id)

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
                    Text(localizedPathName(path), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$aspect · ${localizedPathEpithet(path)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Brass.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                localizedPathDescription(path),
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Mist
            )
            OutlinedButton(
                onClick = onChoose,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    if (enabled) stringResource(R.string.path_choose_this)
                    else stringResource(R.string.path_locked_after_advancement)
                )
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
    val invocation = localizedPathInvocation(profile.path.id)

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
                stringResource(R.string.path_advance_to, nextRank),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                Text(
                    stringResource(
                        R.string.path_advance_body,
                        localizedPathName(profile.path)
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    localizedRitualDescription(profile.path.id, profile.rankIndex),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist,
                    textAlign = TextAlign.Center
                )
                Text(
                    invocation,
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
            ) { Text(stringResource(R.string.path_advance)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.path_not_yet),
                    color = VeilPalette.Moon.copy(alpha = 0.72f)
                )
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
