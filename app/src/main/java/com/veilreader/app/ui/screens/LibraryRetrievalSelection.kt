package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import java.util.Locale

internal enum class LibrarySortMode {
    AUTO, RELEVANCE, RECENT, TITLE, AUTHOR, SERIES, PROGRESS, ARCHIVE_DEPTH
}

/** Legacy Recent represented both default and explicit choice; retain its contextual behavior. */
internal fun restoreLibrarySort(value: String): LibrarySortMode = when (value) {
    "Recent" -> LibrarySortMode.AUTO
    "Title" -> LibrarySortMode.TITLE
    "Author" -> LibrarySortMode.AUTHOR
    "Series" -> LibrarySortMode.SERIES
    "Progress" -> LibrarySortMode.PROGRESS
    "Archive Depth" -> LibrarySortMode.ARCHIVE_DEPTH
    else -> LibrarySortMode.entries.firstOrNull { it.name == value } ?: LibrarySortMode.AUTO
}

internal fun effectiveLibrarySort(mode: LibrarySortMode, hasSearch: Boolean): LibrarySortMode =
    when (mode) {
        LibrarySortMode.AUTO, LibrarySortMode.RELEVANCE ->
            if (hasSearch) LibrarySortMode.RELEVANCE else LibrarySortMode.RECENT
        else -> mode
    }

/** Uses existing primary orderings, with identity as the final deterministic tie-break. */
internal fun sortLibraryBooks(
    books: List<Book>,
    mode: LibrarySortMode,
    searchScores: Map<String, Int> = emptyMap(),
    archiveDepthFor: (String) -> Long = { 0L }
): List<Book> {
    val comparator = when (mode) {
        LibrarySortMode.TITLE -> compareBy<Book> { it.title.lowercase(Locale.ROOT) }
        LibrarySortMode.AUTHOR -> compareBy<Book> { it.author.lowercase(Locale.ROOT) }
        LibrarySortMode.PROGRESS -> compareByDescending<Book> { it.progress }
        LibrarySortMode.ARCHIVE_DEPTH -> compareByDescending<Book> { archiveDepthFor(it.id) }
        LibrarySortMode.SERIES -> compareBy<Book> { it.seriesName?.lowercase(Locale.ROOT) ?: "\uffff" }
            .thenBy { it.seriesIndex ?: Double.MAX_VALUE }
            .thenBy { it.title.lowercase(Locale.ROOT) }
        LibrarySortMode.RELEVANCE -> compareByDescending<Book> { searchScores[it.id] ?: Int.MIN_VALUE }
            .thenByDescending { maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs) }
        LibrarySortMode.AUTO, LibrarySortMode.RECENT ->
            compareByDescending<Book> { maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs) }
    }
    return books.sortedWith(comparator.thenBy { it.id })
}

internal fun libraryLabelKey(value: String): String = value.trim().lowercase(Locale.ROOT)

/** Available labels must come from all canonical books, not a query or truncated wing preview. */
internal fun reconcileLibraryLabel(
    selected: String,
    available: List<String>,
    allowRemoval: Boolean = true
): String {
    if (selected.isBlank()) return ""
    val key = libraryLabelKey(selected)
    return available.firstOrNull { libraryLabelKey(it) == key }?.trim()
        ?: if (allowRemoval) "" else selected
}

internal fun libraryLabelMatches(value: String?, selected: String): Boolean =
    selected.isBlank() || (value != null && libraryLabelKey(value) == libraryLabelKey(selected))
