# System Design — Practice Q&A

Senior-depth scenarios from interview-prep §8 and §13. Treat the commerce system as a design case,
not proof of deployed lab capabilities or achieved scale.

### Q1. Walk through order creation end to end, including failures at each boundary.

??? question "Reveal answer"
    - **Separate durable acceptance from asynchronous fulfillment and read visibility.**
    - **Mechanism:** authenticate/authorize, validate and admit, reserve scoped idempotency, commit order/result/outbox together, then publish and process deduplicated local transitions; projections and caches update later.
    - **Failure:** a lost response leaves the client uncertain; Kafka failure queues intent; a poison event blocks fulfillment; Redis failure increases source demand.
    - **Fix:** replay committed results on safe retries, expose pending status, make every required async transition recoverable, and reconcile stuck workflows through stable identities.
    - **Trade-off:** fast acceptance is not a completed purchase; define precisely whether 201 or 202 communicates the promised resource state.
    - **Follow-up:** What does the client see when inventory ultimately rejects an accepted order?
    - **Takeaway:** Give each acknowledgment a precise business meaning.

### Q2. How would you handle a flash sale with 100× demand on one product?

??? question "Reveal answer"
    - **Protect admission and enforce stock atomically before optimizing throughput.**
    - **Mechanism:** cache descriptive reads; issue bounded checkout access; use positive-quantity conditional decrements or serialized reservations with unique operation IDs and expiry.
    - **Failure:** SELECT-then-UPDATE oversells; Redis-only admission can diverge from durable stock; partitioned buckets can strand stock or permit double allocation during refill.
    - **Fix:** define the authoritative stock ledger, durable reservation transitions, payment outcome handling, and reconciliation. Bound queues and per-user attempts; load-test hot-key skew, not uniform products.
    - **Trade-off:** sharded stock and async confirmation increase throughput but require allocation/transfer rules and a pending UX. Cache hits do not solve write contention.
    - **Follow-up:** How does expiry racing with payment success avoid releasing purchased stock?
    - **Takeaway:** Stock correctness, fairness, and throughput need separate protocols.

### Q3. How would payment join the order saga safely?

??? question "Reveal answer"
    - **Persist payment intent and reconcile ambiguous outcomes before compensating.**
    - **Mechanism:** use a stable provider idempotency identity, durable saga states, outbox transitions, authenticated callbacks, and deduplicated webhook handling.
    - **Failure:** a timed-out charge may have succeeded; retrying with a new identity charges twice; refund racing late success creates inconsistent order state.
    - **Fix:** query/reconcile provider status, distinguish authorize/capture/refund states, apply legal versioned transitions, and route unresolved cases to controlled operational handling.
    - **Trade-off:** orchestration simplifies timers and recovery visibility but adds a workflow owner. Provider idempotency retention may be shorter than your replay horizon.
    - **Follow-up:** When is the order allowed to ship, and what happens if capture later fails?
    - **Takeaway:** Unknown payment status is a real state, not failure by default.

### Q4. How do you rebuild a projection while live traffic continues?

??? question "Reveal answer"
    - **Build a shadow model from a consistent baseline plus a defined change-stream boundary.**
    - **Mechanism:** a new group/model replays retained history or applies a snapshot followed by changes; idempotency and versions handle overlap.
    - **Failure:** a table dump with no source watermark misses or duplicates changes; retention may omit creation events; historical replay can retrigger external actions if handlers are not isolated.
    - **Fix:** record snapshot/stream positions, handle deletions, compare invariants and sampled rows, catch up, switch reads atomically, and retain a rollback model.
    - **Trade-off:** dual projection increases broker/DB load; zero lag alone does not prove semantic equivalence.
    - **Follow-up:** How do you verify a correction when the old model is known to be wrong?
    - **Takeaway:** A rebuild is a consistency migration with a cutover protocol.

### Q5. What changes when an order platform becomes multi-region?

??? question "Reveal answer"
    - **Choose ownership, failover, and conflict semantics before choosing replication tools.**
    - **Mechanism:** active-passive accepts failover delay and potentially lost unreplicated writes; active-active often assigns authoritative home regions or partitions ownership.
    - **Failure:** both regions accept stock/payment operations during a partition; failback replays duplicate intent; region-local caches retain obsolete ownership.
    - **Fix:** fence ownership transitions, scope identities and event provenance globally, test failover/reconciliation, and define which operations stop during partitions.
    - **Trade-off:** cross-region synchronization adds distance-dependent latency; it is not universally impossible within 200 ms, but geography and workload must be measured. LWW/CRDTs do not automatically preserve financial invariants.
    - **Follow-up:** What happens to an acknowledged order absent from the promoted region?
    - **Takeaway:** Multi-region is primarily a data ownership and recovery decision.

### Q6. How do you scale writes beyond a single PostgreSQL primary?

??? question "Reveal answer"
    - **Remove unnecessary work and hot spots before accepting distributed data complexity.**
    - **Mechanism:** indexes, batching, short transactions, efficient queries, and read offload improve the existing primary; vertical scale, context separation, and sharding address different limits.
    - **Failure:** table partitioning does not by itself distribute writes across machines; read replicas do not scale writes; an unshardable hot key remains hot.
    - **Fix:** identify the saturated resource, choose a shard key from transaction locality, and plan resharding, unique identity, routing, cross-shard queries, and recovery before migration.
    - **Trade-off:** independent databases reduce shared contention but remove simple joins and transactions; sagas add partial-failure handling rather than replacing every invariant transparently.
    - **Follow-up:** How is a globally unique business constraint enforced across shards?
    - **Takeaway:** Shard only against a demonstrated limit and an explicit ownership model.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Solutions](solutions.md)
- [Architecture practice](../architecture/practice.md)
- [Distributed data practice](../distributed-data-patterns/practice.md)
