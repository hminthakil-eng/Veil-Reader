package com.veilreader.app.ui.screens

import com.veilreader.app.domain.VeiledDiscoveryPolicy

internal data class VeiledDiscoveryPresentation(
    val id: String,
    val symbol: String,
    val title: String,
    val clue: String,
    val lore: String
)

internal val veiledDiscoveryPresentations = listOf(
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.PATIENT_FLAME,
        symbol = "◈",
        title = "The Patient Flame",
        clue = "A flame kept for many returns begins to remember the hand that lit it.",
        lore = "Consistency leaves a different mark than intensity. The Castle has begun to recognize your return."
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.MARGINALIA_GATE,
        symbol = "✧",
        title = "The Marginalia Gate",
        clue = "Some doors are written in the margins rather than printed on the page.",
        lore = "Enough passages have been preserved that your annotations now form a second text beside the books themselves."
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.DEEP_SHELF,
        symbol = "▥",
        title = "The Deep Shelf",
        clue = "Finished volumes gather weight. Eventually the shelf becomes a foundation.",
        lore = "Your completed books and first Path threshold now reinforce one another. The archive is becoming a place, not a list."
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.LONG_WATCH,
        symbol = "◐",
        title = "The Long Watch",
        clue = "There is a point when time spent reading stops feeling counted.",
        lore = "Fifty hours have passed inside books. The Castle records the duration, but the deeper change cannot be measured in minutes."
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.VEIL_THINS,
        symbol = "⌁",
        title = "When the Veil Thins",
        clue = "Several marks must awaken before they begin to answer one another.",
        lore = "Your earned sigils are no longer isolated milestones. Together they form the first readable pattern in the Veil."
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.UNNAMED_CHAMBER,
        symbol = "⬡",
        title = "The Unnamed Chamber",
        clue = "The deepest chamber does not open to a single achievement.",
        lore = "A mature Path and a complete core sigil constellation have revealed a chamber that the early Castle could not name."
    )
)

internal fun discoveryPresentationFor(id: String): VeiledDiscoveryPresentation? =
    veiledDiscoveryPresentations.firstOrNull { it.id == id }
