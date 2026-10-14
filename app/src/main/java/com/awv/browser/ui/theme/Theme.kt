package com.awv.browser.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Design tokens taken from the browser demo. */
object Awv {
    val Bg = Color(0xFF1B0F33)
    val Surface = Color(0xFFF6F5FA)
    val Ink = Color(0xFF14121F)
    val Muted = Color(0xFF6B6880)
    val Line = Color(0xFFE6E4EE)
    val Disabled = Color(0xFFB9B6C8)
    val Track = Color(0xFFE9E7F1)

    val Brand = Color(0xFFD93A12)
    val BrandSoft = Color(0xFFFFEDE6)
    val Coral = Color(0xFFFF5B2E)
    val Orange = Color(0xFFFF6A3D)

    val Purple = Color(0xFF5B3FD1)
    val Pink = Color(0xFFC42B6F)
    val Teal = Color(0xFF0E8F7A)

    val Shadow = Color(0x590A041E)
}

/**
 * Demo uses Manrope. To enable it: put manrope_regular/medium/bold/extrabold.ttf in
 * app/src/main/res/font/ and replace this with a FontFamily(Font(R.font.manrope_...)) list.
 */
val AwvFont: FontFamily = FontFamily.SansSerif
