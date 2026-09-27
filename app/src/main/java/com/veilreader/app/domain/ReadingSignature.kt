package com.veilreader.app.domain

import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToLong

enum class ReadingDaypart {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

/**
 * A factual summary of reading behavior Veil can derive from durable session/cycle records.
 *
 * It deliberately avoids personality labels, motivation claims, or semantic interpretation.
 * Historical session timestamps do not store their original timezone, so daypart analysis maps
 * epoch timestamps through the currently supplied device zone.
 */
data class ReadingSignature(
    val recordedSessionCount: Int,
    val timedSessionCount: Int,
    val measuredActiveSessionCount: Int,
    val recordedActiveMillis: Long,
    val medianActiveSessionMillis: Long?,
    val activeDayCount: Int,
    val booksTouchedCount: Int,
    val firstSessionAtEpochMs: Long?,
    val latestSessionAtEpochMs: Long?,
    val daypartSessionCounts: Map<ReadingDaypart, Int>,
    val leadingDaypart: ReadingDaypart?,
    val timezoneId: String,
    val pacedPageTurnCount: Int,
    val highlightEventCount: Int,
    val noteEventCount: Int,
    val pacedPageTurnsPerActiveHour: Float?,
    val highlightEventsPerActiveHour: Float?,
    val notesPerHighlightEvent: Float?,
    val completionCycleCount: Int,
    val rereadCycleCount: Int,
    val rereadCycleShare: Float?
) {
    companion object {
        fun empty(zoneId: ZoneId): ReadingSignature = ReadingSignature(
            recordedSessionCount = 0,
            timedSessionCount = 0,
            measuredActiveSessionCount = 0,
            recordedActiveMillis = 0L,
            medianActiveSessionMillis = null,
            activeDayCount = 0,
            booksTouchedCount = 0,
            firstSessionAtEpochMs = null,
            latestSessionAtEpochMs = null,
            daypartSessionCounts = ReadingDaypart.values().toList().associateWith { 0 },
            leadingDaypart = null,
            timezoneId = zoneId.id,
            pacedPageTurnCount = 0,
            highlightEventCount = 0,
            noteEventCount = 0,
            pacedPageTurnsPerActiveHour = null,
            highlightEventsPerActiveHour = null,
            notesPerHighlightEvent = null,
            completionCycleCount = 0,
            rereadCycleCount = 0,
            rereadCycleShare = null
        )
    }
}

fun deriveReadingSignature(
    sessions: List<ReadingSessionSnapshot>,
    cycles: List<ReadingCycleRecord>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    nowEpochMs: Long = System.currentTimeMillis()
): ReadingSignature {
    val safeNow = nowEpochMs.coerceAtLeast(0L)
    val recordedSessions = sessions
    val timedSessions = recordedSessions
        .filter { it.startedAtEpochMs > 0L && it.startedAtEpochMs <= safeNow }
        .sortedBy { it.startedAtEpochMs }

    if (recordedSessions.isEmpty() && cycles.isEmpty()) {
        return ReadingSignature.empty(zoneId)
    }

    val activeDurations = recordedSessions
        .map { it.activeMillis.coerceAtLeast(0L) }
        .filter { it > 0L }
        .sorted()

    val totalActiveMillis = recordedSessions.sumOf { it.activeMillis.coerceAtLeast(0L) }
    val totalPageTurns = recordedSessions.sumOf { it.pacedPageTurns.coerceAtLeast(0) }
    val totalHighlightEvents = recordedSessions.sumOf { it.highlightCount.coerceAtLeast(0) }
    val totalNoteEvents = recordedSessions.sumOf { it.noteCount.coerceAtLeast(0) }

    val daypartCounts = ReadingDaypart.values().toList().associateWith { 0 }.toMutableMap()
    val activeDays = linkedSetOf<java.time.LocalDate>()

    timedSessions.forEach { session ->
        val localDateTime = Instant
            .ofEpochMilli(session.startedAtEpochMs)
            .atZone(zoneId)
        activeDays += localDateTime.toLocalDate()
        val daypart = readingDaypartForHour(localDateTime.hour)
        daypartCounts[daypart] = daypartCounts.getValue(daypart) + 1
    }

    val leadingDaypart = if (timedSessions.size >= 3) {
        val maxCount = daypartCounts.values.maxOrNull() ?: 0
        val leaders = daypartCounts.filterValues { it == maxCount && it > 0 }.keys
        leaders.singleOrNull()
    } else {
        null
    }

    val activeHours = totalActiveMillis / 3_600_000f
    val sufficientRateWindow = totalActiveMillis >= 15L * 60L * 1000L

    val validCycles = cycles.filter {
        it.completedAtEpochMs > 0L && it.completedAtEpochMs <= safeNow
    }
    val rereadCycles = validCycles.count { it.cycleIndex > 1 }

    return ReadingSignature(
        recordedSessionCount = recordedSessions.size,
        timedSessionCount = timedSessions.size,
        measuredActiveSessionCount = activeDurations.size,
        recordedActiveMillis = totalActiveMillis,
        medianActiveSessionMillis = medianMillis(activeDurations),
        activeDayCount = activeDays.size,
        booksTouchedCount = recordedSessions
            .mapNotNull { session ->
                session.bookId
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }
            .toSet()
            .size,
        firstSessionAtEpochMs = timedSessions.firstOrNull()?.startedAtEpochMs,
        latestSessionAtEpochMs = timedSessions.lastOrNull()?.startedAtEpochMs,
        daypartSessionCounts = daypartCounts.toMap(),
        leadingDaypart = leadingDaypart,
        timezoneId = zoneId.id,
        pacedPageTurnCount = totalPageTurns,
        highlightEventCount = totalHighlightEvents,
        noteEventCount = totalNoteEvents,
        pacedPageTurnsPerActiveHour = if (sufficientRateWindow && activeHours > 0f) {
            totalPageTurns / activeHours
        } else {
            null
        },
        highlightEventsPerActiveHour = if (sufficientRateWindow && activeHours > 0f) {
            totalHighlightEvents / activeHours
        } else {
            null
        },
        notesPerHighlightEvent = if (totalHighlightEvents > 0) {
            (totalNoteEvents.toFloat() / totalHighlightEvents.toFloat()).coerceAtLeast(0f)
        } else {
            null
        },
        completionCycleCount = validCycles.size,
        rereadCycleCount = rereadCycles,
        rereadCycleShare = if (validCycles.isNotEmpty()) {
            rereadCycles.toFloat() / validCycles.size.toFloat()
        } else {
            null
        }
    )
}

fun readingDaypartForHour(hour: Int): ReadingDaypart {
    val safeHour = ((hour % 24) + 24) % 24
    return when (safeHour) {
        in 5..11 -> ReadingDaypart.MORNING
        in 12..16 -> ReadingDaypart.AFTERNOON
        in 17..21 -> ReadingDaypart.EVENING
        else -> ReadingDaypart.NIGHT
    }
}

private fun medianMillis(sortedDurations: List<Long>): Long? {
    if (sortedDurations.isEmpty()) return null
    val middle = sortedDurations.size / 2
    return if (sortedDurations.size % 2 == 1) {
        sortedDurations[middle]
    } else {
        ((sortedDurations[middle - 1].toDouble() + sortedDurations[middle].toDouble()) / 2.0)
            .roundToLong()
    }
}
