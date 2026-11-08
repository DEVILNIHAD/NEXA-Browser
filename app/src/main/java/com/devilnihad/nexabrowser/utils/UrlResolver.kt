package com.devilnihad.nexabrowser.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UrlResolver {
    fun resolve(input: String, search: String): String {
        val s = input.trim()
        if (s.isEmpty()) return "about:home"
        if (s.equals("about:home", ignoreCase = true)) return s
        if (s.startsWith("http://") || s.startsWith("https://")) return s
        if (s.startsWith("mailto:") || s.startsWith("tel:") || s.startsWith("sms:") || s.startsWith("geo:")) return s

        val looksLikeHost = s.contains('.') && !s.contains(' ')
        return if (looksLikeHost) {
            "https://$s"
        } else {
            val encoded = URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
            search + encoded.replace("+", "%20")
        }
    }
}
