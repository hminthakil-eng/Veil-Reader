package com.veilreader.app.ui.theme

import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderTheme

/**
 * Executable product rules for Grayfog.
 *
 * This file is deliberately free of Composable state. It defines the invariants screens consume:
 * materials communicate meaning, motion communicates intent, and atmosphere may degrade before
 * functionality. Reader/Sanctuary is the strictest surface in the product.
 */
enum class VeilMaterialRole {
    OBSIDIAN,
    ARCHIVE_IRON,
    AGED_BRASS,
    PAPER,
    MIST
}

enum class VeilQualityTier {
    FULL,
    BALANCED,
    ESSENTIAL
}

data class VeilQualityPolicy(
    val atmosphereMultiplier: Float,
    val ornamentalDetail: Float,
    val ambientMotionEnabled: Boolean,
    val expensiveBlurAllowed: Boolean
)

fun qualityPolicyFor(tier: VeilQualityTier): VeilQualityPolicy =
    when (tier) {
        VeilQualityTier.FULL -> VeilQualityPolicy(
            atmosphereMultiplier = 1.0f,
            ornamentalDetail = 1.0f,
            ambientMotionEnabled = true,
            expensiveBlurAllowed = true
        )
        VeilQualityTier.BALANCED -> VeilQualityPolicy(
            atmosphereMultiplier = 0.72f,
            ornamentalDetail = 0.72f,
            ambientMotionEnabled = true,
            expensiveBlurAllowed = false
        )
        VeilQualityTier.ESSENTIAL -> VeilQualityPolicy(
            atmosphereMultiplier = 0.34f,
            ornamentalDetail = 0.42f,
            ambientMotionEnabled = false,
            expensiveBlurAllowed = false
        )
    }

enum class VeilAdaptiveClass {
    COMPACT,
    WIDE,
    LARGE
}

/**
 * Layout classes are compositional, not scale factors.
 * Compact = phone portrait, Wide = landscape/foldable, Large = tablet/desktop-like.
 */
fun adaptiveClassFor(widthDp: Float): VeilAdaptiveClass =
    when {
        !widthDp.isFinite() || widthDp < 600f -> VeilAdaptiveClass.COMPACT
        widthDp < 840f -> VeilAdaptiveClass.WIDE
        else -> VeilAdaptiveClass.LARGE
    }

data class VeilArchiveLayoutPolicy(
    val galleryMinCellDp: Float,
    val horizontalPaddingDp: Float,
    val shelfItemWidthDp: Float,
    val shelfCoverWidthDp: Float,
    val shelfCoverHeightDp: Float,
    val showIndexMemorySummary: Boolean
)

fun archiveLayoutPolicyFor(
    adaptiveClass: VeilAdaptiveClass
): VeilArchiveLayoutPolicy =
    when (adaptiveClass) {
        VeilAdaptiveClass.COMPACT -> VeilArchiveLayoutPolicy(
            galleryMinCellDp = 140f,
            horizontalPaddingDp = 12f,
            shelfItemWidthDp = 118f,
            shelfCoverWidthDp = 108f,
            shelfCoverHeightDp = 158f,
            showIndexMemorySummary = false
        )
        VeilAdaptiveClass.WIDE -> VeilArchiveLayoutPolicy(
            galleryMinCellDp = 160f,
            horizontalPaddingDp = 20f,
            shelfItemWidthDp = 136f,
            shelfCoverWidthDp = 124f,
            shelfCoverHeightDp = 182f,
            showIndexMemorySummary = true
        )
        VeilAdaptiveClass.LARGE -> VeilArchiveLayoutPolicy(
            galleryMinCellDp = 184f,
            horizontalPaddingDp = 28f,
            shelfItemWidthDp = 150f,
            shelfCoverWidthDp = 138f,
            shelfCoverHeightDp = 202f,
            showIndexMemorySummary = true
        )
    }

/** Accessibility grows the record measure, not the artwork itself. */
fun galleryCellMeasureDp(baseMeasureDp: Float, fontScale: Float): Float {
    val scale = if (fontScale.isFinite()) fontScale.coerceAtLeast(1f) else 1f
    return baseMeasureDp * scale.coerceAtMost(2f)
}

data class VeilThresholdLayoutPolicy(
    val contentMaxWidthDp: Float,
    val horizontalPaddingDp: Float,
    val headerHeightDp: Float,
    val heroCoverWidthDp: Float,
    val heroCoverHeightDp: Float,
    val recentItemWidthDp: Float,
    val recentCoverWidthDp: Float,
    val recentCoverHeightDp: Float
)

fun thresholdLayoutPolicyFor(
    adaptiveClass: VeilAdaptiveClass
): VeilThresholdLayoutPolicy =
    when (adaptiveClass) {
        VeilAdaptiveClass.COMPACT -> VeilThresholdLayoutPolicy(
            contentMaxWidthDp = 860f,
            horizontalPaddingDp = 16f,
            headerHeightDp = 252f,
            heroCoverWidthDp = 118f,
            heroCoverHeightDp = 174f,
            recentItemWidthDp = 118f,
            recentCoverWidthDp = 96f,
            recentCoverHeightDp = 142f
        )
        VeilAdaptiveClass.WIDE -> VeilThresholdLayoutPolicy(
            contentMaxWidthDp = 920f,
            horizontalPaddingDp = 20f,
            headerHeightDp = 292f,
            heroCoverWidthDp = 132f,
            heroCoverHeightDp = 194f,
            recentItemWidthDp = 132f,
            recentCoverWidthDp = 108f,
            recentCoverHeightDp = 158f
        )
        VeilAdaptiveClass.LARGE -> VeilThresholdLayoutPolicy(
            contentMaxWidthDp = 980f,
            horizontalPaddingDp = 28f,
            headerHeightDp = 316f,
            heroCoverWidthDp = 146f,
            heroCoverHeightDp = 216f,
            recentItemWidthDp = 146f,
            recentCoverWidthDp = 118f,
            recentCoverHeightDp = 174f
        )
    }

/**
 * Threshold wakes with the library but saturates early; more books must not turn Home into Castle.
 */
fun thresholdAtmosphereIntensityFor(bookCount: Int): Float {
    val count = bookCount.coerceAtLeast(0)
    if (count == 0) return 0.72f
    if (count == 1) return 0.84f
    val normalized = (count.coerceAtMost(12) - 1) / 11f
    return (0.84f + normalized * 0.16f)
        .coerceIn(0.84f, 1f)
}

data class VeilCastleLayoutPolicy(
    val contentMaxWidthDp: Float,
    val horizontalPaddingDp: Float,
    val keepMinHeightDp: Float,
    val mapHorizontalPaddingDp: Float,
    val chamberMinHeightDp: Float,
    val observatoryHeightDp: Float
)

fun castleLayoutPolicyFor(
    adaptiveClass: VeilAdaptiveClass
): VeilCastleLayoutPolicy =
    when (adaptiveClass) {
        VeilAdaptiveClass.COMPACT -> VeilCastleLayoutPolicy(
            contentMaxWidthDp = 860f,
            horizontalPaddingDp = 16f,
            keepMinHeightDp = 220f,
            mapHorizontalPaddingDp = 10f,
            chamberMinHeightDp = 116f,
            observatoryHeightDp = 330f
        )
        VeilAdaptiveClass.WIDE -> VeilCastleLayoutPolicy(
            contentMaxWidthDp = 980f,
            horizontalPaddingDp = 22f,
            keepMinHeightDp = 260f,
            mapHorizontalPaddingDp = 18f,
            chamberMinHeightDp = 128f,
            observatoryHeightDp = 390f
        )
        VeilAdaptiveClass.LARGE -> VeilCastleLayoutPolicy(
            contentMaxWidthDp = 1120f,
            horizontalPaddingDp = 30f,
            keepMinHeightDp = 300f,
            mapHorizontalPaddingDp = 28f,
            chamberMinHeightDp = 142f,
            observatoryHeightDp = 450f
        )
    }

enum class VeilMotionClass {
    MICRO,
    FUNCTIONAL,
    SPATIAL,
    PHYSICAL,
    CEREMONIAL,
    ATMOSPHERIC
}

data class VeilMotionPolicy(
    val fixedDurationMillis: Int?,
    val translationAllowed: Boolean,
    val ambientLoopAllowed: Boolean
)

/**
 * Reduced motion removes spatial displacement and ambient looping rather than merely shortening it.
 * Physical motion remains engine-owned; callers should switch to the functional fallback path.
 */
fun motionPolicyFor(
    motionClass: VeilMotionClass,
    reducedMotion: Boolean
): VeilMotionPolicy {
    if (reducedMotion) {
        return when (motionClass) {
            VeilMotionClass.MICRO,
            VeilMotionClass.FUNCTIONAL -> VeilMotionPolicy(
                fixedDurationMillis = VeilMotion.REDUCED_MOTION_FADE_MS,
                translationAllowed = false,
                ambientLoopAllowed = false
            )
            VeilMotionClass.SPATIAL,
            VeilMotionClass.PHYSICAL,
            VeilMotionClass.CEREMONIAL,
            VeilMotionClass.ATMOSPHERIC -> VeilMotionPolicy(
                fixedDurationMillis = VeilMotion.REDUCED_MOTION_FADE_MS,
                translationAllowed = false,
                ambientLoopAllowed = false
            )
        }
    }

    return when (motionClass) {
        VeilMotionClass.MICRO -> VeilMotionPolicy(
            fixedDurationMillis = VeilMotion.TAP_MS,
            translationAllowed = false,
            ambientLoopAllowed = false
        )
        VeilMotionClass.FUNCTIONAL -> VeilMotionPolicy(
            fixedDurationMillis = VeilMotion.FUNCTIONAL_MS,
            translationAllowed = true,
            ambientLoopAllowed = false
        )
        VeilMotionClass.SPATIAL -> VeilMotionPolicy(
            fixedDurationMillis = VeilMotion.SPATIAL_MS,
            translationAllowed = true,
            ambientLoopAllowed = false
        )
        VeilMotionClass.PHYSICAL -> VeilMotionPolicy(
            fixedDurationMillis = null,
            translationAllowed = true,
            ambientLoopAllowed = false
        )
        VeilMotionClass.CEREMONIAL -> VeilMotionPolicy(
            fixedDurationMillis = VeilMotion.RITUAL_MS,
            translationAllowed = true,
            ambientLoopAllowed = false
        )
        VeilMotionClass.ATMOSPHERIC -> VeilMotionPolicy(
            fixedDurationMillis = null,
            translationAllowed = false,
            ambientLoopAllowed = true
        )
    }
}

enum class VeilHapticLevel {
    NONE,
    CONFIRM,
    MARK,
    PHYSICAL,
    RITUAL
}

data class VeilSanctuaryContract(
    val chromeOrnamentAllowed: Boolean,
    val persistentDecorativeControlsAllowed: Boolean,
    val atmosphereIntensity: Float,
    val minimumPageStackDp: Float,
    val maximumPageStackDp: Float,
    val chromeAutoHideMillis: Long
)

/**
 * The Reader is intentionally the least decorated realm.
 * When chrome is hidden, publication + paper are the experience.
 */
val VeilSanctuary = VeilSanctuaryContract(
    chromeOrnamentAllowed = false,
    persistentDecorativeControlsAllowed = false,
    atmosphereIntensity = 0.02f,
    minimumPageStackDp = 2f,
    maximumPageStackDp = 8f,
    chromeAutoHideMillis = VeilMotion.READER_AUTO_HIDE_MS
)

data class VeilSanctuaryPageMaterial(
    val showPhysicalPageStack: Boolean,
    val showEdgeFalloff: Boolean,
    val showMicroFibres: Boolean
)

/**
 * Navigation modes must read differently before the user even moves a finger.
 *
 * PAPER_CURL and static PAGED preserve a visible book-block edge. SLIDE stays a flat moving sheet
 * so it never masquerades as a curl. SCROLL is a continuous paper field without page-stack depth.
 */
fun sanctuaryPageMaterialFor(mode: ReaderNavigationMode): VeilSanctuaryPageMaterial =
    VeilSanctuaryPageMaterial(
        showPhysicalPageStack =
            mode == ReaderNavigationMode.PAPER_CURL ||
                mode == ReaderNavigationMode.PAGED,
        showEdgeFalloff = true,
        showMicroFibres = true
    )


data class VeilSanctuarySurfaceProfile(
    val patina: Float,
    val pageShadeAlpha: Float,
    val stackEdgeAlpha: Float,
    val sheetLineAlpha: Float,
    val mottleAlpha: Float,
    val edgeOxidationAlpha: Float,
    val fibreAlpha: Float,
    val fibreCount: Int,
    val speckAlpha: Float,
    val speckCount: Int
)

/**
 * Deterministic Sanctuary material policy.
 *
 * Paper patina is an actual rendering input, not decorative preference state.
 * Dark themes deliberately collapse patina to zero so texture never competes with contrast.
 */
fun sanctuarySurfaceProfileFor(
    theme: ReaderTheme,
    paperPatina: Float
): VeilSanctuarySurfaceProfile {
    val dark = theme == ReaderTheme.DUSK || theme == ReaderTheme.OLED
    val p = if (dark || !paperPatina.isFinite()) 0f else paperPatina.coerceIn(0f, 1f)

    return VeilSanctuarySurfaceProfile(
        patina = p,
        pageShadeAlpha = if (dark) 0.075f else 0.022f + 0.050f * p,
        stackEdgeAlpha = if (dark) 0.20f else 0.055f + 0.055f * p,
        sheetLineAlpha = if (dark) 0.018f else 0.025f + 0.022f * p,
        mottleAlpha = if (dark || p <= 0.04f) 0f else 0.004f + 0.010f * p,
        edgeOxidationAlpha = if (dark || p <= 0.04f) 0f else 0.008f + 0.026f * p,
        fibreAlpha = if (dark) 0f else 0.004f + 0.012f * p,
        fibreCount = if (dark) 0 else 12 + (18f * p).toInt(),
        speckAlpha = if (dark) 0f else 0.003f + 0.010f * p,
        speckCount = if (dark) 0 else 14 + (22f * p).toInt()
    )
}

/** Two rooms need actual space after navigation/insets, not just a nominal tablet window. */
fun useArchitecturalPair(widthDp: Float, fontScale: Float): Boolean =
    widthDp.isFinite() && fontScale.isFinite() && fontScale > 0f &&
        widthDp >= VeilComposition.ArchitecturalPairMinWidthDp &&
        widthDp / fontScale.coerceAtLeast(1f) >= VeilComposition.ArchitecturalPairReadableWidthDp

/** Optional approach copy yields to instruments when text or vertical pressure grows. */
fun condenseRealmApproach(fontScale: Float, heightDp: Int): Boolean {
    val scale = if (fontScale.isFinite() && fontScale > 0f) fontScale else 1f
    return scale >= VeilComposition.ApproachCondenseFontScale ||
        heightDp in 1 until VeilComposition.ApproachShortHeightDp
}
