package com.veilreader.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert

data class MangaWorkMergeWithMembers(
    @Embedded val merge: MangaWorkMergeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mergeId"
    )
    val members: List<MangaMergeMemberEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mergeId"
    )
    val chapters: List<MangaMergeChapterEntity>
)

@Dao
interface MangaMergeDao {
    @Transaction
    @Query("SELECT * FROM manga_work_merges WHERE id = :mergeId LIMIT 1")
    suspend fun find(mergeId: String): MangaWorkMergeWithMembers?

    @Transaction
    @Query("SELECT * FROM manga_work_merges WHERE targetBookId = :targetBookId LIMIT 1")
    suspend fun findForTarget(targetBookId: String): MangaWorkMergeWithMembers?

    @Transaction
    @Query(
        """
        SELECT m.* FROM manga_work_merges m
        INNER JOIN manga_merge_members mm ON mm.mergeId = m.id
        WHERE mm.sourceBookId = :sourceBookId
        LIMIT 1
        """
    )
    suspend fun findForSource(sourceBookId: String): MangaWorkMergeWithMembers?

    @Transaction
    @Query("SELECT * FROM manga_work_merges ORDER BY createdAtEpochMs ASC, id ASC")
    suspend fun listAll(): List<MangaWorkMergeWithMembers>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM manga_merge_members
            WHERE sourceBookId = :bookId
        )
        """
    )
    suspend fun isHiddenSource(bookId: String): Boolean

    @Upsert
    suspend fun upsertMerge(merge: MangaWorkMergeEntity)

    @Upsert
    suspend fun upsertMembers(members: List<MangaMergeMemberEntity>)

    @Upsert
    suspend fun upsertChapters(chapters: List<MangaMergeChapterEntity>)

    @Query("DELETE FROM manga_merge_chapters WHERE mergeId = :mergeId")
    suspend fun deleteChapterMappings(mergeId: String)

    @Query("DELETE FROM manga_merge_members WHERE mergeId = :mergeId")
    suspend fun deleteMembers(mergeId: String)

    @Query("DELETE FROM manga_work_merges WHERE id = :mergeId")
    suspend fun deleteMerge(mergeId: String)
}
