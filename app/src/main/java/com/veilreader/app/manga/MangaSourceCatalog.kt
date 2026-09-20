package com.veilreader.app.manga

import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.OkHttpMangaHttpClient
import com.veilreader.app.manga.net.PacedMangaHttpClient
import com.veilreader.app.manga.sources.mangadex.MangaDexSourceProvider

data class MangaSourceFeatureFlags(
    val mangaDexEnabled: Boolean = false
)

object MangaSourceCatalog {
    fun providers(
        flags: MangaSourceFeatureFlags,
        translatedLanguage: String = "en",
        httpClient: MangaHttpClient = OkHttpMangaHttpClient()
    ): List<MangaSourceProvider> {
        if (!flags.mangaDexEnabled) return emptyList()

        val pacedHttp = PacedMangaHttpClient(httpClient, requestsPerSecond = 4)
        return listOf(
            MangaDexSourceProvider(
                http = pacedHttp,
                translatedLanguage = translatedLanguage
            )
        )
    }
}