package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.veilreader.app.ui.theme.withVeilContentScript
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.search.isSearchable

internal enum class ReaderNotebookTab(val labelRes: Int) {
    CONTENTS(R.string.reader_notebook_tab_contents),
    BOOKMARKS(R.string.reader_notebook_tab_bookmarks),
    NOTES(R.string.reader_notebook_tab_notes),
    SEARCH(R.string.reader_notebook_tab_search)
}


private data class ReaderNotebookAction(
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun ReaderNotebookActionRail(
    actions: List<ReaderNotebookAction>
) {
    if (actions.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = actions.size
        )

        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                actions.forEach { action ->
                    TextButton(
                        onClick = action.onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(action.label)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                actions.forEach { action ->
                    TextButton(
                        onClick = action.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(action.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderNotebookTabButton(
    tab: ReaderNotebookTab, selected: Boolean, count: Int?,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val formatInteger = rememberVeilIntegerFormatter()
    Surface(
        modifier = modifier.heightIn(min = 48.dp).selectable(selected, role = Role.Tab, onClick = onClick),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) colors.surfaceVariant else colors.surface,
        border = if (selected) BorderStroke(1.dp, colors.primary) else null,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface)
            count?.let { Text(formatInteger(it), style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant) }
            if (selected) Box(Modifier.padding(top = 4.dp).width(24.dp).height(2.dp).background(colors.primary))
        }
    }
}

/** Keep the chapter viewport available: a dropdown in short windows, wrapping tabs otherwise. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderNotebookTabs(
    tabs: List<ReaderNotebookTab>, tab: ReaderNotebookTab, counts: Map<ReaderNotebookTab, Int>,
    shortWindow: Boolean, onTab: (ReaderNotebookTab) -> Unit
) {
    if (shortWindow) {
        var expanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(tab.labelRes), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                ReaderToolIcon(ReaderToolSymbol.EXPAND)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                tabs.forEach { item -> DropdownMenuItem(
                    text = { Text(stringResource(item.labelRes)) },
                    onClick = { expanded = false; onTab(item) },
                    trailingIcon = { RadioButton(selected = tab == item, onClick = null) },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { selected = tab == item }
                ) }
            }
        }
    } else BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        val columns = if (shouldStackDenseChoices(maxWidth.value.toInt(), LocalDensity.current.fontScale, tabs.size)) 2 else tabs.size
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { item -> ReaderNotebookTabButton(item, tab == item, counts[item],
                Modifier.weight(1f), onClick = { onTab(item) }) }
        }
    }
}

/** Reading tools stay in a dismissible native sheet, away from the publication. */
@OptIn(ExperimentalMaterial3Api::class)
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
    onSearchResult: (Locator) -> Unit,
    onSaveNote: suspend (String, String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 720.dp, containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface, tonalElevation = 0.dp) {
        ReaderNotebookPanel(opened, readerSessionInstanceId, currentHref, highlights, bookmarks,
            passageVisits, onDismiss, onGo, onChapter, onSearchResult, onSaveNote,
            onDeleteHighlight, onDeleteBookmark, Modifier.fillMaxWidth().fillMaxHeight(.9f))
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
internal fun ReaderNotebookPanel(
    opened: OpenedPublication,
    readerSessionInstanceId: String,
    currentHref: String? = null,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    passageVisits: List<PassageVisit>,
    onDismiss: () -> Unit,
    onGo: (String) -> Unit,
    onChapter: (Link) -> Unit,
    onSearchResult: (Locator) -> Unit,
    onSaveNote: suspend (String, String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCurrentRequest by remember { mutableIntStateOf(0) }
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var noteSaveJob by remember(readerSessionInstanceId) { mutableStateOf<Job?>(null) }
    DisposableEffect(readerSessionInstanceId) {
        onDispose {
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
        mutableStateOf(ReaderNotebookTab.CONTENTS.name)
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
    var chapterQuery by rememberSaveable(opened.book.id, readerSessionInstanceId) { mutableStateOf("") }

    val chapters = remember(opened.book.id, readerSessionInstanceId) {
        fun flatten(links: List<Link>, depth: Int): List<Pair<Link, Int>> =
            links.flatMap { listOf(it to depth) + flatten(it.children, depth + 1) }
        flatten(opened.publication.tableOfContents.ifEmpty { opened.publication.readingOrder }, 0)
    }
    val matchingHighlights = highlights.filter {
        query.isBlank() || it.quote.contains(query.trim(), true) || it.note.contains(query.trim(), true)
    }

    val visibleChapters = chapters.filter { (link, _) ->
        chapterQuery.isBlank() || link.title.orEmpty().contains(chapterQuery.trim(), ignoreCase = true)
    }
    val fullCurrentIndex = chapters.indexOfFirst { (link, _) ->
        isCurrentReaderSection(link.href.toString(), currentHref)
    }
    val currentIndex = visibleChapters.indexOfFirst { (link, _) ->
        isCurrentReaderSection(link.href.toString(), currentHref)
    }.coerceAtLeast(0)

    BoxWithConstraints(modifier.fillMaxSize().background(colors.surface)) {
        val shortWindow = maxHeight < 420.dp ||
            (LocalDensity.current.fontScale >= 1.75f && maxHeight < 600.dp)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            ReaderToolsHeader(stringResource(R.string.reader_notebook_title), onDismiss)
            if (!shortWindow) Text(opened.book.title.ifBlank { stringResource(R.string.common_untitled_book) },
                style = MaterialTheme.typography.bodyMedium.withVeilContentScript(opened.book.title),
                color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp))
            ReaderNotebookTabs(tabs, tab, mapOf(ReaderNotebookTab.CONTENTS to chapters.size,
                ReaderNotebookTab.BOOKMARKS to bookmarks.size, ReaderNotebookTab.NOTES to highlights.size),
                shortWindow, onTab = { tabName = it.name })
            if (tab == ReaderNotebookTab.SEARCH) {
                ReaderBookSearchPanel(opened.publication, onSearchResult, Modifier.weight(1f))
            } else key(tab) {
                val listState = rememberLazyListState()
                // Recenter when the current resource arrives; fragment updates retain the viewport.
                // Filters always start with the first match.
                LaunchedEffect(tab, chapterQuery, currentIndex, showCurrentRequest) {
                    if (tab == ReaderNotebookTab.CONTENTS) {
                        listState.scrollToItem(if (chapterQuery.isBlank()) currentIndex else 0)
                    }
                }
                if (tab == ReaderNotebookTab.CONTENTS) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(chapterQuery, onValueChange = { chapterQuery = it },
                            label = { Text(stringResource(R.string.reader_notebook_search_chapters)) },
                            singleLine = true, modifier = Modifier.weight(1f))
                        if (fullCurrentIndex >= 0) IconButton(onClick = {
                            chapterQuery = ""
                            showCurrentRequest++
                        }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                            ReaderToolIcon(ReaderToolSymbol.CURRENT, stringResource(R.string.reader_notebook_show_current))
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f), state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)) {
                    if (tab == ReaderNotebookTab.NOTES) item {
                        OutlinedTextField(query, onValueChange = { query = it },
                            label = { Text(stringResource(R.string.reader_notebook_search_notes)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                when (tab) {
                    ReaderNotebookTab.CONTENTS -> {
                        if (visibleChapters.isEmpty()) item {
                            Text(stringResource(if (chapterQuery.isBlank()) R.string.reader_notebook_no_chapters
                                else R.string.reader_notebook_no_matching_chapters), color = colors.onSurfaceVariant)
                        }
                        items(visibleChapters) { (link, depth) ->
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
                                    colors.surfaceVariant
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                                border = if (current) {
                                    BorderStroke(
                                        1.dp,
                                        colors.outline
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
                                        .semantics { selected = current }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            link.title?.takeIf { it.isNotBlank() } ?: stringResource(
                                                R.string.reader_notebook_untitled_section
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.bodyLarge.withVeilContentScript(link.title.orEmpty()),
                                            color = if (current) {
                                                colors.onSurface
                                            } else {
                                                LocalContentColor.current
                                            }
                                        )
                                        if (current) {
                                            Text(
                                                stringResource(
                                                    R.string.reader_notebook_current_section
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colors.primary
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
                                modifier = Modifier.fillMaxWidth().veilLedgerRule(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = colors.surface,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.reader_notebook_saved_place),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary
                                    )
                                    val bookmarkLabel = bookmark.label.ifBlank {
                                        stringResource(R.string.reader_notebook_saved_place)
                                    }
                                    Text(
                                        bookmarkLabel,
                                        style = MaterialTheme.typography.titleSmall.withVeilContentScript(bookmarkLabel),
                                        color = colors.onSurface
                                    )
                                    ReaderNotebookActionRail(
                                        actions = listOf(
                                            ReaderNotebookAction(
                                                label = stringResource(R.string.reader_notebook_return),
                                                onClick = { onGo(bookmark.locatorJson) }
                                            ),
                                            ReaderNotebookAction(
                                                label = stringResource(R.string.reader_notebook_remove),
                                                onClick = { onDeleteBookmark(bookmark.id) }
                                            )
                                        )
                                    )
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
                                modifier = Modifier.fillMaxWidth().veilLedgerRule(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = colors.surface,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        localizedHighlightAgeLabel(marginMemory),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary
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
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.secondary
                                        )
                                    } else if (marginMemory.bookActivityAfterMark) {
                                        Text(
                                            stringResource(R.string.reader_notebook_activity_after_mark),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    MemoryArtifactContent(
                                        highlight = highlight,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = colors
                                    )

                                    ReaderNotebookActionRail(
                                        actions = listOf(
                                            ReaderNotebookAction(
                                                label = stringResource(R.string.archive_return_to_passage),
                                                onClick = { onGo(highlight.locatorJson) }
                                            ),
                                            ReaderNotebookAction(
                                                label = stringResource(
                                                    if (highlight.note.isBlank()) R.string.reader_notebook_annotate
                                                    else R.string.reader_notebook_edit_annotation
                                                ),
                                                onClick = {
                                                    editingId = highlight.id
                                                    note = highlight.note
                                                    noteSaveErrorRes = null
                                                }
                                            ),
                                            ReaderNotebookAction(
                                                label = stringResource(R.string.common_delete),
                                                onClick = { deletingId = highlight.id }
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    ReaderNotebookTab.SEARCH -> Unit
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
                        .widthIn(max = 560.dp)
                        .fillMaxWidth().fillMaxHeight(.98f),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surface,
                    border = BorderStroke(
                        1.dp,
                        colors.outlineVariant
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    ReaderNoteEditorContent(highlight, note,
                        onNoteChange = { note = it }, saving = savingNote, errorRes = noteSaveErrorRes,
                        onCancel = { editingId = null; noteSaveErrorRes = null },
                        onSave = {
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
                        modifier = Modifier.fillMaxSize()
                    )
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
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surface,
                    border = BorderStroke(
                        1.dp,
                        colors.outlineVariant
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.reader_notebook_eyebrow),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary
                        )
                        Text(
                            stringResource(R.string.reader_notebook_delete_highlight),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface
                        )
                        highlight.quote.trim().takeIf { it.isNotBlank() }?.let { quote ->
                            Text(
                                "“$quote”",
                                style = MaterialTheme.typography.bodyMedium.withVeilContentScript(quote),
                                color = colors.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        } ?: Text(
                            stringResource(R.string.reader_notebook_note_only),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary
                        )
                        Text(
                            stringResource(
                                R.string.reader_notebook_delete_highlight_body
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VeilAdaptiveDialogActions(
                            spacing = 8.dp,
                            first = { actionModifier ->
                                OutlinedButton(
                                    onClick = { deletingId = null },
                                    modifier = actionModifier.heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.reader_notebook_keep
                                        )
                                    )
                                }
                            },
                            second = { actionModifier ->
                                Button(
                                    onClick = {
                                        onDeleteHighlight(highlight.id)
                                        deletingId = null
                                    },
                                    modifier = actionModifier.heightIn(min = 48.dp),
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
                        )
                    }
                }
            }
        }
    }
}

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
