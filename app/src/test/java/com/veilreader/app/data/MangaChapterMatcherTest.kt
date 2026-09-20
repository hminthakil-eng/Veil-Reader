package com.veilreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaChapterMatcherTest {
    @Test
    fun exactVolumeAndChapter_matchesBeforeOtherSignals() {
        val plan = planMangaChapterMatches(
            existing = listOf(
                existing("stable-1", "Old title", chapter = 12.5, volume = 3.0)
            ),
            incoming = listOf(
                incoming(0, "Different title", chapter = 12.5, volume = 3.0)
            )
        )

        assertEquals("stable-1", plan.matches.single().existingChapterId)
        assertEquals(
            MangaChapterMatchReason.EXACT_VOLUME_AND_NUMBER,
            plan.matches.single().reason
        )
    }

    @Test
    fun uniqueChapterNumber_matchesWhenOneSourceOmitsVolume() {
        val plan = planMangaChapterMatches(
            existing = listOf(
                existing("stable-1", "Chapter 8", chapter = 8.0, volume = 2.0)
            ),
            incoming = listOf(
                incoming(0, "Ch. 8", chapter = 8.0, volume = null)
            )
        )

        assertEquals(MangaChapterMatchReason.UNIQUE_NUMBER, plan.matches.single().reason)
    }

    @Test
    fun conflictingExplicitVolumes_doNotAutoMatchByNumber() {
        val plan = planMangaChapterMatches(
            existing = listOf(
                existing("stable-1", "Chapter Eight", chapter = 8.0, volume = 1.0)
            ),
            incoming = listOf(
                incoming(0, "Different name", chapter = 8.0, volume = 2.0)
            )
        )

        assertTrue(plan.matches.isEmpty())
        assertEquals(setOf(0), plan.unmatchedIncomingIndices)
    }

    @Test
    fun duplicateChapterNumbers_areAmbiguousAndRemainUnmatched() {
        val plan = planMangaChapterMatches(
            existing = listOf(
                existing("a", "Chapter 10 - Group A", chapter = 10.0),
                existing("b", "Chapter 10 - Group B", chapter = 10.0)
            ),
            incoming = listOf(
                incoming(0, "Chapter 10", chapter = 10.0)
            )
        )

        assertTrue(plan.matches.isEmpty())
        assertEquals(setOf(0), plan.unmatchedIncomingIndices)
    }

    @Test
    fun duplicateIncomingReleases_areNotCollapsedIntoOneStableChapter() {
        val plan = planMangaChapterMatches(
            existing = listOf(
                existing("stable", "Chapter 10", chapter = 10.0)
            ),
            incoming = listOf(
                incoming(0, "Chapter 10 - A", chapter = 10.0),
                incoming(1, "Chapter 10 - B", chapter = 10.0)
            )
        )

        assertTrue(plan.matches.isEmpty())
        assertEquals(setOf(0, 1), plan.unmatchedIncomingIndices)
    }

    @Test
    fun uniqueNormalizedTitle_isSafeFallbackWhenNumbersAreMissing() {
        val plan = planMangaChapterMatches(
            existing = listOf(
                existing("stable", "Special — The Moon Door", chapter = null)
            ),
            incoming = listOf(
                incoming(0, "special: the moon door", chapter = null)
            )
        )

        assertEquals(MangaChapterMatchReason.UNIQUE_TITLE, plan.matches.single().reason)
    }

    @Test
    fun genericOrNumericTitles_areNotUsedAsFallback() {
        val plan = planMangaChapterMatches(
            existing = listOf(
                existing("stable", "12.5", chapter = null)
            ),
            incoming = listOf(
                incoming(0, "12.5", chapter = null)
            )
        )

        assertTrue(plan.matches.isEmpty())
    }

    private fun existing(
        id: String,
        title: String,
        chapter: Double?,
        volume: Double? = null
    ) = ExistingMangaChapterIdentity(
        id = id,
        title = title,
        chapterNumber = chapter,
        volumeNumber = volume
    )

    private fun incoming(
        index: Int,
        title: String,
        chapter: Double?,
        volume: Double? = null
    ) = IncomingMangaChapterIdentity(
        index = index,
        title = title,
        chapterNumber = chapter,
        volumeNumber = volume
    )
}
