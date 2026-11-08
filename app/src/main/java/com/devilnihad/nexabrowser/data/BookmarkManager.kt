package com.devilnihad.nexabrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val title: String, val url: String)

class BookmarkManager(context: Context) {
    private val prefs = context.getSharedPreferences("nexa_bookmarks", Context.MODE_PRIVATE)

    fun list(): List<Bookmark> {
        val array = JSONArray(prefs.getString("items", "[]"))
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(Bookmark(item.optString("title"), item.optString("url")))
            }
        }
    }

    fun add(title: String, url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        val array = JSONArray(prefs.getString("items", "[]"))
        if ((0 until array.length()).any { array.getJSONObject(it).optString("url") == url }) return
        array.put(JSONObject().apply {
            put("title", title.ifBlank { url })
            put("url", url)
        })
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun remove(url: String) {
        val source = JSONArray(prefs.getString("items", "[]"))
        val result = JSONArray()
        for (i in 0 until source.length()) {
            if (source.getJSONObject(i).optString("url") != url) {
                result.put(source.getJSONObject(i))
            }
        }
        prefs.edit().putString("items", result.toString()).apply()
    }
}
