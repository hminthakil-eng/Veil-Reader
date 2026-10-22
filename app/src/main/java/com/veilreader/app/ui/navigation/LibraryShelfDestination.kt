package com.veilreader.app.ui.navigation

import com.veilreader.app.domain.Book

/** Shared catalog predicates keep Home counts and Library results consistent. */
enum class LibraryShelfDestination(val filterId: String) {
    ALL("All"), READING("Reading"), COMPLETED("Finished"), FAVORITES("Favorites");

    fun contains(book: Book): Boolean = when (this) {
        ALL -> true
        READING -> !book.finished && book.progress > 0f
        COMPLETED -> book.finished
        FAVORITES -> book.favorite
    }
}
