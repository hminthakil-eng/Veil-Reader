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
    fun pagePaceInterval_usesEngagedActiveTime_betweenRealTurns() {
        val tracker = ReadingSessionTracker(
            sessionId = "pace",
            bookId = "book",
            startedAtEpochMs = 0L,
            startedAtElapsedMs = 0L
        )
        tracker.onResume(0L)
        tracker.tick(30_000L)

        assertEquals(null, tracker.recordPacedPageTurn())

        tracker.tick(75_000L)
        assertEquals(45_000L, tracker.recordPacedPageTurn())
    }

    @Test
    fun pagePaceInterval_resetsAcrossPauseAndResume() {
        val tracker = ReadingSessionTracker(
            sessionId = "pace-reset",
            bookId = "book",
            startedAtEpochMs = 0L,
            startedAtElapsedMs = 0L
        )
        tracker.onResume(0L)
        tracker.tick(20_000L)
        assertEquals(null, tracker.recordPacedPageTurn())

        tracker.onPause(30_000L)
        tracker.onResume(300_000L)
        tracker.tick(330_000L)

        // The first turn after resume establishes a new anchor rather than
        // including the background gap or the previous foreground segment.
        assertEquals(null, tracker.recordPacedPageTurn())
        tracker.tick(360_000L)
        assertEquals(30_000L, tracker.recordPacedPageTurn())
    }

    @Test
    fun restore_preservesDurableCounters_andDoesNotRecountKnownNotes() {
        val restored = requireNotNull(
            ReadingSessionTracker.restore(
                snapshot = ReadingSessionSnapshot(
                    id = "session-restored",
                    bookId = "book",
                    startedAtEpochMs = 1_000L,
                    endedAtEpochMs = 9_000L,
                    activeMillis = 95_000L,
                    pacedPageTurns = 7,
                    highlightCount = 3,
                    noteCount = 2
                ),
                bookId = "book",
                startedAtElapsedMs = 50_000L,
                notedHighlightIds = setOf("note-existing")
            )
        )

        restored.onResume(50_000L)
        restored.tick(80_000L)
        restored.recordPacedPageTurn()
        restored.recordHighlight()
        restored.recordNote("note-existing", "edited after recreation")
        restored.recordNote("note-new", "new note")

        val snapshot = restored.snapshot(10_000L)
        assertEquals("session-restored", snapshot.id)
        assertEquals(1_000L, snapshot.startedAtEpochMs)
        assertEquals(125_000L, snapshot.activeMillis)
        assertEquals(8, snapshot.pacedPageTurns)
        assertEquals(4, snapshot.highlightCount)
        assertEquals(3, snapshot.noteCount)
    }

    @Test
    fun restore_rejectsSnapshotOwnedByAnotherBook() {
        val restored = ReadingSessionTracker.restore(
            snapshot = ReadingSessionSnapshot(
                id = "session",
                bookId = "other-book",
                startedAtEpochMs = 1L,
                endedAtEpochMs = 2L,
                activeMillis = 0L,
                pacedPageTurns = 0,
                highlightCount = 0,
                noteCount = 0
            ),
            bookId = "book",
            startedAtElapsedMs = 0L
        )

        assertEquals(null, restored)
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
