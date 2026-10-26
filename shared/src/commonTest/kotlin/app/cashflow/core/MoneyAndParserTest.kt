package app.cashflow.core

import kotlin.test.*

class MoneyAndParserTest {
    @Test fun exactMoneyAndCurrencyExponents() {
        assertEquals(850050L, Money.parse("8500.50", "NGN"))
        assertEquals(120L, Money.parse("120", "JPY"))
        assertNull(Money.parse("120.1", "JPY"))
        assertEquals(1234L, Money.parse("1.234", "KWD"))
        assertNull(Money.parse("1.234", "USD"))
        assertNull(Money.parse("-50", "USD"))
        assertNull(Money.parse("9223372036854775808", "JPY"))
    }
    @Test fun totalsKeepCurrenciesAndPendingSeparate() {
        fun e(id: String, amount: Long, currency: String, direction: Direction, kind: Kind = Kind.UNCLASSIFIED, status: PaymentStatus = PaymentStatus.COMPLETED) =
            Entry(id, amount, currency, direction, kind, status, "", date = "2026-10-25")
        val entries = listOf(
            e("income", 10000, "NGN", Direction.IN), e("expense", 2500, "NGN", Direction.OUT),
            e("usd", 9900, "USD", Direction.IN), e("transfer", 5000, "NGN", Direction.OUT, Kind.TRANSFER),
            e("pending", 1000, "NGN", Direction.OUT, status = PaymentStatus.PENDING),
        )
        assertEquals(Totals(10000, 2500), totals(entries, "NGN"))
        assertEquals(7500, totals(entries, "NGN").net)
    }
    @Test fun conservativeNotificationInterpretation() {
        val debit = AlertParser.parse("Debit alert: NGN 8,500.00 debited to Ada. Ref: ABCDE123", "USD", "2026-10-25")
        assertNull(debit.reason)
        assertEquals("NGN", debit.draft!!.currency)
        assertEquals(Direction.OUT, debit.draft!!.direction)
        assertEquals("ABCDE123", debit.reference)
        assertEquals("Ada", debit.draft!!.counterparty)
        assertNotNull(AlertParser.parse("You sent $50 to Ada", "NGN", "2026-10-25").reason)
        assertNotNull(AlertParser.parse("NGN 50 debited. Balance NGN 200", "NGN", "2026-10-25").reason)
        assertTrue(AlertParser.parse("OTP 123456 for NGN 50 payment", "NGN", "2026-10-25").irrelevant)
        assertNotNull(AlertParser.parse("NGN 50 debited and reversed", "NGN", "2026-10-25").reason)
        assertNotNull(AlertParser.parse("NGN 1,2 debited", "NGN", "2026-10-25").reason)
    }
    @Test fun realCalendarDates() {
        assertTrue(validDate("2024-02-29"))
        assertFalse(validDate("2026-02-29"))
        assertFalse(validDate("2026-04-31"))
    }
}
