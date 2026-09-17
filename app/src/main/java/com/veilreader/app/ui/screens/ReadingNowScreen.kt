package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.ui.theme.VeilSpacing

/**
 * The Threshold keeps the publication dominant and lets the world sit behind reading.
 * Resume first, recent books second, progression only as quiet context.
 */
@Composable
fun ReadingNowScreen(
    books: List<Book>,
    profile: ReaderProfile,
    quests: List<Quest>,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCastle: () -> Unit
) {
    val current = selectCurrentBook(books)
    val recent = recentBooks(books, excludingBookId = current?.id, limit = 6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg)
            .padding(top = VeilSpacing.xl, bottom = VeilSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        ScreenHeader(
            eyebrow = "Veil Reader",
            title = if (current == null) "Begin at the Threshold" else "The Threshold",
            subtitle = if (current == null) {
                "Import an EPUB or PDF to begin. Your library and reading history remain on this device."
            } else {
                "Return to the book that matters now, with the rest of your library close behind."
            }
        )

        if (current == null) {
            EmptyReadingState(onOpenLibrary)
        } else {
            ContinueReadingHero(
                current = current,
                onOpenBook = onOpenBook,
                onOpenLibrary = onOpenLibrary
            )
        }

        if (recent.isNotEmpty()) {
            RecentBooksShelf(
                books = recent,
                onOpenBook = onOpenBook,
                onOpenLibrary = onOpenLibrary
            )
        }

        ReadingLifeStrip(profile)
        PathSignalCard(profile = profile, onOpenCastle = onOpenCastle)

        if (quests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                SectionTitle(
                    eyebrow = "Optional",
                    title = "Whispers"
                )
                Text(
                    "Small reading prompts when you want them. Nothing here interrupts the book.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                quests.take(3).forEach { quest -> QuestRow(quest) }
            }
        }
    }
}

@Composable
private fun ContinueReadingHero(
    current: Book,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val progress = current.progress.coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()
    val shape = MaterialTheme.shapes.extraLarge

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = 0.68f),
                        colors.surfaceVariant.copy(alpha = 0.78f),
                        colors.surface.copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.64f)),
                shape
            )
            .padding(VeilSpacing.xl)
    ) {
        val wide = maxWidth >= 590.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroCover(current)
                HeroDetails(
                    current = current,
                    progressPercent = progressPercent,
                    progress = progress,
                    onOpenBook = onOpenBook,
                    onOpenLibrary = onOpenLibrary,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeroCover(current)
                HeroDetails(
                    current = current,
                    progressPercent = progressPercent,
                    progress = progress,
                    onOpenBook = onOpenBook,
                    onOpenLibrary = onOpenLibrary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeroCover(current: Book) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 190.dp, height = 250.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )
        BookCover(
            title = current.title,
            subtitle = current.author,
            imagePath = current.coverCachePath,
            modifier = Modifier.width(152.dp).height(222.dp)
        )
    }
}

@Composable
private fun HeroDetails(
    current: Book,
    progressPercent: Int,
    progress: Float,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
        ) {
            Text(
                if (progress > 0f) "$progressPercent% READ" else "NEW BOOK",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            current.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            current.author.ifBlank { "Unknown author" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
        Text(
            when {
                current.finished -> "Finished"
                progress <= 0f -> "Ready to begin"
                current.currentChapter.isNotBlank() && current.currentChapter != "Not started" -> current.currentChapter
                else -> "$progressPercent% complete"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = VeilSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onOpenBook(current) },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
            ) {
                Text(if (progress > 0f) "Continue" else "Start reading")
            }
            TextButton(
                onClick = onOpenLibrary,
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Text("Library")
            }
        }
    }
}

@Composable
private fun RecentBooksShelf(
    books: List<Book>,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            SectionTitle(eyebrow = "Library", title = "Recent books")
            TextButton(onClick = onOpenLibrary, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("View all")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            books.forEach { book ->
                RecentBookCard(book = book, onOpenBook = onOpenBook)
            }
        }
    }
}

@Composable
private fun RecentBookCard(book: Book, onOpenBook: (Book) -> Unit) {
    val progress = book.progress.coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()

    Surface(
        onClick = { onOpenBook(book) },
        modifier = Modifier.width(126.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(vertical = VeilSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            BookCover(
                title = book.title,
                subtitle = book.author,
                imagePath = book.coverCachePath,
                modifier = Modifier.width(112.dp).height(164.dp)
            )
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                when {
                    book.finished -> "Finished"
                    progress > 0f -> "$progressPercent% read"
                    else -> "Not started"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReadingLifeStrip(profile: ReaderProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReadingMetric(value = "${profile.streakDays}", label = "day streak", modifier = Modifier.weight(1f))
            ReadingMetric(value = formatReadingTime(profile.minutesRead), label = "read", modifier = Modifier.weight(1f))
            ReadingMetric(value = "${profile.booksFinished}", label = "finished", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadingMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun PathSignalCard(profile: ReaderProfile, onOpenCastle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VeilSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PathSigil(Modifier.size(44.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    profile.path.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${profile.rankName} · Level ${profile.level}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onOpenCastle, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Castle")
            }
        }
    }
}

@Composable
private fun PathSigil(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.secondary
    Canvas(modifier) {
        drawCircle(
            color = tint.copy(alpha = 0.10f),
            radius = size.minDimension * 0.5f
        )
        val stroke = Stroke(
            width = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val w = size.width
        val h = size.height
        val sigil = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.61f, h * 0.39f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.61f, h * 0.61f)
            lineTo(w * 0.50f, h * 0.88f)
            lineTo(w * 0.39f, h * 0.61f)
            lineTo(w * 0.12f, h * 0.50f)
            lineTo(w * 0.39f, h * 0.39f)
            close()
        }
        drawPath(sigil, color = tint, style = stroke)
        drawCircle(color = tint, radius = w * 0.055f)
    }
}

@Composable
private fun QuestRow(quest: Quest) {
    val target = quest.target.coerceAtLeast(1)
    val complete = quest.progress >= target
    val progress = (quest.progress.toFloat() / target).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (complete) "DONE" else "+${quest.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (complete) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
            )
        }
    }
}

@Composable
private fun EmptyReadingState(onOpenLibrary: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = 0.54f),
                        colors.surface.copy(alpha = 0.96f)
                    )
                )
            )
            .border(BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.56f)), shape)
            .padding(VeilSpacing.xl)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
            Text("Bring your first book through the Veil", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Choose an EPUB or PDF from Android Files. Veil Reader will keep the publication and your reading data private on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = VeilSpacing.xs)
            ) {
                Text("Open Library")
            }
        }
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.35.sp),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

private fun formatReadingTime(minutes: Int): String = when {
    minutes >= 6000 -> "${minutes / 60}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}
