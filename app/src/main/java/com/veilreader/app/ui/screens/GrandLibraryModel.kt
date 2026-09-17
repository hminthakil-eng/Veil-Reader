package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import java.util.Locale

enum class SmartShelfKind {
    CONTINUE_READING,
    RECENTLY_ADDED,
    FINISHED,
    FAVORITES,
    SERIES,
    COLLECTION
}

data class SmartShelf(
    val id: String,
    val title: String,
    val kind: SmartShelfKind,
    val books: List<Book>
)

fun buildSmartShelves(
    books: List<Book>,
    shelfBookLimit: Int = 12
): List<SmartShelf> {
    val limit = shelfBookLimit.coerceAtLeast(0)
    if (limit == 0) return emptyList()

    val imported = books.filter { it.isImported }
    if (imported.isEmpty()) return emptyList()

    val shelves = mutableListOf<SmartShelf>()

    fun addShelf(id: String, title: String, kind: SmartShelfKind, candidates: List<Book>) {
        val visible = candidates.take(limit)
        if (visible.isNotEmpty()) {
            shelves += SmartShelf(id = id, title = title, kind = kind, books = visible)
        }
    }

    addShelf(
        id = "continue-reading",
        title = "Continue Reading",
        kind = SmartShelfKind.CONTINUE_READING,
        candidates = imported
            .filter { !it.finished && it.progress > 0f }
            .sortedByDescending(::bookRecency)
    )

    addShelf(
        id = "recently-added",
        title = "Recently Added",
        kind = SmartShelfKind.RECENTLY_ADDED,
        candidates = imported.sortedByDescending { it.addedAtEpochMs }
    )

    addShelf(
        id = "finished",
        title = "Finished",
        kind = SmartShelfKind.FINISHED,
        candidates = imported.filter { it.finished }.sortedByDescending(::bookRecency)
    )

    addShelf(
        id = "favorites",
        title = "Favorites",
        kind = SmartShelfKind.FAVORITES,
        candidates = imported.filter { it.favorite }.sortedByDescending(::bookRecency)
    )

    addShelf(
        id = "series",
        title = "Series",
        kind = SmartShelfKind.SERIES,
        candidates = imported
            .filter { !it.seriesName.isNullOrBlank() }
            .sortedWith(
                compareBy<Book> { it.seriesName.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.seriesIndex ?: Double.MAX_VALUE }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
    )

    val collectionNames = imported
        .flatMap { it.allCollections }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .groupBy { it.lowercase(Locale.ROOT) }
        .mapValues { (_, names) -> names.first() }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    collectionNames.forEach { (normalized, displayName) ->
        addShelf(
            id = "collection:$normalized",
            title = displayName,
            kind = SmartShelfKind.COLLECTION,
            candidates = imported
                .filter { book -> book.allCollections.any { it.equals(displayName, ignoreCase = true) } }
                .sortedByDescending(::bookRecency)
        )
    }

    return shelves
}

private fun bookRecency(book: Book): Long =
    book.lastOpenedAtEpochMs.takeIf { it > 0L } ?: book.addedAtEpochMs
