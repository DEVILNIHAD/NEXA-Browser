package com.devilnihad.nexabrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val title: String, val url: String)
class BookmarkManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
    fun list(): List<Bookmark> { val a = JSONArray(prefs.getString("items", "[]")); return buildList { for (i in 0 until a.length()) { val o=a.getJSONObject(i); add(Bookmark(o.optString("title"),o.optString("url"))) } } }
    fun add(title: String, url: String) { if (url.isBlank()) return; val a=JSONArray(prefs.getString("items","[]")); if ((0 until a.length()).any { a.getJSONObject(it).optString("url")==url }) return; a.put(JSONObject().apply{put("title",title);put("url",url)}); prefs.edit().putString("items",a.toString()).apply() }
    fun remove(url: String) { val a=JSONArray(prefs.getString("items","[]")); val out=JSONArray(); for(i in 0 until a.length()) if(a.getJSONObject(i).optString("url")!=url) out.put(a.getJSONObject(i)); prefs.edit().putString("items",out.toString()).apply() }
}
