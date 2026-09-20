# RabbitMQ Solutions

Production-grade implementations corresponding to the code review exercises.

## Manual acknowledgment after processing

### Implementation

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/manualack/OrderNotification.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/manualack/SafeOrderConfirmationConsumer.java"
```

### Why it works

1. **At-Least-Once Delivery**: `channel.basicAck(deliveryTag, false)` is executed only after both external payment verification and database order confirmation succeed.
2. **Crash Resilience**: If the consumer JVM terminates abruptly during processing, unacknowledged messages return to RabbitMQ upon TCP socket closure and are redelivered to healthy nodes.
3. **Explicit Error Routing**: On processing failure, `channel.basicReject(deliveryTag, false)` routes the failed notification directly to dead-letter storage rather than dropping it or blindly acknowledging it.

### Trade-offs

Manual acknowledgments require downstream operations to be idempotent, as network disconnects between execution and broker ACK can cause redeliveries.

---

## Bounded retries and dead-letter exchange routing

### Implementation

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/retrydlx/InvoiceTask.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/retrydlx/SafeInvoiceProcessingConsumer.java"
```

### Why it works

1. **Transient vs Deterministic Separation**: Differentiates between recoverable transient exceptions (network glitch, DB lock timeout) and permanent validation failures (corrupted VAT format).
2. **Immediate Rejection of Poison Pills**: Deterministic errors invoke `channel.basicReject(deliveryTag, false)` immediately without retrying, preventing poison pill loops.
3. **Bounded Retries with DLX Fallback**: Transient failures are tracked using delivery attempts or AMQP `x-death` headers. Once maximum attempts are exceeded, the message is routed to DLX with `requeue = false`.

### Trade-offs

Requires maintaining attempt metadata (either in memory, cache, or AMQP headers) and provisioning dedicated dead-letter queues for unrecoverable invoices.

---

## Dead-letter exchange and parking lot queue configuration

### Implementation

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/dlx/AlertNotification.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/dlx/DeadLetterConfiguration.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/dlx/SafeExpiringAlertConsumer.java"
```

### Why it works

1. **Declarative DLX Binding**: `x-dead-letter-exchange` and `x-dead-letter-routing-key` arguments configure the broker to automatically route expired (`x-message-ttl`), rejected, or queue-length-exceeded messages to the dead-letter exchange.
2. **Dedicated Parking Lot**: The DLX routes dead letters to a parking lot queue (`ops.alerts.dlq`), preserving message payload, error headers, and timestamps for operational triage.
3. **Zero Silent Data Loss**: Messages exceeding the 60-second TTL are preserved indefinitely in the DLQ until reviewed or replayed.

### Trade-offs

Dead-letter queues accumulate messages over time and require disk space alerting, monitoring, and automated or manual replay tooling.

---

## Fair dispatch and bounded QoS prefetch

### Implementation

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/prefetch/TranscodingJob.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/prefetch/FairTranscodingConsumer.java"
```

### Why it works

1. **Bounded Heap Footprint**: Setting `prefetchCount = 1` enforces strict fair dispatch in AMQP. The broker pushes only one unacknowledged message to the worker at a time.
2. **Even Work Distribution**: Fast workers that complete transcoding quickly receive new jobs immediately, while slow workers do not hoard unstarted jobs.
3. **OOM Prevention**: JVM heap is not saturated with queued media payloads, ensuring stability even when queues hold millions of messages.

### Trade-offs

A low prefetch value ($1$) introduces minor network roundtrip latency between messages. For very high-throughput, lightweight messages (e.g. telemetry metrics), prefetch values of $50-250$ are preferred over $1$.

---

## Idempotent consumer with deduplication store

### Implementation

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/idempotent/PointAwardCommand.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/idempotent/DeduplicationStore.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/idempotent/SafeLoyaltyPointConsumer.java"
```

### Why it works

1. **Distributed Deduplication**: An atomic `store.putIfAbsent(commandId)` or database unique constraint checks whether `commandId` has already been processed before mutating user balances.
2. **Redelivery Safe**: When RabbitMQ redelivers a duplicate message (`redelivered = true`), the duplicate is detected, points are not re-awarded, and the message is acknowledged safely.
3. **Transactional Integrity**: Balances and idempotency markers can be updated atomically within the same database transaction.

### Trade-offs

Requires allocating storage for deduplication records with appropriate TTL policies (e.g., retaining idempotency keys for 7–30 days).

---

## Correlated publisher confirms and returns handling

### Implementation

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/publisher/AuditMessage.java"
```

```java
--8<-- "modules/15-rabbitmq/src/main/java/lab/rabbitmq/publisher/SafeAuditLogPublisher.java"
```

### Why it works

1. **Correlated Confirms**: `CorrelationData` tracks publication state. The publisher awaits broker acknowledgment before reporting success, guaranteeing disk persistence on durable queues.
2. **Unroutable Message Capture**: Setting `mandatory = true` with a `ReturnsCallback` captures messages that reach the exchange but have no matching queue bindings, triggering immediate fallback alerting.
3. **Bounded Timeout**: Publisher confirm futures enforce strict timeouts (e.g. 5 seconds) to prevent caller threads from hanging indefinitely during broker partitions.

### Trade-offs

Publisher confirms introduce network and disk fsync latency on the broker, reducing raw publish throughput compared to fire-and-forget publishing.

---

## Related

- [Code Review](code-review.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Tests](tests.md)
- [Production](production.md)
