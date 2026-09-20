# Module 14 — Kafka

This module contains practical implementations, unit tests, and broken code review exercises for
Apache Kafka and Spring Kafka: offset commit semantics, consumer idempotency, partition key ordering,
Dead Letter Topic (DLT) error handling, poll timeout and rebalance safety, and transactional outbox integration.

Full theory, concepts, internals diagrams, interview questions, and deep walkthroughs live in the documentation:

👉 **[Kafka Documentation](../../docs/topics/kafka/index.md)**

## Structure

- `broken-examples/`: Intentionally flawed Kafka implementations for code review practice
  - `ack-before-processing/`: Premature offset commit before processing completion losing messages on failure
  - `non-idempotent-consumer/`: Duplicate message delivery causing double mutations without idempotency key
  - `assuming-global-ordering/`: Missing partition key round-robining events and breaking sequential ordering
  - `infinite-retries-no-dlt/`: Poison pill message triggering infinite crash loop without dead-letter routing
  - `blocking-listener-poll-timeout/`: Long blocking listener exceeding `max.poll.interval.ms` triggering rebalance storm
  - `order-processing-v1/`: Combined pull request mixing transactional dual-writes, missing DLT, and keying errors
- `src/main/java/lab/kafka/`: Production-grade correct implementations
- `src/test/java/lab/kafka/`: Fast unit tests
- `src/integrationTest/java/lab/kafka/`: Real Kafka Testcontainers integration tests
- `src/examples/java/lab/kafka/questions/`: Standalone compilable question verification classes
