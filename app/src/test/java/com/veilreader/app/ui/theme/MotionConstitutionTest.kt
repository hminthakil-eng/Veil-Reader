package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionConstitutionTest {
    @Test
    fun `motion classes stay inside Cathedral target ranges`() {
        assertEquals(110, motionBudgetFor(CathedralMotionClass.MICRO).targetDurationMs)
        assertEquals(210, motionBudgetFor(CathedralMotionClass.MATERIAL).targetDurationMs)
        assertEquals(360, motionBudgetFor(CathedralMotionClass.SPATIAL).targetDurationMs)
        assertEquals(520, motionBudgetFor(CathedralMotionClass.REALM).targetDurationMs)
        assertEquals(1080, motionBudgetFor(CathedralMotionClass.CEREMONIAL).targetDurationMs)
        assertEquals(16_000, motionBudgetFor(CathedralMotionClass.AMBIENT).targetDurationMs)

        CathedralMotionClass.entries.forEach { motionClass ->
            val budget = motionBudgetFor(motionClass)
            assertTrue(budget.targetDurationMs in budget.minDurationMs..budget.maxDurationMs)
        }
    }

    @Test
    fun `gesture-like classes remain interruptible`() {
        listOf(
            CathedralMotionClass.MICRO,
            CathedralMotionClass.MATERIAL,
            CathedralMotionClass.SPATIAL,
            CathedralMotionClass.REALM,
            CathedralMotionClass.AMBIENT
        ).forEach { motionClass ->
            assertTrue(motionBudgetFor(motionClass).interruptible)
        }
        assertFalse(motionBudgetFor(CathedralMotionClass.CEREMONIAL).interruptible)
    }

    @Test
    fun `reduced motion removes ambient loops and shortens all active choreography`() {
        assertEquals(0, effectiveMotionDurationMs(CathedralMotionClass.AMBIENT, reducedMotion = true))
        CathedralMotionClass.entries
            .filter { it != CathedralMotionClass.AMBIENT }
            .forEach { motionClass ->
                assertTrue(
                    effectiveMotionDurationMs(motionClass, reducedMotion = true) <
                        effectiveMotionDurationMs(motionClass, reducedMotion = false)
                )
            }
    }

    @Test
    fun `animation can never become state ownership`() {
        CathedralMotionClass.entries.forEach { motionClass ->
            assertFalse(motionMayCarryState(motionClass))
        }
    }
}
