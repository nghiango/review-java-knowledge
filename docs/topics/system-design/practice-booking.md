# Case 8 — Hotel / Ticket Booking: Senior Interview Walkthrough

Finish [File Processing](practice-files.md). Complete this booking lifecycle before returning to
the [mixed question bank](questions.md). Explain each prompt before revealing its reasoning and
spoken answer. These are design proposals and illustrative loads, not tested lab guarantees.

## Scenario and progression

| Dimension | Working assumption |
|---|---|
| Assigned-seat product | Event/seat is exclusive; group holds should be all-or-nothing |
| Hotel variant | Room-type capacity across every occupied night; exact room assignment may come later |
| Demand | Illustrative 20k buyers at launch; concentrated seat/property contention |
| Hold | Ten-minute authority-defined deadline; bounded authorized renewal policy |
| Payment | External provider; unknown outcomes and late observations require reconciliation |
| Invariants | No duplicate confirmed seat; room allocations stay within declared capacity policy |

Progress through scope → capacity → model → lifecycle → concurrency choice → group/date atomicity →
renewal/expiry → payment race → durable timers → live maps → fairness → modification → security →
regions → evidence. Do not silently treat assigned seats and pooled room nights as the same invariant.

## Round 1 — Resource and business contract

### Q1. What must be clarified before claiming the system prevents double booking?

??? question "Reveal answer"

    **Verdict:** Define the whole resource/time invariant, hold promise, and payment cutoff.

    | Clarification | Consequence |
    |---|---|
    | Assigned seat or pooled capacity? | Exclusive row versus quantity/date accounting |
    | Group/itinerary atomicity? | One transaction or explicit partial workflow |
    | What starts/ends the hold? | Authority timestamp and transition rules |
    | Renewals and cancellation? | Fairness, generations, and refund/release policy |
    | Intentional hotel overbooking allowed? | Explicit business capacity, not accidental races |
    | Checkout success point? | Pending payment versus confirmed booking |

    A displayed available seat is not a granted right. Define whether one night of a multi-night
    stay can be accepted alone and whether exact room identity must remain constant throughout.
    Ten minutes is an eligibility rule; background cleanup cannot be assumed to run at that exact instant.

    **Interview-ready answer:** “I would identify every resource and date the promise covers, then
    define hold, confirmation, and expiry semantics. No-double-booking means a specific durable
    allocation invariant, not a claim that the seat map is always fresh.”

    **Follow-up:** Can a hotel sell room-type capacity without promising one specific room for the whole stay?

## Round 2 — Demand and contention sizing

### Q2. How do 20,000 simultaneous buyers affect reads, writes, timers, and downstream capacity?

??? question "Reveal answer"

    **Verdict:** Derive arrivals and hot-resource contention instead of equating users with RPS.

    | Illustrative behavior | Derived work |
    |---|---|
    | 20k viewers poll every two seconds | 10k map requests/s before retries |
    | 20k attempt holds within five seconds | 4k hold attempts/s |
    | Four seats per group attempt | Up to 16k seat checks/updates/s before admission |
    | 6k holds expire over one minute | 100 expiry operations/s before per-resource work |

    These are separate assumptions, not a combined forecast. Measure event/section/seat skew:
    many requests for one front-row group serialize differently from uniform random seats. Payment
    duration determines occupied holds and provider concurrency. Timer/reconciliation backlog matters
    even when browsing remains fast.

    **Interview-ready answer:** “I would model map reads, hot allocation writes, payment concurrency,
    and expiry independently. Bounded admission protects the authority; more app pods cannot create
    parallel capacity for a single contested exclusive seat.”

    **Follow-up:** How does a slow payment provider reduce sellable availability without changing seat count?

## Round 3 — API and durable model

### Q3. Which records establish ownership and survive retries, renewal, and recovery?

??? question "Reveal answer"

    **Verdict:** Resource identity, hold identity, generation, and business operation identity are distinct.

    | Record | Important state |
    |---|---|
    | Seat allocation | Unique event/seat authority, current state/version/hold reference |
    | Room capacity | Property/type/local date, capacity and allocated quantity |
    | Hold and lines | Owner, resource/date set, generation, deadline, legal state |
    | Request/result | Principal/key, fingerprint, recoverable response |
    | Booking/payment operation | Stable references, verified outcome/UNKNOWN |
    | Workflow/inbox/outbox | Timers, deduplication, required next intent |

    APIs search, create/query/renew a hold, initiate/observe payment, confirm, and cancel. Scope
    every mutation to the verified owner. The server establishes price/quote validity; a client-provided
    seat ID or amount does not authorize either ownership or a discount.

    Do not use a permanent unique resource column on all historical holds if it prevents legitimate
    later holds; keep one current allocation authority plus audited history or an equivalent valid model.

    **Interview-ready answer:** “I persist an explicit hold lifecycle over stable resources, with
    durable request identity and guarded generations. History and current exclusivity are modeled
    separately so legitimate reuse is possible without allowing concurrent owners.”

    **Follow-up:** Does retrying the original create-hold request extend its deadline?

## Round 4 — Full booking flow

### Q4. Trace hold, pay, and confirm without locking rows for ten minutes.

??? question "Reveal answer"

    **Verdict:** Persist the hold quickly, release DB locks, then coordinate payment and confirmation durably.

    ```mermaid
    sequenceDiagram
        actor Buyer
        participant API
        participant DB as Booking authority
        participant PSP as Provider
        participant Timer as Durable expiry worker
        Buyer->>API: Hold resources with operation key
        API->>DB: Atomic allocation, hold, result, deadline, outbox
        DB-->>API: Commit hold and authoritative deadline
        API-->>Buyer: Hold identity and status
        API->>PSP: Stable payment identity outside DB transaction
        PSP-->>API: Verified result or unknown
        API->>DB: Guarded booking transition or reconciliation intent
        Timer->>DB: Guarded expiry of eligible same-generation hold
    ```

    The hold row, not a continuously held database lock, preserves business ownership during human
    checkout. Required events/timers are recoverable after process death. Local transaction scope
    includes only co-owned state; a payment provider or separate inventory database needs its own protocol.

    **Interview-ready answer:** “I acquire resources in a bounded transaction and persist the deadline.
    Payment occurs independently with stable identity. Confirmation and expiry compete through the
    same authority; no connection or row lock waits for the customer to finish checkout.”

    **Follow-up:** What proves the booking exists if the confirmation response is lost?

## Round 5 — Concurrency mechanism comparison

### Q5. Optimistic locking, row locks, or Redis TTL: which prevents double booking?

??? question "Reveal answer"

    **Verdict:** The chosen mechanism must enforce the durable resource invariant under the actual failure model.

    | Choice | Benefit | Cost / limitation |
    |---|---|---|
    | Conditional/versioned update | Atomic stale-state rejection | Conflicts and retry policy |
    | Short row locking | Complex local/group decisions | Lock order, wait budget, deadlock handling |
    | Redis TTL admission/cache | Fast provisional filtering | Expiry and failover do not prove durable availability |
    | Disjoint allocation rights | Local throughput and isolation | Ownership transfer and stranded capacity |

    SELECT-then-update without a guard races. A Redis key disappearing is not proof a booked seat
    became available. If Redis is the authority, explain acknowledged-write durability, failover,
    fencing, reconciliation, and recovery rather than relying solely on Lua atomicity.

    **Trade-off:** DB-first allocation plus bounded admission is often the safer baseline. Single-row
    contention is not automatically deadlock; cycles arise from multiple resource acquisition patterns.

    **Interview-ready answer:** “I would choose a short durable authority transaction and qualify
    its contention behavior. Redis can accelerate reads/admission, but every successful hold must
    have a proven durable exclusivity boundary.”

    **Follow-up:** Which store wins if the cache says AVAILABLE while the allocation authority says BOOKED?

## Round 6 — Group seats and room-date atomicity

### Q6. Two buyers request overlapping seat groups or multi-night stays. How do you prevent partial success and deadlock?

??? question "Reveal answer"

    **Verdict:** Enforce the complete requested resource set in one bounded local transaction when all-or-nothing is promised.

    Order seat/date locks consistently, validate every resource, then commit all allocations and
    the hold—or roll back all. A failed late resource check must undo earlier tentative changes.
    Bound group size, lock waiting, and retries with jitter; retry the whole transaction under the
    original operation identity after deadlock/serialization failure.

    | Model | Important invariant |
    |---|---|
    | Assigned group seats | Every specific seat belongs to the same successful hold |
    | Pooled rooms | Quantity fits capacity on every occupied night |
    | Cross-shard itinerary | No assumed local ACID; explicit partial/compensation contract |

    Define check-in-inclusive/check-out-exclusive dates, property timezone, and capacity changes.
    Individually valid nights do not imply an available multi-night continuous stay under every room-assignment policy.

    **Interview-ready answer:** “I coordinate the whole resource set, acquire it in a consistent
    order, and fail atomically when the product promises that. If the set crosses authorities,
    I either change the product contract or design a recoverable distributed reservation workflow.”

    **Follow-up:** What happens to availability while compensation for a partially acquired itinerary is still pending?

## Round 7 — Hold generations and renewals

### Q7. A ten-minute hold is renewed, but its original expiry event arrives afterward. Can it release the seats?

??? question "Reveal answer"

    **Verdict:** Expiry targets a particular hold generation and current deadline, not merely a seat or user ID.

    Renewal verifies ownership, current eligibility, policy limits, and expected generation; it
    durably records the new deadline/version. An old expiry command must match the generation/deadline
    it was created for and recheck authoritative current state before release.

    | Check | Prevents |
    |---|---|
    | Hold identity | Releasing another hold on the same seat |
    | Generation/version | Applying stale timers after renewal |
    | Deadline predicate | Premature release |
    | Legal current state | Releasing already confirmed or already released stock |

    Bound extension count/total duration to prevent indefinite hoarding. Retrying a lost renewal
    response returns the same logical renewal rather than extending again accidentally.

    **Interview-ready answer:** “I make renewal a guarded durable operation and expiry a conditional
    transition against the same authority. Timers may duplicate or be late; they cannot free a
    different generation simply because a local ten-minute clock fired.”

    **Follow-up:** What if the same person obtains a second hold after the first expired?

## Round 8 — Payment versus expiration

### Q8. Payment succeeds just before expiry, but the callback arrives after another buyer gets the seat. What now?

??? question "Reveal answer"

    **Verdict:** Distinguish payment truth, booking eligibility, and compensation; never overwrite the new owner's allocation.

    Decide which verified provider timestamp/evidence determines eligibility and how unknown outcomes
    affect expiry. Confirmation and release use guarded legal transitions; one current hold generation
    can be confirmed or released, not both. Persist financial observations even when confirmation fails.

    | Outcome | Resolution |
    |---|---|
    | Valid hold and verified eligible success | Confirm once |
    | Provider outcome uncertain at deadline | Review/hold or release under explicit risk policy |
    | Payment succeeds after legal release | Defined reacquire/refund/manual resolution |
    | Duplicate expiry or callback | No repeated stock release, booking, or refund |

    If payment and seat authority are separate services, durable command/acknowledgment and
    reconciliation replace a fictional cross-service local transaction.

    **Interview-ready answer:** “A late callback is not automatically a late payment. I record
    evidence and apply the agreed deadline policy, but an already reallocated seat cannot simply
    be stolen back. Refund or alternative booking is a separate idempotent business operation.”

    **Follow-up:** What must remain reserved when the provider cannot prove whether a charge succeeded?

## Round 9 — Durable expiry and outage recovery

### Q9. Expiry workers are down for an hour. How do holds recover safely and how do users behave?

??? question "Reveal answer"

    **Verdict:** Persist deadlines and recheck them at the authority; timers schedule cleanup rather than define truth.

    Use an indexed due-hold scan in bounded batches or a qualified durable timer service. Claim work
    safely, conditionally release the same generation, and commit release accounting plus next intent.
    Duplicate timers must be harmless. Redis expiry pub/sub alone is not durable delivery; ordinary
    Kafka records do not natively provide arbitrary per-record scheduled delivery.

    A logically expired hold may still occupy physical counters until reconciled. Reclaim only with
    an atomic protocol that handles payment uncertainty and other contenders. Do not separately mark
    available in cache while the authority still grants an old confirmation.

    **Trade-off:** conservative cleanup can strand seats temporarily, but unsafe release can double-book.
    Scale recovery with controlled scan/drain capacity instead of overwhelming the primary.

    **Interview-ready answer:** “I would recover due work from durable deadlines, not remembered
    in-process timers. Confirmation checks authority state; release is once-only and auditable.
    Timer backlog and stuck payment reviews are operational signals, not hidden implementation details.”

    **Follow-up:** Can a new hold request safely reclaim an expired resource before the periodic reaper reaches it?

## Round 10 — Live availability and cache correctness

### Q10. Should every seat-map change be pushed instantly to all 20,000 viewers?

??? question "Reveal answer"

    **Verdict:** UI freshness improves experience but cannot replace authoritative allocation checks.

    | Delivery model | Benefit | Cost |
    |---|---|---|
    | Bounded polling | Simple recovery and clients | Repeated reads and update delay |
    | SSE / push stream | Efficient one-way updates | Connections, fan-out, reconnect/resume |
    | WebSockets | Bidirectional interaction if needed | More connection/protocol operations |

    Use section/event versions, incremental updates, bounded buffers, and snapshot/resume behavior.
    A reconnect gap must trigger refresh rather than leave a permanently incorrect map. Do not
    broadcast every individual mutation to every user without estimating bandwidth and client work.
    Caches expose “as of” freshness; conflicts from stale maps receive clear responses.

    Never acknowledge a hold from cached availability. Authorize private availability/pricing and
    avoid leaking another buyer's identity in public seat updates.

    **Interview-ready answer:** “I choose update delivery from the UX latency budget and fan-out
    cost. The map is advisory; every hold checks the authority. Versioned snapshots and reconnect
    recovery keep UI state coherent without pretending all viewers see instantaneous truth.”

    **Follow-up:** What happens when one slow client's update buffer fills during a launch burst?

## Round 11 — Fairness and scarce holds

### Q11. Bots create many identities and continuously renew holds. What protects real buyers?

??? question "Reveal answer"

    **Verdict:** Scarce inventory needs bounded admission and business eligibility controls, not only request throttling.

    Apply authenticated hold quotas, extension limits, scoped signed admission tokens where useful,
    risk/abuse checks, and clear waiting-room semantics. IP-only restrictions penalize NAT users and
    do not stop distributed bots; account limits alone do not stop account farms. Avoid claiming
    globally first-come order without defining an actual ordering authority.

    Separate available seat display from the number of admitted checkout attempts. Protect the
    database with bounded concurrency, retain capacity for existing holds/payment callbacks, and
    distinguish sold-out conflicts from overload. Do not let privileged retry paths bypass all ceilings.

    **Trade-off:** stronger anti-bot checks add friction and privacy obligations. Reclaiming apparent
    hoarded holds must respect actual ownership, payment outcome, and cancellation policy.

    **Interview-ready answer:** “I bound hold occupancy and renewal, then combine fair admission
    with abuse-aware verified identity. Allocation remains authoritative; queue position or a token
    is not a seat guarantee unless that stronger promise is actually enforced.”

    **Follow-up:** How do you measure fairness without publishing personal buyer identities?

## Round 12 — Modification and cancellation

### Q12. A customer changes dates or swaps seats. Can you release the old booking before acquiring the new one?

??? question "Reveal answer"

    **Verdict:** Define whether modification is atomic, conditional, or an explicit multi-step trade-off.

    Releasing first can lose the original resource if the new allocation fails. Acquiring first
    temporarily consumes extra capacity and may require a bounded modification hold. Within one
    authority, a short transaction can validate/transition the old and new resource sets consistently;
    across authorities, use an explicit recoverable workflow and customer-visible partial states.

    | Concern | Required rule |
    |---|---|
    | Price difference | Authorized quote, capture/refund/void operation |
    | Cancellation deadline | Business policy evaluated at the agreed authority/time |
    | Retry after response loss | Stable modification identity and result |
    | Partial external payment | Unknown-outcome reconciliation, not blind rollback |

    Preserve the original booking history and trace the replacement; do not mutate identifiers so
    old callbacks apply to the wrong resource generation.

    **Interview-ready answer:** “I would protect the customer's existing right until the agreed
    replacement protocol permits release. Modification has its own identity, financial adjustments,
    and recovery state; it is not two unrelated delete-and-insert calls.”

    **Follow-up:** What should the user see if the price adjustment succeeds but replacement confirmation is uncertain?

## Round 13 — Authorization, audit, and rollout

### Q13. How do you prevent unauthorized holds/refunds and evolve booking states safely?

??? question "Reveal answer"

    **Verdict:** Secure ownership and operational authority while preserving old/new lifecycle interpretation.

    Authorize resource/tenant/customer scope for hold, renewal, cancellation, modification, download,
    and operator repair. Validate provider callbacks and expected payment amount/currency/reference;
    do not trust a client's PAID flag. Audit overrides and financial actions under least privilege.

    Schema/event changes need compatible readers/writers and historical replay support. A new hold
    state unknown to old code must not be interpreted as available. Backfill from authoritative
    evidence; avoid defaults that mark unknown historical bookings unconfirmed or unpaid. Tighten
    constraints only after active writers and rollback paths comply.

    Keep personal data in protected audit rather than metric labels. Deleting customer data must
    respect applicable financial/booking retention and deduplication requirements without inventing policy.

    **Interview-ready answer:** “I would verify ownership on every transition and audit repairs.
    Deployment compatibility covers legal state interpretation, not only columns. A healthy new
    binary is insufficient if old workers can still release or confirm under obsolete rules.”

    **Follow-up:** Can an old timer consumer safely process a hold created under a new renewal policy?

## Round 14 — Partitioning and regional recovery

### Q14. How can bookings scale across properties/events and fail over without two owners?

??? question "Reveal answer"

    **Verdict:** Keep the invariant near one resource authority and transfer write rights explicitly.

    Event/property ownership often keeps seat groups or room-date transactions local. A hot event
    remains hot under event hashing; section/resource partitioning changes group transaction scope.
    Cross-property itineraries need a separate multi-authority contract, not assumed local atomicity.

    Regional replicas/caches improve search, but confirmation needs current authority. Promotion
    must preserve acknowledged allocation state and fence old writers. A DNS change or expired
    coordinator lease does not prove an isolated regional database stopped selling seats.

    | Recovery state | Action |
    |---|---|
    | Ownership and committed allocation known | Resume under fenced new authority |
    | Old owner may still write | Do not duplicate allocation rights |
    | Provider operation uncertain | Reconcile original identity before new financial action |
    | Old region rejoins | Validate ownership and replay safely before writes |

    **Interview-ready answer:** “I partition by resource invariants and keep region writes under
    enforceable ownership. Failure can temporarily reduce availability; uncertain exclusive seats
    are not safely recreated from stale replicated maps.”

    **Follow-up:** What RPO is compatible with promising no loss of acknowledged seat confirmations?

## Round 15 — Evidence and final defense

### Q15. What demonstrates no double booking across the whole lifecycle?

??? question "Reveal answer"

    **Verdict:** Check resource accounting under concurrent histories, external ambiguity, and recovery—not only happy-path checkout.

    | Test | Evidence |
    |---|---|
    | Two owners / overlapping groups | At most one current owner; atomic group result |
    | Multi-night capacity and modifications | Every date respects explicit quantity policy |
    | Renewal/expiry/payment races | Correct generation and one legal release/confirmation |
    | Lost responses, duplicate timers/callbacks | Stable operation result and once-only effects |
    | Expiry outage/cache reconnect | Honest availability and safe bounded recovery |
    | Rollout/failover/stale-region rejoin | Authority and acknowledged-state guarantees maintained |

    Observe hold age/expiry lag, conflicts, payment unknowns, refund backlog, map drift, lock/pool
    waits, and invariant violations. Qualify real database semantics and provider sandbox contracts
    in controlled tests; a UI test or mocked success cannot establish distributed correctness.

    **Interview-ready answer:** “I define resource/time invariants, atomically create durable
    versioned holds, and coordinate payment outside database locks. Guarded confirmation/expiry,
    durable timers, and stable financial identities resolve races. Maps stay advisory, modifications
    preserve explicit rights, and regions never duplicate ownership. I accept bounded delay or
    refusal before confirming the same resource twice.”

    **Follow-up:** Which failure assumption would break your strongest no-double-booking claim?

## Review gate

Defend exact seats versus room capacity, group/date atomicity, renewal generations, payment truth,
durable expiry, map staleness, modifications, and regional authority before mixed interview practice.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Payment walkthrough](practice-payments.md)
- [Database concurrency mechanisms](../database-sql/practice.md)
- [Flash-sale allocation and ownership](practice-flash-sale.md)
