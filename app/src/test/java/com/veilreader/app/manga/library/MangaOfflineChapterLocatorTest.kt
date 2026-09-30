package com.veilreader.app.manga.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MangaOfflineChapterLocatorTest {

    @Test
    fun numberedChapterKeepsMainDiscriminator() {
        val id = MangaOfflineChapterLocator.idFor(
            mangaId = CanonicalMangaId("work"),
            anchor = MangaChapterAnchor(
                volume = 2.0,
                number = 12.5,
                languageTag = "en",
                normalizedTitle = "Chapter 12.5",
                providerChapterKeyHint = "old-key"
            )
        )

        assertNotNull(id)
        assertEquals("main", id!!.discriminator)
        assertEquals(12.5, id.number)
    }

    @Test
    fun unnumberedSpecialUsesTitleAndSurvivesProviderKeyReplacement() {
        val old = MangaOfflineChapterLocator.idFor(
            mangaId = CanonicalMangaId("work"),
            anchor = MangaChapterAnchor(
                languageTag = "en",
                normalizedTitle = "Bonus: Winter Day",
                providerChapterKeyHint = "old-source-key"
            )
        )
        val replacement = MangaOfflineChapterLocator.idFor(
            mangaId = CanonicalMangaId("work"),
            anchor = MangaChapterAnchor(
                languageTag = "en",
                normalizedTitle = "Bonus - Winter Day",
                providerChapterKeyHint = "new-source-key"
            )
        )

        assertEquals(old, replacement)
        assertEquals(true, old!!.discriminator.startsWith("title-"))
    }

    @Test
    fun differentUnnumberedTitlesDoNotShareCacheIdentity() {
        val first = MangaOfflineChapterLocator.idFor(
            CanonicalMangaId("work"),
            MangaChapterAnchor(normalizedTitle = "Special A")
        )
        val second = MangaOfflineChapterLocator.idFor(
            CanonicalMangaId("work"),
            MangaChapterAnchor(normalizedTitle = "Special B")
        )

        assertNotEquals(first, second)
    }

    @Test
    fun insufficientChapterIdentityIsRejectedBeforeOfflineLookup() {
        assertThrows(IllegalArgumentException::class.java) {
            MangaChapterAnchor(languageTag = "en")
        }
    }
}
