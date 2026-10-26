package app.cashflow.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cashflow.db.LedgerDatabase
import kotlin.test.*

class LedgerStoreTest {
    private class Hooks : PlatformHooks {
        var next = 0
        var notifications = 0
        var exported = ""
        override fun now() = "2026-10-25T12:00:00Z"
        override fun id() = "local-${next++}"
        override fun captureState(selected: Set<String>) = CaptureState("Test")
        override fun openCaptureSettings() { }
        override fun requestNotificationPermission() { }
        override fun exportCsv(contents: String): String? { exported = contents; return null }
        override fun recorded(entry: Entry) { notifications++ }
    }
    private fun fixture(): Triple<LedgerStore, JdbcSqliteDriver, Hooks> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LedgerDatabase.Schema.create(driver)
        val hooks = Hooks()
        return Triple(LedgerStore(driver, hooks), driver, hooks)
    }
    @Test fun persistsEditsDeletionAndSuppressionAcrossRecreation() {
        val (store, driver, hooks) = fixture()
        val alert = "NGN 50.00 debited to Ada. Ref: SAME123"
        assertNull(store.ingest(alert, "Bank", "event-a"))
        assertNull(store.ingest(alert, "Bank", "event-a"))
        assertEquals(1, store.state.value.entries.size)
        val entry = store.state.value.entries.single()
        assertNull(store.save(EntryDraft(amount = "45", counterparty = "Ada"), entry.id))
        assertNull(store.ingest(alert, "Bank", "event-b"))
        assertEquals(4500L, store.state.value.entries.single().amountMinor)
        assertEquals(1, store.state.value.review.size) // conflicting amount is not overwritten
        store.delete(entry.id)
        val reloaded = LedgerStore(driver, hooks)
        reloaded.ingest(alert, "Bank", "event-a")
        assertEquals(1, reloaded.state.value.entries.size)
        assertTrue(reloaded.state.value.entries.single().deleted)
        reloaded.restore(entry.id)
        assertFalse(reloaded.state.value.entries.single().deleted)
        driver.close()
    }
    @Test fun twoRealSameAmountPaymentsRemainSeparateWithoutSharedReference() {
        val (store, driver) = fixture()
        store.ingest("NGN 50 debited to Ada", "Bank", "notification-1")
        store.ingest("NGN 50 debited to Ada", "Bank", "notification-2")
        assertEquals(2, store.state.value.entries.size)
        driver.close()
    }
    @Test fun pendingCompletesOnceAndNeverOverwritesUserEdit() {
        val (store, driver) = fixture()
        store.ingest("NGN 50 debited pending. Ref: PAYMENT123", "Bank", "pending")
        assertEquals(PaymentStatus.PENDING, store.state.value.entries.single().status)
        store.ingest("NGN 50 debited completed. Ref: PAYMENT123", "Bank", "completed")
        assertEquals(1, store.state.value.entries.size)
        assertEquals(5000L, totals(store.state.value.entries, "NGN").outgoing)
        driver.close()
    }
    @Test fun reviewMustBeResolvedAndCsvEscapesFormulaText() {
        val (store, driver, hooks) = fixture()
        store.captureText("USD 50 sent to Ada")
        assertEquals(0, store.state.value.entries.size)
        val review = store.state.value.review.single()
        assertNull(store.save(review.draft.copy(counterparty = "=2+2"), reviewId = review.id))
        assertTrue(store.state.value.review.isEmpty())
        assertNull(store.exportCsv())
        assertTrue(hooks.exported.contains("'=2+2"))
        driver.close()
    }
    @Test fun invalidClassificationCannotEnterLedger() {
        val (store, driver) = fixture()
        assertNotNull(store.save(EntryDraft(amount = "50", direction = Direction.OUT, kind = Kind.INCOME)))
        assertTrue(store.state.value.entries.isEmpty())
        driver.close()
    }
    @Test fun aggregateOverflowCannotPersistAnUnreadableLedger() {
        val (store, driver) = fixture()
        assertNull(store.save(EntryDraft(amount = "92233720368547758.07")))
        assertNotNull(store.save(EntryDraft(amount = "0.01")))
        assertEquals(1, store.state.value.entries.size)
        assertEquals(Long.MAX_VALUE, totals(store.state.value.entries, "NGN").outgoing)
        driver.close()
    }
}
