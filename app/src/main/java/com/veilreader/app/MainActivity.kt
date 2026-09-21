package com.veilreader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.veilreader.app.manga.challenge.MangaChallengeHost
import com.veilreader.app.ui.VeilApp
import com.veilreader.app.ui.theme.VeilTheme

class MainActivity : FragmentActivity() {
    private var externalOpenUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        externalOpenUri = if (savedInstanceState == null) viewUriFrom(intent) else null

        setContent {
            VeilTheme {
                MangaChallengeHost {
                    VeilApp(
                        externalOpenUri = externalOpenUri,
                        onExternalOpenUriConsumed = { externalOpenUri = null }
                    )
                }
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
