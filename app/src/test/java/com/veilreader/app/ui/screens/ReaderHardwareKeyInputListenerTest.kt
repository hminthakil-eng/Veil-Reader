package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderHardwareKeyAction
import com.veilreader.app.domain.ReaderHardwareKeyMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
class ReaderHardwareKeyInputListenerTest {
    @Test
    fun `system volume mapping never consumes the key`() {
        val listener = ReaderHardwareKeyInputListener(
            mapping = { ReaderHardwareKeyMap() },
            isEnabled = { true },
            onPreviousPage = { error("must not run") },
            onNextPage = { error("must not run") },
            onToggleControls = { error("must not run") },
            nowElapsedMs = { 1000L }
        )

        assertFalse(listener.onKey(volumeEvent(Key.AudioVolumeUp)))
        assertFalse(listener.onKey(volumeEvent(Key.AudioVolumeDown)))
    }

    @Test
    fun `custom volume mapping dispatches semantic page action`() {
        var nextCalls = 0
        val listener = ReaderHardwareKeyInputListener(
            mapping = {
                ReaderHardwareKeyMap(
                    volumeUp = ReaderHardwareKeyAction.NEXT_PAGE
                )
            },
            isEnabled = { true },
            onPreviousPage = { false },
            onNextPage = { nextCalls += 1; true },
            onToggleControls = { false },
            nowElapsedMs = { 1000L }
        )

        assertTrue(listener.onKey(volumeEvent(Key.AudioVolumeUp)))
        assertEquals(1, nextCalls)
    }

    @Test
    fun `held hardware key is rate limited`() {
        var now = 1000L
        var nextCalls = 0
        val listener = ReaderHardwareKeyInputListener(
            mapping = {
                ReaderHardwareKeyMap(
                    volumeDown = ReaderHardwareKeyAction.NEXT_PAGE
                )
            },
            isEnabled = { true },
            onPreviousPage = { false },
            onNextPage = { nextCalls += 1; true },
            onToggleControls = { false },
            nowElapsedMs = { now }
        )
        val event = volumeEvent(Key.AudioVolumeDown)

        assertTrue(listener.onKey(event))
        now += 80L
        assertTrue(listener.onKey(event))
        assertEquals(1, nextCalls)

        now += 120L
        assertTrue(listener.onKey(event))
        assertEquals(2, nextCalls)
    }

    @Test
    fun `repeat guard fails calm when elapsed clock moves backwards`() {
        assertTrue(shouldHandleReaderHardwareRepeat(900L, 1000L))
        assertFalse(shouldHandleReaderHardwareRepeat(1100L, 1000L))
        assertTrue(shouldHandleReaderHardwareRepeat(1180L, 1000L))
    }

    private fun volumeEvent(key: Key): KeyEvent =
        KeyEvent(
            type = KeyEvent.Type.Down,
            key = key,
            modifiers = emptySet(),
            characters = null
        )
}
