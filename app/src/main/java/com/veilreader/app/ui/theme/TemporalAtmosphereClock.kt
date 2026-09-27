package com.veilreader.app.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Instant
import java.time.ZoneId

internal data class VeilTemporalContext(
    val phase: VeilTemporalPhase,
    val zoneId: ZoneId
)

// Stable fallback for isolated previews. VeilTheme supplies the one live source for the app.
private val LocalVeilTemporalContext = compositionLocalOf {
    VeilTemporalContext(VeilTemporalPhase.DAY, ZoneId.systemDefault())
}

@Composable
fun currentVeilTemporalPhase(): VeilTemporalPhase = LocalVeilTemporalContext.current.phase

@Composable
fun currentVeilZoneId(): ZoneId = LocalVeilTemporalContext.current.zoneId

/**
 * One foreground-only clock for all shell realms. System clock broadcasts replace polling,
 * per-screen timers and animation loops. The current zone is read anew on every refresh.
 */
@Composable
internal fun ProvideVeilTemporalPhase(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val temporalContext = remember { mutableStateOf(readLocalTemporalContext()) }

    DisposableEffect(context, lifecycle) {
        var registered = false
        fun refresh() {
            temporalContext.value = readLocalTemporalContext()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // A queued broadcast after ON_STOP must not update an inactive surface.
                if (registered) refresh()
            }
        }

        fun start() {
            if (registered) return
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            registered = true
            // Covers missed boundaries and clock/zone changes while backgrounded.
            refresh()
        }

        fun stop() {
            if (!registered) return
            registered = false
            context.unregisterReceiver(receiver)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose {
            lifecycle.removeObserver(observer)
            stop()
        }
    }

    CompositionLocalProvider(
        LocalVeilTemporalContext provides temporalContext.value,
        content = content
    )
}

private fun readLocalTemporalContext(): VeilTemporalContext {
    val zone = ZoneId.systemDefault()
    return VeilTemporalContext(
        phase = temporalPhaseAt(Instant.now(), zone),
        zoneId = zone
    )
}
