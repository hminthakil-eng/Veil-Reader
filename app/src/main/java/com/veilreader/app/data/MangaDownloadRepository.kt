package com.veilreader.app.data

import com.veilreader.app.data.db.MangaDownloadEntity
import com.veilreader.app.data.db.MangaDownloadState
import com.veilreader.app.data.db.VeilDatabase

class MangaDownloadRepository internal constructor(
    private val database: VeilDatabase
) {
    suspend fun queue(
        chapterId: String,
        totalPages: Int = 0,
        requestedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        requireNotNull(database.mangaChapters().findById(chapterId)) {
            "Cannot queue a download for a missing manga chapter."
        }
        val existing = database.mangaDownloads().find(chapterId)
        val current = existing?.state?.let(MangaDownloadState::valueOf)
        require(
            current == null ||
                current == MangaDownloadState.FAILED ||
                current == MangaDownloadState.PAUSED
        ) {
            "Manga chapter is already queued, running, or completed."
        }

        database.mangaDownloads().upsert(
            MangaDownloadEntity(
                chapterId = chapterId,
                state = MangaDownloadState.QUEUED.name,
                downloadedPages = 0,
                totalPages = totalPages.coerceAtLeast(0),
                downloadedBytes = 0L,
                totalBytes = null,
                rootPath = null,
                failureCode = null,
                updatedAtEpochMs = requestedAtEpochMs
            )
        )
    }

    suspend fun begin(
        chapterId: String,
        totalPages: Int,
        totalBytes: Long? = null,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        transition(
            chapterId = chapterId,
            target = MangaDownloadState.RUNNING,
            totalPages = totalPages.coerceAtLeast(0),
            totalBytes = totalBytes?.coerceAtLeast(0L),
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    suspend fun updateProgress(
        chapterId: String,
        downloadedPages: Int,
        totalPages: Int,
        downloadedBytes: Long,
        totalBytes: Long? = null,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        val current = requireNotNull(database.mangaDownloads().find(chapterId)) {
            "Manga download does not exist."
        }
        require(MangaDownloadState.valueOf(current.state) == MangaDownloadState.RUNNING) {
            "Only a running manga download can report progress."
        }
        require(downloadedPages in 0..totalPages.coerceAtLeast(0)) {
            "Downloaded manga page count is out of range."
        }
        require(downloadedBytes >= 0L) { "Downloaded manga bytes cannot be negative." }

        check(
            database.mangaDownloads().updateState(
                chapterId = chapterId,
                state = MangaDownloadState.RUNNING.name,
                downloadedPages = downloadedPages,
                totalPages = totalPages.coerceAtLeast(0),
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes?.coerceAtLeast(downloadedBytes),
                rootPath = current.rootPath,
                failureCode = null,
                updatedAtEpochMs = updatedAtEpochMs
            ) == 1
        ) {
            "Manga download disappeared during progress update."
        }
    }

    suspend fun pause(
        chapterId: String,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        transition(
            chapterId = chapterId,
            target = MangaDownloadState.PAUSED,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    suspend fun complete(
        chapterId: String,
        rootPath: String,
        downloadedPages: Int,
        totalPages: Int,
        downloadedBytes: Long,
        totalBytes: Long? = null,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        require(rootPath.isNotBlank()) { "Completed manga download needs a durable root path." }
        require(totalPages > 0 && downloadedPages == totalPages) {
            "Manga download cannot complete until every page is present."
        }
        require(downloadedBytes > 0L) { "Completed manga download cannot be empty." }

        transition(
            chapterId = chapterId,
            target = MangaDownloadState.COMPLETED,
            downloadedPages = downloadedPages,
            totalPages = totalPages,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes?.coerceAtLeast(downloadedBytes),
            rootPath = rootPath,
            failureCode = null,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    suspend fun fail(
        chapterId: String,
        failureCode: String,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        require(failureCode.isNotBlank()) { "Manga download failure code cannot be blank." }
        transition(
            chapterId = chapterId,
            target = MangaDownloadState.FAILED,
            failureCode = failureCode,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    suspend fun remove(chapterId: String) {
        database.mangaDownloads().delete(chapterId)
    }

    private suspend fun transition(
        chapterId: String,
        target: MangaDownloadState,
        downloadedPages: Int? = null,
        totalPages: Int? = null,
        downloadedBytes: Long? = null,
        totalBytes: Long? = null,
        rootPath: String? = null,
        failureCode: String? = null,
        updatedAtEpochMs: Long
    ) {
        val current = requireNotNull(database.mangaDownloads().find(chapterId)) {
            "Manga download does not exist."
        }
        val from = MangaDownloadState.valueOf(current.state)
        require(isAllowedMangaDownloadTransition(from, target)) {
            "Invalid manga download transition: $from -> $target"
        }

        check(
            database.mangaDownloads().updateState(
                chapterId = chapterId,
                state = target.name,
                downloadedPages = downloadedPages ?: current.downloadedPages,
                totalPages = totalPages ?: current.totalPages,
                downloadedBytes = downloadedBytes ?: current.downloadedBytes,
                totalBytes = totalBytes ?: current.totalBytes,
                rootPath = rootPath ?: current.rootPath,
                failureCode = failureCode,
                updatedAtEpochMs = updatedAtEpochMs
            ) == 1
        ) {
            "Manga download disappeared during state transition."
        }
    }
}

internal fun isAllowedMangaDownloadTransition(
    from: MangaDownloadState,
    to: MangaDownloadState
): Boolean = when (from) {
    MangaDownloadState.QUEUED -> to == MangaDownloadState.RUNNING ||
        to == MangaDownloadState.PAUSED ||
        to == MangaDownloadState.FAILED

    MangaDownloadState.RUNNING -> to == MangaDownloadState.PAUSED ||
        to == MangaDownloadState.COMPLETED ||
        to == MangaDownloadState.FAILED

    MangaDownloadState.PAUSED -> to == MangaDownloadState.RUNNING ||
        to == MangaDownloadState.QUEUED ||
        to == MangaDownloadState.FAILED

    MangaDownloadState.FAILED -> to == MangaDownloadState.QUEUED
    MangaDownloadState.COMPLETED -> false
}
