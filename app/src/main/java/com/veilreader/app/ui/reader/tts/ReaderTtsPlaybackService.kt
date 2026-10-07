package com.veilreader.app.ui.reader.tts

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Sole lifetime owner for background listening.
 *
 * External media controllers receive ordinary transport controls only. Loading a publication,
 * enumerating voices and previewing a voice are app-private custom commands, so another controller
 * cannot ask Veil to expose publication text or probe the user's installed TTS inventory.
 */
@OptIn(UnstableApi::class)
class ReaderTtsPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ReaderTtsMediaPlayer
    private lateinit var mediaSession: MediaSession
    private val sleepTimerStore by lazy {
        ReaderTtsSleepTimerStore(applicationContext)
    }
    private var sleepTimerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        player = ReaderTtsMediaPlayer(applicationContext)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .build()
        serviceScope.launch {
            player.restoreCheckpoint()
        }
        serviceScope.launch {
            restoreSleepTimer()
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession = mediaSession

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val base = super.onConnect(session, controller)
            if (controller.packageName != packageName) return base
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    base.availableSessionCommands.buildUpon()
                        .add(SessionCommand(ReaderTtsPlaybackRequest.ACTION_LOAD_AND_PLAY, Bundle.EMPTY))
                        .add(SessionCommand(ReaderTtsPlaybackRequest.ACTION_LOAD_PAUSED, Bundle.EMPTY))
                        .add(SessionCommand(ReaderTtsPlaybackRequest.ACTION_QUERY_VOICES, Bundle.EMPTY))
                        .add(SessionCommand(ReaderTtsPlaybackRequest.ACTION_PREVIEW_VOICE, Bundle.EMPTY))
                        .add(
                            SessionCommand(
                                ReaderTtsPlaybackRequest.ACTION_UPDATE_VOICE_PREFERENCES,
                                Bundle.EMPTY
                            )
                        )
                        .add(
                            SessionCommand(
                                ReaderTtsPlaybackRequest.ACTION_SET_SLEEP_TIMER,
                                Bundle.EMPTY
                            )
                        )
                        .add(
                            SessionCommand(
                                ReaderTtsPlaybackRequest.ACTION_QUERY_SLEEP_TIMER,
                                Bundle.EMPTY
                            )
                        )
                        .build()
                )
                .setAvailablePlayerCommands(base.availablePlayerCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (controller.packageName != packageName) {
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED)
                )
            }

            return when (customCommand.customAction) {
                ReaderTtsPlaybackRequest.ACTION_LOAD_AND_PLAY,
                ReaderTtsPlaybackRequest.ACTION_LOAD_PAUSED -> {
                    val request = ReaderTtsPlaybackRequest.fromBundle(args)
                        ?: return Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                        )
                    player.loadRequest(
                        request,
                        autoplay = customCommand.customAction ==
                            ReaderTtsPlaybackRequest.ACTION_LOAD_AND_PLAY
                    )
                    Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    )
                }

                ReaderTtsPlaybackRequest.ACTION_QUERY_VOICES -> queryVoices()
                ReaderTtsPlaybackRequest.ACTION_PREVIEW_VOICE -> previewVoice(args)
                ReaderTtsPlaybackRequest.ACTION_UPDATE_VOICE_PREFERENCES -> {
                    val preferred = ReaderTtsPlaybackRequest.decodePreferredVoices(
                        args.getString(ReaderTtsPlaybackRequest.EXTRA_PREFERRED_VOICES_JSON)
                    )
                    player.updateVoicePreferences(preferred)
                    Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    )
                }
                ReaderTtsPlaybackRequest.ACTION_SET_SLEEP_TIMER ->
                    setSleepTimer(args)
                ReaderTtsPlaybackRequest.ACTION_QUERY_SLEEP_TIMER ->
                    querySleepTimer()
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        private fun setSleepTimer(args: Bundle): ListenableFuture<SessionResult> {
            val minutes = args.getInt(
                ReaderTtsPlaybackRequest.EXTRA_SLEEP_MINUTES,
                Int.MIN_VALUE
            )
            val normalized = normalizedTtsSleepMinutes(minutes)
                ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                )
            if (normalized > 0 && player.mediaItemCount <= 0) {
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                )
            }

            val future = SettableFuture.create<SessionResult>()
            serviceScope.launch {
                val deadline = if (normalized == 0) {
                    null
                } else {
                    ttsSleepDeadline(
                        nowEpochMs = System.currentTimeMillis(),
                        minutes = normalized
                    )
                }
                scheduleSleepTimer(deadline)
                future.set(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            deadline?.let {
                                putLong(
                                    ReaderTtsPlaybackRequest.EXTRA_SLEEP_DEADLINE_EPOCH_MS,
                                    it
                                )
                            }
                        }
                    )
                )
            }
            return future
        }

        private fun querySleepTimer(): ListenableFuture<SessionResult> {
            val future = SettableFuture.create<SessionResult>()
            serviceScope.launch {
                val now = System.currentTimeMillis()
                val deadline = sleepTimerStore.read()?.takeIf { it > now }
                if (deadline == null) {
                    sleepTimerStore.save(null)
                }
                future.set(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            deadline?.let {
                                putLong(
                                    ReaderTtsPlaybackRequest.EXTRA_SLEEP_DEADLINE_EPOCH_MS,
                                    it
                                )
                            }
                        }
                    )
                )
            }
            return future
        }

        private fun queryVoices(): ListenableFuture<SessionResult> {
            val future = SettableFuture.create<SessionResult>()
            serviceScope.launch {
                val backend = AndroidReaderTtsBackend(applicationContext)
                try {
                    val problem = try {
                        withTimeout(5_000L) { backend.initialize() }
                    } catch (_: TimeoutCancellationException) {
                        ReaderTtsProblem.TIMEOUT
                    }
                    if (problem != null) {
                        future.set(
                            SessionResult(
                                SessionResult.RESULT_ERROR_SESSION_SETUP_REQUIRED,
                                Bundle().apply {
                                    putString(
                                        ReaderTtsPlaybackRequest.EXTRA_PROBLEM,
                                        problem.name
                                    )
                                }
                            )
                        )
                    } else {
                        future.set(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS,
                                Bundle().apply {
                                    putString(
                                        ReaderTtsPlaybackRequest.EXTRA_VOICES_JSON,
                                        ReaderTtsPlaybackRequest.encodeVoiceCatalog(
                                            backend.voices
                                        )
                                    )
                                }
                            )
                        )
                    }
                } catch (_: Exception) {
                    future.set(
                        SessionResult(SessionResult.RESULT_ERROR_UNKNOWN)
                    )
                } finally {
                    backend.close()
                }
            }
            return future
        }

        private fun previewVoice(args: Bundle): ListenableFuture<SessionResult> {
            if (player.isPlaying) {
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                )
            }

            val language = args
                .getString(ReaderTtsPlaybackRequest.EXTRA_LANGUAGE_TAG)
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it.length <= 64 }
                ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                )
            val voiceId = args
                .getString(ReaderTtsPlaybackRequest.EXTRA_VOICE_ID)
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it.length <= 256 }
                ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                )
            val sample = args
                .getString(ReaderTtsPlaybackRequest.EXTRA_SAMPLE)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.take(240)
                ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                )

            val speed = args.getFloat("speed", 1f)
            val pitch = args.getFloat("pitch", 1f)
            val future = SettableFuture.create<SessionResult>()
            serviceScope.launch {
                val backend = AndroidReaderTtsBackend(applicationContext)
                try {
                    val initProblem = try {
                        withTimeout(5_000L) { backend.initialize() }
                    } catch (_: TimeoutCancellationException) {
                        ReaderTtsProblem.TIMEOUT
                    }
                    val problem = if (initProblem != null) {
                        initProblem
                    } else {
                        try {
                            withTimeout(15_000L) {
                                backend.speak(
                                    text = sample,
                                    languageTag = language,
                                    preferences = ReaderTtsPreferences(
                                        speed = speed,
                                        pitch = pitch,
                                        preferredVoiceIds = mapOf(language to voiceId)
                                    )
                                )
                            }
                        } catch (_: TimeoutCancellationException) {
                            ReaderTtsProblem.TIMEOUT
                        }
                    }

                    if (problem == null) {
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS))
                    } else {
                        future.set(
                            SessionResult(
                                SessionResult.RESULT_ERROR_SESSION_SETUP_REQUIRED,
                                Bundle().apply {
                                    putString(
                                        ReaderTtsPlaybackRequest.EXTRA_PROBLEM,
                                        problem.name
                                    )
                                }
                            )
                        )
                    }
                } catch (_: Exception) {
                    future.set(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
                } finally {
                    backend.close()
                }
            }
            return future
        }
    }

    private suspend fun restoreSleepTimer() {
        val now = System.currentTimeMillis()
        val deadline = sleepTimerStore.read()
        if (deadline == null || deadline <= now) {
            sleepTimerStore.save(null)
            return
        }
        scheduleSleepTimer(deadline)
    }

    private suspend fun scheduleSleepTimer(deadlineEpochMs: Long?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerStore.save(deadlineEpochMs)
        if (deadlineEpochMs == null) return

        val remaining = (deadlineEpochMs - System.currentTimeMillis())
            .coerceAtLeast(0L)
        sleepTimerJob = serviceScope.launch {
            delay(remaining)
            player.pause()
            sleepTimerStore.save(null)
            sleepTimerJob = null
        }
    }

}
