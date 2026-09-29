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
    milestones: List<ReadingMilestoneRecord> = emptyList(),
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
                    timestampEpochMs = book.addedAtEpochMs
                )
            )
        }
        relevantSessions.forEach { session ->
            add(
                ReadingHistoryEvent(
                    id = "session:${session.id}",
                    kind = ReadingHistoryEventKind.READING_SESSION,
                    timestampEpochMs = session.startedAtEpochMs,
                    activeMillis = session.activeMillis.coerceAtLeast(0L),
                    pacedPageTurns = session.pacedPageTurns.coerceAtLeast(0),
                    highlightEventCount = session.highlightCount.coerceAtLeast(0),
                    noteEventCount = session.noteCount.coerceAtLeast(0)
                )
            )
        }
        relevantHighlights.forEach { highlight ->
            add(
                ReadingHistoryEvent(
                    id = "highlight:${highlight.id}",
                    kind = ReadingHistoryEventKind.PASSAGE_PRESERVED,
                    timestampEpochMs = highlight.createdAtEpochMs,
                    annotated = highlight.note.isNotBlank(),
                    excerpt = highlight.quote.replace(Regex("\\s+"), " ").trim().take(120)
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
                    locationLabel = bookmark.label.trim().takeIf(String::isNotBlank)
                )
            )
        }
        milestones
            .asSequence()
            .filter { it.bookId == book.id && it.reachedAtEpochMs in 1L..completedAtEpochMs }
            .sortedBy { it.reachedAtEpochMs }
            .forEach { milestone ->
                add(
                    ReadingHistoryEvent(
                        id = milestone.id,
                        kind = ReadingHistoryEventKind.READING_MILESTONE,
                        timestampEpochMs = milestone.reachedAtEpochMs,
                        milestoneKind = milestone.kind
                    )
                )
            }
        add(
            ReadingHistoryEvent(
                id = "completed:${book.id}:$cycleIndex",
                kind = ReadingHistoryEventKind.COMPLETED,
                timestampEpochMs = completedAtEpochMs
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


enum class ReadingMilestoneKind(val progression: Float) {
    FIRST_OPENED(0f),
    PROGRESS_25(0.25f),
    PROGRESS_50(0.50f),
    PROGRESS_75(0.75f)
}

data class ReadingMilestoneRecord(
    val id: String,
    val bookId: String,
    val kind: ReadingMilestoneKind,
    val reachedAtEpochMs: Long,
    val progression: Float,
    val locatorJson: String?
)

fun crossedReadingMilestones(
    bookId: String,
    previousProgress: Float,
    newProgress: Float,
    reachedAtEpochMs: Long,
    locatorJson: String
): List<ReadingMilestoneRecord> {
    if (reachedAtEpochMs <= 0L) return emptyList()
    val from = previousProgress.coerceIn(0f, 1f)
    val to = newProgress.coerceIn(0f, 1f)
    if (to <= from) return emptyList()

    return listOf(
        ReadingMilestoneKind.PROGRESS_25,
        ReadingMilestoneKind.PROGRESS_50,
        ReadingMilestoneKind.PROGRESS_75
    ).mapNotNull { kind ->
        if (from < kind.progression && to >= kind.progression) {
            ReadingMilestoneRecord(
                id = "milestone:$bookId:${kind.name}",
                bookId = bookId,
                kind = kind,
                reachedAtEpochMs = reachedAtEpochMs,
                progression = kind.progression,
                locatorJson = locatorJson
            )
        } else {
            null
        }
    }
}

fun firstOpenedMilestone(
    bookId: String,
    openedAtEpochMs: Long,
    locatorJson: String?
): ReadingMilestoneRecord? =
    openedAtEpochMs.takeIf { it > 0L }?.let {
        ReadingMilestoneRecord(
            id = "milestone:$bookId:${ReadingMilestoneKind.FIRST_OPENED.name}",
            bookId = bookId,
            kind = ReadingMilestoneKind.FIRST_OPENED,
            reachedAtEpochMs = it,
            progression = 0f,
            locatorJson = locatorJson
        )
    }
