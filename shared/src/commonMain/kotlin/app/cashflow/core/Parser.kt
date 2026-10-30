package app.cashflow.core

data class ParsedAlert(val draft: EntryDraft?, val reason: String?, val reference: String? = null, val irrelevant: Boolean = false)

/** Conservative baseline: unknown or conflicting wording never auto-enters the ledger. */
object AlertParser {
    private val currencyAmount = Regex("(?i)(NGN|USD|GBP|EUR|CAD|GHS|KES|ZAR|INR|JPY|KWD|₦|£|€)\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)")
    private val authentication = Regex("(?i)\\b(otp|one[- ]time|verification code|security code|passcode)\\b")
    fun parse(text: String, currency: String, today: String): ParsedAlert {
        if (authentication.containsMatchIn(text)) return ParsedAlert(null, null, irrelevant = true)
        val inWords = Regex("(?i)\\b(credited|received|credit alert|deposit received)\\b").containsMatchIn(text)
        val outWords = Regex("(?i)\\b(debited|sent|paid|debit alert|payment to)\\b").containsMatchIn(text)
        val draft = EntryDraft(currency = "", date = today, note = "", kind = Kind.UNCLASSIFIED)
        val amounts = currencyAmount.findAll(text).toList()
        if (!inWords && !outWords && amounts.isEmpty()) return ParsedAlert(null, null, irrelevant = true)
        val directionKnown = inWords != outWords
        val direction = if (inWords) Direction.IN else Direction.OUT
        val base = draft.copy(direction = direction, directionConfirmed = directionKnown, kind = Kind.UNCLASSIFIED)
        if (amounts.size != 1) return ParsedAlert(base, if (amounts.isEmpty()) "Amount or currency missing" else "Several amounts appear in this alert")
        val match = amounts.single()
        val code = when (val raw = match.groupValues[1].uppercase()) { "₦" -> "NGN"; "£" -> "GBP"; "€" -> "EUR"; else -> raw }
        val rawAmount = match.groupValues[2]
        if (!Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]+)?").matches(rawAmount)) {
            return ParsedAlert(base.copy(currency = code), "Check the amount separators")
        }
        val amount = rawAmount.replace(",", "")
        if (Money.parse(amount, code) == null) return ParsedAlert(base.copy(currency = code), "Check the amount")
        val pending = Regex("(?i)\\b(pending|processing|scheduled|initiated)\\b").containsMatchIn(text)
        val failed = Regex("(?i)\\b(failed|declined|unsuccessful|cancelled)\\b").containsMatchIn(text)
        val reversal = Regex("(?i)\\b(reversed|reversal|refund)\\b").containsMatchIn(text)
        val counterparty = Regex("(?i)\\b(?:to|from)\\s+([\\p{L}][\\p{L} .'-]{0,70})").find(text)?.groupValues?.get(1)
            ?.split(Regex("(?i)\\s+(?:ref(?:erence)?\\b|balance\\b|session\\s+id\\b)"), limit = 2)
            ?.first()?.trim()?.trimEnd('.') ?: ""
        val reference = Regex("(?i)\\b(?:ref(?:erence)?|session id)[: #]+([a-z0-9-]{5,80})").find(text)?.groupValues?.get(1)?.uppercase()
        val result = base.copy(
            amount = amount, currency = code, counterparty = counterparty,
            status = if (failed) PaymentStatus.FAILED else if (pending) PaymentStatus.PENDING else PaymentStatus.COMPLETED,
            kind = if (reversal) Kind.REFUND else Kind.UNCLASSIFIED,
        )
        // Broad text support is useful for review, but only explicit debit/credit alerts auto-record.
        val explicit = Regex("(?i)\\b(debited|credited|debit alert|credit alert)\\b").containsMatchIn(text)
        return ParsedAlert(result, when {
            !directionKnown -> "Check whether money came in or went out"
            reversal -> "Link this refund or reversal to its original payment"
            !explicit -> "Confirm the payment details"
            else -> null
        }, reference)
    }
}
