package com.awv.browser.ui.browser

import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.awv.browser.browser.BrowserController
import com.awv.browser.browser.BrowserTab
import com.awv.browser.browser.UrlUtils
import com.awv.browser.ui.components.AwvIcon
import com.awv.browser.ui.components.AwvIcons
import com.awv.browser.ui.components.pressable
import com.awv.browser.ui.home.BottomBar
import com.awv.browser.ui.home.HomeActions
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.awvText

/** A tab that is showing a web page: address bar + progress + WebView + bottom bar. */
@Composable
fun BrowserPage(c: BrowserController, tab: BrowserTab, actions: HomeActions) {
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        PageTopBar(c, tab)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val webView = tab.webView
            if (webView != null) {
                key(webView) {
                    AndroidView(
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            val error = tab.error
            if (error != null) ErrorView(tab, error) { c.retry(tab) }
        }
        if (!imeVisible) {
            BottomBar(
                tabCount = c.tabsOf(tab.isPrivate).size,
                canGoBack = c.canGoBack(tab),
                canGoForward = c.canGoForward(tab),
                isPrivate = tab.isPrivate,
                a = actions
            )
        }
    }
}

// ---------- top bar ----------

@Composable
private fun PageTopBar(c: BrowserController, tab: BrowserTab) {
    val dark = tab.isPrivate
    val barBg = if (dark) Awv.Bg else Color.White
    val pillBg = if (dark) Color.White.copy(alpha = .14f) else Awv.Surface
    val textColor = if (dark) Color.White else Awv.Ink
    val hintColor = if (dark) Color.White.copy(alpha = .6f) else Awv.Muted
    val divider = if (dark) Color.White.copy(alpha = .12f) else Awv.Line
    val secure = UrlUtils.isSecure(tab.url)
    val focus = LocalFocusManager.current

    var focused by remember(tab.id) { mutableStateOf(false) }
    var field by remember(tab.id) { mutableStateOf(TextFieldValue(UrlUtils.displayHost(tab.url))) }

    // While not editing, the field shows just the host and follows navigation.
    LaunchedEffect(tab.id, tab.url, focused) {
        if (!focused) field = TextFieldValue(UrlUtils.displayHost(tab.url))
    }

    Column(Modifier.fillMaxWidth().background(barBg).statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(44.dp)
                .background(pillBg, RoundedCornerShape(22.dp))
                .padding(start = 4.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(36.dp).background(if (secure) Awv.BrandSoft else Awv.Line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AwvIcon(
                    if (secure) AwvIcons.Lock else AwvIcons.LockOpen,
                    size = 20.dp,
                    tint = if (secure) Awv.Brand else Awv.Muted,
                    strokeWidth = 1.9f
                )
            }

            BasicTextField(
                value = field,
                onValueChange = { field = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        if (state.isFocused && !focused) {
                            field = TextFieldValue(tab.url, TextRange(0, tab.url.length))
                        }
                        focused = state.isFocused
                    },
                singleLine = true,
                textStyle = awvText(15, FontWeight.Medium, textColor),
                cursorBrush = SolidColor(Awv.Coral),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val text = field.text.trim()
                    if (text.isNotEmpty()) {
                        focus.clearFocus()
                        c.load(tab, text)
                    }
                }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (field.text.isEmpty()) {
                            BasicText(
                                "Search or type URL",
                                style = awvText(15, FontWeight.Medium, hintColor),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                }
            )

            Box(
                Modifier.size(40.dp).pressable(
                    label = if (tab.loading) "Stop loading" else "Reload",
                    onClick = { c.reloadOrStop(tab) }
                ),
                contentAlignment = Alignment.Center
            ) {
                AwvIcon(
                    if (tab.loading) AwvIcons.Close else AwvIcons.Reload,
                    size = 20.dp, tint = textColor, strokeWidth = 2f
                )
            }
        }
        ProgressStrip(tab, divider)
    }
}

@Composable
private fun ProgressStrip(tab: BrowserTab, divider: Color) {
    val target = if (tab.loading) tab.progress.coerceIn(8, 100) / 100f else 1f
    val fraction by animateFloatAsState(target, label = "progress")
    val visible by animateFloatAsState(if (tab.loading) 1f else 0f, label = "progressAlpha")
    Box(Modifier.fillMaxWidth().height(3.dp)) {
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(divider))
        if (visible > 0.01f) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .alpha(visible)
                    .background(Awv.Coral)
            )
        }
    }
}

// ---------- load error ----------

@Composable
private fun ErrorView(tab: BrowserTab, raw: String, onRetry: () -> Unit) {
    val host = UrlUtils.displayHost(tab.url).ifBlank { "this page" }
    Column(
        Modifier.fillMaxSize().background(Awv.Surface).padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        Box(
            Modifier.size(96.dp).background(Color.White, CircleShape).border(1.dp, Awv.Line, CircleShape),
            contentAlignment = Alignment.Center
        ) { AwvIcon(AwvIcons.Globe, size = 48.dp, tint = Awv.Purple, strokeWidth = 1.5f) }
        BasicText("Can't open this page", style = awvText(20, FontWeight.ExtraBold, Awv.Ink))
        BasicText(
            friendlyError(raw, host),
            style = awvText(14, FontWeight.Normal, Awv.Muted, lh = 21).copy(textAlign = TextAlign.Center)
        )
        Box(
            Modifier
                .padding(top = 6.dp)
                .height(48.dp)
                .pressable(label = "Try again", onClick = onRetry)
                .background(Awv.Brand, RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) { BasicText("Try again", style = awvText(15, FontWeight.ExtraBold, Color.White)) }
    }
}

private fun friendlyError(raw: String, host: String): String = when {
    raw.contains("NAME_NOT_RESOLVED", true) ->
        "We couldn't find $host. Check the address and try again."
    raw.contains("INTERNET_DISCONNECTED", true) || raw.contains("NETWORK_CHANGED", true) ||
        raw.contains("CONNECTION", true) || raw.contains("TIMED_OUT", true) ->
        "Check your internet connection and try again."
    raw.contains("CERT", true) || raw.contains("SSL", true) ->
        "This site's security certificate isn't trusted, so Awv blocked it."
    raw.contains("PAGE_CRASHED", true) ->
        "The page ran out of memory and was closed. Try loading it again."
    else -> raw
}
