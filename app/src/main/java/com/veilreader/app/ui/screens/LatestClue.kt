package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight

internal data class LatestClue(val book: Book, val highlight: Highlight)

/** Saved text only; no publication reads, generated context or reading-progress inference. */
internal fun deriveLatestClue(books: List<Book>, highlights: List<Highlight>): LatestClue? {
    val availableBooks = books.filter { it.id.isNotBlank() && !it.sourceUri.isNullOrBlank() }
        .associateBy { it.id }
    val highlight = highlights.asSequence()
        .filter { it.bookId.isNotBlank() && it.bookId in availableBooks &&
            it.quote.isNotBlank() && it.locatorJson.isNotBlank() }
        .sortedWith(compareByDescending<Highlight> { it.createdAtEpochMs }
            .thenBy { it.id }.thenBy { it.bookId }.thenBy { it.locatorJson }.thenBy { it.quote })
        .firstOrNull() ?: return null
    return LatestClue(availableBooks.getValue(highlight.bookId), highlight)
}
