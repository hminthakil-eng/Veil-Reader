package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.VeilSpacing

private enum class NotebookSection { HIGHLIGHTS, BOOKMARKS }

@Composable
fun ArchiveScreen(
    books: List<Book>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    onClose: () -> Unit,
    onOpenPassage: (Book, String) -> Unit,
    onSaveNote: (String, String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSectionName by rememberSaveable { mutableStateOf(NotebookSection.HIGHLIGHTS.name) }
    var editingHighlightId by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var deleteHighlightId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteBookmarkId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedSection = runCatching { NotebookSection.valueOf(selectedSectionName) }
        .getOrDefault(NotebookSection.HIGHLIGHTS)
    val booksById = remember(books) { books.associateBy { it.id } }
    val cleanQuery = query.trim()

    val matchingHighlights = remember(highlights, booksById, cleanQuery) {
        highlights.filter { highlight ->
            cleanQuery.isBlank() || listOf(
                highlight.quote,
                highlight.note,
                booksById[highlight.bookId]?.title.orEmpty(),
                booksById[highlight.bookId]?.author.orEmpty()
            ).any { text -> text.contains(cleanQuery, ignoreCase = true) }
        }
    }

    val matchingBookmarks = remember(bookmarks, booksById, cleanQuery) {
        bookmarks.filter { bookmark ->
            cleanQuery.isBlank() || listOf(
                bookmark.label,
                booksById[bookmark.bookId]?.title.orEmpty(),
                booksById[bookmark.bookId]?.author.orEmpty()
            ).any { text -> text.contains(cleanQuery, ignoreCase = true) }
        }
    }

    BackHandler { onClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.md),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("‹ Back")
            }
            Text(
                "HIDDEN ARCHIVE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Notebook", style = MaterialTheme.typography.headlineLarge)
            Text(
                highlights.size.toString() + " highlights · " + bookmarks.size + " bookmarks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search books, passages and notes") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            FilterChip(
                selected = selectedSection == NotebookSection.HIGHLIGHTS,
                onClick = { selectedSectionName = NotebookSection.HIGHLIGHTS.name },
                label = { Text("Highlights " + highlights.size) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
            FilterChip(
                selected = selectedSection == NotebookSection.BOOKMARKS,
                onClick = { selectedSectionName = NotebookSection.BOOKMARKS.name },
                label = { Text("Bookmarks " + bookmarks.size) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
            contentPadding = PaddingValues(bottom = VeilSpacing.xl)
        ) {
            when (selectedSection) {
                NotebookSection.HIGHLIGHTS -> {
                    if (matchingHighlights.isEmpty()) {
                        item {
                            NotebookEmptyState(
                                title = if (highlights.isEmpty()) "No highlights yet" else "No matching highlights",
                                body = if (highlights.isEmpty()) {
                                    "Select text while reading to save a passage. Notes added to highlights will gather here too."
                                } else {
                                    "Try a book title, author, quote or note."
                                }
                            )
                        }
                    }
                    items(matchingHighlights, key = { it.id }) { highlight ->
                        val book = booksById[highlight.bookId]
                        NotebookHighlightCard(
                            highlight = highlight,
                            book = book,
                            onRead = if (book == null) null else { { onOpenPassage(book, highlight.locatorJson) } },
                            onEditNote = {
                                editingHighlightId = highlight.id
                                noteDraft = highlight.note
                            },
                            onDelete = { deleteHighlightId = highlight.id }
                        )
                    }
                }

                NotebookSection.BOOKMARKS -> {
                    if (matchingBookmarks.isEmpty()) {
                        item {
                            NotebookEmptyState(
                                title = if (bookmarks.isEmpty()) "No bookmarks yet" else "No matching bookmarks",
                                body = if (bookmarks.isEmpty()) {
                                    "Use Bookmark in the Reader chrome to keep important locations one gesture away."
                                } else {
                                    "Try searching by book title, author or bookmark label."
                                }
                            )
                        }
                    }
                    items(matchingBookmarks, key = { it.id }) { bookmark ->
                        val book = booksById[bookmark.bookId]
                        NotebookBookmarkCard(
                            bookmark = bookmark,
                            book = book,
                            onRead = if (book == null) null else { { onOpenPassage(book, bookmark.locatorJson) } },
                            onDelete = { deleteBookmarkId = bookmark.id }
                        )
                    }
                }
            }
        }
    }

    editingHighlightId?.let { highlightId ->
        AlertDialog(
            onDismissRequest = { editingHighlightId = null; noteDraft = "" },
            title = { Text("Note on this passage") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text("Your note") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveNote(highlightId, noteDraft)
                    editingHighlightId = null
                    noteDraft = ""
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingHighlightId = null; noteDraft = "" }) { Text("Cancel") }
            }
        )
    }

    deleteHighlightId?.let { highlightId ->
        DeleteNotebookItemDialog(
            title = "Delete highlight?",
            body = "This removes the saved passage and its note from your local library.",
            onConfirm = { onDeleteHighlight(highlightId); deleteHighlightId = null },
            onDismiss = { deleteHighlightId = null }
        )
    }

    deleteBookmarkId?.let { bookmarkId ->
        DeleteNotebookItemDialog(
            title = "Delete bookmark?",
            body = "This removes the saved location from your local library.",
            onConfirm = { onDeleteBookmark(bookmarkId); deleteBookmarkId = null },
            onDismiss = { deleteBookmarkId = null }
        )
    }
}

@Composable
private fun NotebookHighlightCard(
    highlight: Highlight,
    book: Book?,
    onRead: (() -> Unit)?,
    onEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    book?.title ?: "Unknown book",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                book?.author?.takeIf { it.isNotBlank() }?.let { author ->
                    Text(
                        author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(highlight.quote, style = MaterialTheme.typography.bodyLarge)
            if (highlight.note.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                ) {
                    Column(
                        modifier = Modifier.padding(VeilSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "NOTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(highlight.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                if (onRead != null) {
                    TextButton(onClick = onRead, modifier = Modifier.heightIn(min = 48.dp)) { Text("Read") }
                }
                TextButton(onClick = onEditNote, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (highlight.note.isBlank()) "Add note" else "Edit note")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun NotebookBookmarkCard(
    bookmark: Bookmark,
    book: Book?,
    onRead: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.44f))
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Text(
                book?.title ?: "Unknown book",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                bookmark.label.ifBlank { "Saved location" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                if (onRead != null) {
                    TextButton(onClick = onRead, modifier = Modifier.heightIn(min = 48.dp)) { Text("Read") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun NotebookEmptyState(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeleteNotebookItemDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
