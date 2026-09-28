package com.veilreader.app.ui.screens

import com.veilreader.app.domain.VeiledDiscoveryPolicy
import com.veilreader.app.domain.mysteryChainDefinitions
import org.junit.Assert.assertEquals
import org.junit.Test

class VeiledDiscoveryPresentationTest {
    @Test
    fun `every catalog discovery has exactly one presentation in canonical order`() {
        val ids = veiledDiscoveryPresentations.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(VeiledDiscoveryPolicy.orderedIds, ids)
    }
    @Test
    fun `every localized discovery covers every domain mystery fragment`() {
        val domain = mysteryChainDefinitions().associateBy { it.id }

        veiledDiscoveryPresentations.forEach { presentation ->
            val definition = requireNotNull(domain[presentation.id])
            assertEquals(
                "fragment coverage for ${presentation.id}",
                definition.fragments.size,
                presentation.fragmentRes.size
            )
        }
    }

    @Test
    fun `every discovery owns concrete localized presentation resources`() {
        veiledDiscoveryPresentations.forEach { presentation ->
            require(presentation.titleRes != 0)
            require(presentation.clueRes != 0)
            require(presentation.loreRes != 0)
            require(presentation.fragmentRes.all { it != 0 })
        }
    }

}
