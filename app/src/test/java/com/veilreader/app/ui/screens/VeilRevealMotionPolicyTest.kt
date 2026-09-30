package com.veilreader.app.ui.screens

import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilRealm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilRevealMotionPolicyTest {
    @Test
    fun `Threshold and Archive reveal faster than world realms`() {
        val threshold = veilRevealDurationFor(VeilRealm.THRESHOLD)
        val archive = veilRevealDurationFor(VeilRealm.ARCHIVE)
        val castle = veilRevealDurationFor(VeilRealm.CASTLE)

        assertEquals(VeilMotion.FUNCTIONAL_MS, threshold)
        assertEquals(VeilMotion.FUNCTIONAL_MS, archive)
        assertTrue(threshold < castle)
    }

    @Test
    fun `Sanctuary keeps the shortest non reduced reveal`() {
        val sanctuary = veilRevealDurationFor(VeilRealm.SANCTUARY)
        val threshold = veilRevealDurationFor(VeilRealm.THRESHOLD)

        assertEquals(VeilMotion.FUNCTIONAL_ENTER_MS, sanctuary)
        assertTrue(sanctuary < threshold)
    }

    @Test
    fun `rich realms retain spatial reveal duration`() {
        listOf(
            VeilRealm.CASTLE,
            VeilRealm.WORLD,
            VeilRealm.RITUAL,
            VeilRealm.SANCTUM
        ).forEach { realm ->
            assertEquals(VeilMotion.SPATIAL_MS, veilRevealDurationFor(realm))
        }
    }
}
