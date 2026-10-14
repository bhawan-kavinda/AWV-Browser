package com.awv.browser.ui.menu

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awv.browser.browser.BrowserController
import com.awv.browser.browser.UrlUtils
import com.awv.browser.ui.components.AwvIcon
import com.awv.browser.ui.components.AwvIcons
import com.awv.browser.ui.components.IconDef
import com.awv.browser.ui.components.pressable
import com.awv.browser.ui.components.tappable
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.awvText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

// ============================================================ Bookmarks

@Composable
fun BookmarksScreen(c: BrowserController, onClose: () -> Unit) {
    val list by remember { c.db.bookmarks().all() }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    LibraryPage("Bookmarks", onClose) {
        if (list.isEmpty()) {
            Empty(AwvIcons.Bookmark, "No bookmarks yet", "Open a page, then tap Menu → Bookmark.")
        } else LazyColumn(contentPadding = listPad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.url }) { b ->
                Row2(
                    title = b.title.ifBlank { UrlUtils.displayHost(b.url) },
                    subtitle = UrlUtils.displayHost(b.url),
                    onClick = { c.load(c.active, b.url); onClose() },
                    trailing = AwvIcons.Trash, trailingLabel = "Remove bookmark",
                    onTrailing = { scope.launch { c.db.bookmarks().remove(b.url) } }
                )
            }
        }
    }
}

// ============================================================ History

@Composable
fun HistoryScreen(c: BrowserController, onClose: () -> Unit) {
    val list by remember { c.db.history().all() }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val timeFmt = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    val groups = remember(list) { list.groupBy { dayLabel(ctx, it.time) } }

    LibraryPage(
        "History", onClose,
        action = if (list.isNotEmpty()) "Clear all" else null,
        onAction = { scope.launch { c.db.history().clear() } }
    ) {
        if (list.isEmpty()) {
            Empty(AwvIcons.History, "No history", "Pages you visit appear here. Private tabs are never saved.")
        } else LazyColumn(contentPadding = listPad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { (day, entries) ->
                item(key = "h$day") {
                    BasicText(day, Modifier.padding(top = 10.dp, start = 4.dp), style = awvText(13, FontWeight.ExtraBold, Awv.Muted))
                }
                items(entries, key = { it.id }) { e ->
                    Row2(
                        title = e.title.ifBlank { UrlUtils.displayHost(e.url) },
                        subtitle = UrlUtils.displayHost(e.url) + "  ·  " + timeFmt.format(Date(e.time)),
                        onClick = { c.load(c.active, e.url); onClose() },
                        trailing = AwvIcons.Close, trailingLabel = "Remove from history",
                        onTrailing = { scope.launch { c.db.history().delete(e.id) } }
                    )
                }
            }
        }
    }
}

private fun dayLabel(ctx: Context, t: Long): String = when {
    DateUtils.isToday(t) -> "Today"
    DateUtils.isToday(t + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
    else -> DateUtils.formatDateTime(ctx, t, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR)
}

// ============================================================ Downloads

private class DownloadItem(
    val id: Long, val title: String, val status: Int,
    val total: Long, val done: Long, val time: Long
)

private fun queryDownloads(dm: DownloadManager): List<DownloadItem> {
    val out = ArrayList<DownloadItem>()
    dm.query(DownloadManager.Query()).use { cur ->
        while (cur.moveToNext()) {
            fun col(name: String) = cur.getColumnIndexOrThrow(name)
            out.add(
                DownloadItem(
                    id = cur.getLong(col(DownloadManager.COLUMN_ID)),
                    title = cur.getString(col(DownloadManager.COLUMN_TITLE)).orEmpty(),
                    status = cur.getInt(col(DownloadManager.COLUMN_STATUS)),
                    total = cur.getLong(col(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    done = cur.getLong(col(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    time = cur.getLong(col(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
                )
            )
        }
    }
    return out.sortedByDescending { it.time }
}

@Composable
fun DownloadsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val dm = remember { ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    var items by remember { mutableStateOf<List<DownloadItem>?>(null) }
    var tick by remember { mutableStateOf(0) }

    // Poll while the screen is open so progress bars move.
    LaunchedEffect(tick) {
        while (true) {
            items = withContext(Dispatchers.IO) { queryDownloads(dm) }
            delay(1000)
        }
    }

    LibraryPage("Downloads", onClose) {
        val list = items
        if (list != null && list.isEmpty()) {
            Empty(AwvIcons.Download, "No downloads", "Files you download from the web appear here.")
        } else LazyColumn(contentPadding = listPad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list.orEmpty(), key = { it.id }) { d ->
                val ok = d.status == DownloadManager.STATUS_SUCCESSFUL
                val running = d.status == DownloadManager.STATUS_RUNNING
                val size = { b: Long -> Formatter.formatShortFileSize(ctx, b) }
                val subtitle = when (d.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> "Done  ·  " + size(d.total.coerceAtLeast(d.done))
                    DownloadManager.STATUS_FAILED -> "Failed"
                    DownloadManager.STATUS_PAUSED -> "Paused"
                    DownloadManager.STATUS_PENDING -> "Waiting…"
                    else -> if (d.total > 0) "${d.done * 100 / d.total}%  ·  ${size(d.done)} of ${size(d.total)}" else size(d.done)
                }
                Row2(
                    title = d.title.ifBlank { "Download" },
                    subtitle = subtitle,
                    progress = if (running && d.total > 0) d.done.toFloat() / d.total else null,
                    onClick = { if (ok) openDownload(ctx, dm, d.id) },
                    trailing = if (ok) AwvIcons.Trash else AwvIcons.Close,
                    trailingLabel = if (ok) "Delete file" else "Cancel download",
                    onTrailing = { dm.remove(d.id); tick++ }
                )
            }
        }
    }
}

private fun openDownload(ctx: Context, dm: DownloadManager, id: Long) {
    try {
        val uri = dm.getUriForDownloadedFile(id) ?: throw IllegalStateException()
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, dm.getMimeTypeForDownloadedFile(id) ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(ctx, "No app can open this file", Toast.LENGTH_SHORT).show()
    }
}

// ============================================================ shared pieces

private val listPad = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)

@Composable
private fun LibraryPage(
    title: String,
    onClose: () -> Unit,
    action: String? = null,
    onAction: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Awv.Surface)
            .pointerInput(Unit) { detectTapGestures { } } // swallow taps so the page below never gets them
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(44.dp).pressable(label = "Back", onClick = onClose),
                contentAlignment = Alignment.Center
            ) { AwvIcon(AwvIcons.Back, tint = Awv.Ink, strokeWidth = 2f) }
            BasicText(title, Modifier.weight(1f), style = awvText(26, FontWeight.ExtraBold, Awv.Ink, ls = -.6f))
            if (action != null) {
                BasicText(
                    action,
                    Modifier.tappable(label = action, onClick = onAction).padding(8.dp),
                    style = awvText(14, FontWeight.ExtraBold, Awv.Brand)
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

@Composable
private fun Row2(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: IconDef,
    trailingLabel: String,
    onTrailing: () -> Unit,
    progress: Float? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .tappable(label = title, onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).background(Awv.BrandSoft, CircleShape), contentAlignment = Alignment.Center) {
            BasicText(
                title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•",
                style = awvText(16, FontWeight.ExtraBold, Awv.Brand)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(title, style = awvText(15, FontWeight.Bold, Awv.Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(subtitle, style = awvText(12, FontWeight.Medium, Awv.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress != null) {
                Box(Modifier.padding(top = 3.dp).fillMaxWidth().height(4.dp).background(Awv.Track, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(Awv.Coral, RoundedCornerShape(2.dp)))
                }
            }
        }
        Box(
            Modifier.size(44.dp).pressable(label = trailingLabel, onClick = onTrailing),
            contentAlignment = Alignment.Center
        ) { AwvIcon(trailing, size = 20.dp, tint = Awv.Muted, strokeWidth = 1.9f) }
    }
}

@Composable
private fun Empty(icon: IconDef, title: String, hint: String) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Box(Modifier.size(84.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            AwvIcon(icon, size = 40.dp, tint = Awv.Purple, strokeWidth = 1.6f)
        }
        BasicText(title, style = awvText(18, FontWeight.ExtraBold, Awv.Ink))
        BasicText(hint, style = awvText(14, FontWeight.Normal, Awv.Muted, lh = 21).copy(textAlign = TextAlign.Center))
    }
}
