package com.veilreader.app.ui.reader.tts

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsSharedRuntimeAssetsTest {
    @Test
    fun matchaRequiresOneSharedVocoderButPiperRequiresNone() {
        assertEquals(
            setOf(ReaderTtsSharedRuntimeAsset.MATCHA_VOCODER),
            requiredSharedRuntimeAssets(ReaderTtsNeuralModelFamily.MATCHA)
        )
        assertTrue(
            requiredSharedRuntimeAssets(
                ReaderTtsNeuralModelFamily.VITS_PIPER
            ).isEmpty()
        )
    }

    @Test
    fun matchaSharedVocoderMustBeRealNonEmptyFileInsideAssetRoot() {
        val root = createTempDirectory("veil-matcha-shared").toFile()
        try {
            val vocoder = File(root, "vocos-22khz-univ.onnx")
            vocoder.writeBytes(byteArrayOf(1, 2, 3))

            val valid = ReaderTtsSharedRuntimeAssets(
                root = root,
                components = mapOf(
                    ReaderTtsSharedRuntimeAsset.MATCHA_VOCODER to vocoder.name
                )
            )
            assertEquals(
                ReaderTtsSharedAssetsValidation.Valid,
                validateReaderTtsSharedRuntimeAssets(
                    ReaderTtsNeuralModelFamily.MATCHA,
                    valid
                )
            )

            vocoder.writeBytes(byteArrayOf())
            assertEquals(
                ReaderTtsSharedAssetsValidation.Rejected(
                    ReaderTtsSharedAssetsValidation.Reason.EMPTY_FILE,
                    ReaderTtsSharedRuntimeAsset.MATCHA_VOCODER
                ),
                validateReaderTtsSharedRuntimeAssets(
                    ReaderTtsNeuralModelFamily.MATCHA,
                    valid
                )
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun sharedAssetTraversalIsRejectedBeforeRuntimeConstruction() {
        val root = createTempDirectory("veil-matcha-shared").toFile()
        try {
            val bundle = ReaderTtsSharedRuntimeAssets(
                root = root,
                components = mapOf(
                    ReaderTtsSharedRuntimeAsset.MATCHA_VOCODER to "../outside.onnx"
                )
            )

            assertEquals(
                ReaderTtsSharedAssetsValidation.Rejected(
                    ReaderTtsSharedAssetsValidation.Reason.INVALID_BUNDLE
                ),
                validateReaderTtsSharedRuntimeAssets(
                    ReaderTtsNeuralModelFamily.MATCHA,
                    bundle
                )
            )
        } finally {
            root.deleteRecursively()
        }
    }
}
