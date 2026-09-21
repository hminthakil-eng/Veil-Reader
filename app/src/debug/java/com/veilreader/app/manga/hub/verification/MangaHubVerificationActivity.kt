package com.veilreader.app.manga.hub.verification

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.veilreader.app.data.db.MIGRATION_1_2
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.manga.MangaRoomRepository
import com.veilreader.app.manga.debug.MangaDebugFixtureAssets
import com.veilreader.app.manga.hub.MangaHubCatalogService
import com.veilreader.app.manga.hub.MangaHubDiscoverySeed
import com.veilreader.app.manga.hub.MangaHubScreen
import com.veilreader.app.manga.reader.presentation.MangaChapterAvailability
import com.veilreader.app.manga.reader.presentation.MangaChapterPresentationLoader
import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationState
import com.veilreader.app.manga.reader.presentation.MangaReadyPresentation
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.manga.source.SourceRegistry
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debug-only end-to-end Manga Hub host.
 *
 * Production Hub code is exercised unchanged. Only the provider and page bytes are deterministic
 * local fixtures so catalog/library/reader behavior can be verified without a live website.
 */
class MangaHubVerificationActivity : FragmentActivity() {

    private var dependencies by mutableStateOf<VerificationDependencies?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VeilTheme {
                val ready = dependencies
                if (ready == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    MangaHubScreen(
                        service = ready.service,
                        readerLoader = ready.readerLoader,
                        progressStore = ready.repository,
                        cacheRoot = ready.cacheRoot,
                        onClose = ::finish,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        val resetLibrary = savedInstanceState == null &&
            intent.getBooleanExtra(EXTRA_RESET_LIBRARY, true)

        lifecycleScope.launch {
            dependencies = withContext(Dispatchers.IO) {
                buildDependencies(resetLibrary)
            }
        }
    }

    private suspend fun buildDependencies(
        resetLibrary: Boolean
    ): VerificationDependencies {
        val cacheRoot = File(cacheDir, "manga-hub-verification").apply { mkdirs() }
        val repository = MangaHubVerificationDatabase.repository(applicationContext)

        if (resetLibrary) {
            repository.listWorks().forEach { repository.deleteWork(it.id) }
            repository.clearOfflineIndex()
        }

        val provider = FixtureMangaSourceProvider()
        val registry = SourceRegistry(listOf(provider))
        val execution = SourceExecutionCoordinator()
        val service = MangaHubCatalogService(
            sources = registry,
            execution = execution,
            library = repository,
            discoverySeed = MangaHubDiscoverySeed { provider.discoveryRefs() }
        )

        val loader = MangaChapterPresentationLoader { request ->
            val sourceChapter = requireNotNull(request.sourceChapter) {
                "Fixture Hub reader expects a source-backed chapter"
            }
            val files = (0 until 4).map { pageIndex ->
                val webtoonLike = sourceChapter.mangaKey == "midnight-pharmacy"
                MangaDebugFixtureAssets.ensurePng(
                    root = cacheRoot,
                    name = sourceChapter.mangaKey + "-" +
                        sourceChapter.chapterKey + "-" + pageIndex + ".png",
                    width = if (webtoonLike) 720 else 700,
                    height = if (webtoonLike) 2_400 else 1_050,
                    seed = sourceChapter.chapterKey.hashCode() + pageIndex
                )
            }

            MangaReaderPresentationState.Ready(
                MangaReadyPresentation(
                    chapter = request.chapter,
                    pages = files.mapIndexed { index, file ->
                        MangaPageAsset.Local(
                            index = index,
                            relativePath = file.name,
                            byteSize = file.length()
                        )
                    },
                    availability = MangaChapterAvailability.COMPLETE_OFFLINE,
                    complete = true
                )
            )
        }

        return VerificationDependencies(
            cacheRoot = cacheRoot,
            repository = repository,
            service = service,
            readerLoader = loader
        )
    }

    data class VerificationDependencies(
        val cacheRoot: File,
        val repository: MangaRoomRepository,
        val service: MangaHubCatalogService,
        val readerLoader: MangaChapterPresentationLoader
    )

    companion object {
        const val EXTRA_RESET_LIBRARY = "reset_library"
    }
}

private object MangaHubVerificationDatabase {
    @Volatile
    private var database: VeilDatabase? = null

    fun repository(context: android.content.Context): MangaRoomRepository {
        val existing = database
        if (existing != null) return MangaRoomRepository(existing)

        return synchronized(this) {
            val current = database
            if (current != null) {
                MangaRoomRepository(current)
            } else {
                val created = Room.databaseBuilder(
                    context.applicationContext,
                    VeilDatabase::class.java,
                    "manga_hub_verification.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                database = created
                MangaRoomRepository(created)
            }
        }
    }
}
