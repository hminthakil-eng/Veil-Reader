package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.withVeilContentScript

internal data class LibrarySeriesChoice(
    val key: String,
    val name: String,
    val bookCount: Int,
    val searchText: String
)

/** Search equivalence is deliberately broader than persisted filter identity. */
internal fun buildLibrarySeriesChoices(books: List<Book>): List<LibrarySeriesChoice> =
    groupLibraryBooksByLabel(books.mapNotNull { book -> book.seriesName?.let { it to book } })
        .sortedWith(compareBy<LibraryNamedBookGroup> { libraryLabelKey(it.name) }.thenBy { it.name })
        .map { group ->
            LibrarySeriesChoice(
                key = libraryLabelKey(group.name),
                name = group.name,
                bookCount = group.books.size,
                searchText = normalizeLibrarySearchText(group.name)
            )
        }

internal fun filterLibrarySeriesChoices(
    choices: List<LibrarySeriesChoice>,
    query: String
): List<LibrarySeriesChoice> {
    val normalized = normalizeLibrarySearchText(query)
    return if (normalized.isEmpty()) choices else choices.filter { normalized in it.searchText }
}

@Composable
internal fun LibrarySeriesPickerDialog(
    choices: List<LibrarySeriesChoice>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(choices, query) { filterLibrarySeriesChoices(choices, query) }
    val selectedKey = libraryLabelKey(selected)
    val parentDensity = LocalDensity.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        // Preserve caller font conversion across the dialog's separate Compose owner.
        CompositionLocalProvider(LocalDensity provides parentDensity) {
            // A Dialog owns a distinct Compose focus root; use its locals for the IME action.
            val focus = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            Box(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .padding(VeilSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = modifier.widthIn(max = 620.dp).fillMaxWidth()
                        .heightIn(max = 720.dp).fillMaxHeight(0.9f),
                    shape = MaterialTheme.shapes.medium,
                    color = VeilPalette.Archive,
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.5f)),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        Modifier.padding(VeilSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        val title = stringResource(R.string.library_group_series)
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge.withVeilContentScript(title),
                            modifier = Modifier.semantics { heading() }
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.library_series_search)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focus.clearFocus()
                                keyboard?.hide()
                            }),
                            trailingIcon = if (query.isNotEmpty()) {
                                {
                                    val label = stringResource(R.string.library_series_search_clear)
                                    IconButton(
                                        onClick = { query = "" },
                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                            .semantics { contentDescription = label }
                                    ) { Text("×", modifier = Modifier.clearAndSetSemantics {}) }
                                }
                            } else null
                        )
                        HorizontalDivider(color = VeilPalette.Brass.copy(alpha = 0.3f))
                        LazyColumn(
                            Modifier.weight(1f).fillMaxWidth().selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item(key = "series:all") {
                                SeriesChoiceRow(
                                    stringResource(R.string.library_all_series), null,
                                    selectedKey.isEmpty(), onClick = { onSelect("") }
                                )
                            }
                            items(matches, key = { "series:choice:${it.key}" }) { choice ->
                                SeriesChoiceRow(
                                    choice.name,
                                    pluralStringResource(R.plurals.library_series_books, choice.bookCount, choice.bookCount),
                                    selectedKey == choice.key,
                                    onClick = { onSelect(choice.name) }
                                )
                            }
                            if (matches.isEmpty()) {
                                item(key = "series:no-results") {
                                    Column(Modifier.padding(VeilSpacing.sm)) {
                                        Text(
                                            stringResource(R.string.library_series_no_match),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (query.isNotEmpty()) {
                                            TextButton(
                                                onClick = { query = "" },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) { Text(stringResource(R.string.library_series_search_clear)) }
                                        }
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.common_cancel)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesChoiceRow(name: String, count: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge.withVeilContentScript(name))
            if (count != null) {
                Text(count, style = MaterialTheme.typography.bodySmall.withVeilContentScript(count),
                    color = VeilPalette.Brass)
            }
        }
        RadioButton(selected = selected, onClick = null)
    }
}
