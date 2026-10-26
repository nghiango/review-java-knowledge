# Caching / Redis — Practice Q&A

Senior-depth practice from interview-prep §5, §15, and §17.6.
The source describes a commerce case study; its custom classes, TTLs, and benchmark numbers are not verified lab facts.

### Q1. How should a two-tier Caffeine and Redis read path work?

??? question "Reveal answer"
    - **Use L1 for local hot data and L2 for cross-pod reuse, with explicit freshness and failure budgets.**
    - **Mechanism:** L1 miss checks L2, then the source of truth; successful loads populate tiers with bounded sizes and TTLs.
    - **Failure:** promotion can extend stale data's lifetime; per-pod caches diverge; JSON decoding or large values dominate nominal cache speed.
    - **Fix:** include tenant, schema version, and relevant query parameters in keys; retain source versions or expiry metadata when promoting entries.
    - **Verify/trade-off:** measure total request latency, per-tier hit rates, load concurrency, retained bytes, and stale-read frequency. L1 speed costs coherence complexity.
    - **Follow-up:** Can L1 continue serving safely when L2 is unavailable?
    - **Takeaway:** A cache hierarchy needs a consistency contract, not just lookup order.

### Q2. Cache-aside, read-through, write-through, or write-behind?

??? question "Reveal answer"
    - **Choose by acceptable staleness, write durability, and ownership of the loading policy.**
    - **Mechanism:** cache-aside is application-managed; read-through delegates loading; write-through synchronously persists; write-behind defers persistence.
    - **Failure:** synchronous DB/cache writes are not automatically atomic; write-behind can lose accepted writes without a durable journal.
    - **Fix:** make the DB authoritative for orders, commit first, then invalidate through a recoverable path. Use refresh-ahead only with bounded loaders and a documented stale window.
    - **Trade-off:** invalidation is idempotent but still races; updating can work with version checks. Neither is universally consistent without a protocol.
    - **Follow-up:** What durability does a successful cache write actually acknowledge?
    - **Takeaway:** Cache policy must preserve the business write guarantee.

### Q3. How do you prevent a hot-key stampede?

??? question "Reveal answer"
    - **Coalesce loads, spread expirations, and cap total source concurrency.**
    - **Mechanism:** Caffeine atomic loading coalesces one key within one cache instance; N pods can still issue N loads. Jitter spreads unrelated expirations.
    - **Failure:** distributed lock expiry permits overlapping loaders; a loader returning nothing may trigger repeated misses. Unbounded waiting turns stampedes into request pileups.
    - **Fix:** use local single-flight, bounded refresh-ahead or stale-while-revalidate where allowed, and admission limits. If distributed coordination is needed, use ownership-safe release and bounded leases.
    - **Trade-off:** stale serving protects availability but is inappropriate for inventory decisions or authorization.
    - **Follow-up:** How will a slow old loader be prevented from overwriting a newer value?
    - **Takeaway:** Coordinate loads without making a lock the next outage source.

### Q4. How do penetration, hot-key breakdown, and avalanche differ?

??? question "Reveal answer"
    - **They stress the source differently: absent keys, one expired hot key, or mass misses.**
    - **Mechanism:** absent results may never be cached; synchronized expirations and tier outages suddenly restore all DB demand.
    - **Failure:** unlimited negative-cache entries create their own memory attack; Bloom filters can falsely reject newly created keys if maintenance is wrong.
    - **Fix:** bounded short-lived negative entries, validation, single-flight, jitter, circuit breakers, and source admission limits. Rate-limit using trustworthy identity.
    - **Verify/trade-off:** load-test random nonexistent keys, hot-key expiry, and total Redis loss separately. Negative TTL trades DB protection for delayed visibility of creation.
    - **Follow-up:** How are negative entries invalidated when a new object appears?
    - **Takeaway:** Protect the source against both ordinary misses and adversarial cardinality.

### Q5. How do you invalidate every pod without confusing Kafka groups with broadcast?

??? question "Reveal answer"
    - **Every independent L1 cache must receive invalidations or have a bounded recovery strategy.**
    - **Mechanism:** one Kafka group shares partitions among members; separate groups each receive the stream. Publish invalidation after the source commit through durable intent if necessary.
    - **Failure:** one shared group invalidates only some pods; random new groups with unsuitable initial offsets miss startup events. Offline pods and lost publications leave stale data.
    - **Fix:** define group lifecycle, start position, startup clearing, TTL fallback, and replay behavior. Ensure invalidation does not recreate expired data with a new freshness clock.
    - **Trade-off:** per-pod groups multiply broker overhead; version checks or namespace epochs can reduce fan-out dependence.
    - **Follow-up:** What happens if a pod disconnects longer than topic retention?
    - **Takeaway:** Invalidation delivery is part of the coherence protocol.

### Q6. Why does commit-then-delete still permit stale cache entries?

Merged from: §5 invalidation race + §15 invalidation follow-up

??? question "Reveal answer"
    - **A reader can repopulate old state after the writer's invalidation.**
    - **Mechanism:** the reader loads version 1, the writer commits version 2 and deletes, then the reader stores version 1; replicas widen this window.
    - **Failure:** double-delete and repeated broadcasts reduce probability but do not prove correctness. CDC ordering alone does not order concurrent cache fills.
    - **Fix:** validate versions against retained watermarks/tombstones, use generation tokens, or route strict reads to the authority. After-commit invalidation plus TTL may suffice only for an explicit eventual-consistency contract.
    - **Verify/trade-off:** test the interleaving with barriers; stronger coordination increases writes, storage, and latency.
    - **Follow-up:** If deletion removes the version, what stops an old fill from winning afterward?
    - **Takeaway:** A delete is not a fence against in-flight readers.

### Q7. How should Redis failure degrade without taking down the database?

Merged from: §5 outage + §15 Redis-down follow-up

??? question "Reveal answer"
    - **Fail open only when the source can absorb bounded fallback demand.**
    - **Mechanism:** L1 continues serving; L2 failures bypass Redis after short deadline-aware timeouts and a circuit breaker opens.
    - **Failure:** one second of Redis waiting already violates a 200 ms budget; connect/read/pool/retry costs can exceed any one configured timeout.
    - **Fix:** apply total budgets, bulkhead fallback DB reads, coalesce loads, and shed low-priority work. Use stale values only for permitted data.
    - **Trade-off:** Redis HA reduces outages but does not remove the need for cold-cache qualification or circuit-breaker recovery controls.
    - **Follow-up:** How do half-open probes avoid a second load spike?
    - **Takeaway:** Optional caches must be optional under actual failure load.

### Q8. Why is `KEYS prefix*` unsafe for online invalidation?

??? question "Reveal answer"
    - **It scans the keyspace synchronously and can stall unrelated operations.**
    - **Mechanism:** Redis command execution shares critical server resources; large blocking commands and values increase tail latency for all clients.
    - **Failure:** SCAN-based deletion is not an atomic snapshot and may miss concurrently added keys; cluster scanning must cover relevant nodes.
    - **Fix:** use incremental SCAN with bounded batches and UNLINK, or versioned namespaces with TTL reclamation and a well-defined epoch-change protocol.
    - **Trade-off:** namespace changes leave old data consuming memory until expiry; UNLINK moves reclamation off the command path, not out of the memory budget.
    - **Follow-up:** How do writers learn the current namespace without racing a reset?
    - **Takeaway:** Operationally cheap invalidation still needs concurrency semantics.

### Q9. Why does Caffeine outperform a simple LRU under scan-heavy load?

??? question "Reveal answer"
    - **Admission policy prevents one-hit scans from evicting the useful working set.**
    - **Mechanism:** Window TinyLFU combines a recency window, frequency estimation, and a main eviction region; it is not pure LRU.
    - **Failure:** labeling every size eviction as LRU misleads diagnosis. Entry count also ignores variable-sized payloads.
    - **Fix:** choose size or weight bounds appropriate to values; measure hit rates and load cost with realistic skew rather than synthetic uniform access alone.
    - **Trade-off:** approximate frequency saves memory but is not exact; workload changes and large entries still need capacity analysis.
    - **Follow-up:** Why can a high hit rate coexist with poor latency?
    - **Takeaway:** Optimize avoided cost, not only hit count.

### Q10. Which cache metrics demonstrate value and reveal harm?

??? question "Reveal answer"
    - **Measure load avoided, user latency, freshness, and failure amplification together.**
    - **Mechanism:** per-tier hits/misses, loader latency, concurrent loads, failures, eviction causes, and invalidation delay explain the read path.
    - **Failure:** a global hit ratio hides expensive misses; idle time since the last event is not invalidation lag. Raw keys as metric labels explode cardinality.
    - **Fix:** use bounded cache/level/outcome labels; compare equivalent warm and cold workloads; track source QPS and latency alongside stale-read checks.
    - **Trade-off:** the source case study's benchmark is illustrative, not a verified lab result or proof of other endpoints' capacity.
    - **Follow-up:** Does your miss denominator count requests or actual coalesced loads?
    - **Takeaway:** Prove the cache helps the service, not merely itself.

### Q11. Which data should not use ordinary stale cache-aside reads?

??? question "Reveal answer"
    - **Avoid stale caching where incorrect decisions have financial, security, or legal consequences.**
    - **Mechanism:** cache-aside commonly offers eventual freshness, not linearizable read-your-writes. Cross-tenant keys and mutable values introduce additional correctness risks.
    - **Failure:** cached stock authorizes overselling; revoked access remains usable; global keys expose another customer's order.
    - **Fix:** keep authoritative decisions on transactional state, enforce authorization independently, scope keys, and document invalidation plus TTL guarantees.
    - **Trade-off:** immutable reference data is often ideal; user-specific data may still be cacheable with correct scoping and low reuse considered.
    - **Follow-up:** Can a cached price be displayed while checkout uses a versioned authoritative quote?
    - **Takeaway:** Cache representations, not unvalidated authority.

### Q12. How would you implement and review a small access-order LRU?

??? question "Reveal answer"
    - **An access-order `LinkedHashMap` can demonstrate LRU, but is not a production cache by itself.**
    - **Mechanism:** access moves an entry in order; `removeEldestEntry` can enforce a positive capacity after insertion.
    - **Failure:** even reads can change access order, so concurrent `get` is not automatically safe. Iteration and compound loads need coordinated locking.
    - **Fix:** validate capacity, synchronize the complete protocol, and test eviction order and concurrency; prefer Caffeine for bounded loading, expiry, and metrics.
    - **Trade-off:** a synchronized wrapper serializes operations and does not automatically make an external get-then-load sequence atomic.
    - **Follow-up:** How would two simultaneous misses avoid duplicate expensive loads?
    - **Takeaway:** Demonstrating eviction is simpler than engineering a cache.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Resilience practice](../resilience/practice.md)
