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
    fun firstLevelThresholdWorks() {
        assertEquals(1, GamificationEngine.levelFor(349))
        assertEquals(2, GamificationEngine.levelFor(350))
    }
}
