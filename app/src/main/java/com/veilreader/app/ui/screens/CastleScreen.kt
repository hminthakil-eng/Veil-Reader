package com.veilreader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.veilreader.app.domain.PathArchitecturalMotif
import com.veilreader.app.domain.PathWorldSignature
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.RitualAftermathRecord
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.WorldMutationLedger
import com.veilreader.app.domain.WorldMutationRealm
import com.veilreader.app.domain.WorldProgressionProjection
import com.veilreader.app.domain.deriveWorldMutationLedger
import com.veilreader.app.domain.deriveWorldProgressionProjection
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.deriveCastleMemoryState
import com.veilreader.app.domain.derivePathWorldSignature
import com.veilreader.app.domain.ritualAfterglowIntensity
import com.veilreader.app.ui.VeilEyebrowText
import com.veilreader.app.ui.VeilMastheadMetaRow
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.hallRouteUsesTabTransition
import com.veilreader.app.ui.hallSharedBoundsKey
import com.veilreader.app.ui.rememberVeilTouchExplorationEnabled
import com.veilreader.app.ui.veilSharedBounds
import com.veilreader.app.ui.veilTabSharedBounds
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilLanguage
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.LocalVeilScriptGroup
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.CathedralMotionClass
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.castleLayoutPolicyFor
import com.veilreader.app.ui.theme.appMetadataDivider
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.narrativeArchitectureField
import com.veilreader.app.ui.theme.currentVeilTemporalPhase
import com.veilreader.app.ui.theme.localizeAppNumerals
import com.veilreader.app.ui.theme.localizedMetadataValue
import com.veilreader.app.ui.theme.motionBudgetFor

/**
 * The Castle is a living map, not a dashboard.
 * Every chamber still routes to an existing useful Veil Reader surface.
 */
@Composable
fun CastleScreen(
    profile: ReaderProfile,
    onOpenRoom: (String) -> Unit,
    quests: List<Quest> = emptyList(),
    books: List<Book> = emptyList(),
    highlights: List<Highlight> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    readingCycles: List<ReadingCycleRecord> = emptyList(),
    memoryStateOverride: CastleMemoryState? = null,
    silentNamesSealed: Boolean = false,
    silentNamesReceiptPathId: String? = null,
    silentNamesStorageBlocked: Boolean = false
) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)
    val awakenedRooms = SampleData.rooms.count { profile.rankIndex >= it.unlockRankIndex }
    val livingMirrorNoteCount = highlights.count { it.note.isNotBlank() }
    val touchExplorationEnabled = rememberVeilTouchExplorationEnabled()
    var greatHallMode by remember { mutableStateOf(GreatHallMode.HALL) }
    val effectiveGreatHallMode =
        if (touchExplorationEnabled) GreatHallMode.REGISTRY else greatHallMode
    val temporalPhase = currentVeilTemporalPhase()
    val castleNowEpochMs = remember(
        temporalPhase,
        books,
        highlights,
        bookmarks,
        readingSessions,
        readingCycles
    ) { System.currentTimeMillis() }
    val memoryState = remember(
        memoryStateOverride,
        books,
        highlights,
        bookmarks,
        readingSessions,
        readingCycles,
        castleNowEpochMs
    ) {
        memoryStateOverride ?: deriveCastleMemoryState(
            books = books,
            highlights = highlights,
            bookmarks = bookmarks,
            sessions = readingSessions,
            readingCycles = readingCycles,
            nowEpochMs = castleNowEpochMs
        )
    }
    val mutationLedger = remember(profile, memoryState) {
        deriveWorldMutationLedger(profile, memoryState)
    }
    val ritualAfterglow = remember(profile.ritualAftermath, castleNowEpochMs) {
        ritualAfterglowIntensity(
            record = profile.ritualAftermath,
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
    val castleAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val castleLayout = castleLayoutPolicyFor(castleAdaptiveClass)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.CASTLE,
                seed = profile.rankIndex * 31 + memoryState.volumeCount + worldProjection.stage.ordinal * 101,
                intensity = (
                    0.82f +
                        worldProjection.architecturalPresence * 0.14f +
                        ritualAfterglow * 0.04f
                    ).coerceIn(0.82f, 0.98f),
                temporalPhase = temporalPhase
            )
            .narrativeArchitectureField(
                realm = VeilRealm.CASTLE,
                seed = profile.rankIndex * 31 + memoryState.volumeCount + worldProjection.stage.ordinal * 101,
                intensity = (
                    0.62f +
                        worldProjection.architecturalPresence * 0.26f +
                        ritualAfterglow * 0.06f
                    ).coerceIn(0.62f, 0.94f)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            alpha = (
                0.26f +
                    worldProjection.architecturalPresence * 0.16f +
                    ritualAfterglow * 0.04f
                ).coerceIn(0.26f, 0.46f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(760.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(820.dp)
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.06f),
                        0.38f to Color.Transparent,
                        0.72f to VeilPalette.Ink.copy(alpha = 0.62f),
                        1f to VeilPalette.Ink
                    )
                )
        )

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
        CastleGrandMasthead(
            memoryState = memoryState,
            worldProjection = worldProjection,
            awakenedRooms = awakenedRooms,
            totalRooms = SampleData.rooms.size
        )

        CastleKeep(
            profile = profile,
            memoryState = memoryState,
            worldProjection = worldProjection,
            canAdvance = canAdvance,
            awakenedRooms = awakenedRooms,
            totalRooms = SampleData.rooms.size,
            minHeightDp = castleLayout.keepMinHeightDp,
            onOpenRitual = { onOpenRoom("ritual") }
        )

        CastleRitualAftermath(
            profile = profile,
            aftermath = profile.ritualAftermath,
            afterglow = ritualAfterglow
        )
        CastleMemoryInscription(memoryState)
        CastleWorldProgressionInscription(worldProjection)
        CastleMutationInscription(memoryState)
        CastleWorldMutationLedger(mutationLedger)

        SilentNamesHallPortal(
            sealed = silentNamesSealed,
            receiptPathId = silentNamesReceiptPathId,
            storageBlocked = silentNamesStorageBlocked,
            onOpen = { onOpenRoom(SILENT_NAMES_HALL_ROUTE) }
        )

        GreatHallArtifactNavigator(
            profile = profile,
            memoryState = memoryState,
            worldProjection = worldProjection,
            livingMirrorNoteCount = livingMirrorNoteCount,
            canAdvance = canAdvance,
            mode = effectiveGreatHallMode,
            hallModeEnabled = !touchExplorationEnabled,
            onModeChange = { greatHallMode = it },
            onOpenArtifact = onOpenRoom
        )

        BrassRule(Modifier.fillMaxWidth())

        Text(
            "Nothing in the Castle is sold or time-gated. It grows from reading progress already stored on this device.",
            modifier = Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.82f)
        )
    }
    }
}

private enum class GreatHallMode { HALL, REGISTRY }

internal const val SILENT_NAMES_HALL_ROUTE = "silent_names"

@Composable
private fun SilentNamesHallPortal(
    sealed: Boolean,
    receiptPathId: String?,
    storageBlocked: Boolean,
    onOpen: () -> Unit
) {
    val title = stringResource(R.string.silent_names_title)
    val action = stringResource(
        when {
            storageBlocked -> R.string.silent_names_inspect_preserved
            sealed -> R.string.silent_names_revisit
            else -> R.string.silent_names_enter
        }
    )
    val summary = stringResource(
        when {
            storageBlocked -> R.string.silent_names_record_unavailable_short
            sealed -> silentNamesHallAfterglowRes(receiptPathId)
            else -> R.string.silent_names_intro
        }
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .semantics {
                contentDescription = "$title. $action"
            }
            .clickable(
                role = Role.Button,
                onClick = onOpen
            ),
        color = VeilPalette.RaisedIron.copy(alpha = 0.78f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = if (sealed) 0.62f else 0.40f)
        )
    ) {
        Row(
            modifier = Modifier.padding(VeilSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(68.dp)
                    .border(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.34f),
                        CircleShape
                    )
                    .padding(8.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val strong = 1.2.dp.toPx()
                val thin = 0.75.dp.toPx()
                drawCircle(
                    VeilPalette.Brass.copy(alpha = 0.72f),
                    size.minDimension * 0.38f,
                    center,
                    style = Stroke(strong)
                )
                drawLine(
                    VeilPalette.Brass.copy(alpha = 0.48f),
                    Offset(center.x, size.height * 0.12f),
                    Offset(center.x, size.height * 0.88f),
                    strong
                )
                drawArc(
                    VeilPalette.Brass.copy(alpha = if (sealed) 0.92f else 0.56f),
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.20f),
                    size = Size(size.width * 0.56f, size.height * 0.60f),
                    style = Stroke(thin)
                )
                if (sealed) {
                    drawCircle(
                        VeilPalette.Brass,
                        2.4.dp.toPx(),
                        Offset(center.x, size.height * 0.69f)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VeilEyebrowText(
                    text = stringResource(R.string.silent_names_hall_eyebrow),
                    color = VeilPalette.Brass.copy(alpha = 0.86f),
                    trackingSp = 1.2f
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    action,
                    style = MaterialTheme.typography.labelLarge,
                    color = VeilPalette.Brass
                )
            }
        }
    }
}

private fun silentNamesHallAfterglowRes(pathId: String?): Int = when (pathId) {
    "oracle" -> R.string.silent_names_hall_afterglow_oracle
    "dreamwalker" -> R.string.silent_names_hall_afterglow_dreamwalker
    "archivist" -> R.string.silent_names_hall_afterglow_archivist
    "vanguard" -> R.string.silent_names_hall_afterglow_vanguard
    "nocturne" -> R.string.silent_names_hall_afterglow_nocturne
    "artificer" -> R.string.silent_names_hall_afterglow_artificer
    else -> R.string.silent_names_saved
}

internal enum class GreatHallArtifactKind {
    MIRROR,
    ASTROLABE,
    ARCHIVE_GATE,
    LEDGER,
    RITUAL_SEAL,
    RELIQUARY,
    VEILED_DOOR,
    READING_SEAT
}

internal data class GreatHallArtifact(
    val kind: GreatHallArtifactKind,
    val title: String,
    val subtitle: String,
    val route: String,
    val unlockRank: Int,
    val resonance: Float,
    val awakened: Boolean
) {
    val id: String get() = kind.name.lowercase()
}

internal fun greatHallArtifacts(
    profile: ReaderProfile,
    memoryState: CastleMemoryState,
    worldProjection: WorldProgressionProjection = deriveWorldProgressionProjection(
        profile = profile,
        quests = emptyList(),
        memory = memoryState
    ),
    livingMirrorNoteCount: Int,
    canAdvance: Boolean
): List<GreatHallArtifact> =
    listOf(
        GreatHallArtifact(
            kind = GreatHallArtifactKind.MIRROR,
            title = "Living Mirror",
            subtitle = if (livingMirrorNoteCount > 0) {
                "$livingMirrorNoteCount notes remember being revisited"
            } else {
                "Still water · write the first note to wake it"
            },
            route = "mirror",
            unlockRank = 0,
            resonance = maxOf((livingMirrorNoteCount / 24f).coerceIn(0f, 1f), worldProjection.mirrorClarity),
            awakened = livingMirrorNoteCount > 0
        ),
        GreatHallArtifact(
            kind = GreatHallArtifactKind.ASTROLABE,
            title = "Astrolabe",
            subtitle = "Observatory · factual relations in reading history",
            route = "observatory",
            unlockRank = 2,
            resonance = maxOf(memoryState.observatoryResonance, worldProjection.observatorySignal),
            awakened = memoryState.atlasLinkCount > 0
        ),
        GreatHallArtifact(
            kind = GreatHallArtifactKind.ARCHIVE_GATE,
            title = "Archive Gate",
            subtitle = "Volumes, collections, and the entrance to the Archive",
            route = "library",
            unlockRank = 0,
            resonance = maxOf(memoryState.libraryResonance, worldProjection.architecturalPresence),
            awakened = memoryState.volumeCount > 0
        ),
        GreatHallArtifact(
            kind = GreatHallArtifactKind.LEDGER,
            title = "Sealed Ledger",
            subtitle = "Dossier · reading signature · durable record",
            route = "profile",
            unlockRank = 0,
            resonance = maxOf(memoryState.archiveResonance, worldProjection.archiveDepth),
            awakened = memoryState.overallPresence > 0.05f
        ),
        GreatHallArtifact(
            kind = GreatHallArtifactKind.RITUAL_SEAL,
            title = "Ritual Seal",
            subtitle = if (canAdvance) "Advancement is ready" else "Path, rank, and the next transformation",
            route = "ritual",
            unlockRank = 1,
            resonance = if (canAdvance) 1f else maxOf(memoryState.overallPresence, worldProjection.ritualCharge),
            awakened = canAdvance
        ),
        GreatHallArtifact(
            kind = GreatHallArtifactKind.RELIQUARY,
            title = "Reliquary",
            subtitle = "Treasury · relics, sigils, and reading-earned marks",
            route = "treasury",
            unlockRank = 4,
            resonance = maxOf(memoryState.treasuryResonance, worldProjection.relicWeight),
            awakened = memoryState.sealedCapsuleCount > 0
        ),
        GreatHallArtifact(
            kind = GreatHallArtifactKind.VEILED_DOOR,
            title = "Veiled Door",
            subtitle = "Inner Sanctum · rare permanent records",
            route = "sanctum",
            unlockRank = 5,
            resonance = maxOf(memoryState.sanctumResonance, worldProjection.sanctumPresence),
            awakened = profile.rankIndex >= 5
        ),
        GreatHallArtifact(
            kind = GreatHallArtifactKind.READING_SEAT,
            title = "Reading Seat",
            subtitle = "Return to the quiet center of your reading life",
            route = "reading",
            unlockRank = 0,
            resonance = maxOf(memoryState.overallPresence, worldProjection.architecturalPresence),
            awakened = memoryState.daysSinceLastActivity != null
        )
    )

@Composable
private fun GreatHallArtifactNavigator(
    profile: ReaderProfile,
    memoryState: CastleMemoryState,
    worldProjection: WorldProgressionProjection,
    livingMirrorNoteCount: Int,
    canAdvance: Boolean,
    mode: GreatHallMode,
    hallModeEnabled: Boolean,
    onModeChange: (GreatHallMode) -> Unit,
    onOpenArtifact: (String) -> Unit
) {
    val artifacts = remember(
        profile.rankIndex,
        memoryState,
        worldProjection,
        livingMirrorNoteCount,
        canAdvance
    ) {
        greatHallArtifacts(
            profile = profile,
            memoryState = memoryState,
            worldProjection = worldProjection,
            livingMirrorNoteCount = livingMirrorNoteCount,
            canAdvance = canAdvance
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            VeilEyebrowText(
                text = "THE GREAT HALL · ARTIFACTS",
                trackingSp = 1.45f
            )
            Text(
                "The Hall is not a menu",
                style = MaterialTheme.typography.titleLarge,
                color = VeilPalette.Moon
            )
            Text(
                "Every destination manifests as an object. Registry mode keeps the same paths fast and conventional.",
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GreatHallModeButton(
                label = "Hall",
                selected = mode == GreatHallMode.HALL,
                enabled = hallModeEnabled,
                onClick = { onModeChange(GreatHallMode.HALL) },
                modifier = Modifier.weight(1f)
            )
            GreatHallModeButton(
                label = "Registry",
                selected = mode == GreatHallMode.REGISTRY,
                onClick = { onModeChange(GreatHallMode.REGISTRY) },
                modifier = Modifier.weight(1f)
            )
        }

        if (!hallModeEnabled) {
            Text(
                "TalkBack uses Registry presentation so artifacts follow reading order.",
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.78f)
            )
        }

        when (mode) {
            GreatHallMode.HALL -> GreatHallArtifactField(
                artifacts = artifacts,
                rankIndex = profile.rankIndex,
                memoryState = memoryState,
                onOpenArtifact = onOpenArtifact
            )
            GreatHallMode.REGISTRY -> GreatHallArtifactRegistry(
                artifacts = artifacts,
                rankIndex = profile.rankIndex,
                onOpenArtifact = onOpenArtifact
            )
        }
    }
}

@Composable
private fun GreatHallModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = VeilPalette.DeepBrass.copy(alpha = 0.72f),
                contentColor = VeilPalette.Moon
            ),
            border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.54f))
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            border = BorderStroke(1.dp, VeilPalette.BorderDark)
        ) {
            Text(label)
        }
    }
}

@Composable
private fun GreatHallArtifactField(
    artifacts: List<GreatHallArtifact>,
    rankIndex: Int,
    memoryState: CastleMemoryState,
    onOpenArtifact: (String) -> Unit
) {
    val reducedMotion = LocalVeilReducedMotion.current
    val revealDuration =
        if (reducedMotion) 0 else motionBudgetFor(CathedralMotionClass.REALM).targetDurationMs

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070A0F),
                        Color(0xFF0B1118),
                        Color(0xFF11141A),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.34f)),
                MaterialTheme.shapes.extraSmall
            )
            .semantics {
                contentDescription =
                    "Great Hall artifact field. ${artifacts.size} destinations."
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val vanish = Offset(w * 0.50f, h * 0.13f)

            listOf(0.08f, 0.27f, 0.50f, 0.73f, 0.92f).forEach { fraction ->
                drawLine(
                    color = VeilPalette.Brass.copy(alpha = 0.075f),
                    start = Offset(w * fraction, h * 0.92f),
                    end = vanish,
                    strokeWidth = 0.8.dp.toPx()
                )
            }
            repeat(6) { index ->
                val t = (index + 1f) / 7f
                val eased = t * t
                val y = vanish.y + (h * 0.92f - vanish.y) * eased
                val half = w * (0.11f + eased * 0.39f)
                drawLine(
                    color = VeilPalette.StrongBorderDark.copy(alpha = 0.11f + eased * 0.04f),
                    start = Offset(w * 0.50f - half, y),
                    end = Offset(w * 0.50f + half, y),
                    strokeWidth = 0.75.dp.toPx()
                )
            }

            repeat(3) { index ->
                val inset = 0.14f + index * 0.055f
                drawArc(
                    color = VeilPalette.Brass.copy(alpha = 0.07f - index * 0.012f),
                    startAngle = 192f,
                    sweepAngle = 156f,
                    useCenter = false,
                    topLeft = Offset(w * inset, h * (0.03f + index * 0.035f)),
                    size = Size(w * (1f - inset * 2f), h * (0.32f + index * 0.05f)),
                    style = Stroke(0.9.dp.toPx())
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        VeilPalette.Brass.copy(alpha = 0.10f + memoryState.returnAwakening * 0.08f),
                        VeilPalette.Spirit.copy(alpha = 0.028f),
                        Color.Transparent
                    ),
                    center = vanish,
                    radius = size.minDimension * 0.32f
                ),
                center = vanish,
                radius = size.minDimension * 0.32f
            )

            listOf(
                Offset(w * 0.23f, h * 0.36f),
                Offset(w * 0.77f, h * 0.36f),
                Offset(w * 0.20f, h * 0.70f),
                Offset(w * 0.80f, h * 0.70f)
            ).forEach { center ->
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            VeilPalette.Brass.copy(alpha = 0.055f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 54.dp.toPx()
                    ),
                    center = center,
                    radius = 54.dp.toPx()
                )
            }
        }

        val nodeWidth = when {
            maxWidth < 380.dp -> 96.dp
            maxWidth < 430.dp -> 104.dp
            else -> 124.dp
        }
        val nodeHeight = if (maxWidth < 430.dp) 96.dp else 104.dp

        artifacts.forEachIndexed { index, artifact ->
            val (xFraction, yFraction) = greatHallArtifactPosition(artifact.kind)
            val x = (maxWidth - nodeWidth) * xFraction
            val y = (maxHeight - nodeHeight) * yFraction
            val unlocked = rankIndex >= artifact.unlockRank

            GreatHallArtifactPedestal(
                artifact = artifact,
                unlocked = unlocked,
                revealDelayMs = if (reducedMotion) 0 else index * 45,
                revealDurationMs = revealDuration,
                onOpen = { onOpenArtifact(artifact.route) },
                modifier = Modifier
                    .offset(x = x, y = y)
                    .width(nodeWidth)
                    .heightIn(min = nodeHeight)
            )
        }

        VeilEyebrowText(
            text = "VEIL ABOVE",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            color = VeilPalette.Brass.copy(alpha = 0.72f),
            trackingSp = 1.6f
        )
    }
}

private fun greatHallArtifactPosition(
    kind: GreatHallArtifactKind
): Pair<Float, Float> =
    when (kind) {
        GreatHallArtifactKind.MIRROR -> 0.02f to 0.17f
        GreatHallArtifactKind.ASTROLABE -> 0.98f to 0.17f
        GreatHallArtifactKind.ARCHIVE_GATE -> 0.00f to 0.43f
        GreatHallArtifactKind.LEDGER -> 0.50f to 0.35f
        GreatHallArtifactKind.RITUAL_SEAL -> 1.00f to 0.43f
        GreatHallArtifactKind.RELIQUARY -> 0.04f to 0.73f
        GreatHallArtifactKind.READING_SEAT -> 0.50f to 0.68f
        GreatHallArtifactKind.VEILED_DOOR -> 0.96f to 0.73f
    }

@Composable
private fun GreatHallArtifactPedestal(
    artifact: GreatHallArtifact,
    unlocked: Boolean,
    revealDelayMs: Int,
    revealDurationMs: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(revealDurationMs == 0) }
    LaunchedEffect(revealDurationMs, revealDelayMs) {
        if (revealDurationMs > 0) {
            kotlinx.coroutines.delay(revealDelayMs.toLong())
        }
        revealed = true
    }
    val presence by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (revealDurationMs == 0) {
            snap()
        } else {
            tween(revealDurationMs)
        },
        label = "hall-artifact-presence"
    )
    val active = unlocked
    val glow = if (artifact.awakened) {
        (0.42f + artifact.resonance.coerceIn(0f, 1f) * 0.50f)
    } else {
        0.24f
    }

    val sharedModifier =
        if (hallRouteUsesTabTransition(artifact.route)) {
            modifier.veilTabSharedBounds(hallSharedBoundsKey(artifact.route))
        } else {
            modifier.veilSharedBounds(hallSharedBoundsKey(artifact.route))
        }

    Column(
        modifier = sharedModifier
            .semantics {
                contentDescription = if (active) {
                    "${artifact.title}. ${artifact.subtitle}. Open."
                } else {
                    "${artifact.title}. Sealed until rank ${artifact.unlockRank + 1}."
                }
            }
            .clickable(
                enabled = active,
                role = Role.Button,
                onClick = onOpen
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (active) {
                                VeilPalette.Brass.copy(alpha = 0.08f * presence)
                            } else {
                                VeilPalette.StrongBorderDark.copy(alpha = 0.08f)
                            },
                            VeilPalette.Ink.copy(alpha = 0.88f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (active) {
                            VeilPalette.Brass.copy(alpha = glow * presence)
                        } else {
                            VeilPalette.BorderDark.copy(alpha = 0.52f)
                        }
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            GreatHallArtifactGlyph(
                kind = artifact.kind,
                tint = if (active) {
                    if (artifact.kind == GreatHallArtifactKind.MIRROR) {
                        VeilPalette.Spirit.copy(alpha = presence)
                    } else {
                        VeilPalette.Brass.copy(alpha = presence)
                    }
                } else {
                    VeilPalette.Mist.copy(alpha = 0.32f)
                },
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(Modifier.height(5.dp))
        Text(
            artifact.title,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) {
                VeilPalette.Moon.copy(alpha = 0.94f * presence)
            } else {
                VeilPalette.Mist.copy(alpha = 0.44f)
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            if (active) {
                if (artifact.awakened) "AWAKENED" else "DORMANT"
            } else {
                "SEALED · RANK ${artifact.unlockRank + 1}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (active && artifact.awakened) {
                VeilPalette.Brass.copy(alpha = 0.78f * presence)
            } else {
                VeilPalette.Mist.copy(alpha = 0.44f)
            },
            maxLines = 1
        )
    }
}

@Composable
private fun GreatHallArtifactRegistry(
    artifacts: List<GreatHallArtifact>,
    rankIndex: Int,
    onOpenArtifact: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        artifacts.forEach { artifact ->
            val unlocked = rankIndex >= artifact.unlockRank
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clickable(
                        enabled = unlocked,
                        role = Role.Button
                    ) {
                        onOpenArtifact(artifact.route)
                    }
                    .semantics {
                        contentDescription = if (unlocked) {
                            "${artifact.title}. ${artifact.subtitle}. Open."
                        } else {
                            "${artifact.title}. Sealed until rank ${artifact.unlockRank + 1}."
                        }
                    },
                shape = MaterialTheme.shapes.extraSmall,
                color = VeilPalette.Archive.copy(alpha = if (unlocked) 0.72f else 0.42f),
                border = BorderStroke(
                    1.dp,
                    if (unlocked) {
                        VeilPalette.Brass.copy(alpha = 0.26f)
                    } else {
                        VeilPalette.BorderDark.copy(alpha = 0.40f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (unlocked) {
                                        VeilPalette.Brass.copy(alpha = 0.44f)
                                    } else {
                                        VeilPalette.BorderDark
                                    }
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GreatHallArtifactGlyph(
                            kind = artifact.kind,
                            tint = if (unlocked) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.36f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            artifact.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (unlocked) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.48f)
                        )
                        Text(
                            artifact.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = VeilPalette.Mist.copy(alpha = if (unlocked) 0.78f else 0.42f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        if (unlocked) "OPEN" else "R${artifact.unlockRank + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unlocked) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.40f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GreatHallArtifactGlyph(
    kind: GreatHallArtifactKind,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val thin = 0.9.dp.toPx()
        val strong = 1.2.dp.toPx()
        val faint = tint.copy(alpha = tint.alpha * 0.44f)

        when (kind) {
            GreatHallArtifactKind.MIRROR -> {
                drawOval(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.10f),
                    size = Size(w * 0.56f, h * 0.72f),
                    style = Stroke(strong)
                )
                repeat(3) { index ->
                    drawArc(
                        color = faint,
                        startAngle = 15f + index * 8f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(w * (0.28f + index * 0.04f), h * (0.22f + index * 0.06f)),
                        size = Size(w * (0.44f - index * 0.08f), h * (0.40f - index * 0.07f)),
                        style = Stroke(thin)
                    )
                }
            }

            GreatHallArtifactKind.ASTROLABE -> {
                drawCircle(tint, w * 0.30f, center, style = Stroke(strong))
                drawOval(
                    faint,
                    Offset(w * 0.08f, h * 0.38f),
                    Size(w * 0.84f, h * 0.24f),
                    style = Stroke(thin)
                )
                drawLine(tint, Offset(center.x, h * 0.08f), Offset(center.x, h * 0.92f), thin)
                drawCircle(tint, 2.dp.toPx(), center)
            }

            GreatHallArtifactKind.ARCHIVE_GATE -> {
                drawArc(
                    tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.10f),
                    size = Size(w * 0.68f, h * 0.56f),
                    style = Stroke(strong)
                )
                drawLine(tint, Offset(w * 0.16f, h * 0.38f), Offset(w * 0.16f, h * 0.88f), strong)
                drawLine(tint, Offset(w * 0.84f, h * 0.38f), Offset(w * 0.84f, h * 0.88f), strong)
                repeat(3) { index ->
                    val x = w * (0.34f + index * 0.16f)
                    drawLine(faint, Offset(x, h * 0.44f), Offset(x, h * 0.82f), thin)
                }
            }

            GreatHallArtifactKind.LEDGER -> {
                drawRect(
                    tint,
                    topLeft = Offset(w * 0.18f, h * 0.16f),
                    size = Size(w * 0.64f, h * 0.68f),
                    style = Stroke(strong)
                )
                drawLine(faint, Offset(w * 0.36f, h * 0.16f), Offset(w * 0.36f, h * 0.84f), thin)
                repeat(3) { index ->
                    val y = h * (0.34f + index * 0.13f)
                    drawLine(faint, Offset(w * 0.44f, y), Offset(w * 0.72f, y), thin)
                }
            }

            GreatHallArtifactKind.RITUAL_SEAL -> {
                val diamond = Path().apply {
                    moveTo(center.x, h * 0.08f)
                    lineTo(w * 0.90f, center.y)
                    lineTo(center.x, h * 0.92f)
                    lineTo(w * 0.10f, center.y)
                    close()
                }
                drawPath(diamond, tint, style = Stroke(strong))
                drawCircle(faint, w * 0.23f, center, style = Stroke(thin))
                drawCircle(tint, 2.dp.toPx(), center)
            }

            GreatHallArtifactKind.RELIQUARY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.34f),
                    size = Size(w * 0.70f, h * 0.48f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(strong)
                )
                drawArc(
                    tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.14f),
                    size = Size(w * 0.56f, h * 0.42f),
                    style = Stroke(strong)
                )
                drawCircle(faint, 3.dp.toPx(), center)
            }

            GreatHallArtifactKind.VEILED_DOOR -> {
                drawRect(
                    tint,
                    Offset(w * 0.24f, h * 0.10f),
                    Size(w * 0.52f, h * 0.80f),
                    style = Stroke(strong)
                )
                drawLine(
                    faint,
                    Offset(w * 0.50f, h * 0.15f),
                    Offset(w * 0.54f, h * 0.86f),
                    thin
                )
                drawCircle(tint, 1.8.dp.toPx(), Offset(w * 0.64f, h * 0.54f))
            }

            GreatHallArtifactKind.READING_SEAT -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.24f), Offset(w * 0.28f, h * 0.68f), strong)
                drawLine(tint, Offset(w * 0.28f, h * 0.68f), Offset(w * 0.72f, h * 0.68f), strong)
                drawLine(tint, Offset(w * 0.72f, h * 0.68f), Offset(w * 0.76f, h * 0.86f), strong)
                drawLine(tint, Offset(w * 0.34f, h * 0.68f), Offset(w * 0.30f, h * 0.86f), strong)
                drawLine(faint, Offset(w * 0.30f, h * 0.36f), Offset(w * 0.66f, h * 0.36f), thin)
            }
        }
    }
}

@Composable
private fun CastleGrandMasthead(
    memoryState: CastleMemoryState,
    worldProjection: WorldProgressionProjection,
    awakenedRooms: Int,
    totalRooms: Int
) {
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (fontScale > 1.35f) 318.dp else 252.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.66f)),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.10f),
                        0.42f to Color.Transparent,
                        1f to VeilPalette.Ink.copy(alpha = 0.96f)
                    )
                )
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VeilPalette.Ink.copy(alpha = 0.50f),
                            Color.Transparent,
                            VeilPalette.Ink.copy(alpha = 0.18f)
                        )
                    )
                )
        )
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.88f
        )
        VeilRealmEmblem(
            realm = VeilRealm.CASTLE,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = VeilSpacing.lg)
                .size(144.dp),
            tint = VeilPalette.Brass.copy(alpha = 0.22f)
        )

        VeilMastheadMetaRow(
            primary = "VEIL ABOVE · ${worldProjection.stage.label.uppercase()} ARCHITECTURE",
            secondary = "$awakenedRooms / $totalRooms CHAMBERS",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(VeilSpacing.md)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "The Great Hall",
                style = MaterialTheme.typography.displaySmall,
                color = VeilPalette.Moon
            )
            Text(
                memoryState.inscription,
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Moon.copy(alpha = 0.84f),
                modifier = Modifier.widthIn(max = 560.dp)
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .width(156.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                VeilPalette.Brass,
                                VeilPalette.Brass.copy(alpha = 0.34f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun CastleKeep(
    profile: ReaderProfile,
    memoryState: CastleMemoryState,
    worldProjection: WorldProgressionProjection,
    canAdvance: Boolean,
    awakenedRooms: Int,
    totalRooms: Int,
    minHeightDp: Float,
    onOpenRitual: () -> Unit
) {
    val language = LocalVeilLanguage.current
    val scriptGroup = LocalVeilScriptGroup.current
    val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(1)
    val targetProgress = (profile.rankIndex.toFloat() / finalRank).coerceIn(0f, 1f)
    val reducedMotion = LocalVeilReducedMotion.current
    val pathSignature = remember(
        profile.path.id,
        profile.rankIndex,
        profile.path.ranks.size,
        worldProjection.ritualCharge
    ) {
        derivePathWorldSignature(
            pathId = profile.path.id,
            rankIndex = profile.rankIndex,
            rankCount = profile.path.ranks.size,
            ritualCharge = worldProjection.ritualCharge
        )
    }
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
            memoryState = memoryState,
            worldProjection = worldProjection,
            pathSignature = pathSignature
        )
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.34f
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
                    Text(
                        localizeAppNumerals(
                            "KEEP TIER ${profile.rankIndex + 1}",
                            language
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.30.sp),
                        color = VeilPalette.Brass
                    )
                    Text(
                        profile.rankName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = VeilPalette.Moon
                    )
                    Text(
                        listOf(
                            localizedMetadataValue(profile.path.name, language),
                            localizedMetadataValue(
                                localizeAppNumerals(
                                    "${profile.booksFinished} finished ${if (profile.booksFinished == 1) "volume" else "volumes"}",
                                    language
                                ),
                                language
                            )
                        ).joinToString(appMetadataDivider(scriptGroup)),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist
                    )
                }

                Text(
                    localizeAppNumerals(
                        "${profile.rankIndex + 1}/${profile.path.ranks.size}",
                        language
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = VeilPalette.Brass
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AWAKENED CHAMBERS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.20.sp),
                    color = VeilPalette.Mist.copy(alpha = 0.72f)
                )
                Text(
                    localizeAppNumerals(
                        "$awakenedRooms/${totalRooms.coerceAtLeast(1)}",
                        language
                    ),
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
                    onClick = onOpenRitual,
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
                        "Enter advancement ritual",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CastleMemoryInscription(memory: CastleMemoryState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VeilEyebrowText(
            text = "FOUNDATION MEMORY",
            color = VeilPalette.Brass.copy(alpha = 0.78f),
            trackingSp = 1.35f
        )
        Text(
            buildString {
                append(memory.volumeCount).append(" volumes")
                if (memory.passageCount > 0) {
                    append(" · ").append(memory.passageCount).append(" preserved passages")
                }
                if (memory.sealedCapsuleCount > 0) {
                    append(" · ").append(memory.sealedCapsuleCount).append(" sealed records")
                }
                if (memory.atlasLinkCount > 0) {
                    append(" · ").append(memory.atlasLinkCount).append(" atlas links")
                }
            },
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "WORLD RESONANCE · ${world.stage.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.35.sp),
                    color = VeilPalette.Brass.copy(alpha = 0.86f)
                )
                Text(
                    world.inscription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Moon.copy(alpha = 0.86f)
                )
            }
            if (world.streakEmbers > 0) {
                Text(
                    "${world.streakEmbers} EMBER",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Spirit.copy(alpha = 0.86f)
                )
            }
        }

        Text(
            buildString {
                append("Ritual ").append((world.ritualCharge * 100f).toInt()).append("%")
                if (world.directiveCount > 0) {
                    append(" · ")
                    append(world.completedDirectives)
                        .append("/")
                        .append(world.directiveCount)
                        .append(" daily directives")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.64f)
        )

        BrassRule(
            modifier = Modifier.fillMaxWidth(),
            strong = world.ritualCharge >= 0.80f
        )
    }
}

@Composable
private fun CastleRitualAftermath(
    profile: ReaderProfile,
    aftermath: RitualAftermathRecord?,
    afterglow: Float
) {
    val record = aftermath ?: return
    val fromName = profile.path.ranks.getOrElse(record.fromRankIndex) {
        "Rank ${record.fromRankIndex}"
    }
    val toName = profile.path.ranks.getOrElse(record.toRankIndex) {
        "Rank ${record.toRankIndex}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "RITUAL AFTERMATH · SEALED",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.35.sp),
                color = VeilPalette.Brass.copy(alpha = 0.90f)
            )
            Text(
                if (afterglow > 0.01f) "SEAL WARM" else "SEAL ANCHORED",
                style = MaterialTheme.typography.labelSmall,
                color = if (afterglow > 0.01f) {
                    VeilPalette.Spirit.copy(alpha = 0.88f)
                } else {
                    VeilPalette.Mist.copy(alpha = 0.62f)
                }
            )
        }

        Text(
            "$fromName  →  $toName",
            style = MaterialTheme.typography.titleMedium,
            color = VeilPalette.Moon
        )
        Text(
            if (afterglow > 0.01f) {
                "The ceremonial glow is still moving through the Hall. The architectural change is already permanent."
            } else {
                "The ceremonial glow has cooled. The advancement remains written into the Hall, Reliquary, and Sanctum."
            },
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.76f)
        )

        if (afterglow > 0.01f) {
            LinearProgressIndicator(
                progress = { afterglow.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = VeilPalette.Brass,
                trackColor = VeilPalette.BorderDark.copy(alpha = 0.42f)
            )
        }
        BrassRule(Modifier.fillMaxWidth())
    }
}

@Composable
private fun CastleWorldMutationLedger(ledger: WorldMutationLedger) {
    val entries = ledger.forRealm(WorldMutationRealm.GREAT_HALL).take(5)
    if (entries.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "MUTATION LEDGER",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.35.sp),
                color = VeilPalette.Brass.copy(alpha = 0.86f)
            )
            Text(
                "${ledger.durableCount} ANCHORED",
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Mist.copy(alpha = 0.64f)
            )
        }

        entries.forEach { mutation ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(5.dp)
                        .background(
                            if (mutation.durable) VeilPalette.Brass
                            else VeilPalette.Spirit,
                            CircleShape
                        )
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        mutation.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Moon
                    )
                    Text(
                        mutation.inscription,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.72f)
                    )
                }
                Text(
                    mutation.evidenceCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Spirit.copy(alpha = 0.78f)
                )
            }
        }

        BrassRule(Modifier.fillMaxWidth())
    }
}

@Composable
private fun CastleMutationInscription(memory: CastleMemoryState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "LIVING STONE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.35.sp),
                    color = VeilPalette.Brass.copy(alpha = 0.82f)
                )
                Text(
                    memory.mutationInscription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Moon.copy(alpha = 0.82f)
                )
            }

            if (memory.rereadCycleCount > 0) {
                Text(
                    "${memory.rereadCycleCount} REREAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Spirit.copy(alpha = 0.86f)
                )
            }
        }

        Text(
            buildString {
                if (memory.archiveAgeDays > 0) {
                    append(memory.archiveAgeDays).append(" days of recorded archive age")
                } else {
                    append("Newly awakened archive")
                }
                memory.daysSinceLastActivity?.let { days ->
                    append(" · ")
                    append(
                        when {
                            days == 0 -> "active today"
                            days == 1 -> "last active yesterday"
                            else -> "last active ${days}d ago"
                        }
                    )
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.62f)
        )

        BrassRule(
            modifier = Modifier.fillMaxWidth(),
            strong = memory.returnAwakening > 0.20f
        )
    }
}

@Composable
private fun CastleKeepBackdrop(
    rankIndex: Int,
    rankCount: Int,
    memoryState: CastleMemoryState,
    worldProjection: WorldProgressionProjection,
    pathSignature: PathWorldSignature,
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
        val worldGlow = worldProjection.architecturalPresence
        val glow = (
            rankGlow * 0.34f +
                memoryGlow * 0.33f +
                worldGlow * 0.33f
            ).coerceIn(0f, 1f)

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

        val pathAlpha = (0.055f + pathSignature.strength * 0.16f)
            .coerceIn(0.055f, 0.22f)
        val motifCenter = Offset(w * 0.50f, h * 0.31f)

        when (pathSignature.motif) {
            PathArchitecturalMotif.CIPHER -> {
                repeat(pathSignature.ornamentCount.coerceAtMost(6)) { index ->
                    val radius = size.minDimension * (0.055f + index * 0.022f)
                    drawCircle(
                        color = brass.copy(alpha = pathAlpha * (1f - index * 0.09f)),
                        radius = radius,
                        center = motifCenter,
                        style = Stroke((0.65f + index * 0.08f).dp.toPx())
                    )
                }
                drawLine(
                    brass.copy(alpha = pathAlpha),
                    Offset(motifCenter.x, h * 0.18f),
                    Offset(motifCenter.x, h * 0.43f),
                    0.75.dp.toPx()
                )
            }

            PathArchitecturalMotif.DREAM -> {
                repeat(pathSignature.ornamentCount.coerceAtMost(7)) { index ->
                    val inset = w * (0.23f + index * 0.018f)
                    drawArc(
                        color = VeilPalette.Spirit.copy(
                            alpha = pathAlpha * (0.92f - index * 0.07f)
                        ),
                        startAngle = 196f + index * 3f,
                        sweepAngle = 148f - index * 4f,
                        useCenter = false,
                        topLeft = Offset(inset, h * (0.10f + index * 0.012f)),
                        size = Size(w - inset * 2f, h * (0.34f + index * 0.018f)),
                        style = Stroke(0.72.dp.toPx())
                    )
                }
            }

            PathArchitecturalMotif.ARCHIVE -> {
                repeat(pathSignature.ornamentCount.coerceAtMost(8)) { index ->
                    val y = h * (0.28f + index * 0.045f)
                    val half = w * (0.11f + index * 0.008f)
                    drawLine(
                        color = brass.copy(alpha = pathAlpha * 0.82f),
                        start = Offset(w * 0.50f - half, y),
                        end = Offset(w * 0.50f + half, y),
                        strokeWidth = 0.72.dp.toPx()
                    )
                }
            }

            PathArchitecturalMotif.VANGUARD -> {
                repeat(pathSignature.ornamentCount.coerceAtMost(6)) { index ->
                    val y = h * (0.25f + index * 0.052f)
                    val spread = w * (0.055f + index * 0.010f)
                    drawLine(
                        brass.copy(alpha = pathAlpha),
                        Offset(w * 0.50f - spread, y),
                        Offset(w * 0.50f, y + h * 0.034f),
                        0.92.dp.toPx()
                    )
                    drawLine(
                        brass.copy(alpha = pathAlpha),
                        Offset(w * 0.50f + spread, y),
                        Offset(w * 0.50f, y + h * 0.034f),
                        0.92.dp.toPx()
                    )
                }
            }

            PathArchitecturalMotif.NOCTURNE -> {
                val radius = size.minDimension * (0.095f + pathSignature.strength * 0.025f)
                drawCircle(
                    color = VeilPalette.Spirit.copy(alpha = pathAlpha * 0.88f),
                    radius = radius,
                    center = motifCenter,
                    style = Stroke(1.05.dp.toPx())
                )
                drawCircle(
                    color = VeilPalette.Ink.copy(alpha = 0.92f),
                    radius = radius * 0.91f,
                    center = motifCenter + Offset(radius * 0.27f, -radius * 0.06f)
                )
            }

            PathArchitecturalMotif.ARTIFICE -> {
                drawCircle(
                    color = brass.copy(alpha = pathAlpha),
                    radius = size.minDimension * 0.085f,
                    center = motifCenter,
                    style = Stroke(1.0.dp.toPx())
                )
                val ticks = listOf(
                    Offset(0f, -1f), Offset(0.71f, -0.71f),
                    Offset(1f, 0f), Offset(0.71f, 0.71f),
                    Offset(0f, 1f), Offset(-0.71f, 0.71f),
                    Offset(-1f, 0f), Offset(-0.71f, -0.71f)
                )
                val inner = size.minDimension * 0.095f
                val outer = size.minDimension * 0.115f
                ticks.forEach { unit ->
                    drawLine(
                        brass.copy(alpha = pathAlpha),
                        motifCenter + unit * inner,
                        motifCenter + unit * outer,
                        1.0.dp.toPx()
                    )
                }
            }
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

        if (worldProjection.ritualCharge > 0.001f) {
            val charge = worldProjection.ritualCharge.coerceIn(0f, 1f)
            drawArc(
                color = VeilPalette.Brass.copy(alpha = 0.07f + charge * 0.18f),
                startAngle = 205f,
                sweepAngle = 130f * charge,
                useCenter = false,
                topLeft = Offset(w * 0.36f, h * 0.23f),
                size = Size(w * 0.28f, h * 0.26f),
                style = Stroke((0.8f + charge * 0.9f).dp.toPx(), cap = StrokeCap.Round)
            )
        }

        if (worldProjection.streakEmbers > 0) {
            repeat(worldProjection.streakEmbers.coerceAtMost(7)) { index ->
                val x = w * (0.38f + index * 0.04f)
                val y = h * (0.74f - (index % 2) * 0.018f)
                drawCircle(
                    color = VeilPalette.Brass.copy(alpha = 0.16f + worldGlow * 0.18f),
                    radius = (1.6f + worldGlow * 1.4f).dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        if (memoryState.returnAwakening > 0.001f) {
            val awakening = memoryState.returnAwakening.coerceIn(0f, 1f)
            val gateCenter = Offset(w * 0.50f, baseY - h * 0.02f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        brass.copy(alpha = 0.12f * awakening),
                        VeilPalette.Spirit.copy(alpha = 0.035f * awakening),
                        Color.Transparent
                    ),
                    center = gateCenter,
                    radius = size.minDimension * 0.42f
                ),
                center = gateCenter,
                radius = size.minDimension * 0.42f
            )
        }

        repeat(memoryState.rereadRings) { index ->
            val expansion = index * 0.018f
            drawArc(
                color = VeilPalette.Spirit.copy(
                    alpha = 0.045f + memoryState.patina * 0.055f
                ),
                startAngle = 198f,
                sweepAngle = 144f,
                useCenter = false,
                topLeft = Offset(
                    w * (0.31f - expansion),
                    h * (0.145f - expansion * 0.40f)
                ),
                size = Size(
                    w * (0.38f + expansion * 2f),
                    h * (0.28f + expansion)
                ),
                style = Stroke(0.7.dp.toPx())
            )
        }

        repeat(memoryState.completionAlcoves) { index ->
            val leftSide = index % 2 == 0
            val row = index / 2
            val alcoveW = w * 0.048f
            val alcoveH = h * 0.064f
            val x = if (leftSide) {
                w * 0.125f
            } else {
                w * 0.827f
            }
            val y = h * (0.50f + row * 0.043f)

            drawRoundRect(
                color = VeilPalette.Ink.copy(alpha = 0.42f),
                topLeft = Offset(x, y),
                size = Size(alcoveW, alcoveH),
                cornerRadius = CornerRadius(alcoveW * 0.48f)
            )
            drawRoundRect(
                color = brass.copy(
                    alpha = 0.075f + memoryState.treasuryResonance * 0.12f
                ),
                topLeft = Offset(x, y),
                size = Size(alcoveW, alcoveH),
                cornerRadius = CornerRadius(alcoveW * 0.48f),
                style = Stroke(0.65.dp.toPx())
            )
        }

        repeat(memoryState.scriptoriumLamps) { index ->
            val leftSide = index % 2 == 0
            val row = index / 2
            val x = if (leftSide) w * 0.205f else w * 0.795f
            val y = h * (0.49f + row * 0.068f)
            drawCircle(
                color = brass.copy(
                    alpha = 0.22f + memoryState.archiveResonance * 0.28f
                ),
                radius = 1.5.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = brass.copy(alpha = 0.035f),
                radius = 8.dp.toPx(),
                center = Offset(x, y)
            )
        }

        repeat(memoryState.foundationCourses) { index ->
            val fraction = (index + 1f) / (memoryState.foundationCourses + 1f)
            val y = h * (0.73f + fraction * 0.12f)
            val inset = w * (0.20f + fraction * 0.025f)
            drawLine(
                color = stone.copy(
                    alpha = 0.07f + memoryState.patina * 0.07f
                ),
                start = Offset(inset, y),
                end = Offset(w - inset, y),
                strokeWidth = 0.65.dp.toPx()
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
                    brass.copy(
                        alpha = (
                            0.12f +
                                memoryGlow * 0.16f +
                                memoryState.returnAwakening * 0.18f
                            ).coerceIn(0.12f, 0.46f)
                    )
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

        drawLine(
            brass.copy(alpha = 0.16f + memoryGlow * 0.10f),
            Offset(w * 0.08f, baseY),
            Offset(w * 0.92f, baseY),
            1.dp.toPx()
        )
        if (memoryState.longSilence > 0.001f) {
            val silence = memoryState.longSilence.coerceIn(0f, 1f)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        VeilPalette.Spirit.copy(alpha = 0.018f * silence),
                        Color.Transparent,
                        VeilPalette.Ink.copy(alpha = 0.16f * silence)
                    )
                ),
                size = size
            )
        }

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
