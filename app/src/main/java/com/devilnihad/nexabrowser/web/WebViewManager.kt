package com.devilnihad.nexabrowser.web

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Message
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class WebViewManager(
    private val activity: Activity,
    private val privateMode: Boolean,
    private val onUrl: (String) -> Unit,
    private val onTitle: (String) -> Unit,
    private val onProgress: (Int) -> Unit,
    private val onFileChooser: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(web: WebView, desktop: Boolean) {
        val settings = web.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = !privateMode
        settings.loadsImagesAutomatically = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.mediaPlaybackRequiresUserGesture = true
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.textZoom = 100
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString = if (desktop) desktopUserAgent(settings.userAgentString) else settings.userAgentString

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_AUTO)
        }

        if (privateMode) {
            web.clearHistory()
            web.clearCache(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "http" || uri.scheme == "https") return false
                return try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
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
                    view.loadUrl("file:///android_asset/error.html")
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: android.webkit.SslErrorHandler,
                error: android.net.http.SslError
            ) {
                handler.cancel()
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                onProgress(progress)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                onTitle(title)
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean = false

            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                onFileChooser(callback, params.createIntent())
                return true
            }

            // Web-origin camera, microphone and location requests are denied until the app
            // has a complete runtime-permission consent flow.
            override fun onPermissionRequest(request: PermissionRequest) {
                request.deny()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, false, false)
            }
        }

        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.isNullOrBlank()) return@setDownloadListener
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    .setDescription("NEXA Browser download")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                val manager = activity.getSystemService(DownloadManager::class.java)
                manager?.enqueue(request)
            } catch (_: Exception) {
                // Ignore malformed download links or unavailable system download service.
            }
        }
    }

    private fun desktopUserAgent(base: String): String =
        base.replace("Mobile", "").replace("Android", "X11; Linux x86_64")
}
