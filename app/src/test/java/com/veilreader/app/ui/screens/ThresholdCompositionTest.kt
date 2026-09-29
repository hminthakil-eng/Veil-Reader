package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.Quest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThresholdCompositionTest {
    @Test
    fun `latest preserved passage wins over reading prompt`() {
        val book = Book(id = "book", title = "The Volume", author = "Author")
        val whisper = deriveThresholdWhisper(
            visibleBooks = listOf(book),
            highlights = listOf(
                Highlight(
                    id = "old",
                    bookId = book.id,
                    quote = "Older passage",
                    locatorJson = "old",
                    createdAtEpochMs = 10L
                ),
                Highlight(
                    id = "new",
                    bookId = book.id,
                    quote = "  A   newer\npassage  ",
                    locatorJson = "new",
                    note = "margin note",
                    createdAtEpochMs = 20L
                )
            ),
            quests = listOf(
                Quest(
                    id = "quest",
                    title = "Read quietly",
                    progress = 0,
                    target = 10,
                    xpReward = 20
                )
            )
        )

        requireNotNull(whisper)
        assertEquals(ThresholdWhisperKind.PRESERVED_PASSAGE, whisper.kind)
        assertEquals("book", whisper.bookId)
        assertEquals("new", whisper.locatorJson)
        assertEquals("A newer passage", whisper.body)
        assertEquals("margin note", whisper.detail)
    }

    @Test
    fun `passages outside the visible threshold are ignored`() {
        val visible = Book(id = "visible", title = "Visible", author = "Author")
        val hidden = Highlight(
            id = "hidden",
            bookId = "other",
            quote = "Do not surface this",
            locatorJson = "locator",
            createdAtEpochMs = 100L
        )

        val whisper = deriveThresholdWhisper(
            visibleBooks = listOf(visible),
            highlights = listOf(hidden),
            quests = listOf(
                Quest(
                    id = "quest",
                    title = "Read ten pages",
                    progress = 2,
                    target = 10,
                    xpReward = 10
                )
            )
        )

        requireNotNull(whisper)
        assertEquals(ThresholdWhisperKind.READING_PROMPT, whisper.kind)
        assertNull(whisper.bookId)
        assertEquals(2, whisper.progress)
        assertEquals(10, whisper.target)
        assertNull(whisper.detail)
    }

    @Test
    fun `completed prompts do not create threshold noise`() {
        val whisper = deriveThresholdWhisper(
            visibleBooks = emptyList(),
            highlights = emptyList(),
            quests = listOf(
                Quest(
                    id = "done",
                    title = "Already complete",
                    progress = 10,
                    target = 10,
                    xpReward = 10
                )
            )
        )

        assertNull(whisper)
    }
}
