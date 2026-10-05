package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailAdaptivePolicyTest {
    @Test
    fun normalPhone_usesCompactSideBySideHero() {
        val policy = bookDetailAdaptivePolicy(
            widthDp = 390,
            fontScale = 1f
        )

        assertTrue(policy.compactHero)
        assertEquals(BookDetailCompactHeroLayout.SIDE_BY_SIDE, policy.compactLayout)
    }

    @Test
    fun largeTextPhone_prioritizesIdentityBeforeArtwork() {
        val policy = bookDetailAdaptivePolicy(
            widthDp = 390,
            fontScale = 1.5f
        )

        assertTrue(policy.compactHero)
        assertEquals(BookDetailCompactHeroLayout.IDENTITY_FIRST, policy.compactLayout)
    }

    @Test
    fun narrowPhone_prioritizesIdentityBeforeArtwork() {
        val policy = bookDetailAdaptivePolicy(
            widthDp = 360,
            fontScale = 1f
        )

        assertTrue(policy.compactHero)
        assertEquals(BookDetailCompactHeroLayout.IDENTITY_FIRST, policy.compactLayout)
    }

    @Test
    fun expandedLayout_isNotCompact() {
        val policy = bookDetailAdaptivePolicy(
            widthDp = 840,
            fontScale = 1f
        )

        assertFalse(policy.compactHero)
    }
}
