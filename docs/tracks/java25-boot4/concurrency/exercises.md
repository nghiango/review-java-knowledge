# Exercises: Concurrency Katas in Java 25

!!! info "Delta from baseline"
    Baseline exercises in [`docs/topics/concurrency/exercises.md`](../../../topics/concurrency/exercises.md) cover thread pool tuning and lock contention.
    These exercises focus on mastering **Java 25 Concurrency constructs**: migrating CompletableFuture pipelines to `StructuredTaskScope`, and replacing `ThreadLocal` context holders with `ScopedValue`.

---

## Exercise 1: Migrate Fan-Out to StructuredTaskScope

### Problem Statement
You have an asynchronous aggregator fetching user profile and order history using `CompletableFuture.supplyAsync()`. Refactor it to `StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())`.

### Requirements
1. Fork profile and order tasks as subtasks in a single scope.
2. If the user profile fetch throws `UserNotFoundException`, verify that the order history subtask is cancelled immediately.
3. Combine the results into a single record.

---

## Exercise 2: Tenant Context Propagation with ScopedValue

### Problem Statement
A multitenant filter currently uses `InheritableThreadLocal` to propagate tenant headers across downstream services and background threads.

### Requirements
1. Declare a static constant `ScopedValue<TenantInfo> CURRENT_TENANT = ScopedValue.newInstance();`.
2. Wrap request processing in `ScopedValue.where(CURRENT_TENANT, tenant).run(...)`.
3. Verify that spawning virtual child threads inside a `StructuredTaskScope` automatically inherits `CURRENT_TENANT` without manual context copying.
