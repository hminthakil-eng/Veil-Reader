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
    @Query("SELECT * FROM books ORDER BY lastOpenedAtEpochMs DESC, addedAtEpochMs DESC")
    suspend fun listAllWithCollections(): List<BookWithCollections>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun findWithCollections(id: String): BookWithCollections?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun findEntity(id: String): BookEntity?

    @Transaction
    @Query("SELECT * FROM books WHERE contentFingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): BookWithCollections?

    @Upsert suspend fun upsert(book: BookEntity)
    @Upsert suspend fun upsertAll(books: List<BookEntity>)

    /**
     * Hot-path reader persistence. Keep progress writes narrow so locator changes do not upsert
     * immutable book metadata on every page/scroll event.
     */
    @Query(
        """
        UPDATE books
        SET progress = :progress,
            pagesRead = :pagesRead,
            locatorJson = :locatorJson,
            lastOpenedAtEpochMs = :lastOpenedAtEpochMs,
            finished = :finished
        WHERE id = :id
        """
    )
    suspend fun updateProgress(
        id: String,
        progress: Float,
        pagesRead: Int,
        locatorJson: String,
        lastOpenedAtEpochMs: Long,
        finished: Boolean
    ): Int

    @Query("UPDATE books SET lastOpenedAtEpochMs = :openedAtEpochMs WHERE id = :id")
    suspend fun updateLastOpened(id: String, openedAtEpochMs: Long): Int

    @Query("DELETE FROM books WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM books") suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM books") suspend fun count(): Int
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdAtEpochMs DESC")
    suspend fun listAll(): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): HighlightEntity?

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAtEpochMs DESC")
    fun observeForBook(bookId: String): Flow<List<HighlightEntity>>

    @Upsert suspend fun upsert(highlight: HighlightEntity)
    @Upsert suspend fun upsertAll(highlights: List<HighlightEntity>)
    @Query("DELETE FROM highlights WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM highlights") suspend fun deleteAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAtEpochMs DESC")
    suspend fun listAll(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAtEpochMs DESC")
    fun observeForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Upsert suspend fun upsert(bookmark: BookmarkEntity)
    @Upsert suspend fun upsertAll(bookmarks: List<BookmarkEntity>)
    @Query("DELETE FROM bookmarks WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM bookmarks") suspend fun deleteAll()
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
    @Query("DELETE FROM book_collection") suspend fun clearAllLinks()
    @Query("DELETE FROM collections") suspend fun deleteAll()
}

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startedAtEpochMs DESC")
    suspend fun listAll(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAtEpochMs DESC")
    fun observeForBook(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAtEpochMs ASC")
    suspend fun listForBook(bookId: String): List<ReadingSessionEntity>

    @Upsert suspend fun upsert(session: ReadingSessionEntity)
    @Upsert suspend fun upsertAll(sessions: List<ReadingSessionEntity>)
    @Query("DELETE FROM reading_sessions") suspend fun deleteAll()
    @Query("SELECT COALESCE(SUM(activeMillis), 0) FROM reading_sessions")
    suspend fun totalActiveMillis(): Long
}


@Dao
interface ReadingCycleDao {
    @Query("SELECT * FROM reading_cycles ORDER BY completedAtEpochMs DESC, cycleIndex DESC")
    fun observeAll(): Flow<List<ReadingCycleEntity>>

    @Query("SELECT * FROM reading_cycles ORDER BY completedAtEpochMs DESC, cycleIndex DESC")
    suspend fun listAll(): List<ReadingCycleEntity>

    @Query("SELECT * FROM reading_cycles WHERE bookId = :bookId ORDER BY cycleIndex ASC")
    suspend fun listForBook(bookId: String): List<ReadingCycleEntity>

    @Query("SELECT COALESCE(MAX(cycleIndex), 0) FROM reading_cycles WHERE bookId = :bookId")
    suspend fun maxCycleIndex(bookId: String): Int

    @Upsert suspend fun upsert(cycle: ReadingCycleEntity)
    @Upsert suspend fun upsertAll(cycles: List<ReadingCycleEntity>)
    @Query("DELETE FROM reading_cycles") suspend fun deleteAll()
}

@Dao
interface PassageVisitDao {
    @Query("SELECT * FROM passage_visits ORDER BY viewedAtEpochMs DESC")
    fun observeAll(): Flow<List<PassageVisitEntity>>

    @Query("SELECT * FROM passage_visits ORDER BY viewedAtEpochMs DESC")
    suspend fun listAll(): List<PassageVisitEntity>

    @Query("SELECT * FROM passage_visits WHERE highlightId = :highlightId ORDER BY viewedAtEpochMs ASC")
    suspend fun listForHighlight(highlightId: String): List<PassageVisitEntity>

    @Query("SELECT MAX(viewedAtEpochMs) FROM passage_visits WHERE highlightId = :highlightId")
    suspend fun latestViewedAt(highlightId: String): Long?

    @Upsert suspend fun upsert(visit: PassageVisitEntity)
    @Upsert suspend fun upsertAll(visits: List<PassageVisitEntity>)
    @Query("DELETE FROM passage_visits") suspend fun deleteAll()
}
