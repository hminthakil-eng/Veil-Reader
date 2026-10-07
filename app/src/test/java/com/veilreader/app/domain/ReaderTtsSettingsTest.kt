package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsSettingsTest {
    @Test
    fun `tts settings default to neutral voice controls`() {
        val settings = ReaderTtsSettings()
        assertEquals(1.0, settings.speed, 0.0001)
        assertEquals(1.0, settings.pitch, 0.0001)
        assertTrue(settings.preferredVoiceIds.isEmpty())
    }

    @Test
    fun `tts settings normalize malformed and extreme values`() {
        val malformed = ReaderTtsSettings(
            speed = Double.NaN,
            pitch = Double.POSITIVE_INFINITY
        ).normalized()
        assertEquals(1.0, malformed.speed, 0.0001)
        assertEquals(1.0, malformed.pitch, 0.0001)

        val clamped = ReaderTtsSettings(
            speed = 9.0,
            pitch = 0.1
        ).normalized()
        assertEquals(3.0, clamped.speed, 0.0001)
        assertEquals(0.6, clamped.pitch, 0.0001)
    }

    @Test
    fun `preferred voices normalize per language without cross-language leakage`() {
        val settings = ReaderTtsSettings(
            preferredVoiceIds = mapOf(
                " fa-IR " to " persian-offline ",
                "en-US" to "english-offline",
                "und" to "invalid",
                "ar" to " "
            )
        ).normalized()

        assertEquals("persian-offline", settings.preferredVoiceId("fa-AF"))
        assertEquals("english-offline", settings.preferredVoiceId("en-GB"))
        assertNull(settings.preferredVoiceId("ar"))
        assertEquals(2, settings.preferredVoiceIds.size)
    }
}
