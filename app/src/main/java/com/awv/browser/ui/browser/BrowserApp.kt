package com.awv.browser.ui.browser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.awv.browser.browser.BrowserController
import com.awv.browser.ui.home.HomeActions
import com.awv.browser.ui.home.HomeScreen
import com.awv.browser.ui.menu.BookmarksScreen
import com.awv.browser.ui.menu.DownloadsScreen
import com.awv.browser.ui.menu.HistoryScreen
import com.awv.browser.ui.menu.MenuSheet
import com.awv.browser.ui.menu.Sheet
import com.awv.browser.ui.tabs.TabsOverlay
import com.awv.browser.ui.theme.Awv

/** Root screen: start page or web page for the active tab, plus the tabs overlay and video fullscreen. */
@Composable
fun BrowserApp(c: BrowserController) {
    val tab = c.active
    var showTabs by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(Sheet.None) }
    val fullscreen = c.customView

    val actions = HomeActions(
        onSubmit = { c.load(c.active, it) },
        onBack = c::goBack,
        onForward = c::goForward,
        onNewTab = { c.newTab(c.active.isPrivate) },
        onMenu = { sheet = Sheet.Menu },
        onTabs = {
            c.captureThumbnail(c.active)
            showTabs = true
        }
    )

    StatusBarIcons(darkIcons = showTabs || sheet >= Sheet.Bookmarks || (!tab.showHome && !tab.isPrivate))

    BackHandler(enabled = fullscreen != null || sheet != Sheet.None || showTabs || !tab.showHome) {
        when {
            fullscreen != null -> c.exitFullscreen()
            sheet != Sheet.None -> sheet = Sheet.None
            showTabs -> showTabs = false
            else -> c.goBack()
        }
    }

    Box(Modifier.fillMaxSize().background(Awv.Bg)) {
        key(tab.id) {
            if (tab.showHome) {
                HomeScreen(
                    tabCount = c.tabsOf(tab.isPrivate).size,
                    canGoBack = c.canGoBack(tab),
                    canGoForward = c.canGoForward(tab),
                    isPrivate = tab.isPrivate,
                    actions = actions
                )
            } else {
                BrowserPage(c, tab, actions)
            }
        }
        if (showTabs) TabsOverlay(c) { showTabs = false }
        when (sheet) {
            Sheet.Menu -> MenuSheet(c, tab, onDismiss = { sheet = Sheet.None }, onOpen = { sheet = it })
            Sheet.Bookmarks -> BookmarksScreen(c) { sheet = Sheet.None }
            Sheet.History -> HistoryScreen(c) { sheet = Sheet.None }
            Sheet.Downloads -> DownloadsScreen { sheet = Sheet.None }
            Sheet.None -> Unit
        }
        if (fullscreen != null) FullscreenVideo(fullscreen)
    }

    // Closed tabs' WebViews are destroyed only after Compose has let go of them.
    SideEffect { c.flushGraveyard() }
}

@Composable
private fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity().window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}

@Composable
private fun FullscreenVideo(content: View) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(Unit) {
        val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insets.show(WindowInsetsCompat.Type.systemBars()) }
    }
    AndroidView(
        factory = {
            (content.parent as? ViewGroup)?.removeView(content)
            content
        },
        modifier = Modifier.fillMaxSize().background(Color.Black)
    )
}

private fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in context chain")
}
