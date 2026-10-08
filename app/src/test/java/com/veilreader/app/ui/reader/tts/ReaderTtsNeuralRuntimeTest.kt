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
    fun emptyOrSilentSamplesCannotPassACompletedBenchmark() {
        val baseline = validBenchmark()
        assertFalse(baseline.copy(inputCharacters = 0).passesInteractiveGate())
        assertFalse(baseline.copy(synthesisDurationMs = 0).passesInteractiveGate())
        assertFalse(baseline.copy(generatedAudioDurationMs = 0).passesInteractiveGate())
    }

    @Test
    fun optimisticReportedRatioCannotHideSlowerThanRealtimeMeasuredDurations() {
        assertFalse(validBenchmark().copy(
            synthesisDurationMs = 8_000, generatedAudioDurationMs = 6_000,
            averageRealtimeFactor = 0.1
        ).passesInteractiveGate())
    }

    @Test(expected = IllegalArgumentException::class)
    fun infiniteReportedRatioIsInvalidEvidence() {
        validBenchmark().copy(averageRealtimeFactor = Double.POSITIVE_INFINITY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun infiniteGateCannotApproveEveryModel() {
        validBenchmark().passesInteractiveGate(maxRealtimeFactor = Double.POSITIVE_INFINITY)
    }

    private fun validBenchmark() = ReaderTtsNeuralBenchmark(
        "candidate", "en-US", "arm64-v8a", 300, 900, 4_000, 6_000, null, 0.66, true
    )

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
