package com.veilreader.app.ui.reader.tts

import android.content.Context
import android.content.Intent

/**
 * Discovery only. The separate, official SherpaOnnxTtsEngine Android app
 * implements Android's TextToSpeechService interface and must be installed
 * and configured with a model (Kokoro for English; a separately validated
 * Persian model for fa-IR) before narration can start.
 *
 * Do NOT mistake an installed package for a loaded/working model.
 * This package is intentionally not bundled into Veil Reader.
 */
internal object ReaderTtsSherpaEngineDiscovery {
    const val PACKAGE = "com.k2fsa.sherpa.onnx.tts.engine"
    private const val SERVICE_ACTION = "android.intent.action.TTS_SERVICE"

    fun installed(context: Context): Boolean = runCatching {
        context.packageManager.queryIntentServices(Intent(SERVICE_ACTION), 0)
            .any { it.serviceInfo?.packageName == PACKAGE }
    }.getOrDefault(false)
}
