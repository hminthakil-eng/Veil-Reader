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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookReturnRitual
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.grayfogAtmosphere

enum class BookEntryStage {
    PREPARING,
    HANDOFF
}

data class BookEntryMemory(
    val returning: Boolean,
    val progressPercent: Int,
    val chapter: String?
)

fun bookEntryMemory(book: Book): BookEntryMemory {
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

    return BookEntryMemory(
        returning = returning,
        progressPercent = percent,
        chapter = chapter
    )
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
        val memory = remember(book) { bookEntryMemory(book) }
        val ritual = remember(book.id, returnRitual) {
            returnRitual?.takeIf { it.bookId == book.id }
        }
        val entryStatus = localizedBookEntryStatus(book, memory)
        val returnGap = continuity?.let { localizedReadingReturnGap(it) }
        val history = continuity?.let { localizedReadingHistory(it) }
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
                Text(
                    stringResource(
                        when {
                            ritual != null && stage == BookEntryStage.PREPARING ->
                                R.string.entry_return_ritual_deep_shelf
                            ritual != null -> R.string.entry_old_seal_opens
                            stage == BookEntryStage.PREPARING -> R.string.entry_opening_threshold
                            else -> R.string.entry_entering_sanctuary
                        }
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.65.sp),
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
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.author.isNotBlank()) {
                    Text(
                        book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (ritual != null) {
                    Text(
                        localizedReturnRitualTitle(ritual).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = VeilPalette.Brass
                    )
                    Text(
                        localizedReturnRitualSilence(ritual.silenceMillis),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.05.sp),
                        color = VeilPalette.Moon.copy(alpha = 0.82f)
                    )
                    if (stage == BookEntryStage.PREPARING) {
                        Text(
                            localizedReturnRitualInvocation(ritual),
                            style = MaterialTheme.typography.bodySmall,
                            color = VeilPalette.Mist.copy(alpha = 0.66f)
                        )
                        ritual.fragment?.let { fragment ->
                            ReturnRitualFragmentPanel(
                                quote = fragment.quote.ifBlank {
                                    stringResource(R.string.library_memory_preserved_passage)
                                },
                                label = stringResource(
                                    R.string.entry_margin_age,
                                    stringResource(
                                        if (fragment.annotated) R.string.entry_annotated_margin
                                        else R.string.entry_preserved_margin
                                    ),
                                    localizedReturnRitualFragmentAge(fragment.ageDays)
                                )
                            )
                        }
                    }
                } else {
                    returnGap?.let { returnLabel ->
                        Text(
                            returnLabel,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.92.sp),
                            color = VeilPalette.Brass.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    entryStatus,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                    color = aura.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                history?.let { historyLabel ->
                    Text(
                        historyLabel,
                        style = MaterialTheme.typography.labelSmall,
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
                    Text(
                        stringResource(
                            when {
                                ritual != null -> R.string.entry_handoff_ritual
                                memory.returning -> R.string.entry_handoff_returning
                                else -> R.string.entry_handoff_first
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.58f)
                    )
                }
            }
        }
    }
}


@Composable
private fun localizedBookEntryStatus(book: Book, memory: BookEntryMemory): String = when {
    book.finished -> stringResource(R.string.entry_status_completed_returning)
    memory.progressPercent > 0 && memory.chapter != null -> stringResource(
        R.string.entry_status_returning_chapter,
        memory.progressPercent,
        memory.chapter
    )
    memory.progressPercent > 0 -> stringResource(
        R.string.entry_status_returning_progress,
        memory.progressPercent
    )
    memory.returning -> stringResource(R.string.entry_status_opening_again)
    else -> stringResource(R.string.entry_status_first_entry)
}

@Composable
private fun localizedReadingReturnGap(summary: ReadingContinuitySummary): String? {
    val gap = summary.returnGapMillis ?: return null
    if (!summary.hasHistory) return null
    val hour = 60L * 60L * 1000L
    val day = 24L * hour
    return when {
        gap < 2L * hour -> stringResource(R.string.entry_returning_to_page)
        gap < 2L * day -> stringResource(
            R.string.entry_returned_hours,
            (gap / hour).coerceAtLeast(2L)
        )
        gap < 60L * day -> stringResource(
            R.string.entry_returned_days,
            (gap / day).coerceAtLeast(2L)
        )
        else -> stringResource(
            R.string.entry_returned_months,
            (gap / (30L * day)).coerceAtLeast(2L)
        )
    }
}

@Composable
private fun localizedReadingHistory(summary: ReadingContinuitySummary): String? {
    if (!summary.hasHistory || summary.priorSessionCount <= 0) return null
    val totalMinutes = summary.totalActiveMillis / 60_000L
    val duration = when {
        totalMinutes >= 60L -> {
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            if (minutes == 0L) {
                stringResource(R.string.entry_duration_hours, hours)
            } else {
                stringResource(R.string.entry_duration_hours_minutes, hours, minutes)
            }
        }
        totalMinutes > 0L -> stringResource(R.string.entry_duration_minutes, totalMinutes)
        else -> stringResource(R.string.entry_duration_less_minute)
    }
    return stringResource(
        if (summary.priorSessionCount == 1) R.string.entry_history_one_session
        else R.string.entry_history_many_sessions,
        summary.priorSessionCount,
        duration
    )
}

@Composable
private fun localizedReturnRitualTitle(ritual: BookReturnRitual): String = when (ritual.kind) {
    com.veilreader.app.domain.ReturnRitualKind.FORGOTTEN_VOLUME ->
        stringResource(R.string.entry_ritual_forgotten_title)
}

@Composable
private fun localizedReturnRitualInvocation(ritual: BookReturnRitual): String = when (ritual.kind) {
    com.veilreader.app.domain.ReturnRitualKind.FORGOTTEN_VOLUME ->
        stringResource(R.string.entry_ritual_forgotten_invocation)
}

@Composable
private fun localizedReturnRitualSilence(gapMillis: Long): String {
    val days = gapMillis.coerceAtLeast(0L) / 86_400_000L
    return when {
        days >= 730L -> {
            val years = days / 365L
            val months = (days % 365L) / 30L
            if (months > 0L) stringResource(R.string.entry_silent_years_months, years, months)
            else stringResource(R.string.entry_silent_years, years)
        }
        days >= 365L -> {
            val months = (days % 365L) / 30L
            if (months > 0L) stringResource(R.string.entry_silent_one_year_months, months)
            else stringResource(R.string.entry_silent_one_year)
        }
        else -> stringResource(R.string.entry_silent_months, (days / 30L).coerceAtLeast(6L))
    }
}

@Composable
private fun localizedReturnRitualFragmentAge(ageDays: Long): String = when {
    ageDays >= 730L -> stringResource(R.string.entry_preserved_years, ageDays / 365L)
    ageDays >= 365L -> stringResource(R.string.entry_preserved_one_year)
    ageDays >= 60L -> stringResource(R.string.entry_preserved_months, ageDays / 30L)
    else -> stringResource(R.string.entry_preserved_days, ageDays)
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
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
            color = VeilPalette.Brass.copy(alpha = 0.78f)
        )
        Text(
            "“$quote”",
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Moon.copy(alpha = 0.80f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
