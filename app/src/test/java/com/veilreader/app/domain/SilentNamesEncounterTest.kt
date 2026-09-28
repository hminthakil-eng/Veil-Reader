package com.veilreader.app.domain

import com.veilreader.app.data.SampleData
import org.junit.Assert.*
import org.junit.Test

class SilentNamesEncounterTest {
    private fun resolve(
        path: String = "oracle",
        choice: SilentNamesChoice = SilentNamesChoice.EXAMINE_SEAL,
        dice: SilentNamesDice? = SilentNamesDice(10),
        mode: SilentNamesMode = SilentNamesMode.DICE,
        existing: SilentNamesReceipt? = null
    ) = SilentNamesEncounter.resolve(path, choice, mode, dice, 1000L, existing)

    @Test fun `every existing path has exactly one specialty and every choice is accessible`() {
        for (path in SampleData.paths) {
            assertEquals(1, SilentNamesChoice.entries.count {
                SilentNamesEncounter.modifier(path.id, it) == 3
            })
            for (choice in SilentNamesChoice.entries) {
                for (face in 1..20) {
                    val resolution = resolve(path.id, choice, SilentNamesDice(face))
                    assertTrue(SilentNamesEncounter.isValid(resolution.receipt))
                    assertEquals(SilentNamesEncounter.REWARD_ID, resolution.receipt.rewardId)
                    assertTrue(resolution.needsCommit)
                }
            }
        }
    }

    @Test fun `advertised odds match exhaustive dice including advantage for every path and choice`() {
        val assured = setOf(SilentNamesOutcome.RESTORED_INSCRIPTION,
            SilentNamesOutcome.LANTERN_BRIDGE, SilentNamesOutcome.KEEPER_TESTIMONY)
        for (path in SampleData.paths.map { it.id } + "future_path") {
            for (choice in SilentNamesChoice.entries) {
                val normal = (1..20).count {
                    resolve(path, choice, SilentNamesDice(it)).receipt.outcome in assured
                } / 20.0
                var successes = 0
                for (first in 1..20) for (second in 1..20) {
                    if (resolve(path, choice, SilentNamesDice(first, second)).receipt.outcome in assured)
                        successes++
                }
                assertEquals(normal, SilentNamesEncounter.successProbability(path, choice, false), 1e-10)
                assertEquals(successes / 400.0,
                    SilentNamesEncounter.successProbability(path, choice, true), 1e-10)
            }
        }
    }

    @Test fun `threshold is inclusive and low rolls take an alternate route`() {
        assertEquals(SilentNamesOutcome.WORKSHOP_TRAIL,
            resolve(dice = SilentNamesDice(9)).receipt.outcome)
        assertEquals(SilentNamesOutcome.RESTORED_INSCRIPTION,
            resolve(dice = SilentNamesDice(10)).receipt.outcome)
        assertEquals(SilentNamesOutcome.RESTORED_INSCRIPTION,
            resolve(dice = SilentNamesDice(1, 10)).receipt.outcome)
        assertEquals(0.55, SilentNamesEncounter.successProbability("oracle",
            SilentNamesChoice.EXAMINE_SEAL, false), 1e-10)
        assertEquals(0.7975, SilentNamesEncounter.successProbability("oracle",
            SilentNamesChoice.EXAMINE_SEAL, true), 1e-10)
    }

    @Test fun `story mode offers each choice without a die and gives the same reward`() {
        val outcomes = SilentNamesChoice.entries.map {
            val r = resolve(choice = it, mode = SilentNamesMode.STORY, dice = null)
            assertTrue(SilentNamesEncounter.isValid(r.receipt))
            assertEquals(SilentNamesEncounter.REWARD_ID, r.receipt.rewardId)
            assertNull(r.receipt.dice)
            r.receipt.outcome
        }
        assertEquals(3, outcomes.toSet().size)
    }

    @Test fun `restored receipt wins over new die choice path and timestamp`() {
        val original = resolve(dice = SilentNamesDice(1)).receipt
        // Reconstituted value emulates deserialization, not an in-memory identity check.
        val restored = original.copy()
        val retried = SilentNamesEncounter.resolve("artificer", SilentNamesChoice.SPEAK_TO_KEEPER,
            SilentNamesMode.STORY, null, 999999L, restored)
        assertEquals(original, retried.receipt)
        assertFalse(retried.needsCommit)
    }

    @Test fun `invalid or future receipts cannot be reset into another reward`() {
        val original = resolve().receipt
        val invalid = listOf(
            original.copy(contentVersion = 2), original.copy(encounterId = "other"),
            original.copy(rewardId = "extra_reward"), original.copy(recordedAtEpochMs = 0L),
            original.copy(outcome = SilentNamesOutcome.LOWER_PASSAGE),
            original.copy(dice = null), original.copy(pathId = " ")
        )
        for (receipt in invalid) {
            assertFalse(SilentNamesEncounter.isValid(receipt))
            assertThrows(IllegalArgumentException::class.java) { resolve(existing = receipt) }
        }
    }

    @Test fun `invalid dice and mismatched modes fail before creating a resolution`() {
        for (face in listOf(Int.MIN_VALUE, 0, 21, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { SilentNamesDice(face) }
            assertThrows(IllegalArgumentException::class.java) { SilentNamesDice(10, face) }
        }
        assertThrows(IllegalArgumentException::class.java) { resolve(dice = null) }
        assertThrows(IllegalArgumentException::class.java) { resolve(mode = SilentNamesMode.STORY) }
        assertThrows(IllegalArgumentException::class.java) { resolve(path = "") }
    }

    @Test fun `unknown path has neutral odds and can finish without losing its identity`() {
        val receipt = resolve(path = "future_path", dice = SilentNamesDice(1)).receipt
        assertEquals("future_path", receipt.pathId)
        assertTrue(SilentNamesEncounter.isValid(receipt))
        assertEquals(0.45, SilentNamesEncounter.successProbability("future_path",
            SilentNamesChoice.EXAMINE_SEAL, false), 1e-10)
    }
}
