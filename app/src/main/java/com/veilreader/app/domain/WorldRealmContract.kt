package com.veilreader.app.domain

/**
 * Domain-level realm identity.
 *
 * Kept separate from UI rendering enums so the World Kernel cannot acquire a dependency on
 * Compose/theme code.
 */
data class WorldRealmId private constructor(val value: String) {
    companion object {
        val THRESHOLD = WorldRealmId("Threshold")
        val GREAT_HALL = WorldRealmId("GreatHall")
        val LIVING_MIRROR = WorldRealmId("LivingMirror")
        val ARCHIVE = WorldRealmId("Archive")
        val OBSERVATORY = WorldRealmId("Observatory")
        val RITUAL = WorldRealmId("Ritual")
        val TREASURY = WorldRealmId("Treasury")
        val SANCTUM = WorldRealmId("Sanctum")
        val SANCTUARY = WorldRealmId("Sanctuary")

        val canonical = listOf(
            THRESHOLD,
            GREAT_HALL,
            LIVING_MIRROR,
            ARCHIVE,
            OBSERVATORY,
            RITUAL,
            TREASURY,
            SANCTUM,
            SANCTUARY
        )
    }
}

enum class WorldHistoryAccess {
    NONE,
    READ_ONLY_PROJECTION
}

/**
 * Realm contract for W1.
 *
 * A realm may project durable history into presentation, but it never becomes the owner of that
 * history, progression, or Reader state. Inactivity is never allowed to punish or erase history.
 */
data class WorldRealmContract(
    val realm: WorldRealmId,
    val historyAccess: WorldHistoryAccess,
    val acceptsDecorativeWorldState: Boolean,
    val mayMutateReadingHistory: Boolean = false,
    val mayMutateProgression: Boolean = false,
    val mayAppendWorldEventsDirectly: Boolean = false,
    val mayPenalizeInactivity: Boolean = false
)

val canonicalWorldRealmContracts: List<WorldRealmContract> = listOf(
    WorldRealmContract(
        realm = WorldRealmId.THRESHOLD,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.GREAT_HALL,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.LIVING_MIRROR,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.ARCHIVE,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.OBSERVATORY,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.RITUAL,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.TREASURY,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.SANCTUM,
        historyAccess = WorldHistoryAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.SANCTUARY,
        historyAccess = WorldHistoryAccess.NONE,
        acceptsDecorativeWorldState = false
    )
)

fun worldRealmContractFor(realm: WorldRealmId): WorldRealmContract =
    canonicalWorldRealmContracts.first { it.realm == realm }
