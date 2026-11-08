package com.devilnihad.nexabrowser.data

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("nexa_settings", Context.MODE_PRIVATE)

    var searchEngine: String
        get() = prefs.getString("search_engine", "https://www.google.com/search?q=") ?: "https://www.google.com/search?q="
        set(value) = prefs.edit().putString("search_engine", value).apply()

    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) = prefs.edit().putString("theme", value).apply()

    var desktopSite: Boolean
        get() = prefs.getBoolean("desktop_site", false)
        set(value) = prefs.edit().putBoolean("desktop_site", value).apply()

    var homepage: String
        get() = prefs.getString("homepage", "about:home") ?: "about:home"
        set(value) = prefs.edit().putString("homepage", value).apply()
}
