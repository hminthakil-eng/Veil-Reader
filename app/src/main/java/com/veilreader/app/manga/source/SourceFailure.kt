package com.veilreader.app.manga.source

/**
 * Failures are classified so recovery can be deterministic instead of being scattered through UI.
 *
 * Coroutine cancellation must be rethrown by adapters and must never be converted to UNKNOWN.
 */
enum class SourceFailureKind {
    NETWORK,
    TIMEOUT,
    RATE_LIMITED,
    BLOCKED,
    CHALLENGE_REQUIRED,
    AUTH_REQUIRED,
    NOT_FOUND,
    PARSE_CHANGED,
    SOURCE_REMOVED,
    UNSUPPORTED,
    UNKNOWN
}

data class SourceFailure(
    val kind: SourceFailureKind,
    val message: String,
    val retryAfterMillis: Long? = null,
    val cause: Throwable? = null
) {
    companion object {
        fun unsupported(capability: MangaSourceCapability): SourceFailure = SourceFailure(
            kind = SourceFailureKind.UNSUPPORTED,
            message = "Source does not support $capability"
        )
    }
}

sealed interface SourceOutcome<out T> {
    data class Success<T>(val value: T) : SourceOutcome<T>
    data class Failure(val error: SourceFailure) : SourceOutcome<Nothing>
}

enum class SourceRecoveryAction {
    RETRY_SAME_SOURCE,
    WAIT_AND_RETRY,
    TRY_ALTERNATE_DOMAIN,
    REQUIRE_BROWSER_CHALLENGE,
    REQUIRE_AUTH,
    TRY_ALTERNATIVE_SOURCE,
    STOP
}

/**
 * One-step recovery decision. Retry budgets/cooldowns are owned by the execution layer so this
 * policy cannot accidentally create an infinite retry loop.
 */
class SourceRecoveryPolicy {

    fun decide(
        failure: SourceFailure,
        hasAlternateDomain: Boolean,
        hasAlternativeSource: Boolean
    ): SourceRecoveryAction = when (failure.kind) {
        SourceFailureKind.NETWORK,
        SourceFailureKind.TIMEOUT -> if (hasAlternateDomain) {
            SourceRecoveryAction.TRY_ALTERNATE_DOMAIN
        } else {
            SourceRecoveryAction.RETRY_SAME_SOURCE
        }

        SourceFailureKind.RATE_LIMITED -> SourceRecoveryAction.WAIT_AND_RETRY

        SourceFailureKind.BLOCKED -> if (hasAlternateDomain) {
            SourceRecoveryAction.TRY_ALTERNATE_DOMAIN
        } else {
            SourceRecoveryAction.REQUIRE_BROWSER_CHALLENGE
        }

        SourceFailureKind.CHALLENGE_REQUIRED -> SourceRecoveryAction.REQUIRE_BROWSER_CHALLENGE
        SourceFailureKind.AUTH_REQUIRED -> SourceRecoveryAction.REQUIRE_AUTH

        SourceFailureKind.NOT_FOUND,
        SourceFailureKind.PARSE_CHANGED,
        SourceFailureKind.SOURCE_REMOVED,
        SourceFailureKind.UNKNOWN -> if (hasAlternativeSource) {
            SourceRecoveryAction.TRY_ALTERNATIVE_SOURCE
        } else {
            SourceRecoveryAction.STOP
        }

        SourceFailureKind.UNSUPPORTED -> SourceRecoveryAction.STOP
    }
}
