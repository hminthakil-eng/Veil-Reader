package com.veilreader.app.domain

/** A small, original encounter. This policy never grants XP, rank, rooms or reading evidence. */
enum class SilentNamesChoice { EXAMINE_SEAL, FOLLOW_LIGHT, SPEAK_TO_KEEPER }

enum class SilentNamesMode { DICE, STORY }

enum class SilentNamesOutcome {
    RESTORED_INSCRIPTION, WORKSHOP_TRAIL,
    LANTERN_BRIDGE, LOWER_PASSAGE,
    KEEPER_TESTIMONY, KEEPER_REQUEST
}

/** Dice are supplied once by the caller, then stored with the resolution, never rerolled on render. */
data class SilentNamesDice(val first: Int, val second: Int? = null) {
    init {
        require(first in 1..20)
        require(second == null || second in 1..20)
    }

    val hasAdvantage: Boolean get() = second != null
    val selected: Int get() = maxOf(first, second ?: first)
}

data class SilentNamesReceipt(
    val encounterId: String,
    val contentVersion: Int,
    val pathId: String,
    val choice: SilentNamesChoice,
    val mode: SilentNamesMode,
    val dice: SilentNamesDice?,
    val outcome: SilentNamesOutcome,
    val rewardId: String,
    val recordedAtEpochMs: Long
)

/**
 * needsCommit is an intent, not proof of durability. The repository must read the existing
 * receipt before resolution and durably commit one authoritative receipt. Cosmetic reward
 * ownership is derived from that committed receipt, avoiding a second fallible award write.
 * Do not expose the reward before persistence succeeds. Replays are read-only.
 */
data class SilentNamesResolution(val receipt: SilentNamesReceipt, val needsCommit: Boolean)

object SilentNamesEncounter {
    const val ID = "silent_names_window"
    const val CONTENT_VERSION = 1
    const val REWARD_ID = "silent_names_memory_lantern"
    const val DIFFICULTY = 13

    // Existing canonical path IDs, not a second character/progression system.
    private val specialties = mapOf(
        "oracle" to SilentNamesChoice.EXAMINE_SEAL,
        "archivist" to SilentNamesChoice.EXAMINE_SEAL,
        "dreamwalker" to SilentNamesChoice.FOLLOW_LIGHT,
        "vanguard" to SilentNamesChoice.FOLLOW_LIGHT,
        "nocturne" to SilentNamesChoice.SPEAK_TO_KEEPER,
        "artificer" to SilentNamesChoice.SPEAK_TO_KEEPER
    )

    /** Unknown future paths can still finish the encounter, using the neutral modifier. */
    fun modifier(pathId: String, choice: SilentNamesChoice): Int =
        if (specialties[pathId] == choice) 3 else 1

    fun successProbability(pathId: String, choice: SilentNamesChoice, advantage: Boolean): Double {
        val successfulFaces = (21 - DIFFICULTY + modifier(pathId, choice)).coerceIn(0, 20)
        val p = successfulFaces / 20.0
        return if (advantage) 1.0 - (1.0 - p) * (1.0 - p) else p
    }

    /**
     * Existing valid receipts win even when the retried command has different inputs. Invalid or
     * newer-version receipts fail closed: callers must retain them, not reset the encounter.
     * STORY takes the assured branch without a roll; all DICE branches still finish the episode.
     */
    fun resolve(
        pathId: String,
        choice: SilentNamesChoice,
        mode: SilentNamesMode,
        dice: SilentNamesDice?,
        recordedAtEpochMs: Long,
        existing: SilentNamesReceipt? = null
    ): SilentNamesResolution {
        if (existing != null) {
            require(isValid(existing)) { "Retain unsupported or invalid encounter receipt" }
            return SilentNamesResolution(existing, needsCommit = false)
        }
        require(pathId.isNotBlank())
        require(recordedAtEpochMs > 0L)
        require((mode == SilentNamesMode.DICE) == (dice != null))
        return SilentNamesResolution(
            receipt = SilentNamesReceipt(
                encounterId = ID,
                contentVersion = CONTENT_VERSION,
                pathId = pathId,
                choice = choice,
                mode = mode,
                dice = dice,
                outcome = outcome(pathId, choice, mode, dice),
                rewardId = REWARD_ID,
                recordedAtEpochMs = recordedAtEpochMs
            ),
            needsCommit = true
        )
    }

    fun isValid(receipt: SilentNamesReceipt): Boolean =
        receipt.encounterId == ID &&
            receipt.contentVersion == CONTENT_VERSION &&
            receipt.pathId.isNotBlank() &&
            receipt.rewardId == REWARD_ID &&
            receipt.recordedAtEpochMs > 0L &&
            ((receipt.mode == SilentNamesMode.DICE) == (receipt.dice != null)) &&
            receipt.outcome == outcome(receipt.pathId, receipt.choice, receipt.mode, receipt.dice)

    private fun outcome(
        pathId: String,
        choice: SilentNamesChoice,
        mode: SilentNamesMode,
        dice: SilentNamesDice?
    ): SilentNamesOutcome {
        val succeeds = mode == SilentNamesMode.STORY ||
            (requireNotNull(dice).selected + modifier(pathId, choice) >= DIFFICULTY)
        return when (choice) {
            SilentNamesChoice.EXAMINE_SEAL -> if (succeeds)
                SilentNamesOutcome.RESTORED_INSCRIPTION else SilentNamesOutcome.WORKSHOP_TRAIL
            SilentNamesChoice.FOLLOW_LIGHT -> if (succeeds)
                SilentNamesOutcome.LANTERN_BRIDGE else SilentNamesOutcome.LOWER_PASSAGE
            SilentNamesChoice.SPEAK_TO_KEEPER -> if (succeeds)
                SilentNamesOutcome.KEEPER_TESTIMONY else SilentNamesOutcome.KEEPER_REQUEST
        }
    }
}
