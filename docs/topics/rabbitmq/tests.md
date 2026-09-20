# RabbitMQ Tests

Testing AMQP applications requires verifying both business logic state transitions and broker communication semantics under happy paths and failure scenarios.

## Testing Strategy

1. **Fast Unit Testing with Mockito**: Isolates consumer acknowledgment logic (`basicAck`, `basicNack`, `basicReject`), deduplication storage, and publisher callback execution without starting external containers.
2. **Contract Testing with Testcontainers**: Uses `SharedRabbitContainer` (singleton `rabbitmq:3.13-alpine`) to verify real AMQP topology declaration, message routing across direct/topic exchanges, publisher confirms, and dead-letter queue (DLX) delivery.
3. **Fault Injection & Async Assertions**: Simulates unroutable routing keys, message rejections, and broker timeouts using Awaitility for asynchronous queue delivery assertions.

## Test Suite Overview

```bash
# Fast unit tests (no Docker required)
./gradlew :modules:15-rabbitmq:test

# Integration tests with Testcontainers (Docker required)
./gradlew :modules:15-rabbitmq:integrationTest
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `OrderConfirmationConsumerTest` | `basicAck` executes only after downstream payment and DB succeed; `basicReject` on failure | Mockito unit verification |
| `SafeInvoiceProcessingConsumerTest` | Deterministic validation errors reject without requeuing; transient errors retry | Mockito unit verification |
| `DeadLetterConfigurationTest` | Queue arguments declare `x-dead-letter-exchange` and `x-dead-letter-routing-key` | Spring AMQP queue argument assertions |
| `FairTranscodingConsumerTest` | Fair QoS prefetch processes one job at a time without accumulating unacknowledged work | Unit state assertions |
| `SafeLoyaltyPointConsumerTest` | Duplicate redelivered messages are ignored by deduplication store | Unit deduplication assertions |
| `SafeAuditLogPublisherTest` | Correlated publisher confirms and unroutable returns handling notify callers | Mockito publisher assertions |
| `RabbitMqIntegrationTest` | Publisher confirms, manual ACK removal, and DLX rejection routing against real RabbitMQ broker | Testcontainers + Awaitility |

## Key Test Snippets

### Proving Manual Acknowledgment After Downstream Operations

```java
--8<-- "modules/15-rabbitmq/src/test/java/lab/rabbitmq/manualack/OrderConfirmationConsumerTest.java"
```

### Proving Poison Pill Non-Requeue to DLX

```java
--8<-- "modules/15-rabbitmq/src/test/java/lab/rabbitmq/retrydlx/SafeInvoiceProcessingConsumerTest.java"
```

### Proving Consumer Idempotency on Redeliveries

```java
--8<-- "modules/15-rabbitmq/src/test/java/lab/rabbitmq/idempotent/SafeLoyaltyPointConsumerTest.java"
```

### Proving Real RabbitMQ Topology and DLX Routing with Testcontainers

```java
--8<-- "modules/15-rabbitmq/src/integrationTest/java/lab/rabbitmq/RabbitMqIntegrationTest.java"
```

## Related

- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Production](production.md)
