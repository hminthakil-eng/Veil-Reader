package com.veilreader.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.data.WorldStageStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingHomeProgression
import com.veilreader.app.domain.ReadingHomeStage
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.deriveReadingHomeProgression
import com.veilreader.app.domain.ragsToRichesStageFor
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette
import kotlin.math.roundToInt

/**
 * Premium experience experiment: three clear destinations—read, archive, world.
 *
 * This is presentation-only. It uses the existing book/progress owners,
 * never awards XP, changes a locator or requests persistence from recomposition.
 * The actual Reader, Library and Castle continue to own all of their actions.
 */
@Composable
fun PremiumReadingHomeScreen(
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
    val sorted = remember(books) {
        books.sortedWith(compareByDescending<Book> { it.lastOpenedAtEpochMs }
            .thenByDescending { it.addedAtEpochMs })
    }
    val current = sorted.firstOrNull { !it.finished && it.progress > 0f }
        ?: sorted.firstOrNull { !it.finished }
    val recent = sorted.filterNot { it.id == current?.id }.take(8)
    val context = LocalContext.current
    val home = remember(profile.minutesRead, profile.booksFinished, context) {
        val live = deriveReadingHomeProgression(profile.minutesRead, profile.booksFinished)
        val savedTier = WorldStageStore(context).read().tier
        val savedStage = when (savedTier) {
            3 -> ReadingHomeStage.CASTLE
            2 -> ReadingHomeStage.HOUSE
            1 -> ReadingHomeStage.HUT
            else -> ReadingHomeStage.CAMP
        }
        // Previously earned homes remain visible if balance thresholds ever change.
        if (savedTier > ragsToRichesStageFor(live.stage).tier) {
            live.copy(stage = savedStage, nextStage = null, stageProgress = 1f, minutesToNextStage = 0)
        } else live
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(VeilMaterials.RealmBackground, VeilMaterials.Depth, VeilMaterials.RealmBackground)
            )
        ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PremiumMasthead(bookCount = books.size)
            PremiumReadHero(book = current, onOpen = { current?.let(onOpenBook) },
                onOpenLibrary = onOpenLibrary)
            PremiumPulse(profile = profile)
            if (recent.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumSectionLabel(
                        title = stringResource(R.string.premium_recent_title),
                        action = stringResource(R.string.premium_open_archive),
                        onAction = onOpenLibrary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        recent.forEach { book ->
                            PremiumBookTile(book, { onOpenBook(book) })
                        }
                    }
                }
            }
            PremiumWorldGateway(home, onOpenCastle)
            if (highlights.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(VeilMaterials.Surface)
                        .border(1.dp, VeilMaterials.Frame.copy(alpha = 0.48f), RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenMirror)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.premium_memory_title),
                            color = VeilPalette.Moon, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.premium_memory_count, highlights.size),
                            color = VeilPalette.Mist, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("↗", color = VeilPalette.Brass, fontSize = 26.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumMasthead(bookCount: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("V E I L  /  R E A D E R", style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass)
            Text(stringResource(R.string.premium_home_heading),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium, color = VeilPalette.Moon)
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50))
                .background(VeilMaterials.Surface)
                .border(1.dp, VeilMaterials.Frame.copy(alpha = 0.42f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(stringResource(R.string.premium_volume_count, bookCount),
                color = VeilPalette.Mist, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PremiumReadHero(book: Book?, onOpen: () -> Unit, onOpenLibrary: () -> Unit) {
    val radius = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 330.dp)
            .clip(radius).border(1.dp, VeilPalette.Brass.copy(alpha = 0.38f), radius)
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(listOf(
                VeilPalette.Ink.copy(alpha = 0.24f),
                VeilPalette.Ink.copy(alpha = 0.76f),
                VeilPalette.Ink.copy(alpha = 0.98f)
            ))
        ))
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.premium_featured_label),
                style = MaterialTheme.typography.labelMedium, color = VeilPalette.Brass)
            Spacer(Modifier.weight(1f))
            if (book != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BookCover(
                        title = book.title, subtitle = book.author,
                        imagePath = book.coverCachePath,
                        modifier = Modifier.width(98.dp).height(145.dp)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(book.title, style = MaterialTheme.typography.titleLarge,
                            maxLines = 3, overflow = TextOverflow.Ellipsis,
                            color = VeilPalette.Moon)
                        Text(book.author, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = VeilPalette.Mist)
                        val progress = book.progress.coerceIn(0f, 1f)
                        Text(stringResource(R.string.premium_progress, (progress * 100).roundToInt()),
                            style = MaterialTheme.typography.labelSmall, color = VeilPalette.Moon)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = VeilPalette.Brass,
                            trackColor = VeilPalette.Moon.copy(alpha = 0.20f)
                        )
                    }
                }
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = VeilPalette.Ink
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Text(stringResource(R.string.premium_resume), style = MaterialTheme.typography.titleSmall)
                }
            } else {
                Text(stringResource(R.string.premium_empty_title),
                    style = MaterialTheme.typography.headlineSmall, color = VeilPalette.Moon)
                Text(stringResource(R.string.premium_empty_body),
                    style = MaterialTheme.typography.bodyMedium, color = VeilPalette.Mist)
                Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.premium_open_archive))
                }
            }
        }
    }
}

@Composable
private fun PremiumPulse(profile: ReaderProfile) {
    val tiles = listOf(
        profile.minutesRead.coerceAtLeast(0).toString() to R.string.premium_total_minutes,
        profile.streakDays.coerceAtLeast(0).toString() to R.string.premium_streak_days,
        profile.booksFinished.coerceAtLeast(0).toString() to R.string.premium_completed_books
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        tiles.forEach { (value, label) ->
            Column(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VeilMaterials.Surface)
                    .border(1.dp, VeilMaterials.Divider, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, color = VeilPalette.Moon, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1)
                Text(stringResource(label), color = VeilPalette.Mist,
                    style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
        }
    }
}

@Composable
private fun PremiumSectionLabel(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium, color = VeilPalette.Moon)
        Text(action, modifier = Modifier.clickable(onClick = onAction).padding(8.dp),
            style = MaterialTheme.typography.labelMedium, color = VeilPalette.Brass)
    }
}

@Composable
private fun PremiumBookTile(book: Book, onOpen: () -> Unit) {
    Column(
        modifier = Modifier.width(126.dp).clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BookCover(title = book.title, subtitle = book.author, imagePath = book.coverCachePath,
            modifier = Modifier.width(126.dp).height(178.dp))
        Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
            color = VeilPalette.Moon, style = MaterialTheme.typography.bodyMedium)
        Text(book.author, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = VeilPalette.Mist, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PremiumWorldGateway(home: ReadingHomeProgression, onOpenWorld: () -> Unit) {
    val stage = when (home.stage) {
        ReadingHomeStage.CAMP -> R.string.premium_world_camp
        ReadingHomeStage.HUT -> R.string.premium_world_hut
        ReadingHomeStage.HOUSE -> R.string.premium_world_house
        ReadingHomeStage.CASTLE -> R.string.premium_world_castle
    }
    val radius = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxWidth().heightIn(min = 228.dp)
        .clip(radius).border(1.dp, VeilMaterials.Frame.copy(alpha = 0.42f), radius)) {
        Image(painter = painterResource(R.drawable.grayfog_keep_v3),
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(
            Brush.horizontalGradient(listOf(
                VeilPalette.Ink.copy(alpha = 0.97f),
                VeilPalette.Ink.copy(alpha = 0.82f),
                VeilPalette.Ink.copy(alpha = 0.12f)
            ))
        ))
        PremiumWorldProgressionArt(
            stage = home.stage,
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxWidth(0.50f).height(212.dp).padding(end = 8.dp)
        )
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.premium_world_eyebrow),
                color = VeilPalette.Brass, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(stage), color = VeilPalette.Moon,
                style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.premium_world_story),
                modifier = Modifier.widthIn(max = 270.dp),
                color = VeilPalette.Mist, style = MaterialTheme.typography.bodyMedium,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (home.nextStage != null) {
                LinearProgressIndicator(
                    progress = { home.stageProgress },
                    modifier = Modifier.width(200.dp).height(4.dp),
                    color = VeilPalette.Brass,
                    trackColor = VeilPalette.Moon.copy(alpha = 0.2f)
                )
            }
            Spacer(Modifier.height(2.dp))
            OutlinedButton(onClick = onOpenWorld,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VeilPalette.Moon)) {
                Text(stringResource(R.string.premium_enter_world))
            }
        }
    }
}
