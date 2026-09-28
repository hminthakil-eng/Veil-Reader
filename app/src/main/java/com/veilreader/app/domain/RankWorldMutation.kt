package com.veilreader.app.domain

data class RankRealmMutation(
    val title: String,
    val inscription: String,
    val provenance: DualBookProvenance
)

fun deriveRankRealmMutation(
    pathId: String,
    rankName: String,
    rankIndex: Int,
    rankCount: Int,
    realm: WorldMutationRealm
): RankRealmMutation {
    val count = rankCount.coerceAtLeast(1)
    val rank = rankIndex.coerceIn(0, count - 1)
    val tier = when {
        count <= 1 -> "first threshold"
        rank == 0 -> "first threshold"
        rank == count - 1 -> "final threshold"
        rank * 2 < count -> "deepening threshold"
        else -> "high threshold"
    }

    val pathCore = when (pathId) {
        "oracle" -> Triple(
            "Cipher",
            "Questions begin to outnumber decorations; gaps and alignments become part of the architecture.",
            DualBookProvenance(
                lotmConcepts = setOf(
                    DualBookMotif.DIVINATION_AND_INFERENCE,
                    DualBookMotif.SEALED_KNOWLEDGE,
                    DualBookMotif.RITUAL_TRANSFORMATION
                ),
                coiConcepts = setOf(
                    DualBookMotif.FATE_PRESSURE,
                    DualBookMotif.MIRROR_SYMBOLISM
                ),
                veilTransformation =
                    "Veil turns occult inference and fate pressure into visible architecture that preserves uncertainty instead of pretending to predict reality."
            )
        )
        "dreamwalker" -> Triple(
            "Dream",
            "Distances become less literal; repeated returns teach the world which impossible geometry may remain.",
            DualBookProvenance(
                lotmConcepts = setOf(
                    DualBookMotif.ROLE_EMBODIMENT,
                    DualBookMotif.RITUAL_TRANSFORMATION
                ),
                coiConcepts = setOf(
                    DualBookMotif.DREAM_LOGIC,
                    DualBookMotif.CYCLICAL_RETURN,
                    DualBookMotif.FATE_PRESSURE
                ),
                veilTransformation =
                    "Veil converts dream traversal into controlled spatial metaphor driven only by durable reading history."
            )
        )
        "archivist" -> Triple(
            "Archive",
            "Indexes, ribs and marginal structures become load-bearing; what was preserved gains physical weight.",
            DualBookProvenance(
                lotmConcepts = setOf(
                    DualBookMotif.SEALED_KNOWLEDGE,
                    DualBookMotif.HIDDEN_ORDER
                ),
                coiConcepts = setOf(
                    DualBookMotif.MIRROR_SYMBOLISM,
                    DualBookMotif.CYCLICAL_RETURN
                ),
                veilTransformation =
                    "Veil makes recorded evidence alter the archive's material organization without inventing facts about the books."
            )
        )
        "vanguard" -> Triple(
            "Vector",
            "Lines tighten toward thresholds; motion becomes deliberate architecture rather than speed for its own sake.",
            DualBookProvenance(
                lotmConcepts = setOf(
                    DualBookMotif.ROLE_EMBODIMENT,
                    DualBookMotif.RITUAL_TRANSFORMATION
                ),
                coiConcepts = setOf(
                    DualBookMotif.FATE_PRESSURE,
                    DualBookMotif.CYCLICAL_RETURN
                ),
                veilTransformation =
                    "Veil turns advancement pressure into directional form while keeping progression evidence-based and non-punitive."
            )
        )
        "nocturne" -> Triple(
            "Eclipse",
            "Light withdraws selectively; warnings, absences and protected darkness become readable instead of merely decorative.",
            DualBookProvenance(
                lotmConcepts = setOf(
                    DualBookMotif.SEALED_KNOWLEDGE,
                    DualBookMotif.CORRUPTION_RISK
                ),
                coiConcepts = setOf(
                    DualBookMotif.FATE_PRESSURE,
                    DualBookMotif.CONTRACT_AND_PRICE,
                    DualBookMotif.CORRUPTION_RISK
                ),
                veilTransformation =
                    "Veil uses darkness to expose consequence and uncertainty, never to reward harm, compulsion or hidden penalties."
            )
        )
        "artificer" -> Triple(
            "Mechanism",
            "Hidden teeth, measured arcs and cause-and-effect marks become visible beneath the ornament.",
            DualBookProvenance(
                lotmConcepts = setOf(
                    DualBookMotif.MECHANISM_AND_CONSEQUENCE,
                    DualBookMotif.RITUAL_TRANSFORMATION
                ),
                coiConcepts = setOf(
                    DualBookMotif.CONTRACT_AND_PRICE,
                    DualBookMotif.FATE_PRESSURE
                ),
                veilTransformation =
                    "Veil renders ritual and bargain motifs as inspectable mechanisms with explicit causes and no secret power economy."
            )
        )
        else -> Triple(
            "Threshold",
            "The architecture records change without claiming a meaning the reading evidence cannot support.",
            DualBookProvenance(
                lotmConcepts = setOf(DualBookMotif.RITUAL_TRANSFORMATION),
                coiConcepts = setOf(DualBookMotif.FATE_PRESSURE),
                veilTransformation =
                    "Unknown future Paths receive a neutral threshold language that never invents unsupported lore."
            )
        )
    }

    val realmEffect = when (realm) {
        WorldMutationRealm.GREAT_HALL ->
            "In the Great Hall, ${pathCore.first.lowercase()} geometry now frames the routes between chambers."
        WorldMutationRealm.ARCHIVE ->
            "In the Archive, the ${pathCore.first.lowercase()} signature changes margins, indexing marks and the spacing around preserved evidence."
        WorldMutationRealm.MIRROR ->
            "In the Living Mirror, the ${pathCore.first.lowercase()} signature alters fog, silvering and the way factual reflections are revealed—not their content."
        WorldMutationRealm.OBSERVATORY ->
            "In the Observatory, the ${pathCore.first.lowercase()} signature becomes a lens over the atlas; nodes and links remain factual-memory owned."
        WorldMutationRealm.TREASURY ->
            "In the Treasury, the ${pathCore.first.lowercase()} signature changes pedestals, seals and relic response without changing ownership."
        WorldMutationRealm.SANCTUM ->
            "In the Sanctum, the ${pathCore.first.lowercase()} signature is carved into permanent threshold ornament and identity records."
    }

    return RankRealmMutation(
        title = "$rankName · ${pathCore.first} $tier",
        inscription = "${pathCore.second} $realmEffect",
        provenance = pathCore.third
    )
}
