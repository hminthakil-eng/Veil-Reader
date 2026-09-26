package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookArtifactMemoryTest {
    private val hour = 3_600_000L

    @Test
    fun `untouched volume has no invented physical wear`() {
        val memory = deriveBookArtifactMemory(
            book = Book(id = "b", title = "B", author = "Veil"),
            sessions = emptyList(),
            highlights = emptyList(),
            bookmarks = emptyList()
        )

        assertEquals(BookArtifactMemory.EMPTY, memory)
    }

    @Test
    fun `durable sessions create handling and fore-edge wear`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val memory = deriveBookArtifactMemory(
            book = book,
            sessions = listOf(
                ReadingSessionSnapshot("s1", "b", 1, 2, 12L * hour, 180, 0, 0),
                ReadingSessionSnapshot("s2", "b", 3, 4, 8L * hour, 120, 0, 0)
            ),
            highlights = emptyList(),
            bookmarks = emptyList()
        )

        assertEquals(2, memory.sessionCount)
        assertEquals(20L * hour, memory.activeMillis)
        assertEquals(300, memory.pacedPageTurns)
        assertTrue(memory.handlingWear > 0f)
        assertTrue(memory.foreEdgeWear > 0f)
        assertEquals(0f, memory.marginMemory, 0.0001f)
    }

    @Test
    fun `preserved passages create margin memory without faking reading time`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val memory = deriveBookArtifactMemory(
            book = book,
            sessions = emptyList(),
            highlights = listOf(
                Highlight("h1", "b", "one", "{}", note = "margin"),
                Highlight("h2", "b", "two", "{}")
            ),
            bookmarks = emptyList()
        )

        assertEquals(2, memory.highlightCount)
        assertEquals(1, memory.annotationCount)
        assertTrue(memory.marginMemory > 0f)
        assertEquals(0f, memory.handlingWear, 0.0001f)
        assertEquals(3, memory.marginFleckCount)
    }

    @Test
    fun `bookmark ribbons are bounded and remain book local`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val memory = deriveBookArtifactMemory(
            book = book,
            sessions = emptyList(),
            highlights = emptyList(),
            bookmarks = listOf(
                Bookmark("1", "b", "one", "{}"),
                Bookmark("2", "b", "two", "{}"),
                Bookmark("3", "b", "three", "{}"),
                Bookmark("4", "b", "four", "{}"),
                Bookmark("x", "other", "other", "{}")
            )
        )

        assertEquals(4, memory.bookmarkCount)
        assertEquals(3, memory.ribbonCount)
    }

    @Test
    fun `negative corrupted session counters cannot create wear`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val memory = deriveBookArtifactMemory(
            book = book,
            sessions = listOf(
                ReadingSessionSnapshot("bad", "b", 1, 2, -hour, -5, 0, 0)
            ),
            highlights = emptyList(),
            bookmarks = emptyList()
        )

        assertEquals(0L, memory.activeMillis)
        assertEquals(0, memory.pacedPageTurns)
        assertTrue(memory.handlingWear >= 0f)
        assertTrue(memory.foreEdgeWear >= 0f)
    }
}
