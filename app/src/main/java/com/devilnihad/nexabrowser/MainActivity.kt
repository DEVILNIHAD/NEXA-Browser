package com.devilnihad.nexabrowser

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.devilnihad.nexabrowser.data.*
import com.devilnihad.nexabrowser.utils.UrlResolver
import com.devilnihad.nexabrowser.web.WebViewManager

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout; private lateinit var web: WebView; private lateinit var address: TextInputEditText; private lateinit var progress: ProgressBar
    private val tabs=TabManager(); private lateinit var history:HistoryManager; private lateinit var bookmarks:BookmarkManager; private lateinit var settings:SettingsManager
    private var privateMode=false; private var fileCallback: android.webkit.ValueCallback<Array<Uri>>?=null
    private var pendingWebPermission: android.webkit.PermissionRequest?=null
    private var pendingWebPermissionName: String?=null
    private val filePicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ r->fileCallback?.onReceiveValue(if(r.resultCode==Activity.RESULT_OK) arrayOf(r.data?.data ?: Uri.EMPTY) else null); fileCallback=null }

    override fun onCreate(state:Bundle?){super.onCreate(state); WindowCompat.setDecorFitsSystemWindows(window,false); history=HistoryManager(this);bookmarks=BookmarkManager(this);settings=SettingsManager(this); buildUi(); tabs.create(); loadHome();
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){if(web.canGoBack())web.goBack()else if(tabs.all().size>1){tabs.current()?.id?.let{tabs.close(it)};showCurrent()}else finish()}})
    }
    private fun buildUi(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(10,28,10,8);setBackgroundColor(Color.TRANSPARENT)}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        address=TextInputEditText(this).apply{hint=getString(R.string.search_hint);singleLine=true;setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));setPadding(18,0,18,0)}
        top.addView(address,LinearLayout.LayoutParams(0,56.dp(),1).apply{setMargins(0,0,8.dp(),0)})
        val menu=MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle).apply{text="⋮";minWidth=48.dp();setOnClickListener{showMenu()}}
        top.addView(menu,LinearLayout.LayoutParams(52.dp(),56.dp()))
        progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;visibility=View.GONE}
        root.addView(top);root.addView(progress,LinearLayout.LayoutParams(-1,2.dp()))
        web=WebView(this);root.addView(web,LinearLayout.LayoutParams(-1,0,1f));
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER;orientation=LinearLayout.HORIZONTAL}
        fun btn(label:String,click:()->Unit)=MaterialButton(this).apply{text=label;minWidth=48.dp();setOnClickListener{click()}}
        bar.addView(btn("‹"){if(web.canGoBack())web.goBack()});bar.addView(btn("›"){if(web.canGoForward())web.goForward()});bar.addView(btn("+"){newTab(false)});bar.addView(btn("▣"){showTabs()});bar.addView(btn("⌂"){loadHome()})
        root.addView(bar,LinearLayout.LayoutParams(-1,60.dp()));setContentView(root)
        WebViewManager(this,{u->address.setText(u);history.add(web.title.orEmpty(),u)},{}, {p->progress.visibility=if(p<100)View.VISIBLE else View.GONE;progress.progress=p},{u->newTab(false,u)},{cb,intent->fileCallback=cb;filePicker.launch(intent)}).configure(web,settings.desktop)
        address.setOnEditorActionListener{_,_,_->loadInput();true}
    }
    private fun loadHome(){web.loadDataWithBaseURL(null,homeHtml(),"text/html","UTF-8",null);address.setText("")}
    private fun loadInput(){web.loadUrl(UrlResolver.resolve(address.text?.toString().orEmpty(),settings.searchEngine))}
    private fun newTab(privateMode:Boolean,url:String="about:blank"){this.privateMode=privateMode;tabs.create(privateMode).also{it.url=url}; if(url=="about:blank")loadHome()else web.loadUrl(url)}
    private fun showCurrent(){tabs.current()?.let{privateMode=it.privateMode; if(it.url=="about:blank")loadHome()else web.loadUrl(it.url)}}
    private fun showTabs(){val d=BottomSheetDialog(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,40)};box.addView(TextView(this).apply{text="Tabs   +";textSize=22f;setPadding(0,0,0,18);setOnClickListener{d.dismiss();newTab(false)}});for(t in tabs.all()){val b=MaterialButton(this).apply{text="${t.title.ifBlank{"New tab"}}\n${t.url}";gravity=Gravity.START;setOnClickListener{tabs.select(t.id);d.dismiss();showCurrent()}};box.addView(b)};d.setContentView(box);d.show()}
    private fun showMenu(){val d=BottomSheetDialog(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,40)};val items=listOf("New tab","New private tab","Bookmarks","History","Downloads","Find in page","Desktop site","Share","Add bookmark","Settings");items.forEach{label->val b=MaterialButton(this).apply{text=label;setOnClickListener{d.dismiss();when(label){"New tab"->newTab(false);"New private tab"->newTab(true);"Bookmarks"->showBookmarks();"History"->showHistory();"Downloads"->openDownloads();"Find in page"->findPage();"Desktop site"->{settings.desktop=!settings.desktop;web.settings.userAgentString=if(settings.desktop) web.settings.userAgentString.replace(Regex("Mobile;|Mobile"),"") else android.webkit.WebSettings.getDefaultUserAgent(this);web.reload()};"Share"->share();"Add bookmark"->bookmarks.add(web.title.orEmpty(),web.url.orEmpty());"Settings"->showSettings()}}};box.addView(b)};d.setContentView(box);d.show()}
    private fun showBookmarks(){val list=bookmarks.list();MaterialAlertDialogBuilder(this).setTitle("Bookmarks").setItems(list.map{it.title.ifBlank{it.url}}.toTypedArray()){_,w->web.loadUrl(list[w].url)}.setNegativeButton("Close",null).show()}
    private fun showHistory(){val list=history.list();MaterialAlertDialogBuilder(this).setTitle("History").setItems(list.map{it.title.ifBlank{it.url}}.toTypedArray()){_,w->web.loadUrl(list[w].url)}.setNeutralButton("Clear",null).show()}
    private fun findPage(){val e=TextInputEditText(this);e.hint="Find in page";MaterialAlertDialogBuilder(this).setTitle("Find in page").setView(e).setPositiveButton("Find"){_,_->web.findAllAsync(e.text?.toString().orEmpty())}.show()}
    private fun share(){startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,web.url)},"Share page"))}
    private fun openDownloads(){startActivity(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS))}
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray){ super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==900 && grantResults.all{it==PackageManager.PERMISSION_GRANTED}) pendingWebPermission?.grant(pendingWebPermission?.resources ?: emptyArray()) else if(requestCode==900) pendingWebPermission?.deny(); pendingWebPermission=null }
    private fun showSettings(){val e=TextInputEditText(this).apply{setText(settings.searchEngine)};MaterialAlertDialogBuilder(this).setTitle("Settings").setMessage("Search engine URL\nTheme follows Android system. Privacy data stays on device.").setView(e).setPositiveButton("Save"){_,_->settings.searchEngine=e.text.toString()}.setNegativeButton("Clear browsing data"){_,_->CookieManager.getInstance().removeAllCookies(null);web.clearCache(true);history.clear()}.show()}
    private fun homeHtml()="""<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>body{font-family:sans-serif;background:#f7f7fb;color:#15151c;padding:36px 22px}h1{font-size:42px;margin:24px 0 4px}p{opacity:.7}.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-top:30px}.s{padding:18px;border-radius:18px;background:white;text-align:center} @media(prefers-color-scheme:dark){body{background:#0b0b10;color:#fff}.s{background:#17171f}}</style></head><body><h1>NEXA</h1><p>Fast. Private. Yours.</p><div class='grid'><div class='s'>Google</div><div class='s'>YouTube</div><div class='s'>GitHub</div><div class='s'>Wikipedia</div></div></body></html>"""
    private fun resolveColor(attr:Int)=com.google.android.material.color.MaterialColors.getColor(this,attr,Color.WHITE)
    private fun Int.dp()=(this*resources.displayMetrics.density).toInt()
}
