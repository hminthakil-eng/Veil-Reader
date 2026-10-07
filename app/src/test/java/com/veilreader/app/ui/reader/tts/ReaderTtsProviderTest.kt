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

    @Test
    fun neuralProviderRequiresGateModelAndRuntimeTogether() {
        val partial = readerTtsProviderCatalog(
            ReaderTtsProviderReadiness(
                localNeuralGateEnabled = true,
                localNeuralModelInstalled = true,
                localNeuralRuntimeAvailable = false
            )
        ).single { it.kind == ReaderTtsProviderKind.LOCAL_NEURAL }
        assertFalse(partial.available)

        val ready = readerTtsProviderCatalog(
            ReaderTtsProviderReadiness(
                localNeuralGateEnabled = true,
                localNeuralModelInstalled = true,
                localNeuralRuntimeAvailable = true
            )
        ).single { it.kind == ReaderTtsProviderKind.LOCAL_NEURAL }
        assertTrue(ready.available)
    }

    @Test
    fun networkProviderRequiresExplicitConsentEvenWhenConfigured() {
        val withoutConsent = readerTtsProviderCatalog(
            ReaderTtsProviderReadiness(
                networkGateEnabled = true,
                networkConfigured = true,
                networkConsentGranted = false
            )
        ).single { it.kind == ReaderTtsProviderKind.NETWORK }
        assertFalse(withoutConsent.available)
        assertTrue(withoutConsent.capabilities.sendsPublicationTextOffDevice)

        val withConsent = readerTtsProviderCatalog(
            ReaderTtsProviderReadiness(
                networkGateEnabled = true,
                networkConfigured = true,
                networkConsentGranted = true
            )
        ).single { it.kind == ReaderTtsProviderKind.NETWORK }
        assertTrue(withConsent.available)
    }
}
