package com.veilreader.app.domain

/**
 * Stable discovery conditions.
 *
 * A discovery is a historical achievement, not a live badge. Once a condition is observed and
 * persisted by GameRepository, later changes such as a broken streak must never hide it again.
 */
object VeiledDiscoveryPolicy {
    const val PATIENT_FLAME = "patient_flame"
    const val MARGINALIA_GATE = "marginalia_gate"
    const val DEEP_SHELF = "deep_shelf"
    const val LONG_WATCH = "long_watch"
    const val VEIL_THINS = "veil_thins"
    const val UNNAMED_CHAMBER = "unnamed_chamber"

    val allIds: Set<String> = linkedSetOf(
        PATIENT_FLAME,
        MARGINALIA_GATE,
        DEEP_SHELF,
        LONG_WATCH,
        VEIL_THINS,
        UNNAMED_CHAMBER
    )

    fun currentlyQualified(
        profile: ReaderProfile,
        highlightCount: Int
    ): Set<String> = buildSet {
        if (profile.streakDays >= 7 && profile.minutesRead >= 600) add(PATIENT_FLAME)
        if (highlightCount >= 10 && profile.pagesRead >= 1_000) add(MARGINALIA_GATE)
        if (profile.booksFinished >= 10 && profile.rankIndex >= 1) add(DEEP_SHELF)
        if (profile.minutesRead >= 3_000) add(LONG_WATCH)
        if (profile.earnedSigils.size >= 4) add(VEIL_THINS)
        if (profile.rankIndex >= 3 && profile.earnedSigils.size >= 5) add(UNNAMED_CHAMBER)
    }

    fun mergeHistorical(
        persisted: Set<String>,
        currentlyQualified: Set<String>
    ): Set<String> =
        (persisted.filterTo(linkedSetOf()) { it in allIds } + currentlyQualified)
            .filterTo(linkedSetOf()) { it in allIds }
}
