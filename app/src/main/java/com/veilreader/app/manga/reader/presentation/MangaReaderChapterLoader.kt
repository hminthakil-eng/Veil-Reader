package com.veilreader.app.manga.reader.presentation

import com.veilreader.app.manga.library.MangaOfflineCacheIndex
import com.veilreader.app.manga.library.OfflineChapterManifest
import com.veilreader.app.manga.source.MangaPageImage
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceOutcome
import kotlinx.coroutines.CancellationException

class MangaReaderChapterLoader(
    private val offlineIndex: MangaOfflineCacheIndex,
    private val sourceExecution: SourceExecutionCoordinator
) {

    suspend fun load(
        request: MangaChapterPresentationRequest
    ): MangaReaderPresentationState {
        val manifest = offlineIndex.load(request.offlineChapterId())
        val offline = validateOffline(manifest)

        if (offline is OfflineValidation.Complete) {
            return MangaReaderPresentationState.Ready(
                MangaReadyPresentation(
                    chapter = request.chapter,
                    pages = offline.pages,
                    availability = MangaChapterAvailability.COMPLETE_OFFLINE,
                    complete = true
                )
            )
        }

        if (!request.canUseNetwork) {
            return when (offline) {
                is OfflineValidation.Partial -> partialOffline(request, offline.pages)
                is OfflineValidation.Corrupt,
                OfflineValidation.Empty -> error(
                    request,
                    MangaPresentationErrorKind.OFFLINE_UNAVAILABLE,
                    "This chapter is not fully available offline.",
                    retryable = false
                )
                is OfflineValidation.Complete -> error(
                    request,
                    MangaPresentationErrorKind.INVALID_PAGE_SET,
                    "Unexpected offline validation state.",
                    retryable = false
                )
            }
        }

        val provider = requireNotNull(request.provider)
        val sourceChapter = requireNotNull(request.sourceChapter)
        val result = try {
            sourceExecution.pages(provider, sourceChapter)
        } catch (cancelled: CancellationException) {
            throw cancelled
        }

        return when (val outcome = result.outcome) {
            is SourceOutcome.Success -> {
                val remote = validateRemote(outcome.value)
                when (remote) {
                    RemoteValidation.Empty -> {
                        if (offline is OfflineValidation.Partial) {
                            partialOffline(request, offline.pages)
                        } else {
                            error(
                                request,
                                MangaPresentationErrorKind.EMPTY_CHAPTER,
                                "The source returned no pages for this chapter.",
                                retryable = true
                            )
                        }
                    }

                    is RemoteValidation.Invalid -> {
                        if (offline is OfflineValidation.Partial) {
                            partialOffline(request, offline.pages)
                        } else {
                            error(
                                request,
                                MangaPresentationErrorKind.INVALID_PAGE_SET,
                                remote.reason,
                                retryable = true
                            )
                        }
                    }

                    is RemoteValidation.Valid -> {
                        val localByIndex = when (offline) {
                            is OfflineValidation.Partial -> offline.pages.associateBy { it.index }
                            is OfflineValidation.Corrupt,
                            OfflineValidation.Empty,
                            is OfflineValidation.Complete -> emptyMap()
                        }
                        val merged = remote.pages.map { page ->
                            localByIndex[page.index] ?: page
                        }
                        val availability = when {
                            localByIndex.isEmpty() -> MangaChapterAvailability.COMPLETE_ONLINE
                            merged.all { it is MangaPageAsset.Local } ->
                                MangaChapterAvailability.COMPLETE_OFFLINE
                            else -> MangaChapterAvailability.HYBRID
                        }

                        MangaReaderPresentationState.Ready(
                            MangaReadyPresentation(
                                chapter = request.chapter,
                                pages = merged,
                                availability = availability,
                                complete = true
                            )
                        )
                    }
                }
            }

            is SourceOutcome.Failure -> {
                if (offline is OfflineValidation.Partial) {
                    partialOffline(request, offline.pages)
                } else {
                    error(
                        request,
                        MangaPresentationErrorKind.SOURCE_FAILURE,
                        userMessage(outcome.error.kind),
                        retryable = isRetryable(outcome.error.kind),
                        sourceFailure = outcome.error
                    )
                }
            }
        }
    }

    private fun partialOffline(
        request: MangaChapterPresentationRequest,
        pages: List<MangaPageAsset.Local>
    ): MangaReaderPresentationState.Ready =
        MangaReaderPresentationState.Ready(
            MangaReadyPresentation(
                chapter = request.chapter,
                pages = pages,
                availability = MangaChapterAvailability.PARTIAL_OFFLINE,
                complete = false
            )
        )

    private fun error(
        request: MangaChapterPresentationRequest,
        kind: MangaPresentationErrorKind,
        message: String,
        retryable: Boolean,
        sourceFailure: com.veilreader.app.manga.source.SourceFailure? = null
    ): MangaReaderPresentationState.Error =
        MangaReaderPresentationState.Error(
            chapter = request.chapter,
            error = MangaPresentationError(
                kind = kind,
                message = message,
                retryable = retryable,
                sourceFailure = sourceFailure
            )
        )

    private sealed interface OfflineValidation {
        data object Empty : OfflineValidation
        data class Complete(val pages: List<MangaPageAsset.Local>) : OfflineValidation
        data class Partial(val pages: List<MangaPageAsset.Local>) : OfflineValidation
        data class Corrupt(val reason: String) : OfflineValidation
    }

    private fun validateOffline(
        manifest: OfflineChapterManifest?
    ): OfflineValidation {
        manifest ?: return OfflineValidation.Empty
        if (manifest.pages.isEmpty()) return OfflineValidation.Empty

        val sorted = manifest.pages.sortedBy { it.index }
        if (sorted.map { it.index }.distinct().size != sorted.size) {
            return OfflineValidation.Corrupt("Offline page indices are duplicated")
        }

        val assets = sorted.map {
            MangaPageAsset.Local(
                index = it.index,
                relativePath = it.relativePath,
                byteSize = it.byteSize,
                contentSha256 = it.contentSha256
            )
        }

        if (manifest.completed) {
            val contiguous = assets.map { it.index } == assets.indices.toList()
            return if (contiguous) {
                OfflineValidation.Complete(assets)
            } else {
                OfflineValidation.Corrupt("Completed offline manifest contains page gaps")
            }
        }

        // For partial offline reopen we only expose the contiguous prefix from page zero. That keeps
        // reader itemIndex semantics stable and never shifts page 7 into UI index 0.
        val prefix = mutableListOf<MangaPageAsset.Local>()
        for ((expected, page) in assets.withIndex()) {
            if (page.index != expected) break
            prefix += page
        }
        return if (prefix.isEmpty()) {
            OfflineValidation.Corrupt("Partial offline cache does not start at page zero")
        } else {
            OfflineValidation.Partial(prefix)
        }
    }

    private sealed interface RemoteValidation {
        data object Empty : RemoteValidation
        data class Valid(val pages: List<MangaPageAsset.Remote>) : RemoteValidation
        data class Invalid(val reason: String) : RemoteValidation
    }

    private fun validateRemote(
        pages: List<MangaPageImage>
    ): RemoteValidation {
        if (pages.isEmpty()) return RemoteValidation.Empty
        val sorted = pages.sortedBy { it.index }
        if (sorted.map { it.index }.distinct().size != sorted.size) {
            return RemoteValidation.Invalid("Source returned duplicate page indices")
        }
        if (sorted.map { it.index } != sorted.indices.toList()) {
            return RemoteValidation.Invalid("Source returned non-contiguous page indices")
        }
        return RemoteValidation.Valid(
            sorted.map {
                MangaPageAsset.Remote(
                    index = it.index,
                    imageUrl = it.imageUrl,
                    requestHeaders = it.requestHeaders
                )
            }
        )
    }

    private fun isRetryable(kind: SourceFailureKind): Boolean = when (kind) {
        SourceFailureKind.NETWORK,
        SourceFailureKind.TIMEOUT,
        SourceFailureKind.RATE_LIMITED,
        SourceFailureKind.BLOCKED,
        SourceFailureKind.CHALLENGE_REQUIRED,
        SourceFailureKind.UNKNOWN -> true

        SourceFailureKind.AUTH_REQUIRED,
        SourceFailureKind.NOT_FOUND,
        SourceFailureKind.PARSE_CHANGED,
        SourceFailureKind.SOURCE_REMOVED,
        SourceFailureKind.UNSUPPORTED -> false
    }

    private fun userMessage(kind: SourceFailureKind): String = when (kind) {
        SourceFailureKind.NETWORK -> "Network is unavailable."
        SourceFailureKind.TIMEOUT -> "The source took too long to respond."
        SourceFailureKind.RATE_LIMITED -> "The source asked Veil to wait before retrying."
        SourceFailureKind.BLOCKED,
        SourceFailureKind.CHALLENGE_REQUIRED -> "The source requires verification."
        SourceFailureKind.AUTH_REQUIRED -> "This source requires authentication."
        SourceFailureKind.NOT_FOUND -> "This chapter is no longer available at the source."
        SourceFailureKind.PARSE_CHANGED -> "The source layout changed and needs an adapter update."
        SourceFailureKind.SOURCE_REMOVED -> "This source is no longer available."
        SourceFailureKind.UNSUPPORTED -> "This source cannot provide chapter pages."
        SourceFailureKind.UNKNOWN -> "The chapter could not be loaded."
    }
}
