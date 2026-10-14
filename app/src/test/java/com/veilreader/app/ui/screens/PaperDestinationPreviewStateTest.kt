package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
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
        state.begin(PaperTurnDirection.FORWARD, 7L, locator(), 120, 240)
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
        state.begin(PaperTurnDirection.FORWARD, 8L, locator(), 120, 240)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.FORWARD, 7L, locator(), 240, 120)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.BACKWARD, 7L, locator(), 120, 240)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.FORWARD, 7L, locator(), 120, 240)
        assertNotNull(state.activeSnapshot)
        state.release()
        state.begin(PaperTurnDirection.FORWARD, 7L, locator(), 120, 240)
        assertNull(state.activeSnapshot)
    }

    @Test fun `same epoch cannot pin a destination captured for another page`() {
        val state = PaperDestinationPreviewState()
        val cached = snapshot()
        state.publish(PaperTurnDirection.FORWARD, cached)
        state.begin(PaperTurnDirection.FORWARD, 7L, locator("other.xhtml"), 120, 240)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.FORWARD, 7L, locator(progression = 0.3), 120, 240)
        assertNull(state.activeSnapshot)
        state.begin(PaperTurnDirection.FORWARD, 7L, locator(), 120, 240)
        assertSame(cached, state.activeSnapshot)
    }

    @Test fun `page witness observes locator updates after the pagination event`() = runBlocking {
        val events = MutableStateFlow(0L)
        val locators = MutableStateFlow(locator())
        val witness = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(2_000L) {
                awaitPaperPreviewPageWitness(events, locators, 0L, locator(), expectsDifferentPage = true)
            }
        }
        events.value = 1L
        yield() // Readium may emit onPageChanged before currentLocator updates.
        assertFalse(witness.isCompleted)
        locators.value = locator(progression = 0.2)
        witness.await()
    }

    @Test fun `return to origin observes locator change after pagination event`() = runBlocking {
        val events = MutableStateFlow(3L)
        val locators = MutableStateFlow(locator(progression = 0.2))
        val witness = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(2_000L) {
                awaitPaperPreviewPageWitness(events, locators, 3L, locator(), expectsDifferentPage = false)
            }
        }
        events.value = 4L
        yield()
        assertFalse(witness.isCompleted)
        locators.value = locator()
        witness.await()
    }

    @Test fun `resource boundary is distinct even when progression restarts at zero`() {
        assertFalse(paperPreviewSamePage(locator("a.xhtml", 0.0), locator("b.xhtml", 0.0)))
        assertFalse(paperPreviewSamePage(locator(progression = 0.1), locator(progression = 0.2)))
        assertTrue(paperPreviewSamePage(locator(), locator()))
        assertFalse(paperPreviewSamePage(locator(), Locator(Url("chapter.xhtml")!!, MediaType.XHTML)))
    }

    @Test fun `canceled fold needs no new adjacent captures for the unchanged source`() {
        val state = PaperDestinationPreviewState()
        val forward = snapshot()
        val backward = snapshot().copy(destination = locator(progression = 0.0))
        state.publish(PaperTurnDirection.FORWARD, forward)
        state.publish(PaperTurnDirection.BACKWARD, backward)
        state.begin(PaperTurnDirection.FORWARD, 7L, locator(), 120, 240)
        state.end()
        assertTrue(state.unpreparedDirections(locator(), 7L, 120, 240).isEmpty())
        assertSame(forward, state.prepared(PaperTurnDirection.FORWARD))
        assertSame(backward, state.prepared(PaperTurnDirection.BACKWARD))
        assertFalse(forward.bitmap.isRecycled)
        assertFalse(backward.bitmap.isRecycled)
    }

    @Test fun `partial preparation only requests the missing direction`() {
        val state = PaperDestinationPreviewState()
        state.publish(PaperTurnDirection.FORWARD, snapshot())
        assertEquals(listOf(PaperTurnDirection.BACKWARD),
            state.unpreparedDirections(locator(), 7L, 120, 240))
    }

    @Test fun `same-sized new chapter revision and viewport reject both cached directions`() {
        val state = PaperDestinationPreviewState()
        PaperTurnDirection.entries.forEach { state.publish(it, snapshot()) }
        val all = PaperTurnDirection.entries.toList()
        assertEquals(all, state.unpreparedDirections(locator("next.xhtml"), 7L, 120, 240))
        assertEquals(all, state.unpreparedDirections(locator(progression = 0.2), 7L, 120, 240))
        assertEquals(all, state.unpreparedDirections(locator(), 8L, 120, 240))
        assertEquals(all, state.unpreparedDirections(locator(), 7L, 240, 120))
        assertEquals(all, state.unpreparedDirections(locator(), 0L, 120, 240))
        state.invalidate()
        assertEquals(all, state.unpreparedDirections(locator(), 7L, 120, 240))
    }

    @Test fun `recycled adjacent ink requests repair without replacing the valid opposite direction`() {
        val state = PaperDestinationPreviewState()
        val forward = snapshot()
        state.publish(PaperTurnDirection.FORWARD, forward)
        val backward = snapshot()
        state.publish(PaperTurnDirection.BACKWARD, backward)
        backward.bitmap.recycle()
        assertEquals(listOf(PaperTurnDirection.BACKWARD),
            state.unpreparedDirections(locator(), 7L, 120, 240))
        assertSame(forward, state.prepared(PaperTurnDirection.FORWARD))
        assertFalse(forward.bitmap.isRecycled)
    }
}
