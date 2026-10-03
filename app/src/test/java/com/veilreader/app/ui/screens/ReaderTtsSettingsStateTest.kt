package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderTtsSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTtsSettingsStateTest {
    @Test
    fun rapidCombinedSliderEditsSurviveOldPersistenceAcknowledgements() {
        val original = ReaderTtsSettings()
        val state = ReaderTtsSettingsState(original)
        val first = state.update(original.copy(speed = 1.5))
        val latest = state.update(state.value.copy(pitch = 0.8))
        state.acceptPersisted(first)
        state.acceptPersisted(original)
        assertEquals(latest, state.value)
        state.acceptPersisted(latest)
        state.acceptPersisted(original)
        assertEquals(original, state.value)
    }
}
