package com.veilreader.app.ui.screens

import android.graphics.PointF
import android.os.Looper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalReadiumApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class ReaderPdfInputTest {
    @Test
    fun `native links cancel chrome before the message queue executes it`() {
        var chrome = 0
        val arbiter = arbiter { chrome += 1 }
        assertTrue(arbiter.onTap(tap()))
        assertEquals(0, chrome)
        arbiter.cancelPendingTap()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, chrome)
        arbiter.onTap(tap())
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, chrome)
    }

    @Test
    fun `Reader detach cancels queued taps and refuses new ones`() {
        var chrome = 0
        val arbiter = arbiter { chrome += 1 }
        arbiter.onTap(tap())
        arbiter.dispose()
        assertFalse(arbiter.onTap(tap()))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, chrome)
    }

    @Test
    fun `only the latest pending tap runs and uses current interaction ownership`() {
        var allowed = true
        var chrome = 0
        val arbiter = arbiter { if (allowed) chrome += 1 }
        arbiter.onTap(tap())
        arbiter.onTap(tap())
        allowed = false // A dialog or selection appeared before the deferred action.
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, chrome)
        allowed = true
        arbiter.onTap(tap())
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, chrome)
    }

    @Test
    fun `background between renderer callback and dispatch cannot run a Reader tap`() {
        var foreground = true
        var chrome = 0
        val arbiter = ReaderPdfTapArbiter(object : InputListener {
            override fun onTap(event: TapEvent): Boolean { chrome += 1; return true }
        }, isEnabled = { foreground })
        arbiter.onTap(tap())
        foreground = false
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, chrome)
        assertFalse(arbiter.onTap(tap()))
    }

    @Test
    fun `document destination stays one based regardless of displayed RTL page order`() {
        val source = locator()
        val target = requireNotNull(pdfInternalLinkLocator(source, 2, 4))
        assertEquals(listOf("page=3"), target.locations.fragments)
        assertEquals(3, target.locations.position)
        assertEquals(source.href, target.href)
        assertNull(target.locations.progression)
        assertNull(target.locations.totalProgression)
        assertTrue(target.locations.otherLocations.isEmpty())
        assertEquals(Locator.Text(), target.text)
        assertNull(target.title)
        val restored = requireNotNull(Locator.fromJSON(JSONObject(target.toJSON().toString())))
        assertEquals(3, pdfPageNumber(restored))
        // Physical PDFView page 1 for an RTL four-page book still means document page 3.
        assertNotEquals(2, pdfPageNumber(target))
    }

    @Test
    fun `bad native page destinations cannot create a jump or overflow`() {
        for (page in listOf(-1, Int.MIN_VALUE, 4, Int.MAX_VALUE)) {
            assertNull(pdfInternalLinkLocator(locator(), page, 4))
        }
        assertNull(pdfInternalLinkLocator(locator(), 0, 0))
        assertNull(pdfInternalLinkLocator(locator(), 0, -1))
        assertEquals(1, pdfPageNumber(requireNotNull(pdfInternalLinkLocator(locator(), 0, 4))))
    }

    @Test
    fun `PDF bookmark page accepts valid fragments and safe position fallback`() {
        assertEquals(2, pdfPageNumber(locator()))
        assertEquals(3, pdfPageNumber(locator().copy(locations = Locator.Locations(
            fragments = listOf("#page=3&zoom=200"), position = 1
        ))))
        for (fragment in listOf("page=0", "page=-1", "page=NaN", "page=999999999999")) {
            assertEquals(5, pdfPageNumber(locator().copy(locations = Locator.Locations(
                fragments = listOf(fragment), position = 5
            ))))
        }
        assertNull(pdfPageNumber(locator().copy(locations = Locator.Locations())))
    }

    @Test
    fun `PDF links only launch actual HTTP web addresses`() {
        for (value in listOf("https://example.org/a", "http://example.org", "HTTPS://example.org")) {
            assertNotNull(value, safePdfExternalLink(value))
        }
        for (value in listOf("javascript:alert(1)", "file:///private", "content://local", "intent://app", "https:", "https:///", "", "relative")) {
            assertNull(value, safePdfExternalLink(value))
        }
    }

    private fun arbiter(action: () -> Unit) = ReaderPdfTapArbiter(object : InputListener {
        override fun onTap(event: TapEvent): Boolean { action(); return true }
    })
    private fun tap() = TapEvent(PointF(10f, 10f))
    private fun locator() = Locator(
        href = requireNotNull(Url("book.pdf")), mediaType = MediaType.PDF,
        title = "Source page", locations = Locator.Locations(
            fragments = listOf("page=2"), position = 2, totalProgression = 0.25,
            progression = 0.25, otherLocations = mapOf("zoom" to 3)
        ), text = Locator.Text(highlight = "Old source text")
    )
}
