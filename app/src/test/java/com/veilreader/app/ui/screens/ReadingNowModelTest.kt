package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingNowModelTest {
    @Test
    fun `current book prefers most recently opened unfinished book`() {
        val books = listOf(
            book(id = "finished-newer", lastOpened = 900L, finished = true),
            book(id = "unfinished-older", lastOpened = 600L),
            book(id = "unfinished-newer", lastOpened = 800L)
        )

        assertEquals("unfinished-newer", selectCurrentBook(books)?.id)
    }

    @Test
    fun `current book falls back to newest book when every book is finished`() {
        val books = listOf(
            book(id = "older", lastOpened = 100L, addedAt = 50L, finished = true),
            book(id = "newer", lastOpened = 0L, addedAt = 300L, finished = true)
        )

        assertEquals("newer", selectCurrentBook(books)?.id)
    }

    @Test
    fun `recent shelf excludes current book and sorts by meaningful activity`() {
        val books = listOf(
            book(id = "current", lastOpened = 900L),
            book(id = "recent-open", lastOpened = 800L),
            book(id = "recent-add", lastOpened = 0L, addedAt = 700L),
            book(id = "older", lastOpened = 500L),
            book(id = "oldest", lastOpened = 100L)
        )

        val ids = recentBooks(books, excludingBookId = "current", limit = 3).map { it.id }

        assertEquals(listOf("recent-open", "recent-add", "older"), ids)
        assertTrue("current" !in ids)
    }

    private fun book(
        id: String,
        lastOpened: Long = 0L,
        addedAt: Long = 1L,
        finished: Boolean = false
    ) = Book(
        id = id,
        title = id,
        author = "Author",
        sourceUri = "file://$id.epub",
        lastOpenedAtEpochMs = lastOpened,
        addedAtEpochMs = addedAt,
        finished = finished
    )
}
