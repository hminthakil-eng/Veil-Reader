package com.veilreader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookReturnRitual
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.domain.ReturnRitualKind
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.veilContentTextStyle

enum class BookEntryStage {
    PREPARING,
    HANDOFF
}

enum class BookEntryMemoryKind {
    FIRST_ENTRY,
    OPENING_AGAIN,
    RETURNING_PROGRESS,
    COMPLETED_RETURN
}

data class BookEntryMemory(
    val returning: Boolean,
    val progressPercent: Int,
    val chapter: String?,
    val kind: BookEntryMemoryKind,
    val returnGapMillis: Long?,
    val priorSessionCount: Int,
    val totalActiveMillis: Long
)

fun bookEntryMemory(
    book: Book,
    continuity: ReadingContinuitySummary? = null
): BookEntryMemory {
    val progress = book.progress.coerceIn(0f, 1f)
    val percent = (progress * 100f).toInt().coerceIn(0, 100)
    val chapter = book.currentChapter
        .trim()
        .takeIf { it.isNotBlank() && !it.equals("Not started", ignoreCase = true) }
    val returning =
        book.finished ||
            progress > 0f ||
            book.lastOpenedAtEpochMs > 0L ||
            !book.locatorJson.isNullOrBlank()


    val kind = when {
        book.finished -> BookEntryMemoryKind.COMPLETED_RETURN
        progress > 0f -> BookEntryMemoryKind.RETURNING_PROGRESS
        returning -> BookEntryMemoryKind.OPENING_AGAIN
        else -> BookEntryMemoryKind.FIRST_ENTRY
    }
    val prior = continuity?.takeIf { it.hasHistory }

    return BookEntryMemory(
        returning = returning,
        progressPercent = percent,
        chapter = chapter,
        kind = kind,
        returnGapMillis = prior?.returnGapMillis,
        priorSessionCount = prior?.priorSessionCount?.coerceAtLeast(0) ?: 0,
        totalActiveMillis = prior?.totalActiveMillis?.coerceAtLeast(0L) ?: 0L
    )
}
@Composable
private fun bookEntryMemoryLabel(memory: BookEntryMemory): String =
    when (memory.kind) {
        BookEntryMemoryKind.COMPLETED_RETURN ->
            stringResource(R.string.entry_memory_completed_return)
        BookEntryMemoryKind.RETURNING_PROGRESS ->
            memory.chapter?.let { chapter ->
                stringResource(
                    R.string.entry_memory_progress_chapter,
                    memory.progressPercent,
                    chapter
                )
            } ?: stringResource(
                R.string.entry_memory_progress,
                memory.progressPercent
            )
        BookEntryMemoryKind.OPENING_AGAIN ->
            stringResource(R.string.entry_memory_opening_again)
        BookEntryMemoryKind.FIRST_ENTRY ->
            stringResource(R.string.entry_memory_first_entry)
    }

@Composable
private fun bookEntryReturnGapLabel(memory: BookEntryMemory): String? {
    val gap = memory.returnGapMillis ?: return null
    val hour = 60L * 60L * 1000L
    val day = 24L * hour
    return when {
        gap < 2L * hour -> stringResource(R.string.entry_gap_returning_page)
        gap < 2L * day -> {
            val hours = (gap / hour).coerceAtLeast(2L).toInt()
            pluralStringResource(R.plurals.entry_gap_hours, hours, hours)
        }
        gap < 60L * day -> {
            val days = (gap / day).coerceAtLeast(2L).toInt()
            pluralStringResource(R.plurals.entry_gap_days, days, days)
        }
        else -> {
            val months = (gap / (30L * day)).coerceAtLeast(2L).toInt()
            pluralStringResource(R.plurals.entry_gap_months, months, months)
        }
    }
}

@Composable
private fun bookEntryHistoryLabel(memory: BookEntryMemory): String? {
    if (memory.priorSessionCount <= 0) return null
    val sessions = pluralStringResource(
        R.plurals.entry_prior_sessions,
        memory.priorSessionCount,
        memory.priorSessionCount
    )
    return stringResource(
        R.string.entry_history_preserved,
        sessions,
        bookEntryDurationLabel(memory.totalActiveMillis)
    )
}

@Composable
private fun bookEntryDurationLabel(activeMillis: Long): String {
    val minutes = activeMillis.coerceAtLeast(0L) / 60_000L
    return when {
        minutes >= 60L -> {
            val hours = (minutes / 60L).toInt()
            val rest = (minutes % 60L).toInt()
            if (rest == 0) {
                stringResource(R.string.capsule_duration_hours, hours)
            } else {
                stringResource(R.string.capsule_duration_hours_minutes, hours, rest)
            }
        }
        minutes > 0L ->
            stringResource(R.string.capsule_duration_minutes, minutes.toInt())
        else -> stringResource(R.string.capsule_duration_less_than_minute)
    }
}

@Composable
private fun returnRitualTitle(ritual: BookReturnRitual): String =
    when (ritual.kind) {
        ReturnRitualKind.FORGOTTEN_VOLUME ->
            stringResource(R.string.library_memory_forgotten_title)
    }

@Composable
private fun returnRitualSilenceLabel(silenceMillis: Long): String {
    val days = silenceMillis.coerceAtLeast(0L) / 86_400_000L
    return when {
        days >= 365L -> {
            val years = (days / 365L).toInt()
            val months = ((days % 365L) / 30L).toInt()
            if (months > 0) {
                stringResource(R.string.ritual_silent_year_month, years, months)
            } else {
                pluralStringResource(R.plurals.ritual_silent_years, years, years)
            }
        }
        else -> {
            val months = (days / 30L).coerceAtLeast(6L).toInt()
            pluralStringResource(R.plurals.ritual_silent_months, months, months)
        }
    }
}

@Composable
private fun returnRitualFragmentAgeLabel(ageDays: Long): String {
    val days = ageDays.coerceAtLeast(0L)
    return when {
        days >= 365L -> {
            val years = (days / 365L).toInt()
            val months = ((days % 365L) / 30L).toInt()
            if (months > 0) {
                stringResource(R.string.ritual_preserved_year_month, years, months)
            } else {
                pluralStringResource(R.plurals.ritual_preserved_years, years, years)
            }
        }
        days >= 60L -> {
            val months = (days / 30L).toInt()
            pluralStringResource(R.plurals.ritual_preserved_months, months, months)
        }
        else -> {
            val count = days.toInt()
            pluralStringResource(R.plurals.ritual_preserved_days, count, count)
        }
    }
}
/**
 * Continuity bridge between the app shell and the reading surface.
 *
 * PREPARING is shown while Readium opens the publication. HANDOFF remains above the Reader until
 * its navigator is attached, preventing a flash of unfinished reader chrome or a blank fragment.
 */
@Composable
fun BookThresholdTransitionOverlay(
    book: Book,
    stage: BookEntryStage,
    visible: Boolean,
    continuity: ReadingContinuitySummary? = null,
    returnRitual: BookReturnRitual? = null,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalVeilReducedMotion.current

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            tween(
                if (reducedMotion) {
                    VeilMotion.REDUCED_MOTION_FADE_MS
                } else {
                    VeilMotion.FUNCTIONAL_ENTER_MS
                }
            )
        ),
        exit = fadeOut(
            tween(
                if (reducedMotion) {
                    VeilMotion.REDUCED_MOTION_FADE_MS
                } else {
                    VeilMotion.FUNCTIONAL_MS
                }
            )
        )
    ) {
        val artifact = remember(book) { bookArtifactState(book) }
        val memory = remember(book, continuity) { bookEntryMemory(book, continuity) }
        val ritual = remember(book.id, returnRitual) {
            returnRitual?.takeIf { it.bookId == book.id }
        }
        val aura = remember(artifact) { fallbackBookAura(artifact) }
        val scale = remember(book.id, stage, reducedMotion) {
            Animatable(
                if (reducedMotion) {
                    1f
                } else if (stage == BookEntryStage.PREPARING) {
                    0.94f
                } else {
                    1f
                }
            )
        }
        val seal = remember(book.id, stage, ritual?.kind, reducedMotion) {
            Animatable(
                if (reducedMotion || ritual == null) {
                    1f
                } else {
                    0f
                }
            )
        }

        LaunchedEffect(book.id, stage, reducedMotion) {
            if (reducedMotion) {
                scale.snapTo(1f)
            } else {
                scale.animateTo(
                    targetValue = if (stage == BookEntryStage.PREPARING) 1f else 1.055f,
                    animationSpec = tween(
                        durationMillis = if (stage == BookEntryStage.PREPARING) {
                            VeilMotion.SPATIAL_MS
                        } else {
                            VeilMotion.RITUAL_MS
                        }
                    )
                )
            }
        }

        LaunchedEffect(book.id, stage, ritual?.kind, reducedMotion) {
            if (ritual != null) {
                if (reducedMotion) {
                    seal.snapTo(1f)
                } else {
                    seal.snapTo(0f)
                    seal.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = if (stage == BookEntryStage.PREPARING) {
                                VeilMotion.RITUAL_MS
                            } else {
                                VeilMotion.SPATIAL_MS
                            }
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VeilPalette.Ink)
                .grayfogAtmosphere(
                    realm = VeilRealm.THRESHOLD,
                    seed = book.title.hashCode(),
                    intensity = if (stage == BookEntryStage.PREPARING) 0.94f else 0.72f
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.66f)
                        )
                    )
            )

            GrayfogOrnamentFrame(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                strength = when {
                    ritual != null && stage == BookEntryStage.PREPARING -> 0.72f
                    stage == BookEntryStage.PREPARING -> 0.52f
                    else -> 0.34f
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val stageLabel = when {
                    ritual != null && stage == BookEntryStage.PREPARING ->
                        stringResource(R.string.entry_stage_return_ritual)
                    ritual != null -> stringResource(R.string.entry_stage_old_seal)
                    stage == BookEntryStage.PREPARING ->
                        stringResource(R.string.entry_stage_opening_threshold)
                    else -> stringResource(R.string.entry_stage_entering_sanctuary)
                }
                Text(
                    stageLabel,
                    style = veilContentTextStyle(
                        MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.65.sp),
                        stageLabel
                    ),
                    color = VeilPalette.Brass
                )

                Box(
                    modifier = Modifier
                        .width(136.dp)
                        .height(200.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                ) {
                    BookCover(
                        title = book.title,
                        subtitle = book.author,
                        imagePath = book.coverCachePath,
                        artifact = artifact,
                        modifier = Modifier.matchParentSize()
                    )

                    if (ritual != null) {
                        ReturnRitualSeal(
                            progress = seal.value,
                            modifier = Modifier
                                .matchParentSize()
                                .padding(7.dp)
                        )
                    }

                    if (memory.returning && !book.finished) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 21.dp)
                                .width(10.dp)
                                .height(38.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            VeilPalette.MoonCrimson.copy(alpha = 0.92f),
                                            VeilPalette.MoonCrimson.copy(alpha = 0.62f)
                                        )
                                    )
                                )
                        )
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 23.dp, top = 33.dp)
                                .size(6.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(VeilPalette.MoonCrimson.copy(alpha = 0.72f))
                        )
                    }
                }

                Text(
                    book.title,
                    style = veilContentTextStyle(MaterialTheme.typography.titleLarge, book.title),
                    color = VeilPalette.Moon,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.author.isNotBlank()) {
                    Text(
                        book.author,
                        style = veilContentTextStyle(MaterialTheme.typography.bodySmall, book.author),
                        color = VeilPalette.Mist.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (ritual != null) {
                    val ritualTitle = returnRitualTitle(ritual)
                    Text(
                        ritualTitle,
                        style = veilContentTextStyle(
                            MaterialTheme.typography.titleMedium,
                            ritualTitle
                        ),
                        color = VeilPalette.Brass
                    )
                    val silenceLabel = returnRitualSilenceLabel(ritual.silenceMillis)
                    Text(
                        silenceLabel,
                        style = veilContentTextStyle(
                            MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.05.sp),
                            silenceLabel
                        ),
                        color = VeilPalette.Moon.copy(alpha = 0.82f)
                    )
                    if (stage == BookEntryStage.PREPARING) {
                        val invocation = stringResource(R.string.ritual_forgotten_invocation)
                        Text(
                            invocation,
                            style = veilContentTextStyle(MaterialTheme.typography.bodySmall, invocation),
                            color = VeilPalette.Mist.copy(alpha = 0.66f)
                        )
                        ritual.fragment?.let { fragment ->
                            val marginLabel = if (fragment.annotated) {
                                stringResource(R.string.ritual_annotated_margin)
                            } else {
                                stringResource(R.string.ritual_preserved_margin)
                            }
                            ReturnRitualFragmentPanel(
                                quote = fragment.quote
                                    ?: stringResource(R.string.library_memory_preserved_passage),
                                label = "$marginLabel · ${returnRitualFragmentAgeLabel(fragment.ageDays)}"
                            )
                        }
                    }
                } else {
                    bookEntryReturnGapLabel(memory)?.let { returnLabel ->
                        Text(
                            returnLabel,
                            style = veilContentTextStyle(
                                MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.92.sp),
                                returnLabel
                            ),
                            color = VeilPalette.Brass.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val memoryLabel = bookEntryMemoryLabel(memory)
                Text(
                    memoryLabel,
                    style = veilContentTextStyle(
                        MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                        memoryLabel
                    ),
                    color = aura.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                bookEntryHistoryLabel(memory)?.let { historyLabel ->
                    Text(
                        historyLabel,
                        style = veilContentTextStyle(
                            MaterialTheme.typography.labelSmall,
                            historyLabel
                        ),
                        color = VeilPalette.Mist.copy(alpha = 0.58f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (stage == BookEntryStage.PREPARING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(118.dp)
                            .height(1.dp),
                        color = VeilPalette.Brass.copy(alpha = 0.82f),
                        trackColor = VeilPalette.Moon.copy(alpha = 0.08f)
                    )
                } else {
                    val handoffLabel = when {
                        ritual != null -> stringResource(R.string.entry_handoff_ritual)
                        memory.returning -> stringResource(R.string.entry_handoff_returning)
                        else -> stringResource(R.string.entry_handoff_first)
                    }
                    Text(
                        handoffLabel,
                        style = veilContentTextStyle(MaterialTheme.typography.bodySmall, handoffLabel),
                        color = VeilPalette.Mist.copy(alpha = 0.58f)
                    )
                }
            }
        }
    }
}


@Composable
private fun ReturnRitualSeal(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val p = progress.coerceIn(0f, 1f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * (0.45f - (1f - p) * 0.05f)
        val brass = VeilPalette.Brass

        drawCircle(
            color = brass.copy(alpha = 0.16f + p * 0.28f),
            radius = radius,
            center = center,
            style = Stroke(1.1.dp.toPx())
        )
        drawCircle(
            color = brass.copy(alpha = 0.08f + p * 0.18f),
            radius = radius * 0.86f,
            center = center,
            style = Stroke(0.7.dp.toPx())
        )

        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0 - 90.0)
            val inner = radius * 0.91f
            val outer = radius * (1.02f + p * 0.03f)
            val sx = center.x + kotlin.math.cos(angle).toFloat() * inner
            val sy = center.y + kotlin.math.sin(angle).toFloat() * inner
            val ex = center.x + kotlin.math.cos(angle).toFloat() * outer
            val ey = center.y + kotlin.math.sin(angle).toFloat() * outer
            drawLine(
                color = brass.copy(alpha = 0.12f + p * 0.36f),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = 0.8.dp.toPx()
            )
        }
    }
}

@Composable
private fun ReturnRitualFragmentPanel(
    quote: String,
    label: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = VeilPalette.Brass.copy(alpha = 0.26f),
                shape = MaterialTheme.shapes.extraSmall
            )
            .background(
                color = VeilPalette.Archive.copy(alpha = 0.48f),
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = veilContentTextStyle(
                MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                label
            ),
            color = VeilPalette.Brass.copy(alpha = 0.78f)
        )
        Text(
            "“$quote”",
            style = veilContentTextStyle(MaterialTheme.typography.bodySmall, quote),
            color = VeilPalette.Moon.copy(alpha = 0.80f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
