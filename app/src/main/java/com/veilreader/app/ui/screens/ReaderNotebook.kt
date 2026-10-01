package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.data.OpenedPublication
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.deriveHighlightMemory
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.usesArabicScript
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.search.isSearchable
import org.readium.r2.shared.publication.services.search.search

private enum class ReaderNotebookTab(val labelRes: Int) {
    CONTENTS(R.string.reader_notebook_tab_contents),
    BOOKMARKS(R.string.reader_notebook_tab_bookmarks),
    NOTES(R.string.reader_notebook_tab_notes),
    SEARCH(R.string.reader_notebook_tab_search)
}

@Composable
private fun ReaderNotebookTabButton(
    tab: ReaderNotebookTab,
    selected: Boolean,
    count: Int?,
    onClick: () -> Unit
) {
    val formatInteger = rememberVeilIntegerFormatter()
    val rawLabel = stringResource(tab.labelRes)
    val label = if (usesArabicScript(rawLabel)) {
        rawLabel
    } else {
        rawLabel.uppercase()
    }

    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilPalette.Archive.copy(alpha = 0.52f)
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        VeilPalette.Moon
                    } else {
                        VeilPalette.Mist.copy(alpha = 0.74f)
                    }
                )
                count?.let {
                    Text(
                        formatInteger(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            VeilPalette.Brass
                        } else {
                            VeilPalette.Mist.copy(alpha = 0.48f)
                        }
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        if (selected) {
                            VeilPalette.Brass.copy(alpha = 0.82f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
            )
        }
    }
}

/** Reading tools stay in a dismissible sheet, away from the reading surface. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun ReaderNotebook(
    opened: OpenedPublication,
    readerSessionInstanceId: String,
    currentHref: String? = null,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    passageVisits: List<PassageVisit>,
    onDismiss: () -> Unit,
    onGo: (String) -> Unit,
    onChapter: (Link) -> Unit,
    onSaveNote: suspend (String, String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchJob by remember(readerSessionInstanceId) { mutableStateOf<Job?>(null) }
    var searchSerial by remember(readerSessionInstanceId) { mutableIntStateOf(0) }
    var noteSaveJob by remember(readerSessionInstanceId) { mutableStateOf<Job?>(null) }
    DisposableEffect(readerSessionInstanceId) {
        onDispose {
            searchJob?.cancel()
            noteSaveJob?.cancel()
        }
    }
    val searchable = remember(opened.book.id, readerSessionInstanceId) { opened.publication.isSearchable }
    val tabs = remember(searchable) {
        buildList {
            add(ReaderNotebookTab.CONTENTS)
            add(ReaderNotebookTab.BOOKMARKS)
            add(ReaderNotebookTab.NOTES)
            if (searchable) add(ReaderNotebookTab.SEARCH)
        }
    }
    var tabName by rememberSaveable(opened.book.id, readerSessionInstanceId, searchable) {
        mutableStateOf(ReaderNotebookTab.NOTES.name)
    }
    val tab = ReaderNotebookTab.entries
        .firstOrNull { it.name == tabName && it in tabs }
        ?: ReaderNotebookTab.CONTENTS
    var query by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf("") }
    var editingId by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf<String?>(null) }
    val editing = editingId?.let { id -> highlights.firstOrNull { it.id == id } }
    var note by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf("") }
    var savingNote by remember(readerSessionInstanceId) { mutableStateOf(false) }
    var noteSaveErrorRes by remember(readerSessionInstanceId) { mutableStateOf<Int?>(null) }
    var deletingId by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf<String?>(null) }
    val deleting = deletingId?.let { id -> highlights.firstOrNull { it.id == id } }
    var bookSearchQuery by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf("") }
    var bookSearchResults by remember(readerSessionInstanceId) { mutableStateOf<List<Locator>>(emptyList()) }
    var bookSearchErrorRes by remember(readerSessionInstanceId) { mutableStateOf<Int?>(null) }
    var bookSearchLimited by remember(readerSessionInstanceId) { mutableStateOf(false) }
    var searchingBook by remember(readerSessionInstanceId) { mutableStateOf(false) }

    val chapters = remember(opened.book.id, readerSessionInstanceId) {
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
        searchJob?.cancel()
        val requestSerial = ++searchSerial
        searchJob = scope.launch {
            searchingBook = true
            bookSearchErrorRes = null
            bookSearchLimited = false
            bookSearchResults = emptyList()
            try {
                val iterator = opened.publication.search(term)
                if (iterator == null) {
                    if (requestSerial == searchSerial) {
                        bookSearchErrorRes = R.string.reader_notebook_search_unavailable
                    }
                    return@launch
                }
                try {
                    val found = ArrayList<Locator>(
                        READER_SEARCH_RESULT_LIMIT + 1
                    )
                    var failed = false
                    while (
                        requestSerial == searchSerial &&
                        !failed &&
                        found.size <= READER_SEARCH_RESULT_LIMIT
                    ) {
                        var pageLocators: List<Locator>? = null
                        var reachedEnd = false
                        iterator.next()
                            .onSuccess { page ->
                                if (page == null) {
                                    reachedEnd = true
                                } else {
                                    pageLocators = page.locators
                                }
                            }
                            .onFailure {
                                failed = true
                                if (requestSerial == searchSerial) {
                                    bookSearchErrorRes =
                                        R.string.reader_notebook_search_failed
                                }
                            }

                        if (failed || reachedEnd || requestSerial != searchSerial) break
                        val remaining =
                            READER_SEARCH_RESULT_LIMIT + 1 - found.size
                        found += pageLocators.orEmpty().take(remaining)
                    }
                    if (requestSerial != searchSerial) return@launch
                    bookSearchLimited =
                        found.size > READER_SEARCH_RESULT_LIMIT
                    bookSearchResults =
                        found.take(READER_SEARCH_RESULT_LIMIT)
                } finally {
                    iterator.close()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (requestSerial == searchSerial) {
                    bookSearchErrorRes = R.string.reader_notebook_search_failed
                }
            } finally {
                if (requestSerial == searchSerial) {
                    searchingBook = false
                    searchJob = null
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VeilPalette.Ink,
        contentColor = VeilPalette.Moon,
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(.85f)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    stringResource(R.string.reader_notebook_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass
                )
                BrassRule(Modifier.width(84.dp))
                Text(
                    stringResource(R.string.reader_notebook_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = VeilPalette.Moon
                )
                Text(
                    opened.book.title.ifBlank {
                        stringResource(R.string.common_untitled_book)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.74f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { item ->
                    val selected = tab == item
                    val count = when (item) {
                        ReaderNotebookTab.CONTENTS -> chapters.size
                        ReaderNotebookTab.BOOKMARKS -> bookmarks.size
                        ReaderNotebookTab.NOTES -> highlights.size
                        ReaderNotebookTab.SEARCH -> null
                    }
                    ReaderNotebookTabButton(
                        tab = item,
                        selected = selected,
                        count = count,
                        onClick = { tabName = item.name }
                    )
                }
            }

            if (tab == ReaderNotebookTab.NOTES) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.reader_notebook_search_notes)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
            }

            if (tab == ReaderNotebookTab.SEARCH) {
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bookSearchQuery,
                        onValueChange = { value ->
                            bookSearchQuery = value
                            if (searchingBook) {
                                searchSerial += 1
                                searchJob?.cancel()
                                searchJob = null
                                searchingBook = false
                                bookSearchResults = emptyList()
                                bookSearchLimited = false
                                bookSearchErrorRes = null
                            }
                        },
                        label = { Text(stringResource(R.string.reader_notebook_search_book)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = ::runBookSearch,
                        enabled = bookSearchQuery.trim().length >= 2 && !searchingBook,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text(if (searchingBook) "…" else stringResource(R.string.reader_notebook_find)) }
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                when (tab) {
                    ReaderNotebookTab.CONTENTS -> {
                        if (chapters.isEmpty()) item { Text(stringResource(R.string.reader_notebook_no_chapters)) }
                        items(chapters) { (link, depth) ->
                            val current = isCurrentReaderSection(
                                linkHref = link.href.toString(),
                                currentHref = currentHref
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = (depth.coerceAtMost(4) * 12).dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (current) {
                                    VeilPalette.Archive.copy(alpha = 0.72f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                                border = if (current) {
                                    BorderStroke(
                                        1.dp,
                                        VeilPalette.Brass.copy(alpha = 0.32f)
                                    )
                                } else {
                                    null
                                },
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                TextButton(
                                    onClick = { onChapter(link) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            link.title ?: stringResource(
                                                R.string.reader_notebook_untitled_section
                                            ),
                                            modifier = Modifier.weight(1f),
                                            color = if (current) {
                                                VeilPalette.Moon
                                            } else {
                                                LocalContentColor.current
                                            }
                                        )
                                        if (current) {
                                            Text(
                                                stringResource(
                                                    R.string.reader_notebook_current_section
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VeilPalette.Brass
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ReaderNotebookTab.BOOKMARKS -> {
                        if (bookmarks.isEmpty()) item {
                            Text(stringResource(R.string.reader_notebook_bookmarks_empty))
                        }
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = VeilPalette.Archive.copy(alpha = 0.68f),
                                border = BorderStroke(
                                    1.dp,
                                    VeilPalette.BorderDark.copy(alpha = 0.72f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.reader_notebook_saved_place),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VeilPalette.Brass
                                    )
                                    Text(
                                        bookmark.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = VeilPalette.Moon
                                    )
                                    Row(
                                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(
                                            onClick = { onGo(bookmark.locatorJson) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text(stringResource(R.string.reader_notebook_return)) }
                                        TextButton(
                                            onClick = { onDeleteBookmark(bookmark.id) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text(stringResource(R.string.reader_notebook_remove)) }
                                    }
                                }
                            }
                        }
                    }

                    ReaderNotebookTab.NOTES -> {
                        if (matchingHighlights.isEmpty()) item {
                            Text(
                                stringResource(
                                    if (query.isBlank()) R.string.reader_notebook_notes_empty
                                    else R.string.reader_notebook_no_matching_notes
                                )
                            )
                        }
                        items(matchingHighlights, key = { it.id }) { highlight ->
                            val marginMemory = remember(highlight, opened.book, passageVisits) {
                                deriveHighlightMemory(
                                    highlight = highlight,
                                    book = opened.book,
                                    passageVisits = passageVisits
                                )
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = VeilPalette.Archive.copy(alpha = 0.72f),
                                border = BorderStroke(
                                    1.dp,
                                    VeilPalette.BorderDark.copy(alpha = 0.72f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        localizedHighlightAgeLabel(marginMemory),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VeilPalette.Brass
                                    )
                                    if (marginMemory.revisitCount > 0) {
                                        Text(
                                            buildString {
                                                append(
                                                    stringResource(
                                                        if (marginMemory.revisitCount == 1) R.string.reader_notebook_revisited_one
                                                        else R.string.reader_notebook_revisited_many,
                                                        marginMemory.revisitCount
                                                    )
                                                )
                                                localizedLastViewedLabel(marginMemory)?.let {
                                                    append(" · ").append(it)
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VeilPalette.Spirit.copy(alpha = 0.72f)
                                        )
                                    } else if (marginMemory.bookActivityAfterMark) {
                                        Text(
                                            stringResource(R.string.reader_notebook_activity_after_mark),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VeilPalette.Mist.copy(alpha = 0.52f)
                                        )
                                    }
                                    highlight.quote.trim().takeIf { it.isNotBlank() }?.let { quote ->
                                        Text(
                                            "“$quote”",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = VeilPalette.Moon
                                        )
                                    } ?: Text(
                                        stringResource(R.string.reader_notebook_note_only),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VeilPalette.Brass.copy(alpha = 0.82f)
                                    )
                                    if (highlight.note.isNotBlank()) {
                                        Text(
                                            highlight.note,
                                            color = VeilPalette.Brass.copy(alpha = 0.88f)
                                        )
                                    }
                                    Row(
                                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        TextButton(
                                            onClick = { onGo(highlight.locatorJson) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text(stringResource(R.string.archive_return_to_passage)) }
                                        TextButton(
                                            onClick = {
                                                editingId = highlight.id
                                                note = highlight.note
                                                noteSaveErrorRes = null
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text(
                                                stringResource(
                                                    if (highlight.note.isBlank()) R.string.reader_notebook_annotate
                                                    else R.string.reader_notebook_edit_annotation
                                                )
                                            )
                                        }
                                        TextButton(
                                            onClick = { deletingId = highlight.id },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) { Text(stringResource(R.string.common_delete)) }
                                    }
                                }
                            }
                        }
                    }

                    ReaderNotebookTab.SEARCH -> {
                        if (bookSearchLimited) {
                            item {
                                ReaderCapabilityNotice(
                                    text = stringResource(
                                        R.string.reader_notebook_search_limited,
                                        READER_SEARCH_RESULT_LIMIT
                                    )
                                )
                            }
                        }
                        bookSearchErrorRes?.let { messageRes ->
                            item { Text(stringResource(messageRes), color = MaterialTheme.colorScheme.error) }
                        }
                        if (!searchingBook && bookSearchErrorRes == null && bookSearchQuery.isNotBlank() && bookSearchResults.isEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.reader_notebook_no_search_matches),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(bookSearchResults) { locator ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = VeilPalette.Archive.copy(alpha = 0.64f),
                                border = BorderStroke(
                                    1.dp,
                                    VeilPalette.BorderDark.copy(alpha = 0.68f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.reader_notebook_match),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VeilPalette.Brass
                                    )
                                    locator.title?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = VeilPalette.Moon
                                        )
                                    }
                                    Text(
                                        searchSnippet(locator, stringResource(R.string.reader_notebook_search_match)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VeilPalette.Mist.copy(alpha = 0.88f)
                                    )
                                    TextButton(
                                        onClick = { onGo(locator.toJSON().toString()) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) { Text(stringResource(R.string.reader_notebook_return_match)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { highlight ->
        Dialog(
            onDismissRequest = {
                if (!savingNote) {
                    editingId = null
                    noteSaveErrorRes = null
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !savingNote,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = VeilPalette.Archive,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.48f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box {
                        GrayfogOrnamentFrame(
                            modifier = Modifier.matchParentSize(),
                            strength = 0.24f
                        )
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.reader_notebook_eyebrow),
                                style = MaterialTheme.typography.labelSmall,
                                color = VeilPalette.Brass
                            )
                            Text(
                                stringResource(R.string.reader_notebook_passage_note),
                                style = MaterialTheme.typography.titleLarge,
                                color = VeilPalette.Moon
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = VeilPalette.Ink.copy(alpha = 0.44f),
                                border = BorderStroke(
                                    1.dp,
                                    VeilPalette.Brass.copy(alpha = 0.22f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                highlight.quote.trim().takeIf { it.isNotBlank() }?.let { quote ->
                                    Text(
                                        "“$quote”",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VeilPalette.Moon.copy(alpha = 0.78f),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } ?: Text(
                                    stringResource(R.string.reader_notebook_note_only),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VeilPalette.Brass.copy(alpha = 0.82f)
                                )
                            }
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.reader_notebook_your_thoughts
                                        )
                                    )
                                },
                                minLines = 4,
                                maxLines = 8,
                                enabled = !savingNote,
                                modifier = Modifier.fillMaxWidth()
                            )
                            noteSaveErrorRes?.let { messageRes ->
                                Text(
                                    stringResource(messageRes),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    enabled = !savingNote,
                                    onClick = {
                                        editingId = null
                                        noteSaveErrorRes = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                                Button(
                                    enabled = !savingNote,
                                    onClick = {
                                        noteSaveJob = scope.launch {
                                            savingNote = true
                                            noteSaveErrorRes = null
                                            try {
                                                onSaveNote(highlight.id, note)
                                                editingId = null
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (error: Exception) {
                                                noteSaveErrorRes =
                                                    R.string.reader_notebook_note_save_failed
                                            } finally {
                                                savingNote = false
                                                noteSaveJob = null
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VeilPalette.Brass,
                                        contentColor = androidx.compose.ui.graphics.Color(
                                            0xFF17120A
                                        )
                                    )
                                ) {
                                    Text(
                                        stringResource(
                                            if (savingNote) {
                                                R.string.reader_notebook_saving
                                            } else {
                                                R.string.common_save
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { highlight ->
        Dialog(
            onDismissRequest = { deletingId = null },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = VeilPalette.Archive,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.40f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.reader_notebook_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Brass
                        )
                        Text(
                            stringResource(R.string.reader_notebook_delete_highlight),
                            style = MaterialTheme.typography.titleLarge,
                            color = VeilPalette.Moon
                        )
                        highlight.quote.trim().takeIf { it.isNotBlank() }?.let { quote ->
                            Text(
                                "“$quote”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VeilPalette.Moon.copy(alpha = 0.72f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        } ?: Text(
                            stringResource(R.string.reader_notebook_note_only),
                            style = MaterialTheme.typography.labelMedium,
                            color = VeilPalette.Brass.copy(alpha = 0.82f)
                        )
                        Text(
                            stringResource(
                                R.string.reader_notebook_delete_highlight_body
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { deletingId = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    stringResource(
                                        R.string.reader_notebook_keep
                                    )
                                )
                            }
                            Button(
                                onClick = {
                                    onDeleteHighlight(highlight.id)
                                    deletingId = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.errorContainer,
                                    contentColor =
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text(stringResource(R.string.common_delete))
                            }
                        }
                    }
                }
            }
        }
    }}

@Composable
private fun localizedHighlightAgeLabel(memory: com.veilreader.app.domain.HighlightMemory): String = when {
    !memory.ageKnown -> stringResource(R.string.reader_notebook_mark_unknown)
    memory.ageDays == 0 -> stringResource(R.string.reader_notebook_mark_today)
    memory.ageDays == 1 -> stringResource(R.string.reader_notebook_mark_yesterday)
    memory.ageDays < 60 -> stringResource(R.string.reader_notebook_mark_days, memory.ageDays)
    memory.ageDays < 730 -> stringResource(
        R.string.reader_notebook_mark_months,
        (memory.ageDays / 30).coerceAtLeast(2)
    )
    else -> stringResource(
        R.string.reader_notebook_mark_years,
        (memory.ageDays / 365).coerceAtLeast(2)
    )
}

@Composable
private fun localizedLastViewedLabel(memory: com.veilreader.app.domain.HighlightMemory): String? {
    val days = memory.lastViewedDaysAgo ?: return null
    return when {
        days == 0 -> stringResource(R.string.reader_notebook_last_viewed_today)
        days == 1 -> stringResource(R.string.reader_notebook_last_viewed_yesterday)
        days < 60 -> stringResource(R.string.reader_notebook_last_viewed_days, days)
        days < 730 -> stringResource(
            R.string.reader_notebook_last_viewed_months,
            (days / 30).coerceAtLeast(2)
        )
        else -> stringResource(
            R.string.reader_notebook_last_viewed_years,
            (days / 365).coerceAtLeast(2)
        )
    }
}

private const val READER_SEARCH_RESULT_LIMIT = 250

internal fun isCurrentReaderSection(
    linkHref: String,
    currentHref: String?
): Boolean {
    val current = currentHref
        ?.trim()
        ?.substringBefore('#')
        ?.takeIf { it.isNotEmpty() }
        ?: return false
    val link = linkHref
        .trim()
        .substringBefore('#')
        .takeIf { it.isNotEmpty() }
        ?: return false
    return link == current
}

private fun searchSnippet(locator: Locator, fallback: String): String {
    val before = locator.text.before.orEmpty().replace(Regex("\\s+"), " ").trim().takeLast(100)
    val hit = locator.text.highlight.orEmpty().replace(Regex("\\s+"), " ").trim()
    val after = locator.text.after.orEmpty().replace(Regex("\\s+"), " ").trim().take(100)
    return buildString {
        if (before.isNotBlank()) append("…").append(before).append(' ')
        if (hit.isNotBlank()) append(hit)
        if (after.isNotBlank()) append(' ').append(after).append("…")
    }.ifBlank { locator.title ?: fallback }
}
