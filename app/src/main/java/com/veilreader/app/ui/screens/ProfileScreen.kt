package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.PathMasteryAxis
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.effectivePathMastery
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere
import java.text.DateFormat
import java.util.Date

private data class VeiledDiscovery(
    val id: String,
    val symbol: String
)

private val veiledDiscoveries = listOf(
    VeiledDiscovery("patient_flame", "◈"),
    VeiledDiscovery("marginalia_gate", "✧"),
    VeiledDiscovery("deep_shelf", "▥"),
    VeiledDiscovery("long_watch", "◐"),
    VeiledDiscovery("veil_thins", "⌁"),
    VeiledDiscovery("unnamed_chamber", "⬡")
)

private fun VeiledDiscovery.isRevealed(profile: ReaderProfile): Boolean =
    id in profile.earnedDiscoveries

@Composable
fun ProfileScreen(
    profile: ReaderProfile,
    highlightCount: Int,
    dailyGoalMinutes: Int,
    castleTitle: String,
    equippedSigilId: String?,
    books: List<Book> = emptyList(),
    readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    readingCycles: List<ReadingCycleRecord> = emptyList(),
    onSetDailyGoal: (Int) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val p = profile
    val pathName = localizedPathName(p.path)
    val rankName = localizedRankName(p.path.id, p.rankIndex, p.rankName)
    val revealedDiscoveries = veiledDiscoveries.count { it.isRevealed(p) }
    val dossierHistory = remember(books, readingSessions, readingCycles) {
        deriveReaderDossierHistory(
            books = books,
            sessions = readingSessions,
            cycles = readingCycles
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = p.level * 17 + dossierHistory.recordedSessionCount
            ),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        ScreenHeader(
            eyebrow = stringResource(R.string.profile_header_eyebrow),
            title = localizedCastleTitle(p, castleTitle),
            subtitle = "$pathName · $rankName"
        )

        VeilReveal(delayMillis = 40, distance = 10.dp) {
            ArchivistDossierPanel(
                profile = p,
                highlightCount = highlightCount,
                equippedSigilId = equippedSigilId,
                revealedDiscoveries = revealedDiscoveries,
                totalDiscoveries = veiledDiscoveries.size,
                onOpenSettings = onOpenSettings
            )
        }

        ProfileMasteryPanel(profile = p)

        ProfileSectionHeading(
            eyebrow = stringResource(R.string.profile_recorded_history),
            title = stringResource(R.string.profile_reading_record)
        )
        DossierRecordGrid(
            profile = p,
            highlightCount = highlightCount
        )

        DossierHistoryLedger(dossierHistory)

        ProfileSectionHeading(
            eyebrow = stringResource(R.string.profile_rhythm),
            title = stringResource(R.string.profile_daily_goal)
        )
        Text(
            stringResource(R.string.profile_daily_goal_body),
            color = VeilPalette.Mist,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.profile_minutes_current, minutes), style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSetDailyGoal(minutes) },
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.82f)),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.profile_minutes, minutes), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        ProfileSectionHeading(
            eyebrow = stringResource(R.string.profile_known_marks),
            title = stringResource(R.string.profile_sigil_registry),
            trailing = stringResource(R.string.profile_awakened_count, p.earnedSigils.size)
        )

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(
                "first_hour" to (p.minutesRead to 60),
                "passage_keeper" to (highlightCount to 10),
                "seven_days" to (p.longestStreakDays to 7),
                "ten_tomes" to (p.booksFinished to 10),
                "first_threshold" to (p.rankIndex to 1)
            ).forEachIndexed { index, (id, progress) ->
                val name = localizedSigilName(id)
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
            eyebrow = stringResource(R.string.profile_restricted_folio),
            title = stringResource(R.string.profile_veiled_discoveries),
            trailing = stringResource(
                R.string.profile_revealed_count,
                revealedDiscoveries,
                veiledDiscoveries.size
            )
        )
        Text(
            stringResource(R.string.profile_discovery_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Mist
        )

        veiledDiscoveries.forEachIndexed { index, discovery ->
            VeilReveal(
                delayMillis = 60 + index * 40,
                distance = 8.dp
            ) {
                DiscoveryCard(
                    index = index,
                    discovery = discovery,
                    revealed = discovery.isRevealed(p)
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
            Text(stringResource(R.string.profile_open_hidden_archive), style = MaterialTheme.typography.labelMedium)
        }
    }
    }
}

@Composable
private fun ProfileMasteryPanel(profile: ReaderProfile) {
    val mastery = effectivePathMastery(profile)
    val doctrine = localizedPathDoctrine(profile.path.id)
    val status = stringResource(
        if (mastery.ritualReady) R.string.profile_mastery_ready
        else R.string.profile_mastery_building
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.64f),
        border = BorderStroke(
            1.dp,
            if (mastery.ritualReady) VeilPalette.Brass.copy(alpha = 0.54f)
            else VeilPalette.BorderDark.copy(alpha = 0.78f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.profile_mastery_eyebrow),
                        strong = true
                    )
                    Text(
                        stringResource(R.string.profile_mastery_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon
                    )
                }
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (mastery.ritualReady) VeilPalette.Brass else VeilPalette.Mist
                )
            }

            Text(
                doctrine.maxim,
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Spirit.copy(alpha = 0.82f)
            )

            ProfileMasteryAxisRow(doctrine.embodimentName, mastery.embodiment, VeilPalette.Brass)
            ProfileMasteryAxisRow(doctrine.insightName, mastery.insight, VeilPalette.Spirit)
            ProfileMasteryAxisRow(doctrine.stabilityName, mastery.stability, VeilPalette.Moon)

            if (mastery.dissonance > 0 && !mastery.ritualReady) {
                Text(
                    stringResource(R.string.profile_mastery_dissonance, mastery.dissonance),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.70f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMasteryAxisRow(
    label: String,
    axis: PathMasteryAxis,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = VeilPalette.Moon)
            Text(
                "${axis.value}/${axis.target}",
                style = MaterialTheme.typography.labelSmall,
                color = if (axis.ready) color else VeilPalette.Mist
            )
        }
        LinearProgressIndicator(
            progress = { axis.progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = color,
            trackColor = VeilPalette.Moon.copy(alpha = 0.07f),
            drawStopIndicator = {}
        )
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
    val startDate = formatDossierDate(history.firstRecordedAtEpochMs)
    val endDate = formatDossierDate(history.latestRecordedAtEpochMs)
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
                VeilMicroLabel(
                    text = stringResource(R.string.profile_ledger_eyebrow),
                    strong = true
                )
                Text(
                    stringResource(R.string.profile_ledger_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
            }
            Text(
                stringResource(R.string.profile_sessions_count, history.recordedSessionCount),
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Mist.copy(alpha = 0.72f)
            )
        }

        BrassRule(Modifier.fillMaxWidth())

        DossierLedgerLine(
            label = stringResource(R.string.profile_archive_span),
            value = "$startDate — $endDate"
        )
        DossierLedgerLine(
            label = stringResource(R.string.profile_active_time),
            value = formatDossierDuration(history.recordedActiveMillis)
        )
        DossierLedgerLine(
            label = stringResource(R.string.profile_completion_records),
            value = history.completionCycleCount.toString()
        )
        DossierLedgerLine(
            label = stringResource(R.string.profile_reread_cycles),
            value = history.rereadCycleCount.toString()
        )
        DossierLedgerLine(
            label = stringResource(R.string.profile_archived_volumes),
            value = history.archivedVolumeCount.toString()
        )

        Text(
            stringResource(R.string.profile_ledger_note),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VeilMicroLabel(
            text = label,
            modifier = Modifier.weight(1f),
            color = VeilPalette.Mist.copy(alpha = 0.62f)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = VeilPalette.Moon,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun formatDossierDate(epochMs: Long?): String =
    epochMs
        ?.takeIf { it > 0L }
        ?.let {
            DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(Date(it))
        }
        ?: stringResource(R.string.profile_no_record)

@Composable
private fun formatDossierDuration(activeMillis: Long): String {
    val minutes = activeMillis.coerceAtLeast(0L) / 60_000L
    return when {
        minutes >= 60L -> {
            val hours = minutes / 60L
            val rest = minutes % 60L
            if (rest == 0L) {
                stringResource(R.string.profile_duration_hours, hours)
            } else {
                stringResource(R.string.profile_duration_hours_minutes, hours, rest)
            }
        }
        minutes > 0L -> stringResource(R.string.profile_duration_minutes, minutes)
        else -> stringResource(R.string.profile_duration_under_minute)
    }
}

@Composable
private fun ArchivistDossierPanel(
    profile: ReaderProfile,
    highlightCount: Int,
    equippedSigilId: String?,
    revealedDiscoveries: Int,
    totalDiscoveries: Int,
    onOpenSettings: () -> Unit
) {
    val xpTarget = profile.xpForNextLevel.coerceAtLeast(1)
    val xpProgress = (profile.xp.toFloat() / xpTarget).coerceIn(0f, 1f)
    val identity = localizedPathIdentity(profile.path)
    val pathName = localizedPathName(profile.path)
    val rankName = localizedRankName(profile.path.id, profile.rankIndex, profile.rankName)
    val equippedSigilName = equippedSigilId?.let { localizedSigilName(it) }

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
                    VeilMicroLabel(
                        text = stringResource(R.string.profile_private_record),
                        strong = true
                    )
                    Text(
                        rankName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VeilPalette.Moon
                    )
                    Text(
                        identity.epithet,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist
                    )
                }

                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.profile_settings)
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
                    DossierFact(stringResource(R.string.profile_fact_path), pathName)
                    DossierFact(stringResource(R.string.profile_fact_level), profile.level.toString())
                    DossierFact(
                        stringResource(R.string.profile_fact_castle_tier),
                        (profile.rankIndex + 1).toString()
                    )
                    equippedSigilName?.let {
                        DossierFact(stringResource(R.string.profile_fact_equipped_sigil), it)
                    }
                }
            }

            BrassRule(Modifier.fillMaxWidth())

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VeilMicroLabel(
                    text = stringResource(R.string.profile_experience),
                    color = VeilPalette.Mist
                )
                Text(
                    stringResource(R.string.profile_xp, profile.xp, xpTarget),
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
                    stringResource(R.string.profile_sigils_count, profile.earnedSigils.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.78f)
                )
                Text(
                    stringResource(
                        R.string.profile_discoveries_count,
                        revealedDiscoveries,
                        totalDiscoveries
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.78f)
                )
                Text(
                    stringResource(R.string.profile_marks_count, highlightCount),
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
            DossierStat(
                stringResource(R.string.profile_stat_return),
                stringResource(R.string.profile_stat_streak_value, profile.streakDays),
                stringResource(R.string.profile_stat_current_streak),
                Modifier.weight(1f)
            )
            DossierStat(
                stringResource(R.string.profile_stat_volumes),
                profile.booksFinished.toString(),
                stringResource(R.string.profile_stat_finished),
                Modifier.weight(1f)
            )
            DossierStat(
                stringResource(R.string.profile_stat_marks),
                highlightCount.toString(),
                stringResource(R.string.profile_stat_highlights),
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DossierStat(
                stringResource(R.string.profile_stat_pages),
                profile.pagesRead.toString(),
                stringResource(R.string.profile_stat_turned),
                Modifier.weight(1f)
            )
            DossierStat(
                stringResource(R.string.profile_stat_time),
                formatMinutes(profile.minutesRead),
                stringResource(R.string.profile_stat_inside_books),
                Modifier.weight(1f)
            )
            DossierStat(
                stringResource(R.string.profile_stat_tier),
                (profile.rankIndex + 1).toString(),
                stringResource(R.string.profile_stat_castle),
                Modifier.weight(1f)
            )
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
                    if (earned) {
                        stringResource(R.string.profile_awakened)
                    } else {
                        "${value.coerceAtMost(target)}/$target"
                    },
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
private fun DiscoveryCard(index: Int, discovery: VeiledDiscovery, revealed: Boolean) {
    val shape = MaterialTheme.shapes.extraSmall
    val accent = if (revealed) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.48f)
    val copy = localizedDiscoveryCopy(discovery.id)

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
                    if (revealed) copy.title
                    else stringResource(R.string.profile_veiled_fragment, index + 1),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (revealed) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.62f)
                )
                Text(
                    stringResource(
                        if (revealed) R.string.profile_revealed else R.string.profile_clue
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp),
                    color = accent
                )
                Text(
                    if (revealed) copy.lore else copy.clue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist
                )
                if (revealed) {
                    Text(
                        stringResource(R.string.profile_discovery_durable_note),
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
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

@Composable
private fun formatMinutes(minutes: Int): String =
    if (minutes < 60) {
        stringResource(R.string.profile_duration_minutes, minutes)
    } else {
        val hours = minutes / 60
        val rest = minutes % 60
        if (rest == 0) {
            stringResource(R.string.profile_duration_hours, hours)
        } else {
            stringResource(R.string.profile_duration_hours_minutes, hours, rest)
        }
    }
