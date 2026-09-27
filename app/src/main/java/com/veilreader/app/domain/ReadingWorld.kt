package com.veilreader.app.domain

/**
 * Shared factual input for Cathedral realms.
 *
 * This is deliberately a pure projection: it owns no persistence and never mutates Reader or
 * progression state. Archive, Castle, Living Mirror and Ritual may interpret this snapshot
 * differently, but they must agree on these underlying reading facts.
 */
data class ReadingWorldSnapshot(
    val importedBookCount: Int,
    val startedBookCount: Int,
    val completedBookCount: Int,
    val sessionCount: Int,
    val totalActiveMillis: Long,
    val pacedPageTurns: Int,
    val highlightCount: Int,
    val noteCount: Int,
    val bookmarkCount: Int,
    val rereadCycleCount: Int,
    val lastActivityAtEpochMs: Long?
) {
    val hasReadingHistory: Boolean
        get() = sessionCount > 0 || startedBookCount > 0 || highlightCount > 0 || bookmarkCount > 0
}

/**
 * Derives realm-independent facts from canonical Reader/Library history.
 *
 * No rank, XP, atmosphere, resonance or visual language belongs here. Those remain downstream
 * interpretations so reading truth cannot be rewritten by gamification.
 */
fun deriveReadingWorldSnapshot(
    books: List<Book>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    sessions: List<ReadingSessionSnapshot>,
    readingCycles: List<ReadingCycleRecord>
): ReadingWorldSnapshot {
    val imported = books.filter { it.isImported }
    val sessionActivity = sessions.maxOfOrNull { it.endedAtEpochMs.coerceAtLeast(it.startedAtEpochMs) }
    val bookActivity = books.maxOfOrNull { it.lastOpenedAtEpochMs }?.takeIf { it > 0L }

    return ReadingWorldSnapshot(
        importedBookCount = imported.size,
        startedBookCount = books.count {
            it.progress > 0f || it.pagesRead > 0 || it.lastOpenedAtEpochMs > 0L || it.finished
        },
        completedBookCount = books.count { it.finished },
        sessionCount = sessions.size,
        totalActiveMillis = sessions.sumOf { it.activeMillis.coerceAtLeast(0L) },
        pacedPageTurns = sessions.sumOf { it.pacedPageTurns.coerceAtLeast(0) },
        highlightCount = highlights.size,
        noteCount = highlights.count { it.note.isNotBlank() },
        bookmarkCount = bookmarks.size,
        rereadCycleCount = readingCycles.count { it.cycleIndex > 1 },
        lastActivityAtEpochMs = listOfNotNull(sessionActivity, bookActivity).maxOrNull()
    )
}
