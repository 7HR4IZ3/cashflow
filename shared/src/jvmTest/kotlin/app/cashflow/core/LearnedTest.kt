package app.cashflow.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cashflow.db.LedgerDatabase
import kotlin.test.*

class LearnedTest {
    private class Hooks : PlatformHooks {
        var next = 0
        override fun now() = "2026-10-25T12:00:00Z"
        override fun id() = "local-${next++}"
        override fun captureState(selected: Set<String>) = CaptureState("Test")
        override fun openCaptureSettings() { }
        override fun requestNotificationPermission() { }
        override fun exportCsv(contents: String): String? = null
        override fun recorded(entry: Entry) { }
    }
    private fun fixture(): Pair<LedgerStore, JdbcSqliteDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LedgerDatabase.Schema.create(driver)
        return LedgerStore(driver, Hooks()) to driver
    }
    private fun alert(amount: String, who: String) =
        "NGN $amount received from $who via Acme Bank transfer. Thank you for banking with us today."

    private fun confirm(store: LedgerStore, kind: Kind = Kind.INCOME) {
        val item = store.state.value.review.single()
        assertNull(store.save(item.draft.copy(directionConfirmed = true, direction = Direction.IN, kind = kind), reviewId = item.id))
    }

    @Test fun twoConfirmationsPromoteARuleThenSimilarAlertsRecordThemselves() {
        val (store, driver) = fixture()
        store.ingest(alert("210.00", "Ada"), "Bank", "e1")
        assertEquals(AlertParser.REASON_CONFIRM, store.state.value.review.single().reason)
        confirm(store)
        assertEquals(1, store.state.value.learnedRules.single().confirmations)

        // One correction is only a hint: prefilled, still waiting for the user.
        store.ingest(alert("5,000.00", "Chidi"), "Bank", "e2")
        val hinted = store.state.value.review.single()
        assertEquals(Learned.REASON_HINT, hinted.reason)
        assertEquals(Direction.IN, hinted.draft.direction)
        assertFalse(hinted.draft.directionConfirmed)
        confirm(store)
        assertEquals(2, store.state.value.learnedRules.single().confirmations)

        // Now it records on its own, with the learned kind.
        store.ingest(alert("1,250.00", "Bola"), "Bank", "e3")
        assertTrue(store.state.value.review.isEmpty())
        val entry = store.state.value.entries.maxByOrNull { it.capturedAt + it.id }!!
        assertEquals(3, store.state.value.entries.size)
        assertEquals(Kind.INCOME, store.state.value.entries.first { it.amountMinor == 125000L }.kind)
        assertEquals(1, store.state.value.learnedRules.single().uses)
        assertNotNull(entry)
        driver.close()
    }

    @Test fun rulesStayWithTheirSourceAndNeverOverrideBadAmountsOrOtps() {
        val (store, driver) = fixture()
        store.ingest(alert("210.00", "Ada"), "Bank", "e1"); confirm(store)
        store.ingest(alert("300.00", "Ada"), "Bank", "e2"); confirm(store)
        store.ingest(alert("400.00", "Ada"), "Other", "e3")
        assertEquals(1, store.state.value.review.size, "another app's alerts are not covered")
        assertEquals(AlertParser.REASON_CONFIRM, store.state.value.review.single().reason)
        store.ingest("Your OTP is 123456. Thank you for banking with Acme Bank transfer today.", "Bank", "e4")
        assertEquals(1, store.state.value.review.size)
        // Two amounts always go to Review, even for a learned template.
        store.ingest("NGN 100.00 received from Ada via Acme Bank transfer. Balance NGN 900.00. Thank you for banking with us today.", "Bank", "e5")
        assertEquals(2, store.state.value.review.size)
        driver.close()
    }

    @Test fun confirmingAnAiSuggestionPromotesImmediatelyAndRulesCanBeForgotten() {
        val (store, driver) = fixture()
        store.setAiEnabled(true)
        store.ingest(alert("210.00", "Ada"), "Bank", "e1")
        val item = store.state.value.review.single()
        store.completeAi(item.id, item.text, AiSuggestion(relevant = true, direction = Direction.IN, kind = Kind.INCOME, model = "test"))
        confirm(store)
        val rule = store.state.value.learnedRules.single()
        assertEquals("ai", rule.origin)
        assertEquals(Learned.AUTO_AT, rule.confirmations)
        store.ingest(alert("999.00", "Chidi"), "Bank", "e2")
        assertTrue(store.state.value.review.isEmpty())
        store.deleteLearnedRule(rule.id)
        assertTrue(store.state.value.learnedRules.isEmpty())
        store.ingest(alert("50.00", "Bola"), "Bank", "e3")
        assertEquals(1, store.state.value.review.size)
        driver.close()
    }

    @Test fun contradictingARuleResetsItsConfidence() {
        val (store, driver) = fixture()
        store.ingest(alert("210.00", "Ada"), "Bank", "e1"); confirm(store)
        store.ingest(alert("300.00", "Ada"), "Bank", "e2"); confirm(store)
        assertEquals(2, store.state.value.learnedRules.single().confirmations)
        // A rule can only auto-record while it is trusted; the next review answer replaces it if it differs.
        val rules = Learned.learn(store.state.value.learnedRules, "Bank", alert("1.00", "Zed"), Direction.OUT, Kind.EXPENSE, false, "x", "now")
        assertEquals(Direction.OUT, rules.single().direction)
        assertEquals(1, rules.single().confirmations)
        driver.close()
    }
}

class CounterpartyTest {
    private fun who(text: String) = AlertParser.parse(text, "NGN", "2026-11-09").draft?.counterparty
    @Test fun nameStopsBeforeTheBanksBoilerplate() {
        assertEquals("DIVINE JOHN ALAGBADA", who("You have just received NGN6,000.00 from DIVINE JOHN ALAGBADA. View in your Personal Banking app"))
        assertEquals("Ada", who("NGN 50.00 debited to Ada. Ref: SAME123"))
        assertEquals("J. Doe", who("NGN 50.00 credited from J. Doe"))
        assertEquals("ABDULAZEEZ OMOKHAFE ALHASSAN", who("NGN 100.00 debit alert to ABDULAZEEZ OMOKHAFE ALHASSAN Balance available"))
    }
}
