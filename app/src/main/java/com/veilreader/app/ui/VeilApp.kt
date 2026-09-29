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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LibraryExport
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookReturnRitual
import com.veilreader.app.domain.ReaderAppearance
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
    onSaveReaderAppearance: (ReaderAppearance) -> Unit = {},
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun exportData(uri: Uri, backup: Boolean) {
        if (exporting || restoring) return
        exporting = true
        scope.launch {
            try {
                val exporter = LibraryExport(context, library)
                if (backup) exporter.writeBackup(uri) else exporter.writeNotebook(uri)
                errorMessage = if (backup) "Library backup exported." else "Notebook exported."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorMessage = "Export failed. The destination may contain an incomplete file. ${error.message.orEmpty()}"
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
                    errorMessage = "Restored ${result.booksRestored} books and ${result.highlightsRestored} highlights. Reopen Veil Reader to load them."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorMessage = "Restore failed. Your existing local data was kept. ${error.message.orEmpty()}"
            } finally {
                restoring = false
            }
        }
    }

    fun requestOpenBook(book: Book, locatorOverride: String? = null) {
        if (restoring) return
        if (!book.isImported) {
            errorMessage = "This sample entry has no source file. Import an EPUB or PDF from Android Files."
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
                    errorMessage = inspectionError.message ?: "Could not import this publication."
                    return@launch
                }

                val commit = library.addImportedBook(inspected.getOrThrow())
                routeViewModel.selectTab(VeilTab.LIBRARY)
                routeViewModel.requestBook(commit.book.id)
                if (commit.duplicate) {
                    errorMessage = "${commit.book.title} is already in your Grand Library. I opened the existing copy."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorMessage = error.message ?: "Could not import this publication."
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
            errorMessage = "This book no longer has a local publication file."
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
                errorMessage = error.message ?: "Could not open this book."
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
            errorMessage = "The book opened, but older PDF reading positions could not be upgraded yet. ${error.message.orEmpty()}"
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
                        errorMessage = "The requested reading position is open, but could not be saved yet. ${error.message.orEmpty()}"
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
                        errorMessage = "Your restored reading position is open, but could not be made durable yet. ${error.message.orEmpty()}"
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
                        errorMessage = "Complete the current advancement ritual first."
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
                        errorMessage = "Complete the current advancement ritual first."
                    } else {
                        sensory.perform(view, VeilSensoryEvent.ADVANCEMENT)
                    }
                },
                onChoosePath = { pathId ->
                    if (!game.choosePath(pathId)) {
                        errorMessage = "Your Path is sealed after the first rank advancement."
                    }
                }
            )

            VeilTab.PROFILE -> ProfileScreen(
                profile = requireNotNull(profile),
                highlightCount = highlights.size,
                dailyGoalMinutes = requireNotNull(dailyGoalMinutes),
                castleTitle = requireNotNull(castleTitle),
                equippedSigilName = equippedSigil?.let(::sigilDisplayName),
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
    Box(Modifier.fillMaxSize()) {
        if (opened != null) {
        ReaderScreen(
            opened = opened,
            library = library,
            game = game,
            readerAppearance = appSettings.readerAppearance,
            onReaderAppearanceChange = onSaveReaderAppearance,
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
                        errorMessage = "Your note changed locally, but storage confirmation failed. " + error.message.orEmpty()
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
                    errorMessage = "That sigil has not awakened yet."
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
                    errorMessage = "That Castle title is still sealed."
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

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
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
                        "INTERRUPTION · LOCAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        "The action could not be completed",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("Return")
                }
            }
        )
    }
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

private fun sigilDisplayName(id: String): String = when (id) {
    "first_hour" -> "Quiet Hour"
    "passage_keeper" -> "Passage Keeper"
    "seven_days" -> "Seven-Day Lantern"
    "ten_tomes" -> "Ten Tomes"
    "first_threshold" -> "First Threshold"
    else -> "Unknown Sigil"
}

