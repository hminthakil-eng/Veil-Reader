package com.veilreader.app.data

import com.veilreader.app.data.db.MangaChapterBindingEntity
import com.veilreader.app.data.db.MangaChapterEntity
import com.veilreader.app.data.db.MangaSourceBindingEntity

data class MangaSourceBindingSnapshot(
    val bookId: String,
    val sourceId: String,
    val providerId: String,
    val sourceKey: String,
    val language: String,
    val sourceVersion: Int,
    val isPreferred: Boolean,
    val lastSyncedAtEpochMs: Long
)

data class MangaChapterSnapshot(
    val id: String,
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

data class MangaChapterBindingSnapshot(
    val chapterId: String,
    val sourceId: String,
    val sourceChapterKey: String,
    val lastSeenAtEpochMs: Long
)

internal fun MangaSourceBindingEntity.toSnapshot() = MangaSourceBindingSnapshot(
    bookId = bookId,
    sourceId = sourceId,
    providerId = providerId,
    sourceKey = sourceKey,
    language = language,
    sourceVersion = sourceVersion,
    isPreferred = isPreferred,
    lastSyncedAtEpochMs = lastSyncedAtEpochMs
)

internal fun MangaSourceBindingSnapshot.toEntity() = MangaSourceBindingEntity(
    bookId = bookId,
    sourceId = sourceId,
    providerId = providerId,
    sourceKey = sourceKey,
    language = language,
    sourceVersion = sourceVersion,
    isPreferred = isPreferred,
    lastSyncedAtEpochMs = lastSyncedAtEpochMs
)

internal fun MangaChapterEntity.toSnapshot() = MangaChapterSnapshot(
    id = id,
    bookId = bookId,
    title = title,
    chapterNumber = chapterNumber,
    volumeNumber = volumeNumber,
    publishedAtEpochMs = publishedAtEpochMs,
    displayOrder = displayOrder,
    pageCount = pageCount,
    lastPageIndex = lastPageIndex,
    read = read,
    lastReadAtEpochMs = lastReadAtEpochMs
)

internal fun MangaChapterSnapshot.toEntity() = MangaChapterEntity(
    id = id,
    bookId = bookId,
    title = title,
    chapterNumber = chapterNumber,
    volumeNumber = volumeNumber,
    publishedAtEpochMs = publishedAtEpochMs,
    displayOrder = displayOrder,
    pageCount = pageCount,
    lastPageIndex = lastPageIndex,
    read = read,
    lastReadAtEpochMs = lastReadAtEpochMs
)

internal fun MangaChapterBindingEntity.toSnapshot() = MangaChapterBindingSnapshot(
    chapterId = chapterId,
    sourceId = sourceId,
    sourceChapterKey = sourceChapterKey,
    lastSeenAtEpochMs = lastSeenAtEpochMs
)

internal fun MangaChapterBindingSnapshot.toEntity() = MangaChapterBindingEntity(
    chapterId = chapterId,
    sourceId = sourceId,
    sourceChapterKey = sourceChapterKey,
    lastSeenAtEpochMs = lastSeenAtEpochMs
)
