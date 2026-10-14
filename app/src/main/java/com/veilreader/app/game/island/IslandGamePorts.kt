package com.veilreader.app.game.island

/**
 * Stable seams: a renderer is a replaceable consumer, never an authority for game state.
 * No Readium, Android View, Godot, Compose, database or private-book types cross this API.
 */
data class ReaderRewardTotals(
    val committedActiveMinutes: Long,
    val finishedBooks: Int
)

/** Adapts durable counters already owned by GameRepository, NOT a second ledger. */
fun interface EligibleReadingTotalsPort {
    fun readTotals(): ReaderRewardTotals
}

/** Implement with a real Room transaction: reducer result + action journal in one write. */
interface IslandShopStatePort {
    suspend fun load(): IslandShopState
    suspend fun dispatch(command: ShopCommand): ShopDecision
}

/** Saved customization is owned by AvatarAppearanceStore, never duplicated into city state. */
data class AvatarVisualProjection(
    val hairStyleId: String,
    val hairPaletteId: String,
    val outfitId: String,
    val skinPaletteId: String,
    val accessoryId: String? = null
)

data class PixelCityScene(
    val mapId: String,
    val virtualDay: Int,
    val actor: AvatarVisualProjection,
    val reducedMotion: Boolean,
    val pixelScale: Int
)

/** Native 2D / libGDX / Godot implementations share this port. */
interface IslandScenePort {
    suspend fun open(scene: PixelCityScene)
    suspend fun pause()
    /** Must release world resources before caller navigates back to Reader. */
    suspend fun releaseBeforeReader(): SceneReleaseReceipt
}

data class SceneReleaseReceipt(
    val resourcesReleased: Boolean,
    val rendererId: String,
    val errorCode: String? = null
)

/** Separate original story assets; no real user EPUB title or content should appear here. */
interface IslandContentPort {
    fun hasMap(mapId: String): Boolean
    fun hasPortrait(assetId: String): Boolean
    fun hasFictionBook(bookId: String): Boolean
}
