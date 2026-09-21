package com.veilreader.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

data class MangaWorkWithLinks(
    @androidx.room.Embedded val work: MangaWorkEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "mangaId"
    )
    val sourceLinks: List<MangaSourceLinkEntity>
)

data class MangaOfflineManifestRecord(
    @androidx.room.Embedded val chapter: MangaOfflineChapterEntity,
    @androidx.room.Relation(
        parentColumn = "manifestId",
        entityColumn = "manifestId"
    )
    val pages: List<MangaOfflinePageEntity>
)

@Dao
interface MangaLibraryDao {
    @Transaction
    @Query("SELECT * FROM manga_works WHERE id = :id LIMIT 1")
    suspend fun find(id: String): MangaWorkWithLinks?

    @Transaction
    @Query("SELECT * FROM manga_works ORDER BY createdAtEpochMs ASC, id ASC")
    suspend fun listAll(): List<MangaWorkWithLinks>

    @Query("SELECT EXISTS(SELECT 1 FROM manga_works WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Upsert
    suspend fun upsertWork(work: MangaWorkEntity)

    @Upsert
    suspend fun upsertLinks(links: List<MangaSourceLinkEntity>)

    @Query("DELETE FROM manga_source_links WHERE mangaId = :mangaId")
    suspend fun deleteLinksForManga(mangaId: String)

    @Query("DELETE FROM manga_works WHERE id = :id")
    suspend fun deleteWork(id: String)

    @Query("DELETE FROM manga_works")
    suspend fun deleteAll()
}

@Dao
interface MangaProgressDao {
    @Query("SELECT * FROM manga_progress WHERE mangaId = :mangaId LIMIT 1")
    suspend fun find(mangaId: String): MangaProgressEntity?

    @Query("SELECT * FROM manga_progress ORDER BY updatedAtEpochMs DESC")
    suspend fun listAll(): List<MangaProgressEntity>

    @Upsert
    suspend fun upsert(progress: MangaProgressEntity)

    @Query("DELETE FROM manga_progress WHERE mangaId = :mangaId")
    suspend fun delete(mangaId: String)

    @Query("DELETE FROM manga_progress")
    suspend fun deleteAll()
}

@Dao
interface MangaOfflineDao {
    @Transaction
    @Query("SELECT * FROM manga_offline_chapters WHERE manifestId = :manifestId LIMIT 1")
    suspend fun find(manifestId: String): MangaOfflineManifestRecord?

    @Transaction
    @Query(
        "SELECT * FROM manga_offline_chapters " +
            "WHERE mangaId = :mangaId ORDER BY updatedAtEpochMs DESC, manifestId ASC"
    )
    suspend fun listForManga(mangaId: String): List<MangaOfflineManifestRecord>

    @Upsert
    suspend fun upsertChapter(chapter: MangaOfflineChapterEntity)

    @Upsert
    suspend fun upsertPages(pages: List<MangaOfflinePageEntity>)

    @Query("DELETE FROM manga_offline_pages WHERE manifestId = :manifestId")
    suspend fun deletePages(manifestId: String)

    @Query("DELETE FROM manga_offline_chapters WHERE manifestId = :manifestId")
    suspend fun deleteManifest(manifestId: String)

    @Query("DELETE FROM manga_offline_chapters")
    suspend fun deleteAll()
}
