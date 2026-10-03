package com.veilreader.app.ui.reader

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.ReaderProgressWriterLease
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.domain.ReadingSessionTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderUiState(
    val bookId: String? = null,
    val progress: Float = 0f,
    val activeMillis: Long = 0L,
    val sessionStartProgress: Float = 0f,
    val sessionProgressDelta: Float = 0f
)

/**
 * Screen-level business state for the reader.
 *
 * Readium navigator objects stay in the UI because they are Android/Fragment UI plumbing. Durable
 * progress, gamification events and lifecycle-aware reading-session accounting live here.
 */
class ReaderViewModel(
    private val library: LocalLibraryRepository,
    private val game: GameRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var tracker: ReadingSessionTracker? = null
    private var openInstanceId: String? = null
    private var progressWriterLease: ReaderProgressWriterLease? = null
    private val openAttemptGate = ReaderOpenAttemptGate()
    private var openConfirmed = false
    private var resumed = false
    private var uncreditedActiveMillis = 0L
    private val locatorDeduplicator = ReaderLocatorDeduplicator()
    private var locatorSequence = 0L

    init {
        viewModelScope.launch {
            while (true) {
                delay(HEARTBEAT_MS)
                heartbeat()
            }
        }
    }

    /**
     * Prepares one Reader session for UI handoff.
     *
     * Suspend storage work happens outside the ViewModel monitor. The attempt generation is checked
     * again before installing tracker/writer ownership, so an older open that resumes later cannot
     * overtake a newer request.
     *
     * The prepared session is intentionally not written into session history until [confirmOpen].
     */
    suspend fun openBook(
        bookId: String,
        initialProgress: Float,
        openInstanceId: String
    ): Boolean {
        val attempt = synchronized(this) {
            if (tracker?.bookId == bookId && this.openInstanceId == openInstanceId) {
                return true
            }
            finishCurrentSession()
            openAttemptGate.begin(
                bookId = bookId,
                sessionInstanceId = openInstanceId
            )
        }

        ReaderTrace.event(
            "reader_open_attempt_started",
            bookId = bookId,
            sessionId = openInstanceId,
            details = "generation=${attempt.generation}"
        )

        try {
            val nowWall = System.currentTimeMillis()
            val nowElapsed = SystemClock.elapsedRealtime()
            val resumeState = library.loadReadingSessionForResume(
                sessionId = openInstanceId,
                bookId = bookId
            )
            currentCoroutineContext().ensureActive()

            val restoredTracker = resumeState?.let { state ->
                ReadingSessionTracker.restore(
                    snapshot = state.snapshot,
                    bookId = bookId,
                    startedAtElapsedMs = nowElapsed,
                    notedHighlightIds = state.notedHighlightIds
                )
            }
            val preparedTracker = restoredTracker ?: ReadingSessionTracker(
                sessionId = openInstanceId,
                bookId = bookId,
                startedAtEpochMs = nowWall,
                startedAtElapsedMs = nowElapsed
            )

            return synchronized(this) {
                if (!openAttemptGate.complete(attempt)) {
                    ReaderTrace.event(
                        "reader_open_attempt_superseded",
                        bookId = bookId,
                        sessionId = openInstanceId,
                        details = "generation=${attempt.generation}"
                    )
                    return@synchronized false
                }

                val writerLease = library.beginReaderProgressSession(
                    bookId = bookId,
                    sessionId = preparedTracker.sessionId
                )
                this.openInstanceId = openInstanceId
                progressWriterLease = writerLease
                tracker = preparedTracker
                openConfirmed = false
                resumed = false
                uncreditedActiveMillis = preparedTracker.activeMillis % ONE_MINUTE_MS
                locatorDeduplicator.reset()
                locatorSequence = 0L

                val startProgress = initialProgress.coerceIn(0f, 1f)
                _uiState.value = ReaderUiState(
                    bookId = bookId,
                    progress = startProgress,
                    activeMillis = preparedTracker.activeMillis,
                    sessionStartProgress = startProgress,
                    sessionProgressDelta = 0f
                )

                ReaderTrace.event(
                    if (restoredTracker != null) {
                        "reader_session_prepared_restored"
                    } else {
                        "reader_session_prepared"
                    },
                    bookId = bookId,
                    sessionId = preparedTracker.sessionId,
                    details = buildString {
                        append("generation=")
                        append(attempt.generation)
                        append(" initialProgress=")
                        append(startProgress)
                        if (restoredTracker != null) {
                            append(" activeMillis=")
                            append(preparedTracker.activeMillis)
                            append(" pacedPageTurns=")
                            append(preparedTracker.pacedPageTurns)
                            append(" highlights=")
                            append(preparedTracker.highlightCount)
                            append(" notes=")
                            append(preparedTracker.noteCount)
                        }
                    }
                )
                true
            }
        } catch (error: Throwable) {
            synchronized(this) {
                openAttemptGate.cancel(attempt)
            }
            ReaderTrace.event(
                if (error is CancellationException) {
                    "reader_open_attempt_cancelled"
                } else {
                    "reader_open_attempt_failed"
                },
                bookId = bookId,
                sessionId = openInstanceId,
                details = "generation=${attempt.generation} error=${error::class.java.simpleName}"
            )
            throw error
        }
    }

    @Synchronized
    fun confirmOpen(expectedOpenInstanceId: String): Boolean {
        val current = tracker ?: return false
        if (
            openInstanceId != expectedOpenInstanceId ||
            current.sessionId != expectedOpenInstanceId
        ) {
            return false
        }
        if (openConfirmed) return true

        library.saveReadingSession(current.snapshot(System.currentTimeMillis()))
        library.flushReadingSession(current.sessionId)
        openConfirmed = true
        ReaderTrace.event(
            "reader_open_confirmed",
            bookId = current.bookId,
            sessionId = current.sessionId
        )
        return true
    }

    @Synchronized
    fun cancelOpen(expectedOpenInstanceId: String) {
        openAttemptGate.cancelSession(expectedOpenInstanceId)
        if (
            openInstanceId == expectedOpenInstanceId &&
            tracker != null &&
            !openConfirmed
        ) {
            ReaderTrace.event(
                "reader_open_preparation_aborted",
                bookId = tracker?.bookId,
                sessionId = expectedOpenInstanceId
            )
            finishCurrentSession()
        }
    }

    fun onResume(expectedOpenInstanceId: String) {
        val current = currentTrackerFor(expectedOpenInstanceId) ?: return
        if (resumed) return
        resumed = true
        creditActive(current.onResume(SystemClock.elapsedRealtime()))
        game.rebasePagePacing()
        publishActiveMillis()
        persistSession()
    }

    fun onPause(expectedOpenInstanceId: String) {
        val current = currentTrackerFor(expectedOpenInstanceId) ?: return
        ReaderTrace.event("reader_pause", bookId = current.bookId, sessionId = current.sessionId)
        library.flushProgress(current.bookId)
        ReaderTrace.event("locator_flush_enqueued", bookId = current.bookId, sessionId = current.sessionId)

        if (!resumed) {
            library.flushReadingSession(current.sessionId)
            game.pauseReading()
        } else {
            creditActive(current.onPause(SystemClock.elapsedRealtime()))
            resumed = false
            game.pauseReading()
            publishActiveMillis()
            persistSession(immediate = true)
        }

        // Do not rely on a composition-owned coroutine here. ON_PAUSE/ON_STOP can be followed by
        // immediate UI disposal; the repository owns this barrier for the rest of the app process.
        library.requestLifecycleDurability(
            bookId = current.bookId,
            sessionId = current.sessionId
        )
    }

    fun onUserInteraction(expectedOpenInstanceId: String) {
        val current = currentTrackerFor(expectedOpenInstanceId) ?: return
        creditActive(current.onInteraction(SystemClock.elapsedRealtime()))
        publishActiveMillis()
    }

    @Synchronized
    internal fun onLocatorUpdate(
        bookId: String,
        expectedOpenInstanceId: String,
        progression: Double,
        locatorJson: String,
        locationKey: String,
        event: ReaderLocatorEvent
    ): ReaderLocatorCommit? {
        val current = currentTrackerFor(expectedOpenInstanceId, bookId) ?: return null

        if (!event.commitsLocator) {
            ReaderTrace.event(
                "locator_observed_uncommitted",
                bookId = bookId,
                sessionId = current.sessionId,
                details = "event=$event"
            )
            return null
        }

        if (!locatorDeduplicator.acceptCommit(locationKey)) {
            ReaderTrace.event(
                "locator_duplicate_commit_ignored",
                bookId = bookId,
                sessionId = current.sessionId,
                details = "event=$event"
            )
            return null
        }

        val nowElapsedMs = SystemClock.elapsedRealtime()
        creditActive(current.onInteraction(nowElapsedMs))

        if (event.countsPageTurn && resumed && game.recordPageTurn(locationKey)) {
            current.recordPacedPageTurn()?.let { intervalMillis ->
                library.recordReadingPaceInterval(
                    bookId = bookId,
                    intervalMillis = intervalMillis
                )
            }
        }

        val safe = (if (progression.isFinite()) progression else _uiState.value.progress.toDouble())
            .coerceIn(0.0, 1.0).toFloat()
        val sequence = ++locatorSequence
        ReaderTrace.event(
            "locator_committed",
            bookId = bookId,
            sessionId = current.sessionId,
            details = "seq=$sequence progress=$safe event=$event"
        )
        val writerLease = progressWriterLease
            ?.takeIf { lease ->
                lease.bookId == bookId &&
                    lease.sessionId == current.sessionId
            }
            ?: return null
        val completionNow = System.currentTimeMillis()
        val saveOutcome = library.saveReaderProgress(
            lease = writerLease,
            progression = safe.toDouble(),
            locatorJson = locatorJson,
            sequence = sequence,
            completionSessionSnapshot = current.snapshot(completionNow),
            nowEpochMs = completionNow
        )
        if (!saveOutcome.accepted) {
            ReaderTrace.event(
                "locator_save_rejected",
                bookId = bookId,
                sessionId = current.sessionId,
                details = "epoch=${writerLease.epoch} seq=$sequence event=$event"
            )
            return null
        }
        ReaderTrace.event(
            "locator_save_enqueued",
            bookId = bookId,
            sessionId = current.sessionId,
            details = "epoch=${writerLease.epoch} seq=$sequence progress=$safe event=$event"
        )
        if (saveOutcome.newlyFinished) game.recordBookFinished()
        val sessionStart = _uiState.value.sessionStartProgress
        _uiState.value = _uiState.value.copy(
            progress = safe,
            activeMillis = current.activeMillis,
            sessionProgressDelta = (safe - sessionStart).coerceIn(-1f, 1f)
        )
        persistSession()
        return ReaderLocatorCommit(sequence, locatorJson, safe)
    }

    fun onHighlightAdded(expectedOpenInstanceId: String) {
        val current = currentTrackerFor(expectedOpenInstanceId) ?: return
        creditActive(current.onInteraction(SystemClock.elapsedRealtime()))
        current.recordHighlight()
        game.recordHighlight()
        publishActiveMillis()
        persistSession()
    }

    fun onNoteSaved(expectedOpenInstanceId: String, highlightId: String, note: String) {
        val current = currentTrackerFor(expectedOpenInstanceId) ?: return
        creditActive(current.onInteraction(SystemClock.elapsedRealtime()))
        current.recordNote(highlightId, note)
        game.recordNote(highlightId, note)
        publishActiveMillis()
        persistSession()
    }

    @Synchronized
    fun closeBook(expectedOpenInstanceId: String) {
        val current = currentTrackerFor(expectedOpenInstanceId) ?: return
        ReaderTrace.event(
            "reader_close_requested",
            bookId = current.bookId,
            sessionId = current.sessionId
        )
        finishCurrentSession()
    }

    fun traceSessionId(): String? = tracker?.sessionId

    private fun currentTrackerFor(
        expectedOpenInstanceId: String,
        bookId: String? = null
    ): ReadingSessionTracker? {
        if (!openConfirmed) return null
        val current = tracker ?: return null
        if (
            !readerEventBelongsToSession(
                activeOpenInstanceId = openInstanceId,
                expectedOpenInstanceId = expectedOpenInstanceId,
                activeBookId = current.bookId,
                expectedBookId = bookId
            )
        ) {
            return null
        }
        return current
    }

    private fun heartbeat() {
        if (!openConfirmed) return
        val current = tracker ?: return
        if (resumed) {
            creditActive(current.tick(SystemClock.elapsedRealtime()))
            publishActiveMillis()
        }
        persistSession()
    }

    @Synchronized
    private fun finishCurrentSession() {
        val current = tracker
        val writerLease = progressWriterLease
        val wasConfirmed = openConfirmed

        if (current == null) {
            writerLease?.let(library::endReaderProgressSession)
            progressWriterLease = null
            openInstanceId = null
            openConfirmed = false
            resumed = false
            return
        }

        if (wasConfirmed) {
            if (resumed) creditActive(current.onPause(SystemClock.elapsedRealtime()))
            resumed = false
            game.pauseReading()
            publishActiveMillis()
            library.flushProgress(current.bookId)
            library.saveReadingSession(current.snapshot(System.currentTimeMillis()))
            library.flushReadingSession(current.sessionId)
            ReaderTrace.event(
                "reader_closed",
                bookId = current.bookId,
                sessionId = current.sessionId
            )
        } else {
            resumed = false
            _uiState.value = ReaderUiState()
            ReaderTrace.event(
                "reader_prepared_session_discarded",
                bookId = current.bookId,
                sessionId = current.sessionId
            )
        }

        writerLease?.let(library::endReaderProgressSession)
        tracker = null
        progressWriterLease = null
        openInstanceId = null
        openConfirmed = false
        uncreditedActiveMillis = 0L
        locatorDeduplicator.reset()
        locatorSequence = 0L
    }

    private fun creditActive(deltaMillis: Long) {
        if (deltaMillis <= 0L) return
        uncreditedActiveMillis += deltaMillis
        while (uncreditedActiveMillis >= ONE_MINUTE_MS) {
            game.recordReadingMinute()
            uncreditedActiveMillis -= ONE_MINUTE_MS
        }
    }

    private fun publishActiveMillis() {
        val current = tracker ?: return
        _uiState.value = _uiState.value.copy(activeMillis = current.activeMillis)
    }

    private fun persistSession(immediate: Boolean = false) {
        if (!openConfirmed) return
        val current = tracker ?: return
        library.saveReadingSession(current.snapshot(System.currentTimeMillis()))
        if (immediate) library.flushReadingSession(current.sessionId)
    }

    companion object {
        private const val HEARTBEAT_MS = 15_000L
        private const val ONE_MINUTE_MS = 60_000L

        fun factory(
            library: LocalLibraryRepository,
            game: GameRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ReaderViewModel::class.java))
                return ReaderViewModel(library, game) as T
            }
        }
    }
}


internal fun readerEventBelongsToSession(
    activeOpenInstanceId: String?,
    expectedOpenInstanceId: String,
    activeBookId: String?,
    expectedBookId: String? = null
): Boolean =
    activeOpenInstanceId == expectedOpenInstanceId &&
        activeBookId != null &&
        (expectedBookId == null || activeBookId == expectedBookId)
