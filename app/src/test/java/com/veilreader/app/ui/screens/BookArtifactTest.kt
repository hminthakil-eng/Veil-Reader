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
}
