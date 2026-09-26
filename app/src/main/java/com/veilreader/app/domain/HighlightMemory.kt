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
    val ageLabel: String,
    val echoDepth: EchoDepth,
    val echoLabel: String?,
    val bookActivityAfterMark: Boolean,
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
    nowEpochMs: Long = System.currentTimeMillis()
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

    val ageLabel = when {
        !ageKnown -> "MARK DATE UNKNOWN"
        ageDays == 0 -> "MARKED TODAY"
        ageDays == 1 -> "MARKED YESTERDAY"
        ageDays < 60 -> "MARKED $ageDays DAYS AGO"
        ageDays < 730 -> {
            val months = (ageDays / 30).coerceAtLeast(2)
            "MARKED $months MONTHS AGO"
        }
        else -> {
            val years = (ageDays / 365).coerceAtLeast(2)
            "MARKED $years YEARS AGO"
        }
    }

    val echoLabel = when {
        depth == EchoDepth.FRESH -> null
        ageDays < 60 -> "AN ECHO FROM $ageDays DAYS AGO"
        ageDays < 730 -> {
            val months = (ageDays / 30).coerceAtLeast(2)
            "AN ECHO FROM $months MONTHS AGO"
        }
        else -> {
            val years = (ageDays / 365).coerceAtLeast(2)
            "AN ECHO FROM $years YEARS AGO"
        }
    }

    val laterActivity = book?.lastOpenedAtEpochMs?.let { lastOpened ->
        ageKnown &&
            lastOpened > created &&
            lastOpened - created >= CONTINUED_ACTIVITY_GAP_MS
    } ?: false

    val annotated = highlight.note.isNotBlank()
    val eligible = depth != EchoDepth.FRESH

    // A deterministic resurfacing priority, not a claim about the passage's importance.
    val resonance = if (!eligible) {
        0
    } else {
        ageDays.coerceAtMost(365) +
            if (annotated) 80 else 0 +
            if (book?.finished == true) 45 else 0 +
            if (book?.favorite == true) 25 else 0 +
            if (laterActivity) 20 else 0
    }

    return HighlightMemory(
        ageKnown = ageKnown,
        ageDays = ageDays,
        ageLabel = ageLabel,
        echoDepth = depth,
        echoLabel = echoLabel,
        bookActivityAfterMark = laterActivity,
        annotated = annotated,
        eligibleForEcho = eligible,
        resonanceScore = resonance
    )
}

fun deriveArchiveEchoes(
    highlights: List<Highlight>,
    booksById: Map<String, Book>,
    nowEpochMs: Long = System.currentTimeMillis(),
    limit: Int = 24
): List<ArchiveEcho> {
    if (limit <= 0) return emptyList()

    return highlights
        .mapNotNull { highlight ->
            val book = booksById[highlight.bookId] ?: return@mapNotNull null
            val memory = deriveHighlightMemory(highlight, book, nowEpochMs)
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
