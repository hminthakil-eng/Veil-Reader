package com.veilreader.app.domain

import org.junit.Assert.*
import org.junit.Test

class GamificationEngineTest {
    @Test
    fun readingXpCombinesPagesAndMinutes() {
        assertEquals(50, GamificationEngine.readingXp(pages = 10, minutes = 10))
    }

    @Test
    fun negativeReadingCannotFarmNegativeXp() {
        assertEquals(0, GamificationEngine.readingXp(pages = -10, minutes = -5))
    }

    @Test
    fun `path directives use the Path's real ritual-shaped behavior`() {
        val oracle = GamificationEngine.pathDirective(
            pathId = "oracle",
            todayMinutes = 99,
            todayPages = 99,
            todayHighlights = 2,
            todayNotes = 9,
            todayNightMinutes = 99
        )
        val archivist = GamificationEngine.pathDirective(
            pathId = "archivist",
            todayMinutes = 99,
            todayPages = 99,
            todayHighlights = 99,
            todayNotes = 1,
            todayNightMinutes = 99
        )
        val nocturne = GamificationEngine.pathDirective(
            pathId = "nocturne",
            todayMinutes = 99,
            todayPages = 99,
            todayHighlights = 99,
            todayNotes = 99,
            todayNightMinutes = 7
        )

        assertEquals(2, oracle.progress)
        assertEquals(3, oracle.target)
        assertEquals(1, archivist.progress)
        assertEquals(1, archivist.target)
        assertEquals(7, nocturne.progress)
        assertEquals(10, nocturne.target)
    }

    @Test
    fun `path directive counters fail closed for negative data`() {
        val directive = GamificationEngine.pathDirective(
            pathId = "vanguard",
            todayMinutes = -1,
            todayPages = -10,
            todayHighlights = -2,
            todayNotes = -3,
            todayNightMinutes = -4
        )

        assertEquals(0, directive.progress)
        assertTrue(directive.target > 0)
    }

    @Test
    fun `mastery requires embodiment insight and stability instead of XP alone`() {
        val path = com.veilreader.app.data.SampleData.paths.first()
        val incomplete = ReaderProfile(
            level = 99,
            xp = 99999,
            xpForNextLevel = 1,
            streakDays = 0,
            pagesRead = 0,
            minutesRead = 0,
            booksFinished = 0,
            path = path,
            rankIndex = 0,
            ritualProgress = 5,
            ritualTarget = 5,
            pathMastery = PathMasterySnapshot(
                embodiment = PathMasteryAxis(5, 5),
                insight = PathMasteryAxis(1, 3),
                stability = PathMasteryAxis(3, 3),
                dissonance = 66
            )
        )
        val ready = incomplete.copy(
            pathMastery = PathMasterySnapshot(
                embodiment = PathMasteryAxis(5, 5),
                insight = PathMasteryAxis(3, 3),
                stability = PathMasteryAxis(3, 3),
                dissonance = 0
            )
        )

        assertFalse(GamificationEngine.canAdvanceRank(incomplete))
        assertTrue(GamificationEngine.canAdvanceRank(ready))
    }

    @Test
    fun `path mastery makes imbalance visible without deleting progress`() {
        val mastery = derivePathMastery(
            pathId = "oracle",
            rankIndex = 0,
            embodimentValue = 5,
            embodimentTarget = 5,
            totalHighlights = 0,
            substantialNotes = 0,
            pagesRead = 0,
            minutesRead = 0,
            booksFinished = 0,
            readingDays = 0
        )

        assertTrue(mastery.embodiment.ready)
        assertFalse(mastery.insight.ready)
        assertFalse(mastery.stability.ready)
        assertTrue(mastery.dissonance > 0)
        assertFalse(mastery.ritualReady)
    }

    @Test
    fun `higher Path ranks require deeper secondary mastery`() {
        val low = derivePathMastery(
            pathId = "archivist",
            rankIndex = 0,
            embodimentValue = 3,
            embodimentTarget = 3,
            totalHighlights = 4,
            substantialNotes = 2,
            pagesRead = 200,
            minutesRead = 180,
            booksFinished = 1,
            readingDays = 3
        )
        val high = derivePathMastery(
            pathId = "archivist",
            rankIndex = 4,
            embodimentValue = 7,
            embodimentTarget = 7,
            totalHighlights = 4,
            substantialNotes = 2,
            pagesRead = 200,
            minutesRead = 180,
            booksFinished = 1,
            readingDays = 3
        )

        assertTrue(high.insight.target > low.insight.target)
        assertTrue(high.stability.target > low.stability.target)
    }

    @Test
    fun `all six canonical Paths expose distinct doctrines`() {
        val doctrines = listOf(
            "oracle",
            "dreamwalker",
            "archivist",
            "vanguard",
            "nocturne",
            "artificer"
        ).map(::pathDoctrineFor)

        assertEquals(6, doctrines.map { it.maxim }.distinct().size)
        assertEquals(6, doctrines.map { it.embodimentName }.distinct().size)
    }

    @Test
    fun firstLevelThresholdWorks() {
        assertEquals(1, GamificationEngine.levelFor(349))
        assertEquals(2, GamificationEngine.levelFor(350))
    }
}
