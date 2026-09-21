package com.veilreader.app.manga.library

import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaChapterMatcherTest {

    @Test
    fun exactNumberVolumeLanguageMatchWinsAcrossDifferentProviderKeys() {
        val anchor = MangaChapterAnchor(
            volume = 2.0,
            number = 12.5,
            languageTag = "en",
            normalizedTitle = "Chapter 12.5",
            providerChapterKeyHint = "old-key"
        )
        val candidates = listOf(
            chapter("new-wrong", 11.0, 2.0, "en", "Chapter 11"),
            chapter("new-correct", 12.5, 2.0, "en", "Chapter 12.5")
        )

        val match = MangaChapterMatcher.bestSafeMatch(anchor, candidates)

        assertEquals("new-correct", match?.chapter?.chapterKey)
        assertTrue((match?.confidence ?: 0.0) >= 0.99)
        assertTrue("chapter-number" in match!!.reasons)
    }

    @Test
    fun ambiguousEqualMatchesRequireManualDecision() {
        val anchor = MangaChapterAnchor(number = 5.0, languageTag = "en")
        val candidates = listOf(
            chapter("a", 5.0, null, "en", "Five"),
            chapter("b", 5.0, null, "en", "Five duplicate")
        )

        assertNull(MangaChapterMatcher.bestSafeMatch(anchor, candidates))
    }

    @Test
    fun weakMismatchDoesNotSilentlyMoveProgress() {
        val anchor = MangaChapterAnchor(volume = 3.0, number = 20.0, languageTag = "en")
        val candidates = listOf(
            chapter("bad", 2.0, 1.0, "es", "Unrelated")
        )

        assertNull(MangaChapterMatcher.bestSafeMatch(anchor, candidates))
    }

    private fun chapter(
        key: String,
        number: Double?,
        volume: Double?,
        language: String?,
        title: String
    ) = SourceChapter(
        sourceId = SourceId("new.source"),
        mangaKey = "manga",
        chapterKey = key,
        title = title,
        number = number,
        volume = volume,
        languageTag = language
    )
}
