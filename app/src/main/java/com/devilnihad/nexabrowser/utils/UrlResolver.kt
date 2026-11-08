package com.devilnihad.nexabrowser.utils

import android.net.Uri

object UrlResolver {
    fun resolve(input: String, search: String): String {
        val s=input.trim(); if(s.isEmpty()) return "about:home"
        if(s.equals("about:home",true)) return s
        if(s.startsWith("http://") || s.startsWith("https://")) return s
        val uri=Uri.parse(s)
        if(uri.scheme != null) return s
        val looksLikeHost=s.contains('.') && !s.contains(' ')
        return if(looksLikeHost) "https://$s" else search + Uri.encode(s)
    }
}
