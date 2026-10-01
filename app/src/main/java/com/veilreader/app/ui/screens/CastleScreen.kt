package com.veilreader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.veilreader.app.R
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.CastleMemoryState
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.WorldMutationLedger
import com.veilreader.app.domain.WorldProgressionProjection
import com.veilreader.app.domain.deriveWorldMutationLedger
import com.veilreader.app.domain.deriveWorldProgressionProjection
import com.veilreader.app.domain.ritualAfterglowIntensity
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.deriveCastleMemoryState
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.castleLayoutPolicyFor
import com.veilreader.app.ui.theme.grayfogAtmosphere
import kotlinx.coroutines.delay

/**
 * The Castle is a living map, not a dashboard.
 * Every chamber still routes to an existing useful Veil Reader surface.
 */
@Composable
fun CastleScreen(
    profile: ReaderProfile,
    onAdvanceRank: () -> Unit,
    onOpenRoom: (String) -> Unit,
    quests: List<Quest> = emptyList(),
    books: List<Book> = emptyList(),
    highlights: List<Highlight> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    readingCycles: List<ReadingCycleRecord> = emptyList()
) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)
    val awakenedRooms = SampleData.rooms.count { profile.rankIndex >= it.unlockRankIndex }
    val castleNowEpochMs by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(60_000L)
            value = System.currentTimeMillis()
        }
    }
    val memoryState = remember(
        books,
        highlights,
        bookmarks,
        readingSessions,
        readingCycles,
        castleNowEpochMs
    ) {
        deriveCastleMemoryState(
            books = books,
            highlights = highlights,
            bookmarks = bookmarks,
            sessions = readingSessions,
            readingCycles = readingCycles,
            nowEpochMs = castleNowEpochMs
        )
    }
    val worldProjection = remember(profile, quests, memoryState) {
        deriveWorldProgressionProjection(
            profile = profile,
            quests = quests,
            memory = memoryState
        )
    }
    val mutationLedger = remember(profile, memoryState) {
        deriveWorldMutationLedger(profile, memoryState)
    }
    val ritualAfterglow = remember(profile.ritualAftermath, castleNowEpochMs) {
        ritualAfterglowIntensity(profile.ritualAftermath, castleNowEpochMs)
    }
    val castleAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val castleLayout = castleLayoutPolicyFor(castleAdaptiveClass)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.CASTLE,
                seed = profile.rankIndex * 31 +
                    memoryState.volumeCount +
                    worldProjection.stage.ordinal * 101,
                intensity = (
                    0.82f +
                        worldProjection.architecturalPresence * 0.14f +
                        ritualAfterglow * 0.04f
                    ).coerceIn(0.82f, 0.98f)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = castleLayout.contentMaxWidthDp.dp)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = castleLayout.horizontalPaddingDp.dp,
                vertical = VeilSpacing.lg
            ),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
    ) {
        ScreenHeader(
            eyebrow = stringResource(R.string.castle_header_eyebrow),
            title = stringResource(R.string.castle_header_title),
            subtitle = localizedCastleMemoryNarrative(memoryState.memoryNarrative)
        )

        CastleKeep(
            profile = profile,
            memoryState = memoryState,
            canAdvance = canAdvance,
            awakenedRooms = awakenedRooms,
            totalRooms = SampleData.rooms.size,
            minHeightDp = castleLayout.keepMinHeightDp,
            onAdvanceRank = onAdvanceRank
        )

        CastleMemoryInscription(memoryState)
        CastleWorldProgressionInscription(worldProjection)
        CastleMutationInscription(memoryState)
        CastleRitualAftermath(profile, ritualAfterglow)
        CastleMutationLedgerSummary(mutationLedger)

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            VeilMicroLabel(
                text = stringResource(R.string.castle_inner_keep_eyebrow),
                strong = true
            )
            Text(
                stringResource(R.string.castle_awakened_chambers_title),
                style = MaterialTheme.typography.titleLarge,
                color = VeilPalette.Moon
            )
            Text(
                stringResource(R.string.castle_inner_keep_body),
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Mist
            )
        }

        CastleWorldMap(
            rankIndex = profile.rankIndex,
            memoryState = memoryState,
            mapHorizontalPaddingDp = castleLayout.mapHorizontalPaddingDp,
            chamberMinHeightDp = castleLayout.chamberMinHeightDp,
            onOpenRoom = onOpenRoom
        )

        BrassRule(Modifier.fillMaxWidth())

        Text(
            stringResource(R.string.castle_growth_note),
            modifier = Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.82f)
        )
    }
    }
}

@Composable
private fun CastleKeep(
    profile: ReaderProfile,
    memoryState: CastleMemoryState,
    canAdvance: Boolean,
    awakenedRooms: Int,
    totalRooms: Int,
    minHeightDp: Float,
    onAdvanceRank: () -> Unit
) {
    val formatNumber = rememberVeilIntegerFormatter()
    val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(1)
    val localizedRank = localizedRankName(profile.path.id, profile.rankIndex, profile.rankName)
    val localizedPath = localizedPathName(profile.path)
    val pathSummary = stringResource(
        if (profile.booksFinished == 1) R.string.castle_path_summary_one
        else R.string.castle_path_summary_many,
        localizedPath,
        profile.booksFinished
    )
    val targetProgress = (profile.rankIndex.toFloat() / finalRank).coerceIn(0f, 1f)
    val reducedMotion = LocalVeilReducedMotion.current
    val castleProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(VeilMotion.SPATIAL_MS)
        },
        label = "castle-tier-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeightDp.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF17140F),
                        VeilPalette.Archive.copy(alpha = 0.98f),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f)),
                MaterialTheme.shapes.small
            )
    ) {
        CastleKeepBackdrop(
            modifier = Modifier.matchParentSize(),
            rankIndex = profile.rankIndex,
            rankCount = profile.path.ranks.size,
            memoryState = memoryState
        )

        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(VeilPalette.Ink.copy(alpha = 0.52f))
                        .border(
                            BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.48f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CastleCrest(
                        modifier = Modifier.size(34.dp),
                        tint = VeilPalette.Brass
                    )
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.castle_keep_tier, profile.rankIndex + 1),
                        strong = true
                    )
                    Text(
                        localizedRank,
                        style = MaterialTheme.typography.headlineSmall,
                        color = VeilPalette.Moon
                    )
                    Text(
                        pathSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist
                    )
                }

                Text(
                    "${formatNumber(profile.rankIndex + 1)}/${formatNumber(profile.path.ranks.size)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = VeilPalette.Brass
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VeilMicroLabel(
                    text = stringResource(R.string.castle_awakened_chambers_eyebrow),
                    color = VeilPalette.Mist.copy(alpha = 0.72f)
                )
                Text(
                    "${formatNumber(awakenedRooms)}/${formatNumber(totalRooms.coerceAtLeast(1))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Brass
                )
            }

            LinearProgressIndicator(
                progress = { castleProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = VeilPalette.Brass,
                trackColor = VeilPalette.Moon.copy(alpha = 0.10f),
                drawStopIndicator = {}
            )

            if (canAdvance) {
                Button(
                    onClick = onAdvanceRank,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.castle_perform_advancement),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CastleMemoryInscription(memory: CastleMemoryState) {
    val volumesText = stringResource(R.string.castle_memory_volumes, memory.volumeCount)
    val passagesText = stringResource(R.string.castle_memory_passages, memory.passageCount)
    val recordsText = stringResource(R.string.castle_memory_records, memory.sealedCapsuleCount)
    val linksText = stringResource(R.string.castle_memory_links, memory.atlasLinkCount)
    val facts = buildList {
        add(volumesText)
        if (memory.passageCount > 0) add(passagesText)
        if (memory.sealedCapsuleCount > 0) add(recordsText)
        if (memory.atlasLinkCount > 0) add(linksText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VeilMicroLabel(
            text = stringResource(R.string.castle_foundation_memory),
            color = VeilPalette.Brass.copy(alpha = 0.78f)
        )
        Text(
            facts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.72f)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VeilPalette.Brass.copy(alpha = 0.34f),
                            VeilPalette.Brass.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun CastleWorldProgressionInscription(world: WorldProgressionProjection) {
    val stage = localizedWorldStage(world.stage)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VeilMicroLabel(
            text = stringResource(R.string.castle_world_label, stage),
            color = VeilPalette.Brass.copy(alpha = 0.82f)
        )
        Text(
            localizedWorldInscription(world.inscriptionKind),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.76f)
        )
        LinearProgressIndicator(
            progress = { world.architecturalPresence },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = VeilPalette.Brass.copy(alpha = 0.82f),
            trackColor = VeilPalette.Moon.copy(alpha = 0.08f),
            drawStopIndicator = {}
        )
    }
}

@Composable
private fun CastleMutationInscription(memory: CastleMemoryState) {
    if (
        memory.mutationInscription.isBlank() ||
        memory == CastleMemoryState.EMPTY
    ) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.46f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.20f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            localizedCastleMutationSignal(memory.mutationSignal),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.80f)
        )
    }
}

@Composable
private fun CastleRitualAftermath(
    profile: ReaderProfile,
    afterglow: Float
) {
    val aftermath = profile.ritualAftermath ?: return
    val fromFallback = profile.path.ranks.getOrNull(aftermath.fromRankIndex) ?: return
    val toFallback = profile.path.ranks.getOrNull(aftermath.toRankIndex) ?: return
    val fromRank = localizedRankName(profile.path.id, aftermath.fromRankIndex, fromFallback)
    val toRank = localizedRankName(profile.path.id, aftermath.toRankIndex, toFallback)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Ink.copy(alpha = 0.56f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.24f + afterglow * 0.36f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VeilMicroLabel(
                text = stringResource(R.string.castle_sealed_advancement),
                strong = true
            )
            Text(
                "$fromRank → $toRank",
                style = MaterialTheme.typography.titleSmall,
                color = VeilPalette.Moon
            )
            Text(
                stringResource(
                    if (afterglow > 0f) R.string.castle_afterglow_active
                    else R.string.castle_afterglow_faded
                ),
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.74f)
            )
        }
    }
}

@Composable
private fun CastleMutationLedgerSummary(ledger: WorldMutationLedger) {
    if (ledger.entries.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VeilMicroLabel(
            text = stringResource(R.string.castle_world_mutations, ledger.durableCount),
            color = VeilPalette.Brass.copy(alpha = 0.80f)
        )
        ledger.entries.take(3).forEach { entry ->
            val durability = stringResource(
                if (entry.durable) R.string.castle_mutation_durable
                else R.string.castle_mutation_transient
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    localizedWorldMutationTitle(entry.kind),
                    style = MaterialTheme.typography.titleSmall,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(
                        R.string.castle_mutation_evidence,
                        entry.evidenceCount,
                        durability
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.68f)
                )
            }
        }
    }
}

@Composable
private fun CastleKeepBackdrop(
    rankIndex: Int,
    rankCount: Int,
    memoryState: CastleMemoryState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val brass = VeilPalette.Brass
        val stone = VeilPalette.StrongBorderDark
        val baseY = h * 0.86f
        val towerBottom = h * 0.78f
        val rankGlow = ((rankIndex + 1f) / rankCount.coerceAtLeast(1)).coerceIn(0f, 1f)
        val memoryGlow = memoryState.overallPresence
        val glow = (rankGlow * 0.52f + memoryGlow * 0.48f).coerceIn(0f, 1f)

        drawRect(
            color = Color(0xFF07090C).copy(alpha = 0.54f),
            topLeft = Offset(w * 0.17f, h * 0.38f),
            size = Size(w * 0.66f, h * 0.44f)
        )
        drawRect(
            color = Color(0xFF07090C).copy(alpha = 0.66f),
            topLeft = Offset(w * 0.10f, h * 0.48f),
            size = Size(w * 0.16f, h * 0.34f)
        )
        drawRect(
            color = Color(0xFF07090C).copy(alpha = 0.66f),
            topLeft = Offset(w * 0.74f, h * 0.48f),
            size = Size(w * 0.16f, h * 0.34f)
        )

        val roof = Path().apply {
            moveTo(w * 0.30f, h * 0.38f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.70f, h * 0.38f)
        }
        drawPath(
            roof,
            color = brass.copy(alpha = 0.18f + glow * 0.20f),
            style = Stroke(
                width = 1.25.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        listOf(0.18f, 0.34f, 0.50f, 0.66f, 0.82f).forEachIndexed { index, x ->
            drawLine(
                stone.copy(alpha = if (index == 2) 0.20f else 0.11f),
                Offset(w * x, h * 0.34f),
                Offset(w * x, towerBottom),
                1.dp.toPx()
            )
        }

        repeat(memoryState.shelfRibs) { index ->
            val fraction = (index + 1f) / (memoryState.shelfRibs + 1f)
            val y = h * (0.51f + fraction * 0.24f)
            val alpha = 0.045f + memoryState.libraryResonance * 0.08f
            drawLine(
                brass.copy(alpha = alpha),
                Offset(w * 0.115f, y),
                Offset(w * 0.245f, y),
                0.75.dp.toPx()
            )
            drawLine(
                brass.copy(alpha = alpha),
                Offset(w * 0.755f, y),
                Offset(w * 0.885f, y),
                0.75.dp.toPx()
            )
        }

        repeat(9) { index ->
            val row = index / 3
            val col = index % 3
            val x = w * (0.38f + col * 0.12f)
            val y = h * (0.47f + row * 0.095f)
            val lit = index < memoryState.litWindows
            drawRoundRect(
                color = if (lit) {
                    brass.copy(alpha = 0.14f + memoryGlow * 0.18f)
                } else {
                    stone.copy(alpha = 0.075f)
                },
                topLeft = Offset(x, y),
                size = Size(w * 0.035f, h * 0.046f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }

        repeat(memoryState.starPoints) { index ->
            val xUnit = ((index * 37 + 11) % 97) / 96f
            val yUnit = ((index * 53 + 7) % 29) / 28f
            drawCircle(
                color = VeilPalette.Spirit.copy(
                    alpha = 0.12f + memoryState.observatoryResonance * 0.22f
                ),
                radius = if (index % 4 == 0) 1.2.dp.toPx() else 0.72.dp.toPx(),
                center = Offset(
                    w * (0.12f + xUnit * 0.76f),
                    h * (0.08f + yUnit * 0.20f)
                )
            )
        }

        repeat(memoryState.sealedCapsuleCount.coerceAtMost(7)) { index ->
            val x = w * (0.34f + index * 0.053f)
            drawCircle(
                color = brass.copy(
                    alpha = 0.16f + memoryState.treasuryResonance * 0.18f
                ),
                radius = 3.2.dp.toPx(),
                center = Offset(x, baseY - 6.dp.toPx()),
                style = Stroke(0.8.dp.toPx())
            )
        }

        // Durable reading history becomes architecture, never a second unlock system.
        repeat(memoryState.foundationCourses.coerceIn(2, 10)) { index ->
            val y = baseY + (index - 1) * 2.1.dp.toPx()
            drawLine(
                color = stone.copy(alpha = 0.06f + memoryState.overallPresence * 0.05f),
                start = Offset(w * 0.18f, y),
                end = Offset(w * 0.82f, y),
                strokeWidth = 0.72.dp.toPx()
            )
        }
        repeat(memoryState.scriptoriumLamps.coerceIn(0, 7)) { index ->
            val x = w * (0.24f + index * 0.085f)
            drawCircle(
                color = brass.copy(alpha = 0.10f + memoryState.archiveResonance * 0.16f),
                radius = 2.1.dp.toPx(),
                center = Offset(x, h * 0.44f)
            )
        }
        repeat(memoryState.rereadRings.coerceIn(0, 6)) { index ->
            drawCircle(
                color = brass.copy(alpha = 0.035f + memoryState.patina * 0.06f),
                radius = size.minDimension * (0.08f + index * 0.026f),
                center = Offset(w * 0.50f, h * 0.64f),
                style = Stroke(0.65.dp.toPx())
            )
        }

        drawLine(
            brass.copy(alpha = 0.16f + memoryGlow * 0.10f),
            Offset(w * 0.08f, baseY),
            Offset(w * 0.92f, baseY),
            1.dp.toPx()
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    VeilPalette.Ink.copy(alpha = memoryState.fogAlpha)
                ),
                startY = h * 0.58f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.56f),
            size = Size(w, h * 0.44f)
        )
    }
}

@Composable
private fun CastleWorldMap(
    rankIndex: Int,
    memoryState: CastleMemoryState,
    mapHorizontalPaddingDp: Float,
    chamberMinHeightDp: Float,
    onOpenRoom: (String) -> Unit
) {
    val rooms = SampleData.rooms
    val reducedMotion = LocalVeilReducedMotion.current
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(reducedMotion) { revealed = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090C10),
                        VeilPalette.Archive.copy(alpha = 0.98f),
                        Color(0xFF0B1016),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.28f)),
                MaterialTheme.shapes.small
            )
    ) {
        CastleArchitectureBackdrop(
            modifier = Modifier.matchParentSize(),
            rankIndex = rankIndex,
            roomCount = rooms.size,
            memoryState = memoryState
        )

        AnimatedVisibility(
            visible = revealed,
            enter = if (reducedMotion) {
                fadeIn(tween(VeilMotion.REDUCED_MOTION_FADE_MS))
            } else {
                fadeIn(tween(VeilMotion.SPATIAL_MS)) +
                    slideInVertically(tween(VeilMotion.SPATIAL_MS)) { it / 18 }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = mapHorizontalPaddingDp.dp,
                        vertical = 26.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                CastleGateLabel(
                    title = stringResource(R.string.castle_crown),
                    subtitle = stringResource(R.string.castle_upper_halls)
                )

                rooms.forEachIndexed { index, room ->
                    val localizedRoomName = localizedCastleRoomName(room.id, room.name)
                    val localizedRoomPurpose = localizedCastleRoomPurpose(room.id, room.purpose)
                    VeilReveal(
                        delayMillis = 90 + index * 70,
                        distance = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CastleFloor(
                            floor = rooms.size - index,
                            id = room.id,
                            name = localizedRoomName,
                            purpose = localizedRoomPurpose,
                            unlockRank = room.unlockRankIndex,
                            unlocked = rankIndex >= room.unlockRankIndex,
                            resonance = memoryState.resonanceFor(room.id),
                            roomOnLeft = index % 2 == 0,
                            isLast = index == rooms.lastIndex,
                            chamberMinHeightDp = chamberMinHeightDp,
                            onOpenRoom = onOpenRoom
                        )
                    }
                }

                CastleGateLabel(
                    title = stringResource(R.string.castle_foundation),
                    subtitle = stringResource(R.string.castle_first_stone)
                )
            }
        }
    }
}

@Composable
private fun CastleArchitectureBackdrop(
    rankIndex: Int,
    roomCount: Int,
    memoryState: CastleMemoryState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f

        drawLine(
            VeilPalette.StrongBorderDark.copy(alpha = 0.18f),
            Offset(w * 0.08f, 0f),
            Offset(w * 0.17f, h),
            1.dp.toPx()
        )
        drawLine(
            VeilPalette.StrongBorderDark.copy(alpha = 0.18f),
            Offset(w * 0.92f, 0f),
            Offset(w * 0.83f, h),
            1.dp.toPx()
        )

        drawLine(
            VeilPalette.Brass.copy(alpha = 0.14f),
            Offset(centerX, 26.dp.toPx()),
            Offset(centerX, h - 26.dp.toPx()),
            1.dp.toPx()
        )

        repeat(3) { index ->
            val inset = 0.12f + index * 0.055f
            drawArc(
                color = VeilPalette.Brass.copy(alpha = 0.045f + index * 0.012f),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(w * inset, h * (0.03f + index * 0.045f)),
                size = Size(w * (1f - inset * 2f), h * (0.34f + index * 0.03f)),
                style = Stroke(1.dp.toPx())
            )
        }

        val floors = roomCount.coerceAtLeast(1)
        repeat(floors + 1) { index ->
            val y = h * ((index + 1f) / (floors + 2f))
            drawLine(
                VeilPalette.StrongBorderDark.copy(alpha = 0.09f),
                Offset(w * 0.12f, y),
                Offset(w * 0.88f, y),
                1.dp.toPx()
            )
        }

        drawArc(
            color = VeilPalette.Brass.copy(alpha = 0.14f),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, -h * 0.02f),
            size = Size(w * 0.60f, h * 0.24f),
            style = Stroke(1.1.dp.toPx())
        )

        val unlockedFraction =
            ((rankIndex + 1f) / roomCount.coerceAtLeast(1)).coerceIn(0f, 1f)
        val glowHeight = h * unlockedFraction * 0.42f
        drawLine(
            VeilPalette.Brass.copy(
                alpha = 0.24f + memoryState.overallPresence * 0.18f
            ),
            Offset(centerX, h - 34.dp.toPx()),
            Offset(centerX, h - 34.dp.toPx() - glowHeight),
            2.dp.toPx(),
            StrokeCap.Round
        )

        repeat(memoryState.starPoints.coerceAtMost(14)) { index ->
            val x = w * (0.16f + ((index * 41 + 13) % 71) / 100f)
            val y = h * (0.035f + ((index * 29 + 5) % 18) / 100f)
            drawCircle(
                color = VeilPalette.Spirit.copy(
                    alpha = 0.09f + memoryState.observatoryResonance * 0.16f
                ),
                radius = if (index % 5 == 0) 1.dp.toPx() else 0.6.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    VeilPalette.Ink.copy(alpha = memoryState.fogAlpha * 0.66f)
                ),
                startY = h * 0.72f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.70f),
            size = Size(w, h * 0.30f)
        )
    }
}

@Composable
private fun CastleFloor(
    floor: Int,
    id: String,
    name: String,
    purpose: String,
    unlockRank: Int,
    unlocked: Boolean,
    resonance: Float,
    roomOnLeft: Boolean,
    isLast: Boolean,
    chamberMinHeightDp: Float,
    onOpenRoom: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(
                        if (unlocked) (15f + resonance.coerceIn(0f, 1f) * 5f).dp
                        else 14.dp
                    )
                    .clip(CircleShape)
                    .background(
                        if (unlocked) {
                            VeilPalette.Brass.copy(
                                alpha = 0.62f + resonance.coerceIn(0f, 1f) * 0.38f
                            )
                        } else {
                            VeilPalette.BorderDark
                        }
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (unlocked) VeilPalette.Moon.copy(alpha = 0.42f)
                            else VeilPalette.StrongBorderDark.copy(alpha = 0.54f)
                        ),
                        CircleShape
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (roomOnLeft) {
                    CastleChamberNode(
                        id = id,
                        name = name,
                        purpose = purpose,
                        unlockRank = unlockRank,
                        unlocked = unlocked,
                        resonance = resonance,
                        chamberMinHeightDp = chamberMinHeightDp,
                        onOpenRoom = onOpenRoom,
                        modifier = Modifier.weight(1f)
                    )
                    CastleBridge(unlocked = unlocked, resonance = resonance)
                    FloorInscription(
                        floor = floor,
                        unlocked = unlocked,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    FloorInscription(
                        floor = floor,
                        unlocked = unlocked,
                        modifier = Modifier.weight(1f)
                    )
                    CastleBridge(unlocked = unlocked, resonance = resonance)
                    CastleChamberNode(
                        id = id,
                        name = name,
                        purpose = purpose,
                        unlockRank = unlockRank,
                        unlocked = unlocked,
                        resonance = resonance,
                        chamberMinHeightDp = chamberMinHeightDp,
                        onOpenRoom = onOpenRoom,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (!isLast) {
            Box(
                Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(
                        if (unlocked) {
                            VeilPalette.Brass.copy(alpha = 0.24f)
                        } else {
                            VeilPalette.BorderDark.copy(alpha = 0.62f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun CastleBridge(
    unlocked: Boolean,
    resonance: Float
) {
    Box(
        Modifier
            .width(20.dp)
            .height(1.dp)
            .background(
                if (unlocked) {
                    VeilPalette.Brass.copy(
                        alpha = 0.24f + resonance.coerceIn(0f, 1f) * 0.34f
                    )
                } else {
                    VeilPalette.BorderDark.copy(alpha = 0.62f)
                }
            )
    )
}

@Composable
private fun FloorInscription(
    floor: Int,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VeilMicroLabel(
            text = stringResource(R.string.castle_floor, floor.toString().padStart(2, '0')),
            color = if (unlocked) {
                VeilPalette.Brass.copy(alpha = 0.78f)
            } else {
                VeilPalette.Mist.copy(alpha = 0.50f)
            }
        )
        Text(
            stringResource(
                if (unlocked) R.string.castle_awakened else R.string.castle_silent
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) {
                VeilPalette.Moon.copy(alpha = 0.62f)
            } else {
                VeilPalette.Mist.copy(alpha = 0.42f)
            }
        )
    }
}

@Composable
private fun CastleChamberNode(
    id: String,
    name: String,
    purpose: String,
    unlockRank: Int,
    unlocked: Boolean,
    resonance: Float,
    chamberMinHeightDp: Float,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeResonance = resonance.coerceIn(0f, 1f)
    val chamberDescription = if (unlocked) {
        stringResource(R.string.castle_chamber_open_semantics, name, purpose)
    } else {
        stringResource(R.string.castle_chamber_sealed_semantics, name, unlockRank + 1)
    }
    val sealedBody = stringResource(R.string.castle_awakens_rank, unlockRank + 1)
    val actionLabel = stringResource(
        if (unlocked) R.string.castle_enter else R.string.castle_sealed
    )
    val edge = if (unlocked) {
        VeilPalette.Brass.copy(alpha = 0.42f + safeResonance * 0.36f)
    } else {
        VeilPalette.BorderDark.copy(alpha = 0.86f)
    }

    Column(
        modifier = modifier
            .heightIn(min = chamberMinHeightDp.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.verticalGradient(
                    if (unlocked) {
                        listOf(
                            VeilPalette.DeepBrass.copy(alpha = 0.12f + safeResonance * 0.26f),
                            VeilPalette.RaisedIron.copy(alpha = 0.24f + safeResonance * 0.14f),
                            VeilPalette.Archive.copy(alpha = 0.93f)
                        )
                    } else {
                        listOf(
                            VeilPalette.Iron.copy(alpha = 0.34f),
                            VeilPalette.Ink.copy(alpha = 0.88f)
                        )
                    }
                )
            )
            .border(BorderStroke(1.dp, edge), MaterialTheme.shapes.extraSmall)
            .semantics {
                contentDescription = chamberDescription
            }
            .clickable(
                enabled = unlocked,
                role = Role.Button
            ) { onOpenRoom(id) }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .width(50.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(
                    if (unlocked) VeilPalette.DeepBrass.copy(alpha = 0.38f)
                    else VeilPalette.Ink.copy(alpha = 0.74f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (unlocked) VeilPalette.Brass.copy(alpha = 0.46f)
                        else VeilPalette.BorderDark
                    ),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            CastleRoomIcon(
                id = id,
                unlocked = unlocked,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(7.dp))

        if (unlocked && safeResonance > 0.01f) {
            Box(
                Modifier
                    .width((26f + safeResonance * 34f).dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                VeilPalette.Brass.copy(
                                    alpha = 0.24f + safeResonance * 0.46f
                                ),
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(Modifier.height(5.dp))
        }

        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (unlocked) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.58f)
        )

        Text(
            if (unlocked) purpose else sealedBody,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (unlocked) VeilPalette.Mist else VeilPalette.Mist.copy(alpha = 0.46f)
        )

        Spacer(Modifier.height(5.dp))

        VeilMicroLabel(
            text = actionLabel,
            color = if (unlocked) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.44f)
        )
    }
}

@Composable
private fun CastleGateLabel(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        VeilMicroLabel(
            text = title,
            color = VeilPalette.Brass.copy(alpha = 0.80f),
            strong = true
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.58f)
        )
    }
}

@Composable
private fun CastleCrest(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val stroke = Stroke(1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val top = h * .22f
        val mid = h * .46f
        drawLine(tint, Offset(w * .16f, mid), Offset(w * .84f, mid), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .22f, mid), Offset(w * .22f, h * .84f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .78f, mid), Offset(w * .78f, h * .84f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .22f, h * .84f), Offset(w * .78f, h * .84f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .22f, top), Offset(w * .22f, mid), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .78f, top), Offset(w * .78f, mid), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .14f, top), Offset(w * .31f, top), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .69f, top), Offset(w * .86f, top), stroke.width, StrokeCap.Round)
        drawRoundRect(
            tint,
            topLeft = Offset(w * .43f, h * .64f),
            size = Size(w * .14f, h * .20f),
            cornerRadius = CornerRadius(w * .07f),
            style = stroke
        )
    }
}

@Composable
private fun CastleRoomIcon(
    id: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (unlocked) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.44f)

    Canvas(modifier) {
        val stroke = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        when (id) {
            "library" -> {
                repeat(3) { index ->
                    val left = w * (.12f + index * .28f)
                    drawRoundRect(
                        tint,
                        topLeft = Offset(left, h * .18f + if (index == 1) h * .06f else 0f),
                        size = Size(w * .20f, h * .64f - if (index == 1) h * .06f else 0f),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = stroke
                    )
                }
            }
            "ritual" -> {
                val p = Path().apply {
                    moveTo(w * .50f, h * .08f)
                    lineTo(w * .62f, h * .38f)
                    lineTo(w * .92f, h * .50f)
                    lineTo(w * .62f, h * .62f)
                    lineTo(w * .50f, h * .92f)
                    lineTo(w * .38f, h * .62f)
                    lineTo(w * .08f, h * .50f)
                    lineTo(w * .38f, h * .38f)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            "observatory" -> {
                drawCircle(tint, w * .30f, Offset(w * .48f, h * .48f), style = stroke)
                drawCircle(tint, w * .08f, Offset(w * .48f, h * .48f))
                drawLine(
                    tint,
                    Offset(w * .66f, h * .70f),
                    Offset(w * .86f, h * .88f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
            "archive" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * .14f, h * .26f),
                    size = Size(w * .72f, h * .56f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(
                    tint,
                    Offset(w * .14f, h * .42f),
                    Offset(w * .86f, h * .42f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .40f, h * .56f),
                    Offset(w * .60f, h * .56f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
            "treasury" -> {
                val p = Path().apply {
                    moveTo(w * .50f, h * .10f)
                    lineTo(w * .86f, h * .40f)
                    lineTo(w * .66f, h * .88f)
                    lineTo(w * .34f, h * .88f)
                    lineTo(w * .14f, h * .40f)
                    close()
                }
                drawPath(p, tint, style = stroke)
                drawLine(
                    tint,
                    Offset(w * .14f, h * .40f),
                    Offset(w * .86f, h * .40f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
            "sanctum" -> {
                drawCircle(tint, w * .29f, Offset(w * .50f, h * .50f), style = stroke)
                drawCircle(tint, w * .10f, Offset(w * .50f, h * .50f), style = stroke)
                drawLine(
                    tint,
                    Offset(w * .50f, h * .06f),
                    Offset(w * .50f, h * .20f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .50f, h * .80f),
                    Offset(w * .50f, h * .94f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .06f, h * .50f),
                    Offset(w * .20f, h * .50f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .80f, h * .50f),
                    Offset(w * .94f, h * .50f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
        }
    }
}
