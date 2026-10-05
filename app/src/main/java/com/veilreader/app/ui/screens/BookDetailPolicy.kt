package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingMilestoneKind
import com.veilreader.app.domain.ReadingMilestoneRecord
import com.veilreader.app.ui.theme.VeilComposition

internal data class BookDetailAdaptivePolicy(
    val compactHero: Boolean,
    val stackUtilityActions: Boolean
)

internal fun bookDetailAdaptivePolicy(
    widthDp: Int,
    fontScale: Float
): BookDetailAdaptivePolicy {
    val safeWidth = widthDp.coerceAtLeast(0)
    val safeScale = if (fontScale.isFinite() && fontScale > 0f) fontScale else 1f
    return BookDetailAdaptivePolicy(
        compactHero = safeWidth / safeScale.coerceAtLeast(1f) < VeilComposition.ArtifactIdentityMinWidthDp,
        stackUtilityActions = shouldStackDenseChoices(
            widthDp = safeWidth,
            fontScale = safeScale,
            optionCount = 2
        )
    )
}

internal enum class BookDetailJourneyPhase {
    NOT_STARTED,
    READING,
    COMPLETED
}

internal enum class BookDetailJourneyAction {
    OPEN,
    CONTINUE,
    READ_AGAIN,
    UNAVAILABLE
}

internal data class BookDetailJourneyState(
    val progress: Float,
    val phase: BookDetailJourneyPhase,
    val action: BookDetailJourneyAction,
    val chapter: String?
)

internal fun bookDetailJourneyState(
    book: Book,
    progress: Float
): BookDetailJourneyState {
    val safeProgress = when {
        book.finished -> 1f
        progress.isFinite() -> progress.coerceIn(0f, 1f)
        else -> 0f
    }
    val phase = when {
        book.finished -> BookDetailJourneyPhase.COMPLETED
        safeProgress > 0f -> BookDetailJourneyPhase.READING
        else -> BookDetailJourneyPhase.NOT_STARTED
    }
    val action = when {
        !book.isImported -> BookDetailJourneyAction.UNAVAILABLE
        phase == BookDetailJourneyPhase.COMPLETED -> BookDetailJourneyAction.READ_AGAIN
        phase == BookDetailJourneyPhase.READING -> BookDetailJourneyAction.CONTINUE
        else -> BookDetailJourneyAction.OPEN
    }
    val chapter = book.currentChapter
        .trim()
        .takeIf {
            phase == BookDetailJourneyPhase.READING &&
                it.isNotBlank() &&
                !it.equals("Not started", ignoreCase = true)
        }

    return BookDetailJourneyState(
        progress = safeProgress,
        phase = phase,
        action = action,
        chapter = chapter
    )
}

internal enum class BookDetailArchiveEventKind {
    ARCHIVED,
    FIRST_OPENED,
    MILESTONE,
    COMPLETED
}

internal data class BookDetailArchiveEvent(
    val id: String,
    val kind: BookDetailArchiveEventKind,
    val timestampEpochMs: Long,
    val progression: Float? = null,
    val cycleIndex: Int? = null
)

internal fun bookDetailArchiveTimeline(
    book: Book,
    milestones: List<ReadingMilestoneRecord>,
    cycles: List<ReadingCycleRecord>
): List<BookDetailArchiveEvent> {
    val events = mutableListOf<BookDetailArchiveEvent>()

    book.addedAtEpochMs.takeIf { it > 0L }?.let { archivedAt ->
        events += BookDetailArchiveEvent(
            id = "archived:${book.id}",
            kind = BookDetailArchiveEventKind.ARCHIVED,
            timestampEpochMs = archivedAt
        )
    }

    milestones
        .asSequence()
        .filter {
            it.kind == ReadingMilestoneKind.FIRST_OPENED &&
                it.reachedAtEpochMs > 0L
        }
        .minWithOrNull(compareBy<ReadingMilestoneRecord> { it.reachedAtEpochMs }.thenBy { it.id })
        ?.let { firstOpened ->
            events += BookDetailArchiveEvent(
                id = firstOpened.id,
                kind = BookDetailArchiveEventKind.FIRST_OPENED,
                timestampEpochMs = firstOpened.reachedAtEpochMs
            )
        }

    milestones
        .asSequence()
        .filter {
            it.kind != ReadingMilestoneKind.FIRST_OPENED &&
                it.reachedAtEpochMs > 0L &&
                it.progression.isFinite() &&
                it.progression > 0f &&
                it.progression <= 1f
        }
        .groupBy { it.kind }
        .values
        .mapNotNull { records ->
            records.minWithOrNull(
                compareBy<ReadingMilestoneRecord> { it.reachedAtEpochMs }.thenBy { it.id }
            )
        }
        .forEach { milestone ->
            events += BookDetailArchiveEvent(
                id = milestone.id,
                kind = BookDetailArchiveEventKind.MILESTONE,
                timestampEpochMs = milestone.reachedAtEpochMs,
                progression = milestone.progression
            )
        }

    cycles
        .asSequence()
        .filter { it.cycleIndex >= 1 && it.completedAtEpochMs > 0L }
        .groupBy { it.cycleIndex }
        .values
        .mapNotNull { records ->
            records.minWithOrNull(
                compareBy<ReadingCycleRecord> { it.completedAtEpochMs }.thenBy { it.id }
            )
        }
        .forEach { cycle ->
            events += BookDetailArchiveEvent(
                id = cycle.id,
                kind = BookDetailArchiveEventKind.COMPLETED,
                timestampEpochMs = cycle.completedAtEpochMs,
                cycleIndex = cycle.cycleIndex
            )
        }

    return events.sortedWith(
        compareBy<BookDetailArchiveEvent> { it.timestampEpochMs }
            .thenBy { it.kind.ordinal }
            .thenBy { it.id }
    )
}

internal enum class PreservedMemoryKind { QUOTE_ONLY, NOTE_ONLY, QUOTE_AND_NOTE }

internal data class PreservedMemoryFragment(
    val id: String,
    val quote: String?,
    val note: String?,
    val createdAtEpochMs: Long,
    val kind: PreservedMemoryKind
)

internal data class BookDetailPreservedMemory(
    val fragments: List<PreservedMemoryFragment>,
    val totalUseful: Int
) {
    val hiddenCount: Int get() = (totalUseful - fragments.size).coerceAtLeast(0)
}

internal fun bookDetailPreservedMemory(
    highlights: List<Highlight>,
    sampleLimit: Int = 3
): BookDetailPreservedMemory {
    val useful = highlights.mapNotNull { highlight ->
        val quote = highlight.quote.trim().takeIf(String::isNotBlank)
        val note = highlight.note.trim().takeIf(String::isNotBlank)
        if (quote == null && note == null) return@mapNotNull null
        PreservedMemoryFragment(
            id = highlight.id,
            quote = quote,
            note = note,
            createdAtEpochMs = highlight.createdAtEpochMs,
            kind = when {
                quote != null && note != null -> PreservedMemoryKind.QUOTE_AND_NOTE
                quote != null -> PreservedMemoryKind.QUOTE_ONLY
                else -> PreservedMemoryKind.NOTE_ONLY
            }
        )
    }.sortedWith(
        compareByDescending<PreservedMemoryFragment> { it.createdAtEpochMs }.thenBy { it.id }
    )
    return BookDetailPreservedMemory(
        fragments = useful.take(sampleLimit.coerceAtLeast(0)),
        totalUseful = useful.size
    )
}

internal data class BookDetailIdentityText(
    val title: String,
    val author: String,
    val seriesName: String?,
    val seriesIndex: Double?,
    val collections: List<String>
)

internal fun bookDetailIdentityText(
    book: Book,
    untitledBook: String,
    unknownAuthor: String
): BookDetailIdentityText {
    val seriesName = book.seriesName?.trim()?.takeIf(String::isNotBlank)
    return BookDetailIdentityText(
        title = book.title.trim().ifBlank { untitledBook },
        author = book.author.trim().ifBlank { unknownAuthor },
        seriesName = seriesName,
        seriesIndex = book.seriesIndex?.takeIf { seriesName != null && it.isFinite() },
        collections = book.allCollections
    )
}
