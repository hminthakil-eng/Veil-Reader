package com.veilreader.app.ui.screens

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun commitThresholdAcceptsDistanceOrCurlProgress() {
        assertTrue(
            shouldCommitPaperTurn(
                inwardDistance = 260f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.10f
            )
        )
        assertTrue(
            shouldCommitPaperTurn(
                inwardDistance = 20f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.40f
            )
        )
    }

    @Test
    fun commitThresholdRejectsSmallTentativeDrag() {
        assertTrue(
            !shouldCommitPaperTurn(
                inwardDistance = 60f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.10f
            )
        )
    }

    @Test
    fun stationaryBodyDragDoesNotPreCurlOrCommit() {
        for (startX in listOf(100f, 250f, 500f, 750f, 980f)) {
            val start = Offset(startX, 500f)
            val edge = paperCurlPageEdge(1000f, start, start)
            val centerX = (edge.top.x + edge.bottom.x) * 0.5f
            val progress = (1f - centerX / 1000f).coerceIn(0f, 1f)

            assertEquals(1000f, centerX, 0.001f)
            assertFalse(shouldCommitPaperTurn(0f, 1000f, 1f, progress))
        }
    }

    @Test
    fun shortBodyDragCancelsAtEveryStartingPosition() {
        for (startX in listOf(100f, 250f, 500f, 750f, 980f)) {
            val start = Offset(startX, 500f)
            val edge = paperCurlPageEdge(1000f, start, Offset(startX - 20f, 500f))
            val centerX = (edge.top.x + edge.bottom.x) * 0.5f
            val progress = (1f - centerX / 1000f).coerceIn(0f, 1f)

            assertEquals(990f, centerX, 0.001f)
            assertFalse(shouldCommitPaperTurn(20f, 1000f, 1f, progress))
        }
    }

    @Test
    fun equalFingerDisplacementProducesEqualFoldFromBodyOrEdge() {
        val body = paperCurlPageEdge(1000f, Offset(400f, 500f), Offset(140f, 540f))
        val edge = paperCurlPageEdge(1000f, Offset(980f, 500f), Offset(720f, 540f))

        assertEquals(edge.top, body.top)
        assertEquals(edge.bottom, body.bottom)
        val progress = (1f - (body.top.x + body.bottom.x) * 0.5f / 1000f)
        assertTrue(shouldCommitPaperTurn(260f, 1000f, 1f, progress))
    }

    @Test
    fun returnedOrOutwardDragCannotCommitFromStaleVisualProgress() {
        assertFalse(shouldCommitPaperTurn(0f, 1000f, 1f, 0.8f))
        assertFalse(shouldCommitPaperTurn(-60f, 1000f, 1f, 0.8f))
    }

    @Test
    fun foldLiftPeaksAtMidTurnAndFallsAtRest() {
        assertEquals(0f, paperFoldLift(0f), 0.0001f)
        assertEquals(1f, paperFoldLift(0.5f), 0.0001f)
        assertEquals(0f, paperFoldLift(1f), 0.0001f)
    }

    @Test
    fun weightedDragTrailsTheFingerWithoutChangingDirection() {
        val start = Offset(900f, 500f)
        val finger = Offset(300f, 620f)
        val weighted = paperWeightedDragCurrent(start, finger)

        assertTrue(weighted.x < start.x)
        assertTrue(weighted.x > finger.x)
        assertTrue(weighted.y > start.y)
        assertTrue(weighted.y < finger.y)
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
