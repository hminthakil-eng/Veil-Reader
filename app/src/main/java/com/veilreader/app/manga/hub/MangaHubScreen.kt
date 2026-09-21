package com.veilreader.app.manga.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veilreader.app.manga.library.MangaProgressStore
import com.veilreader.app.manga.reader.presentation.MangaChapterPresentationLoader
import com.veilreader.app.manga.reader.screen.MangaReaderIntegratedScreen
import java.io.File

@Composable
fun MangaHubScreen(
    service: MangaHubCatalogService,
    readerLoader: MangaChapterPresentationLoader,
    progressStore: MangaProgressStore,
    cacheRoot: File,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = remember(service) { MangaHubViewModel.factory(service) }
    val model: MangaHubViewModel = viewModel(
        key = "manga-hub",
        factory = factory
    )
    val state by model.state.collectAsStateWithLifecycle()

    state.activeReaderSession?.let { session ->
        MangaReaderIntegratedScreen(
            session = session,
            loader = readerLoader,
            progressStore = progressStore,
            cacheRoot = cacheRoot,
            onClose = model::closeReader,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    BackHandler {
        when {
            state.selectedDetails != null -> model.closeDetails()
            else -> onClose()
        }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        model.consumeError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
            .fillMaxSize()
            .testTag(MangaHubVerificationTags.ROOT)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MangaHubHeader(
                tab = state.tab,
                onClose = onClose,
                onTab = model::selectTab
            )

            if (state.sourceIssueCount > 0) {
                Text(
                    text = state.sourceIssueCount.toString() +
                        " source issue(s) were skipped. Your library is still available.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            val selected = state.selectedDetails
            if (selected != null) {
                MangaHubDetails(
                    state = state,
                    onBack = model::closeDetails,
                    onRead = { model.startReading() },
                    onReadChapter = { key -> model.startReading(key) },
                    onAdd = model::addSelectedToLibrary,
                    onRemove = model::removeSelectedFromLibrary,
                    modifier = Modifier.weight(1f)
                )
            } else {
                when (state.tab) {
                    MangaHubTab.DISCOVER -> MangaHubCatalogList(
                        items = state.discoverItems,
                        loading = state.discoverLoading,
                        emptyMessage = "No discovery items are configured yet.",
                        onItem = model::openCatalogItem,
                        modifier = Modifier.weight(1f)
                    )

                    MangaHubTab.SEARCH -> MangaHubSearch(
                        state = state,
                        onQuery = model::updateQuery,
                        onSearch = model::search,
                        onItem = model::openCatalogItem,
                        modifier = Modifier.weight(1f)
                    )

                    MangaHubTab.LIBRARY -> MangaHubLibrary(
                        state = state,
                        onWork = model::openLibraryWork,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaHubHeader(
    tab: MangaHubTab,
    onClose: () -> Unit,
    onTab: (MangaHubTab) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Manga Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Local-first · source-resilient",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(onClick = onClose) {
                Text("Close")
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MangaHubTab.entries.forEach { value ->
                if (value == tab) {
                    Button(
                        onClick = { onTab(value) },
                        modifier = Modifier.testTag(value.testTag())
                    ) {
                        Text(value.label())
                    }
                } else {
                    OutlinedButton(
                        onClick = { onTab(value) },
                        modifier = Modifier.testTag(value.testTag())
                    ) {
                        Text(value.label())
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaHubSearch(
    state: MangaHubUiState,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onItem: (MangaHubCatalogItem) -> Unit,
    modifier: Modifier
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                label = { Text("Search Manga") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(MangaHubVerificationTags.SEARCH_FIELD)
            )
            Button(
                onClick = onSearch,
                enabled = state.query.isNotBlank() && !state.searchLoading,
                modifier = Modifier.testTag(MangaHubVerificationTags.SEARCH_ACTION)
            ) {
                Text("Search")
            }
        }

        MangaHubCatalogList(
            items = state.searchItems,
            loading = state.searchLoading,
            emptyMessage = if (state.query.isBlank()) {
                "Search across installed Manga sources."
            } else {
                "No results."
            },
            onItem = onItem,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MangaHubCatalogList(
    items: List<MangaHubCatalogItem>,
    loading: Boolean,
    emptyMessage: String,
    onItem: (MangaHubCatalogItem) -> Unit,
    modifier: Modifier
) {
    Box(modifier.fillMaxSize()) {
        when {
            loading && items.isEmpty() ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            items.isEmpty() ->
                Text(
                    emptyMessage,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.summary.ref.sourceId.value + ":" + it.summary.ref.key }) { item ->
                    MangaHubCatalogCard(item, onItem)
                }
            }
        }
    }
}

@Composable
private fun MangaHubCatalogCard(
    item: MangaHubCatalogItem,
    onItem: (MangaHubCatalogItem) -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag(
                MangaHubVerificationTags.catalog(
                    item.summary.ref.sourceId.value,
                    item.summary.ref.key
                )
            )
            .clickable { onItem(item) }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                item.summary.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                item.source.displayName +
                    if (item.inLibrary) " · In library" else "",
                style = MaterialTheme.typography.bodySmall
            )
            item.summary.alternativeTitles.firstOrNull()?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MangaHubLibrary(
    state: MangaHubUiState,
    onWork: (com.veilreader.app.manga.library.CanonicalManga) -> Unit,
    modifier: Modifier
) {
    Box(modifier.fillMaxSize()) {
        when {
            state.libraryLoading && state.libraryWorks.isEmpty() ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            state.libraryWorks.isEmpty() ->
                Text(
                    "Your Manga library is empty. Search or discover a title to add it.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.libraryWorks, key = { it.id.value }) { work ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag(MangaHubVerificationTags.library(work.id.value))
                            .clickable { onWork(work) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                work.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                work.sourceRefs.size.toString() + " linked source(s)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaHubDetails(
    state: MangaHubUiState,
    onBack: () -> Unit,
    onRead: () -> Unit,
    onReadChapter: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier
) {
    val selected = requireNotNull(state.selectedDetails)
    LazyColumn(
        modifier
            .fillMaxSize()
            .testTag(MangaHubVerificationTags.DETAILS)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        item {
            Text(
                selected.details.summary.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                selected.source.displayName,
                style = MaterialTheme.typography.bodySmall
            )
        }

        selected.details.description?.takeIf { it.isNotBlank() }?.let { description ->
            item {
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRead,
                    enabled = !state.readerLoading && selected.chaptersInReadingOrder.isNotEmpty(),
                    modifier = Modifier.testTag(MangaHubVerificationTags.READ)
                ) {
                    Text(if (selected.canonical == null) "Read & save" else "Read")
                }

                if (selected.canonical == null) {
                    OutlinedButton(
                        onClick = onAdd,
                        enabled = !state.detailsLoading,
                        modifier = Modifier.testTag(MangaHubVerificationTags.ADD)
                    ) {
                        Text("Add to Library")
                    }
                } else {
                    OutlinedButton(
                        onClick = onRemove,
                        enabled = !state.detailsLoading,
                        modifier = Modifier.testTag(MangaHubVerificationTags.REMOVE)
                    ) {
                        Text("Remove")
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Text(
                selected.chaptersInReadingOrder.size.toString() + " chapters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        items(
            selected.chaptersInReadingOrder,
            key = { it.sourceId.value + ":" + it.chapterKey }
        ) { chapter ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag(
                        MangaHubVerificationTags.chapter(
                            chapter.sourceId.value,
                            chapter.chapterKey
                        )
                    )
                    .clickable { onReadChapter(chapter.chapterKey) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    chapter.title
                        ?: chapter.number?.let { "Chapter " + it.toString() }
                        ?: "Chapter",
                    style = MaterialTheme.typography.bodyLarge
                )
                chapter.languageTag?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun MangaHubTab.label(): String = when (this) {
    MangaHubTab.DISCOVER -> "Discover"
    MangaHubTab.SEARCH -> "Search"
    MangaHubTab.LIBRARY -> "Library"
}


private fun MangaHubTab.testTag(): String = when (this) {
    MangaHubTab.DISCOVER -> MangaHubVerificationTags.DISCOVER
    MangaHubTab.SEARCH -> MangaHubVerificationTags.SEARCH
    MangaHubTab.LIBRARY -> MangaHubVerificationTags.LIBRARY
}
