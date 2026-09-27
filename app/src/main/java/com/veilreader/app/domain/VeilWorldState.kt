package com.veilreader.app.domain

/**
 * Derived progression facts exposed to the living world.
 *
 * GameRepository remains the mutation owner. This snapshot is read-only and lets world surfaces
 * react to progression without gaining authority to award XP, advance ranks or unlock discoveries.
 */
data class ProgressionSnapshot(
    val level: Int,
    val xp: Int,
    val xpForNextLevel: Int,
    val streakDays: Int,
    val pathId: String,
    val pathName: String,
    val rankIndex: Int,
    val rankName: String,
    val ritualProgress: Int,
    val ritualTarget: Int,
    val earnedSigilCount: Int,
    val earnedDiscoveryCount: Int
) {
    val ritualFraction: Float
        get() = if (ritualTarget <= 0) 0f
        else (ritualProgress.toFloat() / ritualTarget).coerceIn(0f, 1f)
}

fun ReaderProfile.toProgressionSnapshot(): ProgressionSnapshot = ProgressionSnapshot(
    level = level.coerceAtLeast(0),
    xp = xp.coerceAtLeast(0),
    xpForNextLevel = xpForNextLevel.coerceAtLeast(0),
    streakDays = streakDays.coerceAtLeast(0),
    pathId = path.id,
    pathName = path.name,
    rankIndex = rankIndex.coerceAtLeast(0),
    rankName = rankName,
    ritualProgress = ritualProgress.coerceAtLeast(0),
    ritualTarget = ritualTarget.coerceAtLeast(0),
    earnedSigilCount = earnedSigils.size,
    earnedDiscoveryCount = earnedDiscoveries.size
)

/**
 * Unified read-only contract for Cathedral surfaces.
 *
 * [reading] answers what actually happened in the Reader. [progression] answers where the reader
 * stands in Veil's game systems. Neither side can mutate the other through this model.
 */
data class VeilWorldState(
    val reading: ReadingWorldSnapshot,
    val progression: ProgressionSnapshot
) {
    val isAwake: Boolean
        get() = reading.hasReadingHistory || progression.level > 0 || progression.rankIndex > 0

    val depth: Float
        get() {
            val readingDepth = (
                reading.completedBookCount * 0.12f +
                    reading.rereadCycleCount * 0.10f +
                    reading.noteCount.coerceAtMost(20) * 0.0125f
                ).coerceIn(0f, 0.65f)
            val progressionDepth = (
                progression.rankIndex * 0.07f +
                    progression.earnedDiscoveryCount.coerceAtMost(12) * 0.015f
                ).coerceIn(0f, 0.35f)
            return (readingDepth + progressionDepth).coerceIn(0f, 1f)
        }
}

fun deriveVeilWorldState(
    reading: ReadingWorldSnapshot,
    profile: ReaderProfile
): VeilWorldState = VeilWorldState(
    reading = reading,
    progression = profile.toProgressionSnapshot()
)
