package com.devilnihad.nexabrowser

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.devilnihad.nexabrowser.data.BookmarkManager
import com.devilnihad.nexabrowser.data.HistoryManager
import com.devilnihad.nexabrowser.data.SettingsManager
import com.devilnihad.nexabrowser.data.Tab
import com.devilnihad.nexabrowser.data.TabManager
import com.devilnihad.nexabrowser.utils.UrlResolver
import com.devilnihad.nexabrowser.web.WebViewManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var webContainer: FrameLayout
    private lateinit var address: TextInputEditText
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
        callback?.onReceiveValue(
            if (result.resultCode == Activity.RESULT_OK && uri != null) arrayOf(uri) else null
        )
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
        if (restoredUrls.isNullOrEmpty()) {
            createTab(false)
        } else {
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
            setPadding(dp(10), dp(28), dp(10), dp(8))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        address = TextInputEditText(this).apply {
            hint = getString(R.string.search_hint)
            setSingleLine(true)
            setPadding(dp(18), 0, dp(18), 0)
            textSize = 16f
        }
        val menu = MaterialButton(this).apply {
            text = "⋮"
            minWidth = dp(48)
            setOnClickListener { showMenu() }
        }
        top.addView(address, LinearLayout.LayoutParams(0, dp(54), 1f).apply {
            setMargins(0, 0, dp(8), 0)
        })
        top.addView(menu, LinearLayout.LayoutParams(dp(54), dp(54)))

        progress = View(this).apply {
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorPrimary))
            visibility = View.GONE
        }
        webContainer = FrameLayout(this)
        root.addView(top)
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(2)))
        root.addView(webContainer, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buildBottomBar(), LinearLayout.LayoutParams(-1, dp(62)))
        setContentView(root)

        address.setOnEditorActionListener { _, _, _ ->
            loadAddress()
            true
        }
    }

    private fun buildBottomBar(): View {
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        fun button(label: String, action: () -> Unit) = MaterialButton(this).apply {
            text = label
            minWidth = dp(48)
            setOnClickListener { action() }
        }
        bar.addView(button("‹") { currentWeb()?.let { if (it.canGoBack()) it.goBack() } })
        bar.addView(button("›") { currentWeb()?.let { if (it.canGoForward()) it.goForward() } })
        bar.addView(button("+") { createTab(false) })
        bar.addView(button("▣") { showTabs() })
        bar.addView(button("⌂") { loadHome() })
        return bar
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
                if (tabs.currentId == tab.id) address.setText(url)
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
        tabs.all().forEach { tab ->
            tab.webView?.visibility = if (tab.id == id) View.VISIBLE else View.GONE
        }
        val tab = tabs.current() ?: return
        address.setText(if (tab.url == "about:home" || tab.url == "about:blank") "" else tab.url)
        progress.visibility = View.GONE
    }

    private fun currentWeb(): WebView? = tabs.current()?.webView

    private fun loadHome() {
        val html = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
            body{font-family:sans-serif;background:#f7f7fb;color:#15151c;padding:30px 22px}
            .brand{font-size:44px;font-weight:800;letter-spacing:-2px;margin-top:30px}
            .tag{opacity:.65;margin-top:4px}
            .grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-top:34px}
            .item{padding:20px;border-radius:20px;background:#fff;text-decoration:none;color:inherit}
            .name{font-weight:700}.url{font-size:12px;opacity:.55;margin-top:5px}
            @media(prefers-color-scheme:dark){body{background:#0b0b10;color:#fff}.item{background:#17171f}}
            </style></head><body><div class="brand">NEXA</div><div class="tag">Fast. Private. Yours.</div>
            <div class="grid">
            <a class="item" href="https://www.google.com"><div class="name">Google</div><div class="url">google.com</div></a>
            <a class="item" href="https://www.youtube.com"><div class="name">YouTube</div><div class="url">youtube.com</div></a>
            <a class="item" href="https://github.com"><div class="name">GitHub</div><div class="url">github.com</div></a>
            <a class="item" href="https://wikipedia.org"><div class="name">Wikipedia</div><div class="url">wikipedia.org</div></a>
            </div></body></html>
        """.trimIndent()
        val tab = tabs.current() ?: return
        tab.url = "about:home"
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
            setPadding(dp(20), dp(20), dp(20), dp(40))
        }
        box.addView(TextView(this).apply {
            text = "Tabs (${tabs.size()})"
            textSize = 22f
            setPadding(0, 0, 0, dp(18))
        })
        tabs.all().forEach { tab ->
            box.addView(MaterialButton(this).apply {
                text = "${if (tab.privateMode) "Private · " else ""}${tab.title}\n${tab.url}"
                gravity = Gravity.START
                setOnClickListener {
                    switchTo(tab.id)
                    dialog.dismiss()
                }
            })
        }
        box.addView(MaterialButton(this).apply {
            text = "＋ New tab"
            setOnClickListener {
                createTab(false)
                dialog.dismiss()
            }
        })
        dialog.setContentView(box)
        dialog.show()
    }

    private fun showMenu() {
        val dialog = BottomSheetDialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(40))
        }
        listOf(
            "New tab", "New private tab", "Bookmarks", "History", "Downloads",
            "Find in page", "Desktop site", "Share", "Add bookmark", "Settings"
        ).forEach { label ->
            box.addView(MaterialButton(this).apply {
                text = label
                setOnClickListener {
                    dialog.dismiss()
                    when (label) {
                        "New tab" -> createTab(false)
                        "New private tab" -> createTab(true)
                        "Bookmarks" -> showBookmarks()
                        "History" -> showHistory()
                        "Downloads" -> openDownloads()
                        "Find in page" -> findInPage()
                        "Desktop site" -> {
                            settings.desktopSite = !settings.desktopSite
                            recreateCurrentTab()
                        }
                        "Share" -> sharePage()
                        "Add bookmark" -> tabs.current()?.let { bookmarks.add(it.title, it.url) }
                        "Settings" -> showSettings()
                    }
                }
            })
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
            web.stopLoading()
            webContainer.removeView(web)
            web.removeAllViews()
            web.destroy()
            tab.webView = null
        }
        if (tabs.size() == 0 && createReplacement) {
            createTab(false)
        } else {
            tabs.current()?.id?.let(::switchTo)
        }
    }

    private fun showBookmarks() {
        val list = bookmarks.list()
        if (list.isEmpty()) {
            MaterialAlertDialogBuilder(this).setTitle(getString(R.string.bookmarks))
                .setMessage("No bookmarks yet.").setPositiveButton("OK", null).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.bookmarks))
            .setItems(list.map { it.title.ifBlank { it.url } }.toTypedArray()) { _, which ->
                tabs.current()?.url = list[which].url
                currentWeb()?.loadUrl(list[which].url)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showHistory() {
        val list = history.list()
        if (list.isEmpty()) {
            MaterialAlertDialogBuilder(this).setTitle(getString(R.string.history))
                .setMessage("No browsing history yet.").setPositiveButton("OK", null).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.history))
            .setItems(list.map { it.title.ifBlank { it.url } }.toTypedArray()) { _, which ->
                tabs.current()?.url = list[which].url
                currentWeb()?.loadUrl(list[which].url)
            }
            .setNegativeButton("Close", null)
            .setNeutralButton("Clear") { _, _ -> history.clear() }
            .show()
    }

    private fun findInPage() {
        val input = TextInputEditText(this).apply { hint = getString(R.string.find_in_page) }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.find_in_page))
            .setView(input)
            .setPositiveButton("Find") { _, _ ->
                input.text?.toString()?.takeIf { it.isNotBlank() }?.let { currentWeb()?.findAllAsync(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sharePage() {
        val url = currentWeb()?.url ?: return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }, getString(R.string.share)))
    }

    private fun openDownloads() {
        try {
            startActivity(Intent("android.intent.action.VIEW_DOWNLOADS"))
        } catch (_: Exception) {
            startActivity(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        }
    }

    private fun showSettings() {
        val names = arrayOf("Google", "Bing", "DuckDuckGo", "Brave Search", "Startpage", "Ecosia")
        val urls = arrayOf(
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q=",
            "https://search.brave.com/search?q=",
            "https://www.startpage.com/sp/search?query=",
            "https://www.ecosia.org/search?q="
        )
        val selected = urls.indexOf(settings.searchEngine).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings))
            .setSingleChoiceItems(names, selected) { dialog, which ->
                settings.searchEngine = urls[which]
                dialog.dismiss()
            }
            .setNeutralButton("Theme") { _, _ -> showThemePicker() }
            .setNegativeButton(getString(R.string.clear_data)) { _, _ -> clearBrowsingData() }
            .show()
    }

    private fun showThemePicker() {
        val names = arrayOf("System", "Light", "Dark")
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("Appearance")
            .setSingleChoiceItems(names, modes.indexOf(currentNightMode()).coerceAtLeast(0)) { dialog, which ->
                settings.theme = names[which].lowercase(Locale.US)
                AppCompatDelegate.setDefaultNightMode(modes[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun currentNightMode(): Int = when (settings.theme) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(currentNightMode())
    }

    private fun clearBrowsingData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        tabs.all().forEach { it.webView?.clearCache(true) }
        history.clear()
    }

    private fun openFileChooser(callback: ValueCallback<Array<Uri>>, intent: Intent) {
        fileCallback?.onReceiveValue(null)
        fileCallback = callback
        try {
            filePicker.launch(intent)
        } catch (_: Exception) {
            fileCallback?.onReceiveValue(null)
            fileCallback = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val savedTabs = tabs.all().filterNot { it.privateMode }
        val urls = ArrayList<String>(savedTabs.map { it.url })
        val currentIndex = savedTabs.indexOfFirst { it.id == tabs.currentId }.coerceAtLeast(0)
        outState.putStringArrayList(STATE_TAB_URLS, urls)
        outState.putInt(STATE_CURRENT_TAB, currentIndex)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        tabs.all().forEach { tab ->
            tab.webView?.let { web ->
                web.stopLoading()
                webContainer.removeView(web)
                web.removeAllViews()
                web.destroy()
                tab.webView = null
            }
        }
        super.onDestroy()
    }

    private fun resolveColor(attr: Int): Int =
        com.google.android.material.color.MaterialColors.getColor(this, attr, Color.WHITE)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val STATE_TAB_URLS = "nexa_urls"
        private const val STATE_CURRENT_TAB = "nexa_current"
    }
}
