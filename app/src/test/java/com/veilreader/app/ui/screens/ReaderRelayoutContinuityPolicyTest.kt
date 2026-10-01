package com.veilreader.app.ui.screens

import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

class ReaderRelayoutContinuityPolicyTest {

    @Test
    fun `typography and pagination changes require a continuity anchor`() {
        val base = ReaderAppearance()

        assertTrue(epubPreferencesMayRelayout(base, base.copy(fontScale = base.fontScale + 0.1)))
        assertTrue(epubPreferencesMayRelayout(base, base.copy(lineHeight = base.lineHeight + 0.1)))
        assertTrue(epubPreferencesMayRelayout(base, base.copy(scroll = !base.scroll)))
        assertTrue(epubPreferencesMayRelayout(base, base.copy(publisherStyles = !base.publisherStyles)))
    }

    @Test
    fun `Veil-only page turn and sensory appearance do not force EPUB relayout anchoring`() {
        val base = ReaderAppearance()
        val alternateTurn = when (base.pageTurnStyle) {
            PageTurnStyle.PAPER -> PageTurnStyle.SLIDE
            else -> PageTurnStyle.PAPER
        }

        assertFalse(epubPreferencesMayRelayout(base, base.copy(pageTurnStyle = alternateTurn)))
        assertFalse(epubPreferencesMayRelayout(base, base.copy(screenBrightness = 0.42)))
        assertFalse(epubPreferencesMayRelayout(base, base.copy(paperPatina = 0.73)))
    }

    @Test
    fun `stable anchor prefers exact publication position`() {
        val positions = listOf(
            locator(position = 1, total = 0.0, progression = 0.0),
            locator(position = 2, total = 0.5, progression = 0.0),
            locator(position = 3, total = 0.9, progression = 0.0)
        )
        val current = locator(position = 2, total = 0.5, progression = 0.83)

        val anchor = stableEpubPositionAnchor(current, positions)

        assertEquals(2, anchor?.locations?.position)
        assertEquals(0.5, anchor?.locations?.totalProgression ?: -1.0, 0.0001)
        assertEquals(0.0, anchor?.locations?.progression ?: -1.0, 0.0001)
    }

    @Test
    fun `stable anchor falls back to nearest non-exceeding total progression`() {
        val positions = listOf(
            locator(position = 1, total = 0.0, progression = 0.0),
            locator(position = 2, total = 0.4, progression = 0.0),
            locator(position = 3, total = 0.8, progression = 0.0)
        )
        val current = locator(position = null, total = 0.62, progression = 0.75)

        assertEquals(2, stableEpubPositionAnchor(current, positions)?.locations?.position)
        assertNull(stableEpubPositionAnchor(locator(null, null, 0.4), positions))
    }

    private fun locator(
        position: Int?,
        total: Double?,
        progression: Double
    ): Locator =
        Locator(
            href = requireNotNull(Url("chapter.xhtml")),
            mediaType = MediaType.XHTML,
            locations = Locator.Locations(
                progression = progression,
                position = position,
                totalProgression = total
            )
        )
}
