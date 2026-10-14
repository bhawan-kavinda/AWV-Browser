package com.awv.browser.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Owns every tab and its WebView. Lives as long as MainActivity
 * (the activity handles config changes itself, so WebViews survive rotation).
 */
class BrowserController(private val activity: Activity) {

    val tabs = mutableStateListOf<BrowserTab>()

    var activeId by mutableIntStateOf(0)
        private set

    /** Non-null while a page video is in fullscreen. */
    var customView by mutableStateOf<View?>(null)
        private set

    private var nextId = 1
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private val graveyard = mutableListOf<WebView>()

    /** Chrome-like user agent (no "; wv" / "Version/4.0" markers) so sites treat Awv as a full browser. */
    val baseUserAgent: String = WebSettings.getDefaultUserAgent(activity)
        .replace("; wv)", ")")
        .replace("Version/4.0 ", "")

    val active: BrowserTab get() = tabs.first { it.id == activeId }

    init {
        newTab(isPrivate = false)
    }

    // ------------------------------------------------------------------ tabs

    fun tabsOf(isPrivate: Boolean): List<BrowserTab> = tabs.filter { it.isPrivate == isPrivate }

    fun newTab(isPrivate: Boolean): BrowserTab {
        val tab = BrowserTab(nextId++, isPrivate)
        tabs.add(tab)
        activate(tab)
        return tab
    }

    fun activate(tab: BrowserTab) {
        val previous = tabs.firstOrNull { it.id == activeId }
        if (previous != null && previous !== tab) captureThumbnail(previous)
        activeId = tab.id
    }

    fun closeTab(tab: BrowserTab) {
        val index = tabs.indexOf(tab)
        if (index < 0) return
        val wasActive = tab.id == activeId
        tabs.removeAt(index)
        release(tab)

        if (wasActive) {
            val sameMode = tabs.filter { it.isPrivate == tab.isPrivate }
            val next = sameMode.lastOrNull { tabs.indexOf(it) < index }
                ?: sameMode.firstOrNull()
                ?: tabs.firstOrNull { !it.isPrivate }
            if (next != null) activeId = next.id
        }
        // There is always at least one normal tab.
        if (tabs.none { !it.isPrivate }) {
            val fresh = BrowserTab(nextId++, false)
            tabs.add(0, fresh)
            if (wasActive) activeId = fresh.id
        }
    }

    private fun openPopupTab(isPrivate: Boolean): WebView {
        val tab = BrowserTab(nextId++, isPrivate)
        tab.showHome = false
        tabs.add(tab)
        val webView = createWebView(tab)
        activate(tab)
        return webView
    }

    // ------------------------------------------------------------ navigation

    fun canGoBack(tab: BrowserTab): Boolean = !tab.showHome

    fun canGoForward(tab: BrowserTab): Boolean =
        if (tab.showHome) tab.webView != null && tab.url.isNotEmpty() else tab.webCanGoForward

    /** Address-bar / omnibox input: URL or search text. */
    fun load(tab: BrowserTab, input: String) {
        if (input.isBlank()) return
        val url = UrlUtils.resolve(input)
        val webView = tab.webView ?: createWebView(tab)
        tab.error = null
        tab.url = url
        tab.title = ""
        tab.progress = 5
        tab.loading = true
        tab.showHome = false
        webView.loadUrl(url)
    }

    fun goBack() {
        val tab = active
        if (tab.showHome) return
        val webView = tab.webView
        if (webView != null && webView.canGoBack()) {
            tab.error = null
            webView.goBack()
        } else {
            tab.showHome = true // back from the first page returns to the start page
        }
    }

    fun goForward() {
        val tab = active
        if (tab.showHome) {
            if (canGoForward(tab)) tab.showHome = false
        } else {
            tab.webView?.goForward()
        }
    }

    /** Reload button; becomes Stop while the page is loading. */
    fun reloadOrStop(tab: BrowserTab) {
        if (tab.loading) {
            tab.webView?.stopLoading()
            tab.loading = false
        } else {
            retry(tab)
        }
    }

    fun retry(tab: BrowserTab) {
        if (tab.url.isEmpty()) return
        if (tab.error != null || tab.webView == null) load(tab, tab.url) else tab.webView?.reload()
    }

    // --------------------------------------------------------------- lifecycle

    fun onResume() {
        tabs.forEach { it.webView?.onResume() }
        active.webView?.resumeTimers()
    }

    fun onPause() {
        tabs.forEach { it.webView?.onPause() }
        active.webView?.pauseTimers()
    }

    fun destroy() {
        tabs.toList().forEach { release(it) }
        flushGraveyard()
    }

    private fun release(tab: BrowserTab) {
        tab.webView?.let { graveyard.add(it) }
        tab.webView = null
    }

    /** Called from a SideEffect: destroys WebViews of closed tabs once Compose has detached them. */
    fun flushGraveyard() {
        if (graveyard.isEmpty()) return
        val dead = graveyard.toList()
        graveyard.clear()
        dead.forEach { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
    }

    fun exitFullscreen() {
        if (customView == null) return
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    // ---------------------------------------------------------------- thumbnails

    /** Snapshot of the top of the page for the tab switcher. */
    fun captureThumbnail(tab: BrowserTab) {
        val webView = tab.webView ?: return
        if (tab.showHome || tab.error != null) return
        if (webView.width <= 0 || webView.height <= 0 || !webView.isAttachedToWindow) return
        try {
            val bitmap = Bitmap.createBitmap(THUMB_W, THUMB_H, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scale = THUMB_W.toFloat() / webView.width
            canvas.scale(scale, scale)
            webView.draw(canvas)
            tab.thumbnail = bitmap.asImageBitmap()
        } catch (e: Exception) {
            // Thumbnail is optional; keep the previous one.
        }
    }

    // ------------------------------------------------------------------ WebView

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tab: BrowserTab): WebView {
        val webView = WebView(activity)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.setBackgroundColor(Color.WHITE)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = baseUserAgent
        }
        webView.webViewClient = AwvClient(tab)
        webView.webChromeClient = AwvChrome(tab)
        tab.webView = webView
        return webView
    }

    private fun syncNavState(tab: BrowserTab, view: WebView?) {
        if (view == null) return
        tab.webCanGoBack = view.canGoBack()
        tab.webCanGoForward = view.canGoForward()
    }

    private fun openExternal(url: String, tab: BrowserTab) {
        try {
            val intent = if (url.startsWith("intent:", ignoreCase = true)) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null
            activity.startActivity(intent)
        } catch (e: Exception) {
            // No app can handle it: use the page's own fallback URL if it gave one.
            val fallback = try {
                if (url.startsWith("intent:", ignoreCase = true)) {
                    Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                } else null
            } catch (e2: Exception) {
                null
            }
            if (fallback != null && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                tab.webView?.loadUrl(fallback)
            }
        }
    }

    private inner class AwvClient(private val tab: BrowserTab) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            if (request == null) return false
            val uri = request.url
            return when (uri.scheme?.lowercase()) {
                "http", "https", "about", "data", "blob", "javascript" -> false
                else -> {
                    // mailto:, tel:, intent:, market:, app deep links... only when the user actually tapped.
                    if (request.hasGesture()) openExternal(uri.toString(), tab)
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (url != null) tab.url = url
            tab.error = null
            tab.loading = true
            if (tab.progress >= 100) tab.progress = 5
            syncNavState(tab, view)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            tab.loading = false
            tab.progress = 100
            syncNavState(tab, view)
            view?.postDelayed({ captureThumbnail(tab) }, 700)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            if (url != null && tab.error == null) tab.url = url
            syncNavState(tab, view)
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            if (request == null || !request.isForMainFrame) return
            val description = error?.description?.toString().orEmpty()
            if (description.contains("ERR_ABORTED", ignoreCase = true)) return
            tab.url = request.url.toString()
            tab.error = description.ifBlank { "net::ERR_FAILED" }
            tab.loading = false
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.cancel()
            val failing = error?.url?.trimEnd('/')
            if (failing != null && failing == tab.url.trimEnd('/')) {
                tab.error = "net::ERR_CERT_INVALID"
                tab.loading = false
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            // The page's renderer died (usually out of memory). Drop the WebView and let the user retry.
            if (view != null) {
                (view.parent as? ViewGroup)?.removeView(view)
                graveyard.add(view)
            }
            tab.webView = null
            tab.loading = false
            tab.error = "net::ERR_PAGE_CRASHED"
            return true
        }
    }

    private inner class AwvChrome(private val tab: BrowserTab) : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            tab.progress = newProgress
            tab.loading = newProgress < 100
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            tab.title = title.orEmpty()
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            // target=_blank / window.open -> new tab. Pop-ups without a user tap are blocked.
            if (!isUserGesture || resultMsg == null) return false
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            transport.webView = openPopupTab(tab.isPrivate)
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView?) {
            closeTab(tab)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view == null) return
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }
            customView = view
            customViewCallback = callback
        }

        override fun onHideCustomView() {
            exitFullscreen()
        }

        // Some WebView versions crash when this returns null while a video is loading.
        override fun getDefaultVideoPoster(): Bitmap? =
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val THUMB_W = 360
        const val THUMB_H = 342
    }
}
