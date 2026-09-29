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
     * One daily directive follows the reader's chosen Path. The ID intentionally remains "mark"
     * for backward-compatible same-day reward claiming across older builds.
     */
    fun pathDirective(
        pathId: String,
        todayMinutes: Int,
        todayPages: Int,
        todayHighlights: Int,
        todayNotes: Int,
        todayNightMinutes: Int
    ): Quest =
        when (pathId) {
            "dreamwalker" -> Quest(
                id = "mark",
                title = "Dwell in another world for 12 active minutes",
                progress = todayMinutes.coerceAtLeast(0).coerceAtMost(12),
                target = 12,
                xpReward = 80
            )
            "vanguard" -> Quest(
                id = "mark",
                title = "Advance through 10 paced pages",
                progress = todayPages.coerceAtLeast(0).coerceAtMost(10),
                target = 10,
                xpReward = 90
            )
            "nocturne" -> Quest(
                id = "mark",
                title = "Keep the night watch for 10 reading minutes",
                progress = todayNightMinutes.coerceAtLeast(0).coerceAtMost(10),
                target = 10,
                xpReward = 100
            )
            "archivist" -> Quest(
                id = "mark",
                title = "Write one substantial passage note",
                progress = todayNotes.coerceAtLeast(0).coerceAtMost(1),
                target = 1,
                xpReward = 100
            )
            "artificer" -> Quest(
                id = "mark",
                title = "Capture one substantial concept note",
                progress = todayNotes.coerceAtLeast(0).coerceAtMost(1),
                target = 1,
                xpReward = 100
            )
            else -> Quest(
                id = "mark",
                title = "Preserve 3 passages that reveal hidden structure",
                progress = todayHighlights.coerceAtLeast(0).coerceAtMost(3),
                target = 3,
                xpReward = 75
            )
        }

    /**
     * Advancement is deliberately not XP-only. A reader must satisfy a path-specific ritual.
     */
    fun canAdvanceRank(
        profile: ReaderProfile,
        expectedPathId: String = profile.path.id,
        expectedRankIndex: Int = profile.rankIndex
    ): Boolean {
        val masteryReady =
            profile.pathMastery?.ritualReady
                ?: (profile.ritualProgress >= profile.ritualTarget)
        return profile.path.id == expectedPathId &&
            profile.rankIndex == expectedRankIndex &&
            masteryReady &&
            profile.rankIndex < profile.path.ranks.lastIndex
    }
}
