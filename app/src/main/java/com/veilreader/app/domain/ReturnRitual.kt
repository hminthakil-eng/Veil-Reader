package com.veilreader.app.domain

private const val RITUAL_DAY_MS = 86_400_000L
private const val FORGOTTEN_RITUAL_AFTER_MS = 180L * RITUAL_DAY_MS
private const val OLD_MARGIN_FRAGMENT_AFTER_MS = 90L * RITUAL_DAY_MS

enum class ReturnRitualKind {
    FORGOTTEN_VOLUME
}

data class ReturnRitualFragment(
    val highlightId: String,
    val quote: String?,
    val ageDays: Long,
    val annotated: Boolean
)

data class BookReturnRitual(
    val bookId: String,
    val kind: ReturnRitualKind,
    val silenceMillis: Long,
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
                    .takeIf { it.isNotBlank() },
                ageDays = (safeNow - highlight.createdAtEpochMs) / RITUAL_DAY_MS,
                annotated = highlight.note.isNotBlank()
            )
        }
        .firstOrNull()

    return BookReturnRitual(
        bookId = book.id,
        kind = ReturnRitualKind.FORGOTTEN_VOLUME,
        silenceMillis = memory.inactiveMillis,
        fragment = fragment
    )
}
