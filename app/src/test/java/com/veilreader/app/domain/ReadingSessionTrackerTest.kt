package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingSessionTrackerTest {
    @Test
    fun activeTime_excludesPausedTime_andCapsAtIdleTimeout() {
        val tracker = ReadingSessionTracker(
            sessionId = "session",
            bookId = "book",
            startedAtEpochMs = 1_000L,
            startedAtElapsedMs = 0L,
            idleTimeoutMs = 5 * 60_000L
        )

        tracker.onResume(0L)
        assertEquals(60_000L, tracker.tick(60_000L))
        // No interaction after resume: only the first five minutes are engaged reading.
        assertEquals(240_000L, tracker.tick(10 * 60_000L))
        assertEquals(300_000L, tracker.activeMillis)

        tracker.onPause(11 * 60_000L)
        tracker.onResume(20 * 60_000L)
        assertEquals(30_000L, tracker.tick(20 * 60_000L + 30_000L))
        assertEquals(330_000L, tracker.activeMillis)
    }

    @Test
    fun interaction_restartsIdleWindow_withoutCountingIdleGap() {
        val tracker = ReadingSessionTracker("s", "b", 0L, 0L, idleTimeoutMs = 60_000L)
        tracker.onResume(0L)
        tracker.tick(120_000L)
        assertEquals(60_000L, tracker.activeMillis)

        tracker.onInteraction(180_000L)
        tracker.tick(210_000L)
        assertEquals(90_000L, tracker.activeMillis)
    }

    @Test
    fun snapshot_countsPacedPagesHighlightsAndUniqueNotes() {
        val tracker = ReadingSessionTracker("s", "b", 10L, 0L)
        tracker.recordPacedPageTurn()
        tracker.recordPacedPageTurn()
        tracker.recordHighlight()
        tracker.recordNote("h1", "first note")
        tracker.recordNote("h1", "edited note")
        tracker.recordNote("h2", "   ")
        tracker.recordNote("h2", "second note")

        val snapshot = tracker.snapshot(99L)
        assertEquals(2, snapshot.pacedPageTurns)
        assertEquals(1, snapshot.highlightCount)
        assertEquals(2, snapshot.noteCount)
        assertEquals(99L, snapshot.endedAtEpochMs)
    }
}
