@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.ui.resourceQuantity
import com.veilreader.app.ui.reader.ReaderBookSearchOpen
import com.veilreader.app.ui.reader.ReaderBookSearchPage
import com.veilreader.app.ui.reader.ReaderBookSearchSession
import com.veilreader.app.ui.reader.openVeilBookSearch
import com.veilreader.app.ui.theme.withVeilContentScript
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

internal enum class ReaderSearchStatus {
    IDLE, LOADING, RESULTS, UNSUPPORTED, INVALID_QUERY, ERROR, TOO_BROAD
}

/** One native search surface; Readium owns extraction and the Reader owns jumps/history. */
@Composable
internal fun ReaderBookSearchDialog(
    publication: Publication,
    onDismiss: () -> Unit,
    onResult: (Locator) -> Unit
) {
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    ReaderToolsHeader(stringResource(R.string.reader_search_title), onDismiss)
                    ReaderBookSearchPanel(publication, onResult, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Pinned, labeled exit remains reachable even in a short window or with the keyboard open. */
@Composable
internal fun ReaderToolsHeader(title: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = onDismiss, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
            ReaderToolIcon(ReaderToolSymbol.CLOSE, stringResource(R.string.reader_back_to_reading))
        }
    }
}

internal enum class ReaderToolSymbol { CLOSE, EXPAND, CURRENT }

/** Same lightweight Canvas/stroke language as the existing native Reader and Library icons. */
@Composable
internal fun ReaderToolIcon(symbol: ReaderToolSymbol, label: String? = null) {
    val tint = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp).semantics { label?.let { contentDescription = it } }) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(w * x1, h * y1), Offset(w * x2, h * y2), stroke, StrokeCap.Round)
        when (symbol) {
            ReaderToolSymbol.CLOSE -> { line(.25f, .25f, .75f, .75f); line(.75f, .25f, .25f, .75f) }
            ReaderToolSymbol.EXPAND -> { line(.2f, .4f, .5f, .7f); line(.5f, .7f, .8f, .4f) }
            ReaderToolSymbol.CURRENT -> {
                drawCircle(tint, w * .28f, Offset(w * .5f, h * .5f), style = Stroke(stroke))
                drawCircle(tint, w * .08f, Offset(w * .5f, h * .5f))
                line(.5f, .08f, .5f, .22f); line(.5f, .78f, .5f, .92f)
                line(.08f, .5f, .22f, .5f); line(.78f, .5f, .92f, .5f)
            }
        }
    }
}

/**
 * Shared by the direct Search action and Notebook. Removing this composition (book change,
 * tab change, dismissal) releases the cursor and disowns queued callbacks.
 */
@Composable
internal fun ReaderBookSearchPanel(
    publication: Publication,
    onResult: (Locator) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var query by remember(publication) { mutableStateOf("") }
    val results = remember(publication) { mutableStateListOf<Locator>() }
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
        results.clear()
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
                        results.addAll(page.locators)
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
        if (loading) return
        keyboard?.hide()
        resetSearch()
        val token = generation
        val requested = query
        status = ReaderSearchStatus.LOADING
        loading = true
        work = scope.launch {
            var acquired: ReaderBookSearchSession? = null
            try {
                val opened = withContext(Dispatchers.IO) {
                    publication.openVeilBookSearch(requested).also {
                        if (it is ReaderBookSearchOpen.Ready) acquired = it.session
                    }
                }
                if (generation != token) {
                    if (opened is ReaderBookSearchOpen.Ready) opened.session.close()
                    return@launch
                }
                when (opened) {
                    is ReaderBookSearchOpen.Ready -> {
                        session = opened.session
                        acquired = null // Ownership transfer survived the IO dispatcher return.
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
                acquired?.close() // Includes cancellation before withContext can return Ready.
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

    ReaderBookSearchContent(
        query = query, results = results, status = status, loading = loading,
        canLoadMore = !lastPage && session != null && status == ReaderSearchStatus.RESULTS,
        onQueryChange = { query = it; resetSearch() },
        onSearch = ::startSearch, onCancel = ::resetSearch,
        onMore = { session?.let { loadNext(it, generation) } },
        onResult = { match -> resetSearch(); onResult(match) }, modifier = modifier
    )
}

/** Controls, status and results share one bounded viewport, including in landscape/IME. */
@Composable
internal fun ReaderBookSearchContent(
    query: String,
    results: List<Locator>,
    status: ReaderSearchStatus,
    loading: Boolean,
    canLoadMore: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCancel: () -> Unit,
    onMore: () -> Unit,
    onResult: (Locator) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange,
                singleLine = true, label = { Text(stringResource(R.string.reader_search_query)) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { onQueryChange("") }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        ReaderToolIcon(ReaderToolSymbol.CLOSE, stringResource(R.string.library_search_clear))
                    } }
                } else null,
                isError = query.trim().length > 128 || '\u0000' in query,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (!loading) onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(enabled = !loading && query.isNotBlank() && query.trim().length <= 128 && '\u0000' !in query,
                onClick = onSearch, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reader_search_submit))
            }
        }
        if (loading) item {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text(stringResource(R.string.reader_search_loading),
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
        val messageRes = when {
            query.trim().length > 128 || '\u0000' in query -> R.string.reader_search_invalid
            status == ReaderSearchStatus.INVALID_QUERY -> R.string.reader_search_invalid
            status == ReaderSearchStatus.UNSUPPORTED -> R.string.reader_search_unsupported
            status == ReaderSearchStatus.ERROR -> R.string.reader_search_failed
            status == ReaderSearchStatus.TOO_BROAD -> R.string.reader_search_too_broad
            status == ReaderSearchStatus.RESULTS && results.isEmpty() && !canLoadMore && !loading -> R.string.reader_search_empty
            else -> null
        }
        messageRes?.let { res -> item {
            Text(stringResource(res), Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } }
        if (results.isNotEmpty()) item {
            Text(pluralStringResource(R.plurals.reader_search_matches_shown,
                resourceQuantity(results.size), results.size),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
        itemsIndexed(results) { index, match ->
            val context = buildAnnotatedString {
                append(match.text.before.orEmpty().replace(Regex("\\s+"), " ").takeLast(85))
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.primary)) {
                    append(match.text.highlight.orEmpty().take(120))
                }
                append(match.text.after.orEmpty().replace(Regex("\\s+"), " ").take(85))
            }
            val title = match.title?.takeIf(String::isNotBlank)
                ?: stringResource(R.string.reader_search_result, index + 1)
            Surface(color = colors.surface, contentColor = colors.onSurface,
                shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, colors.outlineVariant)) {
                Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onResult(match) }
                    .heightIn(min = 56.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.withVeilContentScript(title))
                    if (context.isNotBlank()) Text(context, color = colors.onSurface,
                        style = MaterialTheme.typography.bodyLarge.withVeilContentScript(context.text))
                }
            }
        }
        if (canLoadMore) item {
            TextButton(enabled = !loading, onClick = onMore,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reader_search_more))
            }
        }
    }
}
