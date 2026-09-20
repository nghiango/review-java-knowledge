# Data Consistency Issues

Lost updates, stale state, partial writes and identity/invariant failures.

## Entries

### Mutable key after HashMap insertion

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Core Java, HashMap · **Interview frequency:** High · **Production impact:** High

A HashMap records the insertion-time bucket; it cannot observe later mutation of equality/hash
state. The entry consumes memory but normal lookup and removal can miss it. Use an immutable
identity key and explicitly remove/reinsert when identity genuinely changes.

**Detection:** Compare lookup and hash values before/after mutation; inspect entries in a debugger.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

**Trade-off:** Immutable identity separates profile data and requires explicit identity migration.

**Interview follow-up:** Why does ConcurrentHashMap not repair a mutated key?

### Import mutates caller state before validation completes

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

Appending directly to caller-owned state makes a later parse failure leave a partial import. Build
an explicit result locally, then let the application choose atomic, chunked or compensating commit.

**Appears in:** [Core Java — resource and collection mutation](../topics/core-java/code-review.md#resource-and-collection-mutation)

### Parallel report loses deterministic order and rows

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

Parallel `forEach` does not order side effects, and an unsafe accumulator can lose values. Produce
one immutable result per input and join in the documented order.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

### Missing Cache Invalidation on Mutation (Stale Cache)

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Basic

Modifying or deleting an entity in persistent storage without invalidating or updating the corresponding cache entry (`@CacheEvict`) leaves stale data in the cache until TTL expiration or causes zombie reads of deleted records. Evict the cache key upon successful database mutation with `beforeInvocation = false`.

**Appears in:** `modules/13-caching-redis/broken-examples/stale-cache-after-update`

### Dirty Cache Writes on Database Transaction Rollback

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Senior

Mutating external Redis caches synchronously inside an active relational database transaction leaves phantom, uncommitted data in Redis if the database transaction rolls back. External caches cannot be rolled back by Spring's `PlatformTransactionManager`. Defer cache evictions to `TransactionSynchronization.afterCommit()`.

**Appears in:** `modules/13-caching-redis/broken-examples/dual-write-consistency-ordering`

---

### Physical Wall-Clock Last-Write-Wins (LWW) Causing Lost Updates

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Senior

**Technology:** Distributed Systems, Multi-Region Replication, NTP · **Interview frequency:** High · **Production impact:** Critical

Using physical server timestamps (`System.currentTimeMillis()`) for Last-Write-Wins (LWW) conflict resolution across distributed replicas causes silent data loss. Quartz oscillators drift by milliseconds per day, and NTP synchronization across geographic regions has 5–50+ ms uncertainty bounds. A transaction executed later in real wall-clock time will be assigned an earlier timestamp if its node's clock runs slow, causing LWW to discard the fresh update. Replace physical LWW with Hybrid Logical Clocks (HLC) or single-master account affinity.

**Appears in:** `modules/17-distributed-systems/broken-examples/wall-clock-order-assumption`

---

### Retrying Non-Idempotent HTTP POST Causing Duplicate Mutations

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** HTTP, REST, Distributed Systems · **Interview frequency:** High · **Production impact:** High

Retrying mutating operations (`POST /charges`) upon encountering a network socket read timeout causes duplicate executions (such as double credit card charges). In distributed systems, a socket timeout indicates an unknown outcome: the downstream server may have successfully committed the transaction before the network connection dropped. Always enforce client-generated unique `Idempotency-Key` headers on mutating requests retried across network boundaries.

**Appears in:** `modules/18-resilience/broken-examples/retrying-non-idempotent-call`

---

### Non-Idempotent Saga Compensation (Duplicate Refunds on Redelivery)

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Distributed Sagas, Microservices · **Interview frequency:** High · **Production impact:** Critical

In distributed Sagas, compensating transactions (such as customer refunds or inventory unreservations) can be redelivered due to network timeouts or consumer rebalances. If compensation methods credit balances or undo state mutations without recording prior execution in a deduplication ledger, redeliveries cause catastrophic double payouts or negative inventory counts. Ensure all Saga compensations are strictly idempotent.

**Appears in:** `modules/19-distributed-data-patterns/broken-examples/non-idempotent-compensation`

---

### Consumer Missing Inbox Deduplication Table (Duplicate Processing)

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Kafka, Spring Kafka, Event-Driven Architecture · **Interview frequency:** High · **Production impact:** Critical

Because message brokers provide at-least-once delivery, consumer rebalances and container restarts cause events to be redelivered. Consumers that apply state mutations directly without verifying an atomic inbox store process identical events multiple times. Implement the Inbox Pattern using `INSERT ON CONFLICT DO NOTHING` in the same database transaction as the business state mutation.

**Appears in:** `modules/19-distributed-data-patterns/broken-examples/inbox-without-dedup-key`

## Related

- [Issue catalogue](index.md)
- [Distributed Systems topic documentation](../topics/distributed-systems/index.md)
