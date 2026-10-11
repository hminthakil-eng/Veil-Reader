package com.veilreader.app.ui.reader.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.veilreader.app.BuildConfig
import com.veilreader.app.domain.ReaderTtsEngineChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Debug-only bridge into the *real EPUB* ReaderTtsSession.
 *
 * Do not expose this implementation in release, which has not passed physical
 * quality/latency gates. SHERPA_ONNX remains the externally installed provider
 * in normal builds; only the isolated Forge QA variant uses in-process JNI.
 */
internal class ReaderTtsForgeBackendFactory(private val context: Context) {
    fun create(): ReaderTtsBackend =
        if (BuildConfig.DEBUG && BuildConfig.FORGE_QA) {
            ReaderTtsForgeBackend(context.applicationContext)
        } else {
            AndroidReaderTtsBackend(context.applicationContext)
        }
}

private class ReaderTtsForgeBackend(context: Context) : ReaderTtsBackend {
    private val application = context.applicationContext
    private var engine: ReaderTtsBackend? = null
    private var selected: ReaderTtsEngineChoice? = null
    override var onInterruption: ((ReaderTtsInterruption) -> Unit)? = null
        set(value) {
            field = value
            engine?.onInterruption = value
        }
    override var onFocusGained: (() -> Unit)? = null
        set(value) {
            field = value
            engine?.onFocusGained = value
        }
    override val voices get() = engine?.voices.orEmpty()

    override fun selectEngine(choice: ReaderTtsEngineChoice) {
        if (selected == choice) return
        check(engine == null) { "Changing speech engines requires closing this owner" }
        selected = choice
    }

    private fun resolved(): ReaderTtsBackend {
        engine?.let { return it }
        val choice = selected ?: ReaderTtsEngineChoice.SYSTEM
        val result = if (choice == ReaderTtsEngineChoice.SHERPA_ONNX) {
            ReaderTtsForgeKokoroBackend(application)
        } else {
            AndroidReaderTtsBackend(application)
        }
        result.selectEngine(choice)
        result.onInterruption = onInterruption
        result.onFocusGained = onFocusGained
        engine = result
        return result
    }

    override suspend fun initialize(): ReaderTtsProblem? = resolved().initialize()
    override suspend fun speak(
        text: String,
        languageTag: String,
        preferences: ReaderTtsPreferences
    ): ReaderTtsProblem? = resolved().speak(text, languageTag, preferences)

    override fun stop(abandonAudioFocus: Boolean) {
        engine?.stop(abandonAudioFocus)
    }

    override fun close() {
        engine?.close()
        engine = null
        selected = null
    }
}

/**
 * Same-process native speech with Veil-owned output, media focus and cleanup.
 *
 * A model is available only in the special Forge QA APK. This class never
 * downloads one or looks for a separate Android TTS provider application.
 */
private class ReaderTtsForgeKokoroBackend(context: Context) : ReaderTtsBackend {
    private val application = context.applicationContext
    private val audio = application.getSystemService(AudioManager::class.java)
    private val focusHandler = Handler(Looper.getMainLooper())
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runtime: ReaderTtsSherpaKokoroRuntime? = null
    private val sink = ReaderTtsAndroidAudioTrackSink()
    private var coordinator: ReaderTtsInProcessCoordinator? = null
    private var focusHeld = false
    private var receiverRegistered = false
    private var active = false
    private var resumeAfterTransientLoss = false
    private var closed = false
    private var pinnedVoice: String? = null
    override var onInterruption: ((ReaderTtsInterruption) -> Unit)? = null
    override var onFocusGained: (() -> Unit)? = null
    override var voices: List<ReaderTtsVoice> = emptyList()
        private set

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ focusChange ->
            if (closed) return@setOnAudioFocusChangeListener
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusHeld = true
                    if (resumeAfterTransientLoss) {
                        resumeAfterTransientLoss = false
                        onFocusGained?.invoke()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    focusHeld = false
                    resumeAfterTransientLoss = active
                    if (active) onInterruption?.invoke(ReaderTtsInterruption.TRANSIENT_FOCUS)
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    focusHeld = false
                    resumeAfterTransientLoss = false
                    if (active) onInterruption?.invoke(ReaderTtsInterruption.PERMANENT_FOCUS)
                }
            }
        }, focusHandler)
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!closed && intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                resumeAfterTransientLoss = false
                onInterruption?.invoke(ReaderTtsInterruption.BECOMING_NOISY)
            }
        }
    }

    override fun selectEngine(choice: ReaderTtsEngineChoice) {
        check(choice == ReaderTtsEngineChoice.SHERPA_ONNX && BuildConfig.DEBUG &&
            BuildConfig.FORGE_QA) { "In-process Kokoro is only available in Forge QA" }
    }

    override suspend fun initialize(): ReaderTtsProblem? {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (closed) return ReaderTtsProblem.NO_ENGINE
        if (runtime != null) return null
        val loaded = ReaderTtsSherpaKokoroRuntime.openBundledDebugAssets(application.assets)
        if (closed) {
            if (loaded is ReaderTtsKokoroOpenResult.Ready) loaded.runtime.close()
            return ReaderTtsProblem.NO_ENGINE
        }
        if (loaded !is ReaderTtsKokoroOpenResult.Ready) return ReaderTtsProblem.NO_ENGINE
        runtime = loaded.runtime
        coordinator = ReaderTtsInProcessCoordinator(loaded.runtime, sink)
        // The QA package has one pinned model; actual speaker count is
        // checked natively in open(). Do not show an OS-provided narrator.
        voices = (0..10).map { id ->
            ReaderTtsVoice("kokoro-en-v0_19-$id", "en-US", 400, false, true)
        }
        return null
    }

    override suspend fun speak(
        text: String, languageTag: String, preferences: ReaderTtsPreferences
    ): ReaderTtsProblem? {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (closed) return ReaderTtsProblem.NO_ENGINE
        if (Locale.forLanguageTag(languageTag).language != "en") {
            return ReaderTtsProblem.NO_OFFLINE_VOICE
        }
        val player = coordinator ?: return ReaderTtsProblem.NO_ENGINE
        val desired = preferences.preferredVoiceId(languageTag) ?: pinnedVoice
            ?: "kokoro-en-v0_19-0"
        if (voices.none { it.id == desired }) return ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE
        if (!focusHeld) {
            focusHeld = audio?.requestAudioFocus(focusRequest) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!focusHeld) return ReaderTtsProblem.AUDIO_FOCUS
        }
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                application, noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
        pinnedVoice = desired
        active = true
        return try {
            when (val outcome = player.speak(text, desired, preferences.speed)) {
                ReaderTtsInProcessOutcome.Completed -> null
                ReaderTtsInProcessOutcome.Interrupted -> ReaderTtsProblem.SYNTHESIS
                is ReaderTtsInProcessOutcome.Failed -> outcome.problem
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            active = false
        }
    }

    override fun stop(abandonAudioFocus: Boolean) {
        // The coordinator revokes JNI generation and flushes audible PCM
        // synchronously; no lock in the native model is required for Pause.
        coordinator?.pause()
        active = false
        if (abandonAudioFocus) {
            resumeAfterTransientLoss = false
            runCatching { audio?.abandonAudioFocusRequest(focusRequest) }
            focusHeld = false
            if (receiverRegistered) {
                runCatching { application.unregisterReceiver(noisyReceiver) }
                receiverRegistered = false
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        stop(true)
        val previous = runtime
        runtime = null
        coordinator = null
        voices = emptyList()
        // Native free must wait until generation callback completes.
        previous?.close()
        cleanupScope.launch {
            try { sink.close() } finally { cleanupScope.cancel() }
        }
    }
}
