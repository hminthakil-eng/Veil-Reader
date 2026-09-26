package com.veilreader.app.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.ui.navigation.VeilAppViewModel
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.screens.ArchiveScreen
import com.veilreader.app.ui.screens.CastleScreen
import com.veilreader.app.ui.screens.LibraryScreen
import com.veilreader.app.ui.screens.PathScreen
import com.veilreader.app.ui.screens.ProfileScreen
import com.veilreader.app.ui.screens.ReaderScreen
import com.veilreader.app.ui.screens.ReadingNowScreen
import com.veilreader.app.ui.screens.SanctumScreen
import com.veilreader.app.ui.screens.SettingsScreen
import com.veilreader.app.ui.screens.TreasuryScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun VeilApp(
    externalOpenUri: Uri? = null,
    onExternalOpenUriConsumed: () -> Unit = {},
    appSettings: AppSettings = AppSettings(),
    onSetAppThemeMode: (AppThemeMode) -> Unit = {},
    onSaveReaderAppearance: (ReaderAppearance) -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val library = remember(context) { LocalLibraryRepository(context) }
    val game = remember(context) { GameRepository(context) }
    val readerEngine = remember(context) { ReadiumEngine(context) }
    val routeViewModel: VeilAppViewModel = viewModel()
    val route by routeViewModel.route.collectAsStateWithLifecycle()
    var openedPublication by remember { mutableStateOf<OpenedPublication?>(null) }

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
    val books = booksState?.value.orEmpty()
    val highlights = highlightsState?.value.orEmpty()
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

        val book = library.getBook(targetId) ?: targetBook ?: return@LaunchedEffect
        if (!book.isImported) {
            routeViewModel.bookOpenFailed(targetId)
            errorMessage = "This book no longer has a local publication file."
            return@LaunchedEffect
        }

        val locatorOverride = route.locatorOverrideJson
        val readerCheckpoint = route.readerLocatorCheckpointJson
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
        openedPublication = opened

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
                onOpenBook = { requestOpenBook(it) },
                onOpenLibrary = { routeViewModel.selectTab(VeilTab.LIBRARY) },
                onOpenCastle = { routeViewModel.selectTab(VeilTab.CASTLE) }
            )

            VeilTab.LIBRARY -> LibraryScreen(
                books = books,
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
                        "observatory" -> routeViewModel.selectTab(VeilTab.PROFILE)
                        "archive" -> routeViewModel.openArchive()
                        "treasury", "sanctum" -> routeViewModel.openChamber(room)
                    }
                },
                onAdvanceRank = {
                    if (!game.advanceRank()) {
                        errorMessage = "Complete the current advancement ritual first."
                    }
                }
            )

            VeilTab.PATH -> PathScreen(
                profile = requireNotNull(profile),
                onAdvanceRank = {
                    if (!game.advanceRank()) {
                        errorMessage = "Complete the current advancement ritual first."
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
                onSetDailyGoal = game::setDailyGoal,
                onOpenArchive = routeViewModel::openArchive,
                onOpenSettings = routeViewModel::openSettings
            )
        }
    }

    val opened = openedPublication
    if (opened != null) {
        ReaderScreen(
            opened = opened,
            library = library,
            game = game,
            readerAppearance = appSettings.readerAppearance,
            onReaderAppearanceChange = onSaveReaderAppearance,
            onClose = {
                openedPublication = null
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
                onExportBackup = { exportData(it, true) },
                onRestoreBackup = ::restoreData,
                onExportNotes = { exportData(it, false) },
                onClose = routeViewModel::closeSettings
            )
        }
    } else if (route.showArchive) {
        ArchiveScreen(
            books,
            highlights,
            onClose = routeViewModel::closeArchive,
            onOpenPassage = { book, locator -> requestOpenBook(book, locator) }
        )
    } else if (route.activeChamber == "treasury") {
        TreasuryScreen(
            profile = requireNotNull(profile),
            equippedSigil = equippedSigil,
            onEquip = { id ->
                if (!game.equipSigil(id)) errorMessage = "That sigil has not awakened yet."
            },
            onClose = routeViewModel::closeChamber
        )
    } else if (route.activeChamber == "sanctum") {
        SanctumScreen(
            profile = requireNotNull(profile),
            castleTitle = requireNotNull(castleTitle),
            availableTitles = game.availableCastleTitles(),
            onSelectTitle = { title ->
                if (!game.selectCastleTitle(title)) errorMessage = "That Castle title is still sealed."
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
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    VeilAnimatedTabHost(
                        selectedTab = route.selectedTab,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { tab ->
                        mainContent(tab)
                    }
                    VeilBottomDock(
                        selected = route.selectedTab,
                        onSelect = routeViewModel::selectTab
                    )
                }
            }
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Veil Reader") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) { Text("OK") }
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

private fun sigilDisplayName(id: String): String = when (id) {
    "first_hour" -> "Quiet Hour"
    "passage_keeper" -> "Passage Keeper"
    "seven_days" -> "Seven-Day Lantern"
    "ten_tomes" -> "Ten Tomes"
    "first_threshold" -> "First Threshold"
    else -> "Unknown Sigil"
}

