package com.devilnihad.nexabrowser.utils

import android.net.Uri

object UrlResolver {
    fun resolve(input: String, searchEngine: String): String {
        val value = input.trim()
        if (value.isEmpty()) return "about:home"
        if (value.equals("about:home", ignoreCase = true)) return value
        if (value.startsWith("http://") || value.startsWith("https://")) return value

        val parsed = Uri.parse(value)
        if (!parsed.scheme.isNullOrEmpty()) return value

        val looksLikeDomain = value.contains('.') && !value.contains(' ')
        return if (looksLikeDomain) {
            "https://$value"
        } else {
            searchEngine + Uri.encode(value)
        }
    }
}
