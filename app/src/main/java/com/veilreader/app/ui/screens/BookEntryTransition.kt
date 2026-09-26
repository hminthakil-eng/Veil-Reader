package com.veilreader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
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
    val chapter: String?,
    val label: String,
    val returnGapLabel: String? = null,
    val historyLabel: String? = null
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

    val label = when {
        book.finished -> "COMPLETED VOLUME · RETURNING"
        progress > 0f && chapter != null -> "RETURNING · $percent% · $chapter"
        progress > 0f -> "RETURNING · $percent%"
        returning -> "OPENING AGAIN"
        else -> "FIRST ENTRY"
    }

    return BookEntryMemory(
        returning = returning,
        progressPercent = percent,
        chapter = chapter,
        label = label,
        returnGapLabel = continuity?.let(::readingReturnGapLabel),
        historyLabel = continuity?.let(::readingHistoryLabel)
    )
}

fun readingReturnGapLabel(summary: ReadingContinuitySummary): String? {
    val gap = summary.returnGapMillis ?: return null
    if (!summary.hasHistory) return null

    val hour = 60L * 60L * 1000L
    val day = 24L * hour
    return when {
        gap < 2L * hour -> "RETURNING TO THE PAGE"
        gap < 2L * day -> "RETURNED AFTER ${(gap / hour).coerceAtLeast(2L)} HOURS"
        gap < 60L * day -> "RETURNED AFTER ${(gap / day).coerceAtLeast(2L)} DAYS"
        else -> "RETURNED AFTER ${(gap / (30L * day)).coerceAtLeast(2L)} MONTHS"
    }
}

fun readingHistoryLabel(summary: ReadingContinuitySummary): String? {
    if (!summary.hasHistory || summary.priorSessionCount <= 0) return null
    val totalMinutes = summary.totalActiveMillis / 60_000L
    val duration = when {
        totalMinutes >= 60L -> {
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            if (minutes == 0L) "${hours}H" else "${hours}H ${minutes}M"
        }
        totalMinutes > 0L -> "${totalMinutes}M"
        else -> "<1M"
    }
    val sessionWord = if (summary.priorSessionCount == 1) "SESSION" else "SESSIONS"
    return "${summary.priorSessionCount} PRIOR $sessionWord · $duration PRESERVED"
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
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(260))
    ) {
        val artifact = remember(book) { bookArtifactState(book) }
        val memory = remember(book, continuity) { bookEntryMemory(book, continuity) }
        val aura = remember(artifact) { fallbackBookAura(artifact) }
        val scale = remember(book.id, stage) {
            Animatable(if (stage == BookEntryStage.PREPARING) 0.94f else 1f)
        }

        LaunchedEffect(book.id, stage) {
            scale.animateTo(
                targetValue = if (stage == BookEntryStage.PREPARING) 1f else 1.055f,
                animationSpec = tween(
                    durationMillis = if (stage == BookEntryStage.PREPARING) 320 else 560
                )
            )
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
                strength = if (stage == BookEntryStage.PREPARING) 0.52f else 0.34f
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
                    if (stage == BookEntryStage.PREPARING) {
                        "OPENING THRESHOLD"
                    } else {
                        "ENTERING SANCTUARY"
                    },
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

                memory.returnGapLabel?.let { returnLabel ->
                    Text(
                        returnLabel,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.92.sp),
                        color = VeilPalette.Brass.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    memory.label,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                    color = aura.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                memory.historyLabel?.let { historyLabel ->
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
                        if (memory.returning) {
                            "The archive recedes. Your book remains."
                        } else {
                            "The archive recedes. The first page remains."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.58f)
                    )
                }
            }
        }
    }
}
