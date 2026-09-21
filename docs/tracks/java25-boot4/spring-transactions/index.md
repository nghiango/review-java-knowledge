# Spring Transactions in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline module [`modules/07-spring-transactions`](../../../topics/spring-transactions/index.md) covers core Spring transactions: `@Transactional` proxy mechanics, propagation levels, rollback rules, isolation levels, and HikariCP connection pool lifecycles.
    This track module teaches the **Spring Boot 4.0 / Java 25 delta**:
    
    - **Transaction Boundaries across Virtual Threads & Structured Scopes**: Why transactions are strictly thread-bound (`ThreadLocal`), the risks of invoking `StructuredTaskScope` inside `@Transactional`, and clean multi-threaded orchestration.
    - **Scoped Value Transaction Coordination**: Using Java 25 `ScopedValue` with `TransactionTemplate` for zero-cleanup, leak-proof context sharing across concurrent virtual worker pipelines.
    - **Transactional Event Listeners on Virtual Threads**: Verifying post-commit (`AFTER_COMMIT`) decoupled event processing.

---

## 1. The Transaction Architecture on Modern Stacks

| Feature | Baseline (Boot 3.5 / Java 21) | Track (Boot 4.0 / Java 25) |
|---|---|---|
| **Thread Context Storage** | `ThreadLocal` in `TransactionSynchronizationManager` | Unchanged: still thread-bound, but coordinating virtual workers requires explicit scoping |
| **Virtual Subtask Coordination** | Manual context passing or unmanaged background threads | Explicit orchestrator separation with `StructuredTaskScope` outside DB transactions |
| **Transaction Metadata Propagation** | Heavy `ThreadLocal` / MDC copies | Lightweight, stack-confined `ScopedValue` inheritance |
| **Event Listeners** | Synchronous or `@Async` thread pool dispatch | Virtual-thread-backed `@TransactionalEventListener(phase = AFTER_COMMIT)` |

---

## 2. Module Roadmap

1. [Concepts](concepts.md) — Virtual thread transaction boundaries, connection lease duration, and scoped coordination.
2. [Internals](internals.md) — `TransactionSynchronizationManager` state, connection holding, and thread confinement.
3. [Interview Questions](questions.md) — 13 questions with runnable examples.
4. [Code Review](code-review.md) — Review targets with structured scope transaction leaks and context loss.
5. [Solutions](solutions.md) — Step-by-step refactoring with production patterns.
6. [Testing Guide](tests.md) — Testing transaction rollback semantics in virtual thread environments.
7. [Production Scenarios](production.md) — Sizing HikariCP pools with virtual threads, connection leak detection, and timeout monitoring.
8. [Exercises](exercises.md) — Hands-on katas for refactoring multi-threaded transactions.
