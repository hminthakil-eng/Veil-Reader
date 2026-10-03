package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingTimeEstimateTest {
    @Test
    fun paceCalibration_usesPublicationPositionsAcrossViewportSizes() {
        assertEquals(60_000L, normalizedReadingPaceInterval(60_000L, 0.20, 0.21, 100))
        assertEquals(60_000L, normalizedReadingPaceInterval(120_000L, 0.20, 0.22, 100))
        assertEquals(null, normalizedReadingPaceInterval(60_000L, 0.21, 0.20, 100))
        assertEquals(null, normalizedReadingPaceInterval(60_000L, 0.20, 0.20, 100))
        assertEquals(null, normalizedReadingPaceInterval(60_000L, null, 0.20, 100))
        assertEquals(null, normalizedReadingPaceInterval(60_000L, 0.20, Double.NaN, 100))
        assertEquals(null, normalizedReadingPaceInterval(60_000L, 0.20, 0.21, 0))
    }

    @Test
    fun `invalid intervals do not mutate the pace profile`() {
        val profile = ReadingPaceProfile()
        assertEquals(profile, recordReadingPaceInterval(profile, 2_000L))
        assertEquals(
            profile,
            recordReadingPaceInterval(
                profile,
                10L * 60L * 1000L
            )
        )
    }

    @Test
    fun `pace stays hidden until enough interval evidence exists`() {
        var profile = ReadingPaceProfile()
        repeat(7) {
            profile = recordReadingPaceInterval(profile, 30_000L)
        }
        assertNull(deriveReadingPace(profile))
    }

    @Test
    fun `valid active page intervals learn a personal pace`() {
        var profile = ReadingPaceProfile()
        repeat(10) {
            profile = recordReadingPaceInterval(profile, 30_000L)
        }

        val pace = requireNotNull(deriveReadingPace(profile))
        assertEquals(10, pace.observedIntervals)
        assertEquals(300_000L, pace.observedActiveMillis)
        assertEquals(30_000.0, pace.millisecondsPerPage, 0.001)
        assertEquals(0.0, pace.standardDeviationMillis, 0.001)
        assertEquals(ReadingPaceConfidence.LEARNING, pace.confidence)
    }

    @Test
    fun `running variance survives non-uniform samples`() {
        var profile = ReadingPaceProfile()
        listOf(
            20_000L, 25_000L, 30_000L, 35_000L,
            40_000L, 20_000L, 25_000L, 30_000L,
            35_000L, 40_000L
        ).forEach {
            profile = recordReadingPaceInterval(profile, it)
        }

        val pace = requireNotNull(deriveReadingPace(profile))
        assertEquals(30_000.0, pace.millisecondsPerPage, 0.001)
        assertTrue(pace.standardDeviationMillis > 0.0)
    }

    @Test
    fun `adaptive guard rejects a later catastrophic pause sample`() {
        var profile = ReadingPaceProfile()
        repeat(10) {
            profile = recordReadingPaceInterval(profile, 30_000L)
        }
        val before = profile
        val after = recordReadingPaceInterval(
            profile,
            5L * 60L * 1000L
        )

        assertEquals(before, after)
    }

    @Test
    fun `book eta requires publication page count`() {
        val pace = ReadingPaceEstimate(
            millisecondsPerPage = 60_000.0,
            observedIntervals = 20,
            observedActiveMillis = 20L * 60L * 1000L,
            standardDeviationMillis = 5_000.0,
            confidence = ReadingPaceConfidence.ESTABLISHED
        )

        assertNull(estimateBookTimeRemaining(0, 0.5f, pace))
    }

    @Test
    fun `eta uses remaining publication pages and measured uncertainty`() {
        val pace = ReadingPaceEstimate(
            millisecondsPerPage = 60_000.0,
            observedIntervals = 50,
            observedActiveMillis = 50L * 60L * 1000L,
            standardDeviationMillis = 20_000.0,
            confidence = ReadingPaceConfidence.STRONG
        )

        val estimate = requireNotNull(
            estimateBookTimeRemaining(
                totalPages = 400,
                progress = 0.75f,
                pace = pace
            )
        )

        assertEquals(100, estimate.remainingPages)
        assertEquals(100L * 60_000L, estimate.centerMillis)
        assertTrue(estimate.lowMillis < estimate.centerMillis)
        assertTrue(estimate.highMillis > estimate.centerMillis)
    }

    @Test
    fun `finished book reports zero remaining without negative duration`() {
        val pace = ReadingPaceEstimate(
            millisecondsPerPage = 60_000.0,
            observedIntervals = 20,
            observedActiveMillis = 20L * 60L * 1000L,
            standardDeviationMillis = 5_000.0,
            confidence = ReadingPaceConfidence.ESTABLISHED
        )

        val estimate = requireNotNull(
            estimateBookTimeRemaining(
                totalPages = 300,
                progress = 1f,
                pace = pace
            )
        )
        assertEquals(0, estimate.remainingPages)
        assertEquals(0L, estimate.centerMillis)
        assertEquals(0L, estimate.lowMillis)
        assertEquals(0L, estimate.highMillis)
    }
}
