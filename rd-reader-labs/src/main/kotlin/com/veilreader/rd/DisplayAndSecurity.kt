package com.veilreader.rd

enum class EInkRefreshPolicy { NORMAL, REDUCED_MOTION, PAGE_SETTLE }
data class EInkProfile(
    val animationsEnabled: Boolean = false,
    val grayscaleOnly: Boolean = true,
    val contrastBoost: Double = 1.0,
    val refreshPolicy: EInkRefreshPolicy = EInkRefreshPolicy.PAGE_SETTLE
) {
    fun normalized(): EInkProfile =
        copy(contrastBoost = contrastBoost.takeIf { it.isFinite() }?.coerceIn(0.8, 2.0) ?: 1.0)
}

enum class AppLockMethod { NONE, DEVICE_CREDENTIAL, BIOMETRIC, APP_PIN }
data class AppLockPolicy(
    val method: AppLockMethod,
    val lockOnBackground: Boolean = false,
    val gracePeriodSeconds: Int = 0
) {
    fun normalized(): AppLockPolicy = copy(gracePeriodSeconds = gracePeriodSeconds.coerceIn(0, 3600))
}

data class HomeSurfaceShortcut(
    val bookId: String,
    val title: String,
    val locator: String? = null
)
