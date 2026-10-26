package app.cashflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cashflow.ui.theme.LocalCashflowColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Cashflow's signature: the connector line.
 *
 * One continuous, slightly irregular hand-drawn stroke at ONE constant stroke
 * width everywhere it appears. It is decoration only: it never carries data,
 * never grows with a total, never animates as a transition, and it is hidden
 * from screen readers. It marks "these things belong to one book".
 *
 * Appearances: under the Home hero figure, beneath Activity date headers,
 * across the confirmation card's top edge, under quiet empty states.
 */
@Composable
fun ConnectorLine(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color? = null,
    strokeWidth: Dp = ConnectorStroke,
    seed: Int = 3,
) {
    val colors = LocalCashflowColors.current
    val inkColor = color ?: if (colors.dark) colors.ink else colors.lineStrong
    val width = strokeWidth.value
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clearAndSetSemantics {},
    ) {
        val w = size.width
        val midY = size.height / 2f
        val step = 6f
        var i = 0
        var previous: Offset? = null
        while (i * step <= w) {
            val x = i * step
            val phase = (x / (w.coerceAtLeast(1f)) * 3.2f) + seed * 0.7f
            val wobble = sin(phase.toDouble() * PI) * 0.9 + sin(phase * 2.1) * 0.5
            val y = midY + wobble.toFloat()
            val point = Offset(x, y)
            val prev = previous
            if (prev != null && w > 0) {
                drawLine(color = inkColor, start = prev, end = point, strokeWidth = width)
            }
            previous = point
            i++
        }
    }
}

/** Constant everywhere in the app. Never scaled by any value. */
val ConnectorStroke: Dp = 1.5f.dp

/**
 * A short resting underline variant for quiet empty states and headers.
 */
@Composable
fun ConnectorUnderline(
    modifier: Modifier = Modifier,
    seed: Int = 5,
) {
    ConnectorLine(modifier = modifier.fillMaxWidth(0.42f), seed = seed)
}
