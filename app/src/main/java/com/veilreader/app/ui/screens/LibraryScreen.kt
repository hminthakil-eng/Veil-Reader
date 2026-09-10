package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Grand Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${books.size} books · yours to explore", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = { launcher.launch(arrayOf("application/epub+zip", "application/pdf")) },
                enabled = !isImporting) { Text(if (isImporting) "Importing…" else "Import") }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text("Search title, author or collection") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Reading", "Unread", "Finished", "Favorites").forEach { label ->
                FilterChip(selected = shelf == label, onClick = { shelf = label }, label = { Text(label) })
            }
        }
        if (collections.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = collection.isEmpty(), onClick = { collection = "" }, label = { Text("Every collection") })
                collections.forEach { label ->
                    FilterChip(selected = collection == label, onClick = { collection = label }, label = { Text(label) })
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            Text("Sort: ", style = MaterialTheme.typography.labelMedium)
            listOf("Recent", "Title", "Author", "Progress").forEach { label ->
                TextButton(onClick = { sort = label }) { Text(if (sort == label) "• $label" else label) }
            }
        }
        if (filtered.isEmpty()) {
            MysteryCard(Modifier.fillMaxWidth()) {
                Text(if (books.isEmpty()) "Your first tome awaits" else "No books match", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(if (books.isEmpty()) "Import an EPUB or PDF. A private copy stays on this device, with your progress and notes."
                    else "Try another search or clear your filters.")
                if (books.isNotEmpty()) TextButton(onClick = { query = ""; shelf = "All"; collection = "" }) { Text("Clear filters") }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.id }) { book ->
                    Column(Modifier.fillMaxWidth()) {
                        Column(Modifier.clickable(onClickLabel = "Read ${book.title}") { onOpenBook(book) }) {
                            Box {
                                BookCover(book.title, Modifier.fillMaxWidth().aspectRatio(.72f))
                                Surface(Modifier.align(Alignment.TopEnd).padding(8.dp), shape = MaterialTheme.shapes.small) {
                                    Text(book.format.name, Modifier.padding(6.dp), fontSize = 10.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth())
                            Text(if (book.finished) "Finished" else "${(book.progress * 100).toInt()}% read", fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { onFavorite(book.id) }) { Text(if (book.favorite) "Unsave" else "Favorite") }
                            TextButton(onClick = {
                                editing = book; title = book.title; author = book.author; collectionName = book.collection
                            }) { Text("Edit") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { book ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Book details") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, isError = title.isBlank())
                OutlinedTextField(author, { author = it }, label = { Text("Author") })
                OutlinedTextField(collectionName, { collectionName = it }, label = { Text("Collection (optional)") })
                Text("Use the same collection name on several books to group them.", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onEditMetadata(book.id, title, author, collectionName); editing = null
            }) { Text("Save") }
        }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
}
