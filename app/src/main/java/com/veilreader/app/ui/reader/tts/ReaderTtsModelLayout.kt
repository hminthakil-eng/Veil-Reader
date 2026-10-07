package com.veilreader.app.ui.reader.tts

import java.io.File
import java.nio.file.Files
import java.util.EnumMap

internal enum class ReaderTtsModelComponent {
    MODEL,
    ACOUSTIC_MODEL,
    VOCODER,
    VOICES,
    TOKENS,
    DATA_DIR,
    DICT_DIR,
    LEXICON,
    RULE_FST,
    RULE_FAR,
    ENCODER,
    DECODER,
    DURATION_PREDICTOR,
    TEXT_ENCODER,
    VECTOR_ESTIMATOR,
    TTS_JSON,
    UNICODE_INDEXER,
    VOICE_STYLE,
    LM_FLOW,
    LM_MAIN,
    TEXT_CONDITIONER,
    VOCAB_JSON,
    TOKEN_SCORES_JSON
}

internal data class ReaderTtsModelLayout(
    val family: ReaderTtsNeuralModelFamily,
    val components: Map<ReaderTtsModelComponent, String>
) {
    fun normalizedOrNull(): ReaderTtsModelLayout? {
        val safe = EnumMap<ReaderTtsModelComponent, String>(ReaderTtsModelComponent::class.java)
        components.forEach { (component, rawPath) ->
            val path = normalizeRelativeModelPath(rawPath) ?: return null
            safe[component] = path
        }
        if (!safe.keys.containsAll(requiredComponents(family))) return null
        return copy(components = safe.toMap())
    }
}

internal sealed interface ReaderTtsModelLayoutValidation {
    data object Valid : ReaderTtsModelLayoutValidation

    data class Rejected(
        val reason: Reason,
        val component: ReaderTtsModelComponent? = null
    ) : ReaderTtsModelLayoutValidation

    enum class Reason {
        INVALID_LAYOUT,
        PAYLOAD_ROOT_MISSING,
        MISSING_COMPONENT,
        UNSAFE_COMPONENT_PATH,
        SYMBOLIC_LINK,
        WRONG_COMPONENT_TYPE,
        EMPTY_FILE
    }
}

internal fun validateReaderTtsModelLayout(
    payloadRoot: File,
    layout: ReaderTtsModelLayout
): ReaderTtsModelLayoutValidation {
    val safe = layout.normalizedOrNull()
        ?: return ReaderTtsModelLayoutValidation.Rejected(
            ReaderTtsModelLayoutValidation.Reason.INVALID_LAYOUT
        )
    if (!payloadRoot.isDirectory) {
        return ReaderTtsModelLayoutValidation.Rejected(
            ReaderTtsModelLayoutValidation.Reason.PAYLOAD_ROOT_MISSING
        )
    }
    val canonicalRoot = runCatching { payloadRoot.canonicalFile }.getOrNull()
        ?: return ReaderTtsModelLayoutValidation.Rejected(
            ReaderTtsModelLayoutValidation.Reason.PAYLOAD_ROOT_MISSING
        )

    for ((component, relativePath) in safe.components) {
        val target = runCatching { File(canonicalRoot, relativePath).canonicalFile }.getOrNull()
            ?: return ReaderTtsModelLayoutValidation.Rejected(
                ReaderTtsModelLayoutValidation.Reason.UNSAFE_COMPONENT_PATH,
                component
            )
        if (!isInsideModelRoot(canonicalRoot, target)) {
            return ReaderTtsModelLayoutValidation.Rejected(
                ReaderTtsModelLayoutValidation.Reason.UNSAFE_COMPONENT_PATH,
                component
            )
        }

        val rawTarget = File(canonicalRoot, relativePath)
        if (Files.isSymbolicLink(rawTarget.toPath())) {
            return ReaderTtsModelLayoutValidation.Rejected(
                ReaderTtsModelLayoutValidation.Reason.SYMBOLIC_LINK,
                component
            )
        }
        if (!rawTarget.exists()) {
            return ReaderTtsModelLayoutValidation.Rejected(
                ReaderTtsModelLayoutValidation.Reason.MISSING_COMPONENT,
                component
            )
        }

        if (component in DIRECTORY_COMPONENTS) {
            if (!rawTarget.isDirectory) {
                return ReaderTtsModelLayoutValidation.Rejected(
                    ReaderTtsModelLayoutValidation.Reason.WRONG_COMPONENT_TYPE,
                    component
                )
            }
        } else {
            if (!rawTarget.isFile) {
                return ReaderTtsModelLayoutValidation.Rejected(
                    ReaderTtsModelLayoutValidation.Reason.WRONG_COMPONENT_TYPE,
                    component
                )
            }
            if (rawTarget.length() <= 0L) {
                return ReaderTtsModelLayoutValidation.Rejected(
                    ReaderTtsModelLayoutValidation.Reason.EMPTY_FILE,
                    component
                )
            }
        }
    }

    return ReaderTtsModelLayoutValidation.Valid
}

private fun requiredComponents(
    family: ReaderTtsNeuralModelFamily
): Set<ReaderTtsModelComponent> = when (family) {
    ReaderTtsNeuralModelFamily.VITS_PIPER -> setOf(
        ReaderTtsModelComponent.MODEL,
        ReaderTtsModelComponent.TOKENS
    )
    ReaderTtsNeuralModelFamily.MATCHA -> setOf(
        ReaderTtsModelComponent.ACOUSTIC_MODEL,
        ReaderTtsModelComponent.VOCODER,
        ReaderTtsModelComponent.TOKENS
    )
    ReaderTtsNeuralModelFamily.KOKORO,
    ReaderTtsNeuralModelFamily.KITTEN -> setOf(
        ReaderTtsModelComponent.MODEL,
        ReaderTtsModelComponent.VOICES,
        ReaderTtsModelComponent.TOKENS
    )
    ReaderTtsNeuralModelFamily.ZIPVOICE -> setOf(
        ReaderTtsModelComponent.ENCODER,
        ReaderTtsModelComponent.DECODER,
        ReaderTtsModelComponent.VOCODER,
        ReaderTtsModelComponent.TOKENS
    )
    ReaderTtsNeuralModelFamily.SUPERTONIC -> setOf(
        ReaderTtsModelComponent.DURATION_PREDICTOR,
        ReaderTtsModelComponent.TEXT_ENCODER,
        ReaderTtsModelComponent.VECTOR_ESTIMATOR,
        ReaderTtsModelComponent.VOCODER,
        ReaderTtsModelComponent.TTS_JSON,
        ReaderTtsModelComponent.UNICODE_INDEXER,
        ReaderTtsModelComponent.VOICE_STYLE
    )
    ReaderTtsNeuralModelFamily.POCKET -> setOf(
        ReaderTtsModelComponent.LM_FLOW,
        ReaderTtsModelComponent.LM_MAIN,
        ReaderTtsModelComponent.ENCODER,
        ReaderTtsModelComponent.DECODER,
        ReaderTtsModelComponent.TEXT_CONDITIONER,
        ReaderTtsModelComponent.VOCAB_JSON,
        ReaderTtsModelComponent.TOKEN_SCORES_JSON
    )
}

private fun normalizeRelativeModelPath(rawPath: String): String? {
    val path = rawPath.trim()
    if (
        path.isEmpty() ||
        path.length > 1_024 ||
        path.startsWith("/") ||
        path.startsWith("\\") ||
        path.contains('\\') ||
        WINDOWS_DRIVE_PREFIX.matches(path)
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

private fun isInsideModelRoot(root: File, target: File): Boolean =
    target.path == root.path ||
        target.path.startsWith(root.path + File.separator)

private val DIRECTORY_COMPONENTS = setOf(
    ReaderTtsModelComponent.DATA_DIR,
    ReaderTtsModelComponent.DICT_DIR
)

private val WINDOWS_DRIVE_PREFIX = Regex("^[A-Za-z]:.*")
