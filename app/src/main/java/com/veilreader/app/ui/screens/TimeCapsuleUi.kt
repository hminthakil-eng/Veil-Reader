package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.domain.ReadingHistoryEvent
import com.veilreader.app.domain.ReadingHistoryEventKind
import com.veilreader.app.domain.ReadingTimeCapsule
import com.veilreader.app.ui.books.bookArtifactState
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.currentVeilTemporalPhase
import com.veilreader.app.ui.theme.grayfogAtmosphere
import java.text.DateFormat
import java.util.Date

@Composable
fun ReadingTimeCapsuleCard(
    capsule: ReadingTimeCapsule,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.34f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(VeilSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                BookCover(
                title = capsule.book.title,
                subtitle = capsule.book.author,
                imagePath = capsule.book.coverCachePath,
                artifact = bookArtifactState(capsule.book),
                modifier = Modifier
                    .width(66.dp)
                    .height(98.dp)
                )
                CapsuleSeal(
                    capsule = capsule,
                    modifier = Modifier
                        .offset(x = 8.dp, y = 8.dp)
                        .size(32.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (capsule.cycleIndex > 1) "SEALED READING RECORD · CYCLE ${capsule.cycleIndex}"
                        else "SEALED READING RECORD",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                        color = VeilPalette.Brass
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        capsule.sealCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Mist.copy(alpha = 0.52f)
                    )
                }

                Text(
                    capsule.book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = VeilPalette.Moon,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                capsule.book.author.takeIf { it.isNotBlank() }?.let { author ->
                    Text(
                        author,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    buildCapsuleMetricLine(capsule),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.64f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                capsule.completedAtEpochMs
                    ?.takeIf { capsule.exactCompletionTimeKnown && it > 0L }
                    ?.let { completedAt ->
                        Text(
                            "COMPLETED · ${formatCapsuleDate(completedAt)}",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                            color = VeilPalette.Brass.copy(alpha = 0.78f)
                        )
                    }

                Text(
                    "Open preserved history",
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Brass.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
fun ReadingTimeCapsuleSheet(
    capsule: ReadingTimeCapsule,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VeilPalette.Ink)
                .grayfogAtmosphere(
                    realm = VeilRealm.ARCHIVE,
                    seed = capsule.sealCode.hashCode(),
                    intensity = 0.90f,
                    temporalPhase = currentVeilTemporalPhase()
                )
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.md),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(VeilBackLabel("Archive"))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "SEALED · LOCAL · PRESERVED RECORD",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.85.sp),
                        color = VeilPalette.Mist.copy(alpha = 0.66f)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = VeilPalette.Archive.copy(alpha = 0.72f),
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.48f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box {
                        Canvas(Modifier.matchParentSize()) {
                            val center = Offset(size.width * 0.84f, size.height * 0.24f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        VeilPalette.Brass.copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.54f
                                ),
                                center = center,
                                radius = size.minDimension * 0.54f
                            )
                            repeat(4) { index ->
                                val inset = (index + 1) * 12.dp.toPx()
                                drawRect(
                                    color = VeilPalette.Brass.copy(
                                        alpha = 0.025f + index * 0.010f
                                    ),
                                    topLeft = Offset(inset, inset),
                                    size = androidx.compose.ui.geometry.Size(
                                        (size.width - inset * 2f).coerceAtLeast(0f),
                                        (size.height - inset * 2f).coerceAtLeast(0f)
                                    ),
                                    style = Stroke(0.65.dp.toPx())
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.padding(VeilSpacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                BookCover(
                                    title = capsule.book.title,
                                    subtitle = capsule.book.author,
                                    imagePath = capsule.book.coverCachePath,
                                    artifact = bookArtifactState(capsule.book),
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(144.dp)
                                )
                                CapsuleSeal(
                                    capsule = capsule,
                                    modifier = Modifier
                                        .offset(x = 10.dp, y = 10.dp)
                                        .size(46.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    if (capsule.cycleIndex > 1) {
                                        "TIME CAPSULE · CYCLE ${capsule.cycleIndex}"
                                    } else {
                                        "TIME CAPSULE"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.35.sp
                                    ),
                                    color = VeilPalette.Brass
                                )
                                Text(
                                    capsule.book.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = VeilPalette.Moon,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                capsule.book.author.takeIf { it.isNotBlank() }?.let { author ->
                                    Text(
                                        author,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VeilPalette.Mist.copy(alpha = 0.76f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    capsule.sealCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VeilPalette.Brass.copy(alpha = 0.82f)
                                )
                                Text(
                                    if (capsule.exactCompletionTimeKnown) {
                                        "DURABLE CYCLE RECORD · COMPLETION TIME PRESERVED"
                                    } else {
                                        "LEGACY COMPLETION STATE · EXACT COMPLETION TIME UNKNOWN"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (capsule.exactCompletionTimeKnown) {
                                        VeilPalette.Spirit.copy(alpha = 0.82f)
                                    } else {
                                        VeilPalette.Mist.copy(alpha = 0.62f)
                                    }
                                )
                            }
                        }
                    }
                }

                BrassRule(Modifier.fillMaxWidth(), strong = true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapsuleMetric(
                        label = "SESSIONS",
                        value = capsule.sessionCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    CapsuleMetric(
                        label = "ACTIVE",
                        value = formatCapsuleDuration(capsule.totalActiveMillis),
                        modifier = Modifier.weight(1f)
                    )
                    CapsuleMetric(
                        label = "PASSAGES",
                        value = capsule.highlightCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "PRESERVED READING HISTORY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                    color = VeilPalette.Brass
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(bottom = VeilSpacing.xl)
                ) {
                    if (capsule.timeline.isEmpty()) {
                        item("empty-timeline") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = VeilPalette.Archive.copy(alpha = 0.52f),
                                border = BorderStroke(
                                    1.dp,
                                    VeilPalette.BorderDark.copy(alpha = 0.68f)
                                ),
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    "This completed volume predates detailed session history. Its completion state is preserved, but no dated timeline survives.",
                                    modifier = Modifier.padding(VeilSpacing.md),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VeilPalette.Mist
                                )
                            }
                        }
                    } else {
                        items(capsule.timeline, key = { it.id }) { event ->
                            CapsuleTimelineEvent(event)
                        }
                    }

                    item("record-footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = VeilSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CapsuleMetric(
                                    label = "NOTES",
                                    value = capsule.noteCount.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                CapsuleMetric(
                                    label = "MARKS",
                                    value = capsule.bookmarkCount.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            capsule.completedAtEpochMs
                                ?.takeIf { capsule.exactCompletionTimeKnown && it > 0L }
                                ?.let { completedAt ->
                                    Text(
                                        "SEALED ON ${formatCapsuleDate(completedAt)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 0.92.sp
                                        ),
                                        color = VeilPalette.Brass.copy(alpha = 0.82f)
                                    )
                                }

                            if (!capsule.exactCompletionTimeKnown) {
                                Text(
                                    "Veil preserves the completion state but does not invent a completion timestamp that was never recorded.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeilPalette.Mist.copy(alpha = 0.74f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapsuleSeal(
    capsule: ReadingTimeCapsule,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val ringCount = (2 + capsule.cycleIndex.coerceIn(1, 4))
        val exact = capsule.exactCompletionTimeKnown
        val primary = if (exact) VeilPalette.Brass else VeilPalette.Mist

        drawCircle(
            color = VeilPalette.Ink.copy(alpha = 0.92f),
            radius = size.minDimension * 0.49f
        )
        repeat(ringCount) { index ->
            val radius = size.minDimension * (0.44f - index * 0.065f)
            if (radius > 0f) {
                drawCircle(
                    color = primary.copy(
                        alpha = (0.52f - index * 0.08f).coerceAtLeast(0.18f)
                    ),
                    radius = radius,
                    style = Stroke(
                        width = if (index == 0) 1.1.dp.toPx() else 0.65.dp.toPx()
                    )
                )
            }
        }

        drawLine(
            color = primary.copy(alpha = 0.72f),
            start = Offset(size.width * 0.28f, size.height * 0.50f),
            end = Offset(size.width * 0.72f, size.height * 0.50f),
            strokeWidth = 0.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = VeilPalette.Spirit.copy(alpha = if (exact) 0.54f else 0.22f),
            start = Offset(size.width * 0.50f, size.height * 0.28f),
            end = Offset(size.width * 0.50f, size.height * 0.72f),
            strokeWidth = 0.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun CapsuleMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.66f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = VeilPalette.Moon
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                color = VeilPalette.Brass.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun CapsuleTimelineEvent(event: ReadingHistoryEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Column(
            modifier = Modifier.width(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(Modifier.size(14.dp)) {
                drawCircle(
                    color = eventColor(event.kind),
                    radius = 3.2.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = VeilPalette.Brass.copy(alpha = 0.36f),
                    radius = 5.5.dp.toPx(),
                    center = center,
                    style = Stroke(0.8.dp.toPx())
                )
            }
            Canvas(
                Modifier
                    .width(1.dp)
                    .height(46.dp)
            ) {
                drawLine(
                    color = VeilPalette.BorderDark.copy(alpha = 0.82f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                formatCapsuleDate(event.timestampEpochMs),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass.copy(alpha = 0.78f)
            )
            Text(
                event.title,
                style = MaterialTheme.typography.titleSmall,
                color = VeilPalette.Moon
            )
            event.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.74f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun eventColor(kind: ReadingHistoryEventKind) = when (kind) {
    ReadingHistoryEventKind.ARCHIVED -> VeilPalette.Brass
    ReadingHistoryEventKind.READING_SESSION -> VeilPalette.Spirit
    ReadingHistoryEventKind.PASSAGE_PRESERVED -> VeilPalette.Moon
    ReadingHistoryEventKind.LOCATION_MARKED -> VeilPalette.MoonCrimson
    ReadingHistoryEventKind.READING_MILESTONE -> VeilPalette.Spirit
    ReadingHistoryEventKind.COMPLETED -> VeilPalette.Brass
    ReadingHistoryEventKind.LATEST_VOLUME_ACTIVITY -> VeilPalette.Brass
}

private fun formatCapsuleDate(epochMs: Long): String =
    if (epochMs <= 0L) {
        "DATE UNKNOWN"
    } else {
        DateFormat.getDateInstance(DateFormat.MEDIUM)
            .format(Date(epochMs))
            .uppercase()
    }

private fun formatCapsuleDuration(activeMillis: Long): String {
    val minutes = activeMillis.coerceAtLeast(0L) / 60_000L
    return when {
        minutes >= 60L -> {
            val hours = minutes / 60L
            val rest = minutes % 60L
            if (rest == 0L) "${hours}H" else "${hours}H ${rest}M"
        }
        minutes > 0L -> "${minutes}M"
        else -> "<1M"
    }
}

private fun buildCapsuleMetricLine(capsule: ReadingTimeCapsule): String =
    buildString {
        append(capsule.sessionCount).append(" sessions")
        append(" · ").append(formatCapsuleDuration(capsule.totalActiveMillis))
        append(" · ").append(capsule.highlightCount).append(" passages")
        if (capsule.noteCount > 0) append(" · ").append(capsule.noteCount).append(" notes")
    }
