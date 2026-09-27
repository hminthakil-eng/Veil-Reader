package com.veilreader.app.domain

data class VeiledDiscoveryRecord(
    val id: String,
    /**
     * The first instant this app version durably recorded the discovery.
     *
     * This is intentionally not called "earnedAt": older installs may already satisfy a discovery
     * before the ledger existed, and Veil must not fabricate the original earning timestamp.
     */
    val recordedAtEpochMs: Long?
)

object VeiledDiscoveryCatalog {
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

    fun qualifyingIds(
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

    /**
     * Merge-only ledger semantics.
     *
     * A discovery can enter the ledger but can never leave it because a streak later resets or
     * another transient profile field changes.
     */
    fun mergeEarned(
        existingIds: Set<String>,
        profile: ReaderProfile,
        highlightCount: Int
    ): Set<String> =
        (existingIds + qualifyingIds(profile, highlightCount))
            .filterTo(linkedSetOf()) { it in orderedIds }
}
