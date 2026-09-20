# Distributed Data Tests

Testing distributed data patterns requires validating local transaction atomicity, asynchronous broker relaying, and consumer deduplication across real infrastructure.

## Testing Strategy

1. **Fast Unit Testing with Mockito**: Proves outbox publisher loop logic, inbox state transitions, and Saga compensation rollback ordering without spinning up Docker containers.
2. **End-to-End Infrastructure Testing**: Uses Testcontainers (`SharedPostgresContainer` and `SharedKafkaContainer`) to run a real PostgreSQL database and Apache Kafka broker:
   - Verifies SQL schemas and `SKIP LOCKED` query execution.
   - Proves transactional outbox insertion, Kafka publishing, and asynchronous consumption.
   - Proves atomic inbox deduplication on duplicate redeliveries.

## Test Suite Overview

```bash
# Fast unit tests (no Docker required)
./gradlew :modules:19-distributed-data-patterns:test

# Full PostgreSQL + Kafka Testcontainers integration tests
./gradlew :modules:19-distributed-data-patterns:integrationTest
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `OutboxPublisherTest` | Outbox records marked processed only after broker returns successful `RecordMetadata` | Mockito unit verification |
| `SafeInventoryConsumerTest` | Inbox lease acquisition allows first delivery and skips business mutation on duplicates | Mockito unit verification |
| `SagaCoordinatorTest` | Multi-step saga executes in order and rolls back completed steps in reverse order on failure | Mockito unit verification |
| `DistributedDataIntegrationTest` | End-to-end Outbox + Inbox pipeline: PostgreSQL transactional write, Kafka publishing, and idempotent deduplication | Testcontainers PostgreSQL + Kafka + Awaitility |

## Key Test Snippets

### Proving Outbox Publisher Broker Acknowledgment

```java
--8<-- "modules/19-distributed-data-patterns/src/test/java/lab/distributeddata/outbox/OutboxPublisherTest.java"
```

### Proving Consumer Deduplication with Inbox Pattern

```java
--8<-- "modules/19-distributed-data-patterns/src/test/java/lab/distributeddata/inbox/SafeInventoryConsumerTest.java"
```

### Proving End-to-End Flow with PostgreSQL and Kafka

```java
--8<-- "modules/19-distributed-data-patterns/src/integrationTest/java/lab/distributeddata/DistributedDataIntegrationTest.java"
```

## Related

- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Production](production.md)
