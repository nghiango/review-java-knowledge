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

---

### Premature AMQP basicAck (At-Most-Once Loss on Worker Crash)

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Basic

**Technology:** RabbitMQ, AMQP, Spring AMQP · **Interview frequency:** High · **Production impact:** Critical

Invoking `channel.basicAck()` before downstream operations (payment validation, database persistence) complete removes the message from RabbitMQ storage immediately. If the worker JVM crashes, runs out of memory, or encounters an unexpected exception, the message is permanently lost, degrading processing semantics to at-most-once delivery. Issue `basicAck()` strictly after all business operations and database writes succeed.

**Appears in:** `modules/15-rabbitmq/broken-examples/auto-ack-before-processing`

---

### Infinite Requeue Poison Pill Loop on basicNack

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** RabbitMQ, AMQP Error Handling · **Interview frequency:** High · **Production impact:** Critical

When a deterministic validation error occurs (malformed payload or invalid business identifier), invoking `channel.basicNack(deliveryTag, false, true)` immediately returns the message to the head of the queue. The consumer receives the same failing message in sub-millisecond loops, driving CPU utilization to 100% and blocking all healthy messages behind it. Route deterministic failures to a Dead Letter Exchange (DLX) via `basicReject(deliveryTag, false)` with bounded retries.

**Appears in:** `modules/15-rabbitmq/broken-examples/infinite-requeue-loop`

---

### Missing Dead-Letter Exchange (DLX) for Expired TTL and Rejected Messages

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** RabbitMQ, Dead-Letter Exchange · **Interview frequency:** High · **Production impact:** High

Declaring queue message expiration (`x-message-ttl`) or rejecting failed messages with `requeue = false` without configuring `x-dead-letter-exchange` and `x-dead-letter-routing-key` causes RabbitMQ to permanently discard expired and rejected messages. Critical events are lost with zero audit trail or recovery path. Always configure a dead-letter exchange and dedicated parking lot queue for expired and dead-lettered messages.

**Appears in:** `modules/15-rabbitmq/broken-examples/no-dlq-expired-ttl`

---

### Unbounded Prefetch (QoS = 0) Heap Starvation and OutOfMemoryError

**Type:** Resource management issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** RabbitMQ, basicQos, Prefetch · **Interview frequency:** High · **Production impact:** High

In AMQP, setting `prefetchCount = 0` configures unbounded prefetch, instructing the broker to push all queued messages into the consumer's socket buffer. For memory-intensive or long-running jobs (video transcoding, report generation), this causes consumer JVM memory exhaustion (`OutOfMemoryError`) while leaving other worker instances completely starved of work. Configure bounded prefetch (`basicQos(1)` for heavy tasks, `20-50` for fast transactional tasks).

**Appears in:** `modules/15-rabbitmq/broken-examples/unbounded-prefetch-oom`

---

### Non-Idempotent Consumer Processing on RabbitMQ Redeliveries

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** RabbitMQ, At-Least-Once Delivery · **Interview frequency:** High · **Production impact:** High

RabbitMQ guarantees at-least-once message delivery. If a consumer processes business state mutations successfully but encounters a network disconnect before `basicAck` is confirmed by the broker, RabbitMQ redelivers the message with `redelivered = true`. Non-idempotent consumers apply side effects multiple times (e.g. double-crediting balances). Pair at-least-once AMQP delivery with an atomic deduplication store or idempotent database operations.

**Appears in:** `modules/15-rabbitmq/broken-examples/non-idempotent-consumer`

---

### Fire-and-Forget Publishing Without Confirms or Returns Handling

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Senior

**Technology:** RabbitMQ, Publisher Confirms, ReturnsCallback · **Interview frequency:** High · **Production impact:** Critical

Using `rabbitTemplate.convertAndSend()` without publisher confirms or returns handling silently drops messages if the broker is unavailable, under memory/disk alarm, or if the routing key does not match any bound queues. Enable correlated publisher confirms (`spring.rabbitmq.publisher-confirm-type = correlated`), track `CorrelationData` confirmations with timeouts, and set `mandatory = true` with a `ReturnsCallback` to capture unroutable messages.

**Appears in:** `modules/15-rabbitmq/broken-examples/fire-and-forget-publisher`

## Related

- [Issue catalogue](index.md)
- [Kafka topic documentation](../topics/kafka/index.md)
- [RabbitMQ topic documentation](../topics/rabbitmq/index.md)
