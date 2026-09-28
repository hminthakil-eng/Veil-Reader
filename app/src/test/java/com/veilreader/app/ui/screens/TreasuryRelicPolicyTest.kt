package com.veilreader.app.ui.screens

import com.veilreader.app.domain.CastleMemoryState
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import com.veilreader.app.domain.SilentNamesChoice
import com.veilreader.app.domain.SilentNamesEncounter
import com.veilreader.app.domain.SilentNamesMode
import com.veilreader.app.domain.deriveStoryRelics
import com.veilreader.app.domain.WorldMutationKind
import com.veilreader.app.domain.deriveWorldMutationLedger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreasuryRelicPolicyTest {
    private val path = ReadingPath(
        id = "oracle",
        name = "Oracle",
        epithet = "Sight",
        description = "",
        ranks = listOf("I", "II", "III", "IV", "V", "VI")
    )

    private fun profile(
        rank: Int = 0,
        streak: Int = 0,
        minutes: Int = 0,
        pages: Int = 0,
        books: Int = 0,
        sigils: Set<String> = emptySet()
    ) = ReaderProfile(
        level = 1,
        xp = 0,
        xpForNextLevel = 350,
        streakDays = streak,
        pagesRead = pages,
        minutesRead = minutes,
        booksFinished = books,
        path = path,
        rankIndex = rank,
        ritualProgress = 0,
        ritualTarget = 5,
        earnedSigils = sigils
    )

    @Test
    fun `new relic policy prefers durable evidence`() {
        val memory = CastleMemoryState.EMPTY.copy(
            annotationCount = 7,
            scriptoriumLamps = 3
        )
        val ledger = deriveWorldMutationLedger(profile(), memory)
        val state = relicUnlockState("brass_quill", profile(), ledger)

        assertTrue(
            ledger.entries.any { it.kind == WorldMutationKind.SCRIPTORIUM_LIGHT }
        )
        assertTrue(state.awakened)
        assertEquals(RelicProvenance.READING_EVIDENCE, state.provenance)
        assertEquals(7, state.evidenceCount)
    }

    @Test
    fun `legacy profile thresholds never relock an existing relic`() {
        val oldProfile = profile(pages = 600)
        val state = relicUnlockState(
            relicId = "brass_quill",
            profile = oldProfile,
            ledger = com.veilreader.app.domain.WorldMutationLedger.EMPTY
        )

        assertTrue(state.awakened)
        assertEquals(RelicProvenance.LEGACY_PROFILE, state.provenance)
    }

    @Test
    fun `sealed relic exposes evidence progress without pretending unlock`() {
        val state = relicUnlockState(
            relicId = "ivory_bookplate",
            profile = profile(),
            ledger = com.veilreader.app.domain.WorldMutationLedger.EMPTY
        )

        assertFalse(state.awakened)
        assertEquals(0, state.evidenceCount)
        assertEquals(3, state.target)
        assertEquals(RelicProvenance.SEALED, state.provenance)
    }

    @Test
    fun `Treasury story relic registry accepts only projected known records`() {
        val valid = SilentNamesEncounter.resolve(
            pathId = "oracle",
            choice = SilentNamesChoice.FOLLOW_LIGHT,
            mode = SilentNamesMode.STORY,
            dice = null,
            recordedAtEpochMs = 900L
        ).receipt

        val validDisplays = storyRelicDisplayModels(deriveStoryRelics(valid))
        val forgedDisplays = storyRelicDisplayModels(
            deriveStoryRelics(valid.copy(rewardId = "forged_reward"))
        )

        assertEquals(1, validDisplays.size)
        assertEquals(SilentNamesEncounter.REWARD_ID, validDisplays.single().record.relicId)
        assertTrue(forgedDisplays.isEmpty())
        assertTrue(storyRelicDisplayModels(emptyList()).isEmpty())
    }

    @Test
    fun `sovereign crown still requires final Path and all core sigils`() {
        val notReady = relicUnlockState(
            relicId = "veil_crown",
            profile = profile(rank = 5, sigils = setOf("one", "two", "three", "four")),
            ledger = com.veilreader.app.domain.WorldMutationLedger.EMPTY
        )
        val ready = relicUnlockState(
            relicId = "veil_crown",
            profile = profile(
                rank = 5,
                sigils = setOf("one", "two", "three", "four", "five")
            ),
            ledger = com.veilreader.app.domain.WorldMutationLedger.EMPTY
        )

        assertFalse(notReady.awakened)
        assertTrue(ready.awakened)
        assertEquals(RelicProvenance.SOVEREIGN_COMPOSITE, ready.provenance)
    }
}
