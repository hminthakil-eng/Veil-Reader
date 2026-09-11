package com.veilreader.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val progress: Float,
    val currentChapter: String,
    val totalPages: Int,
    val pagesRead: Int,
    val format: String,
    val sourceUri: String?,
    val mediaType: String?,
    val locatorJson: String?,
    val addedAtEpochMs: Long,
    val lastOpenedAtEpochMs: Long,
    val finished: Boolean,
    val favorite: Boolean,
    val coverCachePath: String? = null,
    val contentFingerprint: String? = null,
    val seriesName: String? = null,
    val seriesIndex: Double? = null,
    val language: String? = null
)

@Entity(
    tableName = "highlights",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bookId")]
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val quote: String,
    val locatorJson: String,
    val note: String,
    val createdAtEpochMs: Long,
    val colorArgb: Long? = null
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bookId")]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val label: String,
    val locatorJson: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "collections",
    indices = [Index(value = ["name"], unique = true)]
)
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "book_collection",
    primaryKeys = ["bookId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("collectionId")]
)
data class BookCollectionCrossRef(
    val bookId: String,
    val collectionId: String
)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("bookId"), Index("startedAtEpochMs")]
)
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    val bookId: String?,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val activeMillis: Long,
    val pacedPageTurns: Int,
    val highlightCount: Int,
    val noteCount: Int
)
