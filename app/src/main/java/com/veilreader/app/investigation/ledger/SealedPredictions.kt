package com.veilreader.app.investigation.ledger

enum class SealState { SEALED, REVEALED }
enum class PredictionStatus { LOCKED, ELIGIBLE_FOR_EXPLICIT_REVEAL, REVEALED, UNVERIFIABLE }

/** Private author-owned domain record. Not presentation state; intentionally no text-bearing toString. */
class SealedPrediction(
    val id: PredictionId,
    val publication: PublicationIdentity,
    val ownerId: String,
    val privateText: String,
    val creationAnchor: AnchorReference,
    val targetAnchor: AnchorReference,
    val sealState: SealState = SealState.SEALED
) {
    init {
        require(ownerId.isNotBlank() && privateText.isNotBlank())
        require(creationAnchor.publication == publication && targetAnchor.publication == publication)
    }
}

/** Caller explicitly trusts coverage of precisely this target. Not generated from navigation,
 * dwell time, percentages or any reading history here; not proof of actual reading.
 */
data class SectionCoverage(val target: AnchorReference)

data class EvaluationContext(
    val publication: PublicationIdentity,
    val currentAnchor: AnchorReference? = null,
    val trustedCoverage: SectionCoverage? = null
)

/** No text is returned, and no seal state is changed by evaluation. */
fun evaluatePrediction(prediction: SealedPrediction, context: EvaluationContext): PredictionStatus {
    if (context.publication != prediction.publication ||
        (context.currentAnchor != null && context.currentAnchor.publication != prediction.publication)
    ) return PredictionStatus.UNVERIFIABLE
    val coverage = context.trustedCoverage
    if (coverage != null && coverage.target != prediction.targetAnchor) return PredictionStatus.UNVERIFIABLE
    if (prediction.sealState == SealState.REVEALED) return PredictionStatus.REVEALED
    return if (coverage != null) PredictionStatus.ELIGIBLE_FOR_EXPLICIT_REVEAL else PredictionStatus.LOCKED
}

/** Must be created by the caller in response to a deliberate owner action, never a background task.
 * The owner string is an assertion, not authentication; no security boundary is claimed.
 */
data class RevealAction(val ownerId: String) { init { require(ownerId.isNotBlank()) } }

/** Returns a new private record, never raw text. Null means the reveal was denied. */
fun revealPrediction(
    prediction: SealedPrediction,
    context: EvaluationContext,
    action: RevealAction
): SealedPrediction? {
    if (action.ownerId != prediction.ownerId ||
        evaluatePrediction(prediction, context) != PredictionStatus.ELIGIBLE_FOR_EXPLICIT_REVEAL
    ) return null
    return SealedPrediction(prediction.id, prediction.publication, prediction.ownerId,
        prediction.privateText, prediction.creationAnchor, prediction.targetAnchor, SealState.REVEALED)
}

/** Safe even after reveal: displaying private text requires a separate, intentionally designed UI. */
data class PredictionSummary(val id: PredictionId, val status: PredictionStatus)
fun predictionSummary(prediction: SealedPrediction, context: EvaluationContext) =
    PredictionSummary(prediction.id, evaluatePrediction(prediction, context))
