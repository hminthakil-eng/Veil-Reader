package com.veilreader.app.manga.reader

enum class MangaReaderMode { PAGED, WEBTOON }

enum class MangaReadingDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }

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