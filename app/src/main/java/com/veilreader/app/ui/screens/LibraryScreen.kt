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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.books.bookArtifactRecordLabel
import com.veilreader.app.ui.books.bookArtifactState
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilLanguage
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.archiveLayoutPolicyFor
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.localizeAppNumerals
import com.veilreader.app.ui.theme.localizedMetadataValue
import com.veilreader.app.ui.theme.currentVeilTemporalPhase
import com.veilreader.app.ui.theme.libraryArchiveAtmosphere
import com.veilreader.app.ui.theme.narrativeArchitectureField
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.usesArabicScript
import java.text.DateFormat
import java.util.Date
import java.util.Locale
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

internal fun deriveLibraryShelfGroups(
    books: List<Book>,
    filtered: List<Book>,
    filterActive: Boolean
): List<LibraryShelfGroup> {
    if (filterActive) {
        return listOf(
            LibraryShelfGroup(
                eyebrow = "Filtered archive",
                title = "Matching volumes",
                books = filtered
            )
        ).filter { it.books.isNotEmpty() }
    }

    val groups = mutableListOf<LibraryShelfGroup>()

    books
        .filter { !it.finished && it.progress > 0f }
        .sortedByDescending { it.lastOpenedAtEpochMs }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup("Journey", "Currently reading", it) }

    books
        .flatMap { book -> book.allCollections.map { it to book } }
        .groupBy({ it.first }, { it.second })
        .toList()
        .sortedByDescending { it.second.size }
        .take(6)
        .forEach { (name, volumes) ->
            groups += LibraryShelfGroup("Collection", name, volumes)
        }

    books
        .filter { !it.seriesName.isNullOrBlank() }
        .groupBy { requireNotNull(it.seriesName) }
        .toList()
        .sortedByDescending { it.second.size }
        .take(6)
        .forEach { (name, volumes) ->
            groups += LibraryShelfGroup(
                eyebrow = "Series",
                title = name,
                books = volumes.sortedWith(
                    compareBy<Book> { it.seriesIndex ?: Double.MAX_VALUE }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                )
            )
        }

    books
        .filter { it.author.isNotBlank() }
        .groupBy { it.author }
        .filterValues { it.size >= 2 }
        .toList()
        .sortedByDescending { it.second.size }
        .take(4)
        .forEach { (name, volumes) ->
            groups += LibraryShelfGroup("Author", name, volumes)
        }

    books
        .filter { it.finished }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup("Record", "Completed volumes", it) }

    books
        .filter { !it.finished && it.progress <= 0f }
        .takeIf { it.isNotEmpty() }
        ?.let { groups += LibraryShelfGroup("Unopened", "Waiting on the shelf", it) }

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
    onOpenSettings: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val archiveAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val archiveLayout = archiveLayoutPolicyFor(archiveAdaptiveClass)
    var query by rememberSaveable { mutableStateOf("") }
    var shelf by rememberSaveable { mutableStateOf("All") }
    var collection by rememberSaveable { mutableStateOf("") }
    var seriesFilter by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Recent") }
    var viewModeName by rememberSaveable { mutableStateOf(LibraryViewMode.GALLERY.name) }
    val viewMode = libraryViewModeFromStored(viewModeName)
    var overviewExpanded by rememberSaveable { mutableStateOf(false) }
    var collectionMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Book?>(null) }
    var detailBookId by rememberSaveable { mutableStateOf<String?>(null) }
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
        seriesIndex = book.seriesIndex?.let(::formatSeriesIndex).orEmpty()
        language = book.language.orEmpty()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportUri)
    }
    val collections = books
        .flatMap { it.allCollections }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

    LaunchedEffect(collections) {
        if (collection.isNotEmpty() && collections.none { it.equals(collection, ignoreCase = true) }) {
            collection = ""
        }
    }

    val libraryMemoryNow = remember(books, highlights, readingSessions) {
        System.currentTimeMillis()
    }
    val memoryState = remember(books, highlights, readingSessions, libraryMemoryNow) {
        deriveLibraryMemoryState(
            books = books,
            highlights = highlights,
            sessions = readingSessions,
            nowEpochMs = libraryMemoryNow
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
    val filtered = books.filter { book ->
        val searchable = buildList {
            add(book.title)
            add(book.author)
            book.seriesName?.let(::add)
            book.language?.let(::add)
            addAll(book.allCollections)
        }
        val matchesQuery = trimmedQuery.isBlank() || searchable.any {
            it.contains(trimmedQuery, ignoreCase = true)
        }
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
            else -> list.sortedByDescending { maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs) }
        }
    }

    val recentReading = books
        .asSequence()
        .filter { !it.finished && it.progress > 0f }
        .sortedByDescending { it.lastOpenedAtEpochMs }
        .take(5)
        .toList()
    val detailBook = detailBookId?.let { id -> books.firstOrNull { it.id == id } }

    // Headers and books share one lazy viewport, including landscape and large-text layouts.
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = 0.28f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(780.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(820.dp)
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.06f),
                        0.34f to Color.Transparent,
                        0.70f to VeilPalette.Ink.copy(alpha = 0.64f),
                        1f to VeilPalette.Ink
                    )
                )
        )

        LazyVerticalGrid(
        columns = if (viewMode == LibraryViewMode.GALLERY) {
            GridCells.Adaptive(archiveLayout.galleryMinCellDp.dp)
        } else {
            GridCells.Fixed(1)
        },
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = books.size + filtered.size,
                intensity = 0.88f + atmosphereState.archiveDensity * 0.12f,
                temporalPhase = currentVeilTemporalPhase()
            )
            .libraryArchiveAtmosphere(
                state = atmosphereState,
                seed = books.size * 31 + collections.size * 7
            )
            .narrativeArchitectureField(
                realm = VeilRealm.ARCHIVE,
                seed = books.size * 37 + collections.size * 11 + filtered.size,
                intensity = (0.58f + atmosphereState.archiveDensity * 0.16f)
                    .coerceAtMost(0.74f)
            ),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
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
                    onImport = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
                    onOpenSettings = onOpenSettings
                )
                LibraryAtmosphereLedger(atmosphereState)
            }
        }

        item(key = "library:search", span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                placeholder = {
                    Text(
                        "Search the archive…",
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
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VeilPalette.Brass.copy(alpha = 0.82f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-14).dp)
                    .padding(horizontal = 6.dp)
            )
        }

        if (isImporting) {
            item(key = "library:import-status", span = { GridItemSpan(maxLineSpan) }) {
                LibraryImportStatus()
            }
        }

        item(key = "library:status-shelves", span = { GridItemSpan(maxLineSpan) }) {
            if (viewMode != LibraryViewMode.SHELVES) Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = VeilSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                LibrarySectionHeading(
                    eyebrow = "Archive",
                    title = "Shelves",
                    trailing = "Tap to filter"
                )

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val shelfWidth = if (maxWidth < 600.dp) {
                        (maxWidth - VeilSpacing.xs) / 2f
                    } else {
                        (maxWidth - VeilSpacing.xs * 2f) / 3f
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                    ) {
                        LibraryShelfCard(
                            title = "Favorites",
                            subtitle = "Volumes kept close",
                            count = books.count { it.favorite },
                            selected = shelf == "Favorites",
                            width = shelfWidth,
                            onClick = { shelf = if (shelf == "Favorites") "All" else "Favorites" }
                        )
                        LibraryShelfCard(
                            title = "Currently Reading",
                            subtitle = "Open journeys",
                            count = books.count { !it.finished && it.progress > 0f },
                            selected = shelf == "Reading",
                            width = shelfWidth,
                            onClick = { shelf = if (shelf == "Reading") "All" else "Reading" }
                        )
                        LibraryShelfCard(
                            title = "Completed",
                            subtitle = "Closed volumes",
                            count = books.count { it.finished },
                            selected = shelf == "Finished",
                            width = shelfWidth,
                            onClick = { shelf = if (shelf == "Finished") "All" else "Finished" }
                        )
                        LibraryShelfCard(
                            title = "Deep Shelf",
                            subtitle = "Long-unopened volumes",
                            count = memoryState.deepShelfBookIds.size,
                            selected = shelf == "Deep Shelf",
                            width = shelfWidth,
                            onClick = {
                                shelf = if (shelf == "Deep Shelf") "All" else "Deep Shelf"
                            }
                        )
                        LibraryShelfCard(
                            title = "Plan to Read",
                            subtitle = "Still unopened",
                            count = books.count { !it.finished && it.progress <= 0f },
                            selected = shelf == "Unread",
                            width = shelfWidth,
                            onClick = { shelf = if (shelf == "Unread") "All" else "Unread" }
                        )
                    }
                }
            }
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

        item(key = "library:controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BrassRule(Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        localizeAppNumerals(
                            "${filtered.size.toString().padStart(2, '0')} VOLUMES",
                            LocalVeilLanguage.current
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                        color = VeilPalette.Brass,
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
                                    if (collection.isBlank()) "Collection" else collection,
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
                                    text = { Text("All collections") },
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

                    Box {
                        OutlinedButton(
                            onClick = { sortMenu = true },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Sort books: $sort" },
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
                            )
                        ) {
                            Text(
                                sort,
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
                                "Recent",
                                "Archive Depth",
                                "Title",
                                "Author",
                                "Series",
                                "Progress"
                            ).forEach { label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { sort = label; sortMenu = false }
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
                                "Series · ${localizedMetadataValue(seriesFilter, LocalVeilLanguage.current)} ×",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    ViewModeToggle(
                        mode = viewMode,
                        onChange = { viewModeName = it.name }
                    )

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
                            Text("Reset", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
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
                        eyebrow = "Recovered memory",
                        title = "The archive remembers",
                        trailing = "${memoryState.events.size} traces"
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        memoryState.events.take(4).forEach { event ->
                            val eventBook = books.firstOrNull { it.id == event.bookId }
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
                    ?.let { id -> books.firstOrNull { it.id == id } }

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
                            eyebrow = "Recently opened",
                            title = "Volumes in progress",
                            trailing = "${recentReading.size} active"
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

        if (filtered.isEmpty()) {
            item(key = "library:empty", span = { GridItemSpan(maxLineSpan) }) {
                LibraryEmptyState(
                    hasBooks = books.isNotEmpty(),
                    isImporting = isImporting,
                    onImport = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
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
                            groups = deriveLibraryShelfGroups(
                                books = books,
                                filtered = filtered,
                                filterActive = trimmedQuery.isNotBlank() ||
                                    shelf != "All" ||
                                    collection.isNotEmpty() ||
                                    seriesFilter.isNotEmpty()
                            ),
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
        item(key = "library:overview", span = { GridItemSpan(maxLineSpan) }) {
            if (books.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { overviewExpanded = !overviewExpanded }) {
                        Text(if (overviewExpanded) "Hide archive overview" else "Archive overview")
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
        }
    }

    detailBook?.let { book ->
        BookDetailSheet(
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
            }
        )
    }

    editing?.let { book ->
        val parsedSeriesIndex = seriesIndex.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val seriesIndexInvalid = seriesIndex.isNotBlank() && (parsedSeriesIndex == null || !parsedSeriesIndex.isFinite())
        AlertDialog(
            onDismissRequest = { editing = null },
            shape = MaterialTheme.shapes.small,
            containerColor = VeilPalette.Archive,
            titleContentColor = VeilPalette.Moon,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "ARCHIVE RECORD",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.book_metadata_dialog_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.book_metadata_title)) }, isError = title.isBlank())
                    OutlinedTextField(author, { author = it }, label = { Text(stringResource(R.string.book_metadata_author)) })
                    OutlinedTextField(
                        collectionNames,
                        { collectionNames = it },
                        label = { Text(stringResource(R.string.book_metadata_collections)) },
                        supportingText = { Text(stringResource(R.string.book_metadata_collections_hint)) }
                    )
                    OutlinedTextField(seriesName, { seriesName = it }, label = { Text(stringResource(R.string.book_metadata_series)) })
                    OutlinedTextField(
                        seriesIndex,
                        { seriesIndex = it },
                        label = { Text(stringResource(R.string.book_metadata_series_number)) },
                        isError = seriesIndexInvalid,
                        supportingText = { if (seriesIndexInvalid) Text(stringResource(R.string.book_metadata_series_number_error)) }
                    )
                    OutlinedTextField(
                        language,
                        { language = it },
                        label = { Text(stringResource(R.string.book_metadata_language)) },
                        supportingText = { Text(stringResource(R.string.book_metadata_language_hint)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && !seriesIndexInvalid,
                    onClick = {
                        onEditMetadata(
                            BookMetadataUpdate(
                                bookId = book.id,
                                title = title,
                                author = author,
                                collections = parseCollectionNames(collectionNames),
                                seriesName = seriesName.trim().takeIf { it.isNotEmpty() },
                                seriesIndex = parsedSeriesIndex,
                                language = language.trim().takeIf { it.isNotEmpty() }
                            )
                        )
                        editing = null
                    }
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailSheet(
    book: Book,
    archiveMemory: BookArchiveMemory?,
    artifactMemory: BookArtifactMemory?,
    readingCycles: List<ReadingCycleRecord>,
    readingMilestones: List<ReadingMilestoneRecord>,
    preservedHighlights: List<Highlight>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onEditMetadata: () -> Unit
) {
    val progress = book.progress.coerceIn(0f, 1f)
    val status = when {
        book.finished -> stringResource(R.string.book_detail_finished)
        progress > 0f -> stringResource(R.string.book_detail_percent_read, (progress * 100).toInt())
        else -> stringResource(R.string.book_detail_not_started)
    }
    val primaryAction = when {
        book.finished -> stringResource(R.string.book_detail_read_again)
        progress > 0f -> stringResource(R.string.book_detail_continue_reading)
        else -> stringResource(R.string.book_detail_open_book)
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
                        intensity = 1.0f,
                        temporalPhase = currentVeilTemporalPhase()
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
                    .fillMaxWidth()
                    .heightIn(min = 430.dp)
            ) {
                val compact = maxWidth < 520.dp

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
                                0f to VeilPalette.Ink.copy(alpha = 0.08f),
                                0.38f to Color.Transparent,
                                0.72f to VeilPalette.Ink.copy(alpha = 0.58f),
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
                                    VeilPalette.Ink.copy(alpha = 0.36f),
                                    Color.Transparent,
                                    VeilPalette.Ink.copy(alpha = 0.28f)
                                )
                            )
                        )
                )

                GrayfogOrnamentFrame(
                    modifier = Modifier.matchParentSize(),
                    strength = 0.86f
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ARTIFACT CHAMBER · ${book.format.name}",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                            color = VeilPalette.Brass,
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
                                "CLOSE",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.25.sp)
                            )
                        }
                    }

                    if (compact) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                        ) {
                            BookCover(
                                title = book.title,
                                subtitle = book.author,
                                imagePath = book.coverCachePath,
                artifact = bookArtifactState(book, memory = artifactMemory),
                                modifier = Modifier.width(172.dp).height(252.dp)
                            )
                            BookDetailIdentity(
                                book = book,
                                artifactMemory = artifactMemory
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xl),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookCover(
                                title = book.title,
                                subtitle = book.author,
                                imagePath = book.coverCachePath,
                artifact = bookArtifactState(book, memory = artifactMemory),
                                modifier = Modifier.width(182.dp).height(268.dp)
                            )
                            BookDetailIdentity(
                                book = book,
                                artifactMemory = artifactMemory,
                                modifier = Modifier.weight(1f)
                            )
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = VeilPalette.ReaderPaper,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.82f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(VeilSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "READING PROGRESS",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                                color = Color(0xFF665035),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2A241D)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFF2A241D),
                            trackColor = Color(0xFF8D795A).copy(alpha = 0.20f),
                            drawStopIndicator = {}
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                status,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF665A49)
                            )

                            book.currentChapter
                                .takeIf { it.isNotBlank() && it != "Not started" }
                                ?.let { chapter ->
                                    Text(
                                        chapter,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF5B4630),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 250.dp)
                                    )
                                }
                        }
                    }
                }

                Button(
                    onClick = onOpen,
                    enabled = book.isImported,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A)
                    )
                ) {
                    Text(
                        if (book.isImported) primaryAction
                        else stringResource(R.string.book_detail_publication_unavailable)
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = onFavorite,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.46f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            if (book.favorite) {
                                stringResource(R.string.book_detail_favorited)
                            } else {
                                stringResource(R.string.book_detail_favorite)
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onEditMetadata,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)
                        )
                    ) {
                        Text(stringResource(R.string.book_detail_edit_details))
                    }
                }

                BrassRule(Modifier.fillMaxWidth())

                if (preservedHighlights.isNotEmpty()) {
                    BookDetailFragments(
                        highlights = preservedHighlights
                    )
                    BrassRule(Modifier.fillMaxWidth())
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    Text(
                        "ARCHIVE HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                        color = VeilPalette.Brass
                    )
                    BookDetailFact(stringResource(R.string.book_detail_format), book.format.name)
                    book.language?.takeIf { it.isNotBlank() }?.let {
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
                    book.addedAtEpochMs.takeIf { it > 0L }?.let { archivedAt ->
                        BookDetailFact("Archived", formatArchiveRecordDate(archivedAt))
                    }
                    readingMilestones
                        .firstOrNull { it.kind == ReadingMilestoneKind.FIRST_OPENED }
                        ?.let { firstOpen ->
                            BookDetailFact(
                                "First opened",
                                formatArchiveRecordDate(firstOpen.reachedAtEpochMs)
                            )
                        }
                    val progressMarks = readingMilestones
                        .filter { it.kind != ReadingMilestoneKind.FIRST_OPENED }
                        .sortedBy { it.progression }
                    if (progressMarks.isNotEmpty()) {
                        BookDetailFact(
                            "Journey marks",
                            progressMarks.joinToString(" · ") {
                                "${(it.progression * 100).toInt()}%"
                            }
                        )
                    }
                    readingCycles.maxByOrNull { it.cycleIndex }?.let { latestCycle ->
                        BookDetailFact(
                            if (latestCycle.cycleIndex > 1) {
                                "Latest completion · Cycle ${latestCycle.cycleIndex}"
                            } else {
                                "Completed"
                            },
                            formatArchiveRecordDate(latestCycle.completedAtEpochMs)
                        )
                    }
                    if (readingCycles.size > 1) {
                        BookDetailFact("Reading cycles", readingCycles.size.toString())
                    }
                    archiveMemory?.let { memory ->
                        BookDetailFact(
                            "Archive depth",
                            archiveDepthRecord(memory)
                        )
                    }
                    artifactMemory?.takeIf {
                        it.sessionCount > 0 ||
                            it.highlightCount > 0 ||
                            it.bookmarkCount > 0
                    }?.let { material ->
                        BookDetailFact(
                            "Material memory",
                            buildString {
                                if (material.sessionCount > 0) {
                                    append(material.sessionCount)
                                        .append(if (material.sessionCount == 1) " session" else " sessions")
                                }
                                if (material.highlightCount > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append(material.highlightCount)
                                        .append(if (material.highlightCount == 1) " passage" else " passages")
                                }
                                if (material.bookmarkCount > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append(material.bookmarkCount)
                                        .append(if (material.bookmarkCount == 1) " saved place" else " saved places")
                                }
                            }
                        )
                    }
                }

                if (book.allCollections.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        Text(
                            stringResource(R.string.book_detail_collections).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                            color = VeilPalette.Brass
                        )

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                        ) {
                            book.allCollections.forEach { collection ->
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
                                    border = BorderStroke(
                                        1.dp,
                                        VeilPalette.Brass.copy(alpha = 0.32f)
                                    )
                                ) {
                                    Text(
                                        collection,
                                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
private fun BookDetailFragments(
    highlights: List<Highlight>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "PRESERVED MEMORY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    "Preserved fragments",
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
            }
            Text(
                "${highlights.size} passages",
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Mist.copy(alpha = 0.72f)
            )
        }

        highlights.take(3).forEachIndexed { index, highlight ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall,
                color = VeilPalette.Archive.copy(alpha = 0.62f),
                border = BorderStroke(
                    1.dp,
                    if (index == 0) {
                        VeilPalette.Brass.copy(alpha = 0.34f)
                    } else {
                        VeilPalette.BorderDark.copy(alpha = 0.68f)
                    }
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(VeilSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        formatArchiveRecordDate(highlight.createdAtEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass.copy(alpha = 0.78f)
                    )
                    Text(
                        "“${highlight.quote}”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VeilPalette.Moon,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    highlight.note.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = VeilPalette.Mist.copy(alpha = 0.82f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (highlights.size > 3) {
            Text(
                "+${highlights.size - 3} more preserved in Hidden Archive",
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Spirit.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun BookDetailIdentity(
    book: Book,
    artifactMemory: BookArtifactMemory?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            book.title,
            style = MaterialTheme.typography.headlineMedium,
            color = VeilPalette.Moon,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            if (book.author.isBlank()) {
                stringResource(R.string.common_unknown_author)
            } else {
                book.author
            },
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Moon.copy(alpha = 0.76f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        book.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
            Text(
                buildString {
                    append(series)
                    book.seriesIndex?.let { append(" · #${formatSeriesIndex(it)}") }
                },
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Brass,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            bookArtifactRecordLabel(
                bookArtifactState(book, memory = artifactMemory)
            ),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.90.sp),
            color = VeilPalette.Mist.copy(alpha = 0.72f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = VeilPalette.Ink.copy(alpha = 0.58f),
                border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.32f))
            ) {
                Text(
                    book.format.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Moon.copy(alpha = 0.84f)
                )
            }

            if (book.favorite) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = VeilPalette.DeepBrass.copy(alpha = 0.54f),
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f))
                ) {
                    Text(
                        "FAVORITE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                }
            }
        }
    }
}

@Composable
private fun BookDetailFact(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(78.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LibraryHeader(
    bookCount: Int,
    isImporting: Boolean,
    onImport: () -> Unit,
    onOpenSettings: () -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.64f)),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        val compact = maxWidth < 560.dp
        val fontScale = LocalDensity.current.fontScale
        val headerHeight = when {
            fontScale > 1.55f -> 322.dp
            fontScale > 1.30f -> 278.dp
            compact -> 218.dp
            else -> 256.dp
        }

        Box(Modifier.fillMaxWidth().height(headerHeight)) {
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
                            0f to VeilPalette.Ink.copy(alpha = 0.06f),
                            0.34f to Color.Transparent,
                            0.72f to VeilPalette.Ink.copy(alpha = 0.52f),
                            1f to VeilPalette.Ink.copy(alpha = 0.98f)
                        )
                    )
            )

            GrayfogOrnamentFrame(
                modifier = Modifier.matchParentSize(),
                strength = 0.86f
            )

            VeilRealmEmblem(
                realm = VeilRealm.ARCHIVE,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(126.dp),
                tint = VeilPalette.Brass.copy(alpha = 0.18f)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(VeilSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VeilPalette.Moon,
                        containerColor = VeilPalette.Ink.copy(alpha = 0.48f)
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Settings", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A)
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        if (isImporting) "Importing…" else "Import",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "VEIL READER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.7.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    "Grayfog Archive",
                    style = if (compact) {
                        MaterialTheme.typography.headlineLarge
                    } else {
                        MaterialTheme.typography.displaySmall
                    },
                    color = VeilPalette.Moon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Fragments · Records · Truths · Echoes",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.78.sp),
                        color = VeilPalette.Moon.copy(alpha = 0.76f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (bookCount == 0) {
                            "EMPTY"
                        } else {
                            "$bookCount ${if (bookCount == 1) "VOLUME" else "VOLUMES"}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.75.sp),
                        color = VeilPalette.Brass.copy(alpha = 0.88f),
                        maxLines = 1
                    )
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
                    Text("ARCHIVE STATUS", style = MaterialTheme.typography.labelMedium, color = VeilPalette.Brass)
                    Text("Catalogued locally. Private, offline, and ready to reopen.", style = MaterialTheme.typography.bodyMedium)
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
                            ArchiveStat("Books", total, Modifier.weight(1f))
                            ArchiveStat("Reading", reading, Modifier.weight(1f))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                        ) {
                            ArchiveStat("Finished", finished, Modifier.weight(1f))
                            ArchiveStat("Collections", collections, Modifier.weight(1f))
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                    ) {
                        ArchiveStat("Books", total, Modifier.weight(1f))
                        ArchiveStat("Reading", reading, Modifier.weight(1f))
                        ArchiveStat("Finished", finished, Modifier.weight(1f))
                        ArchiveStat("Collections", collections, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveStat(label: String, count: Int, modifier: Modifier = Modifier) {
    val language = LocalVeilLanguage.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            localizeAppNumerals(count.toString(), language),
            style = MaterialTheme.typography.titleLarge
        )
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

    val phrase = when {
        state.deepQuiet >= 0.72f -> "The lower stacks are quiet and deep."
        state.archiveDensity >= 0.72f -> "The Archive has grown into many chambers."
        state.memoryWarmth >= 0.58f -> "Reading light is active through the stacks."
        state.archiveDensity >= 0.32f -> "The shelves are beginning to gain depth."
        else -> "The first shelves are taking shape."
    }

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
    subtitle: String,
    count: Int,
    selected: Boolean,
    width: Dp = 142.dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .heightIn(min = 82.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilPalette.DeepBrass.copy(alpha = 0.62f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) VeilPalette.Brass.copy(alpha = 0.88f)
            else VeilPalette.Brass.copy(alpha = 0.30f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VeilPalette.RaisedIron.copy(alpha = if (selected) 0.44f else 0.26f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = VeilSpacing.sm, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    localizeAppNumerals(count.toString().padStart(2, '0'), LocalVeilLanguage.current),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) VeilPalette.Moon
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LibrarySectionHeading(eyebrow: String, title: String, trailing: String? = null) {
    val arabicScriptEyebrow = usesArabicScript(eyebrow)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (arabicScriptEyebrow) eyebrow else eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = if (arabicScriptEyebrow) 0.sp else 1.35.sp
                ),
                color = VeilPalette.Brass
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
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
    Surface(
        modifier = Modifier.width(224.dp).clickable(
            role = Role.Button,
            onClickLabel = "Continue ${book.title}",
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
                Text("Continue reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    book.author.ifBlank { stringResource(R.string.common_unknown_author) },
                    style = MaterialTheme.typography.labelMedium,
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
                    "${(book.progress.coerceIn(0f, 1f) * 100).toInt()}% read",
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
            eyebrow = "Spatial index",
            title = "Archive Wings",
            trailing = "${visibleWings.size} mapped"
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
                Text(
                    when (wing.kind) {
                        ArchiveWingKind.COLLECTION -> "COLLECTION WING"
                        ArchiveWingKind.SERIES -> "SERIES CORRIDOR"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.82.sp),
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
                    buildString {
                        append(wing.volumeCount)
                            .append(if (wing.volumeCount == 1) " volume" else " volumes")
                        if (wing.activeCount > 0) {
                            append(" · ").append(wing.activeCount).append(" active")
                        }
                        if (wing.completedCount > 0) {
                            append(" · ").append(wing.completedCount).append(" sealed")
                        }
                    },
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
    val eyebrow = when (event.kind) {
        LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN -> "RETURN EVENT"
        LibraryMemoryEventKind.OLD_MARGIN_RETURN -> "MARGIN ECHO"
        LibraryMemoryEventKind.LONG_SILENCE_RETURN -> "ARCHIVE RETURN"
    }

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
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.08.sp
                    ),
                    color = VeilPalette.Brass,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                color = VeilPalette.Moon
            )
            Text(
                book.title,
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Mist.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                event.detail,
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.66f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
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
                Text(
                    "THE DEEP SHELF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.32.sp
                    ),
                    color = VeilPalette.Brass
                )
                Text(
                    "$count ${if (count == 1) "volume has" else "volumes have"} gone quiet",
                    style = MaterialTheme.typography.titleMedium,
                    color = VeilPalette.Moon
                )
                if (oldestBook != null && oldestMemory != null) {
                    Text(
                        "Deepest record · ${oldestBook.title} · " +
                            formatArchiveSilence(oldestMemory.inactiveMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.66f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "DESCEND",
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

    Text(
        archiveDepthRecord(visible).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.68.sp),
        color = if (visible.depth == ArchiveDepth.FORGOTTEN) {
            VeilPalette.Brass.copy(alpha = 0.78f)
        } else {
            VeilPalette.Mist.copy(alpha = 0.58f)
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
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

private fun archiveDepthRecord(memory: BookArchiveMemory): String {
    val age = formatArchiveSilence(memory.inactiveMillis)
    return when (memory.depth) {
        ArchiveDepth.SURFACE -> "Surface shelf"
        ArchiveDepth.SETTLED -> "Settled · $age"
        ArchiveDepth.DEEP_SHELF -> "Deep Shelf · $age silent"
        ArchiveDepth.FORGOTTEN -> "Forgotten · $age silent"
    }
}

private fun formatArchiveSilence(inactiveMillis: Long): String {
    val days = inactiveMillis.coerceAtLeast(0L) / 86_400_000L
    return when {
        days >= 365L -> {
            val years = days / 365L
            val months = (days % 365L) / 30L
            if (months > 0L) "${years}y ${months}mo" else "${years}y"
        }
        days >= 60L -> "${days / 30L} months"
        days >= 14L -> "${days / 7L} weeks"
        days > 0L -> "$days days"
        else -> "today"
    }
}

@Composable
private fun BookLibraryTile(
    book: Book,
    archiveMemory: BookArchiveMemory?,
    artifactMemory: BookArtifactMemory?,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDetails: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "Read ${book.title}",
                onClick = onOpen
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BookCover(
            title = book.title,
            subtitle = book.author,
            imagePath = book.coverCachePath,
                artifact = bookArtifactState(book, memory = artifactMemory),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.69f)
        )

        Text(
            book.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            book.author.ifBlank { stringResource(R.string.common_unknown_author) },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        ArchiveDepthMark(archiveMemory)

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when {
                    book.finished -> "Finished"
                    book.progress > 0f -> "${(book.progress.coerceIn(0f, 1f) * 100).toInt()}%"
                    else -> book.format.name
                },
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass.copy(alpha = 0.88f),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onFavorite,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = if (book.favorite) {
                            "Remove ${book.title} from favorites"
                        } else {
                            "Add ${book.title} to favorites"
                        }
                    }
            ) {
                FavoriteIcon(book.favorite, Modifier.size(15.dp))
            }

            IconButton(
                onClick = onDetails,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Book details for ${book.title}"
                    }
            ) {
                EllipsisIcon(
                    Modifier.size(15.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BookLibraryRow(
    book: Book,
    archiveMemory: BookArchiveMemory?,
    artifactMemory: BookArtifactMemory?,
    showMemorySummary: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDetails: () -> Unit
) {
    val artifact = bookArtifactState(book, memory = artifactMemory)
    val registrationColor = when {
        book.finished -> VeilPalette.Brass
        artifact.recentlyOpened -> VeilPalette.Spirit
        book.favorite -> VeilPalette.Brass.copy(alpha = 0.76f)
        book.progress > 0f -> VeilPalette.Mist.copy(alpha = 0.72f)
        else -> VeilPalette.BorderDark
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "Read ${book.title}",
                onClick = onOpen
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.52f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
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
                    style = MaterialTheme.typography.titleSmall,
                    color = VeilPalette.Moon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    buildString {
                        append(
                            book.author.ifBlank {
                                "Unknown author"
                            }
                        )
                        book.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
                            append(" · ").append(series)
                            book.seriesIndex?.let {
                                append(" #").append(formatSeriesIndex(it))
                            }
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Mist.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when {
                            book.finished -> "COMPLETED"
                            book.progress > 0f ->
                                "${(book.progress.coerceIn(0f, 1f) * 100).toInt()}%"
                            else -> "UNOPENED"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass.copy(alpha = 0.84f)
                    )
                    Text(
                        book.format.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Mist.copy(alpha = 0.62f)
                    )
                    archiveMemory?.let { memory ->
                        Text(
                            archiveDepthRecord(memory),
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Mist.copy(alpha = 0.54f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (
                showMemorySummary &&
                artifactMemory != null &&
                (artifactMemory.highlightCount > 0 || artifactMemory.bookmarkCount > 0)
            ) {
                Text(
                    buildString {
                        if (artifactMemory.highlightCount > 0) {
                            append(artifactMemory.highlightCount).append(" marks")
                        }
                        if (artifactMemory.bookmarkCount > 0) {
                            if (isNotEmpty()) append(" · ")
                            append(artifactMemory.bookmarkCount).append(" saved")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Spirit.copy(alpha = 0.68f),
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onFavorite,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = if (book.favorite) {
                            "Remove ${book.title} from favorites"
                        } else {
                            "Add ${book.title} to favorites"
                        }
                    }
            ) {
                FavoriteIcon(book.favorite, Modifier.size(17.dp))
            }

            IconButton(
                onClick = onDetails,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Archive record for ${book.title}"
                    }
            ) {
                EllipsisIcon(
                    Modifier.size(17.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BookProgress(book: Book) {
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
                book.progress > 0f -> "${(book.progress.coerceIn(0f, 1f) * 100).toInt()}% read"
                else -> "Unopened"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val bookCollections = book.allCollections
        if (bookCollections.isNotEmpty()) {
            val label = if (bookCollections.size == 1) bookCollections.first()
            else "${bookCollections.first()} +${bookCollections.size - 1}"
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
                Text(
                    "PREPARING PUBLICATION",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    "Inspecting the file and preparing its local archive record.",
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

            Text(
                if (hasBooks) "NO MATCHING VOLUMES" else "YOUR ARCHIVE IS EMPTY",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.35.sp),
                color = VeilPalette.Brass
            )

            Text(
                if (hasBooks) "Nothing in this part of the archive" else "The first volume begins the world",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                if (hasBooks) {
                    "No book matches the current search, shelf, or collection. Clear the filters and the archive will return."
                } else {
                    "Import an EPUB or PDF. The first volume establishes your Archive; progress, highlights, and notes remain local on this device."
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
                    Text("Clear active filters")
                }
            } else {
                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A)
                    )
                ) {
                    Text(if (isImporting) "Preparing publication…" else "Import your first volume")
                }
            }
        }
    }
}

@Composable
private fun LibraryShelvesView(
    groups: List<LibraryShelfGroup>,
    artifactMemoryByBookId: Map<String, BookArtifactMemory>,
    itemWidthDp: Float,
    coverWidthDp: Float,
    coverHeightDp: Float,
    onOpen: (Book) -> Unit,
    onDetails: (Book) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        groups.forEach { group ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                LibrarySectionHeading(
                    eyebrow = group.eyebrow,
                    title = group.title,
                    trailing = "${group.books.size} volumes"
                )
                BrassRule(Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                ) {
                    group.books.forEach { book ->
                        Column(
                            modifier = Modifier
                                .width(itemWidthDp.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Read ${book.title}"
                                ) { onOpen(book) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = VeilPalette.Moon,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                when {
                                    book.finished -> "Completed"
                                    book.progress > 0f ->
                                        "${(book.progress.coerceIn(0f, 1f) * 100).toInt()}% read"
                                    else -> book.format.name
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = VeilPalette.Brass.copy(alpha = 0.82f),
                                maxLines = 1
                            )
                            TextButton(
                                onClick = { onDetails(book) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 0.dp)
                            ) {
                                Text("Archive record")
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            LibraryViewMode.GALLERY to "Gallery",
            LibraryViewMode.SHELVES to "Shelves",
            LibraryViewMode.INDEX to "Index"
        ).forEach { (candidate, label) ->
            val active = mode == candidate
            TextButton(
                onClick = { onChange(candidate) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "$label view"
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
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp)
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

private fun parseCollectionNames(value: String): List<String> = value
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinctBy { it.lowercase(Locale.ROOT) }

private fun formatSeriesIndex(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

