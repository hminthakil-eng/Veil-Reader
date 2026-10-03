package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderHardwareKeyDispatcherTest {
    @Test
    fun `stale disposal cannot remove the newer handler`() {
        val dispatcher = ReaderHardwareKeyDispatcher()
        dispatcher.installReaderHardwareKeyHandler("old") { false }
        dispatcher.installReaderHardwareKeyHandler("new") { true }
        dispatcher.clearReaderHardwareKeyHandler("old")
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN)))
    }

    @Test
    fun `consumed press survives disposal without retaining the handler`() {
        val dispatcher = ReaderHardwareKeyDispatcher()
        dispatcher.installReaderHardwareKeyHandler("old") { true }
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN)))
        dispatcher.clearReaderHardwareKeyHandler("old")
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN, 1)))
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.UP)))
        assertFalse(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN)))
    }

    @Test
    fun `new physical press replaces a stale press whose up went elsewhere`() {
        val dispatcher = ReaderHardwareKeyDispatcher()
        dispatcher.installReaderHardwareKeyHandler("old") { true }
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN)))
        dispatcher.clearReaderHardwareKeyHandler("old")
        dispatcher.installReaderHardwareKeyHandler("new") { false }
        assertFalse(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN)))
        assertFalse(dispatcher.handle(event(ReaderHardwareButtonPhase.UP)))
    }

    @Test
    fun `replacement reader never receives the tail of an old press`() {
        val dispatcher = ReaderHardwareKeyDispatcher()
        var newCalls = 0
        dispatcher.installReaderHardwareKeyHandler("old") { true }
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN)))
        dispatcher.installReaderHardwareKeyHandler("new") { newCalls += 1; true }
        dispatcher.clearReaderHardwareKeyHandler("old")
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN, 1)))
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.UP)))
        assertEquals(0, newCalls)
        assertTrue(dispatcher.handle(event(ReaderHardwareButtonPhase.DOWN)))
        assertEquals(1, newCalls)
    }

    private fun event(phase: ReaderHardwareButtonPhase, repeatCount: Int = 0) =
        ReaderHardwareButtonEvent(ReaderHardwareButton.VOLUME_UP, phase, 1000L, repeatCount)
}
