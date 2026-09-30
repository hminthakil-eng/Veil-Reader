package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onOpenLibrary: () -> Unit,
    onClose: () -> Unit
) {
    val highContrast = LocalVeilHighContrast.current
    val mangaBooks = remember(books) {
        books
            .filter { it.format == BookFormat.COMIC }
            .sortedByDescending { maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs) }
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
                    onClick = { onOpenBook(book) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f)
                    )
                ) {
                    Column(
                        Modifier.padding(VeilSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        Text(
                            stringResource(
                                R.string.manga_hub_progress,
                                (book.progress.coerceIn(0f, 1f) * 100).toInt()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
                        )
                    }
                }
            }
        }
    }
}
