package com.veilreader.app.data

import com.veilreader.app.data.db.MangaDownloadState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaDownloadTransitionTest {
    @Test
    fun activeDownloadTransitions_areExplicit() {
        assertTrue(
            isAllowedMangaDownloadTransition(
                MangaDownloadState.QUEUED,
                MangaDownloadState.RUNNING
            )
        )
        assertTrue(
            isAllowedMangaDownloadTransition(
                MangaDownloadState.RUNNING,
                MangaDownloadState.COMPLETED
            )
        )
        assertTrue(
            isAllowedMangaDownloadTransition(
                MangaDownloadState.RUNNING,
                MangaDownloadState.PAUSED
            )
        )
        assertTrue(
            isAllowedMangaDownloadTransition(
                MangaDownloadState.RUNNING,
                MangaDownloadState.FAILED
            )
        )
    }

    @Test
    fun completedDownload_isTerminalUntilExplicitRemoval() {
        MangaDownloadState.entries.forEach { target ->
            assertFalse(
                isAllowedMangaDownloadTransition(
                    MangaDownloadState.COMPLETED,
                    target
                )
            )
        }
    }

    @Test
    fun failedDownload_mustReturnToQueueBeforeRunning() {
        assertTrue(
            isAllowedMangaDownloadTransition(
                MangaDownloadState.FAILED,
                MangaDownloadState.QUEUED
            )
        )
        assertFalse(
            isAllowedMangaDownloadTransition(
                MangaDownloadState.FAILED,
                MangaDownloadState.RUNNING
            )
        )
    }
}
