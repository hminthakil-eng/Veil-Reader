package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderFocusGuideMode
import com.veilreader.app.domain.ReaderFocusGuideSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderFocusGuideStateTest {
    @Test
    fun `rapid toggles do not read a stale persisted snapshot`() {
        val original = ReaderFocusGuideSettings(mode = ReaderFocusGuideMode.LINE).normalized()
        val state = ReaderFocusGuideState(original)
        val off = state.toggle()
        val on = state.toggle()
        assertEquals(ReaderFocusGuideMode.OFF, off.mode)
        assertEquals(original, on)
        state.acceptPersisted(off) // Delayed acknowledgement of the first press.
        assertEquals(on, state.value)
        state.acceptPersisted(on)
        assertEquals(original, state.value)
    }

    @Test
    fun `recreated state resumes the durably saved mode`() {
        val off = ReaderFocusGuideState(ReaderFocusGuideSettings(mode = ReaderFocusGuideMode.LINE)).toggle()
        val restored = ReaderFocusGuideState(off)
        assertEquals(ReaderFocusGuideMode.OFF, restored.value.mode)
        assertEquals(ReaderFocusGuideMode.LINE, restored.toggle().mode)
    }

    @Test
    fun `external preferences remain authoritative after acknowledgement`() {
        val state = ReaderFocusGuideState(ReaderFocusGuideSettings())
        state.acceptPersisted(state.toggle())
        val external = ReaderFocusGuideSettings(mode = ReaderFocusGuideMode.LINE, dimStrength = 0.41).normalized()
        state.acceptPersisted(external)
        assertEquals(external, state.value)
    }
}
