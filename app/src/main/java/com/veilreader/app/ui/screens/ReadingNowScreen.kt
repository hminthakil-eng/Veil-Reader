package com.veilreader.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.veilreader.app.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPolicy
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
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCastle: () -> Unit
) {
    val snapshot = buildThresholdSnapshot(books)
    val current = snapshot.hero

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VeilSpacing.lg)
                .padding(top = VeilSpacing.xl, bottom = VeilSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
        ) {
        VeilReveal(delayMillis = 20, modifier = Modifier.fillMaxWidth()) {
            ThresholdHeader(hasCurrentBook = current != null)
        }

        VeilReveal(delayMillis = 90, modifier = Modifier.fillMaxWidth()) {
            if (current == null) {
                EmptyReadingState(onOpenLibrary)
            } else {
                ContinueReadingHero(
                    current = current,
                    onOpenBook = onOpenBook,
                    onOpenLibrary = onOpenLibrary
                )
            }
        }

        if (snapshot.recent.isNotEmpty()) {
            VeilReveal(delayMillis = 160, modifier = Modifier.fillMaxWidth()) {
                RecentBooksShelf(
                    books = snapshot.recent,
                    onOpenBook = onOpenBook,
                    onOpenLibrary = onOpenLibrary
                )
            }
        }

        VeilReveal(delayMillis = 220, modifier = Modifier.fillMaxWidth()) {
            ReadingPulse(profile)
        }

        VeilReveal(delayMillis = 280, modifier = Modifier.fillMaxWidth()) {
            PathSummary(
                profile = profile,
                onOpenCastle = onOpenCastle
            )
        }

        if (quests.isNotEmpty()) {
            VeilReveal(delayMillis = 340, modifier = Modifier.fillMaxWidth()) {
                WhispersSection(quests)
            }
        }
        }
    }
}

@Composable
private fun ThresholdHeader(hasCurrentBook: Boolean) {
    Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, VeilPalette.Ink.copy(alpha = 0.94f)))
            )
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 120.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "VEIL READER",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = VeilPalette.Brass
            )
            Text(
                if (hasCurrentBook) "The Library Awaits" else "Your Library Begins",
                style = MaterialTheme.typography.headlineLarge,
                color = VeilPalette.Moon
            )
            Text(
                if (hasCurrentBook) "Every book is a door. Continue where you left off."
                else "Bring a book. Build a world of your own.",
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Moon.copy(alpha = 0.84f)
            )
        }
    }
}

@Composable
private fun ContinueReadingHero(
    current: Book,
    onOpenBook: (Book) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val progress = current.progress.coerceIn(0f, 1f)
    val paper = Color(0xFFE9DDC4)
    val ink = Color(0xFF29251F)
    val secondaryInk = Color(0xFF625440)
    val shape = MaterialTheme.shapes.small
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFF2E8D3), paper, Color(0xFFE1D0AF))))
            .border(BorderStroke(1.dp, VeilPalette.Brass), shape)
            .padding(VeilSpacing.md)
    ) {
        val stacked = maxWidth < 260.dp || fontScale > 1.4f
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)) {
            Text(
                "CONTINUE READING",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = secondaryInk
            )
            if (stacked) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    HeroCover(current)
                }
                HeroDetails(current, ink, secondaryInk)
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroCover(current)
                    HeroDetails(current, ink, secondaryInk, Modifier.weight(1f))
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = ink,
                trackColor = secondaryInk.copy(alpha = 0.20f),
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
                colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = paper)
            ) {
                Text(if (progress > 0f && !current.finished) "Continue reading" else "Open book")
            }
            TextButton(
                onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = ink)
            ) { Text("Browse the archive") }
        }
    }
}

@Composable
private fun HeroCover(current: Book) {
    BookCover(
        title = current.title,
        subtitle = current.author,
        imagePath = current.coverCachePath,
        modifier = Modifier.width(88.dp).height(128.dp)
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
            current.author.ifBlank { "Unknown author" },
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

private fun heroProgressLabel(current: Book, progressPercent: Int, progress: Float): String = when {
    current.finished -> "Finished — open again anytime"
    progress <= 0f -> "Ready to begin"
    current.currentChapter.isNotBlank() && current.currentChapter != "Not started" -> "${current.currentChapter} · $progressPercent%"
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
                "Recent tomes",
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
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
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = VeilPalette.DeepBrass.copy(alpha = 0.64f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
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
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.46f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
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

