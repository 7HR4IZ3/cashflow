package app.cashflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
 * Cashflow's night-native world.
 *
 * Intent: a quiet dark ledger, read moments after a bank alert buzzes — matte
 * charcoal, one citron accent for action and incoming money, lavender only
 * for uncertainty, red only for failure and destructive moments. No cards on
 * black beyond the one-level layering below; borders and surface shifts do
 * all structural work. Cashflow is dark-native, so the theme always renders
 * the night palette.
 *
 * Surface ladder (each step one quiet lift):
 *   bg #101213  ->  surface #1A1D1E  ->  field #262B2C
 * Fields sit slightly darker/lighter-forward as "write here" ground, no
 * heavy borders, no shadows except the floating dock's one quiet lift.
 */
@Immutable
data class CashflowColors(
    // Surfaces: bg (page), surface (stages, tiles, dock), sheet (floating panels).
    val bg: Color,
    val surface: Color,
    val sheet: Color,
    // Field: recessed inputs and pressed controls, one lift above surface.
    val field: Color,
    val fieldSoft: Color,
    // Ink levels: primary, calm emphasis, muted, faint (placeholder only).
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val faint: Color,
    // Hairlines: soft row separators, standard module borders, emphasis.
    val hairline: Color,
    val line: Color,
    val lineStrong: Color,
    // Citron is the single brand/action accent: primary action, selected
    // surfaces, money-in. Never failure, never decoration.
    val citron: Color,
    val citronWash: Color,
    val onCitron: Color,
    // Lavender only for uncertainty (review badges, "amount missing").
    val lavender: Color,
    val lavenderWash: Color,
    // Red only for failure and the destructive moment — never for "out".
    val red: Color,
    val redWash: Color,
    val dark: Boolean,
)

fun nightCashflowColors() = CashflowColors(
    bg = Color(0xFF101213),
    surface = Color(0xFF1A1D1E),
    sheet = Color(0xFF1A1D1E),
    field = Color(0xFF262B2C),
    fieldSoft = Color(0xFF16191A),
    ink = Color(0xFFF5F4EE),
    ink2 = Color(0xFFC9CDC9),
    muted = Color(0xFFA0A7A6),
    faint = Color(0xFF5E666B),
    hairline = Color(0x0EF5F4EE),
    line = Color(0x17F5F4EE),
    lineStrong = Color(0x38F5F4EE),
    citron = Color(0xFFE1F58C),
    citronWash = Color(0x1AE1F58C),
    onCitron = Color(0xFF1B2405),
    lavender = Color(0xFFB7A9E8),
    lavenderWash = Color(0x29B7A9E8),
    red = Color(0xFFF08578),
    redWash = Color(0x29F08578),
    dark = true,
)

@Deprecated("Renamed for the night-current palette; use nightCashflowColors().")
fun lightCashflowColors() = nightCashflowColors()

val LocalCashflowColors = staticCompositionLocalOf { nightCashflowColors() }

/** CashflowShapes: 12dp controls, 14dp fields, 16dp cards, 22dp stages, 26dp dock & sheets. */
val CashflowShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/*
 * Type roles:
 *  - Voice: human wording — headings, sentences, empty states; never money.
 *  - Ledger: every amount, figure, row; tabular figures always.
 *  - Reference (mono): evidence text and IDs only, never prose.
 * Bundled faces arrive with the resource phase; the platform family stands in.
 */
object CamelType {
    val Ledger = FontFamily.SansSerif
    val Voice = FontFamily.SansSerif
    val Mono = FontFamily.Monospace

    val VoiceHeadline = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp,
        letterSpacing = (-0.4).sp,
    )
    /** 1.35rem hero used on the intro screen. */
    val VoiceHero = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    )
    val VoiceTitle = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
    )
    val VoiceBody = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp,
    )
    /**
     * Feed-row lead: 1.02rem, weight 650, tiny tight tracking — the name row
     * of the feed.
     */
    val RowLead = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    )
    /** Amount display for the focused Review question: 2.1rem, weight 750. */
    val LedgerStage = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp,
        letterSpacing = (-1.0).sp, fontFeatureSettings = "tnum",
    )
    /** The Add-calculator hero: 3.4rem tabular, tight tracking. */
    val LedgerDisplay = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 56.sp,
        letterSpacing = (-2.0).sp, fontFeatureSettings = "tnum",
    )
    /** Tile values for the In/Out pair: 1.05rem tabular, weight 700. */
    val LedgerTile = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp,
        letterSpacing = (-0.2).sp, fontFeatureSettings = "tnum",
    )
    /** Long tile values shrink instead of clipping — money is never truncated. */
    val LedgerTileLong = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp,
        letterSpacing = 0.sp, fontFeatureSettings = "tnum",
    )
    val LedgerAmount = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp,
        letterSpacing = (-0.2).sp, fontFeatureSettings = "tnum",
    )
    val LedgerInput = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp,
        fontFeatureSettings = "tnum",
    )
    val LedgerBody = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
    )
    /** Tiny uppercase kicker labels over fields and tiles. */
    val Kicker = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp,
        letterSpacing = 0.8.sp, fontFeatureSettings = "tnum",
    )
    /** Uppercase date-group headers: .74rem, weight 700, +.09em tracking. */
    val GroupHeader = TextStyle(
        fontFamily = Voice, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp,
        letterSpacing = 1.0.sp, fontFeatureSettings = "tnum",
    )
    val LedgerLabel = TextStyle(
        fontFamily = Ledger, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp,
        fontFeatureSettings = "tnum",
    )
    val Reference = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp,
    )
}

/** Restrained motion timings; the scale is fixed milliseconds, never distance-based. */
@Immutable
data class Motion(val reduced: Boolean) {
    val none: Int get() = 0
    /** 160ms micro-interactions (chips, icon buttons). */
    val fast: Int get() = if (reduced) 0 else 160
    /** ~200ms screen fades. */
    val med: Int get() = if (reduced) 0 else 200
    /** 220ms sheet entrances. */
    val slow: Int get() = if (reduced) 0 else 220
}

val LocalMotion = staticCompositionLocalOf { Motion(reduced = false) }

private fun CashflowColors.materialColors() = darkColorScheme().copy(
    primary = citron,
    onPrimary = onCitron,
    primaryContainer = citronWash,
    onPrimaryContainer = ink,
    secondary = muted,
    onSecondary = bg,
    secondaryContainer = lavenderWash,
    onSecondaryContainer = ink,
    tertiary = lavender,
    onTertiary = Color(0xFF191327),
    tertiaryContainer = lavenderWash,
    onTertiaryContainer = ink,
    background = bg,
    onBackground = ink,
    surface = bg,
    onSurface = ink,
    surfaceVariant = surface,
    onSurfaceVariant = muted,
    surfaceContainerLowest = bg,
    surfaceContainerLow = bg,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = sheet,
    surfaceDim = bg,
    surfaceBright = surface,
    inverseSurface = ink,
    inverseOnSurface = bg,
    inversePrimary = citron,
    error = red,
    onError = Color(0xFF191327),
    errorContainer = redWash,
    onErrorContainer = ink,
    outline = lineStrong,
    outlineVariant = line,
    scrim = Color(0x99060707),
)

/**
 * CashflowTheme: the one theme; Cashflow is night-native, so no light variant
 * is provided. [reduceMotion] is the honest, manual shaping of motion on top
 * of any platform posture the caller already removed.
 */
@Composable
fun CashflowTheme(
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = nightCashflowColors()
    CompositionLocalProvider(
        LocalCashflowColors provides colors,
        LocalMotion provides Motion(reduceMotion),
    ) {
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
