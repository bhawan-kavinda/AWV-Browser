package com.awv.browser.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awv.browser.browser.BrowserController
import com.awv.browser.browser.BrowserTab
import com.awv.browser.ui.components.AwvIcon
import com.awv.browser.ui.components.AwvIcons
import com.awv.browser.ui.components.IconDef
import com.awv.browser.ui.components.pressable
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.awvText

/** Order matters: BrowserApp treats everything from Bookmarks onwards as a full-screen page. */
enum class Sheet { None, Menu, Shield, Bookmarks, History, Downloads, Settings }

@Composable
fun MenuSheet(
    c: BrowserController,
    tab: BrowserTab,
    onDismiss: () -> Unit,
    onOpen: (Sheet) -> Unit
) {
    val onPage = !tab.showHome && tab.url.isNotBlank()
    val bookmarked by remember(tab.url) { c.db.bookmarks().isBookmarked(tab.url) }.collectAsState(false)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .45f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Awv.Card, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.padding(bottom = 6.dp).width(40.dp).height(4.dp).background(Awv.Line, RoundedCornerShape(2.dp)))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(
                    if (bookmarked) "Bookmarked" else "Bookmark", AwvIcons.Bookmark,
                    active = bookmarked, enabled = onPage
                ) {
                    c.toggleBookmark(tab, bookmarked)
                    onDismiss()
                }
                Tile("Desktop site", AwvIcons.Desktop, active = tab.desktop, enabled = onPage) {
                    c.toggleDesktop(tab)
                    onDismiss()
                }
                Tile("Private tab", AwvIcons.Private) {
                    c.newTab(isPrivate = true)
                    onDismiss()
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Bookmarks", AwvIcons.Bookmark) { onOpen(Sheet.Bookmarks) }
                Tile("History", AwvIcons.History) { onOpen(Sheet.History) }
                Tile("Downloads", AwvIcons.Download) { onOpen(Sheet.Downloads) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val shieldOn = c.privacy.shieldOn
                Tile(if (shieldOn) "Shield on" else "Shield off", AwvIcons.Shield, active = shieldOn) {
                    c.toggleShield()
                    onDismiss()
                }
                Tile("Settings", AwvIcons.Settings) { onOpen(Sheet.Settings) }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.Tile(
    label: String,
    icon: IconDef,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = if (active) Awv.Brand else Awv.Ink
    Column(
        Modifier
            .weight(1f)
            .height(88.dp)
            .alpha(if (enabled) 1f else .4f)
            .pressable(enabled = enabled, label = label, onClick = onClick)
            .background(if (active) Awv.BrandSoft else Awv.Surface, RoundedCornerShape(20.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        AwvIcon(icon, size = 26.dp, tint = tint, strokeWidth = 1.9f)
        BasicText(label, style = awvText(12, FontWeight.Bold, tint).copy(textAlign = TextAlign.Center), maxLines = 1)
    }
}
