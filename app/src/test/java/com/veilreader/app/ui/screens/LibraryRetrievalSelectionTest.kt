package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.*
import org.junit.Test

class LibraryRetrievalSelectionTest {
    private fun book(id: String, title: String = "Veil", added: Long = 100L) = Book(
        id = id, title = title, author = "Same author", addedAtEpochMs = added,
        lastOpenedAtEpochMs = 0L, seriesName = "Cycle", seriesIndex = 1.0
    )

    private fun scores(query: String, books: List<Book>): Map<String, Int> {
        val normalized = normalizeLibrarySearchText(query)
        return books.associate { book ->
            book.id to requireNotNull(librarySearchRelevance(normalized,
                normalizedLibrarySearchDocument(book.title, book.author, book.seriesName,
                    book.language, book.allCollections)))
        }
    }

    @Test fun legacyDefaultIsContextualButNewExplicitRecentIsNot() {
        assertEquals(LibrarySortMode.AUTO, restoreLibrarySort("Recent"))
        assertEquals(LibrarySortMode.RECENT, restoreLibrarySort("RECENT"))
        assertEquals(LibrarySortMode.TITLE, restoreLibrarySort("Title"))
        assertEquals(LibrarySortMode.ARCHIVE_DEPTH, restoreLibrarySort("Archive Depth"))
        assertEquals(LibrarySortMode.AUTO, restoreLibrarySort("future-unknown"))
        LibrarySortMode.entries.forEach { assertEquals(it, restoreLibrarySort(it.name)) }
    }

    @Test fun defaultOrderTracksEffectiveSearchWithoutOverridingExplicitChoices() {
        assertEquals(LibrarySortMode.RELEVANCE, effectiveLibrarySort(LibrarySortMode.AUTO, true))
        assertEquals(LibrarySortMode.RECENT, effectiveLibrarySort(LibrarySortMode.AUTO, false))
        for (mode in listOf(LibrarySortMode.RECENT, LibrarySortMode.TITLE, LibrarySortMode.SERIES)) {
            assertEquals(mode, effectiveLibrarySort(mode, true))
            assertEquals(mode, effectiveLibrarySort(mode, false))
        }
        assertEquals(LibrarySortMode.RECENT,
            effectiveLibrarySort(LibrarySortMode.AUTO, normalizeLibrarySearchText("  ِ  ").isNotBlank()))
    }

    @Test fun explicitRecentIgnoresScoresWhileRelevancePrefersTheExactTitle() {
        val old = book("old", "Veil", 100L)
        val recent = book("new", "Veil Notes", 200L)
        val books = listOf(recent, old)
        val scores = scores("veil", books)
        assertEquals(listOf("old", "new"), sortLibraryBooks(books, LibrarySortMode.RELEVANCE, scores).map { it.id })
        assertEquals(listOf("new", "old"), sortLibraryBooks(books, LibrarySortMode.RECENT, scores).map { it.id })
    }

    @Test fun persianKeyboardVariantsKeepTheExistingTitleRelevance() {
        val books = listOf(book("contains", "دفتر تاریخ", 200L), book("exact", "تاريخ", 100L))
        assertEquals(listOf("exact", "contains"),
            sortLibraryBooks(books, LibrarySortMode.RELEVANCE, scores("تاریخ", books)).map { it.id })
    }

    @Test fun equalKeysHaveStableIdentityOrderInEveryMode() {
        val books = listOf(book("b"), book("a"))
        for (mode in LibrarySortMode.entries) {
            for (input in listOf(books, books.reversed())) {
                assertEquals(mode.name, listOf("a", "b"),
                    sortLibraryBooks(input, mode, mapOf("a" to 10, "b" to 10)) { 5L }.map { it.id })
            }
        }
    }

    @Test fun seriesUsesNumericVolumeOrderBeforeTitleAndIdentity() {
        val books = listOf(book("ten").copy(seriesIndex = 10.0), book("two").copy(seriesIndex = 2.0))
        assertEquals(listOf("two", "ten"), sortLibraryBooks(books, LibrarySortMode.SERIES).map { it.id })
    }

    @Test fun explicitComparatorsPreserveTheirPrimaryOrdering() {
        val books = listOf(
            book("a", "Zebra").copy(author = "Able", progress = .8f),
            book("b", "Alpha").copy(author = "Zed", progress = .2f)
        )
        assertEquals(listOf("b", "a"), sortLibraryBooks(books, LibrarySortMode.TITLE).map { it.id })
        assertEquals(listOf("a", "b"), sortLibraryBooks(books, LibrarySortMode.AUTHOR).map { it.id })
        assertEquals(listOf("a", "b"), sortLibraryBooks(books, LibrarySortMode.PROGRESS).map { it.id })
        assertEquals(listOf("b", "a"), sortLibraryBooks(books, LibrarySortMode.ARCHIVE_DEPTH) {
            if (it == "b") 20L else 10L
        }.map { it.id })
    }

    @Test fun recentRetainsTheExistingAddedOrOpenedTimestampRule() {
        val books = listOf(book("added", added = 200L), book("opened").copy(lastOpenedAtEpochMs = 300L))
        assertEquals(listOf("opened", "added"), sortLibraryBooks(books, LibrarySortMode.RECENT).map { it.id })
    }

    @Test fun staleAndBlankSelectionsClearButEquivalentMetadataSurvives() {
        assertEquals("", reconcileLibraryLabel("Cycle", emptyList()))
        assertEquals("", reconcileLibraryLabel("Cycle", listOf("Different")))
        assertEquals("Cycle", reconcileLibraryLabel(" CYCLE ", listOf(" Cycle ")))
        assertEquals("", reconcileLibraryLabel(" ", listOf("Cycle")))
    }

    @Test fun unknownMetadataAbsenceDefersRemovalUntilCanonicalBooksArrive() {
        assertEquals("Cycle", reconcileLibraryLabel("Cycle", emptyList(), allowRemoval = false))
        assertEquals("Cycle", reconcileLibraryLabel("Cycle", listOf("Cycle"), allowRemoval = true))
        assertEquals("", reconcileLibraryLabel("Cycle", listOf("Different"), allowRemoval = true))
        assertEquals("", reconcileLibraryLabel(" ", emptyList(), allowRemoval = false))
    }

    @Test fun matchingUsesGroupIdentityWithoutFuzzyMergingNames() {
        assertTrue(libraryLabelMatches(" Cycle ", "cycle"))
        assertTrue(libraryLabelMatches(null, ""))
        assertFalse(libraryLabelMatches(null, "Cycle"))
        assertFalse(libraryLabelMatches("Deep Space", "DeepSpace"))
        assertFalse(libraryLabelMatches("كتاب", "کتاب"))
        assertNotEquals(libraryLabelKey("I"), libraryLabelKey("ı"))
    }
}
