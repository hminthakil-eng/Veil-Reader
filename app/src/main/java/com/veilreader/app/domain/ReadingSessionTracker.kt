package com.veilreader.app.domain

/**
 * Pure state machine for measuring engaged reading time.
 *
 * Time accrues only while the reader is resumed and for at most [idleTimeoutMs] after the most
 * recent reader interaction. This keeps background time and long idle screen-on time out of stats.
 */
class ReadingSessionTracker(
    val sessionId: String,
    val bookId: String,
    val startedAtEpochMs: Long,
    startedAtElapsedMs: Long,
    private val idleTimeoutMs: Long = DEFAULT_IDLE_TIMEOUT_MS,
    initialActiveMillis: Long = 0L,
    initialPacedPageTurns: Int = 0,
    initialHighlightCount: Int = 0,
    initialNoteCount: Int = 0,
    initialNotedHighlightIds: Set<String> = emptySet()
) {
    private var resumed = false
    private var lastTickElapsedMs = startedAtElapsedMs
    private var lastInteractionElapsedMs = startedAtElapsedMs
    private var lastPacedPageActiveMillis: Long? = null
    private val notedHighlightIds = initialNotedHighlightIds.toMutableSet()

    var activeMillis: Long = initialActiveMillis.coerceAtLeast(0L)
        private set
    var pacedPageTurns: Int = initialPacedPageTurns.coerceAtLeast(0)
        private set
    var highlightCount: Int = initialHighlightCount.coerceAtLeast(0)
        private set
    var noteCount: Int = initialNoteCount.coerceAtLeast(0)
        private set

    fun onResume(nowElapsedMs: Long): Long {
        val accrued = accrue(nowElapsedMs)
        resumed = true
        lastTickElapsedMs = nowElapsedMs
        lastInteractionElapsedMs = nowElapsedMs
        lastPacedPageActiveMillis = null
        return accrued
    }

    fun onInteraction(nowElapsedMs: Long): Long {
        val accrued = accrue(nowElapsedMs)
        lastInteractionElapsedMs = nowElapsedMs
        return accrued
    }

    fun onPause(nowElapsedMs: Long): Long {
        val accrued = accrue(nowElapsedMs)
        resumed = false
        lastTickElapsedMs = nowElapsedMs
        lastPacedPageActiveMillis = null
        return accrued
    }

    fun tick(nowElapsedMs: Long): Long = accrue(nowElapsedMs)

    /**
     * Records a real user page turn and returns the engaged-reading interval since the
     * previous page turn in the same uninterrupted foreground segment.
     *
     * The first turn after open/resume establishes an anchor only. This prevents startup,
     * background time and cross-session gaps from contaminating personal pace analytics.
     */
    fun recordPacedPageTurn(): Long? {
        pacedPageTurns += 1
        val currentActive = activeMillis
        val previousActive = lastPacedPageActiveMillis
        lastPacedPageActiveMillis = currentActive
        return previousActive
            ?.let { currentActive - it }
            ?.takeIf { it > 0L }
    }

    fun recordHighlight() {
        highlightCount += 1
    }

    fun recordNote(highlightId: String, note: String) {
        if (note.isBlank()) return
        if (notedHighlightIds.add(highlightId)) noteCount += 1
    }

    fun snapshot(endedAtEpochMs: Long): ReadingSessionSnapshot = ReadingSessionSnapshot(
        id = sessionId,
        bookId = bookId,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
        activeMillis = activeMillis,
        pacedPageTurns = pacedPageTurns,
        highlightCount = highlightCount,
        noteCount = noteCount
    )

    private fun accrue(nowElapsedMs: Long): Long {
        require(nowElapsedMs >= lastTickElapsedMs) { "Elapsed realtime must be monotonic." }
        if (!resumed) {
            lastTickElapsedMs = nowElapsedMs
            return 0L
        }

        val activeUntil = minOf(nowElapsedMs, lastInteractionElapsedMs + idleTimeoutMs)
        val delta = (activeUntil - lastTickElapsedMs).coerceAtLeast(0L)
        activeMillis += delta
        lastTickElapsedMs = nowElapsedMs
        return delta
    }

    companion object {
        const val DEFAULT_IDLE_TIMEOUT_MS = 5L * 60L * 1000L

        fun restore(
            snapshot: ReadingSessionSnapshot,
            bookId: String,
            startedAtElapsedMs: Long,
            notedHighlightIds: Set<String> = emptySet(),
            idleTimeoutMs: Long = DEFAULT_IDLE_TIMEOUT_MS
        ): ReadingSessionTracker? {
            if (
                snapshot.id.isBlank() ||
                snapshot.bookId != bookId ||
                snapshot.startedAtEpochMs < 0L
            ) {
                return null
            }
            return ReadingSessionTracker(
                sessionId = snapshot.id,
                bookId = bookId,
                startedAtEpochMs = snapshot.startedAtEpochMs,
                startedAtElapsedMs = startedAtElapsedMs,
                idleTimeoutMs = idleTimeoutMs,
                initialActiveMillis = snapshot.activeMillis,
                initialPacedPageTurns = snapshot.pacedPageTurns,
                initialHighlightCount = snapshot.highlightCount,
                initialNoteCount = snapshot.noteCount,
                initialNotedHighlightIds = notedHighlightIds
            )
        }
    }
}

/** Durable/portable representation. bookId can be null after the source book is deleted. */
data class ReadingSessionSnapshot(
    val id: String,
    val bookId: String?,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val activeMillis: Long,
    val pacedPageTurns: Int,
    val highlightCount: Int,
    val noteCount: Int
)
