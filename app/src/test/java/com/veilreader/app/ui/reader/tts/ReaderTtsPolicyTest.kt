package com.veilreader.app.ui.reader.tts

import org.junit.Assert.*
import org.junit.Test

class ReaderTtsPolicyTest {
    @Test
    fun malformedSettingsCannotPassNonfiniteOrInvalidValuesToAndroid() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val safe = ReaderTtsPreferences(
                speed = bad,
                pitch = bad,
                languageTag = "und",
                preferredVoiceIds = mapOf("und" to " ")
            ).normalized()
            assertEquals(1f, safe.speed, 0f)
            assertEquals(1f, safe.pitch, 0f)
            assertNull(safe.languageTag)
            assertTrue(safe.preferredVoiceIds.isEmpty())
        }
        assertEquals(0.5f, ReaderTtsPreferences(speed = -1f).normalized().speed, 0f)
        assertEquals(3f, ReaderTtsPreferences(speed = 99f).normalized().speed, 0f)
        assertEquals(2f, ReaderTtsPreferences(pitch = 99f).normalized().pitch, 0f)
        assertEquals("fa-IR", ReaderTtsPreferences(languageTag = " fa-IR ").normalized().languageTag)
    }

    @Test
    fun missingOfflineVoiceNeverFallsBackToNetworkOrAnotherLanguage() {
        val voices = listOf(voice("fa-network", "fa-IR", network = true),
            voice("fa-missing", "fa-IR", installed = false), voice("en", "en-US"), voice("ar", "ar"))
        assertNull(selectOfflineTtsVoice(voices, "fa-IR"))
        assertNull(selectOfflineTtsVoice(voices, "und"))
    }

    @Test
    fun preferredVoiceAndDeterministicLocaleFallbackUseOnlyInstalledOfflineVoices() {
        val voices = listOf(voice("en-high", "en-GB", quality = 500), voice("en-exact", "en-US"),
            voice("network", "en-US", network = true), voice("gone", "en-US", installed = false))
        assertEquals("en-exact", selectOfflineTtsVoice(voices, "en-US")?.id)
        assertEquals("en-high", selectOfflineTtsVoice(voices, "en-US", "en-high")?.id)
        assertNull(selectOfflineTtsVoice(voices, "en-US", "network"))
        assertNull(selectOfflineTtsVoice(voices, "en-US", "gone"))
        assertEquals("en-exact", selectOfflineTtsVoice(voices.reversed(), "en-US")?.id)
    }

    @Test
    fun languageAwarePreferencesKeepPersianAndEnglishVoicesIndependent() {
        val prefs = ReaderTtsPreferences(
            preferredVoiceIds = mapOf(
                "fa-IR" to "fa-local",
                "en-US" to "en-local"
            )
        ).normalized()
        assertEquals("fa-local", prefs.preferredVoiceId("fa-AF"))
        assertEquals("en-local", prefs.preferredVoiceId("en-GB"))
        assertNull(prefs.preferredVoiceId("ar"))
    }

    @Test
    fun explicitCjkScriptCannotChooseTheOppositeScriptVoice() {
        assertNull(selectOfflineTtsVoice(listOf(voice("simplified", "zh-Hans-CN")), "zh-Hant-TW"))
    }

    @Test
    fun boundedUnicodeChunksPreserveEveryCharacterIncludingAstralAndRtlText() {
        val text = "متن فارسی English العربية 漢字 🕯️😀\n".repeat(500)
        for (limit in listOf(2, 7, 31, 1000, 4000)) {
            var start = 0
            val rebuilt = StringBuilder()
            while (start < text.length) {
                val end = ttsChunkEnd(text, start, limit)
                assertTrue(end > start)
                assertTrue(end - start <= limit)
                assertFalse(text[end - 1].isHighSurrogate())
                rebuilt.append(text.substring(start, end))
                start = end
            }
            assertEquals(text, rebuilt.toString())
        }
    }

    @Test
    fun giantLimitsAndUnbrokenTokensStayBoundedWithoutIntegerOverflow() {
        val text = "字".repeat(10_000)
        assertEquals(4000, ttsChunkEnd(text, 0, 4000))
        assertEquals(text.length, ttsChunkEnd(text, 5000, Int.MAX_VALUE))
    }

    private fun voice(id: String, language: String, quality: Int = 300, network: Boolean = false, installed: Boolean = true) =
        ReaderTtsVoice(id, language, quality, network, installed)
}
