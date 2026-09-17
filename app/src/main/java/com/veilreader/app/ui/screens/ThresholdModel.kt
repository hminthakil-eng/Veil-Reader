package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book

/** Stable, UI-independent model for the Reading Now / Threshold surface. */
data class ThresholdSnapshot(
    val hero: Book?,
    val recent: List<Book>
)

fun buildThresholdSnapshot(
    books: List<Book>,
    recentLimit: Int = 5
): ThresholdSnapshot {
    if (books.isEmpty()) return ThresholdSnapshot(hero = null, recent = emptyList())

    val ordered = books.sortedByDescending(::bookRecency)
    val hero = ordered.firstOrNull { !it.finished } ?: ordered.first()
    val recent = ordered
        .asSequence()
        .filterNot { it.id == hero.id }
        .take(recentLimit.coerceAtLeast(0))
        .toList()

    return ThresholdSnapshot(hero = hero, recent = recent)
}

private fun bookRecency(book: Book): Long =
    book.lastOpenedAtEpochMs.takeIf { it > 0L } ?: book.addedAtEpochMs
