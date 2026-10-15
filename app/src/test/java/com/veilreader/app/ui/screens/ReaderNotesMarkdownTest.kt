package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Highlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderNotesMarkdownTest {
    @Test fun exportFiltersOtherBooksAndStreamsAllNotes() {
        val records = listOf(
            Highlight("a", "book-A", "The moon\nwas crimson.", "{}", "A clue\nfor later", createdAtEpochMs = 2),
            Highlight("b", "book-B", "PRIVATE OTHER BOOK", "{}", "hidden", createdAtEpochMs = 1),
            Highlight("c", "book-A", "A second quote", "{}", "", createdAtEpochMs = 3)
        )
        val markdown = StringBuilder()
        val count = writeReaderNotesMarkdown(markdown, "book-A", "The Novel", records)

        assertEquals(2, count)
        assertTrue(markdown.contains("# Veil Reader — The Novel"))
        assertTrue(markdown.contains("> The moon\n> was crimson."))
        assertTrue(markdown.contains("> A clue\n> for later"))
        assertTrue(markdown.contains("A second quote"))
        assertFalse(markdown.contains("PRIVATE OTHER BOOK"))
        assertFalse(markdown.contains("hidden"))
    }

    @Test fun handlesEmptyBookWithoutFabricatingNotes() {
        val markdown = StringBuilder()
        assertEquals(0, writeReaderNotesMarkdown(markdown, "book-A", "A Book", emptyList()))
        assertTrue(markdown.startsWith("# Veil Reader — A Book"))
        assertFalse(markdown.contains("## 1"))
    }
}
