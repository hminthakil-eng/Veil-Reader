package com.veilreader.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.search.isSearchable
import org.readium.r2.shared.publication.services.search.search

private enum class ReaderNotebookTab(val label: String) {
    CONTENTS("Contents"),
    BOOKMARKS("Bookmarks"),
    NOTES("Notes"),
    SEARCH("Search")
}

/** Reading tools stay in a dismissible sheet, away from the reading surface. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun ReaderNotebook(
    opened: OpenedPublication,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onGo: (String) -> Unit,
    onChapter: (Link) -> Unit,
    onSaveNote: suspend (String, String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val searchable = remember(opened.book.id) { opened.publication.isSearchable }
    val tabs = remember(searchable) {
        buildList {
            add(ReaderNotebookTab.CONTENTS)
            add(ReaderNotebookTab.BOOKMARKS)
            add(ReaderNotebookTab.NOTES)
            if (searchable) add(ReaderNotebookTab.SEARCH)
        }
    }
    var tab by remember(opened.book.id, searchable) { mutableStateOf(ReaderNotebookTab.CONTENTS) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Highlight?>(null) }
    var note by remember { mutableStateOf("") }
    var savingNote by remember { mutableStateOf(false) }
    var noteSaveError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<Highlight?>(null) }
    var bookSearchQuery by remember { mutableStateOf("") }
    var bookSearchResults by remember { mutableStateOf<List<Locator>>(emptyList()) }
    var bookSearchError by remember { mutableStateOf<String?>(null) }
    var searchingBook by remember { mutableStateOf(false) }

    val chapters = remember(opened.book.id) {
        fun flatten(links: List<Link>, depth: Int): List<Pair<Link, Int>> =
            links.flatMap { listOf(it to depth) + flatten(it.children, depth + 1) }
        flatten(opened.publication.tableOfContents.ifEmpty { opened.publication.readingOrder }, 0)
    }
    val matchingHighlights = highlights.filter {
        query.isBlank() || it.quote.contains(query.trim(), true) || it.note.contains(query.trim(), true)
    }

    fun runBookSearch() {
        val term = bookSearchQuery.trim()
        if (term.length < 2 || searchingBook) return
        scope.launch {
            searchingBook = true
            bookSearchError = null
            bookSearchResults = emptyList()
            try {
                val iterator = opened.publication.search(term)
                if (iterator == null) {
                    bookSearchError = "Search is not available for this publication."
                    return@launch
                }
                try {
                    val found = mutableListOf<Locator>()
                    iterator.forEach { page -> found += page.locators }
                        .onFailure { error -> bookSearchError = error.message }
                    bookSearchResults = found
                } finally {
                    iterator.close()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                bookSearchError = error.message ?: "Search failed."
            } finally {
                searchingBook = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(.85f)
                .padding(horizontal = 20.dp)
        ) {
            Text("Reading tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                opened.book.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { item ->
                    FilterChip(
                        selected = tab == item,
                        onClick = { tab = item },
                        label = { Text(item.label) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            if (tab == ReaderNotebookTab.NOTES) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search highlights and notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
            }

            if (tab == ReaderNotebookTab.SEARCH) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bookSearchQuery,
                        onValueChange = { bookSearchQuery = it },
                        label = { Text("Search inside this book") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = ::runBookSearch,
                        enabled = bookSearchQuery.trim().length >= 2 && !searchingBook,
                        modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp)
                    ) { Text(if (searchingBook) "…" else "Find") }
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                when (tab) {
                    ReaderNotebookTab.CONTENTS -> {
                        if (chapters.isEmpty()) item { Text("This book has no chapter list.") }
                        items(chapters) { (link, depth) ->
                            TextButton(
                                onClick = { onChapter(link) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(start = (depth.coerceAtMost(4) * 12).dp)
                            ) {
                                Text(link.title ?: "Untitled section", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    ReaderNotebookTab.BOOKMARKS -> {
                        if (bookmarks.isEmpty()) item {
                            Text("Save a place using Bookmark in the reader. Your bookmarks will appear here.")
                        }
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(bookmark.label, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { onGo(bookmark.locatorJson) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text("Go to place") }
                                        TextButton(
                                            onClick = { onDeleteBookmark(bookmark.id) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text("Remove") }
                                    }
                                }
                            }
                        }
                    }

                    ReaderNotebookTab.NOTES -> {
                        if (matchingHighlights.isEmpty()) item {
                            Text(
                                if (query.isBlank()) "Highlight a passage to start your notebook. EPUB text highlights are supported; PDF bookmarks are available in the Bookmarks tab."
                                else "No matching highlights or notes."
                            )
                        }
                        items(matchingHighlights, key = { it.id }) { highlight ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(highlight.quote, style = MaterialTheme.typography.bodyLarge)
                                    if (highlight.note.isNotBlank()) {
                                        Text(highlight.note, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        TextButton(
                                            onClick = { onGo(highlight.locatorJson) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text("Go") }
                                        TextButton(
                                            onClick = {
                                                editing = highlight
                                                note = highlight.note
                                                noteSaveError = null
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text(if (highlight.note.isBlank()) "Add note" else "Edit note")
                                        }
                                        TextButton(
                                            onClick = { deleting = highlight },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }

                    ReaderNotebookTab.SEARCH -> {
                        bookSearchError?.let { message ->
                            item { Text(message, color = MaterialTheme.colorScheme.error) }
                        }
                        if (!searchingBook && bookSearchError == null && bookSearchQuery.isNotBlank() && bookSearchResults.isEmpty()) {
                            item {
                                Text(
                                    "No matches found in this book.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(bookSearchResults) { locator ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    locator.title?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(searchSnippet(locator), style = MaterialTheme.typography.bodyMedium)
                                    TextButton(
                                        onClick = { onGo(locator.toJSON().toString()) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) { Text("Go to match") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { highlight ->
        AlertDialog(
            onDismissRequest = { if (!savingNote) editing = null },
            title = { Text("Passage note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Your thoughts") },
                        minLines = 4,
                        maxLines = 8,
                        enabled = !savingNote
                    )
                    noteSaveError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !savingNote,
                    onClick = {
                        scope.launch {
                            savingNote = true
                            noteSaveError = null
                            try {
                                onSaveNote(highlight.id, note)
                                editing = null
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                noteSaveError = error.message ?: "Could not save this note."
                            } finally {
                                savingNote = false
                            }
                        }
                    }
                ) { Text(if (savingNote) "Saving…" else "Save") }
            },
            dismissButton = {
                TextButton(enabled = !savingNote, onClick = { editing = null }) { Text("Cancel") }
            }
        )
    }

    deleting?.let { highlight ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this highlight?") },
            text = { Text("Its attached note will also be removed.") },
            confirmButton = {
                TextButton(onClick = { onDeleteHighlight(highlight.id); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }
        )
    }
}

private fun searchSnippet(locator: Locator): String {
    val before = locator.text.before.orEmpty().replace(Regex("\\s+"), " ").trim().takeLast(100)
    val hit = locator.text.highlight.orEmpty().replace(Regex("\\s+"), " ").trim()
    val after = locator.text.after.orEmpty().replace(Regex("\\s+"), " ").trim().take(100)
    return buildString {
        if (before.isNotBlank()) append("…").append(before).append(' ')
        if (hit.isNotBlank()) append(hit)
        if (after.isNotBlank()) append(' ').append(after).append("…")
    }.ifBlank { locator.title ?: "Search match" }
}
