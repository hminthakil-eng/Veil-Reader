package com.veilreader.app.domain

/**
 * A deterministic bridge between reading progression and the visible Veil world.
 *
 * This is deliberately presentation-only. Rank remains the sole room-unlock authority; XP,
 * streaks and daily directives may change atmosphere, density and feedback but never access.
 */
enum class WorldAwakeningStage(val label: String) {
    DORMANT("Dormant"),
    KINDLED("Kindled"),
    ATTUNED("Attuned"),
    RESONANT("Resonant"),
    ASCENDANT("Ascendant")
}

data class WorldProgressionProjection(
    val stage: WorldAwakeningStage,
    val rankProgress: Float,
    val ritualCharge: Float,
    val questResonance: Float,
    val streakEmbers: Int,
    val architecturalPresence: Float,
    val archiveDepth: Float,
    val mirrorClarity: Float,
    val observatorySignal: Float,
    val relicWeight: Float,
    val sanctumPresence: Float,
    val completedDirectives: Int,
    val directiveCount: Int,
    val inscription: String
)

fun deriveWorldProgressionProjection(
    profile: ReaderProfile,
    quests: List<Quest>,
    memory: CastleMemoryState
): WorldProgressionProjection {
    val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(1)
    val rankProgress =
        (profile.rankIndex.coerceIn(0, finalRank).toFloat() / finalRank.toFloat())
            .coerceIn(0f, 1f)

    val ritualTarget = profile.ritualTarget.coerceAtLeast(1)
    val ritualCharge =
        (profile.ritualProgress.coerceAtLeast(0).toFloat() / ritualTarget.toFloat())
            .coerceIn(0f, 1f)

    val completedDirectives = quests.count { it.target > 0 && it.progress >= it.target }
    val directiveCount = quests.count { it.target > 0 }
    val questResonance = if (directiveCount == 0) {
        0f
    } else {
        completedDirectives.toFloat() / directiveCount.toFloat()
    }.coerceIn(0f, 1f)

    val levelSignal = (profile.level.coerceAtLeast(1) / 30f).coerceIn(0f, 1f)
    val streakSignal = (profile.streakDays.coerceAtLeast(0) / 14f).coerceIn(0f, 1f)
    val streakEmbers = profile.streakDays.coerceIn(0, 7)

    // Durable reading memory deliberately carries the most weight. Temporary reward signals may
    // warm the world, but cannot overpower actual books, sessions, passages and completed cycles.
    val architecturalPresence = (
        memory.overallPresence * 0.45f +
            rankProgress * 0.22f +
            ritualCharge * 0.12f +
            levelSignal * 0.08f +
            streakSignal * 0.07f +
            questResonance * 0.06f
        ).coerceIn(0f, 1f)

    val archiveDepth = (
        memory.archiveResonance * 0.72f +
            rankProgress * 0.16f +
            levelSignal * 0.12f
        ).coerceIn(0f, 1f)

    val mirrorClarity = (
        memory.archiveResonance * 0.74f +
            ritualCharge * 0.10f +
            streakSignal * 0.06f +
            questResonance * 0.10f
        ).coerceIn(0f, 1f)

    val observatorySignal = (
        memory.observatoryResonance * 0.76f +
            rankProgress * 0.14f +
            levelSignal * 0.10f
        ).coerceIn(0f, 1f)

    val relicWeight = (
        memory.treasuryResonance * 0.72f +
            rankProgress * 0.18f +
            questResonance * 0.10f
        ).coerceIn(0f, 1f)

    val sanctumPresence = (
        memory.sanctumResonance * 0.78f +
            rankProgress * 0.22f
        ).coerceIn(0f, 1f)

    val stage = when {
        architecturalPresence >= 0.78f -> WorldAwakeningStage.ASCENDANT
        architecturalPresence >= 0.56f -> WorldAwakeningStage.RESONANT
        architecturalPresence >= 0.34f -> WorldAwakeningStage.ATTUNED
        architecturalPresence >= 0.12f -> WorldAwakeningStage.KINDLED
        else -> WorldAwakeningStage.DORMANT
    }

    val inscription = when {
        GamificationEngine.canAdvanceRank(profile) ->
            "The seal is complete. The Hall is waiting for your next transformation."
        stage == WorldAwakeningStage.ASCENDANT ->
            "Your reading no longer decorates the keep; it has become part of its architecture."
        memory.returnAwakening > 0.28f ->
            "The old lamps recognized your return and the lower halls are warming again."
        ritualCharge >= 0.66f ->
            "The ritual seal is gathering weight. One more deliberate act may change the Path."
        streakEmbers >= 5 ->
            "A chain of reading nights is burning steadily through the brasswork."
        archiveDepth >= 0.45f ->
            "The Archive has enough memory to cast a visible shadow into the Hall."
        stage == WorldAwakeningStage.KINDLED ->
            "The first traces of your reading have begun to wake the stone."
        else ->
            "The Hall is quiet. It will change only when reading leaves durable evidence."
    }

    return WorldProgressionProjection(
        stage = stage,
        rankProgress = rankProgress,
        ritualCharge = ritualCharge,
        questResonance = questResonance,
        streakEmbers = streakEmbers,
        architecturalPresence = architecturalPresence,
        archiveDepth = archiveDepth,
        mirrorClarity = mirrorClarity,
        observatorySignal = observatorySignal,
        relicWeight = relicWeight,
        sanctumPresence = sanctumPresence,
        completedDirectives = completedDirectives,
        directiveCount = directiveCount,
        inscription = inscription
    )
}
