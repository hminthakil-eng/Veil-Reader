package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightMemoryTest {
    private val day = 86_400_000L
    private val now = 500L * day

    @Test
    fun `all echo bonuses contribute independently`() {
        for (flags in 0 until 16) {
            val annotated = flags and 1 != 0
            val finished = flags and 2 != 0
            val favorite = flags and 4 != 0
            val laterActivity = flags and 8 != 0
            val book = Book(
                id = "book", title = "Archive", author = "Veil",
                finished = finished, favorite = favorite,
                lastOpenedAtEpochMs = if (laterActivity) now - day else 0L
            )
            val highlight = Highlight(
                "mark", "book", "Remember this", "{}",
                note = if (annotated) "A note" else "",
                createdAtEpochMs = now - 120L * day
            )
            val expected = 120 + (if (annotated) 80 else 0) +
                (if (finished) 45 else 0) + (if (favorite) 25 else 0) +
                (if (laterActivity) 20 else 0)
            assertEquals("bonus flags=$flags", expected,
                deriveHighlightMemory(highlight, book, now).resonanceScore)
        }
    }

    @Test
    fun `fresh marks do not become echoes prematurely`() {
        val highlight = Highlight(
            id = "fresh",
            bookId = "book",
            quote = "New passage",
            locatorJson = "{}",
            createdAtEpochMs = now - 5L * day
        )

        val memory = deriveHighlightMemory(highlight, null, now)

        assertEquals(EchoDepth.FRESH, memory.echoDepth)
        assertFalse(memory.eligibleForEcho)
        assertNull(memory.echoLabel)
    }

    @Test
    fun `older annotated marks become deterministic echoes`() {
        val book = Book(
            id = "book",
            title = "Archive",
            author = "Veil",
            favorite = true,
            finished = true,
            lastOpenedAtEpochMs = now - 2L * day
        )
        val highlight = Highlight(
            id = "echo",
            bookId = "book",
            quote = "Something worth keeping",
            locatorJson = "{}",
            note = "A margin thought",
            createdAtEpochMs = now - 120L * day
        )

        val memory = deriveHighlightMemory(highlight, book, now)

        assertEquals(EchoDepth.ECHO, memory.echoDepth)
        assertTrue(memory.eligibleForEcho)
        assertTrue(memory.annotated)
        assertTrue(memory.bookActivityAfterMark)
        assertEquals("AN ECHO FROM 4 MONTHS AGO", memory.echoLabel)
        assertTrue(memory.resonanceScore > 120)
    }

    @Test
    fun `unknown or future timestamps never invent age`() {
        val unknown = deriveHighlightMemory(
            Highlight("u", "b", "quote", "{}", createdAtEpochMs = 0L),
            null,
            now
        )
        val future = deriveHighlightMemory(
            Highlight("f", "b", "quote", "{}", createdAtEpochMs = now + day),
            null,
            now
        )

        assertFalse(unknown.ageKnown)
        assertFalse(future.ageKnown)
        assertFalse(unknown.eligibleForEcho)
        assertFalse(future.eligibleForEcho)
    }

    @Test
    fun `archive echoes are ranked by real state and remain bounded`() {
        val books = mapOf(
            "a" to Book(id = "a", title = "A", author = "Veil"),
            "b" to Book(id = "b", title = "B", author = "Veil", favorite = true)
        )
        val echoes = deriveArchiveEchoes(
            highlights = listOf(
                Highlight("plain", "a", "plain", "{}", createdAtEpochMs = now - 40L * day),
                Highlight("fav", "b", "fav", "{}", note = "note", createdAtEpochMs = now - 40L * day),
                Highlight("fresh", "a", "fresh", "{}", createdAtEpochMs = now - day)
            ),
            booksById = books,
            nowEpochMs = now,
            limit = 1
        )

        assertEquals(1, echoes.size)
        assertEquals("fav", echoes.single().highlight.id)
    }
}
