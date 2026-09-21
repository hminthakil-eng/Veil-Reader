package com.veilreader.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaSourceBindingDao {
    @Query(
        """
        SELECT * FROM manga_source_bindings
        WHERE bookId = :bookId
        ORDER BY isPreferred DESC, providerId, sourceId
        """
    )
    fun observeForBook(bookId: String): Flow<List<MangaSourceBindingEntity>>

    @Query(
        """
        SELECT * FROM manga_source_bindings
        WHERE bookId = :bookId
        ORDER BY isPreferred DESC, providerId, sourceId
        """
    )
    suspend fun listForBook(bookId: String): List<MangaSourceBindingEntity>

    @Query(
        """
        SELECT * FROM manga_source_bindings
        WHERE sourceId = :sourceId AND sourceKey = :sourceKey
        LIMIT 1
        """
    )
    suspend fun findByExternalRef(
        sourceId: String,
        sourceKey: String
    ): MangaSourceBindingEntity?

    @Upsert
    suspend fun upsert(binding: MangaSourceBindingEntity)

    @Upsert
    suspend fun upsertAll(bindings: List<MangaSourceBindingEntity>)

    @Query("SELECT * FROM manga_source_bindings ORDER BY bookId, isPreferred DESC, sourceId")
    suspend fun listAll(): List<MangaSourceBindingEntity>

    @Query("DELETE FROM manga_source_bindings")
    suspend fun deleteAll()

    @Query("UPDATE manga_source_bindings SET isPreferred = 0 WHERE bookId = :bookId")
    suspend fun clearPreferred(bookId: String)

    @Query(
        """
        UPDATE manga_source_bindings
        SET isPreferred = 1
        WHERE bookId = :bookId AND sourceId = :sourceId
        """
    )
    suspend fun markPreferred(bookId: String, sourceId: String): Int

    @Transaction
    suspend fun setPreferred(bookId: String, sourceId: String) {
        clearPreferred(bookId)
        check(markPreferred(bookId, sourceId) == 1) {
            "Cannot prefer a manga source binding that does not exist."
        }
    }

    @Query("DELETE FROM manga_source_bindings WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface MangaChapterDao {
    @Query(
        """
        SELECT * FROM manga_chapters
        WHERE bookId = :bookId
        ORDER BY displayOrder ASC, id ASC
        """
    )
    fun observeForBook(bookId: String): Flow<List<MangaChapterEntity>>

    @Query(
        """
        SELECT * FROM manga_chapters
        WHERE bookId = :bookId
        ORDER BY displayOrder ASC, id ASC
        """
    )
    suspend fun listForBook(bookId: String): List<MangaChapterEntity>

    @Query("SELECT * FROM manga_chapters WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): MangaChapterEntity?

    @Upsert
    suspend fun upsert(chapter: MangaChapterEntity)

    @Upsert
    suspend fun upsertAll(chapters: List<MangaChapterEntity>)

    @Query("SELECT * FROM manga_chapters ORDER BY bookId, displayOrder")
    suspend fun listAll(): List<MangaChapterEntity>

    @Query("DELETE FROM manga_chapters")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE manga_chapters
        SET lastPageIndex = :lastPageIndex,
            pageCount = :pageCount,
            read = :read,
            lastReadAtEpochMs = :lastReadAtEpochMs
        WHERE id = :chapterId
        """
    )
    suspend fun updateReadProgress(
        chapterId: String,
        lastPageIndex: Int,
        pageCount: Int,
        read: Boolean,
        lastReadAtEpochMs: Long
    ): Int

    @Query("DELETE FROM manga_chapters WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface MangaChapterBindingDao {
    @Query(
        """
        SELECT * FROM manga_chapter_bindings
        WHERE chapterId = :chapterId
        ORDER BY sourceId
        """
    )
    suspend fun listForChapter(chapterId: String): List<MangaChapterBindingEntity>

    @Query(
        """
        SELECT * FROM manga_chapter_bindings
        WHERE sourceId = :sourceId
          AND mangaSourceKey = :mangaSourceKey
          AND sourceChapterKey = :sourceChapterKey
        LIMIT 1
        """
    )
    suspend fun findByExternalRef(
        sourceId: String,
        mangaSourceKey: String,
        sourceChapterKey: String
    ): MangaChapterBindingEntity?

    @Upsert
    suspend fun upsert(binding: MangaChapterBindingEntity)

    @Upsert
    suspend fun upsertAll(bindings: List<MangaChapterBindingEntity>)

    @Query("SELECT * FROM manga_chapter_bindings ORDER BY chapterId, sourceId")
    suspend fun listAll(): List<MangaChapterBindingEntity>

    @Query("DELETE FROM manga_chapter_bindings")
    suspend fun deleteAll()
}

@Dao
interface MangaDownloadDao {
    @Query(
        """
        SELECT * FROM manga_downloads
        ORDER BY updatedAtEpochMs DESC
        """
    )
    fun observeAll(): Flow<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE chapterId = :chapterId LIMIT 1")
    suspend fun find(chapterId: String): MangaDownloadEntity?

    @Query(
        """
        SELECT * FROM manga_downloads
        WHERE state IN ('QUEUED', 'RUNNING', 'PAUSED')
        ORDER BY updatedAtEpochMs ASC
        """
    )
    suspend fun listPending(): List<MangaDownloadEntity>

    @Upsert
    suspend fun upsert(download: MangaDownloadEntity)

    @Query(
        """
        UPDATE manga_downloads
        SET state = :state,
            downloadedPages = :downloadedPages,
            totalPages = :totalPages,
            downloadedBytes = :downloadedBytes,
            totalBytes = :totalBytes,
            rootPath = :rootPath,
            failureCode = :failureCode,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE chapterId = :chapterId
        """
    )
    suspend fun updateState(
        chapterId: String,
        state: String,
        downloadedPages: Int,
        totalPages: Int,
        downloadedBytes: Long,
        totalBytes: Long?,
        rootPath: String?,
        failureCode: String?,
        updatedAtEpochMs: Long
    ): Int

    @Query("DELETE FROM manga_downloads WHERE chapterId = :chapterId")
    suspend fun delete(chapterId: String)

    @Query("DELETE FROM manga_downloads")
    suspend fun deleteAll()
}
