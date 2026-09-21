# Exercises: Spring Transactions Katas in Spring Boot 4

!!! info "Delta from baseline"
    Baseline exercises in [`docs/topics/spring-transactions/exercises.md`](../../../topics/spring-transactions/exercises.md) cover propagation levels and proxy self-invocation fixes.
    These exercises focus on **Spring Boot 4 / Java 25 transaction coordination**: refactoring multi-threaded operations to transactional outbox and propagating metadata with `ScopedValue`.

---

## Exercise 1: Refactor Multi-Threaded Batch Processor

### Problem Statement
A batch processing service creates a `StructuredTaskScope` inside `@Transactional` and inserts records across 20 forked virtual threads. When a single record validation fails, the parent transaction rolls back but child inserts persist.

### Requirements
1. Remove `StructuredTaskScope` from within the database transaction.
2. Refactor the database persistence to execute in a single atomic batch or sequential transaction.
3. If parallel processing is needed for external notifications, publish events post-commit using `@TransactionalEventListener(phase = AFTER_COMMIT)`.

---

## Exercise 2: Context Propagation with ScopedValue

### Problem Statement
An asynchronous auditing listener running on virtual threads attempts to read transaction correlation IDs from `ThreadLocal`, finding `null`.

### Requirements
1. Define a static constant `ScopedValue<AuditMetadata> CURRENT_AUDIT = ScopedValue.newInstance()`.
2. Wrap transaction execution with `ScopedValue.where(CURRENT_AUDIT, meta).run(...)`.
3. Fork virtual workers inside a `StructuredTaskScope` and verify that `CURRENT_AUDIT.get()` is available with zero context loss.
