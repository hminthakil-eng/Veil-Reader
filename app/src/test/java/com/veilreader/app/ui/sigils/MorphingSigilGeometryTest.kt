package com.veilreader.app.ui.sigils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun advancement_changesGeometryWithoutChangingPathIdentity() {
        canonicalPaths.forEach { pathId ->
            val rankZero = sigilGeometryFor(pathId, rankIndex = 0)
            val rankOne = sigilGeometryFor(pathId, rankIndex = 1)
            val rankTwo = sigilGeometryFor(pathId, rankIndex = 2)

            assertNotEquals(rankZero, rankOne)
            assertNotEquals(rankOne, rankTwo)
        }
    }

    @Test
    fun unknownPath_usesOneDeterministicFallback() {
        val fallback = sigilGeometryFor("unknown", rankIndex = 3)

        assertEquals(fallback, sigilGeometryFor("another-unknown", rankIndex = 3))
        assertEquals(fallback, sigilGeometryFor("UNKNOWN", rankIndex = 3))
    }
}
