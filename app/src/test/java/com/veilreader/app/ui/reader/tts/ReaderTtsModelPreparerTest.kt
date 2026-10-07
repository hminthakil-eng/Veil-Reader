package com.veilreader.app.ui.reader.tts

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsModelPreparerTest {
    @Test
    fun alreadyValidatedPayloadRemainsUsableAfterCompressedArchiveIsRemoved() {
        val root = createTempDirectory("veil-prepared-reuse").toFile()
        try {
            val modelDirectory = File(root, "fa-model-1.0").apply { mkdirs() }
            val payload = File(modelDirectory, "payload").apply { mkdirs() }
            File(payload, "model.onnx").writeBytes(byteArrayOf(1))
            File(payload, "tokens.txt").writeText("a")

            val installed = ReaderTtsInstalledModel(
                packageInfo = packageInfo(),
                directory = modelDirectory,
                archive = File(modelDirectory, "model.package"),
                lastUsedEpochMs = 1L
            )
            val result = ReaderTtsModelPreparer(
                ReaderTtsModelStore(root, budgetBytes = 16_384L)
            ).prepare(installed, piperLayout())

            assertTrue(result is ReaderTtsModelPrepareResult.Ready)
            assertEquals(
                payload.canonicalFile,
                (result as ReaderTtsModelPrepareResult.Ready)
                    .model.payloadRoot.canonicalFile
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun extractionNeverStartsWhenWorstCaseExpandedFootprintExceedsBudget() {
        val root = createTempDirectory("veil-prepared-budget").toFile()
        try {
            val modelDirectory = File(root, "fa-model-1.0").apply { mkdirs() }
            val archive = File(modelDirectory, "model.package").apply {
                writeBytes(ByteArray(2_048) { 1 })
            }
            val installed = ReaderTtsInstalledModel(
                packageInfo = packageInfo(
                    expectedBytes = 2_048L,
                    maxExpandedBytes = 4_096L
                ),
                directory = modelDirectory,
                archive = archive,
                lastUsedEpochMs = 1L
            )

            val result = ReaderTtsModelPreparer(
                ReaderTtsModelStore(root, budgetBytes = 4_096L)
            ).prepare(installed, piperLayout())

            assertEquals(
                ReaderTtsModelPrepareResult.Rejected(
                    ReaderTtsModelPrepareResult.Reason.STORAGE_BUDGET
                ),
                result
            )
            assertTrue(
                modelDirectory.listFiles()
                    .orEmpty()
                    .none { it.name.startsWith(".payload-staging-") }
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingArchiveIsActionableOnlyWhenNoValidPreparedPayloadExists() {
        val root = createTempDirectory("veil-prepared-missing").toFile()
        try {
            val modelDirectory = File(root, "fa-model-1.0").apply { mkdirs() }
            val installed = ReaderTtsInstalledModel(
                packageInfo = packageInfo(),
                directory = modelDirectory,
                archive = File(modelDirectory, "model.package"),
                lastUsedEpochMs = 1L
            )

            val result = ReaderTtsModelPreparer(
                ReaderTtsModelStore(root, budgetBytes = 16_384L)
            ).prepare(installed, piperLayout())

            assertEquals(
                ReaderTtsModelPrepareResult.Rejected(
                    ReaderTtsModelPrepareResult.Reason.INSTALLED_ARCHIVE_MISSING
                ),
                result
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private fun piperLayout() = ReaderTtsModelLayout(
        family = ReaderTtsNeuralModelFamily.VITS_PIPER,
        components = mapOf(
            ReaderTtsModelComponent.MODEL to "model.onnx",
            ReaderTtsModelComponent.TOKENS to "tokens.txt"
        )
    )

    private fun packageInfo(
        expectedBytes: Long = 2_048L,
        maxExpandedBytes: Long = 4_096L
    ) = ReaderTtsModelPackage(
        id = "fa-model",
        version = "1.0",
        languageTag = "fa-IR",
        displayName = "Persian fixture",
        expectedBytes = expectedBytes,
        maxExpandedBytes = maxExpandedBytes,
        sha256 = "0".repeat(64),
        licenseSpdx = "MIT",
        licenseUrl = "https://example.invalid/license",
        sourceUrl = "https://example.invalid/model"
    )
}
