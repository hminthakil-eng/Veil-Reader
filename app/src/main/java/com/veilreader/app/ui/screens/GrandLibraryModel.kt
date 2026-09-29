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
    val kind: SmartShelfKind,
    val books: List<Book>,
    /** User-authored collection name; standard shelf labels belong to the UI locale layer. */
    val collectionName: String? = null
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

    fun addShelf(id: String, kind: SmartShelfKind, candidates: List<Book>, collectionName: String? = null) {
        val visible = candidates.take(limit)
        if (visible.isNotEmpty()) {
            shelves += SmartShelf(id = id, kind = kind, books = visible, collectionName = collectionName)
        }
    }

    addShelf(
        id = "continue-reading",
        kind = SmartShelfKind.CONTINUE_READING,
        candidates = imported
            .filter { !it.finished && it.progress > 0f }
            .sortedWith(recencyComparator())
    )

    addShelf(
        id = "recently-added",
        kind = SmartShelfKind.RECENTLY_ADDED,
        candidates = imported.sortedWith(
            compareByDescending<Book> { it.addedAtEpochMs }
                .thenBy { it.id }
        )
    )

    addShelf(
        id = "finished",
        kind = SmartShelfKind.FINISHED,
        candidates = imported.filter { it.finished }.sortedWith(recencyComparator())
    )

    addShelf(
        id = "favorites",
        kind = SmartShelfKind.FAVORITES,
        candidates = imported.filter { it.favorite }.sortedWith(recencyComparator())
    )

    addShelf(
        id = "series",
        kind = SmartShelfKind.SERIES,
        candidates = imported
            .filter { !it.seriesName.isNullOrBlank() }
            .sortedWith(
                compareBy<Book> { it.seriesName.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.seriesIndex ?: Double.MAX_VALUE }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
                    .thenBy { it.id }
            )
    )

    val collectionNames = imported
        .flatMap { it.allCollections }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .groupBy { it.lowercase(Locale.ROOT) }
        .mapValues { (_, names) ->
            names.sortedWith(
                compareBy<String> { it.lowercase(Locale.ROOT) }
                    .thenBy { it }
            ).first()
        }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    collectionNames.forEach { (normalized, displayName) ->
        addShelf(
            id = "collection:$normalized",
            kind = SmartShelfKind.COLLECTION,
            collectionName = displayName,
            candidates = imported
                .filter { book ->
                    book.allCollections.any { collection ->
                        collection.trim().lowercase(Locale.ROOT) == normalized
                    }
                }
                .sortedWith(recencyComparator())
        )
    }

    return shelves
}

private fun bookRecency(book: Book): Long =
    book.lastOpenedAtEpochMs.takeIf { it > 0L } ?: book.addedAtEpochMs

private fun recencyComparator(): Comparator<Book> =
    compareByDescending<Book> { bookRecency(it) }
        .thenBy { it.id }
