package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionConstitutionTest {
    @Test
    fun `motion classes stay inside Cathedral target ranges`() {
        assertEquals(110, motionBudgetFor(VeilMotionClass.MICRO).targetDurationMs)
        assertEquals(210, motionBudgetFor(VeilMotionClass.MATERIAL).targetDurationMs)
        assertEquals(360, motionBudgetFor(VeilMotionClass.SPATIAL).targetDurationMs)
        assertEquals(520, motionBudgetFor(VeilMotionClass.REALM).targetDurationMs)
        assertEquals(1080, motionBudgetFor(VeilMotionClass.CEREMONIAL).targetDurationMs)
        assertEquals(16_000, motionBudgetFor(VeilMotionClass.AMBIENT).targetDurationMs)

        VeilMotionClass.entries.forEach { motionClass ->
            val budget = motionBudgetFor(motionClass)
            assertTrue(budget.targetDurationMs in budget.minDurationMs..budget.maxDurationMs)
        }
    }

    @Test
    fun `gesture-like classes remain interruptible`() {
        listOf(
            VeilMotionClass.MICRO,
            VeilMotionClass.MATERIAL,
            VeilMotionClass.SPATIAL,
            VeilMotionClass.REALM,
            VeilMotionClass.AMBIENT
        ).forEach { motionClass ->
            assertTrue(motionBudgetFor(motionClass).interruptible)
        }
        assertFalse(motionBudgetFor(VeilMotionClass.CEREMONIAL).interruptible)
    }

    @Test
    fun `reduced motion removes ambient loops and shortens all active choreography`() {
        assertEquals(0, effectiveMotionDurationMs(VeilMotionClass.AMBIENT, reducedMotion = true))
        VeilMotionClass.entries
            .filter { it != VeilMotionClass.AMBIENT }
            .forEach { motionClass ->
                assertTrue(
                    effectiveMotionDurationMs(motionClass, reducedMotion = true) <
                        effectiveMotionDurationMs(motionClass, reducedMotion = false)
                )
            }
    }

    @Test
    fun `animation can never become state ownership`() {
        VeilMotionClass.entries.forEach { motionClass ->
            assertFalse(motionMayCarryState(motionClass))
        }
    }
}
