# Messaging Issues

Delivery, acknowledgement, ordering, duplication and poison-message failures.

## Entries

### Premature Offset Acknowledgment (At-Most-Once Message Loss)

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Basic

**Technology:** Kafka, Spring Kafka · **Interview frequency:** High · **Production impact:** Critical

Acknowledging the Kafka message offset (`ack.acknowledge()`) before executing downstream business operations commits the consumer group's partition offset to the cluster. If the worker crashes, encounters an OOM, or restarts while executing downstream logic, the message is never redelivered, resulting in silent message loss. Always invoke `ack.acknowledge()` strictly after all transactional and downstream operations complete.

**Appears in:** `modules/14-kafka/broken-examples/ack-before-processing`

---

### Non-Idempotent Consumer Processing on Duplicate Delivery

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Kafka, Spring Kafka · **Interview frequency:** High · **Production impact:** High

Kafka provides at-least-once delivery guarantees. During broker leader re-elections, network timeouts on offset commits, or consumer group rebalances, identical messages are redelivered. Consumers that apply state mutations without checking an atomic deduplication store will execute actions multiple times (e.g. double-crediting balances or double-reserving inventory). Pair at-least-once delivery with an idempotent consumer backed by an atomic deduplication store.

**Appears in:** `modules/14-kafka/broken-examples/non-idempotent-consumer`

---

### Missing Partition Key Scattering Events (Assuming Global Ordering)

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Kafka, Partitioning · **Interview frequency:** High · **Production impact:** High

Kafka guarantees strict total ordering only within a single partition, never across multiple partitions of a topic. Publishing records without a message key causes the producer to scatter events across partitions. Multi-threaded consumers process partitions in parallel, causing lifecycle events (such as `USER_DELETED` before `USER_CREATED`) to be processed out-of-order. Supply the domain entity ID as the message key to enforce partition affinity and causal sequence.

**Appears in:** `modules/14-kafka/broken-examples/assuming-global-ordering`

---

### Infinite Retry Loop on Poison Pill Without Dead Letter Topic

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Kafka, Spring Kafka Error Handling · **Interview frequency:** High · **Production impact:** High

Configuring a consumer error handler with unlimited retry attempts and no Dead Letter Topic causes the consumer thread to loop infinitely on deterministic validation or deserialization errors. Because the partition offset cannot advance past the failing record, all subsequent healthy messages in that partition are blocked indefinitely. Use bounded retries with exponential backoff and a `DeadLetterPublishingRecoverer` to isolate poison pills to `.DLT`.

**Appears in:** `modules/14-kafka/broken-examples/infinite-retries-no-dlt`

---

### Blocking Listener Exceeding Max Poll Interval Triggering Rebalance Storm

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Senior

**Technology:** Kafka, Consumer Group Coordinator · **Interview frequency:** High · **Production impact:** High

Long-running CPU or I/O processing executing synchronously on the Kafka listener thread prevents the consumer from invoking `poll()` within `max.poll.interval.ms`. The group coordinator broker assumes the consumer is dead, revokes its partitions, and triggers a rebalance. When the thread finishes and tries to commit, Kafka throws `CommitFailedException`, and the new consumer reprocesses the same heavy task, sparking a cascading rebalance storm. Offload heavy processing to async worker pools.

**Appears in:** `modules/14-kafka/broken-examples/blocking-listener-poll-timeout`

---

### Dual-Write Hazard Publishing Inside Uncommitted Database Transaction

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Senior

**Technology:** Kafka, Spring Transactions · **Interview frequency:** High · **Production impact:** Critical

Invoking `kafkaTemplate.send()` directly inside a Spring `@Transactional` database method causes messages to be published before the database commits. If the database transaction subsequently fails or rolls back, the Kafka message cannot be recalled, causing downstream consumers to fulfill phantom orders. Decouple publishing via the Transactional Outbox pattern or defer publishing using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

**Appears in:** `modules/14-kafka/broken-examples/order-processing-v1`

## Related

- [Issue catalogue](index.md)
- [Kafka topic documentation](../topics/kafka/index.md)
