package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.ui.books.BookPatina
import com.veilreader.app.ui.books.BookReadingState
import com.veilreader.app.ui.books.bookArtifactRecordLabel
import com.veilreader.app.ui.books.bookArtifactState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookArtifactTest {
    private val day = 86_400_000L
    private val now = 200L * day

    @Test
    fun `unopened books remain canonical and do not invent history`() {
        val state = bookArtifactState(
            Book(
                id = "a",
                title = "Untouched",
                author = "Archive",
                addedAtEpochMs = now - 2L * day
            ),
            nowEpochMs = now
        )

        assertEquals(BookReadingState.UNOPENED, state.readingState)
        assertEquals(BookPatina.FRESH, state.patina)
        assertFalse(state.finished)
        assertFalse(state.recentlyOpened)
        assertEquals(0.14f, state.leftPageStack, 0.0001f)
        assertEquals(1f, state.rightPageStack, 0.0001f)
    }

    @Test
    fun `reading progress transfers the canonical page stack`() {
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

        assertEquals(BookReadingState.ACTIVE, state.readingState)
        assertEquals(BookPatina.AGED, state.patina)
        assertEquals(0.64f, state.progress, 0.0001f)
        assertTrue(state.leftPageStack > state.rightPageStack)
        assertTrue(state.recentlyOpened)
        assertTrue(bookArtifactRecordLabel(state).contains("AGED"))
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

        assertEquals(BookReadingState.FINISHED, state.readingState)
        assertEquals(BookPatina.ARCHIVAL, state.patina)
        assertTrue(state.finished)
        assertTrue(state.favorite)
        assertEquals(1f, state.leftPageStack, 0.0001f)
        assertEquals(0.14f, state.rightPageStack, 0.0001f)
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

        assertEquals(BookPatina.FRESH, state.patina)
        assertFalse(state.recentlyOpened)
    }
}
