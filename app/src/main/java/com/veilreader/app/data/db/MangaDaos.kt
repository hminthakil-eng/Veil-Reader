package com.veilreader.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class MangaOfflineChapterWithPages(
    @Embedded val chapter: MangaOfflineChapterEntity,
    @Relation(
        parentColumn = "chapterId",
        entityColumn = "chapterId"
    )
    val pages: List<MangaOfflinePageEntity>
)

@Dao
interface MangaCatalogDao {
    @Query("SELECT * FROM manga_chapters WHERE bookId = :bookId ORDER BY readingOrder ASC")
    fun observeChapters(bookId: String): Flow<List<MangaChapterEntity>>

    @Query("SELECT * FROM manga_chapters WHERE bookId = :bookId ORDER BY readingOrder ASC")
    suspend fun listChapters(bookId: String): List<MangaChapterEntity>

    @Query("SELECT * FROM manga_chapters WHERE id = :chapterId LIMIT 1")
    suspend fun findChapter(chapterId: String): MangaChapterEntity?

    @Query("SELECT * FROM manga_chapters WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun findChapterByCacheKey(cacheKey: String): MangaChapterEntity?

    @Upsert
    suspend fun upsertChapter(chapter: MangaChapterEntity)

    @Upsert
    suspend fun upsertChapters(chapters: List<MangaChapterEntity>)

    @Query("DELETE FROM manga_chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: String)

    @Query("SELECT * FROM manga_source_links WHERE bookId = :bookId ORDER BY sourceId ASC")
    suspend fun listSourceLinks(bookId: String): List<MangaSourceLinkEntity>

    @Upsert
    suspend fun upsertSourceLink(link: MangaSourceLinkEntity)

    @Upsert
    suspend fun upsertSourceLinks(links: List<MangaSourceLinkEntity>)

    @Query("DELETE FROM manga_source_links WHERE bookId = :bookId")
    suspend fun deleteSourceLinksForBook(bookId: String)

    @Query(
        "SELECT * FROM manga_chapter_sources " +
            "WHERE chapterId = :chapterId ORDER BY sourceId ASC"
    )
    suspend fun listChapterSources(chapterId: String): List<MangaChapterSourceEntity>

    @Upsert
    suspend fun upsertChapterSource(source: MangaChapterSourceEntity)

    @Upsert
    suspend fun upsertChapterSources(sources: List<MangaChapterSourceEntity>)
}

@Dao
interface MangaProgressDao {
    @Query("SELECT * FROM manga_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun find(bookId: String): MangaProgressEntity?

    @Upsert
    suspend fun upsert(progress: MangaProgressEntity)

    @Query("DELETE FROM manga_progress WHERE bookId = :bookId")
    suspend fun delete(bookId: String)
}

@Dao
interface MangaOfflineDao {
    @Transaction
    @Query("SELECT * FROM manga_offline_chapters WHERE chapterId = :chapterId LIMIT 1")
    suspend fun findChapter(chapterId: String): MangaOfflineChapterWithPages?

    @Transaction
    @Query(
        "SELECT oc.* FROM manga_offline_chapters oc " +
            "INNER JOIN manga_chapters c ON c.id = oc.chapterId " +
            "WHERE c.cacheKey = :cacheKey LIMIT 1"
    )
    suspend fun findByCacheKey(cacheKey: String): MangaOfflineChapterWithPages?

    @Transaction
    @Query(
        "SELECT oc.* FROM manga_offline_chapters oc " +
            "INNER JOIN manga_chapters c ON c.id = oc.chapterId " +
            "WHERE c.bookId = :bookId ORDER BY c.readingOrder ASC"
    )
    suspend fun listForBook(bookId: String): List<MangaOfflineChapterWithPages>

    @Upsert
    suspend fun upsertChapter(chapter: MangaOfflineChapterEntity)

    @Upsert
    suspend fun upsertPages(pages: List<MangaOfflinePageEntity>)

    @Query("DELETE FROM manga_offline_pages WHERE chapterId = :chapterId")
    suspend fun deletePages(chapterId: String)

    @Query("DELETE FROM manga_offline_chapters WHERE chapterId = :chapterId")
    suspend fun deleteChapter(chapterId: String)

    @Transaction
    suspend fun replaceChapter(
        chapter: MangaOfflineChapterEntity,
        pages: List<MangaOfflinePageEntity>
    ) {
        deletePages(chapter.chapterId)
        upsertChapter(chapter)
        if (pages.isNotEmpty()) {
            upsertPages(pages)
        }
    }
}
