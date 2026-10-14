package com.awv.browser.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Shared text style builder (size in sp, letter-spacing in sp, line-height in sp). */
fun awvText(size: Int, weight: FontWeight, color: Color, ls: Float = 0f, lh: Int? = null) = TextStyle(
    fontFamily = AwvFont, fontSize = size.sp, fontWeight = weight, color = color,
    letterSpacing = ls.sp, lineHeight = lh?.sp ?: TextStyle.Default.lineHeight
)
