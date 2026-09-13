package com.veilreader.app.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LibraryExport
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.AppPreferences
import com.veilreader.app.domain.AppTheme
import com.veilreader.app.ui.theme.VeilTheme
import com.veilreader.app.ui.navigation.visibleTabs
import com.veilreader.app.ui.screens.SettingsScreen
import com.veilreader.app.ui.screens.WelcomeScreen
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
    val library = remember(context) { LocalLibraryRepository(context) }
    val preferences by library.appPreferences.collectAsStateWithLifecycle()
    val appearance by library.appearance.collectAsStateWithLifecycle()
    VeilTheme(appTheme = preferences?.theme ?: AppTheme.SYSTEM, reduceMotion = appearance.reduceMotion) {
        VeilAppContent(library, preferences, externalOpenUri, onExternalOpenUriConsumed)
    }
}

@Composable
private fun VeilAppContent(
    library: LocalLibraryRepository,
    preferences: AppPreferences?,
    externalOpenUri: Uri?,
    onExternalOpenUriConsumed: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val game = remember(context) { GameRepository(context) }
    val readerEngine = remember(context) { ReadiumEngine(context) }
    val routeViewModel: VeilAppViewModel = viewModel()
    val route by routeViewModel.route.collectAsStateWithLifecycle()

    val books by library.books.collectAsState()
    val booksLoaded by library.booksLoaded.collectAsStateWithLifecycle()
    val startupFailure by library.startupFailure.collectAsStateWithLifecycle()
    val appearance by library.appearance.collectAsStateWithLifecycle()
    val gameVisible = preferences?.gameVisible ?: true
    val tabs = visibleTabs(gameVisible)
    val selectedTab = route.selectedTab.takeIf { it in tabs } ?: VeilTab.READING
    var replayWelcome by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(gameVisible) { routeViewModel.applyGameVisibility(gameVisible) }
    LaunchedEffect(booksLoaded, books.isNotEmpty(), preferences?.onboardingCompleted) {
        if (booksLoaded && books.isNotEmpty() && preferences != null && !preferences.onboardingCompleted) {
            library.saveAppPreferences(preferences.copy(onboardingCompleted = true))
        }
    }
    val highlights by library.highlights.collectAsState()
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

    val profile by game.profile.collectAsState()
    val quests by game.quests.collectAsState()
    val dailyGoalMinutes by game.dailyGoalMinutes.collectAsState()
    val equippedSigil by game.equippedSigil.collectAsState()
    val castleTitle by game.castleTitle.collectAsState()
    val estate by game.estate.collectAsStateWithLifecycle()

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
    var openedPublication by remember { mutableStateOf<OpenedPublication?>(null) }
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
                gameVisible = gameVisible,
                onOpenSettings = { routeViewModel.openSettings() },
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
                estate = estate,
                onBuild = game::buildEstate,
                onClaim = game::claimStoryChapter,
                onDesign = { game.designEstate(it.name, it.palette, it.grounds, it.sky) },
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
                gameVisible = gameVisible,
                dailyGoalMinutes = dailyGoalMinutes,
                castleTitle = castleTitle,
                equippedSigilName = equippedSigil?.let(::sigilDisplayName),
                onSetDailyGoal = game::setDailyGoal,
                onOpenSettings = routeViewModel::openSettings,
                onOpenArchive = routeViewModel::openArchive
            )
        }
    }

    val opened = openedPublication
    if (startupFailure != null) {
        VeilWorldBackdrop {
            Column(Modifier.align(Alignment.Center).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Could not open Veil Reader")
                Text(startupFailure.orEmpty())
                Button(onClick = { activity?.recreate() }) { Text("Try again") }
            }
        }
    } else if (preferences == null || !booksLoaded) {
        VeilWorldBackdrop {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else if (opened != null) {
        ReaderScreen(
            opened = opened,
            library = library,
            game = game,
            onClose = {
                openedPublication = null
                routeViewModel.closeReader()
            }
        )
    } else if (route.settingsSection != null) {
        SettingsScreen(
            preferences = preferences,
            appearance = appearance,
            section = route.settingsSection!!,
            dailyGoalMinutes = dailyGoalMinutes,
            exporting = exporting,
            restoring = restoring,
            onSection = routeViewModel::openSettings,
            onPreferences = library::saveAppPreferences,
            onAppearance = library::saveAppearance,
            onDailyGoal = game::setDailyGoal,
            onExportBackup = { exportData(it, true) },
            onRestoreBackup = ::restoreData,
            onExportNotes = { exportData(it, false) },
            onShowWelcome = { routeViewModel.closeSettings(); replayWelcome = true },
            onClose = routeViewModel::closeSettings
        )
    } else if ((replayWelcome || (!preferences.onboardingCompleted && books.isEmpty())) &&
        route.activeBookId == null && !isImporting && externalOpenUri == null) {
        WelcomeScreen(
            gameVisible = gameVisible,
            onStart = { world ->
                library.saveAppPreferences(preferences.copy(gameVisible = world, onboardingCompleted = true))
                replayWelcome = false
                routeViewModel.selectTab(VeilTab.LIBRARY)
            },
            onSkip = {
                library.saveAppPreferences(preferences.copy(onboardingCompleted = true))
                replayWelcome = false
            },
            onRestore = { replayWelcome = false; routeViewModel.openSettings("data") }
        )
    } else if (route.showArchive) {
        ArchiveScreen(
            books,
            highlights,
            onClose = routeViewModel::closeArchive,
            onOpenPassage = { book, locator -> requestOpenBook(book, locator) }
        )
    } else if (gameVisible && route.activeChamber == "treasury") {
        TreasuryScreen(
            profile = profile,
            equippedSigil = equippedSigil,
            onEquip = { id ->
                if (!game.equipSigil(id)) errorMessage = "That sigil has not awakened yet."
            },
            onClose = routeViewModel::closeChamber
        )
    } else if (gameVisible && route.activeChamber == "sanctum") {
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
        VeilWorldBackdrop {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wideLayout = maxWidth >= 840.dp || (maxWidth >= 600.dp && maxHeight < 480.dp)

                if (wideLayout) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        VeilNavigationRail(
                            selected = selectedTab,
                            tabs = tabs,
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
                                selectedTab = selectedTab,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .widthIn(max = 1180.dp)
                            ) { tab ->
                                mainContent(tab)
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().statusBarsPadding()) {
                        VeilAnimatedTabHost(
                            selectedTab = selectedTab,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) { tab -> mainContent(tab) }
                        VeilBottomDock(
                            selected = selectedTab,
                            tabs = tabs,
                            onSelect = routeViewModel::selectTab
                        )
                    }
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
