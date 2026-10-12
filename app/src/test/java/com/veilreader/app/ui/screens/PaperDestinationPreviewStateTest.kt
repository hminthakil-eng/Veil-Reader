package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class PaperDestinationPreviewStateTest {
    private fun locator(href: String = "chapter.xhtml", progression: Double = 0.1) =
        Locator(Url(href)!!, MediaType.XHTML, locations = Locator.Locations(progression = progression))
    private fun snapshot(revision: Long = 7L) = PaperDestinationSnapshot(
        Bitmap.createBitmap(120, 240, Bitmap.Config.ARGB_8888), revision,
        locator(), locator(progression = 0.2))

    @Test fun `live release invalidation keeps this gesture destination pinned`() {
        val state = PaperDestinationPreviewState()
        val destination = snapshot()
        state.publish(PaperTurnDirection.FORWARD, destination)
        state.begin(PaperTurnDirection.FORWARD, 7L, 120, 240)
        state.invalidate() // The live navigator now commits the destination.
        state.publish(PaperTurnDirection.FORWARD, snapshot(8L))
        assertSame(destination, state.activeSnapshot)
        state.end()
        assertNull(state.activeSnapshot)
        assertFalse("RenderThread may still own this bitmap", destination.bitmap.isRecycled)
    }

    @Test fun `reflow size source epoch and wrong direction cannot expose stale ink`() {
        val state = PaperDestinationPreviewState()
        state.publish(PaperTurnDirection.FORWARD, snapshot())
        state.begin(PaperTurnDirection.FORWARD, 8L, 120, 240)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.FORWARD, 7L, 240, 120)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.BACKWARD, 7L, 120, 240)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.FORWARD, 7L, 120, 240)
        assertNotNull(state.activeSnapshot)
        state.release()
        state.begin(PaperTurnDirection.FORWARD, 7L, 120, 240)
        assertNull(state.activeSnapshot)
    }

    @Test fun `resource boundary is distinct even when progression restarts at zero`() {
        assertFalse(paperPreviewSamePage(locator("a.xhtml", 0.0), locator("b.xhtml", 0.0)))
        assertFalse(paperPreviewSamePage(locator(progression = 0.1), locator(progression = 0.2)))
        assertTrue(paperPreviewSamePage(locator(), locator()))
        assertFalse(paperPreviewSamePage(locator(), Locator(Url("chapter.xhtml")!!, MediaType.XHTML)))
    }
}
