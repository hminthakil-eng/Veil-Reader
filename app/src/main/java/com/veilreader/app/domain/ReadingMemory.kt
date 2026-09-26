package com.veilreader.app.domain

data class ReadingContinuitySummary(
    val priorSessionCount: Int,
    val totalActiveMillis: Long,
    val pacedPageTurns: Int,
    val recordedHighlightEvents: Int,
    val recordedNoteEvents: Int,
    val firstSessionAtEpochMs: Long?,
    val latestSessionAtEpochMs: Long?,
    val returnGapMillis: Long?,
    val hasHistory: Boolean
)

data class ReadingTimeCapsuleSeed(
    val bookId: String,
    val title: String,
    val author: String,
    val firstReadAtEpochMs: Long?,
    val lastReadAtEpochMs: Long?,
    val sessionCount: Int,
    val totalActiveMillis: Long,
    val pacedPageTurns: Int,
    val recordedHighlightEvents: Int,
    val recordedNoteEvents: Int,
    val completed: Boolean
)

fun deriveReadingContinuity(
    book: Book,
    sessions: List<ReadingSessionSnapshot>,
    nowEpochMs: Long = System.currentTimeMillis()
): ReadingContinuitySummary {
    val relevant = sessions
        .filter { it.bookId == book.id }
        .sortedBy { it.startedAtEpochMs }

    val firstSession = relevant.firstOrNull()?.startedAtEpochMs
    val latestSession = relevant.maxOfOrNull { it.endedAtEpochMs }
    val lastKnownRead = maxOf(
        book.lastOpenedAtEpochMs.coerceAtLeast(0L),
        latestSession?.coerceAtLeast(0L) ?: 0L
    )
    val safeNow = nowEpochMs.coerceAtLeast(0L)
    val gap = if (lastKnownRead > 0L && safeNow >= lastKnownRead) {
        safeNow - lastKnownRead
    } else {
        null
    }

    return ReadingContinuitySummary(
        priorSessionCount = relevant.size,
        totalActiveMillis = relevant.sumOf { it.activeMillis.coerceAtLeast(0L) },
        pacedPageTurns = relevant.sumOf { it.pacedPageTurns.coerceAtLeast(0) },
        recordedHighlightEvents = relevant.sumOf { it.highlightCount.coerceAtLeast(0) },
        recordedNoteEvents = relevant.sumOf { it.noteCount.coerceAtLeast(0) },
        firstSessionAtEpochMs = firstSession,
        latestSessionAtEpochMs = latestSession,
        returnGapMillis = gap,
        hasHistory = relevant.isNotEmpty() || book.lastOpenedAtEpochMs > 0L || book.progress > 0f
    )
}

fun buildTimeCapsuleSeed(
    book: Book,
    continuity: ReadingContinuitySummary
): ReadingTimeCapsuleSeed = ReadingTimeCapsuleSeed(
    bookId = book.id,
    title = book.title,
    author = book.author,
    firstReadAtEpochMs = continuity.firstSessionAtEpochMs,
    lastReadAtEpochMs = continuity.latestSessionAtEpochMs
        ?: book.lastOpenedAtEpochMs.takeIf { it > 0L },
    sessionCount = continuity.priorSessionCount,
    totalActiveMillis = continuity.totalActiveMillis,
    pacedPageTurns = continuity.pacedPageTurns,
    recordedHighlightEvents = continuity.recordedHighlightEvents,
    recordedNoteEvents = continuity.recordedNoteEvents,
    completed = book.finished
)
