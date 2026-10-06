package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.veilreader.app.ui.theme.withVeilContentScript
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.thresholdAtmosphereIntensityFor
import com.veilreader.app.ui.theme.thresholdLayoutPolicyFor
import com.veilreader.app.ui.theme.shouldAbbreviateThresholdEntry
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
    onOpenCastle: () -> Unit,
    onOpenMirror: () -> Unit = {}
) {
    val snapshot = buildThresholdSnapshot(books)
    val current = snapshot.hero
    val windowSize = currentVeilWindowSizeDp()
    val thresholdAdaptiveClass = adaptiveClassFor(windowSize.width)
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
            VeilArchitecturalPair(
                primaryFraction = com.veilreader.app.ui.theme.VeilProportion.ThresholdPrimary,
                spacing = VeilSpacing.xs,
                primary = {
                    VeilReveal(delayMillis = 10, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.padding(
                                horizontal = VeilSpacing.Cluster,
                                vertical = if (current != null) VeilSpacing.Micro else VeilSpacing.Inline
                            )
                        ) {
                            ThresholdHeader(
                                bookCount = books.size,
                                hasCurrentBook = current != null,
                                headerHeightDp = thresholdLayout.headerHeightDp
                            )
                        }
                    }
                },
                secondary = {
                    VeilReveal(delayMillis = 70, modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(horizontal = thresholdLayout.horizontalPaddingDp.dp)) {
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
                }
            )

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

            if (highlights.isNotEmpty()) {
                Spacer(Modifier.height(VeilSpacing.xl))
                VeilReveal(delayMillis = 200, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.padding(
                            horizontal = thresholdLayout.horizontalPaddingDp.dp
                        )
                    ) {
                        MirrorPortalCard(
                            savedTraceCount = highlights.size,
                            onOpenMirror = onOpenMirror
                        )
                    }
                }
            }

            Spacer(Modifier.height(VeilSpacing.xl))
            VeilReveal(delayMillis = 230, modifier = Modifier.fillMaxWidth()) {
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
    val highContrast = com.veilreader.app.ui.theme.LocalVeilHighContrast.current
    val windowSize = currentVeilWindowSizeDp()
    val compactReturningEntry =
        hasCurrentBook &&
            windowSize.width < com.veilreader.app.ui.theme.VeilComposition.ArchitecturalPairMinWidthDp
    // Returning phone users came here to resume reading. Preserve the doorway,
    // but do not make them re-read the cinematic prologue on every visit.
    val abbreviatedEntry = shouldAbbreviateThresholdEntry(
        hasCurrentBook = hasCurrentBook,
        widthDp = windowSize.width,
        heightDp = windowSize.height,
        fontScale = LocalDensity.current.fontScale
    )
    val approachHeightDp = if (compactReturningEntry) {
        headerHeightDp.coerceAtMost(
            com.veilreader.app.ui.theme.VeilComposition.ThresholdActiveApproachMaxHeightDp
        )
    } else {
        headerHeightDp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(VeilPalette.Ink)
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (highContrast) 0.18f else 1f,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to VeilPalette.Ink.copy(alpha = 0.48f),
                    0.38f to VeilPalette.Ink.copy(alpha = 0.18f),
                    0.70f to VeilPalette.Ink.copy(alpha = 0.92f),
                    1f to VeilPalette.Ink
                )
            )
        )
        ThresholdDepthField(bookCount = bookCount, modifier = Modifier.matchParentSize())

        // Copy determines height. At 200% the doorway grows instead of painting over the seal.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (abbreviatedEntry) 0.dp else approachHeightDp.dp)
                .padding(
                    horizontal = VeilSpacing.Content,
                    vertical = if (abbreviatedEntry) VeilSpacing.Inline else VeilSpacing.Content
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (abbreviatedEntry) VeilSpacing.Micro else VeilSpacing.Cluster
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThresholdLiminalSeal(
                    modifier = Modifier.size(40.dp),
                    waking = bookCount.coerceIn(0, 12) / 12f
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    VeilMicroLabel(text = stringResource(R.string.app_name), strong = true)
                    VeilMicroLabel(
                        text = stringResource(R.string.threshold_grayfog_archive),
                        modifier = if (abbreviatedEntry) {
                            Modifier.semantics { heading() }
                        } else {
                            Modifier
                        },
                        color = VeilPalette.Mist
                    )
                }
            }
            if (!abbreviatedEntry) {
                Spacer(Modifier.height(VeilSpacing.Section))
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
                    modifier = Modifier.semantics { heading() },
                    color = VeilPalette.Moon,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    color = VeilPalette.Moon,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.widthIn(max = 540.dp)
                )
                BrassRule(Modifier.width(112.dp), strong = true)
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
private fun ThresholdLiminalSeal(
    modifier: Modifier = Modifier,
    waking: Float
) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val glow = (0.22f + waking * 0.28f).coerceIn(0f, 0.56f)
        val outer = size.minDimension * 0.44f
        val inner = size.minDimension * 0.25f

        drawCircle(
            color = VeilPalette.Brass.copy(alpha = 0.34f + waking * 0.12f),
            radius = outer,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
        )
        drawCircle(
            color = VeilPalette.Moon.copy(alpha = glow),
            radius = inner,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(0.8.dp.toPx())
        )

        repeat(8) { index ->
            val angle = Math.toRadians((index * 45.0) - 90.0)
            val start = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * inner,
                y = center.y + kotlin.math.sin(angle).toFloat() * inner
            )
            val end = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * outer,
                y = center.y + kotlin.math.sin(angle).toFloat() * outer
            )
            drawLine(
                color = VeilPalette.Brass.copy(alpha = 0.24f + waking * 0.08f),
                start = start,
                end = end,
                strokeWidth = 0.7.dp.toPx()
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VeilPalette.Brass.copy(alpha = 0.11f + waking * 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = outer
            ),
            radius = outer,
            center = center
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
    val progress = current.progress.coerceIn(0f, 1f)
    val fontScale = LocalDensity.current.fontScale
    val shellShape = MaterialTheme.shapes.extraSmall

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(shellShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        VeilPalette.LightSurface,
                        VeilPalette.ReaderPaper,
                        VeilPalette.LightElevated
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    VeilPalette.LightBrass.copy(alpha = 0.82f)
                ),
                shellShape
            )
    ) {
        // Keep the physical book beside its identity at normal compact widths;
        // large text still owns a full column rather than being squeezed.
        val coverBudget = maxWidth.value - 36f - 16f -
            com.veilreader.app.ui.theme.VeilComposition.ResumeIdentityMinWidthDp * fontScale.coerceAtLeast(1f)
        val objectWidth = if (coverBudget >= com.veilreader.app.ui.theme.VeilComposition.ThresholdCoverMinObjectWidthDp)
            coverWidthDp.coerceAtMost(coverBudget) else coverWidthDp
        val objectHeight = coverHeightDp * objectWidth / coverWidthDp
        val stacked = (maxWidth.value - 36f - objectWidth - 16f) /
            fontScale.coerceAtLeast(1f) < com.veilreader.app.ui.theme.VeilComposition.ResumeIdentityMinWidthDp

        ThresholdParchmentTexture(
            modifier = Modifier.matchParentSize()
        )

        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.20f
        )

        @Composable
        fun ResumeAction() {
            Surface(
                onClick = { onOpenBook(current) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = shellShape,
                color = Color.Transparent,
                contentColor = VeilPalette.LightInk,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    VeilPalette.LightBrass.copy(alpha = 0.60f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = VeilSpacing.Content,
                            end = VeilSpacing.Inline,
                            top = VeilSpacing.Micro,
                            bottom = VeilSpacing.Micro
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(
                            if (progress > 0f && !current.finished) {
                                R.string.threshold_return_volume
                            } else {
                                R.string.threshold_open_volume
                            }
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge
                    )

                    Text(
                        if (androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl) "‹" else "›",
                        modifier = Modifier.padding(horizontal = VeilSpacing.sm),
                        style = MaterialTheme.typography.titleMedium,
                        color = VeilPalette.LightInk
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(
                horizontal = VeilSpacing.Content,
                vertical = VeilSpacing.Cluster
            ),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.Cluster)
        ) {
            if (stacked) {
                HeroDetails(
                    current = current,
                    ink = VeilPalette.LightInk,
                    secondaryInk = VeilPalette.LightMist,
                    readingAction = { ResumeAction() },
                    includeMetadata = false
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    HeroCover(
                        current,
                        artifactMemory,
                        objectWidth,
                        objectHeight
                    )
                }
                HeroMetadata(current, VeilPalette.LightMist)

            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroCover(
                        current,
                        artifactMemory,
                        objectWidth,
                        objectHeight
                    )
                    HeroDetails(
                        current = current,
                        ink = VeilPalette.LightInk,
                        secondaryInk = VeilPalette.LightMist,
                        maxTitleLines = 4,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = VeilPalette.LightBrass,
                trackColor = VeilPalette.BorderLight.copy(alpha = 0.58f),
                drawStopIndicator = {}
            )

            if (!stacked) ResumeAction()
            val progressInscription = heroProgressLabel(current, progress)
            Text(
                progressInscription,
                style = MaterialTheme.typography.labelMedium.withVeilContentScript(progressInscription),
                color = VeilPalette.LightMist,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

        }
    }
}

@Composable
private fun ThresholdParchmentTexture(
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val ink = VeilPalette.LightInk
        val brass = VeilPalette.LightBrass

        repeat(9) { index ->
            val y = size.height * ((index + 1f) / 10f)
            val inset = size.width * (0.018f + (index % 3) * 0.008f)
            drawLine(
                color = ink.copy(alpha = 0.022f + (index % 2) * 0.006f),
                start = Offset(inset, y),
                end = Offset(size.width - inset, y + (index % 3 - 1) * 0.7f),
                strokeWidth = 0.55.dp.toPx()
            )
        }

        repeat(6) { index ->
            val x = size.width * ((index + 1f) / 7f)
            drawLine(
                color = brass.copy(alpha = 0.016f),
                start = Offset(x, size.height * 0.03f),
                end = Offset(x + ((index % 2) * 2 - 1) * 1.2f, size.height * 0.97f),
                strokeWidth = 0.45.dp.toPx()
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    VeilPalette.LightSurface.copy(alpha = 0.16f),
                    Color.Transparent,
                    VeilPalette.LightInk.copy(alpha = 0.035f)
                )
            )
        )

        val corner = 12.dp.toPx()
        val cornerInk = VeilPalette.LightBrass.copy(alpha = 0.34f)
        drawLine(
            cornerInk,
            Offset(0f, corner),
            Offset(corner, 0f),
            0.8.dp.toPx()
        )
        drawLine(
            cornerInk,
            Offset(size.width - corner, 0f),
            Offset(size.width, corner),
            0.8.dp.toPx()
        )
        drawLine(
            cornerInk,
            Offset(0f, size.height - corner),
            Offset(corner, size.height),
            0.8.dp.toPx()
        )
        drawLine(
            cornerInk,
            Offset(size.width - corner, size.height),
            Offset(size.width, size.height - corner),
            0.8.dp.toPx()
        )
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
    modifier: Modifier = Modifier,
    readingAction: (@Composable () -> Unit)? = null,
    includeMetadata: Boolean = true,
    maxTitleLines: Int = 3
) {
    val displayTitle = bookDisplayTitle(current.title)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
        Text(
            displayTitle,
            style = MaterialTheme.typography.titleLarge.withVeilContentScript(displayTitle),
            color = ink,
            maxLines = maxTitleLines,
            overflow = TextOverflow.Ellipsis
        )
        readingAction?.invoke()
        if (includeMetadata) HeroMetadata(current, secondaryInk)
    }
}

@Composable
private fun HeroMetadata(current: Book, secondaryInk: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
        Text(
            current.author.ifBlank { stringResource(R.string.common_unknown_author) },
            style = MaterialTheme.typography.bodyMedium.withVeilContentScript(current.author.ifBlank { stringResource(R.string.common_unknown_author) }),
            color = secondaryInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        current.seriesName?.takeIf { it.isNotBlank() }?.let { series ->
            Text(series, style = MaterialTheme.typography.labelMedium.withVeilContentScript(series), color = secondaryInk,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun heroProgressLabel(current: Book, progress: Float): String {
    val percent = rememberVeilPercentFormatter()(progress)
    return when {
    current.finished -> stringResource(R.string.threshold_progress_finished_reopen)
    progress <= 0f -> stringResource(R.string.threshold_progress_ready)
    current.currentChapter.isNotBlank() && current.currentChapter != "Not started" ->
        stringResource(R.string.threshold_progress_chapter, current.currentChapter, percent)
    else -> stringResource(R.string.threshold_progress_complete, percent)
    }
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
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                VeilMicroLabel(
                    text = stringResource(R.string.threshold_recent_eyebrow),
                    strong = true
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
    val displayTitle = bookDisplayTitle(book.title)
    Surface(
        onClick = { onOpenBook(book) },
        modifier = Modifier.width(itemWidthDp.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BookCover(
                    title = displayTitle,
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

                Canvas(
                    Modifier
                        .matchParentSize()
                        .padding(horizontal = 2.dp)
                ) {
                    val hairline = VeilPalette.Brass.copy(alpha = 0.28f)
                    drawLine(
                        color = hairline,
                        start = Offset(0f, size.height - 1.dp.toPx()),
                        end = Offset(size.width, size.height - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawCircle(
                        color = VeilPalette.MoonCrimson.copy(alpha = 0.74f),
                        radius = 2.2.dp.toPx(),
                        center = Offset(size.width - 6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Text(
                displayTitle,
                style = MaterialTheme.typography.titleSmall.withVeilContentScript(displayTitle),
                color = VeilPalette.Moon,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            VeilMicroLabel(
                text = recentBookStatus(book),
                color = VeilPalette.Mist.copy(alpha = 0.80f),
                modifier = Modifier.fillMaxWidth()
            )

            if (book.progress > 0f && !book.finished) {
                LinearProgressIndicator(
                    progress = { book.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = VeilPalette.Brass,
                    trackColor = VeilPalette.BorderDark.copy(alpha = 0.42f),
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
private fun recentBookStatus(book: Book): String {
    val progress = book.progress.coerceIn(0f, 1f)
    val formatPercent = rememberVeilPercentFormatter()
    return when {
        book.finished -> stringResource(R.string.book_detail_finished)
        progress <= 0f -> stringResource(R.string.book_detail_not_started)
        else -> stringResource(R.string.book_detail_percent_read_text, formatPercent(progress))
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
    val renderedBody = if (whisper.kind == ThresholdWhisperKind.PRESERVED_PASSAGE) {
        "“$displayBody”"
    } else {
        displayBody
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Ink.copy(alpha = 0.40f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.24f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VeilMicroLabel(
                text = stringResource(
                    when (whisper.kind) {
                        ThresholdWhisperKind.PRESERVED_PASSAGE -> R.string.threshold_whisper_preserved
                        ThresholdWhisperKind.READING_PROMPT -> R.string.threshold_whisper_optional
                    }
                ),
                strong = true
            )
            Text(
                displayTitle,
                style = MaterialTheme.typography.titleMedium.withVeilContentScript(displayTitle),
                color = VeilPalette.Moon,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                renderedBody,
                style = MaterialTheme.typography.bodyMedium.withVeilContentScript(renderedBody),
                color = VeilPalette.Mist.copy(alpha = 0.86f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            displayDetail?.let { detail ->
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall.withVeilContentScript(detail),
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
            VeilMicroLabel(
                text = stringResource(R.string.profile_reading_record),
                modifier = Modifier.weight(1f),
                strong = true
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
                modifier = Modifier.weight(0.9f)
            )
            ReadingPulseValue(
                value = formatReadingTime(profile.minutesRead),
                label = stringResource(R.string.threshold_stat_reading),
                modifier = Modifier.weight(1.2f)
            )
            ReadingPulseValue(
                value = "${profile.booksFinished}",
                label = stringResource(R.string.profile_stat_finished),
                modifier = Modifier.weight(0.9f)
            )
        }
    }
}

@Composable
private fun ReadingPulseValue(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Clip
        )
        VeilMicroLabel(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyReadingState(onOpenLibrary: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.90f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.46f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeilSpacing.lg)
        ) {
            GrayfogOrnamentFrame(
                modifier = Modifier.matchParentSize(),
                strength = 0.50f
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                VeilMicroLabel(
                    text = stringResource(R.string.threshold_first_eyebrow),
                    strong = true
                )
                Text(
                    stringResource(R.string.threshold_first_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(R.string.threshold_first_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist.copy(alpha = 0.86f)
                )
                BrassRule(Modifier.width(112.dp))
                Surface(
                    onClick = onOpenLibrary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = VeilPalette.ReaderPaper,
                    contentColor = VeilPalette.InkOnPaper,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.82f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VeilSpacing.md, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.threshold_enter_library),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            if (androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl) "‹" else "›",
                            style = MaterialTheme.typography.titleMedium,
                            color = VeilPalette.DeepBrass
                        )
                    }
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



@Composable
private fun MirrorPortalCard(
    savedTraceCount: Int,
    onOpenMirror: () -> Unit
) {
    Surface(
        onClick = onOpenMirror,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.small,
        color = VeilPalette.Archive.copy(alpha = 0.82f),
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.36f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(VeilSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 72.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF050608),
                                Color(0xFF151A21),
                                Color(0xFF08090B)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.58f)),
                        RoundedCornerShape(percent = 50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "◌",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VeilPalette.Brass.copy(alpha = 0.82f)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    stringResource(R.string.mirror_portal_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass
                )
                Text(
                    stringResource(R.string.mirror_portal_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(R.string.mirror_portal_body, savedTraceCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.86f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                stringResource(R.string.mirror_portal_action),
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Brass
            )
        }
    }
}
