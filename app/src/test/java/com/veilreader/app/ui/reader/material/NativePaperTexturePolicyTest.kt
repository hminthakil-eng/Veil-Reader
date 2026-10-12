package com.veilreader.app.ui.reader.material

import org.junit.Assert.*
import org.junit.Test

class NativePaperTexturePolicyTest {
    @Test fun exactExtensionTokensGateNativeSizeTextures() {
        assertTrue(nativePaperSupportsNpot("GL_EXT_other GL_OES_texture_npot GL_EXT_more"))
        assertTrue(nativePaperSupportsNpot("GL_APPLE_texture_2D_limited_npot"))
        assertFalse(nativePaperSupportsNpot("GL_OES_texture_npot_fake"))
        assertFalse(nativePaperSupportsNpot("GL_EXT_other"))
        assertFalse(nativePaperSupportsNpot(null))
    }

    @Test fun fullResolutionInkAvoidsThePaddedTextureOnSupportingContexts() {
        assertEquals(1080, nativePaperTextureExtent(1080, true))
        assertEquals(2400, nativePaperTextureExtent(2400, true))
        assertEquals(2048, nativePaperTextureExtent(1080, false))
        assertEquals(4096, nativePaperTextureExtent(2400, false))
    }

    @Test fun compatibilityExtentNeverWrapsIntoAnAcceptedSmallTexture() {
        assertEquals(1, nativePaperTextureExtent(1, false))
        assertEquals(1024, nativePaperTextureExtent(1024, false))
        assertEquals(0, nativePaperTextureExtent(0, false))
        assertEquals(0, nativePaperTextureExtent(-1, true))
        assertEquals(Int.MAX_VALUE, nativePaperTextureExtent(Int.MAX_VALUE, false))
    }
}
