package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilWorldStateTest {
    private val path = ReadingPath(
        id = "seer",
        name = "Seer",
        epithet = "Watcher",
        description = "A test path",
        ranks = listOf("Awake", "Witness", "Keeper")
    )

    @Test
    fun `reading snapshot contains facts only from durable inputs`() {
        val books = listOf(
            Book(id = "a", title = "A", author = "V", sourceUri = "file://a", progress = 0.5f, pagesRead = 40),
            Book(id = "b", title = "B", author = "V", sourceUri = "file://b", progress = 1f, finished = true)
        )
        val snapshot = deriveReadingWorldSnapshot(
            books = books,
            highlights = listOf(
                Highlight("h1", "a", "quote", "{}", note = "note"),
                Highlight("h2", "b", "quote", "{}")
            ),
            bookmarks = listOf(Bookmark("m1", "a", "mark", "{}")),
            sessions = listOf(
                ReadingSessionSnapshot("s1", "a", 10L, 20L, 10L, 3, 1, 1)
            ),
            readingCycles = emptyList()
        )

        assertEquals(2, snapshot.importedBookCount)
        assertEquals(2, snapshot.startedBookCount)
        assertEquals(1, snapshot.completedBookCount)
        assertEquals(1, snapshot.sessionCount)
        assertEquals(10L, snapshot.totalActiveMillis)
        assertEquals(3, snapshot.pacedPageTurns)
        assertEquals(2, snapshot.highlightCount)
        assertEquals(1, snapshot.noteCount)
        assertEquals(1, snapshot.bookmarkCount)
        assertTrue(snapshot.hasReadingHistory)
    }

    @Test
    fun `progression snapshot is read only projection of profile`() {
        val profile = ReaderProfile(
            level = 4,
            xp = 120,
            xpForNextLevel = 200,
            streakDays = 7,
            pagesRead = 300,
            minutesRead = 500,
            booksFinished = 2,
            path = path,
            rankIndex = 1,
            ritualProgress = 3,
            ritualTarget = 5,
            earnedSigils = setOf("s1", "s2"),
            earnedDiscoveries = setOf("d1")
        )

        val snapshot = profile.toProgressionSnapshot()

        assertEquals("seer", snapshot.pathId)
        assertEquals(1, snapshot.rankIndex)
        assertEquals("Witness", snapshot.rankName)
        assertEquals(2, snapshot.earnedSigilCount)
        assertEquals(1, snapshot.earnedDiscoveryCount)
        assertEquals(0.6f, snapshot.ritualFraction, 0.0001f)
    }

    @Test
    fun `world wakes from either real reading or real progression without mutating either`() {
        val emptyReading = deriveReadingWorldSnapshot(
            books = emptyList(),
            highlights = emptyList(),
            bookmarks = emptyList(),
            sessions = emptyList(),
            readingCycles = emptyList()
        )
        val dormantProfile = ReaderProfile(
            level = 0, xp = 0, xpForNextLevel = 100, streakDays = 0,
            pagesRead = 0, minutesRead = 0, booksFinished = 0,
            path = path, rankIndex = 0, ritualProgress = 0, ritualTarget = 5
        )
        assertFalse(deriveVeilWorldState(emptyReading, dormantProfile).isAwake)

        val progressed = dormantProfile.copy(level = 1)
        assertTrue(deriveVeilWorldState(emptyReading, progressed).isAwake)

        val reading = emptyReading.copy(sessionCount = 1)
        assertTrue(deriveVeilWorldState(reading, dormantProfile).isAwake)
    }

    @Test
    fun `world depth is deterministic and bounded`() {
        val reading = ReadingWorldSnapshot(
            importedBookCount = 20,
            startedBookCount = 20,
            completedBookCount = 20,
            sessionCount = 100,
            totalActiveMillis = Long.MAX_VALUE,
            pacedPageTurns = 10000,
            highlightCount = 100,
            noteCount = 100,
            bookmarkCount = 100,
            rereadCycleCount = 50,
            lastActivityAtEpochMs = 100L
        )
        val profile = ReaderProfile(
            level = 99, xp = 9999, xpForNextLevel = 1, streakDays = 999,
            pagesRead = 99999, minutesRead = 99999, booksFinished = 99,
            path = path, rankIndex = 2, ritualProgress = 999, ritualTarget = 1,
            earnedDiscoveries = (1..100).map { "d$it" }.toSet()
        )

        val world = deriveVeilWorldState(reading, profile)
        assertTrue(world.depth in 0f..1f)
        assertEquals(world.depth, deriveVeilWorldState(reading, profile).depth, 0f)
    }
}
