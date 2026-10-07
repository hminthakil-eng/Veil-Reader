package com.veilreader.app.ui.reader.tts

/**
 * Benchmark candidates mirrored from the current official sherpa-onnx Android TTS model generator.
 *
 * These are research identifiers only. They intentionally contain no download URL, checksum or
 * license metadata and therefore cannot be installed by ReaderTtsModelStore. Promotion into the
 * install registry requires a reviewed ReaderTtsModelPackage plus device evidence.
 */
internal data class ReaderTtsNeuralCandidate(
    val upstreamModelDir: String,
    val family: ReaderTtsNeuralModelFamily,
    val languageTags: Set<String>,
    val benchmarkPriority: Int
)

internal fun persianNeuralBenchmarkCandidates(): List<ReaderTtsNeuralCandidate> = listOf(
    ReaderTtsNeuralCandidate(
        upstreamModelDir = "vits-piper-fa_IR-amir-medium",
        family = ReaderTtsNeuralModelFamily.VITS_PIPER,
        languageTags = setOf("fa-IR"),
        benchmarkPriority = 1
    ),
    ReaderTtsNeuralCandidate(
        upstreamModelDir = "vits-piper-fa_IR-ganji-medium",
        family = ReaderTtsNeuralModelFamily.VITS_PIPER,
        languageTags = setOf("fa-IR"),
        benchmarkPriority = 1
    ),
    ReaderTtsNeuralCandidate(
        upstreamModelDir = "vits-piper-fa_IR-ganji_adabi-medium",
        family = ReaderTtsNeuralModelFamily.VITS_PIPER,
        languageTags = setOf("fa-IR"),
        benchmarkPriority = 1
    ),
    ReaderTtsNeuralCandidate(
        upstreamModelDir = "vits-piper-fa_IR-gyro-medium",
        family = ReaderTtsNeuralModelFamily.VITS_PIPER,
        languageTags = setOf("fa-IR"),
        benchmarkPriority = 1
    ),
    ReaderTtsNeuralCandidate(
        upstreamModelDir = "vits-piper-fa_IR-reza_ibrahim-medium",
        family = ReaderTtsNeuralModelFamily.VITS_PIPER,
        languageTags = setOf("fa-IR"),
        benchmarkPriority = 1
    ),
    ReaderTtsNeuralCandidate(
        upstreamModelDir = "matcha-tts-fa_en-musa",
        family = ReaderTtsNeuralModelFamily.MATCHA,
        languageTags = setOf("fa-IR", "en"),
        benchmarkPriority = 2
    ),
    ReaderTtsNeuralCandidate(
        upstreamModelDir = "matcha-tts-fa_en-khadijah",
        family = ReaderTtsNeuralModelFamily.MATCHA,
        languageTags = setOf("fa-IR", "en"),
        benchmarkPriority = 2
    )
).sortedWith(
    compareBy<ReaderTtsNeuralCandidate> { it.benchmarkPriority }
        .thenBy { it.upstreamModelDir }
)
