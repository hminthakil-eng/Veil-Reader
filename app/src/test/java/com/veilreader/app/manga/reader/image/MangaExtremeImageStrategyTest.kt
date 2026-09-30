package com.veilreader.app.manga.reader.image

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaExtremeImageStrategyTest {

    private val strategy = MangaExtremeImageStrategy()

    @Test
    fun ordinaryLocalPageStaysOnStandardCoil() {
        val decision = strategy.decide(
            local(),
            MangaImageDimensions(widthPx = 1600, heightPx = 2400)
        )

        assertEquals(MangaImageDeliveryStrategy.STANDARD_COIL, decision.strategy)
        assertFalse(decision.extreme)
    }

    @Test
    fun extremeTallLocalPageUsesSubsampling() {
        val decision = strategy.decide(
            local(),
            MangaImageDimensions(widthPx = 1440, heightPx = 50_000)
        )

        assertEquals(
            MangaImageDeliveryStrategy.LOCAL_SUBSAMPLING,
            decision.strategy
        )
        assertTrue(decision.extreme)
        assertTrue("extreme-tall-aspect" in decision.reasons)
        assertTrue("long-edge" in decision.reasons)
    }

    @Test
    fun largePixelCountLocalPageUsesSubsamplingEvenWithoutTallAspect() {
        val decision = strategy.decide(
            local(),
            MangaImageDimensions(widthPx = 8000, heightPx = 8000)
        )

        assertEquals(
            MangaImageDeliveryStrategy.LOCAL_SUBSAMPLING,
            decision.strategy
        )
        assertTrue("pixel-count" in decision.reasons)
    }

    @Test
    fun extremeRemotePageNeverClaimsLocalSubsampling() {
        val decision = strategy.decide(
            remote(),
            MangaImageDimensions(widthPx = 1440, heightPx = 50_000)
        )

        assertEquals(
            MangaImageDeliveryStrategy.REMOTE_BOUNDED_PREVIEW,
            decision.strategy
        )
        assertTrue(decision.extreme)
    }

    @Test
    fun unknownRemoteDimensionsStayOnBoundedPreview() {
        val decision = strategy.decide(remote(), null)

        assertEquals(
            MangaImageDeliveryStrategy.REMOTE_BOUNDED_PREVIEW,
            decision.strategy
        )
        assertFalse(decision.extreme)
        assertEquals(setOf("dimensions-unknown"), decision.reasons)
    }

    @Test
    fun plannerUsesProbeInsteadOfFilenameHeuristics() = runBlocking {
        val page = local()
        val planner = MangaImageDeliveryPlanner(
            dimensionProbe = object : MangaImageDimensionProbe {
                override suspend fun probe(page: MangaResolvedPage) =
                    MangaImageDimensions(widthPx = 1200, heightPx = 30_000)
            }
        )

        val plan = planner.plan(page)

        assertEquals(
            MangaImageDeliveryStrategy.LOCAL_SUBSAMPLING,
            plan.decision.strategy
        )
        assertEquals(30_000, plan.dimensions?.heightPx)
    }

    private fun local() = MangaResolvedPage.Local(
        index = 0,
        file = File("extreme-page.jpg")
    )

    private fun remote() = MangaResolvedPage.Remote(
        index = 0,
        url = "https://example.test/extreme-page.jpg",
        headers = emptyMap()
    )
}
