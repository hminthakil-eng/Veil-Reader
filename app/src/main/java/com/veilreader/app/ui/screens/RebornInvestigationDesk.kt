package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight

/**
 * Privacy-first, optional discovery of *previously saved* marks.
 * No generative text, no unsaved EPUB search, and no new persistence.
 */
@Composable
internal fun RebornInvestigationDesk(
    groups: List<InvestigationDeskGroup>,
    onOpenPassage: (Book, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(groups, query) {
        val q = query.trim()
        if (q.isEmpty()) groups else groups.filter { it.book.title.contains(q, ignoreCase = true) }
    }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).testTag("reborn-investigation-scroll"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "desk_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("reborn-desk-back")
                    ) { Text(stringResource(R.string.reborn_desk_back)) }
                    Text(
                        stringResource(R.string.reborn_desk_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(stringResource(R.string.reborn_desk_subtitle), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (groups.isNotEmpty()) {
                item(key = "desk_search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.reborn_desk_search)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            if (filtered.isEmpty()) {
                item(key = "desk_empty") {
                    Text(
                        stringResource(
                            if (groups.isEmpty()) R.string.reborn_desk_empty
                            else R.string.reborn_desk_no_results
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            filtered.forEach { group ->
                item(key = "book_header:${group.book.id}") {
                    Text(
                        group.book.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                items(
                    items = group.highlights,
                    key = { "mark:${group.book.id}:${it.id}" },
                    contentType = { "saved-clue" }
                ) { highlight ->
                    RebornDeskClueCard(group.book, highlight, onOpenPassage)
                }
            }
        }
    }
}

@Composable
private fun RebornDeskClueCard(
    book: Book,
    highlight: Highlight,
    onOpenPassage: (Book, String) -> Unit
) {
    // A reveal is ephemeral. Never persist it through rotation or process-state restore.
    var isRevealed by remember(book.id, highlight.id) { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, book.id, highlight.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) isRevealed = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.reborn_desk_saved), style = MaterialTheme.typography.labelLarge)
            if (!isRevealed) {
                Text(
                    stringResource(R.string.reborn_desk_sealed),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isRevealed) {
                if (highlight.quote.isNotBlank()) {
                    Text(highlight.quote, style = MaterialTheme.typography.bodyLarge)
                }
                if (highlight.note.isNotBlank()) {
                    Text(highlight.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
            TextButton(
                onClick = { isRevealed = !isRevealed },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(if (isRevealed) R.string.reborn_desk_hide else R.string.reborn_desk_reveal))
            }
            TextButton(
                onClick = { onOpenPassage(book, highlight.locatorJson) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.reborn_desk_return))
            }
        }
    }
}
