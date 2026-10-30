package app.cashflow.ui.util

import app.cashflow.core.Direction
import app.cashflow.core.Kind
import app.cashflow.core.Money
import app.cashflow.core.PaymentStatus

/**
 * Plain words for money meanings, plus the currency glyph/word tables behind
 * the closed glyph pickers. Every glyph stays paired with one of these words
 * in the interface; no icon ever carries a financial meaning alone.
 */
object MoneyWords {

    fun direction(direction: Direction): String = when (direction) {
        Direction.IN -> "Money in"
        Direction.OUT -> "Money out"
    }

    /** The verb used in confirmation sentences: "Recorded 8,500 sent to Ada". */
    fun directionVerb(direction: Direction): String = when (direction) {
        Direction.IN -> "received"
        Direction.OUT -> "sent"
    }

    fun kind(kind: Kind): String = when (kind) {
        Kind.INCOME -> "Income"
        Kind.EXPENSE -> "Expense"
        Kind.TRANSFER -> "Own transfer"
        Kind.REFUND -> "Refund"
        Kind.FEE -> "Fee"
        Kind.UNCLASSIFIED -> "Not yet classified"
    }

    /** The constraint a kind forces on direction, as one short sentence. */
    fun directionHint(kind: Kind): String? = when (kind) {
        Kind.INCOME -> "Income must be money IN"
        Kind.EXPENSE -> "Expense must be money OUT"
        Kind.FEE -> "A fee goes OUT"
        else -> null
    }

    fun status(status: PaymentStatus): String = when (status) {
        PaymentStatus.COMPLETED -> "Completed"
        PaymentStatus.PENDING -> "Pending"
        PaymentStatus.FAILED -> "Payment failed"
    }

    /** Spoken currency word for accessibility phrases: "8,500 naira". */
    fun currencyName(code: String): String = when (code.uppercase()) {
        "NGN" -> "naira"
        "USD" -> "dollars"
        "GBP" -> "pounds sterling"
        "EUR" -> "euros"
        "CAD" -> "Canadian dollars"
        "GHS" -> "cedis"
        "KES" -> "Kenyan shillings"
        "ZAR" -> "rand"
        "INR" -> "rupees"
        "JPY" -> "yen"
        "KWD" -> "Kuwaiti dinars"
        else -> code
    }

    /** Human long name shown in the open currency chooser. */
    fun currencyLong(code: String): String = when (code.uppercase()) {
        "NGN" -> "Nigerian naira"
        "USD" -> "US dollar"
        "GBP" -> "British pound"
        "EUR" -> "Euro"
        "CAD" -> "Canadian dollar"
        "GHS" -> "Ghanaian cedi"
        "KES" -> "Kenyan shilling"
        "ZAR" -> "South African rand"
        "INR" -> "Indian rupee"
        "JPY" -> "Japanese yen"
        "KWD" -> "Kuwaiti dinar"
        else -> code
    }

    /**
     * The closed picker shows one currency glyph — real-world symbols, never
     * ISO codes — so the folded state stays unambiguous without words.
     */
    fun symbol(code: String): String = when (code.uppercase()) {
        "NGN" -> "\u20A6"
        "USD" -> "$"
        "GBP" -> "\u00A3"
        "EUR" -> "\u20AC"
        "CAD" -> "C$"
        "GHS" -> "\u20B5"
        "KES" -> "KSh"
        "ZAR" -> "R"
        "INR" -> "\u20B9"
        "JPY" -> "\u00A5"
        "KWD" -> "KD"
        else -> "?"
    }

    /** True when the code is one of the ledger's real supported members. */
    fun isSupported(code: String?): Boolean =
        code != null && Money.currencies.contains(code.uppercase())

    fun origin(source: String): String =
        if (source.equals("Manual", ignoreCase = true)) "Entered by hand" else "Captured from $source"
}
