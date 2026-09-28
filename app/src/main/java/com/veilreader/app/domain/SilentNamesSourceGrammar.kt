package com.veilreader.app.domain

/**
 * Design provenance for the first authored Veil encounter.
 *
 * These are concept labels, never copied text, characters, plot beats, rank ladders or book lore.
 * The source books provide a design grammar; Veil transforms that grammar into original fiction.
 */
enum class SilentNamesMotif {
    SEALED_KNOWLEDGE,
    DIVINATION_AND_INFERENCE,
    RITUAL_CONSEQUENCE,
    ROLE_CONSISTENT_ACTION,
    VEILED_ARCHIVE,
    DREAM_LOGIC,
    MIRROR_SYMBOLISM,
    FATE_PRESSURE,
    CONTRACT_AND_PRICE,
    CORRUPTION_RISK
}

data class SilentNamesDualBookProvenance(
    val lotmConcepts: Set<SilentNamesMotif>,
    val coiConcepts: Set<SilentNamesMotif>,
    val veilTransformation: String
) {
    init {
        require(lotmConcepts.isNotEmpty())
        require(coiConcepts.isNotEmpty())
        require(veilTransformation.isNotBlank())
    }
}

object SilentNamesSourceGrammar {
    fun forChoice(choice: SilentNamesChoice): SilentNamesDualBookProvenance = when (choice) {
        SilentNamesChoice.EXAMINE_SEAL -> SilentNamesDualBookProvenance(
            lotmConcepts = setOf(
                SilentNamesMotif.SEALED_KNOWLEDGE,
                SilentNamesMotif.DIVINATION_AND_INFERENCE,
                SilentNamesMotif.VEILED_ARCHIVE
            ),
            coiConcepts = setOf(
                SilentNamesMotif.MIRROR_SYMBOLISM,
                SilentNamesMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "An erased civic record is reconstructed through evidence and omen without importing book lore."
        )
        SilentNamesChoice.FOLLOW_LIGHT -> SilentNamesDualBookProvenance(
            lotmConcepts = setOf(
                SilentNamesMotif.DIVINATION_AND_INFERENCE,
                SilentNamesMotif.RITUAL_CONSEQUENCE
            ),
            coiConcepts = setOf(
                SilentNamesMotif.DREAM_LOGIC,
                SilentNamesMotif.MIRROR_SYMBOLISM,
                SilentNamesMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "A reflected dream-route changes under observation and creates two valid original routes."
        )
        SilentNamesChoice.SPEAK_TO_KEEPER -> SilentNamesDualBookProvenance(
            lotmConcepts = setOf(
                SilentNamesMotif.ROLE_CONSISTENT_ACTION,
                SilentNamesMotif.RITUAL_CONSEQUENCE
            ),
            coiConcepts = setOf(
                SilentNamesMotif.CONTRACT_AND_PRICE,
                SilentNamesMotif.CORRUPTION_RISK,
                SilentNamesMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "A clockwork witness frames information as a bounded contract whose cost never overrides user agency."
        )
    }

    fun forOutcome(outcome: SilentNamesOutcome): SilentNamesDualBookProvenance = when (outcome) {
        SilentNamesOutcome.RESTORED_INSCRIPTION,
        SilentNamesOutcome.WORKSHOP_TRAIL -> SilentNamesDualBookProvenance(
            lotmConcepts = setOf(
                SilentNamesMotif.SEALED_KNOWLEDGE,
                SilentNamesMotif.VEILED_ARCHIVE
            ),
            coiConcepts = setOf(
                SilentNamesMotif.MIRROR_SYMBOLISM,
                SilentNamesMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "Success and detour both reveal evidence; neither fabricates missing source truth."
        )
        SilentNamesOutcome.LANTERN_BRIDGE,
        SilentNamesOutcome.LOWER_PASSAGE -> SilentNamesDualBookProvenance(
            lotmConcepts = setOf(
                SilentNamesMotif.DIVINATION_AND_INFERENCE,
                SilentNamesMotif.RITUAL_CONSEQUENCE
            ),
            coiConcepts = setOf(
                SilentNamesMotif.DREAM_LOGIC,
                SilentNamesMotif.MIRROR_SYMBOLISM
            ),
            veilTransformation =
                "A route can be discovered directly or through its reflection, preserving consequence without dead-end failure."
        )
        SilentNamesOutcome.KEEPER_TESTIMONY,
        SilentNamesOutcome.KEEPER_REQUEST -> SilentNamesDualBookProvenance(
            lotmConcepts = setOf(
                SilentNamesMotif.ROLE_CONSISTENT_ACTION,
                SilentNamesMotif.RITUAL_CONSEQUENCE
            ),
            coiConcepts = setOf(
                SilentNamesMotif.CONTRACT_AND_PRICE,
                SilentNamesMotif.CORRUPTION_RISK
            ),
            veilTransformation =
                "The encounter makes terms and limits explicit; the user can accept information without surrendering future choices."
        )
    }

    fun forPath(pathId: String): SilentNamesDualBookProvenance = when (pathId) {
        "oracle" -> provenance(
            SilentNamesMotif.DIVINATION_AND_INFERENCE,
            SilentNamesMotif.FATE_PRESSURE,
            "Inference is strongest when certainty remains provisional."
        )
        "dreamwalker" -> provenance(
            SilentNamesMotif.RITUAL_CONSEQUENCE,
            SilentNamesMotif.DREAM_LOGIC,
            "Dream navigation is treated as changing spatial logic, not a cosmetic effect."
        )
        "archivist" -> provenance(
            SilentNamesMotif.VEILED_ARCHIVE,
            SilentNamesMotif.MIRROR_SYMBOLISM,
            "Missing information leaves catalog and reflection traces that can be interpreted without being invented."
        )
        "vanguard" -> provenance(
            SilentNamesMotif.ROLE_CONSISTENT_ACTION,
            SilentNamesMotif.FATE_PRESSURE,
            "Decisive action matters when routes close, but momentum never substitutes for evidence."
        )
        "nocturne" -> provenance(
            SilentNamesMotif.SEALED_KNOWLEDGE,
            SilentNamesMotif.CORRUPTION_RISK,
            "Silence, warning and temptation become readable signals without rewarding reckless escalation."
        )
        "artificer" -> provenance(
            SilentNamesMotif.RITUAL_CONSEQUENCE,
            SilentNamesMotif.CONTRACT_AND_PRICE,
            "Ritual and contract are exposed as mechanisms with inputs, costs and durable traces."
        )
        else -> provenance(
            SilentNamesMotif.DIVINATION_AND_INFERENCE,
            SilentNamesMotif.FATE_PRESSURE,
            "Unknown future Paths remain compatible through a neutral evidence-versus-uncertainty grammar."
        )
    }

    private fun provenance(
        lotm: SilentNamesMotif,
        coi: SilentNamesMotif,
        transform: String
    ) = SilentNamesDualBookProvenance(
        lotmConcepts = setOf(lotm),
        coiConcepts = setOf(coi),
        veilTransformation = transform
    )
}
