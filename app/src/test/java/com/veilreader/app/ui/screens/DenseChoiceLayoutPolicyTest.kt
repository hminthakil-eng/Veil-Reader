package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DenseChoiceLayoutPolicyTest {
    @Test
    fun `three-way controls stack early as text grows`() {
        assertFalse(shouldStackDenseChoices(widthDp = 411, fontScale = 1f, optionCount = 3))
        assertTrue(shouldStackDenseChoices(widthDp = 411, fontScale = 1.35f, optionCount = 3))
        assertTrue(shouldStackDenseChoices(widthDp = 320, fontScale = 1f, optionCount = 3))
    }

    @Test
    fun `two-way controls preserve horizontal layout longer`() {
        assertFalse(shouldStackDenseChoices(widthDp = 411, fontScale = 1.4f, optionCount = 2))
        assertTrue(shouldStackDenseChoices(widthDp = 411, fontScale = 1.6f, optionCount = 2))
        assertTrue(shouldStackDenseChoices(widthDp = 300, fontScale = 1f, optionCount = 2))
    }

    @Test
    fun `extreme large text always chooses the safer stacked layout`() {
        assertTrue(shouldStackDenseChoices(widthDp = 1000, fontScale = 1.75f, optionCount = 2))
        assertTrue(shouldStackDenseChoices(widthDp = 1000, fontScale = 2f, optionCount = 4))
    }

    @Test
    fun `many-option controls abandon horizontal browsing at accessibility scale`() {
        assertFalse(shouldStackDenseChoices(widthDp = 720, fontScale = 1f, optionCount = 7))
        assertTrue(shouldStackDenseChoices(widthDp = 720, fontScale = 1.35f, optionCount = 7))
        assertTrue(shouldStackDenseChoices(widthDp = 720, fontScale = 2f, optionCount = 7))
    }

    @Test
    fun `invalid font scale falls back to normal density behavior`() {
        assertFalse(shouldStackDenseChoices(widthDp = 411, fontScale = Float.NaN, optionCount = 3))
    }
}
