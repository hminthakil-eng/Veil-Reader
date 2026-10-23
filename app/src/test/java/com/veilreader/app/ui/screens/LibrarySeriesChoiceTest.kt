package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.*
import org.junit.Test

class LibrarySeriesChoiceTest {
    private fun book(id: String, series: String?) =
        Book(id = id, title = "Volume $id", author = "Fixture", seriesName = series)

    @Test fun completeTenThousandSeriesCanBeRetrieved() {
        val choices = buildLibrarySeriesChoices((0 until 10_000).map {
            book("$it", "Cycle %05d".format(java.util.Locale.ROOT, it))
        })
        assertEquals(10_000, choices.size)
        assertEquals("Cycle 00000", choices.first().name)
        assertEquals("Cycle 09999", choices.last().name)
        assertEquals(listOf(choices.last()), filterLibrarySeriesChoices(choices, "09999"))
    }

    @Test fun equivalentLabelsHaveDeterministicNamesAndDistinctBookCounts() {
        val books = listOf(book("one", " cycle "), book("two", "Cycle"), book("one", " cycle "))
        val choices = buildLibrarySeriesChoices(books)
        assertEquals(listOf(LibrarySeriesChoice("cycle", "Cycle", 2, "cycle")), choices)
        assertEquals(choices, buildLibrarySeriesChoices(books.reversed()))
    }

    @Test fun blankAndMissingSeriesDoNotBecomeChoices() {
        assertTrue(buildLibrarySeriesChoices(listOf(book("one", null), book("two", "  "))).isEmpty())
    }

    @Test fun persianKeyboardEquivalenceDoesNotMergeFilterIdentities() {
        val choices = buildLibrarySeriesChoices(listOf(
            book("one", "کتاب‌های تاریخ ۲"), book("two", "كتاب‌های تاریخ ۲")
        ))
        assertEquals(2, choices.size)
        assertEquals(2, filterLibrarySeriesChoices(choices, "كتاب هاي تاريخ 2").size)
        assertNotEquals(choices[0].key, choices[1].key)
    }

    @Test fun broadSearchPreservesNamesAndEmptyQueryRestoresAllChoices() {
        val choices = buildLibrarySeriesChoices(listOf(
            book("one", "DeepSpace"), book("two", "Deep Space")
        ))
        assertEquals(2, choices.size)
        assertEquals(choices, filterLibrarySeriesChoices(choices, "DEEP SPACE"))
        assertEquals(choices, filterLibrarySeriesChoices(choices, " \u064e "))
        assertTrue(filterLibrarySeriesChoices(choices, "missing").isEmpty())
    }
}
