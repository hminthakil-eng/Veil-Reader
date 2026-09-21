package com.veilreader.app.manga.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceRegistryTest {

    @Test
    fun duplicateSourceIds_areRejected() {
        val first = fakeProvider(id = "alpha.source", name = "Alpha")
        val second = fakeProvider(id = "alpha.source", name = "Other Alpha")

        assertThrows(IllegalArgumentException::class.java) {
            SourceRegistry(listOf(first, second))
        }
    }

    @Test
    fun registryOrdersSourcesDeterministically_andFiltersCapabilities() {
        val beta = fakeProvider(
            id = "beta.source",
            name = "Beta",
            capabilities = setOf(MangaSourceCapability.SEARCH)
        )
        val alpha = fakeProvider(
            id = "alpha.source",
            name = "alpha",
            capabilities = setOf(
                MangaSourceCapability.SEARCH,
                MangaSourceCapability.PAGES
            )
        )

        val registry = SourceRegistry(listOf(beta, alpha))

        assertEquals(
            listOf(SourceId("alpha.source"), SourceId("beta.source")),
            registry.all().map { it.descriptor.id }
        )
        assertEquals(listOf(alpha), registry.supporting(MangaSourceCapability.PAGES))
        assertSame(beta, registry.require(SourceId("beta.source")))
    }

    @Test
    fun candidatesRespectLanguageAndContentType_withoutOverFilteringGenericSources() {
        val englishManga = fakeProvider(
            id = "en.manga",
            name = "English Manga",
            capabilities = setOf(MangaSourceCapability.SEARCH),
            localeTags = setOf("en"),
            contentTypes = setOf(MangaContentType.MANGA)
        )
        val generic = fakeProvider(
            id = "generic.source",
            name = "Generic",
            capabilities = setOf(MangaSourceCapability.SEARCH),
            contentTypes = setOf(MangaContentType.UNKNOWN)
        )
        val spanish = fakeProvider(
            id = "es.manga",
            name = "Spanish Manga",
            capabilities = setOf(MangaSourceCapability.SEARCH),
            localeTags = setOf("es"),
            contentTypes = setOf(MangaContentType.MANGA)
        )

        val candidates = SourceRegistry(listOf(spanish, generic, englishManga)).candidates(
            capability = MangaSourceCapability.SEARCH,
            languageTag = "en",
            contentType = MangaContentType.MANGA
        )

        assertEquals(
            listOf(SourceId("en.manga"), SourceId("generic.source")),
            candidates.map { it.descriptor.id }
        )
    }

    private fun fakeProvider(
        id: String,
        name: String,
        capabilities: Set<MangaSourceCapability> = emptySet(),
        localeTags: Set<String> = emptySet(),
        contentTypes: Set<MangaContentType> = setOf(MangaContentType.UNKNOWN)
    ): MangaSourceProvider = object : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = SourceId(id),
            displayName = name,
            domains = listOf("${id}.example"),
            localeTags = localeTags,
            contentTypes = contentTypes
        )
        override val capabilities = capabilities
    }
}
