package app.cashflow.ui.util

import app.cashflow.core.Direction
import app.cashflow.core.Kind
import app.cashflow.core.PaymentStatus

/**
 * Plain words for money meanings. Every glyph stays paired with one of these
 * words in the interface; no icon ever carries a financial meaning alone.
 */
object MoneyWords {

    fun direction(direction: Direction): String = when (direction) {
        Direction.IN -> "Money in"
        Direction.OUT -> "Money out"
    }

    /** The verb used in the confirmation sentence: "Recorded 8,500 sent to Ada". */
    fun directionVerb(direction: Direction): String = when (direction) {
        Direction.IN -> "received"
        Direction.OUT -> "sent"
    }

    fun kind(kind: Kind): String = when (kind) {
        Kind.INCOME -> "Income"
        Kind.EXPENSE -> "Expense"
        Kind.TRANSFER -> "Transfer"
        Kind.REFUND -> "Refund"
        Kind.FEE -> "Fee"
        Kind.UNCLASSIFIED -> "Not yet classified"
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

    /** Human long name shown under codes in the currency picker. */
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

    fun origin(source: String): String =
        if (source.equals("Manual", ignoreCase = true)) "Entered by hand" else "Captured from $source"
}
