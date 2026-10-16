package com.veilreader.app.investigation.ledger

/** Shape validation only: caller must supply SHA-256 of ALL original source bytes. */
data class PublicationIdentity(val fullSourceSha256: String, val sourceType: SourceType) {
    init { require(fullSourceSha256.matches(Regex("[0-9a-f]{64}"))) }
}

enum class SourceType { EPUB, PDF }

// IDs are opaque caller-issued tokens, never derived from text or hashCode.
// The caller owns global issuance; LedgerSnapshot rejects duplicates within a snapshot.
data class TheoryId(val value: String) { init { require(value.isNotBlank()) } }
data class LinkId(val value: String) { init { require(value.isNotBlank()) } }
data class PredictionId(val value: String) { init { require(value.isNotBlank()) } }

/** Caller-validated resource/chapter key, not an inferred page number or decoded locator. */
data class ResourceIdentity(val sourceType: SourceType, val key: String) {
    init { require(key.isNotBlank()) }
}

/** Retains the original opaque locator privately. Does not parse, order or normalize it. */
data class AnchorReference(
    val publication: PublicationIdentity,
    val resource: ResourceIdentity,
    val opaqueLocator: String
) {
    init {
        require(publication.sourceType == resource.sourceType)
        require(opaqueLocator.isNotBlank())
    }
}

class ClueTheory(
    val id: TheoryId,
    val publication: PublicationIdentity,
    val ownerId: String,
    val privateTitle: String,
    val privateBody: String,
    highlightIds: List<String>
) {
    val highlightIds: List<String> = highlightIds.toList()
    init {
        require(ownerId.isNotBlank())
        require(privateTitle.isNotBlank())
        require(this.highlightIds.all { it.isNotBlank() })
    }
}

/** Both endpoints belong to this exact publication version. Cross-publication links are unsupported. */
data class ClueLink(
    val id: LinkId,
    val publication: PublicationIdentity,
    val sourceHighlightId: String,
    val targetHighlightId: String,
    val privateRelationLabel: String
) {
    init {
        require(sourceHighlightId.isNotBlank() && targetHighlightId.isNotBlank())
        require(sourceHighlightId != targetHighlightId)
        require(privateRelationLabel.isNotBlank())
    }
}

/** Immutable snapshot boundary; duplicate record identities are rejected, never silently merged. */
class LedgerSnapshot(theories: List<ClueTheory>, links: List<ClueLink>, predictions: List<SealedPrediction>) {
    val theories = theories.toList()
    val links = links.toList()
    val predictions = predictions.toList()
    init {
        require(this.theories.map { it.id }.distinct().size == this.theories.size)
        require(this.links.map { it.id }.distinct().size == this.links.size)
        require(this.predictions.map { it.id }.distinct().size == this.predictions.size)
    }
}

data class TheorySummary(val id: TheoryId, val highlightCount: Int)
fun theorySummary(theory: ClueTheory) = TheorySummary(theory.id, theory.highlightIds.size)

data class LinkSummary(val id: LinkId, val source: HighlightReferenceStatus, val target: HighlightReferenceStatus)
fun linkSummary(link: ClueLink, snapshot: HighlightSnapshot): LinkSummary {
    val resolved = resolveHighlights(link.publication, listOf(link.sourceHighlightId, link.targetHighlightId), snapshot)
    return LinkSummary(link.id, resolved[0].status, resolved[1].status)
}
