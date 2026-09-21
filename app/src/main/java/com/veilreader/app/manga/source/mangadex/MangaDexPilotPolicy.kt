package com.veilreader.app.manga.source.mangadex

/**
 * Source-specific compliance facts for the controlled MangaDex pilot.
 *
 * Keep this provider disabled from the production route until the current MangaDex acceptable-use
 * policy has been reviewed again for the intended distribution model.
 */
object MangaDexPilotPolicy {
    const val SOURCE_CREDIT = "MangaDex"
    const val SOURCE_WEBSITE = "https://mangadex.org"
    const val REQUIRES_SCANLATION_GROUP_CREDIT = true
    const val MONETIZED_DISTRIBUTION_ALLOWED_BY_REVIEWED_POLICY = false
    const val PRODUCTION_ENABLE_REQUIRES_POLICY_REVIEW = true
}
