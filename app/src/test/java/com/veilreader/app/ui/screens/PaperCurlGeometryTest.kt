package com.veilreader.app.ui.screens

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.navigator.preferences.ReadingProgression

class PaperCurlGeometryTest {
    @Test
    fun ltrMapsRightToForwardAndLeftToBackward() {
        assertEquals(
            PaperTurnDirection.FORWARD,
            paperTurnDirectionFor(
                PaperCurlSide.RIGHT,
                ReadingProgression.LTR
            )
        )
        assertEquals(
            PaperTurnDirection.BACKWARD,
            paperTurnDirectionFor(
                PaperCurlSide.LEFT,
                ReadingProgression.LTR
            )
        )
    }

    @Test
    fun rtlReversesPhysicalEdgeMapping() {
        assertEquals(
            PaperTurnDirection.BACKWARD,
            paperTurnDirectionFor(
                PaperCurlSide.RIGHT,
                ReadingProgression.RTL
            )
        )
        assertEquals(
            PaperTurnDirection.FORWARD,
            paperTurnDirectionFor(
                PaperCurlSide.LEFT,
                ReadingProgression.RTL
            )
        )
    }

    @Test
    fun pageEdgeMovesInwardAsPointerPullsPage() {
        val start = Offset(980f, 500f)
        val shallow = paperCurlPageEdge(
            width = 1000f,
            start = start,
            current = Offset(820f, 540f)
        )
        val deep = paperCurlPageEdge(
            width = 1000f,
            start = start,
            current = Offset(420f, 610f)
        )

        val shallowCenter =
            (shallow.top.x + shallow.bottom.x) * 0.5f
        val deepCenter =
            (deep.top.x + deep.bottom.x) * 0.5f

        assertTrue(shallowCenter < 1000f)
        assertTrue(deepCenter < shallowCenter)
        assertTrue(deep.top != deep.bottom)
    }
}
