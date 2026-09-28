package com.veilreader.app.domain

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilentNamesSourceGrammarTest {
    @Test fun `every choice has dual-book provenance and an original Veil transformation`() {
        SilentNamesChoice.entries.forEach { choice ->
            val provenance = SilentNamesSourceGrammar.forChoice(choice)
            assertFalse(provenance.lotmConcepts.isEmpty())
            assertFalse(provenance.coiConcepts.isEmpty())
            assertTrue(provenance.veilTransformation.length >= 32)
        }
    }

    @Test fun `every outcome has dual-book provenance`() {
        SilentNamesOutcome.entries.forEach { outcome ->
            val provenance = SilentNamesSourceGrammar.forOutcome(outcome)
            assertFalse(provenance.lotmConcepts.isEmpty())
            assertFalse(provenance.coiConcepts.isEmpty())
            assertTrue(provenance.veilTransformation.isNotBlank())
        }
    }

    @Test fun `all canonical paths and unknown future paths retain dual-source identity`() {
        (SampleData.paths.map { it.id } + "future_path").forEach { pathId ->
            val provenance = SilentNamesSourceGrammar.forPath(pathId)
            assertFalse(provenance.lotmConcepts.isEmpty())
            assertFalse(provenance.coiConcepts.isEmpty())
            assertTrue(provenance.veilTransformation.isNotBlank())
        }
    }
}
