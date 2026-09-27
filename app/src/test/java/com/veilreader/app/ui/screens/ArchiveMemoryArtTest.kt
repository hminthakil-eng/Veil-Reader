package com.veilreader.app.ui.screens

import com.veilreader.app.domain.EchoDepth
import com.veilreader.app.domain.HighlightMemory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveMemoryArtTest {
    private fun memory(
        depth: EchoDepth,
        ageDays: Int,
        revisitCount: Int = 0,
        annotated: Boolean = false,
        continuedActivity: Boolean = false
    ) = HighlightMemory(
        ageKnown = true,
        ageDays = ageDays,
        ageLabel = "AGE",
        echoDepth = depth,
        echoLabel = if (depth == EchoDepth.FRESH) null else "ECHO",
        bookActivityAfterMark = continuedActivity,
        revisitCount = revisitCount,
        lastViewedAtEpochMs = null,
        lastViewedLabel = null,
        annotated = annotated,
        eligibleForEcho = depth != EchoDepth.FRESH,
        resonanceScore = 100
    )

    @Test
    fun `older memory gains physical strata and patina`() {
        val fresh = archiveMemoryMaterialFor(
            memory(EchoDepth.FRESH, ageDays = 2),
            echoMode = false
        )
        val deep = archiveMemoryMaterialFor(
            memory(EchoDepth.DEEP_ECHO, ageDays = 800),
            echoMode = true
        )

        assertTrue(deep.strataCount > fresh.strataCount)
        assertTrue(deep.patina > fresh.patina)
        assertTrue(deep.edgeGlow > fresh.edgeGlow)
        assertTrue(deep.resurfaced)
        assertFalse(fresh.resurfaced)
    }

    @Test
    fun `exact passage revisits strengthen spirit trace`() {
        val neverReturned = archiveMemoryMaterialFor(
            memory(EchoDepth.ECHO, ageDays = 90),
            echoMode = true
        )
        val revisited = archiveMemoryMaterialFor(
            memory(EchoDepth.ECHO, ageDays = 90, revisitCount = 4),
            echoMode = true
        )

        assertTrue(revisited.spiritTrace > neverReturned.spiritTrace)
        assertTrue(revisited.edgeGlow > neverReturned.edgeGlow)
    }

    @Test
    fun `annotation affects patina without fabricating a revisit`() {
        val plain = archiveMemoryMaterialFor(
            memory(EchoDepth.TRACE, ageDays = 20),
            echoMode = false
        )
        val annotated = archiveMemoryMaterialFor(
            memory(EchoDepth.TRACE, ageDays = 20, annotated = true),
            echoMode = false
        )

        assertTrue(annotated.patina > plain.patina)
        assertTrue(annotated.spiritTrace == plain.spiritTrace)
    }

    @Test
    fun `continued volume activity adds a trace independently of revisit count`() {
        val quiet = archiveMemoryMaterialFor(
            memory(EchoDepth.ECHO, ageDays = 90),
            echoMode = true
        )
        val continued = archiveMemoryMaterialFor(
            memory(
                EchoDepth.ECHO,
                ageDays = 90,
                continuedActivity = true
            ),
            echoMode = true
        )

        assertTrue(continued.spiritTrace > quiet.spiritTrace)
        assertTrue(continued.edgeGlow == quiet.edgeGlow)
    }

    @Test
    fun `unknown or corrupt negative age never produces an invalid material policy`() {
        val policy = archiveMemoryMaterialFor(
            memory(EchoDepth.FRESH, ageDays = -1, revisitCount = -3),
            echoMode = false
        )

        assertTrue(policy.strataCount >= 1)
        assertTrue(policy.patina in 0f..1f)
        assertTrue(policy.edgeGlow in 0f..1f)
        assertTrue(policy.spiritTrace in 0f..1f)
        assertFalse(policy.resurfaced)
    }

}
