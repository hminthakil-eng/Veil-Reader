package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryMemoryTest {
    private val day = 86_400_000L

    @Test
    fun `old untouched imports sink into the deep shelf without fake reading history`() {
        val now = 240L * day
        val book = Book(
            id = "old",
            title = "Old Volume",
            author = "Veil",
            addedAtEpochMs = 10L * day
        )

        val state = deriveLibraryMemoryState(
            books = listOf(book),
            highlights = emptyList(),
            sessions = emptyList(),
            nowEpochMs = now
        )

        val memory = state.memoryFor("old")!!
        assertEquals(ArchiveDepth.FORGOTTEN, memory.depth)
        assertTrue(state.isDeepShelf("old"))
        assertTrue(state.events.isEmpty())
    }

    @Test
    fun `a durable long return gap creates a forgotten volume event even after the book is recent again`() {
        val book = Book(
            id = "b",
            title = "Returned",
            author = "Veil",
            lastOpenedAtEpochMs = 260L * day,
            progress = 0.5f
        )
        val sessions = listOf(
            ReadingSessionSnapshot("s1", "b", 10L * day, 10L * day + day, day, 3, 0, 0),
            ReadingSessionSnapshot("s2", "b", 250L * day, 250L * day + day, day, 4, 0, 0)
        )

        val state = deriveLibraryMemoryState(
            books = listOf(book),
            highlights = emptyList(),
            sessions = sessions,
            nowEpochMs = 261L * day
        )

        assertEquals(ArchiveDepth.SURFACE, state.memoryFor("b")!!.depth)
        assertTrue(
            state.events.any {
                it.kind == LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN
            }
        )
    }

    @Test
    fun `global long silence is factual and needs two durable sessions`() {
        val books = listOf(
            Book(id = "a", title = "A", author = "Veil"),
            Book(id = "b", title = "B", author = "Veil")
        )
        val sessions = listOf(
            ReadingSessionSnapshot("a1", "a", day, 2L * day, day, 1, 0, 0),
            ReadingSessionSnapshot("b1", "b", 70L * day, 71L * day, day, 1, 0, 0)
        )

        val state = deriveLibraryMemoryState(
            books = books,
            highlights = emptyList(),
            sessions = sessions,
            nowEpochMs = 72L * day
        )

        assertTrue(
            state.events.any {
                it.kind == LibraryMemoryEventKind.LONG_SILENCE_RETURN
            }
        )
    }

    @Test
    fun `an old margin returns only after a later durable reading session`() {
        val book = Book(id = "b", title = "Margin", author = "Veil")
        val highlight = Highlight(
            id = "h",
            bookId = "b",
            quote = "A line kept in the archive",
            locatorJson = "{}",
            createdAtEpochMs = 5L * day
        )

        val tooSoon = deriveLibraryMemoryState(
            books = listOf(book),
            highlights = listOf(highlight),
            sessions = listOf(
                ReadingSessionSnapshot("s1", "b", 40L * day, 41L * day, day, 1, 0, 0)
            ),
            nowEpochMs = 120L * day
        )
        assertFalse(
            tooSoon.events.any {
                it.kind == LibraryMemoryEventKind.OLD_MARGIN_RETURN
            }
        )

        val returned = deriveLibraryMemoryState(
            books = listOf(book),
            highlights = listOf(highlight),
            sessions = listOf(
                ReadingSessionSnapshot("s2", "b", 120L * day, 121L * day, day, 1, 0, 0)
            ),
            nowEpochMs = 122L * day
        )
        assertTrue(
            returned.events.any {
                it.kind == LibraryMemoryEventKind.OLD_MARGIN_RETURN
            }
        )
    }

    @Test
    fun `forgotten volume wins over long silence at the same return moment`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val sessions = listOf(
            ReadingSessionSnapshot("s1", "b", day, 2L * day, day, 1, 0, 0),
            ReadingSessionSnapshot("s2", "b", 220L * day, 221L * day, day, 1, 0, 0)
        )

        val state = deriveLibraryMemoryState(
            books = listOf(book),
            highlights = emptyList(),
            sessions = sessions,
            nowEpochMs = 222L * day
        )

        val sameMoment = state.events.filter { it.atEpochMs == 220L * day }
        assertEquals(1, sameMoment.size)
        assertEquals(
            LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN,
            sameMoment.single().kind
        )
    }
}
