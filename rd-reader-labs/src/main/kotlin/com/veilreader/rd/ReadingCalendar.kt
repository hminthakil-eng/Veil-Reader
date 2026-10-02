package com.veilreader.rd

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReadingSessionEvent(
    val startedAtEpochMs: Long,
    val activeMillis: Long
)

data class ReadingDaySummary(
    val date: LocalDate,
    val activeMillis: Long,
    val sessions: Int
)

object ReadingCalendarAggregator {
    fun aggregate(
        sessions: List<ReadingSessionEvent>,
        zoneId: ZoneId
    ): List<ReadingDaySummary> =
        sessions
            .filter { it.startedAtEpochMs >= 0 && it.activeMillis >= 0 }
            .groupBy {
                Instant.ofEpochMilli(it.startedAtEpochMs).atZone(zoneId).toLocalDate()
            }
            .map { (date, values) ->
                ReadingDaySummary(
                    date = date,
                    activeMillis = values.sumOf { it.activeMillis },
                    sessions = values.size
                )
            }
            .sortedBy { it.date }
}
