package com.veilreader.app.ui.reader.tts

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.veilreader.app.domain.ReaderTtsSettings
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Activity-side client for the service-owned TTS session.
 *
 * Releasing this controller never stops background speech. Playback lifetime belongs to
 * [ReaderTtsPlaybackService], which is the reason screen lock/activity recreation can be safe.
 */
@androidx.annotation.OptIn(UnstableApi::class)
internal class ReaderTtsServiceController(context: Context) : AutoCloseable {
    private val application = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(application)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val checkpointStore = ReaderTtsCheckpointStore(application)
    private val closed = AtomicBoolean(false)
    private val controllerFuture = MediaController.Builder(
        application,
        SessionToken(
            application,
            ComponentName(application, ReaderTtsPlaybackService::class.java)
        )
    ).buildAsync()

    private var controller: MediaController? = null
    private var pendingStart: PendingStart? = null
    // Pause pressed while MediaController reconnects must also stop an already
    // playing service owner, not only change the queued new-book autoplay.
    private var pendingPauseOnConnect = false

    private val mutableState = MutableStateFlow(ReaderTtsState())
    val state: StateFlow<ReaderTtsState> = mutableState.asStateFlow()

    private val mutableCheckpoint = MutableStateFlow<ReaderTtsCheckpoint?>(null)
    val checkpoint: StateFlow<ReaderTtsCheckpoint?> = mutableCheckpoint.asStateFlow()

    private val mutableVoices = MutableStateFlow<List<ReaderTtsVoice>>(emptyList())
    val voices: StateFlow<List<ReaderTtsVoice>> = mutableVoices.asStateFlow()

    private val mutableVoiceCatalogLoading = MutableStateFlow(false)
    val voiceCatalogLoading: StateFlow<Boolean> = mutableVoiceCatalogLoading.asStateFlow()

    private val mutableVoiceCatalogProblem = MutableStateFlow<ReaderTtsProblem?>(null)
    val voiceCatalogProblem: StateFlow<ReaderTtsProblem?> =
        mutableVoiceCatalogProblem.asStateFlow()

    private val mutablePreviewProblem = MutableStateFlow<ReaderTtsProblem?>(null)
    val previewProblem: StateFlow<ReaderTtsProblem?> = mutablePreviewProblem.asStateFlow()

    private val mutableSleepDeadlineEpochMs = MutableStateFlow<Long?>(null)
    val sleepDeadlineEpochMs: StateFlow<Long?> =
        mutableSleepDeadlineEpochMs.asStateFlow()

    private val mutableActiveSegmentText = MutableStateFlow<String?>(null)
    val activeSegmentText: StateFlow<String?> = mutableActiveSegmentText.asStateFlow()

    val connected: Boolean
        get() = controller != null && !closed.get()

    init {
        scope.launch {
            checkpointStore.checkpoints.collect { latest ->
                if (!closed.get()) {
                    mutableCheckpoint.value = latest
                    refreshActiveSegment()
                }
            }
        }
        controllerFuture.addListener(
            {
                if (closed.get()) return@addListener
                val connectedController = runCatching { controllerFuture.get() }.getOrNull()
                if (connectedController == null) {
                    mutableState.value = ReaderTtsState(
                        phase = ReaderTtsPhase.FAILED,
                        problem = ReaderTtsProblem.NO_ENGINE
                    )
                    return@addListener
                }
                controller = connectedController
                connectedController.addListener(playerListener)
                syncState(connectedController)
                refreshActiveSegment()
                refreshVoiceCatalog()
                refreshSleepTimer()
                if (pendingPauseOnConnect) {
                    pendingPauseOnConnect = false
                    connectedController.sendCustomCommand(
                        SessionCommand(ReaderTtsPlaybackRequest.ACTION_PAUSE_OWNER, Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                }
                pendingStart?.also {
                    pendingStart = null
                    start(it.bookId, it.locatorJson, it.settings, autoplay = it.autoplay)
                }
            },
            mainExecutor
        )
    }

    fun start(
        bookId: String,
        locatorJson: String,
        settings: ReaderTtsSettings,
        autoplay: Boolean = true
    ) {
        if (closed.get()) return
        pendingPauseOnConnect = !autoplay
        val target = controller
        if (target == null) {
            pendingStart = PendingStart(bookId, locatorJson, settings.normalized(), autoplay)
            mutableState.value = mutableState.value.copy(
                phase = if (autoplay) ReaderTtsPhase.PREPARING else ReaderTtsPhase.PAUSED,
                problem = null
            )
            return
        }

        val safeSettings = settings.normalized()
        val request = ReaderTtsPlaybackRequest(
            bookId = bookId,
            locatorJson = locatorJson,
            preferences = ReaderTtsPreferences(
                speed = safeSettings.speed.toFloat(),
                pitch = safeSettings.pitch.toFloat(),
                engine = safeSettings.engine,
                preferredVoiceIds = safeSettings.preferredVoiceIds
            )
        ).normalized() ?: run {
            mutableState.value = ReaderTtsState(
                phase = ReaderTtsPhase.FAILED,
                problem = ReaderTtsProblem.CONTENT
            )
            return
        }

        mutableState.value = mutableState.value.copy(
            phase = if (autoplay) ReaderTtsPhase.PREPARING else ReaderTtsPhase.PAUSED,
            problem = null
        )
        target.sendCustomCommand(
            SessionCommand(
                if (autoplay) ReaderTtsPlaybackRequest.ACTION_LOAD_AND_PLAY
                else ReaderTtsPlaybackRequest.ACTION_LOAD_PAUSED,
                Bundle.EMPTY
            ),
            request.toBundle()
        )
    }

    fun resume() {
        pendingPauseOnConnect = false
        pendingStart?.let { queued ->
            pendingStart = queued.copy(autoplay = true)
            mutableState.value = mutableState.value.copy(
                phase = ReaderTtsPhase.PREPARING,
                problem = null
            )
        }
        controller?.sendCustomCommand(
            SessionCommand(ReaderTtsPlaybackRequest.ACTION_RESUME_OWNER, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    fun pause() {
        pendingPauseOnConnect = controller == null
        // An immediate Pause cancels pending autoplay even if the MediaController
        // has not connected or has no media item yet. This is not a no-op.
        pendingStart = pendingStart?.copy(autoplay = false)
        mutableState.value = mutableState.value.copy(phase = ReaderTtsPhase.PAUSED)
        controller?.sendCustomCommand(
            SessionCommand(ReaderTtsPlaybackRequest.ACTION_PAUSE_OWNER, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    fun stop() {
        pendingPauseOnConnect = false
        pendingStart = null
        controller?.stop()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun updateSettings(settings: ReaderTtsSettings) {
        val safe = settings.normalized()
        val target = controller ?: return
        target.playbackParameters = PlaybackParameters(
            safe.speed.toFloat(),
            safe.pitch.toFloat()
        )
        target.sendCustomCommand(
            SessionCommand(
                ReaderTtsPlaybackRequest.ACTION_UPDATE_VOICE_PREFERENCES,
                Bundle.EMPTY
            ),
            Bundle().apply {
                putString(ReaderTtsPlaybackRequest.EXTRA_ENGINE_CHOICE, safe.engine.name)
                putString(
                    ReaderTtsPlaybackRequest.EXTRA_PREFERRED_VOICES_JSON,
                    ReaderTtsPlaybackRequest.encodePreferredVoices(
                        safe.preferredVoiceIds
                    )
                )
            }
        )
    }

    fun setSleepTimer(minutes: Int) {
        val target = controller ?: return
        val resultFuture = target.sendCustomCommand(
            SessionCommand(
                ReaderTtsPlaybackRequest.ACTION_SET_SLEEP_TIMER,
                Bundle.EMPTY
            ),
            Bundle().apply {
                putInt(ReaderTtsPlaybackRequest.EXTRA_SLEEP_MINUTES, minutes)
            }
        )
        resultFuture.addListener(
            {
                if (closed.get()) return@addListener
                val result = runCatching { resultFuture.get() }.getOrNull()
                if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                    mutableSleepDeadlineEpochMs.value =
                        result.extras.getLong(
                            ReaderTtsPlaybackRequest.EXTRA_SLEEP_DEADLINE_EPOCH_MS,
                            0L
                        ).takeIf { it > System.currentTimeMillis() }
                }
            },
            mainExecutor
        )
    }

    fun refreshSleepTimer() {
        val target = controller ?: return
        val resultFuture = target.sendCustomCommand(
            SessionCommand(
                ReaderTtsPlaybackRequest.ACTION_QUERY_SLEEP_TIMER,
                Bundle.EMPTY
            ),
            Bundle.EMPTY
        )
        resultFuture.addListener(
            {
                if (closed.get()) return@addListener
                val result = runCatching { resultFuture.get() }.getOrNull()
                mutableSleepDeadlineEpochMs.value =
                    if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                        result.extras.getLong(
                            ReaderTtsPlaybackRequest.EXTRA_SLEEP_DEADLINE_EPOCH_MS,
                            0L
                        ).takeIf { it > System.currentTimeMillis() }
                    } else {
                        null
                    }
            },
            mainExecutor
        )
    }

    fun refreshActiveSegment() {
        val target = controller ?: return
        val resultFuture = target.sendCustomCommand(
            SessionCommand(
                ReaderTtsPlaybackRequest.ACTION_QUERY_ACTIVE_SEGMENT,
                Bundle.EMPTY
            ),
            Bundle.EMPTY
        )
        resultFuture.addListener(
            {
                if (closed.get()) return@addListener
                val result = runCatching { resultFuture.get() }.getOrNull()
                mutableActiveSegmentText.value =
                    if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                        result.extras
                            .getString(ReaderTtsPlaybackRequest.EXTRA_ACTIVE_SEGMENT_TEXT)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
            },
            mainExecutor
        )
    }

    fun refreshVoiceCatalog() {
        val target = controller ?: return
        if (mutableVoiceCatalogLoading.value) return
        mutableVoiceCatalogLoading.value = true
        mutableVoiceCatalogProblem.value = null
        val resultFuture = target.sendCustomCommand(
            SessionCommand(
                ReaderTtsPlaybackRequest.ACTION_QUERY_VOICES,
                Bundle.EMPTY
            ),
            Bundle.EMPTY
        )
        resultFuture.addListener(
            {
                if (closed.get()) return@addListener
                val result = runCatching { resultFuture.get() }.getOrNull()
                mutableVoiceCatalogLoading.value = false
                if (result == null || result.resultCode != SessionResult.RESULT_SUCCESS) {
                    mutableVoiceCatalogProblem.value =
                        result?.extras?.problemOrNull() ?: ReaderTtsProblem.NO_ENGINE
                    return@addListener
                }
                mutableVoices.value = ReaderTtsPlaybackRequest.decodeVoiceCatalog(
                    result.extras.getString(
                        ReaderTtsPlaybackRequest.EXTRA_VOICES_JSON
                    )
                )
                mutableVoiceCatalogProblem.value = null
            },
            mainExecutor
        )
    }

    fun previewVoice(
        languageTag: String,
        voiceId: String,
        sample: String,
        settings: ReaderTtsSettings
    ) {
        val target = controller ?: return
        val safe = settings.normalized()
        mutablePreviewProblem.value = null
        val resultFuture = target.sendCustomCommand(
            SessionCommand(
                ReaderTtsPlaybackRequest.ACTION_PREVIEW_VOICE,
                Bundle.EMPTY
            ),
            Bundle().apply {
                putString(ReaderTtsPlaybackRequest.EXTRA_LANGUAGE_TAG, languageTag)
                putString(ReaderTtsPlaybackRequest.EXTRA_VOICE_ID, voiceId)
                putString(ReaderTtsPlaybackRequest.EXTRA_SAMPLE, sample)
                putFloat("speed", safe.speed.toFloat())
                putFloat("pitch", safe.pitch.toFloat())
            }
        )
        resultFuture.addListener(
            {
                if (closed.get()) return@addListener
                val result = runCatching { resultFuture.get() }.getOrNull()
                mutablePreviewProblem.value = when {
                    result == null -> ReaderTtsProblem.NO_ENGINE
                    result.resultCode == SessionResult.RESULT_SUCCESS -> null
                    else -> result.extras.problemOrNull() ?: ReaderTtsProblem.SYNTHESIS
                }
            },
            mainExecutor
        )
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pendingStart = null
        pendingPauseOnConnect = false
        mutableActiveSegmentText.value = null
        controller?.removeListener(playerListener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
        scope.cancel()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let(::syncState)
        }

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int
        ) {
            controller?.let(::syncState)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            controller?.let(::syncState)
        }

        override fun onPlayerError(error: PlaybackException) {
            controller?.let(::syncState)
        }
    }

    private fun syncState(player: Player) {
        val phase = when (player.playbackState) {
            Player.STATE_BUFFERING -> ReaderTtsPhase.PREPARING
            Player.STATE_ENDED -> ReaderTtsPhase.ENDED
            Player.STATE_READY -> if (player.playWhenReady || player.isPlaying) {
                ReaderTtsPhase.PLAYING
            } else {
                ReaderTtsPhase.PAUSED
            }
            else -> ReaderTtsPhase.STOPPED
        }
        mutableState.value = mutableState.value.copy(
            phase = phase,
            problem = player.playerError?.extras?.problemOrNull()
        )
        refreshActiveSegment()
    }

    private fun Bundle.problemOrNull(): ReaderTtsProblem? =
        getString(ReaderTtsPlaybackRequest.EXTRA_PROBLEM)
            ?.let { raw ->
                runCatching { ReaderTtsProblem.valueOf(raw) }.getOrNull()
            }

    private data class PendingStart(
        val bookId: String,
        val locatorJson: String,
        val settings: ReaderTtsSettings,
        val autoplay: Boolean
    )
}
