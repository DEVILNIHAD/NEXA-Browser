package com.devilnihad.nexabrowser

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.devilnihad.nexabrowser.data.BookmarkManager
import com.devilnihad.nexabrowser.data.HistoryManager
import com.devilnihad.nexabrowser.data.SettingsManager
import com.devilnihad.nexabrowser.data.TabManager
import com.devilnihad.nexabrowser.utils.UrlResolver
import com.devilnihad.nexabrowser.web.WebViewManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var address: TextInputEditText
    private lateinit var progress: ProgressBar

    private val tabs = TabManager()
    private lateinit var history: HistoryManager
    private lateinit var bookmarks: BookmarkManager
    private lateinit var settings: SettingsManager
    private var privateMode = false
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
        buildUi()
        tabs.create(false)
        loadHome()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    web.canGoBack() -> web.goBack()
                    tabs.all().size > 1 -> {
                        val currentId = tabs.currentId ?: return
                        tabs.close(currentId)
                        showCurrentTab()
                    }
                    else -> finish()
                }
            }
        })
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10.dp(), 28.dp(), 10.dp(), 8.dp())
            setBackgroundColor(Color.TRANSPARENT)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        address = TextInputEditText(this).apply {
            hint = getString(R.string.search_hint)
            setSingleLine(true)
            setTextColor(com.google.android.material.color.MaterialColors.getColor(
                this@MainActivity,
                com.google.android.material.R.attr.colorOnSurface,
                Color.WHITE
            ))
            setPadding(18.dp(), 0, 18.dp(), 0)
        }
        top.addView(address, LinearLayout.LayoutParams(0, 56.dp(), 1f).apply {
            setMargins(0, 0, 8.dp(), 0)
        })
        top.addView(MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "⋮"
            minWidth = 48.dp()
            setOnClickListener { showMenu() }
        }, LinearLayout.LayoutParams(52.dp(), 56.dp()))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        root.addView(top)
        root.addView(progress, LinearLayout.LayoutParams(-1, 2.dp()))

        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottomBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        fun button(label: String, onClick: () -> Unit) = MaterialButton(this).apply {
            text = label
            minWidth = 48.dp()
            setOnClickListener { onClick() }
        }
        bottomBar.addView(button("‹") { if (web.canGoBack()) web.goBack() })
        bottomBar.addView(button("›") { if (web.canGoForward()) web.goForward() })
        bottomBar.addView(button("+") { newTab(false) })
        bottomBar.addView(button("▣") { showTabs() })
        root.addView(bottomBar, LinearLayout.LayoutParams(-1, 60.dp()))
        setContentView(root)

        WebViewManager(
            activity = this,
            onUrl = { url ->
                address.setText(url)
                tabs.current()?.url = url
                if (!privateMode) history.add(web.title.orEmpty(), url)
            },
            onTitle = { title -> tabs.current()?.title = title },
            onProgress = { value ->
                progress.progress = value
                progress.visibility = if (value < 100) View.VISIBLE else View.GONE
            },
            onNewTab = { url -> newTab(false, url) },
            onFileChooser = ::openFileChooser
        ).configure(web, settings.desktop)

        address.setOnEditorActionListener { _, _, _ ->
            val target = UrlResolver.resolve(address.text?.toString().orEmpty(), settings.searchEngine)
            tabs.current()?.url = target
            web.loadUrl(target)
            true
        }
    }

    private fun loadHome() {
        val homepage = settings.homepage
        if (homepage.startsWith("http://") || homepage.startsWith("https://")) {
            web.loadUrl(homepage)
        } else {
            web.loadDataWithBaseURL(null, homeHtml(), "text/html", "UTF-8", null)
        }
        address.setText("")
    }

    private fun newTab(isPrivate: Boolean, url: String = "about:blank") {
        privateMode = isPrivate
        tabs.create(isPrivate).url = url
        if (url == "about:blank") loadHome() else web.loadUrl(url)
    }

    private fun showCurrentTab() {
        val tab = tabs.current() ?: run {
            tabs.create(false)
            loadHome()
            return
        }
        privateMode = tab.privateMode
        if (tab.url == "about:blank") loadHome() else web.loadUrl(tab.url)
    }

    private fun showTabs() {
        val dialog = BottomSheetDialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 20.dp(), 20.dp(), 40.dp())
        }
        content.addView(TextView(this).apply {
            text = "Tabs"
            textSize = 18f
        })
        tabs.all().forEach { tab ->
            content.addView(MaterialButton(this).apply {
                text = tab.title.ifBlank { tab.url.ifBlank { "New tab" } }
                setOnClickListener {
                    tabs.select(tab.id)
                    showCurrentTab()
                    dialog.dismiss()
                }
            })
        }
        dialog.setContentView(content)
        dialog.show()
    }

    private fun showMenu() {
        val dialog = BottomSheetDialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 20.dp(), 20.dp(), 40.dp())
        }
        val actions: List<Pair<String, () -> Unit>> = listOf(
            "New tab" to { newTab(false) },
            "New private tab" to { newTab(true) },
            "Bookmark this page" to { bookmarks.add(web.title.orEmpty(), web.url.orEmpty()) },
            "Bookmarks" to { showBookmarks() },
            "History" to { showHistory() },
            "Find in page" to { findInPage() },
            "Settings" to { showSettings() },
            "Share" to { sharePage() }
        )
        actions.forEach { (label, action) ->
            content.addView(MaterialButton(this).apply {
                text = label
                setOnClickListener {
                    dialog.dismiss()
                    action()
                }
            })
        }
        dialog.setContentView(content)
        dialog.show()
    }

    private fun showBookmarks() {
        val items = bookmarks.list()
        if (items.isEmpty()) {
            MaterialAlertDialogBuilder(this).setTitle("Bookmarks")
                .setMessage("No bookmarks yet.").setPositiveButton("OK", null).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Bookmarks")
            .setItems(items.map { it.title.ifBlank { it.url } }.toTypedArray()) { _, index ->
                tabs.current()?.url = items[index].url
                web.loadUrl(items[index].url)
            }
            .show()
    }

    private fun showHistory() {
        val items = history.list()
        if (items.isEmpty()) {
            MaterialAlertDialogBuilder(this).setTitle("History")
                .setMessage("No browsing history yet.").setPositiveButton("OK", null).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("History")
            .setItems(items.map { it.title.ifBlank { it.url } }.toTypedArray()) { _, index ->
                tabs.current()?.url = items[index].url
                web.loadUrl(items[index].url)
            }
            .show()
    }

    private fun findInPage() {
        val input = TextInputEditText(this).apply { hint = "Find in page" }
        MaterialAlertDialogBuilder(this)
            .setTitle("Find in page")
            .setView(input)
            .setPositiveButton("Find") { _, _ ->
                input.text?.toString()?.takeIf { it.isNotBlank() }?.let(web::findAllAsync)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettings() {
        val input = TextInputEditText(this).apply { setText(settings.searchEngine) }
        MaterialAlertDialogBuilder(this)
            .setTitle("Search engine URL")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                input.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    settings.searchEngine = it
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sharePage() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, web.url.orEmpty())
        }
        startActivity(Intent.createChooser(intent, "Share page"))
    }

    private fun openFileChooser(callback: ValueCallback<Array<Uri>>?) {
        fileCallback?.onReceiveValue(null)
        fileCallback = callback
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            filePicker.launch(intent)
        } catch (_: Exception) {
            fileCallback?.onReceiveValue(null)
            fileCallback = null
        }
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            web.webChromeClient = null
            web.webViewClient = null
            web.destroy()
        }
        super.onDestroy()
    }

    private fun homeHtml(): String = """
        <!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>
        <style>body{font-family:sans-serif;background:#f7f7fb;color:#15151c;padding:24px}h1{font-size:28px}p{font-size:16px;color:#3a3a3a}</style>
        </head><body><h1>NEXA Browser</h1><p>Fast. Private. Yours.</p></body></html>
    """.trimIndent()

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
