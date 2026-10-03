package com.veilreader.app.domain

import kotlin.math.ceil
import kotlin.math.roundToLong

enum class ReadingPaceConfidence {
    LEARNING,
    ESTABLISHED,
    STRONG
}

data class ReadingPaceEstimate(
    val millisecondsPerPage: Double,
    val observedPageTurns: Int,
    val observedActiveMillis: Long,
    val confidence: ReadingPaceConfidence
)

data class ReadingTimeRemainingEstimate(
    val remainingPages: Int,
    val centerMillis: Long,
    val lowMillis: Long,
    val highMillis: Long,
    val confidence: ReadingPaceConfidence
)

/**
 * Learns a page pace only from sessions that contain both engaged time and real paced page turns.
 *
 * Scroll-only and TTS-only sessions naturally contribute no page-turn sample. Broad sanity bounds
 * reject corrupted/outlier snapshots without trying to impose a "normal" reading speed.
 */
fun deriveReadingPace(
    sessions: List<ReadingSessionSnapshot>,
    bookId: String
): ReadingPaceEstimate? {
    if (bookId.isBlank()) return null

    val eligible = sessions.asSequence()
        .filter { it.bookId == bookId }
        .filter { it.pacedPageTurns >= MIN_SESSION_PAGE_TURNS }
        .filter { it.activeMillis >= MIN_SESSION_ACTIVE_MILLIS }
        .mapNotNull { session ->
            val msPerPage = session.activeMillis.toDouble() /
                session.pacedPageTurns.toDouble()
            if (
                !msPerPage.isFinite() ||
                msPerPage !in MIN_MILLIS_PER_PAGE..MAX_MILLIS_PER_PAGE
            ) {
                null
            } else {
                session
            }
        }
        .toList()

    if (eligible.isEmpty()) return null

    val totalTurns = eligible.sumOf { it.pacedPageTurns.toLong() }
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
    val totalMillis = eligible.sumOf { it.activeMillis.coerceAtLeast(0L) }

    if (
        totalTurns < MIN_TOTAL_PAGE_TURNS ||
        totalMillis < MIN_TOTAL_ACTIVE_MILLIS
    ) {
        return null
    }

    val msPerPage = totalMillis.toDouble() / totalTurns.toDouble()
    if (!msPerPage.isFinite()) return null

    val confidence = when {
        totalTurns >= STRONG_PAGE_TURNS &&
            totalMillis >= STRONG_ACTIVE_MILLIS ->
            ReadingPaceConfidence.STRONG

        totalTurns >= ESTABLISHED_PAGE_TURNS &&
            totalMillis >= ESTABLISHED_ACTIVE_MILLIS ->
            ReadingPaceConfidence.ESTABLISHED

        else ->
            ReadingPaceConfidence.LEARNING
    }

    return ReadingPaceEstimate(
        millisecondsPerPage = msPerPage,
        observedPageTurns = totalTurns,
        observedActiveMillis = totalMillis,
        confidence = confidence
    )
}

/**
 * Estimates remaining book time only when the publication supplies a page count and a personal
 * pace has enough evidence. The UI should present this as an estimate, never an exact promise.
 */
fun estimateBookTimeRemaining(
    totalPages: Int,
    progress: Float,
    pace: ReadingPaceEstimate?
): ReadingTimeRemainingEstimate? {
    val safePace = pace ?: return null
    if (totalPages <= 0) return null

    val safeProgress = if (progress.isFinite()) {
        progress.coerceIn(0f, 1f)
    } else {
        return null
    }
    if (safeProgress >= 0.995f) {
        return ReadingTimeRemainingEstimate(
            remainingPages = 0,
            centerMillis = 0L,
            lowMillis = 0L,
            highMillis = 0L,
            confidence = safePace.confidence
        )
    }

    val remainingPages = ceil(totalPages * (1.0 - safeProgress.toDouble()))
        .toInt()
        .coerceIn(1, totalPages)

    val center = safeDurationMillis(
        remainingPages.toDouble() * safePace.millisecondsPerPage
    )
    val spread = when (safePace.confidence) {
        ReadingPaceConfidence.LEARNING -> 0.35
        ReadingPaceConfidence.ESTABLISHED -> 0.25
        ReadingPaceConfidence.STRONG -> 0.18
    }
    val low = safeDurationMillis(center.toDouble() * (1.0 - spread))
    val high = safeDurationMillis(center.toDouble() * (1.0 + spread))

    return ReadingTimeRemainingEstimate(
        remainingPages = remainingPages,
        centerMillis = center,
        lowMillis = low.coerceAtMost(center),
        highMillis = high.coerceAtLeast(center),
        confidence = safePace.confidence
    )
}

private fun safeDurationMillis(value: Double): Long =
    value
        .takeIf { it.isFinite() && it >= 0.0 }
        ?.coerceAtMost(MAX_ESTIMATE_MILLIS.toDouble())
        ?.roundToLong()
        ?: 0L

private const val MIN_SESSION_PAGE_TURNS = 3
private const val MIN_SESSION_ACTIVE_MILLIS = 30_000L
private const val MIN_TOTAL_PAGE_TURNS = 8
private const val MIN_TOTAL_ACTIVE_MILLIS = 3L * 60L * 1000L

private const val ESTABLISHED_PAGE_TURNS = 20
private const val ESTABLISHED_ACTIVE_MILLIS = 8L * 60L * 1000L
private const val STRONG_PAGE_TURNS = 50
private const val STRONG_ACTIVE_MILLIS = 20L * 60L * 1000L

private const val MIN_MILLIS_PER_PAGE = 5_000.0
private const val MAX_MILLIS_PER_PAGE = 10.0 * 60.0 * 1000.0
private const val MAX_ESTIMATE_MILLIS = 30L * 24L * 60L * 60L * 1000L
