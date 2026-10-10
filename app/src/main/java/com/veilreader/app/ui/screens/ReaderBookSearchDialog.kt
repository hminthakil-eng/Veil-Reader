@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.ui.reader.ReaderBookSearchOpen
import com.veilreader.app.ui.reader.ReaderBookSearchPage
import com.veilreader.app.ui.reader.ReaderBookSearchSession
import com.veilreader.app.ui.reader.openVeilBookSearch
import com.veilreader.app.ui.theme.VeilPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

private enum class ReaderSearchStatus {
    IDLE, LOADING, RESULTS, UNSUPPORTED, INVALID_QUERY, ERROR, TOO_BROAD
}

/**
 * Search occupies a transient full-screen Sanctuary surface, never the page itself.
 * Readium owns extraction and semantic locators; the parent Reader owns jumps and history.
 * A query edit or closed Reader must release its cursor and disown all queued callbacks.
 */
@Composable
internal fun ReaderBookSearchDialog(
    publication: Publication,
    onDismiss: () -> Unit,
    onResult: (Locator) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember(publication) { mutableStateOf("") }
    var results by remember(publication) { mutableStateOf<List<Locator>>(emptyList()) }
    var status by remember(publication) { mutableStateOf(ReaderSearchStatus.IDLE) }
    var loading by remember(publication) { mutableStateOf(false) }
    var lastPage by remember(publication) { mutableStateOf(false) }
    var session by remember(publication) { mutableStateOf<ReaderBookSearchSession?>(null) }
    var work by remember(publication) { mutableStateOf<Job?>(null) }
    var generation by remember(publication) { mutableIntStateOf(0) }

    fun resetSearch() {
        generation++
        work?.cancel()
        work = null
        session?.close()
        session = null
        results = emptyList()
        lastPage = false
        loading = false
        status = ReaderSearchStatus.IDLE
    }

    fun loadNext(active: ReaderBookSearchSession, token: Int) {
        if (loading || lastPage) return
        loading = true
        work = scope.launch {
            try {
                val page = withContext(Dispatchers.IO) { active.nextPage(limit = 40) }
                if (generation != token || session !== active) return@launch
                when (page) {
                    is ReaderBookSearchPage.Hits -> {
                        results = results + page.locators
                        lastPage = page.isLast
                        status = ReaderSearchStatus.RESULTS
                    }
                    ReaderBookSearchPage.TooBroad -> status = ReaderSearchStatus.TOO_BROAD
                    ReaderBookSearchPage.Failed,
                    ReaderBookSearchPage.Closed -> status = ReaderSearchStatus.ERROR
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == token && session === active) status = ReaderSearchStatus.ERROR
            } finally {
                if (generation == token && session === active) loading = false
            }
        }
    }

    fun startSearch() {
        resetSearch()
        val token = generation
        val requested = query
        status = ReaderSearchStatus.LOADING
        loading = true
        work = scope.launch {
            try {
                val opened = withContext(Dispatchers.IO) {
                    publication.openVeilBookSearch(requested)
                }
                if (generation != token) {
                    if (opened is ReaderBookSearchOpen.Ready) opened.session.close()
                    return@launch
                }
                when (opened) {
                    is ReaderBookSearchOpen.Ready -> {
                        session = opened.session
                        loading = false
                        loadNext(opened.session, token)
                    }
                    ReaderBookSearchOpen.Unsupported -> status = ReaderSearchStatus.UNSUPPORTED
                    ReaderBookSearchOpen.InvalidQuery -> status = ReaderSearchStatus.INVALID_QUERY
                    ReaderBookSearchOpen.Failed -> status = ReaderSearchStatus.ERROR
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == token) status = ReaderSearchStatus.ERROR
            } finally {
                if (generation == token && session == null) loading = false
            }
        }
    }

    DisposableEffect(publication) {
        onDispose {
            generation++
            work?.cancel()
            session?.close()
        }
    }

    Dialog(
        onDismissRequest = {
            resetSearch()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = VeilPalette.Archive,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).heightIn(min = 280.dp, max = 700.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.reader_search_title),
                            modifier = Modifier.weight(1f),
                            color = VeilPalette.Brass,
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = {
                                resetSearch()
                                onDismiss()
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.reader_image_viewer_close)) }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            resetSearch()
                        },
                        singleLine = true,
                        maxLines = 1,
                        label = { Text(stringResource(R.string.reader_search_query)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = !loading && query.isNotBlank() && query.length <= 128,
                        onClick = { startSearch() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.reader_search_submit)) }

                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(stringResource(R.string.reader_search_loading))
                        }
                    }
                    when (status) {
                        ReaderSearchStatus.INVALID_QUERY ->
                            Text(stringResource(R.string.reader_search_invalid))
                        ReaderSearchStatus.UNSUPPORTED ->
                            Text(stringResource(R.string.reader_search_unsupported))
                        ReaderSearchStatus.ERROR ->
                            Text(stringResource(R.string.reader_search_failed))
                        ReaderSearchStatus.TOO_BROAD ->
                            Text(stringResource(R.string.reader_search_too_broad))
                        ReaderSearchStatus.RESULTS ->
                            if (lastPage && results.isEmpty()) {
                                Text(stringResource(R.string.reader_search_empty))
                            }
                        ReaderSearchStatus.IDLE, ReaderSearchStatus.LOADING -> Unit
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(results) { index, match ->
                            val context = buildString {
                                append(match.text.before.orEmpty().takeLast(85))
                                append(match.text.highlight.orEmpty().take(120))
                                append(match.text.after.orEmpty().take(85))
                            }.trim()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        resetSearch()
                                        onResult(match)
                                    }
                                    .heightIn(min = 48.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    match.title?.takeIf(String::isNotBlank)
                                        ?: stringResource(R.string.reader_search_result, index + 1),
                                    color = VeilPalette.Brass,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    context,
                                    color = VeilPalette.Moon,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (!lastPage && session != null && status == ReaderSearchStatus.RESULTS) {
                            item {
                                TextButton(
                                    enabled = !loading,
                                    onClick = {
                                        val active = session ?: return@TextButton
                                        loadNext(active, generation)
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                ) { Text(stringResource(R.string.reader_search_more)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
