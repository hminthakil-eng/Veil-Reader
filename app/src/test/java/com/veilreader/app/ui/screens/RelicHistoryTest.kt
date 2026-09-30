package com.veilreader.app.ui.screens

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelicHistoryTest {
    @Test
    fun earnedThreeDayRelicSurvivesAReadingBreak() {
        val profileAfterBreak = SampleData.profile.copy(
            streakDays = 0,
            longestStreakDays = 3
        )
        assertTrue(emberBookmarkAwakened(profileAfterBreak))
        assertFalse(emberBookmarkAwakened(profileAfterBreak.copy(longestStreakDays = 2)))
    }
}
