package com.awv.browser.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awv.browser.browser.BrowserController
import com.awv.browser.browser.BrowserTab
import com.awv.browser.browser.UrlUtils
import com.awv.browser.ui.components.AwvIcon
import com.awv.browser.ui.components.AwvIcons
import com.awv.browser.ui.components.tappable
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.awvText

/** Shield bottom sheet: on/off switch, blocked count for this page, and the "Protected this week" numbers. */
@Composable
fun ShieldSheet(c: BrowserController, tab: BrowserTab, onDismiss: () -> Unit) {
    val on = c.privacy.shieldOn
    val stats = c.privacy.stats
    val onPage = !tab.showHome && tab.url.isNotBlank()

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
                .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.padding(bottom = 6.dp).width(40.dp).height(4.dp).background(Awv.Line, RoundedCornerShape(2.dp)))

            // switch row
            Row(
                Modifier.fillMaxWidth().tappable(label = "Shield", role = Role.Switch) { c.toggleShield() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(44.dp).background(if (on) Awv.BrandSoft else Awv.Line, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AwvIcon(AwvIcons.Shield, size = 24.dp, tint = if (on) Awv.Brand else Awv.Muted, strokeWidth = 1.9f)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BasicText("Shield", style = awvText(17, FontWeight.ExtraBold, Awv.Ink))
                    BasicText(
                        if (on) "Blocking ads and trackers" else "Off: pages load everything",
                        style = awvText(13, FontWeight.Normal, Awv.Muted)
                    )
                }
                AwvSwitch(on)
            }

            if (onPage) {
                Row(
                    Modifier.fillMaxWidth().background(Awv.Surface, RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BasicText(
                        if (on) "%,d".format(tab.blockedCount) else "-",
                        style = awvText(30, FontWeight.ExtraBold, if (on) Awv.Brand else Awv.Disabled, ls = -.5f)
                    )
                    Column(Modifier.weight(1f)) {
                        BasicText("Blocked on this page", style = awvText(14, FontWeight.Bold, Awv.Ink))
                        BasicText(
                            UrlUtils.displayHost(tab.url),
                            style = awvText(12, FontWeight.Normal, Awv.Muted),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().background(Awv.Surface, RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicText("Protected this week", style = awvText(14, FontWeight.Bold, Awv.Ink))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("%,d".format(stats.trackersBlocked), "Trackers blocked")
                    Stat("${stats.dataSavedMb} MB", "Data saved")
                    Stat("${stats.timeSavedMin} min", "Time saved")
                }
                BasicText(
                    "Blocked count is exact. Data and time saved are estimates.",
                    style = awvText(11, FontWeight.Normal, Awv.Muted)
                )
            }
        }
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String) {
    Column(Modifier.weight(1f)) {
        BasicText(value, style = awvText(20, FontWeight.ExtraBold, Awv.Ink, ls = -.3f), maxLines = 1)
        BasicText(label, style = awvText(11, FontWeight.Normal, Awv.Muted))
    }
}

@Composable
private fun AwvSwitch(on: Boolean) {
    val track by animateColorAsState(if (on) Awv.Brand else Awv.Disabled, label = "switchTrack")
    val x by animateDpAsState(if (on) 22.dp else 0.dp, label = "switchThumb")
    Box(
        Modifier.width(52.dp).height(30.dp).background(track, RoundedCornerShape(15.dp)).padding(3.dp)
    ) {
        Box(Modifier.offset(x = x).size(24.dp).background(Color.White, CircleShape))
    }
}
