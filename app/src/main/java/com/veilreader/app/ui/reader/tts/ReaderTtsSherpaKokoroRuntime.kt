package com.veilreader.app.ui.reader.tts

import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val KOKORO_MODEL_ID = "kokoro-en-v0_19"
private val KOKORO_VOICE_REGEX = Regex("^kokoro-en-v0_19-(0|[1-9][0-9]*)$")

/**
 * Exact narrator mapping. A malformed or unavailable speaker ID is an error,
 * never a reason to secretly speak as narrator 0.
 */
internal fun sherpaKokoroSpeakerId(voiceId: String?, numSpeakers: Int): Int? {
    if (numSpeakers <= 0) return null
    val id = voiceId?.let { KOKORO_VOICE_REGEX.matchEntire(it)?.groupValues?.get(1) }
        ?.toIntOrNull() ?: return null
    return id.takeIf { it in 0 until numSpeakers }
}

internal sealed interface ReaderTtsKokoroOpenResult {
    data class Ready(val runtime: ReaderTtsSherpaKokoroRuntime) : ReaderTtsKokoroOpenResult
    data class Unavailable(val reason: Reason) : ReaderTtsKokoroOpenResult

    enum class Reason {
        INVALID_OR_UNVERIFIED_PAYLOAD,
        MISSING_PHONEMIZER,
        INCOMPATIBLE_NATIVE_LIBRARY,
        NATIVE_MODEL_INITIALIZATION_FAILED,
        INVALID_NATIVE_METADATA
    }
}

/**
 * Native Kokoro / sherpa-onnx bridge running in *Veil's own process*.
 *
 * Keeps the existing ReaderTtsNeuralRuntime contract and its suspend PCM
 * callback. The upstream JNI callback is synchronous, so the JNI worker
 * blocks for a bounded downstream AudioTrack submit: backpressure propagates
 * directly instead of allocating an unbounded PCM queue.
 *
 * Factory can only accept ReaderTtsPreparedModel (previously verified archive,
 * validated layout and atomically published payload). Native binaries are
 * supplied only by a reviewed ARM64 debug-lab build; this class deliberately
 * does not download native code or silently fall back to System TTS.
 *
 * No Readium objects or raw publication text are ever written to logs.
 */
internal class ReaderTtsSherpaKokoroRuntime private constructor(
    private val native: OfflineTts,
    override val model: ReaderTtsNeuralModelSpec,
    override val sampleRateHz: Int,
    private val numSpeakers: Int
) : ReaderTtsNeuralRuntime {
    private val nativeMutex = Mutex()
    private val closeRequested = AtomicBoolean(false)
    private val closeStarted = AtomicBoolean(false)
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun synthesize(
        text: String,
        voiceId: String?,
        speed: Float,
        onPcmChunk: suspend (FloatArray) -> Boolean
    ): ReaderTtsProblem? = nativeMutex.withLock {
        if (closeRequested.get()) return@withLock ReaderTtsProblem.NO_ENGINE
        if (text.isBlank() || !speed.isFinite() || speed !in 0.5f..3f) {
            return@withLock ReaderTtsProblem.CONTENT
        }
        val speaker = sherpaKokoroSpeakerId(voiceId, numSpeakers)
            ?: return@withLock ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE

        // Never block the main thread on native inference or JNI callbacks.
        withContext(Dispatchers.IO) {
            val ownerJob: Job? = currentCoroutineContext()[Job]
            val acceptedChunks = AtomicInteger(0)
            val callbackRejected = AtomicBoolean(false)
            try {
                native.generateWithConfigAndCallback(
                    text = text,
                    config = GenerationConfig(sid = speaker, speed = speed)
                ) { samples ->
                    if (closeRequested.get() || ownerJob?.isActive == false ||
                        samples.isEmpty()
                    ) {
                        callbackRejected.set(true)
                        return@generateWithConfigAndCallback 0
                    }

                    // JNI invokes a non-suspend callback, potentially on a
                    // native worker. Bound native production to consumer pace.
                    // Coordinator copies borrowed native PCM before AudioTrack
                    // writes; no native sample array survives that callback.
                    val kept = try {
                        runBlocking { onPcmChunk(samples) }
                    } catch (_: Exception) {
                        false
                    }
                    if (kept && !closeRequested.get() && ownerJob?.isActive != false) {
                        acceptedChunks.incrementAndGet()
                        1
                    } else {
                        callbackRejected.set(true)
                        0
                    }
                }
                if (closeRequested.get() || ownerJob?.isActive == false ||
                    callbackRejected.get() || acceptedChunks.get() == 0
                ) ReaderTtsProblem.SYNTHESIS else null
            } catch (_: LinkageError) {
                ReaderTtsProblem.NO_ENGINE
            } catch (_: Exception) {
                ReaderTtsProblem.SYNTHESIS
            }
        }
    }

    /**
     * Never free native OfflineTts from the UI thread or while the JNI
     * generate callback is still using its native pointer.
     * Pending syntheses fail closed as soon as closeRequested is set.
     */
    override fun close() {
        closeRequested.set(true)
        if (!closeStarted.compareAndSet(false, true)) return
        cleanupScope.launch {
            try {
                nativeMutex.withLock { native.free() }
            } finally {
                cleanupScope.cancel()
            }
        }
    }

    companion object {
        /**
         * Debug lab only. Packaged assets are never installed by a release
         * build; production always requires verified model store data.
         */
        suspend fun openBundledDebugAssets(
            assets: android.content.res.AssetManager,
            nativeThreads: Int = 2
        ): ReaderTtsKokoroOpenResult = withContext(Dispatchers.IO) {
            if (!com.veilreader.app.BuildConfig.DEBUG || nativeThreads !in 1..4) {
                return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.INVALID_NATIVE_METADATA
                )
            }
            val folder = KOKORO_MODEL_ID
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = "$folder/model.onnx",
                        voices = "$folder/voices.bin",
                        tokens = "$folder/tokens.txt",
                        dataDir = "$folder/espeak-ng-data",
                        lang = "eng"
                    ),
                    numThreads = nativeThreads,
                    debug = false,
                    provider = "cpu"
                )
            )
            val engine = try {
                OfflineTts(assetManager = assets, config = config)
            } catch (_: LinkageError) {
                return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.INCOMPATIBLE_NATIVE_LIBRARY
                )
            } catch (_: Exception) {
                return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.NATIVE_MODEL_INITIALIZATION_FAILED
                )
            }
            try {
                val speakers = engine.numSpeakers()
                val rate = engine.sampleRate()
                if (speakers !in 1..256 || rate !in 8_000..96_000) {
                    engine.free()
                    return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                        ReaderTtsKokoroOpenResult.Reason.INVALID_NATIVE_METADATA
                    )
                }
                ReaderTtsKokoroOpenResult.Ready(
                    ReaderTtsSherpaKokoroRuntime(
                        engine,
                        ReaderTtsNeuralModelSpec(
                            packageId = KOKORO_MODEL_ID,
                            family = ReaderTtsNeuralModelFamily.KOKORO,
                            languageTags = setOf("en-US"),
                            defaultVoiceId = "$KOKORO_MODEL_ID-0",
                            supportsMultipleVoices = speakers > 1,
                            expectedSampleRateHz = rate
                        ),
                        rate,
                        speakers
                    )
                )
            } catch (_: LinkageError) {
                runCatching { engine.free() }
                ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.INCOMPATIBLE_NATIVE_LIBRARY
                )
            } catch (_: Exception) {
                runCatching { engine.free() }
                ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.NATIVE_MODEL_INITIALIZATION_FAILED
                )
            }
        }

        suspend fun open(
            prepared: ReaderTtsPreparedModel,
            nativeThreads: Int = 2
        ): ReaderTtsKokoroOpenResult = withContext(Dispatchers.IO) {
            if (nativeThreads !in 1..4) {
                return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.INVALID_NATIVE_METADATA
                )
            }
            val layout = prepared.layout
            if (layout.family != ReaderTtsNeuralModelFamily.KOKORO ||
                validateReaderTtsModelLayout(prepared.payloadRoot, layout) !=
                    ReaderTtsModelLayoutValidation.Valid
            ) return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                ReaderTtsKokoroOpenResult.Reason.INVALID_OR_UNVERIFIED_PAYLOAD
            )

            fun component(which: ReaderTtsModelComponent): File? =
                layout.components[which]?.let { File(prepared.payloadRoot, it) }

            // Kokoro needs espeak-ng-data for English phonemization; the
            // generic KOKORO layout allows it to be absent, but the actual
            // runtime must reject that incomplete installation explicitly.
            val data = component(ReaderTtsModelComponent.DATA_DIR)
                ?.takeIf(File::isDirectory)
                ?: return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.MISSING_PHONEMIZER
                )
            val modelFile = component(ReaderTtsModelComponent.MODEL)
            val voices = component(ReaderTtsModelComponent.VOICES)
            val tokens = component(ReaderTtsModelComponent.TOKENS)
            if (modelFile?.isFile != true || voices?.isFile != true ||
                tokens?.isFile != true
            ) return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                ReaderTtsKokoroOpenResult.Reason.INVALID_OR_UNVERIFIED_PAYLOAD
            )

            val nativeConfig = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = modelFile.absolutePath,
                        voices = voices.absolutePath,
                        tokens = tokens.absolutePath,
                        dataDir = data.absolutePath,
                        lang = "eng"
                    ),
                    numThreads = nativeThreads,
                    debug = false,
                    provider = "cpu"
                )
            )
            val engine = try {
                OfflineTts(config = nativeConfig)
            } catch (_: LinkageError) {
                return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.INCOMPATIBLE_NATIVE_LIBRARY
                )
            } catch (_: Exception) {
                return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.NATIVE_MODEL_INITIALIZATION_FAILED
                )
            }

            try {
                val speakers = engine.numSpeakers()
                val rate = engine.sampleRate()
                if (speakers !in 1..256 || rate !in 8_000..96_000) {
                    engine.free()
                    return@withContext ReaderTtsKokoroOpenResult.Unavailable(
                        ReaderTtsKokoroOpenResult.Reason.INVALID_NATIVE_METADATA
                    )
                }
                val spec = ReaderTtsNeuralModelSpec(
                    packageId = prepared.installed.packageInfo.id,
                    family = ReaderTtsNeuralModelFamily.KOKORO,
                    languageTags = setOf("en-US"),
                    defaultVoiceId = "$KOKORO_MODEL_ID-0",
                    supportsMultipleVoices = speakers > 1,
                    expectedSampleRateHz = rate
                )
                ReaderTtsKokoroOpenResult.Ready(
                    ReaderTtsSherpaKokoroRuntime(engine, spec, rate, speakers)
                )
            } catch (_: LinkageError) {
                runCatching { engine.free() }
                ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.INCOMPATIBLE_NATIVE_LIBRARY
                )
            } catch (_: Exception) {
                runCatching { engine.free() }
                ReaderTtsKokoroOpenResult.Unavailable(
                    ReaderTtsKokoroOpenResult.Reason.NATIVE_MODEL_INITIALIZATION_FAILED
                )
            }
        }
    }
}
