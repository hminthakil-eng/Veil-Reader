package com.veilreader.app.data.migration

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LegacyLibraryMigratorTest {
    @Test
    fun plan_preserves_valid_records_filters_orphans_and_deduplicates_collections() {
        val books = listOf(
            Book(id = "a", title = "A", author = "One", format = BookFormat.EPUB, collection = "Mystery", addedAtEpochMs = 10L),
            Book(id = "b", title = "B", author = "Two", format = BookFormat.PDF, collection = "mystery", addedAtEpochMs = 20L),
            Book(id = "c", title = "C", author = "Three", format = BookFormat.EPUB, collection = "Science", addedAtEpochMs = 30L)
        )
        val highlights = listOf(
            Highlight("h1", "a", "quote", "{}"),
            Highlight("orphan", "missing", "lost", "{}")
        )
        val bookmarks = listOf(
            Bookmark("m1", "b", "place", "{}"),
            Bookmark("orphan", "missing", "lost", "{}")
        )

        val plan = buildLegacyImportPlan(books, highlights, bookmarks)

        assertEquals(3, plan.books.size)
        assertEquals(listOf("h1"), plan.highlights.map { it.id })
        assertEquals(listOf("m1"), plan.bookmarks.map { it.id })
        assertEquals(2, plan.collections.size)
        assertEquals(3, plan.collectionLinks.size)
        assertEquals(1, plan.skippedOrphanHighlights)
        assertEquals(1, plan.skippedOrphanBookmarks)
    }

    @Test
    fun plan_is_deterministic_for_collection_ids() {
        val first = buildLegacyImportPlan(
            books = listOf(Book(id = "a", title = "A", author = "One", collection = "Fantasy")),
            highlights = emptyList(),
            bookmarks = emptyList()
        )
        val second = buildLegacyImportPlan(
            books = listOf(Book(id = "b", title = "B", author = "Two", collection = "fantasy")),
            highlights = emptyList(),
            bookmarks = emptyList()
        )
        val science = buildLegacyImportPlan(
            books = listOf(Book(id = "c", title = "C", author = "Three", collection = "Science")),
            highlights = emptyList(),
            bookmarks = emptyList()
        )

        assertEquals(first.collections.single().id, second.collections.single().id)
        assertNotEquals(first.collections.single().id, science.collections.single().id)
    }
}
