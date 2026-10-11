package com.veilreader.app.ui.reader.tts

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.veilreader.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Internal-only real-on-device neural lab. It is another screen inside the
 * SAME Veil APK, not a second Android TTS provider application.
 *
 * The model and arm64 libraries are staged into this debug APK by a distinct
 * pinned GitHub Actions lab workflow. Ordinary debug builds do not include
 * the large model. Release builds cannot compile this Activity.
 */
internal class VeilNeuralVoiceLabActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionGeneration = 0L
    private var nativeRuntime: ReaderTtsSherpaKokoroRuntime? = null
    private var sink: ReaderTtsAndroidAudioTrackSink? = null
    private var coordinator: ReaderTtsInProcessCoordinator? = null
    private var running: Job? = null
    private lateinit var status: TextView
    private lateinit var sample: EditText
    private lateinit var voices: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            finish()
            return
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (18 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        fun label(value: String) = TextView(this).apply {
            text = value
            textSize = 17f
            root.addView(this)
        }
        label("VEIL · Neural Voice Lab")
        label("Debug only · offline Kokoro · no second app")
        status = TextView(this).apply {
            text = "Ready. Playback requires the optional in-app lab model."
            textSize = 14f
            root.addView(this)
        }
        label("Test text (English)")
        sample = EditText(this).apply {
            setText(
                "Beyond the old library, the morning mist slowly lifted. " +
                    "A quiet bell echoed across the empty streets, and the story began."
            )
            setMinLines(3)
            maxLines = 6
            root.addView(
                this, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        label("Narrator (native speaker IDs 0–10)")
        voices = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@VeilNeuralVoiceLabActivity,
                android.R.layout.simple_spinner_dropdown_item,
                (0..10).map { "Kokoro · voice $it" }
            )
            root.addView(this)
        }
        val play = Button(this).apply {
            text = "Play selected voice"
            setOnClickListener { playSelectedVoice() }
            root.addView(this)
        }
        Button(this).apply {
            text = "Pause · flush all queued audio"
            setOnClickListener { stopImmediately() }
            root.addView(this)
        }
        label(
            "Acceptance: tap Pause during first loading and while speaking. " +
                "No delayed audio may start. Try all 11 voices, then close/reopen."
        )
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun playSelectedVoice() {
        val userText = sample.text.toString().trim()
        if (userText.isBlank()) {
            status.text = "Enter test text first."
            return
        }
        // Every new user Play is explicit; revoke old generation and its PCM.
        stopImmediately()
        val generation = sessionGeneration
        val requestedVoice = "kokoro-en-v0_19-${voices.selectedItemPosition}"
        running = scope.launch {
            if (nativeRuntime == null) {
                status.text = "Loading Kokoro weights on native worker…"
                val loaded = ReaderTtsSherpaKokoroRuntime.openBundledDebugAssets(assets)
                if (generation != sessionGeneration) {
                    if (loaded is ReaderTtsKokoroOpenResult.Ready) loaded.runtime.close()
                    return@launch
                }
                if (loaded !is ReaderTtsKokoroOpenResult.Ready) {
                    status.text = "Model/native unavailable: " +
                        (loaded as ReaderTtsKokoroOpenResult.Unavailable).reason
                    return@launch
                }
                nativeRuntime = loaded.runtime
            }
            if (generation != sessionGeneration) return@launch
            val native = nativeRuntime ?: return@launch
            val output = sink ?: ReaderTtsAndroidAudioTrackSink().also { sink = it }
            val player = coordinator
                ?: ReaderTtsInProcessCoordinator(native, output).also { coordinator = it }
            status.text = "Preparing $requestedVoice…"
            val result = player.speak(userText, requestedVoice, speed = 1f)
            if (generation == sessionGeneration) {
                status.text = "Voice $requestedVoice: $result"
            }
        }
    }

    private fun stopImmediately() {
        sessionGeneration++
        coordinator?.pause()
        if (coordinator == null) sink?.silenceImmediately()
        status.text = "Paused. Native loading/inference output is revoked."
    }

    override fun onDestroy() {
        stopImmediately()
        // The native engine owns a JNI pointer; close() defers destruction
        // until the worker finishes. Never free it on the UI thread.
        nativeRuntime?.close()
        val output = sink
        sink = null
        // The UI scope is canceled below, so do not schedule native device
        // release on that soon-to-be-canceled scope.
        if (output != null) {
            Thread({ output.close() }, "VeilTtsAudioRelease").start()
        }
        scope.cancel()
        super.onDestroy()
    }
}
