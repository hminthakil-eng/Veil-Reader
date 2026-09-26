package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.VeilPalette

@Composable
fun ArchiveScreen(books: List<Book>, highlights: List<Highlight>, onClose: () -> Unit,
    onOpenPassage: (Book, String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var notesOnly by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val titles = books.associateBy { it.id }
    val needle = query.trim()
    val matches = highlights.filter {
        (!notesOnly || it.note.isNotBlank()) && (needle.isBlank() ||
            listOf(it.quote, it.note, titles[it.bookId]?.title.orEmpty()).any { text -> text.contains(needle, true) })
    }
    BackHandler { onClose() }
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(VeilPalette.VeilBlack, VeilPalette.Obsidian, VeilPalette.GrayfogBlue)))
            .statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "archive:heading") {
            Column {
                TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.OldGold)) {
                    Text("‹ Back")
                }
                Text("HIDDEN ARCHIVE", style = MaterialTheme.typography.labelMedium, color = VeilPalette.OldGold)
                Text("Collected Fragments", style = MaterialTheme.typography.headlineLarge, color = VeilPalette.Moon)
                Text("${highlights.size} passages across your library", style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist)
                VeilOrnamentDivider()
            }
        }
        item(key = "archive:search") {
            OutlinedTextField(query, { query = it }, label = { Text("Search books, quotes and notes") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = VeilPalette.Moon,
                    unfocusedTextColor = VeilPalette.Moon, focusedBorderColor = VeilPalette.OldGold,
                    unfocusedBorderColor = VeilPalette.TarnishedBrass,
                    focusedLabelColor = VeilPalette.OldGold, unfocusedLabelColor = VeilPalette.Mist),
                modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item(key = "archive:filters") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false, true).forEach { notes ->
                    FilterChip(selected = notesOnly == notes, onClick = { notesOnly = notes },
                        label = { Text(if (notes) "Notes · ${highlights.count { it.note.isNotBlank() }}" else "Highlights · ${highlights.size}") },
                        colors = FilterChipDefaults.filterChipColors(labelColor = VeilPalette.Mist,
                            selectedContainerColor = VeilPalette.DeepAmethyst, selectedLabelColor = VeilPalette.Moon))
                }
            }
        }
        if (matches.isEmpty()) item(key = "archive:empty") {
            GrayfogPanel {
                Text(if (notesOnly) "No notes here yet" else "No passages here yet",
                    style = MaterialTheme.typography.titleLarge, color = VeilPalette.Moon)
                Text(if (needle.isNotEmpty()) "Try a different search or clear the filters."
                    else if (notesOnly) "Add a note to a highlighted passage while reading."
                    else "Your highlighted passages will gather here as you read.", color = VeilPalette.Mist)
                if (needle.isNotEmpty() || notesOnly) {
                    TextButton(onClick = { query = ""; notesOnly = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.OldGold)) { Text("Clear filters") }
                }
            }
        }
        items(matches, key = { "passage:${it.id}" }) { passage ->
            GrayfogPanel(Modifier.fillMaxWidth()) {
                val book = titles[passage.bookId]
                Text(book?.title ?: "Unknown book", style = MaterialTheme.typography.titleLarge, color = VeilPalette.OldGold)
                Text(passage.quote, style = MaterialTheme.typography.bodyLarge, color = VeilPalette.Moon)
                if (passage.note.isNotBlank()) {
                    ParchmentSurface(Modifier.fillMaxWidth()) {
                        Text("YOUR NOTE", style = MaterialTheme.typography.labelSmall, color = VeilPalette.InkOnPaper)
                        Text(passage.note, style = MaterialTheme.typography.bodyMedium, color = VeilPalette.InkOnPaper)
                    }
                }
                book?.let {
                    TextButton(onClick = { onOpenPassage(it, passage.locatorJson) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = VeilPalette.OldGold)) { Text("Read this passage") }
                }
            }
        }
    }
}
