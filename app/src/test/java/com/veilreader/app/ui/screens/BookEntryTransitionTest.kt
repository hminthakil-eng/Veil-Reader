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
        assertEquals(100, memory.progressPercent)
    }
}
