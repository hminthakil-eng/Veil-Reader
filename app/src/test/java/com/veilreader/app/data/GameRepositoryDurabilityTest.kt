package com.veilreader.app.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameRepositoryDurabilityTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        gamePrefs().edit().clear().commit()
    }

    @After
    fun tearDown() {
        gamePrefs().edit().clear().commit()
    }

    @Test
    fun `background durability barrier preserves the current reading counters`() {
        val repository = GameRepository(context)

        repository.recordReadingMinute()
        assertTrue(repository.flushDurably())

        val prefs = gamePrefs()
        assertEquals(1, prefs.getInt("todayMinutes", 0))
        assertEquals(1, prefs.getInt("minutesRead", 0))
        assertTrue(prefs.getInt("totalXp", 0) > 0)
    }

    @Test
    fun `quest completion persists its claim with awarded xp`() {
        val repository = GameRepository(context)
        repository.setDailyGoal(5)

        repeat(5) { repository.recordReadingMinute() }
        assertTrue(repository.flushDurably())

        val prefs = gamePrefs()
        val claimed = prefs.getStringSet("claimedQuestIds", emptySet()).orEmpty()
        assertTrue(claimed.isNotEmpty())
        assertTrue(prefs.getInt("totalXp", 0) > 0)
    }

    @Test
    fun `earned discoveries remain historical after live conditions fall`() {
        val prefs = gamePrefs()
        prefs.edit()
            .putInt("minutesRead", 600)
            .putInt("streakDays", 7)
            .putString("lastReadDate", java.time.LocalDate.now().toString())
            .putStringSet("earnedSigils", setOf("seven_days"))
            .commit()

        val repository = GameRepository(context)
        assertTrue("patient_flame" in repository.profile.value.earnedDiscoveries)

        prefs.edit()
            .putInt("minutesRead", 0)
            .putInt("streakDays", 0)
            .putString("lastReadDate", "2000-01-01")
            .commit()
        repository.refresh()

        assertTrue("patient_flame" in repository.profile.value.earnedDiscoveries)
    }

    private fun gamePrefs() =
        context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
}
