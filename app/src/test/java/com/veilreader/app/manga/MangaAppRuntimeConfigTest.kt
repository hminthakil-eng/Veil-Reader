package com.veilreader.app.manga

import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.MangaHttpResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaAppRuntimeConfigTest {
    @Test
    fun defaultConfig_enablesOnlyExplicitMangaDexSource() {
        val config = defaultMangaRuntimeConfig("en")
        val catalog = MangaRuntimeSourceAssembly.catalog(
            config = config,
            directHttpClient = MangaHttpClient { _, _ ->
                MangaHttpResponse(code = 200, body = "{}")
            }
        )

        val snapshot = catalog.snapshot()
        assertTrue(snapshot.hubEnabled)
        assertEquals(listOf("mangadex.en"), snapshot.entries.map { it.descriptor.id.value })
        assertEquals(listOf("mangadex.en"), snapshot.enabledSources.map { it.id.value })
        assertFalse(config.policy.directSourcesEnabled)
        assertFalse(config.policy.gatewaySourcesEnabled)
        assertEquals(setOf(MangaSourceId("mangadex.en")), config.policy.explicitlyEnabledSources)
    }

    @Test
    fun defaultConfig_usesRequestedTranslatedLanguageInStableSourceId() {
        val config = defaultMangaRuntimeConfig("fa")
        val catalog = MangaRuntimeSourceAssembly.catalog(
            config = config,
            directHttpClient = MangaHttpClient { _, _ ->
                MangaHttpResponse(code = 200, body = "{}")
            }
        )

        val entry = catalog.snapshot().entries.single()
        assertEquals("fa", entry.descriptor.language)
        assertEquals("mangadex.fa", entry.descriptor.id.value)
        assertTrue(entry.enabled)
    }

    @Test(expected = IllegalArgumentException::class)
    fun defaultConfig_rejectsBlankLanguage() {
        defaultMangaRuntimeConfig("   ")
    }
}
