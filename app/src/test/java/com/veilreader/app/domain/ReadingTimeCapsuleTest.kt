package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingTimeCapsuleTest {
    private val day = 86_400_000L

    @Test
    fun `only completed books form sealed capsules`() {
        val capsules = deriveReadingTimeCapsules(
            books = listOf(
                Book(id = "open", title = "Open", author = "Veil", progress = 0.7f),
                Book(id = "done", title = "Done", author = "Veil", progress = 1f, finished = true)
            ),
            sessions = emptyList(),
            highlights = emptyList(),
            bookmarks = emptyList()
        )

        assertEquals(listOf("done"), capsules.map { it.book.id })
    }

    @Test
    fun `capsule timeline uses only durable dated events and stays chronological`() {
        val book = Book(
            id = "done",
            title = "Done",
            author = "Veil",
            progress = 1f,
            finished = true,
            addedAtEpochMs = 2L * day,
            lastOpenedAtEpochMs = 9L * day
        )
        val capsule = deriveReadingTimeCapsule(
            book = book,
            sessions = listOf(
                ReadingSessionSnapshot(
                    id = "s1",
                    bookId = "done",
                    startedAtEpochMs = 4L * day,
                    endedAtEpochMs = 4L * day + 30_000L,
                    activeMillis = 30_000L,
                    pacedPageTurns = 6,
                    highlightCount = 1,
                    noteCount = 0
                )
            ),
            highlights = listOf(
                Highlight(
                    id = "h1",
                    bookId = "done",
                    quote = "A preserved line",
                    locatorJson = "{}",
                    note = "Margin note",
                    createdAtEpochMs = 5L * day
                )
            ),
            bookmarks = listOf(
                Bookmark(
                    id = "b1",
                    bookId = "done",
                    label = "Return",
                    locatorJson = "{}",
                    createdAtEpochMs = 6L * day
                )
            )
        )

        assertEquals(
            capsule.timeline.map { it.timestampEpochMs }.sorted(),
            capsule.timeline.map { it.timestampEpochMs }
        )
        assertEquals(1, capsule.sessionCount)
        assertEquals(1, capsule.highlightCount)
        assertEquals(1, capsule.noteCount)
        assertEquals(1, capsule.bookmarkCount)
        assertEquals(9L * day, capsule.latestRecordedAtEpochMs)
    }

    @Test
    fun `capsule never claims an exact completion timestamp that is not stored`() {
        val capsule = deriveReadingTimeCapsule(
            book = Book(
                id = "done",
                title = "Done",
                author = "Veil",
                progress = 1f,
                finished = true,
                lastOpenedAtEpochMs = 12L * day
            ),
            sessions = emptyList(),
            highlights = emptyList(),
            bookmarks = emptyList()
        )

        assertFalse(capsule.exactCompletionTimeKnown)
        assertTrue(
            capsule.timeline.none {
                it.title.contains("completed", ignoreCase = true)
            }
        )
    }

    @Test
    fun `persisted cycle wins over later mutable book history`() {
        val book = Book(
            id = "done",
            title = "Renamed Later",
            author = "New Metadata",
            finished = true
        )
        val sealed = ReadingCycleRecord(
            id = "cycle",
            bookId = "done",
            cycleIndex = 1,
            titleSnapshot = "Original Title",
            authorSnapshot = "Original Author",
            startedAtEpochMs = 2L * day,
            completedAtEpochMs = 8L * day,
            finalLocatorJson = "{\"end\":true}",
            sessionCount = 2,
            totalActiveMillis = 90_000L,
            pacedPageTurns = 42,
            highlightCount = 3,
            noteCount = 1,
            bookmarkCount = 2,
            sealCode = "VR-SEALED01",
            timeline = listOf(
                ReadingHistoryEvent(
                    id = "completed",
                    kind = ReadingHistoryEventKind.COMPLETED,
                    timestampEpochMs = 8L * day,
                    title = "Reading cycle completed"
                )
            )
        )

        val capsule = deriveReadingTimeCapsules(
            books = listOf(book),
            sessions = listOf(
                ReadingSessionSnapshot("late", "done", 20L * day, 21L * day, day, 999, 9, 9)
            ),
            highlights = listOf(
                Highlight("late-h", "done", "Later", "{}", createdAtEpochMs = 22L * day)
            ),
            bookmarks = emptyList(),
            sealedCycles = listOf(sealed)
        ).single()

        assertTrue(capsule.exactCompletionTimeKnown)
        assertEquals(8L * day, capsule.completedAtEpochMs)
        assertEquals("Original Title", capsule.book.title)
        assertEquals("Original Author", capsule.book.author)
        assertEquals(2, capsule.sessionCount)
        assertEquals(3, capsule.highlightCount)
        assertEquals("VR-SEALED01", capsule.sealCode)
    }

    @Test
    fun `seal code is stable for the same durable record`() {
        val first = readingCapsuleSealCode("book", 10L, 20L)
        val second = readingCapsuleSealCode("book", 10L, 20L)
        val changed = readingCapsuleSealCode("book", 10L, 21L)

        assertEquals(first, second)
        assertTrue(first.startsWith("VR-"))
        assertTrue(first != changed)
    }
    @Test
    fun `annotated preserved passage carries structured presentation evidence`() {
        val book = Book(
            id = "annotated",
            title = "Annotated",
            author = "Veil",
            finished = true
        )
        val highlight = Highlight(
            id = "h",
            bookId = book.id,
            quote = "A preserved sentence",
            locatorJson = "{}",
            note = "A note",
            createdAtEpochMs = 10L
        )

        val event = deriveReadingTimeCapsule(
            book = book,
            sessions = emptyList(),
            highlights = listOf(highlight),
            bookmarks = emptyList()
        ).timeline.single { it.kind == ReadingHistoryEventKind.PASSAGE_PRESERVED }

        assertTrue(event.annotated)
    }

}
