package com.awv.browser.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.awv.browser.ui.home.Shortcut
import org.json.JSONArray
import org.json.JSONObject

/** Start-page shortcut tiles added by the user. Saved across launches, newest last. */
class ShortcutStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)

    val items = mutableStateListOf<Shortcut>().apply { addAll(read()) }

    /** The start page has room for [MAX] tiles (two rows including the "Add" tile). */
    val isFull: Boolean get() = items.size >= MAX

    /** Returns false when the list is full or the site is already there. */
    fun add(shortcut: Shortcut): Boolean {
        if (isFull) return false
        val key = shortcut.url.trimEnd('/')
        if (items.any { it.url.trimEnd('/').equals(key, ignoreCase = true) }) return false
        items.add(shortcut)
        save()
        return true
    }

    fun remove(shortcut: Shortcut) {
        items.remove(shortcut)
        save()
    }

    private fun read(): List<Shortcut> = try {
        val array = JSONArray(prefs.getString("list", "[]"))
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("url")
            if (!url.startsWith("http://") && !url.startsWith("https://")) return@mapNotNull null
            Shortcut(o.optString("title").ifBlank { url }, url)
        }.take(MAX)
    } catch (e: Exception) {
        emptyList()
    }

    private fun save() {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("title", it.title).put("url", it.url)) }
        prefs.edit().putString("list", array.toString()).apply()
    }

    companion object {
        const val MAX = 7
    }
}
