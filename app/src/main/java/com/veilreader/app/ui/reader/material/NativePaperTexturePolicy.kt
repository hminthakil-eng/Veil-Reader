package com.veilreader.app.ui.reader.material

/** Exact tokens prevent claiming NPOT from a similarly named unsupported extension. */
internal fun nativePaperSupportsNpot(extensions: String?): Boolean =
    extensions.orEmpty().splitToSequence(' ').any {
        it == "GL_OES_texture_npot" || it == "GL_APPLE_texture_2D_limited_npot"
    }

internal fun nativePaperTextureExtent(pixels: Int, supportsNpot: Boolean): Int {
    if (pixels <= 0) return 0
    if (supportsNpot || pixels == 1) return pixels
    val highest = Integer.highestOneBit(pixels - 1)
    return if (highest >= (1 shl 30)) Int.MAX_VALUE else highest * 2
}
