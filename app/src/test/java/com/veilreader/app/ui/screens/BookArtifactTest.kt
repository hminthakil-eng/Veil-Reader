package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookArtifactTest {
    private val day = 86_400_000L
    private val now = 200L * day

    @Test
    fun `unopened books remain pristine and do not invent history`() {
        val state = bookArtifactState(
            Book(
                id = "a",
                title = "Untouched",
                author = "Archive",
                addedAtEpochMs = now - 2L * day
            ),
            nowEpochMs = now
        )

        assertEquals(BookPresence.PRISTINE, state.presence)
        assertEquals(BookArchiveAge.NEW, state.archiveAge)
        assertFalse(state.completed)
        assertFalse(state.recentlyOpened)
        assertEquals(0f, state.leftStack, 0.0001f)
        assertEquals(1f, state.rightStack, 0.0001f)
    }

    @Test
    fun `reading progress physically transfers the page stack`() {
        val state = bookArtifactState(
            Book(
                id = "b",
                title = "Halfway",
                author = "Reader",
                progress = 0.64f,
                addedAtEpochMs = now - 80L * day,
                lastOpenedAtEpochMs = now - 2L * 60L * 60L * 1000L
            ),
            nowEpochMs = now
        )

        assertEquals(BookPresence.READING, state.presence)
        assertEquals(BookArchiveAge.AGED, state.archiveAge)
        assertEquals(0.64f, state.leftStack, 0.0001f)
        assertEquals(0.36f, state.rightStack, 0.0001f)
        assertTrue(state.recentlyOpened)
        assertTrue(state.patina > 0.20f)
    }

    @Test
    fun `completed old volumes receive deep archive treatment`() {
        val state = bookArtifactState(
            Book(
                id = "c",
                title = "Long Memory",
                author = "Veil",
                progress = 1f,
                finished = true,
                favorite = true,
                addedAtEpochMs = now - 190L * day,
                lastOpenedAtEpochMs = now - 50L * day
            ),
            nowEpochMs = now
        )

        assertEquals(BookPresence.COMPLETED, state.presence)
        assertEquals(BookArchiveAge.ARCHIVAL, state.archiveAge)
        assertTrue(state.completed)
        assertTrue(state.favorite)
        assertEquals(1f, state.leftStack, 0.0001f)
        assertEquals(0f, state.rightStack, 0.0001f)
        assertTrue(bookArtifactRecordLabel(state).contains("DEEP ARCHIVE"))
    }

    @Test
    fun `future timestamps never create false recent-return state`() {
        val state = bookArtifactState(
            Book(
                id = "d",
                title = "Future",
                author = "Clock",
                addedAtEpochMs = now + day,
                lastOpenedAtEpochMs = now + day
            ),
            nowEpochMs = now
        )

        assertEquals(BookArchiveAge.NEW, state.archiveAge)
        assertFalse(state.recentlyOpened)
    }
    @Test
    fun `archive age and recent return agree with the library at boundary dates`() {
        val book = Book(
            id = "boundary",
            title = "Return",
            author = "Archive",
            addedAtEpochMs = now - 50L * day,
            lastOpenedAtEpochMs = now - 40L * 60L * 60L * 1000L
        )
        val cover = bookArtifactState(book, nowEpochMs = now)
        val library = com.veilreader.app.ui.books.bookArtifactState(book, nowEpochMs = now)

        assertEquals(BookArchiveAge.AGED, cover.archiveAge)
        assertEquals(com.veilreader.app.ui.books.BookPatina.AGED, library.patina)
        assertEquals(BookPresence.OPENED, cover.presence)
        assertEquals(library.recentlyOpened, cover.recentlyOpened)
        assertTrue(cover.recentlyOpened)
    }

    @Test
    fun `finished favorite gets full progress and additive patina on every cover`() {
        val book = Book(
            id = "sealed",
            title = "Sealed",
            author = "Archive",
            progress = 0.64f,
            finished = true,
            favorite = true,
            addedAtEpochMs = now - 2L * day
        )
        val cover = bookArtifactState(book, nowEpochMs = now)
        val library = com.veilreader.app.ui.books.bookArtifactState(book, nowEpochMs = now)

        assertEquals(library.progress, cover.progress, 0f)
        assertEquals(1f, cover.leftStack, 0f)
        assertEquals(0f, cover.rightStack, 0f)
        assertEquals(0.33f, cover.patina, 0.0001f)
        assertTrue(cover.completed)
    }

}
