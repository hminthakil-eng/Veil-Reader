package com.veilreader.app.manga.source

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Boundary for browser/anti-bot verification.
 *
 * The implementation may use WebView or another user-visible challenge mechanism later. Keeping it
 * outside source adapters prevents challenge lifecycle from leaking into parsing code.
 */
interface SourceChallengeAdapter {
    suspend fun resolve(
        source: MangaSourceDescriptor,
        domain: String,
        failure: SourceFailure
    ): Boolean
}

data class SourceExecutionResult<T>(
    val outcome: SourceOutcome<T>,
    val finalDomain: String,
    val attempts: Int,
    /** Action a higher-level coordinator may take after this source stops retrying. */
    val recommendedAction: SourceRecoveryAction? = null
)

/**
 * Executes one provider operation with bounded retry, mirror failover and per-source concurrency.
 *
 * It deliberately does not choose a different source. Cross-source alternatives need canonical work
 * matching and migration confidence, so that decision belongs to a later Alternatives coordinator.
 */
class SourceExecutionCoordinator(
    private val recoveryPolicy: SourceRecoveryPolicy = SourceRecoveryPolicy(),
    private val challengeAdapter: SourceChallengeAdapter? = null,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val maxParallelPerSource: Int = DEFAULT_MAX_PARALLEL_PER_SOURCE,
    private val defaultRetryDelayMillis: Long = DEFAULT_RETRY_DELAY_MILLIS,
    private val maxRetryDelayMillis: Long = DEFAULT_MAX_RETRY_DELAY_MILLIS,
    private val sleeper: suspend (Long) -> Unit = { delay(it) }
) {

    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(maxParallelPerSource > 0) { "maxParallelPerSource must be positive" }
        require(defaultRetryDelayMillis >= 0) { "defaultRetryDelayMillis cannot be negative" }
        require(maxRetryDelayMillis >= defaultRetryDelayMillis) {
            "maxRetryDelayMillis must be >= defaultRetryDelayMillis"
        }
    }

    private val sourceSemaphores = ConcurrentHashMap<SourceId, Semaphore>()

    suspend fun search(
        provider: MangaSourceProvider,
        request: SourceSearchRequest
    ): SourceExecutionResult<PagedSourceResult<SourceMangaSummary>> =
        execute(provider) { context -> provider.search(request, context) }

    suspend fun details(
        provider: MangaSourceProvider,
        manga: SourceMangaRef
    ): SourceExecutionResult<SourceMangaDetails> =
        execute(provider) { context -> provider.details(manga, context) }

    suspend fun chapters(
        provider: MangaSourceProvider,
        manga: SourceMangaRef
    ): SourceExecutionResult<List<SourceChapter>> =
        execute(provider) { context -> provider.chapters(manga, context) }

    suspend fun pages(
        provider: MangaSourceProvider,
        chapter: SourceChapter
    ): SourceExecutionResult<List<MangaPageImage>> =
        execute(provider) { context -> provider.pages(chapter, context) }

    suspend fun resolvePublicUrl(
        provider: MangaSourceProvider,
        url: String
    ): SourceExecutionResult<SourceMangaRef?> =
        execute(provider) { context -> provider.resolvePublicUrl(url, context) }

    private suspend fun <T> execute(
        provider: MangaSourceProvider,
        operation: suspend (SourceRequestContext) -> SourceOutcome<T>
    ): SourceExecutionResult<T> {
        val semaphore = sourceSemaphores.computeIfAbsent(provider.descriptor.id) {
            Semaphore(maxParallelPerSource)
        }
        return semaphore.withPermit {
            executeWithPermit(provider, operation)
        }
    }

    private suspend fun <T> executeWithPermit(
        provider: MangaSourceProvider,
        operation: suspend (SourceRequestContext) -> SourceOutcome<T>
    ): SourceExecutionResult<T> {
        val domains = provider.descriptor.domains
        var domainIndex = 0
        var lastFailure = SourceFailure(
            kind = SourceFailureKind.UNKNOWN,
            message = "Source execution ended without an attempt"
        )
        var lastAction: SourceRecoveryAction? = null
        val challengedDomains = mutableSetOf<String>()

        for (attempt in 1..maxAttempts) {
            val domain = domains[domainIndex]
            val outcome = try {
                operation(SourceRequestContext(domain = domain, attempt = attempt))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                SourceOutcome.Failure(
                    SourceFailure(
                        kind = SourceFailureKind.UNKNOWN,
                        message = error.message ?: error::class.java.simpleName,
                        cause = error
                    )
                )
            }

            when (outcome) {
                is SourceOutcome.Success -> return SourceExecutionResult(
                    outcome = outcome,
                    finalDomain = domain,
                    attempts = attempt
                )

                is SourceOutcome.Failure -> {
                    lastFailure = outcome.error
                    val hasAlternateDomain = domainIndex < domains.lastIndex
                    val action = recoveryPolicy.decide(
                        failure = outcome.error,
                        hasAlternateDomain = hasAlternateDomain,
                        hasAlternativeSource = false
                    )
                    lastAction = action

                    if (attempt == maxAttempts) {
                        return failed(
                            failure = lastFailure,
                            domain = domain,
                            attempts = attempt,
                            action = action
                        )
                    }

                    when (action) {
                        SourceRecoveryAction.RETRY_SAME_SOURCE -> Unit

                        SourceRecoveryAction.WAIT_AND_RETRY -> {
                            val requested = outcome.error.retryAfterMillis
                                ?: defaultRetryDelayMillis
                            sleeper(requested.coerceIn(0, maxRetryDelayMillis))
                        }

                        SourceRecoveryAction.TRY_ALTERNATE_DOMAIN -> {
                            if (hasAlternateDomain) {
                                domainIndex += 1
                            } else {
                                return failed(lastFailure, domain, attempt, action)
                            }
                        }

                        SourceRecoveryAction.REQUIRE_BROWSER_CHALLENGE -> {
                            val adapter = challengeAdapter
                                ?: return failed(lastFailure, domain, attempt, action)
                            if (!challengedDomains.add(domain)) {
                                // A solved challenge that immediately repeats is ineffective. Never
                                // loop the verification UI for the same domain in one operation.
                                return failed(lastFailure, domain, attempt, action)
                            }
                            val resolved = try {
                                adapter.resolve(provider.descriptor, domain, outcome.error)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Throwable) {
                                return failed(
                                    failure = SourceFailure(
                                        kind = SourceFailureKind.UNKNOWN,
                                        message = error.message ?: "Challenge resolution failed",
                                        cause = error
                                    ),
                                    domain = domain,
                                    attempts = attempt,
                                    action = SourceRecoveryAction.STOP
                                )
                            }
                            if (!resolved) {
                                return failed(lastFailure, domain, attempt, action)
                            }
                        }

                        SourceRecoveryAction.REQUIRE_AUTH,
                        SourceRecoveryAction.TRY_ALTERNATIVE_SOURCE,
                        SourceRecoveryAction.STOP -> return failed(
                            lastFailure,
                            domain,
                            attempt,
                            action
                        )
                    }
                }
            }
        }

        return failed(
            failure = lastFailure,
            domain = domains[domainIndex],
            attempts = maxAttempts,
            action = lastAction ?: SourceRecoveryAction.STOP
        )
    }

    private fun <T> failed(
        failure: SourceFailure,
        domain: String,
        attempts: Int,
        action: SourceRecoveryAction
    ): SourceExecutionResult<T> = SourceExecutionResult(
        outcome = SourceOutcome.Failure(failure),
        finalDomain = domain,
        attempts = attempts,
        recommendedAction = action
    )

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val DEFAULT_MAX_PARALLEL_PER_SOURCE = 2
        const val DEFAULT_RETRY_DELAY_MILLIS = 1_000L
        const val DEFAULT_MAX_RETRY_DELAY_MILLIS = 30_000L
    }
}
