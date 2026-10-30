package app.cashflow.ui.components

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * Cashflow's icon vocabulary, drawn to the concept spec: one consistent stroke
 * language (2px default, round caps) on a 24 grid. The financial direction
 * arrows follow a single convention everywhere — incoming = an arrow pointing
 * to the lower-left, outgoing = an arrow pointing to the upper-right, never the
 * old up/down double meaning. Material core icons appear only where the exact
 * glyph matches this language (house, plus, cog, X, pencil, trash); every
 * Cashflow-specific meaning is a custom vector so meaning never drifts.
 */
object CFIcons {

    /** Money in: arrow to the lower-left (financial convention). */
    val ArrowIn: ImageVector = icon("In") {
        s(2.6f) {
            moveTo(18f, 6f)
            lineTo(6.5f, 17.5f)
            moveTo(6.5f, 10.5f)
            verticalLineTo(17.5f)
            lineTo(13.5f, 17.5f)
        }
    }

    /** Money out: arrow to the upper-right (financial convention). */
    val ArrowOut: ImageVector = icon("Out") {
        s(2.6f) {
            moveTo(6f, 18f)
            lineTo(17.5f, 6.5f)
            moveTo(10.5f, 6.5f)
            horizontalLineTo(17.5f)
            verticalLineTo(13.5f)
        }
    }

    /** Review tray: an open tray; the numeric badge rides on it. */
    val Tray: ImageVector = icon("Tray") {
        s {
            moveTo(3f, 6f)
            horizontalLineTo(21f)
            verticalLineTo(12f)
            arcTo(6f, 6f, 0f, false, true, 15f, 18f)
            horizontalLineTo(9f)
            arcTo(6f, 6f, 0f, false, true, 3f, 12f)
            close()
            moveTo(7f, 12f)
            horizontalLineTo(9f)
            moveTo(13f, 12f)
            horizontalLineTo(15f)
        }
    }

    /** Receipt/document: opens the original alert evidence, never a warning card. */
    val Receipt: ImageVector = icon("Receipt") {
        s {
            moveTo(6f, 3f)
            horizontalLineTo(18f)
            verticalLineTo(21f)
            lineTo(15.5f, 19.5f)
            lineTo(13f, 21f)
            lineTo(10.5f, 19.5f)
            lineTo(8f, 21f)
            lineTo(6f, 19.5f)
            close()
            moveTo(9.5f, 8f)
            horizontalLineTo(14.5f)
            moveTo(9.5f, 12f)
            horizontalLineTo(14.5f)
        }
    }

    /** Ignore: minus-circle, a distinct control from record or trash. */
    val MinusCircle: ImageVector = icon("MinusCircle") {
        s {
            moveTo(3f, 12f)
            arcTo(9f, 9f, 0f, false, true, 21f, 12f)
            arcTo(9f, 9f, 0f, false, true, 3f, 12f)
            moveTo(8f, 12f)
            horizontalLineTo(16f)
        }
    }

    val ChevronLeft: ImageVector = icon("ChevronLeft", strokeW = 2.4f) {
        s {
            moveTo(14.5f, 5.5f)
            lineTo(8f, 12f)
            lineTo(14.5f, 18.5f)
        }
    }

    val ChevronRight: ImageVector = icon("ChevronRight", strokeW = 2.4f) {
        s {
            moveTo(9.5f, 5.5f)
            lineTo(16f, 12f)
            lineTo(9.5f, 18.5f)
        }
    }

    /** Filter: funnel; a count badge rides on it while a filter is active. */
    val Funnel: ImageVector = icon("Funnel") {
        s {
            moveTo(4f, 5f)
            horizontalLineTo(20f)
            lineTo(13.5f, 13f)
            verticalLineTo(18f)
            lineTo(10.5f, 21f)
            verticalLineTo(13f)
            close()
        }
    }

    /** Search: magnifier; badge + spoken query state while a query is active. */
    val Magnifier: ImageVector = icon("Magnifier", strokeW = 2.1f) {
        s {
            moveTo(4f, 11f)
            arcTo(7f, 7f, 0f, false, true, 18f, 11f)
            arcTo(7f, 7f, 0f, false, true, 4f, 11f)
            moveTo(20f, 20f)
            lineTo(16.5f, 16.5f)
        }
    }

    /** Keypad erase: a rounded backspace. */
    val Backspace: ImageVector = icon("Backspace", strokeW = 1.5f) {
        s {
            moveTo(9f, 5.5f)
            horizontalLineTo(19f)
            arcTo(1.5f, 1.5f, 0f, false, true, 20.5f, 7f)
            verticalLineTo(17f)
            arcTo(1.5f, 1.5f, 0f, false, true, 19f, 18.5f)
            horizontalLineTo(9f)
            lineTo(3.5f, 12f)
            close()
            moveTo(11.5f, 9.5f)
            lineTo(16f, 14f)
            moveTo(16f, 9.5f)
            lineTo(11.5f, 14f)
        }
    }

    /** Undo: curved restore arrow (toast + demo controls). */
    val Undo: ImageVector = icon("Undo") {
        s {
            moveTo(4f, 10f)
            arcTo(8f, 8f, 0f, false, true, 17.7f, 4.3f)
            lineTo(20f, 6.5f)
            moveTo(20f, 3f)
            verticalLineTo(6.5f)
            horizontalLineTo(16.5f)
        }
    }

    /** More details: the quiet three-dot row. */
    val MoreDots: ImageVector = icon("More") {
        f {
            gap(5f, 12f)
            gap(12f, 12f)
            gap(19f, 12f)
        }
    }

    /** Caret for the closed glyph pickers — 10x6 viewport. */
    val Caret: ImageVector = icon("Caret", strokeW = 1.8f, vw = 10f, vh = 6f, w = 10.dp, h = 6.dp) {
        s {
            moveTo(1f, 1f)
            lineTo(5f, 5f)
            lineTo(9f, 1f)
        }
    }

    /** The Cashflow mark: a small flow wave over one filled point. */
    val Mark: ImageVector = icon("Mark", strokeW = 2.4f) {
        s {
            moveTo(4f, 14f)
            arcTo(3.5f, 3.5f, 0f, false, true, 7.5f, 8f)
            arcTo(4.5f, 4.5f, 0f, false, false, 12f, 8f)
            arcTo(4.5f, 4.5f, 0f, false, true, 16.5f, 8f)
            arcTo(3.5f, 3.5f, 0f, false, true, 20f, 14f)
        }
        f {
            moveTo(10.4f, 17.5f)
            arcTo(1.6f, 1.6f, 0f, false, true, 13.6f, 17.5f)
            arcTo(1.6f, 1.6f, 0f, false, true, 10.4f, 17.5f)
        }
    }

    /** The explicit record/save check. */
    val CheckMark: ImageVector = icon("CheckMark", strokeW = 2.6f) {
        s {
            moveTo(4.5f, 12.5f)
            lineTo(9.5f, 17.5f)
            lineTo(19.5f, 7f)
        }
    }

    private inline fun icon(
        name: String,
        strokeW: Float = 2f,
        vw: Float = 24f,
        vh: Float = 24f,
        w: androidx.compose.ui.unit.Dp = 24.dp,
        h: androidx.compose.ui.unit.Dp = 24.dp,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = "Cashflow.$name",
            defaultWidth = w,
            defaultHeight = h,
            viewportWidth = vw,
            viewportHeight = vh,
        )
        builder.block()
        return builder.build()
    }

    /** A stroked contour; round caps and joins, matching the spec's `stroke-linecap`. */
    private inline fun ImageVector.Builder.s(
        strokeW: Float = 2f,
        block: PathBuilder.() -> Unit,
    ) {
        addPath(
            pathData = PathBuilder().apply(block).nodes,
            stroke = SolidColor(androidx.compose.ui.graphics.Color.White),
            strokeLineWidth = strokeW,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    /** A filled contour (the mark's point only — never decoration elsewhere). */
    private inline fun ImageVector.Builder.f(
        block: PathBuilder.() -> Unit,
    ) {
        addPath(
            pathData = PathBuilder().apply(block).nodes,
            fill = SolidColor(androidx.compose.ui.graphics.Color.White),
        )
    }

    /** Tiny press targets for the dot marks in [MoreDots]: a full-filled 1.3r disc. */
    private fun PathBuilder.gap(cx: Float, cy: Float) {
        moveTo(cx - 1.3f, cy)
        arcTo(1.3f, 1.3f, 0f, false, true, cx + 1.3f, cy)
        arcTo(1.3f, 1.3f, 0f, false, true, cx - 1.3f, cy)
    }
}
