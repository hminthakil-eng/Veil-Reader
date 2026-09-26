package com.veilreader.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

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

/**
 * Collections have a user-facing name and a locale-stable normalized key.
 *
 * The normalized key owns the UNIQUE constraint so `Fantasy`, `fantasy`, and ` FANTASY ` cannot
 * become separate shelves through different write paths. This is a database invariant rather
 * than a convention left to repository callers.
 */
@Entity(
    tableName = "collections",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMs: Long,
    val normalizedName: String = normalizeCollectionName(name)
)

internal fun normalizeCollectionName(name: String): String = name.trim().lowercase(Locale.ROOT)

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


@Entity(
    tableName = "reading_cycles",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("bookId"),
        Index(value = ["bookId", "cycleIndex"], unique = true),
        Index("completedAtEpochMs")
    ]
)
data class ReadingCycleEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val cycleIndex: Int,
    val titleSnapshot: String,
    val authorSnapshot: String,
    val startedAtEpochMs: Long?,
    val completedAtEpochMs: Long,
    val finalLocatorJson: String,
    val sessionCount: Int,
    val totalActiveMillis: Long,
    val pacedPageTurns: Int,
    val highlightCount: Int,
    val noteCount: Int,
    val bookmarkCount: Int,
    val sealCode: String,
    val timelineJson: String
)

@Entity(
    tableName = "passage_visits",
    foreignKeys = [
        ForeignKey(
            entity = HighlightEntity::class,
            parentColumns = ["id"],
            childColumns = ["highlightId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("highlightId"),
        Index("bookId"),
        Index("viewedAtEpochMs")
    ]
)
data class PassageVisitEntity(
    @PrimaryKey val id: String,
    val highlightId: String,
    val bookId: String,
    val locatorJson: String,
    val viewedAtEpochMs: Long
)


@Entity(
    tableName = "reading_milestones",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("bookId"),
        Index(value = ["bookId", "kind"], unique = true),
        Index("reachedAtEpochMs")
    ]
)
data class ReadingMilestoneEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val kind: String,
    val reachedAtEpochMs: Long,
    val progression: Float,
    val locatorJson: String?
)
