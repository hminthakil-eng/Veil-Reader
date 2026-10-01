package com.veilreader.app.ui.reader

import kotlin.math.abs
import org.json.JSONObject

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
    val targetLocatorJson: String?,
    val targetHref: String?,
    val startedAtElapsedMs: Long
)

/**
 * Starts a location-backed jump only when it can actually move away from the current locator.
 *
 * Readium may accept a no-op `go()` without publishing a new locator. Avoiding a transaction for
 * that case prevents the user's next real page turn from being mistaken for a delayed jump settle.
 */
internal fun shouldStartReaderLocationJump(
    originLocatorJson: String?,
    targetLocatorJson: String?
): Boolean {
    val target = targetLocatorJson?.takeIf { it.isNotBlank() } ?: return false
    val origin = originLocatorJson?.takeIf { it.isNotBlank() }
    return origin == null || origin != target
}

internal fun shouldStartReaderLinkJump(
    currentHref: String?,
    targetHref: String?
): Boolean {
    val target = targetHref?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val current = currentHref?.trim()?.takeIf { it.isNotEmpty() }
    return current == null || current != target
}

internal fun readerNavigationTargetMatches(
    observedLocatorJson: String?,
    targetLocatorJson: String?,
    targetHref: String?
): Boolean {
    if (!targetLocatorJson.isNullOrBlank()) {
        return readerLocatorMatchesTarget(
            observedLocatorJson = observedLocatorJson,
            targetLocatorJson = targetLocatorJson
        )
    }
    if (!targetHref.isNullOrBlank()) {
        val observedHref = locatorHref(observedLocatorJson) ?: return false
        return readerResourceHref(observedHref) == readerResourceHref(targetHref)
    }
    return true
}

internal fun readerLocatorMatchesTarget(
    observedLocatorJson: String?,
    targetLocatorJson: String?
): Boolean {
    val observedRaw = observedLocatorJson?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val targetRaw = targetLocatorJson?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    if (observedRaw == targetRaw) return true

    val observed = runCatching { JSONObject(observedRaw) }.getOrNull() ?: return false
    val target = runCatching { JSONObject(targetRaw) }.getOrNull() ?: return false

    val targetHref = target.optString("href").trim().takeIf { it.isNotEmpty() }
    val observedHref = observed.optString("href").trim().takeIf { it.isNotEmpty() }
    if (targetHref != null) {
        if (observedHref == null) return false
        if (readerResourceHref(observedHref) != readerResourceHref(targetHref)) return false
    }

    val observedLocations = observed.optJSONObject("locations")
    val targetLocations = target.optJSONObject("locations")

    val targetPosition = targetLocations?.numberOrNull("position")?.toInt()
    val observedPosition = observedLocations?.numberOrNull("position")?.toInt()
    if (targetPosition != null && observedPosition != null) {
        return targetPosition == observedPosition
    }

    val targetCss = targetLocations
        ?.optString("cssSelector")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val observedCss = observedLocations
        ?.optString("cssSelector")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (targetCss != null && observedCss != null) {
        return targetCss == observedCss
    }

    val targetProgression = targetLocations?.numberOrNull("progression")?.toDouble()
    val observedProgression = observedLocations?.numberOrNull("progression")?.toDouble()
    if (targetProgression != null && observedProgression != null) {
        return abs(targetProgression - observedProgression) <= LOCATOR_PROGRESSION_TOLERANCE
    }

    val targetTotalProgression = targetLocations?.numberOrNull("totalProgression")?.toDouble()
    val observedTotalProgression = observedLocations?.numberOrNull("totalProgression")?.toDouble()
    if (targetTotalProgression != null && observedTotalProgression != null) {
        return abs(targetTotalProgression - observedTotalProgression) <=
            LOCATOR_PROGRESSION_TOLERANCE
    }

    val targetHasLocationDiscriminator =
        targetPosition != null ||
            targetCss != null ||
            targetProgression != null ||
            targetTotalProgression != null
    return !targetHasLocationDiscriminator
}

private fun locatorHref(locatorJson: String?): String? {
    val raw = locatorJson?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching { JSONObject(raw) }
        .getOrNull()
        ?.optString("href")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

private fun readerResourceHref(href: String): String =
    href.trim().substringBefore('#')

private fun JSONObject.numberOrNull(name: String): Number? =
    if (has(name) && !isNull(name)) opt(name) as? Number else null

private const val LOCATOR_PROGRESSION_TOLERANCE = 0.0025

internal class ReaderNavigationTransactionGate(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    private var nextToken = 0L
    private var active: ReaderNavigationTransaction? = null

    @Synchronized
    fun begin(
        originLocatorJson: String?,
        nowElapsedMs: Long,
        targetLocatorJson: String? = null,
        targetHref: String? = null
    ): ReaderNavigationTransaction {
        val transaction = ReaderNavigationTransaction(
            token = ++nextToken,
            originLocatorJson = originLocatorJson,
            targetLocatorJson = targetLocatorJson,
            targetHref = targetHref,
            startedAtElapsedMs = nowElapsedMs
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
        observedLocatorJson: String?,
        nowElapsedMs: Long
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
                observedLocatorJson = observedLocatorJson,
                targetLocatorJson = transaction.targetLocatorJson,
                targetHref = transaction.targetHref
            )
        ) {
            return null
        }

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
