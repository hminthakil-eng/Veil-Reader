package com.veilreader.app.ui.reader.tts

internal fun normalizedTtsSleepMinutes(minutes: Int): Int? =
    when {
        minutes == 0 -> 0
        minutes in MIN_TTS_SLEEP_MINUTES..MAX_TTS_SLEEP_MINUTES -> minutes
        else -> null
    }

internal fun ttsSleepDeadline(nowEpochMs: Long, minutes: Int): Long {
    require(minutes in MIN_TTS_SLEEP_MINUTES..MAX_TTS_SLEEP_MINUTES)
    require(nowEpochMs >= 0L)
    return Math.addExact(nowEpochMs, Math.multiplyExact(minutes.toLong(), 60_000L))
}

internal const val MIN_TTS_SLEEP_MINUTES = 5
internal const val MAX_TTS_SLEEP_MINUTES = 180
