package com.awv.browser.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.URLUtil
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.awv.browser.data.AwvDb
import com.awv.browser.data.Bookmark
import com.awv.browser.data.HistoryEntry
import com.awv.browser.data.ShortcutStore
import com.awv.browser.privacy.Blocker
import com.awv.browser.privacy.PrivacyStore
import com.awv.browser.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

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

    /** Room database: history + bookmarks. */
    val db: AwvDb = AwvDb.get(activity)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Shield on/off + weekly stats, and the ad/tracker filter list. */
    val privacy = PrivacyStore(activity)
    private val blocker = Blocker(activity)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Search engine choice, and the start-page shortcut tiles. */
    val settings = SettingsStore(activity)
    val shortcuts = ShortcutStore(activity)

    /**
     * true = this device's WebView supports separate profiles, so Private tabs get their own cookie jar,
     * storage and cache that are wiped when the last Private tab closes. false = older WebView:
     * Private tabs still keep no history and use no cache, but cookies are shared with normal tabs.
     */
    var privateIsolated: Boolean = try {
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
    } catch (e: Throwable) {
        false
    }
        private set

    /** Same Chrome version as [baseUserAgent], but a desktop (Linux) platform string. */
    private val desktopUserAgent: String = run {
        val v = Regex("Chrome/(\\d+)").find(baseUserAgent)?.groupValues?.get(1) ?: "124"
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$v.0.0.0 Safari/537.36"
    }

    val active: BrowserTab get() = tabs.first { it.id == activeId }

    init {
        wipePrivateProfile() // leftovers from a previous run that was killed while Private tabs were open
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
        val url = UrlUtils.resolve(input, settings.engine.searchUrl)
        val webView = tab.webView ?: createWebView(tab)
        tab.error = null
        tab.url = url
        tab.pageHost = hostOf(url)
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

    // ------------------------------------------------- desktop site / bookmarks

    /** Menu toggle: switches this tab between mobile and desktop site, then reloads. */
    fun toggleDesktop(tab: BrowserTab) {
        tab.desktop = !tab.desktop
        val webView = tab.webView ?: return
        applyMode(webView, tab)
        webView.reload()
        toast(if (tab.desktop) "Desktop site on" else "Mobile site on")
    }

    private fun applyMode(webView: WebView, tab: BrowserTab) {
        with(webView.settings) {
            userAgentString = if (tab.desktop) desktopUserAgent else baseUserAgent
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    fun toggleBookmark(tab: BrowserTab, currentlyBookmarked: Boolean) {
        val url = tab.url
        if (url.isBlank()) return
        val title = tab.title.ifBlank { UrlUtils.displayHost(url) }
        io.launch {
            if (currentlyBookmarked) db.bookmarks().remove(url)
            else db.bookmarks().add(Bookmark(url, title, System.currentTimeMillis()))
        }
        toast(if (currentlyBookmarked) "Bookmark removed" else "Bookmark added")
    }

    private fun logVisit(tab: BrowserTab, view: WebView?, url: String?) {
        val u = url ?: view?.url ?: return
        if (tab.isPrivate || tab.error != null || u == tab.loggedUrl) return
        if (!u.startsWith("http://") && !u.startsWith("https://")) return
        tab.loggedUrl = u
        val title = view?.title?.takeIf { it.isNotBlank() } ?: UrlUtils.displayHost(u)
        io.launch { db.history().insert(HistoryEntry(url = u, title = title, time = System.currentTimeMillis())) }
    }

    // ------------------------------------------------------------- downloads

    private fun startDownload(tab: BrowserTab, url: String, userAgent: String?, disposition: String?, mime: String?) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            toast("This kind of download isn't supported")
            return
        }
        try {
            val name = URLUtil.guessFileName(url, disposition, mime)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(name)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .addRequestHeader("User-Agent", userAgent ?: baseUserAgent)
            if (!mime.isNullOrBlank()) request.setMimeType(mime)
            cookieManagerFor(tab).getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
            // Public Downloads folder needs no permission on Android 10+; older versions use the app's own folder.
            if (Build.VERSION.SDK_INT >= 29) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            } else {
                request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, name)
            }
            (activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            toast("Downloading $name")
        } catch (e: Exception) {
            toast("Couldn't start the download")
        }
    }

    fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    // ---------------------------------------------------------------- privacy

    /** Shield button / menu tile. Reloads the open page so the change takes effect straight away. */
    fun toggleShield() {
        privacy.toggleShield()
        toast(if (privacy.shieldOn) "Shield on" else "Shield off")
        val tab = active
        if (!tab.showHome && tab.error == null && tab.url.isNotBlank()) tab.webView?.reload()
    }

    private fun hostOf(url: String): String = Uri.parse(url).host?.lowercase().orEmpty()

    /**
     * Settings > Clear data. Works on the normal profile only: Private tabs keep nothing
     * (their data is wiped when they close). Open pages stay on screen until reloaded.
     */
    fun clearData(history: Boolean, cookies: Boolean, cache: Boolean) {
        if (history) io.launch { db.history().clear() }
        if (cookies) {
            val manager = CookieManager.getInstance()
            manager.removeAllCookies(null)
            manager.flush()
            WebStorage.getInstance().deleteAllData()
        }
        if (cache) {
            // The HTTP cache is shared by every normal WebView, so any of them can clear it.
            val existing = tabs.firstOrNull { !it.isPrivate && it.webView != null }?.webView
            val webView = existing ?: WebView(activity)
            webView.clearCache(true)
            if (existing == null) webView.destroy()
        }
        toast("Data cleared")
    }

    /** Cookie jar that belongs to this tab: the Private profile's for Private tabs, the normal one otherwise. */
    private fun cookieManagerFor(tab: BrowserTab): CookieManager {
        if (tab.isPrivate && privateIsolated) {
            try {
                return ProfileStore.getInstance().getOrCreateProfile(PRIVATE_PROFILE).cookieManager
            } catch (e: Exception) {
                // fall through
            }
        }
        return CookieManager.getInstance()
    }

    /** Must run before anything else touches the new WebView. */
    private fun attachPrivateProfile(webView: WebView) {
        if (!privateIsolated) return
        try {
            ProfileStore.getInstance().getOrCreateProfile(PRIVATE_PROFILE)
            WebViewCompat.setProfile(webView, PRIVATE_PROFILE)
        } catch (e: Exception) {
            privateIsolated = false
            toast("Private tabs share cookies with normal tabs on this device")
        }
    }

    /** Clears cookies + site data of the Private profile and deletes it. Safe to call when it doesn't exist. */
    private fun wipePrivateProfile() {
        if (!privateIsolated) return
        try {
            val store = ProfileStore.getInstance()
            val profile = store.getProfile(PRIVATE_PROFILE) ?: return
            try {
                profile.cookieManager.removeAllCookies(null)
                profile.webStorage.deleteAllData()
            } catch (e: Exception) {
                // keep going: deleting the profile below removes its data too
            }
            store.deleteProfile(PRIVATE_PROFILE) // throws while a WebView still uses it; retried on the next close / launch
        } catch (e: Exception) {
            // Best effort.
        }
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
        io.cancel()
        tabs.toList().forEach { release(it) }
        flushGraveyard()
        wipePrivateProfile()
    }

    private fun release(tab: BrowserTab) {
        tab.webView?.let { webView ->
            if (tab.isPrivate) {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
            }
            graveyard.add(webView)
        }
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
        if (tabs.none { it.isPrivate }) wipePrivateProfile()
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
        if (tab.isPrivate) attachPrivateProfile(webView)
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
            if (tab.isPrivate) cacheMode = WebSettings.LOAD_NO_CACHE
        }
        applyMode(webView, tab)
        webView.setDownloadListener { url, ua, disposition, mime, _ -> startDownload(tab, url, ua, disposition, mime) }
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

        /** Ad / tracker blocking. Runs on a WebView IO thread; the main page itself is never blocked. */
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            if (request == null || request.isForMainFrame || !privacy.shieldFlag) return null
            val scheme = request.url.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return null
            if (!blocker.shouldBlock(request.url, tab.pageHost)) return null
            mainHandler.post {
                tab.blockedCount++
                privacy.recordBlocked()
            }
            return WebResourceResponse(
                "text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0))
            )
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (url != null) {
                tab.url = url
                tab.pageHost = hostOf(url)
            }
            tab.blockedCount = 0
            tab.error = null
            tab.loading = true
            if (tab.progress >= 100) tab.progress = 5
            syncNavState(tab, view)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            tab.loading = false
            tab.progress = 100
            syncNavState(tab, view)
            if (tab.desktop) view?.evaluateJavascript(DESKTOP_VIEWPORT_JS, null)
            logVisit(tab, view, url)
            view?.postDelayed({ captureThumbnail(tab) }, 700)
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            if (tab.desktop) view?.evaluateJavascript(DESKTOP_VIEWPORT_JS, null)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            if (url != null && tab.error == null) {
                tab.url = url
                tab.pageHost = hostOf(url)
            }
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
        const val PRIVATE_PROFILE = "awv_private"
        const val THUMB_W = 360
        // Forces a 1024px-wide layout so mobile-optimised sites show their desktop version.
        const val DESKTOP_VIEWPORT_JS =
            "(function(){var m=document.querySelector('meta[name=viewport]');" +
            "if(!m){m=document.createElement('meta');m.name='viewport';(document.head||document.documentElement).appendChild(m);}" +
            "m.setAttribute('content','width=1024');})();"
        const val THUMB_H = 342
    }
}
