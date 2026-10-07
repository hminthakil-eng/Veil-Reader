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
        assertEquals(ReaderTtsSettings(3.0, 0.6), decodeReaderTtsPreferences(preferencesOf(
            doublePreferencesKey("reader_tts_speed") to 20.0,
            doublePreferencesKey("reader_tts_pitch") to -1.0
        )))
    }

    @Test
    fun preferredOfflineVoicesRoundTripAndMalformedPayloadIsIgnored() {
        val encoded = encodeReaderTtsPreferredVoices(
            mapOf("fa-IR" to "fa-local", "en-US" to "en-local")
        )
        val decoded = decodeReaderTtsPreferences(
            preferencesOf(
                stringPreferencesKey("reader_tts_preferred_voices") to encoded
            )
        )
        assertEquals("fa-local", decoded.preferredVoiceId("fa-AF"))
        assertEquals("en-local", decoded.preferredVoiceId("en-GB"))

        assertEquals(
            ReaderTtsSettings(),
            decodeReaderTtsPreferences(
                preferencesOf(
                    stringPreferencesKey("reader_tts_preferred_voices") to "not-json"
                )
            )
        )
    }
}
