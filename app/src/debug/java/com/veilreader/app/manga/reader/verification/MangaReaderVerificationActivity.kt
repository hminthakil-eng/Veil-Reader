package com.veilreader.app.manga.reader.verification

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
import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.InMemoryMangaProgressStore
import com.veilreader.app.manga.library.MangaProgressStore
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.reader.MangaPageDirection
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.presentation.MangaChapterAvailability
import com.veilreader.app.manga.reader.presentation.MangaChapterPresentationLoader
import com.veilreader.app.manga.reader.presentation.MangaChapterRoute
import com.veilreader.app.manga.reader.presentation.MangaPageAsset
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationState
import com.veilreader.app.manga.reader.presentation.MangaReadyPresentation
import com.veilreader.app.manga.reader.screen.MangaReaderChapterEntry
import com.veilreader.app.manga.reader.screen.MangaReaderIntegratedScreen
import com.veilreader.app.manga.reader.screen.MangaReaderSession
import com.veilreader.app.manga.reader.screen.MangaReaderSessionOptions
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debug-only deterministic device-verification host.
 *
 * It reconstructs its complete session from Intent extras and local files, so Activity recreation
 * and process relaunch do not depend on static test objects or a live Manga source.
 */
class MangaReaderVerificationActivity : FragmentActivity() {

    private var fixture by mutableStateOf<VerificationFixture?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VeilTheme {
                val ready = fixture
                if (ready == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    MangaReaderIntegratedScreen(
                        session = ready.session,
                        loader = ready.loader,
                        progressStore = ready.progressStore,
                        cacheRoot = ready.cacheRoot,
                        onClose = ::finish,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        lifecycleScope.launch {
            fixture = withContext(Dispatchers.IO) {
                buildFixture()
            }
        }
    }

    private suspend fun buildFixture(): VerificationFixture {
        val cacheRoot = File(cacheDir, "manga-reader-verification").apply { mkdirs() }
        val extreme = intent.getBooleanExtra(EXTRA_EXTREME, false)
        val partial = intent.getBooleanExtra(EXTRA_PARTIAL_OFFLINE, false)
        val resetProgress = intent.getBooleanExtra(EXTRA_RESET_PROGRESS, true)
        val durableProgress = intent.getBooleanExtra(EXTRA_DURABLE_PROGRESS, true)
        val requestedStartPage = intent.getIntExtra(EXTRA_START_PAGE, 0).coerceIn(0, 2)

        val normalFiles = (0..2).map { index ->
            MangaDebugFixtureAssets.ensurePng(
                root = cacheRoot,
                name = "normal-" + index + ".png",
                width = 600,
                height = 900,
                seed = index + 1
            )
        }
        val extremeFile = if (extreme) {
            MangaDebugFixtureAssets.ensurePng(
                root = cacheRoot,
                name = "extreme.png",
                width = 360,
                height = 12_000,
                seed = 50
            )
        } else {
            null
        }

        val mangaId = CanonicalMangaId(VERIFY_MANGA_ID)
        val sourceId = SourceId(VERIFY_SOURCE_ID)
        val chapters = (1..2).map { number ->
            val sourceChapter = SourceChapter(
                sourceId = sourceId,
                mangaKey = VERIFY_MANGA_ID,
                chapterKey = "chapter-" + number,
                title = "Verification Chapter " + number,
                number = number.toDouble(),
                languageTag = "en"
            )
            MangaReaderChapterEntry(
                route = MangaChapterRoute.from(mangaId, sourceChapter)
            )
        }

        val session = MangaReaderSession(
            entriesInReadingOrder = chapters,
            options = MangaReaderSessionOptions(
                mode = MangaReaderMode.PAGED,
                direction = MangaPageDirection.RIGHT_TO_LEFT
            )
        )

        val repository = MangaVerificationDatabase.repository(applicationContext)
        if (repository.loadWork(mangaId) == null) {
            repository.saveWork(
                CanonicalManga(
                    id = mangaId,
                    title = "Veil Verification Manga",
                    createdAtEpochMs = 1L
                )
            )
        }

        val progressStore: MangaProgressStore = if (durableProgress) {
            repository
        } else {
            InMemoryMangaProgressStore()
        }

        if (resetProgress) {
            progressStore.delete(mangaId)
            if (requestedStartPage > 0) {
                val chapter = session.initialEntry.route.readerChapter
                progressStore.save(
                    MangaReadingProgress(
                        mangaId = mangaId,
                        chapter = chapter.anchor,
                        pageIndex = requestedStartPage,
                        pageCount = 3,
                        chapterProgression = requestedStartPage / 2.0,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }

        val loader = MangaChapterPresentationLoader { request ->
            val firstChapter = request.chapter.anchor.number == 1.0
            val files = if (firstChapter && extremeFile != null) {
                listOf(extremeFile, normalFiles[1], normalFiles[2])
            } else {
                normalFiles
            }
            val allPages = files.mapIndexed { index, file ->
                MangaPageAsset.Local(
                    index = index,
                    relativePath = file.name,
                    byteSize = file.length()
                )
            }
            val visiblePages = if (firstChapter && partial) allPages.take(2) else allPages
            MangaReaderPresentationState.Ready(
                MangaReadyPresentation(
                    chapter = request.chapter,
                    pages = visiblePages,
                    availability = if (firstChapter && partial) {
                        MangaChapterAvailability.PARTIAL_OFFLINE
                    } else {
                        MangaChapterAvailability.COMPLETE_OFFLINE
                    },
                    complete = !(firstChapter && partial)
                )
            )
        }

        return VerificationFixture(
            cacheRoot = cacheRoot,
            session = session,
            loader = loader,
            progressStore = progressStore
        )
    }

    private fun MangaDebugFixtureAssets.ensurePng(root = cacheRoot, 
        file: File,
        width: Int,
        height: Int,
        seed: Int
    ): File {
        if (isValidFixture(file, width, height)) return file
        file.delete()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val background = Paint().apply {
                color = Color.rgb(
                    40 + (seed * 37) % 160,
                    40 + (seed * 53) % 160,
                    40 + (seed * 71) % 160
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

            val marker = Paint().apply {
                color = Color.WHITE
                strokeWidth = 8f
                textSize = 42f
            }
            var y = 80
            var markerIndex = 0
            while (y < height) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), marker)
                canvas.drawText(
                    "Veil " + seed + " / " + markerIndex,
                    24f,
                    (y - 16).coerceAtLeast(48).toFloat(),
                    marker
                )
                y += 512
                markerIndex += 1
            }

            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.delete()
            temporary.outputStream().buffered().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Verification image compression failed"
                }
            }
            if (!temporary.renameTo(file)) {
                file.delete()
                check(temporary.renameTo(file)) {
                    "Verification image atomic replace failed"
                }
            }
        } finally {
            bitmap.recycle()
        }
        check(isValidFixture(file, width, height)) {
            "Verification image dimensions are invalid after write"
        }
        return file
    }

    private fun isValidFixture(
        file: File,
        expectedWidth: Int,
        expectedHeight: Int
    ): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth == expectedWidth && options.outHeight == expectedHeight
    }

    data class VerificationFixture(
        val cacheRoot: File,
        val session: MangaReaderSession,
        val loader: MangaChapterPresentationLoader,
        val progressStore: MangaProgressStore
    )

    companion object {
        const val EXTRA_EXTREME = "extreme"
        const val EXTRA_PARTIAL_OFFLINE = "partial_offline"
        const val EXTRA_RESET_PROGRESS = "reset_progress"
        const val EXTRA_DURABLE_PROGRESS = "durable_progress"
        const val EXTRA_START_PAGE = "start_page"

        private const val VERIFY_MANGA_ID = "verification-work"
        private const val VERIFY_SOURCE_ID = "verification.source"
    }
}

private object MangaVerificationDatabase {
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
                    "manga_reader_verification.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                database = created
                MangaRoomRepository(created)
            }
        }
    }
}
