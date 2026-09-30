package com.veilreader.app.domain

/**
 * Runtime fidelity policy for Veil Reader.
 *
 * Reading correctness, typography, touch targets and durable state are invariant across tiers.
 * Only decorative/atmospheric work may scale down.
 */
enum class PerformanceTier {
    FULL,
    BALANCED,
    ESSENTIAL
}
