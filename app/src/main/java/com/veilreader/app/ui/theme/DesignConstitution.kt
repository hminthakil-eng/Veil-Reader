package com.veilreader.app.ui.theme

import com.veilreader.app.domain.ReaderNavigationMode

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
            galleryMinCellDp = 112f,
            horizontalPaddingDp = 12f,
            shelfItemWidthDp = 118f,
            shelfCoverWidthDp = 108f,
            shelfCoverHeightDp = 158f,
            showIndexMemorySummary = false
        )
        VeilAdaptiveClass.WIDE -> VeilArchiveLayoutPolicy(
            galleryMinCellDp = 132f,
            horizontalPaddingDp = 20f,
            shelfItemWidthDp = 136f,
            shelfCoverWidthDp = 124f,
            shelfCoverHeightDp = 182f,
            showIndexMemorySummary = true
        )
        VeilAdaptiveClass.LARGE -> VeilArchiveLayoutPolicy(
            galleryMinCellDp = 152f,
            horizontalPaddingDp = 28f,
            shelfItemWidthDp = 150f,
            shelfCoverWidthDp = 138f,
            shelfCoverHeightDp = 202f,
            showIndexMemorySummary = true
        )
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
            heroCoverWidthDp = 96f,
            heroCoverHeightDp = 142f,
            recentItemWidthDp = 118f,
            recentCoverWidthDp = 108f,
            recentCoverHeightDp = 158f
        )
        VeilAdaptiveClass.WIDE -> VeilThresholdLayoutPolicy(
            contentMaxWidthDp = 920f,
            horizontalPaddingDp = 20f,
            headerHeightDp = 292f,
            heroCoverWidthDp = 118f,
            heroCoverHeightDp = 174f,
            recentItemWidthDp = 132f,
            recentCoverWidthDp = 120f,
            recentCoverHeightDp = 176f
        )
        VeilAdaptiveClass.LARGE -> VeilThresholdLayoutPolicy(
            contentMaxWidthDp = 980f,
            horizontalPaddingDp = 28f,
            headerHeightDp = 316f,
            heroCoverWidthDp = 132f,
            heroCoverHeightDp = 194f,
            recentItemWidthDp = 146f,
            recentCoverWidthDp = 132f,
            recentCoverHeightDp = 194f
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
 * Paged modes are physical sheets; continuous scroll is a paper field.
 * This prevents the scroll surface from inheriting book-block edges that imply a page turn.
 */
fun sanctuaryPageMaterialFor(mode: ReaderNavigationMode): VeilSanctuaryPageMaterial =
    VeilSanctuaryPageMaterial(
        showPhysicalPageStack = mode != ReaderNavigationMode.SCROLL,
        showEdgeFalloff = true,
        showMicroFibres = true
    )
