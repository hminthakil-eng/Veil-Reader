package com.veilreader.app.domain

/**
 * Shared semantic projection consumed by Cathedral realms.
 *
 * Realms remain visually independent, but they no longer reinterpret the same reading/progression
 * facts inconsistently. This object contains meaning, never UI resources or persistent state.
 */
data class VeilWorldManifestation(
    val awakened: Boolean,
    val depth: Float,
    val memoryWeight: Float,
    val progressionWeight: Float,
    val ritualReadiness: Float,
    val completedVolumes: Int,
    val rereadCycles: Int,
    val annotatedPassages: Int,
    val discoveries: Int
)

fun VeilWorldState.toManifestation(): VeilWorldManifestation {
    val memoryWeight = (
        reading.completedBookCount * 0.10f +
            reading.rereadCycleCount * 0.08f +
            reading.noteCount.coerceAtMost(20) * 0.0125f +
            reading.highlightCount.coerceAtMost(30) * 0.005f
        ).coerceIn(0f, 1f)

    val progressionWeight = (
        progression.rankIndex * 0.10f +
            progression.earnedSigilCount.coerceAtMost(8) * 0.025f +
            progression.earnedDiscoveryCount.coerceAtMost(20) * 0.015f
        ).coerceIn(0f, 1f)

    return VeilWorldManifestation(
        awakened = isAwake,
        depth = depth,
        memoryWeight = memoryWeight,
        progressionWeight = progressionWeight,
        ritualReadiness = progression.ritualFraction,
        completedVolumes = reading.completedBookCount,
        rereadCycles = reading.rereadCycleCount,
        annotatedPassages = reading.noteCount,
        discoveries = progression.earnedDiscoveryCount
    )
}
