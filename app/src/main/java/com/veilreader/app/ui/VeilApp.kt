package com.veilreader.app.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.veilreader.app.data.LibraryExport
import com.veilreader.app.ui.screens.ArchiveScreen
import kotlinx.coroutines.CancellationException
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.data.ReadiumEngine
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.screens.CastleScreen
import com.veilreader.app.ui.screens.LibraryScreen
import com.veilreader.app.ui.screens.PathScreen
import com.veilreader.app.ui.screens.ProfileScreen
import com.veilreader.app.ui.screens.ReaderScreen
import com.veilreader.app.ui.screens.ReadingNowScreen
import com.veilreader.app.ui.screens.SanctumScreen
import com.veilreader.app.ui.screens.TreasuryScreen
import kotlinx.coroutines.launch

enum class VeilTab(val label: String, val glyph: String) {
    READING("Reading", "◉"),
    LIBRARY("Library", "▦"),
    CASTLE("Castle", "♜"),
    PATH("Path", "✦"),
    PROFILE("Profile", "◎")
}

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

    val books by library.books.collectAsState()
    val highlights by library.highlights.collectAsState()
    androidx.compose.runtime.LaunchedEffect(library) { game.syncExistingHighlights(library.highlights.value.size) }
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
    var showArchive by remember { mutableStateOf(false) }
    var activeChamber by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var isOpening by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(VeilTab.READING) }
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
            } finally { exporting = false }
        }
    }

    fun restoreData(uri: Uri) {
        if (restoring || exporting) return
        restoring = true
        scope.launch {
            try {
                val result = LibraryExport(context, library).restoreBackup(uri)
                // Recreate so repository StateFlows are rebuilt from the restored preferences and
                // no pre-restore in-memory object can overwrite them later in the session.
                if (activity != null) {
                    activity.recreate()
                } else {
                    errorMessage = "Restored ${result.booksRestored} books and ${result.highlightsRestored} highlights. Reopen Veil Reader to load them."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorMessage = "Restore failed. Your existing local data was kept. ${error.message.orEmpty()}"
            } finally { restoring = false }
        }
    }

    fun openBook(book: Book, locatorOverride: String? = null) {
        if (isOpening || restoring) return
        if (!book.isImported) {
            errorMessage = "This sample entry has no source file. Import an EPUB or PDF from Android Files."
            return
        }
        isOpening = true
        scope.launch {
            try {
                val latest = library.getBook(book.id) ?: book
                readerEngine.openBook(if (locatorOverride == null) latest else latest.copy(locatorJson = locatorOverride))
                    .onSuccess { library.markOpened(book.id); openedPublication = it }
                    .onFailure { if (it is CancellationException) throw it; errorMessage = it.message ?: "Could not open this book." }
            } finally { isOpening = false }
        }
    }

    fun importBook(uri: Uri) {
        if (isImporting || restoring) return
        isImporting = true
        scope.launch {
            try {
                readerEngine.inspectAndCreateBook(uri)
                    .onSuccess { book ->
                        library.addImportedBook(book)
                        selected = VeilTab.LIBRARY
                        openBook(book)
                    }
                    .onFailure { if (it is CancellationException) throw it; errorMessage = it.message ?: "Could not import this publication." }
            } finally { isImporting = false }
        }
    }

    androidx.compose.runtime.LaunchedEffect(externalOpenUri) {
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
                selected = VeilTab.LIBRARY
            }
        )
    } else if (showArchive) {
        ArchiveScreen(
            books,
            highlights,
            onClose = { showArchive = false },
            onOpenPassage = { book, locator -> openBook(book, locator) }
        )
    } else if (activeChamber == "treasury") {
        TreasuryScreen(
            profile = profile,
            equippedSigil = equippedSigil,
            onEquip = { id ->
                if (!game.equipSigil(id)) errorMessage = "That sigil has not awakened yet."
            },
            onClose = { activeChamber = null }
        )
    } else if (activeChamber == "sanctum") {
        SanctumScreen(
            profile = profile,
            castleTitle = castleTitle,
            availableTitles = game.availableCastleTitles(),
            onSelectTitle = { title ->
                if (!game.selectCastleTitle(title)) errorMessage = "That Castle title is still sealed."
            },
            onClose = { activeChamber = null }
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
                            selected = selected == tab,
                            onClick = { selected = tab },
                            icon = { Text(tab.glyph) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selected) {
                    VeilTab.READING -> ReadingNowScreen(
                        books = books,
                        profile = profile,
                        quests = quests,
                        onOpenBook = { openBook(it) },
                        onOpenLibrary = { selected = VeilTab.LIBRARY },
                        onOpenCastle = { selected = VeilTab.CASTLE }
                    )

                    VeilTab.LIBRARY -> LibraryScreen(
                        books = books,
                        isImporting = isImporting,
                        onImportUri = ::importBook,
                        onOpenBook = { openBook(it) },
                        onFavorite = library::toggleFavorite,
                        onEditMetadata = library::editMetadata
                    )

                    VeilTab.CASTLE -> CastleScreen(
                        profile = profile,
                        onOpenRoom = { room ->
                            when (room) {
                                "library" -> selected = VeilTab.LIBRARY
                                "ritual" -> selected = VeilTab.PATH
                                "observatory" -> selected = VeilTab.PROFILE
                                "archive" -> showArchive = true
                                "treasury", "sanctum" -> activeChamber = room
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
                        onOpenArchive = { showArchive = true }
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
