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
    fun `material completion haptics preserve distinct physical identities`() {
        val glossy = materialHapticFeedbackFor(
            VeilMaterialPageSensoryCue(
                material = VeilPageMaterial.GLOSSY,
                action = VeilMaterialPageAction.COMPLETE,
                durationMillis = 60,
                acousticBrightness = 0.9f,
                acousticDryness = 0.2f,
                acousticBody = 0.3f,
                acousticFiber = 0.05f,
                acousticGain = 0.4f,
                hapticSharpness = 0.82f,
                hapticWeight = 0.34f,
                hapticPulseCount = 1,
                hapticPulseMillis = 8,
                hapticGapMillis = 8
            )
        )
        val parchment = materialHapticFeedbackFor(
            VeilMaterialPageSensoryCue(
                material = VeilPageMaterial.PARCHMENT,
                action = VeilMaterialPageAction.COMPLETE,
                durationMillis = 100,
                acousticBrightness = 0.3f,
                acousticDryness = 0.7f,
                acousticBody = 0.7f,
                acousticFiber = 0.5f,
                acousticGain = 0.5f,
                hapticSharpness = 0.34f,
                hapticWeight = 0.72f,
                hapticPulseCount = 1,
                hapticPulseMillis = 14,
                hapticGapMillis = 12
            )
        )

        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, glossy)
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, parchment)
        assertNotEquals(glossy, parchment)
    }

    @Test
    fun `reader boundary uses a non-commit tactile cue`() {
        assertEquals(
            HapticFeedbackConstants.CONTEXT_CLICK,
            hapticFeedbackFor(VeilSensoryEvent.BOUNDARY)
        )
    }
}
