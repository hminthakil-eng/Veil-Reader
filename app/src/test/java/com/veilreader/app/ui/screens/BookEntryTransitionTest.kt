package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookEntryTransitionTest {
    @Test
    fun `new volumes enter without invented memory`() {
        val memory = bookEntryMemory(
            Book(
                id = "new",
                title = "First Door",
                author = "Veil"
            )
        )

        assertFalse(memory.returning)
        assertEquals(0, memory.progressPercent)
        assertEquals(BookEntryMemoryKind.FIRST_ENTRY, memory.kind)
    }

    @Test
    fun `returning volume reports persisted chapter and progress`() {
        val memory = bookEntryMemory(
            Book(
                id = "return",
                title = "Known Door",
                author = "Veil",
                progress = 0.42f,
                currentChapter = "Chapter VII",
                locatorJson = "{saved}"
            )
        )

        assertTrue(memory.returning)
        assertEquals(42, memory.progressPercent)
        assertEquals("Chapter VII", memory.chapter)
        assertEquals(BookEntryMemoryKind.RETURNING_PROGRESS, memory.kind)
    }

    @Test
    fun `continuity stays semantic until the UI formats it`() {
        val hour = 60L * 60L * 1000L
        val continuity = com.veilreader.app.domain.ReadingContinuitySummary(
            priorSessionCount = 3,
            totalActiveMillis = 95L * 60_000L,
            pacedPageTurns = 40,
            recordedHighlightEvents = 2,
            recordedNoteEvents = 1,
            firstSessionAtEpochMs = 1L,
            latestSessionAtEpochMs = 2L,
            returnGapMillis = 5L * hour,
            hasHistory = true
        )

        val memory = bookEntryMemory(
            Book(
                id = "returning",
                title = "Return",
                author = "Veil",
                progress = 0.2f
            ),
            continuity
        )

        assertEquals(BookEntryMemoryKind.RETURNING_PROGRESS, memory.kind)
        assertEquals(5L * hour, memory.returnGapMillis)
        assertEquals(3, memory.priorSessionCount)
        assertEquals(95L * 60_000L, memory.totalActiveMillis)
    }
    @Test
    fun `completed volume has a distinct return state`() {
        val memory = bookEntryMemory(
            Book(
                id = "finished",
                title = "Closed Circle",
                author = "Veil",
                progress = 1f,
                finished = true
            )
        )

        assertTrue(memory.returning)
        assertEquals(BookEntryMemoryKind.COMPLETED_RETURN, memory.kind)
    }
}
