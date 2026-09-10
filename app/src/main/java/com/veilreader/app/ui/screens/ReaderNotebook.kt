package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import org.readium.r2.shared.publication.Link

/** Reading tools stay in a dismissible sheet, away from the reading surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderNotebook(
    opened: OpenedPublication,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onGo: (String) -> Unit,
    onChapter: (Link) -> Unit,
    onSaveNote: (String, String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Highlight?>(null) }
    var note by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Highlight?>(null) }
    val chapters = remember(opened.book.id) {
        fun flatten(links: List<Link>, depth: Int): List<Pair<Link, Int>> =
            links.flatMap { listOf(it to depth) + flatten(it.children, depth + 1) }
        flatten(opened.publication.tableOfContents.ifEmpty { opened.publication.readingOrder }, 0)
    }
    val matchingHighlights = highlights.filter {
        query.isBlank() || it.quote.contains(query.trim(), true) || it.note.contains(query.trim(), true)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).padding(horizontal = 20.dp)) {
            Text("Your reading notebook", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(opened.book.title, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Contents", "Bookmarks", "Notes").forEachIndexed { index, title ->
                    FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(title) })
                }
            }
            if (tab == 2) {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    label = { Text("Search highlights and notes") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp)) {
                when (tab) {
                    0 -> {
                        if (chapters.isEmpty()) item { Text("This book has no chapter list.") }
                        items(chapters) { (link, depth) ->
                            TextButton(onClick = { onChapter(link) },
                                modifier = Modifier.fillMaxWidth().padding(start = (depth.coerceAtMost(4) * 12).dp)) {
                                Text(link.title ?: "Untitled section", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    1 -> {
                        if (bookmarks.isEmpty()) item {
                            Text("Save a place using Bookmark + in the reader. Your bookmarks will appear here.")
                        }
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(bookmark.label, fontWeight = FontWeight.SemiBold)
                                    Row {
                                        TextButton(onClick = { onGo(bookmark.locatorJson) }) { Text("Go to place") }
                                        TextButton(onClick = { onDeleteBookmark(bookmark.id) }) { Text("Remove") }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (matchingHighlights.isEmpty()) item {
                            Text(if (query.isBlank()) "Highlight a passage to start your notebook. EPUB text highlights are supported; PDF bookmarks are available in the Bookmarks tab."
                                else "No matching highlights or notes.")
                        }
                        items(matchingHighlights, key = { it.id }) { highlight ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(highlight.quote, style = MaterialTheme.typography.bodyLarge)
                                    if (highlight.note.isNotBlank()) Text(highlight.note, color = MaterialTheme.colorScheme.primary)
                                    Row {
                                        TextButton(onClick = { onGo(highlight.locatorJson) }) { Text("Go") }
                                        TextButton(onClick = { editing = highlight; note = highlight.note }) {
                                            Text(if (highlight.note.isBlank()) "Add note" else "Edit note")
                                        }
                                        TextButton(onClick = { deleting = highlight }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { highlight ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Passage note") },
            text = {
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("Your thoughts") }, minLines = 4, maxLines = 8)
            },
            confirmButton = { TextButton(onClick = { onSaveNote(highlight.id, note); editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
    deleting?.let { highlight ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this highlight?") },
            text = { Text("Its attached note will also be removed.") },
            confirmButton = { TextButton(onClick = { onDeleteHighlight(highlight.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } })
    }
}
