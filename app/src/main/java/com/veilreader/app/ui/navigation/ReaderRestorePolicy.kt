package com.veilreader.app.ui.navigation

/**
 * Chooses the locator used to reopen a publication after navigation or process recreation.
 *
 * Explicit user navigation wins. A crash journal then wins over SavedState because it is written
 * only after a semantic commit has been accepted and before that commit returns to the UI. Room is
 * the long-term source of truth and remains the final fallback.
 */
internal fun chooseReaderRestoreLocator(
    explicitOverrideJson: String?,
    readerCheckpointJson: String?,
    durableLocatorJson: String?,
    crashCheckpointJson: String? = null
): String? =
    explicitOverrideJson?.takeIf(String::isNotBlank)
        ?: crashCheckpointJson?.takeIf(String::isNotBlank)
        ?: readerCheckpointJson?.takeIf(String::isNotBlank)
        ?: durableLocatorJson?.takeIf(String::isNotBlank)
