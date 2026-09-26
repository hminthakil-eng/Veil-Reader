package com.veilreader.app.domain

/**
 * Durable record of one completed reading cycle.
 *
 * Unlike the older derived Time Capsule, this is a snapshot captured at the exact transition into
 * completion. Later rereads, notes, bookmarks, or metadata edits must not mutate this record.
 */
data class ReadingCycleRecord(
    val id: String,
    val bookId: String,
    val cycleIndex: Int,
    val titleSnapshot: String,
    val authorSnapshot: String,
    val startedAtEpochMs: Long?,
    val completedAtEpochMs: Long,
    val finalLocatorJson: String,
    val sessionCount: Int,
    val totalActiveMillis: Long,
    val pacedPageTurns: Int,
    val highlightCount: Int,
    val noteCount: Int,
    val bookmarkCount: Int,
    val sealCode: String,
    val timeline: List<ReadingHistoryEvent>
)

data class PassageVisit(
    val id: String,
    val highlightId: String,
    val bookId: String,
    val locatorJson: String,
    val viewedAtEpochMs: Long
)

fun buildSealedReadingCycle(
    book: Book,
    cycleIndex: Int,
    sessions: List<ReadingSessionSnapshot>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    completedAtEpochMs: Long,
    finalLocatorJson: String
): ReadingCycleRecord {
    require(cycleIndex >= 1)
    require(completedAtEpochMs > 0L)

    val relevantSessions = sessions
        .filter { it.bookId == book.id && it.startedAtEpochMs in 1L..completedAtEpochMs }
        .sortedBy { it.startedAtEpochMs }
    val relevantHighlights = highlights
        .filter { it.bookId == book.id && it.createdAtEpochMs in 1L..completedAtEpochMs }
        .sortedBy { it.createdAtEpochMs }
    val relevantBookmarks = bookmarks
        .filter { it.bookId == book.id && it.createdAtEpochMs in 1L..completedAtEpochMs }
        .sortedBy { it.createdAtEpochMs }

    val timeline = buildList {
        if (book.addedAtEpochMs in 1L..completedAtEpochMs) {
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
            add(
                ReadingHistoryEvent(
                    id = "session:${session.id}",
                    kind = ReadingHistoryEventKind.READING_SESSION,
                    timestampEpochMs = session.startedAtEpochMs,
                    title = "Reading session",
                    detail = buildString {
                        append(session.activeMillis.coerceAtLeast(0L) / 60_000L)
                        append("m active")
                        if (session.pacedPageTurns > 0) {
                            append(" · ").append(session.pacedPageTurns).append(" paced turns")
                        }
                    }
                )
            )
        }
        relevantHighlights.forEach { highlight ->
            add(
                ReadingHistoryEvent(
                    id = "highlight:${highlight.id}",
                    kind = ReadingHistoryEventKind.PASSAGE_PRESERVED,
                    timestampEpochMs = highlight.createdAtEpochMs,
                    title = if (highlight.note.isBlank()) "Passage preserved" else "Annotated passage preserved",
                    detail = highlight.quote.replace(Regex("\\s+"), " ").trim().take(120)
                        .takeIf(String::isNotBlank)
                )
            )
        }
        relevantBookmarks.forEach { bookmark ->
            add(
                ReadingHistoryEvent(
                    id = "bookmark:${bookmark.id}",
                    kind = ReadingHistoryEventKind.LOCATION_MARKED,
                    timestampEpochMs = bookmark.createdAtEpochMs,
                    title = "Location marked",
                    detail = bookmark.label.trim().takeIf(String::isNotBlank)
                )
            )
        }
        add(
            ReadingHistoryEvent(
                id = "completed:${book.id}:$cycleIndex",
                kind = ReadingHistoryEventKind.COMPLETED,
                timestampEpochMs = completedAtEpochMs,
                title = "Reading cycle completed"
            )
        )
    }.sortedWith(compareBy<ReadingHistoryEvent> { it.timestampEpochMs }.thenBy { it.id })

    val startedAt = relevantSessions.firstOrNull()?.startedAtEpochMs
    val seal = readingCapsuleSealCode(
        bookId = "${book.id}:$cycleIndex",
        firstRecordedAtEpochMs = startedAt ?: book.addedAtEpochMs.takeIf { it > 0L },
        latestRecordedAtEpochMs = completedAtEpochMs
    )

    return ReadingCycleRecord(
        id = "cycle:${book.id}:$cycleIndex:$completedAtEpochMs",
        bookId = book.id,
        cycleIndex = cycleIndex,
        titleSnapshot = book.title,
        authorSnapshot = book.author,
        startedAtEpochMs = startedAt,
        completedAtEpochMs = completedAtEpochMs,
        finalLocatorJson = finalLocatorJson,
        sessionCount = relevantSessions.size,
        totalActiveMillis = relevantSessions.sumOf { it.activeMillis.coerceAtLeast(0L) },
        pacedPageTurns = relevantSessions.sumOf { it.pacedPageTurns.coerceAtLeast(0) },
        highlightCount = relevantHighlights.size,
        noteCount = relevantHighlights.count { it.note.isNotBlank() },
        bookmarkCount = relevantBookmarks.size,
        sealCode = seal,
        timeline = timeline
    )
}

fun exactPassageVisits(
    highlight: Highlight,
    visits: List<PassageVisit>
): List<PassageVisit> =
    visits.asSequence()
        .filter {
            it.highlightId == highlight.id &&
                it.bookId == highlight.bookId &&
                it.viewedAtEpochMs > highlight.createdAtEpochMs
        }
        .sortedBy { it.viewedAtEpochMs }
        .toList()
