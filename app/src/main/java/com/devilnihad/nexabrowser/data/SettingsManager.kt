package com.devilnihad.nexabrowser.data

import android.content.Context

class SettingsManager(context: Context) {
    private val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var searchEngine: String
        get() = p.getString("search", "https://www.google.com/search?q=") ?: "https://www.google.com/search?q="
        set(value) = p.edit().putString("search", value).apply()

    var theme: String
        get() = p.getString("theme", "system") ?: "system"
        set(value) = p.edit().putString("theme", value).apply()

    var desktop: Boolean
        get() = p.getBoolean("desktop", false)
        set(value) = p.edit().putBoolean("desktop", value).apply()

    var homepage: String
        get() = p.getString("home", "about:home") ?: "about:home"
        set(value) = p.edit().putString("home", value).apply()

    fun save() {
        // SharedPreferences setters already persist immediately; this method keeps compatibility for callers.
    }
}
