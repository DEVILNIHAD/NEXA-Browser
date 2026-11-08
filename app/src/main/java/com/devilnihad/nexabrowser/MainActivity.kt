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
    private val filePicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ r->fileCallback?.onReceiveValue(if(r.resultCode==Activity.RESULT_OK) arrayOf(r.data?.data ?: return@registerForActivityResult) else null) }

    override fun onCreate(state:Bundle?){super.onCreate(state); WindowCompat.setDecorFitsSystemWindows(window,false); history=HistoryManager(this);bookmarks=BookmarkManager(this);settings=SettingsManager(this); buildUi(); loadHome(); }
    private fun buildUi(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(10,28,10,8);setBackgroundColor(Color.TRANSPARENT)}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        address=TextInputEditText(this).apply{hint=getString(R.string.search_hint);setSingleLine(true);setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));setPadding(18,0,18,0)}
        top.addView(address,LinearLayout.LayoutParams(0,56.dp(),1f).apply{setMargins(0,0,8.dp(),0)})
        val menu=MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle).apply{text="⋮";minWidth=48.dp();setOnClickListener{showMenu()}}
        top.addView(menu,LinearLayout.LayoutParams(52.dp(),56.dp()))
        progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;visibility=View.GONE}
        root.addView(top);root.addView(progress,LinearLayout.LayoutParams(-1,2.dp()))
        web=WebView(this);root.addView(web,LinearLayout.LayoutParams(-1,0,1f));
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER;orientation=LinearLayout.HORIZONTAL}
        fun btn(label:String,click:()->Unit)=MaterialButton(this).apply{text=label;minWidth=48.dp();setOnClickListener{click()}}
        bar.addView(btn("‹"){if(web.canGoBack())web.goBack()});bar.addView(btn("›"){if(web.canGoForward())web.goForward()});bar.addView(btn("+"){newTab(false)});bar.addView(btn("▣"){showTabs()});
        root.addView(bar,LinearLayout.LayoutParams(-1,60.dp()));setContentView(root)
        WebViewManager(this,{u->address.setText(u);history.add(web.title.orEmpty(),u)},{}, {p->progress.visibility=if(p<100)View.VISIBLE else View.GONE;progress.progress=p},{u->newTab(false,u)},{c->onFileChooser(c)})
        address.setOnEditorActionListener{_,_,_->loadInput();true}
    }
    private fun loadHome(){web.loadDataWithBaseURL(null,homeHtml(),"text/html","UTF-8",null);address.setText("")}
    private fun loadInput(){web.loadUrl(UrlResolver.resolve(address.text?.toString().orEmpty(),settings.searchEngine))}
    private fun newTab(privateMode:Boolean,url:String="about:blank"){this.privateMode=privateMode;tabs.create(privateMode).also{it.url=url}; if(url=="about:blank")loadHome()else web.loadUrl(url)}
    private fun showCurrent(){tabs.current()?.let{privateMode=it.privateMode; if(it.url=="about:blank")loadHome()else web.loadUrl(it.url)}}
    private fun showTabs(){val d=BottomSheetDialog(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,40)};box.addView(TextView(this).apply{text="Tabs";textSize=18f;setTypeface(typeface,android.graphics.Typeface.BOLD)}); tabs.all().forEach{ val item=MaterialButton(this).apply{text=it.url.ifBlank {"New tab"};setOnClickListener{tabs.select(it.id);showCurrent();d.dismiss()}}; box.addView(item) }; d.setContentView(box); d.show() }
    private fun showMenu(){val d=BottomSheetDialog(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,40)};val items=listOf("New tab","New private tab","Bookmarks","History","Find in page","Settings","Share","Downloads"); items.forEachIndexed { index, item -> box.addView(MaterialButton(this).apply{text=item;setOnClickListener{d.dismiss(); when(index){0->newTab(false);1->newTab(true);2->showBookmarks();3->showHistory();4->findPage();5->showSettings();6->share();7->openDownloads()}}}) } ; d.setContentView(box); d.show() }
    private fun showBookmarks(){val list=bookmarks.list();MaterialAlertDialogBuilder(this).setTitle("Bookmarks").setItems(list.map{it.title.ifBlank{it.url}}.toTypedArray()){_,w->web.loadUrl(list[w].url)}.show()}
    private fun showHistory(){val list=history.list();MaterialAlertDialogBuilder(this).setTitle("History").setItems(list.map{it.title.ifBlank{it.url}}.toTypedArray()){_,w->web.loadUrl(list[w].url)}.show()}
    private fun findPage(){val e=TextInputEditText(this);e.hint="Find in page";MaterialAlertDialogBuilder(this).setTitle("Find in page").setView(e).setPositiveButton("Find"){_,_->web.findAllAsync(e.text.toString())}.setNegativeButton("Close",null).show()}
    private fun share(){startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,web.url)},"Share page"))}
    private fun openDownloads(){startActivity(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS))}
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray){ super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==1001 && grantResults.all{it==PackageManager.PERMISSION_GRANTED}) pendingWebPermission?.grant(pendingWebPermission?.resources?.map { it.resourceId }?.toTypedArray()) }
    private fun showSettings(){val e=TextInputEditText(this).apply{setText(settings.searchEngine)};MaterialAlertDialogBuilder(this).setTitle("Settings").setMessage("Search engine URL\nTheme follows system").setView(e).setPositiveButton("Save"){_,_->settings.searchEngine=e.text?.toString().orEmpty(); settings.save()}.setNegativeButton("Cancel",null).show()}
    private fun onFileChooser(callback: android.webkit.ValueCallback<Array<Uri>>?) { fileCallback = callback; val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }; filePicker.launch(intent) }
    private fun homeHtml()="""<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>body{font-family:sans-serif;background:#f7f7fb;color:#15151c;padding:24px}a{color:#1e88e5}h1{font-size:28px;margin-bottom:8px}p{font-size:16px;color:#3a3a3a}</style></head><body><h1>NEXA Browser</h1><p>Fast. Private. Yours.</p></body></html>"""
    private fun resolveColor(attr:Int)=com.google.android.material.color.MaterialColors.getColor(this,attr,Color.WHITE)
    private fun Int.dp()=(this*resources.displayMetrics.density).toInt()
}
