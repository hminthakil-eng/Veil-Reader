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
        assertEquals(CastleInscriptionStage.EMPTY, state.inscriptionStage)
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
        assertTrue(state.inscriptionStage != CastleInscriptionStage.EMPTY)
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

        assertEquals(CastleInscriptionStage.UNTRACED, isolated.inscriptionStage)
        assertEquals(CastleInscriptionStage.UNTRACED, connected.inscriptionStage)
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
}
