# Case 2 — Payments: Senior Interview Walkthrough

Complete [URL Shortener](practice-url-shortener.md), then stay with this payment lifecycle before
[Orders](practice-orders.md). Answer each prompt aloud before revealing the guided explanation.
This is a design exercise: provider contracts, durability, and capacity must be verified rather
than inferred from framework annotations or this lab's smaller examples.

## Scenario and progression

| Dimension | Working assumption |
|---|---|
| Product | Marketplace payments; authorize/capture, partial refund, status, settlement reconciliation |
| Baseline demand | Illustrative 200 new operations/s; 0.5 s mean provider latency |
| Acceptance | Durable pending intent is allowed; it is not a successful charge |
| Invariants | Stable logical charge identity; balanced local ledger; bounded refunds; authorized access |
| Dependencies | External provider with documented idempotency and lookup, plus asynchronous callbacks |

Progress through scope → capacity → identity/model → flow → alternatives → timeout → callbacks →
ledger → refund races → cancellation → resilience → settlement → security → regions → evidence.
Later outage and refund examples are separate challenges, not measurements from this repository.

## Round 1 — Define financial success

### Q1. What must be clarified before promising that a payment succeeded?

??? question "Reveal answer"

    **Verdict:** Authorization, capture, settlement, and customer-visible completion are different states.

    | Question | Design implication |
    |---|---|
    | Authorize now, capture later? | Separate operations and expiry handling |
    | Partial capture/refund? | Amount bounds and concurrent operation accounting |
    | Multiple currencies? | Per-currency balance and explicit FX accounting |
    | When may goods ship? | Defined required financial evidence |
    | Provider and settlement contracts? | Idempotency, finality, reversals, reconciliation |

    An accepted API request proves only durable intent under the chosen contract. An authorized
    amount may never be captured, and a captured payment can later have chargebacks. Do not collapse
    all financial states into one irreversible SUCCESS flag. Clarify retention, dispute handling,
    regulatory responsibilities, and operator approval for repair/refund actions.

    **Interview-ready answer:** “I would define the business success point and distinguish it from
    transport acceptance. My invariants concern one logical money movement, balanced accounting,
    authorized transitions, and traceable reconciliation—not a claim that every network call runs once.”

    **Follow-up:** Which confirmed state is sufficient to ship for this business?

## Round 2 — Capacity and recovery

### Q2. What limits throughput when 200 operations/s meet a slower provider?

??? question "Reveal answer"

    **Verdict:** Provider quotas and unresolved work matter more than application thread count.

    | Assumption | Calculation |
    |---|---|
    | 200/s at 0.5 s mean call latency | 100 mean in-flight calls before headroom |
    | Same arrivals at 8 s mean latency | 1,600 mean in-flight calls if unconstrained |
    | 15-minute total outage at 200/s | 180,000 new intents if all are accepted |
    | Recovery at 300/s with 200/s new work | 100/s net drain; about 30 minutes for that backlog |

    These calculations assume comparable work/rates; status queries, callbacks, retries, and refunds
    may share provider quotas and change the result. Durable workflow deadlines exceed individual
    HTTP deadlines. Avoid holding a DB connection for every provider wait.

    **Trade-off:** accepting unlimited pending payments creates storage and customer uncertainty;
    cap admission and reserve reconciliation capacity rather than scaling workers into a quota wall.

    **Interview-ready answer:** “I would budget provider calls and recovery separately, bound in-flight
    work, and measure unknown-state age. Virtual threads or more pods cannot expand provider capacity.”

    **Follow-up:** How much provider quota must remain available for reconciliation during recovery?

## Round 3 — Durable model and API

### Q3. Why separate intent, attempt, provider identity, and ledger identity?

??? question "Reveal answer"

    **Verdict:** One customer's financial intent can have several observations without becoming several charges.

    | Record | Identity and responsibility |
    |---|---|
    | API operation | Principal/key, request fingerprint, recoverable result |
    | Payment intent | Authorized business reference, amount/currency, lifecycle/version |
    | Provider operation | Stable logical provider key; provider reference and status |
    | Attempt/observation | Request/response/callback evidence and timing |
    | Ledger transaction and entries | Immutable posting identity; balanced accounting |
    | Refund operation | Separate identity and bounded refundable amount |
    | Inbox/outbox | Duplicate observation and recoverable next-step intent |

    Creation returns a resource or durable pending status; status reads authorize the caller.
    Amount/currency and ownership come from a verified quote or permitted merchant operation, not
    a blindly trusted client field. A changed fingerprint cannot reuse the previous key.

    **Interview-ready answer:** “I preserve logical identity across HTTP retries, provider attempts,
    callbacks, and accounting. Attempt history is audit evidence; it must not create a new charge
    identity merely because an earlier response was lost.”

    **Follow-up:** Can one provider operation identity represent both charge and refund?

## Round 4 — End-to-end flow

### Q4. Trace payment initiation without a transaction spanning the provider call.

??? question "Reveal answer"

    **Verdict:** Commit intent, perform the remote effect outside the DB transaction, then reconcile the outcome durably.

    ```mermaid
    sequenceDiagram
        actor Customer
        participant API
        participant DB as Payment authority
        participant Worker
        participant PSP as Provider
        Customer->>API: Create with scoped operation key
        API->>DB: Commit intent, identity, outbox
        API-->>Customer: Durable pending resource
        Worker->>DB: Recover claimed operation identity
        Worker->>PSP: Charge using stable provider key
        PSP-->>Worker: Result or ambiguous timeout
        Worker->>DB: Commit verified outcome or UNKNOWN plus recovery intent
        PSP->>API: Verified callback; duplicates possible
        API->>DB: Inbox, guarded outcome, ledger and next event
    ```

    Worker response and callback are two observations of one operation. They must converge through
    the same guarded state rules. The external commit gap remains even if every local method is
    annotated transactional. A disconnected caller does not cancel durable recovery ownership.

    **Interview-ready answer:** “Short local transactions surround, but do not contain, the provider
    call. Stable identity and reconciliation bridge the crash after provider acceptance and before
    local recording. Every accepted intent retains an owner until resolved or explicitly escalated.”

    **Follow-up:** What happens if the worker dies after charging but before recording the provider reference?

## Round 5 — Architectural comparison

### Q5. Synchronous provider call, durable worker, or distributed transaction?

??? question "Reveal answer"

    **Verdict:** Choose latency and recovery ownership without assuming an HTTP provider supports 2PC.

    | Choice | Benefit | Obligation |
    |---|---|---|
    | Synchronous fast path | Immediate result when provider is fast | Durable intent and timeout recovery still required |
    | Durable asynchronous worker | Survives client/pod lifetime; isolates capacity | Pending UX, queue deadlines, operations |
    | 2PC with participating resources | Atomicity under its specific protocol | Typical PSP HTTP calls do not participate |

    A synchronous path can wait for bounded completion of the same durable workflow; it should not
    be a separate unsafe charge implementation. Queues change scheduling, not payment uniqueness.
    A modular service and one financial database may be simpler than distributing ledger writes
    before a measured need exists.

    **Interview-ready answer:** “I would support immediate completion only inside a finite waiting
    budget, then expose pending status. The durable operation is shared by both paths. I would not
    describe ordinary provider HTTP calls as part of our database transaction.”

    **Follow-up:** If the client abandons pending checkout, who determines whether cancellation is safe?

## Round 6 — Unknown external outcome

### Q6. The provider charged, but the response was lost. Is it safe to retry or refund?

??? question "Reveal answer"

    **Verdict:** Preserve UNKNOWN and obtain evidence using the original provider identity.

    | Observation | Action |
    |---|---|
    | Verified decline | Record the decline; no blind repeated charging |
    | Timeout after possible send | Query/reconcile or retry under documented idempotency semantics |
    | Same provider operation confirmed successful | Apply once locally |
    | No reliable lookup and dedup window expired | Escalate; do not manufacture certainty |
    | Success for a cancelled business operation | Durable policy-driven compensation |

    An application deadline generally stops waiting, not the remote side effect. A new key can
    create a second charge. Provider deduplication windows may be shorter than your recovery horizon;
    old pending operations require special handling. A local unique row cannot force a provider to
    honor an identity it does not support.

    **Interview-ready answer:** “I would persist the uncertainty and reconcile before a new logical
    charge or refund. Permitted retries reuse the same identity. Where the provider contract cannot
    establish the outcome, the system needs a controlled unresolved state rather than a false guarantee.”

    **Follow-up:** What if provider lookup is eventually consistent and initially reports not found?

## Round 7 — Callbacks and ordering

### Q7. A callback arrives before the worker response, then arrives twice after a later refund. How is it applied?

??? question "Reveal answer"

    **Verdict:** Authenticate observations, deduplicate envelopes, and apply legal operation-level transitions.

    Verify callback authenticity and match payment reference, merchant/order, amount, currency, and
    operation type. Persist inbox identity with the local effect. Different callback IDs can still
    describe the same capture, so enforce business operation uniqueness too.

    A late CAPTURED observation must not overwrite REFUNDED with a simplistic status assignment.
    Record capture and refund as distinct facts/operations and derive the correct lifecycle. Provider
    sequence numbers or query reconciliation may be needed when callbacks disagree.

    **Trade-off:** retaining observations improves audit and repair but requires privacy-aware
    storage and schema evolution. Acknowledging a callback before durable recording risks permanent
    loss if the provider stops retrying; rejecting every duplicate may cause unnecessary retry traffic.

    **Interview-ready answer:** “Callback order is not financial order. I retain verified evidence,
    deduplicate at envelope and operation boundaries, and converge through guarded state/accounting
    rules. Late evidence cannot erase a later valid refund.”

    **Follow-up:** Which evidence distinguishes a duplicate callback from a genuine correction?

## Round 8 — Ledger design

### Q8. What does balanced double-entry accounting prove, and what does it not prove?

??? question "Reveal answer"

    **Verdict:** Balanced postings preserve accounting structure, not the truth of an external payment by themselves.

    A captured 100-unit payment might debit provider receivable and credit merchant payable under
    the chosen accounting model. A reversal appends compensating postings; it does not edit history.
    Balance entries per currency and record explicit FX/fee/rounding legs rather than adding unlike units.

    Enforce one authorized posting protocol with a unique logical transaction identity. A simple
    row CHECK cannot generally validate the sum of arbitrary sibling entries; use a controlled
    transaction/validation mechanism appropriate to the database. Capture result, ledger entries,
    and outbox commit locally together where owned by that database.

    Materialized balances accelerate reads but must reconcile against entries. Spending authority
    also needs concurrency/reservation rules; a stale cached balance is insufficient.

    **Interview-ready answer:** “The ledger provides immutable, balanced, traceable local facts.
    It still needs authorization, correct account mapping, external reconciliation, and concurrency
    control. A balanced duplicate charge is still a financial defect.”

    **Follow-up:** How would you rebuild a corrupted balance projection without reposting money movements?

## Round 9 — Concurrent partial refunds

### Q9. Two operators simultaneously refund 70 against a captured 100. How do you avoid refunding 140?

??? question "Reveal answer"

    **Verdict:** Reserve refundable capacity atomically and give each refund its own durable identity.

    Both requests reading remaining refundable amount = 100 is not protection. Guard the aggregate
    so completed plus in-flight reserved refunds never exceed captured refundable value. Commit the
    refund intent and reserved amount together; provider execution happens later outside the lock.

    | Outcome | Capacity/accounting behavior |
    |---|---|
    | Verified refund success | Move reserved refund into completed accounting |
    | Verified failure | Release its reserved refundable amount once |
    | Unknown refund | Retain ownership while reconciling |
    | Duplicate operator retry | Return the same refund operation |

    A timeout cannot justify releasing the refund reservation and sending another refund with a
    new key. Authorization and approval thresholds apply independently of concurrency safety.

    **Interview-ready answer:** “I serialize or conditionally version the refundable budget, not the
    entire provider wait. Each refund reserves capacity durably, and unknown outcomes retain that
    reservation until evidence supports success or release.”

    **Follow-up:** How do chargebacks arriving during refunds affect the accounting model?

## Round 10 — Business cancellation race

### Q10. The order expires while capture is uncertain. Should payment immediately become FAILED?

??? question "Reveal answer"

    **Verdict:** Business cancellation and external financial outcome are independent facts.

    ```mermaid
    stateDiagram-v2
        PROCESSING --> CAPTURED: Verified success
        PROCESSING --> DECLINED: Verified decline
        PROCESSING --> UNKNOWN: Ambiguous outcome
        UNKNOWN --> CAPTURED: Reconciled success
        UNKNOWN --> DECLINED: Reconciled definitive failure
        CAPTURED --> REFUND_PENDING: Business policy requires compensation
        REFUND_PENDING --> REFUNDED: Verified refund
    ```

    Persist the cancellation request and resolve the race under the order/payment contract. A late
    capture may require refund; do not silently discard the successful financial observation because
    the order transition lost. Stock reallocation must not depend on an unverified assumption that
    money movement failed. Across services, compensation commands and acknowledgments are durable.

    **Interview-ready answer:** “I keep order eligibility separate from payment truth. Cancellation
    does not turn an unknown charge into a decline. Reconciliation determines the financial outcome;
    policy then creates a distinct idempotent compensation operation if needed.”

    **Follow-up:** Can the platform safely reopen stock while a payment remains unresolved?

## Round 11 — Overload protection

### Q11. Slow providers cause retry storms and growing UNKNOWN work. How do you recover?

??? question "Reveal answer"

    **Verdict:** Reduce initiation while preserving capacity to resolve already accepted money movement.

    Use per-provider bulkheads, total deadlines, bounded queues, and classified retries with one
    owner and jitter. Circuit breakers reduce futile calls; they do not prevent duplicate charges.
    Reserve quotas and workers for verified callbacks, status queries, settlement ingestion, and
    reconciliation. Track attempts per logical payment, not just request throughput.

    If a provider permits 300 calls/s, 300 new charges/s plus 100 status checks/s is already over
    budget. A priority/reserved-share policy is needed; unlimited priority can starve initiation
    forever, so define a controlled recovery mode and exit criteria.

    **Interview-ready answer:** “I would bound amplification and isolate providers, then measure
    unknown-state age and net drain. More pods or longer timeouts do not fix the shared provider
    limit. Recovery is complete only when prior financial uncertainty is accounted for.”

    **Follow-up:** Who owns a payment that exhausts its automated reconciliation budget?

## Round 12 — Settlement mismatch

### Q12. Local captures total 1 million, but the provider settlement differs. What is the investigation?

??? question "Reveal answer"

    **Verdict:** Reconcile itemized financial evidence, not only aggregate totals.

    | Check | Possible explanation |
    |---|---|
    | Currency and period boundary | Time-zone, cutoff, delayed settlement |
    | Fees, reversals, reserves | Legitimate accounting categories |
    | Provider transaction references | Missing, duplicated, or unmapped operations |
    | Capture/refund status | Late evidence or unknown operation |
    | Ledger uniqueness and posting | Local duplicate or mapping defect |

    Ingest immutable settlement evidence with deduplicated file/report identity, verify completeness,
    and link discrepancies to operations. Do not automatically create correcting charges to make
    totals match. Corrections need approved, traceable accounting and business actions.

    **Trade-off:** daily reconciliation is cheaper but increases detection delay; near-real-time
    checks supplement rather than replace final settlement reconciliation.

    **Interview-ready answer:** “I compare like periods and currencies, then match provider references
    to local operations and ledger facts. I classify legitimate differences separately from defects,
    preserve evidence, and use controlled repair rather than mutating history to hide a mismatch.”

    **Follow-up:** Can equal totals conceal one missing payment and one duplicate payment?

## Round 13 — Security and audit

### Q13. Which controls protect money and sensitive data beyond authenticating the caller?

??? question "Reveal answer"

    **Verdict:** Authorization, minimized data, provider verification, and audited operational authority are separate controls.

    Enforce merchant/customer scope, allowed amounts/currencies, role-based refund limits, and
    approval for sensitive repairs. Use provider tokenization and avoid handling raw card data where
    possible; real compliance scope requires specialist review, not an interview checklist claim.

    Verify callbacks and protect against replay under the provider's contract. Keep provider keys,
    tokens, and personal payloads out of logs/metrics. Financial audit must remain available even
    when traces are sampled; access, retention, encryption, and tamper evidence need explicit policy.
    Secret rotation and callback-key overlap must not break recovery for in-flight operations.

    **Interview-ready answer:** “I minimize sensitive data, authorize every business transition, and
    audit operational actions independently of debug telemetry. Provider authenticity and financial
    identity are verified before accepting an observation or posting money.”

    **Follow-up:** Who can issue a manual refund, and how do you detect unauthorized use?

## Round 14 — Sharding and regional recovery

### Q14. How can payments scale across shards or regions without duplicating financial authority?

??? question "Reveal answer"

    **Verdict:** Partition ownership deliberately and preserve acknowledged operations through promotion.

    Keep commonly atomic intent/ledger work local where possible. Cross-account or cross-shard
    transfers require a defined protocol and recovery; two independent postings do not become one
    atomic transfer because they share an ID. A hot merchant may remain hot under merchant sharding.

    Assign home ownership or use a qualified strongly consistent store. Promotion must fence old
    writers and recover in-flight provider operations under their original identities. A replicated
    database that loses an acknowledged payment intent can lose the very key needed to prevent
    another charge. Provider evidence may be needed to reconstruct uncertain operations before resuming.

    Schema/event changes use compatible readers/writers and immutable ledger meaning; do not reinterpret
    historical postings when introducing new account types or fees.

    **Interview-ready answer:** “I scale around financial ownership, not arbitrary hash buckets.
    Regional failover includes ledger durability and external-operation reconciliation. Traffic
    routing alone does not establish safe authority to initiate payments.”

    **Follow-up:** What is the safe RPO for a provider charge whose local intent was already acknowledged?

## Round 15 — Evidence and final defense

### Q15. Which tests support your guarantees, and how do you explain the design in two minutes?

??? question "Reveal answer"

    **Verdict:** Test every commit gap and reconcile local facts against independent financial evidence.

    | Fault / race | Required evidence |
    |---|---|
    | Same key concurrently; mismatched payload | One operation; mismatch rejected |
    | Provider success then worker death | Original identity recovered; no new logical charge |
    | Callback/response reorder and duplicates | One legal effect; late observations preserved |
    | Concurrent refunds | Completed plus reserved refunds stay within policy |
    | Cancellation/unknown outcome | No fabricated decline; compensation recoverable |
    | Settlement replay and restore | Balanced ledger and accounted provider operations |

    Use approved sandboxes and controlled faults; do not issue real charges as an unapproved test.
    Measure pending/unknown age, provider quota, retry amplification, reconciliation mismatches,
    refund backlog, and accounting invariants with bounded metric labels.

    **Interview-ready answer:** “I persist authorized intent and stable identities before external
    calls. Short local transactions commit outcome, ledger, and next intent; callbacks and retries
    converge through guarded deduplication. Unknown money movement stays recoverable. Refunds reserve
    a bounded budget, settlement independently verifies results, and failover preserves ownership.
    I accept pending states and controlled refusal rather than guessing about money.”

    **Follow-up:** Which guarantees are local, and which depend on the provider's actual contract?

## Review gate

Defend capture versus settlement, the external commit gap, callback ordering, balanced postings,
refund reservations, and regional recovery before starting the order case.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [API idempotency](../rest-api/practice.md)
- [Outbox and inbox mechanisms](../distributed-data-patterns/practice.md)
- [Next: Orders](practice-orders.md)
