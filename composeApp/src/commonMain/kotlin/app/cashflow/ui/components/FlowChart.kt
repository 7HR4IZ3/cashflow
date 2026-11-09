package app.cashflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cashflow.core.ChartStyle
import app.cashflow.core.Entry
import app.cashflow.core.Money
import app.cashflow.core.totals
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.theme.LocalMotion
import app.cashflow.ui.util.PaperDates
import app.cashflow.ui.util.Period

/** One bucket of the chart: what came in and went out in that day, week or month. */
data class FlowPoint(val start: Long, val label: String, val inMinor: Long, val outMinor: Long)

object FlowSeries {
    private const val MAX_BUCKETS = 36

    /**
     * Buckets use the same eligibility rules as the headline totals: completed, same currency, no transfers.
     * "Today" still shows the last week so a single day is not one lonely dot.
     */
    fun build(entries: List<Entry>, currency: String, period: Period, today: Long?): List<FlowPoint> {
        if (today == null) return emptyList()
        val dated = entries.mapNotNull { e -> PaperDates.dayNumber(e.date)?.let { it to e } }
        val (from, step) = when (period) {
            Period.DAY, Period.WEEK -> (today - 6) to 1
            Period.MONTH -> {
                val (y, m) = PaperDates.yearMonth(today)
                val start = PaperDates.monthStartDay(y, m)
                (if (today - start < 6) today - 6 else start) to 1
            }
            Period.ALL -> {
                val first = dated.minOfOrNull { it.first } ?: (today - 6)
                val span = (today - first + 1).coerceAtLeast(7)
                when {
                    span <= 31 -> (today - span + 1) to 1
                    span <= 31 * 7 -> first to 7
                    else -> first to 30
                }
            }
        }
        val count = (((today - from) / step) + 1).toInt().coerceIn(1, MAX_BUCKETS)
        // Keep the newest buckets when history is long, so the right edge always means today.
        val origin = today - (count - 1) * step.toLong()
        return (0 until count).map { i ->
            val s = origin + i * step.toLong()
            val e = s + step - 1
            val inBucket = dated.filter { it.first in s..e }.map { it.second }
            val sum = runCatching { totals(inBucket, currency) }.getOrNull()
            FlowPoint(s, dayLabel(s), sum?.incoming ?: 0L, sum?.outgoing ?: 0L)
        }
    }

    fun dayLabel(day: Long): String {
        val (_, m, d) = PaperDates.daysToCivil(day)
        return "${PaperDates.monthShort(m)} $d"
    }
}

/**
 * "The Current": inflow rises above a centre line, outflow falls below it, edge to edge. It reads like a pulse:
 * a good stretch swells upward in citron, a heavy one drops away in warm white. Drag to scrub a day.
 * The shapes summarise; exact figures stay in the headline, the split bar and the feed.
 */
@Composable
fun FlowChart(
    points: List<FlowPoint>,
    currency: String,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 148.dp,
    style: ChartStyle = ChartStyle.PULSE,
    /** Previews in Settings are look-only: no scrubbing, so the card around them stays tappable. */
    interactive: Boolean = true,
    showAxis: Boolean = true,
) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    val peak = (points.maxOfOrNull { maxOf(it.inMinor, it.outMinor) } ?: 0L).coerceAtLeast(1L)
    val reveal = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(points) {
        if (motion.reduced) reveal.snapTo(1f) else {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(780, easing = FastOutSlowInEasing))
        }
    }
    val quiet = points.all { it.inMinor == 0L && it.outMinor == 0L }
    val describe = if (quiet) "No money moved in this window."
    else "Money flow chart. " + (selected?.let { points.getOrNull(it) }?.let {
        "${it.label}: in ${Money.display(it.inMinor, currency)}, out ${Money.display(it.outMinor, currency)}."
    } ?: "Drag to read a day.")
    val outInk = c.ink2

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = describe }
                .then(
                    if (!interactive) Modifier else Modifier
                        .pointerInput(points.size, style) {
                            detectTapGestures { pos ->
                                val i = indexAt(pos.x, size.width.toFloat(), points.size, style)
                                onSelect(if (i == selected) null else i)
                            }
                        }
                        .pointerInput(points.size, style) {
                            detectDragGestures(
                                onDragEnd = { },
                                onDrag = { change, _ -> onSelect(indexAt(change.position.x, size.width.toFloat(), points.size, style)) },
                            )
                        },
                ),
        ) {
            val w = size.width
            val mid = size.height / 2f
            val room = mid - 10.dp.toPx()
            val grow = reveal.value
            if (style != ChartStyle.PULSE) {
                drawBaselineChart(style, points, selected, grow, peak, c.citron, outInk, c.hairline, c.ink, c.lineStrong)
                return@Canvas
            }
            // A short window reads best as a row of pulses; a long one as a flowing curve.
            val bars = points.size <= 14
            fun x(i: Int) = when {
                bars -> w / points.size * (i + 0.5f)
                points.size <= 1 -> w / 2
                else -> w * i / (points.size - 1)
            }
            val up = points.indices.map { Offset(x(it), mid - room * grow * (points[it].inMinor.toFloat() / peak.toFloat())) }
            val down = points.indices.map { Offset(x(it), mid + room * grow * (points[it].outMinor.toFloat() / peak.toFloat())) }

            // Faint depth rings either side of the axis so height has a scale without numbers.
            listOf(1f).forEach { f ->
                drawLine(c.hairline.copy(alpha = 0.28f), Offset(0f, mid - room * f), Offset(w, mid - room * f), strokeWidth = 1.dp.toPx())
                drawLine(c.hairline.copy(alpha = 0.28f), Offset(0f, mid + room * f), Offset(w, mid + room * f), strokeWidth = 1.dp.toPx())
            }

            if (bars) {
                val slot = w / points.size
                val bw = minOf(slot * 0.5f, 18.dp.toPx())
                val r = androidx.compose.ui.geometry.CornerRadius(bw / 2, bw / 2)
                points.indices.forEach { i ->
                    val on = selected == null || selected == i
                    val a = if (on) 1f else 0.35f
                    val top = up[i].y
                    val bottom = down[i].y
                    // A hairline stub keeps empty days visible as part of the rhythm.
                    drawRoundRect(
                        c.citron.copy(alpha = (if (points[i].inMinor > 0) 0.95f else 0.18f) * a),
                        topLeft = Offset(x(i) - bw / 2, minOf(top, mid - 2.dp.toPx())), size = androidx.compose.ui.geometry.Size(bw, mid - minOf(top, mid - 2.dp.toPx())), cornerRadius = r,
                    )
                    drawRoundRect(
                        outInk.copy(alpha = (if (points[i].outMinor > 0) 0.7f else 0.14f) * a),
                        topLeft = Offset(x(i) - bw / 2, mid), size = androidx.compose.ui.geometry.Size(bw, maxOf(bottom, mid + 2.dp.toPx()) - mid), cornerRadius = r,
                    )
                }
            } else {
                if (up.size > 1) {
                    val area = smooth(up).apply { lineTo(up.last().x, mid); lineTo(up.first().x, mid); close() }
                    drawPath(area, Brush.verticalGradient(listOf(c.citron.copy(alpha = 0.55f), c.citron.copy(alpha = 0.04f)), startY = mid - room, endY = mid))
                    val under = smooth(down).apply { lineTo(down.last().x, mid); lineTo(down.first().x, mid); close() }
                    drawPath(under, Brush.verticalGradient(listOf(outInk.copy(alpha = 0.04f), outInk.copy(alpha = 0.34f)), startY = mid, endY = mid + room))
                }
                line(down, outInk, 1.8.dp.toPx())
                line(up, c.citron, 2.6.dp.toPx())
            }
            // The axis sits on top so the two shapes meet cleanly.
            drawLine(c.lineStrong, Offset(0f, mid), Offset(w, mid), strokeWidth = 1.2.dp.toPx())

            val head = selected?.takeIf { it in points.indices }
            if (head != null) {
                drawLine(c.ink.copy(alpha = 0.5f), Offset(x(head), 0f), Offset(x(head), size.height), strokeWidth = 1.dp.toPx())
                if (!bars) {
                    drawCircle(c.bg, 6.dp.toPx(), up[head]); drawCircle(c.citron, 4.5.dp.toPx(), up[head])
                    drawCircle(c.bg, 6.dp.toPx(), down[head]); drawCircle(outInk, 4.5.dp.toPx(), down[head])
                }
            } else if (!bars && up.isNotEmpty() && grow > 0.95f) {
                // A live dot at "now", pulsing softly, so the chart feels like a running record.
                drawCircle(c.citron.copy(alpha = 0.18f), 9.dp.toPx(), up.last())
                drawCircle(c.citron, 3.8.dp.toPx(), up.last())
                drawCircle(outInk, 3.2.dp.toPx(), down.last())
            }
        }
        if (showAxis) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(points.firstOrNull()?.label ?: "", style = CamelType.LedgerLabel, color = c.faint)
            Box(Modifier.weight(1f))
            Text(if (points.isEmpty()) "" else "Now", style = CamelType.LedgerLabel, color = c.faint)
        }
    }
}

private fun indexAt(x: Float, width: Float, count: Int, style: ChartStyle = ChartStyle.PULSE): Int {
    if (count <= 1 || width <= 0f) return 0
    if (style == ChartStyle.BARS) return (x / width * count).toInt().coerceIn(0, count - 1)
    if (style == ChartStyle.LINES) return ((x / width) * (count - 1) + 0.5f).toInt().coerceIn(0, count - 1)
    // Same rule the drawing uses: short windows are slots, long ones run edge to edge.
    if (count <= 14) return (x / width * count).toInt().coerceIn(0, count - 1)
    return ((x / width) * (count - 1) + 0.5f).toInt().coerceIn(0, count - 1)
}

private fun DrawScope.line(pts: List<Offset>, color: Color, widthPx: Float) {
    if (pts.isEmpty()) return
    if (pts.size == 1) { drawCircle(color, widthPx, pts.first()); return }
    drawPath(smooth(pts), color, style = Stroke(widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Control points share the neighbour's height so a curve never overshoots the data. */
private fun smooth(pts: List<Offset>): Path {
    val path = Path()
    path.moveTo(pts.first().x, pts.first().y)
    for (i in 1 until pts.size) {
        val a = pts[i - 1]; val b = pts[i]
        val mid = (a.x + b.x) / 2f
        path.cubicTo(mid, a.y, mid, b.y, b.x, b.y)
    }
    return path
}

/** Lines and Bars share one rule: everything stands on a baseline at the bottom, taller means more. */
private fun DrawScope.drawBaselineChart(
    style: ChartStyle, points: List<FlowPoint>, selected: Int?, grow: Float, peak: Long,
    inColor: Color, outColor: Color, hairline: Color, ink: Color, axis: Color,
) {
    val n = points.size
    if (n == 0) return
    val w = size.width
    val padTop = 10.dp.toPx()
    val bottom = size.height - 6.dp.toPx()
    val room = bottom - padTop
    fun level(v: Long) = room * grow * (v.toFloat() / peak.toFloat())

    // A few quiet guides so height has a scale without numbers.
    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { f ->
        drawLine(hairline.copy(alpha = 0.22f), Offset(0f, bottom - room * f), Offset(w, bottom - room * f), strokeWidth = 1.dp.toPx())
    }

    if (style == ChartStyle.BARS) {
        val slot = w / n
        val group = minOf(slot * 0.8f, 34.dp.toPx())
        val bar = (group - 2.dp.toPx()) / 2f
        val radius = androidx.compose.ui.geometry.CornerRadius(minOf(bar / 2f, 5.dp.toPx()), minOf(bar / 2f, 5.dp.toPx()))
        points.indices.forEach { i ->
            val on = selected == null || selected == i
            val a = if (on) 1f else 0.35f
            val cx = slot * (i + 0.5f)
            val inH = maxOf(level(points[i].inMinor), 2.dp.toPx())
            val outH = maxOf(level(points[i].outMinor), 2.dp.toPx())
            drawRoundRect(inColor.copy(alpha = (if (points[i].inMinor > 0) 0.95f else 0.18f) * a), Offset(cx - bar - 1.dp.toPx(), bottom - inH), androidx.compose.ui.geometry.Size(bar, inH), radius)
            drawRoundRect(outColor.copy(alpha = (if (points[i].outMinor > 0) 0.7f else 0.14f) * a), Offset(cx + 1.dp.toPx(), bottom - outH), androidx.compose.ui.geometry.Size(bar, outH), radius)
        }
        drawLine(axis, Offset(0f, bottom), Offset(w, bottom), strokeWidth = 1.2.dp.toPx())
        selected?.takeIf { it in points.indices }?.let { drawLine(ink.copy(alpha = 0.4f), Offset(slot * (it + 0.5f), padTop), Offset(slot * (it + 0.5f), bottom), strokeWidth = 1.dp.toPx()) }
        return
    }

    // LINES: two overlaid lines, money in softly filled underneath.
    val padX = 8.dp.toPx()
    fun x(i: Int) = if (n <= 1) w / 2 else padX + (w - 2 * padX) * i / (n - 1)
    val up = points.indices.map { Offset(x(it), bottom - level(points[it].inMinor)) }
    val down = points.indices.map { Offset(x(it), bottom - level(points[it].outMinor)) }
    if (up.size > 1) {
        val area = smooth(up).apply { lineTo(up.last().x, bottom); lineTo(up.first().x, bottom); close() }
        drawPath(area, Brush.verticalGradient(listOf(inColor.copy(alpha = 0.34f), inColor.copy(alpha = 0.02f)), startY = padTop, endY = bottom))
    }
    line(down, outColor, 1.9.dp.toPx())
    line(up, inColor, 2.7.dp.toPx())
    drawLine(axis, Offset(0f, bottom), Offset(w, bottom), strokeWidth = 1.2.dp.toPx())
    val head = selected?.takeIf { it in points.indices }
    if (head != null) {
        drawLine(ink.copy(alpha = 0.45f), Offset(x(head), padTop), Offset(x(head), bottom), strokeWidth = 1.dp.toPx())
        drawCircle(Color.Black.copy(alpha = 0.6f), 6.dp.toPx(), up[head]); drawCircle(inColor, 4.5.dp.toPx(), up[head])
        drawCircle(Color.Black.copy(alpha = 0.6f), 6.dp.toPx(), down[head]); drawCircle(outColor, 4.5.dp.toPx(), down[head])
    } else {
        drawCircle(inColor, 3.8.dp.toPx(), up.last())
        drawCircle(outColor, 3.2.dp.toPx(), down.last())
    }
}
