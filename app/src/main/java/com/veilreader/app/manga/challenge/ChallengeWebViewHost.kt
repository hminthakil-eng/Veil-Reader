package com.veilreader.app.manga.challenge

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MangaChallengeHost(
    modifier: Modifier = Modifier,
    runtime: MangaChallengeRuntime = MangaChallengeRuntime,
    content: @Composable () -> Unit
) {
    RegisterChallengeForegroundHost(runtime.hostRegistry)
    val session by runtime.sessions.session.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        content()
        session?.let { active ->
            key(active.id) {
                ChallengeBrowserOverlay(
                    session = active,
                    sessions = runtime.sessions,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun RegisterChallengeForegroundHost(
    registry: ChallengeForegroundHostRegistry
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val token = remember { Any() }

    DisposableEffect(lifecycle, registry, token) {
        var registration: ChallengeForegroundHostRegistry.Registration? = null

        fun register() {
            if (registration == null) registration = registry.register(token)
        }

        fun unregister() {
            registration?.let(registry::unregister)
            registration = null
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) register()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> unregister()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            unregister()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ChallengeBrowserOverlay(
    session: AndroidChallengeBrowserSession,
    sessions: AndroidChallengeSessionStore,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember(session.id) { mutableStateOf<WebView?>(null) }
    var loading by remember(session.id) { mutableStateOf(true) }
    var errorMessage by remember(session.id) { mutableStateOf<String?>(null) }

    DisposableEffect(session.id) {
        onDispose {
            webViewRef?.let { webView ->
                sessions.saveWebViewState(
                    session.id,
                    ChallengeWebViewStateCodec.capture(webView)
                )
                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.removeAllViews()
                webView.destroy()
            }
            webViewRef = null
        }
    }

    Surface(
        modifier = modifier,
        tonalElevation = 8.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Source verification",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = session.request.source.displayName + " · " + session.request.domain,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Complete the verification in the page below. Veil will reuse only the temporary session cookies and user-agent for this source.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewRef = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)
                            settings.mediaPlaybackRequiresUserGesture = true

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    loading = true
                                    errorMessage = null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest
                                ): Boolean {
                                    if (!request.isForMainFrame) return false
                                    return !ChallengeUrlPolicy.isAllowedTopLevelNavigation(
                                        session.request.domain,
                                        request.url.toString()
                                    )
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler,
                                    error: SslError?
                                ) {
                                    handler.cancel()
                                    sessions.complete(
                                        session.id,
                                        AndroidChallengeBrowserResult(ChallengeUiResult.FAILED)
                                    )
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest,
                                    error: WebResourceError
                                ) {
                                    if (request.isForMainFrame) {
                                        loading = false
                                        errorMessage = error.description?.toString()
                                            ?: "Verification page could not be loaded."
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    sessions.complete(
                                        session.id,
                                        AndroidChallengeBrowserResult(ChallengeUiResult.FAILED)
                                    )
                                    view?.destroy()
                                    return true
                                }
                            }

                            val restored = ChallengeWebViewStateCodec.restore(
                                this,
                                session.restoredWebViewState
                            )
                            if (!restored) loadUrl(session.startUrl)
                        }
                    }
                )

                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                errorMessage?.let { message ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(message, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                errorMessage = null
                                loading = true
                                webViewRef?.reload()
                            }) {
                                Text("Reload")
                            }
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        sessions.complete(
                            session.id,
                            AndroidChallengeBrowserResult(ChallengeUiResult.CANCELLED)
                        )
                    }
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val webView = webViewRef ?: return@Button
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.flush()
                        val currentUrl = webView.url ?: session.startUrl
                        val cookie = cookieManager.getCookie(currentUrl)
                            ?: cookieManager.getCookie(session.startUrl)
                        val userAgent = webView.settings.userAgentString
                        sessions.complete(
                            session.id,
                            AndroidChallengeBrowserResult(
                                uiResult = ChallengeUiResult.SOLVED,
                                cookieHeader = cookie,
                                userAgent = userAgent
                            )
                        )
                    }
                ) {
                    Text("Verification complete")
                }
            }
        }
    }
}