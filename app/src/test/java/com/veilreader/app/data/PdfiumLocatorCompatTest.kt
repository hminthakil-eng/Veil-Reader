package com.veilreader.app.data

import com.veilreader.app.domain.BookFormat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

class PdfiumLocatorCompatTest {

    @Test
    fun `PDF compatibility marker is idempotent and preserves custom locations`() {
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
    fun `marker survives locator JSON round trip`() {
        val persisted = locator().withCurrentVeilPdfiumVersion().toJSON().toString()
        val restored = requireNotNull(Locator.fromJSON(JSONObject(persisted)))

        assertTrue(restored.hasCurrentVeilPdfiumVersion())
        assertEquals(3, restored.locations.position)
    }

    @Test
    fun `PDF persistence stamps marker while EPUB persistence does not`() {
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
