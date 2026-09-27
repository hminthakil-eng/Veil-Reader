package com.veilreader.app.ui.screens

import com.veilreader.app.domain.VeiledDiscoveryPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class VeiledDiscoveryPresentationTest {
    @Test
    fun `every catalog discovery has exactly one presentation in canonical order`() {
        val ids = veiledDiscoveryPresentations.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(VeiledDiscoveryPolicy.orderedIds, ids)
    }
}
