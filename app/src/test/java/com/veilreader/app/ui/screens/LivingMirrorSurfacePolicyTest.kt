package com.veilreader.app.ui.screens

import com.veilreader.app.domain.LivingMirrorNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingMirrorSurfacePolicyTest {
    @Test
    fun `summoning promotes matches into the bounded spatial surface`() {
        val notes = (0 until 40).map(::note)
        val matches = setOf("h35", "h36", "h37", "h38", "h39")

        val visible = selectLivingMirrorSurfaceNotes(
            notes = notes,
            matches = matches,
            queryActive = true,
            maxNodes = 28
        )

        assertEquals(28, visible.size)
        assertEquals(
            listOf("h35", "h36", "h37", "h38", "h39"),
            visible.take(5).map { it.highlightId }
        )
        assertTrue(matches.all { id -> visible.any { it.highlightId == id } })
    }

    @Test
    fun `without a query the mirror preserves factual proximity ordering`() {
        val notes = (0 until 40).map(::note)

        val visible = selectLivingMirrorSurfaceNotes(
            notes = notes,
            matches = emptySet(),
            queryActive = false,
            maxNodes = 28
        )

        assertEquals(notes.take(28), visible)
    }

    @Test
    fun `summoning clears the glass fog without erasing material presence`() {
        val resting = livingMirrorFogAlpha(noteCount = 8, queryActive = false)
        val summoned = livingMirrorFogAlpha(noteCount = 8, queryActive = true)

        assertTrue(resting > summoned)
        assertTrue(resting <= 0.30f)
        assertTrue(summoned >= 0.07f)
    }

    @Test
    fun `denser factual memory clears some resting fog`() {
        val sparse = livingMirrorFogAlpha(noteCount = 1, queryActive = false)
        val dense = livingMirrorFogAlpha(noteCount = 40, queryActive = false)

        assertTrue(dense < sparse)
    }

    @Test
    fun `surface budget is explicit and can be disabled safely`() {
        val notes = (0 until 4).map(::note)

        assertTrue(
            selectLivingMirrorSurfaceNotes(
                notes = notes,
                matches = emptySet(),
                queryActive = false,
                maxNodes = 0
            ).isEmpty()
        )
    }

    private fun note(index: Int): LivingMirrorNote =
        LivingMirrorNote(
            highlightId = "h$index",
            bookId = "b$index",
            bookTitle = "Book $index",
            bookAuthor = "Author",
            quote = "Quote",
            note = "Note",
            locatorJson = "locator",
            recordedAtEpochMs = 1_000L + index,
            lastRevisitedAtEpochMs = null,
            revisitCount = 0,
            cycleIndex = 1,
            clusterX = 0.5f,
            clusterY = 0.5f,
            depth = index / 40f,
            proximity = 1f - index / 40f,
            ringCount = 1
        )
}
