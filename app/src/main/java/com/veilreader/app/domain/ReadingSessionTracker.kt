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
    private val idleTimeoutMs: Long = DEFAULT_IDLE_TIMEOUT_MS
) {
    private var resumed = false
    private var lastTickElapsedMs = startedAtElapsedMs
    private var lastInteractionElapsedMs = startedAtElapsedMs
    private val notedHighlightIds = linkedSetOf<String>()

    var activeMillis: Long = 0L
        private set
    var pacedPageTurns: Int = 0
        private set
    var highlightCount: Int = 0
        private set
    var noteCount: Int = 0
        private set

    fun onResume(nowElapsedMs: Long): Long {
        val accrued = accrue(nowElapsedMs)
        resumed = true
        lastTickElapsedMs = nowElapsedMs
        lastInteractionElapsedMs = nowElapsedMs
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
        return accrued
    }

    fun tick(nowElapsedMs: Long): Long = accrue(nowElapsedMs)

    fun recordPacedPageTurn() {
        pacedPageTurns += 1
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
