package app.cashflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cashflow.core.Direction
import app.cashflow.core.Money
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.MoneyWords
import kotlin.math.cos
import kotlin.math.sin

/*
 * Amount display rules:
 *  - numerals only, tabular figures, never animated in value;
 *  - currency always visible at the row or header level;
 *  - direction carried by a glyph plus a word, no lone minus signs;
 *  - unknown currency: the words "unknown currency", never a guessed symbol,
 *    and never inside confirmed totals.
 */

/**
 * AmountText renders "NGN 8,500" in the Ledger face. The spoken label composes
 * one phrase: "Received 8,500 naira" or "Sent 8,500 naira".
 */
@Composable
fun AmountText(
    minor: Long,
    currency: String,
    modifier: Modifier = Modifier,
    direction: Direction? = null,
    style: androidx.compose.ui.text.TextStyle = CamelType.LedgerAmount,
    color: Color? = null,
) {
    val colors = LocalCashflowColors.current
    Text(
        text = Money.display(minor, currency),
        modifier = modifier.semantics {
            contentDescription = spokenAmountPhrase(minor, currency, direction)
        },
        style = style,
        color = color ?: colors.ink,
    )
}

/** Amount with no known value yet: ruled dashes, never a zero. */
@Composable
fun AmountPending(
    currency: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = CamelType.LedgerAmount,
) {
    Text(
        text = currency + " —",
        modifier = modifier.semantics { contentDescription = "Amount not recorded yet" },
        style = style,
        color = LocalCashflowColors.current.ink3,
    )
}

fun spokenAmountPhrase(minor: Long, currency: String, direction: Direction? = null): String {
    val verb = when (direction) {
        Direction.IN -> "Received"
        Direction.OUT -> "Sent"
        null -> null
    }
    val figure = Money.display(minor, currency)
    val spoken = if (verb != null) "$verb ${figure.removePrefix("$currency ")}" else figure
    return "$spoken ${MoneyWords.currencyName(currency)}"
}

/**
 * DirectionGlyph: a circle-wrapped arrow. Money in points to the centre, money
 * out points away. It always sits near its word in the row.
 */
@Composable
fun DirectionGlyph(
    direction: Direction,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    description: String? = null,
) {
    val colors = LocalCashflowColors.current
    val settle = rememberStampSettle()
    val tint = when (direction) {
        Direction.IN -> colors.green
        Direction.OUT -> colors.ink
    }
    val label = description ?: MoneyWords.direction(direction)
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = label },
    ) {
        val r = this.size.minDimension / 2f
        val c = Offset(r, r)
        val stroke = r * 0.16f
        val angle = if (direction == Direction.IN) -PI_4 else PI_4
        val dx = (cos(angle) * r * 0.62).toFloat()
        val dy = (sin(angle) * r * 0.62).toFloat()
        rotate(degrees = settle.value) {
            drawCircle(color = tint, radius = r - stroke, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round))
            when (direction) {
                Direction.IN -> {
                    drawLine(tint, c + Offset(dx, -dy), c + Offset(dx * 0.12f, dy * 0.12f), stroke, StrokeCap.Round)
                    arrowHead(tint, c, Offset(dx, -dy), stroke)
                }
                Direction.OUT -> {
                    drawLine(tint, c - Offset(dx, dy) * 0.3f, c + Offset(dx, dy), stroke, StrokeCap.Round)
                    arrowHead(tint, c + Offset(dx, dy), Offset(dx, dy), stroke)
                }
            }
        }
    }
}

private val PI_4 = kotlin.math.PI / 4.0

private fun DrawScope.arrowHead(
    color: Color,
    at: Offset,
    move: Offset,
    width: Float,
) {
    val norm = move.div(move.getDistance().coerceAtLeast(0.001f))
    val perp = Offset(-norm.y, norm.x)
    val back = at - norm.times(width * 3.4f)
    val side = perp.times(width * 3.0f)
    drawLine(color, back + side, at, width, StrokeCap.Round)
    drawLine(color, back - side, at, width, StrokeCap.Round)
}

private fun Offset.div(scalar: Float) = Offset(x / scalar, y / scalar)
private fun Offset.times(scalar: Float) = Offset(x * scalar, y * scalar)

/** One settle rotation from 3 degrees to rest, 180 ms, on first appearance. */
@Composable
fun rememberStampSettle(): State<Float> {
    val angle = remember { Animatable(3f) }
    LaunchedEffect(Unit) {
        angle.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        )
    }
    return remember {
        // State view over the animatable's observable value, so each settling
        // frame still drives recomposition without animating the money itself.
        object : State<Float> {
            override val value: Float get() = angle.value
        }
    }
}

/** Compact "glyph + word" row used wherever a word must sit beside a glyph. */
@Composable
fun GlyphWord(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Spacing helper mirroring the 4 dp base scale. */
fun gap(steps: Int): Dp = (4 * steps).dp
