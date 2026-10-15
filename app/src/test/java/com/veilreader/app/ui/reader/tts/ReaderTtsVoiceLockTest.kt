package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Regression for reported surprise narrator switching between adjacent EPUB segments. */
class ReaderTtsVoiceLockTest {
    private val original = ReaderTtsVoice(
        id = "system.en.a", languageTag = "en-US", quality = 200,
        requiresNetwork = false, installed = true
    )
    private val alternate = ReaderTtsVoice(
        id = "system.en.b", languageTag = "en-US", quality = 400,
        requiresNetwork = false, installed = true
    )

    @Test
    fun newHigherQualityVoiceCannotReplaceAnAlreadySelectedNarrator() {
        val first = selectPinnedOfflineTtsVoice(listOf(original), "en-US", null, null)
        assertEquals(original.id, first?.id)

        // Android voice catalog may change order or gain a newer model mid-book.
        val next = selectPinnedOfflineTtsVoice(
            listOf(alternate, original), "en-US", null, first?.id
        )
        assertEquals(original.id, next?.id)
    }

    @Test
    fun disappearingPinnedVoiceFailsInsteadOfSilentlySwitchingSpeakers() {
        assertNull(selectPinnedOfflineTtsVoice(
            listOf(alternate), "en-US", null, original.id
        ))
    }

    @Test
    fun explicitUserChoiceMayReplaceThePinnedNarrator() {
        assertEquals(alternate.id, selectPinnedOfflineTtsVoice(
            listOf(original, alternate), "en-US", alternate.id, original.id
        )?.id)
    }

    @Test
    fun networkOnlyOrUninstalledPinnedVoiceCannotBecomeNarrator() {
        val networkOnly = original.copy(requiresNetwork = true)
        assertNull(selectPinnedOfflineTtsVoice(
            listOf(alternate, networkOnly), "en-US", null, original.id
        ))
        assertNull(selectPinnedOfflineTtsVoice(
            listOf(alternate, original.copy(installed = false)),
            "en-US", null, original.id
        ))
    }
}
