# Database / SQL — Practice Q&A

Senior-depth practice from interview-prep §8, §10, §14, §15, §18.7, and §19.6.
The source's schemas and pool sizes are case-study assumptions, not this lab's deployed configuration.

### Q1. What do ACID guarantees depend on in a real PostgreSQL deployment?

??? question "Reveal answer"
    - **Transactions protect local invariants only within the chosen isolation and durability configuration.**
    - **Mechanism:** atomicity groups changes; constraints preserve declared consistency; isolation governs concurrency; WAL and flush/replication policy govern durability.
    - **Failure:** ACID does not make a DB write plus an HTTP call atomic. Application-only checks can race; relaxed persistence settings change crash guarantees.
    - **Fix:** encode invariants in constraints or serialized operations, use an outbox for external intent, and qualify crash/failover behavior against actual settings.
    - **Trade-off:** stronger synchronization adds latency and can reduce availability. Backups address other failures that replication cannot repair.
    - **Follow-up:** What acknowledged failures are excluded by your RPO statement?
    - **Takeaway:** State the scope and assumptions behind each guarantee.

### Q2. Which anomalies do PostgreSQL isolation levels permit?

??? question "Reveal answer"
    - **READ COMMITTED, snapshot isolation, and SERIALIZABLE solve different concurrency problems.**
    - **Mechanism:** READ COMMITTED takes statement snapshots; PostgreSQL REPEATABLE READ uses a transaction snapshot and prevents ordinary phantoms but allows write skew. SERIALIZABLE uses SSI and may abort conflicting transactions.
    - **Failure:** two transactions check a multi-row rule and update different rows, violating it. SELECT-then-INSERT also races without a constraint.
    - **Fix:** use atomic predicates/constraints, lock a shared invariant owner, or use SERIALIZABLE and retry the entire transaction with bounded jitter on serialization failure.
    - **Trade-off:** stronger isolation adds abort/retry cost; READ COMMITTED is not automatically safe or unsafe for every workflow.
    - **Follow-up:** Why is locking only currently existing rows insufficient for some insert invariants?
    - **Takeaway:** Choose isolation from an explicit concurrent history.

### Q3. Why do MVCC, long transactions, and outbox churn cause bloat?

??? question "Reveal answer"
    - **Old tuple versions remain until no required snapshot needs them and vacuum can reclaim them.**
    - **Mechanism:** updates create versions; vacuum reclaims dead tuples and freezes old transaction IDs. HOT updates avoid some index work only when eligibility and page space allow.
    - **Failure:** long snapshots retain dead rows; changing an indexed predicate such as unpublished status creates index churn. 'Readers never block writers' ignores explicit locks and DDL.
    - **Fix:** shorten transactions, inspect oldest snapshots and replication retention, tune autovacuum per hot table, and separate retention from live backlog.
    - **Trade-off:** partitioning helps lifecycle management but not all update churn; routine VACUUM normally reuses space rather than returning all disk to the OS.
    - **Follow-up:** Why can an idle-in-transaction session hurt a different workload?
    - **Takeaway:** Transaction lifetime is a storage and availability concern.

### Q4. Optimistic versioning or pessimistic locking under contention?

??? question "Reveal answer"
    - **Choose conflict detection or exclusive ownership according to conflict cost and duration.**
    - **Mechanism:** version predicates detect stale updates through affected-row counts; row locks serialize contenders until commit. NOWAIT and SKIP LOCKED change waiting behavior, not business semantics.
    - **Failure:** unbounded optimistic retries amplify a hot-key storm; locks held during user think-time or HTTP calls exhaust resources.
    - **Fix:** make conditional writes explicit, bound retries, shorten lock scopes, and enforce a consistent order for multi-row operations.
    - **Trade-off:** a monotonic incoming-version projection is not the same as compare-and-set; equal-version payload changes need a defined conflict policy.
    - **Follow-up:** Should a failed version check return conflict or transparently retry?
    - **Takeaway:** Contention policy belongs in the domain contract.

### Q5. How do you review indexes against actual query shapes?

??? question "Reveal answer"
    - **Match filters, ordering, selectivity, and write cost to measured plans.**
    - **Mechanism:** B-trees support ordered access; composite order, partial predicates, expressions, and INCLUDE columns change usable paths. GIN/GiST/BRIN serve different shapes.
    - **Failure:** a duplicate index adds writes; PostgreSQL does not automatically index referencing FK columns. Leading-wildcard search usually needs trigram/full-text support rather than an ordinary name B-tree.
    - **Fix:** inspect plans and index usage, include stable pagination ordering, and validate changes against realistic cardinality. Do not delete an apparently redundant index without checking constraints and workload.
    - **Trade-off:** index-only scans still depend on visibility information; additional indexes consume WAL, cache, disk, and maintenance time.
    - **Follow-up:** When can a sequential scan be the correct plan?
    - **Takeaway:** Indexes are workload-specific access paths, not universal speed switches.

### Q6. How do you investigate a slow query safely?

??? question "Reveal answer"
    - **Start with frequency, waits, and plan estimates before changing configuration.**
    - **Mechanism:** `pg_stat_statements` identifies total-cost queries; EXPLAIN shows estimates, and ANALYZE executes to report actual rows/timing. BUFFERS exposes cache and I/O behavior.
    - **Failure:** executing EXPLAIN ANALYZE on a mutating query performs mutations; even rolled-back statements can have nontransactional external effects. Staging plans can differ from production data.
    - **Fix:** inspect estimates, row multiplication, lock waits, sort spills, and stale/correlated statistics; test query/index changes with a controlled budget and safe data.
    - **Trade-off:** raising work_mem multiplies memory across operations and sessions; a per-query improvement can reduce overall capacity.
    - **Follow-up:** Is the bottleneck query execution, pool acquisition, or a blocked transaction?
    - **Takeaway:** Diagnose the wait and plan before tuning knobs.

### Q7. Why can the connection pool exhaust while database CPU is low?

Merged from: §10 Hikari + §8 connection budget + §14 pool gap + §19.6

??? question "Reveal answer"
    - **Connections can be held while threads wait on locks or external work rather than execute SQL.**
    - **Diagnosis:** compare acquisition latency/pending, transaction age, wait events, idle-in-transaction sessions, leaks, nested REQUIRES_NEW calls, and request thread stacks.
    - **Fix:** shorten transactions, move remote work outside them with a reliable workflow, bound statement/lock/acquisition time, and enforce admission before saturation. Session termination is a controlled last resort, not an automatic remedy.
    - **Capacity:** budget each pool against its physical target across replicas, rollout surge, jobs, failover, and reserved DB capacity. Do not add primary and replica counts as if they all hit one server.
    - **Trade-off:** more connections may worsen contention; rules like cores × 2 are starting heuristics, not universal per-pod recommendations. Pooler mode can constrain session features.
    - **Follow-up:** What happens to the connection budget when every read falls back to the primary?
    - **Takeaway:** Size and observe the complete resource graph.

### Q8. What consistency guarantees can replica routing really provide?

??? question "Reveal answer"
    - **A sampled lag threshold is a heuristic, not a strict staleness bound.**
    - **Mechanism:** routing chooses a primary or replica at connection acquisition; asynchronous apply can lag after sampling. Receive LSN equality with replay LSN does not prove the primary has no unreceived WAL.
    - **Failure:** read-your-writes and monotonic reads fail across replicas; old healthy samples keep routing to a stalled replica. Time since last replay is misleading during idle periods.
    - **Fix:** expire samples, measure primary-relative replay positions, use commit-position tokens and bounded waits where available, or use the primary for strict reads.
    - **Trade-off:** time-based primary stickiness is approximate; strict freshness may cost availability or latency. Cached data and CQRS projections need independent freshness handling.
    - **Follow-up:** What does fallback do to primary load during replica failure?
    - **Takeaway:** Routing health is not proof of read consistency.

### Q9. How do PostgreSQL HA, fencing, backups, RPO, and RTO relate?

Merged from: §10 HA + §15 failover and split-brain follow-ups

??? question "Reveal answer"
    - **HA reduces outage duration; durable quorum choices and backups address different loss modes.**
    - **Mechanism:** a leader protocol coordinates promotion; fencing/watchdog measures prevent an old primary accepting writes. Synchronous commit guarantees depend on exact flush/apply and failover eligibility settings.
    - **Failure:** a timed-out commit may have succeeded; retries without stable identity duplicate effects. Replication propagates accidental deletion and is not a backup.
    - **Fix:** reconnect through verified role-aware endpoints, invalidate stale pool connections, reconcile unknown outcomes, test fencing, and regularly restore backups plus WAL to a known point.
    - **Trade-off:** strict synchronous policies can block writes without a qualifying standby. RPO and RTO require measured failure assumptions, not an unconditional zero-loss claim.
    - **Follow-up:** What happens during loss of the coordination quorum but not the database network?
    - **Takeaway:** Test promotion, exclusion of the old leader, and restore separately.

### Q10. How do zero-downtime schema migrations remain safe for old and new code?

??? question "Reveal answer"
    - **Expand, migrate, switch, and contract across compatible releases.**
    - **Mechanism:** add compatible structures, backfill in bounded batches, switch readers/writers, then remove old structures only after the rollback window closes.
    - **Failure:** long lock queues block traffic; a fast metadata change can still need a conflicting lock. Renames or incompatible type changes break old pods.
    - **Fix:** set appropriate lock/statement budgets, rehearse on realistic tables, and separate concurrent index creation from transactional migrations as required. Check for invalid indexes after failure.
    - **Trade-off:** dual reads/writes and backfills add temporary complexity; rollback of application code does not roll back data safely.
    - **Follow-up:** How do you verify a backfill while new writes continue?
    - **Takeaway:** Migration safety includes locks, compatibility, and recovery.

### Q11. When is JSONB appropriate compared with relational columns?

??? question "Reveal answer"
    - **Use it for document-shaped data when flexibility outweighs relational constraints and access-path costs.**
    - **Mechanism:** JSONB supports structured storage and specialized operators/indexes; large values may be compressed or stored out of line through TOAST.
    - **Failure:** unbounded documents cause large reads/rewrites; arbitrary shapes weaken validation. Adding GIN everywhere can increase write costs without helping actual predicates.
    - **Fix:** keep identities, ownership, ordering, and common filters in typed columns; validate documents and index only measured paths. Version event payloads explicitly.
    - **Trade-off:** a read-model document can avoid joins but requires reliable projection/rebuild logic. PostgreSQL can query JSONB efficiently; it is not limited to whole-document reads.
    - **Follow-up:** How would a frequently updated nested field affect storage and WAL volume?
    - **Takeaway:** Flexible storage still needs a schema contract.

### Q12. What should you explain while writing aggregation, pagination, upsert, stock, and queue SQL?

??? question "Reveal answer"
    - **Explain the invariant, ordering, index, and transaction scope—not only valid syntax.**
    - **Mechanism:** GROUP BY/HAVING detects duplicates; `row_number` with partition/order gives top N per group; tuple cursor comparisons support keyset scans; conditional UPDATE prevents oversell when positive quantity is validated.
    - **Failure:** keyset pagination is not universally O(log n); cost includes returned rows and filters. A version upsert accepting equal versions can rewrite side effects; SKIP LOCKED requires a surviving claim transaction/protocol.
    - **Fix:** require deterministic tie-breakers and matching indexes, check affected rows, validate inputs, and apply only domain-valid version transitions.
    - **Trade-off:** cursors handle deep pages well but do not provide arbitrary page numbers or snapshot consistency during concurrent mutation.
    - **Follow-up:** Why can a newer snapshot safely skip versions while an inventory delta cannot?
    - **Takeaway:** SQL review is concurrency and data-contract review.

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Transaction practice](../spring-transactions/practice.md)
