# Kafka Code Review

Review each clean source before expanding its answer.

## Acknowledging offset before processing completion

A payment service listens to Kafka topic `payment-notifications`. Upon receiving a notification, it confirms settlement with an external payment gateway over HTTP and persists the final settled status in a database.

```java
--8<-- "modules/14-kafka/broken-examples/ack-before-processing/PaymentNotification.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/ack-before-processing/PaymentNotificationConsumer.java"
```

Consider message loss hazards, offset commit timing relative to business logic, and exception swallowing.

??? warning "Reveal issues"
    **Reliability issue — Premature offset acknowledgment:** Acknowledging the Kafka message offset (`ack.acknowledge()`) before executing downstream business operations commits the offset prematurely. If the process crashes or an error occurs during HTTP calls or DB saves, the message is permanently lost, degrading to at-most-once delivery.

    **Error handling issue — Swallowing exceptions:** Catching and swallowing generic `Exception` without rethrowing or propagating to Spring Kafka's error handler prevents automatic retries and dead-letter routing.

[Correct implementation](solutions.md#safe-manual-offset-acknowledgment)

---

## Non-idempotent consumer processing

A core banking credit service consumes credit instructions from Kafka topic `account-credits`. For each message received, it adds funds to the target user account balance and appends an entry to the audit log.

```java
--8<-- "modules/14-kafka/broken-examples/non-idempotent-consumer/CreditCommand.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/non-idempotent-consumer/AccountCreditConsumer.java"
```

Consider at-least-once delivery redeliveries, duplicate command execution, and atomic deduplication.

??? warning "Reveal issues"
    **Data consistency issue — Missing consumer idempotency:** Kafka guarantees at-least-once delivery by default. On network timeouts, consumer group rebalances, or producer retries, the identical `CreditCommand` will be redelivered. Incrementing the account balance directly without checking whether `transactionId` was already processed leads to double-crediting customer funds.

[Correct implementation](solutions.md#idempotent-consumer-with-deduplication-store)

---

## Assuming global ordering across partitions

An identity service publishes user lifecycle events (`USER_CREATED`, `USER_UPDATED`, `USER_DELETED`) to a multi-partition topic `user-lifecycle-events`. A downstream consumer updates local user state.

```java
--8<-- "modules/14-kafka/broken-examples/assuming-global-ordering/UserEvent.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/assuming-global-ordering/UserEventProducer.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/assuming-global-ordering/UserActivityConsumer.java"
```

Consider partition routing, ordering boundaries, null-key distribution, and concurrent consumer threads.

??? warning "Reveal issues"
    **Data consistency issue — Missing partition key:** The producer calls `kafkaTemplate.send(TOPIC, event)` without specifying a message key. Unkeyed messages are scattered across partitions. Kafka only guarantees ordering within a single partition.

    **Concurrency issue — Out-of-order execution:** The consumer runs with `concurrency = 3`. Because user events are distributed across different partitions, `USER_DELETED` on Partition 1 can be processed before `USER_CREATED` on Partition 0, corrupting user state.

[Correct implementation](solutions.md#partition-key-ordering-guarantees)

---

## Infinite retries without Dead Letter Topic

A warehouse fulfillment listener processes physical order dispatch requests from `order-fulfillments`. A custom error handler is configured on the listener container factory.

```java
--8<-- "modules/14-kafka/broken-examples/infinite-retries-no-dlt/FulfillmentPayload.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/infinite-retries-no-dlt/OrderFulfillmentConsumer.java"
```

Consider poison pill payloads, deterministic validation failures, retry limits, and partition starvation.

??? warning "Reveal issues"
    **Reliability issue — Infinite retry crash loop:** `DefaultErrorHandler` is configured with `FixedBackOff.UNLIMITED_ATTEMPTS` and no `DeadLetterPublishingRecoverer`. When a malformed message (e.g. invalid zip or negative quantity) arrives, the consumer retries forever every 500ms.

    **Reliability issue — Partition starvation:** Because the offset cannot advance past the failing message, all subsequent healthy messages in that partition are blocked indefinitely.

[Correct implementation](solutions.md#bounded-retries-with-dead-letter-topic-routing)

---

## Blocking listener thread exceeding max poll interval

An analytics service listens to Kafka topic `report-generation-tasks` to render complex PDF reports and upload them to cloud storage.

```java
--8<-- "modules/14-kafka/broken-examples/blocking-listener-poll-timeout/ReportTask.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/blocking-listener-poll-timeout/ReportGenerationListener.java"
```

Consider execution duration versus `max.poll.interval.ms`, consumer group coordinator timeouts, and rebalance loops.

??? warning "Reveal issues"
    **Reliability issue — Blocking listener exceeds max.poll.interval.ms:** Running heavy CPU/IO rendering (8–10 minutes) directly on the listener thread exceeds Kafka's default `max.poll.interval.ms` (5 minutes). The broker coordinator marks the consumer dead, revokes its partitions, and triggers a rebalance.

    **Reliability issue — CommitFailedException and rebalance storms:** When the original thread finally completes and attempts to commit, Kafka throws `CommitFailedException`. Meanwhile, the newly assigned consumer instance starts the heavy report again, sparking an endless rebalance storm across the cluster.

[Correct implementation](solutions.md#asynchronous-workload-offloading-and-poll-liveness)

---

## Order processing service v1 (Combined PR exercise)

A combined pull request implementing order creation and asynchronous inventory fulfillment for an e-commerce platform.

```java
--8<-- "modules/14-kafka/broken-examples/order-processing-v1/OrderEntity.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/order-processing-v1/OrderPlacedEvent.java"
```

```java
--8<-- "modules/14-kafka/broken-examples/order-processing-v1/InsecureOrderProcessor.java"
```

Consider transactional boundaries, dual-write hazards, partition keys, idempotency, and error handling.

??? warning "Reveal issues"
    **Data consistency issue — Dual-write hazard inside database transaction:** `kafkaTemplate.send(TOPIC, event)` is invoked inside an open `@Transactional` method. If the database transaction rolls back later (e.g. status update fails or constraint error), the Kafka event cannot be recalled, creating phantom orders.

    **Data consistency issue — Missing partition key:** Publishing without `orderId` as the record key scatters events across partitions, breaking sequential ordering for order lifecycle updates.

    **Data consistency issue — Non-idempotent consumer:** The listener reserves inventory without checking if the order was already processed, causing duplicate inventory deductions on redelivery.

    **Error handling issue — Swallowing exceptions:** Swallowing exceptions in the listener hides inventory failures and prevents Dead Letter Topic routing.

[Correct implementation](solutions.md#safe-order-processing-orchestrator)
