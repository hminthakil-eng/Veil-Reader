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

private enum class NotebookSection { NOTES, HIGHLIGHTS, BOOKMARKS }

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

    val matchingNotes = remember(matchingHighlights) {
        matchingHighlights.filter { it.note.isNotBlank() }
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("← Back")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "PRIVATE · LOCAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "HIDDEN ARCHIVE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.7.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    "Fragments worth keeping",
                    style = MaterialTheme.typography.headlineLarge
                )
                BrassRule(Modifier.width(92.dp), strong = true)
                Text(
                    "${highlights.count { it.note.isNotBlank() }} notes · ${highlights.size} highlights · ${bookmarks.size} bookmarks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        VeilReveal(delayMillis = 90, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search notes, quotes, and marks…") },
            singleLine = true,
            shape = MaterialTheme.shapes.extraSmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VeilPalette.Brass.copy(alpha = 0.82f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.40f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        }

        VeilReveal(delayMillis = 150, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ArchiveSectionTab(
                    label = "Notes",
                    count = highlights.count { it.note.isNotBlank() },
                    selected = selectedSection == NotebookSection.NOTES,
                    modifier = Modifier.weight(1f)
                ) { selectedSectionName = NotebookSection.NOTES.name }

                ArchiveSectionTab(
                    label = "Highlights",
                    count = highlights.size,
                    selected = selectedSection == NotebookSection.HIGHLIGHTS,
                    modifier = Modifier.weight(1f)
                ) { selectedSectionName = NotebookSection.HIGHLIGHTS.name }

                ArchiveSectionTab(
                    label = "Bookmarks",
                    count = bookmarks.size,
                    selected = selectedSection == NotebookSection.BOOKMARKS,
                    modifier = Modifier.weight(1f)
                ) { selectedSectionName = NotebookSection.BOOKMARKS.name }
            }
        }

        BrassRule(Modifier.fillMaxWidth())

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
            contentPadding = PaddingValues(bottom = VeilSpacing.xl)
        ) {
            when (selectedSection) {
                NotebookSection.NOTES -> {
                    if (matchingNotes.isEmpty()) {
                        item {
                            NotebookEmptyState(
                                title = if (highlights.none { it.note.isNotBlank() }) {
                                    "No notes yet"
                                } else {
                                    "No matching notes"
                                },
                                body = if (highlights.none { it.note.isNotBlank() }) {
                                    "Add a note to any highlighted passage and it will appear here."
                                } else {
                                    "Try a different word, title, or author."
                                }
                            )
                        }
                    }
                    items(matchingNotes, key = { "note:${it.id}" }) { highlight ->
                        val book = booksById[highlight.bookId]
                        NotebookHighlightCard(
                            highlight = highlight,
                            book = book,
                            onRead = if (book == null) null else { { onOpenPassage(book, highlight.locatorJson) } },
                            onEditNote = {
                                editingHighlightId = highlight.id
                                noteDraft = highlight.note
                            },
                            onDelete = { deleteHighlightId = highlight.id },
                            emphasizeNote = true
                        )
                    }
                }

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
            shape = MaterialTheme.shapes.small,
            containerColor = VeilPalette.Archive,
            titleContentColor = VeilPalette.Moon,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "HIDDEN ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.notebook_note_dialog_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
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
private fun ArchiveSectionTab(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilPalette.DeepBrass.copy(alpha = 0.74f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.36f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) VeilPalette.Brass.copy(alpha = 0.84f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) VeilPalette.Moon
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) VeilPalette.Brass
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun NotebookHighlightCard(
    highlight: Highlight,
    book: Book?,
    onRead: (() -> Unit)?,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    emphasizeNote: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.28f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
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
                Text(
                    if (highlight.note.isNotBlank()) "NOTE" else "HIGHLIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!emphasizeNote || highlight.note.isBlank()) {
                Text(
                    "“${highlight.quote}”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (highlight.note.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    Surface(
                        modifier = Modifier.width(2.dp).heightIn(min = 46.dp),
                        color = VeilPalette.Brass.copy(alpha = 0.76f)
                    ) {}
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "ANNOTATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Brass
                        )
                        Text(
                            highlight.note,
                            style = if (emphasizeNote) MaterialTheme.typography.bodyLarge
                            else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRead != null) {
                    TextButton(
                        onClick = onRead,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) { Text("Open passage") }
                }
                TextButton(
                    onClick = onEditNote,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(if (highlight.note.isBlank()) "Add note" else "Edit note")
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
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
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.24f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    book?.title ?: stringResource(R.string.common_unknown_book),
                    style = MaterialTheme.typography.titleMedium,
                    color = VeilPalette.Brass,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "MARK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                if (bookmark.label.isBlank()) {
                    stringResource(R.string.notebook_saved_location)
                } else {
                    bookmark.label
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRead != null) {
                    TextButton(
                        onClick = onRead,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) { Text("Return here") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookEmptyState(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Text(
                "THE ARCHIVE IS QUIET",
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        shape = MaterialTheme.shapes.small,
        containerColor = VeilPalette.Archive,
        titleContentColor = VeilPalette.Moon,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "REMOVE RECORD",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass
                )
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
