package eu.kanade.presentation.webview

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun WebViewScreenContent(
    onNavigateUp: () -> Unit,
    initialTitle: String?,
    url: String,
    headers: Map<String, String> = emptyMap(),
    onUrlChange: (String) -> Unit = {},
    onShare: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onClearCookies: (String) -> Unit,
) {
    val state = rememberWebViewState(url = url, additionalHttpHeaders = headers)
    val navigator = rememberWebViewNavigator()

    Scaffold(
        topBar = {
            Box {
                AppBar(
                    title = state.pageTitle ?: initialTitle,
                    subtitle = state.lastLoadedUrl,
                    navigateUp = onNavigateUp,
                    navigationIcon = Icons.Outlined.Close,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(R.string.action_webview_back),
                                    icon = Icons.Outlined.ArrowBack,
                                    onClick = {
                                        if (navigator.canGoBack) {
                                            navigator.navigateBack()
                                        }
                                    },
                                    enabled = navigator.canGoBack,
                                ),
                                AppBar.Action(
                                    title = stringResource(R.string.action_webview_forward),
                                    icon = Icons.Outlined.ArrowForward,
                                    onClick = {
                                        if (navigator.canGoForward) {
                                            navigator.navigateForward()
                                        }
                                    },
                                    enabled = navigator.canGoForward,
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(R.string.action_webview_refresh),
                                    onClick = { navigator.reload() },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(R.string.action_share),
                                    onClick = { onShare(state.lastLoadedUrl!!) },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(R.string.action_open_in_browser),
                                    onClick = { onOpenInBrowser(state.lastLoadedUrl!!) },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(R.string.pref_clear_cookies),
                                    onClick = { onClearCookies(state.lastLoadedUrl!!) },
                                ),
                            ),
                        )
                    },
                )
                when (val loadingState = state.loadingState) {
                    is LoadingState.Initializing -> LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )
                    is LoadingState.Loading -> LinearProgressIndicator(
                        progress = (loadingState as? LoadingState.Loading)?.progress ?: 1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )
                    else -> {}
                }
            }
        },
    ) { contentPadding ->
        val webClient = remember {
            object : AccompanistWebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { onUrlChange(it) }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    request?.let {
                        // Don't attempt to open blobs as webpages
                        if (it.url.toString().startsWith("blob:http")) {
                            return false
                        }

                        // Continue with request, but with custom headers
                        view?.loadUrl(it.url.toString(), headers)
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        WebView(
            state = state,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            navigator = navigator,
            onCreated = { webView ->
                webView.setDefaultSettings()

                // Debug mode (chrome://inspect/#devices)
                if (BuildConfig.DEBUG &&
                    0 != webView.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
                ) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }

                headers["user-agent"]?.let {
                    webView.settings.userAgentString = it
                }
            },
            client = webClient,
        )
    }
}
