package com.veilreader.app.domain

/**
 * One-way discovery policy for the Veiled folio.
 *
 * Eligibility is derived from truthful reading history. Persistence is owned by GameRepository:
 * once an id has been earned it is unioned with later eligibility and never removed.
 */
object VeiledDiscoveryPolicy {
    const val PATIENT_FLAME = "patient_flame"
    const val MARGINALIA_GATE = "marginalia_gate"
    const val DEEP_SHELF = "deep_shelf"
    const val LONG_WATCH = "long_watch"
    const val VEIL_THINS = "veil_thins"
    const val UNNAMED_CHAMBER = "unnamed_chamber"

    fun eligibleIds(
        profile: ReaderProfile,
        highlightCount: Int
    ): Set<String> = buildSet {
        if (profile.streakDays >= 7 && profile.minutesRead >= 600) {
            add(PATIENT_FLAME)
        }
        if (highlightCount >= 10 && profile.pagesRead >= 1_000) {
            add(MARGINALIA_GATE)
        }
        if (profile.booksFinished >= 10 && profile.rankIndex >= 1) {
            add(DEEP_SHELF)
        }
        if (profile.minutesRead >= 3_000) {
            add(LONG_WATCH)
        }
        if (profile.earnedSigils.size >= 4) {
            add(VEIL_THINS)
        }
        if (profile.rankIndex >= 3 && profile.earnedSigils.size >= 5) {
            add(UNNAMED_CHAMBER)
        }
    }
}
