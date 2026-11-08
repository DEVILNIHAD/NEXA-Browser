package com.devilnihad.nexabrowser

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.ColorStateList
import androidx.core.view.WindowCompat
import com.devilnihad.nexabrowser.data.BookmarkManager
import com.devilnihad.nexabrowser.data.HistoryManager
import com.devilnihad.nexabrowser.data.SettingsManager
import com.devilnihad.nexabrowser.data.Tab
import com.devilnihad.nexabrowser.data.TabManager
import com.devilnihad.nexabrowser.utils.UrlResolver
import com.devilnihad.nexabrowser.web.WebViewManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var webContainer: FrameLayout
    private lateinit var address: EditText
    private lateinit var progress: View
    private lateinit var history: HistoryManager
    private lateinit var bookmarks: BookmarkManager
    private lateinit var settings: SettingsManager

    private val tabs = TabManager()
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = fileCallback
        fileCallback = null
        val uri = result.data?.data
        callback?.onReceiveValue(if (result.resultCode == Activity.RESULT_OK && uri != null) arrayOf(uri) else null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        history = HistoryManager(this)
        bookmarks = BookmarkManager(this)
        settings = SettingsManager(this)
        applyTheme()
        buildUi()

        val restoredUrls = savedInstanceState?.getStringArrayList(STATE_TAB_URLS)
        val restoredCurrent = savedInstanceState?.getInt(STATE_CURRENT_TAB, 0) ?: 0
        if (restoredUrls.isNullOrEmpty()) createTab(false)
        else {
            restoredUrls.forEach { createTab(false, it) }
            tabs.all().getOrNull(restoredCurrent)?.id?.let(::switchTo)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = tabs.current()
                val currentWeb = current?.webView
                when {
                    currentWeb?.canGoBack() == true -> currentWeb.goBack()
                    current != null && tabs.size() > 1 -> closeTab(current.id)
                    else -> finish()
                }
            }
        })
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
            setPadding(dp(12), dp(30), dp(12), dp(10))
        }

        root.addView(buildTopBar(), LinearLayout.LayoutParams(-1, dp(60)))

        progress = View(this).apply {
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorPrimary))
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(2)))

        webContainer = FrameLayout(this).apply {
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        }
        root.addView(webContainer, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buildBottomBar(), LinearLayout.LayoutParams(-1, dp(72)))
        setContentView(root)

        address.setOnEditorActionListener { _, _, _ -> loadAddress(); true }
    }

    private fun buildTopBar(): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val shell = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(6), 0)
            background = rounded(resolveColor(com.google.android.material.R.attr.colorSurfaceContainerHigh), 22)
            elevation = dp(2).toFloat()
        }

        val secure = ImageButton(this).apply {
            setImageResource(R.drawable.ic_lock)
            background = null
            contentDescription = "Security"
            imageTintList = ColorStateList.valueOf(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        shell.addView(secure, LinearLayout.LayoutParams(dp(34), dp(46)))

        address = EditText(this).apply {
            hint = getString(R.string.search_hint)
            setSingleLine(true)
            textSize = 15f
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            background = null
            setPadding(dp(4), 0, dp(4), 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
        }
        shell.addView(address, LinearLayout.LayoutParams(0, -1, 1f))

        val refresh = iconButton(R.drawable.ic_refresh, "Refresh") { currentWeb()?.reload() }
        shell.addView(refresh, LinearLayout.LayoutParams(dp(44), dp(46)))

        row.addView(shell, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(0, 0, dp(8), 0) })
        row.addView(iconButton(R.drawable.ic_more, "Menu") { showMenu() }, LinearLayout.LayoutParams(dp(48), dp(52)))
        return row
    }

    private fun buildBottomBar(): View {
        val outer = FrameLayout(this)
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = rounded(resolveColor(com.google.android.material.R.attr.colorSurfaceContainer), 28)
            elevation = dp(5).toFloat()
        }

        fun nav(icon: Int, desc: String, action: () -> Unit): ImageButton = iconButton(icon, desc, 52, action)
        bar.addView(nav(R.drawable.ic_back, "Back") { currentWeb()?.let { if (it.canGoBack()) it.goBack() } })
        bar.addView(nav(R.drawable.ic_forward, "Forward") { currentWeb()?.let { if (it.canGoForward()) it.goForward() } })
        bar.addView(nav(R.drawable.ic_home, "Home") { loadHome() })

        val tabButton = nav(R.drawable.ic_tabs, "Tabs") { showTabs() }
        bar.addView(tabButton)
        bar.addView(nav(R.drawable.ic_plus, "New tab") { createTab(false) })

        outer.addView(bar, FrameLayout.LayoutParams(-1, dp(58)).apply {
            gravity = Gravity.CENTER
            leftMargin = dp(2); rightMargin = dp(2)
        })
        return outer
    }

    private fun iconButton(icon: Int, desc: String, size: Int = 48, action: () -> Unit): ImageButton = ImageButton(this).apply {
        setImageResource(icon)
        background = rounded(Color.TRANSPARENT, 18)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        contentDescription = desc
        imageTintList = ColorStateList.valueOf(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(48))
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun createTab(privateMode: Boolean, url: String = "about:home") {
        val tab = tabs.create(privateMode)
        val web = WebView(this)
        tab.webView = web
        webContainer.addView(web, FrameLayout.LayoutParams(-1, -1))
        configureWebView(tab)
        switchTo(tab.id)
        if (url == "about:home") loadHome() else web.loadUrl(url)
    }

    private fun configureWebView(tab: Tab) {
        val web = tab.webView ?: return
        WebViewManager(
            activity = this,
            privateMode = tab.privateMode,
            onUrl = { url ->
                tab.url = url
                if (tabs.currentId == tab.id) address.setText(if (url.startsWith("about:")) "" else url)
                if (!tab.privateMode) history.add(tab.title, url)
            },
            onTitle = { title -> tab.title = title.ifBlank { "New tab" } },
            onProgress = { value ->
                if (tabs.currentId == tab.id) {
                    progress.visibility = if (value in 1..99) View.VISIBLE else View.GONE
                    progress.scaleX = value / 100f
                }
            },
            onFileChooser = { callback, intent -> openFileChooser(callback, intent) }
        ).configure(web, settings.desktopSite)
    }

    private fun switchTo(id: Long) {
        if (!tabs.select(id)) return
        tabs.all().forEach { tab -> tab.webView?.visibility = if (tab.id == id) View.VISIBLE else View.GONE }
        val tab = tabs.current() ?: return
        address.setText(if (tab.url == "about:home" || tab.url == "about:blank") "" else tab.url)
        progress.visibility = View.GONE
    }

    private fun currentWeb(): WebView? = tabs.current()?.webView

    private fun loadHome() {
        val dark = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bg = if (dark) "#0B0D12" else "#F7F8FC"
        val card = if (dark) "#151821" else "#FFFFFF"
        val text = if (dark) "#F7F8FA" else "#171923"
        val muted = if (dark) "#9BA1AF" else "#707583"
        val html = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
            *{box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:$bg;color:$text;margin:0;padding:48px 22px 40px}
            .wrap{max-width:720px;margin:auto}.brand{font-size:48px;font-weight:900;letter-spacing:-3px}.tag{color:$muted;font-size:14px;margin-top:4px}
            .search{margin-top:30px;background:$card;border:1px solid rgba(127,127,127,.12);border-radius:25px;padding:16px 18px;box-shadow:0 8px 30px rgba(0,0,0,.06)}
            .search span{color:$muted}.section{margin:32px 0 13px;font-size:13px;font-weight:800;letter-spacing:.5px;color:$muted;text-transform:uppercase}
            .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.item{display:block;padding:18px;border-radius:22px;background:$card;text-decoration:none;color:$text;border:1px solid rgba(127,127,127,.10)}
            .dot{width:10px;height:10px;border-radius:50%;background:#6C63FF;display:inline-block;margin-right:8px}.name{font-weight:750}.url{font-size:12px;color:$muted;margin-top:6px}
            </style></head><body><div class="wrap"><div class="brand">NEXA</div><div class="tag">Fast. Private. Yours.</div>
            <div class="search"><span>Search the web or enter an address</span></div>
            <div class="section">Quick access</div><div class="grid">
            <a class="item" href="https://www.google.com"><div class="name"><span class="dot"></span>Google</div><div class="url">google.com</div></a>
            <a class="item" href="https://www.youtube.com"><div class="name"><span class="dot"></span>YouTube</div><div class="url">youtube.com</div></a>
            <a class="item" href="https://github.com"><div class="name"><span class="dot"></span>GitHub</div><div class="url">github.com</div></a>
            <a class="item" href="https://wikipedia.org"><div class="name"><span class="dot"></span>Wikipedia</div><div class="url">wikipedia.org</div></a>
            </div></div></body></html>
        """.trimIndent()
        val tab = tabs.current() ?: return
        tab.url = "about:home"
        tab.title = "NEXA"
        tab.webView?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        address.setText("")
    }

    private fun loadAddress() {
        val target = UrlResolver.resolve(address.text?.toString().orEmpty(), settings.searchEngine)
        tabs.current()?.url = target
        currentWeb()?.loadUrl(target)
    }

    private fun showTabs() {
        val dialog = BottomSheetDialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(32))
        }
        box.addView(TextView(this).apply {
            text = "Your tabs"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
            setPadding(0, dp(4), 0, dp(4))
        })
        box.addView(TextView(this).apply {
            text = "${tabs.size()} open ${if (tabs.size() == 1) "tab" else "tabs"}"
            textSize = 13f
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, 0, 0, dp(16))
        })
        tabs.all().forEach { tab ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(12), dp(14))
                background = rounded(resolveColor(com.google.android.material.R.attr.colorSurfaceContainerHigh), 20)
            }
            card.addView(TextView(this).apply {
                text = if (tab.privateMode) "Private tab" else tab.title.ifBlank { "New tab" }
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = if (tab.url.startsWith("about:")) "Start page" else tab.url
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(5), 0, 0)
            })
            card.setOnClickListener { switchTo(tab.id); dialog.dismiss() }
            box.addView(card, LinearLayout.LayoutParams(-1, dp(74)).apply { setMargins(0, 0, 0, dp(10)) })
        }
        val add = TextView(this).apply {
            text = "+  New tab"
            gravity = Gravity.CENTER
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary))
            background = rounded(resolveColor(com.google.android.material.R.attr.colorPrimaryContainer), 20)
            setOnClickListener { createTab(false); dialog.dismiss() }
        }
        box.addView(add, LinearLayout.LayoutParams(-1, dp(54)))
        dialog.setContentView(box)
        dialog.show()
    }

    private fun showMenu() {
        val dialog = BottomSheetDialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(28))
        }
        box.addView(TextView(this).apply {
            text = "NEXA"
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(12))
        })
        val items = listOf(
            "New tab" to { createTab(false) },
            "New private tab" to { createTab(true) },
            "Bookmarks" to { showBookmarks() },
            "History" to { showHistory() },
            "Downloads" to { openDownloads() },
            "Find in page" to { findInPage() },
            "Desktop site" to { settings.desktopSite = !settings.desktopSite; recreateCurrentTab() },
            "Share" to { sharePage() },
            "Add bookmark" to { tabs.current()?.let { bookmarks.add(it.title, it.url) } },
            "Settings" to { showSettings() }
        )
        items.forEach { (label, action) ->
            val item = TextView(this).apply {
                text = label
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), 0, dp(16), 0)
                background = rounded(Color.TRANSPARENT, 16)
                setOnClickListener { dialog.dismiss(); action() }
            }
            box.addView(item, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, 0, 0, dp(2)) })
        }
        dialog.setContentView(box)
        dialog.show()
    }

    private fun recreateCurrentTab() {
        val current = tabs.current() ?: return
        val url = current.url
        val isPrivate = current.privateMode
        closeTab(current.id, createReplacement = false)
        createTab(isPrivate, if (url == "about:home" || url == "about:blank") "about:home" else url)
    }

    private fun closeTab(id: Long, createReplacement: Boolean = true) {
        val tab = tabs.close(id) ?: return
        tab.webView?.let { web ->
            web.stopLoading(); webContainer.removeView(web); web.removeAllViews(); web.destroy(); tab.webView = null
        }
        if (tabs.size() == 0 && createReplacement) createTab(false) else tabs.current()?.id?.let(::switchTo)
    }

    private fun showBookmarks() {
        val list = bookmarks.list()
        if (list.isEmpty()) {
            MaterialAlertDialogBuilder(this).setTitle(getString(R.string.bookmarks)).setMessage("No bookmarks yet.").setPositiveButton("OK", null).show(); return
        }
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.bookmarks))
            .setItems(list.map { it.title.ifBlank { it.url } }.toTypedArray()) { _, which -> tabs.current()?.url = list[which].url; currentWeb()?.loadUrl(list[which].url) }
            .setNegativeButton("Close", null).show()
    }

    private fun showHistory() {
        val list = history.list()
        if (list.isEmpty()) {
            MaterialAlertDialogBuilder(this).setTitle(getString(R.string.history)).setMessage("No browsing history yet.").setPositiveButton("OK", null).show(); return
        }
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.history))
            .setItems(list.map { it.title.ifBlank { it.url } }.toTypedArray()) { _, which -> tabs.current()?.url = list[which].url; currentWeb()?.loadUrl(list[which].url) }
            .setNegativeButton("Close", null).setNeutralButton("Clear") { _, _ -> history.clear() }.show()
    }

    private fun findInPage() {
        val input = TextInputEditText(this).apply { hint = getString(R.string.find_in_page) }
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.find_in_page)).setView(input)
            .setPositiveButton("Find") { _, _ -> input.text?.toString()?.takeIf { it.isNotBlank() }?.let { currentWeb()?.findAllAsync(it) } }
            .setNegativeButton("Cancel", null).show()
    }

    private fun sharePage() {
        val url = currentWeb()?.url ?: return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }, getString(R.string.share)))
    }

    private fun openDownloads() {
        try { startActivity(Intent("android.intent.action.VIEW_DOWNLOADS")) }
        catch (_: Exception) { startActivity(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }
    }

    private fun showSettings() {
        val names = arrayOf("Google", "Bing", "DuckDuckGo", "Brave Search", "Startpage", "Ecosia")
        val urls = arrayOf("https://www.google.com/search?q=", "https://www.bing.com/search?q=", "https://duckduckgo.com/?q=", "https://search.brave.com/search?q=", "https://www.startpage.com/sp/search?query=", "https://www.ecosia.org/search?q=")
        val selected = urls.indexOf(settings.searchEngine).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.settings)).setSingleChoiceItems(names, selected) { dialog, which -> settings.searchEngine = urls[which]; dialog.dismiss() }
            .setNeutralButton("Theme") { _, _ -> showThemePicker() }.setNegativeButton(getString(R.string.clear_data)) { _, _ -> clearBrowsingData() }.show()
    }

    private fun showThemePicker() {
        val names = arrayOf("System", "Light", "Dark")
        val modes = intArrayOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)
        MaterialAlertDialogBuilder(this).setTitle("Appearance").setSingleChoiceItems(names, modes.indexOf(currentNightMode()).coerceAtLeast(0)) { dialog, which -> settings.theme = names[which].lowercase(Locale.US); AppCompatDelegate.setDefaultNightMode(modes[which]); dialog.dismiss() }.show()
    }

    private fun currentNightMode(): Int = when (settings.theme) { "light" -> AppCompatDelegate.MODE_NIGHT_NO; "dark" -> AppCompatDelegate.MODE_NIGHT_YES; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
    private fun applyTheme() { AppCompatDelegate.setDefaultNightMode(currentNightMode()) }

    private fun clearBrowsingData() {
        CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush(); tabs.all().forEach { it.webView?.clearCache(true) }; history.clear()
    }

    private fun openFileChooser(callback: ValueCallback<Array<Uri>>, intent: Intent) {
        fileCallback?.onReceiveValue(null); fileCallback = callback
        try { filePicker.launch(intent) } catch (_: Exception) { fileCallback?.onReceiveValue(null); fileCallback = null }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val savedTabs = tabs.all().filterNot { it.privateMode }
        val urls = ArrayList<String>(savedTabs.map { it.url })
        val currentIndex = savedTabs.indexOfFirst { it.id == tabs.currentId }.coerceAtLeast(0)
        outState.putStringArrayList(STATE_TAB_URLS, urls); outState.putInt(STATE_CURRENT_TAB, currentIndex); super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null); fileCallback = null
        tabs.all().forEach { tab -> tab.webView?.let { web -> web.stopLoading(); webContainer.removeView(web); web.removeAllViews(); web.destroy(); tab.webView = null } }
        super.onDestroy()
    }

    private fun resolveColor(attr: Int): Int = com.google.android.material.color.MaterialColors.getColor(this, attr, Color.WHITE)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object { private const val STATE_TAB_URLS = "nexa_urls"; private const val STATE_CURRENT_TAB = "nexa_current" }
}
