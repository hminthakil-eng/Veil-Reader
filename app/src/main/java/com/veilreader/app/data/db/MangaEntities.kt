package com.veilreader.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Manga persistence extends the existing Book catalog instead of creating a second title library.
 *
 * For persisted Manga, BookEntity.id is also the canonical Manga identity used by the reader stack.
 * These tables only own Manga-specific chapter, source, progress and offline-cache facts.
 */
@Entity(
    tableName = "manga_chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookId"),
        Index(value = ["bookId", "readingOrder"], unique = true),
        Index(value = ["id", "bookId"], unique = true),
        Index(value = ["cacheKey"], unique = true)
    ]
)
data class MangaChapterEntity(
    @androidx.room.PrimaryKey val id: String,
    val bookId: String,
    val readingOrder: Int,
    /**
     * Stable lookup key derived from OfflineChapterId / MangaCacheLayout.
     * It is source-neutral and therefore survives source replacement.
     */
    val cacheKey: String,
    val title: String? = null,
    val normalizedTitle: String? = null,
    val volume: Double? = null,
    val number: Double? = null,
    val languageTag: String? = null
) {
    init {
        require(id.isNotBlank())
        require(bookId.isNotBlank())
        require(readingOrder >= 0)
        require(cacheKey.isNotBlank())
        require(volume == null || volume.isFinite())
        require(number == null || number.isFinite())
    }
}

@Entity(
    tableName = "manga_source_links",
    primaryKeys = ["bookId", "sourceId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceId")]
)
data class MangaSourceLinkEntity(
    val bookId: String,
    val sourceId: String,
    val mangaKey: String,
    val publicUrl: String? = null
) {
    init {
        require(bookId.isNotBlank())
        require(sourceId.isNotBlank())
        require(mangaKey.isNotBlank())
    }
}

@Entity(
    tableName = "manga_chapter_sources",
    primaryKeys = ["chapterId", "sourceId"],
    foreignKeys = [
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id", "bookId"],
            childColumns = ["chapterId", "bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MangaSourceLinkEntity::class,
            parentColumns = ["bookId", "sourceId"],
            childColumns = ["bookId", "sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chapterId", "bookId"]),
        Index(value = ["bookId", "sourceId"]),
        Index(value = ["sourceId", "mangaKey", "chapterKey"], unique = true)
    ]
)
data class MangaChapterSourceEntity(
    val chapterId: String,
    val bookId: String,
    val sourceId: String,
    val mangaKey: String,
    val chapterKey: String
) {
    init {
        require(chapterId.isNotBlank())
        require(bookId.isNotBlank())
        require(sourceId.isNotBlank())
        require(mangaKey.isNotBlank())
        require(chapterKey.isNotBlank())
    }
}

@Entity(
    tableName = "manga_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id", "bookId"],
            childColumns = ["chapterId", "bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapterId", "bookId"])]
)
data class MangaProgressEntity(
    @androidx.room.PrimaryKey val bookId: String,
    val chapterId: String,
    val pageIndex: Int,
    val pageCount: Int? = null,
    val chapterProgression: Double,
    val updatedAtEpochMs: Long
) {
    init {
        require(bookId.isNotBlank())
        require(chapterId.isNotBlank())
        require(pageIndex >= 0)
        require(pageCount == null || pageCount > 0)
        require(chapterProgression.isFinite() && chapterProgression in 0.0..1.0)
    }
}

@Entity(
    tableName = "manga_offline_chapters",
    foreignKeys = [
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MangaOfflineChapterEntity(
    @androidx.room.PrimaryKey val chapterId: String,
    val originSourceId: String,
    val originChapterKey: String,
    val completed: Boolean,
    val updatedAtEpochMs: Long
) {
    init {
        require(chapterId.isNotBlank())
        require(originSourceId.isNotBlank())
        require(originChapterKey.isNotBlank())
    }
}

@Entity(
    tableName = "manga_offline_pages",
    primaryKeys = ["chapterId", "pageIndex"],
    foreignKeys = [
        ForeignKey(
            entity = MangaOfflineChapterEntity::class,
            parentColumns = ["chapterId"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["relativePath"], unique = true)]
)
data class MangaOfflinePageEntity(
    val chapterId: String,
    val pageIndex: Int,
    val relativePath: String,
    val byteSize: Long,
    val contentSha256: String? = null
) {
    init {
        require(chapterId.isNotBlank())
        require(pageIndex >= 0)
        require(relativePath.isNotBlank())
        require(byteSize >= 0)
    }
}
