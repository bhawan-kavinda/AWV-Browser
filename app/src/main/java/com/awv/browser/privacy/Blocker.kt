package com.awv.browser.privacy

import android.content.Context
import android.net.Uri

/** Filter list: assets/blocklist.txt (domains) + a couple of unmistakable ad URL paths. */
class Blocker(context: Context) {

    private val domains: Set<String> = try {
        context.assets.open("blocklist.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toHashSet()
        }
    } catch (e: Exception) {
        emptySet()
    }

    /** Called on WebView's IO threads. [pageHost] is the tab's own site: a site is never blocked on itself. */
    fun shouldBlock(url: Uri, pageHost: String): Boolean {
        val host = url.host?.lowercase() ?: return false
        var h = host
        while (true) {
            if (h in domains) return !sameSite(pageHost, h)
            val dot = h.indexOf('.')
            if (dot < 0 || h.indexOf('.', dot + 1) < 0) break // stop before the bare TLD
            h = h.substring(dot + 1)
        }
        val path = url.path?.lowercase().orEmpty()
        return path.startsWith("/pagead/") || path.startsWith("/adserver/")
    }

    private fun sameSite(pageHost: String, blocked: String) =
        pageHost == blocked || pageHost.endsWith(".$blocked")
}
