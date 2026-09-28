package com.veilreader.app.domain

/**
 * One derived, explainable ledger of visible world consequences.
 *
 * This does not persist a second world state. Every entry is projected from existing reading
 * history or the authoritative Path rank. Removing source data may remove derived mutations;
 * permanent discovery records remain owned by VeiledDiscoveryPolicy.
 */
enum class WorldMutationRealm {
    GREAT_HALL,
    ARCHIVE,
    MIRROR,
    OBSERVATORY,
    TREASURY,
    SANCTUM
}

enum class WorldMutationEvidence {
    READING_SESSIONS,
    COMPLETED_VOLUMES,
    ANNOTATIONS,
    REREAD_CYCLES,
    ATLAS_LINKS,
    RETURN_EVENT,
    PATH_RANK,
    RITUAL_SEAL
}

enum class WorldMutationKind {
    FOUNDATION_WEIGHT,
    COMPLETION_ALCOVES,
    SCRIPTORIUM_LIGHT,
    REREAD_PATINA,
    CONSTELLATION_WEB,
    RETURN_AWAKENING,
    PATH_ASCENSION,
    ADVANCEMENT_SEAL
}

data class WorldMutationEntry(
    val id: String,
    val kind: WorldMutationKind,
    val realms: Set<WorldMutationRealm>,
    val evidence: WorldMutationEvidence,
    val evidenceCount: Int,
    val intensity: Float,
    val durable: Boolean,
    val title: String,
    val inscription: String
)

data class WorldMutationLedger(
    val entries: List<WorldMutationEntry>
) {
    fun forRealm(realm: WorldMutationRealm): List<WorldMutationEntry> =
        entries.filter { realm in it.realms }

    val durableCount: Int
        get() = entries.count { it.durable }

    companion object {
        val EMPTY = WorldMutationLedger(emptyList())
    }
}

fun deriveWorldMutationLedger(
    profile: ReaderProfile,
    memory: CastleMemoryState
): WorldMutationLedger {
    val entries = buildList {
        if (memory.returnAwakening > 0.001f) {
            add(
                WorldMutationEntry(
                    id = "return-awakening",
                    kind = WorldMutationKind.RETURN_AWAKENING,
                    realms = setOf(WorldMutationRealm.GREAT_HALL),
                    evidence = WorldMutationEvidence.RETURN_EVENT,
                    evidenceCount = 1,
                    intensity = memory.returnAwakening.coerceIn(0f, 1f),
                    durable = false,
                    title = "The Returning Lamps",
                    inscription =
                        "A recorded return after long silence is warming the Hall. The effect fades; the history does not."
                )
            )
        }

        if (profile.rankIndex > 0) {
            val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(1)
            add(
                WorldMutationEntry(
                    id = "path-ascension",
                    kind = WorldMutationKind.PATH_ASCENSION,
                    realms = setOf(
                        WorldMutationRealm.GREAT_HALL,
                        WorldMutationRealm.TREASURY,
                        WorldMutationRealm.SANCTUM
                    ),
                    evidence = WorldMutationEvidence.PATH_RANK,
                    evidenceCount = profile.rankIndex,
                    intensity =
                        (profile.rankIndex.coerceIn(0, finalRank).toFloat() / finalRank)
                            .coerceIn(0f, 1f),
                    durable = true,
                    title = "Path-Bound Architecture",
                    inscription =
                        "Advancement has entered the architecture. It changes identity and ornament, never factual reading history."
                )
            )
        }

        profile.ritualAftermath?.let { aftermath ->
            val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(1)
            val fromName = profile.path.ranks.getOrElse(aftermath.fromRankIndex) {
                "Rank ${aftermath.fromRankIndex}"
            }
            val toName = profile.path.ranks.getOrElse(aftermath.toRankIndex) {
                "Rank ${aftermath.toRankIndex}"
            }
            add(
                WorldMutationEntry(
                    id = "advancement-seal:${aftermath.pathId}:${aftermath.toRankIndex}",
                    kind = WorldMutationKind.ADVANCEMENT_SEAL,
                    realms = setOf(
                        WorldMutationRealm.GREAT_HALL,
                        WorldMutationRealm.TREASURY,
                        WorldMutationRealm.SANCTUM
                    ),
                    evidence = WorldMutationEvidence.RITUAL_SEAL,
                    evidenceCount = aftermath.toRankIndex,
                    intensity =
                        (aftermath.toRankIndex.toFloat() / finalRank.toFloat())
                            .coerceIn(0f, 1f),
                    durable = true,
                    title = "Sealed Advancement",
                    inscription =
                        "$fromName became $toName in a recorded Ritual of Advancement. The seal persists after its ceremonial glow fades."
                )
            )
        }

        if (memory.rereadCycleCount > 0) {
            add(
                WorldMutationEntry(
                    id = "reread-patina",
                    kind = WorldMutationKind.REREAD_PATINA,
                    realms = setOf(
                        WorldMutationRealm.GREAT_HALL,
                        WorldMutationRealm.ARCHIVE,
                        WorldMutationRealm.SANCTUM
                    ),
                    evidence = WorldMutationEvidence.REREAD_CYCLES,
                    evidenceCount = memory.rereadCycleCount,
                    intensity = (memory.rereadRings / 6f).coerceIn(0f, 1f),
                    durable = true,
                    title = "Rings of Return",
                    inscription =
                        "Completed rereads have worn visible rings into the archive rather than granting arbitrary XP scenery."
                )
            )
        }

        if (memory.completedCount > 0) {
            add(
                WorldMutationEntry(
                    id = "completion-alcoves",
                    kind = WorldMutationKind.COMPLETION_ALCOVES,
                    realms = setOf(
                        WorldMutationRealm.GREAT_HALL,
                        WorldMutationRealm.TREASURY,
                        WorldMutationRealm.SANCTUM
                    ),
                    evidence = WorldMutationEvidence.COMPLETED_VOLUMES,
                    evidenceCount = memory.completedCount,
                    intensity = (memory.completionAlcoves / 12f).coerceIn(0f, 1f),
                    durable = true,
                    title = "Sealed Completion Alcoves",
                    inscription =
                        "Finished volumes have opened physical recesses in the keep and weight in the Reliquary."
                )
            )
        }

        if (memory.annotationCount > 0) {
            add(
                WorldMutationEntry(
                    id = "scriptorium-light",
                    kind = WorldMutationKind.SCRIPTORIUM_LIGHT,
                    realms = setOf(
                        WorldMutationRealm.GREAT_HALL,
                        WorldMutationRealm.ARCHIVE,
                        WorldMutationRealm.MIRROR
                    ),
                    evidence = WorldMutationEvidence.ANNOTATIONS,
                    evidenceCount = memory.annotationCount,
                    intensity = (memory.scriptoriumLamps / 7f).coerceIn(0f, 1f),
                    durable = true,
                    title = "Scriptorium Light",
                    inscription =
                        "Written marginalia is lighting the scriptorium and clarifying the Living Mirror."
                )
            )
        }

        if (memory.atlasLinkCount > 0) {
            add(
                WorldMutationEntry(
                    id = "constellation-web",
                    kind = WorldMutationKind.CONSTELLATION_WEB,
                    realms = setOf(
                        WorldMutationRealm.GREAT_HALL,
                        WorldMutationRealm.OBSERVATORY
                    ),
                    evidence = WorldMutationEvidence.ATLAS_LINKS,
                    evidenceCount = memory.atlasLinkCount,
                    intensity = memory.observatoryResonance.coerceIn(0f, 1f),
                    durable = true,
                    title = "Constellation Web",
                    inscription =
                        "Recorded relations between volumes are tensioning the Observatory sky. No relation is invented for density."
                )
            )
        }

        if (memory.sessionCount > 0 || memory.activeHours > 0f) {
            add(
                WorldMutationEntry(
                    id = "foundation-weight",
                    kind = WorldMutationKind.FOUNDATION_WEIGHT,
                    realms = setOf(WorldMutationRealm.GREAT_HALL),
                    evidence = WorldMutationEvidence.READING_SESSIONS,
                    evidenceCount = memory.sessionCount,
                    intensity =
                        ((memory.foundationCourses - 2).coerceAtLeast(0) / 8f)
                            .coerceIn(0f, 1f),
                    durable = true,
                    title = "Foundation Weight",
                    inscription =
                        "Recorded reading sessions have settled into the foundation as structural weight, not decoration."
                )
            )
        }
    }

    return WorldMutationLedger(entries)
}
