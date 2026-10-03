package com.veilreader.app.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt

enum class ReadingPaceConfidence {
    LEARNING,
    ESTABLISHED,
    STRONG
}

/**
 * Derived analytics cache for one book. It is deliberately not primary reading history.
 *
 * [m2MillisSquared] is Welford's running sum of squared deviations, allowing uncertainty to
 * survive process death without retaining raw page-turn timestamps.
 */
data class ReadingPaceProfile(
    val sampleCount: Int = 0,
    val meanMillisPerPage: Double = 0.0,
    val m2MillisSquared: Double = 0.0,
    val totalObservedMillis: Long = 0L
) {
    fun normalized(): ReadingPaceProfile =
        copy(
            sampleCount = sampleCount.coerceAtLeast(0),
            meanMillisPerPage = meanMillisPerPage
                .takeIf { it.isFinite() && it >= 0.0 }
                ?: 0.0,
            m2MillisSquared = m2MillisSquared
                .takeIf { it.isFinite() && it >= 0.0 }
                ?: 0.0,
            totalObservedMillis = totalObservedMillis.coerceAtLeast(0L)
        )
}

data class ReadingPaceEstimate(
    val millisecondsPerPage: Double,
    val observedIntervals: Int,
    val observedActiveMillis: Long,
    val standardDeviationMillis: Double,
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
 * Adds one active-reading interval between two real page turns.
 *
 * Initial broad bounds reject impossible/corrupt samples. Once a personal baseline exists,
 * an additional very broad adaptive guard prevents a single accidental pause from destroying
 * the profile while still allowing genuine pace changes.
 */
fun recordReadingPaceInterval(
    profile: ReadingPaceProfile,
    intervalMillis: Long
): ReadingPaceProfile {
    val current = profile.normalized()
    if (intervalMillis !in MIN_INTERVAL_MILLIS..MAX_INTERVAL_MILLIS) {
        return current
    }

    val sample = intervalMillis.toDouble()
    if (
        current.sampleCount >= ADAPTIVE_GUARD_MIN_SAMPLES &&
        current.meanMillisPerPage > 0.0
    ) {
        val lower = current.meanMillisPerPage * ADAPTIVE_LOWER_FACTOR
        val upper = current.meanMillisPerPage * ADAPTIVE_UPPER_FACTOR
        if (sample !in lower..upper) return current
    }

    val nextCount = current.sampleCount + 1
    val delta = sample - current.meanMillisPerPage
    val nextMean = current.meanMillisPerPage + delta / nextCount.toDouble()
    val delta2 = sample - nextMean
    val nextM2 = current.m2MillisSquared + delta * delta2

    return ReadingPaceProfile(
        sampleCount = nextCount,
        meanMillisPerPage = nextMean,
        m2MillisSquared = nextM2.coerceAtLeast(0.0),
        totalObservedMillis = safeAddMillis(
            current.totalObservedMillis,
            intervalMillis
        )
    )
}

fun deriveReadingPace(
    profile: ReadingPaceProfile?
): ReadingPaceEstimate? {
    val safe = profile?.normalized() ?: return null
    if (
        safe.sampleCount < MIN_TOTAL_INTERVALS ||
        safe.totalObservedMillis < MIN_TOTAL_ACTIVE_MILLIS ||
        safe.meanMillisPerPage !in
            MIN_INTERVAL_MILLIS.toDouble()..MAX_INTERVAL_MILLIS.toDouble()
    ) {
        return null
    }

    val variance = if (safe.sampleCount > 1) {
        safe.m2MillisSquared / (safe.sampleCount - 1).toDouble()
    } else {
        0.0
    }
    val standardDeviation = sqrt(variance.coerceAtLeast(0.0))

    val confidence = when {
        safe.sampleCount >= STRONG_INTERVALS &&
            safe.totalObservedMillis >= STRONG_ACTIVE_MILLIS ->
            ReadingPaceConfidence.STRONG

        safe.sampleCount >= ESTABLISHED_INTERVALS &&
            safe.totalObservedMillis >= ESTABLISHED_ACTIVE_MILLIS ->
            ReadingPaceConfidence.ESTABLISHED

        else ->
            ReadingPaceConfidence.LEARNING
    }

    return ReadingPaceEstimate(
        millisecondsPerPage = safe.meanMillisPerPage,
        observedIntervals = safe.sampleCount,
        observedActiveMillis = safe.totalObservedMillis,
        standardDeviationMillis = standardDeviation,
        confidence = confidence
    )
}

/**
 * Estimates remaining book time only when the publication supplies a page count and a personal
 * pace has enough evidence. The range widens with measured personal variance and sparse evidence.
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

    val measuredCoefficient =
        if (safePace.millisecondsPerPage > 0.0) {
            (safePace.standardDeviationMillis /
                safePace.millisecondsPerPage).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    val minimumSpread = when (safePace.confidence) {
        ReadingPaceConfidence.LEARNING -> 0.35
        ReadingPaceConfidence.ESTABLISHED -> 0.25
        ReadingPaceConfidence.STRONG -> 0.18
    }
    val spread = max(
        minimumSpread,
        measuredCoefficient * VARIANCE_SPREAD_FACTOR
    ).coerceAtMost(MAX_UNCERTAINTY_SPREAD)

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

private fun safeAddMillis(current: Long, delta: Long): Long =
    if (delta <= 0L) {
        current.coerceAtLeast(0L)
    } else if (Long.MAX_VALUE - current.coerceAtLeast(0L) < delta) {
        Long.MAX_VALUE
    } else {
        current.coerceAtLeast(0L) + delta
    }

private fun safeDurationMillis(value: Double): Long =
    value
        .takeIf { it.isFinite() && it >= 0.0 }
        ?.coerceAtMost(MAX_ESTIMATE_MILLIS.toDouble())
        ?.roundToLong()
        ?: 0L

private const val MIN_INTERVAL_MILLIS = 5_000L
private const val MAX_INTERVAL_MILLIS = 6L * 60L * 1000L
private const val ADAPTIVE_GUARD_MIN_SAMPLES = 8
private const val ADAPTIVE_LOWER_FACTOR = 0.20
private const val ADAPTIVE_UPPER_FACTOR = 5.00

private const val MIN_TOTAL_INTERVALS = 8
private const val MIN_TOTAL_ACTIVE_MILLIS = 3L * 60L * 1000L
private const val ESTABLISHED_INTERVALS = 20
private const val ESTABLISHED_ACTIVE_MILLIS = 8L * 60L * 1000L
private const val STRONG_INTERVALS = 50
private const val STRONG_ACTIVE_MILLIS = 20L * 60L * 1000L

private const val VARIANCE_SPREAD_FACTOR = 0.60
private const val MAX_UNCERTAINTY_SPREAD = 0.60
private const val MAX_ESTIMATE_MILLIS = 30L * 24L * 60L * 60L * 1000L

/** Calibrate against Readium publication positions, never viewport pages that change with fonts. */
internal fun normalizedReadingPaceInterval(
    intervalMillis: Long,
    previousProgression: Double?,
    progression: Double,
    totalPositions: Int
): Long? {
    val previous = previousProgression ?: return null
    if (intervalMillis <= 0L || totalPositions <= 0 || !previous.isFinite() || !progression.isFinite()) return null
    if (previous !in 0.0..1.0 || progression !in 0.0..1.0 || progression <= previous) return null
    val positionsRead = (progression - previous) * totalPositions
    val perPosition = intervalMillis.toDouble() / positionsRead
    return perPosition.takeIf { it.isFinite() && it in MIN_INTERVAL_MILLIS.toDouble()..MAX_INTERVAL_MILLIS.toDouble() }
        ?.roundToLong()
}
