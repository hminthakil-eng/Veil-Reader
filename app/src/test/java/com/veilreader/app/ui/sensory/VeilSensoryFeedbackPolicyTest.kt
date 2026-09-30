package com.veilreader.app.ui.sensory

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VeilSensoryFeedbackPolicyTest {
    @Test
    fun `paper slide and static paged turns have distinct tactile identities`() {
        val paper = hapticFeedbackFor(VeilSensoryEvent.PAGE_TURN)
        val slide = hapticFeedbackFor(VeilSensoryEvent.SLIDE_TURN)
        val paged = hapticFeedbackFor(VeilSensoryEvent.PAGED_TURN)

        assertEquals(HapticFeedbackConstants.CLOCK_TICK, paper)
        assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, slide)
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, paged)
        assertNotEquals(paper, slide)
        assertNotEquals(slide, paged)
        assertNotEquals(paper, paged)
    }

    @Test
    fun `reader boundary uses a non-commit tactile cue`() {
        assertEquals(
            HapticFeedbackConstants.CONTEXT_CLICK,
            hapticFeedbackFor(VeilSensoryEvent.BOUNDARY)
        )
    }
}
