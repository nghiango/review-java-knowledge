# Distributed Data Code Review

Review each clean source before expanding its answer.

## Dual write to database and message broker

An e-commerce order service creates customer orders in an RDBMS database and broadcasts an `OrderCreated` event to Kafka so inventory, payment, and notification microservices can begin processing.

```java
--8<-- "modules/19-distributed-data-patterns/broken-examples/dual-write-db-broker/OrderCreationService.java"
```

Consider consistency guarantees between the database commit and message broker publication, broker crash scenarios, and transaction boundaries.

??? warning "Reveal issues"
    **Data consistency issue — Dual Write problem:** The method writes to two independent storage systems (PostgreSQL and Apache Kafka). Because there is no distributed 2PC transaction manager, if Kafka publish fails or times out, the DB transaction rolls back. But if the DB commit fails after `kafkaTemplate.send()`, the message is published to Kafka for an uncommitted order. Conversely, if the JVM crashes after DB commit, the event is permanently lost, causing silent data inconsistency.

    **Transaction issue — Network I/O inside open database transaction:** `kafkaTemplate.send()` executes while the PostgreSQL transaction is open, holding database connections during network latency.

    **Reliability issue — Fire-and-forget send:** The method does not wait for broker acknowledgment or handle asynchronous delivery failures.

[Correct implementation](solutions.md#transactional-outbox-pattern)

---

## Non-idempotent Saga compensation

In a distributed Saga orchestrating an order checkout, if inventory reservation fails downstream, the orchestrator triggers a compensating transaction to refund the customer's account balance.

```java
--8<-- "modules/19-distributed-data-patterns/broken-examples/non-idempotent-compensation/PaymentCompensationService.java"
```

Consider idempotency guarantees when compensation events are redelivered by message brokers, duplicate execution protection, and lost updates.

??? warning "Reveal issues"
    **Data consistency issue — Non-idempotent compensation:** Sagas rely on at-least-once message delivery. If the compensation event is redelivered (due to network timeout, consumer rebalance, or orchestrator retry), `compensateFailedOrder` executes multiple times, crediting the customer's account balance repeatedly.

    **Concurrency issue — Unlocked balance mutation:** Reading account balance via `findById()` followed by `accountRepository.save()` without pessimistic locking or optimistic `@Version` checks causes lost updates under concurrent transactions.

    **Reliability issue — Missing compensation deduplication:** The method fails to check if a refund for `orderId` was already recorded before applying balance modifications.

[Correct implementation](solutions.md#idempotent-saga-compensation)

---

## Inbox without deduplication key

An inventory microservice consumes `OrderPlacedEvent` messages from Kafka to allocate physical warehouse items for order fulfillment.

```java
--8<-- "modules/19-distributed-data-patterns/broken-examples/inbox-without-dedup-key/InventoryAllocationConsumer.java"
```

Consider consumer idempotency, at-least-once message delivery, and phantom inventory loss.

??? warning "Reveal issues"
    **Data consistency issue — Non-idempotent consumer:** Kafka provides at-least-once delivery. On consumer rebalances, container restarts, or offset commit timeouts, identical events are redelivered. Decrementing stock directly without checking an Inbox table decrements stock multiple times for the same order.

    **Reliability issue — Missing deduplication store:** The listener fails to track processed event IDs in an atomic inbox table, creating silent discrepancies between physical warehouse inventory and database state.

[Correct implementation](solutions.md#inbox-pattern-and-idempotent-consumer)

---

## Outbox processing PR (premature deletion & unbounded batches)

An engineer submitted a PR implementing a scheduled outbox publisher to read pending records from the database outbox table and publish them to Kafka.

```java
--8<-- "modules/19-distributed-data-patterns/broken-examples/combined-pr-saga-outbox/OrderFulfillmentCoordinator.java"
```

Consider order of operations, outbox table deletion vs broker confirmation, and concurrency across multiple worker instances.

??? warning "Reveal issues"
    **Reliability issue — Premature deletion before broker acknowledgment:** The publisher deletes the outbox entry from the database *prior* to sending the message to Kafka. If the broker rejects the message, times out, or the process crashes during `kafkaTemplate.send()`, the outbox event is permanently lost.

    **Reliability issue — Asynchronous send without error handling:** `kafkaTemplate.send()` returns a `CompletableFuture` asynchronously without checking the result or attaching callbacks.

    **Concurrency issue — Unlocked batch polling:** `fetchPending()` performs an unconstrained query without `FOR UPDATE SKIP LOCKED`, causing multiple concurrent outbox workers to process the exact same rows and produce duplicate Kafka messages.

[Correct implementation](solutions.md#transactional-outbox-pattern)
