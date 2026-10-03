package com.veilreader.app.data.settings

import androidx.datastore.preferences.core.*
import com.veilreader.app.domain.ReaderTtsSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTtsPreferencesTest {
    @Test
    fun malformedTypedSpeechPreferencesDoNotTerminateSettingsDecoding() {
        assertEquals(ReaderTtsSettings(), decodeReaderTtsPreferences(preferencesOf(
            stringPreferencesKey("reader_tts_speed") to "invalid",
            doublePreferencesKey("reader_tts_pitch") to Double.NaN
        )))
        assertEquals(ReaderTtsSettings(2.0, 0.6), decodeReaderTtsPreferences(preferencesOf(
            doublePreferencesKey("reader_tts_speed") to 20.0,
            doublePreferencesKey("reader_tts_pitch") to -1.0
        )))
    }
}
