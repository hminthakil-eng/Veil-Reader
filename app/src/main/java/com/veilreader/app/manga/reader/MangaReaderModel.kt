package com.veilreader.app.manga.reader

enum class MangaReaderLayout {
    PAGED,
    VERTICAL_PAGER,
    WEBTOON,
    CONTINUOUS_VERTICAL
}

enum class MangaReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    VERTICAL
}

enum class MangaImageFit {
    SCREEN,
    WIDTH,
    HEIGHT,
    ORIGINAL
}

enum class MangaPageLayout {
    SINGLE,
    DUAL,
    AUTO_DUAL
}

enum class MangaReaderBackground {
    BLACK,
    DARK_GRAY,
    WHITE,
    SEPIA
}

data class MangaReaderPreferences(
    val layout: MangaReaderLayout = MangaReaderLayout.PAGED,
    val direction: MangaReadingDirection = MangaReadingDirection.RIGHT_TO_LEFT,
    val imageFit: MangaImageFit = MangaImageFit.SCREEN,
    val pageLayout: MangaPageLayout = MangaPageLayout.SINGLE,
    val cropBorders: Boolean = false,
    val pageGapDp: Int = 0,
    val dataSaver: Boolean = false,
    val background: MangaReaderBackground = MangaReaderBackground.BLACK
) {
    init {
        require(pageGapDp in 0..64) { "Manga page gap must be between 0 and 64 dp." }
    }
}

/**
 * Compatibility enum for the first prototype screen.
 * New state code uses [MangaReaderLayout]; the screen migrates after renderer benchmarking.
 */
@Deprecated("Use MangaReaderLayout in new reader state code.")
enum class MangaReaderMode {
    PAGED,
    WEBTOON
}

data class MangaChapterWindow(
    val currentChapterId: String,
    val previousChapterId: String?,
    val nextChapterId: String?
)

enum class MangaChapterTransitionDirection {
    PREVIOUS,
    NEXT
}

data class MangaChapterTransition(
    val direction: MangaChapterTransitionDirection,
    val targetChapterId: String
)

object MangaPrefetchWindow {
    fun indices(
        center: Int,
        total: Int,
        radius: Int = 2
    ): List<Int> {
        if (total <= 0) return emptyList()
        require(radius >= 0) { "Prefetch radius cannot be negative." }
        val boundedCenter = center.coerceIn(0, total - 1)
        val start = (boundedCenter - radius).coerceAtLeast(0)
        val end = (boundedCenter + radius).coerceAtMost(total - 1)
        return (start..end).toList()
    }
}

internal fun shouldPreloadNextChapter(
    pageIndex: Int,
    totalPages: Int,
    threshold: Int = 3
): Boolean {
    if (totalPages <= 0) return false
    require(threshold >= 1) { "Chapter preload threshold must be positive." }
    val safePage = pageIndex.coerceIn(0, totalPages - 1)
    return safePage >= (totalPages - threshold).coerceAtLeast(0)
}
