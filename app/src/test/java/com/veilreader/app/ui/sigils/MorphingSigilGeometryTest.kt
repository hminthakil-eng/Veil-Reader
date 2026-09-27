package com.veilreader.app.ui.sigils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphingSigilGeometryTest {

    private val canonicalPaths = listOf(
        "oracle",
        "dreamwalker",
        "archivist",
        "vanguard",
        "nocturne",
        "artificer"
    )

    @Test
    fun baseGeometry_isStableAndDistinctAcrossCanonicalPaths() {
        val first = canonicalPaths.associateWith { sigilGeometryFor(it, rankIndex = 0) }
        val second = canonicalPaths.associateWith { sigilGeometryFor(it, rankIndex = 0) }

        assertEquals(first, second)
        assertEquals(canonicalPaths.size, first.values.toSet().size)
    }

    @Test
    fun everyRank_isDeterministicAndRemainsInsideGeometryBounds() {
        canonicalPaths.forEach { pathId ->
            (-4..32).forEach { rank ->
                val first = sigilGeometryFor(pathId, rank)
                val second = sigilGeometryFor(pathId, rank)

                assertEquals(first, second)
                assertTrue(first.points in 3..9)
                assertTrue(first.innerRadius in 0.38f..0.72f)
                assertTrue(first.rotationQuarterTurns in 0..3)
            }
        }
    }

    @Test
    fun negativeRanks_clampToRankZero() {
        canonicalPaths.forEach { pathId ->
            assertEquals(
                sigilGeometryFor(pathId, rankIndex = 0),
                sigilGeometryFor(pathId, rankIndex = -99)
            )
        }
    }

    @Test
    fun everyCanonicalAdvancement_changesPersistentGeometry() {
        canonicalPaths.forEach { pathId ->
            val ranks = (0..5).map { rank -> sigilGeometryFor(pathId, rank) }

            ranks.zipWithNext().forEachIndexed { rank, (from, to) ->
                assertTrue(
                    "$pathId rank $rank -> ${rank + 1} must change the final sigil",
                    from != to
                )
            }
        }
    }

    @Test
    fun rotationInterpolation_usesTheShortestArcAcrossQuarterTurnWrap() {
        assertEquals(90f, shortestSigilRotationDeltaDegrees(3, 0), 0.001f)
        assertEquals(-90f, shortestSigilRotationDeltaDegrees(0, 3), 0.001f)
        assertEquals(90f, shortestSigilRotationDeltaDegrees(1, 2), 0.001f)
    }

    @Test
    fun unknownPath_usesOneDeterministicFallback() {
        val fallback = sigilGeometryFor("unknown", rankIndex = 3)

        assertEquals(fallback, sigilGeometryFor("another-unknown", rankIndex = 3))
        assertEquals(fallback, sigilGeometryFor("UNKNOWN", rankIndex = 3))
    }
}
