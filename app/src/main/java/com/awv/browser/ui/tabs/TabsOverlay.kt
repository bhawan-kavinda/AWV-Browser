package com.awv.browser.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awv.browser.browser.BrowserController
import com.awv.browser.browser.BrowserTab
import com.awv.browser.browser.UrlUtils
import com.awv.browser.ui.components.AwvIcon
import com.awv.browser.ui.components.AwvIcons
import com.awv.browser.ui.components.pressable
import com.awv.browser.ui.components.tappable
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.awvText
import kotlin.math.max

@Composable
fun TabsOverlay(c: BrowserController, onDone: () -> Unit) {
    var privateMode by remember { mutableStateOf(c.active.isPrivate) }
    val list = c.tabs.filter { it.isPrivate == privateMode }
    val activeId = c.activeId

    Column(
        Modifier
            .fillMaxSize()
            .background(Awv.Surface)
            .pointerInput(Unit) { detectTapGestures { } } // swallow taps so the page below never gets them
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText("Tabs", style = awvText(30, FontWeight.ExtraBold, Awv.Ink, ls = -.8f))
                val doneShape = RoundedCornerShape(22.dp)
                Box(
                    Modifier
                        .height(44.dp)
                        .pressable(label = "Done", onClick = onDone)
                        .background(Awv.Card, doneShape)
                        .border(1.dp, Awv.Line, doneShape)
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) { BasicText("Done", style = awvText(14, FontWeight.Bold, Awv.Ink)) }
            }
            Segmented(privateMode) { privateMode = it }
        }

        if (list.isEmpty()) {
            EmptyPrivate(Modifier.weight(1f), c.privateIsolated)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        active = tab.id == activeId,
                        onOpen = {
                            c.activate(tab)
                            onDone()
                        },
                        onClose = { c.closeTab(tab) }
                    )
                }
            }
        }

        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(56.dp)
                .pressable(label = "New tab") {
                    c.newTab(privateMode)
                    onDone()
                }
                .background(Awv.Brand, RoundedCornerShape(28.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            AwvIcon(AwvIcons.Plus, size = 22.dp, tint = Color.White, strokeWidth = 2.4f)
            BasicText("New tab", style = awvText(16, FontWeight.ExtraBold, Color.White))
        }
    }
}

// ---------- Normal / Private switch ----------

@Composable
private fun Segmented(privateMode: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Awv.Track, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Seg("Normal", !privateMode, Modifier.weight(1f)) { onChange(false) }
        Seg("Private", privateMode, Modifier.weight(1f)) { onChange(true) }
    }
}

@Composable
private fun Seg(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val base = modifier.fillMaxHeight().tappable(label = label, role = Role.Tab, onClick = onClick)
    val styled = if (selected) base.shadow(2.dp, shape).background(Awv.Card, shape) else base
    Box(styled, contentAlignment = Alignment.Center) {
        BasicText(label, style = awvText(14, FontWeight.Bold, if (selected) Awv.Ink else Awv.Muted))
    }
}

@Composable
private fun EmptyPrivate(modifier: Modifier, isolated: Boolean) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        Box(
            Modifier.size(96.dp).background(Awv.Card, CircleShape).border(1.dp, Awv.Line, CircleShape),
            contentAlignment = Alignment.Center
        ) { AwvIcon(AwvIcons.Private, size = 52.dp, tint = Awv.Purple, strokeWidth = 1.5f) }
        BasicText("No private tabs", style = awvText(20, FontWeight.ExtraBold, Awv.Ink))
        BasicText(
            if (isolated) "Private tabs leave no history, cookies or site data on this device."
            else "Private tabs leave no history and use no cache. Cookies are shared with normal tabs on this device.",
            style = awvText(14, FontWeight.Normal, Awv.Muted, lh = 21).copy(textAlign = TextAlign.Center)
        )
    }
}

// ---------- tab card ----------

@Composable
private fun TabCard(tab: BrowserTab, active: Boolean, onOpen: () -> Unit, onClose: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val title = when {
        tab.showHome -> "Start page"
        tab.title.isNotBlank() -> tab.title
        else -> UrlUtils.displayHost(tab.url).ifBlank { "New tab" }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .pressable(label = title, onClick = onOpen)
            .clip(shape)
            .background(Awv.Card, shape)
            .border(if (active) 2.dp else 1.dp, if (active) Awv.Coral else Awv.Line, shape)
    ) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                title,
                Modifier.weight(1f),
                style = awvText(13, FontWeight.Bold, Awv.Ink),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Box(
                Modifier.size(44.dp).pressable(label = "Close tab $title", onClick = onClose),
                contentAlignment = Alignment.Center
            ) { AwvIcon(AwvIcons.Close, size = 16.dp, tint = Awv.Muted, strokeWidth = 2.2f) }
        }
        val shot = tab.thumbnail
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            when {
                tab.showHome -> StartThumb()
                shot != null -> Image(
                    bitmap = shot,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
                else -> Box(Modifier.fillMaxSize().background(Awv.Surface), contentAlignment = Alignment.Center) {
                    BasicText(
                        UrlUtils.displayHost(tab.url).take(1).uppercase(),
                        style = awvText(40, FontWeight.ExtraBold, Awv.Disabled)
                    )
                }
            }
        }
    }
}

private const val TW = 160f
private const val TH = 150f

private val thumbRidges = listOf(
    PathParser().parsePathString("M0 96 L34 72 L62 92 L96 62 L132 96 L160 82 V150 H0 Z").toPath() to Color(0xFF251243),
    PathParser().parsePathString("M0 124 L40 100 L84 124 L124 104 L160 122 V150 H0 Z").toPath() to Color(0xFF150A2B)
)

/** Mini sunset scene used as the thumbnail of a start-page tab (same artwork as the demo's first card). */
@Composable
private fun StartThumb() {
    Canvas(Modifier.fillMaxSize()) {
        val s = max(size.width / TW, size.height / TH)
        withTransform({
            translate((size.width - TW * s) / 2f, (size.height - TH * s) / 2f)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawRect(Color(0xFF3B1B62), size = Size(TW, TH))
            drawCircle(Color(0xFFFFB067), 22f, Offset(112f, 70f))
            thumbRidges.forEach { (path, color) -> drawPath(path, color) }
            drawRoundRect(
                Color.White,
                topLeft = Offset(12f, 12f),
                size = Size(136f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
    }
}
