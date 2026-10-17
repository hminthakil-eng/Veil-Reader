package com.veilreader.app.ui.station

/**
 * Renderer admission and interaction rules for the Victorian station shell.
 *
 * This is intentionally Android-free: UI, storage, rendering and navigation
 * must not share mutable state. Source-of-truth reading position stays with
 * Readium/Room. These policies never fabricate book, clock or platform data.
 */
internal enum class StationRendererMode {
    STATIC,
    COMPOSE_DEPTH,
    FILAMENT_3D
}

internal enum class StationSurface {
    THRESHOLD,
    ARCHIVE,
    CASTLE,
    READER
}

internal enum class StationSceneAction {
    OPEN_LIBRARY,
    OPEN_BOOK,
    OPEN_NOTES,
    OPEN_WORLD
}

internal data class StationRendererCapabilities(
    val featureEnabled: Boolean,
    val filamentInstalled: Boolean = false,
    val sceneAssetsValidated: Boolean = false,
    val hardwareGpuApproved: Boolean = false,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val powerSaveMode: Boolean = false,
    val trimMemoryCritical: Boolean = false
)

/**
 * Hardware renderer is opt-in and fail-closed until actual on-device evidence
 * exists. Compose depth is an interactive, perspective-based fallback, not true
 * 3D geometry. Accessibility always has priority over decorative rendering.
 */
internal fun stationRenderMode(
    capabilities: StationRendererCapabilities,
    surface: StationSurface
): StationRendererMode = when {
    !capabilities.featureEnabled -> StationRendererMode.STATIC
    surface == StationSurface.READER -> StationRendererMode.STATIC
    capabilities.reducedMotion || capabilities.highContrast ||
        capabilities.powerSaveMode || capabilities.trimMemoryCritical -> StationRendererMode.STATIC
    capabilities.filamentInstalled && capabilities.sceneAssetsValidated &&
        capabilities.hardwareGpuApproved -> StationRendererMode.FILAMENT_3D
    else -> StationRendererMode.COMPOSE_DEPTH
}

/** No perpetual decoration; only input-driven or bounded, non-looping animation. */
internal fun stationInputMotionMillis(mode: StationRendererMode): Int = when (mode) {
    StationRendererMode.STATIC -> 0
    StationRendererMode.COMPOSE_DEPTH -> 160
    StationRendererMode.FILAMENT_3D -> 160
}

/** The real 3D renderer must stop while the app is backgrounded. */
internal fun stationCanRenderFrames(
    mode: StationRendererMode,
    lifecycleResumed: Boolean,
    windowVisible: Boolean
): Boolean = mode != StationRendererMode.STATIC && lifecycleResumed && windowVisible

/** Reader text must not be rendered to an animated Filament texture. */
internal fun stationAllowsPerspectiveOnText(
    surface: StationSurface
): Boolean = false
