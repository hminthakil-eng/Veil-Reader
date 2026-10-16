package com.veilreader.app.investigation.ledger

import com.veilreader.app.domain.Highlight
import org.junit.Assert.*
import org.junit.Test

class SealedLedgerTest {
    private val publication = PublicationIdentity("a".repeat(64), SourceType.EPUB)
    private val resource = ResourceIdentity(SourceType.EPUB, "chapter-resource")
    private val start = AnchorReference(publication, resource, "opaque-start")
    private val target = AnchorReference(publication, resource, "opaque-target")
    private val prediction = SealedPrediction(PredictionId("p"), publication, "owner", "PRIVATE prediction", start, target)
    private val coverage = SectionCoverage(target)
    private val context = EvaluationContext(publication, target, coverage)
    private val highlight = Highlight("h1", "book", "PRIVATE quote", "opaque", createdAtEpochMs = 0)
    private fun snapshot(records: List<Highlight> = listOf(highlight), deleted: Set<String> = emptySet()) =
        HighlightSnapshot(records, listOf(PublicationBinding("book", publication)), deleted)

    @Test fun `fingerprint mismatch including reimport is unverifiable`() {
        val changed = publication.copy(fullSourceSha256 = "b".repeat(64))
        assertEquals(PredictionStatus.UNVERIFIABLE, evaluatePrediction(prediction, context.copy(publication = changed)))
    }

    @Test fun `jump alone stays locked`() {
        assertEquals(PredictionStatus.LOCKED, evaluatePrediction(prediction, context.copy(trustedCoverage = null)))
        assertNull(revealPrediction(prediction, context.copy(trustedCoverage = null), RevealAction("owner")))
    }

    @Test fun `exact caller trusted coverage allows only manual eligibility`() {
        assertEquals(PredictionStatus.ELIGIBLE_FOR_EXPLICIT_REVEAL, evaluatePrediction(prediction, context))
        assertEquals(SealState.SEALED, prediction.sealState)
        assertFalse(predictionSummary(prediction, context).toString().contains("PRIVATE"))
    }

    @Test fun `deliberate owner reveal changes state without returning text`() {
        assertNull(revealPrediction(prediction, context, RevealAction("someone else")))
        val revealed = revealPrediction(prediction, context, RevealAction("owner"))!!
        assertEquals(SealState.REVEALED, revealed.sealState)
        assertEquals(PredictionStatus.REVEALED, evaluatePrediction(revealed, context.copy(trustedCoverage = null)))
        assertFalse(predictionSummary(revealed, context).toString().contains("PRIVATE"))
        assertEquals(PredictionStatus.UNVERIFIABLE, evaluatePrediction(revealed,
            context.copy(publication = publication.copy(fullSourceSha256 = "b".repeat(64)))))
    }

    @Test fun `deleted and absent source highlights become tombstones`() {
        assertEquals(HighlightReferenceStatus.MISSING, resolveHighlights(publication, listOf("h1"), snapshot(deleted = setOf("h1"))).single().status)
        assertEquals(HighlightReferenceStatus.MISSING, resolveHighlights(publication, listOf("gone"), snapshot()).single().status)
    }

    @Test fun `duplicate requested or stored ids never pick arbitrary record`() {
        assertTrue(resolveHighlights(publication, listOf("h1", "h1"), snapshot()).all { it.status == HighlightReferenceStatus.MISSING })
        assertNull(resolveHighlights(publication, listOf("h1"), snapshot(listOf(highlight, highlight))).single().highlight)
    }

    @Test fun `links require distinct endpoints and ids are unique per ledger`() {
        rejects { ClueLink(LinkId("l"), publication, "h1", "h1", "because") }
        rejects { ClueLink(LinkId("l"), publication, "h1", "h2", " ") }
        val first = ClueLink(LinkId("l1"), publication, "h1", "h2", "because")
        val second = first.copy(id = LinkId("l2"))
        assertNotEquals(first.id, second.id)
        LedgerSnapshot(emptyList(), listOf(first, second), emptyList())
        rejects { LedgerSnapshot(emptyList(), listOf(first, first), emptyList()) }
        val theory = ClueTheory(TheoryId("t"), publication, "owner", "PRIVATE title", "PRIVATE body", listOf("h1"))
        rejects { LedgerSnapshot(listOf(theory, theory), emptyList(), emptyList()) }
        rejects { LedgerSnapshot(emptyList(), emptyList(), listOf(prediction, prediction)) }
        assertFalse(theorySummary(theory).toString().contains("PRIVATE"))
    }

    @Test fun `fingerprints reject partial uppercase and malformed input`() {
        listOf("a".repeat(63), "A".repeat(64), "g".repeat(64), " a".repeat(32)).forEach {
            rejects { PublicationIdentity(it, SourceType.EPUB) }
        }
    }

    @Test fun `sealed projection has no private text quote body or locator`() {
        val summary = predictionSummary(prediction, context)
        // Compose adds a compiler-only $stable field that Java reflection does not mark synthetic.
        // Exclude only that known metadata; any new private data field must still fail this test.
        assertEquals(setOf("id", "status"), summary.javaClass.declaredFields
            .filterNot { it.isSynthetic || it.name == "\$stable" }.map { it.name }.toSet())
        assertFalse(summary.toString().contains("opaque-target"))
        assertFalse(summary.toString().contains("PRIVATE"))
    }

    @Test fun `incompatible certificate resource source or locator fails closed`() {
        val otherResource = target.copy(resource = resource.copy(key = "other"))
        val pdf = PublicationIdentity(publication.fullSourceSha256, SourceType.PDF)
        val otherSource = AnchorReference(pdf, ResourceIdentity(SourceType.PDF, "page-token"), "opaque-target")
        listOf(otherResource, otherSource, target.copy(opaqueLocator = "different")).forEach {
            assertEquals(PredictionStatus.UNVERIFIABLE, evaluatePrediction(prediction, context.copy(trustedCoverage = SectionCoverage(it))))
        }
        rejects { AnchorReference(publication, ResourceIdentity(SourceType.PDF, "token"), "locator") }
        rejects { ResourceIdentity(SourceType.EPUB, " ") }
    }

    @Test fun `wrong or ambiguous publication binding is missing`() {
        val changed = publication.copy(fullSourceSha256 = "b".repeat(64))
        val bindings = listOf(PublicationBinding("book", publication), PublicationBinding("book", changed))
        assertNull(resolveHighlights(publication, listOf("h1"), HighlightSnapshot(listOf(highlight), bindings)).single().highlight)
        assertNull(resolveHighlights(changed, listOf("h1"), snapshot()).single().highlight)
        assertSame(highlight, resolveHighlights(publication, listOf("h1"), snapshot()).single().highlight)
    }

    private fun rejects(block: () -> Unit) {
        try { block() } catch (_: IllegalArgumentException) { return }
        fail("Expected invalid domain input to be rejected")
    }
}
