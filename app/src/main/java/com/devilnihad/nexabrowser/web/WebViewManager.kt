package com.devilnihad.nexabrowser.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.*
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class WebViewManager(private val activity: Activity, private val onUrl: (String) -> Unit, private val onTitle: (String) -> Unit, private val onProgress: (Int) -> Unit, private val onNewTab: (String) -> Unit, private val onFileChooser: (ValueCallback<Array<Uri>>, Intent) -> Unit) {
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(web: WebView, desktop: Boolean) {
        val s=web.settings
        s.javaScriptEnabled=true; s.domStorageEnabled=true; s.databaseEnabled=true; s.loadsImagesAutomatically=true
        s.javaScriptCanOpenWindowsAutomatically=true; s.setSupportMultipleWindows(true); s.mediaPlaybackRequiresUserGesture=false
        s.allowFileAccess=false; s.allowContentAccess=true; s.builtInZoomControls=false; s.displayZoomControls=false
        s.setSupportZoom(true); s.textZoom=100; s.userAgentString=if(desktop) desktopUserAgent(s.userAgentString) else s.userAgentString
        if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) WebSettingsCompat.setForceDark(s, WebSettingsCompat.FORCE_DARK_AUTO)
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val req=DownloadManager.Request(Uri.parse(url)); req.setTitle(URLUtil.guessFileName(url,contentDisposition,mimeType)); req.setDescription("NEXA Browser download"); req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); req.setMimeType(mimeType); req.addRequestHeader("User-Agent",userAgent); activity.getSystemService(DownloadManager::class.java).enqueue(req)
        }
        web.webViewClient=object: WebViewClient(){
            override fun shouldOverrideUrlLoading(v:WebView, r:WebResourceRequest):Boolean { val u=r.url.toString(); return if(u.startsWith("http://")||u.startsWith("https://")){false}else{try{activity.startActivity(Intent(Intent.ACTION_VIEW,r.url));true}catch(_:Exception){true}} }
            override fun onPageFinished(v:WebView,url:String){onUrl(url);onTitle(v.title.orEmpty());}
            override fun onReceivedError(v:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame) v.loadUrl("file:///android_asset/error.html")}
        }
        web.webChromeClient=object: WebChromeClient(){
            override fun onProgressChanged(v:WebView,p:Int){onProgress(p)}
            override fun onReceivedTitle(v:WebView,t:String){onTitle(t)}
            override fun onCreateWindow(v:WebView,isDialog:Boolean,isUserGesture:Boolean,resultMsg:Message):Boolean { return false }
            override fun onShowFileChooser(v:WebView,callback:ValueCallback<Array<Uri>>,params:FileChooserParams):Boolean { onFileChooser(callback,params.createIntent()); return true }
        }
    }
    private fun desktopUserAgent(base:String):String=base.replace(Regex("Mobile;|Mobile"),"").replace("Android","X11; Linux x86_64")
}
