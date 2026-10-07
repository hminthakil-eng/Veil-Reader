package com.veilreader.app.ui.reader.tts

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTtsModelLayoutTest {
    @Test
    fun piperLayoutRequiresModelAndTokensAndAcceptsOptionalDataDirectory() {
        val root = createTempDirectory("veil-layout-piper").toFile()
        try {
            File(root, "model.onnx").writeBytes(byteArrayOf(1))
            File(root, "tokens.txt").writeText("a")
            File(root, "espeak-ng-data").mkdirs()

            val result = validateReaderTtsModelLayout(
                root,
                ReaderTtsModelLayout(
                    family = ReaderTtsNeuralModelFamily.VITS_PIPER,
                    components = mapOf(
                        ReaderTtsModelComponent.MODEL to "model.onnx",
                        ReaderTtsModelComponent.TOKENS to "tokens.txt",
                        ReaderTtsModelComponent.DATA_DIR to "espeak-ng-data"
                    )
                )
            )

            assertEquals(ReaderTtsModelLayoutValidation.Valid, result)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun matchaVoiceLayoutDoesNotDuplicateSharedVocoder() {
        val root = createTempDirectory("veil-layout-matcha").toFile()
        try {
            File(root, "acoustic.onnx").writeBytes(byteArrayOf(1))
            File(root, "tokens.txt").writeText("a")

            val layout = ReaderTtsModelLayout(
                family = ReaderTtsNeuralModelFamily.MATCHA,
                components = mapOf(
                    ReaderTtsModelComponent.ACOUSTIC_MODEL to "acoustic.onnx",
                    ReaderTtsModelComponent.TOKENS to "tokens.txt"
                )
            )

            assertEquals(
                ReaderTtsModelLayoutValidation.Valid,
                validateReaderTtsModelLayout(root, layout)
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun componentTraversalAndWindowsPathsAreRejectedAtManifestBoundary() {
        val base = mapOf(
            ReaderTtsModelComponent.MODEL to "model.onnx",
            ReaderTtsModelComponent.TOKENS to "tokens.txt"
        )
        assertNull(
            ReaderTtsModelLayout(
                ReaderTtsNeuralModelFamily.VITS_PIPER,
                base + (ReaderTtsModelComponent.DATA_DIR to "../escape")
            ).normalizedOrNull()
        )
        assertNull(
            ReaderTtsModelLayout(
                ReaderTtsNeuralModelFamily.VITS_PIPER,
                base + (ReaderTtsModelComponent.DATA_DIR to "C:\\model")
            ).normalizedOrNull()
        )
    }

    @Test
    fun emptyModelFileIsRejected() {
        val root = createTempDirectory("veil-layout-empty").toFile()
        try {
            File(root, "model.onnx").writeBytes(byteArrayOf())
            File(root, "tokens.txt").writeText("a")
            val result = validateReaderTtsModelLayout(
                root,
                ReaderTtsModelLayout(
                    ReaderTtsNeuralModelFamily.VITS_PIPER,
                    mapOf(
                        ReaderTtsModelComponent.MODEL to "model.onnx",
                        ReaderTtsModelComponent.TOKENS to "tokens.txt"
                    )
                )
            )

            assertEquals(
                ReaderTtsModelLayoutValidation.Rejected(
                    ReaderTtsModelLayoutValidation.Reason.EMPTY_FILE,
                    ReaderTtsModelComponent.MODEL
                ),
                result
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun symlinkComponentIsRejectedEvenIfPayloadWasTamperedAfterExtraction() {
        val root = createTempDirectory("veil-layout-link").toFile()
        val outside = createTempDirectory("veil-layout-outside").toFile()
        try {
            File(outside, "model.onnx").writeBytes(byteArrayOf(1))
            File(root, "tokens.txt").writeText("a")
            val link = File(root, "model.onnx").toPath()
            val symlinkCreated = runCatching {
                java.nio.file.Files.createSymbolicLink(
                    link,
                    File(outside, "model.onnx").toPath()
                )
            }.isSuccess
            if (!symlinkCreated) return

            val result = validateReaderTtsModelLayout(
                root,
                ReaderTtsModelLayout(
                    ReaderTtsNeuralModelFamily.VITS_PIPER,
                    mapOf(
                        ReaderTtsModelComponent.MODEL to "model.onnx",
                        ReaderTtsModelComponent.TOKENS to "tokens.txt"
                    )
                )
            )

            assertEquals(
                ReaderTtsModelLayoutValidation.Rejected(
                    ReaderTtsModelLayoutValidation.Reason.SYMBOLIC_LINK,
                    ReaderTtsModelComponent.MODEL
                ),
                result
            )
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }
}
