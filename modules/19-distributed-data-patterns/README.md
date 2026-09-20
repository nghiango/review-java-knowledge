# Module 19 — Distributed Data Patterns

This module contains practical implementations, unit tests, and broken code review exercises for
distributed data patterns in microservice architectures: distributed transactions, Two-Phase Commit (2PC) limitations,
Saga patterns (orchestration vs choreography, compensation idempotency), Transactional Outbox pattern,
Change Data Capture (CDC / Debezium concepts), Inbox pattern for consumer deduplication, and eventual consistency guarantees.

Full theory, concepts, internals diagrams, interview questions, and deep walkthroughs live in the documentation:

👉 **[Distributed Data Patterns Documentation](../../docs/topics/distributed-data-patterns/index.md)**

## Structure

- `broken-examples/`: Intentionally flawed distributed data implementations for code review practice
  - `dual-write-db-broker/`: Direct dual write to DB and Kafka without transactional outbox causing silent data loss
  - `non-idempotent-compensation/`: Non-idempotent Saga compensation issuing duplicate refunds on redelivery
  - `inbox-without-dedup-key/`: Consumer processing incoming domain events without atomic deduplication checking
  - `combined-pr-saga-outbox/`: Complex PR combining outbox premature deletion, missing DB transactions, and compensation loops
- `src/main/java/lab/distributeddata/`: Production-grade correct implementations
  - `outbox/`: Transactional Outbox entity, repository, and polling publisher with broker ACK acknowledgment
  - `inbox/`: Atomic inbox deduplication entity, repository, and idempotent event consumer
  - `saga/`: Orchestrated Order Saga coordinator managing multi-step execution with idempotent compensation
- `src/test/java/lab/distributeddata/`: Fast unit tests for outbox publishing, inbox deduplication, and saga compensation
- `src/integrationTest/java/lab/distributeddata/`: End-to-end Testcontainers PostgreSQL + Kafka integration test
- `src/examples/java/lab/distributeddata/questions/`: Standalone compilable question verification classes
