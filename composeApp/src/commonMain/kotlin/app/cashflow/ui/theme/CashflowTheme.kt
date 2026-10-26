package app.cashflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cashflow's paper-and-ink world.
 *
 * Intent: a pocket ledger book, kept deliberately. Surfaces are warm paper,
 * structure arrives with one-point borders instead of shadows, and a single
 * accounting green does all brand work plus money-in work, and nothing else.
 *
 * Depth strategy: borders and washes only. No gradient exists anywhere.
 *
 * Text hierarchy is four inks (primary/secondary/tertiary/disabled), never
 * hierarchy by size alone. Money always renders in the Ledger face (tabular
 * figures); never in the Voice face.
 */
@Immutable
data class CashflowColors(
    // Surfaces: paper -> raise (cards, rows, bar) -> sop (floating sheets).
    val paper: Color,
    val raise: Color,
    val sop: Color,
    // Inset ground for form fields: a tone darker, is the "write here" signal.
    val inset: Color,
    // Four ink levels.
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val ink4: Color,
    // Hairlines: line for card edges, lineSoft for row separation.
    val line: Color,
    val lineSoft: Color,
    val lineStrong: Color,
    // Semantics: green is brand + money-in and nothing else. Out is ink, never red.
    val green: Color,
    val greenWash: Color,
    val onGreen: Color,
    val amber: Color,
    val amberWash: Color,
    val violet: Color,
    val violetWash: Color,
    val fail: Color,
    val failWash: Color,
    val read: Color,
    val dark: Boolean,
)

fun lightCashflowColors() = CashflowColors(
    paper = Color(0xFFF7F2E8),
    raise = Color(0xFFFCF8EF),
    sop = Color(0xFFFFFCF3),
    inset = Color(0xFFEFE8D9),
    ink = Color(0xFF221E17),
    ink2 = Color(0xFF6B6456),
    ink3 = Color(0xFF736B5D),
    ink4 = Color(0xFFB9B1A1),
    line = Color(0x1A221E17),
    lineSoft = Color(0x0F221E17),
    lineStrong = Color(0x33221E17),
    green = Color(0xFF0E6B43),
    greenWash = Color(0x1A0E6B43),
    onGreen = Color(0xFFF7F2E8),
    amber = Color(0xFF96650A),
    amberWash = Color(0x1F96650A),
    violet = Color(0xFF5645C2),
    violetWash = Color(0x1A5645C2),
    fail = Color(0xFFB3391F),
    failWash = Color(0x1AB3391F),
    read = Color(0xFF2F5DA8),
    dark = false,
)

fun darkCashflowColors() = CashflowColors(
    paper = Color(0xFF14120D),
    raise = Color(0xFF1D1A13),
    sop = Color(0xFF26221A),
    inset = Color(0xFF0F0D08),
    ink = Color(0xFFF0EBE0),
    ink2 = Color(0xFFBBB3A2),
    ink3 = Color(0xFF9F9788),
    ink4 = Color(0xFF6E675B),
    line = Color(0x1FF0EBE0),
    lineSoft = Color(0x12F0EBE0),
    lineStrong = Color(0x38F0EBE0),
    green = Color(0xFF3FA372),
    greenWash = Color(0x293FA372),
    onGreen = Color(0xFF14120D),
    amber = Color(0xFFD9A441),
    amberWash = Color(0x29D9A441),
    violet = Color(0xFF9A8BE8),
    violetWash = Color(0x299A8BE8),
    fail = Color(0xFFE57157),
    failWash = Color(0x29E57157),
    read = Color(0xFF7FA3DE),
    dark = true,
)

val LocalCashflowColors = staticCompositionLocalOf { lightCashflowColors() }

/** CashflowShapes: small 8dp chips/inputs, medium 16dp cards/rows, large 24dp sheets. */
val CashflowShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/*
 * Type roles (plan):
 *  - Voice: human wording, headings and empty-state lines; never money.
 *  - Ledger: every amount, list rows, dates, statuses. Tabular figures on.
 *  - Reference (Mono): IDs and evidence only, nothing prose-like.
 *
 * Bundled fonts arrive with the resource phase; these roles use the platform
 * family for now and swap to the bundled Ledger/Voice faces with no call-site
 * changes. `tnum` keeps amount columns aligned in the mean time.
 */
object CamelType {
    val Ledger = FontFamily.SansSerif
    val Voice = FontFamily.SansSerif
    val Mono = FontFamily.Monospace

    val VoiceHeadline = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp,
    )
    val VoiceTitle = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp,
    )
    val VoiceBody = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp,
    )
    val LedgerDisplay = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Medium, fontSize = 44.sp, lineHeight = 50.sp,
        fontFeatureSettings = "tnum",
    )
    val LedgerAmount = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp,
        fontFeatureSettings = "tnum",
    )
    val LedgerHero = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 40.sp,
        fontFeatureSettings = "tnum",
    )
    val LedgerInput = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 34.sp,
        fontFeatureSettings = "tnum",
    )
    val LedgerBody = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp,
        fontFeatureSettings = "tnum",
    )
    val LedgerLabel = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp,
        fontFeatureSettings = "tnum",
    )
    val Reference = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp,
    )
}

private fun CashflowColors.materialColors() = run {
    val base = if (dark) {
        androidx.compose.material3.darkColorScheme()
    } else {
        androidx.compose.material3.lightColorScheme()
    }
    base.copy(
        primary = green,
        onPrimary = onGreen,
        primaryContainer = greenWash,
        onPrimaryContainer = ink,
        secondary = ink2,
        onSecondary = paper,
        secondaryContainer = violetWash,
        onSecondaryContainer = ink,
        tertiary = violet,
        onTertiary = paper,
        tertiaryContainer = violetWash,
        onTertiaryContainer = ink,
        background = paper,
        onBackground = ink,
        surface = paper,
        onSurface = ink,
        surfaceVariant = raise,
        onSurfaceVariant = ink2,
        surfaceContainerLowest = paper,
        surfaceContainerLow = paper,
        surfaceContainer = raise,
        surfaceContainerHigh = raise,
        surfaceContainerHighest = sop,
        surfaceDim = paper,
        surfaceBright = raise,
        inverseSurface = ink,
        inverseOnSurface = paper,
        inversePrimary = green,
        error = fail,
        onError = paper,
        errorContainer = failWash,
        onErrorContainer = ink,
        outline = lineStrong,
        outlineVariant = line,
        scrim = if (dark) Color(0x66000000) else Color(0x66221E17),
    )
}

/** CashflowTheme: the one theme for the app; dark mode flips the same token set. */
@androidx.compose.runtime.Composable
fun CashflowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val colors = if (darkTheme) darkCashflowColors() else lightCashflowColors()
    CompositionLocalProvider(LocalCashflowColors provides colors) {
        MaterialTheme(
            colorScheme = colors.materialColors(),
            typography = Typography(
                displayLarge = CamelType.LedgerDisplay,
                headlineMedium = CamelType.VoiceHeadline,
                titleLarge = CamelType.VoiceTitle,
                titleMedium = CamelType.VoiceBody,
                bodyLarge = CamelType.VoiceBody,
                bodyMedium = CamelType.LedgerBody,
                labelMedium = CamelType.LedgerLabel,
            ),
            shapes = CashflowShapes,
            content = content,
        )
    }
}
