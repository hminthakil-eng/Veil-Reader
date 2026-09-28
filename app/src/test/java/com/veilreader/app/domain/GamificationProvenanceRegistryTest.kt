package com.veilreader.app.domain

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationProvenanceRegistryTest {
    private fun assertDual(provenance: DualBookProvenance) {
        assertFalse(provenance.lotmConcepts.isEmpty())
        assertFalse(provenance.coiConcepts.isEmpty())
        assertTrue(provenance.veilTransformation.length >= 32)
    }

    @Test
    fun `every canonical path doctrine and directive has dual-book provenance`() {
        SampleData.paths.forEach { path ->
            assertDual(GamificationProvenanceRegistry.pathDoctrine(path.id))
            assertDual(GamificationProvenanceRegistry.pathDirective(path.id))
        }
    }

    @Test
    fun `every world mutation kind has dual-book provenance`() {
        WorldMutationKind.entries.forEach { kind ->
            assertDual(GamificationProvenanceRegistry.worldMutation(kind))
        }
    }

    @Test
    fun `every narrative relic has dual-book provenance`() {
        GamificationProvenanceRegistry.narrativeRelicIds.forEach { id ->
            assertDual(GamificationProvenanceRegistry.relic(id))
        }
    }

    @Test
    fun `every permanent discovery has dual-book provenance`() {
        VeiledDiscoveryPolicy.orderedIds.forEach { id ->
            assertDual(GamificationProvenanceRegistry.discovery(id))
        }
    }
}
