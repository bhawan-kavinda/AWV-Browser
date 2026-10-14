package com.awv.browser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.awv.browser.ui.theme.AwvTheme
import kotlin.math.max

private const val W = 390f
private const val H = 844f

private val sky = arrayOf(
    0f to Color(0xFF150C33), .38f to Color(0xFF4A2275), .62f to Color(0xFFB8406E),
    .78f to Color(0xFFF2704F), .9f to Color(0xFFFFB067)
)
private val sun = arrayOf(0f to Color(0xFFFFE3A3), 1f to Color(0xFFFF9A5A))
private val halo = Color(0xFFFFB067)

// Dark mode: blue-tinted night sky, pale moon, navy ridges.
private val skyDark = arrayOf(
    0f to Color(0xFF050914), .45f to Color(0xFF0A1226), .75f to Color(0xFF0F1B38), 1f to Color(0xFF14264A)
)
private val moon = arrayOf(0f to Color(0xFFE9EEFF), 1f to Color(0xFF9FB2E0))
private val moonHalo = Color(0xFF7C93D6)

// x, y, radius, alpha
private val stars = listOf(
    floatArrayOf(40f, 250f, 1.4f, .8f), floatArrayOf(96f, 212f, 1f, .7f),
    floatArrayOf(158f, 272f, 1.2f, .6f), floatArrayOf(214f, 226f, 1.5f, .8f),
    floatArrayOf(284f, 262f, 1f, .6f), floatArrayOf(338f, 214f, 1.3f, .8f),
    floatArrayOf(360f, 296f, 1f, .5f), floatArrayOf(22f, 318f, 1f, .5f)
)

private fun path(d: String) = PathParser().parsePathString(d).toPath()

private val ridgesDark = listOf(
    path("M0 560 L58 506 L104 548 L172 470 L246 556 L300 514 L390 580 V844 H0 Z") to Color(0xFF16254A).copy(alpha = .85f),
    path("M0 620 L70 566 L128 612 L204 540 L282 622 L338 588 L390 628 V844 H0 Z") to Color(0xFF101C39),
    path("M0 690 L84 636 L150 684 L232 622 L316 694 L390 660 V844 H0 Z") to Color(0xFF0B1429),
    path("M0 760 L96 712 L170 750 L260 700 L390 756 V844 H0 Z") to Color(0xFF070D1C)
)

private val ridges = listOf(
    path("M0 560 L58 506 L104 548 L172 470 L246 556 L300 514 L390 580 V844 H0 Z") to Color(0xFF6A2A78).copy(alpha = .85f),
    path("M0 620 L70 566 L128 612 L204 540 L282 622 L338 588 L390 628 V844 H0 Z") to Color(0xFF3B1B62),
    path("M0 690 L84 636 L150 684 L232 622 L316 694 L390 660 V844 H0 Z") to Color(0xFF251243),
    path("M0 760 L96 712 L170 750 L260 700 L390 756 V844 H0 Z") to Color(0xFF150A2B)
)

/** Sunset + mountains wallpaper (the demo's SVG, drawn natively, scaled like preserveAspectRatio="slice"). */
@Composable
fun Wallpaper(modifier: Modifier = Modifier) {
    val dark = AwvTheme.dark
    Canvas(modifier.fillMaxSize()) {
        val s = max(size.width / W, size.height / H)
        val dx = (size.width - W * s) / 2f
        val dy = (size.height - H * s) / 2f
        withTransform({
            translate(dx, dy)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawRect(
                brush = Brush.linearGradient(colorStops = if (dark) skyDark else sky, start = Offset(0f, 0f), end = Offset(0f, H)),
                size = Size(W, H)
            )
            stars.forEach { drawCircle(Color.White.copy(alpha = it[3]), it[2], Offset(it[0], it[1])) }

            val c = Offset(268f, 520f)
            drawCircle((if (dark) moonHalo else halo).copy(alpha = if (dark) .08f else .16f), 120f, c)
            drawCircle((if (dark) moonHalo else halo).copy(alpha = if (dark) .12f else .22f), 84f, c)
            drawCircle(
                brush = Brush.linearGradient(colorStops = if (dark) moon else sun, start = Offset(0f, 464f), end = Offset(0f, 576f)),
                radius = if (dark) 44f else 56f, center = c
            )
            (if (dark) ridgesDark else ridges).forEach { (p, color) -> drawPath(p, color) }
        }
    }
}
