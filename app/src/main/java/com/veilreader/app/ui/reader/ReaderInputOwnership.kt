package com.veilreader.app.ui.reader

/**
 * Arbitration contract for competing page input surfaces.
 * This policy has no side effects: a caller must cancel the active preview before transferring
 * ownership, and must never infer a successful cancellation from a requested mode change.
 */
internal enum class ReaderInputOwner { NONE, NAVIGATOR, PAPER, SLIDE, PROGRAMMATIC }

internal data class ReaderInputOwnership(
    val owner: ReaderInputOwner = ReaderInputOwner.NONE,
    val previewPending: Boolean = false
) {
    fun canAcquire(requested: ReaderInputOwner): Boolean =
        when {
            requested == ReaderInputOwner.NONE -> false
            previewPending -> false
            owner == ReaderInputOwner.NONE -> true
            owner == requested -> true
            else -> false
        }

    fun acquire(requested: ReaderInputOwner): ReaderInputOwnership? =
        if (canAcquire(requested)) copy(owner = requested) else null

    fun beginPreview(): ReaderInputOwnership? =
        if (!previewPending && (owner == ReaderInputOwner.PAPER || owner == ReaderInputOwner.SLIDE))
            copy(previewPending = true) else null

    fun finishPreview(): ReaderInputOwnership = copy(previewPending = false)

    fun release(requested: ReaderInputOwner): ReaderInputOwnership =
        if (owner == requested && !previewPending) ReaderInputOwnership() else this
}
