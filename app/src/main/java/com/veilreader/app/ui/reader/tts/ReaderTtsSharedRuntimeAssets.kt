package com.veilreader.app.ui.reader.tts

import java.io.File
import java.nio.file.Files
import java.util.EnumMap

internal enum class ReaderTtsSharedRuntimeAsset {
    MATCHA_VOCODER
}

/**
 * Shared neural runtime files live outside individual voice payloads.
 *
 * This matters for Matcha: multiple Persian/English voices may share one reviewed Vocos package
 * instead of duplicating the same vocoder inside every downloaded voice.
 */
internal data class ReaderTtsSharedRuntimeAssets(
    val root: File,
    val components: Map<ReaderTtsSharedRuntimeAsset, String>
) {
    fun normalizedOrNull(): ReaderTtsSharedRuntimeAssets? {
        val safe = EnumMap<ReaderTtsSharedRuntimeAsset, String>(
            ReaderTtsSharedRuntimeAsset::class.java
        )
        components.forEach { (role, rawPath) ->
            val path = normalizeSharedAssetPath(rawPath) ?: return null
            safe[role] = path
        }
        return copy(components = safe.toMap())
    }
}

internal sealed interface ReaderTtsSharedAssetsValidation {
    data object Valid : ReaderTtsSharedAssetsValidation

    data class Rejected(
        val reason: Reason,
        val asset: ReaderTtsSharedRuntimeAsset? = null
    ) : ReaderTtsSharedAssetsValidation

    enum class Reason {
        INVALID_BUNDLE,
        ROOT_MISSING,
        MISSING_ASSET,
        UNSAFE_PATH,
        SYMBOLIC_LINK,
        NOT_A_FILE,
        EMPTY_FILE
    }
}

internal fun requiredSharedRuntimeAssets(
    family: ReaderTtsNeuralModelFamily
): Set<ReaderTtsSharedRuntimeAsset> = when (family) {
    ReaderTtsNeuralModelFamily.MATCHA -> setOf(
        ReaderTtsSharedRuntimeAsset.MATCHA_VOCODER
    )
    else -> emptySet()
}

internal fun validateReaderTtsSharedRuntimeAssets(
    family: ReaderTtsNeuralModelFamily,
    bundle: ReaderTtsSharedRuntimeAssets?
): ReaderTtsSharedAssetsValidation {
    val required = requiredSharedRuntimeAssets(family)
    if (required.isEmpty()) return ReaderTtsSharedAssetsValidation.Valid

    val safe = bundle?.normalizedOrNull()
        ?: return ReaderTtsSharedAssetsValidation.Rejected(
            ReaderTtsSharedAssetsValidation.Reason.INVALID_BUNDLE
        )
    if (!safe.root.isDirectory) {
        return ReaderTtsSharedAssetsValidation.Rejected(
            ReaderTtsSharedAssetsValidation.Reason.ROOT_MISSING
        )
    }
    val canonicalRoot = runCatching { safe.root.canonicalFile }.getOrNull()
        ?: return ReaderTtsSharedAssetsValidation.Rejected(
            ReaderTtsSharedAssetsValidation.Reason.ROOT_MISSING
        )

    for (asset in required) {
        val relativePath = safe.components[asset]
            ?: return ReaderTtsSharedAssetsValidation.Rejected(
                ReaderTtsSharedAssetsValidation.Reason.MISSING_ASSET,
                asset
            )
        val rawTarget = File(canonicalRoot, relativePath)
        val target = runCatching { rawTarget.canonicalFile }.getOrNull()
            ?: return ReaderTtsSharedAssetsValidation.Rejected(
                ReaderTtsSharedAssetsValidation.Reason.UNSAFE_PATH,
                asset
            )
        if (
            target.path != canonicalRoot.path &&
            !target.path.startsWith(canonicalRoot.path + File.separator)
        ) {
            return ReaderTtsSharedAssetsValidation.Rejected(
                ReaderTtsSharedAssetsValidation.Reason.UNSAFE_PATH,
                asset
            )
        }
        if (Files.isSymbolicLink(rawTarget.toPath())) {
            return ReaderTtsSharedAssetsValidation.Rejected(
                ReaderTtsSharedAssetsValidation.Reason.SYMBOLIC_LINK,
                asset
            )
        }
        if (!rawTarget.exists()) {
            return ReaderTtsSharedAssetsValidation.Rejected(
                ReaderTtsSharedAssetsValidation.Reason.MISSING_ASSET,
                asset
            )
        }
        if (!rawTarget.isFile) {
            return ReaderTtsSharedAssetsValidation.Rejected(
                ReaderTtsSharedAssetsValidation.Reason.NOT_A_FILE,
                asset
            )
        }
        if (rawTarget.length() <= 0L) {
            return ReaderTtsSharedAssetsValidation.Rejected(
                ReaderTtsSharedAssetsValidation.Reason.EMPTY_FILE,
                asset
            )
        }
    }

    return ReaderTtsSharedAssetsValidation.Valid
}

private fun normalizeSharedAssetPath(rawPath: String): String? {
    val path = rawPath.trim()
    if (
        path.isEmpty() ||
        path.length > 1_024 ||
        path.startsWith("/") ||
        path.startsWith("\\") ||
        path.contains('\\') ||
        SHARED_WINDOWS_DRIVE_PREFIX.matches(path)
    ) {
        return null
    }
    val segments = path.split('/')
    if (
        segments.any {
            it.isEmpty() ||
                it == "." ||
                it == ".." ||
                it.indexOf('\u0000') >= 0
        }
    ) {
        return null
    }
    return segments.joinToString("/")
}

private val SHARED_WINDOWS_DRIVE_PREFIX = Regex("^[A-Za-z]:.*")
