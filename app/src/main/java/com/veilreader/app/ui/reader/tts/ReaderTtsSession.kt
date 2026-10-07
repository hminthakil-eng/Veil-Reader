package com.veilreader.app.ui.reader.tts

import java.util.ArrayDeque
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.readium.r2.shared.publication.Locator

internal data class ReaderTtsState(
    val phase: ReaderTtsPhase = ReaderTtsPhase.STOPPED,
    val problem: ReaderTtsProblem? = null,
    val sourceLocator: Locator? = null
)

/**
 * Sole speech-session owner.
 *
 * The session owns synthesis and a forward Readium content iterator, but never owns visual Reader
 * progress. A service may keep this object alive after the Activity disappears. Checkpoints use
 * [ReaderTtsState.sourceLocator], so process recovery repeats at most the active semantic segment
 * rather than guessing from a time position.
 */
internal class ReaderTtsSession(
    private val contentFactory: (Locator) -> ReaderTtsContent?,
    private val backendFactory: () -> ReaderTtsBackend,
    private val publicationLanguage: String?,
    private val canPlay: () -> Boolean,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Main.immediate,
    private val initializationTimeoutMs: Long = 5_000L,
    private val utteranceTimeoutMs: Long = 600_000L
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutableState = MutableStateFlow(ReaderTtsState())
    val state = mutableState.asStateFlow()

    private var backend: ReaderTtsBackend? = null
    private var content: ReaderTtsContent? = null
    private var current: ReaderTtsUtterance? = null
    private val completedHistory = ArrayDeque<Locator>()
    private val sourceMutex = Mutex()
    private var pendingRead: Deferred<ReaderTtsUtterance?>? = null
    private var preferences = ReaderTtsPreferences()
    private var playJob: Job? = null
    private var monitorJob: Job? = null
    private var serial = 0L
    private var resumeAfterTransientFocusLoss = false
    private var closed = false

    fun start(
        locator: Locator,
        requestedPreferences: ReaderTtsPreferences = ReaderTtsPreferences()
    ) {
        load(locator, requestedPreferences, autoplay = true)
    }

    /**
     * Loads a semantic position without necessarily speaking. This is the process/service recovery
     * entry point and avoids the old "start then immediately pause" race.
     */
    fun load(
        locator: Locator,
        requestedPreferences: ReaderTtsPreferences = ReaderTtsPreferences(),
        autoplay: Boolean = false
    ) {
        if (closed) return
        resetSource()
        preferences = requestedPreferences.normalized()
        content = try {
            contentFactory(locator)
        } catch (_: Exception) {
            mutableState.value = ReaderTtsState(
                ReaderTtsPhase.FAILED,
                ReaderTtsProblem.CONTENT,
                sourceLocator = locator
            )
            return
        }
        if (content == null) {
            mutableState.value = ReaderTtsState(
                ReaderTtsPhase.FAILED,
                ReaderTtsProblem.UNSUPPORTED,
                sourceLocator = locator
            )
            return
        }
        mutableState.value = ReaderTtsState(
            phase = ReaderTtsPhase.PAUSED,
            sourceLocator = locator
        )
        if (autoplay) resume()
    }

    fun resume() {
        if (closed || playJob?.isActive == true || content == null || !canPlay()) return
        val ownerSerial = ++serial
        mutableState.value = mutableState.value.copy(
            phase = ReaderTtsPhase.PREPARING,
            problem = null
        )
        playJob = scope.launch {
            try {
                val first = current ?: nextContent()
                if (ownerSerial != serial || closed) return@launch
                if (first == null) {
                    // An initial empty source is not a successfully completed listening session.
                    // Preserve the established contract so unsupported/empty publications remain
                    // actionable instead of looking like an instant normal ending.
                    fail(ReaderTtsProblem.UNSUPPORTED)
                    return@launch
                }
                current = first
                pendingRead = null
                val engine = backend ?: backendFactory().also {
                    backend = it
                    it.onInterruption = ::handleInterruption
                    it.onFocusGained = ::handleFocusGained
                }
                val problem = withTimeout(initializationTimeoutMs.coerceAtLeast(1L)) {
                    engine.initialize()
                }
                if (problem != null) {
                    fail(problem)
                    return@launch
                }
                while (ownerSerial == serial && !closed) {
                    if (!canPlay()) {
                        pause()
                        return@launch
                    }
                    val utterance = current ?: nextContent()
                    if (ownerSerial != serial || closed) return@launch
                    current = utterance
                    pendingRead = null
                    if (utterance == null) {
                        engine.stop()
                        mutableState.value = mutableState.value.copy(
                            phase = ReaderTtsPhase.ENDED,
                            problem = null
                        )
                        return@launch
                    }
                    if (!canPlay()) {
                        pause()
                        return@launch
                    }
                    val language = preferences.languageTag
                        ?: utterance.languageTag
                        ?: publicationLanguage
                        ?: Locale.getDefault().toLanguageTag()
                    mutableState.value = ReaderTtsState(
                        phase = ReaderTtsPhase.PLAYING,
                        sourceLocator = utterance.locator
                    )
                    val result = withTimeout(utteranceTimeoutMs.coerceAtLeast(1L)) {
                        engine.speak(utterance.text, language, preferences)
                    }
                    if (ownerSerial != serial || closed) return@launch
                    if (result != null) {
                        fail(result)
                        return@launch
                    }
                    rememberVisited(utterance.locator)
                    current = null
                }
            } catch (_: TimeoutCancellationException) {
                if (ownerSerial == serial && !closed) fail(ReaderTtsProblem.TIMEOUT)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (ownerSerial == serial && !closed) fail(ReaderTtsProblem.CONTENT)
            } finally {
                if (ownerSerial == serial) playJob = null
            }
        }

        // API 26-32 do not expose a reliable spoken-accessibility state callback. Poll only while
        // speech is active; there is no frame loop and no background busy wait after pause.
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (ownerSerial == serial && !closed && playJob?.isActive == true) {
                delay(500)
                if (!canPlay()) {
                    pause()
                    return@launch
                }
            }
        }
    }

    /** Skip exactly one semantic TTS segment. System "next" never means next arbitrary media item. */
    fun next() {
        if (closed || content == null) return
        val continuePlaying = mutableState.value.phase in
            setOf(ReaderTtsPhase.PLAYING, ReaderTtsPhase.PREPARING)
        val active = current
        cancelPlaybackOnly()

        // If an utterance is already materialized, skipping means advancing beyond it. Keep its
        // semantic locator so Previous can reverse the explicit skip even when it was interrupted.
        if (active != null) {
            rememberVisited(active.locator)
            current = null
        }

        // When the current utterance is already materialized the iterator is already positioned
        // after it, so continuing playback can resume immediately. During PREPARING, however, the
        // pending read still represents the current semantic segment and must be consumed/skipped
        // before speech resumes.
        if (continuePlaying && active != null) {
            resume()
            return
        }

        val ownerSerial = ++serial
        playJob = scope.launch {
            try {
                // A freshly restored paused session or a PREPARING session has not materialized its
                // first segment yet. Consume that current semantic segment before selecting Next.
                if (active == null) {
                    val skipped = nextContent()
                    pendingRead = null
                    skipped?.let { rememberVisited(it.locator) }
                }
                val next = nextContent()
                if (ownerSerial != serial || closed) return@launch
                pendingRead = null
                current = next
                mutableState.value = if (next == null) {
                    ReaderTtsState(ReaderTtsPhase.ENDED)
                } else {
                    ReaderTtsState(
                        phase = ReaderTtsPhase.PAUSED,
                        sourceLocator = next.locator
                    )
                }
                if (continuePlaying && next != null) {
                    // Clear this transition job before resume(); resume refuses to create a second
                    // owner while playJob is active. It increments serial, so finally cannot clear
                    // the newly-created playback job.
                    playJob = null
                    resume()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (ownerSerial == serial && !closed) fail(ReaderTtsProblem.CONTENT)
            } finally {
                if (ownerSerial == serial) playJob = null
            }
        }
    }

    /**
     * Move to the previously completed semantic segment. We recreate the Readium iterator from the
     * exact semantic locator instead of inventing a millisecond seek.
     */
    fun previous() {
        if (closed) return
        val target = completedHistory.pollLast() ?: return
        val continuePlaying = mutableState.value.phase in
            setOf(ReaderTtsPhase.PLAYING, ReaderTtsPhase.PREPARING)
        cancelPlaybackOnly()
        pendingRead?.cancel()
        pendingRead = null
        current = null
        content = try {
            contentFactory(target)
        } catch (_: Exception) {
            null
        }
        if (content == null) {
            fail(ReaderTtsProblem.CONTENT)
            return
        }
        mutableState.value = ReaderTtsState(
            phase = ReaderTtsPhase.PAUSED,
            sourceLocator = target
        )
        if (continuePlaying) resume()
    }

    /** Main-thread preference update; applies on the next bounded synthesis request. */
    fun updatePreferences(value: ReaderTtsPreferences) {
        if (!closed) preferences = value.normalized()
    }

    fun pause() {
        if (closed) return
        resumeAfterTransientFocusLoss = false
        cancelPlaybackOnly(abandonAudioFocus = true)
        if (
            mutableState.value.phase == ReaderTtsPhase.PLAYING ||
            mutableState.value.phase == ReaderTtsPhase.PREPARING
        ) {
            mutableState.value = mutableState.value.copy(phase = ReaderTtsPhase.PAUSED)
        }
    }

    fun stop() {
        if (closed) return
        resetSource()
        mutableState.value = ReaderTtsState()
    }

    fun close() {
        if (closed) return
        stop()
        closed = true
        serial += 1
        scope.cancel()
        backend?.onInterruption = null
        backend?.onFocusGained = null
        backend?.close()
        backend = null
        mutableState.value = ReaderTtsState(ReaderTtsPhase.CLOSED)
    }

    suspend fun awaitClosed() {
        scope.coroutineContext[Job]?.join()
    }

    private fun cancelPlaybackOnly(abandonAudioFocus: Boolean = true) {
        serial += 1
        monitorJob?.cancel()
        monitorJob = null
        playJob?.cancel()
        playJob = null
        backend?.stop(abandonAudioFocus)
    }

    private fun resetSource() {
        resumeAfterTransientFocusLoss = false
        cancelPlaybackOnly(abandonAudioFocus = true)
        pendingRead?.cancel()
        pendingRead = null
        content = null
        current = null
        completedHistory.clear()
    }

    private fun handleInterruption(interruption: ReaderTtsInterruption) {
        if (closed) return
        when (interruption) {
            ReaderTtsInterruption.TRANSIENT_FOCUS -> {
                val wasActive =
                    mutableState.value.phase == ReaderTtsPhase.PLAYING ||
                        mutableState.value.phase == ReaderTtsPhase.PREPARING
                resumeAfterTransientFocusLoss = wasActive
                cancelPlaybackOnly(abandonAudioFocus = false)
                if (wasActive) {
                    // Keep play intent visible to MediaSessionService while Android owns focus.
                    // This avoids the WebNovel-style failure where narration keeps advancing
                    // silently during a call, while still allowing exact semantic resume.
                    mutableState.value = mutableState.value.copy(
                        phase = ReaderTtsPhase.PREPARING,
                        problem = null
                    )
                }
            }

            ReaderTtsInterruption.PERMANENT_FOCUS,
            ReaderTtsInterruption.BECOMING_NOISY -> pause()
        }
    }

    private fun handleFocusGained() {
        if (closed || !resumeAfterTransientFocusLoss) return
        resumeAfterTransientFocusLoss = false
        scope.launch {
            playJob?.join()
            if (!closed && content != null && canPlay()) {
                resume()
            }
        }
    }

    private suspend fun nextContent(): ReaderTtsUtterance? {
        val source = content ?: return null
        // Await cancellation must not discard a chunk already consumed by Readium's iterator.
        // This single pending read is owned by the session, survives pause, and is joined on close.
        val read = pendingRead ?: scope.async {
            sourceMutex.withLock { source.next() }
        }.also { pendingRead = it }
        return read.await()
    }

    private fun rememberVisited(locator: Locator) {
        if (completedHistory.peekLast() == locator) return
        completedHistory.addLast(locator)
        while (completedHistory.size > MAX_HISTORY) completedHistory.removeFirst()
    }

    private fun fail(problem: ReaderTtsProblem) {
        backend?.stop()
        mutableState.value = mutableState.value.copy(
            phase = ReaderTtsPhase.FAILED,
            problem = problem
        )
    }

    private companion object {
        const val MAX_HISTORY = 128
    }
}
