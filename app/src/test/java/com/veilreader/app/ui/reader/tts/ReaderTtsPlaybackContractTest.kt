package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderTtsPlaybackContractTest {
    @Test
    fun appPrivateRequestRoundTripsAndNormalizesPreferences() {
        val request = ReaderTtsPlaybackRequest(
            bookId = "book-1",
            locatorJson = locatorJson(),
            preferences = ReaderTtsPreferences(
                speed = 99f,
                pitch = -4f,
                languageTag = " fa-IR ",
                preferredVoiceIds = mapOf(
                    "fa-IR" to "voice-fa",
                    "en-US" to "voice-en"
                )
            )
        ).normalized()

        assertNotNull(request)
        val restored = ReaderTtsPlaybackRequest.fromBundle(requireNotNull(request).toBundle())
        assertNotNull(restored)
        assertEquals(3f, restored!!.preferences.speed, 0f)
        assertEquals(0.5f, restored.preferences.pitch, 0f)
        assertEquals("fa-IR", restored.preferences.languageTag)
        assertEquals("voice-fa", restored.preferences.preferredVoiceId("fa-IR"))
        assertEquals("voice-en", restored.preferences.preferredVoiceId("en-GB"))
    }

    @Test
    fun malformedLocatorCanNeverBecomeAServicePlaybackRequest() {
        assertNull(
            ReaderTtsPlaybackRequest(
                bookId = "book",
                locatorJson = "not-json"
            ).normalized()
        )
        assertNull(
            ReaderTtsPlaybackRequest(
                bookId = " ",
                locatorJson = locatorJson()
            ).normalized()
        )
    }

    @Test
    fun checkpointRoundTripKeepsListeningPositionSeparateFromInitialRequest() {
        val first = locatorJson("chapter-1.xhtml")
        val later = locatorJson("chapter-2.xhtml")
        val checkpoint = ReaderTtsCheckpoint(
            request = ReaderTtsPlaybackRequest("book-1", first),
            locatorJson = later,
            phase = ReaderTtsPhase.PAUSED,
            updatedAtEpochMs = 1234L
        )

        val restored = ReaderTtsCheckpoint.fromJson(checkpoint.toJson())
        assertNotNull(restored)
        assertEquals(first, restored!!.request.locatorJson)
        assertEquals(later, restored.locatorJson)
        assertEquals(ReaderTtsPhase.PAUSED, restored.phase)
        assertEquals(1234L, restored.updatedAtEpochMs)
    }

    @Test
    fun corruptedCheckpointIsRejectedInsteadOfGuessingAPosition() {
        assertNull(ReaderTtsCheckpoint.fromJson("""{"request":{},"locator_json":"x"}"""))
        assertNull(ReaderTtsCheckpoint.fromJson("not-json"))
    }

    private fun locatorJson(href: String = "chapter.xhtml"): String =
        """{"href":"$href","type":"application/xhtml+xml","locations":{}}"""
}
