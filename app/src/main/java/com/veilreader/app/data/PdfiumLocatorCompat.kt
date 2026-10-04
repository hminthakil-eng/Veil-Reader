package com.veilreader.app.data

import com.veilreader.app.domain.BookFormat
import org.readium.adapter.pdfium.navigator.migrateLegacyPdfiumLocator
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

internal const val VEIL_PDFIUM_LOCATOR_VERSION_KEY = "veilPdfiumLocatorVersion"
internal const val VEIL_PDFIUM_LOCATOR_VERSION = 1

internal fun Locator.hasCurrentVeilPdfiumVersion(): Boolean {
    val raw = locations.otherLocations[VEIL_PDFIUM_LOCATOR_VERSION_KEY]
    return (raw as? Number)?.toInt() == VEIL_PDFIUM_LOCATOR_VERSION
}

internal fun Locator.withCurrentVeilPdfiumVersion(): Locator =
    if (hasCurrentVeilPdfiumVersion()) {
        this
    } else {
        copy(
            locations = locations.copy(
                otherLocations = locations.otherLocations +
                    (VEIL_PDFIUM_LOCATOR_VERSION_KEY to VEIL_PDFIUM_LOCATOR_VERSION)
            )
        )
    }

@OptIn(DelicateReadiumApi::class)
internal suspend fun Publication.migrateVeilLegacyPdfiumLocator(locator: Locator): Locator =
    if (locator.hasCurrentVeilPdfiumVersion()) {
        locator
    } else {
        migrateLegacyPdfiumLocator(locator).withCurrentVeilPdfiumVersion()
    }

internal fun resolveMigratedPdfiumLocatorJson(
    locatorJson: String?,
    migrations: Map<String, String>
): String? =
    locatorJson?.let { migrations[it] ?: it }

internal fun Locator.toVeilPersistedJson(format: BookFormat): String {
    val persisted = if (format == BookFormat.PDF) withCurrentVeilPdfiumVersion() else this
    return persisted.toJSON().toString()
}
