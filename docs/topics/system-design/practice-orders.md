# Case 3 — Order Management: Senior Interview Walkthrough

Finish [Payments](practice-payments.md), then complete this order lifecycle before [Notifications](practice-notifications.md).
Answer each interviewer prompt before revealing the reasoning and spoken answer. This expands the
commerce scenarios from interview-prep without claiming that the complete platform is deployed here.

## Scenario and progression

| Dimension | Working assumption |
|---|---|
| Features | Create/query/cancel orders; reserve inventory; pay; fulfill; show pending status |
| Audience | Illustrative 10k active users and 5× bursts; behavior must determine RPS |
| Consistency | Local aggregate transactions; recoverable cross-context workflow |
| Read path | Optional projection/cache with explicit freshness behavior |
| Invariants | One logical order; legal transitions; no shipment before required confirmations |

Proceed through scope → capacity → model → creation → architecture comparison → saga →
cancellation → delivery/order → poison events → read freshness → rebuild → sharding → regions →
rollout/security → operational evidence. Payment mechanics and inventory algorithms remain canonical
in their linked cases; this case composes them into an accountable order workflow.

## Round 1 — Acknowledgment semantics

### Q1. When POST /orders succeeds, what exactly has the platform promised?

??? question "Reveal answer"

    **Verdict:** Durable order acceptance is not stock reservation, payment success, or shipment.

    | Business question | Contract consequence |
    |---|---|
    | Can stock be rejected later? | Pending state and clear customer resolution |
    | Partial fulfillment allowed? | Line/fulfillment state, not one simplistic order flag |
    | Cancellation cutoff? | Guarded race with payment and dispatch |
    | Price/quote validity? | Server-validated immutable commercial snapshot |
    | Read-your-write required? | Version-aware query or authoritative read path |

    Define whether 201 creates an order resource in pending state or 202 accepts an operation
    whose resource is not yet established. HTTP status alone is not a business completion guarantee.
    A customer retry after response loss must retrieve the same accepted operation.

    **Interview-ready answer:** “I would name the durable promise and its pending outcomes before
    choosing services. Every accepted order has an owner and recovery path, even when stock or
    payment later fails. Shipment requires explicit business evidence, not only API success.”

    **Follow-up:** What does the user see if stock rejects an already accepted order?

## Round 2 — Capacity and amplification

### Q2. How do 10k users and a 5× spike translate into database and broker demand?

??? question "Reveal answer"

    **Verdict:** Request mix and per-order amplification determine load; users alone do not.

    | Illustrative assumption | Derived demand |
    |---|---|
    | 10k users, one request each five seconds | 2,000 RPS |
    | 5% are order creation | 100 orders/s baseline |
    | Five durable events per order | 500 events/s before retries/projections |
    | 5× same-mix burst | 500 orders/s; 2,500 events/s |
    | 50 ms mean API response at 2,000 RPS | About 100 requests in flight |

    Items/order, database queries, consumer fan-out, retries, and payment duration add distinct work.
    At an assumed 2 KB/order, 100 orders/s is about 17.28 GB/day of order payload alone—not total
    storage. Retention, indexes, outbox, audit, and replication multiply the footprint.

    **Trade-off:** HPA can relieve application CPU but increase shared DB/provider pressure. Budget
    rollout surge, background workers, and fallback reads across the fleet.

    **Interview-ready answer:** “I size commands, reads, events, and dependencies separately, then
    qualify skew and recovery drain. A five-times traffic target needs measured headroom and
    admission control; it is not a promise that all latencies stay flat.”

    **Follow-up:** Which bottleneck changes when orders have fifty items instead of two?

## Round 3 — Aggregate and API design

### Q3. What belongs in the order aggregate, and which constraints survive retries?

??? question "Reveal answer"

    **Verdict:** Preserve commercial intent and transition identity independently from mutable catalog data.

    | Data | Purpose |
    |---|---|
    | Order header/version/owner | Lifecycle and authorized concurrency |
    | Line snapshots | Product reference, quoted price/currency, quantity, tax/discount policy |
    | Principal-scoped operation key/fingerprint/result | Retry recovery and changed-request rejection |
    | Reservation/payment/fulfillment references | Correlate independently owned effects |
    | Workflow state/deadlines | Recover required next steps |
    | Inbox/outbox identities | Duplicate-safe local effects and publication |

    Create, query, cancel, and any modification APIs need distinct legal transition rules. Do not
    let the client choose another tenant's order ID or trusted payment state. Listing indexes follow
    tenant/customer and stable pagination keys; they are not justification for unbounded entity loading.

    **Interview-ready answer:** “I model the order as a versioned commercial agreement, not a live
    join to today's catalog. Durable operation identity handles retries, while references and
    persisted workflow state connect stock, money, and fulfillment without sharing their databases.”

    **Follow-up:** Can the same idempotency key be reused after changing the shipping address?

## Round 4 — Creation and commit gaps

### Q4. Trace a creation whose response is lost and whose first event send fails.

??? question "Reveal answer"

    **Verdict:** Commit order, operation result, and publication intent atomically before acknowledging acceptance.

    ```mermaid
    sequenceDiagram
        actor Customer
        participant API
        participant DB as Order authority
        participant Relay
        participant Kafka
        participant Workflow
        Customer->>API: Create with stable scoped key
        API->>DB: Commit order, result, outbox
        API--xCustomer: Response lost
        Customer->>API: Retry same operation
        API->>DB: Retrieve committed result
        Relay->>DB: Recover unpublished intent
        Relay->>Kafka: Publish; duplicate send possible
        Kafka->>Workflow: Deduplicated local transition and next intent
    ```

    A send attempted inside an ordinary DB transaction is not atomic with broker durability. A
    background in-memory task is not durable recovery. Publication after commit may duplicate if
    its acknowledgment/checkpoint is lost, so consumer identity remains necessary.

    **Interview-ready answer:** “The customer's retry finds the committed order, and the relay finds
    committed intent. I do not rely on the original request thread surviving to publish. A failed
    response or send changes observation timing, not the already committed business identity.”

    **Follow-up:** When can an outbox row be safely removed under your recovery policy?

## Round 5 — Architecture comparison

### Q5. Why not begin with a dozen services and a separate CQRS database?

??? question "Reveal answer"

    **Verdict:** Distribution must buy ownership, deployment, or measured scaling benefits.

    | Choice | Benefit | Cost |
    |---|---|---|
    | Modular monolith | Local transactions, simpler deployment/debugging | Shared process/resource/release constraints |
    | Independent contexts | Team ownership and isolated deployment | Network contracts and partial failures |
    | Same DB, separate read model | Query simplification without immediate extra infrastructure | Projection/version maintenance |
    | Separate CQRS store | Independent query scaling/indexing | Freshness, rebuild, and cutover obligations |

    CQRS separates command and query models; it does not inherently require separate databases,
    brokers, or event sourcing. An outbox event log is not automatically a complete authoritative
    event-sourcing history. Identify the first expensive read or ownership conflict before extracting.

    **Interview-ready answer:** “I would use the simplest boundaries meeting current requirements,
    while keeping explicit domain contracts. I add asynchronous projections or independent services
    when their benefit exceeds the cost of failure recovery and operating another consistency model.”

    **Follow-up:** Which observed limit would justify extracting inventory first?

## Round 6 — Saga coordination

### Q6. How do reservation, payment, and fulfillment progress without a distributed rollback?

??? question "Reveal answer"

    **Verdict:** Persist legal workflow transitions, deadlines, and compensating intent at each owner.

    | Style | Good fit | Failure burden |
    |---|---|---|
    | Choreography | Few understandable independent reactions | Hidden dependencies, timers, recovery visibility |
    | Orchestration | Complex branching/deadlines/compensations | Durable coordinator and versioned workflow definition |

    An orchestrator issues idempotent commands, records responses, and advances guarded state; it
    does not make remote operations one ACID transaction. A step that succeeds then loses its
    response is reconciled under the original command identity. Compensation, such as release or
    refund, is another durable business operation that can fail and need escalation.

    Avoid remote waiting while holding an order DB transaction. Persist the next action and resume
    later. Define which confirmations are required before dispatch, including authorization/capture
    policy and inventory ownership.

    **Interview-ready answer:** “I choose coordination for workflow clarity, then make every step
    and compensation recoverable. The saga reaches an accounted outcome; it does not roll back
    external history as though the entire purchase were one transaction.”

    **Follow-up:** Who detects a compensation stuck for hours after the original order failed?

## Round 7 — Cancellation race

### Q7. Cancellation arrives while payment succeeds and fulfillment starts. Can both paths proceed?

??? question "Reveal answer"

    **Verdict:** One owner arbitrates each state transition; already initiated external work needs resolution.

    Define cancellation cutoffs and fulfillment states such as requested, dispatching, and dispatched.
    A conditional order transition can decide whether cancellation is accepted locally, but cannot
    prove a carrier has not already acted. Persist observations even if they conflict with the
    preferred order outcome; do not drop a late payment because cancellation already won.

    | Situation | Resolution |
    |---|---|
    | Cancel before any irreversible step | Durable release/void commands |
    | Payment captured after cancel | Idempotent refund workflow |
    | Dispatch uncertain | Query/reconcile stable shipping operation |
    | Already shipped | Returns/business exception policy, not fictional rollback |

    A boolean cancelled flag checked in several services is vulnerable to stale reads. Use versions,
    scoped operation IDs, explicit command responses, and auditable unresolved states.

    **Interview-ready answer:** “I distinguish accepting cancellation from completing all its
    effects. Guarded transitions decide local eligibility, while durable reconciliation and
    compensation resolve money or shipment that crossed an external boundary.”

    **Follow-up:** Is an acknowledged cancellation allowed to mean 'requested' rather than 'completed'?

## Round 8 — Delivery, ordering, and stale workers

### Q8. Why are same-key Kafka records and a relay lease insufficient by themselves?

??? question "Reveal answer"

    **Verdict:** Broker order, source serialization, duplicate identity, and worker authority solve different problems.

    Commit inbox and effect together; use business uniqueness when distinct envelopes describe one
    operation. Authoritative aggregate updates establish versions; publication and consumer processing
    must preserve the needed order. Parallel processing of one partition can reorder completed effects.

    A relay lease may expire while an old worker still sends. Fencing its checkpoint prevents stale
    completion but cannot unsend an already published record. Stable event identity and consumer
    rules must tolerate it. Partition-count changes or separate producers/connectors can also break
    assumptions about sequence.

    For a delta stream, missing version 2 before version 3 may require defer/recover; blindly using
    the highest version loses changes. Full-state snapshots can use another documented protocol.

    **Interview-ready answer:** “I establish source versions, stable event/business identities, and
    ordered effect application. Leases coordinate work but do not prove once-only publication.
    Consumers detect stale, duplicate, and gap cases explicitly.”

    **Follow-up:** Can a cancellation event arrive before the placement event that contains required item data?

## Round 9 — Poison events and recovery ownership

### Q9. One invalid event blocks a partition. Should you skip it or retry forever?

??? question "Reveal answer"

    **Verdict:** Classify failure, preserve order obligations, and make unresolved business work visible.

    | Failure | Policy |
    |---|---|
    | Transient dependency | Bounded deadline/backoff and recovery |
    | Invalid payload/schema | Quarantine with evidence and owner |
    | Domain conflict | Inspect legal state; may need reconciliation |
    | Missing predecessor | Recover gap or defer according to ordering protocol |

    Moving an event to a DLT changes the ordering path: later events may become invalid if they
    depend on it. Define whether to block an aggregate, defer successors, or process independent
    work. A successful DLT publish is not a completed order; retain stuck workflow state and alert age.

    Recovery tooling must authorize replay, preserve identity, and revalidate current business
    eligibility. A week-old shipment or charge should not execute simply because an operator pressed retry.

    **Interview-ready answer:** “I would avoid infinite partition-wide retries and blind skipping.
    Failure routing preserves evidence and business ownership, while per-aggregate order rules
    determine what may continue. Replay is a controlled workflow, not a reset of history.”

    **Follow-up:** Who is accountable when the Kafka consumer is healthy but the order remains stuck?

## Round 10 — Read-your-write and replica lag

### Q10. Creation returns success, but GET /orders/id returns 404. How do you fix the user contract?

??? question "Reveal answer"

    **Verdict:** Distinguish projection absence from authoritative nonexistence.

    A query replica/projection can lag after durable acceptance. A missing result must not be
    negatively cached as permanent absence when the client has proof of creation. Return the command
    representation, a pending status, or support a minimum-version token and bounded wait/fallback.

    | Option | Benefit | Cost |
    |---|---|---|
    | Authoritative read after create | Simple immediate visibility | Primary load and routing policy |
    | Version-aware projection read | Explicit freshness | Token/wait complexity |
    | Pending operation endpoint | Honest asynchronous UX | More states for clients |

    Apply ownership checks on every path, including fallback and caches. A replica-lag threshold
    does not guarantee a particular record is visible; measure progress/watermarks relevant to the read.

    **Interview-ready answer:** “The API should expose accepted state honestly rather than let
    replication timing look like lost data. I choose a bounded consistency path and protect the
    primary from unbounded projection fallback during an outage.”

    **Follow-up:** What prevents a large projection outage from redirecting every query to the primary?

## Round 11 — Online projection rebuild

### Q11. Rebuild the read model while orders continue changing. What is the cutover protocol?

??? question "Reveal answer"

    **Verdict:** Use a consistent baseline plus a defined change boundary, not an uncoordinated table dump.

    1. Establish snapshot/stream checkpoints and required retained history.
    2. Build a shadow schema/model; preserve identity, versions, deletes, and tenant rules.
    3. Apply overlapping changes idempotently with gap handling; isolate external side effects.
    4. Compare counts, invariants, sampled rows, and business-derived totals.
    5. Catch up, switch routing through a controlled version, and retain rollback capability.

    Retention may omit creation events; replay alone then cannot rebuild complete state. Timestamp
    ordering does not establish transaction commit order. Zero consumer lag proves only transport
    progress, not semantic equality. If the old model is defective, validate against source invariants
    or an independent specification rather than copying its wrong output.

    **Interview-ready answer:** “I treat rebuild as a consistency migration with explicit input
    boundaries, independent validation, and rollback. Historical processing updates the read model
    only; it must not resend payments, emails, or shipment commands.”

    **Follow-up:** How do you handle a deletion that occurs between snapshot copy and stream catch-up?

## Round 12 — Scaling past one primary

### Q12. When is sharding justified, and what key would you choose?

??? question "Reveal answer"

    **Verdict:** Remove avoidable work first, then shard around transaction locality and operational ownership.

    Optimize queries/indexes, batching, short transactions, read models, and measured hot spots.
    Read replicas do not scale writes; table partitioning does not by itself place writes on
    independent machines. A hot order/tenant remains hot under a poorly chosen key.

    | Candidate | Benefit | Cost |
    |---|---|---|
    | Tenant/home context | Local authorization and many transactions | Large-tenant skew |
    | Order hash | Even order distribution | Customer listings and cross-order queries fan out |
    | Time range | Retention/locality | Newest range can become hot |

    Plan routing versions, globally scoped identity, resharding, uniqueness, backup/restore,
    cross-shard workflows, and query semantics before migration. Blind dual writes create split
    ownership and partial commits; choose one active writer per migrated aggregate.

    **Interview-ready answer:** “I shard against an evidenced limit and preserve local invariants.
    The decision includes migration and recovery, not only a hash function. If the workload still
    fits one tuned authority, I retain that simpler consistency model.”

    **Follow-up:** How is a tenant moved while in-flight orders and consumers still use the old routing?

## Round 13 — Regional ownership and DR

### Q13. How would you fail over orders to another region without duplicate money or shipment?

??? question "Reveal answer"

    **Verdict:** Recover acknowledged state and fence former owners before new initiation authority is granted.

    Active-passive has explicit RPO/RTO; active-active often requires home-region ownership for an
    aggregate and deliberate cross-region operations. Last-write-wins does not preserve stock,
    financial, or cancellation invariants. DNS failover cannot stop an isolated old writer.

    | Recovery concern | Required decision |
    |---|---|
    | Acknowledged but unreplicated order | State-loss promise and reconciliation source |
    | Provider/shipping operation in flight | Original external identity and status lookup |
    | Old region still running | Enforced writer exclusion |
    | Rejoin and event replay | Ownership validation and deduplicated repair |

    A promoted DB must satisfy the claimed acknowledged-commit durability policy. Restore outbox,
    workflow timers, operation identities, and inbox retention consistently; an order table alone
    is insufficient to resume business processing safely.

    **Interview-ready answer:** “I would rehearse business recovery, not just database promotion.
    New ownership needs known durable state, exclusion of stale initiators, and reconciliation of
    unknown external effects. Uncertain orders remain visible rather than being recreated.”

    **Follow-up:** Which dependency prevents meeting the advertised recovery time even after the DB is online?

## Round 14 — Rollout, security, and operations

### Q14. How do new order states and event fields roll out safely while keeping tenant isolation?

??? question "Reveal answer"

    **Verdict:** Every deployed writer/reader combination and recovery path must remain semantically valid.

    Expand schema, deploy tolerant readers/compatible writers, backfill authoritative values in
    bounded batches, validate, then enforce and contract after rollback closes. Old code must not
    overwrite a new state it cannot interpret; versioned transitions are as important as optional fields.

    Event consumers must handle old retained payloads and new producers. Same-group rolling consumers
    share partitions, while separate groups each receive work; avoid accidentally duplicating ownership
    of a charge or dispatch workflow during migration.

    Authorize commands, listings, projections, fallback reads, and operational replay by tenant/owner.
    Keep addresses and payment data out of metric labels. Observe workflow/outbox age, unknown
    outcomes, projection freshness, duplicate suppression, DLT, and pool waits—not merely HTTP uptime.

    **Interview-ready answer:** “A safe deployment preserves interpretation and authority across
    old/new versions, replay, and rollback. I verify tenant isolation across every read path and
    require auditable authorization for operational repair.”

    **Follow-up:** Can a structurally compatible new event still make an old consumer perform the wrong business action?

## Round 15 — Evidence and final defense

### Q15. What demonstrates an accountable outcome for every accepted order?

??? question "Reveal answer"

    **Verdict:** Reconcile accepted commands with final or explicitly unresolved durable workflows.

    | Test history | Evidence |
    |---|---|
    | Lost response and concurrent retries | One order and recoverable result |
    | Send/checkpoint and DB/offset crashes | No lost required intent; duplicate-safe effects |
    | Cancel/payment/dispatch races | Legal states and recoverable compensation |
    | Projection lag/rebuild | Honest visibility, no external replay effects |
    | Poison event and operator replay | Named owner, preserved identity, ordering rules |
    | Failover/restore/mixed versions | Correct authority, identity, and business reconciliation |

    Run representative read/write mixes, hot tenants, and outage recovery; report offered versus
    completed work, tail latency, rejection, saturation, and net backlog drain. Case-study targets
    are not measured lab results. A mocked provider does not establish its real idempotency contract.

    **Interview-ready answer:** “I persist one commercial intent with an atomic order/result/outbox
    boundary. Recoverable local transitions coordinate stock, payment, cancellation, and fulfillment.
    Queries expose explicit freshness; projections rebuild under checkpoints. Scaling and regions
    preserve ownership and external identities. I qualify accepted-order accounting through commit-gap
    faults and business invariants, not only fast API responses.”

    **Follow-up:** Can you account for every accepted order after a controlled failure experiment?

## Review gate

Defend the command acknowledgment, saga/compensation ownership, cancellation race, event ordering,
read freshness, shadow rebuild, and regional recovery before starting notifications.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Payments](practice-payments.md)
- [Outbox, inbox, and saga mechanisms](../distributed-data-patterns/practice.md)
- [Next: Notifications](practice-notifications.md)
