package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.ui.theme.VeilSpacing

/**
 * Home is deliberately practical: current book first, useful progress second, game/lore third.
 * The Castle can feel mysterious without forcing the reader to decode the UI.
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
    val current = books
        .filterNot { it.finished }
        .ifEmpty { books }
        .maxByOrNull { it.lastOpenedAtEpochMs.takeIf { time -> time > 0L } ?: it.addedAtEpochMs }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg)
            .padding(top = VeilSpacing.xl, bottom = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        ScreenHeader(
            eyebrow = "Veil Reader",
            title = if (current == null) "Start your library" else "Continue reading",
            subtitle = if (current == null) {
                "Import an EPUB or PDF. Your books and reading data stay on this device."
            } else {
                "Your current book, reading progress, and Path—without getting in the way."
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

        QuickStats(profile)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            OutlinedButton(
                onClick = onOpenLibrary,
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text("Library")
            }
            FilledTonalButton(
                onClick = onOpenCastle,
                modifier = Modifier.weight(1f).heightIn(min = 50.dp)
            ) {
                Text("Castle")
            }
        }

        PathProgressCard(profile)

        if (quests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                SectionTitle(
                    eyebrow = "Today",
                    title = "Small goals, no pressure"
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
                        colors.primaryContainer.copy(alpha = 0.72f),
                        colors.surfaceVariant.copy(alpha = 0.86f),
                        colors.surface.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.72f)),
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
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f))
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
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
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
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
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
private fun QuickStats(profile: ReaderProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        SectionTitle(eyebrow = "Reading life", title = "At a glance")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
        ) {
            StatTile(
                value = "${profile.streakDays}",
                label = "day streak",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = formatReadingTime(profile.minutesRead),
                label = "read",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "${profile.booksFinished}",
                label = "finished",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 84.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.sm, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PathProgressCard(profile: ReaderProfile) {
    val target = profile.ritualTarget.coerceAtLeast(1)
    val ritualProgress = (profile.ritualProgress.toFloat() / target).coerceIn(0f, 1f)
    val xpTarget = profile.xpForNextLevel.coerceAtLeast(1)
    val xpProgress = (profile.xp.toFloat() / xpTarget).coerceIn(0f, 1f)

    MysteryCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    profile.path.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    profile.rankName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f)
            ) {
                Text(
                    "Lv ${profile.level}",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(VeilSpacing.xs))
        ProgressLabel("Ritual", "${profile.ritualProgress}/$target")
        LinearProgressIndicator(
            progress = { ritualProgress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        )
        Text(
            ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(VeilSpacing.xs))
        ProgressLabel("Experience", "${profile.xp}/$xpTarget XP")
        LinearProgressIndicator(
            progress = { xpProgress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
    }
}

@Composable
private fun ProgressLabel(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
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
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
            )
        }
    }
}

@Composable
private fun EmptyReadingState(onOpenLibrary: () -> Unit) {
    MysteryCard(Modifier.fillMaxWidth()) {
        Text("Your first book is one tap away", style = MaterialTheme.typography.titleLarge)
        Text(
            "Veil Reader supports EPUB and PDF now. Import a book and it will appear here for quick return.",
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
