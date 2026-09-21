package com.veilreader.app.manga.reader.image

data class MangaImageDimensions(
    val widthPx: Int,
    val heightPx: Int
) {
    init {
        require(widthPx > 0)
        require(heightPx > 0)
    }

    val pixelCount: Long get() = widthPx.toLong() * heightPx.toLong()
    val longEdgePx: Int get() = maxOf(widthPx, heightPx)
    val aspectRatio: Double
        get() = maxOf(widthPx, heightPx).toDouble() / minOf(widthPx, heightPx).toDouble()
}

enum class MangaImageDeliveryStrategy {
    STANDARD_COIL,
    LOCAL_SUBSAMPLING,
    REMOTE_BOUNDED_PREVIEW
}

data class MangaImageStrategyDecision(
    val strategy: MangaImageDeliveryStrategy,
    val extreme: Boolean,
    val reasons: Set<String>
)

class MangaExtremeImageStrategy(
    private val maxStandardPixels: Long = 24_000_000L,
    private val longEdgeThresholdPx: Int = 12_000,
    private val extremeAspectRatio: Double = 8.0,
    private val minimumTallHeightPx: Int = 8_192
) {
    init {
        require(maxStandardPixels > 0)
        require(longEdgeThresholdPx > 0)
        require(extremeAspectRatio > 1.0)
        require(minimumTallHeightPx > 0)
    }

    fun decide(
        page: MangaResolvedPage,
        dimensions: MangaImageDimensions?
    ): MangaImageStrategyDecision {
        if (dimensions == null) {
            return MangaImageStrategyDecision(
                strategy = if (page is MangaResolvedPage.Remote) {
                    MangaImageDeliveryStrategy.REMOTE_BOUNDED_PREVIEW
                } else {
                    MangaImageDeliveryStrategy.STANDARD_COIL
                },
                extreme = false,
                reasons = setOf("dimensions-unknown")
            )
        }

        val reasons = linkedSetOf<String>()
        if (dimensions.pixelCount >= maxStandardPixels) {
            reasons += "pixel-count"
        }
        if (dimensions.longEdgePx >= longEdgeThresholdPx) {
            reasons += "long-edge"
        }
        if (
            dimensions.heightPx >= minimumTallHeightPx &&
            dimensions.heightPx.toDouble() / dimensions.widthPx.toDouble() >= extremeAspectRatio
        ) {
            reasons += "extreme-tall-aspect"
        }

        val extreme = reasons.isNotEmpty()
        val strategy = when {
            !extreme -> MangaImageDeliveryStrategy.STANDARD_COIL
            page is MangaResolvedPage.Local -> MangaImageDeliveryStrategy.LOCAL_SUBSAMPLING
            else -> MangaImageDeliveryStrategy.REMOTE_BOUNDED_PREVIEW
        }

        return MangaImageStrategyDecision(
            strategy = strategy,
            extreme = extreme,
            reasons = reasons
        )
    }
}
