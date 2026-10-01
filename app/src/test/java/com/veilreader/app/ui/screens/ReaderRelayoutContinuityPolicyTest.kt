package com.veilreader.app.ui.screens

import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            EpubPositionAnchorSample(1, 0.0),
            EpubPositionAnchorSample(2, 0.5),
            EpubPositionAnchorSample(3, 0.9)
        )
        assertEquals(
            1,
            stableEpubPositionAnchorIndex(
                currentPosition = 2,
                currentTotalProgression = 0.83,
                positions = positions
            )
        )
    }

    @Test
    fun `stable anchor falls back to nearest non-exceeding total progression`() {
        val positions = listOf(
            EpubPositionAnchorSample(1, 0.0),
            EpubPositionAnchorSample(2, 0.4),
            EpubPositionAnchorSample(3, 0.8)
        )
        assertEquals(
            1,
            stableEpubPositionAnchorIndex(
                currentPosition = null,
                currentTotalProgression = 0.62,
                positions = positions
            )
        )
        assertEquals(
            0,
            stableEpubPositionAnchorIndex(
                currentPosition = null,
                currentTotalProgression = 0.0,
                positions = positions
            )
        )
        assertNull(
            stableEpubPositionAnchorIndex(
                currentPosition = null,
                currentTotalProgression = null,
                positions = positions
            )
        )
    }
}
