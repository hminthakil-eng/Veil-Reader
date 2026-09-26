package com.veilreader.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.theme.VeilPalette

@Composable
internal fun LibraryShelvesDialog(
    books: List<Book>, collections: List<String>, onDismiss: () -> Unit,
    onShelf: (String) -> Unit, onCollection: (String) -> Unit
) {
    val shelves = listOf(
        Triple("Favorites", "Favorites", books.filter { it.favorite }),
        Triple("Reading", "Currently reading", books.filter { !it.finished && it.progress > 0f }),
        Triple("Finished", "Completed", books.filter { it.finished }),
        Triple("Unread", "Unread", books.filter { !it.finished && it.progress == 0f })
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.94f).background(VeilPalette.Obsidian)
            .systemBarsPadding(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "shelves:heading") {
                Column {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.OldGold)) { Text("‹ Library") }
                    Box(Modifier.fillMaxWidth()) {
                        Image(painterResource(R.drawable.grayfog_threshold_v1), null,
                            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                            listOf(VeilPalette.Ink.copy(alpha = .45f), VeilPalette.Obsidian))))
                        Column(Modifier.padding(16.dp)) {
                            Spacer(Modifier.height(48.dp))
                            Text("Collections", style = MaterialTheme.typography.headlineLarge, color = VeilPalette.Moon)
                            Text("A place for every volume.", style = MaterialTheme.typography.bodyMedium, color = VeilPalette.Mist)
                            VeilOrnamentDivider()
                        }
                    }
                }
            }
            items(shelves, key = { "shelf:${it.first}" }) { (key, label, members) ->
                ShelfEntry(label, members) { onShelf(key) }
            }
            item(key = "shelves:custom") {
                Text("Your collections", style = MaterialTheme.typography.titleLarge, color = VeilPalette.Moon)
            }
            if (collections.isEmpty()) item(key = "shelves:empty") {
                Text("Open a book’s details and choose Edit details to add it to a collection.",
                    style = MaterialTheme.typography.bodyMedium, color = VeilPalette.Mist)
            }
            items(collections, key = { "collection:$it" }) { label ->
                ShelfEntry(label, books.filter { book -> book.allCollections.any { it.equals(label, true) } }) {
                    onCollection(label)
                }
            }
        }
    }
}

@Composable
private fun ShelfEntry(label: String, books: List<Book>, onClick: () -> Unit) {
    GrayfogPanel(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open $label", onClick = onClick),
        contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val cover = books.firstOrNull()
            BookCover(cover?.title ?: label, Modifier.width(46.dp).height(66.dp), imagePath = cover?.coverCachePath)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleLarge, color = VeilPalette.Moon)
                Text("${books.size} ${if (books.size == 1) "book" else "books"}",
                    style = MaterialTheme.typography.bodySmall, color = VeilPalette.Mist)
            }
            Text("›", color = VeilPalette.OldGold, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
