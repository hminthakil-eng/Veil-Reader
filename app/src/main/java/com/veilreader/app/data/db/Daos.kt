package com.veilreader.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Transaction
    @Query("SELECT * FROM books ORDER BY lastOpenedAtEpochMs DESC, addedAtEpochMs DESC")
    fun observeAll(): Flow<List<BookWithCollections>>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun findWithCollections(id: String): BookWithCollections?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun findEntity(id: String): BookEntity?

    @Upsert suspend fun upsert(book: BookEntity)
    @Upsert suspend fun upsertAll(books: List<BookEntity>)
    @Query("DELETE FROM books WHERE id = :id") suspend fun deleteById(id: String)
    @Query("SELECT COUNT(*) FROM books") suspend fun count(): Int
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAtEpochMs DESC")
    fun observeForBook(bookId: String): Flow<List<HighlightEntity>>

    @Upsert suspend fun upsert(highlight: HighlightEntity)
    @Upsert suspend fun upsertAll(highlights: List<HighlightEntity>)
    @Query("DELETE FROM highlights WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAtEpochMs DESC")
    fun observeForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Upsert suspend fun upsert(bookmark: BookmarkEntity)
    @Upsert suspend fun upsertAll(bookmarks: List<BookmarkEntity>)
    @Query("DELETE FROM bookmarks WHERE id = :id") suspend fun deleteById(id: String)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): CollectionEntity?

    @Upsert suspend fun upsert(collection: CollectionEntity)
    @Upsert suspend fun upsertAll(collections: List<CollectionEntity>)
    @Upsert suspend fun attach(crossRef: BookCollectionCrossRef)
    @Upsert suspend fun attachAll(crossRefs: List<BookCollectionCrossRef>)
    @Query("DELETE FROM book_collection WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun detach(bookId: String, collectionId: String)
    @Query("DELETE FROM book_collection WHERE bookId = :bookId") suspend fun clearBook(bookId: String)
}

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAtEpochMs DESC")
    fun observeForBook(bookId: String): Flow<List<ReadingSessionEntity>>

    @Upsert suspend fun upsert(session: ReadingSessionEntity)
    @Query("SELECT COALESCE(SUM(activeMillis), 0) FROM reading_sessions")
    suspend fun totalActiveMillis(): Long
}
