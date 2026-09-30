package com.veilreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere

/**
 * First-class Manga surface for Veil Reader.
 *
 * The hub deliberately starts local/offline-first. Network source adapters remain disabled until
 * explicitly configured, while the dedicated manga reader/source/cache stack can evolve behind this
 * stable product surface without polluting the text-reader navigation model.
 */
@Composable
fun MangaHubScreen(
    books: List<Book>,
    onOpenBook: (Book) -> Unit,
    onAddChapterUri: (Book, Uri) -> Unit,
    onOpenLibrary: () -> Unit,
    onClose: () -> Unit,
    isImporting: Boolean = false
) {
    val highContrast = LocalVeilHighContrast.current
    val mangaBooks = remember(books) {
        books
            .filter { it.format == BookFormat.COMIC }
            .sortedByDescending { maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs) }
    }

    var chapterTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val chapterLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = chapterTargetId
            ?.let { targetId -> mangaBooks.firstOrNull { it.id == targetId } }
        chapterTargetId = null
        if (uri != null && target != null) {
            onAddChapterUri(target, uri)
        }
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = 7001 + mangaBooks.size * 17,
                intensity = if (highContrast) 0.74f else 0.94f
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = VeilSpacing.lg,
            vertical = VeilSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "manga:header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        stringResource(R.string.manga_hub_eyebrow),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.manga_hub_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.manga_hub_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onClose) {
                    Text(stringResource(R.string.manga_hub_close))
                }
            }
        }

        item(key = "manga:policy") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                border = BorderStroke(
                    1.dp,
                    if (highContrast) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        VeilPalette.Brass.copy(alpha = 0.30f)
                    }
                )
            ) {
                Column(
                    Modifier.padding(VeilSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        stringResource(R.string.manga_hub_local_badge),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.manga_hub_engine_ready),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.manga_hub_live_sources_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (mangaBooks.isEmpty()) {
            item(key = "manga:empty") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
                ) {
                    Column(
                        Modifier.padding(VeilSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        Text(
                            stringResource(R.string.manga_hub_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.manga_hub_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenLibrary,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.manga_hub_open_library))
                        }
                    }
                }
            }
        } else {
            items(
                items = mangaBooks,
                key = Book::id
            ) { book ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    border = BorderStroke(
                        1.dp,
                        if (highContrast) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            VeilPalette.Brass.copy(alpha = 0.24f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(VeilSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MangaHubCover(book)

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                        ) {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (book.author.isNotBlank()) {
                                Text(
                                    book.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val progress = book.progress.coerceIn(0f, 1f)
                            Text(
                                stringResource(
                                    R.string.manga_hub_progress,
                                    (progress * 100).toInt()
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (highContrast) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    VeilPalette.Brass
                                }
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                stringResource(
                                    if (book.finished) {
                                        R.string.manga_hub_finished
                                    } else {
                                        R.string.manga_hub_saved_offline
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                            ) {
                                Button(
                                    onClick = { onOpenBook(book) },
                                    enabled = !isImporting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        stringResource(
                                            if (progress > 0f && !book.finished) {
                                                R.string.manga_hub_continue
                                            } else if (book.finished) {
                                                R.string.manga_hub_read_again
                                            } else {
                                                R.string.manga_hub_start
                                            }
                                        )
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        chapterTargetId = book.id
                                        chapterLauncher.launch(
                                            arrayOf(
                                                "application/vnd.comicbook+zip",
                                                "application/x-cbz",
                                                "application/zip"
                                            )
                                        )
                                    },
                                    enabled = !isImporting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(stringResource(R.string.manga_hub_add_chapter))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun MangaHubCover(book: Book) {
    val coverPath = book.coverCachePath?.takeIf { it.isNotBlank() }
    val shape = MaterialTheme.shapes.extraSmall
    if (coverPath != null) {
        AsyncImage(
            model = coverPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(78.dp)
                .aspectRatio(2f / 3f)
                .clip(shape)
        )
    } else {
        Box(
            modifier = Modifier
                .width(78.dp)
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.title.trim().take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = VeilPalette.Brass
            )
        }
    }
}
