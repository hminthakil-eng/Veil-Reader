package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingMemoryTest {
    private val hour = 60L * 60L * 1000L
    private val day = 24L * hour

    @Test
    fun `continuity aggregates only sessions belonging to the book`() {
        val book = Book(
            id = "book",
            title = "Veil",
            author = "Archive",
            progress = 0.41f,
            lastOpenedAtEpochMs = 10L * day
        )
        val summary = deriveReadingContinuity(
            book = book,
            sessions = listOf(
                ReadingSessionSnapshot("a", "book", 2L * day, 2L * day + hour, hour, 12, 2, 1),
                ReadingSessionSnapshot("b", "book", 4L * day, 4L * day + 2L * hour, 2L * hour, 18, 1, 2),
                ReadingSessionSnapshot("other", "other", 8L * day, 9L * day, day, 999, 99, 99)
            ),
            nowEpochMs = 12L * day
        )

        assertEquals(2, summary.priorSessionCount)
        assertEquals(3L * hour, summary.totalActiveMillis)
        assertEquals(30, summary.pacedPageTurns)
        assertEquals(3, summary.recordedHighlightEvents)
        assertEquals(3, summary.recordedNoteEvents)
        assertEquals(2L * day, summary.returnGapMillis)
        assertTrue(summary.hasHistory)
    }

    @Test
    fun `future timestamps never create negative return gaps`() {
        val summary = deriveReadingContinuity(
            book = Book(
                id = "future",
                title = "Clock",
                author = "Veil",
                lastOpenedAtEpochMs = 50L * day
            ),
            sessions = emptyList(),
            nowEpochMs = 40L * day
        )

        assertNull(summary.returnGapMillis)
        assertTrue(summary.hasHistory)
    }

    @Test
    fun `untouched books have no invented continuity`() {
        val summary = deriveReadingContinuity(
            book = Book(id = "new", title = "New", author = "Veil"),
            sessions = emptyList(),
            nowEpochMs = 10L * day
        )

        assertFalse(summary.hasHistory)
        assertEquals(0, summary.priorSessionCount)
        assertEquals(0L, summary.totalActiveMillis)
    }

    @Test
    fun `time capsule seed is built only from real durable history inputs`() {
        val book = Book(
            id = "done",
            title = "Closed Circle",
            author = "Veil",
            progress = 1f,
            finished = true,
            lastOpenedAtEpochMs = 20L * day
        )
        val summary = deriveReadingContinuity(
            book = book,
            sessions = listOf(
                ReadingSessionSnapshot("one", "done", 10L * day, 10L * day + hour, hour, 22, 4, 2)
            ),
            nowEpochMs = 21L * day
        )
        val seed = buildTimeCapsuleSeed(book, summary)

        assertEquals("done", seed.bookId)
        assertEquals(1, seed.sessionCount)
        assertEquals(hour, seed.totalActiveMillis)
        assertTrue(seed.completed)
    }
}
