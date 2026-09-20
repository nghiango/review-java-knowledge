# Code Review — Outbox Processing PR (Premature Deletion & Unbounded Batches)

## Context

An engineer submitted a PR implementing a scheduled outbox publisher to read pending records from the database outbox table and publish them to Kafka.

Review `OrderFulfillmentCoordinator.java` for message loss risks, unacknowledged writes, batch locking, and fault tolerance.

## What to look for

- Order of operations: deleting from the outbox table vs receiving broker ACK
- Asynchronous future handling on `kafkaTemplate.send()`
- Concurrent worker conflicts on `fetchPending()`
