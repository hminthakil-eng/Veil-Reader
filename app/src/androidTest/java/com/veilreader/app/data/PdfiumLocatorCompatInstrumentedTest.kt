package com.veilreader.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.domain.BookFormat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

@RunWith(AndroidJUnit4::class)
class PdfiumLocatorCompatInstrumentedTest {

    @Test
    fun pdfCompatibilityMarker_isIdempotent_andPreservesCustomLocations() {
        val original = locator(
            otherLocations = mapOf("customSelector" to "page-fragment")
        )

        val first = original.withCurrentVeilPdfiumVersion()
        val second = first.withCurrentVeilPdfiumVersion()

        assertEquals(first, second)
        assertEquals("page-fragment", first.locations.otherLocations["customSelector"])
        assertEquals(
            VEIL_PDFIUM_LOCATOR_VERSION,
            (first.locations.otherLocations[VEIL_PDFIUM_LOCATOR_VERSION_KEY] as Number).toInt()
        )
        assertTrue(first.hasCurrentVeilPdfiumVersion())
    }

    @Test
    fun marker_survivesLocatorJsonRoundTrip() {
        val persisted = locator().withCurrentVeilPdfiumVersion().toJSON().toString()
        val restored = requireNotNull(Locator.fromJSON(JSONObject(persisted)))

        assertTrue(restored.hasCurrentVeilPdfiumVersion())
        assertEquals(3, restored.locations.position)
    }

    @Test
    fun ephemeralLocatorReferences_followTheSameMigrationIdentityAsRoom() {
        val legacy = """{"href":"document.pdf","type":"application/pdf","locations":{"position":3}}"""
        val migrated = """{"href":"document.pdf","type":"application/pdf","locations":{"position":2,"veilPdfiumLocatorVersion":1}}"""
        val migrations = mapOf(legacy to migrated)

        assertEquals(migrated, resolveMigratedPdfiumLocatorJson(legacy, migrations))
        assertEquals("fresh", resolveMigratedPdfiumLocatorJson("fresh", migrations))
        assertEquals(null, resolveMigratedPdfiumLocatorJson(null, migrations))
    }

    @Test
    fun pdfPersistence_stampsMarker_whileEpubPersistenceDoesNot() {
        val original = locator()

        val pdf = requireNotNull(
            Locator.fromJSON(JSONObject(original.toVeilPersistedJson(BookFormat.PDF)))
        )
        val epub = requireNotNull(
            Locator.fromJSON(JSONObject(original.toVeilPersistedJson(BookFormat.EPUB)))
        )

        assertTrue(pdf.hasCurrentVeilPdfiumVersion())
        assertFalse(epub.hasCurrentVeilPdfiumVersion())
    }

    private fun locator(
        otherLocations: Map<String, Any> = emptyMap()
    ): Locator = Locator(
        href = requireNotNull(Url("document.pdf")),
        mediaType = MediaType.PDF,
        locations = Locator.Locations(
            position = 3,
            progression = 0.25,
            totalProgression = 0.5,
            otherLocations = otherLocations
        )
    )
}
