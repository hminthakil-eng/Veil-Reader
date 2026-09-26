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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookMetadataUpdate
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private enum class LibraryViewMode { GRID, LIST }

@Composable
fun LibraryScreen(
    books: List<Book>,
    isImporting: Boolean,
    onImportUri: (Uri) -> Unit,
    onOpenBook: (Book) -> Unit,
    onFavorite: (String) -> Unit,
    onEditMetadata: (BookMetadataUpdate) -> Unit,
    onOpenSettings: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var shelf by rememberSaveable { mutableStateOf("All") }
    var collection by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Recent") }
    var viewModeName by rememberSaveable { mutableStateOf(LibraryViewMode.GRID.name) }
    val viewMode = runCatching { LibraryViewMode.valueOf(viewModeName) }.getOrDefault(LibraryViewMode.GRID)
    var overviewExpanded by rememberSaveable { mutableStateOf(false) }
    var showShelves by rememberSaveable { mutableStateOf(false) }
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
            else -> true
        }
        val matchesCollection = collection.isEmpty() || book.allCollections.any {
            it.equals(collection, ignoreCase = true)
        }
        matchesQuery && matchesShelf && matchesCollection
    }.let { list ->
        when (sort) {
            "Title" -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
            "Author" -> list.sortedBy { it.author.lowercase(Locale.ROOT) }
            "Progress" -> list.sortedByDescending { it.progress }
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
    LazyVerticalGrid(
        columns = if (viewMode == LibraryViewMode.GRID) GridCells.Adaptive(148.dp) else GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
        contentPadding = PaddingValues(
            start = VeilSpacing.lg,
            end = VeilSpacing.lg,
            top = VeilSpacing.lg,
            bottom = 30.dp
        )
    ) {
        item(key = "library:heading", span = { GridItemSpan(maxLineSpan) }) {
            LibraryHeader(
                bookCount = books.size,
                isImporting = isImporting,
                onImport = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
                onOpenShelves = { showShelves = true },
                onOpenSettings = onOpenSettings
            )
        }

        item(key = "library:search", span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                label = { Text("Search the archive") },
                placeholder = { Text("Title, author, series, collection, or language") },
                leadingIcon = { SearchIcon(Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { query = "" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(top = VeilSpacing.lg)
            )
        }

        item(key = "library:shelves", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = VeilSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                listOf("All", "Reading", "Unread", "Finished", "Favorites").forEach { label ->
                    FilterChip(
                        selected = shelf == label,
                        onClick = { shelf = label },
                        label = { Text(label) },
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VeilPalette.DeepBrass.copy(alpha = 0.72f),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = shelf == label,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f),
                            selectedBorderColor = VeilPalette.Brass.copy(alpha = 0.68f)
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        item(key = "library:controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = VeilSpacing.xs, bottom = VeilSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "${filtered.size} shown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    ViewModeToggle(
                        mode = viewMode,
                        onChange = { viewModeName = it.name }
                    )
                }
                if (trimmedQuery.isNotBlank() || shelf != "All" || collection.isNotEmpty()) {
                    TextButton(
                        onClick = { query = ""; shelf = "All"; collection = "" },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Clear filters")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
                    if (collections.isNotEmpty()) {
                        Box(Modifier.weight(1f)) {
                            TextButton(onClick = { collectionMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (collection.isBlank()) "Collections" else collection, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(expanded = collectionMenu, onDismissRequest = { collectionMenu = false }) {
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

                    Box(Modifier.weight(1f)) {
                        TextButton(
                            onClick = { sortMenu = true },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Sort books: $sort" }
                        ) { Text(sort, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf("Recent", "Title", "Author", "Series", "Progress").forEach { label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { sort = label; sortMenu = false }
                                )
                            }
                        }
                    }

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

        item(key = "library:recent", span = { GridItemSpan(maxLineSpan) }) {
            if (recentReading.isNotEmpty() && trimmedQuery.isBlank() && shelf == "All" && collection.isEmpty()) {
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
                                RecentReadingBook(book = book, onOpen = { onOpenBook(book) })
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
                    onReset = { query = ""; shelf = "All"; collection = "" }
                )
            }
        } else {
            items(filtered, key = { "book:${it.id}" }, contentType = { viewMode }) { book ->
                when (viewMode) {
                    LibraryViewMode.GRID -> BookLibraryTile(
                        book = book,
                        onOpen = { onOpenBook(book) },
                        onFavorite = { onFavorite(book.id) },
                        onDetails = { detailBookId = book.id }
                    )
                    LibraryViewMode.LIST -> BookLibraryRow(
                        book = book,
                        onOpen = { onOpenBook(book) },
                        onFavorite = { onFavorite(book.id) },
                        onDetails = { detailBookId = book.id }
                    )
                }
            }
        }
    }

    detailBook?.let { book ->
        BookDetailSheet(
            book = book,
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

    if (showShelves) {
        ShelvesSheet(
            books = books,
            collections = collections,
            onDismiss = { showShelves = false },
            onSelectShelf = { selected ->
                shelf = selected
                collection = ""
                showShelves = false
            },
            onSelectCollection = { selected ->
                shelf = "All"
                collection = selected
                showShelves = false
            }
        )
    }

    editing?.let { book ->
        val parsedSeriesIndex = seriesIndex.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val seriesIndexInvalid = seriesIndex.isNotBlank() && (parsedSeriesIndex == null || !parsedSeriesIndex.isFinite())
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.book_metadata_dialog_title)) },
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
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = VeilSpacing.lg)
                        .padding(top = VeilSpacing.md, bottom = VeilSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("← Back")
                        }
                        Text(
                            "BOOK DETAIL",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                            color = VeilPalette.Brass
                        )
                    }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                VeilPalette.DeepBrass.copy(alpha = 0.42f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.52f)),
                        MaterialTheme.shapes.small
                    )
                    .padding(VeilSpacing.lg)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCover(
                        title = book.title,
                        subtitle = book.author,
                        imagePath = book.coverCachePath,
                        modifier = Modifier.width(122.dp).height(178.dp)
                    )
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                    ) {
                        Text(
                            if (book.isImported) stringResource(R.string.book_detail_local_publication_format, book.format.name) else stringResource(R.string.book_detail_sample_entry),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp),
                            color = VeilPalette.Brass
                        )
                        BrassRule(Modifier.width(58.dp), strong = true)
                        Text(
                            book.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (book.author.isBlank()) stringResource(R.string.common_unknown_author) else book.author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        book.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
                            Text(
                                buildString {
                                    append(series)
                                    book.seriesIndex?.let { append(" · #${formatSeriesIndex(it)}") }
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = VeilPalette.Brass,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            status,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            BrassRule(Modifier.fillMaxWidth())

            ArchivePanel(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = if (book.finished) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    book.currentChapter
                        .takeIf { it.isNotBlank() && it != "Not started" }
                        ?.let { chapter ->
                            Text(
                                chapter,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 210.dp)
                            )
                        }
                }
            }

            Button(
                onClick = onOpen,
                enabled = book.isImported,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(if (book.isImported) primaryAction else stringResource(R.string.book_detail_publication_unavailable))
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onFavorite,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(if (book.favorite) stringResource(R.string.book_detail_favorited) else stringResource(R.string.book_detail_favorite))
                }
                OutlinedButton(
                    onClick = onEditMetadata,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.book_detail_edit_details))
                }
            }

            BrassRule(Modifier.fillMaxWidth())

            ArchivePanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.book_detail_publication_details),
                    style = MaterialTheme.typography.titleLarge
                )
                BrassRule(Modifier.width(72.dp))
                BookDetailFact(stringResource(R.string.book_detail_format), book.format.name)
                book.language?.takeIf { it.isNotBlank() }?.let {
                    BookDetailFact(stringResource(R.string.book_detail_language), it)
                }
                if (book.totalPages > 0) {
                    BookDetailFact(
                        stringResource(R.string.book_detail_pages),
                        stringResource(R.string.book_detail_page_progress, book.pagesRead.coerceAtLeast(0).coerceAtMost(book.totalPages), book.totalPages)
                    )
                }
                BookDetailFact(
                    stringResource(R.string.book_detail_stored),
                    if (book.isImported) stringResource(R.string.book_detail_private_local_copy) else stringResource(R.string.book_detail_sample_metadata_only)
                )
            }

            if (book.allCollections.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                    Text(stringResource(R.string.book_detail_collections), style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                    ) {
                        book.allCollections.forEach { collection ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                                border = BorderStroke(
                                    1.dp,
                                    VeilPalette.Brass.copy(alpha = 0.34f)
                                )
                            ) {
                                Text(
                                    collection,
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelvesSheet(
    books: List<Book>,
    collections: List<String>,
    onDismiss: () -> Unit,
    onSelectShelf: (String) -> Unit,
    onSelectCollection: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = VeilPalette.Brass.copy(alpha = 0.58f))
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VeilSpacing.lg)
                .padding(bottom = VeilSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Text(
                "SHELVES",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = VeilPalette.Brass
            )
            Text("Collections", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Move through the archive by reading state or by the collections you created.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BrassRule(Modifier.width(82.dp), strong = true)

            ArchivePanel(Modifier.fillMaxWidth()) {
                ShelfChoiceRow("All books", books.size) { onSelectShelf("All") }
                ShelfChoiceRow(
                    "Currently reading",
                    books.count { !it.finished && it.progress > 0f }
                ) { onSelectShelf("Reading") }
                ShelfChoiceRow(
                    "Favorites",
                    books.count { it.favorite }
                ) { onSelectShelf("Favorites") }
                ShelfChoiceRow(
                    "Completed",
                    books.count { it.finished }
                ) { onSelectShelf("Finished") }
                ShelfChoiceRow(
                    "Plan to read",
                    books.count { !it.finished && it.progress == 0f }
                ) { onSelectShelf("Unread") }
            }

            if (collections.isNotEmpty()) {
                Text("Custom collections", style = MaterialTheme.typography.titleMedium)
                collections.forEach { name ->
                    ShelfChoiceRow(
                        title = name,
                        count = books.count { book ->
                            book.allCollections.any { it.equals(name, ignoreCase = true) }
                        },
                        onClick = { onSelectCollection(name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfChoiceRow(
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.24f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "$count",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("›", style = MaterialTheme.typography.titleMedium, color = VeilPalette.Brass)
        }
    }
}

@Composable
private fun LibraryHeader(
    bookCount: Int,
    isImporting: Boolean,
    onImport: () -> Unit,
    onOpenShelves: () -> Unit,
    onOpenSettings: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 520.dp
        val subtitle = if (bookCount == 0) {
            "Import an EPUB or PDF to begin. Everything stays local on this device."
        } else {
            "$bookCount ${if (bookCount == 1) "volume" else "volumes"} · fragments, records, and stories kept close."
        }
        val shape = MaterialTheme.shapes.small

        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 210.dp else 190.dp)
                .clip(shape)
                .border(BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.44f)), shape)
        ) {
            Image(
                painter = painterResource(R.drawable.grayfog_threshold_v1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            VeilPalette.Ink.copy(alpha = 0.36f),
                            VeilPalette.Ink.copy(alpha = 0.72f),
                            VeilPalette.Ink.copy(alpha = 0.97f)
                        )
                    )
                )
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            VeilPalette.Ink.copy(alpha = 0.74f),
                            Color.Transparent,
                            VeilPalette.Ink.copy(alpha = 0.28f)
                        )
                    )
                )
            )

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(VeilSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                Text(
                    "GRAYFOG ARCHIVE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    "The Grand Library",
                    style = MaterialTheme.typography.headlineLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Moon.copy(alpha = 0.82f)
                )
                BrassRule(Modifier.width(86.dp), strong = true)

                if (compact) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = onOpenShelves,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VeilPalette.Moon)
                        ) {
                            Text("Shelves")
                        }
                        Button(
                            onClick = onImport,
                            enabled = !isImporting,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text(if (isImporting) "Importing…" else "Import volume")
                        }
                    }
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.align(Alignment.End).heightIn(min = 44.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.Moon)
                    ) {
                        Text("Settings")
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        Button(
                            onClick = onImport,
                            enabled = !isImporting,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(if (isImporting) "Importing…" else "Import volume")
                        }
                        OutlinedButton(
                            onClick = onOpenShelves,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VeilPalette.Moon)
                        ) {
                            Text("Shelves")
                        }
                        TextButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.Moon)
                        ) {
                            Text("Settings")
                        }
                    }
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
private fun LibrarySectionHeading(eyebrow: String, title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.35.sp),
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
private fun RecentReadingBook(book: Book, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.width(272.dp).clickable(
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
                modifier = Modifier.width(56.dp).height(80.dp)
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
private fun BookLibraryTile(
    book: Book,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDetails: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Box {
            BookCover(
                title = book.title,
                subtitle = book.author,
                imagePath = book.coverCachePath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f)
                    .clickable(onClickLabel = "Read ${book.title}", onClick = onOpen)
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.91f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    book.format.name,
                    Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp)
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(48.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.small
            ) {
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = if (book.favorite) {
                                "Remove ${book.title} from favorites"
                            } else {
                                "Add ${book.title} to favorites"
                            }
                        }
                ) {
                    FavoriteIcon(
                        favorite = book.favorite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(48.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.28f))
            ) {
                IconButton(
                    onClick = onDetails,
                    modifier = Modifier.fillMaxSize().semantics {
                        contentDescription = "Book details for ${book.title}"
                    }
                ) {
                    EllipsisIcon(Modifier.size(18.dp), MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(VeilSpacing.xs))
        Column(
            Modifier.fillMaxWidth().clickable(
                role = Role.Button,
                onClickLabel = "Read ${book.title}",
                onClick = onOpen
            )
        ) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                book.author.ifBlank { stringResource(R.string.common_unknown_author) },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            book.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
                Text(
                    buildString {
                        append(series)
                        book.seriesIndex?.let { append(" · #${formatSeriesIndex(it)}") }
                    },
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        BookProgress(book)
    }
}

@Composable
private fun BookLibraryRow(
    book: Book,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDetails: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Read ${book.title}", onClick = onOpen),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.24f))
    ) {
        Row(
            Modifier.padding(VeilSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                title = book.title,
                subtitle = book.author,
                imagePath = book.coverCachePath,
                modifier = Modifier.width(68.dp).height(98.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    book.author.ifBlank { stringResource(R.string.common_unknown_author) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                book.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
                    Text(
                        series,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BookProgress(book)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.semantics {
                        contentDescription = if (book.favorite) {
                            "Remove ${book.title} from favorites"
                        } else {
                            "Add ${book.title} to favorites"
                        }
                    }
                ) {
                    FavoriteIcon(book.favorite, Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDetails,
                    modifier = Modifier.semantics { contentDescription = "Book details for ${book.title}" }
                ) {
                    EllipsisIcon(Modifier.size(19.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
private fun LibraryEmptyState(
    hasBooks: Boolean,
    isImporting: Boolean,
    onImport: () -> Unit,
    onReset: () -> Unit
) {
    MysteryCard(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            ShelfIcon(Modifier.size(28.dp), MaterialTheme.colorScheme.secondary)
        }
        Text(
            if (hasBooks) "No books match" else "Your library is empty",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            if (hasBooks) {
                "Try another search, shelf, or collection filter."
            } else {
                "Import an EPUB or PDF. Reading state, notes, and cached publication data stay on this device."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasBooks) {
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Clear filters")
            }
        } else {
            Button(
                onClick = onImport,
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(if (isImporting) "Importing…" else "Import a book")
            }
        }
    }
}

@Composable
private fun ViewModeToggle(mode: LibraryViewMode, onChange: (LibraryViewMode) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f)
    ) {
        Row(Modifier.padding(2.dp)) {
            IconButton(
                onClick = { onChange(LibraryViewMode.GRID) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (mode == LibraryViewMode.GRID) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .semantics {
                        contentDescription = "Grid view"
                        selected = mode == LibraryViewMode.GRID
                    }
            ) {
                GridIcon(
                    Modifier.size(18.dp),
                    if (mode == LibraryViewMode.GRID) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onChange(LibraryViewMode.LIST) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (mode == LibraryViewMode.LIST) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .semantics {
                        contentDescription = "List view"
                        selected = mode == LibraryViewMode.LIST
                    }
            ) {
                ListIcon(
                    Modifier.size(18.dp),
                    if (mode == LibraryViewMode.LIST) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
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

