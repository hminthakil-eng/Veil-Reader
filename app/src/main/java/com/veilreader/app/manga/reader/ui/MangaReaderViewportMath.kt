package com.veilreader.app.manga.reader.ui

object MangaReaderViewportMath {

    fun webtoonOffsetFraction(
        firstVisibleItemScrollOffsetPx: Int,
        firstVisibleItemSizePx: Int
    ): Double {
        if (firstVisibleItemSizePx <= 0) return 0.0
        return (
            firstVisibleItemScrollOffsetPx.toDouble() /
                firstVisibleItemSizePx.toDouble()
            )
            .takeIf(Double::isFinite)
            ?.coerceIn(0.0, 1.0)
            ?: 0.0
    }

    fun progression(
        itemIndex: Int,
        offsetFraction: Double,
        itemCount: Int
    ): Double {
        if (itemCount <= 0) return 0.0
        val safeIndex = itemIndex.coerceIn(0, itemCount - 1)
        val safeOffset = offsetFraction
            .takeIf(Double::isFinite)
            ?.coerceIn(0.0, 1.0)
            ?: 0.0
        return ((safeIndex + safeOffset) / itemCount.toDouble()).coerceIn(0.0, 1.0)
    }
}
