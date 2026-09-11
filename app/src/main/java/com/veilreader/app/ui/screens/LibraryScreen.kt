package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book

@Composable
fun LibraryScreen(
    books: List<Book>,
    isImporting: Boolean,
    onImportUri: (Uri) -> Unit,
    onOpenBook: (Book) -> Unit,
    onFavorite: (String) -> Unit,
    onEditMetadata: (String, String, String, String) -> Unit
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
    var collectionName by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportUri)
    }
    val collections = books.map { it.collection }.filter { it.isNotBlank() }.distinct().sorted()
    LaunchedEffect(collections) {
        if (collection.isNotEmpty() && collection !in collections) collection = ""
    }

    val filtered = books.filter { book ->
        val matchesQuery = query.isBlank() || listOf(book.title, book.author, book.collection)
            .any { it.contains(query.trim(), ignoreCase = true) }
        val matchesShelf = when (shelf) {
            "Reading" -> !book.finished && book.progress > 0f
            "Unread" -> !book.finished && book.progress == 0f
            "Finished" -> book.finished
            "Favorites" -> book.favorite
            else -> true
        }
        matchesQuery && matchesShelf && (collection.isEmpty() || book.collection == collection)
    }.let { list ->
        when (sort) {
            "Title" -> list.sortedBy { it.title.lowercase() }
            "Author" -> list.sortedBy { it.author.lowercase() }
            "Progress" -> list.sortedByDescending { it.progress }
            else -> list.sortedByDescending { maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "GRAND LIBRARY",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    letterSpacing = 1.7.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Your books", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (books.isEmpty()) "A quiet shelf, ready for its first story."
                    else "${books.size} ${if (books.size == 1) "book" else "books"} in your private library",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
                enabled = !isImporting
            ) {
                Text(if (isImporting) "Importing…" else "+ Import")
            }
        }

        if (books.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LibraryStat("Reading", books.count { !it.finished && it.progress > 0f }, Modifier.weight(1f))
                LibraryStat("Finished", books.count { it.finished }, Modifier.weight(1f))
                LibraryStat("Favorites", books.count { it.favorite }, Modifier.weight(1f))
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search books, authors, collections") },
            leadingIcon = { Text("⌕", fontSize = 20.sp) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(onClick = { query = "" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                .padding(top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${filtered.size} shown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            if (collections.isNotEmpty()) {
                Box {
                    TextButton(onClick = { collectionMenu = true }) {
                        Text(if (collection.isBlank()) "All collections ▾" else "$collection ▾")
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
                    listOf("Recent", "Title", "Author", "Progress").forEach { label ->
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
                Text("✦", fontSize = 32.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (books.isEmpty()) "Your first tome awaits" else "Nothing on this shelf",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (books.isEmpty()) {
                        "Import an EPUB or PDF. Veil Reader keeps a private local copy with your progress, notes and bookmarks."
                    } else {
                        "Try another search, shelf or collection."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                if (books.isEmpty()) {
                    Button(
                        onClick = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
                        enabled = !isImporting,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isImporting) "Importing…" else "Import your first book") }
                } else {
                    OutlinedButton(
                        onClick = { query = ""; shelf = "All"; collection = "" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear filters") }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
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
                            collectionName = book.collection
                        }
                    )
                }
            }
        }
    }

    editing?.let { book ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Book details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, isError = title.isBlank())
                    OutlinedTextField(author, { author = it }, label = { Text("Author") })
                    OutlinedTextField(collectionName, { collectionName = it }, label = { Text("Collection (optional)") })
                    Text(
                        "Use the same collection name on several books to group them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onEditMetadata(book.id, title, author, collectionName)
                        editing = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LibraryStat(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f)
                    .clickable(onClickLabel = "Read ${book.title}", onClick = onOpen)
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    book.format.name,
                    Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .clickable(onClickLabel = if (book.favorite) "Remove favorite" else "Add favorite", onClick = onFavorite),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (book.favorite) "★" else "☆",
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    fontSize = 16.sp,
                    color = if (book.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Text(
                    book.author,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text("•••", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { book.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
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
                    book.progress > 0f -> "${(book.progress * 100).toInt()}% read"
                    else -> "Not started"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            book.collection.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
