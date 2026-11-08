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
    companion object {
        const val PERMISSION_REQUEST_CODE = 901
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(web: WebView, desktop: Boolean) {
        val settings = web.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.javaScriptCanOpenWindowsAutomatically = true
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

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return if (uri.scheme == "http" || uri.scheme == "https") {
                    false
                } else {
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                    }
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

            override fun onReceivedSslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
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

            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                onFileChooser(callback, params.createIntent())
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                activity.runOnUiThread {
                    val resources = request.resources.toSet()
                    val permissions = mutableListOf<String>()
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) permissions += Manifest.permission.CAMERA
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) permissions += Manifest.permission.RECORD_AUDIO

                    if (permissions.isEmpty()) {
                        request.deny()
                        return@runOnUiThread
                    }

                    val missing = permissions.filter {
                        ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missing.isEmpty()) {
                        request.grant(request.resources)
                    } else {
                        activity.requestPermissions(missing.toTypedArray(), PERMISSION_REQUEST_CODE)
                        PendingPermission.request = request
                    }
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    PendingPermission.locationCallback = callback
                    PendingPermission.locationOrigin = origin
                    activity.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_CODE + 1
                    )
                }
            }
        }

        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    .setDescription("NEXA Browser download")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                activity.getSystemService(DownloadManager::class.java).enqueue(request)
            } catch (_: Exception) {
            }
        }
    }

    private fun desktopUserAgent(base: String): String {
        return base
            .replace("Mobile", "")
            .replace("Android", "X11; Linux x86_64")
    }

    object PendingPermission {
        var request: PermissionRequest? = null
        var locationCallback: GeolocationPermissions.Callback? = null
        var locationOrigin: String? = null
    }
}
