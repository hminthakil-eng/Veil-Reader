package com.veilreader.app.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookReturnRitual
import com.veilreader.app.domain.PerformanceTier
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderAppearanceScope
import com.veilreader.app.domain.deriveBookReturnRitual
import com.veilreader.app.domain.deriveLibraryMemoryState
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.ui.navigation.VeilAppViewModel
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.screens.ArchiveScreen
import com.veilreader.app.ui.screens.BookEntryStage
import com.veilreader.app.ui.screens.BookThresholdTransitionOverlay
import com.veilreader.app.ui.screens.CastleScreen
import com.veilreader.app.ui.screens.LibraryScreen
import com.veilreader.app.ui.screens.ObservatoryScreen
import com.veilreader.app.ui.screens.PathScreen
import com.veilreader.app.ui.screens.ProfileScreen
import com.veilreader.app.ui.screens.ReaderScreen
import com.veilreader.app.ui.screens.ReadingNowScreen
import com.veilreader.app.ui.screens.SanctumScreen
import com.veilreader.app.ui.screens.SettingsScreen
import com.veilreader.app.ui.screens.TreasuryScreen
import com.veilreader.app.ui.screens.localizedSigilName
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
    onSetPerformanceTier: (PerformanceTier) -> Unit = {},
    onSaveReaderAppearance: (ReaderAppearance) -> Unit = {},
    onSaveBookReaderAppearance: (String, ReaderAppearance) -> Unit = { _, _ -> },
    onClearBookReaderAppearance: (String) -> Unit = {},
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
    val library = remember(context) { LocalLibraryRepository(context) }
    val game = remember(context) { GameRepository(context) }
    val readerEngine = remember(context) { ReadiumEngine(context) }
    val routeViewModel: VeilAppViewModel = viewModel()
    val route by routeViewModel.route.collectAsStateWithLifecycle()
    var openedPublication by remember { mutableStateOf<OpenedPublication?>(null) }
    var activeContinuity by remember { mutableStateOf<ReadingContinuitySummary?>(null) }
    var activeReturnRitual by remember { mutableStateOf<BookReturnRitual?>(null) }
    var activeReturnLocatorJson by remember { mutableStateOf<String?>(null) }

    // While Readium owns the screen, remove these collectors from composition entirely so
    // progress/game writes cannot invalidate the app shell. StateFlow immediately supplies its
    // latest value when these collectors re-enter after the reader closes.
    val booksState = if (openedPublication == null) {
        library.books.collectAsStateWithLifecycle()
    } else {
        null
    }
    val highlightsState = if (openedPublication == null) {
        library.highlights.collectAsStateWithLifecycle()
    } else {
        null
    }
    val bookmarksState = if (openedPublication == null) {
        library.bookmarks.collectAsStateWithLifecycle()
    } else {
        null
    }
    val readingSessionsState = if (openedPublication == null) {
        library.readingSessions.collectAsStateWithLifecycle()
    } else {
        null
    }
    val readingCyclesState = if (openedPublication == null) {
        library.readingCycles.collectAsStateWithLifecycle()
    } else {
        null
    }
    val passageVisitsState = if (openedPublication == null) {
        library.passageVisits.collectAsStateWithLifecycle()
    } else {
        null
    }
    val readingMilestonesState = if (openedPublication == null) {
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

    // Existing libraries and restored backups may have no cached covers. Process one book at a time
    // so each Room update naturally advances this effect to the next pending publication.
    val nextCoverBook = books.firstOrNull { it.isImported && it.coverCachePath == null }
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

    val profileState = if (openedPublication == null) {
        game.profile.collectAsStateWithLifecycle()
    } else {
        null
    }
    val questsState = if (openedPublication == null) {
        game.quests.collectAsStateWithLifecycle()
    } else {
        null
    }
    val dailyGoalState = if (openedPublication == null) {
        game.dailyGoalMinutes.collectAsStateWithLifecycle()
    } else {
        null
    }
    val equippedSigilState = if (openedPublication == null) {
        game.equippedSigil.collectAsStateWithLifecycle()
    } else {
        null
    }
    val castleTitleState = if (openedPublication == null) {
        game.castleTitle.collectAsStateWithLifecycle()
    } else {
        null
    }
    val revealedDiscoveriesState = if (openedPublication == null) {
        game.revealedDiscoveries.collectAsStateWithLifecycle()
    } else {
        null
    }
    val profile = profileState?.value
    val quests = questsState?.value.orEmpty()
    val dailyGoalMinutes = dailyGoalState?.value
    val equippedSigil = equippedSigilState?.value
    val castleTitle = castleTitleState?.value
    val revealedDiscoveries = revealedDiscoveriesState?.value.orEmpty()

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

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) game.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    var exporting by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<VeilNotice?>(null) }

    fun exportData(uri: Uri, backup: Boolean) {
        if (exporting || restoring) return
        exporting = true
        scope.launch {
            try {
                val exporter = LibraryExport(context, library)
                if (backup) exporter.writeBackup(uri) else exporter.writeNotebook(uri)
                notice = VeilNotice(
                    messageRes = if (backup) R.string.notice_backup_exported else R.string.notice_notebook_exported,
                    tone = VeilNoticeTone.INFO
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReaderTrace.event("export_failed", details = "type=${error::class.java.simpleName}")
                notice = VeilNotice(R.string.notice_export_failed)
            } finally {
                exporting = false
            }
        }
    }

    fun restoreData(uri: Uri) {
        if (restoring || exporting) return
        restoring = true
        scope.launch {
            try {
                val result = LibraryExport(context, library).restoreBackup(uri)
                if (activity != null) {
                    activity.recreate()
                } else {
                    notice = VeilNotice(
                        messageRes = R.string.notice_restore_success,
                        formatArgs = listOf(result.booksRestored, result.highlightsRestored),
                        tone = VeilNoticeTone.INFO
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReaderTrace.event("restore_failed", details = "type=${error::class.java.simpleName}")
                notice = VeilNotice(R.string.notice_restore_failed)
            } finally {
                restoring = false
            }
        }
    }

    fun requestOpenBook(book: Book, locatorOverride: String? = null) {
        if (restoring) return
        if (!book.isImported) {
            notice = VeilNotice(R.string.notice_sample_no_source)
            return
        }
        routeViewModel.requestBook(book.id, locatorOverride)
    }

    fun importBook(uri: Uri) {
        if (isImporting || restoring) return
        isImporting = true
        scope.launch {
            try {
                val inspected = readerEngine.inspectAndCreateBook(uri)
                val inspectionError = inspected.exceptionOrNull()
                if (inspectionError != null) {
                    if (inspectionError is CancellationException) throw inspectionError
                    ReaderTrace.event("import_inspection_failed", details = "type=${inspectionError::class.java.simpleName}")
                    notice = VeilNotice(R.string.notice_import_failed)
                    return@launch
                }

                val commit = library.addImportedBook(inspected.getOrThrow())
                routeViewModel.selectTab(VeilTab.LIBRARY)
                routeViewModel.requestBook(commit.book.id)
                if (commit.duplicate) {
                    notice = VeilNotice(
                        messageRes = R.string.notice_duplicate_opened,
                        formatArgs = listOf(commit.book.title),
                        tone = VeilNoticeTone.INFO
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReaderTrace.event("import_failed", details = "type=${error::class.java.simpleName}")
                notice = VeilNotice(R.string.notice_import_failed)
            } finally {
                isImporting = false
            }
        }
    }

    val targetBook = route.activeBookId?.let { id -> books.firstOrNull { it.id == id } }
    LaunchedEffect(
        route.activeBookId,
        route.locatorOverrideJson,
        targetBook?.id,
        openedPublication?.book?.id,
        restoring
    ) {
        val targetId = route.activeBookId ?: return@LaunchedEffect
        if (restoring) return@LaunchedEffect

        val currentlyOpened = openedPublication
        if (currentlyOpened != null) {
            if (currentlyOpened.book.id == targetId) return@LaunchedEffect
            openedPublication = null
            return@LaunchedEffect
        }

        activeContinuity = null
        activeReturnRitual = null
        activeReturnLocatorJson = null

        val book = library.getBook(targetId) ?: targetBook ?: return@LaunchedEffect
        if (!book.isImported) {
            routeViewModel.bookOpenFailed(targetId)
            notice = VeilNotice(R.string.notice_local_file_missing)
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
                routeViewModel.bookOpenFailed(targetId)
                ReaderTrace.event("book_open_failed", bookId = targetId, details = "type=${error::class.java.simpleName}")
                notice = VeilNotice(R.string.notice_book_open_failed)
                return@LaunchedEffect
            }
        )

        if (routeViewModel.route.value.activeBookId != targetId) {
            opened.close()
            return@LaunchedEffect
        }

        try {
            library.applyPdfiumLocatorMigrations(targetId, opened.locatorMigrations)
        } catch (cancelled: CancellationException) {
            opened.close()
            throw cancelled
        } catch (error: Exception) {
            ReaderTrace.event("pdf_locator_migration_failed", bookId = targetId, details = "type=${error::class.java.simpleName}")
            notice = VeilNotice(R.string.notice_pdf_position_upgrade_failed)
        }

        val recoveryLocator = locatorOverride ?: readerCheckpoint
        if (recoveryLocator != null) {
            val persistedLocator = opened.initialLocator?.toJSON()?.toString() ?: recoveryLocator
            val recoveredProgress =
                opened.initialLocator?.locations?.totalProgression ?: book.progress.toDouble()
            library.saveProgress(targetId, recoveredProgress, persistedLocator)
        }
        library.markOpened(targetId)
        if (locatorOverride != null) {
            library.recordPassageVisitForLocator(
                bookId = targetId,
                locatorJson = locatorOverride
            )
        }
        openedPublication = opened
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
                            currentRoute.locatorOverrideJson == locatorOverride
                        ) {
                            routeViewModel.readerOpened(targetId)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        ReaderTrace.event("requested_position_durability_failed", bookId = targetId, details = "type=${error::class.java.simpleName}")
                        notice = VeilNotice(R.string.notice_position_not_durable)
                    }
                }
            }

            readerCheckpoint != null -> {
                scope.launch {
                    try {
                        library.flushWrites()
                        routeViewModel.readerCheckpointPersisted(targetId, readerCheckpoint)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        ReaderTrace.event("restored_position_durability_failed", bookId = targetId, details = "type=${error::class.java.simpleName}")
                        notice = VeilNotice(R.string.notice_restored_position_not_durable)
                    }
                }
            }

            else -> routeViewModel.readerOpened(targetId)
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
                onOpenCastle = { routeViewModel.selectTab(VeilTab.CASTLE) }
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
                onOpenSettings = routeViewModel::openSettings
            )

            VeilTab.CASTLE -> CastleScreen(
                profile = requireNotNull(profile),
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
                    if (!game.advanceRank()) {
                        notice = VeilNotice(R.string.notice_ritual_incomplete)
                    } else {
                        sensory.perform(view, VeilSensoryEvent.ADVANCEMENT)
                    }
                },
                books = books,
                highlights = highlights,
                bookmarks = bookmarks,
                readingSessions = readingSessions
            )

            VeilTab.PATH -> PathScreen(
                profile = requireNotNull(profile),
                onAdvanceRank = {
                    if (!game.advanceRank()) {
                        notice = VeilNotice(R.string.notice_ritual_incomplete)
                    } else {
                        sensory.perform(view, VeilSensoryEvent.ADVANCEMENT)
                    }
                },
                onChoosePath = { pathId ->
                    if (!game.choosePath(pathId)) {
                        notice = VeilNotice(R.string.notice_path_sealed)
                    }
                }
            )

            VeilTab.PROFILE -> ProfileScreen(
                profile = requireNotNull(profile),
                highlightCount = highlights.size,
                dailyGoalMinutes = requireNotNull(dailyGoalMinutes),
                castleTitle = requireNotNull(castleTitle),
                equippedSigilName = equippedSigil?.let { localizedSigilName(it) },
                books = books,
                readingSessions = readingSessions,
                readingCycles = readingCycles,
                revealedDiscoveryIds = revealedDiscoveries,
                onSetDailyGoal = game::setDailyGoal,
                onOpenArchive = routeViewModel::openArchive,
                onOpenSettings = routeViewModel::openSettings
            )
        }
    }

    val opened = openedPublication
    Box(Modifier.fillMaxSize()) {
        if (opened != null) {
        val bookAppearance = appSettings.readerAppearanceOverrides[opened.book.id]
        val effectiveReaderAppearance = bookAppearance ?: appSettings.readerAppearance
        val readerAppearanceScope = if (bookAppearance != null) {
            ReaderAppearanceScope.BOOK
        } else {
            ReaderAppearanceScope.GLOBAL
        }
        ReaderScreen(
            opened = opened,
            library = library,
            game = game,
            readerAppearance = effectiveReaderAppearance,
            globalReaderAppearance = appSettings.readerAppearance,
            appearanceScope = readerAppearanceScope,
            onReaderAppearanceChange = { scope, appearance ->
                when (scope) {
                    ReaderAppearanceScope.GLOBAL -> onSaveReaderAppearance(appearance)
                    ReaderAppearanceScope.BOOK -> onSaveBookReaderAppearance(opened.book.id, appearance)
                }
            },
            onAppearanceScopeChange = { scope ->
                when (scope) {
                    ReaderAppearanceScope.GLOBAL -> onClearBookReaderAppearance(opened.book.id)
                    ReaderAppearanceScope.BOOK -> onSaveBookReaderAppearance(opened.book.id, effectiveReaderAppearance)
                }
            },
            entryContinuity = activeContinuity,
            returnRitual = activeReturnRitual,
            initialReturnLocatorJson = activeReturnLocatorJson,
            onSensoryEvent = { event -> sensory.perform(view, event) },
            onClose = {
                openedPublication = null
                activeContinuity = null
                activeReturnRitual = null
                activeReturnLocatorJson = null
                routeViewModel.closeReader()
            },
            onLocatorCheckpoint = { locatorJson ->
                routeViewModel.checkpointReaderLocator(opened.book.id, locatorJson)
            }
        )
    } else if (route.showSettings) {
        VeilWorldBackdrop {
            SettingsScreen(
                settings = appSettings,
                exporting = exporting,
                restoring = restoring,
                onSetAppThemeMode = onSetAppThemeMode,
                onSetPerformanceTier = onSetPerformanceTier,
                onSaveReaderAppearance = onSaveReaderAppearance,
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
                        ReaderTrace.event("note_durability_failed", details = "type=${error::class.java.simpleName}")
                    notice = VeilNotice(R.string.notice_note_not_durable)
                    }
                }
            },
            onDeleteHighlight = library::deleteHighlight,
            onDeleteBookmark = library::deleteBookmark
        )
    } else if (
        profile == null ||
        dailyGoalMinutes == null ||
        castleTitle == null
    ) {
        VeilWorldBackdrop {
            VeilLoadingState(
                label = if (restoring) "Restoring the archive" else "Opening the archive"
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
                    notice = VeilNotice(R.string.notice_sigil_locked)
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
                    notice = VeilNotice(R.string.notice_castle_title_locked)
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
                        onOpenArchive = routeViewModel::openArchive,
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

    notice?.let { activeNotice ->
        val isError = activeNotice.tone == VeilNoticeTone.ERROR
        AlertDialog(
            onDismissRequest = { notice = null },
            shape = MaterialTheme.shapes.small,
            containerColor = VeilPalette.Archive,
            titleContentColor = VeilPalette.Moon,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            title = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(
                            if (isError) R.string.notice_error_eyebrow
                            else R.string.notice_info_eyebrow
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        stringResource(
                            if (isError) R.string.notice_error_title
                            else R.string.notice_info_title
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Text(
                    stringResource(
                        activeNotice.messageRes,
                        *activeNotice.formatArgs.toTypedArray()
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { notice = null },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(stringResource(R.string.common_return))
                }
            }
        )
    }
}

internal fun shouldUseNavigationRail(windowSizeClass: WindowSizeClass): Boolean =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

internal fun contentMaxWidthDp(windowSizeClass: WindowSizeClass): Int =
    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        1280
    } else {
        1040
    }

