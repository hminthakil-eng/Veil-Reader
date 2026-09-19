package com.veilreader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.AppThemeMode
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.ui.VeilApp
import com.veilreader.app.ui.theme.VeilTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var externalOpenUri by mutableStateOf<Uri?>(null)
    private val settingsStore by lazy {
        SettingsStore(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        externalOpenUri = if (savedInstanceState == null) viewUriFrom(intent) else null

        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.appThemeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            VeilTheme(darkTheme = darkTheme) {
                VeilApp(
                    externalOpenUri = externalOpenUri,
                    onExternalOpenUriConsumed = { externalOpenUri = null },
                    appThemeMode = settings.appThemeMode,
                    onAppThemeModeChange = { mode ->
                        scope.launch {
                            settingsStore.setAppThemeMode(mode)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalOpenUri = viewUriFrom(intent)
    }

    private fun viewUriFrom(intent: Intent?): Uri? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
}
