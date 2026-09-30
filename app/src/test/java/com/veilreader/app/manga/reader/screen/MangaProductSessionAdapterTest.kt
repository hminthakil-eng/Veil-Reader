package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPageImage
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRequestContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaProductSessionAdapterTest {

    private val mangaId = CanonicalMangaId("canonical-work")
    private val sourceId = SourceId("fixture.source")

    @Test
    fun offlineOnlyCatalogBuildsSessionWithoutNetworkOwnership() {
        val chapters = listOf(
            chapter("c1", number = 1.0),
            chapter("c2", number = 2.0)
        )

        val result = MangaProductSessionAdapter().build(
            mangaId = mangaId,
            chaptersInReadingOrder = chapters.map(::MangaProductChapter)
        )

        assertTrue(result is MangaSessionAdapterResult.Ready)
        val session = (result as MangaSessionAdapterResult.Ready).session
        assertEquals(listOf("c1", "c2"), session.routes.map { it.sourceChapter.chapterKey })
        assertFalse(session.initialEntry.request().canUseNetwork)
    }

    @Test
    fun matchingPagesProviderIsAttachedToReaderEntry() {
        val provider = FixtureProvider(sourceId)
        val result = MangaProductSessionAdapter().build(
            mangaId = mangaId,
            chaptersInReadingOrder = listOf(
                MangaProductChapter(
                    sourceChapter = chapter("c1", number = 1.0),
                    provider = provider
                )
            )
        )

        val session = (result as MangaSessionAdapterResult.Ready).session
        assertTrue(session.initialEntry.request().canUseNetwork)
        assertEquals(provider, session.initialEntry.provider)
    }

    @Test
    fun sourceReplacementKeyStillRestoresByLogicalChapterAnchor() {
        val result = MangaProductSessionAdapter().build(
            mangaId = mangaId,
            chaptersInReadingOrder = listOf(
                MangaProductChapter(chapter("replacement-1", number = 1.0)),
                MangaProductChapter(chapter("replacement-2", number = 2.0))
            ),
            initialAnchor = MangaChapterAnchor(
                number = 2.0,
                providerChapterKeyHint = "old-provider-key"
            )
        )

        val session = (result as MangaSessionAdapterResult.Ready).session
        assertEquals("replacement-2", session.initialEntry.route.sourceChapter.chapterKey)
    }

    @Test
    fun duplicateLogicalChaptersAreRejectedBeforeReaderConstruction() {
        val result = MangaProductSessionAdapter().build(
            mangaId = mangaId,
            chaptersInReadingOrder = listOf(
                MangaProductChapter(chapter("a", number = 4.0)),
                MangaProductChapter(chapter("b", number = 4.0))
            )
        )

        assertEquals(
            MangaSessionUnavailableReason.DUPLICATE_LOGICAL_CHAPTER,
            (result as MangaSessionAdapterResult.Unavailable).reason
        )
    }

    @Test
    fun providerWithoutPagesCapabilityIsRejected() {
        val provider = object : MangaSourceProvider {
            override val descriptor = MangaSourceDescriptor(
                id = sourceId,
                displayName = "Fixture",
                domains = listOf("fixture.invalid"),
                contentTypes = setOf(MangaContentType.MANGA)
            )
            override val capabilities = emptySet<MangaSourceCapability>()
        }

        val result = MangaProductSessionAdapter().build(
            mangaId = mangaId,
            chaptersInReadingOrder = listOf(
                MangaProductChapter(chapter("c1", number = 1.0), provider)
            )
        )

        assertEquals(
            MangaSessionUnavailableReason.PROVIDER_CANNOT_LOAD_PAGES,
            (result as MangaSessionAdapterResult.Unavailable).reason
        )
    }

    private fun chapter(key: String, number: Double): SourceChapter =
        SourceChapter(
            sourceId = sourceId,
            mangaKey = "work",
            chapterKey = key,
            number = number,
            languageTag = "en"
        )

    private class FixtureProvider(
        id: SourceId
    ) : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = id,
            displayName = "Fixture",
            domains = listOf("fixture.invalid"),
            contentTypes = setOf(MangaContentType.MANGA)
        )
        override val capabilities = setOf(MangaSourceCapability.PAGES)

        override suspend fun pages(
            chapter: SourceChapter,
            context: SourceRequestContext
        ): SourceOutcome<List<MangaPageImage>> =
            SourceOutcome.Success(
                listOf(MangaPageImage(index = 0, imageUrl = "https://fixture.invalid/p0.jpg"))
            )
    }
}
