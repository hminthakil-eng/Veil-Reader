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
    fun ordinaryPhoneSwipeCommitsPaperWithoutRequiringExtremeTravel() {
        assertTrue(
            shouldCommitPaperTurn(
                inwardDistance = 190f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.12f
            )
        )
        assertFalse(
            shouldCommitPaperTurn(
                inwardDistance = 54f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.08f
            )
        )
    }

    @Test
    fun ordinaryPhoneSwipeCommitsSlideWithoutAccidentalTinyDrag() {
        assertTrue(
            shouldCommitSlideTurn(
                inwardDistance = 140f,
                width = 1000f,
                density = 1f,
                slideProgress = 0.12f
            )
        )
        assertFalse(
            shouldCommitSlideTurn(
                inwardDistance = 42f,
                width = 1000f,
                density = 1f,
                slideProgress = 0.08f
            )
        )
    }

    @Test
    fun fastFlickCanCommitBeforeSlowDistanceThreshold() {
        assertTrue(
            shouldCommitPaperTurn(
                inwardDistance = 50f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.05f,
                releaseVelocityPxPerSec = 1400f
            )
        )
        assertFalse(
            shouldCommitPaperTurn(
                inwardDistance = 50f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.05f,
                releaseVelocityPxPerSec = 300f
            )
        )
        assertFalse(
            shouldCommitPaperTurn(
                inwardDistance = 8f,
                width = 1000f,
                density = 1f,
                curlProgress = 0.02f,
                releaseVelocityPxPerSec = 3000f
            )
        )
    }

    @Test
    fun `release velocity smooths short samples but reacts quickly to reversal`() {
        val stable = nextPaperReleaseVelocity(
            previousVelocityPxPerSec = 1_000f,
            distanceDeltaPx = 10f,
            elapsedMillis = 10L,
            sinceLastMotionMillis = 0L
        )
        val spike = nextPaperReleaseVelocity(
            previousVelocityPxPerSec = stable,
            distanceDeltaPx = 30f,
            elapsedMillis = 10L,
            sinceLastMotionMillis = 0L
        )
        val reversed = nextPaperReleaseVelocity(
            previousVelocityPxPerSec = 1_800f,
            distanceDeltaPx = -20f,
            elapsedMillis = 20L,
            sinceLastMotionMillis = 0L
        )

        assertEquals(1_000f, stable, 0.01f)
        assertTrue(spike > stable)
        assertTrue(spike < 3_000f)
        assertTrue(reversed < 0f)
        assertEquals(
            0f,
            nextPaperReleaseVelocity(
                previousVelocityPxPerSec = Float.NaN,
                distanceDeltaPx = Float.NaN,
                elapsedMillis = 16L,
                sinceLastMotionMillis = 200L
            ),
            0.0001f
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
    fun weightedDragDampsVerticalWobbleMoreThanHorizontalPull() {
        val start = Offset(900f, 500f)
        val finger = Offset(300f, 700f)
        val weighted = paperWeightedDragCurrent(start, finger)

        val rawX = kotlin.math.abs(finger.x - start.x)
        val rawY = kotlin.math.abs(finger.y - start.y)
        val weightedX = kotlin.math.abs(weighted.x - start.x)
        val weightedY = kotlin.math.abs(weighted.y - start.y)

        assertTrue(weightedX / rawX > weightedY / rawY)
    }

    @Test
    fun paperResistanceStartsHeavyAndReleasesWithTheTurn() {
        val earlyHorizontal = paperHorizontalDragResponse(0.05f)
        val midHorizontal = paperHorizontalDragResponse(0.50f)
        val lateHorizontal = paperHorizontalDragResponse(0.95f)

        val earlyVertical = paperVerticalDragResponse(0.05f)
        val lateVertical = paperVerticalDragResponse(0.95f)

        assertTrue(earlyHorizontal < midHorizontal)
        assertTrue(midHorizontal < lateHorizontal)
        assertTrue(earlyHorizontal < 0.80f)
        assertTrue(lateHorizontal > 0.90f)
        assertTrue(earlyVertical < lateVertical)
        assertTrue(earlyVertical < earlyHorizontal)
    }

    @Test
    fun inwardFractionIsStableAndClamped() {
        val start = Offset(900f, 500f)
        assertEquals(
            0.25f,
            paperInwardDragFraction(start, Offset(650f, 620f), 1000f),
            0.0001f
        )
        assertEquals(
            1f,
            paperInwardDragFraction(start, Offset(-500f, 620f), 1000f),
            0.0001f
        )
        assertEquals(
            0f,
            paperInwardDragFraction(start, Offset(300f, 620f), 0f),
            0.0001f
        )
    }

    @Test
    fun paperMaterialEffectsPeakDuringTheFold() {
        assertEquals(0f, paperEdgeThicknessIntensity(0f), 0.0001f)
        assertEquals(0f, paperEdgeThicknessIntensity(1f), 0.0001f)
        assertEquals(0f, paperBacksideInkIntensity(0f), 0.0001f)
        assertEquals(0f, paperBacksideInkIntensity(1f), 0.0001f)

        assertTrue(paperEdgeThicknessIntensity(0.5f) > 0.90f)
        assertTrue(paperBacksideInkIntensity(0.5f) > 0.70f)
    }

    @Test
    fun creaseAndContactShadowDisappearAtRest() {
        assertEquals(0f, paperCreaseIntensity(0f), 0.0001f)
        assertEquals(0f, paperCreaseIntensity(1f), 0.0001f)
        assertEquals(0f, paperContactShadowIntensity(0f), 0.0001f)
        assertEquals(0f, paperContactShadowIntensity(1f), 0.0001f)
        assertTrue(paperCreaseIntensity(0.5f) > 0.75f)
        assertTrue(paperContactShadowIntensity(0.5f) > 0.50f)
    }

    @Test
    fun fastReleaseUsesASeparateFlingRegime() {
        val slow = paperReleaseProfile(420f)
        val fast = paperReleaseProfile(1800f)

        assertEquals(PaperReleaseRegime.MANIPULATION, slow.regime)
        assertEquals(PaperReleaseRegime.FLING, fast.regime)
        assertTrue(fast.durationMillis < slow.durationMillis)
        assertTrue(fast.completionBias > slow.completionBias)
    }

    @Test
    fun pageStackMovesFromUnreadSideToReadSideAndMirrorsForRtl() {
        val start = paperPageStackDepth(0f, ReadingProgression.LTR)
        val middle = paperPageStackDepth(0.5f, ReadingProgression.LTR)
        val end = paperPageStackDepth(1f, ReadingProgression.LTR)
        val rtlStart = paperPageStackDepth(0f, ReadingProgression.RTL)

        assertEquals(2f, start.leftDp, 0.001f)
        assertEquals(8f, start.rightDp, 0.001f)
        assertEquals(5f, middle.leftDp, 0.001f)
        assertEquals(5f, middle.rightDp, 0.001f)
        assertEquals(8f, end.leftDp, 0.001f)
        assertEquals(2f, end.rightDp, 0.001f)
        assertEquals(start.rightDp, rtlStart.leftDp, 0.001f)
        assertEquals(start.leftDp, rtlStart.rightDp, 0.001f)
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
    @Test
    fun `paper curl waits for deliberate movement before lifting the sheet`() {
        assertFalse(
            hasDeliberatePaperIntent(
                offsetX = 12f,
                offsetY = 1f,
                width = 1_000f,
                density = 3f
            )
        )
        assertFalse(
            hasDeliberatePaperIntent(
                offsetX = 34f,
                offsetY = 33f,
                width = 1_000f,
                density = 3f
            )
        )
        assertTrue(
            hasDeliberatePaperIntent(
                offsetX = 34f,
                offsetY = 12f,
                width = 1_000f,
                density = 3f
            )
        )
    }

    @Test
    fun `paper and slide intent remain distinct rather than sharing accidental thresholds`() {
        val x = 28f
        val y = 4f
        assertTrue(
            hasDeliberatePaperIntent(
                offsetX = x,
                offsetY = y,
                width = 1_000f,
                density = 3f
            )
        )
        assertFalse(
            hasDeliberateSlideIntent(
                offsetX = x,
                offsetY = y,
                width = 1_000f,
                density = 3f
            )
        )
    }

    @Test
    fun `paper edge pull permits a natural diagonal while body swipe stays horizontal`() {
        val body = hasDeliberatePaperIntent(
            offsetX = 30f,
            offsetY = 36f,
            width = 1_000f,
            density = 3f,
            startsAtEdge = false
        )
        val edge = hasDeliberatePaperIntent(
            offsetX = 30f,
            offsetY = 36f,
            width = 1_000f,
            density = 3f,
            startsAtEdge = true
        )

        assertFalse(body)
        assertTrue(edge)
    }

    @Test
    fun `paper edge intent still rejects mostly vertical pulls`() {
        assertFalse(
            hasDeliberatePaperIntent(
                offsetX = 24f,
                offsetY = 70f,
                width = 1_000f,
                density = 3f,
                startsAtEdge = true
            )
        )
    }

    @Test
    fun `edge grip increases smoothly only near the physical page edge`() {
        assertEquals(0f, paperEdgeGrip(500f, 1_000f), 0.0001f)
        assertEquals(0f, paperEdgeGrip(700f, 1_000f), 0.0001f)
        assertTrue(paperEdgeGrip(850f, 1_000f) > 0f)
        assertEquals(1f, paperEdgeGrip(1_000f, 1_000f), 0.0001f)
        assertEquals(0f, paperEdgeGrip(900f, 0f), 0.0001f)
    }

    @Test
    fun `paper age deepens lifted sheet material without leaving safe bounds`() {
        val fresh = paperCurlMaterialAge(0f)
        val aged = paperCurlMaterialAge(1f)
        val fallback = paperCurlMaterialAge(Float.NaN)

        assertTrue(aged.backPageShadeAlpha > fresh.backPageShadeAlpha)
        assertTrue(aged.backsideFiberAlpha > fresh.backsideFiberAlpha)
        assertTrue(aged.edgeThicknessAlpha > fresh.edgeThicknessAlpha)
        assertTrue(aged.contactShadowAlpha > fresh.contactShadowAlpha)
        assertTrue(fallback.backsideFiberAlpha in fresh.backsideFiberAlpha..aged.backsideFiberAlpha)
    }

    @Test
    fun `reduced motion uses live mesh edge even with retained debug preview enabled`() {
        assertTrue(shouldCapturePaperTurnSnapshot(reducedMotion = false, liveMaterialEdge = true))
        assertFalse(shouldCapturePaperTurnSnapshot(reducedMotion = true, liveMaterialEdge = true))
    }

    @Test
    fun `paper curl render gate rejects zero and non finite resize frames`() {
        val validEdge = PaperCurlEdge(
            top = Offset(1_000f, 0f),
            bottom = Offset(1_000f, 1_600f)
        )
        assertTrue(isRenderablePaperCurlFrame(1_000f, 1_600f, validEdge))
        assertFalse(isRenderablePaperCurlFrame(0f, 1_600f, validEdge))
        assertFalse(isRenderablePaperCurlFrame(1_000f, 0f, validEdge))
        assertFalse(
            isRenderablePaperCurlFrame(
                1_000f,
                1_600f,
                validEdge.copy(top = Offset(Float.NaN, 0f))
            )
        )
    }

    @Test
    fun `paper line intersection rejects nearly parallel geometry before coordinates explode`() {
        val intersection = paperLineIntersection(
            line1a = Offset(0f, 0f),
            line1b = Offset(4_000f, 0.001f),
            line2a = Offset(0f, 1f),
            line2b = Offset(4_000f, 1.002f)
        )

        assertEquals(null, intersection)
    }

    @Test
    fun `paper line intersection rejects non finite input and keeps ordinary crossings exact`() {
        assertEquals(
            null,
            paperLineIntersection(
                line1a = Offset(Float.NaN, 0f),
                line1b = Offset(1f, 1f),
                line2a = Offset.Zero,
                line2b = Offset(1f, 0f)
            )
        )

        val crossing = requireNotNull(
            paperLineIntersection(
                line1a = Offset(0f, 0f),
                line1b = Offset(10f, 10f),
                line2a = Offset(0f, 10f),
                line2b = Offset(10f, 0f)
            )
        )
        assertEquals(5f, crossing.x, 0.0001f)
        assertEquals(5f, crossing.y, 0.0001f)
    }

    @Test
    fun `physical boundary side mirrors turn direction across reading progression`() {
        assertEquals(
            PaperCurlSide.RIGHT,
            paperTurnSideFor(PaperTurnDirection.FORWARD, ReadingProgression.LTR)
        )
        assertEquals(
            PaperCurlSide.LEFT,
            paperTurnSideFor(PaperTurnDirection.BACKWARD, ReadingProgression.LTR)
        )
        assertEquals(
            PaperCurlSide.LEFT,
            paperTurnSideFor(PaperTurnDirection.FORWARD, ReadingProgression.RTL)
        )
        assertEquals(
            PaperCurlSide.RIGHT,
            paperTurnSideFor(PaperTurnDirection.BACKWARD, ReadingProgression.RTL)
        )
    }

    @Test
    fun `paper terminal turn keeps a folded edge off book`() {
        val edge = paperTerminalTurnEdge(width = 1000f, height = 1600f)

        assertTrue(edge.top.x < 0f)
        assertTrue(edge.bottom.x < edge.top.x)
        assertTrue(edge.top.y > 0f)
        assertTrue(edge.bottom.y < 1600f)
    }

}
