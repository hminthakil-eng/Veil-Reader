package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightMemoryTest {
    private val day = 86_400_000L
    private val now = 500L * day

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
        assertEquals(5, memory.ageDays)
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
        assertEquals(120, memory.ageDays)
        assertEquals(290, memory.resonanceScore)
    }

    @Test
    fun `living margin reports exact revisits separately from later book activity`() {
        val highlight = Highlight(
            id = "h",
            bookId = "book",
            quote = "Remember",
            locatorJson = "{}",
            createdAtEpochMs = now - 180L * day
        )
        val book = Book(
            id = "book",
            title = "Archive",
            author = "Veil",
            lastOpenedAtEpochMs = now - day
        )
        val visits = listOf(
            PassageVisit("v1", "h", "book", "{}", now - 90L * day),
            PassageVisit("v2", "h", "book", "{}", now - 30L * day)
        )

        val memory = deriveHighlightMemory(
            highlight = highlight,
            book = book,
            nowEpochMs = now,
            passageVisits = visits
        )

        assertTrue(memory.bookActivityAfterMark)
        assertEquals(2, memory.revisitCount)
        assertEquals(now - 30L * day, memory.lastViewedAtEpochMs)
        assertEquals(30, memory.lastViewedDaysAgo)
        assertEquals(now - 30L * day, memory.lastViewedAtEpochMs)
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
    fun `exact passage returns add bounded resonance without replacing archival signals`() {
        val highlight = Highlight(
            id = "h",
            bookId = "book",
            quote = "A recurring line",
            locatorJson = "{}",
            createdAtEpochMs = now - 120L * day
        )
        val book = Book(id = "book", title = "Archive", author = "Veil")
        val visits = (1..6).map { index ->
            PassageVisit(
                id = "v$index",
                highlightId = "h",
                bookId = "book",
                locatorJson = "{}",
                viewedAtEpochMs = now - (7L - index) * day
            )
        }

        val withoutVisits = deriveHighlightMemory(highlight, book, now)
        val withVisits = deriveHighlightMemory(
            highlight = highlight,
            book = book,
            nowEpochMs = now,
            passageVisits = visits
        )

        assertEquals(60, withVisits.resonanceScore - withoutVisits.resonanceScore)
        assertEquals(6, withVisits.revisitCount)
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
