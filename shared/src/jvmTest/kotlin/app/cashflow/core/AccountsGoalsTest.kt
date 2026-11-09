package app.cashflow.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cashflow.db.LedgerDatabase
import kotlin.test.*

class AccountsGoalsTest {
    private class Hooks : PlatformHooks {
        var next = 0
        var time = "2026-11-04T09:00:00+01:00"
        override fun now() = time
        override fun id() = "id-${next++}"
        override fun captureState(selected: Set<String>) = CaptureState("Test")
        override fun openCaptureSettings() { }
        override fun requestNotificationPermission() { }
        override fun exportCsv(contents: String): String? = null
        override fun recorded(entry: Entry) { }
    }

    private class Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { LedgerDatabase.Schema.create(it) }
        val hooks = Hooks()
        val store = LedgerStore(driver, hooks)
        val nudges = mutableListOf<Nudge>()
        init { store.nudgeSink = NudgeSink { nudges += it } }
        fun account(name: String, role: AccountRole = AccountRole.SPENDING, vararg hints: String) =
            assertNull(store.saveAccount(Account("", name, role, hints.toList())))
        fun at(time: String) { hooks.time = time }
        val entries get() = store.state.value.entries
        fun spend(amount: String, date: String, who: String = "Shop") =
            assertNull(store.save(EntryDraft(amount = amount, counterparty = who, date = date, kind = Kind.EXPENSE)))
        fun totalsNow() = totals(entries, "NGN")
        fun saved() = savings(entries, store.state.value.accounts, "NGN")
    }

    @Test fun payingYourOwnSavingsAccountIsASavingNotSpending() {
        val f = Fixture()
        f.account("GTBank", hints = arrayOf("gtb"))
        f.account("Piggy", AccountRole.SAVINGS)
        assertNull(f.store.ingest("NGN 50,000.00 debited to Piggy. Ref: ABC123", "gtb-app", "e1"))
        val e = f.entries.single()
        assertEquals(Kind.TRANSFER, e.kind)
        assertEquals("GTBank", e.account)
        assertEquals("Piggy", e.counterAccount)
        assertEquals(0L, f.totalsNow().outgoing)
        assertEquals(5_000_000L, f.saved().saved)
        assertEquals(0L, f.saved().withdrawn)
    }

    @Test fun takingMoneyBackOutOfSavingsIsWithdrawnNotIncome() {
        val f = Fixture()
        f.account("GTBank")
        f.account("Piggy", AccountRole.SAVINGS)
        assertNull(f.store.save(EntryDraft(amount = "200", direction = Direction.IN, kind = Kind.TRANSFER, account = "GTBank", date = "2026-11-03")))
        val id = f.entries.single().id
        assertNull(f.store.markTransfer(id, "Piggy"))
        // Money came into GTBank from Piggy: savings is the source.
        assertEquals(Savings(saved = 0, withdrawn = 20_000L), f.saved())
        assertEquals(0L, f.totalsNow().incoming)
    }

    @Test fun matchingDebitAndCreditAsksOnceThenRemembersTheAnswer() {
        val f = Fixture()
        f.account("Alpha", hints = arrayOf("alpha"))
        f.account("Bravo", hints = arrayOf("bravo"))
        f.at("2026-11-04T09:00:00+01:00")
        f.store.ingest("NGN 100 debited to Ada", "alpha-bank", "d1")
        f.at("2026-11-04T09:01:00+01:00")
        f.store.ingest("NGN 100 credited from Ada", "bravo-bank", "c1")
        assertEquals(1, f.store.state.value.transferSuggestions.size)
        assertTrue(f.entries.all { it.kind != Kind.TRANSFER })
        assertEquals(100_00L, f.totalsNow().outgoing)

        assertNull(f.store.answerTransfer(f.store.state.value.transferSuggestions.single().id, true))
        assertTrue(f.store.state.value.transferSuggestions.isEmpty())
        assertEquals(1, f.entries.mapNotNull { it.transferGroupId }.toSet().size)
        assertTrue(f.entries.all { it.kind == Kind.TRANSFER })
        assertEquals(0L, f.totalsNow().outgoing + f.totalsNow().incoming)

        // Same account pair again: linked without asking.
        f.at("2026-11-05T09:00:00+01:00")
        f.store.ingest("NGN 70 debited to Ada", "alpha-bank", "d2")
        f.at("2026-11-05T09:02:00+01:00")
        f.store.ingest("NGN 70 credited from Ada", "bravo-bank", "c2")
        assertTrue(f.store.state.value.transferSuggestions.isEmpty())
        assertEquals(2, f.entries.mapNotNull { it.transferGroupId }.toSet().size)
    }

    @Test fun sayingNoKeepsTheEntriesAndStopsAsking() {
        val f = Fixture()
        f.account("Alpha", hints = arrayOf("alpha"))
        f.account("Bravo", hints = arrayOf("bravo"))
        f.store.ingest("NGN 100 debited to Ada", "alpha-bank", "d1")
        f.store.ingest("NGN 100 credited from Bola", "bravo-bank", "c1")
        assertNull(f.store.answerTransfer(f.store.state.value.transferSuggestions.single().id, false))
        assertTrue(f.entries.all { it.transferGroupId == null && it.kind == Kind.UNCLASSIFIED })
        f.store.ingest("NGN 40 debited to Ada", "alpha-bank", "d2")
        f.store.ingest("NGN 40 credited from Bola", "bravo-bank", "c2")
        assertTrue(f.store.state.value.transferSuggestions.isEmpty())
    }

    @Test fun lookalikesAreNeverPaired() {
        val f = Fixture()
        f.account("Alpha", hints = arrayOf("alpha"))
        f.account("Bravo", hints = arrayOf("bravo"))
        // Two real payments from one account, then a credit in that same account.
        f.store.ingest("NGN 100 debited to Ada", "alpha-bank", "a")
        f.store.ingest("NGN 100 debited to Ada", "alpha-bank", "b")
        f.store.ingest("NGN 100 credited from Ada", "alpha-bank", "d")
        // A credit in another account, but two hours later.
        f.at("2026-11-04T11:00:00+01:00")
        f.store.ingest("NGN 100 credited from Ada", "bravo-bank", "e")
        assertTrue(f.store.state.value.transferSuggestions.isEmpty())
        assertTrue(f.entries.all { it.transferGroupId == null })
    }

    @Test fun twoPossiblePartnersAreNotGuessed() {
        val f = Fixture()
        f.account("Alpha", hints = arrayOf("alpha"))
        f.account("Bravo", hints = arrayOf("bravo"))
        f.account("Charlie", hints = arrayOf("charlie"))
        f.store.ingest("NGN 100 debited to Ada", "alpha-bank", "a")
        f.store.ingest("NGN 100 debited to Bola", "bravo-bank", "b")
        f.store.ingest("NGN 100 credited from Cy", "charlie-bank", "c")
        assertTrue(f.store.state.value.transferSuggestions.isEmpty())
    }

    @Test fun namedSelfLinksBothLegsWithoutAsking() {
        val f = Fixture()
        f.account("Alpha", hints = arrayOf("alpha"))
        f.account("Bravo", hints = arrayOf("bravo"))
        f.store.setMyNames(listOf("Ada Okafor"))
        f.store.ingest("NGN 500 debited to Ada Okafor", "alpha-bank", "a")
        f.at("2026-11-04T09:03:00+01:00")
        f.store.ingest("NGN 500 credited from Ada Okafor", "bravo-bank", "b")
        assertTrue(f.store.state.value.transferSuggestions.isEmpty())
        assertEquals(1, f.entries.mapNotNull { it.transferGroupId }.toSet().size)
        assertEquals(Totals(0, 0), f.totalsNow())
    }

    @Test fun deletingOneLegLeavesNoDanglingLinkAndUnlinkRestoresMeaning() {
        val f = Fixture()
        f.account("Alpha", hints = arrayOf("alpha"))
        f.account("Bravo", hints = arrayOf("bravo"))
        f.store.setMyNames(listOf("Ada Okafor"))
        f.store.ingest("NGN 500 debited to Ada Okafor", "alpha-bank", "a")
        f.store.ingest("NGN 500 credited from Ada Okafor", "bravo-bank", "b")
        val out = f.entries.first { it.direction == Direction.OUT }
        val inn = f.entries.first { it.direction == Direction.IN }
        assertNull(f.store.delete(out.id))
        assertNull(f.entries.first { it.id == inn.id }.transferGroupId)
        assertNull(f.entries.first { it.id == out.id }.transferGroupId)

        assertNull(f.store.restore(out.id))
        assertNull(f.store.linkTransfer(out.id, inn.id))
        assertNull(f.store.unlinkTransfer(inn.id))
        assertEquals(Kind.EXPENSE, f.entries.first { it.id == out.id }.kind)
        assertEquals(Kind.INCOME, f.entries.first { it.id == inn.id }.kind)
        assertTrue(f.entries.all { it.transferGroupId == null })
        assertNotNull(f.store.linkTransfer(out.id, out.id))
    }

    @Test fun editingALegAwayFromTransferDetachesItsPartner() {
        val f = Fixture()
        f.account("Alpha", hints = arrayOf("alpha"))
        f.account("Bravo", hints = arrayOf("bravo"))
        f.store.setMyNames(listOf("Ada Okafor"))
        f.store.ingest("NGN 500 debited to Ada Okafor", "alpha-bank", "a")
        f.store.ingest("NGN 500 credited from Ada Okafor", "bravo-bank", "b")
        val out = f.entries.first { it.direction == Direction.OUT }
        assertNull(f.store.save(EntryDraft(amount = "450", direction = Direction.OUT, kind = Kind.TRANSFER, counterparty = "Ada Okafor", account = "Alpha"), out.id))
        assertTrue(f.entries.all { it.transferGroupId == null })
    }

    @Test fun renamingAnAccountUpdatesItsEntries() {
        val f = Fixture()
        f.account("Kuda")
        assertNull(f.store.save(EntryDraft(amount = "10", account = "Kuda", date = "2026-11-03")))
        val account = f.store.state.value.accounts.single()
        assertNull(f.store.saveAccount(account.copy(name = "Kuda MFB")))
        assertEquals("Kuda MFB", f.entries.single().account)
        f.account("Opay")
        assertNotNull(f.store.saveAccount(Account("", "kuda mfb")))
    }

    @Test fun unknownAccountNamesBecomeAccounts() {
        val f = Fixture()
        assertNull(f.store.save(EntryDraft(amount = "10", account = "Wema", date = "2026-11-03")))
        assertEquals(listOf("Wema"), f.store.state.value.accounts.map { it.name })
    }

    @Test fun spendingLimitTracksStateAndIgnoresTransfers() {
        val f = Fixture()
        f.account("Piggy", AccountRole.SAVINGS)
        assertNull(f.store.saveGoal(Goal("", "Month", GoalType.SPENDING_LIMIT, 100_000, "NGN")))
        f.spend("500", "2026-11-02")
        assertEquals(GoalState.ON_TRACK, f.store.state.value.goals.single().state)
        assertNull(f.store.save(EntryDraft(amount = "10000", kind = Kind.TRANSFER, date = "2026-11-02")))
        assertEquals(50_000L, f.store.state.value.goals.single().amountMinor)
        f.spend("350", "2026-11-03")
        assertEquals(GoalState.CLOSE, f.store.state.value.goals.single().state)
        f.spend("200", "2026-11-03")
        assertEquals(GoalState.OVER, f.store.state.value.goals.single().state)
        f.spend("900", "2026-10-31") // last month, not counted
        assertEquals(105_000L, f.store.state.value.goals.single().amountMinor)
        val id = f.entries.first { it.date == "2026-11-03" && it.amountMinor == 20_000L }.id
        assertNull(f.store.delete(id))
        assertEquals(GoalState.CLOSE, f.store.state.value.goals.single().state)
    }

    @Test fun scopedLimitOnlyCountsMatchingPayments() {
        val f = Fixture()
        assertNull(f.store.saveGoal(Goal("", "Coffee", GoalType.SPENDING_LIMIT, 10_000, "NGN", counterparty = "cafe")))
        f.spend("20", "2026-11-02", "Blue Cafe")
        f.spend("500", "2026-11-02", "Market")
        assertEquals(2_000L, f.store.state.value.goals.single().amountMinor)
    }

    @Test fun savingTargetCountsNetMoneyMovedIntoSavings() {
        val f = Fixture()
        f.account("GTBank", hints = arrayOf("gtb"))
        f.account("Piggy", AccountRole.SAVINGS)
        assertNull(f.store.saveGoal(Goal("", "Rainy day", GoalType.SAVING_TARGET, 10_000, "NGN")))
        f.store.ingest("NGN 60 debited to Piggy", "gtb-app", "a")
        assertEquals(6_000L, f.store.state.value.goals.single().amountMinor)
        assertEquals(GoalState.ON_TRACK, f.store.state.value.goals.single().state) // early in the month: 4/30 days elapsed
        f.store.ingest("NGN 40 debited to Piggy", "gtb-app", "b")
        assertEquals(10_000L, f.store.state.value.goals.single().amountMinor)
        assertEquals(GoalState.REACHED, f.store.state.value.goals.single().state)
    }

    @Test fun goalValidation() {
        val f = Fixture()
        assertNotNull(f.store.saveGoal(Goal("", "", GoalType.SPENDING_LIMIT, 100, "NGN")))
        assertNotNull(f.store.saveGoal(Goal("", "X", GoalType.SPENDING_LIMIT, 0, "NGN")))
        assertNotNull(f.store.saveGoal(Goal("", "X", GoalType.SPENDING_LIMIT, 100, "NGN", GoalPeriod.CUSTOM, "2026-11-10", "2026-11-01")))
        assertNull(f.store.saveGoal(Goal("", "X", GoalType.SPENDING_LIMIT, 100, "NGN", GoalPeriod.CUSTOM, "2026-11-01", "2026-11-10")))
        val id = f.store.state.value.goals.single().goal.id
        assertNull(f.store.archiveGoal(id))
        assertTrue(f.store.state.value.goals.isEmpty())
    }

    @Test fun goalNudgeIsSentOncePerStateChange() {
        val f = Fixture()
        assertNull(f.store.saveGoal(Goal("", "Month", GoalType.SPENDING_LIMIT, 100_000, "NGN")))
        f.spend("850", "2026-11-02")
        assertEquals(1, f.nudges.size)
        f.spend("10", "2026-11-02")
        assertEquals(1, f.nudges.size) // still CLOSE
        f.spend("300", "2026-11-02")
        assertEquals(2, f.nudges.size) // now OVER
        assertEquals(listOf(NudgeCategory.GOAL, NudgeCategory.GOAL), f.nudges.map { it.category })
    }

    @Test fun nudgesWaitForQuietHoursAndRespectSwitches() {
        val f = Fixture()
        assertNull(f.store.saveGoal(Goal("", "Month", GoalType.SPENDING_LIMIT, 100_000, "NGN")))
        f.at("2026-11-04T23:30:00+01:00")
        f.spend("900", "2026-11-04")
        assertTrue(f.nudges.isEmpty())
        f.at("2026-11-05T08:00:00+01:00")
        f.store.runDailyNudges()
        assertEquals(1, f.nudges.size)

        f.store.setNotify(NudgeCategory.GOAL, false)
        f.spend("500", "2026-11-05")
        assertEquals(1, f.nudges.size)
    }

    @Test fun capsNudgesAtThreeADay() {
        val f = Fixture()
        repeat(5) { n -> assertNull(f.store.saveGoal(Goal("", "G$n", GoalType.SPENDING_LIMIT, 1_000, "NGN"))) }
        f.spend("500", "2026-11-04")
        assertEquals(3, f.nudges.size)
        f.at("2026-11-05T09:00:00+01:00")
        f.store.runDailyNudges()
        assertEquals(5, f.nudges.size) // the remaining two go out the next day
    }

    @Test fun weeklySummaryAndHabitNudgesAreOptInOrDue() {
        val f = Fixture()
        f.account("GTBank")
        f.spend("1000", "2026-10-28")
        f.at("2026-11-02T09:00:00+01:00") // Monday
        f.store.runDailyNudges()
        assertEquals(listOf(NudgeCategory.SUMMARY, NudgeCategory.SUMMARY), f.nudges.map { it.category }) // week and month
        f.store.runDailyNudges()
        assertEquals(2, f.nudges.size) // not repeated
    }

    @Test fun spendingPaceNoticesAQuietWeek() {
        val f = Fixture()
        listOf("2026-10-06", "2026-10-13", "2026-10-20", "2026-10-27").forEach { f.spend("1000", it) }
        f.spend("100", "2026-11-03")
        val signal = f.store.state.value.insights.signals.single { it.kind == SignalKind.SPENDING_PACE }
        assertEquals(true, signal.positive)
    }

    @Test fun dateMathIsCorrect() {
        assertEquals(6, Days.weekday(Days.number("2026-11-01")!!))
        assertEquals("2026-11-02", Days.date(Days.weekStart(Days.number("2026-11-04")!!)))
        assertEquals("2026-12-31", Days.date(Days.monthEnd(Days.number("2026-12-05")!!)))
        assertEquals("2028-02-29", Days.date(Days.monthEnd(Days.number("2028-02-01")!!)))
        assertEquals(Days.epochSeconds("2026-11-04T10:00:00Z"), Days.epochSeconds("2026-11-04T11:00:00+01:00"))
        assertEquals(9, Days.hour("2026-11-04T09:00:00-05:00"))
        assertNull(Days.number("2026-02-30"))
    }
}
