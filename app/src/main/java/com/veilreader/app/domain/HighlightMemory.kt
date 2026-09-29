package com.veilreader.app.domain

enum class EchoDepth {
    FRESH,
    TRACE,
    ECHO,
    DEEP_ECHO
}

data class HighlightMemory(
    val ageKnown: Boolean,
    val ageDays: Int,
    val echoDepth: EchoDepth,
    val bookActivityAfterMark: Boolean,
    val revisitCount: Int,
    val lastViewedAtEpochMs: Long?,
    val annotated: Boolean,
    val eligibleForEcho: Boolean,
    val resonanceScore: Int
)

data class ArchiveEcho(
    val highlight: Highlight,
    val book: Book,
    val memory: HighlightMemory
)

private const val DAY_MS = 86_400_000L
private const val CONTINUED_ACTIVITY_GAP_MS = 12L * 60L * 60L * 1000L

/**
 * Derives a margin memory strictly from timestamps and state Veil already owns.
 *
 * "Book activity after mark" deliberately means only that the volume recorded later reading
 * activity. It never claims that the exact passage itself was revisited.
 */
fun deriveHighlightMemory(
    highlight: Highlight,
    book: Book?,
    nowEpochMs: Long = System.currentTimeMillis(),
    passageVisits: List<PassageVisit> = emptyList()
): HighlightMemory {
    val created = highlight.createdAtEpochMs
    val safeNow = nowEpochMs.coerceAtLeast(0L)
    val ageKnown = created > 0L && safeNow >= created
    val ageDays = if (ageKnown) {
        ((safeNow - created) / DAY_MS).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    } else {
        0
    }

    val depth = when {
        !ageKnown || ageDays < 14 -> EchoDepth.FRESH
        ageDays < 30 -> EchoDepth.TRACE
        ageDays < 180 -> EchoDepth.ECHO
        else -> EchoDepth.DEEP_ECHO
    }

    val laterActivity = book?.lastOpenedAtEpochMs?.let { lastOpened ->
        ageKnown &&
            lastOpened > created &&
            lastOpened - created >= CONTINUED_ACTIVITY_GAP_MS
    } ?: false

    val exactVisits = exactPassageVisits(highlight, passageVisits)
    val lastViewedAt = exactVisits.lastOrNull()?.viewedAtEpochMs
    val annotated = highlight.note.isNotBlank()
    val eligible = depth != EchoDepth.FRESH

    // A deterministic resurfacing priority, not a claim about the passage's importance.
    // Keep each bonus explicit so Kotlin's if-expression grammar cannot accidentally absorb
    // a following addition into an else branch.
    val annotationBonus = if (annotated) 80 else 0
    val completionBonus = if (book?.finished == true) 45 else 0
    val favoriteBonus = if (book?.favorite == true) 25 else 0
    val laterActivityBonus = if (laterActivity) 20 else 0
    // Exact passage returns are a stronger factual signal than generic later book activity,
    // but stay bounded so age and annotation history remain the dominant archival signals.
    val revisitBonus = (exactVisits.size * 15).coerceAtMost(60)
    val resonance = if (!eligible) {
        0
    } else {
        ageDays.coerceAtMost(365) +
            annotationBonus +
            completionBonus +
            favoriteBonus +
            laterActivityBonus +
            revisitBonus
    }

    return HighlightMemory(
        ageKnown = ageKnown,
        ageDays = ageDays,
        echoDepth = depth,
        bookActivityAfterMark = laterActivity,
        revisitCount = exactVisits.size,
        lastViewedAtEpochMs = lastViewedAt,
        annotated = annotated,
        eligibleForEcho = eligible,
        resonanceScore = resonance
    )
}

fun deriveArchiveEchoes(
    highlights: List<Highlight>,
    booksById: Map<String, Book>,
    nowEpochMs: Long = System.currentTimeMillis(),
    limit: Int = 24,
    passageVisits: List<PassageVisit> = emptyList()
): List<ArchiveEcho> {
    if (limit <= 0) return emptyList()

    return highlights
        .mapNotNull { highlight ->
            val book = booksById[highlight.bookId] ?: return@mapNotNull null
            val memory = deriveHighlightMemory(
                highlight = highlight,
                book = book,
                nowEpochMs = nowEpochMs,
                passageVisits = passageVisits
            )
            if (!memory.eligibleForEcho) return@mapNotNull null
            ArchiveEcho(highlight, book, memory)
        }
        .sortedWith(
            compareByDescending<ArchiveEcho> { it.memory.resonanceScore }
                .thenBy { it.highlight.createdAtEpochMs }
                .thenBy { it.highlight.id }
        )
        .take(limit)
}
