package com.veilreader.app.domain

enum class ReaderTapAction {
    VEIL_DEFAULT,
    PREVIOUS_PAGE,
    TOGGLE_CONTROLS,
    NEXT_PAGE,
    RENDERER
}

enum class ReaderTapZone {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    MIDDLE_LEFT,
    MIDDLE_CENTER,
    MIDDLE_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

data class ReaderTapGrid(
    val actions: Map<ReaderTapZone, ReaderTapAction> = defaultReaderTapActions()
) {
    operator fun get(zone: ReaderTapZone): ReaderTapAction =
        actions[zone] ?: defaultReaderTapAction(zone)

    fun withAction(zone: ReaderTapZone, action: ReaderTapAction): ReaderTapGrid =
        copy(actions = actions + (zone to action))

    fun reset(): ReaderTapGrid = ReaderTapGrid()
}

fun defaultReaderTapAction(zone: ReaderTapZone): ReaderTapAction {
    @Suppress("UNUSED_PARAMETER")
    val ignored = zone
    return ReaderTapAction.VEIL_DEFAULT
}

fun defaultReaderTapActions(): Map<ReaderTapZone, ReaderTapAction> =
    ReaderTapZone.entries.associateWith(::defaultReaderTapAction)

fun readerTapZoneAt(
    x: Float,
    y: Float,
    width: Float,
    height: Float
): ReaderTapZone? {
    if (!x.isFinite() || !y.isFinite() || !width.isFinite() || !height.isFinite()) return null
    if (width <= 0f || height <= 0f) return null
    if (x < 0f || y < 0f || x > width || y > height) return null

    val column = (x / (width / 3f)).toInt().coerceIn(0, 2)
    val row = (y / (height / 3f)).toInt().coerceIn(0, 2)
    return ReaderTapZone.entries[row * 3 + column]
}

fun encodeReaderTapGrid(grid: ReaderTapGrid): String =
    "v1:" + ReaderTapZone.entries.joinToString(",") { zone -> grid[zone].name }

fun decodeReaderTapGrid(raw: String?): ReaderTapGrid {
    if (raw.isNullOrBlank()) return ReaderTapGrid()
    val payload = raw.removePrefix("v1:")
    val tokens = payload.split(',')
    if (tokens.size != ReaderTapZone.entries.size) return ReaderTapGrid()

    val decoded = buildMap {
        ReaderTapZone.entries.forEachIndexed { index, zone ->
            val action = runCatching {
                ReaderTapAction.valueOf(tokens[index])
            }.getOrElse {
                defaultReaderTapAction(zone)
            }
            put(zone, action)
        }
    }
    return ReaderTapGrid(decoded)
}
