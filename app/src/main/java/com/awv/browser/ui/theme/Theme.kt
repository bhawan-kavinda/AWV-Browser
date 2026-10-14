package com.awv.browser.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** System = follow the device's dark / light setting (default). Light / Dark = chosen with the home-screen toggle. */
enum class ThemeMode { System, Light, Dark }

/**
 * App-wide theme state. Colors in [Awv] read [dark], which is Compose state,
 * so every screen recomposes automatically when the mode changes.
 */
object AwvTheme {
    private var prefs: android.content.SharedPreferences? = null

    var mode by mutableStateOf(ThemeMode.System)
        private set

    /** Kept in sync with the device setting (see BrowserApp). */
    var systemDark by mutableStateOf(false)

    val dark: Boolean
        get() = when (mode) {
            ThemeMode.System -> systemDark
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

    /** Call once from MainActivity.onCreate, before setContent. */
    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("theme", Context.MODE_PRIVATE)
        prefs = p
        systemDark = isSystemDark(context)
        mode = runCatching { ThemeMode.valueOf(p.getString("mode", null) ?: "System") }
            .getOrDefault(ThemeMode.System)
    }

    /** Flip between dark and light. The choice is saved; until the first tap the app follows the device. */
    fun toggle() {
        mode = if (dark) ThemeMode.Light else ThemeMode.Dark
        prefs?.edit()?.putString("mode", mode.name)?.apply()
    }

    private fun isSystemDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}

/**
 * Design tokens taken from the browser demo.
 * Light values = original look. Dark values = blue-tinted near-black (navy) palette.
 */
object Awv {
    private val d get() = AwvTheme.dark

    val Bg get() = if (d) Color(0xFF080D1A) else Color(0xFF1B0F33)
    val Surface get() = if (d) Color(0xFF0D1324) else Color(0xFFF6F5FA)
    /** Cards, sheets, bars and fields (was plain white). */
    val Card get() = if (d) Color(0xFF151D33) else Color.White
    val Ink get() = if (d) Color(0xFFE8ECF7) else Color(0xFF14121F)
    val Muted get() = if (d) Color(0xFF8E9AB8) else Color(0xFF6B6880)
    val Line get() = if (d) Color(0xFF243049) else Color(0xFFE6E4EE)
    val Disabled get() = if (d) Color(0xFF47536F) else Color(0xFFB9B6C8)
    val Track get() = if (d) Color(0xFF1C2640) else Color(0xFFE9E7F1)

    val Brand get() = if (d) Color(0xFFFF6B45) else Color(0xFFD93A12)
    val BrandSoft get() = if (d) Color(0xFF3A2327) else Color(0xFFFFEDE6)
    val Coral = Color(0xFFFF5B2E)
    val Orange = Color(0xFFFF6A3D)

    val Purple get() = if (d) Color(0xFF9A86F5) else Color(0xFF5B3FD1)
    val Pink = Color(0xFFC42B6F)
    val Teal = Color(0xFF0E8F7A)

    val Shadow get() = if (d) Color(0xB3000000) else Color(0x590A041E)
}

/**
 * Demo uses Manrope. To enable it: put manrope_regular/medium/bold/extrabold.ttf in
 * app/src/main/res/font/ and replace this with a FontFamily(Font(R.font.manrope_...)) list.
 */
val AwvFont: FontFamily = FontFamily.SansSerif
