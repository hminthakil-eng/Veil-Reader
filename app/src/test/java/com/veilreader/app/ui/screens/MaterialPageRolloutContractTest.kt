package com.veilreader.app.ui.screens

import com.veilreader.app.BuildConfig
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.reader.material.MaterialPagePreset
import com.veilreader.app.ui.reader.material.MaterialPageProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialPageRolloutContractTest {

    @Test
    fun `debug build enables material review while release default stays off`() {
        MaterialPageEngineRollout.setDebugOverride(null)
        try {
            assertFalse(MaterialPageEngineRollout.DEFAULT_ENABLED)
            assertTrue(BuildConfig.DEBUG)
            assertTrue(MaterialPageEngineRollout.isEnabled())
            assertFalse(shouldCapturePaperTurnSnapshot(reducedMotion = true))
            assertTrue(shouldCapturePaperTurnSnapshot(reducedMotion = false))

            MaterialPageEngineRollout.setDebugOverride(false)
            assertFalse(MaterialPageEngineRollout.isEnabled())
            assertFalse(shouldCapturePaperTurnSnapshot(reducedMotion = true))
        } finally {
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }

    @Test
    fun `normal motion Paper fails closed until GPU visual is actually active`() {
        MaterialPageEngineRollout.setDebugOverride(true)
        try {
            listOf(
                GpuMaterialPageRendererStatus.INITIALIZING,
                GpuMaterialPageRendererStatus.FAILED,
                GpuMaterialPageRendererStatus.UNSUPPORTED
            ).forEach { status ->
                assertFalse(
                    shouldAllowPaperNavigation(
                        reducedMotion = false,
                        rendererStatus = status,
                        visualActive = false
                    )
                )
            }
            assertFalse(
                shouldAllowPaperNavigation(
                    reducedMotion = false,
                    rendererStatus = GpuMaterialPageRendererStatus.READY,
                    visualActive = false
                )
            )
            assertTrue(
                shouldAllowPaperNavigation(
                    reducedMotion = false,
                    rendererStatus = GpuMaterialPageRendererStatus.READY,
                    visualActive = true
                )
            )
        } finally {
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }

    @Test
    fun `Reduced Motion keeps functional Paper navigation without a curl visual`() {
        MaterialPageEngineRollout.setDebugOverride(true)
        try {
            assertTrue(
                shouldAllowPaperNavigation(
                    reducedMotion = true,
                    rendererStatus = GpuMaterialPageRendererStatus.REDUCED_MOTION,
                    visualActive = false
                )
            )
        } finally {
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }

    @Test
    fun `disabled GPU Paper degrades to static paged not slide`() {
        val requested = ReaderAppearance(
            scroll = false,
            pageTurnStyle = PageTurnStyle.PAPER
        )

        val effective = applyMaterialPageRolloutToAppearance(
            appearance = requested,
            format = BookFormat.EPUB,
            debugReview = false,
            materialPageEnabled = false
        )

        assertFalse(effective.scroll)
        assertEquals(PageTurnStyle.NONE, effective.pageTurnStyle)
    }

    @Test
    fun `debug GPU availability never overwrites explicit slide choice`() {
        val persistedSlide = ReaderAppearance(
            scroll = false,
            pageTurnStyle = PageTurnStyle.SLIDE
        )

        val effective = applyMaterialPageRolloutToAppearance(
            appearance = persistedSlide,
            format = BookFormat.EPUB,
            debugReview = true,
            materialPageEnabled = true
        )

        assertFalse(effective.scroll)
        assertEquals(PageTurnStyle.SLIDE, effective.pageTurnStyle)
    }

    @Test
    fun `debug GPU availability preserves scroll ownership`() {
        val persistedScroll = ReaderAppearance(
            scroll = true,
            pageTurnStyle = PageTurnStyle.PAPER
        )

        val effective = applyMaterialPageRolloutToAppearance(
            appearance = persistedScroll,
            format = BookFormat.EPUB,
            debugReview = true,
            materialPageEnabled = true
        )

        assertTrue(effective.scroll)
        assertEquals(PageTurnStyle.PAPER, effective.pageTurnStyle)
    }

    @Test
    fun `review preset selection is source structured and reversible`() {
        MaterialPageEngineRollout.setDebugOverride(null)
        try {
            MaterialPageEngineRollout.setPreviewPreset(MaterialPagePreset.PAPYRUS)
            assertEquals(
                MaterialPageProfiles.Papyrus,
                MaterialPageEngineRollout.selectedProfile()
            )

            MaterialPageEngineRollout.setPreviewPreset(MaterialPagePreset.MATTE_BOOK)
            assertEquals(
                MaterialPageProfiles.MatteBook,
                MaterialPageEngineRollout.selectedProfile()
            )
        } finally {
            MaterialPageEngineRollout.setPreviewPreset(MaterialPagePreset.MATTE_BOOK)
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }
}
