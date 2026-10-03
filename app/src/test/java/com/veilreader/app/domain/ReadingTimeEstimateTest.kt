package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingTimeEstimateTest {
    @Test
    fun `pace stays hidden until enough evidence exists`() {
        val sessions = listOf(
            session(turns = 3, activeMillis = 90_000L)
        )

        assertNull(deriveReadingPace(sessions, BOOK))
    }

    @Test
    fun `pace uses only sessions with meaningful page-turn evidence`() {
        val sessions = listOf(
            session(turns = 6, activeMillis = 180_000L),
            session(turns = 4, activeMillis = 120_000L),
            session(turns = 0, activeMillis = 900_000L),
            session(turns = 2, activeMillis = 600_000L)
        )

        val pace = requireNotNull(deriveReadingPace(sessions, BOOK))
        assertEquals(10, pace.observedPageTurns)
        assertEquals(300_000L, pace.observedActiveMillis)
        assertEquals(30_000.0, pace.millisecondsPerPage, 0.001)
        assertEquals(ReadingPaceConfidence.LEARNING, pace.confidence)
    }

    @Test
    fun `sessions from another book never alter the estimate`() {
        val sessions = listOf(
            session(turns = 10, activeMillis = 300_000L),
            session(
                turns = 100,
                activeMillis = 100 * 120_000L,
                bookId = "other"
            )
        )

        val pace = requireNotNull(deriveReadingPace(sessions, BOOK))
        assertEquals(30_000.0, pace.millisecondsPerPage, 0.001)
    }

    @Test
    fun `implausible or corrupt pace samples are rejected`() {
        val tooFast = session(turns = 20, activeMillis = 20_000L)
        val tooSlow = session(
            turns = 3,
            activeMillis = 3L * 20L * 60L * 1000L
        )

        assertNull(deriveReadingPace(listOf(tooFast, tooSlow), BOOK))
    }

    @Test
    fun `book eta requires publication page count`() {
        val pace = ReadingPaceEstimate(
            millisecondsPerPage = 60_000.0,
            observedPageTurns = 20,
            observedActiveMillis = 20L * 60L * 1000L,
            confidence = ReadingPaceConfidence.ESTABLISHED
        )

        assertNull(estimateBookTimeRemaining(0, 0.5f, pace))
    }

    @Test
    fun `eta uses remaining publication pages and exposes uncertainty`() {
        val pace = ReadingPaceEstimate(
            millisecondsPerPage = 60_000.0,
            observedPageTurns = 50,
            observedActiveMillis = 50L * 60L * 1000L,
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
            observedPageTurns = 20,
            observedActiveMillis = 20L * 60L * 1000L,
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

    private fun session(
        turns: Int,
        activeMillis: Long,
        bookId: String = BOOK
    ): ReadingSessionSnapshot =
        ReadingSessionSnapshot(
            id = "$bookId-$turns-$activeMillis",
            bookId = bookId,
            startedAtEpochMs = 1L,
            endedAtEpochMs = 2L,
            activeMillis = activeMillis,
            pacedPageTurns = turns,
            highlightCount = 0,
            noteCount = 0
        )

    private companion object {
        const val BOOK = "book"
    }
}
