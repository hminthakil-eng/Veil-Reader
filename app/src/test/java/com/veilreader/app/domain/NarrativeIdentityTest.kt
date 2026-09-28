package com.veilreader.app.domain

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeIdentityTest {
    @Test
    fun `every covenant carries both reference sides and stays optional presentation`() {
        val covenants = allNarrativeCovenants()
        assertEquals(3, covenants.size)
        covenants.forEach { covenant ->
            assertTrue(covenant.id.isNotBlank())
            assertTrue(covenant.vow.isNotBlank())
            assertFalse(covenant.provenance.lotmConcepts.isEmpty())
            assertFalse(covenant.provenance.coiConcepts.isEmpty())
            assertTrue(covenant.provenance.veilTransformation.isNotBlank())
        }
    }

    @Test
    fun `all canonical paths resolve to one original Veil order`() {
        SampleData.paths.forEach { path ->
            val order = veilOrderForPath(path.id)
            assertTrue(path.id in order.pathIds)
            assertFalse(order.provenance.lotmConcepts.isEmpty())
            assertFalse(order.provenance.coiConcepts.isEmpty())
        }
    }

    @Test
    fun `covenant affinity never blocks cross-current paths`() {
        allNarrativeCovenants().forEach { covenant ->
            SampleData.paths.forEach { path ->
                assertNotNull(covenantResonance(path.id, covenant.id))
            }
        }
    }

    @Test
    fun `relic temperament is deterministic and descriptive only`() {
        val first = deriveRelicTemperament(
            relicId = "moonlit_lens",
            pathId = "oracle",
            awakened = true,
            evidenceCount = 5,
            target = 3,
            covenantId = "unwritten_margin"
        )
        val second = deriveRelicTemperament(
            relicId = "moonlit_lens",
            pathId = "oracle",
            awakened = true,
            evidenceCount = 5,
            target = 3,
            covenantId = "unwritten_margin"
        )
        assertEquals(first, second)
        assertEquals(RelicTemperament.WATCHFUL, first.temperament)
        assertFalse(first.provenance.lotmConcepts.isEmpty())
        assertFalse(first.provenance.coiConcepts.isEmpty())
    }

    @Test
    fun `sealed relics stay dormant regardless of path or covenant`() {
        SampleData.paths.forEach { path ->
            val state = deriveRelicTemperament(
                relicId = "astral_key",
                pathId = path.id,
                awakened = false,
                evidenceCount = 1,
                target = 2,
                covenantId = allNarrativeCovenants().first().id
            )
            assertEquals(RelicTemperament.DORMANT, state.temperament)
        }
    }
}
