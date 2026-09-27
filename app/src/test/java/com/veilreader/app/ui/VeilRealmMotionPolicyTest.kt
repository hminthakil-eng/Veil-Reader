package com.veilreader.app.ui

import com.veilreader.app.ui.theme.CathedralMotionClass
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
    fun `Hall routes keep tab and realm transition ownership separate`() {
        listOf("library", "ritual", "profile", "reading").forEach { route ->
            assertTrue(hallRouteUsesTabTransition(route))
        }
        listOf("mirror", "observatory", "treasury", "sanctum").forEach { route ->
            assertTrue(!hallRouteUsesTabTransition(route))
        }
    }

    @Test
    fun `full realm motion uses constitution budgets`() {
        val policy = veilRealmMotionPolicy(reducedMotion = false)

        assertEquals(
            motionBudgetFor(CathedralMotionClass.REALM).targetDurationMs,
            policy.enterDurationMs
        )
        assertEquals(
            motionBudgetFor(CathedralMotionClass.SPATIAL).targetDurationMs,
            policy.exitDurationMs
        )
        assertEquals(
            motionBudgetFor(CathedralMotionClass.SPATIAL).targetDurationMs,
            policy.sharedBoundsDurationMs
        )
        assertTrue(policy.predictiveScaleAtCommit < 1f)
        assertTrue(policy.predictiveAlphaAtCommit < 0.5f)
        assertTrue(policy.predictiveTranslationFractionAtCommit in 0.01f..0.15f)
    }

    @Test
    fun `reduced motion removes shared transforms but preserves short state change`() {
        val policy = veilRealmMotionPolicy(reducedMotion = true)

        assertEquals(0, policy.sharedBoundsDurationMs)
        assertEquals(1f, policy.predictiveScaleAtCommit)
        assertEquals(1f, policy.predictiveAlphaAtCommit)
        assertEquals(0f, policy.predictiveTranslationFractionAtCommit)
        assertEquals(
            effectiveMotionDurationMs(CathedralMotionClass.REALM, true),
            policy.enterDurationMs
        )
    }
}
