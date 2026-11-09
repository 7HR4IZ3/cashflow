package app.cashflow.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cashflow.db.LedgerDatabase
import kotlin.test.*

class SubscriptionTest {
    private class Hooks : PlatformHooks {
        var next = 0
        var clock = 1L
        var analyses = 0
        override fun now() = "2026-10-30T00:00:00Z"
        override fun nowMillis() = clock
        override fun id() = "event-${next++}"
        override fun captureState(selected: Set<String>) = CaptureState("Test", sources = selected.map { SourceApp(it, it, true) })
        override fun openCaptureSettings() { }
        override fun requestNotificationPermission() { }
        override fun exportCsv(contents: String): String? = null
        override fun recorded(entry: Entry) { }
        override fun analyseAlert(item: ReviewItem, automatic: Boolean) { analyses++ }
    }
    private fun fixture(): Triple<LedgerStore, JdbcSqliteDriver, Hooks> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LedgerDatabase.Schema.create(driver)
        val hooks = Hooks()
        return Triple(LedgerStore(driver, hooks), driver, hooks)
    }
    private fun paid(tier: PlanTier = PlanTier.LITE, used: Int = 0) = SubscriptionState(tier, 10000, used, "2026-10", available = true)

    @Test fun tierLimitsAndDowngradePreserveSelectionsAndRecords() {
        val (store, driver, hooks) = fixture()
        repeat(3) { store.selectSource("app$it", true) }
        assertEquals(2, store.state.value.capture.sources.size)
        assertNotNull(store.state.value.error)
        store.updateSubscription(paid())
        repeat(6) { store.selectSource("app$it", true) }
        assertEquals(5, store.state.value.capture.sources.size)
        store.updateSubscription(paid(PlanTier.PREMIUM))
        repeat(40) { store.selectSource("app$it", true) }
        assertEquals(40, store.state.value.capture.sources.size)
        assertNull(store.save(EntryDraft(amount = "10")))
        hooks.clock = 10001
        store.refreshCapture()
        assertEquals(PlanTier.FREE, store.state.value.subscription.tier)
        assertTrue(store.state.value.captureOverLimit)
        assertFalse(store.isSelected("app0"))
        assertNull(store.ingest("USD 10 credited", "app0", "paused"))
        assertEquals(1, store.state.value.entries.size)
        repeat(38) { store.selectSource("app${it + 2}", false) }
        assertFalse(store.state.value.captureOverLimit)
        assertTrue(store.isSelected("app0"))
        assertNull(store.exportCsv())
        assertEquals(1, store.state.value.entries.size)
        driver.close()
    }
    @Test fun aiNeedsConsentActivePlanAndAllowanceAndNeverAutoRecords() {
        val (store, driver, hooks) = fixture()
        store.captureText("USD 50 account activity")
        val item = store.state.value.review.single()
        assertNotNull(store.requestAi(item.id))
        store.setAiEnabled(true)
        assertNotNull(store.requestAi(item.id))
        store.updateSubscription(paid(used = 300))
        assertNotNull(store.requestAi(item.id))
        store.updateSubscription(paid())
        assertNull(store.requestAi(item.id))
        assertNotNull(store.requestAi(item.id))
        assertEquals(1, hooks.analyses)
        store.completeAi(item.id, item.text, AiSuggestion(true, Direction.IN, Kind.INCOME, "test"))
        val updated = store.state.value.review.single()
        assertEquals(item.draft, updated.draft)
        assertFalse(updated.draft.directionConfirmed)
        assertTrue(store.state.value.entries.isEmpty())
        assertNotNull(store.save(updated.draft, reviewId = item.id))
        driver.close()
    }
    @Test fun consentWithdrawalOrResolvedReviewDiscardsLateResponses() {
        val (store, driver) = fixture()
        store.captureText("USD 50 sent to Ada")
        val item = store.state.value.review.single()
        store.updateSubscription(paid())
        store.setAiEnabled(true)
        assertNull(store.requestAi(item.id))
        store.setAiEnabled(false)
        store.completeAi(item.id, item.text, AiSuggestion(true, Direction.OUT))
        assertNull(store.state.value.review.single().aiSuggestion)
        store.setAiEnabled(true)
        assertNull(store.requestAi(item.id))
        store.save(item.draft, reviewId = item.id)
        store.completeAi(item.id, item.text, AiSuggestion(true, Direction.IN))
        assertTrue(store.state.value.review.isEmpty())
        assertEquals(Direction.OUT, store.state.value.entries.single().direction)
        assertTrue(store.state.value.aiBusyIds.isEmpty())
        driver.close()
    }
    @Test fun freeManualAndLocalCaptureHaveNoTransactionQuota() {
        val (store, driver) = fixture()
        repeat(350) { assertNull(store.ingest("USD 1 debited", "Bank", "alert-$it")) }
        assertEquals(350, store.state.value.entries.size)
        driver.close()
    }
    @Test fun automaticAiOnlyQueuesNewUnclearAlertsAfterBothOptIns() {
        val (store, driver, hooks) = fixture()
        store.updateSubscription(paid())
        store.captureText("USD 20 account activity")
        assertEquals(0, hooks.analyses)
        store.setAutomaticAi(true)
        assertFalse(store.state.value.aiAutomatic)
        store.setAiEnabled(true)
        store.setAutomaticAi(true)
        assertTrue(store.state.value.aiAutomatic)
        assertEquals(0, hooks.analyses) // No automatic backlog upload.
        store.ingest("USD 10 account activity", "Bank", "unclear")
        store.ingest("USD 10 account activity", "Bank", "unclear")
        assertEquals(1, hooks.analyses)
        store.ingest("USD 10 credited", "Bank", "clear")
        store.ingest("Your OTP verification code is 123456", "Bank", "otp")
        assertEquals(1, hooks.analyses)
        store.setAiEnabled(false)
        assertFalse(store.state.value.aiAutomatic)
        store.ingest("USD 11 account activity", "Bank", "after-opt-out")
        assertEquals(1, hooks.analyses)
        driver.close()
    }

}
