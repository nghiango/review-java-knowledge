# Kafka Solutions

Production-grade implementations corresponding to the code review exercises.

## Safe manual offset acknowledgment

### Implementation

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/manualack/PaymentNotification.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/manualack/SafePaymentNotificationConsumer.java"
```

### Why it works

1. **Post-Processing Offset Commit**: `ack.acknowledge()` is invoked strictly **after** the external payment gateway confirmation and database persistence operations complete without error.
2. **At-Least-Once Delivery**: If the consumer encounters a network error, database constraint violation, or sudden process crash mid-flight, the acknowledgment line is never reached. On restart, Kafka redelivers the unacknowledged offset to an available consumer instance.
3. **Explicit Error Propagation**: Exceptions are wrapped and rethrown rather than swallowed, allowing Spring Kafka's container error handler to trigger retries and alerts.

### Trade-offs

- **Redelivery Potential**: In the event of a worker crash immediately after the database write but before `ack.acknowledge()`, the message will be reprocessed on restart. The downstream business logic must therefore be idempotent.

---

## Idempotent consumer with deduplication store

### Implementation

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/idempotent/CreditCommand.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/idempotent/DeduplicationStore.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/idempotent/SafeAccountCreditConsumer.java"
```

### Why it works

1. **Atomic Deduplication Check**: `deduplicationStore.tryMarkProcessed(command.transactionId())` performs an atomic test-and-set check (e.g. `INSERT INTO processed_events` or Redis `SETNX`).
2. **Safe Short-Circuiting**: If the transaction ID is already marked as processed, the listener acknowledges the message and returns immediately without applying the balance mutation a second time.
3. **Audit Trail**: Every unique credit execution logs an audit entry, providing full traceability for financial operations.

### Trade-offs

- **Storage Overhead**: Tracking processed IDs requires persistent storage space and periodic cleanup (e.g. partition drop or TTL expiration).
- **Secondary Query Latency**: Adds a database round-trip or cache lookup to every incoming message.

---

## Partition key ordering guarantees

### Implementation

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/ordering/UserEvent.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/ordering/SafeUserEventProducer.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/ordering/SafeUserActivityConsumer.java"
```

### Why it works

1. **Explicit Key Routing**: `kafkaTemplate.send(TOPIC, userId, event)` supplies `userId` as the partition key. Kafka's default partitioner hashes the key using Murmur2, ensuring all events for the same user land in the identical partition.
2. **Sequential Invariant Preserved**: Because Kafka guarantees total order within a single partition, `USER_CREATED`, `USER_UPDATED`, and `USER_DELETED` are written and consumed in strict causal order.
3. **Safe Consumer Concurrency**: Even when consumer concurrency is scaled across multiple partitions, a single partition is processed sequentially by exactly one consumer thread.

### Trade-offs

- **Key Cardinality & Skew**: If a few "celebrity" user IDs have millions of events while others have few, the partition assigned to the celebrity user can become hot (partition skew).

---

## Bounded retries with Dead Letter Topic routing

### Implementation

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/dlt/FulfillmentPayload.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/dlt/SafeOrderFulfillmentConsumer.java"
```

### Why it works

1. **Bounded Retries**: Configures a maximum retry limit (e.g. 3 attempts with exponential backoff) instead of infinite loops.
2. **Fast-Fail on Poison Pills**: Fatal, deterministic exceptions (e.g. `IllegalArgumentException`) are classified as non-retryable and routed immediately to the DLT without wasting retry attempts.
3. **Dead Letter Recovery**: Exhausted or corrupted records are published to `order-fulfillments.DLT` along with diagnostic headers (`kafka_exception-message`), allowing partition offsets to advance while preserving failed events for inspection.

### Trade-offs

- **Out-of-Order DLT Processing**: When a poisoned record is routed to the DLT, subsequent records in the partition continue processing. If subsequent messages depend on the failed message, compensation logic is required.

---

## Asynchronous workload offloading and poll liveness

### Implementation

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/asyncpoll/ReportTask.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/asyncpoll/SafeReportGenerationListener.java"
```

### Why it works

1. **Poll Loop Unblocking**: The heavy PDF rendering is offloaded to a bounded background worker executor (`CompletableFuture.runAsync(...)`). The consumer poll thread returns quickly to continue invoking `KafkaConsumer.poll()`.
2. **Rebalance Storm Elimination**: Because consecutive calls to `poll()` occur well within `max.poll.interval.ms`, the consumer coordinator maintains healthy group membership without triggering false-positive partition revocations.
3. **Async Acknowledgment**: The Kafka offset is acknowledged upon completion of the asynchronous task.

### Trade-offs

- **In-Flight Concurrency Control**: Unbounded async task spawning could saturate JVM memory; background executors must be bounded with appropriate rejection policies.

---

## Safe order processing orchestrator

### Implementation

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/orderprocessing/OrderEntity.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/orderprocessing/OrderPlacedEvent.java"
```

```java
--8<-- "modules/14-kafka/src/main/java/lab/kafka/orderprocessing/SafeOrderOrchestrator.java"
```

### Why it works

1. **Dual-Write Hazard Remediation**: Rather than publishing to Kafka inside an open database transaction, `placeOrder` emits an internal application event. `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` dispatches the event to Kafka strictly **after** the database commit succeeds. If the transaction rolls back, no message is ever sent.
2. **Guaranteed Partition Affinity**: Always supplies `event.orderId()` as the partition key.
3. **Idempotent Consumer Deduplication**: `deduplicationStore.tryMarkFulfillmentStarted(orderId)` prevents double fulfillment and duplicate inventory reservations.
4. **Dead Letter Recovery**: Failed fulfillments update order status to `FAILED` and log the incident to an audit service.

### Trade-offs

- **At-Least-Once Handover**: While `@TransactionalEventListener(AFTER_COMMIT)` prevents dirty publishes on rollback, a JVM crash between DB commit and Kafka send could lose the event. For absolute zero-loss guarantees, a persistent Transactional Outbox table with Change Data Capture (CDC) should be adopted.
