package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.shared.ExperimentalReadiumApi

class ReaderInputArbiterTest {

    @OptIn(ExperimentalReadiumApi::class)
    @Test
    fun `blocked reader returns volume keys to Android instead of consuming them`() {
        var hardwareCalls = 0
        val hardware = object : InputListener {
            override fun onKey(event: KeyEvent): Boolean {
                hardwareCalls += 1
                return true
            }
        }
        val arbiter = ReaderInputArbiter(
            contentTarget = null,
            tapZones = null,
            hardwareKeys = hardware,
            paper = null,
            slide = null,
            staticPaged = null,
            directional = object : InputListener {},
            chromeTap = { true },
            interactionMode = { ReaderInteractionMode.BLOCKED }
        )

        assertFalse(arbiter.onKey(volumeKeyEvent(Key.AudioVolumeUp)))
        assertEquals(0, hardwareCalls)
    }

    @OptIn(ExperimentalReadiumApi::class)
    @Test
    fun `touch exploration never remaps volume keys`() {
        var hardwareCalls = 0
        val hardware = object : InputListener {
            override fun onKey(event: KeyEvent): Boolean {
                hardwareCalls += 1
                return true
            }
        }
        val arbiter = ReaderInputArbiter(
            contentTarget = null,
            tapZones = null,
            hardwareKeys = hardware,
            paper = null,
            slide = null,
            staticPaged = null,
            directional = object : InputListener {},
            chromeTap = { true },
            interactionMode = { ReaderInteractionMode.RENDERER_ACCESSIBILITY }
        )

        assertFalse(arbiter.onKey(volumeKeyEvent(Key.AudioVolumeDown)))
        assertEquals(0, hardwareCalls)
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun volumeKeyEvent(key: Key): KeyEvent =
        KeyEvent(
            type = KeyEvent.Type.Down,
            key = key,
            modifiers = emptySet(),
            characters = null
        )

    @Test
    fun `Veil slide owns only paginated EPUB slide mode`() {
        assertTrue(
            shouldUseVeilSlideNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
        assertFalse(
            shouldUseVeilSlideNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.NONE
            )
        )
        assertFalse(
            shouldUseVeilSlideNavigation(
                format = BookFormat.EPUB,
                scroll = true,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
        assertFalse(
            shouldUseVeilSlideNavigation(
                format = BookFormat.PDF,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
    }

    @Test
    fun `weighted slide commits by distance progress or deliberate flick`() {
        assertTrue(
            shouldCommitSlideTurn(
                inwardDistance = 180f,
                width = 1000f,
                density = 1f,
                slideProgress = 0.10f
            )
        )
        assertTrue(
            shouldCommitSlideTurn(
                inwardDistance = 40f,
                width = 1000f,
                density = 1f,
                slideProgress = 0.34f
            )
        )
        assertTrue(
            shouldCommitSlideTurn(
                inwardDistance = 40f,
                width = 1000f,
                density = 1f,
                slideProgress = 0.04f,
                releaseVelocityPxPerSec = 1200f
            )
        )
        assertFalse(
            shouldCommitSlideTurn(
                inwardDistance = 20f,
                width = 1000f,
                density = 1f,
                slideProgress = 0.02f,
                releaseVelocityPxPerSec = 2200f
            )
        )
        assertFalse(
            shouldCommitSlideTurn(
                inwardDistance = -20f,
                width = 1000f,
                density = 1f,
                slideProgress = 0.80f,
                releaseVelocityPxPerSec = 2200f
            )
        )
    }

    @Test
    fun `static paged drag belongs only to paginated EPUB NONE mode`() {
        assertTrue(
            shouldUseStaticPagedDragNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.NONE
            )
        )
        assertFalse(
            shouldUseStaticPagedDragNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
        assertFalse(
            shouldUseStaticPagedDragNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )
        assertFalse(
            shouldUseStaticPagedDragNavigation(
                format = BookFormat.EPUB,
                scroll = true,
                pageTurnStyle = PageTurnStyle.NONE
            )
        )
        assertFalse(
            shouldUseStaticPagedDragNavigation(
                format = BookFormat.PDF,
                scroll = false,
                pageTurnStyle = PageTurnStyle.NONE
            )
        )
    }

    @Test
    fun `static paged drag requires deliberate horizontal travel and respects RTL`() {
        assertEquals(
            PaperTurnDirection.FORWARD,
            staticPagedDragDirection(
                offsetX = -180f,
                offsetY = 20f,
                width = 1000f,
                density = 1f,
                progression = org.readium.r2.navigator.preferences.ReadingProgression.LTR
            )
        )
        assertEquals(
            PaperTurnDirection.BACKWARD,
            staticPagedDragDirection(
                offsetX = -180f,
                offsetY = 20f,
                width = 1000f,
                density = 1f,
                progression = org.readium.r2.navigator.preferences.ReadingProgression.RTL
            )
        )
        assertEquals(
            null,
            staticPagedDragDirection(
                offsetX = -40f,
                offsetY = 4f,
                width = 1000f,
                density = 1f,
                progression = org.readium.r2.navigator.preferences.ReadingProgression.LTR
            )
        )
        assertEquals(
            null,
            staticPagedDragDirection(
                offsetX = -140f,
                offsetY = 180f,
                width = 1000f,
                density = 1f,
                progression = org.readium.r2.navigator.preferences.ReadingProgression.LTR
            )
        )
    }

    @Test
    fun `directional edge taps belong only to static paged EPUB mode`() {
        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )

        assertTrue(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.NONE
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.EPUB,
                scroll = true,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.PDF,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.COMIC,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
    }

    @Test
    fun `PDF directional animation ignores hidden EPUB page-turn style`() {
        assertFalse(
            shouldAnimateDirectionalNavigation(
                format = BookFormat.PDF,
                scroll = false,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )
        assertFalse(
            shouldAnimateDirectionalNavigation(
                format = BookFormat.PDF,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
    }

    @Test
    fun `EPUB slide alone enables directional animation`() {
        assertFalse(
            shouldAnimateDirectionalNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )
        assertTrue(
            shouldAnimateDirectionalNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
        assertFalse(
            shouldAnimateDirectionalNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.NONE
            )
        )
    }

    @Test
    fun `scroll mode never inherits hidden slide directional animation`() {
        assertFalse(
            shouldAnimateDirectionalNavigation(
                format = BookFormat.EPUB,
                scroll = true,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
    }

    @Test
    fun `selection owns renderer only while no modal or durable close blocks input`() {
        assertEquals(
            ReaderInteractionMode.RENDERER_SELECTION,
            readerInteractionMode(
                selectionModeActive = true,
                overlayVisible = false,
                closeInFlight = false,
                controlsVisible = true
            )
        )
        assertEquals(
            ReaderInteractionMode.BLOCKED,
            readerInteractionMode(
                selectionModeActive = true,
                overlayVisible = true,
                closeInFlight = false,
                controlsVisible = true
            )
        )
        assertEquals(
            ReaderInteractionMode.BLOCKED,
            readerInteractionMode(
                selectionModeActive = true,
                overlayVisible = false,
                closeInFlight = true,
                controlsVisible = true
            )
        )
    }

    @Test
    fun `modal overlays and durable close block reader navigation`() {
        assertEquals(
            ReaderInteractionMode.BLOCKED,
            readerInteractionMode(
                selectionModeActive = false,
                overlayVisible = true,
                closeInFlight = false,
                controlsVisible = false
            )
        )
        assertEquals(
            ReaderInteractionMode.BLOCKED,
            readerInteractionMode(
                selectionModeActive = false,
                overlayVisible = false,
                closeInFlight = true,
                controlsVisible = false
            )
        )
    }

    @Test
    fun `visible chrome owns taps before page navigation while hidden chrome permits navigation`() {
        assertEquals(
            ReaderInteractionMode.CHROME_PRIORITY,
            readerInteractionMode(
                selectionModeActive = false,
                overlayVisible = false,
                closeInFlight = false,
                controlsVisible = true
            )
        )
        assertEquals(
            ReaderInteractionMode.NAVIGATION,
            readerInteractionMode(
                selectionModeActive = false,
                overlayVisible = false,
                closeInFlight = false,
                controlsVisible = false
            )
        )
    }



    @Test
    fun `touch exploration returns touch ownership to renderer`() {
        assertEquals(
            ReaderInteractionMode.RENDERER_ACCESSIBILITY,
            readerInteractionMode(
                selectionModeActive = false,
                overlayVisible = false,
                closeInFlight = false,
                controlsVisible = true,
                touchExplorationEnabled = true
            )
        )
    }

    @Test
    fun `modal and durable close still outrank touch exploration`() {
        assertEquals(
            ReaderInteractionMode.BLOCKED,
            readerInteractionMode(
                selectionModeActive = false,
                overlayVisible = true,
                closeInFlight = false,
                controlsVisible = true,
                touchExplorationEnabled = true
            )
        )
        assertEquals(
            ReaderInteractionMode.BLOCKED,
            readerInteractionMode(
                selectionModeActive = false,
                overlayVisible = false,
                closeInFlight = true,
                controlsVisible = true,
                touchExplorationEnabled = true
            )
        )
    }

    @Test
    fun `page turn tap zone stays comfortable without consuming the center`() {
        val standardPhone = pageTurnTapZonePx(
            width = 1_080f,
            density = 3f,
            preferredFraction = 0.22f
        )
        assertTrue(standardPhone >= 56f * 3f)
        assertTrue(standardPhone <= 1_080f * 0.28f)

        val narrowSurface = pageTurnTapZonePx(
            width = 480f,
            density = 3f,
            preferredFraction = 0.22f
        )
        assertTrue(narrowSurface <= 480f * 0.28f)
        assertTrue(narrowSurface * 2f < 480f)

        val wideTablet = pageTurnTapZonePx(
            width = 2_560f,
            density = 2f,
            preferredFraction = 0.22f
        )
        assertTrue(wideTablet <= 112f * 2f)
        assertTrue(wideTablet * 2f < 2_560f * 0.20f)
    }

    @Test
    fun `invalid tap-zone width produces no navigation zone`() {
        assertEquals(
            0f,
            pageTurnTapZonePx(
                width = 0f,
                density = 3f,
                preferredFraction = 0.22f
            ),
            0.0001f
        )
    }


    @Test
    fun `EPUB navigation ownership is one hot across paper slide paged and scroll`() {
        val cases = listOf(
            Triple(false, PageTurnStyle.PAPER, "paper"),
            Triple(false, PageTurnStyle.SLIDE, "slide"),
            Triple(false, PageTurnStyle.NONE, "paged"),
            Triple(true, PageTurnStyle.NONE, "scroll"),
            Triple(true, PageTurnStyle.PAPER, "scroll"),
            Triple(true, PageTurnStyle.SLIDE, "scroll")
        )

        cases.forEach { (scroll, style, expectedOwner) ->
            val owners = mapOf(
                "paper" to shouldUsePaperCurlNavigation(
                    format = BookFormat.EPUB,
                    scroll = scroll,
                    pageTurnStyle = style
                ),
                "slide" to shouldUseVeilSlideNavigation(
                    format = BookFormat.EPUB,
                    scroll = scroll,
                    pageTurnStyle = style
                ),
                "paged" to shouldUseStaticPagedDragNavigation(
                    format = BookFormat.EPUB,
                    scroll = scroll,
                    pageTurnStyle = style
                ),
                "scroll" to scroll
            )

            assertEquals(
                "exactly one owner for scroll=$scroll style=$style",
                1,
                owners.values.count { it }
            )
            assertEquals(true, owners.getValue(expectedOwner))
        }
    }

    @Test
    fun `paper ownership never leaks into PDF or scroll`() {
        assertFalse(
            shouldUsePaperCurlNavigation(
                format = BookFormat.PDF,
                scroll = false,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )
        assertFalse(
            shouldUsePaperCurlNavigation(
                format = BookFormat.EPUB,
                scroll = true,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )
        assertTrue(
            shouldUsePaperCurlNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )
    }
}
