package com.example

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

@Composable
fun BrowserWebViewContainer(
    tabId: String,
    viewModel: BrowserViewModel,
    webViewMap: MutableMap<String, WebView>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val webView = remember(tabId) {
        webViewMap.getOrPut(tabId) {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setupWebView(this, tabId, viewModel)
                val tab = viewModel.uiState.value.tabs.find { it.id == tabId }
                val initialUrl = tab?.url ?: ""
                if (initialUrl.isNotEmpty()) {
                    loadUrl(initialUrl)
                }
            }
        }
    }

    LaunchedEffect(tabId) {
        viewModel.commands.collect { command ->
            when (command) {
                is BrowserCommand.LoadUrl -> {
                    if (command.tabId == tabId) {
                        webViewMap[tabId]?.loadUrl(command.url)
                    }
                }
                is BrowserCommand.GoBack -> {
                    if (command.tabId == tabId) {
                        val wv = webViewMap[tabId]
                        if (wv?.canGoBack() == true) {
                            wv.goBack()
                        }
                    }
                }
                is BrowserCommand.GoForward -> {
                    if (command.tabId == tabId) {
                        val wv = webViewMap[tabId]
                        if (wv?.canGoForward() == true) {
                            wv.goForward()
                        }
                    }
                }
                is BrowserCommand.Reload -> {
                    if (command.tabId == tabId) {
                        webViewMap[tabId]?.reload()
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            SwipeRefreshLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                (webView.parent as? ViewGroup)?.removeView(webView)
                addView(webView)
                
                setOnRefreshListener {
                    webView.reload()
                }
            }
        },
        update = { swipeRefreshLayout ->
            val currentChild = swipeRefreshLayout.getChildAt(0)
            if (currentChild != webView) {
                swipeRefreshLayout.removeAllViews()
                (webView.parent as? ViewGroup)?.removeView(webView)
                swipeRefreshLayout.addView(webView)
            }
            
            val tab = viewModel.uiState.value.tabs.find { it.id == tabId }
            swipeRefreshLayout.isRefreshing = tab?.isLoading == true && (tab.progress ?: 0) < 15
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun setupWebView(webView: WebView, tabId: String, viewModel: BrowserViewModel) {
    val isIncognito = viewModel.uiState.value.tabs.find { it.id == tabId }?.isIncognito == true
    webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = !isIncognito
        databaseEnabled = !isIncognito
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false
        setSupportZoom(true)
        allowFileAccess = !isIncognito
        allowContentAccess = !isIncognito
        cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        
        // Optimize User Agent slightly to avoid desktop redirects or old browser screens
        userAgentString = userAgentString.replace("Version/", "")
    }

    if (isIncognito) {
        webView.clearCache(true)
        webView.clearHistory()
    }

    webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
        viewModel.startDownload(url, userAgent, contentDisposition, mimetype, contentLength)
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            viewModel.updateTabLoadingState(tabId, true)
            viewModel.updateTabUrlAndFlags(
                tabId = tabId,
                url = url,
                title = view.title ?: "Loading...",
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward()
            )
            injectGlassTheme(view)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            viewModel.updateTabLoadingState(tabId, false)
            viewModel.updateTabUrlAndFlags(
                tabId = tabId,
                url = url,
                title = view.title,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward()
            )
            injectGlassTheme(view)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            if (request.isForMainFrame) {
                viewModel.updateTabError(tabId, error.description.toString())
            }
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            viewModel.updateTabProgress(tabId, newProgress)
            if (newProgress > 25) {
                injectGlassTheme(view)
            }
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            super.onReceivedTitle(view, title)
            viewModel.updateTabUrlAndFlags(
                tabId = tabId,
                url = view.url ?: "",
                title = title,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward()
            )
        }
    }
}

private fun injectGlassTheme(view: WebView) {
    val js = """
        (function() {
            var css = 'html { background: transparent !important; } ' +
                      'body { background: rgba(12, 11, 18, 0.45) !important; backdrop-filter: blur(15px) !important; -webkit-backdrop-filter: blur(15px) !important; } ' +
                      '#viewport, #gsr, #cnt, .sfbg, .sfg, #header, footer, .footer, .header, .gws-local-homepage__footer, .main, div[role="navigation"] { background: transparent !important; background-color: transparent !important; }';
            
            var style = document.getElementById('aura-glass-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'aura-glass-style';
                style.type = 'text/css';
                var target = document.head || document.documentElement;
                if (target) {
                    target.appendChild(style);
                }
            }
            if (style) {
                style.innerHTML = css;
            }
        })();
    """.trimIndent()
    view.evaluateJavascript(js, null)
}

