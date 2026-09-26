package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.ReadingHistoryEvent
import com.veilreader.app.domain.ReadingHistoryEventKind
import com.veilreader.app.domain.ReadingTimeCapsule
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
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
            BookCover(
                title = capsule.book.title,
                subtitle = capsule.book.author,
                imagePath = capsule.book.coverCachePath,
                artifact = bookArtifactState(capsule.book),
                modifier = Modifier
                    .width(66.dp)
                    .height(98.dp)
            )

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
                    "Open preserved history →",
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Brass.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingTimeCapsuleSheet(
    capsule: ReadingTimeCapsule,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VeilPalette.Ink,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = VeilPalette.Brass.copy(alpha = 0.45f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VeilSpacing.lg)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCover(
                    title = capsule.book.title,
                    subtitle = capsule.book.author,
                    imagePath = capsule.book.coverCachePath,
                    artifact = bookArtifactState(capsule.book),
                    modifier = Modifier
                        .width(82.dp)
                        .height(122.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (capsule.cycleIndex > 1) "TIME CAPSULE · CYCLE ${capsule.cycleIndex} · SEALED"
                        else "TIME CAPSULE · SEALED",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                        color = VeilPalette.Brass
                    )
                    Text(
                        capsule.book.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = VeilPalette.Moon,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        capsule.sealCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Mist.copy(alpha = 0.58f)
                    )
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
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CapsuleMetric(
                    label = "PASSAGES",
                    value = capsule.highlightCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                CapsuleMetric(
                    label = "NOTES / MARKS",
                    value = "${capsule.noteCount} / ${capsule.bookmarkCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "READING HISTORY",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                color = VeilPalette.Brass
            )

            if (capsule.timeline.isEmpty()) {
                Text(
                    "This completed volume predates detailed session history. Its completion state is preserved, but no dated timeline survives.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(capsule.timeline, key = { it.id }) { event ->
                        CapsuleTimelineEvent(event)
                    }
                }
            }

            if (!capsule.exactCompletionTimeKnown) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = VeilPalette.Archive.copy(alpha = 0.55f),
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.BorderDark.copy(alpha = 0.72f)
                    ),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        "Completion is preserved as a state. This archive version does not store the exact instant the book first reached completion, so Veil does not invent one.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.76f)
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 44.dp)
            ) {
                Text("Close record")
            }
        }
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
