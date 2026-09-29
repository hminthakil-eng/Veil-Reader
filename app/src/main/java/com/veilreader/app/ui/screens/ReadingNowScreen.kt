package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookArtifactMemory
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.deriveBookArtifactMemory
import com.veilreader.app.ui.books.bookArtifactState
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.thresholdAtmosphereIntensityFor
import com.veilreader.app.ui.theme.thresholdLayoutPolicyFor
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing

/**
 * Threshold is the calm front door to reading: resume first, recent books second, world progress last.
 * Game systems stay visible enough to feel alive without competing with the next reading action.
 */
@Composable
fun ReadingNowScreen(
    books: List<Book>,
    profile: ReaderProfile,
    quests: List<Quest>,
    highlights: List<Highlight> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    onOpenBook: (Book) -> Unit,
    onOpenPassage: (Book, String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCastle: () -> Unit
) {
    val snapshot = buildThresholdSnapshot(books)
    val current = snapshot.hero
    val thresholdAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val thresholdLayout = thresholdLayoutPolicyFor(thresholdAdaptiveClass)
    val artifactMemoryByBookId = remember(
        books,
        highlights,
        bookmarks,
        readingSessions
    ) {
        val sessionsByBook = readingSessions
            .filter { !it.bookId.isNullOrBlank() }
            .groupBy { requireNotNull(it.bookId) }
        val highlightsByBook = highlights.groupBy { it.bookId }
        val bookmarksByBook = bookmarks.groupBy { it.bookId }

        books.associate { book ->
            book.id to deriveBookArtifactMemory(
                book = book,
                sessions = sessionsByBook[book.id].orEmpty(),
                highlights = highlightsByBook[book.id].orEmpty(),
                bookmarks = bookmarksByBook[book.id].orEmpty()
            )
        }
    }
    val thresholdWhisper = remember(
        snapshot.hero,
        snapshot.recent,
        highlights,
        quests
    ) {
        deriveThresholdWhisper(
            visibleBooks = listOfNotNull(snapshot.hero) + snapshot.recent,
            highlights = highlights,
            quests = quests
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.THRESHOLD,
                seed = books.size + profile.level,
                intensity = thresholdAtmosphereIntensityFor(books.size)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = thresholdLayout.contentMaxWidthDp.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = VeilSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            VeilReveal(delayMillis = 10, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(horizontal = VeilSpacing.sm, vertical = VeilSpacing.xs)) {
                    ThresholdHeader(
                        bookCount = books.size,
                        hasCurrentBook = current != null,
                        headerHeightDp = thresholdLayout.headerHeightDp
                    )
                }
            }

            Spacer(Modifier.height(VeilSpacing.sm))

            VeilReveal(delayMillis = 70, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.padding(
                        horizontal = thresholdLayout.horizontalPaddingDp.dp
                    )
                ) {
                    if (current == null) {
                        EmptyReadingState(onOpenLibrary)
                    } else {
                        ContinueReadingHero(
                            current = current,
                            artifactMemory = artifactMemoryByBookId[current.id],
                            coverWidthDp = thresholdLayout.heroCoverWidthDp,
                            coverHeightDp = thresholdLayout.heroCoverHeightDp,
                            onOpenBook = onOpenBook
                        )
                    }
                }
            }

            if (snapshot.recent.isNotEmpty()) {
                Spacer(Modifier.height(VeilSpacing.xl))
                VeilReveal(delayMillis = 130, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.padding(
                            horizontal = thresholdLayout.horizontalPaddingDp.dp
                        )
                    ) {
                        RecentBooksShelf(
                            books = snapshot.recent,
                            artifactMemoryByBookId = artifactMemoryByBookId,
                            itemWidthDp = thresholdLayout.recentItemWidthDp,
                            coverWidthDp = thresholdLayout.recentCoverWidthDp,
                            coverHeightDp = thresholdLayout.recentCoverHeightDp,
                            onOpenBook = onOpenBook,
                            onOpenLibrary = onOpenLibrary
                        )
                    }
                }
            }

            thresholdWhisper?.let { whisper ->
                Spacer(Modifier.height(VeilSpacing.xl))
                VeilReveal(delayMillis = 170, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.padding(
                            horizontal = thresholdLayout.horizontalPaddingDp.dp
                        )
                    ) {
                        ThresholdWhisperCard(
                            whisper = whisper,
                            books = books,
                            pathId = profile.path.id,
                            onOpenPassage = onOpenPassage
                        )
                    }
                }
            }

            Spacer(Modifier.height(VeilSpacing.xl))
            VeilReveal(delayMillis = 210, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.padding(
                        horizontal = thresholdLayout.horizontalPaddingDp.dp
                    )
                ) {
                    ReadingPulse(
                        profile = profile,
                        onOpenCastle = onOpenCastle
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdHeader(
    bookCount: Int,
    hasCurrentBook: Boolean,
    headerHeightDp: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f)),
                MaterialTheme.shapes.medium
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(headerHeightDp.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.grayfog_threshold_v1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.44f to Color.Transparent,
                            1f to VeilPalette.Ink.copy(alpha = 0.98f)
                        )
                    )
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                VeilPalette.Ink.copy(alpha = 0.34f),
                                Color.Transparent,
                                Color.Transparent,
                                VeilPalette.Ink.copy(alpha = 0.22f)
                            )
                        )
                    )
            )

            ThresholdDepthField(
                bookCount = bookCount,
                modifier = Modifier.matchParentSize()
            )

            GrayfogOrnamentFrame(
                modifier = Modifier.matchParentSize(),
                strength = 0.74f
            )

            Text(
                stringResource(R.string.app_name).uppercase(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = VeilSpacing.md, top = VeilSpacing.md),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.9.sp),
                color = VeilPalette.Brass
            )

            Text(
                stringResource(R.string.threshold_grayfog_archive),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = VeilSpacing.md, top = VeilSpacing.md),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.35.sp),
                color = VeilPalette.Moon.copy(alpha = 0.72f)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(
                        when {
                            bookCount == 0 -> R.string.threshold_title_unwritten
                            bookCount == 1 -> R.string.threshold_title_first_volume
                            hasCurrentBook -> R.string.threshold_title_library_awaits
                            else -> R.string.threshold_title_return_archive
                        }
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(
                        when {
                            bookCount == 0 -> R.string.threshold_body_unwritten
                            bookCount == 1 -> R.string.threshold_body_first_volume
                            hasCurrentBook -> R.string.threshold_body_library_awaits
                            else -> R.string.threshold_body_return_archive
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Moon.copy(alpha = 0.82f),
                    modifier = Modifier.widthIn(max = 540.dp)
                )
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(112.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    VeilPalette.Brass.copy(alpha = 0.92f),
                                    VeilPalette.Brass.copy(alpha = 0.42f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ThresholdDepthField(
    bookCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val waking = (bookCount.coerceIn(0, 12) / 12f)
        val vanishing = Offset(
            x = size.width * 0.53f,
            y = size.height * 0.46f
        )
        val line = VeilPalette.Brass.copy(
            alpha = 0.028f + waking * 0.032f
        )
        val mist = VeilPalette.Mist.copy(
            alpha = 0.018f + waking * 0.018f
        )

        repeat(4) { index ->
            val lane = (index + 1f) / 5f
            drawLine(
                color = line,
                start = Offset(size.width * lane, size.height),
                end = vanishing,
                strokeWidth = 0.65.dp.toPx()
            )
        }

        repeat(3) { index ->
            val y = size.height * (0.63f + index * 0.105f)
            val halfWidth = size.width * (0.20f + index * 0.13f)
            drawLine(
                color = line.copy(alpha = line.alpha * (0.92f - index * 0.16f)),
                start = Offset(
                    (vanishing.x - halfWidth).coerceAtLeast(0f),
                    y
                ),
                end = Offset(
                    (vanishing.x + halfWidth).coerceAtMost(size.width),
                    y
                ),
                strokeWidth = 0.55.dp.toPx()
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    mist,
                    Color.Transparent
                ),
                startY = size.height * 0.42f,
                endY = size.height * 0.84f
            ),
            topLeft = Offset(0f, size.height * 0.40f),
            size = androidx.compose.ui.geometry.Size(
                size.width,
                size.height * 0.46f
            )
        )
    }
}

@Composable
private fun ContinueReadingHero(
    current: Book,
    artifactMemory: BookArtifactMemory?,
    coverWidthDp: Float,
    coverHeightDp: Float,
    onOpenBook: (Book) -> Unit
) {
    val formatPercent = rememberVeilPercentFormatter()
    val progress = current.progress.coerceIn(0f, 1f)
    val paper = VeilPalette.ReaderPaper
    val paperLight = Color(0xFFF2E8D2)
    val paperDark = Color(0xFFD9C8A6)
    val ink = Color(0xFF29231C)
    val secondaryInk = Color(0xFF6A5A43)
    val shape = MaterialTheme.shapes.small
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(paperLight, paper, paperDark)
                )
            )
            .border(BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.82f)), shape)
            .padding(VeilSpacing.md)
    ) {
        val stacked = maxWidth < 300.dp || fontScale > 1.45f

        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.library_continue_reading).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.55.sp),
                    color = secondaryInk,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatPercent(progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryInk
                )
            }

            if (stacked) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    HeroCover(current, artifactMemory, coverWidthDp, coverHeightDp)
                }
                HeroDetails(current, ink, secondaryInk)
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroCover(current, artifactMemory, coverWidthDp, coverHeightDp)
                    HeroDetails(
                        current = current,
                        ink = ink,
                        secondaryInk = secondaryInk,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = ink,
                trackColor = secondaryInk.copy(alpha = 0.18f),
                drawStopIndicator = {}
            )

            Text(
                heroProgressLabel(current, (progress * 100).toInt(), progress),
                style = MaterialTheme.typography.labelMedium,
                color = secondaryInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Button(
                onClick = { onOpenBook(current) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ink,
                    contentColor = paperLight
                )
            ) {
                Text(
                    stringResource(
                        if (progress > 0f && !current.finished) {
                            R.string.threshold_return_volume
                        } else {
                            R.string.threshold_open_volume
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun HeroCover(
    current: Book,
    artifactMemory: BookArtifactMemory?,
    coverWidthDp: Float,
    coverHeightDp: Float
) {
    BookCover(
        title = current.title,
        subtitle = current.author,
        imagePath = current.coverCachePath,
        artifact = bookArtifactState(
            current,
            memory = artifactMemory
        ),
        modifier = Modifier
            .width(coverWidthDp.dp)
            .height(coverHeightDp.dp)
    )
}

@Composable
private fun HeroDetails(
    current: Book,
    ink: Color,
    secondaryInk: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
        Text(
            current.title,
            style = MaterialTheme.typography.titleLarge,
            color = ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            current.author.ifBlank { stringResource(R.string.common_unknown_author) },
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        current.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
            Text(series, style = MaterialTheme.typography.labelMedium, color = secondaryInk,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun heroProgressLabel(current: Book, progressPercent: Int, progress: Float): String = when {
    current.finished -> stringResource(R.string.threshold_progress_finished_reopen)
    progress <= 0f -> stringResource(R.string.threshold_progress_ready)
    current.currentChapter.isNotBlank() && current.currentChapter != "Not started" ->
        stringResource(R.string.threshold_progress_chapter, current.currentChapter, progressPercent)
    else -> stringResource(R.string.threshold_progress_complete, progressPercent)
}

@Composable
private fun RecentBooksShelf(
    books: List<Book>,
    artifactMemoryByBookId: Map<String, BookArtifactMemory>,
    itemWidthDp: Float,
    coverWidthDp: Float,
    coverHeightDp: Float,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.threshold_recent_eyebrow),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    stringResource(R.string.threshold_recent_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            TextButton(
                onClick = onOpenLibrary,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = VeilPalette.Brass
                )
            ) {
                Text(stringResource(R.string.common_view_all))
            }
        }

        BrassRule(Modifier.fillMaxWidth())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            books.forEach { book ->
                RecentBookCard(
                    book = book,
                    artifactMemory = artifactMemoryByBookId[book.id],
                    itemWidthDp = itemWidthDp,
                    coverWidthDp = coverWidthDp,
                    coverHeightDp = coverHeightDp,
                    onOpenBook = onOpenBook
                )
            }
        }
    }
}

@Composable
private fun RecentBookCard(
    book: Book,
    artifactMemory: BookArtifactMemory?,
    itemWidthDp: Float,
    coverWidthDp: Float,
    coverHeightDp: Float,
    onOpenBook: (Book) -> Unit
) {
    Surface(
        onClick = { onOpenBook(book) },
        modifier = Modifier.width(itemWidthDp.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BookCover(
                title = book.title,
                subtitle = book.author,
                imagePath = book.coverCachePath,
                artifact = bookArtifactState(
                    book,
                    memory = artifactMemory
                ),
                modifier = Modifier
                    .width(coverWidthDp.dp)
                    .height(coverHeightDp.dp)
            )
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                recentBookStatus(book),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun recentBookStatus(book: Book): String {
    val progress = book.progress.coerceIn(0f, 1f)
    return when {
        book.finished -> stringResource(R.string.book_detail_finished)
        progress <= 0f -> stringResource(R.string.book_detail_not_started)
        else -> stringResource(R.string.book_detail_percent_read, (progress * 100).toInt())
    }
}

internal enum class ThresholdWhisperKind {
    PRESERVED_PASSAGE,
    READING_PROMPT
}

internal data class ThresholdWhisper(
    val kind: ThresholdWhisperKind,
    val bookId: String?,
    val locatorJson: String?,
    val title: String,
    val body: String,
    val detail: String?,
    val questId: String? = null,
    val questProgress: Int? = null,
    val questTarget: Int? = null
)

internal fun deriveThresholdWhisper(
    visibleBooks: List<Book>,
    highlights: List<Highlight>,
    quests: List<Quest>
): ThresholdWhisper? {
    val booksById = visibleBooks
        .distinctBy { it.id }
        .associateBy { it.id }

    highlights
        .asSequence()
        .filter { highlight ->
            highlight.bookId in booksById &&
                highlight.quote.isNotBlank()
        }
        .sortedWith(
            compareByDescending<Highlight> { it.createdAtEpochMs }
                .thenBy { it.id }
        )
        .firstOrNull()
        ?.let { highlight ->
            val book = booksById.getValue(highlight.bookId)
            return ThresholdWhisper(
                kind = ThresholdWhisperKind.PRESERVED_PASSAGE,
                bookId = book.id,
                locatorJson = highlight.locatorJson,
                title = book.title,
                body = highlight.quote
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(220),
                detail = highlight.note
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(120)
                    .takeIf { it.isNotBlank() }
            )
        }

    quests
        .asSequence()
        .filter { quest -> quest.progress < quest.target.coerceAtLeast(1) }
        .firstOrNull()
        ?.let { quest ->
            val target = quest.target.coerceAtLeast(1)
            return ThresholdWhisper(
                kind = ThresholdWhisperKind.READING_PROMPT,
                bookId = null,
                locatorJson = null,
                title = "",
                body = quest.title,
                detail = null,
                questId = quest.id,
                questProgress = quest.progress.coerceAtLeast(0),
                questTarget = target
            )
        }

    return null
}

@Composable
private fun ThresholdWhisperCard(
    whisper: ThresholdWhisper,
    books: List<Book>,
    pathId: String,
    onOpenPassage: (Book, String) -> Unit
) {
    val promptTarget = whisper.questTarget?.coerceAtLeast(1) ?: 1
    val displayTitle = if (whisper.kind == ThresholdWhisperKind.READING_PROMPT) {
        stringResource(R.string.threshold_quiet_invitation)
    } else {
        whisper.title
    }
    val displayBody = if (whisper.kind == ThresholdWhisperKind.READING_PROMPT) {
        localizedThresholdQuest(
            questId = whisper.questId,
            pathId = pathId,
            target = promptTarget
        )
    } else {
        whisper.body
    }
    val displayDetail = if (whisper.kind == ThresholdWhisperKind.READING_PROMPT) {
        stringResource(
            R.string.threshold_quest_progress,
            whisper.questProgress?.coerceAtLeast(0) ?: 0,
            promptTarget
        )
    } else {
        whisper.detail
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.42f),
        border = BorderStroke(
            1.dp,
            VeilPalette.BorderDark.copy(alpha = 0.68f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(
                    when (whisper.kind) {
                        ThresholdWhisperKind.PRESERVED_PASSAGE -> R.string.threshold_whisper_preserved
                        ThresholdWhisperKind.READING_PROMPT -> R.string.threshold_whisper_optional
                    }
                ),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.25.sp),
                color = VeilPalette.Brass
            )
            Text(
                displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = VeilPalette.Moon,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (whisper.kind == ThresholdWhisperKind.PRESERVED_PASSAGE) {
                    "“$displayBody”"
                } else {
                    displayBody
                },
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Mist.copy(alpha = 0.86f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            displayDetail?.let { detail ->
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Spirit.copy(alpha = 0.70f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val book = whisper.bookId?.let { id -> books.firstOrNull { it.id == id } }
            if (book != null && !whisper.locatorJson.isNullOrBlank()) {
                TextButton(
                    onClick = { onOpenPassage(book, requireNotNull(whisper.locatorJson)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = VeilPalette.Brass
                    )
                ) {
                    Text(stringResource(R.string.archive_return_to_passage))
                }
            }
        }
    }
}

@Composable
private fun ReadingPulse(
    profile: ReaderProfile,
    onOpenCastle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.profile_reading_record).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                color = VeilPalette.Brass,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onOpenCastle,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = VeilPalette.Brass
                )
            ) {
                Text(
                    stringResource(R.string.profile_stat_castle),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        BrassRule(Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            ReadingPulseValue(
                value = stringResource(R.string.profile_stat_streak_value, profile.streakDays),
                label = stringResource(R.string.profile_stat_current_streak),
                modifier = Modifier.weight(1f)
            )
            ReadingPulseValue(
                value = formatReadingTime(profile.minutesRead),
                label = stringResource(R.string.threshold_stat_reading),
                modifier = Modifier.weight(1f)
            )
            ReadingPulseValue(
                value = "${profile.booksFinished}",
                label = stringResource(R.string.profile_stat_finished),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReadingPulseValue(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyReadingState(onOpenLibrary: () -> Unit) {
    val paper = VeilPalette.ReaderPaper
    val ink = Color(0xFF29231C)
    val mutedInk = Color(0xFF6A5A43)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.72f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF3E9D5),
                            paper,
                            Color(0xFFD7C6A4)
                        )
                    )
                )
                .padding(VeilSpacing.lg)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                Text(
                    stringResource(R.string.threshold_first_eyebrow),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = mutedInk
                )
                Text(
                    stringResource(R.string.threshold_first_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = ink
                )
                Text(
                    stringResource(R.string.threshold_first_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedInk
                )
                Box(
                    Modifier
                        .width(86.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ink.copy(alpha = 0.74f),
                                    ink.copy(alpha = 0.24f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Button(
                    onClick = onOpenLibrary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .padding(top = 2.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ink,
                        contentColor = Color(0xFFF3E9D5)
                    )
                ) {
                    Text(stringResource(R.string.threshold_enter_library))
                }
            }
        }
    }
}
@Composable
private fun formatReadingTime(minutes: Int): String = when {
    minutes >= 6000 -> stringResource(R.string.profile_duration_hours, minutes / 60)
    minutes >= 60 -> stringResource(R.string.profile_duration_hours_minutes, minutes / 60, minutes % 60)
    else -> stringResource(R.string.profile_duration_minutes, minutes)
}

@Composable
private fun localizedThresholdQuest(
    questId: String?,
    pathId: String,
    target: Int
): String = when (questId) {
    "read" -> stringResource(R.string.threshold_quest_read, target)
    "pages" -> stringResource(R.string.threshold_quest_pages, target)
    "mark" -> when (pathId) {
        "dreamwalker" -> stringResource(R.string.threshold_quest_dreamwalker, target)
        "vanguard" -> stringResource(R.string.threshold_quest_vanguard, target)
        "nocturne" -> stringResource(R.string.threshold_quest_nocturne, target)
        "archivist" -> stringResource(R.string.threshold_quest_archivist)
        "artificer" -> stringResource(R.string.threshold_quest_artificer)
        else -> stringResource(R.string.threshold_quest_oracle, target)
    }
    else -> stringResource(R.string.threshold_quest_generic)
}

