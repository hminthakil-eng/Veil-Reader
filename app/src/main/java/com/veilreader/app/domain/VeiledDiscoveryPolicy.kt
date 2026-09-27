package com.veilreader.app.domain

/**
 * One-way discovery policy for the Veiled folio.
 *
 * Eligibility is derived from truthful reading history. Persistence is owned by GameRepository:
 * once an id has been earned it is unioned with later eligibility and never removed.
 */
data class VeiledDiscoveryRecord(
    val id: String,
    /**
     * First instant this app version durably recorded the discovery.
     * Null means the discovery predates timestamped ledger metadata; Veil does not invent history.
     */
    val recordedAtEpochMs: Long?
)

object VeiledDiscoveryPolicy {
    const val PATIENT_FLAME = "patient_flame"
    const val MARGINALIA_GATE = "marginalia_gate"
    const val DEEP_SHELF = "deep_shelf"
    const val LONG_WATCH = "long_watch"
    const val VEIL_THINS = "veil_thins"
    const val UNNAMED_CHAMBER = "unnamed_chamber"

    val orderedIds = listOf(
        PATIENT_FLAME,
        MARGINALIA_GATE,
        DEEP_SHELF,
        LONG_WATCH,
        VEIL_THINS,
        UNNAMED_CHAMBER
    )

    fun eligibleIds(
        profile: ReaderProfile,
        highlightCount: Int
    ): Set<String> = buildSet {
        val sevenDayReturnRecorded =
            profile.streakDays >= 7 || "seven_days" in profile.earnedSigils
        if (sevenDayReturnRecorded && profile.minutesRead >= 600) {
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

    /** Merge-only semantics: recorded discoveries never reseal after transient state changes. */
    fun mergeEarned(
        existingIds: Set<String>,
        profile: ReaderProfile,
        highlightCount: Int
    ): Set<String> = existingIds + eligibleIds(profile, highlightCount)
}
