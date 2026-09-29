package com.veilreader.app.domain

import kotlin.math.roundToInt

data class PathDoctrine(
    val pathId: String,
    val maxim: String,
    val embodimentName: String,
    val embodimentDescription: String,
    val insightName: String,
    val insightDescription: String,
    val stabilityName: String,
    val stabilityDescription: String
)

data class PathMasteryAxis(
    val value: Int,
    val target: Int
) {
    val progress: Float
        get() = if (target <= 0) 1f else
            (value.coerceAtLeast(0).toFloat() / target).coerceIn(0f, 1f)

    val ready: Boolean
        get() = value >= target.coerceAtLeast(0)
}

data class PathMasterySnapshot(
    val embodiment: PathMasteryAxis,
    val insight: PathMasteryAxis,
    val stability: PathMasteryAxis,
    val dissonance: Int
) {
    val ritualReady: Boolean
        get() = embodiment.ready && insight.ready && stability.ready

    val overallProgress: Float
        get() = ((embodiment.progress + insight.progress + stability.progress) / 3f)
            .coerceIn(0f, 1f)

    companion object {
        val EMPTY = PathMasterySnapshot(
            embodiment = PathMasteryAxis(0, 1),
            insight = PathMasteryAxis(0, 1),
            stability = PathMasteryAxis(0, 1),
            dissonance = 0
        )
    }
}

fun pathDoctrineFor(pathId: String): PathDoctrine =
    when (pathId) {
        "oracle" -> PathDoctrine(
            pathId = pathId,
            maxim = "Observe · connect · interpret",
            embodimentName = "Pattern Sight",
            embodimentDescription = "Preserve passages that expose clues, themes, or hidden structure.",
            insightName = "Deduction",
            insightDescription = "Turn fragments into notes, conclusions, and finished interpretations.",
            stabilityName = "Composure",
            stabilityDescription = "Return often enough that pattern-seeking does not become skimming."
        )
        "dreamwalker" -> PathDoctrine(
            pathId = pathId,
            maxim = "Enter · dwell · return",
            embodimentName = "Immersion",
            embodimentDescription = "Spend active time inside chosen worlds instead of rushing through them.",
            insightName = "World Memory",
            insightDescription = "Carry details, reflections, and completed journeys back out of the book.",
            stabilityName = "Grounding",
            stabilityDescription = "Balance deep immersion with durable return across multiple sessions."
        )
        "archivist" -> PathDoctrine(
            pathId = pathId,
            maxim = "Capture · order · revisit",
            embodimentName = "Record",
            embodimentDescription = "Write substantial notes that preserve ideas worth returning to.",
            insightName = "Synthesis",
            insightDescription = "Connect preserved passages to completed books and broader understanding.",
            stabilityName = "Continuity",
            stabilityDescription = "Build a record slowly enough that the archive remains useful instead of noisy."
        )
        "vanguard" -> PathDoctrine(
            pathId = pathId,
            maxim = "Advance · pace · finish",
            embodimentName = "Momentum",
            embodimentDescription = "Move through pages at a human pace; repeated fast taps do not count.",
            insightName = "Campaign Memory",
            insightDescription = "Finish arcs and preserve the moments that changed the direction of the story.",
            stabilityName = "Endurance",
            stabilityDescription = "Sustain momentum across sessions instead of burning through one burst."
        )
        "nocturne" -> PathDoctrine(
            pathId = pathId,
            maxim = "Attend · endure · reflect",
            embodimentName = "Night Watch",
            embodimentDescription = "Accumulate deliberate reading during the hours chosen for this Path.",
            insightName = "Shadow Reading",
            insightDescription = "Reflect on unsettling passages instead of treating darkness as pure spectacle.",
            stabilityName = "Lantern Discipline",
            stabilityDescription = "Keep the habit steady enough that atmosphere never becomes compulsion."
        )
        "artificer" -> PathDoctrine(
            pathId = pathId,
            maxim = "Observe · deconstruct · rebuild",
            embodimentName = "Mechanism",
            embodimentDescription = "Capture substantial notes about systems, causes, and how ideas fit together.",
            insightName = "Synthesis",
            insightDescription = "Relate mechanisms to finished works and preserved evidence.",
            stabilityName = "Calibration",
            stabilityDescription = "Return across sessions until understanding survives beyond one burst of curiosity."
        )
        else -> PathDoctrine(
            pathId = pathId,
            maxim = "Read · understand · return",
            embodimentName = "Practice",
            embodimentDescription = "Act in ways that match the reading identity you selected.",
            insightName = "Insight",
            insightDescription = "Preserve evidence that reading changed what you understand.",
            stabilityName = "Stability",
            stabilityDescription = "Return often enough that progression reflects a durable habit."
        )
    }

fun pathInsightEvidenceTotal(
    pathId: String,
    totalHighlights: Int,
    substantialNotes: Int,
    pagesRead: Int,
    booksFinished: Int
): Int {
    val safeHighlights = totalHighlights.coerceAtLeast(0)
    val safeNotes = substantialNotes.coerceAtLeast(0)
    val safePages = pagesRead.coerceAtLeast(0)
    val safeBooks = booksFinished.coerceAtLeast(0)
    return when (pathId) {
        "oracle" -> safeNotes * 2 + safeBooks + safeHighlights / 3
        "dreamwalker" -> safeBooks * 2 + safeNotes + safeHighlights / 4
        "archivist" -> safeNotes * 2 + safeBooks + safeHighlights / 2
        "vanguard" -> safeBooks * 3 + safeNotes + safePages / 120
        "nocturne" -> safeNotes * 2 + safeBooks * 2 + safeHighlights / 2
        "artificer" -> safeNotes * 2 + safeBooks + safeHighlights / 3
        else -> safeNotes + safeBooks + safeHighlights / 3
    }
}

fun pathStabilityEvidenceTotal(
    minutesRead: Int,
    booksFinished: Int,
    readingDays: Int
): Int =
    readingDays.coerceAtLeast(0) * 2 +
        minutesRead.coerceAtLeast(0) / 45 +
        booksFinished.coerceAtLeast(0) * 2

fun derivePathMastery(
    pathId: String,
    rankIndex: Int,
    embodimentValue: Int,
    embodimentTarget: Int,
    totalHighlights: Int,
    substantialNotes: Int,
    pagesRead: Int,
    minutesRead: Int,
    booksFinished: Int,
    readingDays: Int,
    insightBaseline: Int = 0,
    stabilityBaseline: Int = 0
): PathMasterySnapshot {
    val rank = rankIndex.coerceAtLeast(0)
    val insightTotal = pathInsightEvidenceTotal(
        pathId = pathId,
        totalHighlights = totalHighlights,
        substantialNotes = substantialNotes,
        pagesRead = pagesRead,
        booksFinished = booksFinished
    )
    val stabilityTotal = pathStabilityEvidenceTotal(
        minutesRead = minutesRead,
        booksFinished = booksFinished,
        readingDays = readingDays
    )
    val insightValue = (insightTotal - insightBaseline.coerceAtLeast(0)).coerceAtLeast(0)
    val stabilityValue = (stabilityTotal - stabilityBaseline.coerceAtLeast(0)).coerceAtLeast(0)

    val insightTarget = 3 + rank * 2
    val stabilityTarget = 3 + rank * 2
    val embodiment = PathMasteryAxis(
        value = embodimentValue.coerceAtLeast(0),
        target = embodimentTarget.coerceAtLeast(1)
    )
    val insight = PathMasteryAxis(insightValue, insightTarget)
    val stability = PathMasteryAxis(stabilityValue, stabilityTarget)

    val secondaryFloor = minOf(insight.progress, stability.progress)
    val imbalance = (embodiment.progress - secondaryFloor).coerceAtLeast(0f)
    val dissonance = (imbalance * 100f).roundToInt().coerceIn(0, 100)

    return PathMasterySnapshot(
        embodiment = embodiment,
        insight = insight,
        stability = stability,
        dissonance = dissonance
    )
}

fun effectivePathMastery(profile: ReaderProfile): PathMasterySnapshot =
    profile.pathMastery ?: PathMasterySnapshot(
        embodiment = PathMasteryAxis(
            profile.ritualProgress.coerceAtLeast(0),
            profile.ritualTarget.coerceAtLeast(1)
        ),
        insight = PathMasteryAxis(1, 1),
        stability = PathMasteryAxis(1, 1),
        dissonance = 0
    )
