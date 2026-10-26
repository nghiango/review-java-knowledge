# Distributed Data Patterns — Practice Q&A

Senior-depth practice from interview-prep §7, §14, §15, and §18.
The supplied HyperScale design is a case study, not verified source for this lab.

### Q1. Why use an outbox instead of directly writing the DB and publishing?

??? question "Reveal answer"
    - **Persist business changes and event intent in one local transaction.**
    - **Mechanism:** the relay publishes committed intent later; a crash after publication but before marking completion permits duplicates rather than lost intent.
    - **Failure:** sequential DB/broker calls have a crash window whichever comes first. Awaiting a broker acknowledgment alone does not make the DB transaction atomic with Kafka.
    - **Fix:** write outbox rows with business state, publish with confirmation, and make consumers idempotent. Test crashes at every transition.
    - **Trade-off:** polling adds latency and table churn; CDC reduces polling but adds replication-slot, connector, retention, and recovery obligations.
    - **Follow-up:** What happens if the database runs out of disk during a prolonged broker outage?
    - **Takeaway:** Atomic intent plus recoverable delivery beats an unprotected dual write.

### Q2. How should a bounded outbox relay publish and acknowledge a batch?

??? question "Reveal answer"
    - **Claim safely, publish with confirmed outcomes, and mark only successfully owned work.**
    - **Mechanism:** a bounded claim selects due records; asynchronous sends complete independently; successful publication is persisted for cleanup while failures remain retryable.
    - **Failure:** one aggregate's failed earlier event can be overtaken; timeout callbacks may arrive after a batch is considered finished. In-memory success lists alone are not durable bookkeeping.
    - **Fix:** use claim tokens, bounded in-flight sends, per-event outcomes, and aggregate sequencing where required. Restore context per event and preserve interrupt/shutdown semantics.
    - **Trade-off:** smaller batches improve fairness and deadline control; larger batches amortize calls but increase recovery scope.
    - **Follow-up:** Can an obsolete worker mark a row completed after its lease was reassigned?
    - **Takeaway:** Relay state needs the same crash discipline as business state.

### Q3. Why can `FOR UPDATE SKIP LOCKED` still produce duplicate claims?

Merged from: §7 multi-replica relay + §14 gap 1

??? question "Reveal answer"
    - **Row locks last only until transaction end; a SELECT result is not a durable claim.**
    - **Mechanism:** autocommit releases selected locks before later network publication. Another worker can then select the same still-unpublished row.
    - **Fix:** atomically persist owner, lease expiry, and a claim generation in a short transaction, then publish outside it. Conditional renewal/completion must check the current generation.
    - **Failure:** a worker can publish after lease expiry, so leases reduce contention but do not eliminate duplicate delivery. Consumers must still deduplicate.
    - **Trade-off:** holding locks through broker I/O is simpler but occupies connections and delays vacuum/other work; leases add recovery complexity.
    - **Follow-up:** Why does fencing the completion UPDATE not stop an already-issued external send?
    - **Takeaway:** Locks protect transactions; durable claims coordinate longer workflows.

### Q4. SKIP LOCKED, advisory locks, or a leader relay?

Merged from: §7 lock alternatives + §15.1

??? question "Reveal answer"
    - **Choose coordination based on throughput, ordering, and failure recovery requirements.**
    - **Mechanism:** SKIP LOCKED partitions available rows among workers; advisory locks can be transaction- or session-scoped; leader ownership serializes publication.
    - **Failure:** pooled session locks outlive the intended operation unless carefully released. SKIP LOCKED does not guarantee global or per-aggregate publish order across workers.
    - **Fix:** use transaction-scoped coordination for short DB work; use partitioned aggregate ownership or explicit sequencing when order matters. Reconcile stale ownership after crashes.
    - **Trade-off:** a leader reduces concurrency and adds failover; parallel claims increase throughput but need ordering/dedup safeguards.
    - **Follow-up:** How would starvation of an old locked row be detected?
    - **Takeaway:** Work distribution and ordered delivery are different problems.

### Q5. What does CQRS gain, and how do you support read-your-writes?

Merged from: §7 CQRS + §15 read-model consistency

??? question "Reveal answer"
    - **Separate models where read needs justify projection and consistency costs.**
    - **Mechanism:** commands enforce invariants; events update a query-optimized representation. CQRS does not inherently require separate processes or event sourcing.
    - **Failure:** successful creation is followed by a missing read result; version guards reject old snapshots but cannot repair missing history by themselves.
    - **Fix:** return created state, expose pending/freshness status, or accept a version token and wait within a deadline. Routing to a physical primary does not make an async projection current.
    - **Trade-off:** independent scaling and simpler reads cost duplicate models, recovery workflows, and observable eventual consistency.
    - **Follow-up:** Which endpoints genuinely need a separate projection rather than an indexed relational query?
    - **Takeaway:** CQRS is a model choice with a user-visible freshness contract.

### Q6. How should a saga distinguish business rejection, transient failure, and unknown outcome?

??? question "Reveal answer"
    - **Use durable local transitions and semantic compensation, not imagined distributed rollback.**
    - **Mechanism:** each step commits locally and reliably emits its next intent. Stock rejection differs from a temporary broker outage or a timed-out payment with unknown status.
    - **Failure:** fire-and-forget failure publication followed by ack loses compensation; repeated compensation refunds twice. Simulated out-of-stock logic proves no real stock invariant.
    - **Fix:** outbox every required transition, make actions and compensations idempotent, guard legal state transitions, and reconcile stuck/unknown workflows.
    - **Trade-off:** compensation may fail or be impossible; irreversible steps need delayed execution, escrow, or explicit manual resolution.
    - **Follow-up:** What if cancellation races with a successful late reservation?
    - **Takeaway:** Model recovery as first-class business states.

### Q7. When should choreography become orchestration?

Merged from: §7 saga alternatives + §15 choreography follow-up

??? question "Reveal answer"
    - **Prefer explicit orchestration when workflow state, timers, and recovery dominate.**
    - **Mechanism:** choreography distributes event reactions; an orchestrator persists progress and issues commands while services retain local business invariants.
    - **Failure:** implicit event cycles make stuck flows hard to diagnose; an in-memory coordinator loses timers and progress on restart.
    - **Fix:** persist step state, correlation, deadlines, retry policy, and compensation ordering. Ensure the orchestrator itself recovers idempotently.
    - **Trade-off:** orchestration adds a central workflow dependency and deployment contract; choreography can remain appropriate for simple independent reactions.
    - **Follow-up:** Can the coordinator be highly available without executing a step twice?
    - **Takeaway:** Make ownership of workflow progress explicit.

### Q8. How do you achieve effectively-once business effects end to end?

Merged from: §7 exactly-once effects + §15 exactly-once follow-up

??? question "Reveal answer"
    - **Deduplicate every non-atomic boundary and define the retention scope of that guarantee.**
    - **Mechanism:** API request identity, business/outbox atomicity, transactional inbox plus business effect, and provider idempotency each protect a different hop.
    - **Failure:** producer idempotence does not deduplicate a relay's new send after restart; deleting inbox history before an old replay permits another effect.
    - **Fix:** use stable operation identity scoped by consumer/business action, atomic dedup writes, and replay-aware retention. Persist unknown external outcomes for reconciliation.
    - **Trade-off:** dedup costs storage; snapshot versioning may replace some dedup but is not safe for all increments, charges, or notifications.
    - **Follow-up:** What is promised after the API idempotency retention period expires?
    - **Takeaway:** Once-only effects are scoped application guarantees, not universal transport semantics.

### Q9. The outbox has 50 million rows: how do you recover without losing unpublished events?

Merged from: §15 outbox growth + §14 retention concerns

??? question "Reveal answer"
    - **Separate stuck unpublished backlog from retained published history before cleanup.**
    - **Diagnosis:** measure oldest unpublished age, arrival/drain rates, claim latency, dead tuples, index size, and long transactions blocking vacuum.
    - **Fix:** repair publication first; batch eligible published-row retention; tune table-specific vacuum and consider time partitioning with a verified completion/retention policy.
    - **Failure:** dropping an old partition by timestamp alone can delete unpublished work. Partition operations and global uniqueness constraints require deliberate schema design.
    - **Trade-off:** partitioning simplifies retention but adds migration and operational complexity; a partial unpublished index reduces search scope, not total table growth.
    - **Follow-up:** How do long-lived leases and delayed retries interact with retention?
    - **Takeaway:** Age is not proof that an event is disposable.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Kafka practice](../kafka/practice.md)
- [API idempotency practice](../rest-api/practice.md)
