package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsNeuralRuntimeTest {
    @Test
    fun interactiveGateRequiresBothFastFirstAudioAndFasterThanRealtimeSynthesis() {
        val baseline = ReaderTtsNeuralBenchmark(
            modelId = "candidate",
            languageTag = "fa-IR",
            deviceAbi = "arm64-v8a",
            inputCharacters = 300,
            firstAudioLatencyMs = 900,
            synthesisDurationMs = 4_000,
            generatedAudioDurationMs = 6_000,
            peakRssBytes = 220L * 1024L * 1024L,
            averageRealtimeFactor = 0.66,
            completed = true
        )

        assertTrue(baseline.passesInteractiveGate())
        assertFalse(
            baseline.copy(firstAudioLatencyMs = 1_500)
                .passesInteractiveGate()
        )
        assertFalse(
            baseline.copy(averageRealtimeFactor = 0.95)
                .passesInteractiveGate()
        )
        assertFalse(
            baseline.copy(completed = false)
                .passesInteractiveGate()
        )
    }

    @Test
    fun currentSherpaCandidateCoversTheFamiliesVeilWantsToBenchmark() {
        assertTrue(
            SherpaOnnxTtsCandidate.supportedFamilies.containsAll(
                setOf(
                    ReaderTtsNeuralModelFamily.VITS_PIPER,
                    ReaderTtsNeuralModelFamily.MATCHA,
                    ReaderTtsNeuralModelFamily.KOKORO,
                    ReaderTtsNeuralModelFamily.KITTEN,
                    ReaderTtsNeuralModelFamily.SUPERTONIC
                )
            )
        )
    }
}
