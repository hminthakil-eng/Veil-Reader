package com.veilreader.app.domain

import org.junit.Assert.*
import org.junit.Test

class EstateCampaignTest {
    @Test fun newReaderStartsWithAShackAndCannotSpendUnearnedMaterials() {
        val state = EstateState()
        assertEquals("Wayside shack", EstateCampaign.stages[state.stage].name)
        assertEquals(0, EstateCampaign.balance(state, 0))
        assertNull(EstateCampaign.build(state, 14))
        assertNull(EstateCampaign.claim(state, 4, "first_light"))
    }

    @Test fun chapterRewardsArePermanentUniqueAndOrdered() {
        val first = EstateCampaign.claim(EstateState(), 5, "first_light")!!
        assertEquals(10, EstateCampaign.balance(first, 5))
        assertNull(EstateCampaign.claim(first, 5, "first_light"))
        assertNull(EstateCampaign.claim(EstateState(), 1200, "beacon"))
        assertNull(EstateCampaign.claim(first, 1200, "unknown"))
    }

    @Test fun constructionSpendsOnceAndNeverSkipsAStage() {
        val cottage = EstateCampaign.build(EstateState(), 15)!!
        assertEquals(1, cottage.stage)
        assertEquals(15, cottage.spent)
        assertEquals(0, EstateCampaign.balance(cottage, 15))
        assertNull(EstateCampaign.build(cottage, 15))
        assertNull(EstateCampaign.build(cottage.copy(spent = 60), 60))
    }

    @Test fun theWholeCampaignIsReachableWithReadingAloneAndNoAnnotations() {
        var state = EstateState()
        // Every construction and chapter must be reachable, including the last gate.
        for (minutes in 0..1200) {
            EstateCampaign.chapters.forEach { chapter ->
                state = EstateCampaign.claim(state, minutes, chapter.id) ?: state
            }
            state = EstateCampaign.build(state, minutes) ?: state
            assertTrue(EstateCampaign.balance(state, minutes) >= 0)
        }
        EstateCampaign.chapters.forEach { state = EstateCampaign.claim(state, 1200, it.id) ?: state }
        assertEquals(6, state.stage)
        assertEquals(EstateCampaign.chapters.size, state.claimedChapters.size)
        assertNull(EstateCampaign.build(state, 1200))
    }

    @Test fun cosmeticsAndLoreRespectEarnedUnlocks() {
        assertFalse(EstateCampaign.groundsUnlocked(EstateGrounds.FOUNTAIN, 2))
        assertTrue(EstateCampaign.groundsUnlocked(EstateGrounds.FOUNTAIN, 3))
        val mara = EstateCampaign.residents.first()
        val newReaderDialogue = EstateCampaign.dialogue(mara, EstateState(), "mystery")
        assertFalse(newReaderDialogue.contains("The gate has no lock"))
        assertEquals(0, EstateCampaign.balance(EstateState(spent = 100), -5))
    }
}
