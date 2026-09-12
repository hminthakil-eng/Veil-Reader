package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookMetadataUpdate
import com.veilreader.app.ui.theme.VeilSpacing
import java.util.Locale

/**
 * The Grand Library is the reader's long-lived archive, not a file manager.
 *
 * Search, filtering, collections and metadata stay practical and scalable. Atmosphere is concentrated
 * in the archive overview and recent-reading shelf so large libraries remain fast and legible.
 */
@Composable
fun LibraryScreen(
    books: List<Book>,
    isImporting: Boolean,
    onImportUri: (Uri) -> Unit,
    onOpenBook: (Book) -> Unit,
    onFavorite: (String) -> Unit,
    onEditMetadata: (BookMetadataUpdate) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var shelf by rememberSaveable { mutableStateOf("All") }
    var collection by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Recent") }
    var collectionMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Book?>(null) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var collectionNames by remember { mutableStateOf("") }
    var seriesName by remember { mutableStateOf("") }
    var seriesIndex by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }

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

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = VeilSpacing.lg)
            .padding(top = VeilSpacing.lg)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Box(Modifier.weight(1f)) {
                ScreenHeader(
                    eyebrow = "Grand Library",
                    title = "The living archive",
                    subtitle = if (books.isEmpty()) {
                        "A quiet chamber waiting for its first story."
                    } else {
                        "${books.size} ${if (books.size == 1) "volume" else "volumes"} kept privately on this device."
                    }
                )
            }
            FilledTonalButton(
                onClick = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
                enabled = !isImporting,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isImporting) "Importing…" else "+ Import")
            }
        }

        if (books.isNotEmpty()) {
            ArchiveOverview(
                total = books.size,
                reading = books.count { !it.finished && it.progress > 0f },
                finished = books.count { it.finished },
                collections = collections.size,
                modifier = Modifier.padding(top = VeilSpacing.lg)
            )
        }

        if (recentReading.isNotEmpty()) {
            Column(
                Modifier.padding(top = VeilSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                LibrarySectionHeading(
                    eyebrow = "Open passages",
                    title = "Stories still in motion",
                    trailing = "${recentReading.size} active"
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                ) {
                    recentReading.forEach { book ->
                        RecentReadingBook(book = book, onOpen = { onOpenBook(book) })
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search title, author, series, collection") },
            leadingIcon = { Text("⌕", fontSize = 20.sp) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(onClick = { query = "" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VeilSpacing.lg)
        )

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
                    label = { Text(label) }
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = VeilSpacing.xs, bottom = VeilSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${filtered.size} ${if (filtered.size == 1) "volume" else "volumes"} shown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            if (collections.isNotEmpty()) {
                Box {
                    TextButton(onClick = { collectionMenu = true }) {
                        Text(if (collection.isBlank()) "Collections ▾" else "$collection ▾")
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

            Box {
                TextButton(onClick = { sortMenu = true }) { Text("$sort ▾") }
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

        if (filtered.isEmpty()) {
            MysteryCard(Modifier.fillMaxWidth()) {
                Text("◇", fontSize = 34.sp, color = MaterialTheme.colorScheme.secondary)
                Text(
                    if (books.isEmpty()) "The first shelf is empty" else "No volume answers that call",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    if (books.isEmpty()) {
                        "Import an EPUB or PDF. Veil Reader keeps its reading state, annotations and cached publication data locally."
                    } else {
                        "Change the search, shelf or collection to reveal more of the archive."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (books.isEmpty()) {
                    Button(
                        onClick = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
                        enabled = !isImporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = VeilSpacing.xs)
                    ) { Text(if (isImporting) "Importing…" else "Bring in the first book") }
                } else {
                    OutlinedButton(
                        onClick = { query = ""; shelf = "All"; collection = "" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = VeilSpacing.xs)
                    ) { Text("Reveal the whole archive") }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(148.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                items(filtered, key = { it.id }) { book ->
                    BookLibraryTile(
                        book = book,
                        onOpen = { onOpenBook(book) },
                        onFavorite = { onFavorite(book.id) },
                        onEdit = {
                            editing = book
                            title = book.title
                            author = book.author
                            collectionNames = book.allCollections.joinToString(", ")
                            seriesName = book.seriesName.orEmpty()
                            seriesIndex = book.seriesIndex?.let(::formatSeriesIndex).orEmpty()
                            language = book.language.orEmpty()
                        }
                    )
                }
            }
        }
    }

    editing?.let { book ->
        val parsedSeriesIndex = seriesIndex.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val seriesIndexInvalid = seriesIndex.isNotBlank() && (parsedSeriesIndex == null || !parsedSeriesIndex.isFinite())
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Book details") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, isError = title.isBlank())
                    OutlinedTextField(author, { author = it }, label = { Text("Author") })
                    OutlinedTextField(
                        collectionNames,
                        { collectionNames = it },
                        label = { Text("Collections") },
                        supportingText = { Text("Separate multiple collections with commas.") }
                    )
                    OutlinedTextField(seriesName, { seriesName = it }, label = { Text("Series (optional)") })
                    OutlinedTextField(
                        seriesIndex,
                        { seriesIndex = it },
                        label = { Text("Series number (optional)") },
                        isError = seriesIndexInvalid,
                        supportingText = {
                            if (seriesIndexInvalid) Text("Use a number such as 1 or 2.5.")
                        }
                    )
                    OutlinedTextField(
                        language,
                        { language = it },
                        label = { Text("Language tag (optional)") },
                        supportingText = { Text("BCP-47, for example en, fa, tr, or en-US.") }
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
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
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
    val shape = MaterialTheme.shapes.large
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                )
            )
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)), shape)
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
                    Text("ARCHIVE AWAKE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    Text("Every finished story leaves a trace.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                ArchiveStat("Volumes", total, Modifier.weight(1f))
                ArchiveStat("Reading", reading, Modifier.weight(1f))
                ArchiveStat("Finished", finished, Modifier.weight(1f))
                ArchiveStat("Collections", collections, Modifier.weight(1f))
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun LibrarySectionHeading(eyebrow: String, title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
                color = MaterialTheme.colorScheme.secondary
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
    Column(
        Modifier
            .width(112.dp)
            .clickable(onClickLabel = "Continue ${book.title}", onClick = onOpen)
    ) {
        BookCover(
            title = book.title,
            subtitle = book.author,
            imagePath = book.coverCachePath,
            modifier = Modifier
                .width(112.dp)
                .height(160.dp)
        )
        Spacer(Modifier.height(VeilSpacing.xs))
        Text(
            book.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${(book.progress.coerceIn(0f, 1f) * 100).toInt()}% read",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun BookLibraryTile(
    book: Book,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit
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
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    book.format.name,
                    Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp)
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .clickable(onClickLabel = if (book.favorite) "Remove favorite" else "Add favorite", onClick = onFavorite),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    if (book.favorite) "★" else "☆",
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    fontSize = 16.sp,
                    color = if (book.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(VeilSpacing.xs))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    book.author.ifBlank { "Unknown author" },
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
            TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text("•••", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { book.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = if (book.finished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                when {
                    book.finished -> "Finished"
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
