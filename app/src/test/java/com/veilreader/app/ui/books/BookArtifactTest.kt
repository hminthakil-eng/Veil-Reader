package com.veilreader.app.ui.books

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookArtifactTest {
    private val day = 86_400_000L
    private val now = 220L * day

    @Test
    fun `invalid progress never contaminates artifact geometry`() {
        for (progress in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val state = bookArtifactState(
                Book(id = "invalid", title = "Invalid", author = "", progress = progress),
                nowEpochMs = now
            )
            assertEquals(0f, state.progress, 0f)
            assertTrue(state.leftPageStack.isFinite())
            assertTrue(state.rightPageStack.isFinite())
            val stack = bookPageStackBalance(progress)
            assertTrue(stack.first.isFinite())
            assertTrue(stack.second.isFinite())
        }
    }

    @Test
    fun `completion remains authoritative even with invalid stored progress`() {
        val state = bookArtifactState(
            Book(id = "sealed", title = "Sealed", author = "", progress = Float.NaN, finished = true),
            nowEpochMs = now
        )
        assertEquals(1f, state.progress, 0f)
        assertEquals(BookReadingState.FINISHED, state.readingState)
    }

    @Test
    fun `page mass transfers from right to left as reading progresses`() {
        val start = bookPageStackBalance(0f)
        val middle = bookPageStackBalance(0.5f)
        val end = bookPageStackBalance(1f)

        assertTrue(start.first < middle.first)
        assertTrue(middle.first < end.first)
        assertTrue(start.second > middle.second)
        assertTrue(middle.second > end.second)
        assertEquals(middle.first, middle.second, 0.0001f)
    }

    @Test
    fun `archive patina comes only from time actually held in the archive`() {
        val fresh = bookArtifactState(
            Book(id = "fresh", title = "Fresh", author = "", addedAtEpochMs = now - 3L * day),
            nowEpochMs = now
        )
        val settled = bookArtifactState(
            Book(id = "settled", title = "Settled", author = "", addedAtEpochMs = now - 12L * day),
            nowEpochMs = now
        )
        val aged = bookArtifactState(
            Book(id = "aged", title = "Aged", author = "", addedAtEpochMs = now - 90L * day),
            nowEpochMs = now
        )
        val archival = bookArtifactState(
            Book(id = "archival", title = "Archival", author = "", addedAtEpochMs = now - 200L * day),
            nowEpochMs = now
        )

        assertEquals(BookPatina.FRESH, fresh.patina)
        assertEquals(BookPatina.SETTLED, settled.patina)
        assertEquals(BookPatina.AGED, aged.patina)
        assertEquals(BookPatina.ARCHIVAL, archival.patina)
    }

    @Test
    fun `recent-return light requires a real recent open event`() {
        val recent = bookArtifactState(
            Book(
                id = "recent",
                title = "Recent",
                author = "",
                lastOpenedAtEpochMs = now - 60L * 60L * 1000L
            ),
            nowEpochMs = now
        )
        val stale = bookArtifactState(
            Book(
                id = "stale",
                title = "Stale",
                author = "",
                lastOpenedAtEpochMs = now - 72L * 60L * 60L * 1000L
            ),
            nowEpochMs = now
        )

        assertTrue(recent.recentlyOpened)
        assertFalse(stale.recentlyOpened)
    }

    @Test
    fun `finished books become sealed artifacts without inventing rereads`() {
        val state = bookArtifactState(
            Book(
                id = "finished",
                title = "Finished",
                author = "",
                progress = 0.64f,
                finished = true,
                favorite = true
            ),
            nowEpochMs = now
        )

        assertEquals(BookReadingState.FINISHED, state.readingState)
        assertEquals(1f, state.progress, 0f)
        assertTrue(state.favorite)
        assertTrue(state.finished)
    }
}

