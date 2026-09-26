package com.veilreader.app.domain

private const val RITUAL_DAY_MS = 86_400_000L
private const val FORGOTTEN_RITUAL_AFTER_MS = 180L * RITUAL_DAY_MS
private const val OLD_MARGIN_FRAGMENT_AFTER_MS = 90L * RITUAL_DAY_MS

enum class ReturnRitualKind {
    FORGOTTEN_VOLUME
}

data class ReturnRitualFragment(
    val highlightId: String,
    val quote: String,
    val ageDays: Long,
    val annotated: Boolean
)

data class BookReturnRitual(
    val bookId: String,
    val kind: ReturnRitualKind,
    val silenceMillis: Long,
    val silenceLabel: String,
    val title: String,
    val invocation: String,
    val fragment: ReturnRitualFragment?
)

/**
 * Creates a rare entry ritual only when current archive depth proves the volume has been silent
 * for at least six months. A preserved-margin fragment is optional and requires its own old,
 * persisted highlight timestamp. No return event or passage revisit is invented here.
 */
fun deriveBookReturnRitual(
    book: Book,
    archiveMemory: BookArchiveMemory?,
    highlights: List<Highlight>,
    nowEpochMs: Long = System.currentTimeMillis()
): BookReturnRitual? {
    val memory = archiveMemory ?: return null
    if (
        memory.bookId != book.id ||
        memory.depth != ArchiveDepth.FORGOTTEN ||
        memory.inactiveMillis < FORGOTTEN_RITUAL_AFTER_MS
    ) {
        return null
    }

    val safeNow = nowEpochMs.coerceAtLeast(0L)
    val fragment = highlights
        .asSequence()
        .filter { highlight ->
            highlight.bookId == book.id &&
                highlight.createdAtEpochMs > 0L &&
                safeNow >= highlight.createdAtEpochMs &&
                safeNow - highlight.createdAtEpochMs >= OLD_MARGIN_FRAGMENT_AFTER_MS
        }
        .sortedWith(
            compareByDescending<Highlight> { it.createdAtEpochMs }
                .thenBy { it.id }
        )
        .map { highlight ->
            ReturnRitualFragment(
                highlightId = highlight.id,
                quote = highlight.quote
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(180)
                    .ifBlank { "A preserved passage" },
                ageDays = (safeNow - highlight.createdAtEpochMs) / RITUAL_DAY_MS,
                annotated = highlight.note.isNotBlank()
            )
        }
        .firstOrNull()

    return BookReturnRitual(
        bookId = book.id,
        kind = ReturnRitualKind.FORGOTTEN_VOLUME,
        silenceMillis = memory.inactiveMillis,
        silenceLabel = returnRitualGapLabel(memory.inactiveMillis),
        title = "The Forgotten Volume",
        invocation = "A sealed volume has returned from the Deep Shelf.",
        fragment = fragment
    )
}

fun returnRitualGapLabel(gapMillis: Long): String {
    val days = gapMillis.coerceAtLeast(0L) / RITUAL_DAY_MS
    return when {
        days >= 730L -> {
            val years = days / 365L
            val months = (days % 365L) / 30L
            if (months > 0L) "${years}Y ${months}MO SILENT" else "${years}Y SILENT"
        }
        days >= 365L -> {
            val months = (days % 365L) / 30L
            if (months > 0L) "1Y ${months}MO SILENT" else "1Y SILENT"
        }
        else -> "${(days / 30L).coerceAtLeast(6L)} MONTHS SILENT"
    }
}

fun returnRitualFragmentAgeLabel(fragment: ReturnRitualFragment): String = when {
    fragment.ageDays >= 730L -> "${fragment.ageDays / 365L} YEARS PRESERVED"
    fragment.ageDays >= 365L -> "1 YEAR PRESERVED"
    fragment.ageDays >= 60L -> "${fragment.ageDays / 30L} MONTHS PRESERVED"
    else -> "${fragment.ageDays} DAYS PRESERVED"
}
