package com.veilreader.app.ui.reader

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.domain.ReadingSessionTracker
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderUiState(
    val bookId: String? = null,
    val progress: Float = 0f,
    val activeMillis: Long = 0L
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
    private var resumed = false
    private var uncreditedActiveMillis = 0L
    private val locatorDeduplicator = ReaderLocatorDeduplicator()

    init {
        viewModelScope.launch {
            while (true) {
                delay(HEARTBEAT_MS)
                heartbeat()
            }
        }
    }

    /** Idempotent across recomposition/configuration changes; starts a new session only per book entry. */
    fun openBook(bookId: String, initialProgress: Float) {
        if (tracker?.bookId == bookId) return
        finishCurrentSession()
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        tracker = ReadingSessionTracker(
            sessionId = UUID.randomUUID().toString(),
            bookId = bookId,
            startedAtEpochMs = nowWall,
            startedAtElapsedMs = nowElapsed
        )
        ReaderTrace.event(
            "reader_open",
            bookId = bookId,
            sessionId = tracker?.sessionId,
            details = "initialProgress=${initialProgress.coerceIn(0f, 1f)}"
        )
        resumed = false
        uncreditedActiveMillis = 0L
        locatorDeduplicator.reset()
        _uiState.value = ReaderUiState(bookId = bookId, progress = initialProgress.coerceIn(0f, 1f))
        persistSession(immediate = true)
    }

    fun onResume() {
        val current = tracker ?: return
        if (resumed) return
        resumed = true
        creditActive(current.onResume(SystemClock.elapsedRealtime()))
        publishActiveMillis()
        persistSession()
    }

    fun onPause() {
        val current = tracker ?: return
        ReaderTrace.event("reader_pause", bookId = current.bookId, sessionId = current.sessionId)
        library.flushProgress(current.bookId)
        ReaderTrace.event("locator_flush_enqueued", bookId = current.bookId, sessionId = current.sessionId)
        if (!resumed) {
            library.flushReadingSession(current.sessionId)
            game.pauseReading()
            return
        }
        creditActive(current.onPause(SystemClock.elapsedRealtime()))
        resumed = false
        game.pauseReading()
        publishActiveMillis()
        persistSession(immediate = true)
    }

    fun onUserInteraction() {
        val current = tracker ?: return
        creditActive(current.onInteraction(SystemClock.elapsedRealtime()))
        publishActiveMillis()
    }

    fun onLocatorUpdate(
        bookId: String,
        progression: Double,
        locatorJson: String,
        locationKey: String,
        event: ReaderLocatorEvent
    ) {
        val current = tracker?.takeIf { it.bookId == bookId } ?: return
        if (!locatorDeduplicator.accept(locationKey)) {
            ReaderTrace.event(
                "locator_duplicate_ignored",
                bookId = bookId,
                sessionId = current.sessionId,
                details = "event=$event"
            )
            return
        }

        creditActive(current.onInteraction(SystemClock.elapsedRealtime()))

        if (event.countsPageTurn && resumed && game.recordPageTurn(locationKey)) {
            current.recordPacedPageTurn()
        }

        val safe = (if (progression.isFinite()) progression else _uiState.value.progress.toDouble())
            .coerceIn(0.0, 1.0).toFloat()
        ReaderTrace.event(
            "locator_save_requested",
            bookId = bookId,
            sessionId = current.sessionId,
            details = "progress=$safe event=$event"
        )
        val completed = library.saveProgress(bookId, safe.toDouble(), locatorJson)
        ReaderTrace.event(
            "locator_save_enqueued",
            bookId = bookId,
            sessionId = current.sessionId,
            details = "progress=$safe event=$event"
        )
        if (completed) game.recordBookFinished()
        _uiState.value = _uiState.value.copy(progress = safe, activeMillis = current.activeMillis)
        persistSession()
    }

    fun onHighlightAdded() {
        val current = tracker ?: return
        creditActive(current.onInteraction(SystemClock.elapsedRealtime()))
        current.recordHighlight()
        game.recordHighlight()
        publishActiveMillis()
        persistSession()
    }

    fun onNoteSaved(highlightId: String, note: String) {
        val current = tracker ?: return
        creditActive(current.onInteraction(SystemClock.elapsedRealtime()))
        current.recordNote(highlightId, note)
        game.recordNote(highlightId, note)
        publishActiveMillis()
        persistSession()
    }

    fun closeBook() {
        tracker?.let { current ->
            ReaderTrace.event("reader_close_requested", bookId = current.bookId, sessionId = current.sessionId)
        }
        finishCurrentSession()
    }

    fun traceSessionId(): String? = tracker?.sessionId

    private fun heartbeat() {
        val current = tracker ?: return
        if (resumed) {
            creditActive(current.tick(SystemClock.elapsedRealtime()))
            publishActiveMillis()
        }
        persistSession()
    }

    private fun finishCurrentSession() {
        val current = tracker ?: return
        if (resumed) creditActive(current.onPause(SystemClock.elapsedRealtime()))
        resumed = false
        game.pauseReading()
        publishActiveMillis()
        library.flushProgress(current.bookId)
        library.saveReadingSession(current.snapshot(System.currentTimeMillis()))
        library.flushReadingSession(current.sessionId)
        ReaderTrace.event("reader_closed", bookId = current.bookId, sessionId = current.sessionId)
        tracker = null
        uncreditedActiveMillis = 0L
        locatorDeduplicator.reset()
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
