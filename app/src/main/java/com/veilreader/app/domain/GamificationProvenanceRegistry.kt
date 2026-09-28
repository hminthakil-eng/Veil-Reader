package com.veilreader.app.domain

/**
 * Provenance registry for authored gamification surfaces outside a single encounter.
 *
 * The registry exists so new narrative systems cannot silently drift back into generic fantasy
 * or copy one source in isolation. It is design metadata only and grants no gameplay authority.
 */
object GamificationProvenanceRegistry {
    val narrativeRelicIds: Set<String> = setOf(
        "ember_bookmark",
        "moonlit_lens",
        "brass_quill",
        "ivory_bookplate",
        "astral_key",
        "veil_crown",
        SilentNamesEncounter.REWARD_ID
    )

    fun pathDoctrine(pathId: String): DualBookProvenance = when (pathId) {
        "oracle" -> provenance(
            lotm = setOf(DualBookMotif.DIVINATION_AND_INFERENCE, DualBookMotif.SEALED_KNOWLEDGE),
            coi = setOf(DualBookMotif.FATE_PRESSURE, DualBookMotif.MIRROR_SYMBOLISM),
            transform = "Clue-seeking becomes a reading discipline that rewards evidence, notes and provisional interpretation."
        )
        "dreamwalker" -> provenance(
            lotm = setOf(DualBookMotif.ROLE_EMBODIMENT, DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.DREAM_LOGIC, DualBookMotif.CYCLICAL_RETURN),
            transform = "Immersion becomes durable return: time in a world matters only when memory or understanding survives it."
        )
        "archivist" -> provenance(
            lotm = setOf(DualBookMotif.SEALED_KNOWLEDGE, DualBookMotif.HIDDEN_ORDER),
            coi = setOf(DualBookMotif.MIRROR_SYMBOLISM, DualBookMotif.CYCLICAL_RETURN),
            transform = "Archive motifs become a non-fictional memory practice built only from the user's recorded highlights and notes."
        )
        "vanguard" -> provenance(
            lotm = setOf(DualBookMotif.ROLE_EMBODIMENT, DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.FATE_PRESSURE, DualBookMotif.CYCLICAL_RETURN),
            transform = "Forward pressure becomes paced reading and finishing behavior, never speed farming or coercive streak pressure."
        )
        "nocturne" -> provenance(
            lotm = setOf(DualBookMotif.SEALED_KNOWLEDGE, DualBookMotif.CORRUPTION_RISK),
            coi = setOf(DualBookMotif.CONTRACT_AND_PRICE, DualBookMotif.FATE_PRESSURE),
            transform = "Dark atmosphere becomes reflective attention to warnings and consequences without rewarding unhealthy late-night compulsion."
        )
        "artificer" -> provenance(
            lotm = setOf(DualBookMotif.MECHANISM_AND_CONSEQUENCE, DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.CONTRACT_AND_PRICE, DualBookMotif.FATE_PRESSURE),
            transform = "Mechanism and bargain motifs become systems-thinking notes, visible causality and inspectable product rules."
        )
        else -> provenance(
            lotm = setOf(DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.FATE_PRESSURE),
            transform = "Future Paths fall back to evidence, reflection and reversible choice without invented lore."
        )
    }

    fun pathDirective(pathId: String): DualBookProvenance = pathDoctrine(pathId)

    fun worldMutation(kind: WorldMutationKind): DualBookProvenance = when (kind) {
        WorldMutationKind.FOUNDATION_WEIGHT -> provenance(
            lotm = setOf(DualBookMotif.ROLE_EMBODIMENT),
            coi = setOf(DualBookMotif.CYCLICAL_RETURN),
            transform = "Repeated real sessions add architectural weight; the world remembers recurrence without streak punishment."
        )
        WorldMutationKind.COMPLETION_ALCOVES -> provenance(
            lotm = setOf(DualBookMotif.SEALED_KNOWLEDGE),
            coi = setOf(DualBookMotif.CYCLICAL_RETURN),
            transform = "Completed books open original archival recesses rather than awarding generic loot."
        )
        WorldMutationKind.SCRIPTORIUM_LIGHT -> provenance(
            lotm = setOf(DualBookMotif.DIVINATION_AND_INFERENCE, DualBookMotif.SEALED_KNOWLEDGE),
            coi = setOf(DualBookMotif.MIRROR_SYMBOLISM),
            transform = "Written annotations literally clarify authored world surfaces while factual note content remains untouched."
        )
        WorldMutationKind.REREAD_PATINA -> provenance(
            lotm = setOf(DualBookMotif.ROLE_EMBODIMENT),
            coi = setOf(DualBookMotif.CYCLICAL_RETURN, DualBookMotif.FATE_PRESSURE),
            transform = "Rereading becomes visible wear and return-rings, not recycled XP."
        )
        WorldMutationKind.CONSTELLATION_WEB -> provenance(
            lotm = setOf(DualBookMotif.DIVINATION_AND_INFERENCE),
            coi = setOf(DualBookMotif.FATE_PRESSURE, DualBookMotif.MIRROR_SYMBOLISM),
            transform = "Real recorded relations become a constellation presentation; no relationship is invented by the narrative layer."
        )
        WorldMutationKind.RETURN_AWAKENING -> provenance(
            lotm = setOf(DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.CYCLICAL_RETURN),
            transform = "A return after silence wakes temporary lamps without deleting, shaming or downgrading prior history."
        )
        WorldMutationKind.PATH_ASCENSION -> provenance(
            lotm = setOf(DualBookMotif.ROLE_EMBODIMENT, DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.FATE_PRESSURE, DualBookMotif.CONTRACT_AND_PRICE),
            transform = "Rank changes realm-specific identity and ornament while Rank remains the only room-unlock authority."
        )
        WorldMutationKind.ADVANCEMENT_SEAL -> provenance(
            lotm = setOf(DualBookMotif.RITUAL_TRANSFORMATION, DualBookMotif.SEALED_KNOWLEDGE),
            coi = setOf(DualBookMotif.CONTRACT_AND_PRICE, DualBookMotif.FATE_PRESSURE),
            transform = "Advancement leaves a permanent descriptive seal beside the authoritative rank rather than creating a second progression state."
        )
    }

    fun relic(relicId: String): DualBookProvenance = when (relicId) {
        "moonlit_lens" -> provenance(
            lotm = setOf(DualBookMotif.DIVINATION_AND_INFERENCE),
            coi = setOf(DualBookMotif.MIRROR_SYMBOLISM, DualBookMotif.FATE_PRESSURE),
            transform = "A lens reacts to real atlas evidence and uncertainty; it never predicts or invents reading relationships."
        )
        "brass_quill" -> provenance(
            lotm = setOf(DualBookMotif.SEALED_KNOWLEDGE),
            coi = setOf(DualBookMotif.CONTRACT_AND_PRICE),
            transform = "A writing relic awakens from substantial annotation evidence and exposes its exact provenance."
        )
        "astral_key" -> provenance(
            lotm = setOf(DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.FATE_PRESSURE),
            transform = "A threshold key represents completed advancement evidence but never unlocks rooms independently of Rank."
        )
        "veil_crown" -> provenance(
            lotm = setOf(DualBookMotif.HIDDEN_ORDER, DualBookMotif.RITUAL_TRANSFORMATION),
            coi = setOf(DualBookMotif.FATE_PRESSURE, DualBookMotif.CONTRACT_AND_PRICE),
            transform = "Endgame identity is a visible composite of final Path and earned core records, not a new authority layer."
        )
        SilentNamesEncounter.REWARD_ID -> provenance(
            lotm = setOf(DualBookMotif.SEALED_KNOWLEDGE, DualBookMotif.DIVINATION_AND_INFERENCE),
            coi = setOf(DualBookMotif.DREAM_LOGIC, DualBookMotif.CONTRACT_AND_PRICE),
            transform = "The authored Lantern witnesses one durable encounter choice and changes world presentation without granting rank or XP."
        )
        else -> provenance(
            lotm = setOf(DualBookMotif.SEALED_KNOWLEDGE),
            coi = setOf(DualBookMotif.CYCLICAL_RETURN),
            transform = "A reading relic is evidence-backed, non-consumable and descriptive rather than generic loot."
        )
    }

    fun discovery(id: String): DualBookProvenance = provenance(
        lotm = setOf(DualBookMotif.SEALED_KNOWLEDGE, DualBookMotif.HIDDEN_ORDER),
        coi = setOf(DualBookMotif.FATE_PRESSURE, DualBookMotif.CYCLICAL_RETURN),
        transform = "Discovery $id is a one-way original Veil record derived from truthful reading thresholds and revealed through clues rather than copied lore."
    )

    private fun provenance(
        lotm: Set<DualBookMotif>,
        coi: Set<DualBookMotif>,
        transform: String
    ) = DualBookProvenance(
        lotmConcepts = lotm,
        coiConcepts = coi,
        veilTransformation = transform
    )
}
