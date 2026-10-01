package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCompositionTest {
    private val labels = LibraryShelfLabels(
        filteredArchive = "Filtered archive",
        matchingVolumes = "Matching volumes",
        journey = "Journey",
        currentlyReading = "Currently reading",
        collection = "Collection",
        series = "Series",
        author = "Author",
        record = "Record",
        completedVolumes = "Completed volumes",
        unopened = "Unopened",
        waitingOnShelf = "Waiting on the shelf"
    )
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
            filterActive = true,
            labels = labels
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
            filterActive = false,
            labels = labels
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
        val labels = this.labels.copy(
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


    @Test
    fun `normalized shelf identity collapses case and whitespace variants`() {
        val first = Book(
            id = "first",
            title = "First",
            author = "Shared Author",
            collections = listOf("Mystery"),
            seriesName = "Veil",
            seriesIndex = 2.0
        )
        val second = Book(
            id = "second",
            title = "Second",
            author = " shared author ",
            collections = listOf(" mystery "),
            seriesName = " veil ",
            seriesIndex = 1.0
        )

        val groups = deriveLibraryShelfGroups(
            books = listOf(first, second),
            filtered = listOf(first, second),
            filterActive = false,
            labels = labels
        )

        val collectionGroups = groups.filter { it.eyebrow == "Collection" }
        val seriesGroups = groups.filter { it.eyebrow == "Series" }
        val authorGroups = groups.filter { it.eyebrow == "Author" }

        assertEquals(1, collectionGroups.size)
        assertEquals(setOf("first", "second"), collectionGroups.single().books.map { it.id }.toSet())

        assertEquals(1, seriesGroups.size)
        assertEquals(listOf("second", "first"), seriesGroups.single().books.map { it.id })

        assertEquals(1, authorGroups.size)
        assertEquals(setOf("first", "second"), authorGroups.single().books.map { it.id }.toSet())
    }

    @Test
    fun `label grouping keeps one copy of each volume`() {
        val book = Book(id = "same", title = "Same", author = "Author")
        val groups = groupLibraryBooksByLabel(
            listOf(
                "Shelf" to book,
                " shelf " to book,
                "SHELF" to book
            )
        )

        assertEquals(1, groups.size)
        assertEquals(listOf("same"), groups.single().books.map { it.id })
    }

}
