package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalMemoryTest {
    private val day = 86_400_000L

    @Test
    fun `sealed cycle freezes only history that existed at completion`() {
        val completedAt = 10L * day
        val book = Book(
            id = "book",
            title = "The First Name",
            author = "Archivist",
            finished = true,
            addedAtEpochMs = day
        )
        val cycle = buildSealedReadingCycle(
            book = book,
            cycleIndex = 1,
            sessions = listOf(
                ReadingSessionSnapshot("s1", "book", 2L * day, 3L * day, day, 20, 1, 0),
                ReadingSessionSnapshot("late", "book", 11L * day, 12L * day, day, 200, 9, 9)
            ),
            highlights = listOf(
                Highlight("h1", "book", "Before", "{}", createdAtEpochMs = 4L * day),
                Highlight("late-h", "book", "After", "{}", createdAtEpochMs = 12L * day)
            ),
            bookmarks = listOf(
                Bookmark("b1", "book", "Before", "{}", 5L * day),
                Bookmark("late-b", "book", "After", "{}", 13L * day)
            ),
            completedAtEpochMs = completedAt,
            finalLocatorJson = "{\"progress\":1}"
        )

        assertEquals(1, cycle.sessionCount)
        assertEquals(1, cycle.highlightCount)
        assertEquals(1, cycle.bookmarkCount)
        assertEquals(day, cycle.totalActiveMillis)
        assertEquals(completedAt, cycle.completedAtEpochMs)
        assertTrue(cycle.timeline.any { it.kind == ReadingHistoryEventKind.COMPLETED })
        assertTrue(cycle.timeline.all { it.timestampEpochMs <= completedAt })
    }

    @Test
    fun `progress milestones preserve the first factual threshold crossing`() {
        val crossed = crossedReadingMilestones(
            bookId = "book",
            previousProgress = 0.18f,
            newProgress = 0.76f,
            reachedAtEpochMs = 42L,
            locatorJson = "{\"href\":\"chapter.xhtml\"}"
        )

        assertEquals(
            listOf(
                ReadingMilestoneKind.PROGRESS_25,
                ReadingMilestoneKind.PROGRESS_50,
                ReadingMilestoneKind.PROGRESS_75
            ),
            crossed.map { it.kind }
        )
        assertTrue(crossed.all { it.reachedAtEpochMs == 42L })
        assertTrue(crossed.all { it.id.startsWith("milestone:book:") })
    }

    @Test
    fun `backward reading motion never fabricates a milestone`() {
        assertTrue(
            crossedReadingMilestones(
                bookId = "book",
                previousProgress = 0.80f,
                newProgress = 0.40f,
                reachedAtEpochMs = 50L,
                locatorJson = "{}"
            ).isEmpty()
        )
    }

    @Test
    fun `passage revisits require the exact preserved object and occur after marking`() {
        val highlight = Highlight(
            id = "h",
            bookId = "book",
            quote = "Preserved",
            locatorJson = "{}",
            createdAtEpochMs = 5L * day
        )
        val visits = listOf(
            PassageVisit("wrong", "other", "book", "{}", 8L * day),
            PassageVisit("early", "h", "book", "{}", 4L * day),
            PassageVisit("second", "h", "book", "{}", 9L * day),
            PassageVisit("first", "h", "book", "{}", 7L * day),
            PassageVisit("other-book", "h", "other", "{}", 10L * day)
        )

        val exact = exactPassageVisits(highlight, visits)

        assertEquals(listOf("first", "second"), exact.map { it.id })
        assertEquals(listOf(7L * day, 9L * day), exact.map { it.viewedAtEpochMs })
    }
}
