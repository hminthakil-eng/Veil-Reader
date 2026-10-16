package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w320dp-h900dp-mdpi")
class InvestigationDeskTest {
    private val book = Book("book", "Volume", "Author", sourceUri = "file:///book")
    private val locator = " {\"href\":\"chapter.xhtml\",\"type\":\"application/xhtml+xml\"} "
    private fun mark(id: String = "mark", time: Long = 1) =
        Highlight(id, book.id, "Private quote", locator, "Private note", time)

    @Test fun rejectsInvalidAndDeletedSources() {
        val invalid = listOf(mark().copy(bookId = "deleted"), mark().copy(locatorJson = "broken"),
            mark().copy(locatorJson = "{}"), mark().copy(locatorJson = " "),
            mark().copy(id = " "), mark().copy(quote = " ", note = " "))
        assertTrue(indexInvestigationDesk(listOf(book), invalid).isEmpty())
        assertTrue(indexInvestigationDesk(emptyList(), listOf(mark())).isEmpty())
        assertTrue(indexInvestigationDesk(listOf(book.copy(sourceUri = null)), listOf(mark())).isEmpty())
    }

    @Test fun groupsBooksAndSortsNewestFirstWithStableIdTies() {
        val other = book.copy(id = "other", title = "Another")
        val marks = listOf(mark("z", 10), mark("a", 10), mark("old", 1), mark("other").copy(bookId = other.id))
        val indexed = indexInvestigationDesk(listOf(book, other), marks)
        assertEquals(listOf("other", "book"), indexed.map { it.book.id })
        assertEquals(listOf("a", "z", "old"), indexed.last().highlights.map { it.id })
        assertEquals(indexed, indexInvestigationDesk(listOf(other, book), marks.reversed()))
    }

    @Test fun searchOnlyMatchesBookTitleAndPreservesOriginalObjects() {
        val saved = mark()
        val group = indexInvestigationDesk(listOf(book), listOf(saved), " vOlUmE ").single()
        assertSame(book, group.book)
        assertSame(saved, group.highlights.single())
        assertEquals(locator, group.highlights.single().locatorJson)
        assertTrue(indexInvestigationDesk(listOf(book), listOf(saved), "Private").isEmpty())
    }

    @Test fun supportsNotesOnlyAndThousandMarksWithoutDuplicateKeys() {
        val marks = (0 until 1000).map { mark("id$it", it.toLong()) }
        assertEquals(1000, indexInvestigationDesk(listOf(book), marks).single().highlights.size)
        assertEquals(1, indexInvestigationDesk(listOf(book), listOf(mark().copy(quote = ""))).single().highlights.size)
        // Ambiguous duplicate identities fail closed, independent of input order.
        assertTrue(indexInvestigationDesk(listOf(book), listOf(mark(), mark().copy(note = "other"))).isEmpty())
    }
}
