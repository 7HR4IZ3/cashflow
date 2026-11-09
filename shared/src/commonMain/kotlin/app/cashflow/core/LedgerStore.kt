package app.cashflow.core

import app.cashflow.db.LedgerDatabase
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface PlatformHooks {
    fun now(): String
    fun id(): String
    fun captureState(selected: Set<String>): CaptureState
    fun openCaptureSettings()
    fun requestNotificationPermission()
    fun exportCsv(contents: String): String?
    fun recorded(entry: Entry)
    fun nowMillis(): Long = 0
    fun refreshSubscription() { }
    fun purchase(offerId: String) { }
    fun manageSubscription() { }
    fun analyseAlert(item: ReviewItem, automatic: Boolean) { }
    /** iOS: hand the bundled Shortcuts file to the share sheet so it can be added in one tap. */
    fun installShortcut() { }
    /** iOS: float a small card with this setup step (1-3) over other apps. 0 hides it. */
    fun showSetupGuide(step: Int) { }
    /** iOS: the setup guide is on screen (or not). The floating card may only exist while it is. */
    fun setupGuideVisible(visible: Boolean) { }
}

/** All writes go through SQLite transactions. UI and native ingestion share this instance. */
class LedgerStore(driver: SqlDriver, private val platform: PlatformHooks) : CashflowActions {
    private val database = LedgerDatabase(driver)
    private val queries = database.ledgerQueries
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutableState = MutableStateFlow(CashflowState())
    val state: StateFlow<CashflowState> = mutableState
    private var preferences = emptyMap<String, String>()
    private var focusedId: String? = null
    private var subscription = SubscriptionState()
    private val aiBusy = mutableSetOf<String>()

    /** Registered by the platform to show goal, habit, summary and reminder notifications. */
    var nudgeSink: NudgeSink? = null

    init { reload(); migrateAccounts() }

    private fun reload() {
        preferences = queries.allPreferences().executeAsList().associate { it.key to it.value_ }
        val selected = preferences.filter { it.key.startsWith("source:") && it.value == "true" }.keys.map { it.removePrefix("source:") }.toSet()
        val entries = queries.allEntries().executeAsList().map { json.decodeFromString<Entry>(it.payload) }.sortedByDescending { it.date }
        val accounts = accounts()
        val today = platform.now().take(10)
        val currency = preferences["currency"] ?: "NGN"
        val live = entries.associateBy { it.id }.filterValues { !it.deleted && it.transferGroupId == null }
        mutableState.value = CashflowState(
            subscription = subscription.copy(tier = subscription.effectiveTier(platform.nowMillis())),
            aiEnabled = preferences["aiConsent:v1"] == "true",
            aiAutomatic = preferences["aiConsent:v1"] == "true" && preferences["aiAutomatic:v1"] == "true",
            aiBusyIds = aiBusy.toSet(),
            entries = entries,
            review = queries.allEvents().executeAsList().filter { it.ignored == 0L }.mapNotNull { it.review_payload?.let { body -> json.decodeFromString<ReviewItem>(body) } },
            ignoredReview = queries.allEvents().executeAsList().filter { it.ignored != 0L }.mapNotNull { it.review_payload?.let { body -> json.decodeFromString<ReviewItem>(body) } },
            today = today,
            onboardingComplete = preferences["onboarded"] == "true",
            onboardingStep = preferences["onboardingStep"]?.toIntOrNull() ?: 0,
            currency = currency,
            capturePaused = preferences["paused"] == "true" || overSourceLimit(),
            captureOverLimit = overSourceLimit(),
            confirmations = preferences["confirmations"] != "false",
            capture = platform.captureState(selected).copy(lastReceived = preferences["lastReceived"]),
            requestedEntryId = focusedId,
            accounts = accounts,
            myNames = load("myNames:v1", emptyList()),
            transferSuggestions = load("suggestions:v1", emptyList<TransferSuggestion>()).filter { it.firstId in live && it.secondId in live },
            goals = load("goals:v1", emptyList<Goal>()).filterNot { it.archived }.mapNotNull { Goals.progress(it, entries, accounts, today) },
            insights = runCatching { InsightEngine.compute(entries, accounts, currency, today) }.getOrDefault(Insights(null, null, emptyList())),
            notify = NudgeCategory.entries.associateWith { (preferences["notify:${it.name}"] ?: it.defaultOn.toString()) == "true" },
            learnedRules = load("learned:v1", emptyList()),
            chartStyle = ChartStyle.entries.firstOrNull { it.name == preferences["chartStyle"] } ?: ChartStyle.LINES,
            transferWindowMinutes = transferWindowMinutes(),
        )
    }

    private inline fun <reified T> load(key: String, default: T): T =
        preferences[key]?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() } ?: default
    private inline fun <reified T> store(key: String, value: T) {
        val text = json.encodeToString(value)
        queries.putPreference(key, text)
        preferences = preferences + (key to text)
    }
    private fun accounts(): List<Account> = load("accounts:v1", emptyList())
    private fun transferWindowMinutes(): Int = (preferences["transferWindow:v1"]?.toIntOrNull() ?: 10).coerceIn(1, 60)

    /** Entries from before accounts existed carry a plain account name. Give each one a real account once. */
    private fun migrateAccounts() {
        val known = accounts().map { norm(it.name) }.toSet()
        val missing = state.value.entries.flatMap { listOf(it.account, it.counterAccount) }
            .filter(::isNamed).map { it.trim() }.distinctBy(::norm).filter { norm(it) !in known }
        if (missing.isNotEmpty()) perform { store("accounts:v1", accounts() + missing.map { Account(platform.id(), it) }) }
    }

    private fun perform(block: () -> Unit): String? = try {
        block()
        reload()
        runCatching { checkGoals() }
        null
    } catch (e: Exception) {
        val message = if (e is IllegalArgumentException) e.message ?: "Check these details" else "Could not save. Please try again."
        mutableState.value = mutableState.value.copy(error = message)
        message
    }

    private fun preference(key: String, value: String) { perform { queries.putPreference(key, value) } }
    private fun put(entry: Entry) { queries.putEntry(entry.id, json.encodeToString(entry)) }
    private fun validateTotals(entry: Entry) { totals(state.value.entries.filterNot { it.id == entry.id } + entry, entry.currency) }
    private fun entry(id: String): Entry = state.value.entries.firstOrNull { it.id == id } ?: error("Entry no longer exists")

    override fun save(draft: EntryDraft, entryId: String?, reviewId: String?): String? = perform {
        require(draft.directionConfirmed) { "Choose whether money came in or went out" }
        require(entryId == null || reviewId == null) { "Choose an existing record or a review alert" }
        require(reviewId == null || state.value.review.any { it.id == reviewId }) { "This alert is no longer waiting for review" }
        val amount = Money.parse(draft.amount, draft.currency) ?: throw IllegalArgumentException("Enter a positive amount with the right decimal places and a supported currency")
        require(draft.kind != Kind.INCOME || draft.direction == Direction.IN) { "Income must be money in. Choose a different kind for money out." }
        require(draft.kind !in listOf(Kind.EXPENSE, Kind.FEE) || draft.direction == Direction.OUT) { "Spending and fees must be money out. Choose a different kind for money in." }
        val date = draft.date.ifBlank { platform.now().take(10) }
        require(validDate(date)) { "Use a real date in YYYY-MM-DD format" }
        val reviewed = reviewId?.let { id -> state.value.review.firstOrNull { it.id == id } }
        val old = entryId?.let { entry(it) }
        require(old?.deleted != true) { "Restore this record before editing it" }
        val isTransfer = draft.kind == Kind.TRANSFER
        val sameLeg = old != null && isTransfer && old.amountMinor == amount && old.currency == draft.currency && old.direction == draft.direction
        val keptGroup = old?.transferGroupId.takeIf { sameLeg }
        val item = Entry(
            id = old?.id ?: platform.id(), amountMinor = amount, currency = draft.currency,
            direction = draft.direction, kind = draft.kind, status = draft.status,
            counterparty = draft.counterparty.trim(), account = draft.account.ifBlank { "Unassigned" },
            note = draft.note.trim(), date = date, source = old?.source ?: reviewId?.let { id -> state.value.review.firstOrNull { it.id == id }?.source } ?: "Manual",
            userEdited = old != null || reviewId != null, revision = (old?.revision ?: 0) + 1,
            capturedAt = old?.capturedAt?.takeIf { it.isNotBlank() } ?: platform.now(),
            transferGroupId = keptGroup, counterAccount = if (isTransfer) old?.counterAccount ?: "" else "",
        )
        validateTotals(item)
        database.transaction {
            ensureAccount(item.account)
            // An edit that changes what a transfer leg is must not leave its partner pointing at it.
            old?.transferGroupId?.let { group ->
                state.value.entries.filter { it.transferGroupId == group && it.id != item.id }.forEach { other ->
                    put(if (keptGroup == null) other.copy(transferGroupId = null, revision = other.revision + 1)
                        else other.copy(counterAccount = item.account.takeIf(::isNamed) ?: other.counterAccount))
                }
            }
            put(item)
            if (reviewId != null) queries.resolveEvent(item.id, 0, reviewId)
        }
        reviewed?.let { learnFrom(it, item) }
    }

    override fun delete(entryId: String): String? = perform {
        val e = entry(entryId)
        database.transaction {
            // The other leg stays recorded as a single transfer rather than pointing at a removed entry.
            e.transferGroupId?.let { group ->
                state.value.entries.filter { it.transferGroupId == group && it.id != e.id }
                    .forEach { put(it.copy(transferGroupId = null, revision = it.revision + 1)) }
            }
            put(e.copy(deleted = true, transferGroupId = null, revision = e.revision + 1))
        }
    }
    override fun restore(entryId: String): String? = perform {
        val e = entry(entryId).let { it.copy(deleted = false, revision = it.revision + 1) }
        validateTotals(e)
        put(e)
    }
    override fun ignoreReview(reviewId: String): String? = perform { queries.setEventIgnored(1, reviewId) }
    override fun restoreReview(reviewId: String): String? = perform { queries.setEventIgnored(0, reviewId) }
    override fun captureText(text: String, source: String): String? = ingest(text, source, platform.id(), respectPause = false)

    /** The delivery key identifies a posted event, not a guessed financial duplicate. */
    fun ingest(text: String, source: String, deliveryKey: String, respectPause: Boolean = true): String? = perform {
        require(text.isNotBlank()) { "Paste a payment alert first" }
        require(text.length <= 8192) { "This alert is too long" }
        if (respectPause && state.value.capturePaused) return@perform
        val eventId = deliveryKey
        if (queries.findEvent(eventId).executeAsOneOrNull() != null) return@perform
        var parsed = AlertParser.parse(text, state.value.currency, platform.now().take(10))
        if (parsed.irrelevant) return@perform
        parsed = applyLearned(parsed, source, text)
        var created: Entry? = null
        var waiting: ReviewItem? = null
        database.transaction {
            queries.putPreference("lastReceived", platform.now())
            val draft = parsed.draft ?: return@transaction
            val referenceKey = parsed.reference?.let { "ref:$source:$it" }
            val existing = referenceKey?.let { key -> preferences[key] }?.let { id -> state.value.entries.firstOrNull { it.id == id } }
            var reason = parsed.reason
            var linked: Entry? = null
            val amount = Money.parse(draft.amount, draft.currency)
            if (existing != null) {
                if (existing.amountMinor == amount && existing.currency == draft.currency && existing.direction == draft.direction) {
                    linked = existing
                    if (!existing.deleted && !existing.userEdited && existing.status == PaymentStatus.PENDING && draft.status != PaymentStatus.PENDING) {
                        linked = existing.copy(status = draft.status, revision = existing.revision + 1)
                        validateTotals(linked)
                        put(linked)
                    } else if (existing.status != draft.status && !existing.deleted) reason = "Check this status update against the existing record"
                } else reason = "Reference matches but payment details differ"
            }
            if (reason == null && linked == null && amount != null) {
                val fresh = annotate(Entry(platform.id(), amount, draft.currency, draft.direction, draft.kind, draft.status,
                    draft.counterparty, date = draft.date, source = source, capturedAt = platform.now()), text, source)
                validateTotals(fresh)
                put(fresh)
                created = pairOrSuggest(fresh)
                if (referenceKey != null) queries.putPreference(referenceKey, fresh.id)
            }
            val review = if (reason != null) ReviewItem(eventId, source, text, reason, draft) else null
            waiting = review
            queries.putEvent(eventId, source, text, platform.now(), linked?.id ?: created?.id,
                review?.let { json.encodeToString(it) }, 0)
        }
        // Persist before notifying. Notification failure must never roll back a committed ledger write.
        reload()
        waiting?.takeIf { state.value.aiAutomatic }?.let { queueAi(it.id, automatic = true) }
        created?.takeIf { it.status == PaymentStatus.COMPLETED && state.value.confirmations }?.let {
            try { platform.recorded(it) } catch (_: Exception) { /* Ledger remains committed. */ }
        }
    }

    override fun setCurrency(currency: String) { if (currency in Money.currencies) preference("currency", currency) }
    override fun setOnboardingStep(step: Int) = preference("onboardingStep", step.coerceAtLeast(0).toString())
    override fun finishOnboarding() = preference("onboarded", "true")
    /** Shows the intro again at [step]. Only the intro flag changes; records, rules and settings stay as they are. */
    fun replayOnboarding(step: Int) {
        perform {
            database.transaction {
                queries.putPreference("onboarded", "false")
                queries.putPreference("onboardingStep", step.coerceAtLeast(0).toString())
            }
        }
    }
    override fun setCapturePaused(paused: Boolean) = preference("paused", paused.toString())
    override fun setConfirmations(enabled: Boolean) = preference("confirmations", enabled.toString())
    override fun selectSource(sourceId: String, selected: Boolean) {
        perform {
            val limit = subscription.effectiveTier(platform.nowMillis()).watchedApps
            val count = preferences.count { it.key.startsWith("source:") && it.value == "true" }
            require(!selected || preferences["source:$sourceId"] == "true" || limit == null || count < limit) {
                "${subscription.effectiveTier(platform.nowMillis()).label} supports $limit watched apps. Change your plan in Settings."
            }
            queries.putPreference("source:$sourceId", selected.toString())
        }
    }
    override fun refreshCapture() { perform { } }
    override fun openCaptureSettings() = platform.openCaptureSettings()
    override fun installShortcut() = platform.installShortcut()
    override fun showSetupGuide(step: Int) = platform.showSetupGuide(step)
    override fun setupGuideVisible(visible: Boolean) = platform.setupGuideVisible(visible)
    override fun requestNotificationPermission() = platform.requestNotificationPermission()
    override fun clearError() { mutableState.value = mutableState.value.copy(error = null) }
    fun focusEntry(id: String) { focusedId = id; mutableState.value = mutableState.value.copy(requestedEntryId = id) }
    override fun clearFocus() { focusedId = null; mutableState.value = mutableState.value.copy(requestedEntryId = null) }
    private fun overSourceLimit(): Boolean {
        val limit = subscription.effectiveTier(platform.nowMillis()).watchedApps ?: return false
        return preferences.count { it.key.startsWith("source:") && it.value == "true" } > limit
    }
    fun isSelected(sourceId: String): Boolean = preferences["source:$sourceId"] == "true" && !overSourceLimit()

    override fun setAiEnabled(enabled: Boolean) {
        perform {
            database.transaction {
                queries.putPreference("aiConsent:v1", enabled.toString())
                if (!enabled) queries.putPreference("aiAutomatic:v1", "false")
            }
        }
    }
    override fun setAutomaticAi(enabled: Boolean) {
        perform {
            require(!enabled || state.value.aiEnabled) { "Allow sending alerts before enabling automatic help" }
            queries.putPreference("aiAutomatic:v1", enabled.toString())
        }
    }
    override fun refreshSubscription() = platform.refreshSubscription()
    override fun purchase(offerId: String) = platform.purchase(offerId)
    override fun manageSubscription() = platform.manageSubscription()
    fun updateSubscription(value: SubscriptionState) { subscription = value; reload() }
    fun billingState(): SubscriptionState = subscription
    override fun requestAi(reviewId: String): String? = queueAi(reviewId, automatic = false)
    private fun queueAi(reviewId: String, automatic: Boolean): String? {
        if (!state.value.aiEnabled) return "Enable AI help in Settings first"
        if (subscription.effectiveTier(platform.nowMillis()) == PlanTier.FREE) return "Choose Lite or Premium for AI help"
        if (!subscription.available) return "AI help is not connected. Your alert stays in Review."
        if (subscription.used >= subscription.tier.monthlyAi) return "Monthly AI allowance reached. You can still review this alert yourself."
        val item = state.value.review.firstOrNull { it.id == reviewId } ?: return "This alert is no longer waiting for review"
        if (item.text.length > 2000) return "This alert is too long for AI help. Review it manually."
        if (!aiBusy.add(reviewId)) return "AI help is already running for this alert"
        reload()
        try { platform.analyseAlert(item, automatic) } catch (_: Exception) {
            completeAi(reviewId, item.text, null, "Couldn't queue AI help. Your alert stays in Review.")
            return "Couldn't queue AI help"
        }
        return null
    }
    fun completeAi(reviewId: String, originalText: String, suggestion: AiSuggestion?, error: String? = null) {
        aiBusy.remove(reviewId)
        perform {
            val item = state.value.review.firstOrNull { it.id == reviewId }
            if (state.value.aiEnabled && item != null && item.text == originalText && suggestion != null) {
                // Suggestions never change money, resolve an event, or silently confirm direction.
                val updated = item.copy(aiSuggestion = suggestion)
                queries.updateReview(json.encodeToString(updated), reviewId)
            }
        }
        if (error != null) mutableState.value = state.value.copy(error = error)
    }


    // Learned rules: what the user (or the user confirming the AI) already decided about an alert template

    private fun learnedRules(): List<LearnedRule> = load("learned:v1", emptyList())

    /** A matching rule settles direction and kind only. Anything the parser flags for another reason stays in Review. */
    private fun applyLearned(parsed: ParsedAlert, source: String, text: String): ParsedAlert {
        val draft = parsed.draft ?: return parsed
        if (draft.amount.isBlank() || parsed.reason !in Learned.confirmable) return parsed
        val rules = learnedRules()
        val rule = Learned.best(rules, source, Learned.words(text)) ?: return parsed
        val kind = if (rule.kind != Kind.UNCLASSIFIED) rule.kind else draft.kind
        val learned = draft.copy(direction = rule.direction, kind = kind)
        if (rule.confirmations < Learned.AUTO_AT) {
            // One correction is a hint, not a decision.
            return parsed.copy(draft = learned.copy(directionConfirmed = false), reason = Learned.REASON_HINT)
        }
        store("learned:v1", rules.map { if (it.id == rule.id) it.copy(uses = it.uses + 1) else it })
        return parsed.copy(draft = learned.copy(directionConfirmed = true), reason = null)
    }

    private fun learnFrom(review: ReviewItem, saved: Entry) {
        if (review.reason !in Learned.confirmable || review.draft.amount.isBlank()) return
        val suggestion = review.aiSuggestion
        val aiAgreed = suggestion?.direction == saved.direction && state.value.aiEnabled
        runCatching {
            store("learned:v1", Learned.learn(learnedRules(), review.source, review.text, saved.direction, saved.kind, aiAgreed, platform.id(), platform.now()))
            reload()
        }
    }

    override fun setChartStyle(style: ChartStyle) = preference("chartStyle", style.name)
    override fun deleteLearnedRule(ruleId: String) { perform { store("learned:v1", learnedRules().filterNot { it.id == ruleId }) } }
    override fun clearLearnedRules() { perform { store("learned:v1", emptyList<LearnedRule>()) } }

    // Accounts and transfers

    private fun ensureAccount(name: String) {
        if (!isNamed(name)) return
        val list = accounts()
        if (list.none { it.name.equals(name.trim(), ignoreCase = true) }) store("accounts:v1", list + Account(platform.id(), name.trim()))
    }

    override fun saveAccount(account: Account): String? = perform {
        val name = account.name.trim()
        require(name.isNotEmpty() && name.length <= 40) { "Give the account a name of up to 40 characters" }
        require(isNamed(name)) { "Choose a different account name" }
        val list = accounts()
        val old = list.firstOrNull { it.id == account.id }
        require(list.none { it.id != old?.id && it.name.equals(name, ignoreCase = true) }) { "You already have an account with this name" }
        val hints = account.hints.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy(::norm).take(10)
        require(hints.all { it.length <= 80 }) { "Keep each hint under 80 characters" }
        val saved = account.copy(id = old?.id ?: platform.id(), name = name, hints = hints)
        database.transaction {
            store("accounts:v1", if (old == null) list + saved else list.map { if (it.id == old.id) saved else it })
            if (old != null && old.name != name) {
                fun renamed(value: String) = if (value.equals(old.name, ignoreCase = true)) name else value
                state.value.entries.filter { it.account.equals(old.name, true) || it.counterAccount.equals(old.name, true) }
                    .forEach { put(it.copy(account = renamed(it.account), counterAccount = renamed(it.counterAccount))) }
                store("goals:v1", load("goals:v1", emptyList<Goal>()).map { g -> g.copy(accounts = g.accounts.map(::renamed)) })
            }
        }
    }

    override fun setAccountArchived(accountId: String, archived: Boolean): String? = perform {
        val list = accounts()
        require(list.any { it.id == accountId }) { "This account no longer exists" }
        store("accounts:v1", list.map { if (it.id == accountId) it.copy(archived = archived) else it })
    }

    override fun setMyNames(names: List<String>) {
        perform { store("myNames:v1", names.map { it.trim() }.filter { it.length in 3..60 }.distinctBy(::norm).take(8)) }
    }

    private fun ownership() = OwnershipContext(accounts(), load("myNames:v1", emptyList()),
        load("mine:v1", emptySet<String>()), load("notmine:v1", emptySet<String>()))

    /** Attach the account an alert belongs to, and mark it as a transfer when the counterparty is the user's own. */
    private fun annotate(entry: Entry, text: String, source: String): Entry {
        val account = Transfers.matchAccount(accounts(), source, text)
        var e = if (account != null) entry.copy(account = account.name) else entry
        if (e.status != PaymentStatus.COMPLETED) return e
        Transfers.ownCounterparty(ownership(), e.counterparty, e.account)?.let { own ->
            e = e.copy(kind = Kind.TRANSFER, counterAccount = own.account ?: "")
        }
        return e
    }

    /** Link an obvious partner, or ask once when two entries merely look like both sides of one transfer. */
    private fun pairOrSuggest(entry: Entry): Entry {
        if (entry.status != PaymentStatus.COMPLETED || entry.transferGroupId != null) return entry
        val partners = Transfers.counterparts(entry, state.value.entries, transferWindowMinutes() * 60L)
        val other = partners.singleOrNull() ?: return entry
        val known = Transfers.pairKey(entry.account, other.account)?.let { load("pairs:v1", emptyMap<String, Boolean>())[it] }
        return when {
            entry.kind == Kind.TRANSFER || known == true -> link(entry, other)
            known == false -> entry
            else -> {
                val list = load("suggestions:v1", emptyList<TransferSuggestion>())
                if (list.none { setOf(it.firstId, it.secondId) == setOf(entry.id, other.id) }) {
                    store("suggestions:v1", list + TransferSuggestion(platform.id(), other.id, entry.id))
                }
                entry
            }
        }
    }

    private fun link(a: Entry, b: Entry): Entry {
        val group = platform.id()
        fun leg(self: Entry, other: Entry) = self.copy(
            kind = Kind.TRANSFER, transferGroupId = group, revision = self.revision + 1,
            counterAccount = other.account.takeIf(::isNamed) ?: self.counterAccount,
        )
        val first = leg(a, b)
        put(first)
        put(leg(b, a))
        return first
    }

    private fun resetLeg(e: Entry) = e.copy(
        kind = if (e.direction == Direction.IN) Kind.INCOME else Kind.EXPENSE,
        transferGroupId = null, counterAccount = "", userEdited = true, revision = e.revision + 1,
    )

    private fun rememberPair(a: Entry, b: Entry, mine: Boolean) {
        Transfers.pairKey(a.account, b.account)?.let { store("pairs:v1", load("pairs:v1", emptyMap<String, Boolean>()) + (it to mine)) }
        rememberDestination(a.counterparty, mine)
        rememberDestination(b.counterparty, mine)
    }

    private fun rememberDestination(counterparty: String, mine: Boolean) {
        val key = norm(counterparty)
        if (key.isEmpty()) return
        val yes = load("mine:v1", emptySet<String>())
        val no = load("notmine:v1", emptySet<String>())
        store("mine:v1", if (mine) yes + key else yes - key)
        store("notmine:v1", if (mine) no - key else no + key)
    }

    override fun answerTransfer(suggestionId: String, mine: Boolean): String? = perform {
        val suggestion = load("suggestions:v1", emptyList<TransferSuggestion>()).firstOrNull { it.id == suggestionId }
            ?: throw IllegalArgumentException("This question is no longer open")
        val a = entry(suggestion.firstId)
        val b = entry(suggestion.secondId)
        database.transaction {
            if (mine && !a.deleted && !b.deleted && a.transferGroupId == null && b.transferGroupId == null) link(a, b)
            rememberPair(a, b, mine)
            store("suggestions:v1", load("suggestions:v1", emptyList<TransferSuggestion>()).filterNot { it.id == suggestionId })
        }
    }

    override fun linkTransfer(entryId: String, otherEntryId: String): String? = perform {
        require(entryId != otherEntryId) { "Choose two different entries" }
        val a = entry(entryId)
        val b = entry(otherEntryId)
        require(!a.deleted && !b.deleted) { "Restore both entries first" }
        require(a.transferGroupId == null && b.transferGroupId == null) { "One of these is already linked. Unlink it first." }
        require(a.direction != b.direction && a.currency == b.currency && a.amountMinor == b.amountMinor) {
            "A transfer needs one entry going out and one coming in, for the same amount"
        }
        database.transaction {
            link(a, b)
            rememberPair(a, b, true)
            store("suggestions:v1", load("suggestions:v1", emptyList<TransferSuggestion>())
                .filterNot { it.firstId in setOf(a.id, b.id) || it.secondId in setOf(a.id, b.id) })
        }
    }

    override fun markTransfer(entryId: String, counterAccount: String): String? = perform {
        val e = entry(entryId)
        require(!e.deleted) { "Restore this record before editing it" }
        val other = counterAccount.trim()
        require(other.isEmpty() || accounts().any { it.name.equals(other, ignoreCase = true) }) { "Choose one of your accounts" }
        val name = accounts().firstOrNull { it.name.equals(other, true) }?.name ?: ""
        database.transaction {
            put(e.copy(kind = Kind.TRANSFER, counterAccount = name, userEdited = true, revision = e.revision + 1))
            rememberDestination(e.counterparty, true)
        }
    }

    override fun unlinkTransfer(entryId: String): String? = perform {
        val e = entry(entryId)
        database.transaction {
            val legs = if (e.transferGroupId != null) state.value.entries.filter { it.transferGroupId == e.transferGroupId } else listOf(e)
            legs.forEach { put(resetLeg(it)); rememberDestination(it.counterparty, false) }
        }
    }

    fun creditDetail(entry: Entry): String? {
        if (entry.direction != Direction.IN || entry.kind == Kind.TRANSFER || state.value.notify[NudgeCategory.CREDIT] != true) return null
        val net = runCatching { totals(state.value.entries.filter { it.date == entry.date }, entry.currency).net }.getOrNull() ?: return null
        return "Money in. Net for the day: " + (if (net < 0) "-" else "+") + Money.display(kotlin.math.abs(net), entry.currency)
    }

    // Goals

    override fun saveGoal(goal: Goal): String? = perform {
        val name = goal.name.trim()
        require(name.isNotEmpty() && name.length <= 40) { "Give the goal a name of up to 40 characters" }
        require(goal.amountMinor > 0) { "Enter an amount above zero" }
        require(goal.currency in Money.currencies) { "Choose a supported currency" }
        if (goal.period == GoalPeriod.CUSTOM) {
            val start = Days.number(goal.customStart)
            val end = Days.number(goal.customEnd)
            require(start != null && end != null) { "Use real dates in YYYY-MM-DD format" }
            require(end >= start) { "The end date must not be before the start" }
        }
        val list = load("goals:v1", emptyList<Goal>())
        val saved = goal.copy(
            id = goal.id.takeIf { id -> list.any { it.id == id } } ?: platform.id(), name = name,
            counterparty = if (goal.type == GoalType.SPENDING_LIMIT) goal.counterparty.trim() else "",
            accounts = if (goal.type == GoalType.SAVING_TARGET) goal.accounts else emptyList(),
        )
        store("goals:v1", if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved)
    }

    override fun archiveGoal(goalId: String): String? = perform {
        store("goals:v1", load("goals:v1", emptyList<Goal>()).map { if (it.id == goalId) it.copy(archived = true) else it })
    }

    // Notifications

    override fun setNotify(category: NudgeCategory, enabled: Boolean) = preference("notify:${category.name}", enabled.toString())

    private fun quietNow(): Boolean = Days.hour(platform.now())?.let { it >= 22 || it < 7 } ?: false

    /** Nothing is recorded as sent unless a sink accepted it, so a skipped nudge is retried later. */
    private fun deliver(nudge: Nudge): Boolean {
        val sink = nudgeSink ?: return false
        if (state.value.notify[nudge.category] != true || quietNow()) return false
        val key = "nudgeCount:${platform.now().take(10)}"
        val used = preferences[key]?.toIntOrNull() ?: 0
        if (used >= 3) return false
        try { sink.deliver(nudge) } catch (_: Exception) { return false }
        queries.putPreference(key, (used + 1).toString())
        preferences = preferences + (key to (used + 1).toString())
        return true
    }

    private fun mark(key: String, value: String) { queries.putPreference(key, value); preferences = preferences + (key to value) }

    private fun checkGoals() {
        for (p in state.value.goals) {
            if (p.state !in setOf(GoalState.CLOSE, GoalState.OVER, GoalState.REACHED)) continue
            val key = "goalState:${p.goal.id}:${p.from}"
            if (preferences[key] == p.state.name) continue
            val c = p.goal.currency
            val text = when {
                p.goal.type == GoalType.SPENDING_LIMIT && p.state == GoalState.OVER -> "Over by ${Money.display(p.amountMinor - p.goal.amountMinor, c)}."
                p.goal.type == GoalType.SPENDING_LIMIT -> "${(p.fraction * 100).toInt()}% used, ${p.daysLeft} days left."
                p.state == GoalState.REACHED -> "Target reached."
                else -> "${Money.display(p.remainingMinor, c)} to go in ${p.daysLeft} days."
            }
            if (deliver(Nudge("goal:${p.goal.id}:${p.state}", NudgeCategory.GOAL, p.goal.name, text))) mark(key, p.state.name)
        }
    }

    /** Habits, summaries and reminders. Run about once a day; goals are also checked after every change. */
    fun runDailyNudges() {
        try {
            reload()
            checkGoals()
            val today = Days.number(state.value.today) ?: return
            val s = state.value
            s.insights.signals.firstOrNull { signal ->
                preferences["habit:${signal.key}"] == null &&
                    (preferences["habitLast:${signal.kind}"]?.toLongOrNull()?.let { today - it >= 7 } ?: true)
            }?.takeIf { s.notify[NudgeCategory.HABIT] == true }?.let { signal ->
                val title = when (signal.positive) { true -> "Nice work"; false -> "Spending update"; null -> "Worth a look" }
                if (deliver(Nudge("habit:${signal.key}", NudgeCategory.HABIT, title, signal.text))) {
                    mark("habit:${signal.key}", "sent"); mark("habitLast:${signal.kind}", today.toString())
                }
            }
            summaryNudge(today, "week", Days.weekday(today) <= 2, Days.weekStart(today) - 7, Days.weekStart(today) - 1, "Last week")
            val monthStart = Days.monthStart(today)
            summaryNudge(today, "month", Days.dayOfMonth(today) <= 3, Days.monthStart(monthStart - 1), monthStart - 1, "Last month")
            val waiting = s.review.size
            if (s.notify[NudgeCategory.REMINDER] == true && waiting >= 3 &&
                (preferences["reminderLast"]?.toLongOrNull()?.let { today - it >= 3 } ?: true)
            ) {
                if (deliver(Nudge("reminder:$today", NudgeCategory.REMINDER, "Waiting for you", "$waiting alerts need a quick look."))) mark("reminderLast", today.toString())
            }
        } catch (_: Exception) { /* Nudges are best effort and must never affect the ledger. */ }
    }

    private fun summaryNudge(today: Long, kind: String, due: Boolean, from: Long, to: Long, label: String) {
        val key = "summary:$kind:$from"
        if (!due || preferences[key] != null || state.value.notify[NudgeCategory.SUMMARY] != true) return
        val currency = state.value.currency
        val sum = InsightEngine.summary(state.value.entries, state.value.accounts, currency, from, to)
        if (sum.incoming == 0L && sum.outgoing == 0L && sum.saved == 0L) { mark(key, "empty"); return }
        val parts = mutableListOf("in ${Money.display(sum.incoming, currency)}", "out ${Money.display(sum.outgoing, currency)}")
        if (sum.saved > 0) parts += "saved ${Money.display(sum.saved, currency)}"
        sum.topCounterparty?.let { parts += "most to $it" }
        if (deliver(Nudge("summary:$kind:$from", NudgeCategory.SUMMARY, label, parts.joinToString(", ") + "."))) mark(key, "sent")
    }

    override fun exportCsv(): String? = try {
        val header = "id,date,amount,currency,direction,kind,status,counterparty,account,note,source,counter_account,transfer_group"
        fun cell(value: String): String {
            // Avoid spreadsheet formula execution when opening untrusted notification-derived text.
            val safe = if (value.firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$value" else value
            return "\"${safe.replace("\"", "\"\"")}\""
        }
        val lines = state.value.entries.filterNot { it.deleted }.map { e ->
            listOf(e.id, e.date, Money.decimal(e.amountMinor, e.currency), e.currency, e.direction.name,
                e.kind.name, e.status.name, e.counterparty, e.account, e.note, e.source, e.counterAccount, e.transferGroupId ?: "").joinToString(",", transform = ::cell)
        }
        platform.exportCsv((listOf(header) + lines).joinToString("\r\n"))
    } catch (_: Exception) { "Could not export the ledger" }
}

internal fun validDate(value: String): Boolean {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
    val year = value.take(4).toInt(); val month = value.substring(5, 7).toInt(); val day = value.takeLast(2).toInt()
    if (year !in 1900..2200 || month !in 1..12) return false
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = listOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    return day in 1..days[month - 1]
}
