package com.veilreader.app.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    val books by library.books.collectAsState()
    val highlights by library.highlights.collectAsState()
    LaunchedEffect(library) { game.syncExistingHighlights(library.highlights.value.size) }

    // Existing 0.7 libraries and restored backups have no cached covers. Process one book at a time
    // so each Room update naturally advances this effect to the next pending publication.
    val nextCoverBook = books.firstOrNull { it.isImported && it.coverCachePath == null }
    LaunchedEffect(nextCoverBook?.id) {
        val book = nextCoverBook ?: return@LaunchedEffect
        val cachedPath = readerEngine.extractAndCacheCover(book).getOrDefault("")
        library.updateCoverCachePath(book.id, cachedPath)
    }

    val profile by game.profile.collectAsState()
    val quests by game.quests.collectAsState()
    val dailyGoalMinutes by game.dailyGoalMinutes.collectAsState()
    val equippedSigil by game.equippedSigil.collectAsState()
    val castleTitle by game.castleTitle.collectAsState()

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
                readerEngine.inspectAndCreateBook(uri)
                    .onSuccess { book ->
                        library.addImportedBook(book)
                        routeViewModel.selectTab(VeilTab.LIBRARY)
                        routeViewModel.requestBook(book.id)
                    }
                    .onFailure {
                        if (it is CancellationException) throw it
                        errorMessage = it.message ?: "Could not import this publication."
                    }
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
            // Let ReaderFragmentHost dispose the old publication, then this effect will restart
            // because openedPublication.book.id is one of its keys.
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
                // Persist an explicit archive/bookmark jump before clearing the transient override.
                // The override stays in SavedStateHandle until the queued Room write is durable,
                // so an immediate recreation cannot fall back to the previous reading position.
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
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 0.dp
                ) {
                    VeilTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route.selectedTab == tab,
                            onClick = { routeViewModel.selectTab(tab) },
                            icon = { Text(tab.glyph) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (route.selectedTab) {
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
