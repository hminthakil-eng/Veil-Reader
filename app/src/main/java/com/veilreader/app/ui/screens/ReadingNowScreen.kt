package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPolicy
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
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCastle: () -> Unit
) {
    val snapshot = buildThresholdSnapshot(books)
    val current = snapshot.hero

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg)
            .padding(top = VeilSpacing.xl, bottom = VeilSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xxl)
    ) {
        ThresholdHeader(hasCurrentBook = current != null)

        if (current == null) {
            EmptyReadingState(onOpenLibrary)
        } else {
            ContinueReadingHero(
                current = current,
                onOpenBook = onOpenBook,
                onOpenLibrary = onOpenLibrary
            )
        }

        if (snapshot.recent.isNotEmpty()) {
            RecentBooksShelf(
                books = snapshot.recent,
                onOpenBook = onOpenBook,
                onOpenLibrary = onOpenLibrary
            )
        }

        ReadingPulse(profile)

        PathSummary(
            profile = profile,
            onOpenCastle = onOpenCastle
        )

        if (quests.isNotEmpty()) {
            WhispersSection(quests)
        }
    }
}

@Composable
private fun ThresholdHeader(hasCurrentBook: Boolean) {
    ScreenHeader(
        eyebrow = "The Threshold",
        title = if (hasCurrentBook) "Return to your book" else "Build your private library",
        subtitle = if (hasCurrentBook) {
            "One gesture back into the story. The archive stays quiet until you ask for it."
        } else {
            "Import an EPUB or PDF and begin your private archive. Reading data remains on this device."
        }
    )
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
    val shape = MaterialTheme.shapes.large

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surfaceVariant.copy(alpha = 0.94f),
                        colors.surface.copy(alpha = 0.995f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, colors.primary.copy(alpha = 0.28f)),
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
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

        Spacer(Modifier.height(VeilSpacing.xs))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
        Text(
            heroProgressLabel(current, progressPercent, progress),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = VeilSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            Button(
                onClick = { onOpenBook(current) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(if (progress > 0f && !current.finished) "Continue" else "Open book")
            }
            TextButton(
                onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text("Library")
            }
        }
    }
}

private fun heroProgressLabel(current: Book, progressPercent: Int, progress: Float): String = when {
    current.finished -> "Finished — open again anytime"
    progress <= 0f -> "Ready to begin"
    current.currentChapter.isNotBlank() && current.currentChapter != "Not started" -> current.currentChapter
    else -> "$progressPercent% complete"
}

@Composable
private fun RecentBooksShelf(
    books: List<Book>,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Recent books",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = onOpenLibrary,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("View all") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            books.forEach { book ->
                RecentBookCard(book = book, onOpenBook = onOpenBook)
            }
        }
    }
}

@Composable
private fun RecentBookCard(book: Book, onOpenBook: (Book) -> Unit) {
    Card(
        onClick = { onOpenBook(book) },
        modifier = Modifier.width(124.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
            BookCover(
                title = book.title,
                subtitle = book.author,
                imagePath = book.coverCachePath,
                modifier = Modifier.width(112.dp).height(164.dp)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun recentBookStatus(book: Book): String {
    val progress = book.progress.coerceIn(0f, 1f)
    return when {
        book.finished -> "Finished"
        progress <= 0f -> "Not started"
        else -> "${(progress * 100).toInt()}% read"
    }
}

@Composable
private fun ReadingPulse(profile: ReaderProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.md)
        ) {
            if (maxWidth < 420.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    ReadingPulseValue("${profile.streakDays}", "day streak", Modifier.fillMaxWidth())
                    ReadingPulseValue(formatReadingTime(profile.minutesRead), "reading", Modifier.fillMaxWidth())
                    ReadingPulseValue("${profile.booksFinished}", "finished", Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReadingPulseValue("${profile.streakDays}", "day streak", Modifier.weight(1f))
                    ReadingPulseValue(formatReadingTime(profile.minutesRead), "reading", Modifier.weight(1f))
                    ReadingPulseValue("${profile.booksFinished}", "finished", Modifier.weight(1f))
                }
            }
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
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PathSummary(profile: ReaderProfile, onOpenCastle: () -> Unit) {
    val target = profile.ritualTarget.coerceAtLeast(1)
    val ritualProgress = (profile.ritualProgress.toFloat() / target).coerceIn(0f, 1f)

    MysteryCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "${profile.path.name} · ${profile.rankName}",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(
                onClick = onOpenCastle,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Castle")
            }
        }

        Spacer(Modifier.height(VeilSpacing.xs))
        LinearProgressIndicator(
            progress = { ritualProgress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
        Text(
            "Ritual ${profile.ritualProgress}/$target · Level ${profile.level}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WhispersSection(quests: List<Quest>) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        Text("Whispers", style = MaterialTheme.typography.titleLarge)
        Text(
            "Optional reading prompts. Nothing is lost if you ignore them today.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        quests.take(3).forEach { quest -> QuestRow(quest) }
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f))
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
                    if (complete) "Done" else "+${quest.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
            )
        }
    }
}

@Composable
private fun EmptyReadingState(onOpenLibrary: () -> Unit) {
    MysteryCard(Modifier.fillMaxWidth()) {
        Text("Your first book is one tap away", style = MaterialTheme.typography.titleLarge)
        Text(
            "Open the Library to import an EPUB or PDF. Once you begin reading, this screen becomes your fastest way back in.",
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

private fun formatReadingTime(minutes: Int): String = when {
    minutes >= 6000 -> "${minutes / 60}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}