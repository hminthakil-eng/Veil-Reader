package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastleProgressionPolicyTest {
    @Test
    fun relicRarity_isDeterministicAndDefaultsSafely() {
        assertEquals(RelicRarity.FOUNDATION, relicRarityFor("ember_bookmark"))
        assertEquals(RelicRarity.RESONANT, relicRarityFor("moonlit_lens"))
        assertEquals(RelicRarity.ASCENDANT, relicRarityFor("astral_key"))
        assertEquals(RelicRarity.SOVEREIGN, relicRarityFor("veil_crown"))
        assertEquals(RelicRarity.FOUNDATION, relicRarityFor("unknown"))
    }

    @Test
    fun emberBookmark_requiresThreeDayLongestStreak() {
        assertFalse(emberBookmarkAwakened(profile(longestStreakDays = 2)))
        assertTrue(emberBookmarkAwakened(profile(longestStreakDays = 3)))
    }

    private fun profile(longestStreakDays: Int): ReaderProfile =
        ReaderProfile(
            level = 1,
            xp = 0,
            xpForNextLevel = 100,
            streakDays = longestStreakDays,
            pagesRead = 0,
            minutesRead = 0,
            booksFinished = 0,
            path = ReadingPath(
                id = "test",
                name = "Test",
                epithet = "Test",
                description = "Test",
                ranks = listOf("Rank")
            ),
            rankIndex = 0,
            ritualProgress = 0,
            ritualTarget = 1,
            longestStreakDays = longestStreakDays
        )
}
