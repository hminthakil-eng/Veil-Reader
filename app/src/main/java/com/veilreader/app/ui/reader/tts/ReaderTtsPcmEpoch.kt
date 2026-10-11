package com.veilreader.app.ui.reader.tts

import java.util.concurrent.atomic.AtomicLong

/**
 * Single-owner fence for an in-process native TTS producer and Veil-owned PCM output.
 *
 * This is independent of Android TextToSpeechService, Readium and JNI. Native inference may
 * continue unwinding after Pause; it MUST ask accepts() before each generated PCM chunk.
 * The consumer MUST ask accepts() before each bounded AudioTrack write.
 *
 * A false result means drop PCM and return 0/false to the native callback.
 * Revoking this fence alone is not enough for audible silence: the caller must
 * also pause+flush the owned AudioTrack immediately to discard queued hardware frames.
 */
internal class ReaderTtsPcmEpoch {
    internal data class Lease(
        val generation: Long,
        val modelId: String,
        val voiceId: String
    )

    private val sequence = AtomicLong()
    @Volatile private var active: Lease? = null

    @Synchronized
    fun begin(modelId: String, voiceId: String): Lease {
        require(modelId.isNotBlank()) { "A verified model ID is required" }
        require(voiceId.isNotBlank()) { "Explicit narrator identity is required" }
        check(active == null) { "Revoke the previous TTS owner before starting another" }
        val lease = Lease(
            generation = sequence.incrementAndGet(),
            modelId = modelId,
            voiceId = voiceId
        )
        active = lease
        return lease
    }

    /** Takes effect across threads without waiting for a JNI mutex or audio writer. */
    @Synchronized
    fun revoke() {
        sequence.incrementAndGet()
        active = null
    }

    fun accepts(lease: Lease): Boolean =
        active === lease && sequence.get() == lease.generation

    fun selectedVoiceOrNull(): String? = active?.voiceId

    fun hasOwner(): Boolean = active != null
}

/**
 * A single request's PCM is useful only while the exact model AND voice owner still lives.
 * Audio write code must check the epoch again on every short non-blocking write.
 */
internal fun readerTtsValidPcmChunk(
    epoch: ReaderTtsPcmEpoch,
    lease: ReaderTtsPcmEpoch.Lease,
    sampleRateHz: Int,
    samples: FloatArray
): Boolean =
    epoch.accepts(lease) &&
        sampleRateHz in 8_000..96_000 &&
        samples.isNotEmpty() &&
        samples.size <= 96_000 * 2 &&
        samples.all { it.isFinite() && it in -1.0f..1.0f }
