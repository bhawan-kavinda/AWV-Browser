package com.awv.browser.ui.menu

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awv.browser.browser.BrowserController
import com.awv.browser.settings.SearchEngine
import com.awv.browser.ui.components.AwvIcon
import com.awv.browser.ui.components.AwvIcons
import com.awv.browser.ui.components.ConfirmDialog
import com.awv.browser.ui.components.IconDef
import com.awv.browser.ui.components.pressable
import com.awv.browser.ui.components.tappable
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.awvText

/** Settings: default search engine + clear browsing data. */
@Composable
fun SettingsScreen(c: BrowserController, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var clearHistory by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(false) }
    val nothingSelected = !clearHistory && !clearCookies && !clearCache
    val version = remember { appVersion(ctx) }
    val engine = c.settings.engine

    Box(Modifier.fillMaxSize()) {
        LibraryPage("Settings", onClose) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(AwvIcons.Search, "Search engine")
                Group {
                    SearchEngine.entries.forEachIndexed { i, e ->
                        if (i > 0) Divider()
                        EngineRow(e, selected = e == engine) { c.settings.setEngine(e) }
                    }
                }
                BasicText(
                    "Used when you type words instead of a web address, and for voice search.",
                    Modifier.padding(horizontal = 4.dp),
                    style = awvText(12, FontWeight.Normal, Awv.Muted, lh = 18)
                )

                SectionHeader(AwvIcons.Trash, "Clear browsing data", Modifier.padding(top = 14.dp))
                Group {
                    CheckRow("Browsing history", "Pages you visited", clearHistory) { clearHistory = it }
                    Divider()
                    CheckRow("Cookies and site data", "Signs you out of websites", clearCookies) { clearCookies = it }
                    Divider()
                    CheckRow("Cached files", "Images and files saved for faster loading", clearCache) { clearCache = it }
                }
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(50.dp)
                        .alpha(if (nothingSelected) .4f else 1f)
                        .pressable(enabled = !nothingSelected, label = "Clear data", onClick = { confirm = true })
                        .background(Awv.Brand, RoundedCornerShape(25.dp)),
                    contentAlignment = Alignment.Center
                ) { BasicText("Clear data", style = awvText(15, FontWeight.ExtraBold, Color.White)) }
                BasicText(
                    "Private tabs never save anything, so there is nothing to clear for them.",
                    Modifier.padding(horizontal = 4.dp),
                    style = awvText(12, FontWeight.Normal, Awv.Muted, lh = 18)
                )

                BasicText(
                    "Awv browser  ·  version $version",
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    style = awvText(12, FontWeight.Medium, Awv.Muted).copy(textAlign = TextAlign.Center)
                )
            }
        }
        if (confirm) {
            val parts = buildList {
                if (clearHistory) add("browsing history")
                if (clearCookies) add("cookies and site data")
                if (clearCache) add("cached files")
            }
            ConfirmDialog(
                title = "Clear selected data?",
                message = "This deletes your " + parts.joinToString(", ") + "." +
                    if (clearCookies) " You will be signed out of websites." else "",
                confirmLabel = "Clear",
                onConfirm = {
                    c.clearData(clearHistory, clearCookies, clearCache)
                    confirm = false
                },
                onDismiss = { confirm = false }
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: IconDef, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AwvIcon(icon, size = 18.dp, tint = Awv.Muted, strokeWidth = 2f)
        BasicText(text, style = awvText(13, FontWeight.ExtraBold, Awv.Muted))
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Awv.Card, RoundedCornerShape(18.dp))) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 60.dp).height(1.dp).background(Awv.Line))
}

@Composable
private fun EngineRow(engine: SearchEngine, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .tappable(label = engine.label, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(36.dp).background(if (selected) Awv.Brand else Awv.BrandSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                engine.label.take(1),
                style = awvText(15, FontWeight.ExtraBold, if (selected) Color.White else Awv.Brand)
            )
        }
        BasicText(engine.label, Modifier.weight(1f), style = awvText(15, FontWeight.Bold, Awv.Ink), maxLines = 1)
        if (selected) AwvIcon(AwvIcons.Check, size = 22.dp, tint = Awv.Brand, strokeWidth = 2.4f)
    }
}

@Composable
private fun CheckRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val box = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .tappable(label = title, role = Role.Checkbox, onClick = { onChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(if (checked) Awv.Brand else Color.Transparent, box)
                .border(2.dp, if (checked) Awv.Brand else Awv.Disabled, box),
            contentAlignment = Alignment.Center
        ) {
            if (checked) AwvIcon(AwvIcons.Check, size = 16.dp, tint = Color.White, strokeWidth = 3f)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(title, style = awvText(15, FontWeight.Bold, Awv.Ink), maxLines = 1)
            BasicText(subtitle, style = awvText(12, FontWeight.Medium, Awv.Muted), maxLines = 2)
        }
    }
}

private fun appVersion(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}
