package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.ReadingDaypart
import com.veilreader.app.domain.ReadingSignature
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import kotlin.math.roundToInt

@Composable
internal fun ReadingSignaturePanel(
    signature: ReadingSignature,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Reading Signature from ${signature.recordedSessionCount} recorded sessions"
            },
        shape = MaterialTheme.shapes.small,
        color = VeilPalette.Archive.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.30f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "READING SIGNATURE · FACTUAL",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.25.sp),
                        color = VeilPalette.Brass
                    )
                    Text(
                        "Patterns in the recorded sessions",
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon
                    )
                }
                Text(
                    "${signature.recordedSessionCount} sessions",
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Mist.copy(alpha = 0.72f)
                )
            }

            BrassRule(Modifier.fillMaxWidth())

            if (signature.recordedSessionCount == 0 && signature.completionCycleCount == 0) {
                Text(
                    "No durable session or completion history is available yet. Veil will not invent a reading pattern from missing data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist
                )
                return@Column
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 520.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
                    ) {
                        SignatureClock(
                            signature = signature,
                            modifier = Modifier.size(140.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SignatureFact(
                                label = "MEDIAN ACTIVE SESSION",
                                value = signature.medianActiveSessionMillis
                                    ?.let(::formatSignatureDuration)
                                    ?: "NO MEASURED DURATION"
                            )
                            SignatureFact(
                                label = "RECORDED ACTIVE DAYS",
                                value = signature.activeDayCount.toString()
                            )
                            SignatureFact(
                                label = "VOLUMES TOUCHED",
                                value = signature.booksTouchedCount.toString()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SignatureClock(
                            signature = signature,
                            modifier = Modifier.size(152.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SignatureFact(
                                label = "MEDIAN ACTIVE SESSION",
                                value = signature.medianActiveSessionMillis
                                    ?.let(::formatSignatureDuration)
                                    ?: "NO MEASURED DURATION"
                            )
                            SignatureFact(
                                label = "RECORDED ACTIVE DAYS",
                                value = signature.activeDayCount.toString()
                            )
                            SignatureFact(
                                label = "VOLUMES TOUCHED",
                                value = signature.booksTouchedCount.toString()
                            )
                        }
                    }
                }
            }

            SignatureMetricBand(
                metrics = listOf(
                    "PACED TURNS / H" to (
                        signature.pacedPageTurnsPerActiveHour
                            ?.let(::formatSignatureRate)
                            ?: "—"
                    ),
                    "MARKS / H" to (
                        signature.highlightEventsPerActiveHour
                            ?.let(::formatSignatureRate)
                            ?: "—"
                    ),
                    "NOTE EVENTS / MARK EVENT" to (
                        signature.notesPerHighlightEvent
                            ?.let(::formatSignatureRatio)
                            ?: "—"
                    )
                )
            )

            SignatureMetricBand(
                metrics = listOf(
                    "COMPLETION CYCLES" to signature.completionCycleCount.toString(),
                    "REREAD CYCLES" to signature.rereadCycleCount.toString(),
                    "REREAD SHARE" to (
                        signature.rereadCycleShare
                            ?.let(::formatSignaturePercent)
                            ?: "—"
                    )
                )
            )

            Text(
                "Rates appear only after at least 15 minutes of recorded active time. Note-event/mark-event is an event ratio, not the share of current highlights with notes and not a personality score.",
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.66f)
            )

            Text(
                "Daypart uses ${signature.timedSessionCount}/${signature.recordedSessionCount} sessions with recorded start times, mapped through the current device timezone (${signature.timezoneId}). Historical original timezones were not stored.",
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.52f)
            )
        }
    }
}

@Composable
private fun SignatureClock(
    signature: ReadingSignature,
    modifier: Modifier = Modifier
) {
    val maxCount = signature.daypartSessionCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Box(
        modifier = modifier.semantics {
            contentDescription = buildString {
                append("Session starts by daypart. ")
                ReadingDaypart.entries.forEachIndexed { index, daypart ->
                    if (index > 0) append(", ")
                    append(daypartLabel(daypart))
                    append(" ")
                    append(signature.daypartSessionCounts[daypart] ?: 0)
                }
                signature.leadingDaypart?.let {
                    append(". Most starts: ").append(daypartLabel(it))
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val radius = size.minDimension * 0.39f
            val brass = VeilPalette.Brass

            drawCircle(
                color = brass.copy(alpha = 0.30f),
                radius = radius,
                center = center,
                style = Stroke(1.dp.toPx())
            )
            drawCircle(
                color = brass.copy(alpha = 0.12f),
                radius = radius * 0.66f,
                center = center,
                style = Stroke(0.8.dp.toPx())
            )
            drawLine(
                color = brass.copy(alpha = 0.12f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = brass.copy(alpha = 0.12f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 0.8.dp.toPx()
            )

            val anchors = mapOf(
                ReadingDaypart.MORNING to Offset(center.x, center.y - radius),
                ReadingDaypart.AFTERNOON to Offset(center.x + radius, center.y),
                ReadingDaypart.EVENING to Offset(center.x, center.y + radius),
                ReadingDaypart.NIGHT to Offset(center.x - radius, center.y)
            )
            anchors.forEach { (daypart, point) ->
                val count = signature.daypartSessionCounts[daypart] ?: 0
                val strength = count.toFloat() / maxCount.toFloat()
                val leading = daypart == signature.leadingDaypart
                drawCircle(
                    color = if (leading) {
                        VeilPalette.Brass.copy(alpha = 0.72f)
                    } else {
                        VeilPalette.Spirit.copy(alpha = 0.20f + strength * 0.34f)
                    },
                    radius = (2.6f + strength * 3.4f).dp.toPx(),
                    center = point
                )
                if (leading) {
                    drawCircle(
                        color = VeilPalette.Brass.copy(alpha = 0.42f),
                        radius = 10.dp.toPx(),
                        center = point,
                        style = Stroke(0.9.dp.toPx())
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                signature.leadingDaypart?.let(::daypartLabel) ?: "NO CLEAR",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.80.sp),
                color = if (signature.leadingDaypart != null) {
                    VeilPalette.Brass
                } else {
                    VeilPalette.Mist.copy(alpha = 0.62f)
                },
                textAlign = TextAlign.Center
            )
            Text(
                if (signature.leadingDaypart != null) "MOST STARTS" else "TIME LEAD",
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Mist.copy(alpha = 0.48f),
                textAlign = TextAlign.Center
            )
        }

        Text(
            "M",
            modifier = Modifier.align(Alignment.TopCenter),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.48f)
        )
        Text(
            "A",
            modifier = Modifier.align(Alignment.CenterEnd),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.48f)
        )
        Text(
            "E",
            modifier = Modifier.align(Alignment.BottomCenter),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.48f)
        )
        Text(
            "N",
            modifier = Modifier.align(Alignment.CenterStart),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.48f)
        )
    }
}

@Composable
private fun SignatureFact(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
            color = VeilPalette.Mist.copy(alpha = 0.58f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = VeilPalette.Moon
        )
    }
}

@Composable
private fun SignatureMetricBand(
    metrics: List<Pair<String, String>>
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                metrics.forEach { (label, value) ->
                    SignatureMetric(
                        label = label,
                        value = value,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                metrics.forEach { (label, value) ->
                    SignatureMetric(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SignatureMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        VeilPalette.DeepBrass.copy(alpha = 0.13f),
                        VeilPalette.Ink.copy(alpha = 0.30f)
                    )
                ),
                shape = MaterialTheme.shapes.extraSmall
            )
            .border(
                BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.70f)),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = VeilPalette.Moon
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.55.sp),
            color = VeilPalette.Brass.copy(alpha = 0.72f),
            maxLines = 2
        )
    }
}

private fun daypartLabel(daypart: ReadingDaypart): String = when (daypart) {
    ReadingDaypart.MORNING -> "MORNING"
    ReadingDaypart.AFTERNOON -> "AFTERNOON"
    ReadingDaypart.EVENING -> "EVENING"
    ReadingDaypart.NIGHT -> "NIGHT"
}

private fun formatSignatureDuration(millis: Long): String {
    val minutes = (millis.coerceAtLeast(0L) / 60_000L)
    return when {
        minutes >= 60L -> {
            val hours = minutes / 60L
            val rest = minutes % 60L
            if (rest == 0L) "${hours}h" else "${hours}h ${rest}m"
        }
        minutes > 0L -> "${minutes}m"
        else -> "<1m"
    }
}

private fun formatSignatureRate(value: Float): String =
    if (!value.isFinite()) "—" else {
        val rounded = (value * 10f).roundToInt() / 10f
        if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    }

private fun formatSignatureRatio(value: Float): String =
    if (!value.isFinite()) "—"
    else {
        val rounded = (value.coerceAtLeast(0f) * 100f).roundToInt() / 100f
        if (rounded % 1f == 0f) "${rounded.toInt()}×" else "${rounded}×"
    }

private fun formatSignaturePercent(value: Float): String =
    if (!value.isFinite()) "—"
    else "${(value.coerceAtLeast(0f) * 100f).roundToInt()}%"
