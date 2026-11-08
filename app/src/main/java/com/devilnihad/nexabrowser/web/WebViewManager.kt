package com.devilnihad.nexabrowser.web

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class WebViewManager(
    private val activity: Activity,
    private val onUrl: (String) -> Unit,
    private val onTitle: (String) -> Unit,
    private val onProgress: (Int) -> Unit,
    private val onNewTab: (String) -> Unit,
    private val onFileChooser: (ValueCallback<Array<Uri>>?) -> Unit
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(web: WebView, desktop: Boolean) {
        val webSettings = web.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadsImagesAutomatically = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.mediaPlaybackRequiresUserGesture = true
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = true
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)
        webSettings.textZoom = 100
        webSettings.userAgentString = if (desktop) desktopUserAgent(webSettings.userAgentString) else webSettings.userAgentString
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_AUTO)
        }

        web.setDownloadListener(DownloadListener { url, _, contentDisposition, mimeType, _ ->
            if (url.isNullOrBlank()) return@DownloadListener
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                setDescription("NEXA Browser download")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        })

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.startsWith("http://") || url.startsWith("https://")) return false
                return try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } catch (_: Exception) {
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                onUrl(url)
                onTitle(view.title.orEmpty())
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadDataWithBaseURL(
                        null,
                        "<html><body><h3>Unable to load page</h3><p>${error.description}</p></body></html>",
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgress(newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                onTitle(title)
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                if (!isUserGesture) return false
                onNewTab(view.url.orEmpty())
                return false
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                onFileChooser(filePathCallback)
                return true
            }
        }
    }

    private fun desktopUserAgent(base: String): String =
        base.replace(Regex("Mobile;|Mobile"), "").replace("Android", "X11; Linux x86_64")
}
