package com.awv.browser.browser

import android.net.Uri
import android.util.Patterns
import java.net.URLEncoder

object UrlUtils {
    private const val SEARCH_URL = "https://www.google.com/search?q="

    private val localAddress = Regex(
        """^(localhost|(\d{1,3}\.){3}\d{1,3})(:\d{1,5})?([/?#].*)?$""",
        RegexOption.IGNORE_CASE
    )

    /** Text that is a web address -> loadable http/https URL; anything else (search words) -> null. */
    fun asAddress(input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val lower = text.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return text
        if (localAddress.matches(text)) return "http://$text"
        val looksLikeEmail = '@' in text && '/' !in text
        if (!looksLikeEmail && Patterns.WEB_URL.matcher(text).matches()) return "https://$text"
        return null
    }

    /** Turns whatever the user typed into a loadable URL: address -> https/http URL, anything else -> web search. */
    fun resolve(input: String, searchUrl: String = SEARCH_URL): String =
        asAddress(input) ?: (searchUrl + URLEncoder.encode(input.trim(), "UTF-8"))

    /** "https://www.example.com/a/b" -> "example.com" (falls back to the raw text). */
    fun displayHost(url: String): String {
        if (url.isBlank()) return ""
        val host = Uri.parse(url).host ?: return url
        return host.removePrefix("www.")
    }

    fun isSecure(url: String) = url.startsWith("https://", ignoreCase = true)
}
