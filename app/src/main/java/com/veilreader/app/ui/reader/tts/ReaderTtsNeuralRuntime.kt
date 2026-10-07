package com.veilreader.app.ui.reader.tts

/**
 * Neural-runtime boundary. No Readium objects cross this interface.
 *
 * The runtime consumes already-selected semantic text and produces PCM. Reader position remains
 * owned by ReaderTtsSession, so changing neural engines cannot mutate navigation or checkpoints.
 */
internal interface ReaderTtsNeuralRuntime : AutoCloseable {
    val model: ReaderTtsNeuralModelSpec
    val sampleRateHz: Int
    suspend fun synthesize(
        text: String,
        voiceId: String?,
        speed: Float,
        onPcmChunk: suspend (FloatArray) -> Boolean
    ): ReaderTtsProblem?
}

internal enum class ReaderTtsNeuralModelFamily {
    VITS_PIPER,
    MATCHA,
    KOKORO,
    ZIPVOICE,
    KITTEN,
    SUPERTONIC,
    POCKET
}

internal data class ReaderTtsNeuralModelSpec(
    val packageId: String,
    val family: ReaderTtsNeuralModelFamily,
    val languageTags: Set<String>,
    val defaultVoiceId: String? = null,
    val supportsMultipleVoices: Boolean = false,
    val supportsStreamingChunks: Boolean = true,
    val expectedSampleRateHz: Int? = null
) {
    init {
        require(packageId.isNotBlank())
        require(languageTags.isNotEmpty())
        require(languageTags.none(String::isBlank))
        expectedSampleRateHz?.let { require(it in 8_000..96_000) }
    }
}

/**
 * Candidate integration pin from the current official sherpa-onnx Android line.
 *
 * This is deliberately metadata only. We do not add native binaries to the APK until ABI size,
 * startup, thermal, license and device benchmarks have passed.
 */
internal object SherpaOnnxTtsCandidate {
    const val UPSTREAM_VERSION = "1.13.8"

    val supportedFamilies: Set<ReaderTtsNeuralModelFamily> = setOf(
        ReaderTtsNeuralModelFamily.VITS_PIPER,
        ReaderTtsNeuralModelFamily.MATCHA,
        ReaderTtsNeuralModelFamily.KOKORO,
        ReaderTtsNeuralModelFamily.ZIPVOICE,
        ReaderTtsNeuralModelFamily.KITTEN,
        ReaderTtsNeuralModelFamily.SUPERTONIC,
        ReaderTtsNeuralModelFamily.POCKET
    )
}

internal data class ReaderTtsNeuralBenchmark(
    val modelId: String,
    val languageTag: String,
    val deviceAbi: String,
    val inputCharacters: Int,
    val firstAudioLatencyMs: Long,
    val synthesisDurationMs: Long,
    val generatedAudioDurationMs: Long,
    val peakRssBytes: Long?,
    val averageRealtimeFactor: Double,
    val completed: Boolean
) {
    init {
        require(modelId.isNotBlank())
        require(languageTag.isNotBlank())
        require(deviceAbi.isNotBlank())
        require(inputCharacters >= 0)
        require(firstAudioLatencyMs >= 0L)
        require(synthesisDurationMs >= 0L)
        require(generatedAudioDurationMs >= 0L)
        require(averageRealtimeFactor.isFinite() && averageRealtimeFactor >= 0.0)
        peakRssBytes?.let { require(it >= 0L) }
    }

    fun passesInteractiveGate(
        maxFirstAudioLatencyMs: Long = 1_200L,
        maxRealtimeFactor: Double = 0.85
    ): Boolean {
        require(maxFirstAudioLatencyMs >= 0L)
        require(maxRealtimeFactor.isFinite() && maxRealtimeFactor > 0.0)
        // A completed flag and an optimistic reported ratio are not audio evidence.
        return completed && inputCharacters > 0 &&
            synthesisDurationMs > 0L && generatedAudioDurationMs > 0L &&
            firstAudioLatencyMs <= maxFirstAudioLatencyMs &&
            averageRealtimeFactor <= maxRealtimeFactor &&
            synthesisDurationMs.toDouble() / generatedAudioDurationMs <= maxRealtimeFactor
    }
}
