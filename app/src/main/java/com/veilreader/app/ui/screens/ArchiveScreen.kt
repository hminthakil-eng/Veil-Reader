package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.ArchiveEcho
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.EchoDepth
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.HighlightMemory
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.deriveArchiveEchoes
import com.veilreader.app.domain.deriveHighlightMemory
import com.veilreader.app.domain.deriveReadingTimeCapsules
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.veilContentTextStyle

private enum class NotebookSection { NOTES, HIGHLIGHTS, BOOKMARKS, ECHOES, CAPSULES }

@Composable
fun ArchiveScreen(
    books: List<Book>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    readingSessions: List<ReadingSessionSnapshot>,
    readingCycles: List<ReadingCycleRecord>,
    passageVisits: List<PassageVisit>,
    onClose: () -> Unit,
    onOpenPassage: (Book, String) -> Unit,
    onSaveNote: (String, String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSectionName by rememberSaveable { mutableStateOf(NotebookSection.NOTES.name) }
    var editingHighlightId by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var deleteHighlightId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteBookmarkId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCapsuleSealCode by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedSection = runCatching { NotebookSection.valueOf(selectedSectionName) }
        .getOrDefault(NotebookSection.NOTES)
    val booksById = remember(books) { books.associateBy { it.id } }
    val archiveNow = remember { System.currentTimeMillis() }
    val echoes = remember(highlights, booksById, archiveNow, passageVisits) {
        deriveArchiveEchoes(
            highlights = highlights,
            booksById = booksById,
            nowEpochMs = archiveNow,
            passageVisits = passageVisits
        )
    }
    val capsules = remember(books, readingSessions, highlights, bookmarks, readingCycles) {
        deriveReadingTimeCapsules(
            books = books,
            sessions = readingSessions,
            highlights = highlights,
            bookmarks = bookmarks,
            sealedCycles = readingCycles
        )
    }
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

    val matchingEchoes = remember(matchingHighlights, booksById, archiveNow, passageVisits) {
        deriveArchiveEchoes(
            highlights = matchingHighlights,
            booksById = booksById,
            nowEpochMs = archiveNow,
            passageVisits = passageVisits
        )
    }

    val matchingCapsules = remember(capsules, cleanQuery) {
        if (cleanQuery.isBlank()) capsules
        else capsules.filter { capsule ->
            listOf(capsule.book.title, capsule.book.author, capsule.sealCode)
                .any { it.contains(cleanQuery, ignoreCase = true) }
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = highlights.size * 17 + bookmarks.size * 7 + capsules.size,
                intensity = 0.74f
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
        VeilReveal(delayMillis = 20, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(VeilBackLabel(stringResource(R.string.library_archive_eyebrow)))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.archive_private_local_offline),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.85.sp),
                        color = VeilPalette.Mist.copy(alpha = 0.70f)
                    )
                }

                Text(
                    stringResource(R.string.notebook_eyebrow),
                    style = MaterialTheme.typography.headlineLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(R.string.archive_fragments_worth_keeping),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.72.sp),
                    color = VeilPalette.Brass
                )
                BrassRule(Modifier.width(92.dp), strong = true)

                ArchiveRegister(
                    notes = highlights.count { it.note.isNotBlank() },
                    highlights = highlights.size,
                    bookmarks = bookmarks.size,
                    echoes = echoes.size,
                    capsules = capsules.size
                )
            }
        }

        VeilReveal(delayMillis = 90, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.archive_search_placeholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraSmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VeilPalette.Brass.copy(alpha = 0.82f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f),
                focusedContainerColor = VeilPalette.Archive.copy(alpha = 0.70f),
                unfocusedContainerColor = VeilPalette.Ink.copy(alpha = 0.34f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        )
        }

        VeilReveal(delayMillis = 150, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ArchiveSectionTab(
                    label = stringResource(R.string.archive_tab_notes),
                    count = highlights.count { it.note.isNotBlank() },
                    selected = selectedSection == NotebookSection.NOTES,
                    modifier = Modifier.widthIn(min = 92.dp)
                ) { selectedSectionName = NotebookSection.NOTES.name }

                ArchiveSectionTab(
                    label = stringResource(R.string.archive_tab_passages),
                    count = highlights.size,
                    selected = selectedSection == NotebookSection.HIGHLIGHTS,
                    modifier = Modifier.widthIn(min = 92.dp)
                ) { selectedSectionName = NotebookSection.HIGHLIGHTS.name }

                ArchiveSectionTab(
                    label = stringResource(R.string.archive_tab_marks),
                    count = bookmarks.size,
                    selected = selectedSection == NotebookSection.BOOKMARKS,
                    modifier = Modifier.widthIn(min = 92.dp)
                ) { selectedSectionName = NotebookSection.BOOKMARKS.name }

                ArchiveSectionTab(
                    label = stringResource(R.string.archive_tab_echoes),
                    count = echoes.size,
                    selected = selectedSection == NotebookSection.ECHOES,
                    modifier = Modifier.widthIn(min = 92.dp)
                ) { selectedSectionName = NotebookSection.ECHOES.name }

                ArchiveSectionTab(
                    label = stringResource(R.string.archive_tab_capsules),
                    count = capsules.size,
                    selected = selectedSection == NotebookSection.CAPSULES,
                    modifier = Modifier.widthIn(min = 104.dp)
                ) { selectedSectionName = NotebookSection.CAPSULES.name }
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
                                    stringResource(R.string.archive_no_notes_title)
                                } else {
                                    stringResource(R.string.archive_no_matching_notes_title)
                                },
                                body = if (highlights.none { it.note.isNotBlank() }) {
                                    stringResource(R.string.archive_no_notes_body)
                                } else {
                                    stringResource(R.string.archive_search_try_again)
                                }
                            )
                        }
                    }
                    itemsIndexed(
                        matchingNotes,
                        key = { _, item -> "note:${item.id}" }
                    ) { index, highlight ->
                        val book = booksById[highlight.bookId]
                        val memory = deriveHighlightMemory(
                            highlight = highlight,
                            book = book,
                            nowEpochMs = archiveNow,
                            passageVisits = passageVisits
                        )
                        NotebookHighlightCard(
                            highlight = highlight,
                            book = book,
                            memory = memory,
                            onRead = if (book == null) null else { { onOpenPassage(book, highlight.locatorJson) } },
                            onEditNote = {
                                editingHighlightId = highlight.id
                                noteDraft = highlight.note
                            },
                            onDelete = { deleteHighlightId = highlight.id },
                            emphasizeNote = true,
                            recordNumber = index + 1
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
                    itemsIndexed(
                        matchingHighlights,
                        key = { _, item -> item.id }
                    ) { index, highlight ->
                        val book = booksById[highlight.bookId]
                        val memory = deriveHighlightMemory(
                            highlight = highlight,
                            book = book,
                            nowEpochMs = archiveNow,
                            passageVisits = passageVisits
                        )
                        NotebookHighlightCard(
                            highlight = highlight,
                            book = book,
                            memory = memory,
                            onRead = if (book == null) null else { { onOpenPassage(book, highlight.locatorJson) } },
                            onEditNote = {
                                editingHighlightId = highlight.id
                                noteDraft = highlight.note
                            },
                            onDelete = { deleteHighlightId = highlight.id },
                            recordNumber = index + 1
                        )
                    }
                }

                NotebookSection.ECHOES -> {
                    if (matchingEchoes.isEmpty()) {
                        item {
                            NotebookEmptyState(
                                title = if (echoes.isEmpty()) stringResource(R.string.archive_no_echoes_title) else stringResource(R.string.archive_no_matching_echoes_title),
                                body = if (echoes.isEmpty()) {
                                    stringResource(R.string.archive_no_echoes_body)
                                } else {
                                    "Try a different word, title, or author."
                                }
                            )
                        }
                    }
                    itemsIndexed(
                        matchingEchoes,
                        key = { _, echo -> "echo:${echo.highlight.id}" }
                    ) { index, echo ->
                        NotebookHighlightCard(
                            highlight = echo.highlight,
                            book = echo.book,
                            memory = deriveHighlightMemory(
                                highlight = echo.highlight,
                                book = echo.book,
                                nowEpochMs = archiveNow,
                                passageVisits = passageVisits
                            ),
                            onRead = { onOpenPassage(echo.book, echo.highlight.locatorJson) },
                            onEditNote = {
                                editingHighlightId = echo.highlight.id
                                noteDraft = echo.highlight.note
                            },
                            onDelete = { deleteHighlightId = echo.highlight.id },
                            recordNumber = index + 1,
                            echoMode = true
                        )
                    }
                }

                NotebookSection.CAPSULES -> {
                    if (matchingCapsules.isEmpty()) {
                        item {
                            NotebookEmptyState(
                                title = if (capsules.isEmpty()) {
                                    stringResource(R.string.archive_no_capsules_title)
                                } else {
                                    stringResource(R.string.archive_no_matching_capsules_title)
                                },
                                body = if (capsules.isEmpty()) {
                                    stringResource(R.string.archive_no_capsules_body)
                                } else {
                                    stringResource(R.string.archive_capsule_search_try_again)
                                }
                            )
                        }
                    }

                    items(
                        matchingCapsules,
                        key = { capsule -> "capsule:${capsule.sealCode}" }
                    ) { capsule ->
                        ReadingTimeCapsuleCard(
                            capsule = capsule,
                            onOpen = { selectedCapsuleSealCode = capsule.sealCode }
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
                    itemsIndexed(
                        matchingBookmarks,
                        key = { _, item -> item.id }
                    ) { index, bookmark ->
                        val book = booksById[bookmark.bookId]
                        NotebookBookmarkCard(
                            bookmark = bookmark,
                            book = book,
                            onRead = if (book == null) null else { { onOpenPassage(book, bookmark.locatorJson) } },
                            onDelete = { deleteBookmarkId = bookmark.id },
                            recordNumber = index + 1
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
                        stringResource(R.string.archive_manuscript_note),
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
                    placeholder = { Text(stringResource(R.string.archive_margin_placeholder)) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNote(highlightId, noteDraft)
                        editingHighlightId = null
                        noteDraft = ""
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A)
                    )
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingHighlightId = null; noteDraft = "" }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    selectedCapsuleSealCode
        ?.let { seal -> capsules.firstOrNull { it.sealCode == seal } }
        ?.let { capsule ->
            ReadingTimeCapsuleSheet(
                capsule = capsule,
                onDismiss = { selectedCapsuleSealCode = null }
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
private fun ArchiveRegister(
    notes: Int,
    highlights: Int,
    bookmarks: Int,
    echoes: Int,
    capsules: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(MaterialTheme.shapes.extraSmall)
            .background(VeilPalette.Archive.copy(alpha = 0.56f))
            .border(
                BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.74f)),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ArchiveRegisterStat(stringResource(R.string.archive_stat_notes), notes)
        ArchiveRegisterStat(stringResource(R.string.archive_stat_passages), highlights)
        ArchiveRegisterStat(stringResource(R.string.archive_stat_marks), bookmarks)
        ArchiveRegisterStat(stringResource(R.string.archive_stat_echoes), echoes)
        ArchiveRegisterStat(stringResource(R.string.archive_stat_sealed), capsules)
    }
}

@Composable
private fun ArchiveRegisterStat(
    label: String,
    value: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleSmall,
            color = VeilPalette.Moon
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.75.sp),
            color = VeilPalette.Brass.copy(alpha = 0.78f)
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
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilPalette.DeepBrass.copy(alpha = 0.52f)
        } else {
            VeilPalette.Ink.copy(alpha = 0.24f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) VeilPalette.Brass.copy(alpha = 0.84f)
            else VeilPalette.BorderDark.copy(alpha = 0.62f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
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
    memory: HighlightMemory,
    onRead: (() -> Unit)?,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    emphasizeNote: Boolean = false,
    recordNumber: Int,
    echoMode: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.78f)),
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
                        stringResource(R.string.archive_folio, recordNumber.toString().padStart(3, '0')),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                        color = VeilPalette.Brass
                    )
                    val displayTitle = book?.title ?: stringResource(R.string.common_unknown_book)
                    Text(
                        displayTitle,
                        style = veilContentTextStyle(MaterialTheme.typography.titleMedium, displayTitle),
                        color = VeilPalette.Moon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    book?.author?.takeIf { it.isNotBlank() }?.let { author ->
                        Text(
                            author,
                            style = veilContentTextStyle(MaterialTheme.typography.labelSmall, author),
                            color = VeilPalette.Mist.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    if (echoMode) stringResource(R.string.archive_record_echo)
                    else if (highlight.note.isNotBlank()) stringResource(R.string.archive_record_annotated)
                    else stringResource(R.string.archive_record_passage),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.75.sp),
                    color = if (echoMode) VeilPalette.Brass
                    else VeilPalette.Mist.copy(alpha = 0.64f)
                )
            }

            LivingMarginMemoryStrip(
                memory = memory,
                echoMode = echoMode
            )

            if (!emphasizeNote || highlight.note.isBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .heightIn(min = 54.dp)
                            .background(VeilPalette.Brass.copy(alpha = 0.48f))
                    )
                    Text(
                        "“${highlight.quote}”",
                        style = veilContentTextStyle(MaterialTheme.typography.bodyLarge, highlight.quote),
                        color = VeilPalette.Moon.copy(alpha = 0.90f),
                        modifier = Modifier.weight(1f)
                    )
                }
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
                            stringResource(R.string.archive_annotation),
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Brass
                        )
                        Text(
                            highlight.note,
                            style = veilContentTextStyle(
                                if (emphasizeNote) MaterialTheme.typography.bodyLarge
                                else MaterialTheme.typography.bodyMedium,
                                highlight.note
                            ),
                            color = VeilPalette.Moon
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
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.archive_return_to_passage)) }
                }
                TextButton(
                    onClick = onEditNote,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (highlight.note.isBlank()) stringResource(R.string.archive_annotate) else stringResource(R.string.archive_edit_annotation))
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.heightIn(min = 48.dp)
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
private fun LivingMarginMemoryStrip(
    memory: HighlightMemory,
    echoMode: Boolean
) {
    val ageLabel = highlightAgeLabel(memory)
    val echoLabel = highlightEchoLabel(memory)
    val lastViewedLabel = highlightLastViewedLabel(memory)
    val primaryLabel = if (echoMode) echoLabel ?: ageLabel else ageLabel
    val revisitLabel = if (memory.revisitCount > 0) {
        pluralStringResource(
            R.plurals.archive_revisited_times,
            memory.revisitCount,
            memory.revisitCount
        )
    } else {
        null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            primaryLabel,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
            color = if (echoMode) VeilPalette.Brass
            else VeilPalette.Mist.copy(alpha = 0.64f)
        )
        if (revisitLabel != null) {
            Text(
                listOfNotNull(revisitLabel, lastViewedLabel).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Spirit.copy(alpha = 0.72f)
            )
        } else if (memory.bookActivityAfterMark) {
            Text(
                stringResource(R.string.archive_volume_activity_continued),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Mist.copy(alpha = 0.46f)
            )
        }
    }
}

@Composable
private fun NotebookBookmarkCard(
    bookmark: Bookmark,
    book: Book?,
    onRead: (() -> Unit)?,
    onDelete: () -> Unit,
    recordNumber: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.54f),
        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.76f)),
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
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        stringResource(R.string.archive_folio, recordNumber.toString().padStart(3, '0')),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                        color = VeilPalette.Brass
                    )
                    val displayTitle = book?.title ?: stringResource(R.string.common_unknown_book)
                    Text(
                        displayTitle,
                        style = veilContentTextStyle(MaterialTheme.typography.titleMedium, displayTitle),
                        color = VeilPalette.Moon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    stringResource(R.string.archive_record_bookmark),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.65.sp),
                    color = VeilPalette.Mist.copy(alpha = 0.64f)
                )
            }

            val displayLabel = if (bookmark.label.isBlank()) {
                stringResource(R.string.notebook_saved_location)
            } else {
                bookmark.label
            }
            Text(
                displayLabel,
                style = veilContentTextStyle(MaterialTheme.typography.bodyMedium, displayLabel),
                color = VeilPalette.Mist
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRead != null) {
                    TextButton(
                        onClick = onRead,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.archive_return_here)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.heightIn(min = 48.dp)
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
        color = VeilPalette.Ink.copy(alpha = 0.30f),
        border = BorderStroke(
            1.dp,
            VeilPalette.BorderDark.copy(alpha = 0.60f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Text(
                stringResource(R.string.archive_quiet),
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
                    stringResource(R.string.archive_remove_record),
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
