package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import org.junit.Assert.*
import org.junit.Test

class LatestClueTest {
    private val book = Book("book", "Volume", "Author", sourceUri = "file:///book.epub")
    private fun mark(id: String = "mark", time: Long = 1L) =
        Highlight(id, book.id, "Only a saved passage", "{exact locator}", createdAtEpochMs = time)

    @Test fun absentAndInvalidHighlightsProduceNoClue() {
        assertNull(deriveLatestClue(listOf(book), emptyList()))
        val invalid = listOf(mark().copy(bookId = "deleted"), mark().copy(bookId = " "),
            mark().copy(quote = " \n"), mark().copy(locatorJson = " \t"))
        assertNull(deriveLatestClue(listOf(book), invalid))
        for (source in listOf(null, "", " ")) {
            assertNull(deriveLatestClue(listOf(book.copy(sourceUri = source)), listOf(mark())))
        }
        assertNull(deriveLatestClue(listOf(book.copy(id = " ")), listOf(mark().copy(bookId = " "))))
    }

    @Test fun latestValidMarkWinsWithStableIdTieBreakAcrossInputOrder() {
        val older = mark("older", 5L)
        val winner = mark("a", 10L)
        val other = mark("z", 10L)
        val invalidNewest = mark("invalid", 20L).copy(locatorJson = "")
        val marks = listOf(older, other, winner, invalidNewest)
        assertEquals(winner, deriveLatestClue(listOf(book), marks)?.highlight)
        assertEquals(winner, deriveLatestClue(listOf(book), marks.reversed())?.highlight)
    }

    @Test fun linearSelectionMatchesOriginalSortedOracleEvenWhenBookProgressReset() {
        val reread = book.copy(progress = 0f)
        val marks = (0 until 1200).map { i ->
            mark("id%04d".format(i), (i % 27).toLong()).copy(
                quote = "Clue $i", locatorJson = "{saved-anchor-$i}"
            )
        } + mark("not-visible", 200L).copy(bookId = "removed-book")
        val expected = marks.filter { it.bookId == reread.id && it.quote.isNotBlank() &&
            it.locatorJson.isNotBlank() }.sortedWith(
            compareByDescending<Highlight> { it.createdAtEpochMs }
                .thenBy { it.id }.thenBy { it.bookId }
                .thenBy { it.locatorJson }.thenBy { it.quote }
        ).first()
        assertEquals(expected, deriveLatestClue(listOf(reread), marks)?.highlight)
        assertEquals(expected, deriveLatestClue(listOf(reread), marks.reversed())?.highlight)
    }

    @Test fun anyAvailableLibraryBookCanSupplyClueWithoutChangingSavedTextOrLocator() {
        val saved = mark().copy(quote = "  Saved\nمتن  ", locatorJson = " {untouched} ")
        val clue = requireNotNull(deriveLatestClue(listOf(book), listOf(saved)))
        assertEquals(book, clue.book)
        assertEquals(saved, clue.highlight)
        assertNull(deriveLatestClue(emptyList(), listOf(saved)))
    }
}
