package com.veilreader.app.domain

private const val AFTERGLOW_FULL_MS = 6L * 60L * 60L * 1000L
private const val AFTERGLOW_FADE_MS = 72L * 60L * 60L * 1000L

/**
 * Ritual metadata is descriptive evidence beside the authoritative rank.
 * Invalid metadata is ignored instead of repairing or changing progression.
 */
fun validateRitualAftermath(
    record: RitualAftermathRecord?,
    currentPathId: String,
    currentRankIndex: Int,
    rankCount: Int
): RitualAftermathRecord? {
    val value = record ?: return null
    if (rankCount <= 0) return null
    if (value.pathId != currentPathId) return null
    if (value.fromRankIndex < 0) return null
    if (value.toRankIndex != value.fromRankIndex + 1) return null
    if (value.toRankIndex !in 1 until rankCount) return null
    if (value.toRankIndex > currentRankIndex) return null
    if (value.sealedAtEpochMs <= 0L) return null
    return value
}

/**
 * A sealed advancement remains permanently recorded, while its visible afterglow fades.
 * Future clock-skew timestamps deliberately produce no glow.
 */
fun ritualAfterglowIntensity(
    record: RitualAftermathRecord?,
    nowEpochMs: Long = System.currentTimeMillis()
): Float {
    val value = record ?: return 0f
    if (nowEpochMs <= 0L || value.sealedAtEpochMs <= 0L) return 0f
    if (value.sealedAtEpochMs > nowEpochMs) return 0f

    val age = nowEpochMs - value.sealedAtEpochMs
    if (age <= AFTERGLOW_FULL_MS) return 1f
    if (age >= AFTERGLOW_FADE_MS) return 0f

    val fadeWindow = AFTERGLOW_FADE_MS - AFTERGLOW_FULL_MS
    return (1f - (age - AFTERGLOW_FULL_MS).toFloat() / fadeWindow.toFloat())
        .coerceIn(0f, 1f)
}
