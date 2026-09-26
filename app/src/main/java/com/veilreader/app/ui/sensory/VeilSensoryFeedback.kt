package com.veilreader.app.ui.sensory

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.HapticFeedbackConstants
import android.view.View
import com.veilreader.app.data.settings.AmbientSound
import com.veilreader.app.data.settings.SensorySettings
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class VeilSensoryEvent {
    PAGE_TURN,
    MARK,
    NOTE,
    RETURN_RITUAL,
    ADVANCEMENT,
    RELIC
}

/**
 * Local-only sensory layer for Veil Reader.
 *
 * It intentionally avoids network audio and bundled megabyte-scale samples.
 * Short feedback cues and ambient beds are synthesized in memory so the app
 * can stay silent by default while still supporting tactile/audio polish.
 */
class VeilSensoryFeedback(context: android.content.Context) {
    private val appContext = context.applicationContext
    private val cueExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "veil-sensory-cue").apply { isDaemon = true }
    }
    private val ambientExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "veil-sensory-ambient").apply { isDaemon = true }
    }
    private val ambientGeneration = AtomicInteger(0)

    @Volatile
    private var settings = SensorySettings()

    @Volatile
    private var foreground = true

    fun update(value: SensorySettings) {
        settings = value.copy(
            audioVolume = value.audioVolume
                .takeIf { it.isFinite() }
                ?.coerceIn(0.0, 0.55)
                ?: 0.18
        )
        syncAmbient()
    }

    fun setForeground(value: Boolean) {
        if (foreground == value) return
        foreground = value
        syncAmbient()
    }

    fun perform(view: View, event: VeilSensoryEvent) {
        val snapshot = settings
        if (snapshot.hapticsEnabled) {
            val feedback = when (event) {
                VeilSensoryEvent.PAGE_TURN -> HapticFeedbackConstants.CLOCK_TICK
                VeilSensoryEvent.MARK,
                VeilSensoryEvent.NOTE -> HapticFeedbackConstants.KEYBOARD_TAP
                VeilSensoryEvent.RETURN_RITUAL -> HapticFeedbackConstants.CONTEXT_CLICK
                VeilSensoryEvent.ADVANCEMENT -> HapticFeedbackConstants.LONG_PRESS
                VeilSensoryEvent.RELIC -> HapticFeedbackConstants.CONTEXT_CLICK
            }
            view.performHapticFeedback(feedback)
        }

        if (
            snapshot.interactionSoundsEnabled &&
            snapshot.audioVolume > 0.0 &&
            foreground
        ) {
            cueExecutor.execute {
                playCue(event, snapshot.audioVolume.toFloat())
            }
        }
    }

    fun dispose() {
        ambientGeneration.incrementAndGet()
        cueExecutor.shutdownNow()
        ambientExecutor.shutdownNow()
    }

    private fun syncAmbient() {
        val token = ambientGeneration.incrementAndGet()
        val snapshot = settings
        if (
            !foreground ||
            snapshot.ambientSound == AmbientSound.OFF ||
            snapshot.audioVolume <= 0.0
        ) {
            return
        }

        ambientExecutor.execute {
            if (token != ambientGeneration.get()) return@execute
            streamAmbient(
                mode = snapshot.ambientSound,
                volume = snapshot.audioVolume.toFloat(),
                token = token
            )
        }
    }

    private fun playCue(event: VeilSensoryEvent, volume: Float) {
        val sampleRate = 22_050
        val seconds = when (event) {
            VeilSensoryEvent.PAGE_TURN -> 0.085
            VeilSensoryEvent.MARK -> 0.070
            VeilSensoryEvent.NOTE -> 0.095
            VeilSensoryEvent.RETURN_RITUAL -> 0.240
            VeilSensoryEvent.ADVANCEMENT -> 0.280
            VeilSensoryEvent.RELIC -> 0.180
        }
        val sampleCount = (sampleRate * seconds).toInt().coerceAtLeast(64)
        val pcm = ShortArray(sampleCount)
        var seed = 0x5A17C3D
        var smoothNoise = 0.0

        for (i in pcm.indices) {
            val t = i.toDouble() / sampleRate.toDouble()
            val unit = i.toDouble() / pcm.lastIndex.coerceAtLeast(1).toDouble()
            seed = seed * 1103515245 + 12345
            val white = (((seed ushr 16) and 0x7FFF) / 16383.5) - 1.0
            smoothNoise = smoothNoise * 0.78 + white * 0.22

            val raw = when (event) {
                VeilSensoryEvent.PAGE_TURN -> {
                    val envelope = sin(PI * unit).coerceAtLeast(0.0)
                    smoothNoise * envelope * 0.58 +
                        sin(2.0 * PI * 92.0 * t) * envelope * 0.035
                }
                VeilSensoryEvent.MARK -> {
                    val envelope = exp(-t * 30.0)
                    (
                        sin(2.0 * PI * 860.0 * t) * 0.58 +
                            sin(2.0 * PI * 1290.0 * t) * 0.20
                        ) * envelope
                }
                VeilSensoryEvent.NOTE -> {
                    val envelope = exp(-t * 22.0)
                    (
                        sin(2.0 * PI * 620.0 * t) * 0.48 +
                            sin(2.0 * PI * 930.0 * t) * 0.22
                        ) * envelope
                }
                VeilSensoryEvent.RETURN_RITUAL -> {
                    val attack = (unit / 0.18).coerceIn(0.0, 1.0)
                    val release = (1.0 - unit).coerceIn(0.0, 1.0)
                    val envelope = attack * release * release
                    (
                        sin(2.0 * PI * 196.0 * t) * 0.28 +
                            sin(2.0 * PI * 293.66 * t) * 0.17 +
                            smoothNoise * 0.10
                        ) * envelope
                }
                VeilSensoryEvent.ADVANCEMENT -> {
                    val attack = (unit / 0.12).coerceIn(0.0, 1.0)
                    val release = (1.0 - unit).coerceIn(0.0, 1.0)
                    val envelope = attack * release * release
                    (
                        sin(2.0 * PI * 392.0 * t) * 0.40 +
                            sin(2.0 * PI * 523.25 * t) * 0.30 +
                            sin(2.0 * PI * 659.25 * t) * 0.22
                        ) * envelope
                }
                VeilSensoryEvent.RELIC -> {
                    val envelope = exp(-t * 9.0)
                    (
                        sin(2.0 * PI * 587.33 * t) * 0.42 +
                            sin(2.0 * PI * 880.0 * t) * 0.20
                        ) * envelope
                }
            }

            val scaled = raw * volume.coerceIn(0f, 0.55f) * 0.72
            pcm[i] = (scaled.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, pcm.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        }.getOrNull() ?: return

        try {
            if (track.write(pcm, 0, pcm.size) > 0) {
                track.play()
                Thread.sleep((seconds * 1000.0).toLong() + 24L)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun streamAmbient(
        mode: AmbientSound,
        volume: Float,
        token: Int
    ) {
        val sampleRate = 22_050
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return

        val bufferSamples = maxOf(1024, minBuffer / 2)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer * 2, bufferSamples * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        val random = Random(0x5645494CL + mode.ordinal)
        val buffer = ShortArray(bufferSamples)
        var low = 0.0
        var brown = 0.0
        var crackle = 0.0
        var phase = 0L

        try {
            track.play()
            while (
                token == ambientGeneration.get() &&
                foreground &&
                settings.ambientSound == mode
            ) {
                val liveVolume = settings.audioVolume
                    .takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 0.55)
                    ?: volume.toDouble()

                for (i in buffer.indices) {
                    val white = random.nextDouble() * 2.0 - 1.0
                    low = low * 0.94 + white * 0.06
                    brown = (brown * 0.985 + white * 0.015).coerceIn(-1.0, 1.0)

                    val sample = when (mode) {
                        AmbientSound.OFF -> 0.0
                        AmbientSound.LIBRARY -> {
                            val hum = sin(
                                2.0 * PI * 52.0 *
                                    (phase.toDouble() / sampleRate.toDouble())
                            ) * 0.018
                            low * 0.050 + hum
                        }
                        AmbientSound.RAIN -> {
                            val high = white - low
                            high * 0.105 + low * 0.035
                        }
                        AmbientSound.FIRE -> {
                            if (random.nextDouble() < 0.0012) {
                                crackle = 0.35 + random.nextDouble() * 0.45
                            }
                            crackle *= 0.955
                            brown * 0.085 + crackle * white
                        }
                    }

                    val scaled = sample * liveVolume * 0.62
                    buffer[i] = (
                        scaled.coerceIn(-1.0, 1.0) * Short.MAX_VALUE
                        ).toInt().toShort()
                    phase++
                }

                val written = track.write(
                    buffer,
                    0,
                    buffer.size,
                    AudioTrack.WRITE_BLOCKING
                )
                if (written < 0) break
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }
}
