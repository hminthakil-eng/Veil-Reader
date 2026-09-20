package com.veilreader.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps one stable Veil library item to one external manga source record.
 *
 * bookId is the canonical identity. sourceId/sourceKey are replaceable external bindings.
 */
@Entity(
    tableName = "manga_source_bindings",
    primaryKeys = ["bookId", "sourceId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceId", "sourceKey"], unique = true),
        Index("providerId")
    ]
)
data class MangaSourceBindingEntity(
    val bookId: String,
    val sourceId: String,
    val providerId: String,
    val sourceKey: String,
    val language: String,
    val sourceVersion: Int,
    val isPreferred: Boolean,
    val lastSyncedAtEpochMs: Long
)

/**
 * Veil-owned chapter identity. It survives source replacement.
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
        Index(value = ["bookId", "displayOrder"])
    ]
)
data class MangaChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val title: String,
    val chapterNumber: Double?,
    val volumeNumber: Double?,
    val publishedAtEpochMs: Long?,
    val displayOrder: Int,
    val pageCount: Int,
    val lastPageIndex: Int,
    val read: Boolean,
    val lastReadAtEpochMs: Long?
)

/**
 * Maps a stable Veil chapter to the source-specific chapter key.
 */
@Entity(
    tableName = "manga_chapter_bindings",
    primaryKeys = ["chapterId", "sourceId"],
    foreignKeys = [
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceId", "mangaSourceKey", "sourceChapterKey"], unique = true)
    ]
)
data class MangaChapterBindingEntity(
    val chapterId: String,
    val sourceId: String,
    val mangaSourceKey: String,
    val sourceChapterKey: String,
    val lastSeenAtEpochMs: Long
)

enum class MangaDownloadState {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Durable download state. Page bytes remain on the filesystem; Room owns their lifecycle state.
 */
@Entity(
    tableName = "manga_downloads",
    foreignKeys = [
        ForeignKey(
            entity = MangaChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["state", "updatedAtEpochMs"])
    ]
)
data class MangaDownloadEntity(
    @PrimaryKey val chapterId: String,
    val state: String,
    val downloadedPages: Int,
    val totalPages: Int,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val rootPath: String?,
    val failureCode: String?,
    val updatedAtEpochMs: Long
)
