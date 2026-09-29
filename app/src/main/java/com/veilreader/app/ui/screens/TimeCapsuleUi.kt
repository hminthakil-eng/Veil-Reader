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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.ReadingHistoryEvent
import com.veilreader.app.domain.ReadingHistoryEventKind
import com.veilreader.app.domain.ReadingMilestoneKind
import com.veilreader.app.domain.ReadingTimeCapsule
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.veilContentTextStyle
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
                        if (capsule.cycleIndex > 1) {
                            stringResource(R.string.capsule_sealed_record_cycle, capsule.cycleIndex)
                        } else {
                            stringResource(R.string.capsule_sealed_record)
                        },
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
                    style = veilContentTextStyle(MaterialTheme.typography.titleMedium, capsule.book.title),
                    color = VeilPalette.Moon,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                capsule.book.author.takeIf { it.isNotBlank() }?.let { author ->
                    Text(
                        author,
                        style = veilContentTextStyle(MaterialTheme.typography.bodySmall, author),
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
                            stringResource(R.string.capsule_completed, formatCapsuleDate(completedAt)),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                            color = VeilPalette.Brass.copy(alpha = 0.78f)
                        )
                    }

                Text(
                    stringResource(R.string.capsule_open_history),
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
                        if (capsule.cycleIndex > 1) {
                            stringResource(R.string.capsule_sheet_sealed_cycle, capsule.cycleIndex)
                        } else {
                            stringResource(R.string.capsule_sheet_sealed)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                        color = VeilPalette.Brass
                    )
                    Text(
                        capsule.book.title,
                        style = veilContentTextStyle(MaterialTheme.typography.headlineSmall, capsule.book.title),
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
                    label = stringResource(R.string.capsule_metric_sessions),
                    value = capsule.sessionCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                CapsuleMetric(
                    label = stringResource(R.string.capsule_metric_active),
                    value = formatCapsuleDuration(capsule.totalActiveMillis),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CapsuleMetric(
                    label = stringResource(R.string.capsule_metric_passages),
                    value = capsule.highlightCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                CapsuleMetric(
                    label = stringResource(R.string.capsule_metric_notes_marks),
                    value = "${capsule.noteCount} / ${capsule.bookmarkCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                stringResource(R.string.capsule_reading_history),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.15.sp),
                color = VeilPalette.Brass
            )

            if (capsule.timeline.isEmpty()) {
                Text(
                    stringResource(R.string.capsule_empty_history),
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
                        stringResource(R.string.capsule_completion_unknown),
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
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.capsule_close_record))
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
            val eventTitle = capsuleEventTitle(event)
            val eventDetail = capsuleEventDetail(event)
            Text(
                eventTitle,
                style = veilContentTextStyle(MaterialTheme.typography.titleSmall, eventTitle),
                color = VeilPalette.Moon
            )
            eventDetail?.takeIf { it.isNotBlank() }?.let { detail ->
                Text(
                    detail,
                    style = veilContentTextStyle(MaterialTheme.typography.bodySmall, detail),
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

@Composable
private fun capsuleEventTitle(event: ReadingHistoryEvent): String =
    when (event.kind) {
        ReadingHistoryEventKind.ARCHIVED -> stringResource(R.string.capsule_event_archived)
        ReadingHistoryEventKind.READING_SESSION -> stringResource(R.string.capsule_event_reading_session)
        ReadingHistoryEventKind.PASSAGE_PRESERVED ->
            if (event.annotated) {
                stringResource(R.string.capsule_event_annotated_passage)
            } else {
                stringResource(R.string.capsule_event_passage_preserved)
            }
        ReadingHistoryEventKind.LOCATION_MARKED -> stringResource(R.string.capsule_event_location_marked)
        ReadingHistoryEventKind.READING_MILESTONE -> when (event.milestoneKind) {
            ReadingMilestoneKind.FIRST_OPENED -> stringResource(R.string.capsule_event_first_opened)
            ReadingMilestoneKind.PROGRESS_25 -> stringResource(R.string.capsule_event_reached_25)
            ReadingMilestoneKind.PROGRESS_50 -> stringResource(R.string.capsule_event_reached_50)
            ReadingMilestoneKind.PROGRESS_75 -> stringResource(R.string.capsule_event_reached_75)
            null -> stringResource(R.string.capsule_event_milestone)
        }
        ReadingHistoryEventKind.COMPLETED -> stringResource(R.string.capsule_event_completed)
        ReadingHistoryEventKind.LATEST_VOLUME_ACTIVITY -> stringResource(R.string.capsule_event_latest_activity)
    }

@Composable
private fun capsuleEventDetail(event: ReadingHistoryEvent): String? =
    when (event.kind) {
        ReadingHistoryEventKind.READING_SESSION -> {
            val parts = buildList {
                event.activeMillis?.let { active ->
                    add(stringResource(R.string.capsule_active_duration, formatCapsuleDuration(active)))
                }
                if (event.pacedPageTurns > 0) {
                    add(pluralStringResource(R.plurals.capsule_paced_turns_count, event.pacedPageTurns, event.pacedPageTurns))
                }
                if (event.highlightEventCount > 0) {
                    add(pluralStringResource(R.plurals.capsule_highlight_events_count, event.highlightEventCount, event.highlightEventCount))
                }
                if (event.noteEventCount > 0) {
                    add(pluralStringResource(R.plurals.capsule_note_events_count, event.noteEventCount, event.noteEventCount))
                }
            }
            parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        }
        ReadingHistoryEventKind.PASSAGE_PRESERVED -> event.excerpt
        ReadingHistoryEventKind.LOCATION_MARKED -> event.locationLabel
        else -> null
    }

@Composable
private fun formatCapsuleDate(epochMs: Long): String =
    if (epochMs <= 0L) {
        stringResource(R.string.capsule_date_unknown)
    } else {
        DateFormat.getDateInstance(DateFormat.MEDIUM)
            .format(Date(epochMs))
            .uppercase()
    }

@Composable
private fun formatCapsuleDuration(activeMillis: Long): String {
    val minutes = activeMillis.coerceAtLeast(0L) / 60_000L
    return when {
        minutes >= 60L -> {
            val hours = (minutes / 60L).toInt()
            val rest = (minutes % 60L).toInt()
            if (rest == 0) {
                stringResource(R.string.capsule_duration_hours, hours)
            } else {
                stringResource(R.string.capsule_duration_hours_minutes, hours, rest)
            }
        }
        minutes > 0L -> stringResource(R.string.capsule_duration_minutes, minutes.toInt())
        else -> stringResource(R.string.capsule_duration_less_than_minute)
    }
}

@Composable
private fun buildCapsuleMetricLine(capsule: ReadingTimeCapsule): String {
    val parts = buildList {
        add(pluralStringResource(R.plurals.capsule_sessions_count, capsule.sessionCount, capsule.sessionCount))
        add(formatCapsuleDuration(capsule.totalActiveMillis))
        add(pluralStringResource(R.plurals.capsule_passages_count, capsule.highlightCount, capsule.highlightCount))
        if (capsule.noteCount > 0) {
            add(pluralStringResource(R.plurals.capsule_notes_count, capsule.noteCount, capsule.noteCount))
        }
    }
    return parts.joinToString(" · ")
}
