package com.veilreader.app.data

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import kotlinx.coroutines.flow.Flow

/**
 * Durable library boundary for the 1.0 architecture.
 *
 * The current SharedPreferences repository remains the runtime implementation during migration.
 * Phase 1B will provide the Room implementation and move UI state holders to this suspend/Flow API.
 */
interface LibraryRepository {
    val books: Flow<List<Book>>
    val highlights: Flow<List<Highlight>>
    val bookmarks: Flow<List<Bookmark>>

    suspend fun getBook(id: String): Book?
    suspend fun upsertImportedBook(book: Book)
    suspend fun markOpened(id: String, openedAtEpochMs: Long = System.currentTimeMillis())

    /** Returns true only when the book becomes finished for the first time. */
    suspend fun saveProgress(id: String, progression: Double, locatorJson: String): Boolean

    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun updateMetadata(id: String, title: String, author: String)
    suspend fun setCollections(bookId: String, collectionNames: Set<String>)

    suspend fun addHighlight(bookId: String, quote: String, locatorJson: String): Highlight
    suspend fun updateHighlightNote(id: String, note: String)
    suspend fun deleteHighlight(id: String)

    suspend fun addBookmark(bookId: String, label: String, locatorJson: String): Boolean
    suspend fun deleteBookmark(id: String)

    /** Removes library metadata. File deletion policy is handled explicitly by the caller/use case. */
    suspend fun removeBook(id: String)
}
