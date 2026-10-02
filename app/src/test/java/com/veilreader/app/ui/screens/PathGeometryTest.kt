package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PathGeometryTest {
    @Test
    fun `canonical paths have distinct ritual geometry`() {
        val ids = listOf(
            "oracle",
            "dreamwalker",
            "archivist",
            "vanguard",
            "nocturne",
            "artificer"
        )

        val geometries = ids.map(::pathGeometryFor)

        assertEquals(ids.size, geometries.toSet().size)
        assertEquals(PathGeometryKind.RADIAL_EYE, pathGeometryFor("oracle"))
        assertEquals(
            PathGeometryKind.ASYMMETRIC_CONSTELLATION,
            pathGeometryFor("dreamwalker")
        )
        assertEquals(
            PathGeometryKind.CONCENTRIC_ARCHIVE,
            pathGeometryFor("archivist")
        )
        assertEquals(PathGeometryKind.AXIAL_SPEAR, pathGeometryFor("vanguard"))
        assertEquals(PathGeometryKind.ECLIPSE, pathGeometryFor("nocturne"))
        assertEquals(PathGeometryKind.MECHANICAL, pathGeometryFor("artificer"))
        assertEquals(PathGeometryKind.RADIAL_EYE, pathGeometryFor("unknown"))
    }

    @Test
    fun `committed path hides dead alternatives`() {
        assertEquals(true, showAlternativePathChoices(rankIndex = 0))
        assertEquals(false, showAlternativePathChoices(rankIndex = 1))
        assertEquals(false, showAlternativePathChoices(rankIndex = 8))
    }

    @Test
    fun `advancement action exists only when a real next rank is ready`() {
        assertEquals(false, showAdvancementAction(hasNextRank = false, canAdvance = false))
        assertEquals(false, showAdvancementAction(hasNextRank = false, canAdvance = true))
        assertEquals(false, showAdvancementAction(hasNextRank = true, canAdvance = false))
        assertEquals(true, showAdvancementAction(hasNextRank = true, canAdvance = true))
    }

}
