package com.veilreader.app.data

import android.content.Context
import com.veilreader.app.domain.RagsToRichesStage
import com.veilreader.app.domain.deriveReadingHomeProgression
import com.veilreader.app.domain.highestEarnedRagsStage
import com.veilreader.app.domain.ragsToRichesStageFor

/**
 * High-water mark shares the game preference snapshot used by backup/restore.
 * Only GameRepository's state publisher may reconcile it; Compose must only read it.
 */
class WorldStageStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)

    fun read(): RagsToRichesStage {
        val saved = prefs.getInt(KEY, RagsToRichesStage.WANDERER_CAMP.tier)
        return RagsToRichesStage.entries.firstOrNull { it.tier == saved }
            ?: RagsToRichesStage.WANDERER_CAMP
    }

    /** Idempotent and non-decreasing, even when later balancing changes thresholds. */
    fun reconcile(minutes: Int, finishedBooks: Int): Boolean {
        val eligible = ragsToRichesStageFor(deriveReadingHomeProgression(minutes, finishedBooks).stage)
        val current = read()
        val earned = highestEarnedRagsStage(eligible, current)
        if (earned == current) return true
        return prefs.commitDurably { putInt(KEY, earned.tier) }
    }

    private companion object { const val KEY = "world.highestEarnedTier.v1" }
}
