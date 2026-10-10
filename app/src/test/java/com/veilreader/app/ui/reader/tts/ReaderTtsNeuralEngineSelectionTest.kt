package com.veilreader.app.ui.reader.tts

import android.os.Bundle
import com.veilreader.app.domain.ReaderTtsEngineChoice
import com.veilreader.app.domain.ReaderTtsSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real engine selection must survive app-private MediaSession and checkpoint payloads. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderTtsNeuralEngineSelectionTest {
    private fun request(choice: ReaderTtsEngineChoice) = ReaderTtsPlaybackRequest(
        bookId = "veil-fixture",
        locatorJson = """{"href":"chapter.xhtml","type":"application/xhtml+xml"}""",
        preferences = ReaderTtsPreferences(
            engine = choice, speed = 1.2f, preferredVoiceIds = emptyMap()
        )
    )

    @Test
    fun neuralChoiceSurvivesBundleAndCheckpointJson() {
        val request = request(ReaderTtsEngineChoice.SHERPA_ONNX)
        assertEquals(
            ReaderTtsEngineChoice.SHERPA_ONNX,
            ReaderTtsPlaybackRequest.fromBundle(request.toBundle())?.preferences?.engine
        )
        assertEquals(
            ReaderTtsEngineChoice.SHERPA_ONNX,
            ReaderTtsPlaybackRequest.fromJson(request.toJson())?.preferences?.engine
        )
        val checkpoint = ReaderTtsCheckpoint(
            request = request,
            locatorJson = request.locatorJson,
            phase = ReaderTtsPhase.PAUSED,
            updatedAtEpochMs = 123L
        )
        assertEquals(
            ReaderTtsEngineChoice.SHERPA_ONNX,
            ReaderTtsCheckpoint.fromJson(checkpoint.toJson())?.request?.preferences?.engine
        )
    }

    @Test
    fun legacyRequestsRemainSystemTts() {
        val old = request(ReaderTtsEngineChoice.SYSTEM).toBundle()
        old.remove(ReaderTtsPlaybackRequest.EXTRA_ENGINE_CHOICE)
        assertEquals(
            ReaderTtsEngineChoice.SYSTEM,
            ReaderTtsPlaybackRequest.fromBundle(old)?.preferences?.engine
        )
        assertEquals(ReaderTtsEngineChoice.SYSTEM,
            ReaderTtsPlaybackRequest.decodeEngineChoice("UNTRUSTED"))
        assertEquals(ReaderTtsEngineChoice.SYSTEM, ReaderTtsSettings().engine)
    }

    @Test
    fun switchingBackToSystemRestoresItsSavedNarrator() {
        val system = ReaderTtsSettings().withPreferredVoice("en-US", "google-original")
        val neural = system.withEngine(ReaderTtsEngineChoice.SHERPA_ONNX)
        assertEquals(ReaderTtsEngineChoice.SHERPA_ONNX, neural.engine)
        assertEquals("google-original", neural.savedSystemVoices["en-US"])
        assertTrue(neural.preferredVoiceIds.isEmpty())

        val configured = neural.withPreferredVoice("en-US", "kokoro-premium")
        val back = configured.withEngine(ReaderTtsEngineChoice.SYSTEM)
        assertEquals("google-original", back.preferredVoiceId("en-US"))
        assertEquals("kokoro-premium", back.savedSherpaVoices["en-US"])

        val again = back.withEngine(ReaderTtsEngineChoice.SHERPA_ONNX)
        assertEquals("kokoro-premium", again.preferredVoiceId("en-US"))
    }

    @Test
    fun selectedEngineIsNotAClaimOfAnInstalledModel() {
        val spec = ReaderTtsNeuralModelSpec(
            packageId = "kokoro-en",
            family = ReaderTtsNeuralModelFamily.KOKORO,
            languageTags = setOf("en-US")
        )
        assertEquals(ReaderTtsNeuralModelFamily.KOKORO, spec.family)
        assertFalse(readerTtsProviderCatalog().first {
            it.kind == ReaderTtsProviderKind.LOCAL_NEURAL
        }.available)
        assertTrue(
            ReaderTtsPlaybackRequest.decodeEngineChoice("SHERPA_ONNX") ==
                ReaderTtsEngineChoice.SHERPA_ONNX
        )
    }
}
