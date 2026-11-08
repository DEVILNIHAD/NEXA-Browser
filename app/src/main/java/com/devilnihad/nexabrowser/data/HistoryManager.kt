package com.devilnihad.nexabrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    data class Entry(val title: String, val url: String, val time: Long)
    fun add(title: String, url: String) { if (url.startsWith("about:")) return; val a = JSONArray(prefs.getString("items", "[]")); val out = JSONArray(); out.put(JSONObject().apply { put("title", title); put("url", url); put("time", System.currentTimeMillis()) }); for (i in 0 until minOf(a.length(), 199)) out.put(a.getJSONObject(i)); prefs.edit().putString("items", out.toString()).apply() }
    fun list(): List<Entry> { val a = JSONArray(prefs.getString("items", "[]")); return buildList { for (i in 0 until a.length()) { val o = a.getJSONObject(i); add(Entry(o.optString("title"), o.optString("url"), o.optLong("time"))) } } }
    fun clear() { prefs.edit().remove("items").apply() }
}
