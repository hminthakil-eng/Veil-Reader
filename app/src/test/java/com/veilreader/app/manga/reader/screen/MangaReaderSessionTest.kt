package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.presentation.MangaChapterRoute
import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MangaReaderSessionTest {

    @Test
    fun oneCanonicalMangaWithOrderedDistinctChaptersIsAccepted() {
        val source = provider("source.one")
        val session = MangaReaderSession(
            entriesInReadingOrder = listOf(
                entry("work", source, 1.0, "one"),
                entry("work", source, 2.0, "two")
            )
        )

        assertEquals(2, session.entriesInReadingOrder.size)
        assertEquals("work", session.mangaId.value)
    }

    @Test
    fun duplicateLogicalChapterIsRejectedEvenWhenProviderKeysDiffer() {
        val source = provider("source.one")

        assertThrows(IllegalArgumentException::class.java) {
            MangaReaderSession(
                entriesInReadingOrder = listOf(
                    entry("work", source, 1.0, "old-key"),
                    entry("work", source, 1.0, "new-key")
                )
            )
        }
    }

    @Test
    fun mixedCanonicalWorksAreRejected() {
        val source = provider("source.one")

        assertThrows(IllegalArgumentException::class.java) {
            MangaReaderSession(
                entriesInReadingOrder = listOf(
                    entry("work-a", source, 1.0, "one"),
                    entry("work-b", source, 2.0, "two")
                )
            )
        }
    }

    @Test
    fun providerMustOwnItsChapter() {
        val route = route(
            work = "work",
            sourceId = SourceId("source.one"),
            number = 1.0,
            key = "one"
        )

        assertThrows(IllegalArgumentException::class.java) {
            MangaReaderChapterEntry(
                route = route,
                provider = provider("source.two")
            )
        }
    }

    @Test
    fun replacementRouteCanResolveByLogicalChapter() {
        val source = provider("source.one")
        val session = MangaReaderSession(
            listOf(entry("work", source, 5.0, "old-key"))
        )
        val replacement = route(
            work = "work",
            sourceId = SourceId("source.two"),
            number = 5.0,
            key = "replacement-key"
        )

        assertNotNull(session.entryForRoute(replacement))
    }

    private fun entry(
        work: String,
        provider: MangaSourceProvider,
        number: Double,
        key: String
    ) = MangaReaderChapterEntry(
        route = route(
            work = work,
            sourceId = provider.descriptor.id,
            number = number,
            key = key
        ),
        provider = provider
    )

    private fun route(
        work: String,
        sourceId: SourceId,
        number: Double,
        key: String
    ): MangaChapterRoute {
        val sourceChapter = SourceChapter(
            sourceId = sourceId,
            mangaKey = work,
            chapterKey = key,
            title = "Chapter $number",
            number = number,
            languageTag = "en"
        )
        return MangaChapterRoute(
            readerChapter = MangaReaderChapterRef(
                mangaId = CanonicalMangaId(work),
                anchor = MangaChapterAnchor(
                    number = number,
                    languageTag = "en",
                    providerChapterKeyHint = key
                )
            ),
            sourceChapter = sourceChapter
        )
    }

    private fun provider(id: String): MangaSourceProvider =
        object : MangaSourceProvider {
            override val descriptor = MangaSourceDescriptor(
                id = SourceId(id),
                displayName = id,
                domains = listOf("$id.example"),
                contentTypes = setOf(MangaContentType.MANGA)
            )

            override val capabilities = emptySet<MangaSourceCapability>()
        }
}
