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
}
