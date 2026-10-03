package com.veilreader.app.domain

data class ReaderTtsSettings(
    val speed: Double = 1.0,
    val pitch: Double = 1.0
) {
    fun normalized(): ReaderTtsSettings =
        copy(
            speed = speed
                .takeIf { it.isFinite() }
                ?.coerceIn(0.50, 2.00)
                ?: 1.0,
            pitch = pitch
                .takeIf { it.isFinite() }
                ?.coerceIn(0.60, 1.40)
                ?: 1.0
        )
}
