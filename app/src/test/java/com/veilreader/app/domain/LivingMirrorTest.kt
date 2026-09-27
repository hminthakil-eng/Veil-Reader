package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingMirrorTest {
    private val now = 1_800_000_000_000L

    @Test
    fun `only real notes with existing books enter the mirror`() {
        val book = Book(id = "b1", title = "Book", author = "Author")
        val notes = deriveLivingMirrorNotes(
            books = listOf(book),
            highlights = listOf(
                Highlight("h1", "b1", "quote", "loc", note = "kept", createdAtEpochMs = now - 1_000L),
                Highlight("h2", "b1", "quote", "loc", note = "", createdAtEpochMs = now - 1_000L),
                Highlight("h3", "missing", "quote", "loc", note = "orphan", createdAtEpochMs = now - 1_000L)
            ),
            passageVisits = emptyList(),
            readingCycles = emptyList(),
            nowEpochMs = now
        )
        assertEquals(listOf("h1"), notes.map { it.highlightId })
    }

    @Test
    fun `projection is deterministic for the same factual history`() {
        val book = Book(id = "b1", title = "Book", author = "Author")
        val highlight = Highlight("h1", "b1", "quote", "loc", note = "note", createdAtEpochMs = now - 86_400_000L)
        val first = deriveLivingMirrorNotes(listOf(book), listOf(highlight), emptyList(), emptyList(), now)
        val second = deriveLivingMirrorNotes(listOf(book), listOf(highlight), emptyList(), emptyList(), now)
        assertEquals(first, second)
    }

    @Test
    fun `revisit pulls a note closer without rewriting its recorded time`() {
        val book = Book(id = "b1", title = "Book", author = "Author")
        val created = now - 200L * 86_400_000L
        val highlight = Highlight("h1", "b1", "quote", "loc", note = "note", createdAtEpochMs = created)
        val settled = deriveLivingMirrorNotes(listOf(book), listOf(highlight), emptyList(), emptyList(), now).single()
        val revisited = deriveLivingMirrorNotes(
            books = listOf(book),
            highlights = listOf(highlight),
            passageVisits = listOf(
                PassageVisit("v1", "h1", "b1", "loc", now - 1_000L),
                PassageVisit("v2", "h1", "b1", "loc", now - 500L)
            ),
            readingCycles = emptyList(),
            nowEpochMs = now
        ).single()

        assertEquals(created, revisited.recordedAtEpochMs)
        assertEquals(2, revisited.revisitCount)
        assertTrue(revisited.depth < settled.depth)
        assertTrue(revisited.proximity > settled.proximity)
    }

    @Test
    fun `reread cycles add rings from durable cycle history`() {
        val book = Book(id = "b1", title = "Book", author = "Author")
        val highlight = Highlight("h1", "b1", "quote", "loc", note = "note", createdAtEpochMs = now - 1_000L)
        fun cycle(index: Int) = ReadingCycleRecord(
            id = "c$index",
            bookId = "b1",
            cycleIndex = index,
            titleSnapshot = "Book",
            authorSnapshot = "Author",
            startedAtEpochMs = now - 10_000L,
            completedAtEpochMs = now - index * 1_000L,
            finalLocatorJson = "loc",
            sessionCount = 1,
            totalActiveMillis = 1_000L,
            pacedPageTurns = 1,
            highlightCount = 1,
            noteCount = 1,
            bookmarkCount = 0,
            sealCode = "seal",
            timeline = emptyList()
        )

        val note = deriveLivingMirrorNotes(
            listOf(book),
            listOf(highlight),
            emptyList(),
            listOf(cycle(1), cycle(2), cycle(3)),
            now
        ).single()

        assertEquals(3, note.cycleIndex)
        assertEquals(3, note.ringCount)
    }

    @Test
    fun `mirror positions remain inside the usable surface`() {
        val books = (1..30).map { Book(id = "b$it", title = "B$it", author = "A") }
        val highlights = books.mapIndexed { index, book ->
            Highlight(
                id = "h$index",
                bookId = book.id,
                quote = "q",
                locatorJson = "l",
                note = "n",
                createdAtEpochMs = now - index * 10_000L
            )
        }
        deriveLivingMirrorNotes(books, highlights, emptyList(), emptyList(), now).forEach { note ->
            assertTrue(note.clusterX in 0.08f..0.92f)
            assertTrue(note.clusterY in 0.10f..0.88f)
            assertTrue(note.depth in 0f..1f)
            assertTrue(note.proximity in 0f..1f)
        }
    }
}
