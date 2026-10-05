package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderProfile
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
        assertFalse(emberBookmarkAwakened(ReaderProfile(longestStreakDays = 2)))
        assertTrue(emberBookmarkAwakened(ReaderProfile(longestStreakDays = 3)))
    }
}
