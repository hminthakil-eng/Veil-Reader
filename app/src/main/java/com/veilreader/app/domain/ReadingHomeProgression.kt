package com.veilreader.app.domain

/** Cosmetic residence milestones. These never grant XP or unlock Castle rooms. */
enum class ReadingHomeStage(val threshold: Int) {
    CAMP(0),
    HUT(15),
    HOUSE(120),
    CASTLE(600)
}

data class ReadingHomeProgression(
    val stage: ReadingHomeStage,
    val nextStage: ReadingHomeStage?,
    val activeMinutes: Int,
    val completedBooks: Int,
    val completionBoost: Int,
    val growthUnits: Long,
    val minutesToNextStage: Int,
    val stageProgress: Float
)

/**
 * Reuses the persisted lifetime GameRepository counters, rather than page turns, XP or streaks.
 * No writes occur here: reopening/composing the Castle cannot award duplicate progress.
 *
 * Each of the first four completed books can contribute 30 construction units. The bonus is
 * limited to one unit per four actual reading minutes (at most 20% of total growth), and starts
 * only after 15 active minutes. A restored finished shelf cannot build a home by itself; a long
 * unfinished novel can earn every stage. Construction units never masquerade as reading time.
 */
fun deriveReadingHomeProgression(minutesRead: Int, booksFinished: Int): ReadingHomeProgression {
    val minutes = minutesRead.coerceAtLeast(0)
    val books = booksFinished.coerceAtLeast(0)
    val bonusLimit = books.coerceAtMost(4) * 30
    val bonus = if (minutes >= ReadingHomeStage.HUT.threshold) {
        minOf(bonusLimit, minutes / 4)
    } else {
        0
    }
    val growth = minutes.toLong() + bonus.toLong()
    val stage = ReadingHomeStage.entries.last { growth >= it.threshold }
    val next = ReadingHomeStage.entries.getOrNull(stage.ordinal + 1)
    val progress = if (next == null) {
        1f
    } else {
        ((growth - stage.threshold).toDouble() / (next.threshold - stage.threshold))
            .coerceIn(0.0, 1.0).toFloat()
    }
    // Solve m + min(bonusLimit, floor(m / 4)) >= target. The 15-minute gate also applies.
    val requiredMinutes = next?.let {
        maxOf(it.threshold - bonusLimit, it.threshold - it.threshold / 5,
            ReadingHomeStage.HUT.threshold)
    } ?: minutes

    return ReadingHomeProgression(
        stage = stage,
        nextStage = next,
        activeMinutes = minutes,
        completedBooks = books,
        completionBoost = bonus,
        growthUnits = growth,
        minutesToNextStage = (requiredMinutes - minutes).coerceAtLeast(0),
        stageProgress = progress
    )
}
