package com.awv.browser.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awv.browser.ui.components.AwvIcon
import com.awv.browser.ui.components.AwvIcons
import com.awv.browser.ui.components.IconDef
import com.awv.browser.ui.components.Wallpaper
import com.awv.browser.ui.components.pressable
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.AwvTheme
import com.awv.browser.ui.theme.awvText

// ---------- state & actions (wired to real data in later parts) ----------

data class PrivacyStats(val trackersBlocked: Int = 0, val dataSavedMb: Int = 0, val timeSavedMin: Int = 0)
data class Shortcut(val title: String, val url: String)

class HomeActions(
    val onSubmit: (String) -> Unit = {},
    val onVoice: () -> Unit = {},
    val onScan: () -> Unit = {},
    val onShield: () -> Unit = {},
    val onStats: () -> Unit = {},
    val onShortcut: (Shortcut) -> Unit = {},
    val onShortcutLongPress: (Shortcut) -> Unit = {},
    val onAddShortcut: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onForward: () -> Unit = {},
    val onNewTab: () -> Unit = {},
    val onTabs: () -> Unit = {},
    val onMenu: () -> Unit = {}
)

// ---------- screen ----------

@Composable
fun HomeScreen(
    stats: PrivacyStats = PrivacyStats(),
    shortcuts: List<Shortcut> = emptyList(),
    canAddShortcut: Boolean = true,
    tabCount: Int = 1,
    canGoBack: Boolean = true,
    canGoForward: Boolean = false,
    isPrivate: Boolean = false,
    shieldOn: Boolean = true,
    actions: HomeActions = HomeActions()
) {
    Box(Modifier.fillMaxSize().background(Awv.Bg)) {
        Wallpaper()
        Column(Modifier.fillMaxSize()) {
            HomeContent(stats, shortcuts, canAddShortcut, isPrivate, shieldOn, actions, Modifier.weight(1f))
            BottomBar(tabCount, canGoBack, canGoForward, isPrivate, actions)
        }
    }
}

@Composable
private fun HomeContent(
    stats: PrivacyStats,
    shortcuts: List<Shortcut>,
    canAddShortcut: Boolean,
    isPrivate: Boolean,
    shieldOn: Boolean,
    a: HomeActions,
    modifier: Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 22.dp)
    ) {
        Omnibox(a, shieldOn)
        StatsCard(stats, a.onStats, Modifier.padding(top = 16.dp))
        if (isPrivate) PrivateBadge(Modifier.padding(start = 4.dp, top = 24.dp))
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = if (isPrivate) 12.dp else 28.dp),
            verticalAlignment = Alignment.Top
        ) {
            BasicText(
                "Browse fast.\nStay private.",
                Modifier.weight(1f),
                style = awvText(36, FontWeight.ExtraBold, Color.White, ls = -1f, lh = 38)
            )
            ThemeToggle()
        }
        Spacer(Modifier.weight(1f).height(12.dp))
        ShortcutGrid(shortcuts, canAddShortcut, a)
    }
}

/** Dark / light switch. Shows the sun while dark mode is on (tap -> light) and the moon while light (tap -> dark). */
@Composable
private fun ThemeToggle() {
    val dark = AwvTheme.dark
    Box(
        Modifier
            .size(44.dp)
            .background(Color.White.copy(alpha = .16f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = .24f), CircleShape)
            .pressable(label = if (dark) "Switch to light mode" else "Switch to dark mode", onClick = AwvTheme::toggle),
        contentAlignment = Alignment.Center
    ) {
        AwvIcon(if (dark) AwvIcons.Sun else AwvIcons.Moon, size = 22.dp, tint = Color.White, strokeWidth = 1.9f)
    }
}

@Composable
private fun PrivateBadge(modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .height(32.dp)
            .background(Color.White.copy(alpha = .16f), shape)
            .border(1.dp, Color.White.copy(alpha = .24f), shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AwvIcon(AwvIcons.Private, size = 16.dp, tint = Color.White, strokeWidth = 1.8f)
        BasicText("Private tab", style = awvText(13, FontWeight.Bold, Color.White))
    }
}

// ---------- omnibox ----------

@Composable
private fun Omnibox(a: HomeActions, shieldOn: Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(14.dp, shape, ambientColor = Awv.Shadow, spotColor = Awv.Shadow)
            .background(Awv.Card, shape)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(40.dp).background(if (shieldOn) Awv.BrandSoft else Awv.Line, CircleShape)
                .pressable(label = if (shieldOn) "Shield on" else "Shield off", onClick = a.onShield),
            contentAlignment = Alignment.Center
        ) { AwvIcon(AwvIcons.Shield, size = 22.dp, tint = if (shieldOn) Awv.Brand else Awv.Muted, strokeWidth = 1.9f) }

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = awvText(15, FontWeight.Medium, Awv.Ink),
            cursorBrush = SolidColor(Awv.Coral),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                if (query.isNotBlank()) {
                    focus.clearFocus()
                    a.onSubmit(query.trim())
                }
            }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        BasicText(
                            "Search or type URL",
                            style = awvText(15, FontWeight.Medium, Awv.Muted),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )
        OmniButton(AwvIcons.Mic, "Voice search", a.onVoice)
        OmniButton(AwvIcons.Scan, "Scan code", a.onScan)
    }
}

@Composable
private fun OmniButton(icon: IconDef, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).pressable(label = label, onClick = onClick), contentAlignment = Alignment.Center) {
        AwvIcon(icon, size = 22.dp)
    }
}

// ---------- protected this week ----------

@Composable
private fun StatsCard(stats: PrivacyStats, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = .12f), shape)
            .border(1.dp, Color.White.copy(alpha = .2f), shape)
            .pressable(label = "Protected this week", onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp).background(Awv.Orange, CircleShape), contentAlignment = Alignment.Center) {
                AwvIcon(AwvIcons.Shield, size = 16.dp, tint = Color.White, strokeWidth = 2.2f)
            }
            BasicText("Protected this week", Modifier.weight(1f), style = awvText(14, FontWeight.Bold, Color.White))
            AwvIcon(AwvIcons.Chevron, size = 18.dp, tint = Color.White, strokeWidth = 2f)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("%,d".format(stats.trackersBlocked), "Trackers blocked")
            Stat("${stats.dataSavedMb} MB", "Data saved")
            Stat("${stats.timeSavedMin} min", "Time saved")
        }
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String) {
    Column(Modifier.weight(1f)) {
        BasicText(value, style = awvText(24, FontWeight.ExtraBold, Color.White, ls = -.5f), maxLines = 1)
        BasicText(label, style = awvText(12, FontWeight.Normal, Color.White.copy(alpha = .78f)))
    }
}

// ---------- shortcut tiles (added by the user; tap = open, long-press = remove) ----------

@Composable
private fun ColumnScope.ShortcutGrid(shortcuts: List<Shortcut>, canAdd: Boolean, a: HomeActions) {
    val total = shortcuts.size + if (canAdd) 1 else 0 // + "Add"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        (0 until total).chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { i ->
                    if (i < shortcuts.size) {
                        val s = shortcuts[i]
                        Tile(
                            s.title, Modifier.weight(1f),
                            onLongClick = { a.onShortcutLongPress(s) },
                            onClick = { a.onShortcut(s) }
                        ) {
                            BasicText(
                                s.title.take(1).uppercase(),
                                style = awvText(24, FontWeight.ExtraBold, Awv.Brand)
                            )
                        }
                    } else {
                        Tile("Add", Modifier.weight(1f), add = true, onClick = a.onAddShortcut) {
                            AwvIcon(AwvIcons.Plus, size = 28.dp, tint = Color.White, strokeWidth = 1.9f)
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Tile(
    label: String,
    modifier: Modifier,
    add: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.pressable(label = label, onLongClick = onLongClick, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val box = if (add) {
            Modifier.size(62.dp)
                .background(Color.White.copy(alpha = .16f), shape)
                .border(1.dp, Color.White.copy(alpha = .28f), shape)
        } else {
            Modifier.size(62.dp)
                .shadow(8.dp, shape, ambientColor = Awv.Shadow, spotColor = Awv.Shadow)
                .background(Awv.Card, shape)
        }
        Box(box, contentAlignment = Alignment.Center) { icon() }
        BasicText(
            label,
            style = awvText(12, FontWeight.Medium, Color.White).copy(textAlign = TextAlign.Center),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- bottom bar ----------

@Composable
fun BottomBar(tabCount: Int, canGoBack: Boolean, canGoForward: Boolean, isPrivate: Boolean, a: HomeActions) {
    Column(Modifier.fillMaxWidth().background(Awv.Card).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Awv.Line))
        Row(
            Modifier.fillMaxWidth().height(67.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BarButton("Back", canGoBack, a.onBack) {
                AwvIcon(AwvIcons.Back, tint = if (canGoBack) Awv.Ink else Awv.Disabled, strokeWidth = 1.9f)
            }
            BarButton("Forward", canGoForward, a.onForward) {
                AwvIcon(AwvIcons.Forward, tint = if (canGoForward) Awv.Ink else Awv.Disabled, strokeWidth = 1.9f)
            }
            Box(
                Modifier.size(48.dp)
                    .shadow(10.dp, CircleShape, ambientColor = Awv.Coral.copy(alpha = .4f), spotColor = Awv.Coral.copy(alpha = .4f))
                    .background(Awv.Coral, CircleShape)
                    .pressable(label = "New tab", onClick = a.onNewTab),
                contentAlignment = Alignment.Center
            ) { AwvIcon(AwvIcons.Plus, size = 26.dp, tint = Color.White, strokeWidth = 2.4f) }
            BarButton("Tabs", true, a.onTabs) { TabCount(tabCount, isPrivate) }
            BarButton("Menu", true, a.onMenu) { AwvIcon(AwvIcons.Dots) }
        }
    }
}

@Composable
private fun BarButton(label: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(52.dp).pressable(enabled = enabled, label = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun TabCount(count: Int, isPrivate: Boolean) {
    val color = if (isPrivate) Awv.Purple else Awv.Ink
    Box(
        Modifier.size(26.dp).border(2.dp, color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            if (count > 99) ":D" else count.toString(),
            style = awvText(13, FontWeight.ExtraBold, color)
        )
    }
}
