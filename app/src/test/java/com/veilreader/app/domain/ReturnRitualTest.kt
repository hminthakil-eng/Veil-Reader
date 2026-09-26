package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnRitualTest {
    private val day = 86_400_000L
    private val now = 900L * day

    @Test
    fun `only forgotten depth opens the rare return ritual`() {
        val book = Book(id = "b", title = "Deep Volume", author = "Veil")
        val forgotten = BookArchiveMemory(
            bookId = "b",
            lastRecordedActivityAtEpochMs = now - 240L * day,
            inactiveMillis = 240L * day,
            depth = ArchiveDepth.FORGOTTEN,
            longestReturnGapMillis = null
        )
        val deep = forgotten.copy(
            inactiveMillis = 120L * day,
            depth = ArchiveDepth.DEEP_SHELF
        )

        assertTrue(
            deriveBookReturnRitual(book, forgotten, emptyList(), now) != null
        )
        assertNull(
            deriveBookReturnRitual(book, deep, emptyList(), now)
        )
    }

    @Test
    fun `ritual refuses mismatched archive memory`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val foreign = BookArchiveMemory(
            bookId = "other",
            lastRecordedActivityAtEpochMs = now - 300L * day,
            inactiveMillis = 300L * day,
            depth = ArchiveDepth.FORGOTTEN,
            longestReturnGapMillis = null
        )

        assertNull(deriveBookReturnRitual(book, foreign, emptyList(), now))
    }

    @Test
    fun `latest sufficiently old margin becomes the optional fragment`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val memory = BookArchiveMemory(
            bookId = "b",
            lastRecordedActivityAtEpochMs = now - 220L * day,
            inactiveMillis = 220L * day,
            depth = ArchiveDepth.FORGOTTEN,
            longestReturnGapMillis = null
        )
        val highlights = listOf(
            Highlight(
                id = "old",
                bookId = "b",
                quote = "The first preserved margin",
                locatorJson = "{}",
                createdAtEpochMs = now - 160L * day
            ),
            Highlight(
                id = "newer-old",
                bookId = "b",
                quote = "The nearer preserved margin",
                locatorJson = "{}",
                note = "annotation",
                createdAtEpochMs = now - 100L * day
            ),
            Highlight(
                id = "fresh",
                bookId = "b",
                quote = "Too recent",
                locatorJson = "{}",
                createdAtEpochMs = now - 10L * day
            )
        )

        val ritual = deriveBookReturnRitual(book, memory, highlights, now)!!

        assertEquals("newer-old", ritual.fragment?.highlightId)
        assertTrue(ritual.fragment?.annotated == true)
        assertEquals("7 MONTHS SILENT", ritual.silenceLabel)
    }

    @Test
    fun `ritual never invents a fragment when no old margin exists`() {
        val book = Book(id = "b", title = "B", author = "Veil")
        val memory = BookArchiveMemory(
            bookId = "b",
            lastRecordedActivityAtEpochMs = now - 200L * day,
            inactiveMillis = 200L * day,
            depth = ArchiveDepth.FORGOTTEN,
            longestReturnGapMillis = null
        )
        val ritual = deriveBookReturnRitual(
            book = book,
            archiveMemory = memory,
            highlights = listOf(
                Highlight(
                    id = "fresh",
                    bookId = "b",
                    quote = "Recent passage",
                    locatorJson = "{}",
                    createdAtEpochMs = now - 12L * day
                )
            ),
            nowEpochMs = now
        )!!

        assertNull(ritual.fragment)
    }
}
