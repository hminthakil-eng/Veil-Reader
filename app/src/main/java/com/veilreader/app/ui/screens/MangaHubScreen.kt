package com.veilreader.app.ui.screens

import android.net.Uri
import android.text.format.Formatter
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.veilreader.app.R
import com.veilreader.app.data.manga.MangaLocalChapterMetadata
import com.veilreader.app.data.manga.MangaLocalChapterSummary
import com.veilreader.app.data.manga.MangaLocalStorageSummary
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.VeilAdaptiveClass
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.adaptiveClassFor
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
    onAddChapterUris: (Book, List<Uri>) -> Unit,
    storageSummaryProvider: suspend (Book) -> MangaLocalStorageSummary,
    chapterSummaryProvider: suspend (Book) -> List<MangaLocalChapterSummary>,
    onUpdateChapterMetadata: (
        Book,
        MangaLocalChapterSummary,
        MangaLocalChapterMetadata
    ) -> Unit,
    onMoveChapter: (Book, MangaLocalChapterSummary, Int) -> Unit,
    onDeleteChapter: (Book, MangaLocalChapterSummary) -> Unit,
    onClearDerivedCache: (Book) -> Unit,
    storageRevision: Int,
    onOpenLibrary: () -> Unit,
    onClose: () -> Unit,
    isImporting: Boolean = false
) {
    val highContrast = LocalVeilHighContrast.current
    val context = LocalContext.current
    val adaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val compactLayout = adaptiveClass == VeilAdaptiveClass.COMPACT
    val mangaBooks = remember(books) {
        books
            .filter { it.format == BookFormat.COMIC }
            .sortedByDescending { maxOf(it.lastOpenedAtEpochMs, it.addedAtEpochMs) }
    }

    var chapterTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val chapterLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val target = chapterTargetId
            ?.let { targetId -> mangaBooks.firstOrNull { it.id == targetId } }
        chapterTargetId = null
        if (uris.isNotEmpty() && target != null) {
            onAddChapterUris(target, uris)
        }
    }

    var expandedChapterBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var editTarget by remember { mutableStateOf<Pair<Book, MangaLocalChapterSummary>?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editNumber by remember { mutableStateOf("") }
    var editVolume by remember { mutableStateOf("") }
    var editLanguage by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Pair<Book, MangaLocalChapterSummary>?>(null) }


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
                    .widthIn(max = 860.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.manga_hub_eyebrow),
                        color = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass,
                        strong = true
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
                    .widthIn(max = 860.dp)
                    .fillMaxWidth(),
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
                val storage by produceState<MangaLocalStorageSummary?>(
                    initialValue = null,
                    book.id,
                    storageRevision
                ) {
                    value = runCatching { storageSummaryProvider(book) }.getOrNull()
                }
                val chapters by produceState<List<MangaLocalChapterSummary>>(
                    initialValue = emptyList(),
                    book.id,
                    storageRevision,
                    expandedChapterBookId
                ) {
                    value = if (expandedChapterBookId == book.id) {
                        runCatching { chapterSummaryProvider(book) }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
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
                                book.title.ifBlank {
                                    stringResource(R.string.common_untitled_book)
                                },
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
                            storage?.let { summary ->
                                Text(
                                    stringResource(
                                        R.string.manga_hub_storage_summary,
                                        summary.chapterCount,
                                        Formatter.formatShortFileSize(context, summary.sourceBytes),
                                        Formatter.formatShortFileSize(context, summary.cacheBytes)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (summary.cacheBytes > 0L) {
                                    TextButton(
                                        onClick = { onClearDerivedCache(book) },
                                        enabled = !isImporting
                                    ) {
                                        Text(stringResource(R.string.manga_hub_clear_cache))
                                    }
                                }
                            }

                            TextButton(
                                onClick = {
                                    expandedChapterBookId =
                                        if (expandedChapterBookId == book.id) null else book.id
                                },
                                enabled = !isImporting
                            ) {
                                Text(
                                    stringResource(
                                        if (expandedChapterBookId == book.id) {
                                            R.string.manga_hub_hide_chapters
                                        } else {
                                            R.string.manga_hub_manage_chapters
                                        },
                                        storage?.chapterCount ?: 0
                                    )
                                )
                            }

                            if (expandedChapterBookId == book.id) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                                ) {
                                    chapters.forEachIndexed { index, chapter ->
                                        MangaChapterManagementRow(
                                            chapter = chapter,
                                            canMoveUp = !chapter.isPrimary && chapter.readingOrder > 1,
                                            canMoveDown =
                                                !chapter.isPrimary && index < chapters.lastIndex,
                                            enabled = !isImporting,
                                            onEdit = {
                                                editTarget = book to chapter
                                                editTitle = chapter.title
                                                editNumber = chapter.number
                                                    ?.let(::formatChapterNumber)
                                                    .orEmpty()
                                                editVolume = chapter.volume
                                                    ?.let(::formatChapterNumber)
                                                    .orEmpty()
                                                editLanguage = chapter.languageTag.orEmpty()
                                            },
                                            onMoveUp = {
                                                onMoveChapter(book, chapter, -1)
                                            },
                                            onMoveDown = {
                                                onMoveChapter(book, chapter, 1)
                                            },
                                            onDelete = {
                                                deleteTarget = book to chapter
                                            }
                                        )
                                    }
                                }
                            }

                            MangaHubActions(
                                compact = compactLayout,
                                enabled = !isImporting,
                                primaryLabel = stringResource(
                                    if (progress > 0f && !book.finished) {
                                        R.string.manga_hub_continue
                                    } else if (book.finished) {
                                        R.string.manga_hub_read_again
                                    } else {
                                        R.string.manga_hub_start
                                    }
                                ),
                                secondaryLabel = stringResource(R.string.manga_hub_add_chapter),
                                onOpen = { onOpenBook(book) },
                                onAddChapter = {
                                    chapterTargetId = book.id
                                    chapterLauncher.launch(
                                        arrayOf(
                                            "application/vnd.comicbook+zip",
                                            "application/x-cbz",
                                            "application/zip"
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    editTarget?.let { (book, chapter) ->
        val parsedNumber = parseLocalizedChapterDecimal(editNumber)
        val parsedVolume = editVolume
            .takeIf { it.isNotBlank() }
            ?.let(::parseLocalizedChapterDecimal)
        val numberValid = parsedNumber != null && parsedNumber >= 0.0
        val volumeValid =
            editVolume.isBlank() || (parsedVolume != null && parsedVolume >= 0.0)

        AlertDialog(
            onDismissRequest = {
                editTarget = null
            },
            title = {
                Text(stringResource(R.string.manga_chapter_edit_title))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        singleLine = true,
                        label = {
                            Text(stringResource(R.string.manga_chapter_title_label))
                        }
                    )
                    OutlinedTextField(
                        value = editNumber,
                        onValueChange = { editNumber = it },
                        singleLine = true,
                        label = {
                            Text(stringResource(R.string.manga_chapter_number_label))
                        },
                        supportingText = if (!numberValid && editNumber.isNotBlank()) {
                            {
                                Text(stringResource(R.string.manga_chapter_number_invalid))
                            }
                        } else {
                            null
                        },
                        isError = !numberValid && editNumber.isNotBlank()
                    )
                    OutlinedTextField(
                        value = editVolume,
                        onValueChange = { editVolume = it },
                        singleLine = true,
                        label = {
                            Text(stringResource(R.string.manga_chapter_volume_label))
                        },
                        supportingText = if (!volumeValid) {
                            {
                                Text(stringResource(R.string.manga_chapter_volume_invalid))
                            }
                        } else {
                            null
                        },
                        isError = !volumeValid
                    )
                    OutlinedTextField(
                        value = editLanguage,
                        onValueChange = { editLanguage = it },
                        singleLine = true,
                        label = {
                            Text(stringResource(R.string.manga_chapter_language_label))
                        },
                        supportingText = {
                            Text(stringResource(R.string.manga_chapter_language_hint))
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateChapterMetadata(
                            book,
                            chapter,
                            MangaLocalChapterMetadata(
                                title = editTitle,
                                volume = parsedVolume,
                                number = parsedNumber,
                                languageTag = editLanguage
                            )
                        )
                        editTarget = null
                    },
                    enabled =
                        editTitle.isNotBlank() &&
                            numberValid &&
                            volumeValid &&
                            !isImporting
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    deleteTarget?.let { (book, chapter) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(stringResource(R.string.manga_chapter_delete_title))
            },
            text = {
                Text(
                    stringResource(
                        R.string.manga_chapter_delete_body,
                        chapter.title
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteChapter(book, chapter)
                        deleteTarget = null
                    },
                    enabled = !isImporting
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}


@Composable
private fun MangaChapterManagementRow(
    chapter: MangaLocalChapterSummary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    enabled: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(
                            R.string.manga_chapter_metadata,
                            chapter.readingOrder + 1,
                            chapter.number?.let { formatChapterNumber(it) }
                                ?: stringResource(R.string.manga_chapter_unknown_number),
                            chapter.pageCount,
                            Formatter.formatShortFileSize(
                                context,
                                chapter.sourceBytes + chapter.cacheBytes
                            )
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (chapter.isPrimary) {
                        Text(
                            stringResource(R.string.manga_chapter_primary_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Brass
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                TextButton(onClick = onEdit, enabled = enabled) {
                    Text(stringResource(R.string.action_edit))
                }
                if (!chapter.isPrimary) {
                    TextButton(onClick = onDelete, enabled = enabled) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
            if (!chapter.isPrimary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                ) {
                    TextButton(
                        onClick = onMoveUp,
                        enabled = enabled && canMoveUp
                    ) {
                        Text(stringResource(R.string.manga_chapter_move_up))
                    }
                    TextButton(
                        onClick = onMoveDown,
                        enabled = enabled && canMoveDown
                    ) {
                        Text(stringResource(R.string.manga_chapter_move_down))
                    }
                }
            }
        }
    }
}

private fun parseLocalizedChapterDecimal(value: String): Double? {
    val normalized = buildString(value.length) {
        value.trim().forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> '0' + (char - '۰')
                    in '٠'..'٩' -> '0' + (char - '٠')
                    '٫', ',' -> '.'
                    '٬', ' ', '\u00A0' -> return@forEach
                    else -> char
                }
            )
        }
    }
    return normalized.toDoubleOrNull()
}

private fun formatChapterNumber(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString().trimEnd('0').trimEnd('.')
    }


@Composable
private fun MangaHubActions(
    compact: Boolean,
    enabled: Boolean,
    primaryLabel: String,
    secondaryLabel: String,
    onOpen: () -> Unit,
    onAddChapter: () -> Unit
) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Button(
                onClick = onOpen,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(primaryLabel)
            }
            OutlinedButton(
                onClick = onAddChapter,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(secondaryLabel)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Button(
                onClick = onOpen,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text(primaryLabel)
            }
            OutlinedButton(
                onClick = onAddChapter,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text(secondaryLabel)
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
