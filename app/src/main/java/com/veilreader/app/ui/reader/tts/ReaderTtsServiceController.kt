package com.veilreader.app.ui.reader.tts

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.veilreader.app.domain.ReaderTtsSettings
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Activity-side client for the service-owned TTS session.
 *
 * Releasing this controller never stops background speech. Playback lifetime belongs to
 * [ReaderTtsPlaybackService], which is the reason screen lock/activity recreation can be safe.
 */
@UnstableApi
internal class ReaderTtsServiceController(context: Context) : AutoCloseable {
    private val application = context.applicationContext
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

    private val mutableState = MutableStateFlow(ReaderTtsState())
    val state: StateFlow<ReaderTtsState> = mutableState.asStateFlow()

    val connected: Boolean
        get() = controller != null && !closed.get()

    init {
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
                pendingStart?.also {
                    pendingStart = null
                    start(it.bookId, it.locatorJson, it.settings)
                }
            },
            ContextCompat.getMainExecutor(application)
        )
    }

    fun start(bookId: String, locatorJson: String, settings: ReaderTtsSettings) {
        if (closed.get()) return
        val target = controller
        if (target == null) {
            pendingStart = PendingStart(bookId, locatorJson, settings.normalized())
            mutableState.value = mutableState.value.copy(
                phase = ReaderTtsPhase.PREPARING,
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
            phase = ReaderTtsPhase.PREPARING,
            problem = null
        )
        target.sendCustomCommand(
            SessionCommand(
                ReaderTtsPlaybackRequest.ACTION_LOAD_AND_PLAY,
                android.os.Bundle.EMPTY
            ),
            request.toBundle()
        )
    }

    fun resume() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun stop() {
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
        controller?.playbackParameters = PlaybackParameters(
            safe.speed.toFloat(),
            safe.pitch.toFloat()
        )
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pendingStart = null
        controller?.removeListener(playerListener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
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
            problem = null
        )
    }

    private data class PendingStart(
        val bookId: String,
        val locatorJson: String,
        val settings: ReaderTtsSettings
    )
}
