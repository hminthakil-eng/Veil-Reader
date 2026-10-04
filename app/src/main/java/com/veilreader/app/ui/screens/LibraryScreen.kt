package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.domain.ArchiveDepth
import com.veilreader.app.domain.ArchiveWing
import com.veilreader.app.domain.ArchiveWingKind
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookArchiveMemory
import com.veilreader.app.domain.BookArtifactMemory
import com.veilreader.app.domain.BookMetadataUpdate
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.LibraryAtmosphereState
import com.veilreader.app.domain.LibraryMemoryEvent
import com.veilreader.app.domain.LibraryMemoryEventKind
import com.veilreader.app.domain.LibraryWingState
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingMilestoneKind
import com.veilreader.app.domain.ReadingMilestoneRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.deriveBookArtifactMemory
import com.veilreader.app.domain.deriveLibraryAtmosphereState
import com.veilreader.app.domain.deriveLibraryMemoryState
import com.veilreader.app.domain.deriveLibraryWings
import com.veilreader.app.ui.books.BookArtifactState
import com.veilreader.app.ui.books.BookPatina
import com.veilreader.app.ui.books.BookReadingState
import com.veilreader.app.ui.books.bookArtifactState
import com.veilreader.app.ui.theme.withVeilContentScript
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.galleryCellMeasureDp
import com.veilreader.app.ui.theme.archiveLayoutPolicyFor
import com.veilreader.app.ui.theme.archiveTimePhaseForHour
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.libraryArchiveAtmosphere
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

internal enum class LibraryViewMode { GALLERY, SHELVES, INDEX }

internal fun libraryViewModeFromStored(value: String): LibraryViewMode =
    when (value) {
        "GRID" -> LibraryViewMode.GALLERY
        "LIST" -> LibraryViewMode.INDEX
        else -> runCatching { LibraryViewMode.valueOf(value) }
            .getOrDefault(LibraryViewMode.GALLERY)
    }

internal data class LibraryShelfGroup(
    val eyebrow: String,
    val title: String,
    val books: List<Book>
)

internal data class LibraryShelfLabels(
    val filteredArchive: String,
    val matchingVolumes: String,
    val journey: String,
    val currentlyReading: String,
    val collection: String,
    val series: String,
    val author: String,
    val record: String,
    val completedVolumes: String,
    val unopened: String,
    val waitingOnShelf: String
)

internal data class LibraryNamedBookGroup(
    val name: String,
    val books: List<Book>
)

internal fun groupLibraryBooksByLabel(
    entries: List<Pair<String, Book>>
): List<LibraryNamedBookGroup> =
    entries
        .mapNotNull { (rawName, book) ->
            rawName.trim().takeIf { it.isNotEmpty() }?.let { it to book }
        }
        .groupBy { (name, _) -> name.lowercase(Locale.ROOT) }
        .map { (_, taggedBooks) ->
            val displayName = taggedBooks
                .map { it.first }
                .distinct()
                .sortedWith(compareBy<String> { it.lowercase(Locale.ROOT) }.thenBy { it })
                .first()
            LibraryNamedBookGroup(
                name = displayName,
                books = taggedBooks.map { it.second }.distinctBy { it.id }
            )
        }

internal fun deriveLibraryShelfGroups(
    books: List<Book>,
    filtered: List<Book>,
    filterActive: Boolean,
    labels: LibraryShelfLabels
): List<LibraryShelfGroup> {
    if (filterActive) {
        return listOf(
            LibraryShelfGroup(
                eyebrow = labels.filteredArchive,
                title = labels.matchingVolumes,
                books = filtered
            )
        ).filter { it.books.isNotEmpty() }
    }

    val groups = mutableListOf<LibraryShelfGroup>()

    books
        .filter { !it.finished && it.progress > 0f }
        .sortedByDescending { it.lastOpenedAtEpochMs }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup(labels.journey, labels.currentlyReading, it) }

    groupLibraryBooksByLabel(
        books.flatMap { book -> book.allCollections.map { it to book } }
    )
        .sortedWith(
            compareByDescending<LibraryNamedBookGroup> { it.books.size }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .take(6)
        .forEach { group ->
            groups += LibraryShelfGroup(
                eyebrow = labels.collection,
                title = group.name,
                books = group.books
            )
        }

    groupLibraryBooksByLabel(
        books.mapNotNull { book -> book.seriesName?.let { it to book } }
    )
        .sortedWith(
            compareByDescending<LibraryNamedBookGroup> { it.books.size }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .take(6)
        .forEach { group ->
            groups += LibraryShelfGroup(
                eyebrow = labels.series,
                title = group.name,
                books = group.books.sortedWith(
                    compareBy<Book> { it.seriesIndex ?: Double.MAX_VALUE }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                        .thenBy { it.id }
                )
            )
        }

    groupLibraryBooksByLabel(
        books.map { it.author to it }
    )
        .filter { it.books.size >= 2 }
        .sortedWith(
            compareByDescending<LibraryNamedBookGroup> { it.books.size }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .take(4)
        .forEach { group ->
            groups += LibraryShelfGroup(
                eyebrow = labels.author,
                title = group.name,
                books = group.books
            )
        }

    books
        .filter { it.finished }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup(labels.record, labels.completedVolumes, it) }

    books
        .filter { !it.finished && it.progress <= 0f }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup(labels.unopened, labels.waitingOnShelf, it) }

    return groups
}

@Composable
fun LibraryScreen(
    books: List<Book>,
    highlights: List<Highlight> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    readingCycles: List<ReadingCycleRecord> = emptyList(),
    readingMilestones: List<ReadingMilestoneRecord> = emptyList(),
    isImporting: Boolean,
    onImportUri: (Uri) -> Unit,
    onOpenBook: (Book) -> Unit,
    onFavorite: (String) -> Unit,
    onEditMetadata: (BookMetadataUpdate) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManga: () -> Unit = {}
) {
    LibraryArchiveContent(
        books, highlights, bookmarks, readingSessions, readingCycles, readingMilestones,
        isImporting, onImportUri, onOpenBook, onFavorite, onEditMetadata, onDeleteBook,
        onOpenSettings, onOpenManga
    )
}

@Composable
internal fun LibraryArchiveContent(
    books: List<Book>,
    highlights: List<Highlight> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    readingCycles: List<ReadingCycleRecord> = emptyList(),
    readingMilestones: List<ReadingMilestoneRecord> = emptyList(),
    isImporting: Boolean,
    onImportUri: (Uri) -> Unit,
    onOpenBook: (Book) -> Unit,
    onFavorite: (String) -> Unit,
    onEditMetadata: (BookMetadataUpdate) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManga: () -> Unit = {},
    initialViewMode: LibraryViewMode = LibraryViewMode.GALLERY,
    initialQuery: String = ""
) {
    val focusManager = LocalFocusManager.current
    val archiveAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val archiveLayout = archiveLayoutPolicyFor(archiveAdaptiveClass)
    val libraryNowEpochMs by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(60_000L)
            value = System.currentTimeMillis()
        }
    }
    val archiveTimePhase = remember(libraryNowEpochMs) {
        archiveTimePhaseForHour(LocalTime.now().hour)
    }
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var shelf by rememberSaveable { mutableStateOf("All") }
    var collection by rememberSaveable { mutableStateOf("") }
    var seriesFilter by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Recent") }
    var viewModeName by rememberSaveable { mutableStateOf(initialViewMode.name) }
    val viewMode = libraryViewModeFromStored(viewModeName)
    var overviewExpanded by rememberSaveable { mutableStateOf(false) }
    var collectionMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var seriesMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Book?>(null) }
    var detailBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var collectionNames by remember { mutableStateOf("") }
    var seriesName by remember { mutableStateOf("") }
    var seriesIndex by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }

    fun beginMetadataEdit(book: Book) {
        editing = book
        title = book.title
        author = book.author
        collectionNames = book.allCollections.joinToString(", ")
        seriesName = book.seriesName.orEmpty()
        seriesIndex = book.seriesIndex?.let(::formatSeriesIndexInput).orEmpty()
        language = book.language.orEmpty()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportUri)
    }
    val collections = remember(books) {
        books.flatMap { it.allCollections }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    LaunchedEffect(collections) {
        if (collection.isNotEmpty() && collections.none { it.equals(collection, ignoreCase = true) }) {
            collection = ""
        }
    }

    val memoryState = remember(books, highlights, readingSessions, libraryNowEpochMs) {
        deriveLibraryMemoryState(
            books = books,
            highlights = highlights,
            sessions = readingSessions,
            nowEpochMs = libraryNowEpochMs
        )
    }
    val deepShelfBookIds = remember(memoryState.deepShelfBookIds) {
        memoryState.deepShelfBookIds.toSet()
    }
    val wingState = remember(books) {
        deriveLibraryWings(books)
    }
    val atmosphereState = remember(
        books,
        highlights,
        bookmarks,
        readingSessions,
        memoryState
    ) {
        deriveLibraryAtmosphereState(
            books = books,
            highlights = highlights,
            bookmarks = bookmarks,
            sessions = readingSessions,
            memoryState = memoryState
        )
    }
    val artifactMemoryByBookId = remember(
        books,
        highlights,
        bookmarks,
        readingSessions
    ) {
        val sessionsByBook = readingSessions
            .filter { !it.bookId.isNullOrBlank() }
            .groupBy { requireNotNull(it.bookId) }
        val highlightsByBook = highlights.groupBy { it.bookId }
        val bookmarksByBook = bookmarks.groupBy { it.bookId }

        books.associate { book ->
            book.id to deriveBookArtifactMemory(
                book = book,
                sessions = sessionsByBook[book.id].orEmpty(),
                highlights = highlightsByBook[book.id].orEmpty(),
                bookmarks = bookmarksByBook[book.id].orEmpty()
            )
        }
    }

    val trimmedQuery = query.trim()
    val normalizedQuery = remember(trimmedQuery) { normalizeLibrarySearchText(trimmedQuery) }
    val searchableByBookId = remember(books) {
        books.associate { book ->
            book.id to normalizedLibrarySearchDocument(
                title = book.title,
                author = book.author,
                series = book.seriesName,
                language = book.language,
                collections = book.allCollections
            )
        }
    }
    val searchScoreByBookId = remember(normalizedQuery, searchableByBookId) {
        if (normalizedQuery.isBlank()) {
            emptyMap()
        } else {
            searchableByBookId.mapNotNull { (bookId, document) ->
                librarySearchRelevance(normalizedQuery, document)
                    ?.let { score -> bookId to score }
            }.toMap()
        }
    }
    val filtered = remember(
        books,
        trimmedQuery,
        normalizedQuery,
        searchScoreByBookId,
        shelf,
        collection,
        seriesFilter,
        sort,
        deepShelfBookIds,
        memoryState
    ) {
        books.filter { book ->
            val matchesQuery = normalizedQuery.isBlank() ||
                searchScoreByBookId.containsKey(book.id)
            val matchesShelf = when (shelf) {
                "Reading" -> !book.finished && book.progress > 0f
                "Unread" -> !book.finished && book.progress == 0f
                "Finished" -> book.finished
                "Favorites" -> book.favorite
                "Deep Shelf" -> book.id in deepShelfBookIds
                else -> true
            }
            val matchesCollection = collection.isEmpty() || book.allCollections.any {
                it.equals(collection, ignoreCase = true)
            }
            val matchesSeries = seriesFilter.isEmpty() ||
                book.seriesName?.equals(seriesFilter, ignoreCase = true) == true
            matchesQuery && matchesShelf && matchesCollection && matchesSeries
        }.let { list ->
            when (sort) {
                "Title" -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
                "Author" -> list.sortedBy { it.author.lowercase(Locale.ROOT) }
                "Progress" -> list.sortedByDescending { it.progress }
                "Archive Depth" -> list.sortedByDescending {
                    memoryState.memoryFor(it.id)?.inactiveMillis ?: 0L
                }
                "Series" -> list.sortedWith(
                    compareBy<Book> { it.seriesName?.lowercase(Locale.ROOT) ?: "\uffff" }
                        .thenBy { it.seriesIndex ?: Double.MAX_VALUE }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                )
                else -> if (normalizedQuery.isNotBlank()) {
                    list.sortedWith(
                        compareByDescending<Book> {
                            searchScoreByBookId[it.id] ?: Int.MIN_VALUE
                        }
                            .thenByDescending {
                                maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs)
                            }
                            .thenBy { it.id }
                    )
                } else {
                    list.sortedWith(
                        compareByDescending<Book> {
                            maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs)
                        }.thenBy { it.id }
                    )
                }
            }
        }
    }

    val recentReading = remember(books) {
        books.asSequence()
            .filter { !it.finished && it.progress > 0f }
            .sortedByDescending { it.lastOpenedAtEpochMs }
            .take(5)
            .toList()
    }
    val booksById = remember(books) { books.associateBy { it.id } }
    val detailBook = detailBookId?.let(booksById::get)
    val filterActive = trimmedQuery.isNotBlank() || shelf != "All" ||
        collection.isNotEmpty() || seriesFilter.isNotEmpty()
    val shelfLabels = LibraryShelfLabels(
        filteredArchive = stringResource(R.string.library_group_filtered),
        matchingVolumes = stringResource(R.string.library_group_matching),
        journey = stringResource(R.string.library_group_journey),
        currentlyReading = stringResource(R.string.library_group_currently_reading),
        collection = stringResource(R.string.library_group_collection),
        series = stringResource(R.string.library_group_series),
        author = stringResource(R.string.library_group_author),
        record = stringResource(R.string.library_group_record),
        completedVolumes = stringResource(R.string.library_group_completed),
        unopened = stringResource(R.string.library_group_unopened),
        waitingOnShelf = stringResource(R.string.library_group_waiting)
    )
    val shelfGroups = remember(books, filtered, filterActive, viewMode, shelfLabels) {
        if (viewMode == LibraryViewMode.SHELVES) {
            deriveLibraryShelfGroups(books, filtered, filterActive, shelfLabels)
        } else {
            emptyList()
        }
    }

    // Headers and books share one lazy viewport, including landscape and large-text layouts.
    LazyVerticalGrid(
        columns = if (viewMode == LibraryViewMode.GALLERY) {
            GridCells.Adaptive(galleryCellMeasureDp(archiveLayout.galleryMinCellDp, LocalDensity.current.fontScale).dp)
        } else {
            GridCells.Fixed(1)
        },
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = books.size + filtered.size,
                intensity = 0.88f + atmosphereState.archiveDensity * 0.12f
            )
            .libraryArchiveAtmosphere(
                state = atmosphereState,
                seed = books.size * 31 + collections.size * 7,
                timePhase = archiveTimePhase
            ),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        verticalArrangement = Arrangement.spacedBy(if (viewMode == LibraryViewMode.INDEX) 0.dp else VeilSpacing.md),
        contentPadding = PaddingValues(
            start = archiveLayout.horizontalPaddingDp.dp,
            end = archiveLayout.horizontalPaddingDp.dp,
            top = VeilSpacing.xs,
            bottom = 24.dp
        )
    ) {
        item(key = "library:heading", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LibraryHeader(
                    bookCount = books.size,
                    isImporting = isImporting,
                    onImport = { launcher.launch(arrayOf("application/epub+zip", "application/pdf", "application/vnd.comicbook+zip", "application/x-cbz", "application/zip")) },
                    onOpenSettings = onOpenSettings,
                    retrievalActive = query.isNotBlank()
                )
            }
        }

        item(key = "library:search", span = { GridItemSpan(maxLineSpan) }) {
            val searchAccessibilityLabel = stringResource(R.string.library_search_hint)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = MaterialTheme.typography.bodyMedium.withVeilContentScript(query),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                placeholder = {
                    Text(
                        searchAccessibilityLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                    )
                },
                leadingIcon = {
                    SearchIcon(
                        Modifier.size(18.dp),
                        if (query.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else VeilPalette.Brass
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(
                            onClick = { query = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.library_search_clear), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VeilPalette.Brass.copy(alpha = 0.78f),
                    unfocusedBorderColor = VeilPalette.Brass.copy(alpha = 0.28f),
                    focusedContainerColor = VeilPalette.Ink.copy(alpha = 0.88f),
                    unfocusedContainerColor = VeilPalette.Archive.copy(alpha = 0.78f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VeilSpacing.sm)
                    .semantics { contentDescription = searchAccessibilityLabel }
            )
        }

        if (isImporting) {
            item(key = "library:import-status", span = { GridItemSpan(maxLineSpan) }) {
                LibraryImportStatus()
            }
        }

        item(key = "library:status-shelves", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = VeilSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                ) {
                    LibraryShelfCard(
                        title = stringResource(R.string.library_shelf_favorites),
                        count = books.count { it.favorite },
                        selected = shelf == "Favorites",
                        onClick = { shelf = if (shelf == "Favorites") "All" else "Favorites" }
                    )
                    LibraryShelfCard(
                        title = stringResource(R.string.library_shelf_reading),
                        count = books.count { !it.finished && it.progress > 0f },
                        selected = shelf == "Reading",
                        onClick = { shelf = if (shelf == "Reading") "All" else "Reading" }
                    )
                    LibraryShelfCard(
                        title = stringResource(R.string.library_shelf_completed),
                        count = books.count { it.finished },
                        selected = shelf == "Finished",
                        onClick = { shelf = if (shelf == "Finished") "All" else "Finished" }
                    )
                    LibraryShelfCard(
                        title = stringResource(R.string.library_shelf_deep),
                        count = memoryState.deepShelfBookIds.size,
                        selected = shelf == "Deep Shelf",
                        onClick = {
                            shelf = if (shelf == "Deep Shelf") "All" else "Deep Shelf"
                        }
                    )
                    LibraryShelfCard(
                        title = stringResource(R.string.library_shelf_unread),
                        count = books.count { !it.finished && it.progress <= 0f },
                        selected = shelf == "Unread",
                        onClick = { shelf = if (shelf == "Unread") "All" else "Unread" }
                    )
                }
            }
        }

        item(key = "library:controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BrassRule(Modifier.fillMaxWidth())
                ViewModeToggle(mode = viewMode, onChange = { viewModeName = it.name })

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.library_filtered_volume_count, filtered.size),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    if (collections.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { collectionMenu = true },
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
                                )
                            ) {
                                Text(
                                    if (collection.isBlank()) stringResource(R.string.library_collection) else collection,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = collectionMenu,
                                onDismissRequest = { collectionMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_all_collections)) },
                                    onClick = { collection = ""; collectionMenu = false }
                                )
                                collections.forEach { label ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { collection = label; collectionMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    if (wingState.seriesWings.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { seriesMenu = true },
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                contentPadding = PaddingValues(horizontal = VeilSpacing.sm),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    stringResource(R.string.library_group_series),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            DropdownMenu(
                                expanded = seriesMenu,
                                onDismissRequest = { seriesMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_all_series)) },
                                    onClick = { seriesFilter = ""; seriesMenu = false }
                                )
                                wingState.seriesWings.forEach { wing ->
                                    DropdownMenuItem(
                                        text = { Text(wing.name) },
                                        onClick = {
                                            seriesFilter = wing.name
                                            collection = ""
                                            seriesMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        val sortLabel = when (sort) {
                            "Archive Depth" -> stringResource(R.string.library_sort_archive_depth)
                            "Title" -> stringResource(R.string.library_sort_title)
                            "Author" -> stringResource(R.string.library_sort_author)
                            "Series" -> stringResource(R.string.library_sort_series)
                            "Progress" -> stringResource(R.string.library_sort_progress)
                            else -> stringResource(R.string.library_sort_recent)
                        }
                        val sortDescription = stringResource(R.string.library_sort_books, sortLabel)
                        OutlinedButton(
                            onClick = { sortMenu = true },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = sortDescription },
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
                            )
                        ) {
                            Text(
                                sortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenu,
                            onDismissRequest = { sortMenu = false }
                        ) {
                            listOf(
                                "Recent" to R.string.library_sort_recent,
                                "Archive Depth" to R.string.library_sort_archive_depth,
                                "Title" to R.string.library_sort_title,
                                "Author" to R.string.library_sort_author,
                                "Series" to R.string.library_sort_series,
                                "Progress" to R.string.library_sort_progress
                            ).forEach { (key, labelRes) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(labelRes)) },
                                    onClick = { sort = key; sortMenu = false }
                                )
                            }
                        }
                    }

                    if (seriesFilter.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { seriesFilter = "" },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            border = BorderStroke(
                                1.dp,
                                VeilPalette.Brass.copy(alpha = 0.44f)
                            )
                        ) {
                            Text(
                                stringResource(R.string.library_series_filter, seriesFilter),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (
                        trimmedQuery.isNotBlank() ||
                        shelf != "All" ||
                        collection.isNotEmpty() ||
                        seriesFilter.isNotEmpty()
                    ) {
                        TextButton(
                            onClick = {
                                query = ""
                                shelf = "All"
                                collection = ""
                                seriesFilter = ""
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.Brass)
                        ) {
                            Text(stringResource(R.string.common_reset), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "library:empty", span = { GridItemSpan(maxLineSpan) }) {
                LibraryEmptyState(
                    hasBooks = books.isNotEmpty(),
                    isImporting = isImporting,
                    onImport = { launcher.launch(arrayOf("application/epub+zip", "application/pdf", "application/vnd.comicbook+zip", "application/x-cbz", "application/zip")) },
                    onReset = {
                        query = ""
                        shelf = "All"
                        collection = ""
                        seriesFilter = ""
                    }
                )
            }
        } else {
            when (viewMode) {
                LibraryViewMode.GALLERY -> {
                    items(filtered, key = { "gallery:${it.id}" }, contentType = { "gallery" }) { book ->
                        BookLibraryTile(
                            book = book,
                            archiveMemory = memoryState.memoryFor(book.id),
                            artifactMemory = artifactMemoryByBookId[book.id],
                            onOpen = { onOpenBook(book) },
                            onFavorite = { onFavorite(book.id) },
                            onDetails = { detailBookId = book.id }
                        )
                    }
                }

                LibraryViewMode.INDEX -> {
                    items(filtered, key = { "index:${it.id}" }, contentType = { "index" }) { book ->
                        BookLibraryRow(
                            book = book,
                            archiveMemory = memoryState.memoryFor(book.id),
                            artifactMemory = artifactMemoryByBookId[book.id],
                            showMemorySummary = archiveLayout.showIndexMemorySummary,
                            onOpen = { onOpenBook(book) },
                            onFavorite = { onFavorite(book.id) },
                            onDetails = { detailBookId = book.id }
                        )
                    }
                }

                LibraryViewMode.SHELVES -> {
                    item(key = "library:shelves-mode", span = { GridItemSpan(maxLineSpan) }) {
                        LibraryShelvesView(
                            groups = shelfGroups,
                            artifactMemoryByBookId = artifactMemoryByBookId,
                            itemWidthDp = archiveLayout.shelfItemWidthDp,
                            coverWidthDp = archiveLayout.shelfCoverWidthDp,
                            coverHeightDp = archiveLayout.shelfCoverHeightDp,
                            onOpen = onOpenBook,
                            onDetails = { detailBookId = it.id }
                        )
                    }
                }
            }
        }
        item(key = "library:manga-portal", span = { GridItemSpan(maxLineSpan) }) {
            MangaLibraryPortal(
                localComicCount = books.count {
                    it.format == com.veilreader.app.domain.BookFormat.COMIC
                },
                onOpenManga = onOpenManga
            )
        }

        item(key = "library:wings", span = { GridItemSpan(maxLineSpan) }) {
            if (
                viewMode != LibraryViewMode.SHELVES &&
                trimmedQuery.isBlank() &&
                shelf == "All" &&
                wingState.allWings.isNotEmpty()
            ) {
                LibraryArchiveWings(
                    state = wingState,
                    selectedCollection = collection,
                    selectedSeries = seriesFilter,
                    onCollection = { name ->
                        collection = if (collection.equals(name, ignoreCase = true)) "" else name
                        seriesFilter = ""
                    },
                    onSeries = { name ->
                        seriesFilter = if (seriesFilter.equals(name, ignoreCase = true)) "" else name
                        collection = ""
                    }
                )
            }
        }

        item(key = "library:memory-returns", span = { GridItemSpan(maxLineSpan) }) {
            if (
                viewMode != LibraryViewMode.INDEX &&
                memoryState.events.isNotEmpty() &&
                trimmedQuery.isBlank() &&
                shelf == "All" &&
                collection.isEmpty() &&
            seriesFilter.isEmpty()
            ) {
                Column(
                    Modifier.padding(vertical = VeilSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    LibrarySectionHeading(
                        eyebrow = stringResource(R.string.library_memory_eyebrow),
                        title = stringResource(R.string.library_memory_title),
                        trailing = stringResource(R.string.library_memory_traces, memoryState.events.size)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        memoryState.events.take(4).forEach { event ->
                            val eventBook = booksById[event.bookId]
                            if (eventBook != null) {
                                MemoryReturnCard(
                                    event = event,
                                    book = eventBook,
                                    onInspect = { detailBookId = eventBook.id }
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "library:deep-shelf-gate", span = { GridItemSpan(maxLineSpan) }) {
            if (
                viewMode != LibraryViewMode.INDEX &&
                memoryState.deepShelfBookIds.isNotEmpty() &&
                trimmedQuery.isBlank() &&
                shelf == "All" &&
                collection.isEmpty() &&
            seriesFilter.isEmpty()
            ) {
                val oldestBook = memoryState.deepShelfBookIds
                    .firstOrNull()
                    ?.let(booksById::get)

                DeepShelfPortal(
                    count = memoryState.deepShelfBookIds.size,
                    oldestBook = oldestBook,
                    oldestMemory = oldestBook?.let { memoryState.memoryFor(it.id) },
                    onOpen = {
                        query = ""
                        collection = ""
                        shelf = "Deep Shelf"
                        sort = "Archive Depth"
                    }
                )
            }
        }

        item(key = "library:recent", span = { GridItemSpan(maxLineSpan) }) {
            if (
                viewMode == LibraryViewMode.GALLERY &&
                recentReading.isNotEmpty() &&
                trimmedQuery.isBlank() &&
                shelf == "All" &&
                collection.isEmpty()
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(vertical = VeilSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        LibrarySectionHeading(
                            eyebrow = stringResource(R.string.library_recent_eyebrow),
                            title = stringResource(R.string.library_recent_title),
                            trailing = stringResource(R.string.library_recent_active, recentReading.size)
                        )
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                        ) {
                            recentReading.forEach { book ->
                                RecentReadingBook(
                                    book = book,
                                    artifactMemory = artifactMemoryByBookId[book.id],
                                    onOpen = { onOpenBook(book) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "library:atmosphere-record", span = { GridItemSpan(maxLineSpan) }) {
            LibraryAtmosphereLedger(atmosphereState)
        }
        item(key = "library:overview", span = { GridItemSpan(maxLineSpan) }) {
            if (books.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { overviewExpanded = !overviewExpanded }) {
                        Text(
                            stringResource(
                                if (overviewExpanded) R.string.library_hide_overview
                                else R.string.library_show_overview
                            )
                        )
                    }
                    if (overviewExpanded) {
                        ArchiveOverview(
                            total = books.size,
                            reading = books.count { !it.finished && it.progress > 0f },
                            finished = books.count { it.finished },
                            collections = collections.size,
                            modifier = Modifier.padding(top = VeilSpacing.xs)
                        )
                    }
                }
            }
        }
    }

    detailBook?.let { book ->
        BookDetailDestination(
            book = book,
            archiveMemory = memoryState.memoryFor(book.id),
            artifactMemory = artifactMemoryByBookId[book.id],
            readingCycles = readingCycles.filter { it.bookId == book.id },
            readingMilestones = readingMilestones.filter { it.bookId == book.id },
            preservedHighlights = highlights
                .filter { it.bookId == book.id }
                .sortedByDescending { it.createdAtEpochMs },
            onDismiss = { detailBookId = null },
            onOpen = {
                detailBookId = null
                onOpenBook(book)
            },
            onFavorite = { onFavorite(book.id) },
            onEditMetadata = {
                detailBookId = null
                beginMetadataEdit(book)
            },
            onDelete = {
                deletingBookId = book.id
            }
        )
    }

    deletingBookId
        ?.let(booksById::get)
        ?.let { book ->
            DeleteBookDialog(
                book = book,
                onDismiss = { deletingBookId = null },
                onConfirm = {
                    deletingBookId = null
                    detailBookId = null
                    onDeleteBook(book)
                }
            )
        }

    editing?.let { book ->
        val parsedSeriesIndex = seriesIndex
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let(::parseLocalizedDecimalInput)
        val seriesIndexInvalid =
            seriesIndex.isNotBlank() &&
                (parsedSeriesIndex == null || !parsedSeriesIndex.isFinite())

        Dialog(
            onDismissRequest = { editing = null },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .padding(VeilSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxWidth()
                        .heightIn(max = 720.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = VeilPalette.Archive,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.50f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box {
                        GrayfogOrnamentFrame(
                            modifier = Modifier.matchParentSize(),
                            strength = 0.26f
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(VeilSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                        ) {
                            VeilMicroLabel(
                                text = stringResource(R.string.library_archive_record),
                                strong = true
                            )
                            Text(
                                stringResource(R.string.book_metadata_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = VeilPalette.Moon
                            )
                            BrassRule(Modifier.fillMaxWidth())

                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text(stringResource(R.string.book_metadata_title)) },
                                    isError = title.isBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = author,
                                    onValueChange = { author = it },
                                    label = { Text(stringResource(R.string.book_metadata_author)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = collectionNames,
                                    onValueChange = { collectionNames = it },
                                    label = { Text(stringResource(R.string.book_metadata_collections)) },
                                    supportingText = {
                                        Text(stringResource(R.string.book_metadata_collections_hint))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = seriesName,
                                    onValueChange = { seriesName = it },
                                    label = { Text(stringResource(R.string.book_metadata_series)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = seriesIndex,
                                    onValueChange = { seriesIndex = it },
                                    label = {
                                        Text(stringResource(R.string.book_metadata_series_number))
                                    },
                                    isError = seriesIndexInvalid,
                                    supportingText = {
                                        if (seriesIndexInvalid) {
                                            Text(
                                                stringResource(
                                                    R.string.book_metadata_series_number_error
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = language,
                                    onValueChange = { language = it },
                                    label = { Text(stringResource(R.string.book_metadata_language)) },
                                    supportingText = {
                                        Text(stringResource(R.string.book_metadata_language_hint))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            BrassRule(Modifier.fillMaxWidth())

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                            ) {
                                OutlinedButton(
                                    onClick = { editing = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                                Button(
                                    enabled = title.isNotBlank() && !seriesIndexInvalid,
                                    onClick = {
                                        onEditMetadata(
                                            BookMetadataUpdate(
                                                bookId = book.id,
                                                title = title,
                                                author = author,
                                                collections = parseCollectionNames(collectionNames),
                                                seriesName = seriesName
                                                    .trim()
                                                    .takeIf { it.isNotEmpty() },
                                                seriesIndex = parsedSeriesIndex,
                                                language = language
                                                    .trim()
                                                    .takeIf { it.isNotEmpty() }
                                            )
                                        )
                                        editing = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VeilPalette.Brass,
                                        contentColor = Color(0xFF17120A)
                                    )
                                ) {
                                    Text(stringResource(R.string.common_save))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteBookDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
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
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = VeilPalette.Archive,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.46f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(VeilSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                ) {
                    Text(
                        stringResource(R.string.book_delete_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        stringResource(R.string.book_delete_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.book_delete_body, book.title),
                        style = MaterialTheme.typography.bodyMedium.withVeilContentScript(stringResource(R.string.book_delete_body, book.title)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BrassRule(Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(stringResource(R.string.book_delete_cancel))
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.book_delete_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookDetailEyebrow(
    text: String,
    modifier: Modifier = Modifier
) {
    VeilMicroLabel(
        text = text,
        modifier = modifier,
        strong = true
    )
}

@Composable
private fun BookDetailArtifactStand(
    book: Book,
    artifactMemory: BookArtifactMemory?,
    modifier: Modifier = Modifier
) {
    val artifact = bookArtifactState(book, memory = artifactMemory)
    val fieldColor = when {
        artifact.recentlyOpened -> VeilPalette.Spirit
        artifact.finished -> VeilPalette.Brass
        artifact.favorite -> VeilPalette.Brass.copy(alpha = 0.92f)
        else -> VeilPalette.Mist
    }

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                listOf(
                    fieldColor.copy(alpha = 0.07f),
                    VeilPalette.Ink.copy(alpha = 0.03f),
                    Color.Transparent
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.76f)
                .height(10.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            fieldColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.64f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            VeilPalette.Brass.copy(alpha = 0.56f),
                            Color.Transparent
                        )
                    )
                )
        )

        BookCover(
            title = book.title,
            subtitle = book.author.takeIf { it.isNotBlank() },
            imagePath = book.coverCachePath,
            artifact = artifact,
            focusArtifact = true,
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .aspectRatio(0.69f)
                .align(Alignment.TopCenter)
                .offset(y = 2.dp)
        )
    }
}

internal data class BookDetailAdaptivePolicy(
    val compactHero: Boolean,
    val stackUtilityActions: Boolean
)

internal fun bookDetailAdaptivePolicy(
    widthDp: Int,
    fontScale: Float
): BookDetailAdaptivePolicy {
    val safeWidth = widthDp.coerceAtLeast(0)
    val safeScale = if (fontScale.isFinite() && fontScale > 0f) fontScale else 1f
    return BookDetailAdaptivePolicy(
        compactHero = safeWidth / safeScale.coerceAtLeast(1f) < com.veilreader.app.ui.theme.VeilComposition.ArtifactIdentityMinWidthDp,
        stackUtilityActions = shouldStackDenseChoices(
            widthDp = safeWidth,
            fontScale = safeScale,
            optionCount = 2
        )
    )
}

internal enum class BookDetailJourneyPhase {
    NOT_STARTED,
    READING,
    COMPLETED
}

internal enum class BookDetailJourneyAction {
    OPEN,
    CONTINUE,
    READ_AGAIN,
    UNAVAILABLE
}

internal data class BookDetailJourneyState(
    val progress: Float,
    val phase: BookDetailJourneyPhase,
    val action: BookDetailJourneyAction,
    val chapter: String?
)

internal fun bookDetailJourneyState(
    book: Book,
    progress: Float
): BookDetailJourneyState {
    val safeProgress = when {
        book.finished -> 1f
        progress.isFinite() -> progress.coerceIn(0f, 1f)
        else -> 0f
    }
    val phase = when {
        book.finished -> BookDetailJourneyPhase.COMPLETED
        safeProgress > 0f -> BookDetailJourneyPhase.READING
        else -> BookDetailJourneyPhase.NOT_STARTED
    }
    val action = when {
        !book.isImported -> BookDetailJourneyAction.UNAVAILABLE
        phase == BookDetailJourneyPhase.COMPLETED -> BookDetailJourneyAction.READ_AGAIN
        phase == BookDetailJourneyPhase.READING -> BookDetailJourneyAction.CONTINUE
        else -> BookDetailJourneyAction.OPEN
    }
    val chapter = book.currentChapter
        .trim()
        .takeIf {
            phase == BookDetailJourneyPhase.READING &&
                it.isNotBlank() &&
                !it.equals("Not started", ignoreCase = true)
        }

    return BookDetailJourneyState(
        progress = safeProgress,
        phase = phase,
        action = action,
        chapter = chapter
    )
}

internal enum class BookDetailArchiveEventKind {
    ARCHIVED,
    FIRST_OPENED,
    MILESTONE,
    COMPLETED
}

internal data class BookDetailArchiveEvent(
    val id: String,
    val kind: BookDetailArchiveEventKind,
    val timestampEpochMs: Long,
    val progression: Float? = null,
    val cycleIndex: Int? = null
)

internal fun bookDetailArchiveTimeline(
    book: Book,
    milestones: List<ReadingMilestoneRecord>,
    cycles: List<ReadingCycleRecord>
): List<BookDetailArchiveEvent> {
    val events = mutableListOf<BookDetailArchiveEvent>()

    book.addedAtEpochMs.takeIf { it > 0L }?.let { archivedAt ->
        events += BookDetailArchiveEvent(
            id = "archived:${book.id}",
            kind = BookDetailArchiveEventKind.ARCHIVED,
            timestampEpochMs = archivedAt
        )
    }

    milestones
        .asSequence()
        .filter {
            it.kind == ReadingMilestoneKind.FIRST_OPENED &&
                it.reachedAtEpochMs > 0L
        }
        .minWithOrNull(compareBy<ReadingMilestoneRecord> { it.reachedAtEpochMs }.thenBy { it.id })
        ?.let { firstOpened ->
            events += BookDetailArchiveEvent(
                id = firstOpened.id,
                kind = BookDetailArchiveEventKind.FIRST_OPENED,
                timestampEpochMs = firstOpened.reachedAtEpochMs
            )
        }

    milestones
        .asSequence()
        .filter {
            it.kind != ReadingMilestoneKind.FIRST_OPENED &&
                it.reachedAtEpochMs > 0L &&
                it.progression.isFinite() &&
                it.progression > 0f &&
                it.progression <= 1f
        }
        .groupBy { it.kind }
        .values
        .mapNotNull { records ->
            records.minWithOrNull(
                compareBy<ReadingMilestoneRecord> { it.reachedAtEpochMs }.thenBy { it.id }
            )
        }
        .forEach { milestone ->
            events += BookDetailArchiveEvent(
                id = milestone.id,
                kind = BookDetailArchiveEventKind.MILESTONE,
                timestampEpochMs = milestone.reachedAtEpochMs,
                progression = milestone.progression
            )
        }

    cycles
        .asSequence()
        .filter { it.cycleIndex >= 1 && it.completedAtEpochMs > 0L }
        .groupBy { it.cycleIndex }
        .values
        .mapNotNull { records ->
            records.minWithOrNull(
                compareBy<ReadingCycleRecord> { it.completedAtEpochMs }.thenBy { it.id }
            )
        }
        .forEach { cycle ->
            events += BookDetailArchiveEvent(
                id = cycle.id,
                kind = BookDetailArchiveEventKind.COMPLETED,
                timestampEpochMs = cycle.completedAtEpochMs,
                cycleIndex = cycle.cycleIndex
            )
        }

    return events.sortedWith(
        compareBy<BookDetailArchiveEvent> { it.timestampEpochMs }
            .thenBy { it.kind.ordinal }
            .thenBy { it.id }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailDestination(
    book: Book,
    archiveMemory: BookArchiveMemory?,
    artifactMemory: BookArtifactMemory?,
    readingCycles: List<ReadingCycleRecord>,
    readingMilestones: List<ReadingMilestoneRecord>,
    preservedHighlights: List<Highlight>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onEditMetadata: () -> Unit,
    onDelete: () -> Unit
) {
    val formatPercent = rememberVeilPercentFormatter()
    val artifactState = bookArtifactState(book, memory = artifactMemory)
    val journey = remember(book, artifactState.progress) {
        bookDetailJourneyState(
            book = book,
            progress = artifactState.progress
        )
    }
    val preservedMemory = remember(preservedHighlights) {
        bookDetailPreservedMemory(preservedHighlights)
    }
    val archiveTimeline = remember(book, readingMilestones, readingCycles) {
        bookDetailArchiveTimeline(
            book = book,
            milestones = readingMilestones,
            cycles = readingCycles
        )
    }
    val status = when (journey.phase) {
        BookDetailJourneyPhase.COMPLETED ->
            stringResource(R.string.book_detail_finished)
        BookDetailJourneyPhase.READING ->
            stringResource(
                R.string.book_detail_percent_read_text,
                formatPercent(journey.progress)
            )
        BookDetailJourneyPhase.NOT_STARTED ->
            stringResource(R.string.book_detail_not_started)
    }
    val primaryAction = when (journey.action) {
        BookDetailJourneyAction.READ_AGAIN ->
            stringResource(R.string.book_detail_read_again)
        BookDetailJourneyAction.CONTINUE ->
            stringResource(R.string.book_detail_continue_reading)
        BookDetailJourneyAction.OPEN ->
            stringResource(R.string.book_detail_open_book)
        BookDetailJourneyAction.UNAVAILABLE ->
            stringResource(R.string.book_detail_publication_unavailable)
    }

    @Composable
    fun ReadingAction() {
        Button(
            onClick = onOpen,
            enabled = journey.action != BookDetailJourneyAction.UNAVAILABLE,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = VeilMaterials.Parchment,
                contentColor = VeilMaterials.Ink
            )
        ) {
            Text(primaryAction)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.veilreader.app.ui.VeilSystemBars(lightBackground = false)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VeilPalette.Ink,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .grayfogAtmosphere(
                        realm = VeilRealm.ARCHIVE,
                        seed = book.id.hashCode(),
                        intensity = 1f
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = VeilSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
                ) {
            BoxWithConstraints(
                Modifier
                    .widthIn(max = com.veilreader.app.ui.theme.VeilMeasure.ArchiveContent)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = 356.dp)
            ) {
                val heroPolicy = bookDetailAdaptivePolicy(
                    widthDp = maxWidth.value.toInt(),
                    fontScale = LocalConfiguration.current.fontScale
                )
                val compact = heroPolicy.compactHero

                Image(
                    painter = painterResource(R.drawable.grayfog_threshold_v1),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to VeilPalette.Ink.copy(alpha = 0.34f),
                                0.44f to VeilPalette.Ink.copy(alpha = 0.78f),
                                1f to VeilPalette.Ink
                            )
                        )
                )

                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    VeilPalette.Ink.copy(alpha = 0.46f),
                                    Color.Transparent,
                                    VeilPalette.Ink.copy(alpha = 0.34f)
                                )
                            )
                        )
                )

                GrayfogOrnamentFrame(
                    modifier = Modifier.matchParentSize(),
                    strength = 0.34f
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = if (compact) VeilSpacing.lg else VeilSpacing.xxl,
                            vertical = VeilSpacing.lg
                        ),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookDetailEyebrow(
                            text = stringResource(
                                R.string.book_detail_artifact_chamber,
                                localizedBookFormatLabel(book.format)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = VeilPalette.Moon
                            )
                        ) {
                            Text(
                                stringResource(R.string.book_detail_close),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (compact) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                        ) {
                            BookDetailArtifactStand(
                                book = book,
                                artifactMemory = artifactMemory,
                                modifier = Modifier
                                    .width(184.dp)
                                    .height(260.dp)
                            )
                            BookDetailIdentity(
                                book = book,
                                artifactMemory = artifactMemory,
                                modifier = Modifier
                                    .widthIn(max = 440.dp)
                                    .fillMaxWidth(),
                                readingAction = { ReadingAction() }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xl),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookDetailArtifactStand(
                                book = book,
                                artifactMemory = artifactMemory,
                                modifier = Modifier
                                    .width(224.dp)
                                    .height(316.dp)
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)) {
                                BookDetailIdentity(
                                    book = book,
                                    artifactMemory = artifactMemory,
                                    modifier = Modifier.fillMaxWidth(),
                                    readingAction = { ReadingAction() }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = VeilSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BookDetailEyebrow(
                                text = stringResource(R.string.book_detail_current_journey)
                            )
                            Text(
                                stringResource(R.string.book_detail_reading_progress),
                                style = MaterialTheme.typography.titleMedium,
                                color = VeilPalette.Moon
                            )
                        }
                        Text(
                            formatPercent(journey.progress),
                            style = MaterialTheme.typography.titleSmall,
                            color = VeilPalette.Brass
                        )
                    }

                    LinearProgressIndicator(
                        progress = { journey.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = VeilPalette.Brass,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f),
                        drawStopIndicator = {}
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            status,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        journey.chapter?.let { chapter ->
                            Text(
                                chapter,
                                style = MaterialTheme.typography.labelMedium,
                                color = VeilPalette.Brass.copy(alpha = 0.82f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }


                BookDetailUtilityActions(
                    favorite = book.favorite,
                    onFavorite = onFavorite,
                    onEditMetadata = onEditMetadata
                )

                BrassRule(Modifier.fillMaxWidth())

                if (preservedMemory.totalUseful > 0) {
                    BookDetailFragments(
                        memory = preservedMemory
                    )
                    BrassRule(Modifier.fillMaxWidth())
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    BookDetailEyebrow(
                        text = stringResource(R.string.book_detail_archive_history)
                    )

                    if (archiveTimeline.isNotEmpty()) {
                        BookDetailArchiveTimeline(events = archiveTimeline)
                        BrassRule(Modifier.fillMaxWidth().padding(vertical = VeilSpacing.xs))
                    }

                    BookDetailFact(
                        stringResource(R.string.book_detail_format),
                        localizedBookFormatLabel(book.format)
                    )
                    book.language?.trim()?.takeIf { it.isNotBlank() }?.let {
                        BookDetailFact(stringResource(R.string.book_detail_language), it)
                    }
                    if (book.totalPages > 0) {
                        BookDetailFact(
                            stringResource(R.string.book_detail_pages),
                            stringResource(
                                R.string.book_detail_page_progress,
                                book.pagesRead.coerceAtLeast(0).coerceAtMost(book.totalPages),
                                book.totalPages
                            )
                        )
                    }
                    BookDetailFact(
                        stringResource(R.string.book_detail_stored),
                        if (book.isImported) {
                            stringResource(R.string.book_detail_private_local_copy)
                        } else {
                            stringResource(R.string.book_detail_sample_metadata_only)
                        }
                    )
                    if (readingCycles.size > 1) {
                        BookDetailFact(
                            stringResource(R.string.book_detail_reading_cycles),
                            readingCycles.map { it.cycleIndex }.filter { it >= 1 }.distinct().size.toString()
                        )
                    }
                    archiveMemory?.let { memory ->
                        BookDetailFact(
                            stringResource(R.string.book_detail_archive_depth),
                            archiveDepthRecord(memory)
                        )
                    }
                    artifactMemory?.takeIf {
                        it.sessionCount > 0 ||
                            it.highlightCount > 0 ||
                            it.bookmarkCount > 0
                    }?.let { material ->
                        val pieces = mutableListOf<String>()
                        if (material.sessionCount > 0) {
                            pieces += stringResource(
                                if (material.sessionCount == 1) R.string.book_detail_session_one
                                else R.string.book_detail_session_many,
                                material.sessionCount
                            )
                        }
                        if (material.highlightCount > 0) {
                            pieces += stringResource(
                                if (material.highlightCount == 1) R.string.book_detail_passage_one
                                else R.string.book_detail_passage_many,
                                material.highlightCount
                            )
                        }
                        if (material.bookmarkCount > 0) {
                            pieces += stringResource(
                                if (material.bookmarkCount == 1) R.string.book_detail_place_one
                                else R.string.book_detail_place_many,
                                material.bookmarkCount
                            )
                        }
                        BookDetailFact(
                            stringResource(R.string.book_detail_material_memory),
                            pieces.joinToString(" · ")
                        )
                    }
                }

                if (book.allCollections.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        BookDetailEyebrow(
                            text = stringResource(R.string.book_detail_collections)
                        )

                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                        ) {
                            book.allCollections.forEach { collection ->
                                Text(collection, style = MaterialTheme.typography.bodyMedium,
                                    color = VeilMaterials.TextSecondary)
                            }
                        }
                    }
                }

                if (book.isImported) {
                    BookDetailDestructiveActions(onDelete = onDelete)
                }
            }
                }
            }
        }
    }
}

internal enum class PreservedMemoryKind { QUOTE_ONLY, NOTE_ONLY, QUOTE_AND_NOTE }

internal data class PreservedMemoryFragment(
    val id: String,
    val quote: String?,
    val note: String?,
    val createdAtEpochMs: Long,
    val kind: PreservedMemoryKind
)

internal data class BookDetailPreservedMemory(
    val fragments: List<PreservedMemoryFragment>,
    val totalUseful: Int
) {
    val hiddenCount: Int get() = (totalUseful - fragments.size).coerceAtLeast(0)
}

internal fun bookDetailPreservedMemory(
    highlights: List<Highlight>,
    sampleLimit: Int = 3
): BookDetailPreservedMemory {
    val useful = highlights.mapNotNull { highlight ->
        val quote = highlight.quote.trim().takeIf(String::isNotBlank)
        val note = highlight.note.trim().takeIf(String::isNotBlank)
        if (quote == null && note == null) return@mapNotNull null
        PreservedMemoryFragment(
            id = highlight.id,
            quote = quote,
            note = note,
            createdAtEpochMs = highlight.createdAtEpochMs,
            kind = when {
                quote != null && note != null -> PreservedMemoryKind.QUOTE_AND_NOTE
                quote != null -> PreservedMemoryKind.QUOTE_ONLY
                else -> PreservedMemoryKind.NOTE_ONLY
            }
        )
    }.sortedWith(
        compareByDescending<PreservedMemoryFragment> { it.createdAtEpochMs }.thenBy { it.id }
    )
    return BookDetailPreservedMemory(
        fragments = useful.take(sampleLimit.coerceAtLeast(0)),
        totalUseful = useful.size
    )
}

@Composable
private fun BookDetailUtilityActions(
    favorite: Boolean,
    onFavorite: () -> Unit,
    onEditMetadata: () -> Unit
) {
    VeilAdaptiveDialogActions(
        modifier = Modifier.fillMaxWidth(),
        spacing = VeilSpacing.sm,
        first = { actionModifier ->
            TextButton(
                onClick = onFavorite,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = actionModifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (favorite) VeilPalette.Brass else VeilPalette.Mist
                )
            ) {
                Text(
                    if (favorite) {
                        stringResource(R.string.book_detail_favorited)
                    } else {
                        stringResource(R.string.book_detail_favorite)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        second = { actionModifier ->
            TextButton(
                onClick = onEditMetadata,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = actionModifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.Mist)
            ) {
                Text(
                    stringResource(R.string.book_detail_edit_details),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
private fun BookDetailDestructiveActions(onDelete: () -> Unit) {
    BrassRule(Modifier.fillMaxWidth())
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
    ) {
        VeilMicroLabel(
            text = stringResource(R.string.book_detail_local_copy_controls),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.76f)
        )
        Text(
            stringResource(R.string.book_detail_delete_local_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                stringResource(R.string.book_detail_delete),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BookDetailArchiveTimeline(events: List<BookDetailArchiveEvent>) {
    val formatPercent = rememberVeilPercentFormatter()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        events.forEachIndexed { index, event ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.width(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(VeilPalette.Brass, CircleShape)
                    )
                    if (index < events.lastIndex) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(VeilPalette.Brass.copy(alpha = 0.30f))
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (index < events.lastIndex) VeilSpacing.sm else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        when (event.kind) {
                            BookDetailArchiveEventKind.ARCHIVED ->
                                stringResource(R.string.book_detail_archived)
                            BookDetailArchiveEventKind.FIRST_OPENED ->
                                stringResource(R.string.book_detail_first_opened)
                            BookDetailArchiveEventKind.MILESTONE ->
                                stringResource(
                                    R.string.book_detail_progress_reached,
                                    formatPercent(event.progression ?: 0f)
                                )
                            BookDetailArchiveEventKind.COMPLETED ->
                                stringResource(
                                    R.string.book_detail_cycle_completed,
                                    event.cycleIndex ?: 1
                                )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeilPalette.Moon
                    )
                    Text(
                        formatArchiveRecordDate(event.timestampEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Mist.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookDetailFragments(memory: BookDetailPreservedMemory) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BookDetailEyebrow(text = stringResource(R.string.book_detail_preserved_memory))
                Text(
                    stringResource(R.string.book_detail_preserved_fragments),
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
            }
            Text(
                stringResource(
                    if (memory.totalUseful == 1) R.string.book_detail_passage_one
                    else R.string.book_detail_passage_many,
                    memory.totalUseful
                ),
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Mist.copy(alpha = 0.72f)
            )
        }

        memory.fragments.forEachIndexed { index, fragment ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = VeilSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    formatArchiveRecordDate(fragment.createdAtEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass.copy(alpha = 0.78f)
                )
                fragment.quote?.let { quote ->
                    Text(
                        "“$quote”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VeilPalette.Moon,
                        maxLines = if (fragment.kind == PreservedMemoryKind.QUOTE_ONLY) 6 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                fragment.note?.let { note ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier.width(2.dp).heightIn(min = 30.dp)
                                .background(VeilPalette.Brass.copy(alpha = 0.36f))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (fragment.kind == PreservedMemoryKind.NOTE_ONLY) {
                                Text(
                                    stringResource(R.string.book_detail_preserved_note_only),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VeilPalette.Brass.copy(alpha = 0.80f)
                                )
                            }
                            Text(
                                note,
                                style = if (fragment.kind == PreservedMemoryKind.NOTE_ONLY) {
                                    MaterialTheme.typography.bodyLarge
                                } else MaterialTheme.typography.bodySmall,
                                color = if (fragment.kind == PreservedMemoryKind.NOTE_ONLY) {
                                    VeilPalette.Moon
                                } else VeilPalette.Mist.copy(alpha = 0.82f),
                                maxLines = if (fragment.kind == PreservedMemoryKind.NOTE_ONLY) 5 else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (index < memory.fragments.lastIndex) {
                    BrassRule(Modifier.fillMaxWidth().padding(top = VeilSpacing.xs))
                }
            }
        }

        if (memory.hiddenCount > 0) {
            Text(
                stringResource(R.string.book_detail_more_preserved, memory.hiddenCount),
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Spirit.copy(alpha = 0.72f)
            )
        }
    }
}

internal data class BookDetailIdentityText(
    val title: String,
    val author: String,
    val seriesName: String?,
    val seriesIndex: Double?,
    val collections: List<String>
)

internal fun bookDetailIdentityText(
    book: Book,
    untitledBook: String,
    unknownAuthor: String
): BookDetailIdentityText {
    val seriesName = book.seriesName?.trim()?.takeIf(String::isNotBlank)
    return BookDetailIdentityText(
        title = book.title.trim().ifBlank { untitledBook },
        author = book.author.trim().ifBlank { unknownAuthor },
        seriesName = seriesName,
        seriesIndex = book.seriesIndex?.takeIf { seriesName != null && it.isFinite() },
        collections = book.allCollections
    )
}

@Composable
private fun BookDetailIdentity(
    book: Book,
    artifactMemory: BookArtifactMemory?,
    modifier: Modifier = Modifier,
    readingAction: @Composable () -> Unit
) {
    val formatNumber = rememberVeilNumberFormatter()
    val untitledBook = stringResource(R.string.common_untitled_book)
    val unknownAuthor = stringResource(R.string.common_unknown_author)
    val identity = remember(book, untitledBook, unknownAuthor) {
        bookDetailIdentityText(
            book = book,
            untitledBook = untitledBook,
            unknownAuthor = unknownAuthor
        )
    }
    val visibleCollections = identity.collections.take(2)
    val hiddenCollectionCount = identity.collections.size - visibleCollections.size

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            identity.title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge.withVeilContentScript(identity.title),
            color = VeilPalette.Moon,
            maxLines = 4,
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            identity.author,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall.withVeilContentScript(identity.author),
            color = VeilPalette.Moon.copy(alpha = 0.78f),
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )

        readingAction()

        identity.seriesName?.let { series ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    series,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.withVeilContentScript(series),
                    color = VeilPalette.Brass.copy(alpha = 0.90f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                identity.seriesIndex?.let { index ->
                    Text(
                        stringResource(
                            R.string.book_detail_series_index,
                            formatNumber(index)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass.copy(alpha = 0.68f),
                        maxLines = 1
                    )
                }
            }
        }

        if (visibleCollections.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.book_detail_collection_identity,
                    visibleCollections.joinToString(" · ")
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
            if (hiddenCollectionCount > 0) {
                Text(
                    stringResource(
                        R.string.book_detail_more_collections,
                        formatNumber(hiddenCollectionCount)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.70f)
                )
            }
        }

        val recordLabel = localizedBookArtifactRecordLabel(
            bookArtifactState(book, memory = artifactMemory)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VeilMicroLabel(
                text = recordLabel,
                modifier = Modifier.weight(1f),
                color = VeilPalette.Mist.copy(alpha = 0.72f)
            )

            if (book.favorite) {
                VeilMicroLabel(
                    text = stringResource(R.string.book_detail_favorite_badge),
                    color = VeilPalette.Brass,
                    strong = true
                )
            }
        }
    }
}

@Composable
private fun localizedBookArtifactRecordLabel(state: BookArtifactState): String {
    val presence = when (state.readingState) {
        BookReadingState.UNOPENED -> stringResource(R.string.book_detail_record_pristine)
        BookReadingState.ACTIVE -> if (state.progress > 0f) {
            stringResource(R.string.book_detail_record_active)
        } else {
            stringResource(R.string.book_detail_record_opened)
        }
        BookReadingState.FINISHED -> stringResource(R.string.book_detail_record_completed)
    }
    val age = when (state.patina) {
        BookPatina.FRESH -> stringResource(R.string.book_detail_record_fresh)
        BookPatina.SETTLED -> stringResource(R.string.book_detail_record_settled)
        BookPatina.AGED -> stringResource(R.string.book_detail_record_aged)
        BookPatina.ARCHIVAL -> stringResource(R.string.book_detail_record_archival)
    }
    return "$presence · $age"
}

@Composable
private fun BookDetailFact(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Brass.copy(alpha = 0.72f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LibraryHeader(
    bookCount: Int,
    isImporting: Boolean,
    onImport: () -> Unit,
    onOpenSettings: () -> Unit,
    retrievalActive: Boolean = false
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
    ) {
        val compact = maxWidth < 560.dp
        val condensed = retrievalActive || com.veilreader.app.ui.theme.condenseRealmApproach(
            LocalDensity.current.fontScale, with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp().value.toInt()
        })
        val headerHeight = if (condensed) 0.dp else if (compact) 112.dp else 144.dp
        val adjacent = com.veilreader.app.ui.theme.useArchitecturalPair(
            maxWidth.value - VeilSpacing.md.value * 2f, LocalDensity.current.fontScale)
        @Composable fun HeaderActions(modifier: Modifier = Modifier) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VeilPalette.Moon.copy(alpha = 0.86f),
                        containerColor = VeilPalette.Ink.copy(alpha = 0.72f)
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(
                        stringResource(R.string.library_header_settings),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilMaterials.ElevatedSurface,
                        contentColor = VeilPalette.Moon
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(
                        stringResource(
                            if (isImporting) R.string.library_header_importing
                            else R.string.library_header_import
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        @Composable fun HeaderIdentity(modifier: Modifier = Modifier) {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    stringResource(R.string.library_header_title),
                    style = if (condensed) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    color = VeilPalette.Moon
                )
                if (!condensed) {
                    Text(
                        stringResource(R.string.library_header_tagline),
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Moon.copy(alpha = 0.78f)
                    )
                    Box(
                        Modifier
                            .padding(vertical = 4.dp)
                            .width(104.dp)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        VeilPalette.Brass.copy(alpha = 0.92f),
                                        VeilPalette.Brass.copy(alpha = 0.34f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Text(
                        when (bookCount) {
                            0 -> stringResource(R.string.library_header_empty)
                            1 -> stringResource(R.string.library_header_one, bookCount)
                            else -> stringResource(R.string.library_header_many, bookCount)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeilPalette.Moon.copy(alpha = 0.72f)
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().heightIn(min = headerHeight)) {
            Image(
                painter = painterResource(R.drawable.grayfog_threshold_v1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to VeilPalette.Ink.copy(alpha = 0.18f),
                            0.42f to VeilPalette.Ink.copy(alpha = 0.38f),
                            1f to VeilPalette.Ink.copy(alpha = 0.995f)
                        )
                    )
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                VeilPalette.Ink.copy(alpha = 0.58f),
                                Color.Transparent,
                                VeilPalette.Ink.copy(alpha = 0.22f)
                            )
                        )
                    )
            )


            Column(
                Modifier.fillMaxWidth().padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                if (adjacent) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                        verticalAlignment = Alignment.CenterVertically) {
                        HeaderIdentity(Modifier.weight(1f))
                        HeaderActions(Modifier.width((com.veilreader.app.ui.theme.VeilComposition.InstrumentActionsReadableWidthDp *
                            LocalDensity.current.fontScale.coerceAtLeast(1f)).dp))
                    }
                } else {
                    HeaderActions()
                    HeaderIdentity()
                }
            }
        }
    }
}

@Composable
private fun ArchiveOverview(
    total: Int,
    reading: Int,
    finished: Int,
    collections: Int,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                )
            )
            .border(BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.30f)), shape)
            .padding(VeilSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.library_archive_status), style = MaterialTheme.typography.labelMedium, color = VeilPalette.Brass)
                    Text(stringResource(R.string.library_archive_status_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 480.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                        ) {
                            ArchiveStat(stringResource(R.string.library_stat_books), total, Modifier.weight(1f))
                            ArchiveStat(stringResource(R.string.library_stat_reading), reading, Modifier.weight(1f))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                        ) {
                            ArchiveStat(stringResource(R.string.library_stat_finished), finished, Modifier.weight(1f))
                            ArchiveStat(stringResource(R.string.library_stat_collections), collections, Modifier.weight(1f))
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                    ) {
                        ArchiveStat(stringResource(R.string.library_stat_books), total, Modifier.weight(1f))
                        ArchiveStat(stringResource(R.string.library_stat_reading), reading, Modifier.weight(1f))
                        ArchiveStat(stringResource(R.string.library_stat_finished), finished, Modifier.weight(1f))
                        ArchiveStat(stringResource(R.string.library_stat_collections), collections, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveStat(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LibraryAtmosphereLedger(state: LibraryAtmosphereState) {
    if (state.volumeCount <= 0) return

    val phrase = stringResource(
        when {
            state.deepQuiet >= 0.72f -> R.string.library_atmosphere_deep_quiet
            state.archiveDensity >= 0.72f -> R.string.library_atmosphere_many_chambers
            state.memoryWarmth >= 0.58f -> R.string.library_atmosphere_reading_light
            state.archiveDensity >= 0.32f -> R.string.library_atmosphere_gaining_depth
            else -> R.string.library_atmosphere_first_shelves
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .width(34.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VeilPalette.Brass.copy(alpha = 0.42f),
                            Color.Transparent
                        )
                    )
                )
        )
        Text(
            phrase,
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.52f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LibraryShelfCard(
    title: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val formatNumber = rememberVeilIntegerFormatter()
    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 104.dp, max = 220.dp).heightIn(min = 48.dp)
            .semantics { this.selected = selected },
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) VeilPalette.Archive else Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = VeilSpacing.sm, vertical = VeilSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge.withVeilContentScript(title), color = VeilPalette.Moon)

                }
                Text(formatNumber(count), style = MaterialTheme.typography.labelMedium,
                    color = VeilMaterials.TextSecondary)
            }
            Box(Modifier.fillMaxWidth().height(if (selected) 2.dp else 1.dp)
                .background(if (selected) VeilPalette.Brass else VeilPalette.BorderDark))
        }
    }
}

@Composable
private fun LibrarySectionHeading(eyebrow: String, title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            VeilMicroLabel(
                text = eyebrow,
                strong = true
            )
            Text(title, style = MaterialTheme.typography.titleLarge.withVeilContentScript(title))
        }
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentReadingBook(
    book: Book,
    artifactMemory: BookArtifactMemory?,
    onOpen: () -> Unit
) {
    val formatPercent = rememberVeilPercentFormatter()
    Surface(
        modifier = Modifier.width(224.dp).clickable(
            role = Role.Button,
            onClickLabel = stringResource(R.string.library_continue_book_semantics, book.title),
            onClick = onOpen
        ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.30f))
    ) {
        Row(
            Modifier.padding(VeilSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                title = book.title,
                subtitle = book.author,
                imagePath = book.coverCachePath,
                artifact = bookArtifactState(book, memory = artifactMemory),
                modifier = Modifier.width(48.dp).height(70.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.library_continue_reading), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium.withVeilContentScript(book.title),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    book.author.ifBlank { stringResource(R.string.common_unknown_author) },
                    style = MaterialTheme.typography.labelMedium.withVeilContentScript(book.author.ifBlank { stringResource(R.string.common_unknown_author) }),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { book.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                Text(
                    stringResource(R.string.book_detail_percent_read_text, formatPercent(book.progress.coerceIn(0f, 1f))),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LibraryArchiveWings(
    state: LibraryWingState,
    selectedCollection: String,
    selectedSeries: String,
    onCollection: (String) -> Unit,
    onSeries: (String) -> Unit
) {
    val visibleWings = remember(state) {
        (state.collectionWings.take(6) + state.seriesWings.take(6))
            .sortedWith(
                compareByDescending<ArchiveWing> { it.volumeCount }
                    .thenByDescending { it.lastRecordedActivityAtEpochMs }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
    }
    if (visibleWings.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VeilSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        LibrarySectionHeading(
            eyebrow = stringResource(R.string.library_wings_eyebrow),
            title = stringResource(R.string.library_wings_title),
            trailing = stringResource(R.string.library_wings_mapped, visibleWings.size)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            visibleWings.forEach { wing ->
                val selected = when (wing.kind) {
                    ArchiveWingKind.COLLECTION ->
                        selectedCollection.equals(wing.name, ignoreCase = true)
                    ArchiveWingKind.SERIES ->
                        selectedSeries.equals(wing.name, ignoreCase = true)
                }
                ArchiveWingPortal(
                    wing = wing,
                    selected = selected,
                    onClick = {
                        when (wing.kind) {
                            ArchiveWingKind.COLLECTION -> onCollection(wing.name)
                            ArchiveWingKind.SERIES -> onSeries(wing.name)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun libraryWingSummary(
    volumeCount: Int,
    activeCount: Int,
    completedCount: Int
): String {
    val parts = mutableListOf<String>()
    parts += stringResource(
        if (volumeCount == 1) R.string.library_wing_volume_one
        else R.string.library_wing_volumes_many,
        volumeCount
    )
    if (activeCount > 0) parts += stringResource(R.string.library_wing_active, activeCount)
    if (completedCount > 0) parts += stringResource(R.string.library_wing_sealed, completedCount)
    return parts.joinToString(" · ")
}

@Composable
private fun ArchiveWingPortal(
    wing: ArchiveWing,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(154.dp)
            .heightIn(min = 116.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilPalette.DeepBrass.copy(alpha = 0.26f)
        } else {
            VeilPalette.Ink.copy(alpha = 0.44f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                VeilPalette.Brass.copy(alpha = 0.72f)
            } else {
                VeilPalette.BorderDark.copy(alpha = 0.74f)
            }
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 116.dp)
                .padding(10.dp)
        ) {
            ArchiveWingArchitecture(
                wing = wing,
                selected = selected,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                VeilMicroLabel(
                    text = stringResource(
                        when (wing.kind) {
                            ArchiveWingKind.COLLECTION -> R.string.library_wing_collection
                            ArchiveWingKind.SERIES -> R.string.library_wing_series
                        }
                    ),
                    color = VeilPalette.Brass.copy(alpha = if (selected) 0.96f else 0.72f)
                )
                Text(
                    wing.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = VeilPalette.Moon,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    libraryWingSummary(
                        volumeCount = wing.volumeCount,
                        activeCount = wing.activeCount,
                        completedCount = wing.completedCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.58f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArchiveWingArchitecture(
    wing: ArchiveWing,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val presence = wing.archivePresence.coerceIn(0f, 1f)
        val brass = VeilPalette.Brass
        val mist = VeilPalette.Mist
        val ink = VeilPalette.Ink
        val w = size.width
        val h = size.height

        val arch = Path().apply {
            moveTo(w * 0.14f, h * 0.48f)
            cubicTo(
                w * 0.14f, h * 0.13f,
                w * 0.86f, h * 0.13f,
                w * 0.86f, h * 0.48f
            )
        }
        drawPath(
            path = arch,
            color = brass.copy(alpha = 0.09f + presence * 0.12f),
            style = Stroke(
                width = if (selected) 1.25.dp.toPx() else 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        val bays = (2 + (presence * 4f).toInt()).coerceIn(2, 6)
        repeat(bays) { index ->
            val fraction = (index + 1f) / (bays + 1f)
            val x = w * (0.20f + fraction * 0.60f)
            drawLine(
                color = mist.copy(alpha = 0.035f + presence * 0.055f),
                start = Offset(x, h * 0.24f),
                end = Offset(x, h * 0.56f),
                strokeWidth = 0.65.dp.toPx()
            )
        }

        repeat(wing.completedCount.coerceAtMost(4)) { index ->
            val x = w * (0.27f + index * 0.14f)
            drawCircle(
                color = brass.copy(alpha = 0.11f + presence * 0.10f),
                radius = 2.2.dp.toPx(),
                center = Offset(x, h * 0.36f),
                style = Stroke(0.7.dp.toPx())
            )
        }

        val sealX = w * 0.82f
        val sealY = h * 0.17f
        val sealRadius = if (selected) 6.dp.toPx() else 4.5.dp.toPx()
        drawCircle(
            color = ink.copy(alpha = 0.62f),
            radius = sealRadius + 1.5.dp.toPx(),
            center = Offset(sealX, sealY)
        )
        drawCircle(
            color = brass.copy(alpha = if (selected) 0.82f else 0.44f),
            radius = sealRadius,
            center = Offset(sealX, sealY),
            style = Stroke(0.8.dp.toPx())
        )
    }
}

@Composable
private fun MemoryReturnCard(
    event: LibraryMemoryEvent,
    book: Book,
    onInspect: () -> Unit
) {
    val eyebrow = stringResource(
        when (event.kind) {
            LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN -> R.string.library_memory_event_return
            LibraryMemoryEventKind.OLD_MARGIN_RETURN -> R.string.library_memory_event_margin_echo
            LibraryMemoryEventKind.LONG_SILENCE_RETURN -> R.string.library_memory_event_archive_return
        }
    )
    val eventTitle = localizedLibraryMemoryEventTitle(event.kind)
    val eventDetail = localizedLibraryMemoryEventDetail(event, book)

    Surface(
        onClick = onInspect,
        modifier = Modifier
            .width(246.dp)
            .heightIn(min = 132.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.64f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(
                alpha = if (
                    event.kind == LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN
                ) 0.58f else 0.34f
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoryTraceGlyph(
                    kind = event.kind,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                VeilMicroLabel(
                    text = eyebrow,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                eventTitle,
                style = MaterialTheme.typography.titleMedium,
                color = VeilPalette.Moon
            )
            Text(
                book.title,
                style = MaterialTheme.typography.labelMedium.withVeilContentScript(book.title),
                color = VeilPalette.Mist.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                eventDetail,
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.66f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun localizedLibraryMemoryEventTitle(kind: LibraryMemoryEventKind): String =
    stringResource(
        when (kind) {
            LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN -> R.string.library_memory_title_forgotten
            LibraryMemoryEventKind.OLD_MARGIN_RETURN -> R.string.library_memory_title_old_margin
            LibraryMemoryEventKind.LONG_SILENCE_RETURN -> R.string.library_memory_title_long_silence
        }
    )

@Composable
private fun localizedLibraryMemoryEventDetail(
    event: LibraryMemoryEvent,
    book: Book
): String {
    val gap = formatArchiveSilence(event.gapMillis)
    return when (event.kind) {
        LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN ->
            stringResource(R.string.library_memory_detail_forgotten, book.title, gap)
        LibraryMemoryEventKind.LONG_SILENCE_RETURN ->
            stringResource(R.string.library_memory_detail_long_silence, book.title, gap)
        LibraryMemoryEventKind.OLD_MARGIN_RETURN -> {
            val excerpt = event.passageExcerpt
                ?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.library_memory_preserved_passage)
            stringResource(R.string.library_memory_detail_old_margin, book.title, gap, excerpt)
        }
    }
}

@Composable
private fun DeepShelfPortal(
    count: Int,
    oldestBook: Book?,
    oldestMemory: BookArchiveMemory?,
    onOpen: () -> Unit
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VeilSpacing.xs),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Ink.copy(alpha = 0.50f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.26f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VeilPalette.DeepBrass.copy(alpha = 0.18f),
                            Color.Transparent,
                            VeilPalette.Ink.copy(alpha = 0.20f)
                        )
                    )
                )
                .padding(horizontal = VeilSpacing.md, vertical = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                VeilMicroLabel(
                    text = stringResource(R.string.library_deep_shelf),
                    strong = true
                )
                Text(
                    stringResource(
                        if (count == 1) R.string.library_quiet_volume_one
                        else R.string.library_quiet_volumes_many,
                        count
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = VeilPalette.Moon
                )
                if (oldestBook != null && oldestMemory != null) {
                    Text(
                        stringResource(
                            R.string.library_deepest_record,
                            oldestBook.title,
                            formatArchiveSilence(oldestMemory.inactiveMillis)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.66f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    stringResource(R.string.library_descend),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun ArchiveDepthMark(memory: BookArchiveMemory?) {
    val visible = memory?.takeIf {
        it.depth == ArchiveDepth.DEEP_SHELF || it.depth == ArchiveDepth.FORGOTTEN
    } ?: return

    VeilMicroLabel(
        text = archiveDepthRecord(visible),
        color = if (visible.depth == ArchiveDepth.FORGOTTEN) {
            VeilPalette.Brass.copy(alpha = 0.78f)
        } else {
            VeilPalette.Mist.copy(alpha = 0.58f)
        }
    )
}

@Composable
private fun MemoryTraceGlyph(
    kind: LibraryMemoryEventKind,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val tint = when (kind) {
            LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN -> VeilPalette.Brass
            LibraryMemoryEventKind.OLD_MARGIN_RETURN -> VeilPalette.Moon
            LibraryMemoryEventKind.LONG_SILENCE_RETURN -> VeilPalette.Spirit
        }
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = tint.copy(alpha = 0.72f),
            radius = size.minDimension * 0.32f,
            center = center,
            style = Stroke(1.1.dp.toPx())
        )
        drawCircle(
            color = tint.copy(alpha = 0.92f),
            radius = 1.8.dp.toPx(),
            center = center
        )
        drawLine(
            color = tint.copy(alpha = 0.48f),
            start = Offset(center.x, size.height * 0.06f),
            end = Offset(center.x, size.height * 0.24f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = 0.32f),
            start = Offset(size.width * 0.13f, center.y),
            end = Offset(size.width * 0.28f, center.y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun formatArchiveRecordDate(epochMs: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM)
        .format(Date(epochMs))
        .uppercase(Locale.getDefault())

@Composable
private fun archiveDepthRecord(memory: BookArchiveMemory): String {
    val age = formatArchiveSilence(memory.inactiveMillis)
    return when (memory.depth) {
        ArchiveDepth.SURFACE -> stringResource(R.string.book_detail_archive_surface)
        ArchiveDepth.SETTLED -> stringResource(R.string.book_detail_archive_settled, age)
        ArchiveDepth.DEEP_SHELF -> stringResource(R.string.book_detail_archive_deep, age)
        ArchiveDepth.FORGOTTEN -> stringResource(R.string.book_detail_archive_forgotten, age)
    }
}

@Composable
private fun formatArchiveSilence(inactiveMillis: Long): String {
    val days = inactiveMillis.coerceAtLeast(0L) / 86_400_000L
    return when {
        days >= 365L -> {
            val years = days / 365L
            val months = (days % 365L) / 30L
            if (months > 0L) {
                stringResource(R.string.book_detail_silence_years_months, years, months)
            } else {
                stringResource(R.string.book_detail_silence_years, years)
            }
        }
        days >= 60L -> stringResource(R.string.book_detail_silence_months, days / 30L)
        days >= 14L -> stringResource(R.string.book_detail_silence_weeks, days / 7L)
        days > 0L -> stringResource(R.string.book_detail_silence_days, days)
        else -> stringResource(R.string.book_detail_silence_today)
    }
}

@Composable
internal fun BookLibraryTile(
    book: Book,
    archiveMemory: BookArchiveMemory?,
    artifactMemory: BookArtifactMemory?,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDetails: () -> Unit
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val formatPercent = rememberVeilPercentFormatter()
    val artifact = bookArtifactState(book, memory = artifactMemory)
    val readLabel = stringResource(R.string.library_read_book_semantics, book.title)
    val favoriteLabel = stringResource(
        if (book.favorite) R.string.library_remove_favorite_semantics
        else R.string.library_add_favorite_semantics,
        book.title
    )
    val detailsLabel = stringResource(R.string.library_book_details_semantics, book.title)
    val registrationColor = when {
        artifact.finished -> VeilPalette.Brass
        artifact.recentlyOpened -> VeilPalette.Spirit
        artifact.favorite -> VeilPalette.Brass.copy(alpha = 0.78f)
        artifact.readingState == BookReadingState.ACTIVE -> VeilPalette.Mist.copy(alpha = 0.72f)
        else -> VeilPalette.BorderDark
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = readLabel, onClick = onOpen),
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.69f)
                    .padding(horizontal = VeilSpacing.xs, vertical = VeilSpacing.sm)
            ) {
                BookCover(
                    title = book.title,
                    subtitle = book.author,
                    imagePath = book.coverCachePath,
                    artifact = artifact,
                    modifier = Modifier.fillMaxSize()
                )


            }

            run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (artifact.progress > 0f) VeilPalette.BorderDark.copy(alpha = 0.70f) else Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(artifact.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(registrationColor)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = VeilSpacing.sm,
                    end = VeilSpacing.sm,
                    top = VeilSpacing.sm,
                    bottom = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium.withVeilContentScript(book.title),
                    color = VeilPalette.Moon,
                    minLines = if (largeText) 3 else 2,
                    maxLines = if (largeText) 3 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    book.author.ifBlank { stringResource(R.string.common_unknown_author) },
                    color = VeilMaterials.TextSecondary,
                    style = MaterialTheme.typography.labelMedium.withVeilContentScript(book.author.ifBlank { stringResource(R.string.common_unknown_author) }),
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when {
                            book.finished -> stringResource(R.string.library_completed)
                            book.progress > 0f -> stringResource(
                                R.string.book_detail_percent_read_text,
                                formatPercent(book.progress.coerceIn(0f, 1f))
                            )
                            else -> stringResource(R.string.library_unopened)
                        },
                        color = VeilMaterials.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        minLines = if (largeText) 2 else 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (artifact.recentlyOpened) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(VeilPalette.Spirit.copy(alpha = 0.90f))
                        )
                    }
                }

                ArchiveDepthMark(archiveMemory)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                VeilPalette.Brass.copy(alpha = 0.34f),
                                Color.Transparent
                            )
                        )
                    )
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = 4.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TextButton(
                    onClick = onDetails,
                    modifier = Modifier
                        .then(if (largeText) Modifier.fillMaxWidth() else Modifier.widthIn(min = 48.dp))
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = detailsLabel
                        },
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = VeilPalette.Brass
                    )
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.library_archive_record_button)
                    )
                }

                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = favoriteLabel
                        }
                ) {
                    FavoriteIcon(book.favorite, Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
internal fun BookLibraryRow(
    book: Book,
    archiveMemory: BookArchiveMemory?,
    artifactMemory: BookArtifactMemory?,
    showMemorySummary: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDetails: () -> Unit
) {
    val formatPercent = rememberVeilPercentFormatter()
    val formatNumber = rememberVeilNumberFormatter()
    val artifact = bookArtifactState(book, memory = artifactMemory)
    val readLabel = stringResource(R.string.library_read_book_semantics, book.title)
    val favoriteLabel = stringResource(
        if (book.favorite) R.string.library_remove_favorite_semantics
        else R.string.library_add_favorite_semantics,
        book.title
    )
    val recordLabel = stringResource(R.string.library_archive_record_semantics, book.title)
    val unknownAuthor = stringResource(R.string.common_unknown_author)
    val registrationColor = when {
        book.finished -> VeilPalette.Brass
        artifact.recentlyOpened -> VeilPalette.Spirit
        book.favorite -> VeilPalette.Brass.copy(alpha = 0.76f)
        book.progress > 0f -> VeilPalette.Mist.copy(alpha = 0.72f)
        else -> VeilPalette.BorderDark
    }

    @Composable
    fun IndexActions() {
        IconButton(
            onClick = onFavorite,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = favoriteLabel
                }
        ) {
            FavoriteIcon(book.favorite, Modifier.size(17.dp))
        }

        IconButton(
            onClick = onDetails,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = recordLabel
                }
        ) {
            EllipsisIcon(
                Modifier.size(17.dp),
                VeilPalette.Mist
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = readLabel,
                onClick = onOpen
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = VeilPalette.BorderDark,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .heightIn(min = 80.dp)
                .padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(46.dp)
                    .background(registrationColor)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall.withVeilContentScript(book.title),
                    color = VeilPalette.Moon,
                    maxLines = if (LocalDensity.current.fontScale >= 1.3f) 3 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    buildString {
                        append(
                            book.author.ifBlank { unknownAuthor }
                        )
                        book.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
                            append(" · ").append(series)
                            book.seriesIndex?.let {
                                append(" #").append(formatNumber(it))
                            }
                        }
                    },
                    style = MaterialTheme.typography.labelMedium.withVeilContentScript(buildString {
                        append(
                            book.author.ifBlank { unknownAuthor }
                        )
                        book.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
                            append(" · ").append(series)
                            book.seriesIndex?.let {
                                append(" #").append(formatNumber(it))
                            }
                        }
                    }),
                    color = VeilPalette.Mist,
                    maxLines = if (LocalDensity.current.fontScale >= 1.3f) 3 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        when {
                            book.finished -> stringResource(R.string.library_completed)
                            book.progress > 0f -> stringResource(R.string.book_detail_percent_read_text, formatPercent(book.progress.coerceIn(0f, 1f)))
                            else -> stringResource(R.string.library_unopened)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass.copy(alpha = 0.84f)
                    )
                    Text(
                        localizedBookFormatLabel(book.format),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Mist
                    )
                    archiveMemory?.let { memory ->
                        Text(
                            archiveDepthRecord(memory),
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Mist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (
                    showMemorySummary &&
                    artifactMemory != null &&
                    (artifactMemory.highlightCount > 0 || artifactMemory.bookmarkCount > 0)
                ) {
                    val marksText = stringResource(
                        R.string.library_mark_count,
                        artifactMemory.highlightCount
                    )
                    val savedText = stringResource(
                        R.string.library_saved_count,
                        artifactMemory.bookmarkCount
                    )
                    Text(
                        buildString {
                            if (artifactMemory.highlightCount > 0) append(marksText)
                            if (artifactMemory.bookmarkCount > 0) {
                                if (isNotEmpty()) append(" · ")
                                append(savedText)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Spirit,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (LocalDensity.current.fontScale >= 1.3f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { IndexActions() }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) { IndexActions() }
            }

        }
    }
}

@Composable
private fun BookProgress(book: Book) {
    val formatPercent = rememberVeilPercentFormatter()
    val formatInteger = rememberVeilIntegerFormatter()
    LinearProgressIndicator(
        progress = { book.progress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(3.dp),
        color = if (book.finished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            when {
                book.finished -> stringResource(R.string.book_detail_finished)
                book.progress > 0f -> stringResource(
                    R.string.book_detail_percent_read_text,
                    formatPercent(book.progress.coerceIn(0f, 1f))
                )
                else -> stringResource(R.string.book_detail_not_started)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val bookCollections = book.allCollections
        if (bookCollections.isNotEmpty()) {
            val label = if (bookCollections.size == 1) bookCollections.first()
            else stringResource(R.string.library_collection_more, bookCollections.first(), formatInteger(bookCollections.size - 1))
            Text(
                label,
                modifier = Modifier.weight(1f).padding(start = VeilSpacing.xs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibraryImportStatus() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.56f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.34f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = VeilSpacing.md,
                vertical = VeilSpacing.sm
            ),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .background(VeilPalette.Brass.copy(alpha = 0.82f))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                VeilMicroLabel(
                    text = stringResource(R.string.library_import_preparing_label),
                    strong = true
                )
                Text(
                    stringResource(R.string.library_import_preparing_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    hasBooks: Boolean,
    isImporting: Boolean,
    onImport: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.34f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Box(
                Modifier
                    .width(64.dp)
                    .height(48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                ShelfIcon(
                    Modifier.size(32.dp),
                    VeilPalette.Brass.copy(alpha = 0.88f)
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .width(64.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    VeilPalette.Brass.copy(alpha = 0.72f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            VeilMicroLabel(
                text = stringResource(
                    if (hasBooks) R.string.library_empty_no_match_eyebrow
                    else R.string.library_empty_archive_eyebrow
                ),
                strong = true
            )

            Text(
                stringResource(
                    if (hasBooks) R.string.library_empty_no_match_title
                    else R.string.library_empty_archive_title
                ),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                if (hasBooks) {
                    stringResource(R.string.library_empty_no_match_body)
                } else {
                    stringResource(R.string.library_empty_archive_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasBooks) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.44f)
                    )
                ) {
                    Text(stringResource(R.string.library_empty_clear_filters))
                }
            } else {
                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilMaterials.Parchment,
                        contentColor = VeilMaterials.Ink
                    )
                ) {
                    Text(stringResource(
                        if (isImporting) R.string.library_empty_preparing
                        else R.string.library_empty_import_first
                    ))
                }
            }
        }
    }
}

@Composable
internal fun LibraryShelvesView(
    groups: List<LibraryShelfGroup>,
    artifactMemoryByBookId: Map<String, BookArtifactMemory>,
    itemWidthDp: Float,
    coverWidthDp: Float,
    coverHeightDp: Float,
    onOpen: (Book) -> Unit,
    onDetails: (Book) -> Unit
) {
    val formatPercent = rememberVeilPercentFormatter()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        groups.forEach { group ->
            key(group.eyebrow, group.title) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    LibrarySectionHeading(
                        eyebrow = group.eyebrow,
                        title = group.title,
                        trailing = stringResource(R.string.library_group_volume_count, group.books.size)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().veilShelfDatum(coverHeightDp.dp),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                    ) {
                        lazyRowItems(group.books, key = { it.id }, contentType = { "shelfBook" }) { book ->
                            val readLabel = stringResource(R.string.library_read_book_semantics, book.title)
                            Column(
                                modifier = Modifier
                                    .width(galleryCellMeasureDp(itemWidthDp, LocalDensity.current.fontScale).dp)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = readLabel
                                    ) { onOpen(book) },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.fillMaxWidth().height((coverHeightDp + 16f).dp)) {
                                    BookCover(
                                        title = book.title,
                                        subtitle = book.author,
                                        imagePath = book.coverCachePath,
                                        artifact = bookArtifactState(
                                            book,
                                            memory = artifactMemoryByBookId[book.id]
                                        ),
                                        modifier = Modifier
                                            .width(coverWidthDp.dp)
                                            .height(coverHeightDp.dp)
                                    )

                                }
                                Text(
                                    book.title,
                                    style = MaterialTheme.typography.titleSmall.withVeilContentScript(book.title),
                                    color = VeilPalette.Moon,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    when {
                                        book.finished -> stringResource(R.string.book_detail_finished)
                                        book.progress > 0f -> stringResource(R.string.book_detail_percent_read_text, formatPercent(book.progress.coerceIn(0f, 1f)))
                                        else -> localizedBookFormatLabel(book.format)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VeilMaterials.TextSecondary,
                                    maxLines = 2
                                )
                                TextButton(
                                    onClick = { onDetails(book) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                ) {
                                    Text(stringResource(R.string.library_archive_record_button))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeToggle(mode: LibraryViewMode, onChange: (LibraryViewMode) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
    ) {
        listOf(
            LibraryViewMode.GALLERY to stringResource(R.string.library_view_gallery),
            LibraryViewMode.SHELVES to stringResource(R.string.library_view_shelves),
            LibraryViewMode.INDEX to stringResource(R.string.library_view_index)
        ).forEach { (candidate, label) ->
            val active = mode == candidate
            val viewDescription = stringResource(R.string.library_view_semantics, label)
            TextButton(
                onClick = { onChange(candidate) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = viewDescription
                        selected = active
                    },
                contentPadding = PaddingValues(horizontal = 9.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (active) {
                        VeilPalette.Brass
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                VeilMicroLabel(
                    text = label,
                    color = LocalContentColor.current
                )
            }
        }
    }
}

@Composable
private fun SearchIcon(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val width = 1.8.dp.toPx()
        drawCircle(tint, size.minDimension * .29f, Offset(size.width * .43f, size.height * .43f), style = Stroke(width))
        drawLine(
            tint,
            Offset(size.width * .64f, size.height * .64f),
            Offset(size.width * .88f, size.height * .88f),
            width,
            StrokeCap.Round
        )
    }
}

@Composable
private fun FavoriteIcon(favorite: Boolean, modifier: Modifier = Modifier) {
    val tint = if (favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val outer = size.minDimension * .46f
        val inner = outer * .44f
        val path = Path()
        repeat(10) { index ->
            val radius = if (index % 2 == 0) outer else inner
            val angle = Math.toRadians((-90.0 + index * 36.0))
            val x = size.width / 2f + cos(angle).toFloat() * radius
            val y = size.height / 2f + sin(angle).toFloat() * radius
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (favorite) drawPath(path, tint) else drawPath(path, tint, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun EllipsisIcon(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val r = size.minDimension * .08f
        drawCircle(tint, r, Offset(size.width * .26f, size.height * .50f))
        drawCircle(tint, r, Offset(size.width * .50f, size.height * .50f))
        drawCircle(tint, r, Offset(size.width * .74f, size.height * .50f))
    }
}

@Composable
private fun GridIcon(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val width = 1.6.dp.toPx()
        val stroke = Stroke(width)
        val cell = size.minDimension * .30f
        val gap = size.minDimension * .12f
        val start = size.minDimension * .13f
        drawRect(tint, Offset(start, start), androidx.compose.ui.geometry.Size(cell, cell), style = stroke)
        drawRect(tint, Offset(start + cell + gap, start), androidx.compose.ui.geometry.Size(cell, cell), style = stroke)
        drawRect(tint, Offset(start, start + cell + gap), androidx.compose.ui.geometry.Size(cell, cell), style = stroke)
        drawRect(tint, Offset(start + cell + gap, start + cell + gap), androidx.compose.ui.geometry.Size(cell, cell), style = stroke)
    }
}

@Composable
private fun ListIcon(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val width = 1.7.dp.toPx()
        repeat(3) { i ->
            val y = size.height * (.24f + i * .26f)
            drawCircle(tint, size.minDimension * .035f, Offset(size.width * .14f, y))
            drawLine(tint, Offset(size.width * .28f, y), Offset(size.width * .88f, y), width, StrokeCap.Round)
        }
    }
}

@Composable
private fun ShelfIcon(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val width = 1.7.dp.toPx()
        val stroke = Stroke(width)
        repeat(3) { i ->
            val left = size.width * (.12f + i * .27f)
            drawRoundRect(
                tint,
                topLeft = Offset(left, size.height * (.20f + if (i == 1) .08f else 0f)),
                size = androidx.compose.ui.geometry.Size(size.width * .20f, size.height * (.62f - if (i == 1) .08f else 0f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                style = stroke
            )
        }
    }
}

private fun formatSeriesIndexInput(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private fun parseCollectionNames(value: String): List<String> = value
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinctBy { it.lowercase(Locale.ROOT) }




@Composable
private fun MangaLibraryPortal(
    localComicCount: Int,
    onOpenManga: () -> Unit
) {
    Surface(
        onClick = onOpenManga,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VeilSpacing.xs),
        shape = MaterialTheme.shapes.small,
        color = VeilPalette.Archive.copy(alpha = 0.78f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.30f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = VeilSpacing.md,
                vertical = VeilSpacing.sm
            ),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(VeilPalette.Ink.copy(alpha = 0.72f))
                    .border(
                        BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f)),
                        MaterialTheme.shapes.extraSmall
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "漫",
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Brass
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    stringResource(R.string.manga_portal_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass
                )
                Text(
                    stringResource(R.string.manga_portal_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(R.string.manga_portal_body, localComicCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.84f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                stringResource(R.string.manga_portal_action),
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Brass
            )
        }
    }
}
