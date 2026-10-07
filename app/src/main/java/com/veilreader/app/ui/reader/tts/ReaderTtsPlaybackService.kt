package com.veilreader.app.ui.reader.tts

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Sole lifetime owner for background listening.
 *
 * External media controllers receive ordinary transport controls only. Loading a publication is an
 * app-private custom command, so another controller cannot ask Veil to expose or synthesize an
 * arbitrary book/locator.
 */
@UnstableApi
class ReaderTtsPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ReaderTtsMediaPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ReaderTtsMediaPlayer(applicationContext)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .build()
        serviceScope.launch {
            player.restoreCheckpoint()
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession = mediaSession

    override fun onDestroy() {
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
            val request = ReaderTtsPlaybackRequest.fromBundle(args)
                ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                )
            return when (customCommand.customAction) {
                ReaderTtsPlaybackRequest.ACTION_LOAD_AND_PLAY -> {
                    player.loadRequest(request, autoplay = true)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ReaderTtsPlaybackRequest.ACTION_LOAD_PAUSED -> {
                    player.loadRequest(request, autoplay = false)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }
}
