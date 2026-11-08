package com.devilnihad.nexabrowser.data

import android.content.Context

class SettingsManager(context: Context) {
    private val p=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
    var searchEngine: String get()=p.getString("search", "https://www.google.com/search?q=")!!; set(v){p.edit().putString("search",v).apply()}
    var theme: String get()=p.getString("theme","system")!!; set(v){p.edit().putString("theme",v).apply()}
    var desktop: Boolean get()=p.getBoolean("desktop",false); set(v){p.edit().putBoolean("desktop",v).apply()}
    var homepage: String get()=p.getString("home","about:home")!!; set(v){p.edit().putString("home",v).apply()}
}
