package com.veilreader.app.domain

data class MysteryChainDefinition(
    val id: String,
    val fragments: List<String>
)

data class MysteryChainSnapshot(
    val id: String,
    val visibleFragmentIndex: Int,
    val visibleClue: String,
    val complete: Boolean
)

private val mysteryDefinitions = listOf(
    MysteryChainDefinition(
        id = VeiledDiscoveryPolicy.PATIENT_FLAME,
        fragments = listOf(
            "A quiet flame answers duration before it answers devotion.",
            "Returning matters more than one long vigil.",
            "Time and return must become the same habit."
        )
    ),
    MysteryChainDefinition(
        id = VeiledDiscoveryPolicy.MARGINALIA_GATE,
        fragments = listOf(
            "The margin begins as an echo.",
            "A second text forms only when preserved passages become numerous.",
            "The gate appears when reading and marginalia become inseparable."
        )
    ),
    MysteryChainDefinition(
        id = VeiledDiscoveryPolicy.DEEP_SHELF,
        fragments = listOf(
            "Finished books weigh more than unfinished possibility.",
            "A Path threshold changes what the shelf can bear.",
            "Completion and advancement must reinforce the same foundation."
        )
    ),
    MysteryChainDefinition(
        id = VeiledDiscoveryPolicy.LONG_WATCH,
        fragments = listOf(
            "Some clocks measure pages badly.",
            "Long attention changes the room before it changes the title.",
            "Remain long enough that counted time stops being the point."
        )
    ),
    MysteryChainDefinition(
        id = VeiledDiscoveryPolicy.VEIL_THINS,
        fragments = listOf(
            "One sigil is a mark. Several begin to answer one another.",
            "The pattern needs more than isolated milestones.",
            "When enough marks resonate, the hidden surface becomes legible."
        )
    ),
    MysteryChainDefinition(
        id = VeiledDiscoveryPolicy.UNNAMED_CHAMBER,
        fragments = listOf(
            "The deepest door ignores solitary achievements.",
            "Mature Path identity must meet a nearly complete constellation.",
            "Only convergence reveals the chamber that early ranks cannot name."
        )
    )
)

fun mysteryChainDefinitions(): List<MysteryChainDefinition> = mysteryDefinitions

fun mysteryChainSnapshot(
    id: String,
    profile: ReaderProfile,
    highlightCount: Int
): MysteryChainSnapshot? {
    val definition = mysteryDefinitions.firstOrNull { it.id == id } ?: return null
    val complete = id in profile.earnedDiscoveries
    if (complete) {
        return MysteryChainSnapshot(
            id = id,
            visibleFragmentIndex = definition.fragments.lastIndex,
            visibleClue = definition.fragments.last(),
            complete = true
        )
    }

    val durableSevenDayReturn =
        profile.streakDays >= 7 || "seven_days" in profile.earnedSigils

    val stage = when (id) {
        VeiledDiscoveryPolicy.PATIENT_FLAME -> when {
            durableSevenDayReturn -> 2
            profile.minutesRead >= 180 -> 1
            else -> 0
        }
        VeiledDiscoveryPolicy.MARGINALIA_GATE -> when {
            profile.pagesRead >= 500 && highlightCount >= 3 -> 2
            highlightCount >= 3 -> 1
            else -> 0
        }
        VeiledDiscoveryPolicy.DEEP_SHELF -> when {
            profile.rankIndex >= 1 && profile.booksFinished >= 3 -> 2
            profile.booksFinished >= 3 -> 1
            else -> 0
        }
        VeiledDiscoveryPolicy.LONG_WATCH -> when {
            profile.minutesRead >= 1_500 -> 2
            profile.minutesRead >= 600 -> 1
            else -> 0
        }
        VeiledDiscoveryPolicy.VEIL_THINS -> when {
            profile.earnedSigils.size >= 3 -> 2
            profile.earnedSigils.size >= 2 -> 1
            else -> 0
        }
        VeiledDiscoveryPolicy.UNNAMED_CHAMBER -> when {
            profile.rankIndex >= 2 && profile.earnedSigils.size >= 4 -> 2
            profile.rankIndex >= 2 || profile.earnedSigils.size >= 4 -> 1
            else -> 0
        }
        else -> 0
    }.coerceIn(0, definition.fragments.lastIndex)

    return MysteryChainSnapshot(
        id = id,
        visibleFragmentIndex = stage,
        visibleClue = definition.fragments[stage],
        complete = false
    )
}
