package com.veilreader.app.domain

import kotlin.math.roundToInt

private const val CASTLE_DAY_MS = 86_400_000L
private const val RETURN_GAP_DAYS = 21
private const val RETURN_RECENT_DAYS = 3
private const val SILENCE_GRACE_DAYS = 14

data class CastleMemoryState(
    val volumeCount: Int,
    val completedCount: Int,
    val passageCount: Int,
    val annotationCount: Int,
    val bookmarkCount: Int,
    val sessionCount: Int,
    val activeHours: Float,
    val sealedCapsuleCount: Int,
    val atlasNodeCount: Int,
    val atlasLinkCount: Int,
    val rereadCycleCount: Int,
    val archiveAgeDays: Int,
    val daysSinceLastActivity: Int?,
    val libraryResonance: Float,
    val ritualResonance: Float,
    val observatoryResonance: Float,
    val archiveResonance: Float,
    val treasuryResonance: Float,
    val sanctumResonance: Float,
    val overallPresence: Float,
    val longSilence: Float,
    val returnAwakening: Float,
    val patina: Float,
    val litWindows: Int,
    val shelfRibs: Int,
    val starPoints: Int,
    val completionAlcoves: Int,
    val scriptoriumLamps: Int,
    val foundationCourses: Int,
    val rereadRings: Int,
    val fogAlpha: Float,
    val inscription: String,
    val mutationInscription: String
) {
    fun resonanceFor(roomId: String): Float =
        when (roomId) {
            "library" -> libraryResonance
            "ritual" -> ritualResonance
            "observatory" -> observatoryResonance
            "archive" -> archiveResonance
            "treasury" -> treasuryResonance
            "sanctum" -> sanctumResonance
            else -> 0f
        }.coerceIn(0f, 1f)

    companion object {
        val EMPTY = CastleMemoryState(
            volumeCount = 0,
            completedCount = 0,
            passageCount = 0,
            annotationCount = 0,
            bookmarkCount = 0,
            sessionCount = 0,
            activeHours = 0f,
            sealedCapsuleCount = 0,
            atlasNodeCount = 0,
            atlasLinkCount = 0,
            rereadCycleCount = 0,
            archiveAgeDays = 0,
            daysSinceLastActivity = null,
            libraryResonance = 0f,
            ritualResonance = 0f,
            observatoryResonance = 0f,
            archiveResonance = 0f,
            treasuryResonance = 0f,
            sanctumResonance = 0f,
            overallPresence = 0f,
            longSilence = 0f,
            returnAwakening = 0f,
            patina = 0f,
            litWindows = 1,
            shelfRibs = 3,
            starPoints = 0,
            completionAlcoves = 0,
            scriptoriumLamps = 0,
            foundationCourses = 2,
            rereadRings = 0,
            fogAlpha = 0.48f,
            inscription = "The foundation waits for its first volume.",
            mutationInscription = "No reading history has entered the stone yet."
        )
    }
}

/**
 * Turns existing durable reading history into environmental Castle state.
 *
 * Rank remains the only room-unlock authority. Memory changes architecture, warmth, patina,
 * illumination and inscriptions, never progression or access.
 */
fun deriveCastleMemoryState(
    books: List<Book>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    sessions: List<ReadingSessionSnapshot>,
    readingCycles: List<ReadingCycleRecord> = emptyList(),
    nowEpochMs: Long = System.currentTimeMillis()
): CastleMemoryState {
    if (
        books.isEmpty() &&
        highlights.isEmpty() &&
        bookmarks.isEmpty() &&
        sessions.isEmpty() &&
        readingCycles.isEmpty()
    ) {
        return CastleMemoryState.EMPTY
    }

    val completed = books.count { it.finished }
    val annotations = highlights.count { it.note.isNotBlank() }
    val favorites = books.count { it.favorite }
    val totalActiveMillis = sessions.sumOf { it.activeMillis.coerceAtLeast(0L) }
    val activeHours = totalActiveMillis / 3_600_000f
    val rereads = readingCycles.count { it.cycleIndex > 1 }

    val atlas = buildMemoryAtlas(
        books = books,
        highlights = highlights,
        sessions = sessions
    )
    val capsules = deriveReadingTimeCapsules(
        books = books,
        sessions = sessions,
        highlights = highlights,
        bookmarks = bookmarks,
        sealedCycles = readingCycles
    )

    val activityTimes = buildList<Long> {
        books.forEach { book ->
            if (book.addedAtEpochMs > 0L) add(book.addedAtEpochMs)
            if (book.lastOpenedAtEpochMs > 0L) add(book.lastOpenedAtEpochMs)
        }
        sessions.forEach { session ->
            if (session.startedAtEpochMs > 0L) add(session.startedAtEpochMs)
            if (session.endedAtEpochMs > 0L) add(session.endedAtEpochMs)
        }
        highlights.forEach { highlight ->
            if (highlight.createdAtEpochMs > 0L) add(highlight.createdAtEpochMs)
        }
        bookmarks.forEach { bookmark ->
            if (bookmark.createdAtEpochMs > 0L) add(bookmark.createdAtEpochMs)
        }
        readingCycles.forEach { cycle ->
            if (cycle.completedAtEpochMs > 0L) add(cycle.completedAtEpochMs)
        }
    }

    val safeNow = nowEpochMs.coerceAtLeast(0L)
    val earliestActivity = activityTimes.minOrNull()
    val latestActivity = activityTimes.maxOrNull()
    val archiveAgeDays = earliestActivity
        ?.takeIf { safeNow >= it }
        ?.let { ((safeNow - it) / CASTLE_DAY_MS).toInt() }
        ?.coerceAtLeast(0)
        ?: 0
    val daysSinceLastActivity = latestActivity
        ?.takeIf { safeNow >= it }
        ?.let { ((safeNow - it) / CASTLE_DAY_MS).toInt() }
        ?.coerceAtLeast(0)

    val longSilence = daysSinceLastActivity
        ?.let { days ->
            if (days <= SILENCE_GRACE_DAYS) {
                0f
            } else {
                saturation((days - SILENCE_GRACE_DAYS).toFloat(), 90f)
            }
        }
        ?: 0f

    val sessionStarts = sessions
        .asSequence()
        .map { it.startedAtEpochMs }
        .filter { it > 0L && it <= safeNow }
        .sorted()
        .toList()
    val latestSessionAt = sessionStarts.lastOrNull()
    val previousSessionAt = sessionStarts.getOrNull(sessionStarts.lastIndex - 1)
    val returnGapDays = if (
        latestSessionAt != null &&
        previousSessionAt != null &&
        latestSessionAt >= previousSessionAt
    ) {
        ((latestSessionAt - previousSessionAt) / CASTLE_DAY_MS).toInt()
    } else {
        0
    }
    val recentReturnDays = latestSessionAt
        ?.takeIf { safeNow >= it }
        ?.let { ((safeNow - it) / CASTLE_DAY_MS).toInt() }

    val returnAwakening = if (
        returnGapDays >= RETURN_GAP_DAYS &&
        recentReturnDays != null &&
        recentReturnDays <= RETURN_RECENT_DAYS
    ) {
        val gapStrength = saturation(
            (returnGapDays - RETURN_GAP_DAYS + 1).toFloat(),
            90f
        )
        val recencyStrength =
            1f - (recentReturnDays.toFloat() / (RETURN_RECENT_DAYS + 1f))
        (0.36f + gapStrength * 0.64f)
            .coerceIn(0f, 1f) * recencyStrength.coerceIn(0.25f, 1f)
    } else {
        0f
    }

    val library = weightedPresence(
        saturation(books.size, 36),
        saturation(completed, 18),
        firstWeight = 0.58f
    )
    val ritual = weightedPresence(
        saturation(sessions.size, 80),
        saturation(activeHours, 120f),
        firstWeight = 0.44f
    )
    val observatory = weightedPresence(
        saturation(atlas.nodes.size, 24),
        saturation(atlas.edges.size, 28),
        firstWeight = 0.42f
    )
    val archive = (
        saturation(highlights.size, 90) * 0.48f +
            saturation(annotations, 36) * 0.32f +
            saturation(bookmarks.size, 30) * 0.20f
        ).coerceIn(0f, 1f)
    val treasury = (
        saturation(capsules.size, 14) * 0.58f +
            saturation(favorites, 16) * 0.20f +
            saturation(completed, 30) * 0.22f
        ).coerceIn(0f, 1f)
    val sanctum = (
        saturation(completed, 40) * 0.34f +
            saturation(capsules.size, 20) * 0.28f +
            saturation(activeHours, 220f) * 0.16f +
            saturation(atlas.edges.size, 40) * 0.08f +
            saturation(rereads, 10) * 0.14f
        ).coerceIn(0f, 1f)

    val overall = (
        library * 0.22f +
            ritual * 0.13f +
            observatory * 0.16f +
            archive * 0.20f +
            treasury * 0.15f +
            sanctum * 0.14f
        ).coerceIn(0f, 1f)

    val patina = (
        saturation(rereads, 8) * 0.46f +
            saturation(archiveAgeDays.toFloat(), 540f) * 0.26f +
            saturation(activeHours, 180f) * 0.28f
        ).coerceIn(0f, 1f)

    val baseLitWindows = (1f + overall * 8f).roundToInt().coerceIn(1, 9)
    val warmthMultiplier = (
        1f -
            longSilence * 0.52f +
            returnAwakening * 0.38f
        ).coerceIn(0.38f, 1.18f)

    val inscription = when {
        books.isEmpty() -> "The foundation waits for its first volume."
        sessions.isEmpty() && highlights.isEmpty() ->
            "Volumes stand in the keep, but few traces have entered the stone."
        overall < 0.24f ->
            "A few rooms remember. The rest of the keep is still listening."
        overall < 0.50f ->
            "The keep has begun to retain the shape of your reading."
        overall < 0.76f ->
            "The archive is warm behind the walls; whole chambers now carry memory."
        else ->
            "The keep is dense with memory. Very little inside it is still silent."
    }

    val mutationInscription = when {
        returnAwakening >= 0.28f ->
            "After a long quiet, lamps are waking from the foundation upward."
        longSilence >= 0.58f ->
            "The halls have gone cold with distance, but none of their records were erased."
        rereads >= 3 ->
            "Repeated journeys have worn rings into the stone around familiar shelves."
        annotations >= 12 ->
            "The scriptorium burns late; the margins have become a second archive."
        completed >= 6 ->
            "Finished volumes have opened a line of sealed alcoves beneath the keep."
        activeHours >= 20f ->
            "Long hours have settled into the foundation as weight rather than ornament."
        else ->
            "The keep changes only where your reading leaves durable evidence."
    }

    return CastleMemoryState(
        volumeCount = books.size,
        completedCount = completed,
        passageCount = highlights.size,
        annotationCount = annotations,
        bookmarkCount = bookmarks.size,
        sessionCount = sessions.size,
        activeHours = activeHours,
        sealedCapsuleCount = capsules.size,
        atlasNodeCount = atlas.nodes.size,
        atlasLinkCount = atlas.edges.size,
        rereadCycleCount = rereads,
        archiveAgeDays = archiveAgeDays,
        daysSinceLastActivity = daysSinceLastActivity,
        libraryResonance = library,
        ritualResonance = ritual,
        observatoryResonance = observatory,
        archiveResonance = archive,
        treasuryResonance = treasury,
        sanctumResonance = sanctum,
        overallPresence = overall,
        longSilence = longSilence,
        returnAwakening = returnAwakening,
        patina = patina,
        litWindows = (baseLitWindows * warmthMultiplier)
            .roundToInt()
            .coerceIn(1, 9),
        shelfRibs = (3f + library * 9f).roundToInt().coerceIn(3, 12),
        starPoints = (observatory * 18f).roundToInt().coerceIn(0, 18),
        completionAlcoves = completed.coerceIn(0, 12),
        scriptoriumLamps = (
            saturation(annotations, 28) * 7f
            ).roundToInt().coerceIn(0, 7),
        foundationCourses = (
            2f +
                saturation(activeHours, 120f) * 5f +
                saturation(sessions.size, 90) * 3f
            ).roundToInt().coerceIn(2, 10),
        rereadRings = rereads.coerceIn(0, 6),
        fogAlpha = (
            0.48f -
                overall * 0.28f +
                longSilence * 0.18f -
                returnAwakening * 0.10f
            ).coerceIn(0.14f, 0.64f),
        inscription = inscription,
        mutationInscription = mutationInscription
    )
}

private fun saturation(value: Int, target: Int): Float =
    if (target <= 0) 0f else (value.toFloat() / target.toFloat()).coerceIn(0f, 1f)

private fun saturation(value: Float, target: Float): Float =
    if (target <= 0f) 0f else (value / target).coerceIn(0f, 1f)

private fun weightedPresence(
    first: Float,
    second: Float,
    firstWeight: Float
): Float {
    val safeWeight = firstWeight.coerceIn(0f, 1f)
    return (first * safeWeight + second * (1f - safeWeight)).coerceIn(0f, 1f)
}
