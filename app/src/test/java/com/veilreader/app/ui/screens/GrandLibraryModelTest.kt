package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrandLibraryModelTest {

    @Test
    fun `smart shelves are local deterministic and skip sample placeholders`() {
        val books = listOf(
            book("sample", added = 999L, sourceUri = null),
            book("active-old", added = 10L, opened = 100L, progress = 0.25f),
            book("active-new", added = 20L, opened = 300L, progress = 0.50f, favorite = true),
            book("finished", added = 30L, opened = 200L, progress = 1f, finished = true),
            book("new", added = 500L)
        )

        val shelves = buildSmartShelves(books, shelfBookLimit = 10)

        assertEquals(listOf("active-new", "active-old"), shelves.byId("continue-reading").books.map { it.id })
        assertEquals("new", shelves.byId("recently-added").books.first().id)
        assertEquals(listOf("finished"), shelves.byId("finished").books.map { it.id })
        assertEquals(listOf("active-new"), shelves.byId("favorites").books.map { it.id })
        assertFalse(shelves.flatMap { it.books }.any { it.id == "sample" })
    }

    @Test
    fun `series shelf orders by series then index then title`() {
        val books = listOf(
            book("beta-two", added = 1L, series = "Beta", seriesIndex = 2.0),
            book("alpha-two", added = 2L, series = "Alpha", seriesIndex = 2.0),
            book("alpha-one", added = 3L, series = "Alpha", seriesIndex = 1.0),
            book("plain", added = 4L)
        )

        val series = buildSmartShelves(books).byId("series")

        assertEquals(listOf("alpha-one", "alpha-two", "beta-two"), series.books.map { it.id })
    }

    @Test
    fun `collections become case insensitive shelves without duplicates`() {
        val books = listOf(
            book("one", added = 10L, collections = listOf("Fantasy", "Favorites")),
            book("two", added = 20L, collections = listOf("fantasy")),
            book("three", added = 30L, collections = listOf("Science"))
        )

        val shelves = buildSmartShelves(books)
        val collectionShelves = shelves.filter { it.kind == SmartShelfKind.COLLECTION }

        assertEquals(listOf("Favorites", "Fantasy", "Science"), collectionShelves.map { it.title })
        assertEquals(listOf("two", "one"), shelves.byId("collection:fantasy").books.map { it.id })
    }

    @Test
    fun `empty library produces no smart shelves`() {
        assertTrue(buildSmartShelves(emptyList()).isEmpty())
    }

    private fun List<SmartShelf>.byId(id: String): SmartShelf = first { it.id == id }

    private fun book(
        id: String,
        added: Long,
        opened: Long = 0L,
        progress: Float = 0f,
        finished: Boolean = false,
        favorite: Boolean = false,
        series: String? = null,
        seriesIndex: Double? = null,
        collections: List<String> = emptyList(),
        sourceUri: String? = "content://$id"
    ) = Book(
        id = id,
        title = id,
        author = "Author",
        progress = progress,
        finished = finished,
        favorite = favorite,
        seriesName = series,
        seriesIndex = seriesIndex,
        collections = collections,
        sourceUri = sourceUri,
        addedAtEpochMs = added,
        lastOpenedAtEpochMs = opened
    )
}
