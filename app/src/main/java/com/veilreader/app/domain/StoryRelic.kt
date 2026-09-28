package com.veilreader.app.domain

/**
 * A read-only projection of an already-durable story receipt.
 *
 * Story relics never become a second persistence owner. Encounter repositories keep their
 * authoritative receipts; this model only gives Castle surfaces one stable, reusable contract.
 */
data class StoryRelicRecord(
    val relicId: String,
    val sourceEncounterId: String,
    val sourceContentVersion: Int,
    val pathIdAtAcquisition: String,
    val recordedAtEpochMs: Long,
    val routeVariantId: String,
    val resolutionModeId: String
) {
    init {
        require(relicId.isNotBlank())
        require(sourceEncounterId.isNotBlank())
        require(sourceContentVersion > 0)
        require(pathIdAtAcquisition.isNotBlank())
        require(recordedAtEpochMs > 0L)
        require(routeVariantId.isNotBlank())
        require(resolutionModeId.isNotBlank())
    }
}

/**
 * Canonical IDs and world-level behavior for story collectibles.
 *
 * The catalog describes only product semantics shared by domain surfaces. Localized prose,
 * glyphs and visual treatment stay in UI presentation registries.
 */
data class StoryRelicDefinition(
    val id: String,
    val worldTitle: String,
    val worldInscription: String,
    val visibleRealms: Set<WorldMutationRealm>,
    val atmosphereSeedSalt: Int
) {
    init {
        require(id.isNotBlank())
        require(worldTitle.isNotBlank())
        require(worldInscription.isNotBlank())
        require(visibleRealms.isNotEmpty())
    }
}

object StoryRelicCatalog {
    val lanternOfRemembrance = StoryRelicDefinition(
        id = SilentNamesEncounter.REWARD_ID,
        worldTitle = "Lantern of Remembrance",
        worldInscription =
            "A sealed choice in the Hall has become a permanent story relic. It records no reading claim and grants no rank or access.",
        visibleRealms = setOf(
            WorldMutationRealm.GREAT_HALL,
            WorldMutationRealm.TREASURY
        ),
        atmosphereSeedSalt = 211
    )

    private val byId = listOf(lanternOfRemembrance).associateBy(StoryRelicDefinition::id)

    fun definitionFor(relicId: String): StoryRelicDefinition? = byId[relicId]

    fun knownDefinitions(): List<StoryRelicDefinition> = byId.values.toList()
}

/**
 * Converts known valid encounter receipts into Castle projections.
 *
 * Invalid/future receipts intentionally project nothing. Their authoritative raw data remains
 * retained by the owning repository until a compatible app version can understand it.
 */
fun deriveStoryRelics(
    silentNamesReceipt: SilentNamesReceipt? = null
): List<StoryRelicRecord> = buildList {
    silentNamesReceipt
        ?.takeIf(SilentNamesEncounter::isValid)
        ?.let { receipt ->
            add(
                StoryRelicRecord(
                    relicId = receipt.rewardId,
                    sourceEncounterId = receipt.encounterId,
                    sourceContentVersion = receipt.contentVersion,
                    pathIdAtAcquisition = receipt.pathId,
                    recordedAtEpochMs = receipt.recordedAtEpochMs,
                    routeVariantId = receipt.outcome.name,
                    resolutionModeId = receipt.mode.name
                )
            )
        }
}

/** Stable deduplication by relic identity for future multi-encounter aggregation. */
fun normalizeStoryRelics(records: Iterable<StoryRelicRecord>): List<StoryRelicRecord> =
    records
        .groupBy(StoryRelicRecord::relicId)
        .values
        .map { sameRelic ->
            sameRelic.minWith(
                compareBy<StoryRelicRecord>(
                    StoryRelicRecord::recordedAtEpochMs,
                    StoryRelicRecord::sourceEncounterId
                )
            )
        }
        .sortedWith(
            compareBy<StoryRelicRecord>(
                StoryRelicRecord::recordedAtEpochMs,
                StoryRelicRecord::relicId
            )
        )
