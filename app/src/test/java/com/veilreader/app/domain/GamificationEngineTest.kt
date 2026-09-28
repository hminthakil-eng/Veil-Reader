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
    fun firstLevelThresholdWorks() {
        assertEquals(1, GamificationEngine.levelFor(349))
        assertEquals(2, GamificationEngine.levelFor(350))
    }
}
