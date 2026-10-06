package com.veilreader.app.ui.navigation

/**
 * Chooses the locator used to reopen a publication after navigation or process recreation.
 *
 * Explicit navigation wins, then the crash-durable semantic checkpoint, then the transient
 * SavedState checkpoint, then Room's durable locator.
 */
internal fun chooseReaderRestoreLocator(
    explicitOverrideJson: String?,
    crashRecoveryCheckpointJson: String?,
    readerCheckpointJson: String?,
    durableLocatorJson: String?
): String? =
    explicitOverrideJson?.takeIf(String::isNotBlank)
        ?: crashRecoveryCheckpointJson?.takeIf(String::isNotBlank)
        ?: readerCheckpointJson?.takeIf(String::isNotBlank)
        ?: durableLocatorJson?.takeIf(String::isNotBlank)
