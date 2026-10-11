package com.veilreader.app.ui.reader.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Small adapter around Android AudioTrack for deterministic fake-backed tests.
 * The native output is MONO 32-bit floating point PCM.
 */
internal interface ReaderTtsAudioDevice {
    val sampleRateHz: Int
    val playbackHeadPosition: Long
    val isPlaying: Boolean
    fun play()
    /** Must use WRITE_NON_BLOCKING and return number of PCM frames accepted. */
    fun write(pcm: FloatArray, offset: Int, length: Int): Int
    /** Pause, drop queued PCM and release all native audio resources. */
    fun silenceAndRelease()
}

internal class ReaderTtsPlatformAudioDevice(
    private val output: AudioTrack,
    override val sampleRateHz: Int
) : ReaderTtsAudioDevice {
    override val playbackHeadPosition: Long
        get() = output.playbackHeadPosition.toLong() and 0xffffffffL
    override val isPlaying: Boolean
        get() = output.playState == AudioTrack.PLAYSTATE_PLAYING

    override fun play() = output.play()

    override fun write(pcm: FloatArray, offset: Int, length: Int): Int =
        output.write(pcm, offset, length, AudioTrack.WRITE_NON_BLOCKING)

    override fun silenceAndRelease() {
        runCatching { output.pause() }
        runCatching { output.flush() }
        output.release()
    }

    internal companion object {
        fun open(sampleRateHz: Int): ReaderTtsAudioDevice? {
            if (sampleRateHz !in 8_000..96_000) return null
            val minimum = AudioTrack.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            if (minimum <= 0 || minimum > 1024 * 1024) return null
            val desiredBytes = ((sampleRateHz.toLong() * 4L * 180L) / 1000L)
                .coerceIn(minimum.toLong(), (1024 * 1024).toLong()).toInt()
            val player = AudioTrack.Builder()
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
                .setBufferSizeInBytes(desiredBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            if (player.state != AudioTrack.STATE_INITIALIZED) {
                player.release()
                return null
            }
            return ReaderTtsPlatformAudioDevice(player, sampleRateHz)
        }
    }
}

/**
 * Veil-owned hardware output for optional, in-process neural inference.
 *
 * All writes use short NON_BLOCKING slices. A tiny monitor synchronizes the
 * final epoch check + audio write with Pause's pause/flush/release operation.
 * The monitor is NEVER held during JNI model inference, coroutine delay or
 * native model loading; Pause does not wait for neural model generation.
 *
 * One prepared AudioTrack is reused between ordinary sentences to avoid
 * audible gaps. A Pause destroys it; explicit next Play creates a fresh one.
 *
 * This sink is intentionally not wired to the production Reader until JNI
 * and exact-book device acceptance pass.
 */
internal class ReaderTtsAndroidPcmSink(
    private val openDevice: (Int) -> ReaderTtsAudioDevice? =
        ReaderTtsPlatformAudioDevice::open,
    private val clockMs: () -> Long = SystemClock::elapsedRealtime
) : ReaderTtsOwnedPcmSink, AutoCloseable {
    private val lock = Any()
    private var current: ReaderTtsAudioDevice? = null
    private var writtenFrames = 0L
    private var lastRawHead = 0L
    private var headWraps = 0L

    override suspend fun prepare(
        sampleRateHz: Int,
        stillCurrent: () -> Boolean
    ): Boolean {
        if (!stillCurrent() || sampleRateHz !in 8_000..96_000) return false
        synchronized(lock) {
            current?.let { existing ->
                return stillCurrent() && existing.sampleRateHz == sampleRateHz
            }
        }

        val built = withContext(Dispatchers.IO) {
            runCatching { openDevice(sampleRateHz) }.getOrNull()
        } ?: return false
        synchronized(lock) {
            if (!stillCurrent() || current != null) {
                built.silenceAndRelease()
                return false
            }
            current = built
            writtenFrames = 0L
            lastRawHead = 0L
            headWraps = 0L
            return true
        }
    }

    override suspend fun submit(
        pcm: FloatArray,
        stillCurrent: () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (pcm.isEmpty()) return@withContext false
        var offset = 0
        // A non-blocking short write plus a short deadline protects both UI
        // latency and us from endless zero-progress writes on broken drivers.
        val deadline = clockMs() + WRITE_STALL_TIMEOUT_MS
        while (offset < pcm.size) {
            val n = synchronized(lock) {
                val device = current
                if (!stillCurrent() || device == null) return@withContext false
                try {
                    if (!device.isPlaying) device.play()
                    val slice = minOf(1024, pcm.size - offset)
                    val written = device.write(pcm, offset, slice)
                    if (written > slice) -1 else written
                } catch (_: RuntimeException) {
                    -1
                }
            }
            if (n < 0) return@withContext false
            if (n == 0) {
                if (!stillCurrent() || clockMs() >= deadline) return@withContext false
                delay(5)
                continue
            }
            synchronized(lock) {
                // Pause could have run after AudioTrack.write returned.
                // The hardware was flushed by Pause, so do not count the
                // pre-Pause frames as successfully delivered afterwards.
                if (!stillCurrent() || current == null) return@withContext false
                writtenFrames += n
            }
            offset += n
        }
        stillCurrent()
    }

    override suspend fun drain(stillCurrent: () -> Boolean): Boolean {
        val deadline = clockMs() + DRAIN_TIMEOUT_MS
        while (stillCurrent()) {
            val done = synchronized(lock) {
                val device = current ?: return false
                val raw = device.playbackHeadPosition
                // Raw AudioTrack playback head is a wrapping unsigned 32-bit
                // frame counter. Never treat its high bit as negative.
                if (raw < lastRawHead && lastRawHead - raw > (1L shl 31)) {
                    headWraps++
                }
                lastRawHead = raw
                val played = (headWraps shl 32) + raw
                if (!stillCurrent()) return false
                played >= writtenFrames
            }
            if (done) return stillCurrent()
            if (clockMs() >= deadline) return false
            delay(10)
        }
        return false
    }

    override fun silenceImmediately() {
        synchronized(lock) {
            val old = current
            current = null
            writtenFrames = 0
            lastRawHead = 0L
            headWraps = 0L
            old?.silenceAndRelease()
        }
    }

    override fun close() = silenceImmediately()

    private companion object {
        const val WRITE_STALL_TIMEOUT_MS = 3_000L
        const val DRAIN_TIMEOUT_MS = 8_000L
    }
}
