package com.devilnihad.nexabrowser.data

import android.webkit.WebView

data class Tab(
    val id: Long,
    var url: String = "about:blank",
    var title: String = "New tab",
    val privateMode: Boolean = false,
    var webView: WebView? = null
)
