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
