package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.VeilPalette

@Composable
fun ArchiveScreen(books: List<Book>, highlights: List<Highlight>, onClose: () -> Unit,
    onOpenPassage: (Book, String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val titles = books.associateBy { it.id }
    val matches = highlights.filter {
        query.isBlank() || listOf(it.quote, it.note, titles[it.bookId]?.title.orEmpty())
            .any { text -> text.contains(query.trim(), true) }
    }
    BackHandler { onClose() }
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(VeilPalette.VeilBlack, VeilPalette.Obsidian, VeilPalette.GrayfogBlue.copy(alpha = .58f))))
            .statusBarsPadding().navigationBarsPadding().padding(20.dp)
    ) {
        TextButton(onClick = onClose) { Text("‹ Back") }
        Text("HIDDEN ARCHIVE", style = MaterialTheme.typography.labelMedium, color = VeilPalette.OldGold)
        Text("Collected Fragments", style = MaterialTheme.typography.headlineMedium, color = VeilPalette.Moon)
        Text("${highlights.size} passages across your library", style = MaterialTheme.typography.bodyMedium, color = VeilPalette.Mist)
        VeilOrnamentDivider()
        OutlinedTextField(query, { query = it }, label = { Text("Search books, quotes and notes") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), singleLine = true)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (matches.isEmpty()) item { Text(if (highlights.isEmpty()) "Your highlighted passages will gather here as you read." else "No matching passages.") }
            items(matches, key = { it.id }) { passage ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VeilPalette.Obsidian.copy(alpha = .74f)),
                    border = BorderStroke(1.dp, VeilPalette.TarnishedBrass.copy(alpha = .52f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(titles[passage.bookId]?.title ?: "Unknown book", color = MaterialTheme.colorScheme.primary)
                        Text(passage.quote)
                        if (passage.note.isNotBlank()) Text(passage.note, style = MaterialTheme.typography.bodySmall)
                        titles[passage.bookId]?.let { book ->
                            TextButton(onClick = { onOpenPassage(book, passage.locatorJson) }) { Text("Read this passage") }
                        }
                    }
                }
            }
        }
    }
}
