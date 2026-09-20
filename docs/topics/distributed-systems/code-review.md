# Distributed Systems Code Review

Review each clean architecture design document before expanding its answer.

## Distributed lock without fencing tokens

An engineering team authored an Architecture Decision Record proposing a distributed locking mechanism using Redis (`SET ... NX PX 10000`) to coordinate batch mutations against cloud object storage.

````markdown
--8<-- "modules/17-distributed-systems/broken-examples/distributed-lock-no-fencing/ADR-004-distributed-locking.md"
````

Consider client-side pause hazards (JVM Stop-The-World GC pauses, hypervisor scheduling latency, network buffer delays), lease expiration boundaries, and Martin Kleppmann's fencing token critique.

??? warning "Reveal issues"
    **Concurrency issue — client pause outliving lock TTL causes concurrent dual writes:** If Worker 1 acquires the lock with a 10s TTL and immediately experiences a 12-second Stop-The-World GC pause or network stall, Redis expires the lock and grants it to Worker 2. Worker 1 wakes up unaware of the lease expiration and proceeds to write to S3, corrupting files currently being written by Worker 2.

    **Architecture issue — missing monotonically increasing fencing tokens at the storage boundary:** Distributed locks alone cannot guarantee mutual exclusion if the lock service and storage layer are decoupled. The lock service must issue a monotonically increasing fencing token upon acquisition, and the storage layer must verify that incoming writes carry a token strictly higher than the previously committed write, rejecting stale lock holders.

[Correct implementation](solutions.md#fencing-tokens-for-distributed-locking)

---

## Wall-clock ordering in multi-region ledger replication

A digital wallet platform submitted a design document proposing Last-Write-Wins (LWW) conflict resolution for multi-region active-active account balance updates using server timestamps (`System.currentTimeMillis()`).

````markdown
--8<-- "modules/17-distributed-systems/broken-examples/wall-clock-order-assumption/DESIGN-ledger-replication.md"
````

Consider physical quartz clock drift, Network Time Protocol (NTP) uncertainty bounds across geographic regions, leap seconds, and double-entry ledger invariants.

??? warning "Reveal issues"
    **Data consistency issue — physical wall-clock LWW causes lost updates due to clock drift:** Physical quartz oscillators drift due to heat and virtualization scheduling. NTP synchronizes clocks over asynchronous networks with 5–50+ ms uncertainty, and NTP step adjustments can jump time backward. If Node A's clock is 50ms ahead of Node B's clock, an update on Node B executed after Node A will be assigned a smaller timestamp and silently discarded by LWW, causing silent ledger divergence and lost money.

    **Architecture issue — multi-master LWW overwrites accumulative ledger state and allows overdrafts:** A bank balance is an accumulative ledger, not an idempotent single-value register. Overwriting balance state via LWW drops intermediate transactions and allows concurrent cross-region withdrawals exceeding the balance (double-spending). Ledger writes must be modeled as append-only journal deltas with single-master affinity or causal sequence ordering.

[Correct implementation](solutions.md#logical-clocks-and-causal-ordering-for-ledger-replication)

---

## Synchronous two-phase commit across microservices

An e-commerce engineering team designed a distributed transaction protocol to coordinate checkout across `OrderService`, `PaymentService`, and `InventoryService` using synchronous Two-Phase Commit (2PC).

````markdown
--8<-- "modules/17-distributed-systems/broken-examples/two-phase-commit-coordinator/DESIGN-distributed-checkout-2pc.md"
````

Consider coordinator failure modes, in-doubt participant blocking, database row lock holding times, network partition behavior, and microservice coupling.

??? warning "Reveal issues"
    **Reliability issue — coordinator failure in Phase 2 causes distributed blocking and indefinite lock holding:** 2PC is fundamentally a blocking protocol. If the coordinator crashes or is partitioned after participants vote `VOTE_COMMIT` in Phase 1, participants enter an In-Doubt state where they can neither commit nor abort unilaterally. All participant databases hold physical transactions and exclusive row locks indefinitely, exhausting connection pools and causing system-wide paralysis.

    **Concurrency issue — holding row locks across network RPCs:** Holding pessimistic database locks (`SELECT ... FOR UPDATE`) while waiting for multiple network roundtrips across independent microservices creates severe lock contention and connection pool starvation.

    **Architecture issue — temporal coupling degrades composite system availability:** Composite availability is the product of all participants ($A_{\text{total}} = \prod A_i$). A minor latency spike or partial outage in one service cascades into total checkout failure. Microservices should be decoupled using the Saga pattern with local transactions and compensating workflows.

[Correct implementation](solutions.md#saga-orchestration-vs-two-phase-commit)

---

## Related

- [Solutions](solutions.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Production](production.md)
- [Interview Questions](questions.md)
