package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Veil owns the audible PCM sink, not Android's external TextToSpeechService.
 * This is deliberately an injectable contract until the vetted sherpa-onnx
 * arm64 JNI integration and model package have passed device acceptance.
 */
internal interface ReaderTtsOwnedPcmSink {
    /** Called on worker coroutine, before the first audio chunk. */
    suspend fun prepare(sampleRateHz: Int): Boolean

    /** PCM buffers are caller-owned copies: no JNI callback buffer may escape. */
    suspend fun submit(pcm: FloatArray): Boolean

    /**
     * Wait until buffered audio has actually played. Native inference finishing
     * is NOT equivalent to the user hearing the final sample.
     */
    suspend fun drain(): Boolean

    /** MUST interrupt and flush buffered audio without waiting for JNI inference. */
    fun silenceImmediately()
}

/**
 * Fully testable neural generation -> PCM consumer seam.
 *
 * Native synth is serialized (some upstream JNI engines are not reentrant).
 * Pause always revokes the epoch and flushes the audio sink synchronously;
 * the next callback from stale native inference returns false to halt it.
 * A new explicit Play cannot overlap a still-unwinding native call.
 *
 * This coordinator does not install a model, choose a voice or silently
 * fall back to another synthesizer. The Reader session remains sole owner
 * of locator, audio focus and durable checkpoint.
 */
internal class ReaderTtsInProcessCoordinator(
    private val runtime: ReaderTtsNeuralRuntime,
    private val pcmSink: ReaderTtsOwnedPcmSink,
    private val epoch: ReaderTtsPcmEpoch = ReaderTtsPcmEpoch()
) {
    private val nativeOwner = Mutex()

    suspend fun speak(
        text: String,
        voiceId: String,
        speed: Float,
        endOfStream: Boolean = true
    ): ReaderTtsProblem? = nativeOwner.withLock {
        if (text.isBlank() || !speed.isFinite() || speed !in 0.5f..3f) {
            return@withLock ReaderTtsProblem.CONTENT
        }
        if (voiceId.isBlank()) return@withLock ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE

        val lease = epoch.begin(runtime.model.packageId, voiceId)
        var normalCompletion = false
        try {
            val rate = runtime.sampleRateHz
            if (rate !in 8_000..96_000) return@withLock ReaderTtsProblem.SYNTHESIS
            if (!pcmSink.prepare(rate)) return@withLock ReaderTtsProblem.NO_ENGINE
            val result = runtime.synthesize(
                text = text,
                voiceId = lease.voiceId,
                speed = speed
            ) { nativePcm ->
                // Never call JNI-generated callbacks while holding an epoch
                // lock: Pause must interrupt immediately from the UI thread.
                if (!readerTtsValidPcmChunk(epoch, lease, rate, nativePcm)) {
                    false
                } else {
                    // JNI callbacks may reuse FloatArrays. The sink must never
                    // retain or modify a pointer to engine-owned memory.
                    val detached = nativePcm.copyOf()
                    epoch.accepts(lease) && pcmSink.submit(detached) &&
                        epoch.accepts(lease)
                }
            }
            if (result == null && epoch.accepts(lease)) {
                // Sentence transitions append PCM to the same track. Draining
                // on every sentence introduces audible gaps and falsely
                // advertises playback completion before future segments.
                if (endOfStream && !pcmSink.drain()) {
                    ReaderTtsProblem.SYNTHESIS
                } else {
                    normalCompletion = true
                    null
                }
            } else {
                result
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ReaderTtsProblem.SYNTHESIS
        } finally {
            if (epoch.accepts(lease)) {
                epoch.revoke()
                // Natural segment boundaries must not flush the hardware
                // PCM buffer; subsequent segments append to the same output.
                if (!normalCompletion) pcmSink.silenceImmediately()
            }
        }
    }

    /** UI/MediaSession command: no suspension and no JNI owner mutex. */
    fun pause() {
        epoch.revoke()
        pcmSink.silenceImmediately()
    }

    fun isSpeaking(): Boolean = epoch.hasOwner()

    fun selectedVoiceOrNull(): String? = epoch.selectedVoiceOrNull()
}
