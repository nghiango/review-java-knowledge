# Distributed Data Solutions

Production-grade implementations corresponding to the code review exercises.

## Transactional Outbox Pattern

### Implementation

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/outbox/OutboxEvent.java"
```

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/outbox/OutboxRepository.java"
```

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/outbox/JdbcOutboxRepository.java"
```

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/outbox/OutboxPublisher.java"
```

### Why it works

1. **Atomic Local Persistence**: Outbox events are written in the exact same database transaction as the business entity update via `JdbcOutboxRepository.save()`. This eliminates the Dual Write dilemma entirely.
2. **Lock Contention Free Polling**: Uses `FOR UPDATE SKIP LOCKED` in `lockPendingBatch()`, enabling multiple publisher pods to poll concurrently without lock contention or deadlocks.
3. **Guaranteed Broker Confirmation**: Blocks on `future.join()` to verify broker disk fsync (`RecordMetadata`) before executing `markProcessed()`. If the broker rejects the write, the outbox record remains `PENDING` and is retried.

### Trade-offs

Introduces slight asynchronous latency between database commit and message broker arrival ($100\text{ms} - 500\text{ms}$). Requires downstream consumers to handle duplicate deliveries.

---

## Inbox Pattern and Idempotent Consumer

### Implementation

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/inbox/InboxMessage.java"
```

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/inbox/InboxRepository.java"
```

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/inbox/JdbcInboxRepository.java"
```

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/inbox/SafeInventoryConsumer.java"
```

### Why it works

1. **Atomic Deduplication**: Uses `INSERT ... ON CONFLICT (message_id, consumer_group) DO NOTHING`. In PostgreSQL, this operates as an atomic test-and-set operation.
2. **Single Transaction Boundary**: The inbox lease acquisition and inventory stock decrement execute inside the same database transaction.
3. **Idempotent Redelivery Safety**: If Kafka redelivers the event, `tryAcquireLease()` returns `false`, causing `processOrderPlaced()` to acknowledge without re-applying the stock decrement.

### Trade-offs

Adds database write overhead per consumed event; requires a background cleanup job or table partitioning to prune aged inbox records.

---

## Idempotent Saga Compensation

### Implementation

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/saga/SagaStep.java"
```

```java
--8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/saga/SagaCoordinator.java"
```

### Why it works

1. **Reverse Compensation Order**: When an intermediate step fails, completed steps are compensated strictly in reverse execution order ($C_{n-1} \dots C_1$).
2. **Isolated Step Boundaries**: Each step encapsulates its own forward execution and backward compensating logic.
3. **Compensation Idempotency**: Steps verify prior compensation state before executing financial or balance modifications, ensuring redelivered compensations produce no duplicate side-effects.

### Trade-offs

Sagas provide eventual consistency rather than ACID isolation. Semantic dirty reads can occur during the execution window between forward steps and rollback.
