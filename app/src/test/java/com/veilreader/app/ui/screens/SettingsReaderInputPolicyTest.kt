package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderTapAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsReaderInputPolicyTest {
    @Test
    fun materialPageDebugOverride_isNullableReleaseSafe() {
        assertNull(materialPageReviewDebugOverride(false))
        assertTrue(materialPageReviewDebugOverride(true) == true)
    }

    @Test
    fun tapActionCycle_visitsEveryOwnedActionAndReturnsToDefault() {
        val sequence = buildList {
            var current = ReaderTapAction.VEIL_DEFAULT
            repeat(5) {
                current = nextReaderTapAction(current)
                add(current)
            }
        }

        assertEquals(
            listOf(
                ReaderTapAction.PREVIOUS_PAGE,
                ReaderTapAction.TOGGLE_CONTROLS,
                ReaderTapAction.NEXT_PAGE,
                ReaderTapAction.RENDERER,
                ReaderTapAction.VEIL_DEFAULT
            ),
            sequence
        )
    }

    @Test
    fun everyTapAction_hasCompactGlyph() {
        ReaderTapAction.entries.forEach { action ->
            assertTrue(action.name, readerTapActionGlyph(action).isNotBlank())
        }
    }
}
