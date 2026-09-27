package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.VeiledDiscoveryRecord
import com.veilreader.app.domain.deriveReadingSignature
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilLanguage
import com.veilreader.app.ui.theme.LocalVeilScriptGroup
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.appMetadataDivider
import com.veilreader.app.ui.theme.localizeAppNumerals
import com.veilreader.app.ui.theme.localizedMetadataValue
import com.veilreader.app.ui.theme.currentVeilTemporalPhase
import com.veilreader.app.ui.theme.currentVeilZoneId
import com.veilreader.app.ui.theme.usesArabicScript
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: ReaderProfile,
    highlightCount: Int,
    dailyGoalMinutes: Int,
    castleTitle: String,
    equippedSigilName: String?,
    books: List<Book> = emptyList(),
    readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    readingCycles: List<ReadingCycleRecord> = emptyList(),
    discoveries: List<VeiledDiscoveryRecord> = emptyList(),
    onSetDailyGoal: (Int) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val p = profile
    val language = LocalVeilLanguage.current
    val scriptGroup = LocalVeilScriptGroup.current
    val discoveriesById = remember(discoveries) { discoveries.associateBy { it.id } }
    val revealedDiscoveries = veiledDiscoveryPresentations.count { it.id in discoveriesById }
    val dossierHistory = remember(books, readingSessions, readingCycles) {
        deriveReaderDossierHistory(
            books = books,
            sessions = readingSessions,
            cycles = readingCycles
        )
    }
    val temporalPhase = currentVeilTemporalPhase()
    val readingSignatureZone = currentVeilZoneId()
    val readingSignatureNow = remember(temporalPhase, readingSignatureZone) {
        System.currentTimeMillis()
    }
    val readingSignature = remember(
        readingSessions,
        readingCycles,
        readingSignatureZone,
        readingSignatureNow
    ) {
        deriveReadingSignature(
            sessions = readingSessions,
            cycles = readingCycles,
            zoneId = readingSignatureZone,
            nowEpochMs = readingSignatureNow
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = p.level * 17 + dossierHistory.recordedSessionCount,
                temporalPhase = temporalPhase
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            alpha = 0.28f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(720.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(780.dp)
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.06f),
                        0.40f to Color.Transparent,
                        0.72f to VeilPalette.Ink.copy(alpha = 0.64f),
                        1f to VeilPalette.Ink
                    )
                )
        )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        ProfileGrandMasthead(
            castleTitle = castleTitle,
            pathLabel = listOf(
                localizedMetadataValue(p.path.name, language),
                localizedMetadataValue(p.rankName, language)
            ).joinToString(appMetadataDivider(scriptGroup)),
            level = p.level,
            recordedSessions = dossierHistory.recordedSessionCount
        )

        VeilReveal(delayMillis = 40, distance = 10.dp) {
            ArchivistDossierPanel(
                profile = p,
                highlightCount = highlightCount,
                equippedSigilName = equippedSigilName,
                revealedDiscoveries = revealedDiscoveries,
                totalDiscoveries = veiledDiscoveryPresentations.size,
                onOpenSettings = onOpenSettings
            )
        }

        ProfileSectionHeading(
            eyebrow = "Recorded history",
            title = "Reading record"
        )
        DossierRecordGrid(
            profile = p,
            highlightCount = highlightCount
        )

        DossierHistoryLedger(dossierHistory)

        ReadingSignaturePanel(
            signature = readingSignature
        )

        ProfileSectionHeading(
            eyebrow = "Rhythm",
            title = "Daily reading goal"
        )
        Text(
            "Your first daily quest follows this target. Choose a pace that supports reading instead of turning it into a chore.",
            color = VeilPalette.Mist,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(10, 20, 30, 60).forEach { minutes ->
                val selected = minutes == dailyGoalMinutes
                if (selected) {
                    Button(
                        onClick = { onSetDailyGoal(minutes) },
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VeilPalette.Brass,
                            contentColor = Color(0xFF17120A)
                        ),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("${minutes}m", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSetDailyGoal(minutes) },
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.82f)),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("${minutes}m", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        ProfileSectionHeading(
            eyebrow = "Known marks",
            title = "Sigil registry",
            trailing = "${p.earnedSigils.size} awakened"
        )

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(
                Triple("first_hour", "First Hour", p.minutesRead to 60),
                Triple("passage_keeper", "Passage Keeper", highlightCount to 10),
                Triple("seven_days", "Seven-Day Journey", p.streakDays to 7),
                Triple("ten_tomes", "Ten Tomes", p.booksFinished to 10),
                Triple("first_threshold", "First Threshold", p.rankIndex to 1)
            ).forEachIndexed { index, (id, name, progress) ->
                val (value, target) = progress
                val earned = id in p.earnedSigils
                VeilReveal(
                    delayMillis = 70 + index * 45,
                    distance = 7.dp
                ) {
                    SigilProgressRow(
                        name = name,
                        value = value,
                        target = target,
                        earned = earned
                    )
                }
            }
        }

        ProfileSectionHeading(
            eyebrow = "Restricted folio",
            title = "Veiled discoveries",
            trailing = "$revealedDiscoveries/${veiledDiscoveryPresentations.size} revealed"
        )
        Text(
            "Their conditions remain hidden. They surface when separate parts of your reading history begin to form a pattern.",
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Mist
        )

        veiledDiscoveryPresentations.forEachIndexed { index, discovery ->
            VeilReveal(
                delayMillis = 60 + index * 40,
                distance = 8.dp
            ) {
                DiscoveryCard(
                    index = index,
                    discovery = discovery,
                    record = discoveriesById[discovery.id]
                )
            }
        }

        OutlinedButton(
            onClick = onOpenArchive,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f))
        ) {
            Text("Open Hidden Archive", style = MaterialTheme.typography.labelMedium)
        }
    }
    }
}

@Composable
private fun ProfileGrandMasthead(
    castleTitle: String,
    pathLabel: String,
    level: Int,
    recordedSessions: Int
) {
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (fontScale > 1.35f) 330.dp else 260.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.62f)),
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
                        0f to VeilPalette.Ink.copy(alpha = 0.08f),
                        0.44f to Color.Transparent,
                        1f to VeilPalette.Ink.copy(alpha = 0.97f)
                    )
                )
        )
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.88f
        )
        VeilRealmEmblem(
            realm = VeilRealm.ARCHIVE,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = VeilSpacing.lg)
                .size(140.dp),
            tint = VeilPalette.Brass.copy(alpha = 0.20f)
        )
        Text(
            "ARCHIVIST DOSSIER",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(VeilSpacing.md),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.55.sp),
            color = VeilPalette.Brass
        )
        Text(
            "LVL $level · $recordedSessions SESSIONS",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(VeilSpacing.md),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.80.sp),
            color = VeilPalette.Moon.copy(alpha = 0.74f)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                castleTitle,
                style = MaterialTheme.typography.displaySmall,
                color = VeilPalette.Moon
            )
            Text(
                pathLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Moon.copy(alpha = 0.84f)
            )
            Text(
                "A factual ledger of reading, preserved passages, completed journeys, and discovered marks.",
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.82f),
                modifier = Modifier.widthIn(max = 580.dp)
            )
            BrassRule(Modifier.width(156.dp), strong = true)
        }
    }
}

internal data class ReaderDossierHistory(
    val archivedVolumeCount: Int,
    val recordedSessionCount: Int,
    val recordedActiveMillis: Long,
    val completionCycleCount: Int,
    val rereadCycleCount: Int,
    val firstRecordedAtEpochMs: Long?,
    val latestRecordedAtEpochMs: Long?
)

internal fun deriveReaderDossierHistory(
    books: List<Book>,
    sessions: List<ReadingSessionSnapshot>,
    cycles: List<ReadingCycleRecord>
): ReaderDossierHistory {
    val validSessions = sessions.filter { it.startedAtEpochMs > 0L }
    val validCycles = cycles.filter { it.completedAtEpochMs > 0L }

    val firstCandidates = buildList<Long> {
        books.mapNotNullTo(this) { it.addedAtEpochMs.takeIf { value -> value > 0L } }
        validSessions.mapTo(this) { it.startedAtEpochMs }
        validCycles.mapTo(this) { it.completedAtEpochMs }
    }
    val latestCandidates = buildList<Long> {
        books.mapNotNullTo(this) { it.lastOpenedAtEpochMs.takeIf { value -> value > 0L } }
        validSessions.mapNotNullTo(this) {
            maxOf(it.startedAtEpochMs, it.endedAtEpochMs)
                .takeIf { value -> value > 0L }
        }
        validCycles.mapTo(this) { it.completedAtEpochMs }
    }

    return ReaderDossierHistory(
        archivedVolumeCount = books.size,
        recordedSessionCount = validSessions.size,
        recordedActiveMillis = validSessions.sumOf { it.activeMillis.coerceAtLeast(0L) },
        completionCycleCount = validCycles.size,
        rereadCycleCount = validCycles.count { it.cycleIndex > 1 },
        firstRecordedAtEpochMs = firstCandidates.minOrNull(),
        latestRecordedAtEpochMs = latestCandidates.maxOrNull()
    )
}

@Composable
private fun DossierHistoryLedger(history: ReaderDossierHistory) {
    val language = LocalVeilLanguage.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "DURABLE LEDGER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    "Recorded history",
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
            }
            Text(
                localizeAppNumerals("${history.recordedSessionCount} sessions", language),
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Mist.copy(alpha = 0.72f)
            )
        }

        BrassRule(Modifier.fillMaxWidth())

        DossierLedgerLine(
            label = "Archive span",
            value = buildString {
                append(formatDossierDate(history.firstRecordedAtEpochMs, language))
                append(" — ")
                append(formatDossierDate(history.latestRecordedAtEpochMs, language))
            }
        )
        DossierLedgerLine(
            label = "Recorded active time",
            value = localizeAppNumerals(formatDossierDuration(history.recordedActiveMillis), language)
        )
        DossierLedgerLine(
            label = "Completion records",
            value = localizeAppNumerals("${history.completionCycleCount}", language)
        )
        DossierLedgerLine(
            label = "Reread cycles",
            value = localizeAppNumerals("${history.rereadCycleCount}", language)
        )
        DossierLedgerLine(
            label = "Archived volumes",
            value = localizeAppNumerals("${history.archivedVolumeCount}", language)
        )

        Text(
            "This ledger uses durable local book, session, and completion records; it does not infer missing reading history.",
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.58f)
        )
    }
}

@Composable
private fun DossierLedgerLine(
    label: String,
    value: String
) {
    val arabicScriptLabel = usesArabicScript(label)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (arabicScriptLabel) label else label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = if (arabicScriptLabel) 0.sp else 0.72.sp
            ),
            color = VeilPalette.Mist.copy(alpha = 0.62f),
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = VeilPalette.Moon,
            textAlign = TextAlign.End
        )
    }
}

private fun formatDossierDate(
    epochMs: Long?,
    language: String
): String =
    epochMs
        ?.takeIf { it > 0L }
        ?.let {
            val formatted = DateFormat
                .getDateInstance(
                    DateFormat.MEDIUM,
                    Locale.forLanguageTag(language)
                )
                .format(Date(it))
            if (usesArabicScript(formatted)) formatted else formatted.uppercase()
        }
        ?: "NO RECORD"

private fun formatDossierDuration(activeMillis: Long): String {
    val minutes = activeMillis.coerceAtLeast(0L) / 60_000L
    return when {
        minutes >= 60L -> {
            val hours = minutes / 60L
            val rest = minutes % 60L
            if (rest == 0L) "${hours}h" else "${hours}h ${rest}m"
        }
        minutes > 0L -> "${minutes}m"
        else -> "<1m"
    }
}

@Composable
private fun ArchivistDossierPanel(
    profile: ReaderProfile,
    highlightCount: Int,
    equippedSigilName: String?,
    revealedDiscoveries: Int,
    totalDiscoveries: Int,
    onOpenSettings: () -> Unit
) {
    val xpTarget = profile.xpForNextLevel.coerceAtLeast(1)
    val xpProgress = (profile.xp.toFloat() / xpTarget).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 286.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF17130F),
                        VeilPalette.Archive.copy(alpha = 0.98f),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.38f)),
                MaterialTheme.shapes.small
            )
    ) {
        DossierBackdrop(
            modifier = Modifier.matchParentSize(),
            rankIndex = profile.rankIndex,
            discoveries = revealedDiscoveries
        )

        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "PRIVATE READING RECORD",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                        color = VeilPalette.Brass
                    )
                    Text(
                        profile.rankName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VeilPalette.Moon
                    )
                    Text(
                        profile.path.epithet,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist
                    )
                }

                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                        color = VeilPalette.Brass
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArchivistSeal(
                    rank = profile.rankIndex + 1,
                    modifier = Modifier.size(92.dp)
                )

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DossierFact("PATH", profile.path.name)
                    DossierFact("LEVEL", profile.level.toString())
                    DossierFact("CASTLE TIER", (profile.rankIndex + 1).toString())
                    equippedSigilName?.let { DossierFact("EQUIPPED SIGIL", it) }
                }
            }

            BrassRule(Modifier.fillMaxWidth())

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "EXPERIENCE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.10.sp),
                    color = VeilPalette.Mist
                )
                Text(
                    "${profile.xp}/$xpTarget XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass
                )
            }
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = VeilPalette.Brass,
                trackColor = VeilPalette.Moon.copy(alpha = 0.08f),
                drawStopIndicator = {}
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${profile.earnedSigils.size} SIGILS",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.78f)
                )
                Text(
                    "$revealedDiscoveries/$totalDiscoveries DISCOVERIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.78f)
                )
                Text(
                    "$highlightCount MARKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun DossierBackdrop(
    rankIndex: Int,
    discoveries: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.78f, h * 0.36f)

        drawCircle(
            color = VeilPalette.Brass.copy(alpha = 0.040f),
            radius = size.minDimension * 0.28f,
            center = center,
            style = Stroke(1.dp.toPx())
        )
        drawCircle(
            color = VeilPalette.Brass.copy(alpha = 0.025f),
            radius = size.minDimension * 0.20f,
            center = center,
            style = Stroke(1.dp.toPx())
        )

        repeat(6) { index ->
            val y = h * (0.15f + index * 0.12f)
            drawLine(
                color = VeilPalette.StrongBorderDark.copy(alpha = 0.07f),
                start = Offset(w * 0.05f, y),
                end = Offset(w * 0.95f, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val activeMarks = (rankIndex + discoveries).coerceAtLeast(1).coerceAtMost(8)
        repeat(8) { index ->
            val angle = Math.toRadians(-90.0 + index * 45.0)
            val radius = size.minDimension * 0.31f
            val point = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * radius,
                center.y + kotlin.math.sin(angle).toFloat() * radius
            )
            drawCircle(
                color = if (index < activeMarks) {
                    VeilPalette.Brass.copy(alpha = 0.16f)
                } else {
                    VeilPalette.BorderDark.copy(alpha = 0.10f)
                },
                radius = if (index < activeMarks) 1.4.dp.toPx() else 1.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
private fun ArchivistSeal(rank: Int, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val stroke = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
            val brass = VeilPalette.Brass

            drawCircle(brass.copy(alpha = 0.62f), size.minDimension * 0.44f, center, style = stroke)
            drawCircle(brass.copy(alpha = 0.30f), size.minDimension * 0.33f, center, style = stroke)

            repeat(4) { index ->
                val angle = Math.toRadians(45.0 + index * 90.0)
                val r1 = size.minDimension * 0.33f
                val r2 = size.minDimension * 0.47f
                drawLine(
                    brass.copy(alpha = 0.44f),
                    Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * r1,
                        center.y + kotlin.math.sin(angle).toFloat() * r1
                    ),
                    Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * r2,
                        center.y + kotlin.math.sin(angle).toFloat() * r2
                    ),
                    stroke.width,
                    StrokeCap.Round
                )
            }
        }

        Text(
            rank.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            color = VeilPalette.Brass
        )
    }
}

@Composable
private fun DossierFact(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.85.sp),
            color = VeilPalette.Mist.copy(alpha = 0.66f),
            modifier = Modifier.width(82.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Moon,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
}

@Composable
private fun DossierRecordGrid(
    profile: ReaderProfile,
    highlightCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DossierStat("RETURN", "${profile.streakDays}d", "current streak", Modifier.weight(1f))
            DossierStat("VOLUMES", "${profile.booksFinished}", "finished", Modifier.weight(1f))
            DossierStat("MARKS", "$highlightCount", "highlights", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DossierStat("PAGES", "${profile.pagesRead}", "turned", Modifier.weight(1f))
            DossierStat("TIME", formatMinutes(profile.minutesRead), "inside books", Modifier.weight(1f))
            DossierStat("TIER", "${profile.rankIndex + 1}", "castle", Modifier.weight(1f))
        }
    }
}

@Composable
private fun DossierStat(
    eyebrow: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(VeilPalette.Archive.copy(alpha = 0.68f))
            .border(
                BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.76f)),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            eyebrow,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.85.sp),
            color = VeilPalette.Brass.copy(alpha = 0.78f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = VeilPalette.Moon
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.72f),
            maxLines = 1
        )
    }
}

@Composable
private fun SigilProgressRow(name: String, value: Int, target: Int, earned: Boolean) {
    val progress = if (earned) 1f else (value.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                if (earned) VeilPalette.DeepBrass.copy(alpha = 0.24f)
                else VeilPalette.Archive.copy(alpha = 0.62f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (earned) VeilPalette.Brass.copy(alpha = 0.42f)
                    else VeilPalette.BorderDark.copy(alpha = 0.76f)
                ),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 11.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(
                    BorderStroke(
                        1.dp,
                        if (earned) VeilPalette.Brass.copy(alpha = 0.68f)
                        else VeilPalette.Mist.copy(alpha = 0.26f)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (earned) "✦" else "·",
                color = if (earned) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.40f),
                fontSize = 14.sp
            )
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    color = VeilPalette.Moon
                )
                Text(
                    if (earned) "AWAKENED" else "${value.coerceAtMost(target)}/$target",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (earned) VeilPalette.Brass else VeilPalette.Mist
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = if (earned) VeilPalette.Brass else VeilPalette.Spirit,
                trackColor = VeilPalette.Moon.copy(alpha = 0.07f),
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun DiscoveryCard(
    index: Int,
    discovery: VeiledDiscoveryPresentation,
    record: VeiledDiscoveryRecord?
) {
    val language = LocalVeilLanguage.current
    val revealed = record != null
    val shape = MaterialTheme.shapes.extraSmall
    val accent = if (revealed) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.48f)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (revealed) VeilPalette.DeepBrass.copy(alpha = 0.18f)
                else VeilPalette.Archive.copy(alpha = 0.56f)
            )
            .border(BorderStroke(1.dp, accent.copy(alpha = if (revealed) 0.55f else 0.35f)), shape)
            .padding(VeilSpacing.md)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
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
                    color = if (revealed) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.62f)
                )
                Text(
                    if (revealed) "REVEALED" else "CLUE",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp),
                    color = accent
                )
                Text(
                    if (revealed) discovery.lore else discovery.clue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist
                )
                if (revealed) {
                    Text(
                        buildString {
                            append("PERMANENT LEDGER")
                            record.recordedAtEpochMs.let { timestamp ->
                                append(" · RECORDED ").append(
                                    formatDossierDate(timestamp, language)
                                )
                            }
                        },
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                        color = VeilPalette.Brass.copy(alpha = 0.82f)
                    )
                    Text(
                        "Once recorded, this discovery remains part of your local reading history even if a temporary signal such as a streak later changes.",
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Spirit
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionHeading(eyebrow: String, title: String, trailing: String? = null) {
    val arabicScriptEyebrow = usesArabicScript(eyebrow)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                if (arabicScriptEyebrow) eyebrow else eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = if (arabicScriptEyebrow) 0.sp else 1.5.sp
                ),
                color = VeilPalette.Brass
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Mist,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
