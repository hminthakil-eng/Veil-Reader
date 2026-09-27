package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldRealmContractTest {
    @Test
    fun `every canonical realm has exactly one contract`() {
        assertEquals(
            WorldRealmId.canonical.toSet(),
            canonicalWorldRealmContracts.map { it.realm }.toSet()
        )
        assertEquals(
            WorldRealmId.canonical.size,
            canonicalWorldRealmContracts.size
        )
    }

    @Test
    fun `world realms are read only projections of durable history`() {
        canonicalWorldRealmContracts
            .filter { it.realm != WorldRealmId.SANCTUARY }
            .forEach { contract ->
                assertEquals(WorldHistoryAccess.READ_ONLY_PROJECTION, contract.historyAccess)
                assertTrue(contract.acceptsDecorativeWorldState)
                assertFalse(contract.mayMutateReadingHistory)
                assertFalse(contract.mayMutateProgression)
                assertFalse(contract.mayAppendWorldEventsDirectly)
                assertFalse(contract.mayPenalizeInactivity)
            }
    }

    @Test
    fun `Reader sanctuary is isolated from decorative world state`() {
        val sanctuary = worldRealmContractFor(WorldRealmId.SANCTUARY)

        assertEquals(WorldHistoryAccess.NONE, sanctuary.historyAccess)
        assertFalse(sanctuary.acceptsDecorativeWorldState)
        assertFalse(sanctuary.mayMutateReadingHistory)
        assertFalse(sanctuary.mayMutateProgression)
        assertFalse(sanctuary.mayAppendWorldEventsDirectly)
        assertFalse(sanctuary.mayPenalizeInactivity)
    }

    @Test
    fun `no realm may punish inactivity`() {
        assertTrue(canonicalWorldRealmContracts.none { it.mayPenalizeInactivity })
    }
}
