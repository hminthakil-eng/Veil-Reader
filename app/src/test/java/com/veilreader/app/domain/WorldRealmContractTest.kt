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
    fun `world realms get read only World Kernel projections`() {
        canonicalWorldRealmContracts
            .filter { it.realm != WorldRealmId.SANCTUARY }
            .forEach { contract ->
                assertEquals(WorldEventAccess.READ_ONLY_PROJECTION, contract.eventAccess)
                assertTrue(contract.acceptsDecorativeWorldState)
                assertFalse(contract.worldKernelMayMutateReadingHistory)
                assertFalse(contract.worldKernelMayMutateProgression)
                assertFalse(contract.mayAppendWorldEventsDirectly)
                assertFalse(contract.mayPenalizeInactivity)
            }
    }

    @Test
    fun `Reader sanctuary is isolated from decorative World Kernel state`() {
        val sanctuary = worldRealmContractFor(WorldRealmId.SANCTUARY)

        assertEquals(WorldEventAccess.NONE, sanctuary.eventAccess)
        assertFalse(sanctuary.acceptsDecorativeWorldState)
        assertFalse(sanctuary.worldKernelMayMutateReadingHistory)
        assertFalse(sanctuary.worldKernelMayMutateProgression)
        assertFalse(sanctuary.mayAppendWorldEventsDirectly)
        assertFalse(sanctuary.mayPenalizeInactivity)
    }

    @Test
    fun `World Kernel cannot own Reader or progression mutation in any realm`() {
        assertTrue(
            canonicalWorldRealmContracts.none {
                it.worldKernelMayMutateReadingHistory ||
                    it.worldKernelMayMutateProgression ||
                    it.mayAppendWorldEventsDirectly
            }
        )
    }

    @Test
    fun `no realm may punish inactivity`() {
        assertTrue(canonicalWorldRealmContracts.none { it.mayPenalizeInactivity })
    }
}
