package com.veilreader.app.feature

/**
 * Release-level kill switches for optional or not-yet-product-green capabilities.
 *
 * These gates control availability only. They must never erase, rewrite or migrate
 * user content/settings when a feature is disabled.
 */
internal enum class VeilRiskyFeature {
    GPU_MATERIAL_PAGE,
    LIVE_MANGA_SOURCES,
    ANDROIDX_PDF_EDITOR,
    CLOUD_SYNC,
    BACKGROUND_TTS
}

internal object VeilFeatureGates {
    fun releaseEnabled(feature: VeilRiskyFeature): Boolean =
        when (feature) {
            VeilRiskyFeature.GPU_MATERIAL_PAGE -> false
            VeilRiskyFeature.LIVE_MANGA_SOURCES -> false
            VeilRiskyFeature.ANDROIDX_PDF_EDITOR -> false
            VeilRiskyFeature.CLOUD_SYNC -> false
            VeilRiskyFeature.BACKGROUND_TTS -> false
        }
}
