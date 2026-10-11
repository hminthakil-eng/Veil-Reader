package com.veilreader.app.ui.reader.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Veil-owned Android PCM renderer for the opt-in in-process neural path.
 *
 * The real playback source is a mono float stream, never an external Android
 * TextToSpeechService. The UI can issue Pause synchronously while inference is
 * running on another thread; no native TTS locks are held during pause/flush.
 *
 * Synthesis should only hand this sink validated, detached float PCM from the
 * epoch-fenced ReaderTtsInProcessCoordinator. This class deliberately has no
 * model downloader, media-session owner or hidden provider fallback.
 *
 * This implementation is currently a review seam, not yet the production
 * Reader backend. Physical speaker/route timing acceptance is still required.
 */
internal class ReaderTtsAndroidAudioTrackSink : ReaderTtsOwnedPcmSink, AutoCloseable {
    private val audioLock = Any()
    private var audioTrack: AudioTrack? = null
    private var sampleRateHz = 0
    private var framesSubmitted = 0L
    private var closed = false

    override suspend fun prepare(
        sampleRateHz: Int,
        stillCurrent: () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (sampleRateHz !in 8_000..96_000 || !stillCurrent()) {
            return@withContext false
        }
        synchronized(audioLock) {
            if (closed || !stillCurrent()) return@synchronized false

            val existing = audioTrack
            if (existing != null && this@ReaderTtsAndroidAudioTrackSink.sampleRateHz == sampleRateHz &&
                existing.state == AudioTrack.STATE_INITIALIZED
            ) {
                if (existing.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    val resumed = runCatching { existing.play() }.isSuccess
                    return@synchronized resumed && stillCurrent()
                }
                return@synchronized stillCurrent()
            }

            // Format change cannot reuse queued frames from a different
            // sample rate. Clear old hardware state before releasing it.
            releaseTrackLocked()
            val minBytes = AudioTrack.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            if (minBytes <= 0) return@synchronized false
            val requestedBytes = maxOf(minBytes, (sampleRateHz / 5) * 4)
            val newTrack = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(sampleRateHz)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(requestedBytes)
                    .build()
            }.getOrNull() ?: return@synchronized false

            if (newTrack.state != AudioTrack.STATE_INITIALIZED) {
                runCatching { newTrack.release() }
                return@synchronized false
            }
            audioTrack = newTrack
            this@ReaderTtsAndroidAudioTrackSink.sampleRateHz = sampleRateHz
            framesSubmitted = 0L
            if (!stillCurrent() || runCatching { newTrack.play() }.isFailure) {
                releaseTrackLocked()
                return@synchronized false
            }
            true
        }
    }

    override suspend fun submit(
        pcm: FloatArray,
        stillCurrent: () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (pcm.isEmpty() || !stillCurrent()) return@withContext false
        var offset = 0
        var stalls = 0

        while (offset < pcm.size) {
            if (!stillCurrent()) return@withContext false
            // 20ms-or-less non-blocking writes: the Pause thread can acquire
            // audioLock quickly, flush the hardware queue, and fence stale PCM.
            val result = synchronized(audioLock) {
                if (closed || !stillCurrent()) return@synchronized AudioTrack.ERROR_INVALID_OPERATION
                val device = audioTrack
                    ?.takeIf { it.state == AudioTrack.STATE_INITIALIZED }
                    ?: return@synchronized AudioTrack.ERROR_INVALID_OPERATION
                if (device.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    return@synchronized AudioTrack.ERROR_INVALID_OPERATION
                }
                val maxFloats = minOf(pcm.size - offset, maxOf(128, sampleRateHz / 50))
                val count = runCatching {
                    device.write(
                        pcm,
                        offset,
                        maxFloats,
                        AudioTrack.WRITE_NON_BLOCKING
                    )
                }.getOrDefault(AudioTrack.ERROR)
                if (count > 0) framesSubmitted += count.toLong()
                count
            }

            when {
                result > 0 -> {
                    offset += result
                    stalls = 0
                }
                result == 0 -> {
                    // Do not busy-spin when Android's hardware buffer is full.
                    // A timeout/route failure must surface; it is not a
                    // successful synthesis with mysteriously missing audio.
                    if (++stalls > MAX_BACKPRESSURE_ATTEMPTS) {
                        return@withContext false
                    }
                    delay(BACKPRESSURE_DELAY_MS)
                }
                else -> return@withContext false // Including ERROR_DEAD_OBJECT.
            }
        }
        stillCurrent()
    }

    override suspend fun drain(stillCurrent: () -> Boolean): Boolean =
        withContext(Dispatchers.IO) {
            if (!stillCurrent()) return@withContext false
            val target = synchronized(audioLock) {
                if (closed || !stillCurrent()) return@synchronized -1L
                if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    return@synchronized -1L
                }
                framesSubmitted
            }
            if (target <= 0L) return@withContext false

            val headAtStart = synchronized(audioLock) { playbackFramesLocked() }
            if (headAtStart < 0L) return@withContext false
            // Bound waiting based on the maximum plausible unplayed tail,
            // rather than the total duration of a long audiobook chapter.
            val remainingMs =
                ((target - headAtStart).coerceAtLeast(0L) * 1_000L) /
                    sampleRateHz.coerceAtLeast(1)
            val budgetMs = (remainingMs + 3_000L).coerceIn(3_000L, 30_000L)
            val deadline = SystemClock.elapsedRealtime() + budgetMs
            while (stillCurrent() && SystemClock.elapsedRealtime() < deadline) {
                val played = synchronized(audioLock) {
                    if (closed || !stillCurrent() ||
                        audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING
                    ) -1L else playbackFramesLocked()
                }
                if (played < 0L) return@withContext false
                if (played >= target) return@withContext true
                delay(DRAIN_POLL_MS)
            }
            false
        }

    override fun silenceImmediately() {
        synchronized(audioLock) {
            // Pausing ALONE keeps previously queued sound. The flush after
            // pause discards all pending PCM, not just future JNI chunks.
            val track = audioTrack ?: return
            runCatching { track.pause() }
            runCatching { track.flush() }
            framesSubmitted = 0L
        }
    }

    override fun close() {
        synchronized(audioLock) {
            if (closed) return
            closed = true
            releaseTrackLocked()
        }
    }

    private fun playbackFramesLocked(): Long {
        val value = audioTrack?.playbackHeadPosition ?: return -1L
        // Android returns an unsigned 32-bit frame counter in a signed Int.
        return value.toLong() and 0xFFFF_FFFFL
    }

    private fun releaseTrackLocked() {
        audioTrack?.let { old ->
            runCatching { old.pause() }
            runCatching { old.flush() }
            runCatching { old.release() }
        }
        audioTrack = null
        sampleRateHz = 0
        framesSubmitted = 0L
    }

    private companion object {
        const val BACKPRESSURE_DELAY_MS = 5L
        const val MAX_BACKPRESSURE_ATTEMPTS = 600 // 3 seconds at 5ms.
        const val DRAIN_POLL_MS = 10L
    }
}
