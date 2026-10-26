# Transaction engine

## Core distinction

A source event is evidence. A transaction is a ledger record. Multiple source events may describe one transaction, while one event may describe a payment plus a fee. Keep the relationship many-to-many and preserve provenance.

Never use one flat classifier label for direction, duplicate detection, economic meaning, and lifecycle state.

## Data concepts

- CaptureSource: platform, source kind, app identity if available, user label, selection state, and last observed input. Unknown identity is explicit.
- SourceEvent: local ID, source ID, available external key, title/subtitle/body, supplied event time, received time, revision, content fingerprint, and ingestion state.
- Interpretation: extracted fields with their source spans, candidate matches, decision results, model/parser version, and validation failures.
- Transaction: ID, amount, currency, direction, economic kind, lifecycle status, review state, occurrence time, account, counterparty, category, notes, origin, and revision.
- EvidenceLink: source event, transaction, and relationship such as creation, completion, duplicate evidence, fee, refund, or reversal.
- TransferGroup: linked owned-account legs, currencies and amounts per leg, fees, and unresolved counterpart state.
- Correction: user action, affected fields, previous and resulting revisions, and timestamp.
- Suppression: ignored event, undone capture, or deleted transaction fingerprint with a stated retention period.
- ProcessingJob: durable job ID, retry state, consent version, attempt count, and next eligible attempt.

Amounts use integer minor units with currency exponent metadata and range checks. No floating-point arithmetic for stored money. Currency-ambiguous symbols cannot silently resolve to a country inferred from locale.

Unknown amount, currency, or direction is valid in captured evidence and drafts but prevents acceptance into confirmed movement totals. Manual entries must have a known currency before being accepted. A refund may be incoming or outgoing depending on who is returning money; direction is always relative to the user's account. An internal transfer links directional account legs rather than introducing a third money direction.

## Ingestion and processing

1. Filter to an allowed source before retaining content. Ignore Cashflow's own confirmation notifications to prevent feedback loops.
2. Validate input size and format, assign local capture identity, and persist the event or an explicit failure.
3. Identify exact transport replay or notification revision before financial matching. A notification key may be reused for multiple payments, so key equality alone must not suppress changed content.
4. Parse known formats and extract amounts, currencies, references, account hints, counterparties, and dates. Multiple amounts may be payment, balance, or fee; uncertain roles go to review.
5. Fetch a bounded set of candidate records and relevant earlier source events.
6. Use local rules first. Call a decision provider only with consent and when its judgement can resolve an ambiguity.
7. Validate the result against allowed decisions and ledger invariants.
8. Atomically create, update, link, ignore, or queue review. Persist changes and correction metadata together.
9. Publish a confirmation only after successful commit and only according to notification preferences.

Callbacks and intents must persist input promptly. They must not depend on an indefinitely running UI process or wait unboundedly for a remote provider. The platform prototype determines how queued work resumes under iOS lifecycle limits.

## Model responsibilities

Jev is a candidate for bounded questions: financial relevance, direction, lifecycle status, economic meaning, and choice among candidate matches plus None and Uncertain. Amount extraction remains a separate parser or validated extraction component; a decision model is not assumed to generate arbitrary structured fields.

Send named candidates with stable local IDs, then validate that returned choices belong to the supplied candidate set. Never allow the model to choose an arbitrary record ID or execute a ledger write directly.

Model probabilities are evidence, not calibrated guarantees. Auto-record and auto-match thresholds are independent and must be selected from an evaluation dataset. False merges deserve a stricter policy than a suggested category. Rules can veto a high-confidence model result when amounts, currency, references, or lifecycle transitions conflict.

Notification text is untrusted data. Instructions contained inside it must not change classification rules, reveal other activity, or trigger tools. The provider receives no ability to initiate transfers or perform unrelated actions.

## Recent context

Proposed initial context window: 30 minutes, adjustable under advanced settings from 5 minutes to 24 hours. This is a candidate retrieval window, not a duplicate rule. Give the provider at most 20 relevant events/records within a fixed payload limit; tune through evaluation.

Prioritise matching references, app/account identity, currency, counterparty, amount, and nearby time. Look outside the short window for reference-linked pending completions, refunds, and reversals. Exclude unrelated notifications and do not upload the entire ledger. Explain that a larger context window can increase remote data sharing.

## Decision dimensions

- Relevance: transaction evidence, informational balance, promotion, authentication, or unknown.
- Direction: incoming, outgoing, or unclear.
- Lifecycle: pending, completed, failed, or unknown; a confirmed reversal links to its original record.
- Economic meaning: income, expense, internal transfer, refund, borrowing, repayment, fee, withdrawal, or unclassified.
- Relationship: new, duplicate evidence, status update, linked opposite leg, refund/reversal, or uncertain.
- Review state: accepted automatically, accepted by user, needs review, ignored, or deleted.

Classification into income or expense may remain unresolved even when amount and direction are certain. Show an unclassified cash movement rather than forcing an economic interpretation.

## Duplicate and lifecycle policy

- Exact replay of a previously handled event is idempotent.
- Changed content under an existing notification key produces a revision for interpretation.
- Two independent payments with the same amount and recipient remain separate unless stronger evidence links them.
- Debit and credit alerts for the user's own transfer are separate account legs, linked under one transfer group.
- Pending-to-completed updates modify one record when evidence supports the relationship.
- Failed pending entries contribute no completed movement. A completed debit followed by a reversal preserves the debit and its compensating movement.
- Partial refunds link to the original, retain their own amount, and cannot exceed supported refund evidence without review.
- Cross-currency transfers preserve both amounts and any explicit fee; never invent the missing leg.
- Group summaries and bundled notifications must not be double-counted with their child notifications.
- Late or ambiguous conflicting evidence enters Review rather than silently rewriting settled history.

## User control and concurrency

User-edited fields are protected. A later interpretation may attach evidence or propose a change but cannot overwrite those fields silently. Jobs compare record revisions before committing; a stale job must recompute or create a review proposal.

Undo and delete suppress recreation from the same evidence while keeping restoration possible. Store only the minimum fingerprint needed after raw event expiry; document the limits of replay prevention once suppression data is erased. New evidence of a genuinely different payment must still be eligible for recording.

Merge moves evidence under a surviving record while keeping a reversible operation history. Split restores mistaken evidence assignments and requires the user to resolve missing amounts. A notification Undo against a record that has since been edited or merged should open the current entry for confirmation rather than blindly deleting it.

## Offline and failure handling

Local known-format parsing and manual entry continue offline. Events requiring a remote model stay queued or enter Review with their available fields. Use bounded retries with backoff and idempotent commits. Provider failure is never evidence that a payment failed.

When consent is withdrawn, stop new remote requests, cancel queued uploads, and discard unsent payloads. Explain that already transmitted data cannot be recalled by changing a local setting.

## Evaluation fixtures

Build synthetic or consented, redacted fixtures covering debit/credit, promotions, balances, OTPs, multiple amounts, same-amount repeated payments, cross-app duplicate evidence, own-account transfers, fees, foreign currency, grouped alerts, pending completion, failures, reversals, partial refunds, late arrival, edited entries, and retry races.

Track field accuracy, automatic-record precision, false merge rate, missed duplicates, review rate, latency, and cost per event separately. Do not optimise review rate by accepting uncertain transactions.
