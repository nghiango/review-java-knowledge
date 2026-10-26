# Kafka — Practice Q&A

Senior-depth practice from interview-prep §6, §15, §18, and §19.8.
Source topology, retry settings, and custom metric names are case-study context, not verified lab configuration.

### Q1. How do partitions, groups, offsets, and retention determine scalability?

??? question "Reveal answer"
    - **A group parallelizes partition ownership; retention permits independent replay.**
    - **Mechanism:** partitions are replicated ordered logs. In the conventional consumer-group model, each partition is assigned to one active group member; different groups read independently.
    - **Failure:** adding consumers beyond partition count gives no more partition parallelism; one hot key can dominate one partition despite idle peers.
    - **Fix:** choose meaningful keys, size partitions from measured per-partition throughput and recovery needs, and monitor skew. Distinguish committed position from current processing position.
    - **Trade-off:** more partitions increase metadata, replication, and recovery costs. Broker storage retention, not consumer acknowledgment, normally governs deletion.
    - **Follow-up:** How do you rebuild a projection after required history has expired?
    - **Takeaway:** Partition count is a workload and recovery decision.

### Q2. What can break ordering even when every event uses the order ID as key?

??? question "Reveal answer"
    - **Partition order does not guarantee end-to-end business processing order.**
    - **Mechanism:** the same key maps consistently only under compatible partitioning and stable partition count; separate topics have no shared ordering.
    - **Failure:** concurrent relays publish versions out of order; retry topics bypass failed records; async handlers complete later events first.
    - **Fix:** maintain per-aggregate sequencing or durable version checks with gap handling. A last-version-wins snapshot projection differs from an operation stream that must apply every delta.
    - **Trade-off:** strict serialization lowers throughput; version guards cannot reconstruct missing data or make noncommutative operations safe automatically.
    - **Follow-up:** What happens if version 3 arrives before version 2 for an incremental balance change?
    - **Takeaway:** Define whether order means publish, delivery, or completed effects.

### Q3. What does Kafka exactly-once cover when consumers write PostgreSQL?

Merged from: §6 delivery semantics + §15 Kafka EOS follow-up

??? question "Reveal answer"
    - **Kafka transactions do not atomically include an ordinary PostgreSQL transaction or HTTP call.**
    - **Mechanism:** Kafka transactional producers and offset commits support atomic Kafka read-process-write pipelines with read-committed consumers. Producer idempotence handles retry duplicates within its supported scope.
    - **Failure:** committing offsets first loses effects; committing DB first permits redelivery after a crash.
    - **Fix:** commit a dedup/inbox row and business changes in one DB transaction, then advance offsets. External effects require their own idempotency/reconciliation protocol.
    - **Trade-off:** dedup records need retention matching replay horizons; version guards suit snapshots but not every additive action.
    - **Follow-up:** Does a producer restart preserve application-level deduplication of an outbox event?
    - **Takeaway:** Exactly-once business effects require application boundaries, not a broker checkbox.

### Q4. What do replication factor 3, `acks=all`, and minimum ISR 2 guarantee?

??? question "Reveal answer"
    - **They trade write availability for replicated acknowledgment under stated failure assumptions.**
    - **Mechanism:** the leader waits for the in-sync replica set; minimum ISR rejects writes when too few in-sync replicas are available.
    - **Failure:** RF=3 does not mean three healthy replicas are always in ISR. Losing one broker may temporarily interrupt availability; correlated loss and unsafe leader election remain relevant.
    - **Fix:** require idempotent compatible producer settings, disable unsafe election where appropriate, spread failure domains, and test broker loss with realistic ISR changes.
    - **Trade-off:** an outbox buffers broker outages only until database capacity or retention limits are reached; it is not unlimited availability.
    - **Follow-up:** What does a producer timeout imply about whether the broker accepted the record?
    - **Takeaway:** State the failure model rather than promising absolute no-loss availability.

### Q5. When should retries block a partition versus use retry topics?

Merged from: §6 retries + §18.5 + §15 poison-message follow-up

??? question "Reveal answer"
    - **Short blocking retries preserve local order; retry topics trade ordering for progress.**
    - **Mechanism:** error handlers classify retryable failures, back off with a bounded schedule, then recover to a dead-letter topic after confirmed publication.
    - **Failure:** processing plus retries exceeds poll limits; malformed input retries forever; a failed DLT send is ignored while the original offset advances.
    - **Fix:** separate transient infrastructure failures from invalid payloads, validate recovery publication, and test acknowledgment/recovery configuration. Handle deserialization failures occurring before listener invocation too.
    - **Trade-off:** blocking retries cause head-of-line delay; skipping to DLT also breaks business continuity and may require aggregate quarantine.
    - **Follow-up:** Must a DLT have at least as many partitions when recovery preserves partition number?
    - **Takeaway:** Retry policy is also an ordering and recovery policy.

### Q6. How do you replay dead-letter records without creating a poison loop?

??? question "Reveal answer"
    - **Replay an audited, bounded selection after fixing the failure cause.**
    - **Mechanism:** preserve original identity, key, provenance, and retry history; publish with confirmation and maintain replay progress durably.
    - **Failure:** random replay groups starting at earliest resubmit the same entire backlog; unchanged source headers can reset a redrive count across repeated runs.
    - **Fix:** use a checkpointed replay job with dry-run, filters, a rate limit, durable attempt accounting, and idempotent destinations. Protect the admin action with application authorization and audit logs.
    - **Trade-off:** committing progress after publication permits duplicates on crash; before publication risks loss. Treat it like another at-least-once relay.
    - **Follow-up:** How do you prevent replay from duplicating payments or notifications?
    - **Takeaway:** Replay is a controlled migration, not a blind resend button.

### Q7. What causes rebalances, and how do poll budgets constrain handlers?

Merged from: §6 rebalances + §15 max.poll.interval follow-up

??? question "Reveal answer"
    - **Stable membership helps, but handlers must still make bounded progress.**
    - **Mechanism:** membership, subscription, and timeout changes trigger reassignment; classic client behavior depends on session and poll intervals plus assignor configuration.
    - **Failure:** batch size × worst-case processing plus retries exceeds the poll budget. Static membership can delay recovery instead of eliminating the problem.
    - **Fix:** reduce `max.poll.records`, bound handler/retry duration, stop gracefully, and use compatible cooperative assignment where applicable. Review the actual client/group protocol version.
    - **Trade-off:** longer timeouts tolerate pauses but slow failure recovery; scaling causes churn and cannot exceed useful partition parallelism.
    - **Follow-up:** Can a revoked worker still finish an external side effect?
    - **Takeaway:** Rebalance tuning cannot repair unbounded processing.

### Q8. Which consumer signals reveal true freshness and availability?

??? question "Reveal answer"
    - **Observe offset backlog, oldest unprocessed age, throughput, and consumer presence together.**
    - **Mechanism:** client lag measures assigned partitions while the client exists; broker-side/exporter views can reveal a dead group. Event timestamps help measure end-to-end processing age.
    - **Failure:** time since the last read-model update grows during healthy idle periods; producer clock skew distorts event-age metrics.
    - **Fix:** distinguish idle from stuck with arrival/output rates, expected assignments, last-success timestamps, and partition-level lag. Use synthetic probes for critical idle pipelines.
    - **Trade-off:** event age needs clock assumptions; raw event IDs belong in logs/traces, not metric labels.
    - **Follow-up:** Why can a low record count backlog still violate a freshness SLO?
    - **Takeaway:** Freshness is about waiting time, not just offset distance.

### Q9. How do event schemas evolve across independent deployments?

Merged from: §6 schema evolution + §15 contract follow-up

??? question "Reveal answer"
    - **Test old/new wire combinations and business meaning, not merely shared Java compilation.**
    - **Mechanism:** optional additive fields, defaults, and tolerant readers can support compatibility; breaking changes need explicit versioning or a new event contract.
    - **Failure:** adding an enum value breaks strict readers; a new default changes meaning; shared records compile but deployed old consumers still fail.
    - **Fix:** keep golden payloads, test compatibility in both directions, deploy tolerant consumers first when appropriate, and use registry/CI checks where adopted.
    - **Trade-off:** dual-write/read transitions add complexity; registry structural checks do not prove semantic compatibility.
    - **Follow-up:** How are historical retained events decoded after multiple upgrades?
    - **Takeaway:** The durable contract is the message on the wire.

### Q10. Why choose Kafka rather than RabbitMQ or a PostgreSQL queue?

Also relevant: [RabbitMQ](../rabbitmq/index.md)

??? question "Reveal answer"
    - **Choose a replayable log for independent stream consumers, not simply because messaging is required.**
    - **Mechanism:** Kafka retains partitioned streams; RabbitMQ classic/quorum work queues emphasize routing and acknowledgments; a DB queue can enqueue atomically with local writes.
    - **Failure:** treating all brokers as equivalent hides replay, ordering, latency, and operational differences. RabbitMQ Streams also support replay, so blanket 'RabbitMQ cannot replay' is inaccurate.
    - **Fix:** compare consumer fan-out, replay horizon, routing, delayed delivery, throughput, failure semantics, and team operations cost.
    - **Trade-off:** PostgreSQL queues fit moderate transactional workloads but compete with OLTP; Kafka brings partitioning and cluster management overhead.
    - **Follow-up:** Would a single worker and no replay requirement justify a Kafka cluster?
    - **Takeaway:** Select the abstraction that matches the workload.

### Q11. Why must acknowledgment happen after the DB transaction actually commits?

??? question "Reveal answer"
    - **A successful method body is not yet a successful transaction commit.**
    - **Mechanism:** a proxied service commits when its transactional invocation completes; a listener can acknowledge after that call returns. Manual immediate acknowledgment behavior depends on container/thread configuration.
    - **Failure:** ack inside a transactional listener can advance offsets before commit; commit failure then permanently skips the business effect.
    - **Fix:** separate the transactional inbox/business handler from offset acknowledgment, verify actual ack mode, and test failures during commit and after commit before ack.
    - **Trade-off:** commit-before-ack deliberately permits duplicate delivery, handled by a transactional dedup key.
    - **Follow-up:** What if two distinct consumer applications share the same event-ID dedup namespace?
    - **Takeaway:** Make commit ordering visible in both code and failure tests.

### Q12. How do you investigate steadily growing consumer lag?

??? question "Reveal answer"
    - **Separate arrival-rate overload, partition skew, poison records, and assignment churn.**
    - **Diagnosis:** compare per-partition input/output, repeated failing offsets, rebalances, handler timings, and downstream pool waits. Check broker ISR and fetch health too.
    - **Fix:** repair slow queries, cap batches/retries, recover poisoned records safely, and scale consumers only to useful partition parallelism.
    - **Trade-off:** adding partitions can remap keys; batch processing improves throughput but enlarges failure/replay scope.
    - **Verify:** demonstrate output exceeds input long enough to drain backlog within the freshness SLO, while duplicates remain harmless.
    - **Follow-up:** How long will recovery take at the measured net drain rate?
    - **Takeaway:** Restore sustainable processing, not merely a larger consumer count.

### Q13. How do you choose partition count and safely inspect or rewind a group?

Merged from: §15 partition sizing + §18.8

??? question "Reveal answer"
    - **Plan throughput and recovery headroom; treat offset changes as controlled operational changes.**
    - **Mechanism:** CLI descriptions show group assignments, lag, topic leaders, replicas, and ISR. Reading with a separate inspection group avoids advancing production group progress.
    - **Failure:** rewind re-executes effects; adding partitions changes key mapping and can reorder an aggregate across old/new partitions.
    - **Fix:** check retention, record current offsets, stop or otherwise safely coordinate consumers, dry-run offset changes, and obtain operational approval before execution. Authorize message inspection because payloads may contain personal data.
    - **Trade-off:** too few partitions constrain growth; too many impose recovery and management cost.
    - **Follow-up:** How would a blue/green projection avoid rewinding the live group?
    - **Takeaway:** Partition and offset management are correctness-sensitive operations.

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Outbox and inbox practice](../distributed-data-patterns/practice.md)
