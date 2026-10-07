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

/**
 * Canonical provider registry for the staged rollout.
 *
 * Only Android system TTS is executable today. Local neural and network providers are deliberately
 * present as unavailable descriptors so UI and persistence can evolve without pretending that an
 * engine is ready before model/license/privacy/device gates pass.
 */
internal fun readerTtsProviderCatalog(): List<ReaderTtsProviderDescriptor> = listOf(
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
        available = false,
        unavailableReason = "Local neural models are not installed yet.",
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
        available = false,
        unavailableReason = "Connected synthesis is not configured.",
        capabilities = ReaderTtsProviderCapabilities(
            offline = false,
            streaming = true,
            multiVoice = true,
            requiresModelDownload = false,
            sendsPublicationTextOffDevice = true
        )
    )
)
