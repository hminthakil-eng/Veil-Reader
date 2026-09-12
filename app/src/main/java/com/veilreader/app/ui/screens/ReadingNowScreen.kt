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
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing

/**
 * The Threshold is the doorway into reading, not a dashboard.
 *
 * The current book owns the visual hierarchy. Progression remains present but quiet: Path state and
 * quests live below the book and never compete with the primary continue-reading action.
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
    val current = books.filterNot { it.finished }.ifEmpty { books }
        .maxByOrNull { it.lastOpenedAtEpochMs.takeIf { time -> time > 0L } ?: it.addedAtEpochMs }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xl)
    ) {
        ScreenHeader(
            eyebrow = "The Threshold",
            title = if (current == null) "Your library waits" else "Return beyond the veil",
            subtitle = if (current == null) {
                "Bring a book into Veil Reader and the first door will open."
            } else {
                "One quiet step returns you to where the story left you."
            }
        )

        if (current == null) {
            EmptyThreshold(onOpenLibrary)
        } else {
            ThresholdBookHero(current = current, onOpenBook = onOpenBook)
        }

        PathWhisper(profile = profile)

        if (quests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                SectionTitle(
                    eyebrow = "Quiet traces",
                    title = "What today has revealed"
                )
                quests.take(3).forEach { quest ->
                    QuestTrace(quest)
                }
            }
        }

        OutlinedButton(
            onClick = onOpenCastle,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text("Walk deeper into the Castle")
        }
    }
}

@Composable
private fun ThresholdBookHero(current: Book, onOpenBook: (Book) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val progressPercent = (current.progress.coerceIn(0f, 1f) * 100).toInt()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.20f),
                        colors.surfaceVariant.copy(alpha = 0.88f),
                        colors.surface.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.75f)),
                shape
            )
            .padding(VeilSpacing.xl)
    ) {
        val wide = maxWidth >= 560.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThresholdCover(current)
                ThresholdBookDetails(
                    current = current,
                    progressPercent = progressPercent,
                    onOpenBook = onOpenBook,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
            ) {
                ThresholdCover(current)
                ThresholdBookDetails(
                    current = current,
                    progressPercent = progressPercent,
                    onOpenBook = onOpenBook,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ThresholdCover(current: Book) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 164.dp, height = 220.dp)
                .clip(MaterialTheme.shapes.large)
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
            modifier = Modifier
                .width(142.dp)
                .height(206.dp)
        )
    }
}

@Composable
private fun ThresholdBookDetails(
    current: Book,
    progressPercent: Int,
    onOpenBook: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Text(
                if (current.progress > 0f) "PASSAGE OPEN" else "UNOPENED VOLUME",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
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

        Spacer(Modifier.height(VeilSpacing.xs))

        LinearProgressIndicator(
            progress = { current.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                when {
                    current.finished -> "Journey complete"
                    current.progress <= 0f -> "The first page awaits"
                    else -> "$progressPercent% through the journey"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                current.format.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Button(
            onClick = { onOpenBook(current) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(top = VeilSpacing.xs)
        ) {
            Text(if (current.progress > 0f) "Cross the Threshold" else "Open the first page")
        }
    }
}

@Composable
private fun PathWhisper(profile: ReaderProfile) {
    val target = profile.ritualTarget.coerceAtLeast(1)
    val progress = (profile.ritualProgress.toFloat() / target).coerceIn(0f, 1f)

    MysteryCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${profile.path.name} · ${profile.rankName}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Your Path remembers the reading you have done.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${profile.ritualProgress}/$target",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VeilSpacing.xs),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
        Text(
            ReadingPolicy.ritualDescription(profile.path.id, profile.rankIndex),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestTrace(quest: Quest) {
    val target = quest.target.coerceAtLeast(1)
    val complete = quest.progress >= target
    val progress = (quest.progress.toFloat() / target).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                MaterialTheme.shapes.medium
            )
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.sm)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(VeilSpacing.sm))
                Text(
                    if (complete) "REVEALED" else "+${quest.xpReward} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
            )
        }
    }
}

@Composable
private fun EmptyThreshold(onOpenLibrary: () -> Unit) {
    MysteryCard(Modifier.fillMaxWidth()) {
        Text(
            "✦",
            fontSize = 38.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Text("The first door is still sealed", style = MaterialTheme.typography.titleLarge)
        Text(
            "Import an EPUB or PDF. Nothing else is required: your books remain local, and reading works offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onOpenLibrary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(top = VeilSpacing.xs)
        ) {
            Text("Choose a book from the Grand Library")
        }
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
