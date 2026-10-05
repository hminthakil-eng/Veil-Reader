package com.veilreader.app.feature

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderReadingMode
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.screens.applyMaterialPageRolloutToAppearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VeilFeatureGatesTest {
    @Test
    fun riskyFeatures_areReleaseDisabledByDefault() {
        VeilRiskyFeature.entries.forEach { feature ->
            assertFalse(feature.name, VeilFeatureGates.releaseEnabled(feature))
        }
    }

    @Test
    fun materialPageReleaseDefault_matchesCentralGate() {
        assertEquals(
            VeilFeatureGates.releaseEnabled(VeilRiskyFeature.GPU_MATERIAL_PAGE),
            MaterialPageEngineRollout.DEFAULT_ENABLED
        )
    }

    @Test
    fun disablingMaterialPage_downgradesPresentationWithoutMutatingSourcePreference() {
        val requested = ReaderAppearance()
            .withReadingMode(ReaderReadingMode.PAGED)
            .withPageTurnStyle(PageTurnStyle.PAPER)

        val effective = applyMaterialPageRolloutToAppearance(
            appearance = requested,
            format = BookFormat.EPUB,
            debugReview = false,
            materialPageEnabled = false
        )

        assertEquals(PageTurnStyle.PAPER, requested.pageTurnStyle)
        assertEquals(ReaderReadingMode.PAGED, effective.readingMode)
        assertEquals(PageTurnStyle.NONE, effective.pageTurnStyle)
    }
}
