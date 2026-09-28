package com.veilreader.app.domain

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RankWorldMutationTest {
    @Test
    fun `every canonical path has dual-source realm copy for every world surface`() {
        SampleData.paths.forEach { path ->
            path.ranks.indices.forEach { rankIndex ->
                WorldMutationRealm.entries.forEach { realm ->
                    val mutation = deriveRankRealmMutation(
                        pathId = path.id,
                        rankName = path.ranks[rankIndex],
                        rankIndex = rankIndex,
                        rankCount = path.ranks.size,
                        realm = realm
                    )
                    assertTrue(mutation.title.isNotBlank())
                    assertTrue(mutation.inscription.isNotBlank())
                    assertFalse(mutation.provenance.lotmConcepts.isEmpty())
                    assertFalse(mutation.provenance.coiConcepts.isEmpty())
                    assertTrue(mutation.provenance.veilTransformation.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `rank mutation projects to every realm without changing unlock authority`() {
        val profile = SampleData.profile.copy(
            path = SampleData.paths.first { it.id == "oracle" },
            rankIndex = 2
        )
        val ledger = deriveWorldMutationLedger(profile, CastleMemoryState.EMPTY)
        val pathEntry = ledger.entries.single { it.kind == WorldMutationKind.PATH_ASCENSION }

        assertEquals(WorldMutationRealm.entries.toSet(), pathEntry.realms)
        assertEquals(profile.rankIndex, pathEntry.evidenceCount)
        WorldMutationRealm.entries.forEach { realm ->
            assertTrue(pathEntry.titleFor(realm).isNotBlank())
            assertTrue(pathEntry.inscriptionFor(realm).isNotBlank())
        }
    }

    @Test
    fun `realm copy is meaningfully distinct for a single rank`() {
        val path = SampleData.paths.first { it.id == "dreamwalker" }
        val inscriptions = WorldMutationRealm.entries.map { realm ->
            deriveRankRealmMutation(
                pathId = path.id,
                rankName = path.ranks[3],
                rankIndex = 3,
                rankCount = path.ranks.size,
                realm = realm
            ).inscription
        }
        assertEquals(WorldMutationRealm.entries.size, inscriptions.toSet().size)
    }
}
