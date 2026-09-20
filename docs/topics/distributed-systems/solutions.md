# Distributed Systems Solutions

Architectural solutions and corrected design documents corresponding to the code review exercises.

## Fencing tokens for distributed locking

### Implementation

````markdown
--8<-- "modules/17-distributed-systems/broken-examples/distributed-lock-no-fencing/correct/ADR-004-distributed-locking.md"
````

### Why it works

1. **Monotonically Increasing Sequence**: The lock service issues an atomic, monotonically increasing integer counter (e.g. 101, 102, 103) upon granting each lease.
2. **Storage Gatekeeper Verification**: Shared storage enforces atomic conditional writes (`WHERE last_fencing_token < :current_token`). If a client pauses for longer than the lock TTL and is superseded by another client with a higher token, the storage layer blocks the delayed write.
3. **Lease Watchdog Heartbeat**: While the client is active and healthy, a background thread extends the lease TTL before expiration, ensuring locks are held only as long as work is actively progressing.

### Trade-offs

Requires the underlying storage layer (or a relational manifest metadata database) to support atomic conditional compare-and-swap (CAS) operations on the fencing token.

---

## Logical clocks and causal ordering for ledger replication

### Implementation

````markdown
--8<-- "modules/17-distributed-systems/broken-examples/wall-clock-order-assumption/correct/DESIGN-ledger-replication.md"
````

### Why it works

1. **Single-Master Account Affinity**: Every customer account is mastered in a single home region, ensuring that all balance mutations for that account are serialized linearly on a single primary node, completely preventing double-spending and race conditions.
2. **Append-Only Journal Deltas**: Financial records are stored as immutable debit/credit deltas rather than destructive balance overwrites, preserving full audit history and double-entry accounting invariants.
3. **Hybrid Logical Clocks (HLC)**: Combines physical time with a logical counter, providing strict monotonic causal ordering that is impervious to physical clock drift, NTP adjustments, and leap seconds.

### Trade-offs

Mutating transactions initiated outside the account's home region incur cross-region WAN network latency. Non-mutating read queries can still be served locally with bounded staleness or read-your-writes session routing.

---

## Saga orchestration vs two-phase commit

### Implementation

````markdown
--8<-- "modules/17-distributed-systems/broken-examples/two-phase-commit-coordinator/correct/DESIGN-distributed-checkout-2pc.md"
````

### Why it works

1. **Non-Blocking Local Transactions**: Each microservice (`OrderService`, `InventoryService`, `PaymentService`) executes strictly local, sub-10ms database transactions, immediately committing changes and releasing database row locks.
2. **Compensating Transactions for Failure**: If payment fails, the Saga Orchestrator triggers explicit, idempotent compensating actions (e.g. `releaseReservation`) to reverse earlier steps asynchronously.
3. **High Composite Availability**: Services remain decoupled. If a secondary downstream dependency is slow or temporarily unavailable, reservations can be queued or retried without exhausting thread pools in the checkout service.

### Trade-offs

Sagas introduce intermediate states (e.g. `PENDING`, `RESERVED`) that are visible to concurrent reads, requiring application business logic to handle semantic anomalies (e.g. reserving inventory that is subsequently released upon payment failure).

---

## Related

- [Code Review](code-review.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Production](production.md)
- [Interview Questions](questions.md)
