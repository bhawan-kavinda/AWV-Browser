package com.awv.browser.browser

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/** One browser tab. All fields are Compose state so the UI follows the WebView live. */
class BrowserTab(val id: Int, val isPrivate: Boolean) {
    var url by mutableStateOf("")
    var title by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var loading by mutableStateOf(false)

    /** true = the Awv start page is showing for this tab (WebView, if any, is kept alive behind it). */
    var showHome by mutableStateOf(true)

    /** Main-frame load error (raw net:: description) or null. */
    var error by mutableStateOf<String?>(null)

    var webCanGoBack by mutableStateOf(false)
    var webCanGoForward by mutableStateOf(false)
    var thumbnail by mutableStateOf<ImageBitmap?>(null)

    /** Desktop-site mode (desktop user-agent + wide viewport). */
    var desktop by mutableStateOf(false)

    /** Ads / trackers blocked on the page currently loaded in this tab. */
    var blockedCount by mutableIntStateOf(0)

    /** Host of the page being loaded. Read from WebView IO threads by the blocker, so not Compose state. */
    @Volatile var pageHost: String = ""

    /** Last URL written to history, so reloads / SPA updates are not logged twice. */
    var loggedUrl: String? = null

    /** Created lazily on the first navigation; null for a fresh start-page tab. */
    var webView by mutableStateOf<WebView?>(null)
}
