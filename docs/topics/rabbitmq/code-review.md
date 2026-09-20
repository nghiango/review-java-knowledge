# RabbitMQ Code Review

Review each clean source before expanding its answer.

## Auto-ack before processing completion

An e-commerce order fulfillment service listens to RabbitMQ queue `order.confirmations`. When an order notification arrives, it validates authorization with an external payment gateway and records the confirmed state in a database.

```java
--8<-- "modules/15-rabbitmq/broken-examples/auto-ack-before-processing/OrderNotification.java"
```

```java
--8<-- "modules/15-rabbitmq/broken-examples/auto-ack-before-processing/OrderConfirmationConsumer.java"
```

Consider the timing of `channel.basicAck()` relative to downstream operations, delivery guarantees (at-most-once vs at-least-once), and worker crash resilience.

??? warning "Reveal issues"
    **Reliability issue — premature basicAck before downstream operations complete:** `channel.basicAck(deliveryTag, false)` is issued immediately before calling `paymentClient.verifyAuthorization()` and `orderRepository.markConfirmed()`. Once acknowledged, RabbitMQ discards the message from broker storage. If the worker JVM crashes, OOMs, or throws an unhandled exception during verification or DB persistence, the message is permanently lost, degrading processing semantics to fragile at-most-once delivery.

    **Error handling issue — swallowing generic exceptions prevents retry and dead-letter routing:** Catching and swallowing `Exception` prevents Spring AMQP container error handlers, dead-letter exchanges (DLX), and monitoring metrics from capturing unfulfilled order notifications.

[Correct implementation](solutions.md#manual-acknowledgment-after-processing)

---

## Infinite requeue poison pill loop

An invoice processing service consumes messages from queue `invoices.incoming`. It validates VAT compliance and persists the invoice record to an internal accounting ledger.

```java
--8<-- "modules/15-rabbitmq/broken-examples/infinite-requeue-loop/InvoiceTask.java"
```

```java
--8<-- "modules/15-rabbitmq/broken-examples/infinite-requeue-loop/InvoiceProcessingConsumer.java"
```

Consider consumer acknowledgment handling upon encountering malformed or invalid business payloads.

??? warning "Reveal issues"
    **Reliability issue — unconditional basicNack with requeue=true causes poison pill crash loops:** When a deterministic validation error occurs (such as an invalid VAT number or malformed payload), `channel.basicNack(deliveryTag, false, true)` immediately returns the message to the head of the queue. The consumer receives the exact same payload in tight sub-millisecond loops, spiking CPU utilization to 100%, generating millions of log lines, and blocking all valid invoices queued behind the poison pill.

    **Reliability fix — bounded retries and dead-letter rejection:** Implement bounded retries using Spring RabbitMQ `RetryOperationsInterceptor` or DLX routing with `basicReject(deliveryTag, false)` or `basicNack(deliveryTag, false, false)` upon exhausting retries or encountering unrecoverable business errors.

[Correct implementation](solutions.md#bounded-retries-and-dead-letter-exchange-routing)

---

## No dead-letter queue for expired and rejected messages

An operations alert service configures a TTL on queue `ops.alerts.incoming` to prevent backlog accumulation during downstream incident bursts.

```java
--8<-- "modules/15-rabbitmq/broken-examples/no-dlq-expired-ttl/AlertNotification.java"
```

```java
--8<-- "modules/15-rabbitmq/broken-examples/no-dlq-expired-ttl/QueueConfig.java"
```

```java
--8<-- "modules/15-rabbitmq/broken-examples/no-dlq-expired-ttl/ExpiringAlertConsumer.java"
```

Consider queue arguments, message expiration semantics, and unroutable dead-letter destinations.

??? warning "Reveal issues"
    **Reliability issue — missing dead-letter exchange discards expired and rejected messages:** The queue sets `x-message-ttl` to 60000ms and the consumer invokes `basicReject(deliveryTag, false)` on validation failures. However, because neither `x-dead-letter-exchange` nor `x-dead-letter-routing-key` is configured on the queue, expired and rejected messages are permanently dropped by RabbitMQ without audit trail, alerting, or manual recovery capability.

[Correct implementation](solutions.md#dead-letter-exchange-and-parking-lot-queue-configuration)

---

## Unbounded prefetch buffer exhausting JVM heap

A media processing pipeline processes heavy video transcoding jobs that take 30 to 60 seconds each.

```java
--8<-- "modules/15-rabbitmq/broken-examples/unbounded-prefetch-oom/TranscodingJob.java"
```

```java
--8<-- "modules/15-rabbitmq/broken-examples/unbounded-prefetch-oom/VideoTranscodingConsumer.java"
```

Consider consumer prefetch buffer settings, fair dispatch across worker nodes, and JVM memory allocation under bursty queues.

??? warning "Reveal issues"
    **Resource management issue — unbounded prefetch (prefetchCount = 0) causes consumer starvation and OutOfMemoryError:** In AMQP, setting `prefetchCount = 0` tells the broker there is no limit on unacknowledged messages. RabbitMQ pushes the entire queue contents across the TCP channel into the consumer's client buffer. For slow or memory-intensive jobs, the consumer JVM runs out of heap memory and crashes. Furthermore, other idle worker instances receive zero jobs because all messages are hoarded in the first consumer's unacknowledged buffer.

[Correct implementation](solutions.md#fair-dispatch-and-bounded-qos-prefetch)

---

## Non-idempotent consumer processing

A gamification service consumes point award events to credit user loyalty balances.

```java
--8<-- "modules/15-rabbitmq/broken-examples/non-idempotent-consumer/PointAwardCommand.java"
```

```java
--8<-- "modules/15-rabbitmq/broken-examples/non-idempotent-consumer/LoyaltyPointConsumer.java"
```

Consider network failure between message processing and broker acknowledgment receipt, message redeliveries, and balance mutation semantics.

??? warning "Reveal issues"
    **Data consistency issue — non-idempotent consumer awards duplicate points on redelivery:** RabbitMQ provides at-least-once message delivery. If the consumer processes `accountRepository.addPoints()` successfully but crashes or experiences a network partition before `channel.basicAck()` reaches the broker, RabbitMQ redelivers the message to another consumer with `redelivered = true`. Without message deduplication or an idempotency key check on `commandId`, the user receives duplicate points.

[Correct implementation](solutions.md#idempotent-consumer-with-deduplication-store)

---

## Fire-and-forget publishing

A security audit service publishes critical authentication and authorization events to RabbitMQ.

```java
--8<-- "modules/15-rabbitmq/broken-examples/fire-and-forget-publisher/AuditMessage.java"
```

```java
--8<-- "modules/15-rabbitmq/broken-examples/fire-and-forget-publisher/AuditLogPublisher.java"
```

Consider broker network disconnections, disk alarms, and unroutable messages with standard `RabbitTemplate.convertAndSend()`.

??? warning "Reveal issues"
    **Reliability issue — fire-and-forget publishing risks silent message loss:** `rabbitTemplate.convertAndSend()` writes bytes to the TCP socket buffer and returns immediately. If the broker is restarting, under disk alarm, or rejecting writes, no exception is raised and the message is lost. Additionally, if the routing key does not match any queue binding, RabbitMQ drops unroutable messages silently unless the `mandatory` flag and a returns callback are configured.

[Correct implementation](solutions.md#correlated-publisher-confirms-and-returns-handling)

---

## Related

- [Solutions](solutions.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Tests](tests.md)
- [Production](production.md)
