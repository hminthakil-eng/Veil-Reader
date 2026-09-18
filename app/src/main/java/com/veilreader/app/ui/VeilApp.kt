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
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LibraryExport
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.domain.Book
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
import com.veilreader.app.ui.screens.TreasuryScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun VeilApp(
    externalOpenUri: Uri? = null,
    onExternalOpenUriConsumed: () -> Unit = {}
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

    // While Readium owns the screen, avoid parent-level subscriptions that would recompose the
    // entire reader for every progress/game-state write. The latest values remain available
    // synchronously and subscriptions resume as soon as the reader closes.
    val books = if (openedPublication == null) {
        library.books.collectAsStateWithLifecycle().value
    } else {
        library.books.value
    }
    val highlights = if (openedPublication == null) {
        library.highlights.collectAsStateWithLifecycle().value
    } else {
        library.highlights.value
    }
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

    val profile = if (openedPublication == null) {
        game.profile.collectAsStateWithLifecycle().value
    } else {
        game.profile.value
    }
    val quests = if (openedPublication == null) {
        game.quests.collectAsStateWithLifecycle().value
    } else {
        game.quests.value
    }
    val dailyGoalMinutes = if (openedPublication == null) {
        game.dailyGoalMinutes.collectAsStateWithLifecycle().value
    } else {
        game.dailyGoalMinutes.value
    }
    val equippedSigil = if (openedPublication == null) {
        game.equippedSigil.collectAsStateWithLifecycle().value
    } else {
        game.equippedSigil.value
    }
    val castleTitle = if (openedPublication == null) {
        game.castleTitle.collectAsStateWithLifecycle().value
    } else {
        game.castleTitle.value
    }

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
        val candidate = if (locatorOverride == null) book else book.copy(locatorJson = locatorOverride)
        readerEngine.openBook(candidate)
            .onSuccess { opened ->
                if (routeViewModel.route.value.activeBookId != targetId) {
                    opened.close()
                    return@onSuccess
                }
                if (locatorOverride != null) {
                    library.saveProgress(targetId, book.progress.toDouble(), locatorOverride)
                }
                library.markOpened(targetId)
                openedPublication = opened
                if (locatorOverride == null) {
                    routeViewModel.readerOpened(targetId)
                } else {
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
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                routeViewModel.bookOpenFailed(targetId)
                errorMessage = error.message ?: "Could not open this book."
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
                profile = profile,
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
                onEditMetadata = library::editMetadata
            )

            VeilTab.CASTLE -> CastleScreen(
                profile = profile,
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
                profile = profile,
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
                profile = profile,
                highlightCount = highlights.size,
                exporting = exporting,
                restoring = restoring,
                dailyGoalMinutes = dailyGoalMinutes,
                castleTitle = castleTitle,
                equippedSigilName = equippedSigil?.let(::sigilDisplayName),
                onSetDailyGoal = game::setDailyGoal,
                onExportBackup = { exportData(it, true) },
                onRestoreBackup = ::restoreData,
                onExportNotes = { exportData(it, false) },
                onOpenArchive = routeViewModel::openArchive
            )
        }
    }

    val opened = openedPublication
    if (opened != null) {
        ReaderScreen(
            opened = opened,
            library = library,
            game = game,
            onClose = {
                openedPublication = null
                routeViewModel.closeReader()
            }
        )
    } else if (route.showArchive) {
        ArchiveScreen(
            books,
            highlights,
            onClose = routeViewModel::closeArchive,
            onOpenPassage = { book, locator -> requestOpenBook(book, locator) }
        )
    } else if (route.activeChamber == "treasury") {
        TreasuryScreen(
            profile = profile,
            equippedSigil = equippedSigil,
            onEquip = { id ->
                if (!game.equipSigil(id)) errorMessage = "That sigil has not awakened yet."
            },
            onClose = routeViewModel::closeChamber
        )
    } else if (route.activeChamber == "sanctum") {
        SanctumScreen(
            profile = profile,
            castleTitle = castleTitle,
            availableTitles = game.availableCastleTitles(),
            onSelectTitle = { title ->
                if (!game.selectCastleTitle(title)) errorMessage = "That Castle title is still sealed."
            },
            onClose = routeViewModel::closeChamber
        )
    } else {
        val adaptiveInfo = currentWindowAdaptiveInfoV2()
        val widthSizeClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass
        val heightSizeClass = adaptiveInfo.windowSizeClass.windowHeightSizeClass

        // Prefer the branded rail once there is enough persistent horizontal space, but keep the
        // compact dock on short landscape windows where a rail would compete with reading content.
        val useRail = heightSizeClass != WindowHeightSizeClass.COMPACT &&
            (widthSizeClass == WindowWidthSizeClass.MEDIUM ||
                widthSizeClass == WindowWidthSizeClass.EXPANDED)
        val contentMaxWidth = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 1280.dp else 1040.dp

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
                Box(Modifier.fillMaxSize()) {
                    VeilAnimatedTabHost(
                        selectedTab = route.selectedTab,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(bottom = 88.dp)
                    ) { tab ->
                        mainContent(tab)
                    }
                    VeilBottomDock(
                        selected = route.selectedTab,
                        onSelect = routeViewModel::selectTab,
                        modifier = Modifier.align(Alignment.BottomCenter)
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

private fun sigilDisplayName(id: String): String = when (id) {
    "first_hour" -> "Quiet Hour"
    "passage_keeper" -> "Passage Keeper"
    "seven_days" -> "Seven-Day Lantern"
    "ten_tomes" -> "Ten Tomes"
    "first_threshold" -> "First Threshold"
    else -> "Unknown Sigil"
}
