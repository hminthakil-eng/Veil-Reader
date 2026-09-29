package com.veilreader.app.domain

import kotlin.math.roundToInt

enum class CastleInscriptionStage { EMPTY, UNTRACED, LISTENING, SHAPING, WARM, DENSE }

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
    val libraryResonance: Float,
    val ritualResonance: Float,
    val observatoryResonance: Float,
    val archiveResonance: Float,
    val treasuryResonance: Float,
    val sanctumResonance: Float,
    val overallPresence: Float,
    val litWindows: Int,
    val shelfRibs: Int,
    val starPoints: Int,
    val fogAlpha: Float,
    val inscription: String,
    val inscriptionStage: CastleInscriptionStage = CastleInscriptionStage.EMPTY
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
            libraryResonance = 0f,
            ritualResonance = 0f,
            observatoryResonance = 0f,
            archiveResonance = 0f,
            treasuryResonance = 0f,
            sanctumResonance = 0f,
            overallPresence = 0f,
            litWindows = 1,
            shelfRibs = 3,
            starPoints = 0,
            fogAlpha = 0.48f,
            inscription = "The foundation waits for its first volume."
        )
    }
}

/**
 * Turns existing durable reading history into environmental Castle state.
 *
 * This is deliberately not another progression system: rank still controls room unlocks.
 * Memory state only changes atmosphere, architectural density, and room resonance using facts
 * already stored by the reader.
 */
fun deriveCastleMemoryState(
    books: List<Book>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    sessions: List<ReadingSessionSnapshot>
): CastleMemoryState {
    if (books.isEmpty() && highlights.isEmpty() && bookmarks.isEmpty() && sessions.isEmpty()) {
        return CastleMemoryState.EMPTY
    }

    val completed = books.count { it.finished }
    val annotations = highlights.count { it.note.isNotBlank() }
    val favorites = books.count { it.favorite }
    val totalActiveMillis = sessions.sumOf { it.activeMillis.coerceAtLeast(0L) }
    val activeHours = totalActiveMillis / 3_600_000f

    val atlas = buildMemoryAtlas(
        books = books,
        highlights = highlights,
        sessions = sessions
    )
    val capsules = deriveReadingTimeCapsules(
        books = books,
        sessions = sessions,
        highlights = highlights,
        bookmarks = bookmarks
    )

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
        saturation(completed, 40) * 0.38f +
            saturation(capsules.size, 20) * 0.34f +
            saturation(activeHours, 220f) * 0.18f +
            saturation(atlas.edges.size, 40) * 0.10f
        ).coerceIn(0f, 1f)

    val overall = (
        library * 0.22f +
            ritual * 0.13f +
            observatory * 0.16f +
            archive * 0.20f +
            treasury * 0.15f +
            sanctum * 0.14f
        ).coerceIn(0f, 1f)

    val inscriptionStage = when {
        books.isEmpty() -> CastleInscriptionStage.EMPTY
        sessions.isEmpty() && highlights.isEmpty() -> CastleInscriptionStage.UNTRACED
        overall < 0.24f -> CastleInscriptionStage.LISTENING
        overall < 0.50f -> CastleInscriptionStage.SHAPING
        overall < 0.76f -> CastleInscriptionStage.WARM
        else -> CastleInscriptionStage.DENSE
    }
    val inscription = when (inscriptionStage) {
        CastleInscriptionStage.EMPTY -> "The foundation waits for its first volume."
        CastleInscriptionStage.UNTRACED ->
            "Volumes stand in the keep, but few traces have entered the stone."
        CastleInscriptionStage.LISTENING ->
            "A few rooms remember. The rest of the keep is still listening."
        CastleInscriptionStage.SHAPING ->
            "The keep has begun to retain the shape of your reading."
        CastleInscriptionStage.WARM ->
            "The archive is warm behind the walls; whole chambers now carry memory."
        CastleInscriptionStage.DENSE ->
            "The keep is dense with memory. Very little inside it is still silent."
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
        libraryResonance = library,
        ritualResonance = ritual,
        observatoryResonance = observatory,
        archiveResonance = archive,
        treasuryResonance = treasury,
        sanctumResonance = sanctum,
        overallPresence = overall,
        litWindows = (1f + overall * 8f).roundToInt().coerceIn(1, 9),
        shelfRibs = (3f + library * 9f).roundToInt().coerceIn(3, 12),
        starPoints = (observatory * 18f).roundToInt().coerceIn(0, 18),
        fogAlpha = (0.48f - overall * 0.28f).coerceIn(0.18f, 0.48f),
        inscription = inscription,
        inscriptionStage = inscriptionStage
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
