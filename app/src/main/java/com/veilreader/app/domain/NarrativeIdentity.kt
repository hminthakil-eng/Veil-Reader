package com.veilreader.app.domain

/**
 * Shared dual-source design vocabulary for Veil-authored gamification.
 *
 * These are abstract design motifs only. They are not copied characters, factions, rank ladders,
 * dialogue, plot or lore from either reference novel.
 */
enum class DualBookMotif {
    SEALED_KNOWLEDGE,
    DIVINATION_AND_INFERENCE,
    ROLE_EMBODIMENT,
    RITUAL_TRANSFORMATION,
    HIDDEN_ORDER,
    DREAM_LOGIC,
    MIRROR_SYMBOLISM,
    FATE_PRESSURE,
    CONTRACT_AND_PRICE,
    CORRUPTION_RISK,
    CYCLICAL_RETURN,
    MECHANISM_AND_CONSEQUENCE
}

data class DualBookProvenance(
    val lotmConcepts: Set<DualBookMotif>,
    val coiConcepts: Set<DualBookMotif>,
    val veilTransformation: String
) {
    init {
        require(lotmConcepts.isNotEmpty())
        require(coiConcepts.isNotEmpty())
        require(veilTransformation.isNotBlank())
    }
}

data class NarrativeCovenant(
    val id: String,
    val name: String,
    val vow: String,
    val invitation: String,
    val affinityPaths: Set<String>,
    val provenance: DualBookProvenance
)

private val narrativeCovenants = listOf(
    NarrativeCovenant(
        id = "unwritten_margin",
        name = "Covenant of the Unwritten Margin",
        vow = "Interpret before declaring.",
        invitation =
            "Treat absence as evidence without inventing what the page never gave you.",
        affinityPaths = setOf("oracle", "archivist"),
        provenance = DualBookProvenance(
            lotmConcepts = setOf(
                DualBookMotif.SEALED_KNOWLEDGE,
                DualBookMotif.DIVINATION_AND_INFERENCE
            ),
            coiConcepts = setOf(
                DualBookMotif.MIRROR_SYMBOLISM,
                DualBookMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "Veil turns hidden-record investigation into a voluntary reading ethic: preserve uncertainty instead of fabricating certainty."
        )
    ),
    NarrativeCovenant(
        id = "returning_lamp",
        name = "Covenant of the Returning Lamp",
        vow = "Return with evidence.",
        invitation =
            "Immersion matters only if something survives the return: a passage, a note, a completed arc, a changed understanding.",
        affinityPaths = setOf("dreamwalker", "vanguard"),
        provenance = DualBookProvenance(
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
                "Veil turns journey and dream structures into a non-punitive promise to carry durable reading evidence back into the Castle."
        )
    ),
    NarrativeCovenant(
        id = "measured_price",
        name = "Covenant of the Measured Price",
        vow = "Name the cost before accepting the gift.",
        invitation =
            "Power, shortcuts and promises are never free; inspect the mechanism and keep your future choices yours.",
        affinityPaths = setOf("nocturne", "artificer"),
        provenance = DualBookProvenance(
            lotmConcepts = setOf(
                DualBookMotif.CORRUPTION_RISK,
                DualBookMotif.MECHANISM_AND_CONSEQUENCE
            ),
            coiConcepts = setOf(
                DualBookMotif.CONTRACT_AND_PRICE,
                DualBookMotif.FATE_PRESSURE,
                DualBookMotif.CORRUPTION_RISK
            ),
            veilTransformation =
                "Veil turns dangerous-power and contract motifs into explicit consent, visible cost and recoverable choice rather than punitive mechanics."
        )
    )
)

fun allNarrativeCovenants(): List<NarrativeCovenant> = narrativeCovenants

fun narrativeCovenantFor(id: String?): NarrativeCovenant? =
    narrativeCovenants.firstOrNull { it.id == id }

enum class CovenantResonance { AFFINITY, CROSS_CURRENT }

fun covenantResonance(pathId: String, covenantId: String?): CovenantResonance? =
    narrativeCovenantFor(covenantId)?.let { covenant ->
        if (pathId in covenant.affinityPaths) CovenantResonance.AFFINITY
        else CovenantResonance.CROSS_CURRENT
    }

data class VeilOrder(
    val id: String,
    val name: String,
    val maxim: String,
    val pathIds: Set<String>,
    val worldFunction: String,
    val provenance: DualBookProvenance
)

private val veilOrders = listOf(
    VeilOrder(
        id = "palimpsest",
        name = "The Palimpsest",
        maxim = "What vanishes leaves a shape.",
        pathIds = setOf("oracle", "archivist"),
        worldFunction =
            "Custodians of missing indexes, contradictory records and questions that must remain open.",
        provenance = DualBookProvenance(
            lotmConcepts = setOf(
                DualBookMotif.SEALED_KNOWLEDGE,
                DualBookMotif.DIVINATION_AND_INFERENCE,
                DualBookMotif.HIDDEN_ORDER
            ),
            coiConcepts = setOf(
                DualBookMotif.MIRROR_SYMBOLISM,
                DualBookMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "An original archive-order that protects uncertainty and records provenance instead of claiming supernatural truth."
        )
    ),
    VeilOrder(
        id = "wayfarer_lantern",
        name = "The Wayfarer Lantern",
        maxim = "Cross the threshold; bring something back.",
        pathIds = setOf("dreamwalker", "vanguard"),
        worldFunction =
            "Keepers of routes, returns and the difference between momentum and meaningful passage.",
        provenance = DualBookProvenance(
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
                "An original traveling order that values completed return and recorded memory over speed or grind."
        )
    ),
    VeilOrder(
        id = "brass_vigil",
        name = "The Brass Vigil",
        maxim = "Every promise has a mechanism.",
        pathIds = setOf("nocturne", "artificer"),
        worldFunction =
            "Watchers of thresholds, mechanisms, bargains and consequences that should never be hidden from the reader.",
        provenance = DualBookProvenance(
            lotmConcepts = setOf(
                DualBookMotif.CORRUPTION_RISK,
                DualBookMotif.MECHANISM_AND_CONSEQUENCE,
                DualBookMotif.HIDDEN_ORDER
            ),
            coiConcepts = setOf(
                DualBookMotif.CONTRACT_AND_PRICE,
                DualBookMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "An original vigilance order that turns occult cost into transparent product consequences and visible system rules."
        )
    )
)

fun allVeilOrders(): List<VeilOrder> = veilOrders

fun veilOrderForPath(pathId: String): VeilOrder =
    veilOrders.firstOrNull { pathId in it.pathIds } ?: veilOrders.first()

enum class RelicTemperament {
    DORMANT,
    WATCHFUL,
    DREAMING,
    EXACTING,
    STEADFAST,
    THRESHOLD_SEEKING,
    SILENT
}

data class RelicTemperamentState(
    val temperament: RelicTemperament,
    val label: String,
    val description: String,
    val provenance: DualBookProvenance
)

/**
 * Relic temperament is descriptive only. It never changes unlock thresholds, rank, XP or reading
 * records. The same durable evidence always yields the same ownership result.
 */
fun deriveRelicTemperament(
    relicId: String,
    pathId: String,
    awakened: Boolean,
    evidenceCount: Int,
    target: Int,
    covenantId: String? = null
): RelicTemperamentState {
    if (!awakened) {
        return RelicTemperamentState(
            temperament = RelicTemperament.DORMANT,
            label = "Dormant",
            description = "The relic has a shape, but not yet enough reading evidence to answer.",
            provenance = DualBookProvenance(
                lotmConcepts = setOf(DualBookMotif.SEALED_KNOWLEDGE),
                coiConcepts = setOf(DualBookMotif.FATE_PRESSURE),
                veilTransformation =
                    "A sealed presentation state communicates missing evidence without implying punishment or supernatural certainty."
            )
        )
    }

    val progress = if (target <= 0) 1f else
        (evidenceCount.coerceAtLeast(0).toFloat() / target).coerceAtLeast(0f)
    val covenant = narrativeCovenantFor(covenantId)
    val order = veilOrderForPath(pathId)

    val base = when (relicId) {
        "moonlit_lens" -> RelicTemperament.WATCHFUL
        "brass_quill" -> RelicTemperament.EXACTING
        "ivory_bookplate" -> RelicTemperament.STEADFAST
        "astral_key" -> RelicTemperament.THRESHOLD_SEEKING
        "veil_crown" -> RelicTemperament.SILENT
        else -> if (pathId == "dreamwalker") RelicTemperament.DREAMING
        else RelicTemperament.STEADFAST
    }
    val label = when (base) {
        RelicTemperament.DORMANT -> "Dormant"
        RelicTemperament.WATCHFUL -> "Watchful"
        RelicTemperament.DREAMING -> "Dreaming"
        RelicTemperament.EXACTING -> "Exacting"
        RelicTemperament.STEADFAST -> "Steadfast"
        RelicTemperament.THRESHOLD_SEEKING -> "Threshold-seeking"
        RelicTemperament.SILENT -> "Silent"
    }
    val covenantClause = covenant?.let { " It remembers the vow: “${it.vow}”" }.orEmpty()
    val maturityClause = when {
        progress >= 2f -> " Its response has settled into a mature pattern."
        progress >= 1.25f -> " Repeated evidence has made its response more distinct."
        else -> " It has only just awakened."
    }
    return RelicTemperamentState(
        temperament = base,
        label = label,
        description = "${order.name} reads this relic as $label.$maturityClause$covenantClause",
        provenance = DualBookProvenance(
            lotmConcepts = setOf(
                DualBookMotif.SEALED_KNOWLEDGE,
                DualBookMotif.MECHANISM_AND_CONSEQUENCE
            ),
            coiConcepts = setOf(
                DualBookMotif.CONTRACT_AND_PRICE,
                DualBookMotif.FATE_PRESSURE
            ),
            veilTransformation =
                "Relics gain a deterministic narrative temperament from evidence and identity, never a hidden stat or random personality roll."
        )
    )
}
