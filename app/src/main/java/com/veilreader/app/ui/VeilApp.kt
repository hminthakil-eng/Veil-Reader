package com.veilreader.app.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.veilreader.app.R
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LibraryExport
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.manga.MangaLocalChapterMetadata
import com.veilreader.app.data.manga.MangaLocalChapterSummary
import com.veilreader.app.data.manga.MangaLocalImportCoordinator
import com.veilreader.app.data.manga.RoomMangaOfflineCacheIndex
import com.veilreader.app.data.manga.RoomMangaProgressStore
import com.veilreader.app.data.manga.RoomMangaSessionRepository
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.BookReturnRitual
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderHardwareKeyMap
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.deriveBookReturnRitual
import com.veilreader.app.domain.deriveLibraryMemoryState
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.manga.reader.presentation.MangaReaderChapterLoader
import com.veilreader.app.manga.reader.screen.MangaReaderIntegratedScreen
import com.veilreader.app.manga.reader.screen.MangaReaderSession
import com.veilreader.app.manga.reader.screen.MangaSessionAdapterResult
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.ui.navigation.VeilAppViewModel
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.reader.ReaderOpenResourceGuard
import com.veilreader.app.ui.screens.ArchiveScreen
import com.veilreader.app.ui.screens.ArrodesMirrorScreen
import com.veilreader.app.ui.screens.BookEntryStage
import com.veilreader.app.ui.screens.BookThresholdTransitionOverlay
import com.veilreader.app.ui.screens.CastleScreen
import com.veilreader.app.ui.screens.LibraryScreen
import com.veilreader.app.ui.screens.MangaHubScreen
import com.veilreader.app.ui.screens.ObservatoryScreen
import com.veilreader.app.ui.screens.PathScreen
import com.veilreader.app.ui.screens.ProfileScreen
import com.veilreader.app.ui.screens.ReaderScreen
import com.veilreader.app.ui.screens.ReadingNowScreen
import com.veilreader.app.ui.screens.SanctumScreen
import com.veilreader.app.ui.screens.SettingsScreen
import com.veilreader.app.ui.screens.TreasuryScreen
import com.veilreader.app.ui.sensory.VeilSensoryEvent
import com.veilreader.app.ui.sensory.VeilSensoryFeedback
import com.veilreader.app.ui.theme.VeilPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun VeilApp(
    externalOpenUri: Uri? = null,
    onExternalOpenUriConsumed: () -> Unit = {},
    appSettings: AppSettings = AppSettings(),
    onSetAppThemeMode: (AppThemeMode) -> Unit = {},
    onSetHighContrastEnabled: (Boolean) -> Unit = {},
    onSaveReaderAppearance: (ReaderAppearance) -> Unit = {},
    onSaveReaderTapGrid: (ReaderTapGrid) -> Unit = {},
    onSaveReaderHardwareKeys: (ReaderHardwareKeyMap) -> Unit = {},
    onSaveFixedLayoutSpread: (String, ReaderFixedLayoutSpread) -> Unit = { _, _ -> },
    onSaveSensorySettings: (SensorySettings) -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val activity = LocalActivity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sensory = remember(context) { VeilSensoryFeedback(context) }

    LaunchedEffect(appSettings.sensory) {
        sensory.update(appSettings.sensory)
    }
    DisposableEffect(sensory) {
        onDispose { sensory.dispose() }
    }
    val database = remember(context) { VeilDatabase.get(context) }
    val library = remember(context) { LocalLibraryRepository(context) }
    val game = remember(context) { GameRepository(context) }
    val readerEngine = remember(context) { ReadiumEngine(context) }
    val mangaImporter = remember(context, library, database) {
        MangaLocalImportCoordinator(context, library, database)
    }
    val mangaOfflineStore = remember(database) { RoomMangaOfflineCacheIndex(database) }
    val mangaProgressStore = remember(database) { RoomMangaProgressStore(database) }
    val mangaSessionRepository = remember(database) { RoomMangaSessionRepository(database) }
    val mangaLoader = remember(mangaOfflineStore) {
        MangaReaderChapterLoader(
            offlineIndex = mangaOfflineStore,
            sourceExecution = SourceExecutionCoordinator()
        )
    }
    val routeViewModel: VeilAppViewModel = viewModel()
    val route by routeViewModel.route.collectAsStateWithLifecycle()
    var openedPublication by remember { mutableStateOf<OpenedPublication?>(null) }
    var openedPublicationSessionId by remember { mutableStateOf<String?>(null) }
    var activeMangaSession by remember { mutableStateOf<MangaReaderSession?>(null) }
    var activeContinuity by remember { mutableStateOf<ReadingContinuitySummary?>(null) }
    var activeReturnRitual by remember { mutableStateOf<BookReturnRitual?>(null) }
    var activeReturnLocatorJson by remember { mutableStateOf<String?>(null) }

    // While either dedicated reader owns the screen, remove shell collectors from composition so
    // progress writes cannot invalidate the hidden app shell.
    val readerSurfaceActive = openedPublication != null || activeMangaSession != null
    val booksState = if (!readerSurfaceActive) {
        library.books.collectAsStateWithLifecycle()
    } else {
        null
    }
    val highlightsState = if (!readerSurfaceActive) {
        library.highlights.collectAsStateWithLifecycle()
    } else {
        null
    }
    val bookmarksState = if (!readerSurfaceActive) {
        library.bookmarks.collectAsStateWithLifecycle()
    } else {
        null
    }
    val readingSessionsState = if (!readerSurfaceActive) {
        library.readingSessions.collectAsStateWithLifecycle()
    } else {
        null
    }
    val readingCyclesState = if (!readerSurfaceActive) {
        library.readingCycles.collectAsStateWithLifecycle()
    } else {
        null
    }
    val passageVisitsState = if (!readerSurfaceActive) {
        library.passageVisits.collectAsStateWithLifecycle()
    } else {
        null
    }
    val readingMilestonesState = if (!readerSurfaceActive) {
        library.readingMilestones.collectAsStateWithLifecycle()
    } else {
        null
    }
    val books = booksState?.value.orEmpty()
    val highlights = highlightsState?.value.orEmpty()
    val bookmarks = bookmarksState?.value.orEmpty()
    val readingSessions = readingSessionsState?.value.orEmpty()
    val readingCycles = readingCyclesState?.value.orEmpty()
    val passageVisits = passageVisitsState?.value.orEmpty()
    val readingMilestones = readingMilestonesState?.value.orEmpty()
    LaunchedEffect(library) { game.syncExistingHighlights(library.highlights.value.size) }
    val completionEvidence = remember(books, readingCycles) {
        books.count { it.finished } to readingCycles.map { it.bookId }.distinct().size
    }
    LaunchedEffect(completionEvidence) {
        game.syncExistingBookCompletions(
            finishedBooks = completionEvidence.first,
            sealedBooks = completionEvidence.second
        )
    }

    // Existing libraries and restored backups may have no cached covers. Process one book at a time
    // so each Room update naturally advances this effect to the next pending publication.
    val nextCoverBook = books.firstOrNull {
        it.isImported &&
            it.coverCachePath == null &&
            it.format != BookFormat.COMIC
    }
    LaunchedEffect(nextCoverBook?.id) {
        val book = nextCoverBook ?: return@LaunchedEffect
        val cachedPath = readerEngine.extractAndCacheCover(book).getOrDefault("")
        library.updateCoverCachePath(book.id, cachedPath)
    }

    // Fingerprints are derived cache metadata too. Backfill old/restored books incrementally so
    // importing the same file later resolves to the existing record instead of making a duplicate.
    val nextFingerprintBook = books.firstOrNull { it.isImported && it.contentFingerprint.isNullOrBlank() }
    LaunchedEffect(nextFingerprintBook?.id, nextFingerprintBook?.sourceUri) {
        val book = nextFingerprintBook ?: return@LaunchedEffect
        readerEngine.computeContentFingerprint(book)
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { library.updateContentFingerprint(book.id, it) }
    }

    val profileState = if (!readerSurfaceActive) {
        game.profile.collectAsStateWithLifecycle()
    } else {
        null
    }
    val questsState = if (!readerSurfaceActive) {
        game.quests.collectAsStateWithLifecycle()
    } else {
        null
    }
    val dailyGoalState = if (!readerSurfaceActive) {
        game.dailyGoalMinutes.collectAsStateWithLifecycle()
    } else {
        null
    }
    val equippedSigilState = if (!readerSurfaceActive) {
        game.equippedSigil.collectAsStateWithLifecycle()
    } else {
        null
    }
    val castleTitleState = if (!readerSurfaceActive) {
        game.castleTitle.collectAsStateWithLifecycle()
    } else {
        null
    }
    val profile = profileState?.value
    val quests = questsState?.value.orEmpty()
    val dailyGoalMinutes = dailyGoalState?.value
    val equippedSigil = equippedSigilState?.value
    val castleTitle = castleTitleState?.value

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, sensory) {
        sensory.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        val sensoryObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> sensory.setForeground(true)
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> sensory.setForeground(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(sensoryObserver)
        onDispose {
            lifecycle.removeObserver(sensoryObserver)
            sensory.setForeground(false)
        }
    }

    DisposableEffect(lifecycle, game) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> game.refresh()
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> game.flushDurably()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            game.flushDurably()
            lifecycle.removeObserver(observer)
        }
    }

    var exporting by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var mangaMutationInProgress by remember { mutableStateOf(false) }
    var mangaStorageRevision by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<VeilNotice?>(null) }

    fun showNotice(resourceId: Int, kind: VeilNoticeKind = VeilNoticeKind.ERROR, vararg args: Any) {
        notice = VeilNotice(context.getString(resourceId, *args), kind)
    }

    fun exportData(uri: Uri, backup: Boolean) {
        if (exporting || restoring || isImporting || mangaMutationInProgress) return
        exporting = true
        scope.launch {
            try {
                val exporter = LibraryExport(context, library)
                if (backup) exporter.writeBackup(uri) else exporter.writeNotebook(uri)
                showNotice(
                    if (backup) R.string.notice_backup_exported else R.string.notice_notebook_exported,
                    VeilNoticeKind.SUCCESS
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showNotice(R.string.notice_export_failed)
            } finally {
                exporting = false
            }
        }
    }

    fun restoreData(uri: Uri) {
        if (restoring || exporting || isImporting || mangaMutationInProgress) return
        restoring = true
        scope.launch {
            try {
                val result = LibraryExport(context, library).restoreBackup(uri)
                if (activity != null) {
                    activity.recreate()
                } else {
                    showNotice(
                        R.string.notice_restore_success,
                        VeilNoticeKind.SUCCESS,
                        result.booksRestored,
                        result.highlightsRestored
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showNotice(R.string.notice_restore_failed)
            } finally {
                restoring = false
            }
        }
    }

    fun requestOpenBook(book: Book, locatorOverride: String? = null) {
        if (restoring || mangaMutationInProgress) return
        if (!book.isImported) {
            showNotice(R.string.notice_sample_no_file)
            return
        }
        routeViewModel.requestBook(
            bookId = book.id,
            locatorOverrideJson = locatorOverride.takeUnless { book.format == BookFormat.COMIC }
        )
    }

    fun deleteBook(book: Book) {
        if (!book.isImported || restoring || exporting || isImporting) return
        val mangaDelete = book.format == BookFormat.COMIC
        if (mangaDelete) {
            isImporting = true
            mangaMutationInProgress = true
        }
        scope.launch {
            try {
                val result = if (mangaDelete) {
                    mangaImporter.deleteImportedManga(book.id)
                } else {
                    runCatching { library.deleteImportedBook(book.id) }
                }

                val deleted = result.getOrNull()
                if (deleted != null) {
                    showNotice(
                        R.string.notice_book_deleted,
                        VeilNoticeKind.SUCCESS,
                        deleted.title
                    )
                } else {
                    showNotice(
                        R.string.notice_book_delete_failed,
                        VeilNoticeKind.WARNING
                    )
                }
            } finally {
                if (mangaDelete) {
                    mangaMutationInProgress = false
                    isImporting = false
                }
            }
        }
    }

    fun appendMangaChapters(book: Book, uris: List<Uri>) {
        if (
            isImporting ||
            restoring ||
            book.format != BookFormat.COMIC ||
            uris.isEmpty()
        ) return
        isImporting = true
        mangaMutationInProgress = true
        scope.launch {
            try {
                val result = mangaImporter.appendChapters(book.id, uris)
                val error = result.exceptionOrNull()
                if (error != null) {
                    if (error is CancellationException) throw error
                    showNotice(R.string.notice_manga_chapter_import_failed)
                    return@launch
                }

                val batch = result.getOrThrow()
                if (batch.addedCount > 0) {
                    mangaStorageRevision += 1
                }
                when {
                    batch.addedCount == 0 && batch.duplicateCount > 0 -> {
                        showNotice(
                            R.string.notice_manga_chapter_duplicate,
                            VeilNoticeKind.SUCCESS,
                            book.title
                        )
                    }
                    batch.duplicateCount > 0 -> {
                        showNotice(
                            R.string.notice_manga_chapters_added_with_duplicates,
                            VeilNoticeKind.SUCCESS,
                            batch.addedCount,
                            batch.duplicateCount,
                            book.title
                        )
                    }
                    else -> {
                        showNotice(
                            R.string.notice_manga_chapters_added,
                            VeilNoticeKind.SUCCESS,
                            batch.addedCount,
                            book.title
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showNotice(R.string.notice_manga_chapter_import_failed)
            } finally {
                mangaMutationInProgress = false
                isImporting = false
            }
        }
    }

    fun updateMangaChapterMetadata(
        book: Book,
        chapter: MangaLocalChapterSummary,
        metadata: MangaLocalChapterMetadata
    ) {
        if (isImporting || restoring || book.format != BookFormat.COMIC) return
        isImporting = true
        mangaMutationInProgress = true
        scope.launch {
            try {
                mangaImporter.updateChapterMetadata(
                    bookId = book.id,
                    chapterId = chapter.id,
                    metadata = metadata
                ).getOrThrow()
                mangaStorageRevision += 1
                showNotice(
                    R.string.notice_manga_chapter_metadata_updated,
                    VeilNoticeKind.SUCCESS
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showNotice(R.string.notice_manga_chapter_update_failed)
            } finally {
                mangaMutationInProgress = false
                isImporting = false
            }
        }
    }

    fun moveMangaChapter(
        book: Book,
        chapter: MangaLocalChapterSummary,
        direction: Int
    ) {
        if (isImporting || restoring || book.format != BookFormat.COMIC) return
        isImporting = true
        mangaMutationInProgress = true
        scope.launch {
            try {
                mangaImporter.moveChapter(
                    bookId = book.id,
                    chapterId = chapter.id,
                    direction = direction
                ).getOrThrow()
                mangaStorageRevision += 1
                showNotice(
                    R.string.notice_manga_chapter_reordered,
                    VeilNoticeKind.SUCCESS
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showNotice(R.string.notice_manga_chapter_update_failed)
            } finally {
                mangaMutationInProgress = false
                isImporting = false
            }
        }
    }

    fun deleteMangaChapter(
        book: Book,
        chapter: MangaLocalChapterSummary
    ) {
        if (
            isImporting ||
            restoring ||
            book.format != BookFormat.COMIC ||
            chapter.isPrimary
        ) return
        isImporting = true
        mangaMutationInProgress = true
        scope.launch {
            try {
                mangaImporter.deleteChapter(book.id, chapter.id).getOrThrow()
                mangaStorageRevision += 1
                showNotice(
                    R.string.notice_manga_chapter_deleted,
                    VeilNoticeKind.SUCCESS
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showNotice(R.string.notice_manga_chapter_update_failed)
            } finally {
                mangaMutationInProgress = false
                isImporting = false
            }
        }
    }

    fun clearMangaDerivedCache(book: Book) {
        if (isImporting || restoring || book.format != BookFormat.COMIC) return
        isImporting = true
        mangaMutationInProgress = true
        scope.launch {
            try {
                val result = mangaImporter.clearDerivedCache(book.id)
                if (result.isFailure) {
                    showNotice(R.string.notice_manga_cache_clear_failed)
                } else {
                    mangaStorageRevision += 1
                    showNotice(
                        R.string.notice_manga_cache_cleared,
                        VeilNoticeKind.SUCCESS
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showNotice(R.string.notice_manga_cache_clear_failed)
            } finally {
                mangaMutationInProgress = false
                isImporting = false
            }
        }
    }

    fun importBook(uri: Uri) {
        if (isImporting || restoring || exporting) return
        isImporting = true
        scope.launch {
            try {
                val commit = if (mangaImporter.canImport(uri)) {
                    val imported = mangaImporter.import(uri)
                    val importError = imported.exceptionOrNull()
                    if (importError != null) {
                        if (importError is CancellationException) throw importError
                        showNotice(R.string.notice_import_failed)
                        return@launch
                    }
                    imported.getOrThrow()
                } else {
                    val inspected = readerEngine.inspectAndCreateBook(uri)
                    val inspectionError = inspected.exceptionOrNull()
                    if (inspectionError != null) {
                        if (inspectionError is CancellationException) throw inspectionError
                        showNotice(R.string.notice_import_failed)
                        return@launch
                    }
                    library.addImportedBook(inspected.getOrThrow())
                }

                routeViewModel.selectTab(VeilTab.LIBRARY)
                requestOpenBook(commit.book)
                if (commit.duplicate) {
                    showNotice(
                        R.string.notice_duplicate_book,
                        VeilNoticeKind.SUCCESS,
                        commit.book.title
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showNotice(R.string.notice_import_failed)
            } finally {
                isImporting = false
            }
        }
    }

    val targetBook = route.activeBookId?.let { id -> books.firstOrNull { it.id == id } }
    LaunchedEffect(
        route.activeBookId,
        route.readerSessionInstanceId,
        route.locatorOverrideJson,
        targetBook?.id,
        openedPublication?.book?.id,
        openedPublicationSessionId,
        restoring
    ) {
        val targetId = route.activeBookId ?: return@LaunchedEffect
        val openRequestId = route.readerSessionInstanceId ?: return@LaunchedEffect
        if (restoring) return@LaunchedEffect

        val currentlyOpened = openedPublication
        if (currentlyOpened != null) {
            val ownsCurrentRequest =
                currentlyOpened.book.id == targetId &&
                    openedPublicationSessionId == openRequestId
            if (ownsCurrentRequest) return@LaunchedEffect

            // Reader ownership is one open request, not merely one book id. Tear down the old
            // publication first so its fragment, publication resources and Reader session cannot
            // leak into a fresh request for the same title.
            openedPublication = null
            openedPublicationSessionId = null
            return@LaunchedEffect
        }

        activeContinuity = null
        activeReturnRitual = null
        activeReturnLocatorJson = null

        val book = library.getBook(targetId) ?: targetBook ?: return@LaunchedEffect
        if (!book.isImported) {
            routeViewModel.bookOpenFailed(targetId, openRequestId)
            showNotice(R.string.notice_book_file_missing)
            return@LaunchedEffect
        }

        if (book.format == BookFormat.COMIC) {
            if (activeMangaSession?.instanceId == openRequestId) {
                return@LaunchedEffect
            }
            val repaired = mangaImporter.ensureLocalCache(targetId)
            if (repaired.isFailure) {
                routeViewModel.bookOpenFailed(targetId, openRequestId)
                showNotice(
                    R.string.notice_manga_cache_repair_failed,
                    VeilNoticeKind.WARNING
                )
                return@LaunchedEffect
            }
            if (repaired.getOrDefault(0) > 0) {
                mangaStorageRevision += 1
            }
            when (
                val result = mangaSessionRepository.build(
                    bookId = targetId,
                    instanceId = openRequestId
                )
            ) {
                is MangaSessionAdapterResult.Ready -> {
                    val currentRoute = routeViewModel.route.value
                    if (
                        currentRoute.activeBookId != targetId ||
                        currentRoute.readerSessionInstanceId != openRequestId
                    ) {
                        return@LaunchedEffect
                    }
                    activeMangaSession = result.session
                    library.markOpened(targetId)
                    routeViewModel.readerOpened(targetId, openRequestId)
                }
                is MangaSessionAdapterResult.Unavailable -> {
                    routeViewModel.bookOpenFailed(targetId, openRequestId)
                    showNotice(
                        R.string.notice_manga_open_failed,
                        VeilNoticeKind.WARNING
                    )
                }
            }
            return@LaunchedEffect
        }

        val locatorOverride = route.locatorOverrideJson
        val readerCheckpoint = route.readerLocatorCheckpointJson

        activeReturnLocatorJson = locatorOverride?.let { requested ->
            book.locatorJson
                ?.takeIf { it.isNotBlank() && it != requested }
        }
        activeContinuity = runCatching {
            library.readingContinuity(book)
        }.getOrNull()

        val ritualNow = System.currentTimeMillis()
        val archiveMemory = deriveLibraryMemoryState(
            books = listOf(book),
            highlights = highlights.filter { it.bookId == book.id },
            sessions = readingSessions,
            nowEpochMs = ritualNow
        ).memoryFor(book.id)
        activeReturnRitual = deriveBookReturnRitual(
            book = book,
            archiveMemory = archiveMemory,
            highlights = highlights,
            nowEpochMs = ritualNow
        )

        val initialLocatorJson = com.veilreader.app.ui.navigation.chooseReaderRestoreLocator(
            explicitOverrideJson = locatorOverride,
            readerCheckpointJson = readerCheckpoint,
            durableLocatorJson = book.locatorJson
        )
        val candidate =
            if (initialLocatorJson == book.locatorJson) book
            else book.copy(locatorJson = initialLocatorJson)
        val opened = readerEngine.openBook(
            book = candidate,
            persistedLocatorJsons = library.locatorJsonsForBook(targetId)
        ).fold(
            onSuccess = { it },
            onFailure = { error ->
                if (error is CancellationException) throw error
                routeViewModel.bookOpenFailed(targetId, openRequestId)
                showNotice(R.string.notice_open_failed)
                return@LaunchedEffect
            }
        )

        val publicationGuard = ReaderOpenResourceGuard(opened)
        try {
            val routeAfterOpen = routeViewModel.route.value
            if (
                routeAfterOpen.activeBookId != targetId ||
                routeAfterOpen.readerSessionInstanceId != openRequestId
            ) {
                return@LaunchedEffect
            }

            try {
                library.applyPdfiumLocatorMigrations(targetId, opened.locatorMigrations)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showNotice(R.string.notice_pdf_migration_failed, VeilNoticeKind.WARNING)
            }

            val routeBeforeCommit = routeViewModel.route.value
            if (
                routeBeforeCommit.activeBookId != targetId ||
                routeBeforeCommit.readerSessionInstanceId != openRequestId
            ) {
                return@LaunchedEffect
            }

            val recoveryLocator = locatorOverride ?: readerCheckpoint
            if (recoveryLocator != null) {
                val persistedLocator = opened.initialLocator?.toJSON()?.toString() ?: recoveryLocator
                val recoveredProgress =
                    opened.initialLocator?.locations?.totalProgression ?: book.progress.toDouble()
                val recoveryOutcome = library.saveReaderOpenRecoveryProgress(
                    bookId = targetId,
                    sessionId = openRequestId,
                    progression = recoveredProgress,
                    locatorJson = persistedLocator
                )
                if (!recoveryOutcome.accepted) {
                    routeViewModel.bookOpenFailed(targetId, openRequestId)
                    showNotice(
                        if (locatorOverride != null) {
                            R.string.notice_position_save_failed
                        } else {
                            R.string.notice_checkpoint_save_failed
                        },
                        VeilNoticeKind.WARNING
                    )
                    return@LaunchedEffect
                }
            }
            library.markOpened(targetId)
            if (locatorOverride != null) {
                library.recordPassageVisitForLocator(
                    bookId = targetId,
                    locatorJson = locatorOverride
                )
            }

            openedPublicationSessionId = openRequestId
            openedPublication = publicationGuard.transfer()
            if (activeReturnRitual != null) {
                sensory.perform(view, VeilSensoryEvent.RETURN_RITUAL)
            }

            when {
                locatorOverride != null -> {
                    scope.launch {
                        try {
                            library.flushWrites()
                            val currentRoute = routeViewModel.route.value
                            if (
                                currentRoute.activeBookId == targetId &&
                                currentRoute.readerSessionInstanceId == openRequestId &&
                                currentRoute.locatorOverrideJson == locatorOverride
                            ) {
                                routeViewModel.readerOpened(targetId, openRequestId)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            val currentRoute = routeViewModel.route.value
                            if (
                                currentRoute.activeBookId == targetId &&
                                currentRoute.readerSessionInstanceId == openRequestId
                            ) {
                                showNotice(
                                    R.string.notice_position_save_failed,
                                    VeilNoticeKind.WARNING
                                )
                            }
                        }
                    }
                }

                readerCheckpoint != null -> {
                    scope.launch {
                        try {
                            library.flushWrites()
                            routeViewModel.readerCheckpointPersisted(
                                targetId,
                                openRequestId,
                                readerCheckpoint
                            )
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            val currentRoute = routeViewModel.route.value
                            if (
                                currentRoute.activeBookId == targetId &&
                                currentRoute.readerSessionInstanceId == openRequestId
                            ) {
                                showNotice(
                                    R.string.notice_checkpoint_save_failed,
                                    VeilNoticeKind.WARNING
                                )
                            }
                        }
                    }
                }

                else -> routeViewModel.readerOpened(targetId, openRequestId)
            }
        } finally {
            publicationGuard.closeIfUntransferred()
        }
    }

    LaunchedEffect(externalOpenUri) {
        val uri = externalOpenUri ?: return@LaunchedEffect
        importBook(uri)
        onExternalOpenUriConsumed()
    }

    val mainContent: @Composable (VeilTab) -> Unit = { tab ->
        when (tab) {
            VeilTab.READING -> ReadingNowScreen(
                books = books,
                profile = requireNotNull(profile),
                quests = quests,
                highlights = highlights,
                bookmarks = bookmarks,
                readingSessions = readingSessions,
                onOpenBook = { requestOpenBook(it) },
                onOpenPassage = { book, locator ->
                    requestOpenBook(book, locator)
                },
                onOpenLibrary = { routeViewModel.selectTab(VeilTab.LIBRARY) },
                onOpenCastle = { routeViewModel.selectTab(VeilTab.CASTLE) },
                onOpenMirror = { routeViewModel.openChamber("mirror") }
            )

            VeilTab.LIBRARY -> LibraryScreen(
                books = books,
                highlights = highlights,
                bookmarks = bookmarks,
                readingSessions = readingSessions,
                readingCycles = readingCycles,
                readingMilestones = readingMilestones,
                isImporting = isImporting,
                onImportUri = ::importBook,
                onOpenBook = { requestOpenBook(it) },
                onFavorite = library::toggleFavorite,
                onEditMetadata = library::editMetadata,
                onDeleteBook = ::deleteBook,
                onOpenSettings = routeViewModel::openSettings,
                onOpenManga = { routeViewModel.openChamber("manga") }
            )

            VeilTab.CASTLE -> CastleScreen(
                profile = requireNotNull(profile),
                quests = quests,
                onOpenRoom = { room ->
                    when (room) {
                        "library" -> routeViewModel.selectTab(VeilTab.LIBRARY)
                        "ritual" -> routeViewModel.selectTab(VeilTab.PATH)
                        "observatory" -> routeViewModel.openChamber(room)
                        "archive" -> routeViewModel.openArchive()
                        "treasury", "sanctum" -> routeViewModel.openChamber(room)
                    }
                },
                onAdvanceRank = {
                    val currentProfile = requireNotNull(profile)
                    if (!game.advanceRank(currentProfile.path.id, currentProfile.rankIndex)) {
                        showNotice(R.string.notice_ritual_required)
                    } else {
                        sensory.perform(view, VeilSensoryEvent.ADVANCEMENT)
                    }
                },
                books = books,
                highlights = highlights,
                bookmarks = bookmarks,
                readingSessions = readingSessions,
                readingCycles = readingCycles
            )

            VeilTab.PATH -> PathScreen(
                profile = requireNotNull(profile),
                onAdvanceRank = {
                    val currentProfile = requireNotNull(profile)
                    if (!game.advanceRank(currentProfile.path.id, currentProfile.rankIndex)) {
                        showNotice(R.string.notice_ritual_required)
                    } else {
                        sensory.perform(view, VeilSensoryEvent.ADVANCEMENT)
                    }
                },
                onChoosePath = { pathId ->
                    if (!game.choosePath(pathId)) {
                        showNotice(R.string.notice_path_sealed)
                    }
                }
            )

            VeilTab.PROFILE -> ProfileScreen(
                profile = requireNotNull(profile),
                highlightCount = highlights.size,
                dailyGoalMinutes = requireNotNull(dailyGoalMinutes),
                castleTitle = requireNotNull(castleTitle),
                equippedSigilId = equippedSigil,
                books = books,
                readingSessions = readingSessions,
                readingCycles = readingCycles,
                onSetDailyGoal = game::setDailyGoal,
                onOpenArchive = routeViewModel::openArchive,
                onOpenSettings = routeViewModel::openSettings
            )
        }
    }

    val opened = openedPublication
    val openedSessionId = openedPublicationSessionId
    val mangaSession = activeMangaSession
    Box(Modifier.fillMaxSize()) {
        if (mangaSession != null) {
            MangaReaderIntegratedScreen(
                session = mangaSession,
                loader = mangaLoader,
                progressStore = mangaProgressStore,
                cacheRoot = mangaImporter.cacheRoot,
                onClose = {
                    if (activeMangaSession?.instanceId == mangaSession.instanceId) {
                        activeMangaSession = null
                    }
                    routeViewModel.closeReader(mangaSession.instanceId)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (opened != null && openedSessionId != null) {
        ReaderScreen(
            opened = opened,
            readerSessionInstanceId = openedSessionId,
            library = library,
            game = game,
            readerAppearance = appSettings.readerAppearance,
            readerTapGrid = appSettings.readerTapGrid,
            readerHardwareKeys = appSettings.readerHardwareKeys,
            fixedLayoutSpread = appSettings.fixedLayoutSpreads[opened.book.id]
                ?: ReaderFixedLayoutSpread.AUTO,
            onReaderAppearanceChange = onSaveReaderAppearance,
            onFixedLayoutSpreadChange = { mode ->
                onSaveFixedLayoutSpread(opened.book.id, mode)
            },
            entryContinuity = activeContinuity,
            returnRitual = activeReturnRitual,
            initialReturnLocatorJson = activeReturnLocatorJson,
            onSensoryEvent = { event -> sensory.perform(view, event) },
            onClose = {
                if (openedPublicationSessionId == openedSessionId) {
                    openedPublication = null
                    openedPublicationSessionId = null
                    activeContinuity = null
                    activeReturnRitual = null
                    activeReturnLocatorJson = null
                }
                routeViewModel.closeReader(openedSessionId)
            },
            onLocatorCheckpoint = { locatorJson ->
                routeViewModel.checkpointReaderLocator(
                    bookId = opened.book.id,
                    sessionInstanceId = openedSessionId,
                    locatorJson = locatorJson
                )
            }
        )
    } else if (route.showSettings) {
        VeilWorldBackdrop {
            SettingsScreen(
                settings = appSettings,
                exporting = exporting,
                restoring = restoring,
                onSetAppThemeMode = onSetAppThemeMode,
                onSetHighContrastEnabled = onSetHighContrastEnabled,
                onSaveReaderAppearance = onSaveReaderAppearance,
                onSaveReaderTapGrid = onSaveReaderTapGrid,
                onSaveReaderHardwareKeys = onSaveReaderHardwareKeys,
                onSaveSensorySettings = onSaveSensorySettings,
                onExportBackup = { exportData(it, true) },
                onRestoreBackup = ::restoreData,
                onExportNotes = { exportData(it, false) },
                onClose = routeViewModel::closeSettings
            )
        }
    } else if (route.showArchive) {
        ArchiveScreen(
            books = books,
            highlights = highlights,
            bookmarks = bookmarks,
            readingSessions = readingSessions,
            readingCycles = readingCycles,
            passageVisits = passageVisits,
            onClose = routeViewModel::closeArchive,
            onOpenPassage = { book, locator -> requestOpenBook(book, locator) },
            onSaveNote = { id, note ->
                library.updateHighlightNote(id, note)
                scope.launch {
                    try {
                        library.flushWrites()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        showNotice(R.string.notice_note_save_failed, VeilNoticeKind.WARNING)
                    }
                }
            },
            onDeleteHighlight = library::deleteHighlight,
            onDeleteBookmark = library::deleteBookmark
        )
    } else if (route.activeChamber == "mirror") {
        ArrodesMirrorScreen(
            books = books,
            highlights = highlights,
            passageVisits = passageVisits,
            onOpenSource = { fragment ->
                requestOpenBook(fragment.book, fragment.locatorJson)
            },
            onClose = routeViewModel::closeChamber
        )
    } else if (route.activeChamber == "manga") {
        MangaHubScreen(
            books = books,
            onOpenBook = ::requestOpenBook,
            onAddChapterUris = ::appendMangaChapters,
            storageSummaryProvider = { book -> mangaImporter.storageSummary(book.id) },
            chapterSummaryProvider = { book -> mangaImporter.listChapterSummaries(book.id) },
            onUpdateChapterMetadata = ::updateMangaChapterMetadata,
            onMoveChapter = ::moveMangaChapter,
            onDeleteChapter = ::deleteMangaChapter,
            onClearDerivedCache = ::clearMangaDerivedCache,
            storageRevision = mangaStorageRevision,
            onOpenLibrary = { routeViewModel.selectTab(VeilTab.LIBRARY) },
            onClose = routeViewModel::closeChamber,
            isImporting = isImporting
        )
    } else if (
        profile == null ||
        dailyGoalMinutes == null ||
        castleTitle == null
    ) {
        VeilWorldBackdrop {
            VeilLoadingState(
                label = stringResource(
                    if (restoring) R.string.notice_loading_restore else R.string.notice_loading_open
                )
            )
        }
    } else if (route.activeChamber == "observatory") {
        ObservatoryScreen(
            books = books,
            highlights = highlights,
            readingSessions = readingSessions,
            onOpenBook = { book -> requestOpenBook(book) },
            onClose = routeViewModel::closeChamber
        )
    } else if (route.activeChamber == "treasury") {
        TreasuryScreen(
            profile = requireNotNull(profile),
            equippedSigil = equippedSigil,
            onEquip = { id ->
                if (!game.equipSigil(id)) {
                    showNotice(R.string.notice_sigil_sealed)
                } else {
                    sensory.perform(view, VeilSensoryEvent.RELIC)
                }
            },
            onClose = routeViewModel::closeChamber
        )
    } else if (route.activeChamber == "sanctum") {
        SanctumScreen(
            profile = requireNotNull(profile),
            castleTitle = requireNotNull(castleTitle),
            availableTitles = game.availableCastleTitles(),
            onSelectTitle = { title ->
                if (!game.selectCastleTitle(title)) {
                    showNotice(R.string.notice_title_sealed)
                } else {
                    sensory.perform(view, VeilSensoryEvent.RELIC)
                }
            },
            onClose = routeViewModel::closeChamber
        )
    } else {
        val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

        // Prefer the branded rail once there is enough persistent horizontal space, but keep the
        // compact dock on short landscape windows where a rail would compete with reading content.
        val useRail = shouldUseNavigationRail(windowSizeClass)
        val contentMaxWidth = contentMaxWidthDp(windowSizeClass).dp

        VeilWorldBackdrop {
            if (useRail) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    VeilNavigationRail(
                        selected = route.selectedTab,
                        onSelect = routeViewModel::selectTab
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        VeilAnimatedTabHost(
                            selectedTab = route.selectedTab,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .widthIn(max = contentMaxWidth)
                        ) { tab ->
                            mainContent(tab)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    VeilAnimatedTabHost(
                        selectedTab = route.selectedTab,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) { tab ->
                        mainContent(tab)
                    }
                    VeilBottomDock(
                        selected = route.selectedTab,
                        onSelect = routeViewModel::selectTab,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        val pendingEntryBook =
            if (opened == null && route.activeBookId != null) targetBook else null
        pendingEntryBook?.let { book ->
            BookThresholdTransitionOverlay(
                book = book,
                stage = BookEntryStage.PREPARING,
                visible = true,
                continuity = activeContinuity,
                returnRitual = activeReturnRitual,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    notice?.let { currentNotice ->
        val eyebrow = when (currentNotice.kind) {
            VeilNoticeKind.SUCCESS -> R.string.notice_success_eyebrow
            VeilNoticeKind.WARNING -> R.string.notice_warning_eyebrow
            VeilNoticeKind.ERROR -> R.string.notice_error_eyebrow
        }
        val heading = when (currentNotice.kind) {
            VeilNoticeKind.SUCCESS -> R.string.notice_success_title
            VeilNoticeKind.WARNING -> R.string.notice_warning_title
            VeilNoticeKind.ERROR -> R.string.notice_error_title
        }
        VeilNoticeDialog(
            eyebrow = stringResource(eyebrow),
            title = stringResource(heading),
            message = currentNotice.message,
            actionLabel = stringResource(R.string.notice_return),
            onDismiss = { notice = null }
        )
    }
}
}

@Composable
private fun VeilNoticeDialog(
    eyebrow: String,
    title: String,
    message: String,
    actionLabel: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.medium,
                color = VeilPalette.Archive,
                border = BorderStroke(
                    1.dp,
                    VeilPalette.Brass.copy(alpha = 0.46f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        eyebrow,
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                        color = VeilPalette.Brass.copy(alpha = 0.24f)
                    ) {}
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

private enum class VeilNoticeKind { SUCCESS, WARNING, ERROR }

private data class VeilNotice(val message: String, val kind: VeilNoticeKind)

internal fun shouldUseNavigationRail(windowSizeClass: WindowSizeClass): Boolean =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

internal fun contentMaxWidthDp(windowSizeClass: WindowSizeClass): Int =
    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        1280
    } else {
        1040
    }



