package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import java.util.Locale

internal enum class LibraryViewMode { GALLERY, SHELVES, INDEX }

internal fun libraryViewModeFromStored(value: String): LibraryViewMode =
    when (value) {
        "GRID" -> LibraryViewMode.GALLERY
        "LIST" -> LibraryViewMode.INDEX
        else -> runCatching { LibraryViewMode.valueOf(value) }
            .getOrDefault(LibraryViewMode.GALLERY)
    }

internal data class LibraryShelfGroup(
    val eyebrow: String,
    val title: String,
    val books: List<Book>
)

internal data class LibraryShelfLabels(
    val filteredArchive: String,
    val matchingVolumes: String,
    val journey: String,
    val currentlyReading: String,
    val collection: String,
    val series: String,
    val author: String,
    val record: String,
    val completedVolumes: String,
    val unopened: String,
    val waitingOnShelf: String
)

internal data class LibraryNamedBookGroup(
    val name: String,
    val books: List<Book>
)

internal fun groupLibraryBooksByLabel(
    entries: List<Pair<String, Book>>
): List<LibraryNamedBookGroup> =
    entries
        .mapNotNull { (rawName, book) ->
            rawName.trim().takeIf { it.isNotEmpty() }?.let { it to book }
        }
        .groupBy { (name, _) -> name.lowercase(Locale.ROOT) }
        .map { (_, taggedBooks) ->
            val displayName = taggedBooks
                .map { it.first }
                .distinct()
                .sortedWith(compareBy<String> { it.lowercase(Locale.ROOT) }.thenBy { it })
                .first()
            LibraryNamedBookGroup(
                name = displayName,
                books = taggedBooks.map { it.second }.distinctBy { it.id }
            )
        }

internal fun deriveLibraryShelfGroups(
    books: List<Book>,
    filtered: List<Book>,
    filterActive: Boolean,
    labels: LibraryShelfLabels
): List<LibraryShelfGroup> {
    if (filterActive) {
        return listOf(
            LibraryShelfGroup(
                eyebrow = labels.filteredArchive,
                title = labels.matchingVolumes,
                books = filtered
            )
        ).filter { it.books.isNotEmpty() }
    }

    val groups = mutableListOf<LibraryShelfGroup>()

    books
        .filter { !it.finished && it.progress > 0f }
        .sortedByDescending { it.lastOpenedAtEpochMs }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup(labels.journey, labels.currentlyReading, it) }

    groupLibraryBooksByLabel(
        books.flatMap { book -> book.allCollections.map { it to book } }
    )
        .sortedWith(
            compareByDescending<LibraryNamedBookGroup> { it.books.size }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .take(6)
        .forEach { group ->
            groups += LibraryShelfGroup(
                eyebrow = labels.collection,
                title = group.name,
                books = group.books
            )
        }

    groupLibraryBooksByLabel(
        books.mapNotNull { book -> book.seriesName?.let { it to book } }
    )
        .sortedWith(
            compareByDescending<LibraryNamedBookGroup> { it.books.size }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .take(6)
        .forEach { group ->
            groups += LibraryShelfGroup(
                eyebrow = labels.series,
                title = group.name,
                books = group.books.sortedWith(
                    compareBy<Book> { it.seriesIndex ?: Double.MAX_VALUE }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                        .thenBy { it.id }
                )
            )
        }

    groupLibraryBooksByLabel(
        books.map { it.author to it }
    )
        .filter { it.books.size >= 2 }
        .sortedWith(
            compareByDescending<LibraryNamedBookGroup> { it.books.size }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .take(4)
        .forEach { group ->
            groups += LibraryShelfGroup(
                eyebrow = labels.author,
                title = group.name,
                books = group.books
            )
        }

    books
        .filter { it.finished }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup(labels.record, labels.completedVolumes, it) }

    books
        .filter { !it.finished && it.progress <= 0f }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup(labels.unopened, labels.waitingOnShelf, it) }

    return groups
}
