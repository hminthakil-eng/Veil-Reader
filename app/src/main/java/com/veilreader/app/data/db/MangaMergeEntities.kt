package com.veilreader.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One active reversible local Manga merge.
 *
 * Source Books are never deleted by merge execution. [MangaMergeMemberEntity] makes them invisible
 * to the normal Library projection while the canonical Book rows, source CBZ files, progress and
 * history remain untouched for exact split/restore.
 */
@Entity(
    tableName = "manga_work_merges",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetBookId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id", "bookId"],
            childColumns = ["targetProgressChapterId", "targetBookId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["targetBookId"], unique = true),
        Index(value = ["id", "targetBookId"], unique = true),
        Index("createdAtEpochMs"),
        Index(value = ["targetProgressChapterId", "targetBookId"])
    ]
)
data class MangaWorkMergeEntity(
    @androidx.room.PrimaryKey val id: String,
    val targetBookId: String,
    val createdAtEpochMs: Long,
    val receiptVersion: Int = 1,
    val targetOriginalChapterCount: Int,
    val targetBookProgress: Float,
    val targetBookFinished: Boolean,
    val targetBookLastOpenedAtEpochMs: Long,
    val targetProgressChapterId: String? = null,
    val targetProgressPageIndex: Int? = null,
    val targetProgressPageCount: Int? = null,
    val targetProgressChapterProgression: Double? = null,
    val targetProgressUpdatedAtEpochMs: Long? = null
) {
    init {
        require(id.isNotBlank())
        require(targetBookId.isNotBlank())
        require(createdAtEpochMs >= 0L)
        require(receiptVersion >= 1)
        require(targetOriginalChapterCount > 0)
        require(targetBookProgress.isFinite() && targetBookProgress in 0f..1f)
        require(targetBookLastOpenedAtEpochMs >= 0L)
        require(targetProgressPageIndex == null || targetProgressPageIndex >= 0)
        require(targetProgressPageCount == null || targetProgressPageCount > 0)
        require(
            targetProgressChapterProgression == null ||
                (
                    targetProgressChapterProgression.isFinite() &&
                        targetProgressChapterProgression in 0.0..1.0
                    )
        )
        val hasProgress = targetProgressChapterId != null
        require(
            if (hasProgress) {
                targetProgressPageIndex != null &&
                    targetProgressChapterProgression != null &&
                    targetProgressUpdatedAtEpochMs != null
            } else {
                targetProgressPageIndex == null &&
                    targetProgressPageCount == null &&
                    targetProgressChapterProgression == null &&
                    targetProgressUpdatedAtEpochMs == null
            }
        )
    }
}

/**
 * A source Book hidden by an active merge.
 *
 * UNIQUE(sourceBookId) prevents one intact source work from belonging to several active merge
 * receipts at once. RESTRICT prevents accidental source deletion while a reversible receipt exists.
 */
@Entity(
    tableName = "manga_merge_members",
    primaryKeys = ["mergeId", "sourceBookId"],
    foreignKeys = [
        ForeignKey(
            entity = MangaWorkMergeEntity::class,
            parentColumns = ["id"],
            childColumns = ["mergeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceBookId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("mergeId"),
        Index(value = ["sourceBookId"], unique = true),
        Index(value = ["mergeId", "sourceOrder"], unique = true)
    ]
)
data class MangaMergeMemberEntity(
    val mergeId: String,
    val sourceBookId: String,
    val sourceOrder: Int
) {
    init {
        require(mergeId.isNotBlank())
        require(sourceBookId.isNotBlank())
        require(sourceOrder >= 0)
    }
}

/**
 * Exact identity evidence for every target chapter that existed before the merge.
 *
 * Split validates these rows before removing any merged copy. The chapter key is the durable local
 * CBZ fingerprint-backed identity, so a stale/corrupt receipt cannot silently redefine the
 * original-target boundary by reading order alone.
 */
@Entity(
    tableName = "manga_merge_original_chapters",
    primaryKeys = ["mergeId", "readingOrder"],
    foreignKeys = [
        ForeignKey(
            entity = MangaWorkMergeEntity::class,
            parentColumns = ["id", "targetBookId"],
            childColumns = ["mergeId", "targetBookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id", "bookId"],
            childColumns = ["chapterId", "targetBookId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("mergeId"),
        Index(value = ["mergeId", "targetBookId"]),
        Index(value = ["chapterId", "targetBookId"], unique = true)
    ]
)
data class MangaMergeOriginalChapterEntity(
    val mergeId: String,
    val readingOrder: Int,
    val chapterId: String,
    val targetBookId: String,
    val chapterKey: String
) {
    init {
        require(mergeId.isNotBlank())
        require(readingOrder >= 0)
        require(chapterId.isNotBlank())
        require(targetBookId.isNotBlank())
        require(chapterKey.isNotBlank())
    }
}

/**
 * Durable chapter-level split receipt.
 *
 * REBUILD_FROM_SOURCE_ARCHIVE rows own a target copy created by the merge and removed on split.
 * DEDUPLICATE_EXACT_ARCHIVE rows point at an already-existing target chapter and must never delete
 * it during split.
 */
@Entity(
    tableName = "manga_merge_chapters",
    primaryKeys = ["mergeId", "sourceChapterId"],
    foreignKeys = [
        ForeignKey(
            entity = MangaWorkMergeEntity::class,
            parentColumns = ["id", "targetBookId"],
            childColumns = ["mergeId", "targetBookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MangaMergeMemberEntity::class,
            parentColumns = ["mergeId", "sourceBookId"],
            childColumns = ["mergeId", "sourceBookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id", "bookId"],
            childColumns = ["sourceChapterId", "sourceBookId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id", "bookId"],
            childColumns = ["targetChapterId", "targetBookId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("mergeId"),
        Index(value = ["mergeId", "targetBookId"]),
        Index(value = ["mergeId", "sourceBookId"]),
        Index(value = ["sourceChapterId", "sourceBookId"], unique = true),
        Index(value = ["targetChapterId", "targetBookId"]),
        Index(value = ["mergeId", "targetReadingOrder"])
    ]
)
data class MangaMergeChapterEntity(
    val mergeId: String,
    val sourceChapterId: String,
    val sourceBookId: String,
    val targetChapterId: String,
    val targetBookId: String,
    val sourceReadingOrder: Int,
    val targetReadingOrder: Int,
    val disposition: String
) {
    init {
        require(mergeId.isNotBlank())
        require(sourceChapterId.isNotBlank())
        require(sourceBookId.isNotBlank())
        require(targetChapterId.isNotBlank())
        require(targetBookId.isNotBlank())
        require(sourceReadingOrder >= 0)
        require(targetReadingOrder >= 0)
        require(disposition in VALID_DISPOSITIONS)
    }

    companion object {
        const val REBUILD_FROM_SOURCE_ARCHIVE = "REBUILD_FROM_SOURCE_ARCHIVE"
        const val DEDUPLICATE_EXACT_ARCHIVE = "DEDUPLICATE_EXACT_ARCHIVE"
        val VALID_DISPOSITIONS = setOf(
            REBUILD_FROM_SOURCE_ARCHIVE,
            DEDUPLICATE_EXACT_ARCHIVE
        )
    }
}
