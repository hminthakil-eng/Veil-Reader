package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrodesMirrorTest {

    @Test
    fun notesOutrankPlainHighlightsAndKeepExactLocator() {
        val book = Book(id = "book-1", title = "Volume", author = "Author")
        val plain = Highlight(
            id = "h-plain",
            bookId = book.id,
            quote = "Plain highlight",
            locatorJson = """{"href":"c1.xhtml"}""",
            createdAtEpochMs = 1L
        )
        val noted = Highlight(
            id = "h-note",
            bookId = book.id,
            quote = "Quoted passage",
            locatorJson = """{"href":"c2.xhtml","locations":{"progression":0.42}}""",
            note = "My durable note",
            createdAtEpochMs = 2L
        )

        val result = deriveArrodesFragments(
            highlights = listOf(plain, noted),
            books = listOf(book)
        )

        assertEquals("h-note", result.first().id)
        assertEquals("My durable note", result.first().text)
        assertEquals(noted.locatorJson, result.first().locatorJson)
        assertEquals(ArrodesFragmentKind.NOTE, result.first().kind)
    }


    @Test
    fun sessionDeckKeepsStrongestFirstAndNeverDuplicatesFragments() {
        val book = Book(id = "book-1", title = "Volume", author = "Author")
        val fragments = (0 until 8).map { index ->
            ArrodesFragment(
                id = "fragment-$index",
                text = "Echo $index",
                book = book,
                locatorJson = """{"index":$index}""",
                kind = if (index == 0) ArrodesFragmentKind.NOTE else ArrodesFragmentKind.HIGHLIGHT,
                resonanceScore = 1_000 - index
            )
        }

        val first = orderArrodesFragmentsForSession(fragments, sessionSeed = 73)
        val second = orderArrodesFragmentsForSession(fragments, sessionSeed = 73)

        assertEquals(fragments.first(), first.first())
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(fragments.size, first.map { it.id }.distinct().size)
        assertEquals(fragments.map { it.id }.toSet(), first.map { it.id }.toSet())
    }

    @Test
    fun fragmentsWithoutBookOrLocatorAreRejected() {
        val book = Book(id = "book-1", title = "Volume", author = "Author")
        val missingLocator = Highlight(
            id = "bad",
            bookId = book.id,
            quote = "No return path",
            locatorJson = ""
        )
        val missingBook = Highlight(
            id = "orphan",
            bookId = "missing",
            quote = "Orphan",
            locatorJson = "{}"
        )

        val result = deriveArrodesFragments(
            highlights = listOf(missingLocator, missingBook),
            books = listOf(book)
        )

        assertTrue(result.isEmpty())
    }
}
