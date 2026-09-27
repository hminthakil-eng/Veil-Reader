package com.veilreader.app.ui.screens

import com.veilreader.app.ui.theme.VeilMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancementCeremonyPolicyTest {
    @Test
    fun `only invocation can be dismissed before a seal is committed`() {
        assertTrue(canDismissAdvancementCeremony(AdvancementCeremonyStage.INVOCATION))
        assertFalse(canDismissAdvancementCeremony(AdvancementCeremonyStage.SEALING))
        assertFalse(canDismissAdvancementCeremony(AdvancementCeremonyStage.REVEALED))
    }

    @Test
    fun `reduced motion collapses ceremony timing instead of spatial animation`() {
        val full = advancementCeremonyTiming(reducedMotion = false)
        val reduced = advancementCeremonyTiming(reducedMotion = true)

        assertTrue(full.sealMillis > reduced.sealMillis)
        assertTrue(full.revealHoldMillis > reduced.revealHoldMillis)
        assertEquals(
            VeilMotion.REDUCED_MOTION_FADE_MS.toLong(),
            reduced.sealMillis
        )
    }

    @Test
    fun `ceremony keeps a frozen advancement target`() {
        val snapshot = AdvancementCeremonySnapshot(
            pathId = "archivist",
            pathName = "Archivist",
            pathEpithet = "Keeper of Memory",
            fromRank = "Witness",
            toRank = "Curator",
            rankIndex = 1,
            ritualDescription = "Preserve what the page reveals.",
            invocation = "What is learned deserves a place to remain."
        )

        assertEquals("Witness", snapshot.fromRank)
        assertEquals("Curator", snapshot.toRank)
        assertEquals(1, snapshot.rankIndex)
    }
}
