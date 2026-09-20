# Distributed Data Exercises

Hands-on exercises to master outbox polling with `SKIP LOCKED`, consumer deduplication, and multi-step Saga coordination.

## Exercise 1: Outbox Polling Worker with SKIP LOCKED

### Problem

In a distributed cluster of multiple application pods, implement an outbox worker that locks a batch of pending events, publishes them to Kafka, and updates their status to `PROCESSED` without causing lock contention or deadlocks with sibling pods.

### Requirements

1. Use `SELECT FOR UPDATE SKIP LOCKED` to acquire available rows.
2. Synchronously verify Kafka broker publication (`future.join()`).
3. Update status to `PROCESSED` upon successful delivery.
4. Rollback transaction and keep status `PENDING` if broker send fails.

??? question "Reveal solution"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/outbox/OutboxPublisher.java"
    ```

---

## Exercise 2: Idempotent Saga Coordinator with Rollback

### Problem

Implement an orchestrated Saga coordinator executing a sequence of transactional steps. If an intermediate step fails, the coordinator must halt forward execution and execute compensating actions for all completed steps in reverse order ($C_{n-1} \dots C_1$).

### Requirements

1. Model each step with `execute()` and `compensate()` methods.
2. Track all successfully executed steps.
3. On failure, invoke `compensate()` in reverse order and record execution metrics.
4. Ensure compensation logic is idempotent across repeated executions.

??? question "Reveal solution"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/main/java/lab/distributeddata/saga/SagaCoordinator.java"
    ```

---

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Solutions](solutions.md)
- [Tests](tests.md)
- [Production](production.md)
