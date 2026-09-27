package com.veilreader.app.ui.screens

internal enum class AdvancementCeremonyStage { INVOCATION, SEALING, REVEALED, REJECTED }

internal fun canConfirmAdvancementCeremony(stage: AdvancementCeremonyStage): Boolean =
    stage == AdvancementCeremonyStage.INVOCATION

internal fun canDismissAdvancementCeremony(stage: AdvancementCeremonyStage): Boolean =
    stage != AdvancementCeremonyStage.SEALING

/** The frozen target is confirmed by the repository's current profile, not by a timer alone. */
internal fun advancementStageAfterSeal(
    expectedPathId: String,
    fromRankIndex: Int,
    currentPathId: String,
    currentRankIndex: Int
): AdvancementCeremonyStage =
    if (expectedPathId == currentPathId && currentRankIndex > fromRankIndex) {
        AdvancementCeremonyStage.REVEALED
    } else {
        AdvancementCeremonyStage.REJECTED
    }
