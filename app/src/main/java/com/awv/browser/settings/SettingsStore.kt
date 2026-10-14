package com.awv.browser.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Search engines offered in Settings. [searchUrl] ends right before the query text. */
enum class SearchEngine(val label: String, val searchUrl: String) {
    Google("Google", "https://www.google.com/search?q="),
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com/?q="),
    Bing("Bing", "https://www.bing.com/search?q="),
    Brave("Brave Search", "https://search.brave.com/search?q="),
    Ecosia("Ecosia", "https://www.ecosia.org/search?q=")
}

/** User settings saved across launches. Compose state, so the Settings screen follows changes live. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var engine by mutableStateOf(readEngine())
        private set

    fun setEngine(value: SearchEngine) {
        engine = value
        prefs.edit().putString("engine", value.name).apply()
    }

    private fun readEngine(): SearchEngine =
        runCatching { SearchEngine.valueOf(prefs.getString("engine", null) ?: SearchEngine.Google.name) }
            .getOrDefault(SearchEngine.Google)
}
