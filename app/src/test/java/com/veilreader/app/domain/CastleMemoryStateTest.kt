package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CastleMemoryStateTest {
    private val hour = 3_600_000L

    @Test
    fun `empty archive leaves the keep mostly silent`() {
        val state = deriveCastleMemoryState(
            books = emptyList(),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList()
        )

        assertEquals(CastleMemoryState.EMPTY, state)
        assertEquals(1, state.litWindows)
        assertEquals(0, state.starPoints)
        assertEquals(0f, state.resonanceFor("observatory"), 0.0001f)
    }

    @Test
    fun `real archive history changes environmental density without changing unlocks`() {
        val books = (1..18).map { index ->
            Book(
                id = "b$index",
                title = "Volume $index",
                author = if (index <= 3) "Shared Author" else "Author $index",
                progress = if (index <= 9) 1f else 0.45f,
                finished = index <= 9,
                favorite = index <= 4,
                collections = if (index <= 6) listOf("Mystery") else emptyList()
            )
        }
        val highlights = (1..45).map { index ->
            Highlight(
                id = "h$index",
                bookId = "b${(index % 6) + 1}",
                quote = "threshold archive lantern memory chamber $index",
                locatorJson = "{}",
                note = if (index % 3 == 0) "note $index" else ""
            )
        }
        val sessions = (1..30).map { index ->
            ReadingSessionSnapshot(
                id = "s$index",
                bookId = "b${(index % 10) + 1}",
                startedAtEpochMs = index.toLong(),
                endedAtEpochMs = index.toLong() + hour,
                activeMillis = hour,
                pacedPageTurns = 10,
                highlightCount = 1,
                noteCount = 0
            )
        }

        val state = deriveCastleMemoryState(
            books = books,
            highlights = highlights,
            bookmarks = emptyList(),
            sessions = sessions
        )

        assertTrue(state.overallPresence > 0.25f)
        assertTrue(state.litWindows > 1)
        assertTrue(state.shelfRibs > 3)
        assertTrue(state.starPoints > 0)
        assertTrue(state.fogAlpha < CastleMemoryState.EMPTY.fogAlpha)
        assertTrue(state.resonanceFor("archive") > 0f)
        assertTrue(state.resonanceFor("library") > 0f)
    }

    @Test
    fun `observatory resonance needs recorded atlas structure`() {
        val isolated = deriveCastleMemoryState(
            books = listOf(
                Book(id = "a", title = "A", author = "One"),
                Book(id = "b", title = "B", author = "Two")
            ),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList()
        )
        val connected = deriveCastleMemoryState(
            books = listOf(
                Book(id = "a", title = "A", author = "Same"),
                Book(id = "b", title = "B", author = "Same")
            ),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList()
        )

        assertTrue(connected.atlasLinkCount > isolated.atlasLinkCount)
        assertTrue(connected.observatoryResonance > isolated.observatoryResonance)
    }

    @Test
    fun `sealed capsules come only from completed volumes`() {
        val state = deriveCastleMemoryState(
            books = listOf(
                Book(id = "open", title = "Open", author = "Veil", progress = 0.9f),
                Book(id = "done", title = "Done", author = "Veil", progress = 1f, finished = true)
            ),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList()
        )

        assertEquals(1, state.sealedCapsuleCount)
        assertTrue(state.resonanceFor("treasury") > 0f)
    }

    @Test
    fun `long silence cools the keep without erasing memory`() {
        val day = 86_400_000L
        val state = deriveCastleMemoryState(
            books = listOf(
                Book(
                    id = "quiet",
                    title = "Quiet Volume",
                    author = "Veil",
                    addedAtEpochMs = day,
                    lastOpenedAtEpochMs = day * 5
                )
            ),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = listOf(
                ReadingSessionSnapshot(
                    id = "old-session",
                    bookId = "quiet",
                    startedAtEpochMs = day * 5,
                    endedAtEpochMs = day * 5 + 30_000L,
                    activeMillis = 30_000L,
                    pacedPageTurns = 1,
                    highlightCount = 0,
                    noteCount = 0
                )
            ),
            nowEpochMs = day * 100
        )

        assertTrue(state.longSilence > 0.70f)
        assertEquals(0f, state.returnAwakening, 0.0001f)
        assertTrue(state.mutationInscription.contains("cold"))
        assertTrue(state.volumeCount == 1)
    }

    @Test
    fun `return after a long gap wakes the foundation`() {
        val day = 86_400_000L
        val sessions = listOf(
            ReadingSessionSnapshot(
                id = "before-silence",
                bookId = "returning",
                startedAtEpochMs = day,
                endedAtEpochMs = day + 60_000L,
                activeMillis = 60_000L,
                pacedPageTurns = 2,
                highlightCount = 0,
                noteCount = 0
            ),
            ReadingSessionSnapshot(
                id = "return-session",
                bookId = "returning",
                startedAtEpochMs = day * 60,
                endedAtEpochMs = day * 60 + 60_000L,
                activeMillis = 60_000L,
                pacedPageTurns = 2,
                highlightCount = 0,
                noteCount = 0
            )
        )

        val state = deriveCastleMemoryState(
            books = listOf(
                Book(
                    id = "returning",
                    title = "Returning Volume",
                    author = "Veil",
                    addedAtEpochMs = day,
                    lastOpenedAtEpochMs = day * 60
                )
            ),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = sessions,
            nowEpochMs = day * 61
        )

        assertTrue(state.returnAwakening > 0.35f)
        assertTrue(state.mutationInscription.startsWith("After a long quiet"))
        assertTrue(state.litWindows >= 1)
    }

    @Test
    fun `rereads leave patina and visible rings without unlocking rooms`() {
        val day = 86_400_000L
        val book = Book(
            id = "reread",
            title = "Reread Volume",
            author = "Veil",
            progress = 1f,
            finished = true,
            addedAtEpochMs = day,
            lastOpenedAtEpochMs = day * 220
        )
        val cycles = listOf(
            sealedCycle(book, cycleIndex = 1, completedAt = day * 40),
            sealedCycle(book, cycleIndex = 2, completedAt = day * 100),
            sealedCycle(book, cycleIndex = 3, completedAt = day * 160),
            sealedCycle(book, cycleIndex = 4, completedAt = day * 220)
        )

        val state = deriveCastleMemoryState(
            books = listOf(book),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList(),
            readingCycles = cycles,
            nowEpochMs = day * 230
        )

        assertEquals(3, state.rereadCycleCount)
        assertEquals(3, state.rereadRings)
        assertTrue(state.patina > 0.20f)
        assertTrue(state.mutationInscription.contains("Repeated journeys"))
    }

    @Test
    fun `annotated reading lights the scriptorium`() {
        val day = 86_400_000L
        val book = Book(
            id = "notes",
            title = "Marginalia",
            author = "Veil",
            addedAtEpochMs = day,
            lastOpenedAtEpochMs = day * 20
        )
        val highlights = (1..16).map { index ->
            Highlight(
                id = "note-$index",
                bookId = book.id,
                quote = "Preserved passage $index",
                locatorJson = "{}",
                note = "Annotation $index",
                createdAtEpochMs = day * 10 + index
            )
        }

        val state = deriveCastleMemoryState(
            books = listOf(book),
            highlights = highlights,
            bookmarks = emptyList(),
            sessions = emptyList(),
            nowEpochMs = day * 21
        )

        assertTrue(state.scriptoriumLamps >= 3)
        assertTrue(state.mutationInscription.contains("scriptorium"))
        assertTrue(state.archiveResonance > 0f)
    }


    @Test
    fun `return awakening survives follow-up sessions inside the recent-return window`() {
        val day = 86_400_000L
        val sessions = listOf(
            ReadingSessionSnapshot(
                id = "before-gap",
                bookId = "returning",
                startedAtEpochMs = day,
                endedAtEpochMs = day + 60_000L,
                activeMillis = 60_000L,
                pacedPageTurns = 1,
                highlightCount = 0,
                noteCount = 0
            ),
            ReadingSessionSnapshot(
                id = "first-return",
                bookId = "returning",
                startedAtEpochMs = day * 60,
                endedAtEpochMs = day * 60 + 60_000L,
                activeMillis = 60_000L,
                pacedPageTurns = 1,
                highlightCount = 0,
                noteCount = 0
            ),
            ReadingSessionSnapshot(
                id = "follow-up",
                bookId = "returning",
                startedAtEpochMs = day * 61,
                endedAtEpochMs = day * 61 + 60_000L,
                activeMillis = 60_000L,
                pacedPageTurns = 1,
                highlightCount = 0,
                noteCount = 0
            )
        )

        val state = deriveCastleMemoryState(
            books = listOf(
                Book(
                    id = "returning",
                    title = "Returning Volume",
                    author = "Veil",
                    addedAtEpochMs = day,
                    lastOpenedAtEpochMs = day * 61
                )
            ),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = sessions,
            nowEpochMs = day * 62
        )

        assertTrue(state.returnAwakening > 0f)
        assertTrue(state.mutationInscription.startsWith("After a long quiet"))
    }

    @Test
    fun `future corrupt activity cannot hide the latest valid reading record`() {
        val day = 86_400_000L
        val state = deriveCastleMemoryState(
            books = listOf(
                Book(
                    id = "clock-skew",
                    title = "Clock Skew",
                    author = "Veil",
                    addedAtEpochMs = day,
                    lastOpenedAtEpochMs = day * 500
                )
            ),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = listOf(
                ReadingSessionSnapshot(
                    id = "valid-session",
                    bookId = "clock-skew",
                    startedAtEpochMs = day * 20,
                    endedAtEpochMs = day * 20 + 60_000L,
                    activeMillis = 60_000L,
                    pacedPageTurns = 1,
                    highlightCount = 0,
                    noteCount = 0
                )
            ),
            nowEpochMs = day * 21
        )

        assertEquals(1, state.daysSinceLastActivity)
        assertTrue(state.archiveAgeDays >= 20)
    }

    private fun sealedCycle(
        book: Book,
        cycleIndex: Int,
        completedAt: Long
    ): ReadingCycleRecord =
        ReadingCycleRecord(
            id = "cycle:${book.id}:$cycleIndex:$completedAt",
            bookId = book.id,
            cycleIndex = cycleIndex,
            titleSnapshot = book.title,
            authorSnapshot = book.author,
            startedAtEpochMs = completedAt - 3_600_000L,
            completedAtEpochMs = completedAt,
            finalLocatorJson = "{}",
            sessionCount = 1,
            totalActiveMillis = 3_600_000L,
            pacedPageTurns = 20,
            highlightCount = 0,
            noteCount = 0,
            bookmarkCount = 0,
            sealCode = "VR-TEST-$cycleIndex",
            timeline = emptyList()
        )

}
