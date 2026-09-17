package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book

internal fun selectCurrentBook(books: List<Book>): Book? =
    books
        .filterNot { it.finished }
        .ifEmpty { books }
        .maxByOrNull { it.readingActivityEpochMs() }

internal fun recentBooks(
    books: List<Book>,
    excludingBookId: String?,
    limit: Int = 6
): List<Book> {
    if (limit <= 0) return emptyList()

    return books
        .asSequence()
        .filterNot { it.id == excludingBookId }
        .sortedByDescending { it.readingActivityEpochMs() }
        .take(limit)
        .toList()
}

private fun Book.readingActivityEpochMs(): Long =
    lastOpenedAtEpochMs.takeIf { it > 0L } ?: addedAtEpochMs
