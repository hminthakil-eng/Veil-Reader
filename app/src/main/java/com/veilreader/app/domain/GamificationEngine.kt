package com.veilreader.app.domain

/**
 * Pure Kotlin progression logic. Intentionally independent from Android UI/storage so it can
 * be unit tested and later moved to a backend if cloud sync or social leagues are added.
 */
object GamificationEngine {
    const val XP_PER_PAGE = 2
    const val XP_PER_MINUTE = 3
    const val CHAPTER_BONUS = 45
    const val BOOK_FINISH_BONUS = 500

    fun readingXp(pages: Int, minutes: Int): Int =
        pages.coerceAtLeast(0) * XP_PER_PAGE + minutes.coerceAtLeast(0) * XP_PER_MINUTE

    fun levelFor(totalXp: Int): Int {
        val safeXp = totalXp.coerceAtLeast(0)
        var level = 1
        var remaining = safeXp
        while (remaining >= xpNeededForLevel(level)) {
            remaining -= xpNeededForLevel(level)
            level++
        }
        return level
    }

    fun progressInsideLevel(totalXp: Int): Pair<Int, Int> {
        var remaining = totalXp.coerceAtLeast(0)
        var level = 1
        while (remaining >= xpNeededForLevel(level)) {
            remaining -= xpNeededForLevel(level)
            level++
        }
        return remaining to xpNeededForLevel(level)
    }

    fun xpNeededForLevel(level: Int): Int = 350 + (level.coerceAtLeast(1) - 1) * 125

    /**
     * Advancement is deliberately not XP-only. A reader must satisfy a path-specific ritual.
     */
    fun canAdvanceRank(profile: ReaderProfile): Boolean =
        profile.ritualProgress >= profile.ritualTarget &&
            profile.rankIndex < profile.path.ranks.lastIndex
}
