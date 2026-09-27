package com.veilreader.app.ui.screens

import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancementCeremonyPolicyTest {
    private val profile = ReaderProfile(
        level = 3, xp = 100, xpForNextLevel = 600, streakDays = 2,
        pagesRead = 50, minutesRead = 100, booksFinished = 1,
        path = ReadingPath("oracle", "Oracle", "Sight", "Test path", listOf("A", "B", "C")),
        rankIndex = 0, ritualProgress = 10, ritualTarget = 10
    )

    @Test
    fun eligible_request_advances_only_its_frozen_rank() {
        assertTrue(GamificationEngine.canAdvanceRank(profile, "oracle", 0))
        // Even if the next ritual is already eligible, replaying the old request cannot advance it.
        val advanced = profile.copy(rankIndex = 1)
        assertTrue(GamificationEngine.canAdvanceRank(advanced))
        assertFalse(GamificationEngine.canAdvanceRank(advanced, "oracle", 0))
    }

    @Test
    fun changing_path_invalidates_an_open_confirmation() {
        val changed = profile.copy(path = profile.path.copy(id = "archivist"))
        assertTrue(GamificationEngine.canAdvanceRank(changed))
        assertFalse(GamificationEngine.canAdvanceRank(changed, "oracle", 0))
    }

    @Test
    fun matching_snapshot_never_bypasses_ritual_requirements() {
        assertFalse(GamificationEngine.canAdvanceRank(profile.copy(ritualProgress = 9), "oracle", 0))
        assertFalse(GamificationEngine.canAdvanceRank(profile.copy(rankIndex = 2), "oracle", 2))
    }

    @Test
    fun submission_guard_stays_closed_through_seal_success_and_rejection() {
        assertTrue(canConfirmAdvancementCeremony(AdvancementCeremonyStage.INVOCATION))
        listOf(AdvancementCeremonyStage.SEALING, AdvancementCeremonyStage.REVEALED,
            AdvancementCeremonyStage.REJECTED).forEach { assertFalse(canConfirmAdvancementCeremony(it)) }
    }

    @Test
    fun rejection_and_success_are_dismissible_but_sealing_is_not() {
        assertFalse(canDismissAdvancementCeremony(AdvancementCeremonyStage.SEALING))
        assertTrue(canDismissAdvancementCeremony(AdvancementCeremonyStage.INVOCATION))
        assertTrue(canDismissAdvancementCeremony(AdvancementCeremonyStage.REVEALED))
        assertTrue(canDismissAdvancementCeremony(AdvancementCeremonyStage.REJECTED))
    }

    @Test
    fun timer_completion_without_profile_advancement_never_reveals_success() {
        assertEquals(AdvancementCeremonyStage.REJECTED, advancementStageAfterSeal("oracle", 0, "oracle", 0))
    }

    @Test
    fun restored_presentation_cannot_override_a_rolled_back_profile() {
        assertEquals(AdvancementCeremonyStage.REJECTED, advancementStageAfterSeal("oracle", 1, "oracle", 0))
        assertEquals(AdvancementCeremonyStage.REJECTED, advancementStageAfterSeal("oracle", 0, "archivist", 1))
    }

    @Test
    fun confirmed_rank_reveals_the_original_target() {
        assertEquals(AdvancementCeremonyStage.REVEALED, advancementStageAfterSeal("oracle", 0, "oracle", 1))
        // Later profile updates do not invalidate an already achieved target.
        assertEquals(AdvancementCeremonyStage.REVEALED, advancementStageAfterSeal("oracle", 0, "oracle", 2))
    }
}
