package com.veilreader.app.ui.reader.tts

import android.content.Context
import android.os.Bundle
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.db.toDomain
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

/**
 * Media3 facade over Veil's semantic TTS session.
 *
 * Media time is deliberately not invented. System next/previous are mapped to semantic Readium
 * segments, while the durable listening position remains a Locator checkpoint.
 */
@UnstableApi
internal class ReaderTtsMediaPlayer(
    context: Context,
    private val checkpointStore: ReaderTtsCheckpointStore =
        ReaderTtsCheckpointStore(context)
) : SimpleBasePlayer(Looper.getMainLooper()) {
    private val application = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val database = VeilDatabase.get(application)
    private val readium = ReadiumEngine(application)

    private var opened: OpenedPublication? = null
    private var session: ReaderTtsSession? = null
    private var sessionStateJob: Job? = null
    private var loadJob: Job? = null
    private var generation = 0L

    private var currentBook: Book? = null
    private var currentRequest: ReaderTtsPlaybackRequest? = null
    private var sessionState = ReaderTtsState()
    private var desiredPlayWhenReady = false
    private var released = false

    private val loadedCommands = Player.Commands.Builder()
        .add(Player.COMMAND_PLAY_PAUSE)
        .add(Player.COMMAND_STOP)
        .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
        .add(Player.COMMAND_GET_METADATA)
        .add(Player.COMMAND_GET_TIMELINE)
        .add(Player.COMMAND_SEEK_TO_NEXT)
        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        .add(Player.COMMAND_SET_SPEED_AND_PITCH)
        .add(Player.COMMAND_RELEASE)
        .build()

    private val emptyCommands = Player.Commands.Builder()
        .add(Player.COMMAND_GET_TIMELINE)
        .add(Player.COMMAND_RELEASE)
        .build()

    override fun getState(): State {
        val book = currentBook
        val request = currentRequest
        val playlist = if (book == null) {
            emptyList()
        } else {
            val chapterTitle = sessionState.sourceLocator?.title
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val metadata = MediaMetadata.Builder()
                .setTitle(
                    if (chapterTitle == null) {
                        book.title
                    } else {
                        "${book.title} — $chapterTitle"
                    }
                )
                .setArtist(book.author)
                .build()
            val item = MediaItem.Builder()
                .setMediaId(book.id)
                .setMediaMetadata(metadata)
                .build()
            listOf(
                MediaItemData.Builder(book.id)
                    .setMediaItem(item)
                    .setMediaMetadata(metadata)
                    .setDurationUs(C.TIME_UNSET)
                    // TTS position is semantic Readium content, not a trustworthy media clock.
                    // Do not expose a fake scrubber; previous/next remain semantic commands.
                    .setIsSeekable(false)
                    .build()
            )
        }

        val playbackState = if (book == null) {
            Player.STATE_IDLE
        } else {
            when (sessionState.phase) {
                ReaderTtsPhase.PREPARING -> Player.STATE_BUFFERING
                ReaderTtsPhase.ENDED -> Player.STATE_ENDED
                ReaderTtsPhase.FAILED -> Player.STATE_IDLE
                ReaderTtsPhase.STOPPED,
                ReaderTtsPhase.PLAYING,
                ReaderTtsPhase.PAUSED,
                ReaderTtsPhase.CLOSED -> Player.STATE_READY
            }
        }

        val safePreferences = request?.preferences?.normalized() ?: ReaderTtsPreferences()
        return State.Builder()
            .setAvailableCommands(if (book == null) emptyCommands else loadedCommands)
            .setPlaylist(playlist)
            .setPlaybackState(playbackState)
            .setPlayWhenReady(
                desiredPlayWhenReady && playbackState != Player.STATE_ENDED,
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
            )
            .setPlaybackParameters(
                PlaybackParameters(safePreferences.speed, safePreferences.pitch)
            )
            .setPlayerError(sessionState.problem?.toPlaybackException())
            .build()
    }

    fun loadRequest(request: ReaderTtsPlaybackRequest, autoplay: Boolean) {
        verifyApplicationThread()
        val safe = request.normalized() ?: return
        val ownerGeneration = ++generation
        loadJob?.cancel()
        desiredPlayWhenReady = autoplay
        currentRequest = safe
        sessionState = ReaderTtsState(phase = ReaderTtsPhase.PREPARING)
        invalidateState()

        loadJob = scope.launch {
            try {
                closeCurrentOwner()
                if (ownerGeneration != generation || released) return@launch

                val book = database.books().findWithCollections(safe.bookId)?.toDomain()
                    ?: run {
                        failLoad(ReaderTtsProblem.CONTENT)
                        return@launch
                    }
                if (
                    book.format != BookFormat.EPUB ||
                    book.sourceUri.isNullOrBlank()
                ) {
                    failLoad(ReaderTtsProblem.UNSUPPORTED)
                    return@launch
                }

                val locator = runCatching {
                    Locator.fromJSON(JSONObject(safe.locatorJson))
                }.getOrNull() ?: run {
                    failLoad(ReaderTtsProblem.CONTENT)
                    return@launch
                }

                val publication = readium.openBook(book).getOrElse {
                    failLoad(ReaderTtsProblem.CONTENT)
                    return@launch
                }
                if (ownerGeneration != generation || released) {
                    publication.close()
                    return@launch
                }

                val speechSession = ReaderTtsSession(
                    contentFactory = { target ->
                        ReadiumTtsContent.create(publication.publication, target)
                    },
                    backendFactory = { AndroidReaderTtsBackend(application) },
                    publicationLanguage = publication.publication.metadata.languages.firstOrNull()
                        ?: book.language,
                    canPlay = { !released && ownerGeneration == generation }
                )

                opened = publication
                currentBook = book
                session = speechSession
                sessionState = ReaderTtsState(
                    phase = ReaderTtsPhase.PAUSED,
                    sourceLocator = locator
                )
                invalidateState()

                sessionStateJob = scope.launch {
                    speechSession.state.collectLatest { newState ->
                        if (ownerGeneration != generation || released) return@collectLatest
                        sessionState = newState
                        desiredPlayWhenReady =
                            newState.phase == ReaderTtsPhase.PLAYING ||
                                newState.phase == ReaderTtsPhase.PREPARING
                        invalidateState()
                        persistCheckpoint(newState)
                    }
                }

                speechSession.load(
                    locator = locator,
                    requestedPreferences = safe.preferences,
                    autoplay = autoplay
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (ownerGeneration == generation && !released) {
                    failLoad(ReaderTtsProblem.CONTENT)
                }
            }
        }
    }

    suspend fun restoreCheckpoint() {
        val checkpoint = checkpointStore.read() ?: return
        val request = checkpoint.request.copy(locatorJson = checkpoint.locatorJson)
        // Process recreation is position-safe first. Playback resumes only after a system/user play
        // request, avoiding surprise autoplay after an unrelated process restore.
        loadRequest(request, autoplay = false)
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        desiredPlayWhenReady = playWhenReady
        // Publish playWhenReady first. MediaSessionService can then promote itself to a media
        // foreground service before AndroidReaderTtsBackend asks Android 15+ for audio focus.
        invalidateState()
        if (playWhenReady) {
            session?.resume()
        } else {
            session?.pause()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        desiredPlayWhenReady = false
        ++generation
        loadJob?.cancel()
        loadJob = null
        scope.launch {
            closeCurrentOwner()
            checkpointStore.clear()
            currentBook = null
            currentRequest = null
            sessionState = ReaderTtsState()
            invalidateState()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        @Player.Command seekCommand: Int
    ): ListenableFuture<*> {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_FORWARD -> session?.next()

            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK -> session?.previous()
        }
        return Futures.immediateVoidFuture()
    }

    fun updateVoicePreferences(preferredVoiceIds: Map<String, String>) {
        val request = currentRequest ?: return
        val updatedPreferences = request.preferences.copy(
            preferredVoiceIds = preferredVoiceIds
        ).normalized()
        currentRequest = request.copy(preferences = updatedPreferences)
        session?.updatePreferences(updatedPreferences)
        invalidateState()
        scope.launch { persistCheckpoint(sessionState) }
    }

    override fun handleSetPlaybackParameters(
        playbackParameters: PlaybackParameters
    ): ListenableFuture<*> {
        val request = currentRequest ?: return Futures.immediateVoidFuture()
        val updatedPreferences = request.preferences.copy(
            speed = playbackParameters.speed,
            pitch = playbackParameters.pitch
        ).normalized()
        currentRequest = request.copy(preferences = updatedPreferences)
        session?.updatePreferences(updatedPreferences)
        invalidateState()
        scope.launch { persistCheckpoint(sessionState) }
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        if (released) return Futures.immediateVoidFuture()
        released = true
        ++generation
        loadJob?.cancel()
        loadJob = null
        desiredPlayWhenReady = false
        scope.launch {
            closeCurrentOwner()
            currentBook = null
            currentRequest = null
            sessionState = ReaderTtsState(ReaderTtsPhase.CLOSED)
            invalidateState()
            scope.cancel()
        }
        return Futures.immediateVoidFuture()
    }

    private suspend fun closeCurrentOwner() {
        sessionStateJob?.cancel()
        sessionStateJob = null
        session?.close()
        session?.awaitClosed()
        session = null
        opened?.close()
        opened = null
    }

    private fun failLoad(problem: ReaderTtsProblem) {
        desiredPlayWhenReady = false
        sessionState = ReaderTtsState(
            phase = ReaderTtsPhase.FAILED,
            problem = problem
        )
        invalidateState()
    }

    private fun ReaderTtsProblem.toPlaybackException(): PlaybackException =
        PlaybackException(
            "Veil TTS playback failed",
            null,
            PlaybackException.ERROR_CODE_UNSPECIFIED,
            Bundle().apply {
                putString(
                    ReaderTtsPlaybackRequest.EXTRA_PROBLEM,
                    name
                )
            }
        )

    private suspend fun persistCheckpoint(state: ReaderTtsState) {
        val request = currentRequest ?: return
        val locator = state.sourceLocator ?: return
        val json = locator.toJSON().toString()
        checkpointStore.save(
            ReaderTtsCheckpoint(
                request = request.copy(locatorJson = json),
                locatorJson = json,
                phase = state.phase,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }
}
