package app.cashflow.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import app.cashflow.core.Direction
import app.cashflow.core.Money
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.MoneyWords

/*
 * Amount display rules:
 *  - exact integer minor units rendered as full ISO digit strings — money is
 *    never truncated, clipped, or animated in value;
 *  - currency always visible at the row or header level;
 *  - direction is carried by an arrow glyph and a word, never a lone minus;
 *  - unknown currency speaks "unknown currency" and never a guessed symbol;
 *  - a negative figure carries its sign as a separate word in, not inside, the
 *    formatter (the formatter assumes a non-negative value).
 */

/**
 * AmountText renders "NGN 8,500.50" in the Ledger face. The spoken label
 * composes one phrase: "Received 8,500 naira" or "Sent 8,500 naira".
 */
@Composable
fun AmountText(
    minor: Long,
    currency: String,
    modifier: Modifier = Modifier,
    direction: Direction? = null,
    style: TextStyle = CamelType.LedgerAmount,
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

/** Amount with no value yet: a muted dash, never a zero and never animated. */
@Composable
fun AmountPending(
    currency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = CamelType.LedgerAmount,
) {
    Text(
        text = currency + " \u2014",
        modifier = modifier.semantics { contentDescription = "Amount not recorded yet" },
        style = style,
        color = LocalCashflowColors.current.faint,
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
    val name = if (MoneyWords.isSupported(currency)) MoneyWords.currencyName(currency) else "unknown currency"
    return "$spoken $name"
}

/**
 * Signed display for the net line: the minus is its own character before the
 * unsigned money, exactly as the concept logs it — "− NGN 8,750.50". Zero is
 * unsigned.
 */
fun signedMoney(minor: Long, currency: String): String = when {
    minor < 0 -> "\u2212 ${Money.display(-minor, currency)}"
    else -> Money.display(minor, currency)
}
