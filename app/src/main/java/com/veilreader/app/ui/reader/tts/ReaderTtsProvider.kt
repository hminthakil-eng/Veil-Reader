package com.veilreader.app.ui.reader.tts

/**
 * Product-level synthesis providers.
 *
 * The Reader owns semantic text/locator extraction; a provider owns only speech generation.
 * Keeping those responsibilities separate lets Veil add local neural engines without giving them
 * authority over Reader progress, publication parsing, or navigation.
 */
internal enum class ReaderTtsProviderKind {
    SYSTEM,
    LOCAL_NEURAL,
    NETWORK
}

internal data class ReaderTtsProviderCapabilities(
    val offline: Boolean,
    val streaming: Boolean,
    val multiVoice: Boolean,
    val requiresModelDownload: Boolean,
    val sendsPublicationTextOffDevice: Boolean
)

internal data class ReaderTtsProviderDescriptor(
    val kind: ReaderTtsProviderKind,
    val id: String,
    val displayName: String,
    val available: Boolean,
    val capabilities: ReaderTtsProviderCapabilities,
    val unavailableReason: String? = null
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(available || !unavailableReason.isNullOrBlank())
    }
}

internal data class ReaderTtsProviderReadiness(
    val localNeuralGateEnabled: Boolean = false,
    val localNeuralModelInstalled: Boolean = false,
    val localNeuralRuntimeAvailable: Boolean = false,
    val networkGateEnabled: Boolean = false,
    val networkConfigured: Boolean = false,
    val networkConsentGranted: Boolean = false
)

/**
 * Canonical provider registry for the staged rollout.
 *
 * Availability is fail-closed. A provider becomes selectable only when every gate that protects
 * its privacy, runtime and model requirements has explicitly passed.
 */
internal fun readerTtsProviderCatalog(
    readiness: ReaderTtsProviderReadiness = ReaderTtsProviderReadiness()
): List<ReaderTtsProviderDescriptor> = listOf(
    ReaderTtsProviderDescriptor(
        kind = ReaderTtsProviderKind.SYSTEM,
        id = "android-system",
        displayName = "Android system voice",
        available = true,
        capabilities = ReaderTtsProviderCapabilities(
            offline = true,
            streaming = true,
            multiVoice = true,
            requiresModelDownload = false,
            sendsPublicationTextOffDevice = false
        )
    ),
    ReaderTtsProviderDescriptor(
        kind = ReaderTtsProviderKind.LOCAL_NEURAL,
        id = "sherpa-local",
        displayName = "Veil Neural Voice",
        available =
            readiness.localNeuralGateEnabled &&
                readiness.localNeuralModelInstalled &&
                readiness.localNeuralRuntimeAvailable,
        unavailableReason = when {
            !readiness.localNeuralGateEnabled -> "Local neural listening is still under review."
            !readiness.localNeuralModelInstalled -> "Local neural models are not installed yet."
            !readiness.localNeuralRuntimeAvailable -> "The local neural runtime is unavailable."
            else -> null
        },
        capabilities = ReaderTtsProviderCapabilities(
            offline = true,
            streaming = true,
            multiVoice = true,
            requiresModelDownload = true,
            sendsPublicationTextOffDevice = false
        )
    ),
    ReaderTtsProviderDescriptor(
        kind = ReaderTtsProviderKind.NETWORK,
        id = "network-opt-in",
        displayName = "Connected voice",
        available =
            readiness.networkGateEnabled &&
                readiness.networkConfigured &&
                readiness.networkConsentGranted,
        unavailableReason = when {
            !readiness.networkGateEnabled -> "Connected synthesis is disabled."
            !readiness.networkConfigured -> "Connected synthesis is not configured."
            !readiness.networkConsentGranted ->
                "Publication text sharing has not been explicitly approved."
            else -> null
        },
        capabilities = ReaderTtsProviderCapabilities(
            offline = false,
            streaming = true,
            multiVoice = true,
            requiresModelDownload = false,
            sendsPublicationTextOffDevice = true
        )
    )
)
