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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.VeilPalette
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
        VeilReveal(delayMillis = 20, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.common_back))
                    }
                    Text(
                        stringResource(R.string.notebook_eyebrow),
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Brass
                    )
                }

                Text(
                    stringResource(R.string.notebook_title),
                    style = MaterialTheme.typography.headlineLarge
                )
                BrassRule(Modifier.width(88.dp), strong = true)
                Text(
                    stringResource(R.string.notebook_counts, highlights.size, bookmarks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        VeilReveal(delayMillis = 90, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.notebook_search_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.fillMaxWidth()
        )
        }

        VeilReveal(delayMillis = 150, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            FilterChip(
                selected = selectedSection == NotebookSection.HIGHLIGHTS,
                onClick = { selectedSectionName = NotebookSection.HIGHLIGHTS.name },
                label = { Text(stringResource(R.string.notebook_highlights_count, highlights.size)) },
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VeilPalette.DeepBrass.copy(alpha = 0.72f),
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedSection == NotebookSection.HIGHLIGHTS,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
                    selectedBorderColor = VeilPalette.Brass.copy(alpha = 0.70f)
                ),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
            FilterChip(
                selected = selectedSection == NotebookSection.BOOKMARKS,
                onClick = { selectedSectionName = NotebookSection.BOOKMARKS.name },
                label = { Text(stringResource(R.string.notebook_bookmarks_count, bookmarks.size)) },
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VeilPalette.DeepBrass.copy(alpha = 0.72f),
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedSection == NotebookSection.BOOKMARKS,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
                    selectedBorderColor = VeilPalette.Brass.copy(alpha = 0.70f)
                ),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
        }
        }

        BrassRule(Modifier.fillMaxWidth())

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
                                title = if (highlights.isEmpty()) stringResource(R.string.notebook_no_highlights_title) else stringResource(R.string.notebook_no_matching_highlights_title),
                                body = if (highlights.isEmpty()) {
                                    stringResource(R.string.notebook_no_highlights_body)
                                } else {
                                    stringResource(R.string.notebook_no_matching_highlights_body)
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
                                title = if (bookmarks.isEmpty()) stringResource(R.string.notebook_no_bookmarks_title) else stringResource(R.string.notebook_no_matching_bookmarks_title),
                                body = if (bookmarks.isEmpty()) {
                                    stringResource(R.string.notebook_no_bookmarks_body)
                                } else {
                                    stringResource(R.string.notebook_no_matching_bookmarks_body)
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
    }

    editingHighlightId?.let { highlightId ->
        AlertDialog(
            onDismissRequest = { editingHighlightId = null; noteDraft = "" },
            title = { Text(stringResource(R.string.notebook_note_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text(stringResource(R.string.notebook_note_field_label)) },
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
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingHighlightId = null; noteDraft = "" }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    deleteHighlightId?.let { highlightId ->
        DeleteNotebookItemDialog(
            title = stringResource(R.string.notebook_delete_highlight_title),
            body = stringResource(R.string.notebook_delete_highlight_body),
            onConfirm = { onDeleteHighlight(highlightId); deleteHighlightId = null },
            onDismiss = { deleteHighlightId = null }
        )
    }

    deleteBookmarkId?.let { bookmarkId ->
        DeleteNotebookItemDialog(
            title = stringResource(R.string.notebook_delete_bookmark_title),
            body = stringResource(R.string.notebook_delete_bookmark_body),
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
    ArchivePanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    book?.title ?: stringResource(R.string.common_unknown_book),
                    style = MaterialTheme.typography.titleMedium,
                    color = VeilPalette.Brass,
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
                            stringResource(R.string.notebook_note_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Brass
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
                    TextButton(onClick = onRead, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_read)) }
                }
                TextButton(onClick = onEditNote, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (highlight.note.isBlank()) stringResource(R.string.notebook_add_note) else stringResource(R.string.notebook_edit_note))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
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
    ArchivePanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                book?.title ?: stringResource(R.string.common_unknown_book),
                style = MaterialTheme.typography.titleMedium,
                color = VeilPalette.Brass,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (bookmark.label.isBlank()) stringResource(R.string.notebook_saved_location) else bookmark.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                if (onRead != null) {
                    TextButton(onClick = onRead, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_read)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            }
    }
}

@Composable
private fun NotebookEmptyState(title: String, body: String) {
    ArchivePanel(modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
