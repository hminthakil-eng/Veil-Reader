package com.veilreader.app.ui.reader.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.veilreader.app.domain.ReaderTtsEngineChoice
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/** Main-thread owner. All Binder callbacks are marshalled to this owner's private Handler. */
internal class AndroidReaderTtsBackend(context: Context) : ReaderTtsBackend {
    private val application = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val audio = application.getSystemService(AudioManager::class.java)
    private var engine: TextToSpeech? = null
    private var selectedEngine = ReaderTtsEngineChoice.SYSTEM
    override fun selectEngine(choice: ReaderTtsEngineChoice) {
        checkMainThread()
        // Sessions close/recreate the native backend on a user engine switch.
        check(engine == null || selectedEngine == choice) {
            "TTS engine cannot change while initialized"
        }
        selectedEngine = choice
    }
    private var closed = false
    private var initialized = false
    private var initializationGeneration = 0L
    private var receiverRegistered = false
    private var focusHeld = false
    private var resumeOnFocusGain = false
    private var requestSequence = 0L
    // Android TTS voice inventory/order can change while a book is playing.
    // Lock the resolved offline voice until the user explicitly selects another.
    private var narratorVoiceId: String? = null
    private var narratorLanguageTag: String? = null
    private var activeRequestId: String? = null
    private var activeContinuation: CancellableContinuation<ReaderTtsProblem?>? = null
    override var onInterruption: ((ReaderTtsInterruption) -> Unit)? = null
    override var onFocusGained: (() -> Unit)? = null
    override var voices: List<ReaderTtsVoice> = emptyList()
        private set

    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change ->
            if (closed) return@setOnAudioFocusChangeListener
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusHeld = true
                    if (resumeOnFocusGain) {
                        resumeOnFocusGain = false
                        onFocusGained?.invoke()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    focusHeld = false
                    resumeOnFocusGain = activeRequestId != null
                    if (resumeOnFocusGain) {
                        onInterruption?.invoke(ReaderTtsInterruption.TRANSIENT_FOCUS)
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    focusHeld = false
                    resumeOnFocusGain = false
                    if (activeRequestId != null) {
                        onInterruption?.invoke(ReaderTtsInterruption.PERMANENT_FOCUS)
                    }
                }
            }
        }, handler).build()

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!closed && intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                resumeOnFocusGain = false
                onInterruption?.invoke(ReaderTtsInterruption.BECOMING_NOISY)
            }
        }
    }

    override suspend fun initialize(): ReaderTtsProblem? {
        checkMainThread()
        if (closed) return ReaderTtsProblem.NO_ENGINE
        if (engine != null) return if (initialized) null else ReaderTtsProblem.NO_ENGINE
        val generation = ++initializationGeneration
        return suspendCancellableCoroutine { continuation ->
            try {
                val callback = TextToSpeech.OnInitListener { status ->
                    handler.post {
                        if (!continuation.isActive || closed || generation != initializationGeneration) return@post
                        if (status != TextToSpeech.SUCCESS) {
                            releaseEngine()
                            continuation.resume(ReaderTtsProblem.NO_ENGINE)
                        } else {
                            val target = engine
                            if (target == null) {
                                continuation.resume(ReaderTtsProblem.NO_ENGINE)
                            } else {
                                if (selectedEngine == ReaderTtsEngineChoice.SHERPA_ONNX &&
                                    target.currentEngine != SHERPA_ANDROID_TTS_PACKAGE
                                ) {
                                    releaseEngine()
                                    continuation.resume(ReaderTtsProblem.NO_ENGINE)
                                    return@post
                                }
                                voices = runCatching { target.voices.orEmpty().map { voice ->
                                    ReaderTtsVoice(voice.name, voice.locale.toLanguageTag(), voice.quality,
                                        voice.isNetworkConnectionRequired,
                                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty())
                                } }.getOrDefault(emptyList())
                                initialized = true
                                target.setOnUtteranceProgressListener(progressListener)
                                continuation.resume(null)
                            }
                        }
                    }
                }
                // Explicit SHERPA_ONNX is intentionally fail-closed: if the
                // external official offline engine is unavailable, do not use
                // the phone's default Android narrator without telling user.
                engine = if (selectedEngine == ReaderTtsEngineChoice.SHERPA_ONNX) {
                    TextToSpeech(application, callback, SHERPA_ANDROID_TTS_PACKAGE)
                } else {
                    TextToSpeech(application, callback)
                }
                continuation.invokeOnCancellation {
                    val cleanup = {
                        if (generation == initializationGeneration) releaseEngine()
                    }
                    if (Looper.myLooper() == Looper.getMainLooper()) cleanup()
                    else handler.post { cleanup() }
                }
            } catch (_: Exception) {
                releaseEngine()
                if (continuation.isActive) continuation.resume(ReaderTtsProblem.NO_ENGINE)
            }
        }
    }

    override suspend fun speak(
        text: String,
        languageTag: String,
        preferences: ReaderTtsPreferences
    ): ReaderTtsProblem? {
        checkMainThread()
        val target = engine ?: return ReaderTtsProblem.NO_ENGINE
        if (closed) return ReaderTtsProblem.NO_ENGINE
        val safe = preferences.normalized()
        // Query actual current voices again: installation/removal must not trigger a default fallback.
        val available = runCatching { target.voices.orEmpty() }.getOrDefault(emptySet())
        val metadata = available.map { voice -> ReaderTtsVoice(
            voice.name, voice.locale.toLanguageTag(), voice.quality, voice.isNetworkConnectionRequired,
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
        ) }
        val preferredVoiceId = safe.preferredVoiceId(languageTag)
        val stableVoiceId = preferredVoiceId
            ?: narratorVoiceId?.takeIf { narratorLanguageTag == languageTag }
        val chosen = selectPinnedOfflineTtsVoice(
            voices = metadata,
            languageTag = languageTag,
            explicitPreferredId = preferredVoiceId,
            pinnedVoiceId = narratorVoiceId?.takeIf { narratorLanguageTag == languageTag }
        )
            ?: return if (stableVoiceId != null) {
                ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE
            } else {
                ReaderTtsProblem.NO_OFFLINE_VOICE
            }
        val nativeVoice = available.firstOrNull { it.name == chosen.id }
            ?: return ReaderTtsProblem.NO_OFFLINE_VOICE
        if (target.setVoice(nativeVoice) != TextToSpeech.SUCCESS ||
            target.voice?.name != chosen.id || target.voice?.isNetworkConnectionRequired != false) {
            return ReaderTtsProblem.NO_OFFLINE_VOICE
        }
        // A missing previously resolved voice is a visible error; never let Android
        // silently switch to a different installed voice between two sentences.
        narratorVoiceId = chosen.id
        narratorLanguageTag = languageTag
        if (target.setSpeechRate(safe.speed) != TextToSpeech.SUCCESS ||
            target.setPitch(safe.pitch) != TextToSpeech.SUCCESS) return ReaderTtsProblem.SYNTHESIS
        if (!focusHeld) {
            focusHeld = audio?.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!focusHeld) return ReaderTtsProblem.AUDIO_FOCUS
        }
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(application, noisy,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        return suspendCancellableCoroutine { continuation ->
            val requestId = "veil-tts-${++requestSequence}"
            activeRequestId = requestId
            activeContinuation = continuation
            continuation.invokeOnCancellation {
                if (Looper.myLooper() == Looper.getMainLooper()) stopRequest(requestId)
                else handler.post { stopRequest(requestId) }
            }
            if (target.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), requestId) != TextToSpeech.SUCCESS) {
                completeRequest(requestId, ReaderTtsProblem.SYNTHESIS)
            }
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) {
            handler.post { completeRequest(utteranceId, null) }
        }
        @Deprecated("Required platform fallback")
        override fun onError(utteranceId: String?) {
            handler.post { completeRequest(utteranceId, ReaderTtsProblem.SYNTHESIS) }
        }
        override fun onError(utteranceId: String?, errorCode: Int) {
            handler.post { completeRequest(utteranceId, ReaderTtsProblem.SYNTHESIS) }
        }
        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            handler.post {
                if (activeRequestId == utteranceId) {
                    resumeOnFocusGain = false
                    onInterruption?.invoke(ReaderTtsInterruption.PERMANENT_FOCUS)
                }
            }
        }
    }

    private fun completeRequest(id: String?, problem: ReaderTtsProblem?) {
        if (closed || id == null || id != activeRequestId) return
        val continuation = activeContinuation
        activeContinuation = null
        activeRequestId = null
        if (continuation?.isActive == true) continuation.resume(problem)
    }

    private fun stopRequest(id: String) {
        if (activeRequestId != id) return
        activeRequestId = null
        activeContinuation = null
        engine?.stop()
    }

    override fun stop(abandonAudioFocus: Boolean) {
        checkMainThread()
        val continuation = activeContinuation
        activeRequestId = null
        activeContinuation = null
        continuation?.cancel()
        runCatching { engine?.stop() }
        if (abandonAudioFocus) {
            resumeOnFocusGain = false
            runCatching { audio?.abandonAudioFocusRequest(focus) }
            focusHeld = false
            if (receiverRegistered) runCatching { application.unregisterReceiver(noisy) }
            receiverRegistered = false
        }
    }

    override fun close() {
        checkMainThread()
        if (closed) return
        closed = true
        stop()
        onInterruption = null
        onFocusGained = null
        handler.removeCallbacksAndMessages(null)
        releaseEngine()
        voices = emptyList()
    }

    private fun releaseEngine() {
        initializationGeneration += 1
        initialized = false
        narratorVoiceId = null
        narratorLanguageTag = null
        val released = engine
        engine = null
        runCatching { released?.setOnUtteranceProgressListener(null) }
        runCatching { released?.stop() }
        runCatching { released?.shutdown() }
    }

    private fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper())

    private companion object {
        // Verified against k2-fsa official SherpaOnnxTtsEngine AndroidManifest.
        const val SHERPA_ANDROID_TTS_PACKAGE = "com.k2fsa.sherpa.onnx.tts.engine"
    }
}
