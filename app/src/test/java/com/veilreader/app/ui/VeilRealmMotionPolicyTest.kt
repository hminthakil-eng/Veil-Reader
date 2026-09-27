package com.veilreader.app.ui

import com.veilreader.app.ui.theme.VeilMotionClass
import com.veilreader.app.ui.theme.effectiveMotionDurationMs
import com.veilreader.app.ui.theme.motionBudgetFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilRealmMotionPolicyTest {
    @Test
    fun `Hall shared-bound keys are canonical and reject blanks`() {
        assertEquals("hall:mirror", hallSharedBoundsKey("mirror"))
        assertEquals("hall:observatory", hallSharedBoundsKey("observatory"))
        assertEquals("hall:treasury", hallSharedBoundsKey("treasury"))
        assertEquals("hall:sanctum", hallSharedBoundsKey("sanctum"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Hall shared-bound key rejects blank route`() {
        hallSharedBoundsKey("   ")
    }

    @Test
    fun `full realm motion uses constitution budgets`() {
        val policy = veilRealmMotionPolicy(reducedMotion = false)

        assertEquals(
            motionBudgetFor(VeilMotionClass.REALM).targetDurationMs,
            policy.enterDurationMs
        )
        assertEquals(
            motionBudgetFor(VeilMotionClass.SPATIAL).targetDurationMs,
            policy.exitDurationMs
        )
        assertEquals(
            motionBudgetFor(VeilMotionClass.SPATIAL).targetDurationMs,
            policy.sharedBoundsDurationMs
        )
        assertTrue(policy.predictiveScaleAtCommit < 1f)
        assertTrue(policy.predictiveAlphaAtCommit < 1f)
    }

    @Test
    fun `reduced motion removes shared transforms but preserves short state change`() {
        val policy = veilRealmMotionPolicy(reducedMotion = true)

        assertEquals(0, policy.sharedBoundsDurationMs)
        assertEquals(1f, policy.predictiveScaleAtCommit)
        assertEquals(1f, policy.predictiveAlphaAtCommit)
        assertEquals(
            effectiveMotionDurationMs(VeilMotionClass.REALM, true),
            policy.enterDurationMs
        )
    }
}
