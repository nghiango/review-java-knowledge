# Case 5 — Flash Sale Inventory: Senior Interview Walkthrough

Complete [Notifications](practice-notifications.md) first. Stay with this system through its
failure, recovery, migration, and regional-ownership rounds before moving to
[Distributed Rate Limiter](practice-rate-limiter.md).

This expanded case follows the depth and progression of the supplied flash-sale interview guide.
It is a **design exercise**, not a claim that this lab implements or has benchmarked this entire
platform. Implementation examples are included from existing lab source; additional protocols
are described as designs. Framework mechanisms remain canonical in their linked topics.

## Scenario and interview progression

| Requirement | Working assumption |
|---|---|
| Audience | 10 million registered users; 500,000 concurrent sale users |
| Campaign | 10 minutes; 1,000 products; one especially popular product has 100 units |
| Purchase policy | One unit per authenticated user per product per sale |
| Inventory | Strict no overselling; reserve before payment |
| Payment | Five-minute window; unknown outcomes require reconciliation |
| Response | Asynchronous confirmation allowed; initial acknowledgment p95 below 200 ms |
| Availability | Illustrative campaign target 99.9%; correctness can require rejecting new work |
| Baseline peak | 100,000 purchase attempts/s, subject to explicit burst assumptions |
| Stack | Java 21 / Spring Boot; PostgreSQL, Redis, Kafka; AWS-style deployment |

The later **100k business events/s**, **one million attempts/s**, and **two-region** challenges
are separate escalations. Do not combine them into one unexplained estimate. Attempt traffic is
not reservation throughput, and acknowledgment is not completed payment.

### How to practice

1. Answer each visible interviewer prompt before opening the explanation.
2. Work through the concrete history, not just the named pattern.
3. Give the interview-ready answer in your own words.
4. Defend the trade-off and answer the follow-up before continuing.

The rounds cover scope → capacity → contract/model → architecture → atomic allocation → crash
recovery → payment races → duplicate delivery → quota grants → failover → end-to-end identity →
outbox scale → CDC recovery → migrations → mixed versions → pool saturation → retry storms →
extreme admission → regional ownership → disaster recovery → operational validation.

## Round 1 — Clarify the business promise

### Q1. Before drawing components, what would you ask the business?

??? question "Reveal answer"

    **Verdict:** Define what the customer is promised and which resource owns that promise.

    **Guided reasoning**

    | Clarification | Why it changes the design |
    |---|---|
    | One unit per user, product, or entire campaign? | Changes the durable uniqueness rule |
    | Reserve before payment? | Requires explicit held stock and release transitions |
    | Which payment completion timestamp matters? | Determines late-callback handling |
    | Can the response mean pending? | Allows durable asynchronous processing |
    | What does first-come-first-served mean? | Requires an ordering boundary, not an informal fairness claim |
    | How long is the peak? | Determines capacity, backlog, and cost |
    | What happens during dependency failure? | Determines whether to queue, reject, or degrade |

    Distinguish **available**, **reserved**, and **sold**. All 100 units can be reserved while
    zero purchases have completed. A browse response may be stale; a reservation decision may not
    allocate the same unit twice. Define whether failed attempts can try again and whether released
    stock is re-admitted during the campaign.

    **Common incomplete answer:** “Use Redis and Kafka for high throughput.” Neither identifies
    the allocation authority, purchase-limit rule, or response contract.

    **Interview-ready answer:** “I would clarify purchase limits, reservation and payment deadlines,
    acceptable pending states, peak duration, and fairness. I would separate initial acknowledgment
    latency from checkout completion. Since overselling is prohibited, I would explicitly accept
    bounded rejection or delay when allocation authority is unavailable.”

    **Follow-up:** Can a global arrival order be established when requests reach different gateways?

## Round 2 — Calculate the load instead of guessing

### Q2. How do you size 100,000 attempts/s for a product with only 100 units?

??? question "Reveal answer"

    **Verdict:** Size the offered traffic, but deliberately bound authoritative reservation work.

    **Calculations**

    | Estimate | Calculation | Qualification |
    |---|---|---|
    | Initial burst | 500,000 users / 5 seconds = 100,000 attempts/s | Excludes retries and browse calls |
    | Entire-sale upper bound | 100,000 × 600 = 60 million attempts | Not a forecast of sustained demand |
    | Mean in-flight work | 100,000/s × 0.2 s = 20,000 | Assumes **mean** latency 200 ms, not p95 |
    | Theoretical instances | 100,000 / measured 2,000 RPS = 50 | Per-instance figure is hypothetical |
    | At 70% utilization | ceil(100,000 / (2,000 × 0.7)) = 72 | Still needs downstream/failure qualification |

    A correct conditional stock update still creates contention when every attempt reaches the
    same row. Most requests should be rejected, deduplicated, or held outside the inventory
    transaction. CPU, network, authentication, cache hot keys, and queue ingestion also have limits.

    Across failed-payment and expiry cycles, more than 100 reservation attempts can succeed over
    time. The stock invariant concerns current allocation plus completed sales, not a lifetime
    cap of 100 requests. Virtual threads reduce waiting cost; they do not create DB capacity.

    **Interview-ready answer:** “I would qualify edge and application traffic separately from
    admitted work. For 100 units, sending every purchase attempt to PostgreSQL is the wrong goal.
    I would use measured per-instance capacity and mean latency for estimates, then verify hot-key,
    connection, broker, and recovery headroom under representative skew.”

    **Follow-up:** Which assumption changes if payment becomes eight seconds slower?

## Round 3 — Define the contract and durable model

### Q3. Which APIs, identities, records, and constraints are necessary?

??? question "Reveal answer"

    **Verdict:** Separate permission to try, durable acceptance, confirmed reservation, and payment.

    **API decisions**

    - Purchase submission carries authenticated identity, sale/product, quantity, and a stable
      scoped idempotency key. The server validates eligibility and determines the commercial quote.
    - A durable pending submission can return **202** with an operation-status resource. A **201**
      reservation response means that reservation actually committed, not just that Redis allowed it.
    - Status, payment initiation, cancellation, and any renewal require ownership authorization.
      Define whether an in-progress retry returns pending or a documented conflict.

    | Record | Important state and constraint |
    |---|---|
    | Inventory | Sale/product; available, reserved, sold; nonnegative bounds |
    | Purchase operation | Principal/key uniqueness; fingerprint; durable result/status |
    | Reservation | Stable ID; sale/user/product; state, version, deadline |
    | Purchase eligibility | Enforces the actual one-unit business policy across different keys |
    | Order | Unique reservation identity under the one-order-per-reservation assumption |
    | Payment operation | Stable logical identity and provider reference; explicit UNKNOWN |
    | Inbox / outbox | Consumer/event identity; durable next-step intent |

    A permanent uniqueness rule on all reservation history may wrongly prevent retry after an
    eligible expiry. Choose whether the policy means one successful purchase or one attempt, and
    model its lifecycle explicitly. Request-key uniqueness is not the same business restriction.

    **Interview-ready answer:** “I would persist operation identity, reservation ownership, expiry,
    and legal state transitions. Constraints arbitrate concurrent duplicates. The response states
    whether work is pending or inventory is actually reserved, and every lookup is authorized
    against the same principal scope.”

    **Follow-up:** What happens if the same key is reused with another product or quantity?

## Round 4 — Trace the whole purchase

### Q4. Explain Buy Now through payment and order confirmation without hiding commit boundaries.

??? question "Reveal answer"

    **Verdict:** Use Redis for provisional admission and PostgreSQL for durable inventory ownership.

    ```mermaid
    sequenceDiagram
        actor Buyer
        participant Edge as Edge and waiting room
        participant API as Purchase API
        participant DB as Inventory authority
        participant Relay as Outbox relay or CDC
        participant Kafka
        participant Order as Order workflow
        participant PSP as Payment provider
        Buyer->>Edge: Buy with stable operation identity
        Edge->>API: Bounded authenticated attempt
        API->>DB: Atomic operation, allocation, reservation, outbox
        DB-->>API: Commit durable reservation
        API-->>Buyer: Reservation or durable pending contract
        Relay->>DB: Recover committed event intent
        Relay->>Kafka: Publish with stable event identity
        Kafka->>Order: Reservation created; duplicates possible
        Order->>Order: Commit inbox and order transition
        Order->>PSP: Stable payment operation; no open DB transaction
        PSP-->>Order: Verified result or unknown outcome
        Order->>DB: Durable confirmation, reconciliation, or release workflow
    ```

    The final arrow is a workflow, **not** one local transaction spanning independent databases.
    Each owner commits its transition plus required next intent locally. Inventory releases use
    an idempotent command and acknowledgment when another service owns the stock.

    **Alternative:** Put admitted commands into Kafka before allocation. That can decouple
    ingress, but the initial response means durable command acceptance, not reserved stock. Do
    not mix these two contracts or enqueue all rejected traffic merely to claim throughput.

    **Trade-off:** A modular monolith can keep more state atomic and is a valid starting point.
    Separate services justify extra reconciliation only when ownership or scaling benefits warrant it.

    **Interview-ready answer:** “The authority commits allocation, reservation, identity, and outbox
    together. The relay and consumers tolerate redelivery. Payment runs outside database transactions,
    and unknown results remain recoverable. Redis improves admission speed but cannot establish
    durable stock ownership by itself.”

    **Follow-up:** What happens if the customer disconnects after the reservation commits?

## Round 5 — Atomic inventory and Redis comparison

### Q5. Why is conditional allocation necessary, and what does a Lua script actually guarantee?

??? question "Reveal answer"

    **Verdict:** Atomicity within one store is not atomicity or durable ownership across stores.

    **Concrete concurrency history:** Buyers A and B both read available stock = 1. Both decide
    they may buy. Unconditional later writes can allocate twice or lose an update. The authority
    must combine checking positive availability and changing the counters in **one guarded write**.
    Inspect the affected-row count; commit its reservation and outbox in the same transaction.

    | Choice | Strength | Limitation |
    |---|---|---|
    | Read then unconditional write | Simple code | Races without stronger coordination |
    | Conditional SQL update | Atomic stock predicate | Hot row still serializes writes |
    | Short pessimistic lock | Supports complex serialized decisions | Lock scope/order and waiting must be bounded |
    | Optimistic version update | Detects stale changes | High contention can create retry amplification |
    | Redis Lua admission | Atomic checks/mutations on its execution boundary | Failover durability and DB/Kafka handoff remain separate |
    | Disjoint stock budgets | Local allocation scales | Ownership, transfer, and stranded capacity become protocols |

    A real provisional Lua operation needs identity, duplicate handling, quantity validation,
    eligibility checks, and expiry semantics—not just decrement. Multi-key cluster execution
    requires compatible key placement. A database transaction also depends on the database's
    acknowledged-commit durability and promotion policy.

    **Interview-ready answer:** “I would use a conditional database allocation as the final guard.
    Lua can reject excess demand atomically on a Redis primary, but that does not prove persistence
    across failover or publication to Kafka. I would benchmark contention before introducing more
    complicated stock partitioning.”

    **Follow-up:** What prevents a stock decrement when a duplicate reservation insert loses a race?

## Round 6 — The Redis-to-durable-state crash gap

### Q6. Redis decreases stock, then the application crashes before the DB commit or Kafka send. What recovers?

??? question "Reveal answer"

    **Verdict:** An outbox protects committed database intent, not an earlier independent Redis change.

    ```mermaid
    flowchart LR
        A[Redis provisional decrement] --> B[Application crash]
        B --> C[No durable reservation or publication intent]
        D[Short database transaction] --> E[Operation plus stock plus reservation plus outbox]
        E --> F[Commit]
        F --> G[Recoverable publication]
    ```

    | Failure point | Required recovery |
    |---|---|
    | Before durable transaction commits | Roll back allocation; retry the same logical operation |
    | Unique identity insert fails after tentative allocation | Roll back the complete transaction; retrieve existing result in a valid new scope |
    | Commit succeeds, HTTP response lost | Return stored operation/reservation on retry |
    | Commit succeeds, publication not attempted | Relay recovers the outbox |
    | Publish succeeds, relay checkpoint lost | Re-send is possible; consumer deduplicates |
    | Redis-only provisional claim remains | Reconcile its identity against durable operation state |

    TTL expiry does not prove a provisional claim may safely be incremented back. Reconciliation
    must account for a concurrent or uncertain DB commit. It must not resurrect expired/released
    ownership or double-release stock. Prefer an advisory design whose temporary under-admission
    can expire conservatively instead of requiring a second authoritative stock ledger.

    **Common incomplete answer:** “Compensate in the catch block.” The process can die before
    executing it, and a timeout may hide a successful commit. Compensation needs durable ownership
    and idempotency too.

    **Interview-ready answer:** “I separate provisional admission from durable allocation. Client
    retries find the same operation; committed intent is recovered through the outbox. Redis-only
    claims have identities and a conservative reconciliation protocol. The outbox does not magically
    include the Redis mutation.”

    **Follow-up:** Who owns reconciliation if the original pod never restarts?

## Round 7 — Payment versus expiry

### Q7. A successful payment callback and expiry worker both read PENDING at the deadline. Who wins?

??? question "Reveal answer"

    **Verdict:** Competing decisions need a guarded transition and an explicit ambiguous-payment policy.

    ```mermaid
    stateDiagram-v2
        PENDING_PAYMENT --> PAID: Verified eligible payment; conditional transition wins
        PENDING_PAYMENT --> PAYMENT_REVIEW: Outcome uncertain near deadline
        PENDING_PAYMENT --> CANCELLED: Eligible expiry; conditional transition wins
        PAYMENT_REVIEW --> PAID: Reconciliation confirms eligible success
        PAYMENT_REVIEW --> CANCELLED: Policy permits release after resolution
        CANCELLED --> REFUND_PENDING: Verified late success
        REFUND_PENDING --> REFUNDED: Idempotent compensation
    ```

    The transition checks current state, reservation generation, payment identity, amount/currency,
    and deadline policy. Only a successful cancellation/release transition can make its stock
    available again. Store payment observations even when the order cannot enter PAID; a zero-row
    update may mean a serious conflict rather than a harmless duplicate.

    **Delayed is not late:** payment can complete at 10:04:59 and its callback arrive at 10:05:02.
    Decide which verified timestamp and provider evidence establish eligibility. If cancellation
    already won, resolve the charge through policy and durable compensation—not by overwriting
    another customer's newly allocated stock.

    | Mechanism | Use | Cost |
    |---|---|---|
    | Conditional transition | Small known state changes | Explicit predicates and row-count handling |
    | Optimistic version | Detect stale aggregate decisions | Bounded full-transaction retry |
    | Row lock | Complex local decisions | Short lock scope, consistent acquisition order |

    **Interview-ready answer:** “Transaction annotations alone do not resolve the race. I conditionally
    transition current state and release the reservation exactly once in the local transaction where
    possible. Unknown payments enter review. Late success after cancellation requires an idempotent
    refund or another explicitly defined resolution.”

    **Follow-up:** What if the refund succeeds but its response is lost?

## Round 8 — Kafka redelivery and business uniqueness

### Q8. The order consumer commits, crashes before offset commit, and receives the event again. How do effects stay safe?

??? question "Reveal answer"

    **Verdict:** Commit consumer deduplication with the business effect, then acknowledge; also enforce business identity.

    **Guided reasoning:** An inbox keyed by logical consumer and event ID excludes repeated delivery
    of one envelope. Its insert and business transition must share the same DB transaction; rollback
    must roll back both. The listener acknowledges only after the service commit actually completes.

    **Example from the lab — local transactional composition, not a complete flash-sale service:**

    ??? example "Example"

        ```java
        --8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/inbox/SafeInventoryConsumer.java"
        ```

    This example demonstrates the inbox/effect boundary. It does **not** supply all sale-level
    business constraints, payment semantics, or the Kafka listener configuration required here.

    | Duplicate source | Additional protection |
    |---|---|
    | Same event delivered twice | Consumer-scoped inbox identity |
    | Different event IDs describe one reservation | Unique order/reservation business identity |
    | Repeated confirmation | Legal conditional transition; outbox only for a real transition |
    | Repeated shipping request | Stable fulfillment/provider operation identity |
    | Replay after dedup retention expires | Defined replay horizon and business uniqueness |

    Kafka transactional output/offset semantics do not automatically include a PostgreSQL commit
    or provider call. If the external provider lacks idempotency and reliable lookup, ambiguity
    prevents an unconditional once-only external-effect guarantee.

    **Interview-ready answer:** “I expect redelivery. The inbox, business transition, and next outbox
    intent commit together; offsets advance afterward. Unique business identities protect against
    distinct envelopes describing the same action. Every downstream owner repeats this discipline.”

    **Follow-up:** Why can acknowledging inside a transactional listener method precede the actual commit?

## Round 9 — Distributed admission grants

### Q9. One hundred pods share 5,000 requests/s. How do busy pods borrow idle capacity without a remote call per request?

??? question "Reveal answer"

    **Verdict:** Issue bounded demand-based grants and consume them locally under explicit rate semantics.

    Static 50 RPS per pod wastes capacity when traffic is uneven. A coordinator can allocate small
    batches from one identified window; local atomic consumption must never exceed a grant.

    | Grant field | Purpose |
    |---|---|
    | Stable request/grant ID | Retry returns the same allocation after response loss |
    | Policy and window identity | Avoids ambiguous resets and mixed policies |
    | Owner and size | Bounds and assigns consumption rights |
    | Validity interval | Prevents using old-window permits |
    | Generation/authority | Supports enforced stale-owner rejection where needed |

    Two invariants differ: **issued grants ≤ window budget**, and **consumption per grant ≤ its
    size**. Use coordinator-defined time and conservative delay/clock margins. Repeatedly setting
    a full TTL on each grant can accidentally extend a fixed window.

    **Trade-off:** Large grants reduce coordinator load but strand more quota after crashes. For
    short windows, initially let unused grants expire instead of reclaiming them unsafely. A fixed
    calendar-second budget permits bursts at adjacent boundaries; it is not a strict rolling-second
    cap. Use a different algorithm if that is the actual promise.

    **Interview-ready answer:** “Pods obtain small idempotent grants according to demand. The
    coordinator bounds issuance for an explicit window, and each pod atomically bounds local use.
    I would define expiry and burst semantics and prefer conservative expiry over unsafe reclamation.”

    **Follow-up:** Can a retry of a lost grant response consume the global budget twice?

## Round 10 — Coordinator failover

### Q10. A Redis primary issues 3,000 permits, loses replication, and its replacement issues another 5,000. What fixes this?

??? question "Reveal answer"

    **Verdict:** Strict failover-safe quotas require durable issuance and enforceable consumption authority.

    Atomic operations on two successive primaries do not preserve a shared budget. Redis WAIT can
    improve replication acknowledgment without establishing every promotion, persistence, and
    partition guarantee required for hard issuance.

    **Durable grant protocol**

    1. Atomically establish ownership of a stable grant request.
    2. Check and debit remaining budget; persist the grant in the same durable transaction.
    3. Return it only after commit; ambiguous retries retrieve that same grant.
    4. After failover, reconstruct outstanding issuance from the committed ledger, not stale cache.
    5. Treat issued grants as spent for that window unless safe revocation/redemption proves otherwise.

    | Proposed fix | Why it is insufficient alone |
    |---|---|
    | Another Redis lock | Lock state can be lost; old holders may continue |
    | Lease timeout | Does not prove consumption stopped |
    | Epoch field | Protects nothing unless an authoritative boundary rejects stale use |
    | PostgreSQL ledger | Still depends on acknowledged commits surviving its actual failover policy |

    Finite reclaimable permits may need stable permit IDs and durable redemption. A new epoch in
    an unreachable coordinator does not stop an isolated local allocator automatically.

    **Interview-ready answer:** “I separate fast cached consumption from durable ownership. A
    replacement must account for every committed grant. I would not reclaim from an unreachable
    owner without enforced revocation and known consumption. If authority is uncertain, I stop
    new hard grants rather than invent remaining quota.”

    **Follow-up:** Is violating an advisory admission budget necessarily an inventory oversell?

## Round 11 — End-to-end idempotency

### Q11. HTTP retries, a redelivered reservation event, and duplicate provider callbacks all occur. Which identities matter?

??? question "Reveal answer"

    **Verdict:** Every independent business effect needs stable identity at its own boundary.

    | Identity | Scope and protection |
    |---|---|
    | HTTP operation key | Authenticated principal plus request fingerprint |
    | Purchase-limit identity | Sale/user/product business eligibility, independent of request keys |
    | Reservation ID | One durable allocation lifecycle |
    | Event ID | One envelope, scoped to its logical consumer for dedup |
    | Order ID / reservation reference | One order for the intended reservation policy |
    | Payment operation / provider key | One logical charge across all retries |
    | Refund / fulfillment operation | Separate stable identities for separate effects |

    Completing an idempotency record after the business transaction in an independent transaction
    leaves a crash gap. Commit result and effect together where possible; otherwise tie recovery
    to a durable business operation and reconcile before takeover. A lease alone cannot prove the
    original attempt did nothing.

    Validate provider retention and callback authenticity; amount, currency, and order identity
    must match. A local unique payment row cannot prevent a provider charging twice under new keys.
    Do not erase dedup state before the supported retry/replay horizon without another protection.

    **Interview-ready answer:** “I reuse logical identities across retries and enforce them at HTTP,
    reservation, order, consumer, and provider boundaries. Request identity does not replace the
    purchase-limit constraint. Unknown provider outcomes retain the original payment identity and
    are reconciled before any new charge or compensation.”

    **Follow-up:** What does the system promise after the provider's idempotency retention expires?

## Round 12 — Outbox at a different scale

### Q12. Now assume 100,000 committed business events/s—not just attempts. How does the outbox change?

??? question "Reveal answer"

    **Verdict:** CDC can remove polling overhead, but it cannot remove database event writes or ordering design.

    **Scale check:** 100,000 events/s is **360 million/hour** and **8.64 billion/day**. At an
    assumed 1 KB payload alone, that is roughly 8.64 TB/day before row/index overhead, WAL,
    replication, and broker storage. This is a separate escalation from 100 units in one sale.

    | Approach | Benefit | Cost / failure obligation |
    |---|---|---|
    | Poll with short durable claims | Fewer moving parts; direct recovery | Scanning, claim updates, vacuum, leases, publication duplicates |
    | Append-only outbox plus CDC | Removes application polling/status-update churn | Connector/slot operations, WAL retention, offsets, replay |
    | Direct DB then broker publication | Lower apparent complexity | Unprotected dual-write crash gap |

    Commit source transition and outbox together. Key events by aggregate, serialize authoritative
    changes, preserve source order, and detect aggregate-version gaps. Separate connectors or
    topics do not gain causal order merely because keys match. Delta events may need every version;
    a last-version-wins snapshot rule is not universally valid.

    Partition and retain based on measured size and verified transport/recovery progress. In
    PostgreSQL, a parent unique constraint on a partitioned table must include its partition key;
    do not assume event-ID-only uniqueness is enforced across time partitions.

    **Interview-ready answer:** “I would benchmark the full write/WAL/capture/broker/consumer path.
    CDC is justified when polling churn is a measured problem. Retention preserves a recovery
    source, consumers remain idempotent, and aggregate ordering is an explicit end-to-end protocol.”

    **Follow-up:** How do you retire an outbox partition without destroying the recovery path?

## Round 13 — CDC outage and disk exhaustion

### Q13. Debezium is down 45 minutes at 100,000 source transactions/s; disk is 95% full. Kafka is healthy. What do you do?

??? question "Reveal answer"

    **Verdict:** Protect the source while preserving committed events and a provable replay boundary.

    **Quantify carefully:** 100,000 × 45 × 60 = **270 million transactions**. If each produces one
    event, that is 270 million pending events—not a WAL-byte estimate. Measure actual retained WAL,
    disk growth, free capacity, and time to exhaustion. Slot LSN distance and filesystem usage are
    related but not identical measurements.

    **Recovery sequence**

    1. Reduce event-producing admission and pause noncritical writes; preserve accepted work.
    2. Add supported storage headroom if available, while diagnosing the continuing growth.
    3. Preserve slot, connector offsets, retained WAL, and outbox replay data.
    4. Check connector workers, permissions, publication/schema changes, connectivity, and resources.
    5. Resume from a valid durable position if required WAL remains; verify delivery and gap handling.
    6. Keep admission bounded until capture and consumers can sustainably outpace new input.

    At 150k capture events/s and 100k new events/s, net drain is 50k/s. A 270-million-event backlog
    takes **90 minutes** under those illustrative sustained rates. “Restart and catch up” needs
    enough disk and downstream capacity for that entire recovery period.

    **If the slot is invalid:** a new slot cannot reconstruct missing history. Recover from complete
    retained outbox/archive data with stable IDs, checkpoints, and overlap deduplication. Timestamps
    alone do not prove commit-order completeness. If WAL and complete replay data are both gone,
    zero-loss recovery may be impossible; say so explicitly.

    **Interview-ready answer:** “I throttle new writes, preserve the recovery sources, and repair
    capture. I do not delete WAL, advance/drop slots, or reset offsets merely to hide disk pressure.
    If uninterrupted writes conflict with preserving committed data, I temporarily restrict writes.”

    **Follow-up:** Which evidence proves that replay covered the gap without charging customers again?

## Round 14 — Schema migration under load

### Q14. Add mandatory payment_status to 200 million orders receiving 20k writes/s. How can the rollout remain compatible?

??? question "Reveal answer"

    **Verdict:** Separate schema expansion, compatible writers, data migration, validation, and contraction.

    | Phase | Action | Exit evidence |
    |---|---|---|
    | Expand | Add compatible initially nullable representation | Old binaries still run; lock budget respected |
    | Compatible deploy | New code reads fallback and writes the new representation | Old/new combinations tested |
    | Backfill | Small resumable batches from authoritative payment records | Checkpoint progress and verified meaning |
    | Validate | Check constraints, mappings, and needed indexes | All data and active writers comply |
    | Enforce | Apply final non-null contract through tested DDL | Bounded locking/scan behavior verified |
    | Contract | Remove old fields and fallbacks later | Rollback/replay compatibility window closed |

    A constant default may avoid a table rewrite on supported PostgreSQL versions, but it does
    not make DDL lock-free or make historical PENDING semantically correct. Paid and refunded
    orders cannot be relabeled simply to satisfy a constraint.

    Backfills use stable checkpoints and guarded updates that do not overwrite newer writes.
    Throttle against lock waits, CPU/I/O, WAL, replication lag, and API latency. Concurrent index
    construction has transaction restrictions and invalid-index recovery requirements too.

    **Interview-ready answer:** “I would make the schema tolerant first, then deploy compatibility,
    backfill authoritative state in bounded batches, validate, and only later enforce and remove
    old paths. I would test lock behavior and rollback. Zero downtime means controlled compatibility,
    not no locks or a guessed default.”

    **Follow-up:** How would the same process replace a customer string ID with a UUID without inventing new identities?

## Round 15 — Mixed application and event versions

### Q15. Thirty percent of pods still omit payment_status while old and new consumers coexist. Can you enforce NOT NULL now?

??? question "Reveal answer"

    **Verdict:** Not until every active writer and supported replay path can honor the contract.

    Keep missing distinct from pending. New readers resolve authoritative payment state or expose
    an explicit unresolved state. Old writers may require a compatibility deployment or narrowly
    controlled database mechanism; a default is safe only if true for every affected insert.

    | Boundary | Check |
    |---|---|
    | Old code / new schema | Reads and writes remain valid throughout overlap |
    | New code / historical rows | Missing values do not fabricate payment status |
    | Old consumer / new event | Optional/additive fields retain compatible meaning |
    | New consumer / retained event | Historical payloads decode and process correctly |
    | Rolling consumers in one group | Shared partition ownership, not duplicate broadcast ownership |
    | Different groups | Each processes independently; duplicate business ownership must be intentional |

    Observe old-writer activity, incomplete rows, consumer failures, and reconciliation outcomes.
    Use conditional/versioned transitions to prevent a stale writer undoing new state. Structural
    schema compatibility is not proof of semantic compatibility.

    **Interview-ready answer:** “I keep the compatibility window open. Tolerant readers and safe
    writers must coexist before constraints tighten, and deployed/replayed event combinations are
    tested. Healthy new pods alone do not prove a safe migration or permit removing rollback paths.”

    **Follow-up:** Can rolling back the binary still be safe after the data interpretation changed?

## Round 16 — Connection pool exhaustion

### Q16. One hundred pods have pool size 30; PostgreSQL allows 1,000 connections and is CPU-saturated. Raise each pool to 100?

??? question "Reveal answer"

    **Verdict:** A larger queue of active database work can worsen the bottleneck.

    | Configuration | Potential application connections |
    |---|---:|
    | 100 × 30 | 3,000 |
    | 100 × 100 | 10,000 |
    | Database ceiling | 1,000, including other services/jobs/operations |

    Configured maxima are not current open connections, but expose an incompatible budget. Count
    rollout surge, background jobs, separate physical primary/replica targets, and fallback load.
    A 30-second acquisition timeout is also inconsistent with a 200 ms acknowledgment goal.

    **Diagnose:** pool active/idle/pending and acquisition time; DB wait events and hot rows;
    transaction age; slow SQL/plans; leaked connections; nested REQUIRES_NEW; remote calls inside
    transactions. Low DB CPU would suggest different waiting causes than the stated 90% CPU case.

    **Fix:** throttle admission, shorten transactions, remove remote waiting from connection-held
    scopes, optimize measured SQL/indexes, and enforce a database-wide concurrency budget. A pooler
    reduces connection-management cost but does not create execution capacity. Blind HPA scale-out
    can increase the same shared pressure.

    **Interview-ready answer:** “I would not raise pools blindly. I would find execution and wait
    bottlenecks, control aggregate concurrency, and align timeouts with the actual response budget.
    More connections are useful only if measured spare capacity exists.”

    **Follow-up:** Why can REQUIRES_NEW consume another connection while its outer transaction holds one?

## Round 17 — Payment retry storm

### Q17. Provider latency reaches eight seconds, callers time out at three, retries multiply, and HPA adds pods. How do you recover?

??? question "Reveal answer"

    **Verdict:** Stop amplification while preserving the identity and recoverability of uncertain charges.

    At 10k original operations/s and three additional attempts each, the provider can see 40k
    attempts/s before other layers retry. More pods may increase this rate without completing
    more payments. A timeout usually stops waiting, not necessarily the remote charge.

    1. Give retries one owner; disable independent amplification across gateways, clients, and services.
    2. Bound new initiation with admission, client deadlines, bulkheads, and breakers.
    3. Reserve capacity for verified callbacks, status lookup, and reconciliation.
    4. Persist UNKNOWN with the original payment identity; reconcile instead of creating a new charge.
    5. Allow only safe classified retries within a capped, jittered retry/deadline budget.
    6. Gradually reopen on measured recovery; half-open probes must be bounded too.

    | Result | Policy |
    |---|---|
    | Validation/auth/confirmed decline | No blind retries |
    | 429 or transient 503 | Follow safe retry guidance within budget |
    | Possible-send timeout | Unknown; preserve identity and query/reconcile |
    | Confirmed success after cancellation | Durable idempotent compensation or defined resolution |

    **Interview-ready answer:** “Breakers and bulkheads protect capacity, not payment uniqueness.
    I would stop multiplied retries and keep a durable unknown-outcome workflow. All permitted
    retries reuse the provider identity; recovery includes reconciling existing work, not only
    restoring low HTTP latency.”

    **Follow-up:** Which work must still run while new payments are shed?

## Round 18 — One million attempts, one hundred units

### Q18. How do you avoid making even one Redis hot key or Kafka process every attempt?

??? question "Reveal answer"

    **Verdict:** Optimize for bounded useful work rather than durable storage of unlimited rejected demand.

    One million one-unit attempts competing for 100 initial units means at most **0.01% of that
    million-attempt cohort** obtains an initial reservation. Edge caching, eligibility checks,
    bot controls, and a waiting room should remove most traffic before central allocation.

    | Admission strategy | Benefit | Customer/operational cost |
    |---|---|---|
    | At most 100 finite attempt permits | Very little contention | Abandonment can strand opportunity |
    | Controlled oversubscription, e.g. 150 | Better utilization when attempts fail | Some admitted users still lose allocation |
    | Unlimited queue | Absorbs traffic temporarily | Storage, stale work, unfair delay; no capacity creation |

    Use small local grants to reduce shared checks. An authenticated signed admission token is
    permission under its exact contract, not automatic stock ownership. Protect token replay and
    per-user eligibility; IP-only control is neither complete fairness nor robust bot resistance.

    Before returning accepted/pending, persist work durably. Fail closed for new hard allocation
    when ownership cannot be established, while safely advancing existing reservations, callbacks,
    and reconciliation. Reopen released stock through the same guarded accounting protocol.

    **Interview-ready answer:** “I would reject or queue at the edge, allocate bounded local
    admission, and let only a small identified set reach durable reservation processing. PostgreSQL
    still enforces stock. I would describe admission honestly and measure useful completed work,
    rejection, fairness, and recovery—not just raw ingress RPS.”

    **Follow-up:** How do you avoid holding most of the 100 permits on an idle or failed gateway?

## Round 19 — Multi-region inventory ownership

### Q19. Singapore and Tokyo each receive 500k attempts/s and may be partitioned for minutes. How can both use the same 100 global units safely?

??? question "Reveal answer"

    **Verdict:** Regions may spend exclusive preallocated rights locally; they may not independently copy global stock.

    An illustrative grant is Singapore 60 units and Tokyo 40. A durable global ownership ledger
    governs rights; each local authority enforces its remaining allocation with reservations and
    outbox in a local transaction. The split follows expected demand, not a universal ratio.

    **Accounting:** every unit has one current owner and one lifecycle state. Unallocated plus
    region-owned rights equals issued supply; each region separately accounts for available,
    reserved, sold, and transfer-frozen quantities without counting the same unit twice.

    ```mermaid
    stateDiagram-v2
        REQUESTED --> SOURCE_FROZEN: Source durably stops spending transferred rights
        SOURCE_FROZEN --> OWNERSHIP_COMMITTED: Global ledger commits unique transfer
        OWNERSHIP_COMMITTED --> DESTINATION_ACTIVATED: Destination applies transfer once
        DESTINATION_ACTIVATED --> COMPLETED: Reconcile checkpoints
    ```

    Freeze-before-activate can temporarily strand units, but must never permit dual ownership.
    Transfer retries use stable identity and enforced generations; recovery resumes durable state.
    A region that runs out cannot borrow unreachable rights. Continued local progress is limited
    to its already-owned stock.

    **Comparison:** consensus per purchase simplifies one global authority but adds cross-region
    latency and partition dependence. Escrow reduces coordination frequency at utilization and
    transfer complexity cost. A third quorum voter helps global decisions only on the quorum side;
    it does not make both partitioned sides globally writable.

    **Interview-ready answer:** “I would preallocate exclusive stock rights and spend locally.
    Strongly consistent transfers remove source rights before destination activation. This trades
    stranded capacity for correct local availability. Edge admission remains essential at 500k
    attempts/s because regional ownership does not increase the number of units.”

    **Follow-up:** Can Tokyo take Singapore's reported remaining 20 units when Singapore stops responding?

## Round 20 — Regional disaster and split brain

### Q20. Singapore is unreachable but may still be selling; Tokyo is sold out. What permits safe takeover?

??? question "Reveal answer"

    **Verdict:** Unreachable is not stopped, and a stale available count is not transferable inventory.

    A health check failure, expired Redis lock, DNS change, or coordinator epoch increment does not
    prove isolated applications and their local database can no longer spend old rights. Fencing
    works only where stale operations are actually rejected at an authoritative write boundary.

    | Situation | Safe action |
    |---|---|
    | Source durably freezes/relinquishes and consumption is known | Commit bounded transfer; activate once |
    | Old writer is verifiably fenced and durable consumption recovered | Establish actual remaining rights, then transfer |
    | Source still might sell or consumption is uncertain | Leave disputed rights unavailable |
    | Old region rejoins | Reconcile committed work and obtain current authority before writes |

    **Runbook order:** identify ledger authority → establish exclusion/relinquishment → recover
    committed reservations and unknown payments → determine actual transferable quantity → commit
    transfer → activate destination idempotently → validate rejoin rules. Do not execute irreversible
    recovery operations merely to satisfy an availability dashboard.

    Local DB failover also requires preserving acknowledged stock/ledger commits under the stated
    failure model. Ownership consensus cannot repair a regional store that forgets sales and resumes
    spending their rights without reconciliation.

    **Interview-ready answer:** “I would not infer spare inventory from a dead health check. I need
    both known remaining rights and enforceable exclusion of the old writer. Otherwise I accept
    temporary unavailability rather than create two owners. Traffic failover and inventory-ownership
    failover are separate procedures.”

    **Follow-up:** What concrete component rejects epoch 41 after epoch 42 is issued?

## Round 21 — Operational evidence and final defense

### Q21. How do you demonstrate the complete design works, and summarize it without reciting technologies?

??? question "Reveal answer"

    **Verdict:** Reconcile business invariants through tested failure histories, then state the accepted costs.

    | Boundary | Evidence / targeted test |
    |---|---|
    | Stock conservation | Initial plus valid replenishment = available + reserved + sold, with defined transfer states |
    | Purchase identity | Concurrent same-key requests; different keys under the same business eligibility rule |
    | Reservation commit | Crash before/after commit; lost response; exactly one durable allocation |
    | Broker handoff | Publication/checkpoint crash; stable IDs and safe duplicates |
    | Payment/expiry | Duplicate callbacks, late observation, unknown charge, duplicate release/refund |
    | Quota grants | Lost response, stale promotion, holder partition/resumption, expired use |
    | CDC | Connector outage, disk runway, valid/invalid slot, replay completeness and gap detection |
    | Deployment | Mixed schema/event versions, concurrent backfill, lock budgets, rollback |
    | Regional recovery | Freeze/transfer crash, old writer exclusion, stale-region rejoin |

    Measure offered/admitted/completed traffic, business rejects versus overload, acknowledgment and
    completion latency separately, stock mismatches, oldest pending/unknown work, outbox age,
    consumer/CDC progress, disk runway, lock/pool waits, and retry amplification. Use bounded metric
    labels; operation IDs belong in protected audit/logs, not high-cardinality series.

    Fault/load tests belong in controlled environments with real-engine semantics where relevant.
    Do not imply that one passing stress run proves every distributed schedule or that a mocked
    provider proves its idempotency contract. State deployment durability assumptions and untested
    scenarios honestly.

    **Two-minute interview answer:** “I would clarify eligibility, reservation/payment timing,
    fairness, and what the initial response promises. I would estimate bursts separately from
    useful stock transactions. The edge and provisional admission absorb most attempts; the
    inventory authority commits operation identity, stock, reservation, and outbox together.
    Consumers commit deduplication with business effects before advancing offsets. Payment uses
    stable provider identity; unknown outcomes are reconciled, and expiry/release are guarded
    transitions. Hard admission grants need durable issuance and enforced ownership. CDC can remove
    polling at scale but adds WAL and recovery obligations. Regions may spend exclusive stock rights
    locally, with freeze-before-transfer and enforced fencing for takeover. I accept bounded
    rejection, pending outcomes, and temporarily stranded rights before double allocation. I would
    qualify the full system using invariant checks, commit-gap faults, representative skew, and
    measured recovery capacity.”

    **Follow-up:** Which failure assumption would invalidate your strongest guarantee?

## Review gate — before the next system

- Explain all acknowledgment and commit boundaries without equating pending with reserved.
- Walk through each crash/race and identify its durable recovery owner.
- Distinguish request, event, business-operation, grant, and provider identities.
- Compare Redis admission, SQL authority, polling/CDC, and global consensus/exclusive rights.
- Calculate backlog drain and disk runway from measured rates, not raw event counts alone.
- State why a TTL, lease, health check, or unenforced epoch does not prove safe takeover.
- Separate measured lab behavior from architectural proposals and hypothetical capacity figures.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Outbox, inbox, saga, and effectively-once mechanisms](../distributed-data-patterns/practice.md)
- [Database isolation, pooling, HA, and migration mechanisms](../database-sql/practice.md)
- [Retry, timeout, bulkhead, and admission mechanisms](../resilience/practice.md)
- [Kafka delivery and ordering mechanisms](../kafka/practice.md)
- [Next: Distributed Rate Limiter — continue after reviewing this case](practice-rate-limiter.md)
