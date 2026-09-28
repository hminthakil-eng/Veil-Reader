package com.veilreader.app.ui.theme

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

data class VeilRuntimeQualitySignals(
    val lowRamDevice: Boolean,
    val powerSaveMode: Boolean,
    val thermalStatus: Int
)

/**
 * Product quality governor.
 *
 * Only optional atmosphere, ornament and expensive motion may degrade. Reading content,
 * accessibility, navigation, persistence and progression are never quality-tiered.
 */
fun veilQualityTierFor(signals: VeilRuntimeQualitySignals): VeilQualityTier = when {
    signals.lowRamDevice ||
        signals.thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE ->
        VeilQualityTier.ESSENTIAL

    signals.powerSaveMode ||
        signals.thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE ->
        VeilQualityTier.BALANCED

    else -> VeilQualityTier.FULL
}

val LocalVeilQualityTier = staticCompositionLocalOf { VeilQualityTier.FULL }

@Composable
fun rememberVeilQualityTier(): VeilQualityTier {
    val context = LocalContext.current.applicationContext
    val activityManager = remember(context) {
        context.getSystemService(ActivityManager::class.java)
    }
    val powerManager = remember(context) {
        context.getSystemService(PowerManager::class.java)
    }
    val lowRam = remember(activityManager) {
        activityManager?.isLowRamDevice == true
    }
    var powerSave by remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }
    var thermalStatus by remember(powerManager) {
        mutableIntStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            } else {
                PowerManager.THERMAL_STATUS_NONE
            }
        )
    }

    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiveContext: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    powerSave = powerManager?.isPowerSaveMode == true
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )

        val thermalListener =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
                PowerManager.OnThermalStatusChangedListener { status ->
                    thermalStatus = status
                }.also(powerManager::addThermalStatusListener)
            } else {
                null
            }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                powerManager != null &&
                thermalListener != null
            ) {
                powerManager.removeThermalStatusListener(thermalListener)
            }
        }
    }

    return veilQualityTierFor(
        VeilRuntimeQualitySignals(
            lowRamDevice = lowRam,
            powerSaveMode = powerSave,
            thermalStatus = thermalStatus
        )
    )
}
