package com.veilreader.app.domain

enum class ReadingHistoryEventKind {
    ARCHIVED,
    READING_SESSION,
    PASSAGE_PRESERVED,
    LOCATION_MARKED,
    COMPLETED,
    LATEST_VOLUME_ACTIVITY
}

data class ReadingHistoryEvent(
    val id: String,
    val kind: ReadingHistoryEventKind,
    val timestampEpochMs: Long,
    val title: String,
    val detail: String? = null
)

data class ReadingTimeCapsule(
    val book: Book,
    val sealCode: String,
    val firstRecordedAtEpochMs: Long?,
    val latestRecordedAtEpochMs: Long?,
    val sessionCount: Int,
    val totalActiveMillis: Long,
    val pacedPageTurns: Int,
    val highlightCount: Int,
    val noteCount: Int,
    val bookmarkCount: Int,
    val timeline: List<ReadingHistoryEvent>,
    val cycleIndex: Int = 1,
    val completedAtEpochMs: Long? = null,
    /**
     * Veil currently persists completion state but not the exact instant it first became complete.
     * Never present latestRecordedAtEpochMs as a completion timestamp.
     */
    val exactCompletionTimeKnown: Boolean = false
)

fun deriveReadingTimeCapsules(
    books: List<Book>,
    sessions: List<ReadingSessionSnapshot>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    sealedCycles: List<ReadingCycleRecord> = emptyList()
): List<ReadingTimeCapsule> {
    val booksById = books.associateBy { it.id }
    val sessionsByBook = sessions
        .filter { !it.bookId.isNullOrBlank() }
        .groupBy { requireNotNull(it.bookId) }
    val highlightsByBook = highlights.groupBy { it.bookId }
    val bookmarksByBook = bookmarks.groupBy { it.bookId }
    val sealedBookIds = sealedCycles.mapTo(mutableSetOf()) { it.bookId }

    val exact = sealedCycles.mapNotNull { cycle ->
        booksById[cycle.bookId]?.let { book ->
            deriveReadingTimeCapsule(book, cycle)
        }
    }
    val legacy = books
        .asSequence()
        .filter { it.finished && it.id !in sealedBookIds }
        .map { book ->
            deriveReadingTimeCapsule(
                book = book,
                sessions = sessionsByBook[book.id].orEmpty(),
                highlights = highlightsByBook[book.id].orEmpty(),
                bookmarks = bookmarksByBook[book.id].orEmpty()
            )
        }
        .toList()

    return (exact + legacy)
        .sortedWith(
            compareByDescending<ReadingTimeCapsule> {
                it.completedAtEpochMs ?: it.latestRecordedAtEpochMs ?: 0L
            }
                .thenByDescending { it.cycleIndex }
                .thenBy { it.book.title.lowercase() }
        )
}

fun deriveReadingTimeCapsule(
    book: Book,
    cycle: ReadingCycleRecord
): ReadingTimeCapsule {
    require(cycle.bookId == book.id)
    return ReadingTimeCapsule(
        book = book.copy(
            title = cycle.titleSnapshot,
            author = cycle.authorSnapshot,
            finished = true
        ),
        sealCode = cycle.sealCode,
        firstRecordedAtEpochMs = cycle.startedAtEpochMs,
        latestRecordedAtEpochMs = cycle.completedAtEpochMs,
        sessionCount = cycle.sessionCount,
        totalActiveMillis = cycle.totalActiveMillis,
        pacedPageTurns = cycle.pacedPageTurns,
        highlightCount = cycle.highlightCount,
        noteCount = cycle.noteCount,
        bookmarkCount = cycle.bookmarkCount,
        timeline = cycle.timeline,
        cycleIndex = cycle.cycleIndex,
        completedAtEpochMs = cycle.completedAtEpochMs,
        exactCompletionTimeKnown = true
    )
}

fun deriveReadingTimeCapsule(
    book: Book,
    sessions: List<ReadingSessionSnapshot>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>
): ReadingTimeCapsule {
    require(book.finished) { "Only completed volumes can form a sealed time capsule." }

    val relevantSessions = sessions
        .filter { it.bookId == book.id }
        .sortedBy { it.startedAtEpochMs }
    val relevantHighlights = highlights
        .filter { it.bookId == book.id }
        .sortedBy { it.createdAtEpochMs }
    val relevantBookmarks = bookmarks
        .filter { it.bookId == book.id }
        .sortedBy { it.createdAtEpochMs }

    val events = buildList {
        if (book.addedAtEpochMs > 0L) {
            add(
                ReadingHistoryEvent(
                    id = "archive:${book.id}",
                    kind = ReadingHistoryEventKind.ARCHIVED,
                    timestampEpochMs = book.addedAtEpochMs,
                    title = "Entered the Grayfog Archive"
                )
            )
        }

        relevantSessions.forEach { session ->
            if (session.startedAtEpochMs <= 0L) return@forEach
            val duration = compactDurationLabel(session.activeMillis)
            val detail = buildString {
                append(duration).append(" active")
                if (session.pacedPageTurns > 0) {
                    append(" · ").append(session.pacedPageTurns).append(" paced turns")
                }
                if (session.highlightCount > 0) {
                    append(" · ").append(session.highlightCount).append(" highlight events")
                }
                if (session.noteCount > 0) {
                    append(" · ").append(session.noteCount).append(" note events")
                }
            }
            add(
                ReadingHistoryEvent(
                    id = "session:${session.id}",
                    kind = ReadingHistoryEventKind.READING_SESSION,
                    timestampEpochMs = session.startedAtEpochMs,
                    title = "Reading session",
                    detail = detail
                )
            )
        }

        relevantHighlights.forEach { highlight ->
            if (highlight.createdAtEpochMs <= 0L) return@forEach
            add(
                ReadingHistoryEvent(
                    id = "highlight:${highlight.id}",
                    kind = ReadingHistoryEventKind.PASSAGE_PRESERVED,
                    timestampEpochMs = highlight.createdAtEpochMs,
                    title = if (highlight.note.isBlank()) {
                        "Passage preserved"
                    } else {
                        "Annotated passage preserved"
                    },
                    detail = highlight.quote
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .take(120)
                        .takeIf { it.isNotBlank() }
                )
            )
        }

        relevantBookmarks.forEach { bookmark ->
            if (bookmark.createdAtEpochMs <= 0L) return@forEach
            add(
                ReadingHistoryEvent(
                    id = "bookmark:${bookmark.id}",
                    kind = ReadingHistoryEventKind.LOCATION_MARKED,
                    timestampEpochMs = bookmark.createdAtEpochMs,
                    title = "Location marked",
                    detail = bookmark.label.trim().takeIf { it.isNotBlank() }
                )
            )
        }

        if (book.lastOpenedAtEpochMs > 0L) {
            val latestKnown = listOf(
                relevantSessions.maxOfOrNull { it.endedAtEpochMs } ?: 0L,
                relevantHighlights.maxOfOrNull { it.createdAtEpochMs } ?: 0L,
                relevantBookmarks.maxOfOrNull { it.createdAtEpochMs } ?: 0L,
                book.addedAtEpochMs
            ).maxOrNull() ?: 0L

            if (book.lastOpenedAtEpochMs > latestKnown) {
                add(
                    ReadingHistoryEvent(
                        id = "latest:${book.id}",
                        kind = ReadingHistoryEventKind.LATEST_VOLUME_ACTIVITY,
                        timestampEpochMs = book.lastOpenedAtEpochMs,
                        title = "Latest recorded volume activity"
                    )
                )
            }
        }
    }.sortedWith(
        compareBy<ReadingHistoryEvent> { it.timestampEpochMs }
            .thenBy { it.id }
    )

    val first = events.firstOrNull()?.timestampEpochMs
    val latest = events.lastOrNull()?.timestampEpochMs
        ?: book.lastOpenedAtEpochMs.takeIf { it > 0L }

    return ReadingTimeCapsule(
        book = book,
        sealCode = readingCapsuleSealCode(book.id, first, latest),
        firstRecordedAtEpochMs = first,
        latestRecordedAtEpochMs = latest,
        sessionCount = relevantSessions.size,
        totalActiveMillis = relevantSessions.sumOf { it.activeMillis.coerceAtLeast(0L) },
        pacedPageTurns = relevantSessions.sumOf { it.pacedPageTurns.coerceAtLeast(0) },
        highlightCount = relevantHighlights.size,
        noteCount = relevantHighlights.count { it.note.isNotBlank() },
        bookmarkCount = relevantBookmarks.size,
        timeline = events
    )
}

fun readingCapsuleSealCode(
    bookId: String,
    firstRecordedAtEpochMs: Long?,
    latestRecordedAtEpochMs: Long?
): String {
    val raw = buildString {
        append(bookId)
        append('|')
        append(firstRecordedAtEpochMs ?: 0L)
        append('|')
        append(latestRecordedAtEpochMs ?: 0L)
    }
    val hash = raw.fold(0x45D9F3B) { acc, char -> (acc * 33) xor char.code }
    val unsigned = hash.toLong() and 0xFFFF_FFFFL
    return "VR-" + unsigned.toString(16).uppercase().padStart(8, '0').takeLast(8)
}

private fun compactDurationLabel(activeMillis: Long): String {
    val totalMinutes = activeMillis.coerceAtLeast(0L) / 60_000L
    return when {
        totalMinutes >= 60L -> {
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
        }
        totalMinutes > 0L -> "${totalMinutes}m"
        else -> "<1m"
    }
}
