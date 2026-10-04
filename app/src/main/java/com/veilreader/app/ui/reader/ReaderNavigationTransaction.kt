package com.veilreader.app.ui.reader

import java.net.URI
import kotlin.math.abs
import org.readium.r2.shared.publication.Locator

/**
 * Owns one programmatic Reader jump until the debounced navigator stream publishes its settled
 * destination. A re-emitted origin locator is still part of the handoff and must not consume the
 * transaction; intermediate navigator positions must not be treated as user page turns.
 *
 * The transaction expires defensively. If a navigator accepts a no-op jump and emits nothing,
 * a later real user page turn must not be misclassified forever.
 */
internal data class ReaderNavigationTransaction(
    val token: Long,
    val originLocatorJson: String?,
    val targetIdentity: ReaderNavigationIdentity?,
    val targetHref: String?,
    val passageVisitLocatorJson: String?,
    val startedAtElapsedMs: Long,
    val expectedPdfPage: Int? = null,
    val originPdfPage: Int? = null
)

/**
 * Starts a location-backed jump only when the stable navigation identity can actually move.
 *
 * Persisted locators can differ in title/text metadata while pointing at the same publication
 * position. Readium may accept that semantic no-op without emitting another locator, so compare
 * navigation identity rather than raw JSON before opening a transaction.
 */
internal fun shouldStartReaderIdentityJump(
    origin: ReaderNavigationIdentity?,
    target: ReaderNavigationIdentity?
): Boolean =
    target != null &&
        (origin == null || !readerNavigationIdentityMatchesTarget(origin, target))

internal fun readerEffectiveTargetHref(
    currentHref: String?,
    targetHref: String?
): String? {
    val target = targetHref?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val fragment = target.substringAfter('#', missingDelimiterValue = "")
    val targetResource = target.substringBefore('#')
    if (targetResource.isNotEmpty() || fragment.isEmpty()) return target

    val current = currentHref?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val currentResource = readerResourceHref(current)
    return if (currentResource.isEmpty()) null else "$currentResource#$fragment"
}

internal fun shouldStartReaderLinkJump(
    currentHref: String?,
    targetHref: String?
): Boolean {
    val target = targetHref?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val current = currentHref?.trim()?.takeIf { it.isNotEmpty() } ?: return true
    if (current == target) return false

    val targetFragment = target.substringAfter('#', missingDelimiterValue = "")
    if (targetFragment.isNotEmpty()) return true

    return readerResourceHref(current) != readerResourceHref(target)
}

internal data class ReaderNavigationIdentity(
    val href: String?,
    val position: Int?,
    val cssSelector: String?,
    val totalProgression: Double?,
    val progression: Double? = null
)

internal fun Locator.toReaderNavigationIdentity(): ReaderNavigationIdentity =
    ReaderNavigationIdentity(
        href = href.toString(),
        position = locations.position,
        cssSelector = (locations["cssSelector"] as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() },
        totalProgression = locations.totalProgression,
        progression = locations.progression
    )

internal fun readerNavigationTargetMatches(
    observed: ReaderNavigationIdentity?,
    target: ReaderNavigationIdentity?,
    targetHref: String?
): Boolean {
    if (target != null) {
        return readerNavigationIdentityMatchesTarget(
            observed = observed,
            target = target
        )
    }
    if (!targetHref.isNullOrBlank()) {
        val observedHref = observed?.href?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return readerResourceHref(observedHref) == readerResourceHref(targetHref)
    }
    return true
}

internal fun readerNavigationIdentityMatchesTarget(
    observed: ReaderNavigationIdentity?,
    target: ReaderNavigationIdentity
): Boolean {
    observed ?: return false

    val targetHref = target.href?.trim()?.takeIf { it.isNotEmpty() }
    val observedHref = observed.href?.trim()?.takeIf { it.isNotEmpty() }
    if (targetHref != null) {
        if (observedHref == null) return false
        if (readerResourceHref(observedHref) != readerResourceHref(targetHref)) return false
    }

    if (target.position != null && observed.position != null) {
        return target.position == observed.position
    }

    if (target.cssSelector != null && observed.cssSelector != null) {
        return target.cssSelector == observed.cssSelector
    }

    if (target.totalProgression != null && observed.totalProgression != null) {
        return abs(target.totalProgression - observed.totalProgression) <=
            LOCATOR_PROGRESSION_TOLERANCE
    }

    val targetHasLocationDiscriminator =
        target.position != null ||
            target.cssSelector != null ||
            target.totalProgression != null
    return !targetHasLocationDiscriminator
}

/**
 * Page-turn settlement is intentionally stricter than programmatic-target matching.
 *
 * Readium's EPUB navigator derives locations.position from coarse publication-position
 * chunks, while locations.progression tracks the actual paginated WebView viewport.
 * Adjacent visible pages can therefore share the same position and totalProgression.
 */
internal fun readerNavigationIdentityHasVisuallyDeparted(
    origin: ReaderNavigationIdentity,
    observed: ReaderNavigationIdentity?
): Boolean {
    observed ?: return false

    val originHref = origin.href?.trim()?.takeIf { it.isNotEmpty() }
    val observedHref = observed.href?.trim()?.takeIf { it.isNotEmpty() }
    if (originHref != null && observedHref != null) {
        if (readerResourceHref(originHref) != readerResourceHref(observedHref)) {
            return true
        }
    } else if (originHref != observedHref) {
        return true
    }

    val originProgression =
        origin.progression?.takeIf { it.isFinite() }
    val observedProgression =
        observed.progression?.takeIf { it.isFinite() }
    if (originProgression != null && observedProgression != null) {
        return abs(originProgression - observedProgression) >
            VISUAL_PAGE_PROGRESSION_TOLERANCE
    }

    return !readerNavigationIdentityMatchesTarget(
        observed = observed,
        target = origin
    )
}

private fun readerResourceHref(href: String): String {
    val resource = href.trim().substringBefore('#')
    if (resource.isEmpty()) return resource
    return runCatching { URI(resource).normalize().toString() }
        .getOrDefault(resource)
        .removePrefix("./")
}

private const val LOCATOR_PROGRESSION_TOLERANCE = 0.0025
private const val VISUAL_PAGE_PROGRESSION_TOLERANCE = 0.0001

internal fun nextReaderNavigationTransactionToken(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else (current + 1L).coerceAtLeast(1L)

internal class ReaderNavigationTransactionGate(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    private var nextToken = 0L
    private var active: ReaderNavigationTransaction? = null

    @Synchronized
    fun begin(
        originLocatorJson: String?,
        nowElapsedMs: Long,
        targetIdentity: ReaderNavigationIdentity? = null,
        targetHref: String? = null,
        passageVisitLocatorJson: String? = null,
        expectedPdfPage: Int? = null,
        originPdfPage: Int? = null
    ): ReaderNavigationTransaction {
        nextToken = nextReaderNavigationTransactionToken(nextToken)
        val transaction = ReaderNavigationTransaction(
            token = nextToken,
            originLocatorJson = originLocatorJson,
            targetIdentity = targetIdentity,
            targetHref = targetHref,
            passageVisitLocatorJson = passageVisitLocatorJson,
            startedAtElapsedMs = nowElapsedMs,
            expectedPdfPage = expectedPdfPage?.takeIf { it > 0 },
            originPdfPage = originPdfPage?.takeIf { it > 0 }
        )
        active = transaction
        return transaction
    }

    @Synchronized
    fun cancel(token: Long) {
        if (active?.token == token) active = null
    }

    @Synchronized
    fun isActive(nowElapsedMs: Long): Boolean =
        freshActive(nowElapsedMs) != null

    @Synchronized
    fun consumeSettled(
        observedLocatorJson: String? = null,
        nowElapsedMs: Long,
        observedIdentity: ReaderNavigationIdentity? = null,
        observedPdfPage: Int? = null
    ): ReaderNavigationTransaction? {
        val transaction = freshActive(nowElapsedMs) ?: return null

        // Readium may re-publish the pre-jump currentLocator while go() is still moving the
        // resource/page. That emission is not the destination and must not consume the jump.
        if (
            transaction.originLocatorJson != null &&
            observedLocatorJson == transaction.originLocatorJson
        ) {
            return null
        }

        // Locator-backed and link-backed jumps know enough about their intended destination to
        // ignore Readium's transient intermediate positions. Legacy/targetless transactions keep
        // the original first-different-locator behavior.
        if (
            !readerNavigationTargetMatches(
                observed = observedIdentity,
                target = transaction.targetIdentity,
                targetHref = transaction.targetHref
            )
        ) {
            return null
        }

        if (transaction.expectedPdfPage != null && transaction.expectedPdfPage != observedPdfPage) return null
        active = null
        return transaction
    }

    /** A lifecycle flush may commit a reached PDF destination before the 500ms UI debounce. */
    @Synchronized
    fun consumeReachedPdfDestination(nowElapsedMs: Long, observedPdfPage: Int?): ReaderNavigationTransaction? {
        val transaction = freshActive(nowElapsedMs) ?: return null
        if (transaction.expectedPdfPage == null || observedPdfPage == null || observedPdfPage <= 0) return null
        val reachedDestination = transaction.expectedPdfPage == observedPdfPage
        // An unanimated PDF jump has no intermediate page animation. A real page beyond its
        // source can also be a user swipe immediately after that jump; final flush must not lose it.
        val movedPastSource = transaction.originPdfPage != null && transaction.originPdfPage != observedPdfPage
        if (!reachedDestination && !movedPastSource) return null
        active = null
        return transaction
    }

    @Synchronized
    fun cancelActive(nowElapsedMs: Long): ReaderNavigationTransaction? {
        val transaction = freshActive(nowElapsedMs) ?: return null
        active = null
        return transaction
    }

    @Synchronized
    fun reset() {
        active = null
    }

    private fun freshActive(nowElapsedMs: Long): ReaderNavigationTransaction? {
        val transaction = active ?: return null
        val elapsed = nowElapsedMs - transaction.startedAtElapsedMs
        if (elapsed < 0L || elapsed > timeoutMs.coerceAtLeast(0L)) {
            active = null
            return null
        }
        return transaction
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 3_000L
    }
}

/** Fast PDF swipes can flush beyond a jump target, but cannot fabricate a passage revisit there. */
internal fun ReaderNavigationTransaction.passageVisitAfterSettlement(observedPdfPage: Int? = null): String? =
    passageVisitLocatorJson?.takeIf { expectedPdfPage == null || expectedPdfPage == observedPdfPage }

internal fun ReaderNavigationTransaction.hasReachedObservedDestination(
    observedLocatorJson: String?,
    observedIdentity: ReaderNavigationIdentity?,
    observedPdfPage: Int? = null
): Boolean {
    if (
        originLocatorJson != null &&
        observedLocatorJson == originLocatorJson
    ) {
        return false
    }
    if (
        !readerNavigationTargetMatches(
            observed = observedIdentity,
            target = targetIdentity,
            targetHref = targetHref
        )
    ) {
        return false
    }
    return expectedPdfPage == null || expectedPdfPage == observedPdfPage
}
