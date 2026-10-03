package com.veilreader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veilreader.app.ui.VeilApp
import com.veilreader.app.ui.reader.ReaderHardwareButton
import com.veilreader.app.ui.reader.ReaderHardwareButtonEvent
import com.veilreader.app.ui.reader.ReaderHardwareButtonPhase
import com.veilreader.app.ui.reader.ReaderHardwareKeyHost
import com.veilreader.app.ui.screens.ReaderFragmentRestoration
import com.veilreader.app.ui.settings.SettingsViewModel
import com.veilreader.app.ui.theme.VeilTheme

class MainActivity : FragmentActivity(), ReaderHardwareKeyHost {
    private var externalOpenUri by mutableStateOf<Uri?>(null)
    private var readerHardwareKeyOwnerId: String? = null
    private var readerHardwareKeyHandler: ((ReaderHardwareButtonEvent) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Readium navigator fragments require a custom factory during FragmentManager restore.
        supportFragmentManager.fragmentFactory = ReaderFragmentRestoration.fragmentFactory
        super.onCreate(savedInstanceState)
        ReaderFragmentRestoration.discardRestoredDummies(supportFragmentManager)

        enableEdgeToEdge()
        externalOpenUri = if (savedInstanceState == null) viewUriFrom(intent) else null

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
            VeilTheme(
                themeMode = appSettings.appThemeMode,
                highContrastEnabled = appSettings.highContrastEnabled
            ) {
                VeilApp(
                    externalOpenUri = externalOpenUri,
                    onExternalOpenUriConsumed = { externalOpenUri = null },
                    appSettings = appSettings,
                    onSetAppThemeMode = settingsViewModel::setAppThemeMode,
                    onSetHighContrastEnabled = settingsViewModel::setHighContrastEnabled,
                    onSaveReaderAppearance = settingsViewModel::saveReaderAppearance,
                    onSaveReaderTapGrid = settingsViewModel::saveReaderTapGrid,
                    onSaveReaderHardwareKeys = settingsViewModel::saveReaderHardwareKeys,
                    onSaveReaderFocusGuide = settingsViewModel::saveReaderFocusGuide,
                    onSaveReaderTtsSettings = settingsViewModel::saveReaderTtsSettings,
                    onSaveFixedLayoutSpread = settingsViewModel::saveFixedLayoutSpread,
                    onSaveSensorySettings = settingsViewModel::saveSensorySettings
                )
            }
        }
    }

    override fun installReaderHardwareKeyHandler(
        ownerId: String,
        handler: (ReaderHardwareButtonEvent) -> Boolean
    ) {
        readerHardwareKeyOwnerId = ownerId
        readerHardwareKeyHandler = handler
    }

    override fun clearReaderHardwareKeyHandler(ownerId: String) {
        if (readerHardwareKeyOwnerId != ownerId) return
        readerHardwareKeyOwnerId = null
        readerHardwareKeyHandler = null
    }

    override fun dispatchKeyEvent(event: AndroidKeyEvent): Boolean {
        val button = when (event.keyCode) {
            AndroidKeyEvent.KEYCODE_VOLUME_UP -> ReaderHardwareButton.VOLUME_UP
            AndroidKeyEvent.KEYCODE_VOLUME_DOWN -> ReaderHardwareButton.VOLUME_DOWN
            else -> null
        }
        val phase = when (event.action) {
            AndroidKeyEvent.ACTION_DOWN -> ReaderHardwareButtonPhase.DOWN
            AndroidKeyEvent.ACTION_UP -> ReaderHardwareButtonPhase.UP
            else -> null
        }

        if (button != null && phase != null) {
            val handled = readerHardwareKeyHandler?.invoke(
                ReaderHardwareButtonEvent(
                    button = button,
                    phase = phase,
                    eventTimeMs = event.eventTime,
                    repeatCount = event.repeatCount
                )
            ) == true
            if (handled) return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalOpenUri = viewUriFrom(intent)
    }

    private fun viewUriFrom(intent: Intent?): Uri? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
}
