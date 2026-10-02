package com.veilreader.rd

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingCalendarTest {
    @Test fun sessionsAggregateByLocalDay() {
        val zone = ZoneId.of("UTC")
        val day = Instant.parse("2026-10-02T10:00:00Z").toEpochMilli()
        val result = ReadingCalendarAggregator.aggregate(
            listOf(
                ReadingSessionEvent(day, 60_000),
                ReadingSessionEvent(day + 3_600_000, 120_000)
            ),
            zone
        )
        assertEquals(1, result.size)
        assertEquals(180_000, result.single().activeMillis)
        assertEquals(2, result.single().sessions)
    }
}
