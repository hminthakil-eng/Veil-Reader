package com.veilreader.app.ui.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsProviderTest {
    @Test
    fun systemProviderIsTheOnlyExecutableProviderUntilNeuralAndNetworkGatesPass() {
        val catalog = readerTtsProviderCatalog()

        assertEquals(3, catalog.size)
        val system = catalog.single { it.kind == ReaderTtsProviderKind.SYSTEM }
        assertTrue(system.available)
        assertTrue(system.capabilities.offline)
        assertFalse(system.capabilities.requiresModelDownload)
        assertFalse(system.capabilities.sendsPublicationTextOffDevice)

        val neural = catalog.single { it.kind == ReaderTtsProviderKind.LOCAL_NEURAL }
        assertFalse(neural.available)
        assertTrue(neural.capabilities.offline)
        assertTrue(neural.capabilities.requiresModelDownload)
        assertNotNull(neural.unavailableReason)

        val network = catalog.single { it.kind == ReaderTtsProviderKind.NETWORK }
        assertFalse(network.available)
        assertTrue(network.capabilities.sendsPublicationTextOffDevice)
        assertNotNull(network.unavailableReason)
    }
}
