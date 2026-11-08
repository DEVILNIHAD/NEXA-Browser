package com.devilnihad.nexabrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(context: Context) {
    data class Entry(val title: String, val url: String, val time: Long)

    private val prefs = context.getSharedPreferences("nexa_history", Context.MODE_PRIVATE)

    fun add(title: String, url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        val source = JSONArray(prefs.getString("items", "[]"))
        val result = JSONArray()
        result.put(JSONObject().apply {
            put("title", title.ifBlank { url })
            put("url", url)
            put("time", System.currentTimeMillis())
        })
        for (i in 0 until minOf(source.length(), 199)) {
            val old = source.getJSONObject(i)
            if (old.optString("url") != url) result.put(old)
        }
        prefs.edit().putString("items", result.toString()).apply()
    }

    fun list(): List<Entry> {
        val array = JSONArray(prefs.getString("items", "[]"))
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(Entry(item.optString("title"), item.optString("url"), item.optLong("time")))
            }
        }
    }

    fun clear() = prefs.edit().remove("items").apply()
}
