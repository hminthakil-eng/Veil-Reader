package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThresholdModelTest {

    @Test
    fun `hero prefers most recently opened unfinished book`() {
        val olderUnfinished = book(
            id = "older",
            lastOpened = 100L,
            added = 10L,
            finished = false
        )
        val newerUnfinished = book(
            id = "newer",
            lastOpened = 300L,
            added = 20L,
            finished = false
        )
        val newestFinished = book(
            id = "finished",
            lastOpened = 500L,
            added = 30L,
            finished = true
        )

        val snapshot = buildThresholdSnapshot(
            listOf(olderUnfinished, newestFinished, newerUnfinished)
        )

        assertEquals("newer", snapshot.hero?.id)
    }

    @Test
    fun `hero falls back to most recent book when all books are finished`() {
        val older = book(id = "older", lastOpened = 100L, added = 10L, finished = true)
        val newer = book(id = "newer", lastOpened = 0L, added = 900L, finished = true)

        val snapshot = buildThresholdSnapshot(listOf(older, newer))

        assertEquals("newer", snapshot.hero?.id)
    }

    @Test
    fun `recent books exclude hero sort by recency and respect limit`() {
        val hero = book(id = "hero", lastOpened = 1000L, added = 1L)
        val second = book(id = "second", lastOpened = 900L, added = 2L)
        val third = book(id = "third", lastOpened = 0L, added = 800L)
        val fourth = book(id = "fourth", lastOpened = 700L, added = 3L)

        val snapshot = buildThresholdSnapshot(
            books = listOf(third, hero, fourth, second),
            recentLimit = 2
        )

        assertEquals(listOf("second", "third"), snapshot.recent.map { it.id })
    }

    @Test
    fun `empty library produces no hero and no recent books`() {
        val snapshot = buildThresholdSnapshot(emptyList())

        assertNull(snapshot.hero)
        assertEquals(emptyList<Book>(), snapshot.recent)
    }

    private fun book(
        id: String,
        lastOpened: Long,
        added: Long,
        finished: Boolean = false
    ) = Book(
        id = id,
        title = id,
        author = "Author",
        lastOpenedAtEpochMs = lastOpened,
        addedAtEpochMs = added,
        finished = finished
    )
}
