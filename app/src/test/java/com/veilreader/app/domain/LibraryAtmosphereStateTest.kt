package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAtmosphereStateTest {
    private val hour = 3_600_000L

    @Test
    fun `empty library stays sparse quiet and foggy`() {
        val state = deriveLibraryAtmosphereState(
            books = emptyList(),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList(),
            memoryState = LibraryMemoryState.EMPTY
        )

        assertEquals(LibraryAtmosphereState.EMPTY, state)
        assertEquals(2, state.shelfBays)
        assertEquals(1, state.lampCount)
        assertEquals(0, state.completedAlcoves)
    }

    @Test
    fun `large real archive reveals more architectural depth`() {
        val books = (1..72).map { index ->
            Book(
                id = "b$index",
                title = "Volume $index",
                author = "Author $index",
                collections = listOf("Shelf ${index % 18}")
            )
        }
        val sessions = (1..120).map { index ->
            ReadingSessionSnapshot(
                id = "s$index",
                bookId = "b${(index % 72) + 1}",
                startedAtEpochMs = index.toLong(),
                endedAtEpochMs = index.toLong() + hour,
                activeMillis = hour,
                pacedPageTurns = 8,
                highlightCount = 0,
                noteCount = 0
            )
        }

        val state = deriveLibraryAtmosphereState(
            books = books,
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = sessions,
            memoryState = LibraryMemoryState.EMPTY
        )

        assertTrue(state.archiveDensity > 0.70f)
        assertTrue(state.shelfBays >= 9)
        assertTrue(state.archLayers >= 4)
        assertTrue(state.distantStackLayers >= 4)
    }

    @Test
    fun `finished volumes create alcoves while active history creates light`() {
        val books = (1..30).map { index ->
            Book(
                id = "b$index",
                title = "B$index",
                author = "Veil",
                progress = if (index <= 18) 1f else 0.55f,
                finished = index <= 18
            )
        }
        val sessions = (1..60).map { index ->
            ReadingSessionSnapshot(
                id = "s$index",
                bookId = "b${(index % 30) + 1}",
                startedAtEpochMs = index.toLong(),
                endedAtEpochMs = index.toLong() + 3L * hour,
                activeMillis = 3L * hour,
                pacedPageTurns = 12,
                highlightCount = 0,
                noteCount = 0
            )
        }

        val state = deriveLibraryAtmosphereState(
            books = books,
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = sessions,
            memoryState = LibraryMemoryState.EMPTY
        )

        assertTrue(state.completedAlcoves > 0)
        assertTrue(state.lampCount > 1)
        assertTrue(state.memoryWarmth > 0f)
        assertTrue(state.brassGlow > LibraryAtmosphereState.EMPTY.brassGlow)
    }

    @Test
    fun `deep shelf quiet opens corridors and thickens lower fog without unlocking anything`() {
        val books = (1..20).map { index ->
            Book(id = "b$index", title = "B$index", author = "Veil")
        }
        val memories = books.associate { book ->
            book.id to BookArchiveMemory(
                bookId = book.id,
                lastRecordedActivityAtEpochMs = 1L,
                inactiveMillis = 220L * 86_400_000L,
                depth = ArchiveDepth.FORGOTTEN,
                longestReturnGapMillis = null
            )
        }
        val memoryState = LibraryMemoryState(
            byBookId = memories,
            deepShelfBookIds = books.map { it.id },
            events = emptyList()
        )

        val state = deriveLibraryAtmosphereState(
            books = books,
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList(),
            memoryState = memoryState
        )

        assertTrue(state.deepQuiet > 0.80f)
        assertTrue(state.deepCorridors >= 3)
        assertTrue(state.fogAlpha > 0.46f)
    }

    @Test
    fun `archive marks affect density but never invent book count`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val highlights = (1..20).map { index ->
            Highlight("h$index", "b", "passage $index", "{}", note = if (index % 2 == 0) "note" else "")
        }
        val bookmarks = (1..8).map { index ->
            Bookmark("m$index", "b", "mark $index", "{}")
        }

        val state = deriveLibraryAtmosphereState(
            books = listOf(book),
            highlights = highlights,
            bookmarks = bookmarks,
            sessions = emptyList(),
            memoryState = LibraryMemoryState.EMPTY
        )

        assertEquals(1, state.volumeCount)
        assertEquals(20, state.passageCount)
        assertEquals(8, state.bookmarkCount)
        assertTrue(state.archiveDensity > 0f)
    }
}
