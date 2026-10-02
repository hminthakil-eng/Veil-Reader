package com.veilreader.rd

enum class TapZone {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MIDDLE_LEFT, CENTER, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

enum class ReaderInputAction {
    PREVIOUS_PAGE, NEXT_PAGE, TOGGLE_CHROME, OPEN_CONTENTS, OPEN_APPEARANCE,
    BOOKMARK, SEARCH, NOTES, NONE
}

data class ReaderInputProfile(
    val id: String,
    val zoneActions: Map<TapZone, ReaderInputAction>,
    val keyActions: Map<String, ReaderInputAction> = emptyMap()
) {
    init { require(id.isNotBlank()) }

    fun actionFor(zone: TapZone): ReaderInputAction =
        zoneActions[zone] ?: ReaderInputAction.NONE
}

object ReaderInputProfileDefaults {
    val calm: ReaderInputProfile = ReaderInputProfile(
        id = "calm",
        zoneActions = mapOf(
            TapZone.MIDDLE_LEFT to ReaderInputAction.PREVIOUS_PAGE,
            TapZone.CENTER to ReaderInputAction.TOGGLE_CHROME,
            TapZone.MIDDLE_RIGHT to ReaderInputAction.NEXT_PAGE
        )
    )
}
