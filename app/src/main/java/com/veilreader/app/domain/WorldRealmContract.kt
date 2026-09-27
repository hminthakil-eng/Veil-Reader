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

enum class WorldEventAccess {
    NONE,
    READ_ONLY_PROJECTION
}

/**
 * Realm contract for W1.
 *
 * A realm may project World Kernel history into presentation, but the World Kernel never becomes
 * the owner of Reader persistence or progression. This contract does not prevent Sanctuary from
 * saving normal progress/session data through the existing Reader repositories.
 */
data class WorldRealmContract(
    val realm: WorldRealmId,
    val eventAccess: WorldEventAccess,
    val acceptsDecorativeWorldState: Boolean,
    val worldKernelMayMutateReadingHistory: Boolean = false,
    val worldKernelMayMutateProgression: Boolean = false,
    val mayAppendWorldEventsDirectly: Boolean = false,
    val mayPenalizeInactivity: Boolean = false
)

val canonicalWorldRealmContracts: List<WorldRealmContract> = listOf(
    WorldRealmContract(
        realm = WorldRealmId.THRESHOLD,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.GREAT_HALL,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.LIVING_MIRROR,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.ARCHIVE,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.OBSERVATORY,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.RITUAL,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.TREASURY,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.SANCTUM,
        eventAccess = WorldEventAccess.READ_ONLY_PROJECTION,
        acceptsDecorativeWorldState = true
    ),
    WorldRealmContract(
        realm = WorldRealmId.SANCTUARY,
        eventAccess = WorldEventAccess.NONE,
        acceptsDecorativeWorldState = false
    )
)

fun worldRealmContractFor(realm: WorldRealmId): WorldRealmContract =
    canonicalWorldRealmContracts.first { it.realm == realm }
