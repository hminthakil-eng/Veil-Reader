package com.veilreader.app.manga

import android.content.Context
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.MangaLibraryRepository
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.manga.core.MangaFeaturePolicy
import com.veilreader.app.manga.core.MangaHub
import com.veilreader.app.manga.core.MangaOfflineStore
import com.veilreader.app.manga.core.MangaSourceCatalog
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.net.OkHttpMangaBinaryFetcher
import com.veilreader.app.manga.reader.MangaReaderPreferencesPersistence
import com.veilreader.app.manga.reader.MangaReaderPreferencesStore
import com.veilreader.app.manga.storage.FileMangaOfflineStore
import java.io.File

data class MangaAppRuntime(
    val catalog: MangaSourceCatalog,
    val hub: MangaHub,
    val library: MangaLibraryRepository,
    val offlineStore: MangaOfflineStore,
    val readerPreferences: MangaReaderPreferencesPersistence
)

internal fun defaultMangaRuntimeConfig(
    translatedLanguage: String = "en"
): MangaRuntimeSourceConfig {
    val language = translatedLanguage.trim()
    require(language.isNotEmpty()) { "Manga translated language cannot be blank." }

    val mangaDexId = MangaSourceId("mangadex.$language")
    return MangaRuntimeSourceConfig(
        directFlags = MangaDirectSourceFlags(mangaDexEnabled = true),
        translatedLanguage = language,
        policy = MangaFeaturePolicy(
            hubEnabled = true,
            localSourcesEnabled = true,
            directSourcesEnabled = false,
            gatewaySourcesEnabled = false,
            explicitlyEnabledSources = setOf(mangaDexId)
        )
    )
}

/**
 * Canonical application composition root for the first Manga Hub vertical slice.
 *
 * UI code receives this bundle instead of constructing Room, network, source, or offline-store
 * dependencies itself. The default policy intentionally enables only the reviewed MangaDex adapter;
 * additional direct/gateway sources remain fail-closed until explicitly approved.
 */
object MangaAppRuntimeFactory {
    fun create(
        context: Context,
        library: LocalLibraryRepository,
        translatedLanguage: String = "en"
    ): MangaAppRuntime {
        val appContext = context.applicationContext
        val catalog = MangaRuntimeSourceAssembly.catalog(
            config = defaultMangaRuntimeConfig(translatedLanguage)
        )
        val hub = requireNotNull(catalog.buildHubOrNull()) {
            "Manga Hub must be enabled by the application runtime policy."
        }
        val mangaLibrary = MangaLibraryRepository(
            database = VeilDatabase.get(appContext),
            library = library
        )
        val offlineStore = FileMangaOfflineStore(
            root = File(appContext.filesDir, "manga/offline"),
            fetcher = OkHttpMangaBinaryFetcher()
        )

        return MangaAppRuntime(
            catalog = catalog,
            hub = hub,
            library = mangaLibrary,
            offlineStore = offlineStore,
            readerPreferences = MangaReaderPreferencesStore(appContext)
        )
    }
}
