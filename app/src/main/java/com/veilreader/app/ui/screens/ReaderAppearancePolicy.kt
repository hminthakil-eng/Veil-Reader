package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderReadingMode
import com.veilreader.app.domain.ReaderNavigationMode

/**
 * Pure Reader appearance/capability policy.
 *
 * This file deliberately owns no Compose state, navigator, persistence or gesture
 * behavior. It can be reasoned about and unit-tested independently of ReaderScreen.
 */
@Suppress("UNUSED_PARAMETER")
internal fun applyMaterialPageRolloutToAppearance(
    appearance: ReaderAppearance,
    format: BookFormat,
    debugReview: Boolean,
    materialPageEnabled: Boolean
): ReaderAppearance =
    when {
        // Engine availability is not mode ownership. Debug review enters Paper
        // explicitly from Settings once; subsequent Slide/Paged/Scroll choices
        // must remain user-owned instead of being rewritten on every recomposition.
        format == BookFormat.EPUB &&
            appearance.pageTurnStyle == PageTurnStyle.PAPER &&
            !materialPageEnabled ->
            appearance
                .withReadingMode(ReaderReadingMode.PAGED)
                .withPageTurnStyle(PageTurnStyle.NONE)

        else -> appearance
    }

internal fun shouldMountPaperCurlRuntime(
    format: BookFormat,
    fixedLayout: Boolean,
    appearance: ReaderAppearance,
    materialPageEnabled: Boolean
): Boolean =
    materialPageEnabled &&
        format == BookFormat.EPUB &&
        !fixedLayout &&
        appearance.navigationMode == ReaderNavigationMode.PAPER_CURL

internal fun effectiveReaderAppearanceForPublication(
    appearance: ReaderAppearance,
    fixedLayout: Boolean
): ReaderAppearance =
    if (fixedLayout) {
        // The current Veil Paper/Slide engines own one captured reflowable sheet.
        // A fixed-layout navigator may present a dual spread, so treating its full
        // publicationView as one physical leaf produces incorrect geometry and
        // page ownership. Keep fixed-layout on deterministic paged navigation until
        // a spread-aware per-leaf capture contract exists.
        appearance
            .withReadingMode(ReaderReadingMode.PAGED)
            .withPageTurnStyle(PageTurnStyle.NONE)
    } else {
        appearance
    }

internal data class ReaderAppearanceCapabilities(
    val fixedLayout: Boolean,
    val rtlPublication: Boolean,
    val cjkPublication: Boolean,
    val continuousScroll: Boolean
) {
    val typographyEditable: Boolean
        get() = !fixedLayout

    val continuousScrollEditable: Boolean
        get() = !fixedLayout

    val columnsEditable: Boolean
        get() = !fixedLayout && !continuousScroll

    val textAlignmentEditable: Boolean
        get() = !fixedLayout && !cjkPublication

    val paragraphIndentEditable: Boolean
        get() = !fixedLayout && !cjkPublication

    val hyphenationEditable: Boolean
        get() = !fixedLayout && !rtlPublication && !cjkPublication

    val letterSpacingEditable: Boolean
        get() = !fixedLayout && !rtlPublication && !cjkPublication

    val wordSpacingEditable: Boolean
        get() = !fixedLayout && !rtlPublication && !cjkPublication

    val ligaturesEditable: Boolean
        get() = !fixedLayout && rtlPublication
}

internal fun readerAppearanceCapabilities(
    fixedLayout: Boolean,
    languageTag: String?,
    continuousScroll: Boolean
): ReaderAppearanceCapabilities =
    ReaderAppearanceCapabilities(
        fixedLayout = fixedLayout,
        rtlPublication = usesRtlReaderTypography(languageTag),
        cjkPublication = usesCjkReaderTypography(languageTag),
        continuousScroll = continuousScroll
    )

internal fun usesRtlReaderTypography(languageTag: String?): Boolean {
    val primary = languageTag
        ?.trim()
        ?.substringBefore('-')
        ?.substringBefore('_')
        ?.lowercase(java.util.Locale.ROOT)
        .orEmpty()
    return primary in setOf(
        "ar", "fa", "ur", "ps", "ckb", "he", "iw", "yi", "dv", "sd"
    )
}

internal fun usesCjkReaderTypography(languageTag: String?): Boolean {
    val primary = languageTag
        ?.trim()
        ?.substringBefore('-')
        ?.substringBefore('_')
        ?.lowercase(java.util.Locale.ROOT)
        .orEmpty()
    return primary in setOf("zh", "ja", "ko")
}


internal enum class ReaderTransitionUnavailableReason {
    PAPER_ROLLOUT_DISABLED,
    FIXED_LAYOUT_LEAF_UNSUPPORTED
}

/** Explains a capability remap without changing the user's requested preference. */
internal fun readerTransitionUnavailableReason(
    appearance: ReaderAppearance,
    format: BookFormat,
    fixedLayout: Boolean,
    materialPageEnabled: Boolean
): ReaderTransitionUnavailableReason? {
    if (format != BookFormat.EPUB) return null
    val mode = appearance.navigationMode
    if (fixedLayout && mode in setOf(
            ReaderNavigationMode.PAPER_CURL,
            ReaderNavigationMode.SLIDE,
            ReaderNavigationMode.SCROLL
        )
    ) return ReaderTransitionUnavailableReason.FIXED_LAYOUT_LEAF_UNSUPPORTED
    if (!materialPageEnabled && mode == ReaderNavigationMode.PAPER_CURL) {
        return ReaderTransitionUnavailableReason.PAPER_ROLLOUT_DISABLED
    }
    return null
}
