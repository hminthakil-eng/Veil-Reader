package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryWingsTest {
    @Test
    fun `collections and series become separate factual wings`() {
        val books = listOf(
            Book(
                id = "a",
                title = "A",
                author = "Veil",
                collections = listOf("Mystery"),
                seriesName = "Gray Cycle"
            ),
            Book(
                id = "b",
                title = "B",
                author = "Veil",
                collections = listOf("Mystery", "Favorites Shelf"),
                seriesName = "Gray Cycle"
            )
        )

        val state = deriveLibraryWings(books)

        assertEquals(2, state.collectionWings.size)
        assertEquals(1, state.seriesWings.size)
        assertEquals(2, state.collectionWings.first { it.name == "Mystery" }.volumeCount)
        assertEquals(2, state.seriesWings.single().volumeCount)
    }

    @Test
    fun `case variants merge without rewriting display metadata`() {
        val books = listOf(
            Book(id = "a", title = "A", author = "Veil", collections = listOf("Archive")),
            Book(id = "b", title = "B", author = "Veil", collections = listOf("archive"))
        )

        val state = deriveLibraryWings(books)

        assertEquals(1, state.collectionWings.size)
        assertEquals("Archive", state.collectionWings.single().name)
        assertEquals(listOf("a", "b"), state.collectionWings.single().volumeIds)
    }

    @Test
    fun `wing presence comes only from member book state`() {
        val wing = deriveLibraryWings(
            listOf(
                Book(
                    id = "a",
                    title = "A",
                    author = "Veil",
                    collections = listOf("Shelf"),
                    progress = 1f,
                    finished = true,
                    favorite = true
                ),
                Book(
                    id = "b",
                    title = "B",
                    author = "Veil",
                    collections = listOf("Shelf"),
                    progress = 0.5f
                )
            )
        ).collectionWings.single()

        assertEquals(2, wing.volumeCount)
        assertEquals(1, wing.completedCount)
        assertEquals(1, wing.activeCount)
        assertEquals(1, wing.favoriteCount)
        assertTrue(wing.archivePresence > 0f)
        assertTrue(wing.archivePresence <= 1f)
    }

    @Test
    fun `books without collection or series create no fake wings`() {
        val state = deriveLibraryWings(
            listOf(Book(id = "a", title = "A", author = "Veil"))
        )

        assertTrue(state.collectionWings.isEmpty())
        assertTrue(state.seriesWings.isEmpty())
        assertTrue(state.allWings.isEmpty())
    }

    @Test
    fun `recorded reading activity outranks later import metadata`() {
        val state = deriveLibraryWings(
            listOf(
                Book(
                    id = "older-import",
                    title = "Older import",
                    author = "Veil",
                    collections = listOf("Read first"),
                    addedAtEpochMs = 10_000L,
                    lastOpenedAtEpochMs = 20_000L
                ),
                Book(
                    id = "newer-import",
                    title = "Newer import",
                    author = "Veil",
                    collections = listOf("Imported later"),
                    addedAtEpochMs = 30_000L,
                    lastOpenedAtEpochMs = 0L
                )
            ),
            maxCollectionWings = 2,
            maxSeriesWings = 0
        )

        assertEquals("Read first", state.collectionWings.first().name)
        assertEquals(20_000L, state.collectionWings.first().lastRecordedActivityAtEpochMs)
    }

    @Test
    fun `wing limits are deterministic and bounded`() {
        val books = (1..12).map { index ->
            Book(
                id = "b$index",
                title = "B$index",
                author = "Veil",
                collections = listOf("Shelf $index"),
                addedAtEpochMs = 0L,
                lastOpenedAtEpochMs = index.toLong()
            )
        }

        val state = deriveLibraryWings(
            books = books,
            maxCollectionWings = 4,
            maxSeriesWings = 0
        )

        assertEquals(4, state.collectionWings.size)
        assertFalse(state.collectionWings.any { it.name == "Shelf 1" })
        assertEquals("Shelf 12", state.collectionWings.first().name)
    }
}
