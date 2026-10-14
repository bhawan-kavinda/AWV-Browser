package com.awv.browser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.awv.browser.ui.theme.Awv

enum class Fill { None, Soft, Solid }

class Shape(val path: Path, val fill: Fill, val stroke: Boolean)
class IconDef(val shapes: List<Shape>)

// ---- tiny SVG-like builders (24x24 viewBox, same path data as the demo) ----
private fun p(d: String, fill: Fill = Fill.None) =
    Shape(PathParser().parsePathString(d).toPath(), fill, true)

private fun circle(cx: Float, cy: Float, r: Float, fill: Fill = Fill.None, stroke: Boolean = true) =
    Shape(Path().apply { addOval(Rect(Offset(cx, cy), r)) }, fill, stroke)

private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Fill = Fill.None) =
    Shape(Path().apply { addRoundRect(RoundRect(x, y, x + w, y + h, r, r)) }, fill, true)

private fun icon(vararg s: Shape) = IconDef(s.toList())

object AwvIcons {
    val Shield = icon(
        p("M12 3l7.2 2.8v5.6c0 4.6-3 8-7.2 9.6-4.2-1.6-7.2-5-7.2-9.6V5.8z", Fill.Soft),
        p("M8.8 12.2l2.2 2.2 4.2-4.4")
    )
    val Mic = icon(
        rect(9f, 3f, 6f, 11f, 3f, Fill.Soft),
        p("M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3")
    )
    val Scan = icon(
        p("M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2"),
        rect(8f, 8f, 8f, 8f, 1.5f, Fill.Soft)
    )
    val Back = icon(p("M19 12H5M11 6l-6 6 6 6"))
    val Forward = icon(p("M5 12h14M13 6l6 6-6 6"))
    val Plus = icon(p("M12 5v14M5 12h14"))
    val Close = icon(p("M6 6l12 12M18 6L6 18"))
    val Chevron = icon(p("M9 6l6 6-6 6"))
    val Dots = icon(
        circle(12f, 5f, 2f, Fill.Solid, stroke = false),
        circle(12f, 12f, 2f, Fill.Solid, stroke = false),
        circle(12f, 19f, 2f, Fill.Solid, stroke = false)
    )
    val NewTab = icon(
        rect(3f, 5f, 18f, 14f, 3f, Fill.Soft),
        p("M12 9.5v5M9.5 12h5")
    )
    val Private = icon(
        p("M3 11h18M6 11l1.6-6h8.8L18 11", Fill.Soft),
        circle(8f, 16.5f, 2.6f),
        circle(16f, 16.5f, 2.6f),
        p("M10.6 16.2h2.8")
    )
    val Bookmark = icon(p("M7 3.5h10a1 1 0 0 1 1 1V20l-6-4.2L6 20V4.5a1 1 0 0 1 1-1z", Fill.Soft))
    val History = icon(
        circle(12f, 12f, 8.5f, Fill.Soft),
        p("M12 7.5V12l3 2")
    )
    val Download = icon(
        p("M4 15v3.5A1.5 1.5 0 0 0 5.5 20h13a1.5 1.5 0 0 0 1.5-1.5V15", Fill.Soft),
        p("M12 4v11M7.5 10.5L12 15l4.5-4.5")
    )
    val Desktop = icon(
        rect(3f, 4f, 18f, 12f, 2f, Fill.Soft),
        p("M8.5 20h7M12 16v4")
    )
    val Lock = icon(
        rect(5f, 11f, 14f, 9.5f, 2.5f, Fill.Soft),
        p("M8.5 11V8a3.5 3.5 0 0 1 7 0v3")
    )
    val LockOpen = icon(
        rect(5f, 11f, 14f, 9.5f, 2.5f, Fill.Soft),
        p("M8.5 11V8a3.5 3.5 0 0 1 6.6-1.6")
    )
    val Reload = icon(
        p("M20 12a8 8 0 1 1-2.4-5.7L20 8.5"),
        p("M20 4v4.5h-4.5")
    )
    val Globe = icon(
        circle(12f, 12f, 9f, Fill.Soft),
        p("M3 12h18M12 3c2.5 2.4 3.8 5.5 3.8 9s-1.3 6.6-3.8 9c-2.5-2.4-3.8-5.5-3.8-9s1.3-6.6 3.8-9z")
    )
    val Trash = icon(
        p("M5 7h14M10 7V4.5h4V7M7 7l.8 12.2a1 1 0 0 0 1 .8h6.4a1 1 0 0 0 1-.8L17 7", Fill.Soft),
        p("M10.5 11v5M13.5 11v5")
    )
    val Sun = icon(
        circle(12f, 12f, 4f, Fill.Soft),
        p("M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.3 5.3l1.6 1.6M17.1 17.1l1.6 1.6M18.7 5.3l-1.6 1.6M6.9 17.1l-1.6 1.6")
    )
    val Moon = icon(
        p("M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8z", Fill.Soft)
    )
    val Settings = icon(
        circle(12f, 12f, 3f, Fill.Soft),
        p("M12 3v2.5M12 18.5V21M3 12h2.5M18.5 12H21M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M18.4 5.6l-1.8 1.8M7.4 16.6l-1.8 1.8")
    )
}

@Composable
fun AwvIcon(
    icon: IconDef,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Awv.Ink,
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        scale(k, k, pivot = Offset.Zero) {
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            icon.shapes.forEach { s ->
                when (s.fill) {
                    Fill.Soft -> drawPath(s.path, tint.copy(alpha = tint.alpha * 0.16f))
                    Fill.Solid -> drawPath(s.path, tint)
                    Fill.None -> Unit
                }
                if (s.stroke) drawPath(s.path, tint, style = stroke)
            }
        }
    }
}
