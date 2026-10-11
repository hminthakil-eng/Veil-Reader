package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Veil owns the audible PCM sink, not Android's external TextToSpeechService.
 * This is deliberately an injectable contract until the vetted sherpa-onnx
 * arm64 JNI integration and model package have passed device acceptance.
 */
internal interface ReaderTtsOwnedPcmSink {
    /** Called on worker coroutine, before the first audio chunk. */
    suspend fun prepare(sampleRateHz: Int, stillCurrent: () -> Boolean): Boolean

    /** PCM buffers are caller-owned copies: no JNI callback buffer may escape. */
    suspend fun submit(pcm: FloatArray, stillCurrent: () -> Boolean): Boolean

    /**
     * Wait until buffered audio has actually played. Native inference finishing
     * is NOT equivalent to the user hearing the final sample.
     */
    suspend fun drain(stillCurrent: () -> Boolean): Boolean

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
/** A user-requested Pause is not an engine failure and must never advance the chapter. */
internal sealed interface ReaderTtsInProcessOutcome {
    data object Completed : ReaderTtsInProcessOutcome
    data object Interrupted : ReaderTtsInProcessOutcome
    data class Failed(val problem: ReaderTtsProblem) : ReaderTtsInProcessOutcome
}

internal class ReaderTtsInProcessCoordinator(
    private val runtime: ReaderTtsNeuralRuntime,
    private val pcmSink: ReaderTtsOwnedPcmSink,
    private val epoch: ReaderTtsPcmEpoch = ReaderTtsPcmEpoch()
) {
    private val nativeOwner = Mutex()
    private val transportGate = Any()
    private val commandGeneration = AtomicLong()

    suspend fun speak(
        text: String,
        voiceId: String,
        speed: Float,
        endOfStream: Boolean = true
    ): ReaderTtsInProcessOutcome {
        // A queued Play may wait for a previous JNI invocation to unwind.
        // If Pause occurred while waiting, it must not start after that Pause.
        val requestedGeneration = commandGeneration.get()
        return nativeOwner.withLock {
        if (text.isBlank() || !speed.isFinite() || speed !in 0.5f..3f) {
            return@withLock ReaderTtsInProcessOutcome.Failed(ReaderTtsProblem.CONTENT)
        }
        if (voiceId.isBlank()) return@withLock ReaderTtsInProcessOutcome.Failed(
            ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE
        )

        val lease = synchronized(transportGate) {
            if (requestedGeneration != commandGeneration.get()) null
            else epoch.begin(runtime.model.packageId, voiceId)
        } ?: return@withLock ReaderTtsInProcessOutcome.Interrupted
        var normalCompletion = false
        val anyPcmAccepted = AtomicBoolean(false)
        val pcmRejected = AtomicBoolean(false)
        try {
            val rate = runtime.sampleRateHz
            if (rate !in 8_000..96_000 ||
                runtime.model.expectedSampleRateHz?.let { it != rate } == true
            ) return@withLock ReaderTtsInProcessOutcome.Failed(
                ReaderTtsProblem.SYNTHESIS
            )
            if (!pcmSink.prepare(rate) { epoch.accepts(lease) }) {
                return@withLock if (epoch.accepts(lease)) {
                    ReaderTtsInProcessOutcome.Failed(ReaderTtsProblem.NO_ENGINE)
                } else {
                    ReaderTtsInProcessOutcome.Interrupted
                }
            }
            // Native inference may take seconds to initialize. If Pause was
            // pressed during that setup, do not invoke JNI synthesis at all.
            if (!epoch.accepts(lease)) return@withLock ReaderTtsInProcessOutcome.Interrupted
            val result = runtime.synthesize(
                text = text,
                voiceId = lease.voiceId,
                speed = speed
            ) { nativePcm ->
                // Never call JNI-generated callbacks while holding an epoch
                // lock: Pause must interrupt immediately from the UI thread.
                if (!readerTtsValidPcmChunk(epoch, lease, rate, nativePcm)) {
                    pcmRejected.set(true)
                    false
                } else {
                    // JNI callbacks may reuse FloatArrays. The sink must never
                    // retain or modify a pointer to engine-owned memory.
                    val detached = nativePcm.copyOf()
                    val ok = epoch.accepts(lease) &&
                        pcmSink.submit(detached) { epoch.accepts(lease) } &&
                        epoch.accepts(lease)
                    if (ok) anyPcmAccepted.set(true) else pcmRejected.set(true)
                    ok
                }
            }
            when {
                // A deliberate Pause revokes the native lease. Do not report
                // success (which could advance the chapter) or display an
                // engine-failure snackbar when the user merely paused.
                !epoch.accepts(lease) -> ReaderTtsInProcessOutcome.Interrupted
                // A native provider that ignores callback=false or produces
                // no samples must not pretend it narrated a paragraph.
                pcmRejected.get() || !anyPcmAccepted.get() ->
                    ReaderTtsInProcessOutcome.Failed(ReaderTtsProblem.SYNTHESIS)
                result != null -> ReaderTtsInProcessOutcome.Failed(result)
                endOfStream && !pcmSink.drain { epoch.accepts(lease) } ->
                    if (epoch.accepts(lease)) {
                        ReaderTtsInProcessOutcome.Failed(ReaderTtsProblem.SYNTHESIS)
                    } else {
                        ReaderTtsInProcessOutcome.Interrupted
                    }
                !epoch.accepts(lease) -> ReaderTtsInProcessOutcome.Interrupted
                else -> {
                    normalCompletion = true
                    ReaderTtsInProcessOutcome.Completed
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (epoch.accepts(lease)) {
                ReaderTtsInProcessOutcome.Failed(ReaderTtsProblem.SYNTHESIS)
            } else {
                ReaderTtsInProcessOutcome.Interrupted
            }
        } finally {
            if (epoch.accepts(lease)) {
                epoch.revoke()
                // Natural segment boundaries must not flush the hardware
                // PCM buffer; subsequent segments append to the same output.
                if (!normalCompletion) pcmSink.silenceImmediately()
            }
        }
        }
    }

    /** UI/MediaSession command: no suspension and no JNI owner mutex. */
    fun pause() {
        synchronized(transportGate) {
            commandGeneration.incrementAndGet()
            epoch.revoke()
            // Keep stop-and-flush in this transport transaction, so a
            // subsequent valid Play cannot be flushed by an older Pause.
            // This lock is never held around JNI generation or waiting for
            // a long audio drain.
            pcmSink.silenceImmediately()
        }
    }

    fun isSpeaking(): Boolean = epoch.hasOwner()

    fun selectedVoiceOrNull(): String? = epoch.selectedVoiceOrNull()
}
