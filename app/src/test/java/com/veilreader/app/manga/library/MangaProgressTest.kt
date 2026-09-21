package com.veilreader.app.manga.library

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MangaProgressTest {

    @Test
    fun progressionMapsSafelyWhenReplacementSourceHasDifferentPageCount() {
        val progress = MangaReadingProgress(
            mangaId = CanonicalMangaId("work-1"),
            chapter = MangaChapterAnchor(volume = 2.0, number = 12.0, languageTag = "en"),
            pageIndex = 10,
            pageCount = 20,
            chapterProgression = 0.5,
            updatedAtEpochMs = 10L
        )

        assertEquals(14, progress.pageIndexFor(30))
        assertEquals(0, progress.pageIndexFor(1))
    }

    @Test
    fun progressStoreUsesCanonicalIdentityRatherThanSourceIdentity() = runBlocking {
        val store = InMemoryMangaProgressStore()
        val id = CanonicalMangaId("work-2")
        val progress = MangaReadingProgress(
            mangaId = id,
            chapter = MangaChapterAnchor(number = 3.0, providerChapterKeyHint = "old-source-key"),
            pageIndex = 4,
            pageCount = 10,
            chapterProgression = 4.0 / 9.0,
            updatedAtEpochMs = 20L
        )

        store.save(progress)

        assertEquals(progress, store.load(id))
        store.delete(id)
        assertNull(store.load(id))
    }
}
