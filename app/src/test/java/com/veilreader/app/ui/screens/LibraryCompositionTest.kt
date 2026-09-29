package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCompositionTest {
    @Test
    fun `legacy grid and list preferences migrate to canonical archive modes`() {
        assertEquals(LibraryViewMode.GALLERY, libraryViewModeFromStored("GRID"))
        assertEquals(LibraryViewMode.INDEX, libraryViewModeFromStored("LIST"))
        assertEquals(LibraryViewMode.SHELVES, libraryViewModeFromStored("SHELVES"))
        assertEquals(LibraryViewMode.GALLERY, libraryViewModeFromStored("unknown"))
    }

    @Test
    fun `filtered shelves collapse to one matching shelf`() {
        val all = listOf(
            Book(id = "a", title = "A", author = "Author"),
            Book(id = "b", title = "B", author = "Author")
        )

        val groups = deriveLibraryShelfGroups(
            books = all,
            filtered = listOf(all.first()),
            filterActive = true
        )

        assertEquals(1, groups.size)
        assertEquals("Matching volumes", groups.single().title)
        assertEquals(listOf("a"), groups.single().books.map { it.id })
    }

    @Test
    fun `shelves expose journey collection series author and record semantics`() {
        val books = listOf(
            Book(
                id = "reading",
                title = "Current",
                author = "Shared Author",
                progress = 0.4f,
                collections = listOf("Mystery"),
                seriesName = "Veil",
                seriesIndex = 2.0
            ),
            Book(
                id = "first",
                title = "First",
                author = "Shared Author",
                collections = listOf("Mystery"),
                seriesName = "Veil",
                seriesIndex = 1.0
            ),
            Book(
                id = "done",
                title = "Done",
                author = "Other",
                finished = true
            ),
            Book(
                id = "waiting",
                title = "Waiting",
                author = "Other"
            )
        )

        val groups = deriveLibraryShelfGroups(
            books = books,
            filtered = books,
            filterActive = false
        )

        assertTrue(groups.any { it.eyebrow == "Journey" })
        assertTrue(groups.any { it.eyebrow == "Collection" && it.title == "Mystery" })
        assertTrue(groups.any { it.eyebrow == "Series" && it.title == "Veil" })
        assertTrue(groups.any { it.eyebrow == "Author" && it.title == "Shared Author" })
        assertTrue(groups.any { it.eyebrow == "Record" })
        assertTrue(groups.any { it.eyebrow == "Unopened" })

        val series = groups.first { it.eyebrow == "Series" && it.title == "Veil" }
        assertEquals(listOf("first", "reading"), series.books.map { it.id })
    }

    @Test
    fun `localized shelf labels preserve grouping truth`() {
        val books = listOf(
            Book(id = "reading", title = "Current", author = "Author", progress = 0.5f),
            Book(id = "done", title = "Done", author = "Author", finished = true),
            Book(id = "waiting", title = "Waiting", author = "Other")
        )
        val labels = LibraryShelfLabels(
            journey = "مسیر",
            currentlyReading = "در حال مطالعه",
            author = "نویسنده",
            record = "رکورد",
            completedVolumes = "تمام‌شده",
            unopened = "گشوده‌نشده",
            waitingOnShelf = "در انتظار"
        )

        val groups = deriveLibraryShelfGroups(
            books = books,
            filtered = books,
            filterActive = false,
            labels = labels
        )

        assertTrue(groups.any { it.eyebrow == "مسیر" && it.title == "در حال مطالعه" })
        assertTrue(groups.any { it.eyebrow == "نویسنده" && it.title == "Author" })
        assertTrue(groups.any { it.eyebrow == "رکورد" && it.title == "تمام‌شده" })
        assertTrue(groups.any { it.eyebrow == "گشوده‌نشده" && it.title == "در انتظار" })
        assertEquals(
            setOf("reading", "done", "waiting"),
            groups.flatMap { it.books }.map { it.id }.toSet()
        )
    }

}
