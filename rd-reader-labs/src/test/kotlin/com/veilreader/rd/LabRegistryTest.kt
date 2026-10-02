package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LabRegistryTest {
    @Test fun everyMoonGapHasIndependentLabAndNoProductionDependency() {
        assertEquals(17, ReaderLabRegistry.all.map { it.id }.distinct().size)
        assertFalse(ReaderLabRegistry.all.any { it.productionDependencyAllowed })
    }
}
