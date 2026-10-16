package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import java.text.NumberFormat

/** Opt-in presentation only. The existing shell owns navigation, opening and persisted state. */
@Composable
fun RebornThresholdScreen(
    books: List<Book>,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCastle: () -> Unit,
    modifier: Modifier = Modifier,
    highlights: List<Highlight> = emptyList(),
    onOpenPassage: (Book, String) -> Unit = { _, _ -> }
) {
    val snapshot = remember(books) { buildThresholdSnapshot(books) }
    val clue = remember(books, highlights) { deriveLatestClue(books, highlights) }
    var showWorldNote by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("reborn-threshold-scroll"),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(key = "welcome") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.reborn_brand), style = MaterialTheme.typography.labelLarge)
                        Text(
                            stringResource(R.string.reborn_welcome),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                }
                item(key = "continue") {
                    val book = snapshot.hero
                    if (book == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.reborn_empty), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.reborn_empty_detail))
                            Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.reborn_open_library))
                            }
                        }
                    } else {
                        RebornContinueCard(book, onOpenBook)
                    }
                }
                if (clue != null) {
                    item(key = "latest_clue") { RebornLatestClueCard(clue, onOpenPassage) }
                }
                if (snapshot.hero != null) {
                    item(key = "recent_heading") {
                        Column {
                            if (snapshot.recent.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.reborn_recent),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.semantics { heading() }
                                )
                            }
                            TextButton(onClick = onOpenLibrary, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.reborn_view_library))
                            }
                        }
                    }
                }
                if (snapshot.recent.isNotEmpty()) {
                    item(key = "recent_shelf") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(snapshot.recent, key = { it.id }, contentType = { "book" }) { book ->
                                OutlinedCard(
                                    onClick = { onOpenBook(book) },
                                    modifier = Modifier.width(160.dp)
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RebornCover(book, Modifier.width(80.dp).height(120.dp))
                                        Text(book.title, style = MaterialTheme.typography.titleMedium)
                                        Text(book.author, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "world") {
                    Column {
                        TextButton(onClick = { showWorldNote = !showWorldNote }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(if (showWorldNote) R.string.reborn_hide_world else R.string.reborn_show_world))
                        }
                        if (showWorldNote) {
                            Text(stringResource(R.string.reborn_world_note), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onOpenCastle, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.reborn_open_castle))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RebornLatestClueCard(clue: LatestClue, onOpenPassage: (Book, String) -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reborn_latest_clue),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text(clue.book.title, style = MaterialTheme.typography.bodyMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.reborn_saved_passage), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.reborn_clue_sealed), style = MaterialTheme.typography.bodySmall)
            val actionDescription = stringResource(R.string.reborn_trace_description, clue.book.title)
            TextButton(
                onClick = { onOpenPassage(clue.book, clue.highlight.locatorJson) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { contentDescription = actionDescription }
            ) { Text(stringResource(R.string.reborn_trace_archive)) }
        }
    }
}

@Composable
private fun RebornContinueCard(book: Book, onOpenBook: (Book) -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.reborn_continue_section), style = MaterialTheme.typography.labelLarge)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Stack at narrow widths or large text sizes; no fixed text/card heights.
                val stack = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.3f
                if (stack) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RebornCover(book, Modifier.width(96.dp).height(144.dp))
                        RebornBookDetails(book)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        RebornCover(book, Modifier.width(112.dp).height(168.dp))
                        Column(Modifier.weight(1f)) { RebornBookDetails(book) }
                    }
                }
            }
            Button(
                onClick = { onOpenBook(book) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(when {
                    book.finished -> R.string.reborn_read_again
                    book.lastOpenedAtEpochMs > 0 || book.progress > 0f -> R.string.reborn_continue
                    else -> R.string.reborn_start
                }))
            }
        }
    }
}

@Composable
private fun RebornBookDetails(book: Book) {
    val progress = book.progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(book.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(book.author, style = MaterialTheme.typography.bodyLarge)
        Text(book.currentChapter, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.reborn_progress, NumberFormat.getPercentInstance().format(progress)),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RebornCover(book: Book, modifier: Modifier) {
    // Adjacent text carries identity; avoid announcing the decorative cover twice.
    Box(modifier.clearAndSetSemantics { }) {
        BookCover(title = book.title, imagePath = book.coverCachePath, modifier = Modifier.fillMaxSize())
    }
}
