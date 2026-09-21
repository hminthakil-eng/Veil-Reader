package com.veilreader.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "manga_works")
data class MangaWorkEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val alternativeTitlesJson: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "manga_source_links",
    primaryKeys = ["mangaId", "sourceId"],
    foreignKeys = [ForeignKey(
        entity = MangaWorkEntity::class,
        parentColumns = ["id"],
        childColumns = ["mangaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("mangaId"),
        Index(value = ["sourceId", "sourceKey"], unique = true)
    ]
)
data class MangaSourceLinkEntity(
    val mangaId: String,
    val sourceId: String,
    val sourceKey: String,
    val publicUrl: String?
)

@Entity(
    tableName = "manga_progress",
    foreignKeys = [ForeignKey(
        entity = MangaWorkEntity::class,
        parentColumns = ["id"],
        childColumns = ["mangaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("updatedAtEpochMs")]
)
data class MangaProgressEntity(
    @androidx.room.PrimaryKey val mangaId: String,
    val volume: Double?,
    val chapterNumber: Double?,
    val languageTag: String?,
    val normalizedTitle: String?,
    val providerChapterKeyHint: String?,
    val pageIndex: Int,
    val pageCount: Int?,
    val chapterProgression: Double,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "manga_offline_chapters",
    foreignKeys = [ForeignKey(
        entity = MangaWorkEntity::class,
        parentColumns = ["id"],
        childColumns = ["mangaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mangaId"), Index("updatedAtEpochMs")]
)
data class MangaOfflineChapterEntity(
    @androidx.room.PrimaryKey val manifestId: String,
    val mangaId: String,
    val languageTag: String?,
    val volume: Double?,
    val chapterNumber: Double?,
    val discriminator: String,
    val anchorVolume: Double?,
    val anchorNumber: Double?,
    val anchorLanguageTag: String?,
    val anchorNormalizedTitle: String?,
    val anchorProviderChapterKeyHint: String?,
    val originSourceId: String,
    val originChapterKey: String,
    val completed: Boolean,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "manga_offline_pages",
    primaryKeys = ["manifestId", "pageIndex"],
    foreignKeys = [ForeignKey(
        entity = MangaOfflineChapterEntity::class,
        parentColumns = ["manifestId"],
        childColumns = ["manifestId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("manifestId")]
)
data class MangaOfflinePageEntity(
    val manifestId: String,
    val pageIndex: Int,
    val relativePath: String,
    val byteSize: Long,
    val contentSha256: String?
)
